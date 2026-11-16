package net.morni.angelkoenig.play;

import net.morni.angelkoenig.AngelkoenigPlugin;
import net.morni.angelkoenig.Items;
import net.morni.angelkoenig.Texts;
import net.morni.angelkoenig.Ui;
import net.morni.angelkoenig.data.PlayerData;
import net.morni.angelkoenig.data.Records;
import net.morni.angelkoenig.fish.Bait;
import net.morni.angelkoenig.fish.CatchRules;
import net.morni.angelkoenig.fish.Cosmetic;
import net.morni.angelkoenig.fish.DayTime;
import net.morni.angelkoenig.fish.Quest;
import net.morni.angelkoenig.fish.Rod;
import net.morni.angelkoenig.fish.Species;
import net.morni.angelkoenig.fish.Spot;
import net.morni.angelkoenig.fish.Units;
import net.morni.angelkoenig.fish.Weather;
import net.morni.angelkoenig.menu.Menu;
import net.morni.angelkoenig.world.Layout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Alle Truhen-Fenster: Fischhändler (Verkaufen, Angeln, Köder, Kleidung), Köderdose, Kleiderschrank, Fischbuch,
 * Aufgaben und Wegweiser. Alles per einfachem Klick – geht auch mit Bedrock.
 */
public final class Menus {

    private static final Material FRAME = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
    private static final int ROW = 9;

    private Menus() {
    }

    private static Component title(String lang, String key) {
        return Component.text(Texts.plain(lang, key));
    }

    private static String fish(String lang, Species s) {
        return Texts.plain(lang, "species." + s.id());
    }

    private static void frame(Menu m, int from, int to) {
        for (int i = from; i <= to; i++) {
            m.set(i, Items.pane(FRAME), null);
        }
    }

    private static ItemStack back(String lang) {
        return Items.named(Material.ARROW, 1, Texts.text(lang, "merchant.back"), null);
    }

    // ---------- Fischhändler ----------

    public static void merchant(AngelkoenigPlugin pl, Player p) {
        String lang = pl.lang(p);
        Menu m = new Menu(6, title(lang, "merchant.title"));
        drawMerchant(pl, p, m, lang);
        m.open(p);
    }

    private static void drawMerchant(AngelkoenigPlugin pl, Player p, Menu m, String lang) {
        m.clear();
        PlayerData d = pl.data(p);
        m.set(0, Items.named(Material.SUNFLOWER, 1, Texts.text(lang, "merchant.taler",
                Placeholder.unparsed("n", Units.number(d.taler(), lang))), Texts.lines(lang, "merchant.taler.lore")), null);
        m.set(2, Items.named(Material.FISHING_ROD, 1, Texts.text(lang, "merchant.tab.rods"),
                Texts.lines(lang, "merchant.tab.rods.lore")), q -> rods(pl, q));
        m.set(3, Items.named(Material.WHEAT_SEEDS, 1, Texts.text(lang, "merchant.tab.baits"),
                Texts.lines(lang, "merchant.tab.baits.lore")), q -> baitShop(pl, q));
        m.set(4, Items.named(Material.LEATHER_HELMET, 1, Texts.text(lang, "merchant.tab.clothes"),
                Texts.lines(lang, "merchant.tab.clothes.lore")), q -> clothes(pl, q));
        int[] basket = pl.angling().basket(p);
        m.set(8, Items.glow(Items.named(Material.GOLD_INGOT, 1, Texts.text(lang, "merchant.sell-all"),
                basket[0] == 0 ? Texts.lines(lang, "merchant.nothing") : Texts.lines(lang, "merchant.sell-all.lore",
                        Placeholder.unparsed("n", String.valueOf(basket[0])),
                        Placeholder.unparsed("price", Units.number(basket[1], lang)))), basket[0] > 0), q -> {
                            pl.angling().sellAll(q);
                            drawMerchant(pl, q, m, lang);
                        });
        frame(m, ROW, 2 * ROW - 1);
        var inv = p.getInventory();
        int slot = 2 * ROW;
        for (int i = 0; i < inv.getSize() && slot < 6 * ROW; i++) {
            ItemStack item = inv.getItem(i);
            if (!FishItems.isFish(item)) {
                continue;
            }
            ItemStack shown = item.clone();
            List<Component> lore = new ArrayList<>(Optional.ofNullable(shown.lore()).orElse(List.of()));
            lore.add(Texts.text(lang, "merchant.fish.click"));
            shown.lore(lore);
            int invSlot = i;
            m.set(slot++, shown, q -> {
                pl.angling().sellSlot(q, invSlot);
                drawMerchant(pl, q, m, lang);
            });
        }
    }

    public static void rods(AngelkoenigPlugin pl, Player p) {
        String lang = pl.lang(p);
        Menu m = new Menu(3, title(lang, "merchant.rods.title"));
        drawRods(pl, p, m, lang);
        m.open(p);
    }

    private static void drawRods(AngelkoenigPlugin pl, Player p, Menu m, String lang) {
        m.clear();
        PlayerData d = pl.data(p);
        int slot = 10;
        for (Rod r : Rod.values()) {
            ItemStack item = Hotbar.rod(r, lang);
            List<Component> lore = new ArrayList<>(Optional.ofNullable(item.lore()).orElse(List.of()));
            lore.remove(0);
            if (d.rod() == r) {
                lore.add(Texts.text(lang, "shop.active"));
            } else if (d.owns(r)) {
                lore.add(Texts.text(lang, "shop.owned"));
            } else {
                lore.add(Texts.text(lang, "shop.price", Placeholder.unparsed("price", Units.number(r.price(), lang))));
                lore.add(Texts.text(lang, "shop.buy"));
            }
            item.lore(lore);
            m.set(slot, item, q -> {
                PlayerData qd = pl.data(q);
                if (!qd.owns(r)) {
                    if (!qd.spend(r.price())) {
                        tooExpensive(q, lang, r.price() - qd.taler());
                        return;
                    }
                    qd.addRod(r);
                    q.sendMessage(Texts.msg(lang, "shop.bought", Placeholder.unparsed("what", Texts.plain(lang, "rod." + r.id())),
                            Placeholder.unparsed("price", Units.number(r.price(), lang))));
                    Ui.sound(q, "entity.villager.yes", 1.0f);
                }
                qd.useRod(r);
                q.sendMessage(Texts.msg(lang, "shop.rod-used", Placeholder.unparsed("what", Texts.plain(lang, "rod." + r.id()))));
                Hotbar.refreshRod(pl, q);
                pl.store().save(qd);
                drawRods(pl, q, m, lang);
            });
            slot += 2;
        }
        m.set(18, back(lang), q -> merchant(pl, q));
        m.set(26, Items.named(Material.SUNFLOWER, 1, Texts.text(lang, "merchant.taler",
                Placeholder.unparsed("n", Units.number(d.taler(), lang))), null), null);
    }

    private static void tooExpensive(Player p, String lang, long missing) {
        p.sendMessage(Texts.msg(lang, "shop.too-expensive", Placeholder.unparsed("n", Units.number(missing, lang))));
        Ui.sound(p, "entity.villager.no", 1.0f);
    }

    static String targets(String lang, Bait b) {
        if (b == Bait.GOLDSPINNER) {
            return Texts.plain(lang, "bait.legends");
        }
        return b.targets().stream().map(s -> fish(lang, s)).collect(Collectors.joining(", "));
    }

    public static void baitShop(AngelkoenigPlugin pl, Player p) {
        String lang = pl.lang(p);
        Menu m = new Menu(3, title(lang, "merchant.baits.title"));
        drawBaitShop(pl, p, m, lang);
        m.open(p);
    }

    private static void drawBaitShop(AngelkoenigPlugin pl, Player p, Menu m, String lang) {
        m.clear();
        PlayerData d = pl.data(p);
        int slot = 10;
        for (Bait b : Bait.values()) {
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "shop.bait.lore",
                    Placeholder.unparsed("n", String.valueOf(b.pack())), Placeholder.unparsed("targets", targets(lang, b)),
                    Placeholder.unparsed("have", String.valueOf(d.baits(b)))));
            lore.add(Texts.text(lang, "shop.price", Placeholder.unparsed("price", Units.number(b.price(), lang))));
            lore.add(Texts.text(lang, "shop.buy"));
            ItemStack item = Items.named(Items.material(b.item()), 1, Texts.text(lang, "bait." + b.id())
                    .color(TextColor.color(Texts.EMERALD)), lore);
            m.set(slot++, item, q -> {
                PlayerData qd = pl.data(q);
                if (!qd.spend(b.price())) {
                    tooExpensive(q, lang, b.price() - qd.taler());
                    return;
                }
                qd.addBaits(b, b.pack());
                if (qd.bait() == null) {
                    qd.bait(b);
                }
                q.sendMessage(Texts.msg(lang, "shop.bought", Placeholder.unparsed("what", b.pack() + "× "
                        + Texts.plain(lang, "bait." + b.id())), Placeholder.unparsed("price", Units.number(b.price(), lang))));
                Ui.sound(q, "entity.villager.yes", 1.0f);
                Hotbar.refreshBait(pl, q);
                pl.store().save(qd);
                drawBaitShop(pl, q, m, lang);
            });
        }
        m.set(18, back(lang), q -> merchant(pl, q));
        m.set(26, Items.named(Material.SUNFLOWER, 1, Texts.text(lang, "merchant.taler",
                Placeholder.unparsed("n", Units.number(d.taler(), lang))), null), null);
    }

    public static void clothes(AngelkoenigPlugin pl, Player p) {
        String lang = pl.lang(p);
        Menu m = new Menu(3, title(lang, "merchant.clothes.title"));
        drawClothes(pl, p, m, lang);
        m.open(p);
    }

    private static void drawClothes(AngelkoenigPlugin pl, Player p, Menu m, String lang) {
        m.clear();
        PlayerData d = pl.data(p);
        int slot = 9;
        for (Cosmetic c : Cosmetic.values()) {
            ItemStack item = Hotbar.cosmetic(c, lang);
            List<Component> lore = new ArrayList<>();
            if (d.owns(c)) {
                lore.add(Texts.text(lang, "shop.owned-cosmetic"));
            } else if (!c.forSale()) {
                lore.add(Texts.text(lang, "shop.crown"));
            } else {
                lore.add(Texts.text(lang, "shop.price", Placeholder.unparsed("price", Units.number(c.price(), lang))));
                lore.add(Texts.text(lang, "shop.buy"));
            }
            item.lore(lore);
            m.set(slot++, item, q -> {
                PlayerData qd = pl.data(q);
                if (qd.owns(c) || !c.forSale()) {
                    return;
                }
                if (!qd.spend(c.price())) {
                    tooExpensive(q, lang, c.price() - qd.taler());
                    return;
                }
                qd.addCosmetic(c);
                qd.toggleWear(c);
                Hotbar.armor(q, qd, lang);
                q.sendMessage(Texts.msg(lang, "shop.bought", Placeholder.unparsed("what", Texts.plain(lang, "cosmetic." + c.id())),
                        Placeholder.unparsed("price", Units.number(c.price(), lang))));
                Ui.sound(q, "item.armor.equip_leather", 1.0f);
                pl.store().save(qd);
                drawClothes(pl, q, m, lang);
            });
        }
        m.set(18, back(lang), q -> merchant(pl, q));
        m.set(26, Items.named(Material.SUNFLOWER, 1, Texts.text(lang, "merchant.taler",
                Placeholder.unparsed("n", Units.number(d.taler(), lang))), null), null);
    }

    // ---------- Köderdose ----------

    public static void baits(AngelkoenigPlugin pl, Player p) {
        String lang = pl.lang(p);
        Menu m = new Menu(3, title(lang, "baits.title"));
        drawBaits(pl, p, m, lang);
        m.open(p);
    }

    private static void drawBaits(AngelkoenigPlugin pl, Player p, Menu m, String lang) {
        m.clear();
        PlayerData d = pl.data(p);
        m.set(4, Items.named(Material.BOWL, 1, Texts.text(lang, "baits.title").color(TextColor.color(Texts.EMERALD)),
                Texts.lines(lang, "baits.info")), null);
        int slot = 10;
        for (Bait b : Bait.values()) {
            int n = d.baits(b);
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "baits.lore",
                    Placeholder.unparsed("targets", targets(lang, b)), Placeholder.unparsed("n", String.valueOf(n))));
            lore.add(n <= 0 ? Texts.text(lang, "baits.empty") : d.bait() == b ? Texts.text(lang, "baits.active")
                    : Texts.text(lang, "baits.click"));
            ItemStack item = Items.named(n > 0 ? Items.material(b.item()) : Material.GRAY_DYE, Math.max(1, Math.min(64, n)),
                    Texts.text(lang, "bait." + b.id()).color(TextColor.color(Texts.EMERALD)), lore);
            Items.glow(item, d.bait() == b);
            m.set(slot++, item, q -> {
                PlayerData qd = pl.data(q);
                if (qd.baits(b) <= 0) {
                    return;
                }
                if (qd.bait() == b) {
                    qd.bait(null);
                    q.sendMessage(Texts.msg(lang, "baits.off"));
                } else {
                    qd.bait(b);
                    q.sendMessage(Texts.msg(lang, "baits.on", Placeholder.unparsed("bait", Texts.plain(lang, "bait." + b.id()))));
                }
                Ui.sound(q, "item.bundle.insert", 1.0f);
                Hotbar.refreshBait(pl, q);
                pl.store().save(qd);
                drawBaits(pl, q, m, lang);
            });
        }
    }

    // ---------- Kleiderschrank ----------

    public static void wardrobe(AngelkoenigPlugin pl, Player p) {
        String lang = pl.lang(p);
        Menu m = new Menu(3, title(lang, "wardrobe.title"));
        drawWardrobe(pl, p, m, lang);
        m.open(p);
    }

    private static void drawWardrobe(AngelkoenigPlugin pl, Player p, Menu m, String lang) {
        m.clear();
        PlayerData d = pl.data(p);
        int slot = 9;
        for (Cosmetic c : Cosmetic.values()) {
            if (!d.owns(c)) {
                continue;
            }
            ItemStack item = Hotbar.cosmetic(c, lang);
            boolean worn = d.worn(c.slot()) == c;
            item.lore(List.of(Texts.text(lang, worn ? "wardrobe.worn" : "wardrobe.wear")));
            Items.glow(item, worn || c == Cosmetic.KOENIGSKRONE);
            m.set(slot++, item, q -> {
                PlayerData qd = pl.data(q);
                boolean now = qd.toggleWear(c);
                Hotbar.armor(q, qd, lang);
                q.sendMessage(Texts.msg(lang, now ? "wardrobe.on" : "wardrobe.off",
                        Placeholder.unparsed("what", Texts.plain(lang, "cosmetic." + c.id()))));
                Ui.sound(q, "item.armor.equip_leather", 1.0f);
                pl.store().save(qd);
                drawWardrobe(pl, q, m, lang);
            });
        }
        if (slot == 9) {
            m.set(13, Items.named(Material.ARMOR_STAND, 1, Texts.text(lang, "wardrobe.title"),
                    Texts.lines(lang, "wardrobe.empty")), null);
        }
    }

    // ---------- Fischbuch ----------

    public static void book(AngelkoenigPlugin pl, Player p) {
        String lang = pl.lang(p);
        PlayerData d = pl.data(p);
        Menu m = new Menu(6, title(lang, "book.title"));
        String heaviest = d.heaviest() <= 0 ? "–" : Units.weight(d.heaviest(), lang) + Species.byId(d.heaviestSpecies())
                .map(s -> " (" + fish(lang, s) + ")").orElse("");
        m.set(4, Items.glow(Items.named(Material.BOOK, 1, Texts.text(lang, "book.summary"), Texts.lines(lang,
                "book.summary.lore", Placeholder.unparsed("n", String.valueOf(d.speciesCount())),
                Placeholder.unparsed("total", String.valueOf(Species.fishes().size())),
                Placeholder.unparsed("catches", Units.number(d.catches(), lang)),
                Placeholder.unparsed("finds", Units.number(d.finds(), lang)),
                Placeholder.unparsed("legendary", String.valueOf(d.legendary())),
                Placeholder.unparsed("heaviest", heaviest),
                Placeholder.unparsed("wins", String.valueOf(d.tournamentsWon())),
                Placeholder.unparsed("earned", Units.number(d.earnedTotal(), lang)))), true), null);
        int i = 0;
        for (Species s : Species.values()) {
            int row = 1 + i / 7;
            int col = 1 + i % 7;
            m.set(row * ROW + col, bookEntry(pl, d, s, lang), null);
            i++;
        }
        m.open(p);
    }

    private static ItemStack bookEntry(AngelkoenigPlugin pl, PlayerData d, Species s, String lang) {
        String spots = s.spots().stream().map(sp -> Texts.plain(lang, "spot." + sp.id())).collect(Collectors.joining(", "));
        PlayerData.BookEntry e = d.book(s);
        if (e == null) {
            return Items.named(Material.GRAY_DYE, 1, Texts.text(lang, "book.unknown"), Texts.lines(lang,
                    "book.unknown.lore", Placeholder.unparsed("spots", spots)));
        }
        List<Component> lore = new ArrayList<>();
        lore.add(Texts.text(lang, "fish.lore.rarity", Texts.colour(s.rarity().colour()),
                Placeholder.unparsed("rarity", Texts.plain(lang, "rarity." + s.rarity().id())),
                Placeholder.unparsed("stars", "★".repeat(s.rarity().stars()))));
        lore.add(Texts.text(lang, "species." + s.id() + ".desc").color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        String best = s.junk() ? Units.weight(e.bestGrams(), lang)
                : Units.cm(e.bestMm(), lang) + " · " + Units.weight(e.bestGrams(), lang);
        Records.Best rec = pl.records().best(s);
        String record = rec == null || s.junk() ? Texts.plain(lang, "book.no-record")
                : Units.weight(rec.grams(), lang) + " (" + rec.name() + ")";
        lore.addAll(Texts.lines(lang, "book.entry.lore", Placeholder.unparsed("count", Units.number(e.count(), lang)),
                Placeholder.unparsed("best", best), Placeholder.unparsed("spots", spots),
                Placeholder.unparsed("hint", hint(lang, s)), Placeholder.unparsed("record", record)));
        ItemStack item = Items.named(Items.material(s.item()), 1, Texts.text(lang, "catch.title",
                Texts.colour(s.rarity().colour()), Placeholder.unparsed("fish", fish(lang, s))), lore);
        return Items.glow(item, s.shines());
    }

    /** Wann eine Art gern beißt, aus ihren Faktoren („morgens, bei Regen“). */
    static String hint(String lang, Species s) {
        if (!s.activeAt(DayTime.MORGEN) && !s.activeAt(DayTime.TAG)) {
            return Texts.plain(lang, "hint.only-night");
        }
        List<String> out = new ArrayList<>();
        for (DayTime t : DayTime.values()) {
            if (s.timeFactor(t) >= 1.4) {
                out.add(Texts.plain(lang, "hint." + t.id()));
            }
        }
        for (Weather w : Weather.values()) {
            if (s.weatherFactor(w) >= 1.3) {
                out.add(Texts.plain(lang, "hint." + w.id()));
            }
        }
        return out.isEmpty() ? Texts.plain(lang, "hint.always") : String.join(", ", out);
    }

    // ---------- Aufgaben ----------

    public static String questText(String lang, Quest q) {
        String n = String.valueOf(q.amount());
        String raw = Texts.raw(lang, "quest." + q.kind().name().toLowerCase(Locale.ROOT)).replace("<n>", n);
        return switch (q.kind()) {
            case SPOT -> raw.replace("<spot>", Texts.plain(lang, "spot." + q.target()));
            case SPECIES -> raw.replace("<fish>", Species.byId(q.target()).map(s -> fish(lang, s)).orElse(q.target()));
            case HEAVY -> raw.replace("<weight>", Units.weight(Integer.parseInt(q.target()), lang));
            default -> raw;
        };
    }

    public static void quests(AngelkoenigPlugin pl, Player p) {
        String lang = pl.lang(p);
        PlayerData d = pl.data(p);
        pl.angling().ensureQuests(d);
        Menu m = new Menu(3, title(lang, "quests.title"));
        List<Quest> qs = d.quests();
        String[] level = {"quest.easy", "quest.medium", "quest.hard"};
        for (int i = 0; i < qs.size(); i++) {
            Quest q = qs.get(i);
            boolean done = d.done(i);
            List<Component> lore = new ArrayList<>();
            lore.add(Texts.text(lang, level[Math.min(i, 2)]));
            lore.add(Texts.text(lang, "quest.progress", Placeholder.unparsed("done", String.valueOf(d.progress(i))),
                    Placeholder.unparsed("n", String.valueOf(q.amount())),
                    Placeholder.unparsed("bar", Ui.bar(d.progress(i) / (double) q.amount(), 10))));
            lore.add(Texts.text(lang, "quest.reward", Placeholder.unparsed("n", String.valueOf(q.reward()))));
            if (done) {
                lore.add(Texts.text(lang, "quest.done"));
            }
            ItemStack item = Items.named(done ? Material.LIME_DYE : i == 2 ? Material.FILLED_MAP : Material.PAPER, 1,
                    Component.text(questText(lang, q)).color(TextColor.color(done ? Texts.EMERALD : 0xFFFFFF))
                            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false), lore);
            m.set(11 + 2 * i, Items.glow(item, done), null);
        }
        m.set(22, Items.named(Material.CLOCK, 1, Texts.text(lang, "quests.title").color(TextColor.color(Texts.GOLD)),
                Texts.lines(lang, "quest.reset")), null);
        m.open(p);
    }

    // ---------- Wegweiser ----------

    public static void spots(AngelkoenigPlugin pl, Player p) {
        String lang = pl.lang(p);
        Menu m = new Menu(3, title(lang, "spots.title"));
        m.set(10, Items.named(Material.CAMPFIRE, 1, Texts.text(lang, "spots.village"), List.of(
                Texts.text(lang, "spots.village.lore"), Texts.text(lang, "spots.go"))), q -> travel(pl, q, null));
        Material[] icons = {Material.OAK_BOAT, Material.SALMON, Material.LILY_PAD, Material.PACKED_ICE};
        int slot = 12;
        for (Spot s : Spot.values()) {
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "spot." + s.id() + ".desc"));
            lore.add(Component.empty());
            Location at = place(pl, Layout.SPOTS.get(s));
            lore.add(Texts.text(lang, "spots.now", Placeholder.unparsed("time", Texts.plain(lang, "time." + pl.angling().time().id())),
                    Placeholder.unparsed("weather", pl.angling().weatherName(lang, at))));
            lore.addAll(biteList(pl, lang, s, 4));
            lore.add(Component.empty());
            lore.add(Texts.text(lang, "spots.go"));
            m.set(slot++, Items.named(icons[s.ordinal()], 1, Texts.text(lang, "spot." + s.id())
                    .color(TextColor.color(Texts.EMERALD)), lore), q -> travel(pl, q, s));
        }
        m.open(p);
    }

    /** Die wahrscheinlichsten Fische an einem Platz gerade jetzt (ohne Köder, Haselrute). */
    static List<Component> biteList(AngelkoenigPlugin pl, String lang, Spot s, int n) {
        List<Component> out = new ArrayList<>();
        var chances = CatchRules.chances(new CatchRules.Conditions(s, pl.angling().time(), pl.angling().weather(),
                Rod.HASEL, null));
        int shown = 0;
        for (Map.Entry<Species, Double> e : chances) {
            if (e.getKey().junk() || shown >= n) {
                continue;
            }
            shown++;
            out.add(Texts.text(lang, "spots.fish", Texts.colour(e.getKey().rarity().colour()),
                    Placeholder.unparsed("fish", fish(lang, e.getKey())),
                    Placeholder.unparsed("chance", Math.max(1, Math.round(e.getValue() * 100)) + " %")));
        }
        return out;
    }

    static Location place(AngelkoenigPlugin pl, Layout.Place pl2) {
        return new Location(pl.world(), pl2.x(), pl2.y(), pl2.z(), pl2.yaw(), pl2.pitch());
    }

    /** Zu einem Fangplatz (null = Dorf) reisen. */
    public static void travel(AngelkoenigPlugin pl, Player p, Spot s) {
        String lang = pl.lang(p);
        p.closeInventory();
        if (p.isInsideVehicle()) {
            p.leaveVehicle();
        }
        Layout.Place to = s == null ? Layout.SPAWN : Layout.SPOTS.get(s);
        p.teleport(place(pl, to));
        p.sendMessage(s == null ? Texts.msg(lang, "spots.arrived.village") : Texts.msg(lang, "spots.arrived",
                Placeholder.unparsed("spot", Texts.plain(lang, "spot." + s.id()))));
        Ui.sound(p, "entity.enderman.teleport", 1.4f);
    }
}
