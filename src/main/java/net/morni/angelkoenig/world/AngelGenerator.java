package net.morni.angelkoenig.world;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

/**
 * Das Tal entsteht beim Erzeugen jedes Chunks direkt aus dem Code ({@link Surface}): kein langer Bau beim Start, Paper
 * erzeugt die Chunks parallel, wenn jemand hinkommt. Biome je Gegend (Bergwiese, Sumpf, Schnee) für Farben und Wetter.
 */
public final class AngelGenerator extends ChunkGenerator {

    private final Surface surface;
    private final BlockData[] palette;
    private final Biomes biomes;

    public AngelGenerator(Terrain terrain, Surface surface, BlockData[] palette) {
        this.surface = surface;
        this.palette = palette;
        this.biomes = new Biomes(terrain);
    }

    /** Läuft auf den Erzeuger-Threads: liest nur aus fertigen Tabellen. */
    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData data) {
        Surface.Chunk c = surface.generate(chunkX, chunkZ);
        int min = data.getMinHeight();
        int max = data.getMaxHeight();
        c.forEach((lx, y, lz, id) -> {
            BlockData d = palette[id];
            if (d != null && y >= min && y < max) {
                data.setBlock(lx, y, lz, d);
            }
        });
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    @Override
    public Location getFixedSpawnLocation(org.bukkit.World world, Random random) {
        Layout.Place s = Layout.SPAWN;
        return new Location(world, s.x(), s.y(), s.z(), s.yaw(), s.pitch());
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return biomes;
    }

    /** Biome aus {@link Terrain#biome}. */
    static final class Biomes extends BiomeProvider {
        private static final List<String> KEYS = List.of("minecraft:meadow", "minecraft:swamp",
                "minecraft:snowy_plains", "minecraft:snowy_slopes", "minecraft:frozen_peaks");
        private final Terrain terrain;
        private final Map<String, Biome> byKey = new HashMap<>();
        private final Biome fallback;
        private final List<Biome> all;

        Biomes(Terrain terrain) {
            this.terrain = terrain;
            Biome plains = Biome.PLAINS;
            for (String k : KEYS) {
                NamespacedKey key = NamespacedKey.fromString(k);
                Biome b = key == null ? null : Registry.BIOME.get(key);
                byKey.put(k, b == null ? plains : b);
            }
            fallback = byKey.get("minecraft:meadow");
            all = byKey.values().stream().distinct().toList();
        }

        @Override
        public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
            return byKey.getOrDefault(terrain.biome(x, y, z), fallback);
        }

        @Override
        public List<Biome> getBiomes(WorldInfo worldInfo) {
            return all;
        }
    }
}
