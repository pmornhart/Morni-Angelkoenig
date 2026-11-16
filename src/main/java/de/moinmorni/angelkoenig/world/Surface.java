package de.moinmorni.angelkoenig.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Macht aus Gelände ({@link Terrain}) und Bauplan ({@link Plan}) die Blöcke eines Chunks: Fels, Erde, Wiese, Moor,
 * Schnee, Gewässergrund, Wasser und Eis, Blumen, Farn, Schilf, Seerosen, Bäume – und zuletzt die Bauten. Ohne Server;
 * Ergebnis ist ein Feld mit Palettennummern. Alle Zufälle sind fest (gleiche Welt bei jedem Neubau).
 */
public final class Surface {

    /** Unterste Höhe der Welt (Paper 26.x). */
    public static final int MIN_Y = -64;
    public static final int HEIGHT = 384;
    static final int TREE_CELL = 5;
    static final int TREE_REACH = 4;
    static final int TREE_LINE = 122;
    static final int SNOW_LINE = 150;
    static final int ROCK_LINE = 122;

    /** Blöcke eines Chunks: Index (y − MIN_Y) × 256 + z × 16 + x, 0 = Luft. */
    public static final class Chunk {
        final short[] ids = new short[HEIGHT * 256];

        public short get(int lx, int y, int lz) {
            int yy = y - MIN_Y;
            if (yy < 0 || yy >= HEIGHT) {
                return 0;
            }
            return ids[yy * 256 + lz * 16 + lx];
        }

        void set(int lx, int y, int lz, short id) {
            int yy = y - MIN_Y;
            if (yy >= 0 && yy < HEIGHT) {
                ids[yy * 256 + lz * 16 + lx] = id;
            }
        }

        /** Für den Erzeuger: alle gesetzten Blöcke durchgehen. */
        public void forEach(BlockAction action) {
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] != 0) {
                    action.accept(i & 15, i / 256 + MIN_Y, (i >> 4) & 15, ids[i]);
                }
            }
        }
    }

    /** Ein gesetzter Block (lokal im Chunk). */
    public interface BlockAction {
        void accept(int lx, int y, int lz, short id);
    }

    private final Terrain t;
    private final Plan plan;
    private final Palette pal;
    private final Map<Long, List<long[]>> planByChunk = new HashMap<>();

    private final short air;
    private final short water;
    private final short ice;
    private final short packedIce;
    private final short grass;
    private final short dirt;
    private final short coarse;
    private final short podzol;
    private final short snowBlock;
    private final short mud;
    private final short moss;
    private final short sand;
    private final short gravel;
    private final short clay;
    private final short cobble;
    private final short mossyCobble;
    private final short andesite;
    private final short tuff;
    private final short stone;
    private final short dirtPath;
    private final short rooted;
    private final short bedrock;
    private final short shortGrass;
    private final short fern;
    private final short deadBush;
    private final short firefly;
    private final short sugarCane;
    private final short lilyPad;
    private final short seagrass;
    private final short[] flowers;
    private final short[] snow = new short[9];
    private final short[] tallGrass = new short[2];
    private final short[] largeFern = new short[2];
    private final short spruceLog;
    private final short spruceLeaves;
    private final short birchLog;
    private final short birchLeaves;
    private final short oakLog;
    private final short oakLeaves;

    public Surface(Terrain t, Plan plan, Palette pal) {
        this.t = t;
        this.plan = plan;
        this.pal = pal;
        air = pal.id(Plan.AIR);
        water = pal.id("water");
        ice = pal.id("ice");
        packedIce = pal.id("packed_ice");
        grass = pal.id("grass_block[snowy=false]");
        dirt = pal.id("dirt");
        coarse = pal.id("coarse_dirt");
        podzol = pal.id("podzol[snowy=false]");
        snowBlock = pal.id("snow_block");
        mud = pal.id("mud");
        moss = pal.id("moss_block");
        sand = pal.id("sand");
        gravel = pal.id("gravel");
        clay = pal.id("clay");
        cobble = pal.id("cobblestone");
        mossyCobble = pal.id("mossy_cobblestone");
        andesite = pal.id("andesite");
        tuff = pal.id("tuff");
        stone = pal.id("stone");
        dirtPath = pal.id("dirt_path");
        rooted = pal.id("rooted_dirt");
        bedrock = pal.id("bedrock");
        shortGrass = pal.id("short_grass");
        fern = pal.id("fern");
        deadBush = pal.id("dead_bush");
        firefly = pal.id("firefly_bush");
        sugarCane = pal.id("sugar_cane[age=0]");
        lilyPad = pal.id("lily_pad");
        seagrass = pal.id("seagrass");
        String[] fl = {"poppy", "dandelion", "cornflower", "azure_bluet", "oxeye_daisy", "allium", "red_tulip",
            "lily_of_the_valley", "cornflower", "oxeye_daisy"};
        flowers = new short[fl.length];
        for (int i = 0; i < fl.length; i++) {
            flowers[i] = pal.id(fl[i]);
        }
        for (int i = 1; i <= 8; i++) {
            snow[i] = pal.id("snow[layers=" + i + "]");
        }
        tallGrass[0] = pal.id("tall_grass[half=lower]");
        tallGrass[1] = pal.id("tall_grass[half=upper]");
        largeFern[0] = pal.id("large_fern[half=lower]");
        largeFern[1] = pal.id("large_fern[half=upper]");
        spruceLog = pal.id("spruce_log[axis=y]");
        spruceLeaves = pal.id(Kit.LEAF_SPRUCE);
        birchLog = pal.id("birch_log[axis=y]");
        birchLeaves = pal.id(Kit.LEAF_BIRCH);
        oakLog = pal.id("oak_log[axis=y]");
        oakLeaves = pal.id(Kit.LEAF_OAK);
        plan.forEach((k, spec) -> {
            int x = Plan.keyX(k);
            int z = Plan.keyZ(k);
            short id = pal.id(spec);
            planByChunk.computeIfAbsent(chunkKey(x >> 4, z >> 4), c -> new ArrayList<>())
                    .add(new long[] {k, id});
        });
    }

    public static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    private static int r100(int x, int y, int z, int seed) {
        return Noise.pick(x, z * 7 + y * 131, seed, 100);
    }

    /** Alle Blöcke eines Chunks. Darf auf jedem Thread laufen (liest nur). */
    public Chunk generate(int cx, int cz) {
        Chunk out = new Chunk();
        int x0 = cx << 4;
        int z0 = cz << 4;
        boolean inside = Math.abs(x0) <= Layout.EXTENT + 16 && Math.abs(z0) <= Layout.EXTENT + 16;
        if (inside) {
            int[][] h = new int[20][20];
            Terrain.Column[][] cols = new Terrain.Column[20][20];
            for (int dx = 0; dx < 20; dx++) {
                for (int dz = 0; dz < 20; dz++) {
                    Terrain.Column c = t.column(x0 + dx - 2, z0 + dz - 2);
                    cols[dx][dz] = c;
                    h[dx][dz] = c.wet() ? c.bed() : c.ground();
                }
            }
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    column(out, x0, z0, lx, lz, cols, h);
                }
            }
            trees(out, x0, z0);
        }
        List<long[]> blocks = planByChunk.get(chunkKey(cx, cz));
        if (blocks != null) {
            for (long[] b : blocks) {
                long k = b[0];
                out.set(Plan.keyX(k) - x0, Plan.keyY(k), Plan.keyZ(k) - z0, (short) b[1]);
            }
        }
        return out;
    }

    private void column(Chunk out, int x0, int z0, int lx, int lz, Terrain.Column[][] cols, int[][] h) {
        int x = x0 + lx;
        int z = z0 + lz;
        Terrain.Column c = cols[lx + 2][lz + 2];
        int top = h[lx + 2][lz + 2];
        int minN = top;
        int maxDiff = 0;
        for (int[] d : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
            int n = h[lx + 2 + d[0]][lz + 2 + d[1]];
            minN = Math.min(minN, n);
            maxDiff = Math.max(maxDiff, Math.abs(n - top));
        }
        int bottom = Math.max(MIN_Y + 1, Math.min(minN, top) - 5);
        out.set(lx, MIN_Y, lz, bedrock);
        boolean path = !c.wet() && t.onPath(x, z);
        boolean reserved = plan.reserved(x, z);
        if (c.wet()) {
            wet(out, lx, lz, x, z, c, bottom, reserved);
            return;
        }
        short topBlock = topBlock(x, z, top, maxDiff, path, cols, lx, lz);
        for (int y = bottom; y <= top; y++) {
            int depth = top - y;
            short b;
            if (depth == 0) {
                b = topBlock;
            } else if (depth <= 3) {
                b = sub(topBlock, x, y, z);
            } else {
                b = rock(x, y, z);
            }
            out.set(lx, y, lz, b);
        }
        if (!path && !reserved) {
            decor(out, lx, lz, x, z, top, topBlock, cols);
        }
    }

    private void wet(Chunk out, int lx, int lz, int x, int z, Terrain.Column c, int bottom, boolean reserved) {
        int bed = c.bed();
        Terrain.Water w = c.water();
        int from = Math.max(MIN_Y + 1, Math.min(bottom, bed - 3));
        for (int y = from; y <= bed; y++) {
            out.set(lx, y, lz, y == bed ? bedBlock(w.bed(), x, z, c.waterTop() - bed) : y >= bed - 2
                    ? (w.bed() == Terrain.Bed.MOOR ? mud : gravel) : rock(x, y, z));
        }
        for (int y = bed + 1; y <= c.waterTop(); y++) {
            out.set(lx, y, lz, water);
        }
        int depth = c.waterTop() - bed;
        if (w.frozen()) {
            boolean hole = hole(x, z);
            if (!hole) {
                out.set(lx, c.waterTop(), lz, r100(x, 1, z, 61) < 22 ? packedIce : ice);
                if (!reserved && !nearHole(x, z, 2) && r100(x, 2, z, 62) < 28) {
                    out.set(lx, c.waterTop() + 1, lz, snow[1 + r100(x, 3, z, 63) % 2]);
                }
            }
            return;
        }
        if (reserved) {
            return;
        }
        int roll = r100(x, 4, z, 64);
        if (w.bed() == Terrain.Bed.MOOR) {
            if (roll < 12) {
                out.set(lx, c.waterTop() + 1, lz, lilyPad);
            } else if (roll < 22 && depth >= 2) {
                out.set(lx, bed + 1, lz, seagrass);
            }
        } else if (w.bed() == Terrain.Bed.LAKE) {
            if (depth >= 3 && roll < 5) {
                out.set(lx, bed + 1, lz, seagrass);
            } else if (depth <= 3 && x < -25 && roll >= 95) {
                out.set(lx, c.waterTop() + 1, lz, lilyPad);
            }
        }
    }

    /** Liegt die Stelle in einem Eisloch (dort kein Eis)? */
    public static boolean hole(int x, int z) {
        for (int[] hl : Layout.ICE_HOLES) {
            if (Math.abs(x - hl[0]) <= 1 && Math.abs(z - hl[1]) <= 1) {
                return true;
            }
        }
        return false;
    }

    private static boolean nearHole(int x, int z, int within) {
        for (int[] hl : Layout.ICE_HOLES) {
            if (Math.abs(x - hl[0]) <= 1 + within && Math.abs(z - hl[1]) <= 1 + within) {
                return true;
            }
        }
        return false;
    }

    private short bedBlock(Terrain.Bed bed, int x, int z, int depth) {
        int r = r100(x, 5, z, 65);
        return switch (bed) {
            case LAKE -> depth <= 2 ? (r < 60 ? sand : gravel)
                    : r < 45 ? gravel : r < 65 ? clay : r < 80 ? sand : r < 90 ? mud : stone;
            case STREAM -> r < 50 ? gravel : r < 75 ? cobble : r < 90 ? mossyCobble : sand;
            case MOOR -> r < 70 ? mud : r < 85 ? clay : dirt;
            case KAR -> r < 50 ? gravel : r < 80 ? stone : clay;
        };
    }

    private short topBlock(int x, int z, int top, int slope, boolean path, Terrain.Column[][] cols, int lx, int lz) {
        int r = r100(x, top, z, 66);
        boolean kar = t.inKar(x, z) && top >= Layout.ICE_Y - 1 && top < 100;
        if (path) {
            if (kar || top >= 80) {
                return r < 55 ? gravel : r < 85 ? cobble : andesite;
            }
            return r < 55 ? dirtPath : r < 75 ? coarse : gravel;
        }
        if (kar) {
            return slope >= 3 && r < 50 ? stone : snowBlock;
        }
        if (top >= SNOW_LINE && slope <= 3) {
            return snowBlock;
        }
        if (top >= ROCK_LINE || slope >= 3) {
            if (top >= 138 && slope <= 2 && r < 60) {
                return snowBlock;
            }
            return r < 45 ? stone : r < 65 ? andesite : r < 75 ? tuff : r < 82 ? cobble : r < 90 ? gravel : stone;
        }
        boolean wetNear = waterNear(cols, lx, lz);
        if (t.inMoor(x, z) && top <= 65) {
            return r < 25 ? mud : r < 45 ? moss : r < 85 ? grass : r < 95 ? podzol : coarse;
        }
        if (wetNear && top <= Layout.GROUND_Y + 1) {
            if (t.nearLake(x, z, 3)) {
                return r < 45 ? sand : r < 70 ? gravel : grass;
            }
            return r < 35 ? gravel : r < 55 ? mossyCobble : r < 65 ? cobble : grass;
        }
        if (top >= 72) {
            return r < 12 ? podzol : r < 16 ? coarse : grass;
        }
        return r < 3 ? coarse : grass;
    }

    private static boolean waterNear(Terrain.Column[][] cols, int lx, int lz) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (cols[lx + 2 + dx][lz + 2 + dz].wet()) {
                    return true;
                }
            }
        }
        return false;
    }

    private short sub(short top, int x, int y, int z) {
        if (top == snowBlock || top == stone || top == andesite || top == tuff || top == cobble) {
            return y % 2 == 0 ? stone : andesite;
        }
        if (top == mud || top == moss) {
            return mud;
        }
        if (top == sand) {
            return sand;
        }
        if (top == gravel || top == mossyCobble) {
            return gravel;
        }
        return r100(x, y, z, 67) < 8 ? rooted : dirt;
    }

    private short rock(int x, int y, int z) {
        int r = r100(x, y, z, 68);
        return r < 62 ? stone : r < 80 ? andesite : r < 92 ? tuff : gravel;
    }

    private void decor(Chunk out, int lx, int lz, int x, int z, int top, short topBlock, Terrain.Column[][] cols) {
        int r = r100(x, top + 1, z, 69);
        int y = top + 1;
        if (topBlock == snowBlock) {
            if (r < 70) {
                out.set(lx, y, lz, snow[1 + r100(x, 7, z, 70) % 2]);
            }
            return;
        }
        boolean moorZone = t.inMoor(x, z) && top <= 65;
        if (topBlock == grass || topBlock == podzol || topBlock == moss || topBlock == mud || topBlock == sand
                || topBlock == coarse) {
            if (top == Layout.GROUND_Y && besideWater(cols, lx, lz)) {
                int chance = moorZone ? 45 : t.nearLake(x, z, 2) ? 10 : 6;
                if (r < chance && topBlock != sand) {
                    int tall = 1 + r100(x, 8, z, 71) % 3;
                    out.set(lx, top, lz, dirt);
                    for (int i = 0; i < tall; i++) {
                        out.set(lx, y + i, lz, sugarCane);
                    }
                    return;
                }
            }
        }
        if (topBlock == sand || topBlock == gravel || topBlock == mossyCobble || topBlock == cobble) {
            return;
        }
        if (moorZone) {
            if (r < 18) {
                out.set(lx, y, lz, shortGrass);
            } else if (r < 22) {
                out.set(lx, y, lz, deadBush);
            } else if (r < 25) {
                out.set(lx, y, lz, firefly);
            } else if (r < 29) {
                out.set(lx, y, lz, fern);
            } else if (r < 31) {
                out.set(lx, y, lz, tallGrass[0]);
                out.set(lx, y + 1, lz, tallGrass[1]);
            }
            return;
        }
        if (topBlock != grass && topBlock != podzol) {
            return;
        }
        if (top < 76) {
            if (r < 22) {
                out.set(lx, y, lz, shortGrass);
            } else if (r < 27) {
                out.set(lx, y, lz, flowers[r100(x, 9, z, 72) % flowers.length]);
            } else if (r < 29) {
                out.set(lx, y, lz, tallGrass[0]);
                out.set(lx, y + 1, lz, tallGrass[1]);
            }
        } else if (top < TREE_LINE) {
            if (r < 12) {
                out.set(lx, y, lz, fern);
            } else if (r < 18) {
                out.set(lx, y, lz, shortGrass);
            } else if (r < 20) {
                out.set(lx, y, lz, largeFern[0]);
                out.set(lx, y + 1, lz, largeFern[1]);
            } else if (r == 20) {
                out.set(lx, y, lz, mossyCobble);
            }
        }
    }

    private static boolean besideWater(Terrain.Column[][] cols, int lx, int lz) {
        for (int[] d : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
            Terrain.Column n = cols[lx + 2 + d[0]][lz + 2 + d[1]];
            if (n.wet() && n.waterTop() == Layout.WATER_Y && !n.water().frozen()) {
                return true;
            }
        }
        return false;
    }

    // ---------- Bäume ----------

    private void trees(Chunk out, int x0, int z0) {
        int c1x = Math.floorDiv(x0 - TREE_REACH, TREE_CELL);
        int c2x = Math.floorDiv(x0 + 15 + TREE_REACH, TREE_CELL);
        int c1z = Math.floorDiv(z0 - TREE_REACH, TREE_CELL);
        int c2z = Math.floorDiv(z0 + 15 + TREE_REACH, TREE_CELL);
        for (int cx = c1x; cx <= c2x; cx++) {
            for (int cz = c1z; cz <= c2z; cz++) {
                int tx = cx * TREE_CELL + Noise.pick(cx, cz, 81, TREE_CELL);
                int tz = cz * TREE_CELL + Noise.pick(cx, cz, 82, TREE_CELL);
                tree(out, x0, z0, tx, tz, Noise.hash(cx, cz, 83));
            }
        }
    }

    private void tree(Chunk out, int x0, int z0, int x, int z, double roll) {
        if (Math.abs(x) > Layout.EXTENT || Math.abs(z) > Layout.EXTENT || plan.reserved(x, z)
                || t.inVillage(x, z)) {
            return;
        }
        Terrain.Column c = t.column(x, z);
        if (c.wet()) {
            return;
        }
        int h = c.ground();
        int slope = 0;
        for (int[] d : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
            slope = Math.max(slope, Math.abs(t.height(x + d[0], z + d[1]) - h));
        }
        if (slope > 2 || h >= TREE_LINE || t.pathDistance(x, z) < 2.5 || t.nearLake(x, z, 5)) {
            return;
        }
        for (int[] d : new int[][] {{-2, 0}, {2, 0}, {0, -2}, {0, 2}}) {
            if (t.waterAt(x + d[0], z + d[1]) != null) {
                return;
            }
        }
        boolean kar = t.inKar(x, z);
        boolean moorZone = t.inMoor(x, z);
        double p;
        if (kar) {
            p = h >= Layout.KAR_Y ? 0.18 : 0;
        } else if (moorZone) {
            p = 0.22;
        } else if (h < 66) {
            p = 0.05;
        } else if (h < 74) {
            p = 0.35;
        } else if (h < 116) {
            p = 0.72;
        } else {
            p = 0.3;
        }
        if (roll >= p) {
            return;
        }
        int size = Noise.pick(x, z, 84, 100);
        boolean snowy = kar || h >= 112;
        if (moorZone) {
            birch(out, x0, z0, x, h + 1, z, 4 + size % 3, 2);
        } else if (h < 66 && size < 50) {
            birch(out, x0, z0, x, h + 1, z, 5 + size % 3, 2);
        } else if (h < 66) {
            oak(out, x0, z0, x, h + 1, z, 4 + size % 2);
        } else {
            spruce(out, x0, z0, x, h + 1, z, 7 + size % 7, snowy);
        }
    }

    private void put(Chunk out, int x0, int z0, int x, int y, int z, short id, boolean force) {
        int lx = x - x0;
        int lz = z - z0;
        if (lx < 0 || lx > 15 || lz < 0 || lz > 15) {
            return;
        }
        if (force || out.get(lx, y, lz) == air) {
            out.set(lx, y, lz, id);
        }
    }

    private void spruce(Chunk out, int x0, int z0, int x, int y, int z, int height, boolean snowy) {
        for (int i = 0; i < height; i++) {
            put(out, x0, z0, x, y + i, z, spruceLog, true);
        }
        int top = y + height;
        put(out, x0, z0, x, top, z, spruceLeaves, false);
        put(out, x0, z0, x, top + 1, z, spruceLeaves, false);
        if (snowy) {
            put(out, x0, z0, x, top + 2, z, snow[2], false);
        }
        for (int layer = 1; layer < height - 2; layer++) {
            int ly = top - layer;
            int rad = Math.max(1, Math.min(3, 1 + layer / 2 - (layer % 2 == 0 ? 0 : 1)));
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    if ((dx == 0 && dz == 0) || Math.abs(dx) + Math.abs(dz) > rad + 1
                            || Noise.pick(x + dx, ly * 31 + z + dz, 85, 10) == 0) {
                        continue;
                    }
                    put(out, x0, z0, x + dx, ly, z + dz, spruceLeaves, false);
                    if (snowy && layer % 2 == 1) {
                        put(out, x0, z0, x + dx, ly + 1, z + dz, snow[1], false);
                    }
                }
            }
        }
    }

    private void birch(Chunk out, int x0, int z0, int x, int y, int z, int trunk, int radius) {
        crown(out, x0, z0, x, y, z, trunk, radius, birchLog, birchLeaves);
    }

    private void oak(Chunk out, int x0, int z0, int x, int y, int z, int trunk) {
        crown(out, x0, z0, x, y, z, trunk, 3, oakLog, oakLeaves);
    }

    private void crown(Chunk out, int x0, int z0, int x, int y, int z, int trunk, int radius, short log, short leaf) {
        int cy = y + trunk;
        for (int i = 0; i < trunk + 1; i++) {
            put(out, x0, z0, x, y + i, z, log, true);
        }
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius + 1; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy * 1.7 + dz * dz);
                    if (d <= radius + 0.35 - Noise.pick(x + dx, (cy + dy) * 13 + z + dz, 86, 10) * 0.07) {
                        put(out, x0, z0, x + dx, cy + dy, z + dz, leaf, false);
                    }
                }
            }
        }
    }
}
