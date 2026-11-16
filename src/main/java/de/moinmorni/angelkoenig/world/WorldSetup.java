package de.moinmorni.angelkoenig.world;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.DyeColor;
import org.bukkit.GameRule;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Die Welt des Tals: eine eigene Welt, deren Chunks der {@link AngelGenerator} direkt aus dem Code erzeugt. Ändert sich
 * {@link #VERSION}, wird die Welt gelöscht und entsteht neu – Spieler bauen hier nichts, ihre Daten liegen außerhalb.
 * Schilder und Banner bekommen Text und Muster, sobald ihr Chunk geladen wird.
 */
public final class WorldSetup implements Listener {

    /** Bau-Version des Tals: erhöhen, wenn sich Gelände oder Bauten ändern – dann entsteht die Welt neu. */
    public static final int VERSION = 5;
    private static final String STAMP = "welt.yml";

    private final JavaPlugin plugin;
    private final String name;
    private final Logger log;
    private final Terrain terrain = new Terrain();
    private final Map<Long, List<Plan.Sign>> signs = new HashMap<>();
    private final Map<Long, List<Plan.Banner>> banners = new HashMap<>();
    private World world;
    private AngelGenerator generator;

    public WorldSetup(JavaPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        this.log = plugin.getLogger();
    }

    public Terrain terrain() {
        return terrain;
    }

    public World world() {
        return world;
    }

    /** Welt laden oder (bei neuer Bau-Version) frisch anlegen. */
    public void setup(boolean timeRuns) throws IOException {
        long t = System.currentTimeMillis();
        Plan plan = Valley.build(terrain);
        Palette palette = new Palette();
        Surface surface = new Surface(terrain, plan, palette);
        palette.freeze();
        BlockData[] data = new BlockData[palette.size()];
        List<String> bad = new ArrayList<>();
        for (short i = 1; i < palette.size(); i++) {
            String spec = palette.spec(i);
            try {
                data[i] = Bukkit.createBlockData(spec.indexOf(':') >= 0 ? spec : "minecraft:" + spec);
            } catch (IllegalArgumentException e) {
                bad.add(spec);
            }
        }
        if (!bad.isEmpty()) {
            log.warning("Unbekannte Blöcke im Bauplan (werden ausgelassen): " + bad);
        }
        generator = new AngelGenerator(terrain, surface, data);
        for (Plan.Sign s : plan.signs()) {
            signs.computeIfAbsent(Surface.chunkKey(s.x() >> 4, s.z() >> 4), c -> new ArrayList<>()).add(s);
        }
        for (Plan.Banner b : plan.banners()) {
            banners.computeIfAbsent(Surface.chunkKey(b.x() >> 4, b.z() >> 4), c -> new ArrayList<>()).add(b);
        }
        Path stampFile = plugin.getDataFolder().toPath().resolve(STAMP);
        YamlConfiguration stamp = YamlConfiguration.loadConfiguration(stampFile.toFile());
        String built = stamp.getString("version", "");
        world = create();
        if (!String.valueOf(VERSION).equals(built)) {
            if (!built.isEmpty()) {
                log.info("Neue Bau-Version (" + built + " → " + VERSION + ") – das Tal entsteht neu.");
                File folder = world.getWorldFolder();
                if (!Bukkit.unloadWorld(world, false)) {
                    throw new IOException("Welt " + name + " ließ sich nicht entladen");
                }
                deleteFolder(folder.toPath());
                world = create();
            }
            stamp.set("version", String.valueOf(VERSION));
            stamp.set("bauteile", plan.size());
            stamp.set("angelegt", java.time.Instant.now().toString());
            Files.createDirectories(stampFile.getParent());
            stamp.save(stampFile.toFile());
        }
        rules(timeRuns);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (org.bukkit.Chunk c : world.getLoadedChunks()) {
            decorate(c.getX(), c.getZ());
        }
        log.info("Tal bereit (" + plan.size() + " Bauteile, " + palette.size() + " Blockarten, "
                + (System.currentTimeMillis() - t) + " ms).");
    }

    private World create() {
        World w = Bukkit.createWorld(new WorldCreator(name).generator(generator)
                .environment(World.Environment.NORMAL).generateStructures(false));
        if (w == null) {
            throw new IllegalStateException("Welt " + name + " ließ sich nicht anlegen");
        }
        return w;
    }

    /** Ruhiges Tal: keine Monster, kein Schaden, nichts geht kaputt; Zeit nach Einstellung, Wetter macht das Plugin. */
    private void rules(boolean timeRuns) {
        world.setDifficulty(Difficulty.PEACEFUL);
        Layout.Place s = Layout.SPAWN;
        world.setSpawnLocation(new Location(world, s.x(), s.y(), s.z(), s.yaw(), s.pitch()));
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(Layout.BORDER * 2.0);
        rule(GameRules.ADVANCE_TIME, timeRuns);
        rule(GameRules.ADVANCE_WEATHER, false);
        rule(GameRules.SPAWN_MOBS, false);
        rule(GameRules.SPAWN_MONSTERS, false);
        rule(GameRules.SPAWN_PHANTOMS, false);
        rule(GameRules.SPAWN_PATROLS, false);
        rule(GameRules.SPAWN_WANDERING_TRADERS, false);
        rule(GameRules.SPAWN_WARDENS, false);
        rule(GameRules.FALL_DAMAGE, false);
        rule(GameRules.FIRE_DAMAGE, false);
        rule(GameRules.DROWNING_DAMAGE, false);
        rule(GameRules.FREEZE_DAMAGE, false);
        rule(GameRules.PVP, false);
        rule(GameRules.RANDOM_TICK_SPEED, 0);
        rule(GameRules.KEEP_INVENTORY, true);
        rule(GameRules.IMMEDIATE_RESPAWN, true);
        rule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
        rule(GameRules.SHOW_DEATH_MESSAGES, false);
        rule(GameRules.MOB_GRIEFING, false);
        rule(GameRules.BLOCK_DROPS, false);
        rule(GameRules.MOB_DROPS, false);
        rule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
        rule(GameRules.RESPAWN_RADIUS, 0);
        rule(GameRules.LOCATOR_BAR, false);
        rule(GameRules.WATER_SOURCE_CONVERSION, false);
        rule(GameRules.NATURAL_HEALTH_REGENERATION, true);
    }

    private <T> void rule(GameRule<T> r, T value) {
        try {
            world.setGameRule(r, value);
        } catch (RuntimeException e) {
            log.fine("Spielregel " + r + " nicht gesetzt: " + e.getMessage());
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.getWorld().equals(world)) {
            decorate(event.getChunk().getX(), event.getChunk().getZ());
        }
    }

    /** Schilder beschriften und Banner bemustern (beim Erzeugen entstehen sie leer). */
    private void decorate(int cx, int cz) {
        long key = Surface.chunkKey(cx, cz);
        for (Plan.Sign s : signs.getOrDefault(key, List.of())) {
            Block b = world.getBlockAt(s.x(), s.y(), s.z());
            if (b.getState() instanceof Sign sign) {
                SignSide side = sign.getSide(Side.FRONT);
                boolean same = true;
                for (int i = 0; i < Math.min(4, s.lines().size()); i++) {
                    Component want = Component.text(s.lines().get(i));
                    if (!want.equals(side.line(i))) {
                        side.line(i, want);
                        same = false;
                    }
                }
                if (!same || !sign.isWaxed()) {
                    side.setGlowingText(true);
                    sign.setWaxed(true);
                    sign.update(true, false);
                }
            }
        }
        for (Plan.Banner pb : banners.getOrDefault(key, List.of())) {
            Block b = world.getBlockAt(pb.x(), pb.y(), pb.z());
            if (b.getState() instanceof org.bukkit.block.Banner banner && banner.getPatterns().isEmpty()) {
                List<Pattern> list = new ArrayList<>();
                for (String p : pb.patterns()) {
                    String[] parts = p.split(":");
                    PatternType type = Registry.BANNER_PATTERN.get(NamespacedKey.minecraft(parts[1]));
                    if (type != null) {
                        list.add(new Pattern(DyeColor.valueOf(parts[0].toUpperCase(Locale.ROOT)), type));
                    }
                }
                banner.setPatterns(list);
                banner.update(true, false);
            }
        }
    }

    /** Ordner der alten Welt löschen – nur wenn es wirklich der eigene Welt-Ordner ist. */
    private void deleteFolder(Path folder) throws IOException {
        Path server = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        Path target = folder.toAbsolutePath().normalize();
        Path main = Bukkit.getWorlds().get(0).getWorldFolder().toPath().toAbsolutePath().normalize();
        if (target.equals(server) || target.equals(main) || !target.startsWith(server)
                || !target.getFileName().toString().equals(name)) {
            throw new IOException("Welt-Ordner sieht nicht nach Angelkönig aus, lösche ihn nicht: " + target);
        }
        if (!Files.exists(target)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(target)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }

    /** Nach /angeladmin neubauen: beim nächsten Start neu entstehen lassen. */
    public void scheduleRebuild() throws IOException {
        Path stampFile = plugin.getDataFolder().toPath().resolve(STAMP);
        YamlConfiguration stamp = YamlConfiguration.loadConfiguration(stampFile.toFile());
        stamp.set("version", "neu");
        stamp.save(stampFile.toFile());
    }

    /**
     * Chunks um Dorf und Fangplätze bleiben geladen (Figuren, Boote, Tafeln). Geladen wird im Hintergrund;
     * {@code done} läuft auf dem Hauptthread, sobald alle da sind.
     */
    public void keepLoaded(Runnable done) {
        int[][] areas = {{-32, -72, 40, 0}, {-112, -16, -88, 8}, {64, -8, 96, 16}, {56, 88, 96, 112}};
        List<java.util.concurrent.CompletableFuture<org.bukkit.Chunk>> all = new ArrayList<>();
        for (int[] a : areas) {
            for (int cx = a[0] >> 4; cx <= a[2] >> 4; cx++) {
                for (int cz = a[1] >> 4; cz <= a[3] >> 4; cz++) {
                    all.add(world.getChunkAtAsync(cx, cz).thenApply(c -> {
                        c.addPluginChunkTicket(plugin);
                        return c;
                    }));
                }
            }
        }
        java.util.concurrent.CompletableFuture.allOf(all.toArray(new java.util.concurrent.CompletableFuture[0]))
                .whenComplete((v, e) -> Bukkit.getScheduler().runTask(plugin, done));
    }
}
