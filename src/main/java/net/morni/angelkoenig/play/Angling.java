package net.morni.angelkoenig.play;

import net.morni.angelkoenig.AngelkoenigPlugin;
import net.morni.angelkoenig.Texts;
import net.morni.angelkoenig.Ui;
import net.morni.angelkoenig.data.PlayerData;
import net.morni.angelkoenig.fish.Bait;
import net.morni.angelkoenig.fish.Catch;
import net.morni.angelkoenig.fish.CatchRules;
import net.morni.angelkoenig.fish.DailyQuests;
import net.morni.angelkoenig.fish.DayTime;
import net.morni.angelkoenig.fish.Quest;
import net.morni.angelkoenig.fish.Rarity;
import net.morni.angelkoenig.fish.Species;
import net.morni.angelkoenig.fish.Spot;
import net.morni.angelkoenig.fish.Units;
import net.morni.angelkoenig.fish.Weather;
import net.morni.angelkoenig.world.Layout;
import net.morni.angelkoenig.world.Terrain;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Das Angeln selbst: Wartezeit beim Auswerfen, was anbeißt (Fangplatz, Tageszeit, Wetter, Angel, Köder), der Fang als
 * Gegenstand, Fischbuch, Rekorde, Aufgaben, Turnier und Netzwerk-Statistik – und der Verkauf beim Fischhändler.
 */
public final class Angling {

    public static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final int TICKS = 20;
    private static final int LURE_MIN = 20;
    private static final int LURE_MAX = 60;
    /** Ab so wenigen Ködern sagt die Aktionsleiste Bescheid. */
    private static final int BAIT_WARN = 3;
    /** So lange steht der Biss-Hinweis in der Aktionsleiste. */
    private static final long BITE_HINT_MS = 3000;
    /** Belohnung, wenn alle Arten im Fischbuch stehen. */
    static final int BOOK_COMPLETE = 1000;

    private final AngelkoenigPlugin pl;
    private final SplittableRandom random = new SplittableRandom();
    private final Map<UUID, Species> forced = new HashMap<>();
    private final Map<UUID, Double> forcedCm = new HashMap<>();
    private final Map<UUID, Map<String, Long>> pending = new HashMap<>();
    private final Set<UUID> changed = new HashSet<>();
    private final Map<UUID, Long> bites = new HashMap<>();

    public Angling(AngelkoenigPlugin pl) {
        this.pl = pl;
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public DayTime time() {
        return DayTime.of(pl.world().getTime());
    }

    public Weather weather() {
        World w = pl.world();
        return Weather.of(w.hasStorm(), w.isThundering());
    }

    /** Wettername für eine Stelle (oben am Kar und auf den Gipfeln schneit es statt zu regnen). */
    public String weatherName(String lang, Location at) {
        Weather w = weather();
        if (w != Weather.KLAR && at != null && (pl.terrain().inKar(at.getBlockX(), at.getBlockZ())
                || at.getBlockY() >= 116)) {
            return Texts.plain(lang, "weather.snow");
        }
        return Texts.plain(lang, "weather." + w.id());
    }

    /** Fangplatz des Schwimmers (nächstes Gewässer bis 2 Blöcke daneben, sonst Bergsee). */
    public Spot spotAt(Location at) {
        Terrain t = pl.terrain();
        int x = at.getBlockX();
        int z = at.getBlockZ();
        for (int r = 0; r <= 2; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    Spot s = t.spotAt(x + dx, z + dz);
                    if (s != null) {
                        return s;
                    }
                }
            }
        }
        return Spot.BERGSEE;
    }

    /** Ausgeworfen: Wartezeit nach Angel und Einstellung. */
    public void cast(Player p, FishHook hook) {
        PlayerData d = pl.data(p);
        int min = d.rod().waitTicks(pl.settings().biteMin() * TICKS);
        int max = Math.max(min, d.rod().waitTicks(pl.settings().biteMax() * TICKS));
        hook.setWaitTime(min, max);
        hook.setLureTime(LURE_MIN, LURE_MAX);
        hook.setApplyLure(false);
        hook.setSkyInfluenced(false);
        hook.setRainInfluenced(true);
    }

    /** Ein Fisch hat angebissen: kurzer Hinweis in der Aktionsleiste (auch für Bedrock und bei lauter Umgebung). */
    public void bite(Player p) {
        bites.put(p.getUniqueId(), System.currentTimeMillis());
        p.sendActionBar(Texts.text(pl.lang(p), "catch.bite"));
    }

    /** Für Tests: wie lange der letzte Biss her ist (Millisekunden, −1 = noch keiner). */
    public long sinceBite(UUID uuid) {
        Long t = bites.get(uuid);
        return t == null ? -1 : System.currentTimeMillis() - t;
    }

    /** Für Tests: der nächste Fang dieser Person ist diese Art (optional mit Länge). */
    public void force(UUID uuid, Species s, Double cm) {
        forced.put(uuid, s);
        if (cm == null) {
            forcedCm.remove(uuid);
        } else {
            forcedCm.put(uuid, cm);
        }
    }

    /** Eingeholt ohne Fang: der Biss-Hinweis verschwindet gleich wieder. */
    public void reeled(Player p) {
        Long t = bites.get(p.getUniqueId());
        if (t != null && System.currentTimeMillis() - t < BITE_HINT_MS) {
            p.sendActionBar(Component.empty());
        }
    }

    /** Ein Fisch hängt am Haken. */
    public void caught(PlayerFishEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        // der Biss-Hinweis hat ausgedient (bei seltenen Fängen kommt ein Titel statt einer neuen Aktionsleiste)
        p.sendActionBar(Component.empty());
        Location at = e.getHook().getLocation();
        String lang = pl.lang(p);
        PlayerData d = pl.data(p);
        Spot spot = spotAt(e.getHook().getLocation());
        DayTime time = time();
        Bait bait = d.bait();
        Catch c;
        Species f = forced.remove(uuid);
        if (f != null) {
            Double cm = forcedCm.remove(uuid);
            c = CatchRules.sized(f, cm == null ? f.meanCm() : cm, 1.0);
        } else {
            c = CatchRules.roll(new CatchRules.Conditions(spot, time, weather(), d.rod(), bait), random);
        }
        Bait used = bait == null ? null : d.useBait();
        LocalDate today = today();
        e.setExpToDrop(0);
        PlayerInventory inv = p.getInventory();
        boolean full = inv.firstEmpty() < 0;
        if (full) {
            e.setCancelled(true);
            e.getHook().remove();
        } else if (e.getCaught() instanceof Item item) {
            ItemStack fish = FishItems.create(c, spot, time, today, p.getName(), lang);
            item.setItemStack(fish);
            item.setOwner(uuid);
            item.setCanMobPickup(false);
            item.setPickupDelay(0);
            // Der Fang fliegt sichtbar heran; landet er im Wasser oder am Steg, kommt er trotzdem sicher in den Korb.
            pl.getServer().getScheduler().runTaskLater(pl, () -> deliver(p, item), 12L);
        }
        boolean first = !d.knows(c.species());
        boolean personal = d.addCatch(c, today);
        if (first && !c.species().junk()) {
            d.earn(c.species().rarity().firstBonus());
            if (d.speciesCount() == Species.fishes().size()) {
                d.earn(BOOK_COMPLETE);
                p.sendMessage(Texts.msg(lang, "catch.book-done", Placeholder.unparsed("n", String.valueOf(BOOK_COMPLETE))));
                celebrate(at);
            }
        }
        boolean record = !c.species().junk() && pl.records().offer(uuid, p.getName(), c, today.toEpochDay());
        pl.records().totals(uuid, p.getName(), d.catches(), d.tournamentsWon(), d.legendary());
        announce(p, lang, c, first, personal && !first, record, at);
        if (full) {
            p.sendMessage(Texts.msg(lang, "catch.full"));
        }
        if (used != null) {
            // Wie viele Köder noch da sind, steht in der Leiste rechts; die Aktionsleiste meldet sich nur, wenn sie
            // bald leer ist (sonst überdeckt sie die Fang-Nachricht im Chat)
            int left = d.baits(used);
            if (left <= BAIT_WARN) {
                p.sendActionBar(left > 0 ? Texts.text(lang, "catch.bait", Placeholder.unparsed("bait",
                        Texts.plain(lang, "bait." + used.id())), Placeholder.unparsed("n", String.valueOf(left)))
                        : Texts.text(lang, "catch.bait-empty"));
            }
            Hotbar.refreshBait(pl, p);
        }
        quests(p, Quest.Event.caught(c, spot, used != null));
        pl.tournaments().record(p, c);
        if (!c.species().junk()) {
            Map<String, Long> add = pending.computeIfAbsent(uuid, u -> new HashMap<>());
            add.merge("catches", 1L, Long::sum);
            if (c.species().rarity() == Rarity.LEGENDARY) {
                add.merge("legendary", 1L, Long::sum);
            }
        }
        changed.add(uuid);
        pl.store().save(d);
        if (record) {
            pl.saveRecords();
        }
    }

    /** Fang direkt in den Korb, falls er noch nicht aufgehoben wurde. */
    private void deliver(Player p, Item item) {
        if (!item.isValid() || !p.isOnline()) {
            return;
        }
        ItemStack stack = item.getItemStack();
        item.remove();
        for (ItemStack rest : p.getInventory().addItem(stack).values()) {
            p.getWorld().dropItem(p.getLocation(), rest).setOwner(p.getUniqueId());
        }
    }

    private void announce(Player p, String lang, Catch c, boolean first, boolean personal, boolean record, Location at) {
        Species s = c.species();
        TagResolver colour = Texts.colour(s.rarity().colour());
        String fish = Texts.plain(lang, "species." + s.id());
        String size = s.junk() ? c.weightText(lang) : Texts.raw(lang, "catch.size")
                .replace("<len>", c.lengthText(lang)).replace("<weight>", c.weightText(lang));
        p.sendMessage(Texts.msg(lang, s.junk() ? "catch.find" : "catch.msg", colour,
                Placeholder.unparsed("fish", fish), Placeholder.unparsed("size", size)));
        if (s == Species.FLASCHENPOST) {
            p.sendMessage(Texts.msg(lang, "catch.bottle", Placeholder.unparsed("note",
                    Texts.plain(lang, "bottle." + random.nextInt(5)))));
        }
        if (first && !s.junk()) {
            p.sendMessage(Texts.msg(lang, "catch.first", Placeholder.unparsed("n", String.valueOf(s.rarity().firstBonus()))));
        } else if (personal && !first) {
            p.sendMessage(Texts.msg(lang, "catch.record"));
        }
        if (record && !first) {
            p.sendMessage(Texts.msg(lang, "catch.world-record"));
        }
        if (c.trophy()) {
            p.sendMessage(Texts.msg(lang, "catch.trophy"));
        }
        Rarity r = s.rarity();
        if (r == Rarity.LEGENDARY) {
            Ui.title(p, Texts.text(lang, "catch.title", colour, Placeholder.unparsed("fish", fish)),
                    Texts.text(lang, "catch.legendary.sub"), 3500);
            Ui.sound(p, "ui.toast.challenge_complete", 1.0f);
            celebrate(at);
            for (Player o : pl.world().getPlayers()) {
                if (o.equals(p)) {
                    continue; // wer selbst fängt, hat schon Titel und eigene Zeile
                }
                String ol = pl.lang(o);
                o.sendMessage(Texts.msg(ol, "catch.legendary.all", Placeholder.unparsed("name", p.getName()),
                        Placeholder.unparsed("fish", Texts.plain(ol, "species." + s.id())),
                        Placeholder.unparsed("weight", Units.weight(c.grams(), ol))));
            }
        } else if (r.atLeast(Rarity.RARE)) {
            Ui.title(p, Texts.text(lang, "catch.title", colour, Placeholder.unparsed("fish", fish)),
                    Texts.text(lang, "catch.subtitle", Placeholder.unparsed("size", size)), 1800);
            Ui.sound(p, "block.note_block.chime", r == Rarity.EPIC ? 1.4f : 1.1f);
            p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1.2, 0), 12, 0.4, 0.5, 0.4);
        } else {
            p.getWorld().spawnParticle(Particle.SPLASH, p.getLocation().add(p.getLocation().getDirection().multiply(2))
                    .add(0, 0.5, 0), 25, 0.4, 0.2, 0.4, 0.2);
            p.sendActionBar(Texts.text(lang, "catch.bar", colour, Placeholder.unparsed("fish", fish),
                    Placeholder.unparsed("size", size)));
            Ui.sound(p, "entity.experience_orb.pickup", s.junk() ? 0.7f : 1.2f);
        }
    }

    /** Kleines Feuerwerk über der Fangstelle (nicht vor dem Gesicht des Anglers). */
    public void celebrate(Location at) {
        Location l = at.clone().add(0, 2, 0);
        Firework fw = l.getWorld().spawn(l, Firework.class, f -> {
            var meta = f.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.STAR)
                    .withColor(Color.fromRGB(0xF2B632), Color.WHITE).withFade(Color.fromRGB(0x5DA9E9)).trail(true)
                    .build());
            meta.setPower(0);
            f.setFireworkMeta(meta);
            f.setPersistent(false);
        });
        pl.getServer().getScheduler().runTaskLater(pl, fw::detonate, 12L);
    }

    // ---------- Aufgaben ----------

    /** Aufgaben des Tages bereitstellen (neuer Tag → neue Aufgaben). */
    public void ensureQuests(PlayerData d) {
        LocalDate day = today();
        d.ensureQuests(day, DailyQuests.forDay(day));
    }

    public void quests(Player p, Quest.Event e) {
        PlayerData d = pl.data(p);
        ensureQuests(d);
        List<Integer> done = d.advance(e);
        if (done.isEmpty()) {
            return;
        }
        String lang = pl.lang(p);
        for (int i : done) {
            Quest q = d.quests().get(i);
            d.earn(q.reward());
            p.sendMessage(Texts.msg(lang, "quest.finished", Placeholder.unparsed("quest", Menus.questText(lang, q)),
                    Placeholder.unparsed("n", String.valueOf(q.reward()))));
            Ui.sound(p, "entity.player.levelup", 1.3f);
        }
        pl.store().save(d);
    }

    // ---------- Verkaufen ----------

    /** Wert aller Fänge im Inventar: {Anzahl, Taler}. */
    public int[] basket(Player p) {
        int n = 0;
        int sum = 0;
        for (ItemStack item : p.getInventory().getContents()) {
            Optional<FishItems.Stored> f = FishItems.read(item);
            if (f.isPresent()) {
                n++;
                sum += f.get().c().price();
            }
        }
        return new int[] {n, sum};
    }

    /** Alles verkaufen; liefert die Summe. */
    public int sellAll(Player p) {
        PlayerInventory inv = p.getInventory();
        int n = 0;
        int sum = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            Optional<FishItems.Stored> f = FishItems.read(inv.getItem(i));
            if (f.isPresent()) {
                n++;
                sum += f.get().c().price();
                inv.setItem(i, null);
            }
        }
        String lang = pl.lang(p);
        if (n == 0) {
            p.sendMessage(Texts.msg(lang, "merchant.nothing"));
            return 0;
        }
        PlayerData d = pl.data(p);
        d.sold(sum);
        p.sendMessage(Texts.msg(lang, "merchant.sold-all", Placeholder.unparsed("n", String.valueOf(n)),
                Placeholder.unparsed("price", Units.number(sum, lang)),
                Placeholder.unparsed("taler", Units.number(d.taler(), lang))));
        Ui.sound(p, "entity.villager.yes", 1.0f);
        Ui.sound(p, "block.amethyst_block.chime", 1.4f);
        quests(p, Quest.Event.sold(sum));
        pl.store().save(d);
        return sum;
    }

    /** Einen Fang aus einem Inventarplatz verkaufen. */
    public boolean sellSlot(Player p, int slot) {
        PlayerInventory inv = p.getInventory();
        Optional<FishItems.Stored> f = FishItems.read(inv.getItem(slot));
        if (f.isEmpty()) {
            return false;
        }
        int price = f.get().c().price();
        inv.setItem(slot, null);
        PlayerData d = pl.data(p);
        d.sold(price);
        String lang = pl.lang(p);
        p.sendMessage(Texts.msg(lang, "merchant.sold", Placeholder.unparsed("what",
                Texts.plain(lang, "species." + f.get().c().species().id())),
                Placeholder.unparsed("price", Units.number(price, lang))));
        Ui.sound(p, "block.amethyst_block.chime", 1.4f);
        quests(p, Quest.Event.sold(price));
        pl.store().save(d);
        return true;
    }

    // ---------- Netzwerk ----------

    public void addStat(UUID uuid, String key, long n) {
        pending.computeIfAbsent(uuid, u -> new HashMap<>()).merge(key, n, Long::sum);
        changed.add(uuid);
    }

    /** Gesammelte Werte ans Netzwerk melden (alle 30 s, beim Gehen, nach Turnieren). */
    public void flush(UUID uuid) {
        if (!changed.remove(uuid)) {
            return;
        }
        Map<String, Long> add = pending.remove(uuid);
        Optional<PlayerData> d = pl.store().loaded(uuid);
        Map<String, Long> best = new HashMap<>();
        d.ifPresent(data -> {
            if (data.heaviest() > 0) {
                best.put("heaviest_g", (long) data.heaviest());
            }
            best.put("species", (long) data.speciesCount());
        });
        pl.net().stats(uuid, add == null ? Map.of() : add, best);
    }

    public void flushAll() {
        for (UUID u : Set.copyOf(changed)) {
            flush(u);
        }
    }

    public void forget(UUID uuid) {
        bites.remove(uuid);
        forced.remove(uuid);
        forcedCm.remove(uuid);
    }

    /** Ein Spieler mitten im Tal? (für Ort in der Leiste) */
    public static Component placeName(AngelkoenigPlugin pl, Player p, String lang) {
        Location l = p.getLocation();
        Spot best = null;
        double bestD = 26;
        for (Map.Entry<Spot, Layout.Place> e : Layout.SPOTS.entrySet()) {
            double d = Math.hypot(e.getValue().x() - l.getX(), e.getValue().z() - l.getZ());
            if (d < bestD) {
                bestD = d;
                best = e.getKey();
            }
        }
        if (best == Spot.BERGSEE && pl.terrain().inVillage(l.getBlockX(), l.getBlockZ())) {
            best = null;
        }
        if (best != null) {
            return Component.text(Texts.plain(lang, "spot." + best.id()));
        }
        if (pl.terrain().inVillage(l.getBlockX(), l.getBlockZ()) || pl.terrain().nearLake(l.getBlockX(),
                l.getBlockZ(), 4)) {
            return Component.text(Texts.plain(lang, pl.terrain().inVillage(l.getBlockX(), l.getBlockZ())
                    ? "place.village" : "spot.bergsee"));
        }
        return Component.text(Texts.plain(lang, "place.wild"));
    }
}
