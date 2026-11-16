package de.moinmorni.angelkoenig.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.angelkoenig.fish.Spot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TerrainTest {

    private static Terrain t;
    private static Plan plan;
    private static Palette palette;
    private static Surface surface;

    @BeforeAll
    static void setUp() {
        t = new Terrain();
        plan = Valley.build(t);
        palette = new Palette();
        surface = new Surface(t, plan, palette);
    }

    private static boolean fall(int x, int z) {
        return x == Layout.FALL_X && z >= Layout.FALL_Z1 && z <= Layout.FALL_Z2;
    }

    /** Kein Gewässer läuft aus: jeder Nachbar ist Wasser gleicher Höhe oder Land, das mindestens so hoch ist. */
    @Test
    void everyWaterIsEnclosed() {
        List<String> leaks = new ArrayList<>();
        int e = Layout.EXTENT;
        for (int x = -e; x <= e; x++) {
            for (int z = -e; z <= e; z++) {
                Terrain.Column c = t.column(x, z);
                if (!c.wet()) {
                    continue;
                }
                for (int[] d : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                    int nx = x + d[0];
                    int nz = z + d[1];
                    Terrain.Column n = t.column(nx, nz);
                    boolean ok = n.wet() ? n.waterTop() >= c.waterTop() : n.ground() >= c.waterTop();
                    if (!ok && !(fall(nx, nz) || fall(x, z))) {
                        leaks.add(c.water().id() + " bei " + x + "/" + z + " → " + nx + "/" + nz);
                    }
                }
            }
        }
        assertTrue(leaks.isEmpty(), "Wasser läuft aus: " + leaks.subList(0, Math.min(10, leaks.size())));
    }

    /** Wasser aus dem Bauplan (Bootshaus, Brunnen, Wasserfall) ist ebenfalls eingefasst. */
    @Test
    void plannedWaterIsEnclosed() {
        List<String> leaks = new ArrayList<>();
        plan.forEach((k, spec) -> {
            if (!spec.startsWith("water") || spec.contains("level=8")) {
                return;
            }
            int x = Plan.keyX(k);
            int y = Plan.keyY(k);
            int z = Plan.keyZ(k);
            for (int[] d : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {0, -1, 0}}) {
                int nx = x + d[0];
                int ny = d.length == 3 ? y - 1 : y;
                int nz = z + (d.length == 3 ? 0 : d[1]);
                if (!holds(nx, ny, nz)) {
                    leaks.add(x + "/" + y + "/" + z + " → " + nx + "/" + ny + "/" + nz);
                }
            }
        });
        assertTrue(leaks.isEmpty(), "Wasser aus dem Plan läuft aus: " + leaks);
    }

    private static boolean holds(int x, int y, int z) {
        String p = plan.get(x, y, z);
        if (p != null) {
            return !Plan.AIR.equals(p);
        }
        Terrain.Column c = t.column(x, z);
        if (c.wet()) {
            return y <= c.waterTop();
        }
        return y <= c.ground();
    }

    /** Die Plätze des Wegweisers stehen auf festem Boden, vor ihnen liegt das richtige Wasser. */
    @Test
    void fishingPlacesFaceTheirWater() {
        for (Map.Entry<Spot, Layout.Place> e : Layout.SPOTS.entrySet()) {
            Layout.Place pl = e.getValue();
            int bx = (int) Math.floor(pl.x());
            int bz = (int) Math.floor(pl.z());
            int feet = (int) Math.floor(pl.y() - 0.01);
            assertTrue(solid(bx, feet, bz) || solid(bx, (int) Math.floor(pl.y()) - 1, bz), e.getKey() + " steht in der Luft");
            assertFalse(solid(bx, (int) Math.ceil(pl.y()), bz), e.getKey() + ": Kopf im Block");
            double rad = Math.toRadians(pl.yaw());
            double dx = -Math.sin(rad);
            double dz = Math.cos(rad);
            boolean water = false;
            for (int step = 2; step <= 6; step++) {
                int wx = (int) Math.floor(pl.x() + dx * step);
                int wz = (int) Math.floor(pl.z() + dz * step);
                if (t.spotAt(wx, wz) == e.getKey() && (e.getKey() != Spot.EIS || Surface.hole(wx, wz))) {
                    water = true;
                }
            }
            assertTrue(water, e.getKey() + ": kein passendes Wasser vor dem Platz");
        }
    }

    private static boolean solid(int x, int y, int z) {
        String p = plan.get(x, y, z);
        if (p != null) {
            return !Plan.AIR.equals(p) && !p.startsWith("water") && !p.startsWith("snow[");
        }
        Terrain.Column c = t.column(x, z);
        if (c.wet()) {
            return c.water().frozen() && y == c.waterTop() && !Surface.hole(x, z) || y <= c.bed();
        }
        return y <= c.ground();
    }

    @Test
    void spawnIsOnThePlaza() {
        int x = (int) Math.floor(Layout.SPAWN.x());
        int z = (int) Math.floor(Layout.SPAWN.z());
        assertNotNull(plan.get(x, Layout.GROUND_Y, z), "Pflaster unter der Ankunft");
        assertEquals(Layout.GROUND_Y, t.height(x, z));
        assertFalse(t.column(x, z).wet());
    }

    @Test
    void stegEndsOverDeepWater() {
        for (int x = Layout.DECK_X1; x <= Layout.DECK_X2; x++) {
            Terrain.Column c = t.column(x, Layout.DECK_Z2 + 1);
            assertTrue(c.wet(), "vor der Plattform ist Wasser");
            assertTrue(c.waterTop() - c.bed() >= 3, "tief genug zum Angeln: " + x);
            assertEquals(Spot.BERGSEE, t.spotAt(x, Layout.DECK_Z2 + 2));
        }
        for (Layout.Place b : Layout.BOATS) {
            assertTrue(t.column((int) Math.floor(b.x()), (int) Math.floor(b.z())).wet(), "Boot liegt im Wasser");
        }
    }

    @Test
    void iceHolesAreInTheFrozenLake() {
        for (int[] h : Layout.ICE_HOLES) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Terrain.Column c = t.column(h[0] + dx, h[1] + dz);
                    assertTrue(c.wet() && c.water().frozen(), "Eisloch " + h[0] + "/" + h[1]);
                }
            }
            assertEquals(Spot.EIS, t.spotAt(h[0], h[1]));
        }
    }

    /** Wege kann man gehen: zwischen zwei Schritten höchstens ein Block Höhe. */
    @Test
    void pathsAreWalkable() {
        for (Shape.Line line : t.paths()) {
            List<double[]> pts = line.points();
            int lastH = Integer.MIN_VALUE;
            for (int i = 1; i < pts.size(); i++) {
                double[] a = pts.get(i - 1);
                double[] b = pts.get(i);
                double len = Math.hypot(b[0] - a[0], b[1] - a[1]);
                for (double s = 0; s <= len; s += 0.5) {
                    int x = (int) Math.floor(a[0] + (b[0] - a[0]) * s / len);
                    int z = (int) Math.floor(a[1] + (b[1] - a[1]) * s / len);
                    if (t.column(x, z).wet() || plan.reserved(x, z)) {
                        lastH = Integer.MIN_VALUE;
                        continue;
                    }
                    int h = t.height(x, z);
                    if (lastH != Integer.MIN_VALUE) {
                        assertTrue(Math.abs(h - lastH) <= 1, "Stufe auf dem Weg bei " + x + "/" + z + ": " + lastH
                                + " → " + h);
                    }
                    lastH = h;
                }
            }
        }
    }

    /** Blocknamen von 26.x: alte Namen wie „chain“ oder „grass“ würden still fehlen. */
    @Test
    void noOldBlockNames() {
        Set<String> old = Set.of("chain", "grass", "grass_path", "sign", "wall_sign", "lava_cauldron_old");
        surface.generate(0, -3);
        Set<String> all = new java.util.HashSet<>(plan.materials());
        for (String s : palette.specs()) {
            int i = s.indexOf('[');
            all.add(i < 0 ? s : s.substring(0, i));
        }
        for (String m : all) {
            assertFalse(old.contains(m), "alter Blockname: " + m);
        }
    }

    @Test
    void chunkHasGroundWaterAndBuildings() {
        Surface.Chunk c = surface.generate(0, -2);
        short planks = palette.id("spruce_planks");
        assertEquals(planks, c.get(0, Layout.GROUND_Y, -20 + 32), "Steg aus Planken über dem See");
        Surface.Chunk village = surface.generate(0, -3);
        assertEquals(0, village.get(0, Layout.STAND_Y, -47 + 48), "Ankunft frei");
    }

    @Test
    void biomesFitTheZones() {
        assertEquals("minecraft:swamp", t.biome(100, 63, 6));
        assertEquals("minecraft:snowy_plains", t.biome(78, 83, 100));
        assertEquals("minecraft:meadow", t.biome(0, 63, -40));
        assertEquals("minecraft:frozen_peaks", t.biome(14, 190, 132));
    }
}
