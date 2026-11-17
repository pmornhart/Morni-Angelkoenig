package de.moinmorni.angelkoenig;

import de.moinmorni.angelkoenig.data.PlayerData;
import de.moinmorni.angelkoenig.data.Records;
import de.moinmorni.angelkoenig.data.Store;
import de.moinmorni.angelkoenig.fish.Species;
import de.moinmorni.angelkoenig.fish.Tournament;
import de.moinmorni.angelkoenig.fish.Units;
import de.moinmorni.angelkoenig.listen.FishingListener;
import de.moinmorni.angelkoenig.listen.GuardListener;
import de.moinmorni.angelkoenig.listen.PlayerListener;
import de.moinmorni.angelkoenig.menu.MenuListener;
import de.moinmorni.angelkoenig.net.Network;
import de.moinmorni.angelkoenig.play.Ambience;
import de.moinmorni.angelkoenig.play.Angling;
import de.moinmorni.angelkoenig.play.Boards;
import de.moinmorni.angelkoenig.play.Hotbar;
import de.moinmorni.angelkoenig.play.Npcs;
import de.moinmorni.angelkoenig.play.Seats;
import de.moinmorni.angelkoenig.play.Tournaments;
import de.moinmorni.angelkoenig.world.Layout;
import de.moinmorni.angelkoenig.world.Terrain;
import de.moinmorni.angelkoenig.world.WorldSetup;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Morni Angelkönig: gemütliches Angeln in einem Alpen-Tal – eigene Fische, vier Fangplätze, Fischhändler, Fischbuch,
 * Aufgaben des Tages, Angel-Turniere und Bestenlisten. Jederzeit betretbar, ohne Runden. Spricht mit dem
 * MoinMorni-Netzwerk, wenn es da ist, und läuft sonst allein.
 */
public final class AngelkoenigPlugin extends JavaPlugin {

    private static final String ADMIN = "moinmorni.angelkoenig.admin";
    private static final long SECOND = 20L;
    private static AngelkoenigPlugin instance;

    private Settings settings;
    private Network net;
    private Store store;
    private Records records;
    private WorldSetup worldSetup;
    private World world;
    private Angling angling;
    private Tournaments tournaments;
    private Boards boards;
    private Npcs npcs;
    private Ambience ambience;
    private Seats seats;
    private final Sidebar sidebar = new Sidebar();
    private final Map<UUID, String> lastLang = new HashMap<>();
    private volatile boolean ready;
    private boolean stateQueued;
    private boolean recordsDirty;
    private String lastState = "";
    private int lastPlayers = -1;

    public static AngelkoenigPlugin instance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        settings = Settings.load(getConfig());
        Items.init(this);
        de.moinmorni.angelkoenig.play.FishItems.init(this);
        net = Network.connect(this);
        getLogger().info(net.connected()
                ? "Mit dem MoinMorni-Netzwerk verbunden (Server „" + net.serverName() + "“)."
                : "Kein Netzwerk-Plugin: Angelkönig läuft allein (deutsche Texte, Statistik nur lokal).");
        store = new Store(getLogger(), getDataFolder().toPath());
        records = loadRecords();
        worldSetup = new WorldSetup(this, settings.world());
        try {
            worldSetup.setup(settings.timeRuns());
        } catch (IOException e) {
            throw new IllegalStateException("Das Tal ließ sich nicht anlegen", e);
        }
        world = worldSetup.world();
        angling = new Angling(this);
        tournaments = new Tournaments(this);
        boards = new Boards(this);
        npcs = new Npcs(this);
        ambience = new Ambience(this);
        seats = new Seats(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new MenuListener(), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new FishingListener(this), this);
        pm.registerEvents(new GuardListener(this), this);
        pm.registerEvents(seats, this);
        Commands commands = new Commands(this);
        for (String name : List.of("angeladmin", "fischbuch", "aufgaben", "wegweiser", "koeder", "kleiderschrank",
                "turnier", "stats", "top")) {
            PluginCommand c = getCommand(name);
            if (c != null) {
                c.setExecutor(commands);
                c.setTabCompleter(commands);
            }
        }

        ambience.setWeather(de.moinmorni.angelkoenig.fish.Weather.KLAR, settings.weatherMin());

        var scheduler = getServer().getScheduler();
        scheduler.runTaskTimer(this, () -> {
            tournaments.tick();
            ambience.second();
            npcs.spinTrout();
            for (Player p : world.getPlayers()) {
                updateSidebar(p);
            }
        }, SECOND, SECOND);
        scheduler.runTaskTimer(this, ambience::pulse, 10L, 10L);
        scheduler.runTaskTimer(this, this::checkLanguages, 40L, 40L);
        scheduler.runTaskTimer(this, () -> {
            if (!ready) {
                return;
            }
            boards.spawn();
            boards.refreshSpots();
            npcs.tick();
            seats.sweep();
            if (npcs.missing()) {
                npcs.spawn();
            }
            for (Player p : world.getPlayers()) {
                angling.ensureQuests(data(p));
            }
        }, SECOND * 10, SECOND * 10);
        scheduler.runTaskTimer(this, () -> {
            angling.flushAll();
            if (recordsDirty) {
                writeRecords();
            }
        }, SECOND * 30, SECOND * 30);
        scheduler.runTaskTimer(this, () -> reportState(true), SECOND * 60, SECOND * 60);

        for (Player p : getServer().getOnlinePlayers()) {
            welcome(p);
        }
        worldSetup.keepLoaded(() -> {
            npcs.spawn();
            boards.spawn();
            ready = true;
            stateChanged();
            getLogger().info("Angelkönig ist bereit: " + Species.fishes().size() + " Fischarten an "
                    + Layout.SPOTS.size() + " Fangplätzen.");
        });
    }

    @Override
    public void onDisable() {
        if (angling != null) {
            angling.flushAll();
        }
        if (seats != null) {
            seats.clear();
        }
        if (npcs != null) {
            npcs.removeAll();
        }
        if (boards != null) {
            boards.removeAll();
        }
        Holo.removeAll();
        if (store != null) {
            if (records != null) {
                store.write(recordsFile(), records.toYaml());
            }
            store.close();
        }
        instance = null;
    }

    // ---------- Spieler ----------

    /** Ins Dorf, Schnellleiste, Kleidung, Leiste, Tafeln in der eigenen Sprache. */
    public void welcome(Player p) {
        welcome(p, false);
    }

    /**
     * Wie {@link #welcome(Player)}; mit {@code keepPlace} (zurück ins laufende Turnier) bleibt man, wo man war – nur wer
     * nicht im Tal ist, kommt ins Dorf.
     */
    public void welcome(Player p, boolean keepPlace) {
        if (p.isInsideVehicle()) {
            p.leaveVehicle();
        }
        if (!keepPlace || p.getWorld() != world) {
            p.teleport(spawn());
        }
        if (!builder(p)) {
            p.setGameMode(GameMode.ADVENTURE);
        }
        var max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20 : max.getValue());
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        Hotbar.give(this, p);
        Holo.refresh(this, p, lang(p));
        lastLang.put(p.getUniqueId(), lang(p));
        updateSidebar(p);
    }

    public Location spawn() {
        Layout.Place s = Layout.SPAWN;
        return new Location(world, s.x(), s.y(), s.z(), s.yaw(), s.pitch());
    }

    private void checkLanguages() {
        for (Player p : world.getPlayers()) {
            String lang = lang(p);
            String before = lastLang.put(p.getUniqueId(), lang);
            if (before != null && !before.equals(lang)) {
                Holo.refresh(this, p, lang);
                Hotbar.give(this, p);
            }
        }
    }

    public void forgetLanguage(UUID uuid) {
        lastLang.remove(uuid);
    }

    /** Leiste rechts: Taler, Fänge, Ort, Zeit und Wetter, Köder, Turnier. */
    public void updateSidebar(Player p) {
        String lang = lang(p);
        PlayerData d = data(p);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Texts.text(lang, "sidebar.taler", Placeholder.unparsed("n", Units.number(d.taler(), lang))));
        lines.add(Texts.text(lang, "sidebar.catches", Placeholder.unparsed("n", Units.number(d.catches(), lang)),
                Placeholder.unparsed("species", String.valueOf(d.speciesCount())),
                Placeholder.unparsed("total", String.valueOf(Species.fishes().size()))));
        lines.add(Component.text(" "));
        lines.add(Texts.text(lang, "sidebar.place", Placeholder.component("place", Angling.placeName(this, p, lang))));
        lines.add(Texts.text(lang, "sidebar.time", Placeholder.unparsed("clock",
                de.moinmorni.angelkoenig.fish.DayTime.clock(world.getTime())),
                Placeholder.unparsed("time", Texts.plain(lang, "time." + angling.time().id())),
                Placeholder.unparsed("weather", angling.weatherName(lang, p.getLocation()))));
        lines.add(Texts.text(lang, "sidebar.bait", Placeholder.unparsed("bait", d.bait() == null
                ? Texts.plain(lang, "bait.none") : Texts.plain(lang, "bait." + d.bait().id()) + " ×" + d.baits(d.bait()))));
        lines.add(Component.text("  "));
        Tournament t = tournaments.state();
        long now = System.currentTimeMillis();
        if (t.running()) {
            lines.add(Texts.text(lang, "sidebar.running", Placeholder.unparsed("time", Ui.clock((t.endsAt() - now) / 1000))));
            List<Tournament.Entry> r = t.ranking();
            if (r.isEmpty()) {
                lines.add(Texts.text(lang, "sidebar.no-catch"));
            }
            for (int i = 0; i < Math.min(3, r.size()); i++) {
                lines.add(Texts.text(lang, "sidebar.rank", Placeholder.unparsed("n", String.valueOf(i + 1)),
                        Placeholder.unparsed("name", r.get(i).name()),
                        Placeholder.unparsed("weight", Units.weight(r.get(i).grams(), lang))));
            }
            int place = t.place(p.getUniqueId());
            if (place > 3) {
                lines.add(Texts.text(lang, "sidebar.you", Placeholder.unparsed("n", String.valueOf(place)),
                        Placeholder.unparsed("weight", Units.weight(t.entry(p.getUniqueId()).grams(), lang))));
            }
        } else if (!t.enabled()) {
            lines.add(Texts.text(lang, "sidebar.off"));
        } else {
            long in = Math.max(0, (t.nextStart() - now) / 1000);
            lines.add(Texts.text(lang, t.phase() == Tournament.Phase.ANNOUNCED ? "sidebar.soon" : "sidebar.next",
                    Placeholder.unparsed("time", Ui.clock(in))));
        }
        if (records.king() != null) {
            lines.add(Texts.text(lang, "sidebar.king", Placeholder.unparsed("name", records.kingName())));
        }
        lines.add(Component.text("   "));
        lines.add(Texts.text(lang, "sidebar.address"));
        sidebar.show(p, Texts.text(lang, "sidebar.title"), lines);
    }

    // ---------- Bestenliste ----------

    private Path recordsFile() {
        return getDataFolder().toPath().resolve("bestenliste.yml");
    }

    private Records loadRecords() {
        Path f = recordsFile();
        if (Files.isRegularFile(f)) {
            try {
                return Records.fromYaml(Files.readString(f, StandardCharsets.UTF_8));
            } catch (IOException | IllegalArgumentException e) {
                getLogger().warning("bestenliste.yml ließ sich nicht lesen: " + e.getMessage());
            }
        }
        return new Records();
    }

    /** Rekorde haben sich geändert: Tafeln neu, bald speichern. */
    public void saveRecords() {
        recordsDirty = true;
        boards.refreshRecords();
    }

    /** Bestenliste sofort speichern (Verwaltung). */
    public void writeRecordsNow() {
        writeRecords();
    }

    private void writeRecords() {
        recordsDirty = false;
        store.write(recordsFile(), records.toYaml());
    }

    // ---------- Netzwerk-Zustand ----------

    public void stateChanged() {
        if (stateQueued) {
            return;
        }
        stateQueued = true;
        getServer().getScheduler().runTask(this, () -> {
            stateQueued = false;
            reportState(false);
        });
    }

    private void reportState(boolean force) {
        String state = tournaments.state().running() ? "tournament" : "open";
        int players = getServer().getOnlinePlayers().size();
        if (force || !state.equals(lastState) || players != lastPlayers) {
            lastState = state;
            lastPlayers = players;
            net.state(state, players);
        }
    }

    void reloadSettings() {
        reloadConfig();
        settings = Settings.load(getConfig());
        tournaments.reconfigure();
        world.setGameRule(GameRules.ADVANCE_TIME, settings.timeRuns());
    }

    // ---------- Zugriff ----------

    public Settings settings() {
        return settings;
    }

    public Network net() {
        return net;
    }

    public Store store() {
        return store;
    }

    public Records records() {
        return records;
    }

    public World world() {
        return world;
    }

    public Terrain terrain() {
        return worldSetup.terrain();
    }

    public WorldSetup worldSetup() {
        return worldSetup;
    }

    public Angling angling() {
        return angling;
    }

    public Tournaments tournaments() {
        return tournaments;
    }

    public Boards boards() {
        return boards;
    }

    public Ambience ambience() {
        return ambience;
    }

    public Sidebar sidebar() {
        return sidebar;
    }

    public boolean ready() {
        return ready;
    }

    public String lang(Player p) {
        return net.language(p.getUniqueId());
    }

    public PlayerData data(Player p) {
        return store.get(p.getUniqueId(), p.getName());
    }

    public boolean builder(Player p) {
        return p.getGameMode() == GameMode.CREATIVE && p.hasPermission(ADMIN);
    }
}
