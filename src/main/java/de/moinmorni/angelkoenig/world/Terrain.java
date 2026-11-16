package de.moinmorni.angelkoenig.world;

import de.moinmorni.angelkoenig.fish.Spot;
import java.util.ArrayList;
import java.util.List;

/**
 * Das Gelände des Tals aus dem Code: Berge ringsum (Rauschen + einzelne Gipfel), darin ausgehoben Tal, Dorf,
 * Bachtal, Moor und das Kar mit dem Eissee; Felsstufe mit Wasserfall hinter der Gumpe; Wege; Gewässer. Ohne Server
 * – der Chunk-Erzeuger fragt hier jede Säule ab, Tests prüfen, dass alle Gewässer dicht sind.
 */
public final class Terrain {

    /** Wie ein Aushub wirkt: nur absenken, genau setzen (Wege) oder nur anheben. */
    enum Mode {
        MIN, SET, RAISE
    }

    /** Boden eines Gewässers (bestimmt die Blöcke am Grund). */
    public enum Bed {
        LAKE, STREAM, MOOR, KAR
    }

    /** Höhe eines Aushubs an einer Stelle. */
    interface Floor {
        double at(double x, double z);
    }

    record Carve(Shape shape, Floor floor, double falloff, Mode mode, double[] box) {

        Carve(Shape shape, Floor floor, double falloff, Mode mode) {
            this(shape, floor, falloff, mode, grow(shape.box(), falloff));
        }

        double apply(double x, double z, double h) {
            if (x < box[0] || z < box[1] || x > box[2] || z > box[3]) {
                return h;
            }
            double d = shape.dist(x, z);
            if (d >= falloff) {
                return h;
            }
            double target = floor.at(x, z);
            double t = d <= 0 ? 0 : Noise.smoothstep(0, 1, d / falloff);
            double v = target + (h - target) * t;
            return switch (mode) {
                case MIN -> Math.min(h, v);
                case SET -> v;
                case RAISE -> Math.max(h, v);
            };
        }
    }

    /** Ein Gewässer: Fläche, Wasseroberfläche, Tiefe (wächst vom Ufer weg um {@code slope} je Block). */
    public record Water(String id, Shape shape, int top, double maxDepth, double slope, Spot spot, boolean frozen,
            Bed bed, double[] box) {

        Water(String id, Shape shape, int top, double maxDepth, double slope, Spot spot, boolean frozen, Bed bed) {
            this(id, shape, top, maxDepth, slope, spot, frozen, bed, shape.box());
        }

        /** Tiefe an einer Stelle (mindestens 1) oder −1 außerhalb. */
        double depth(double x, double z) {
            if (x < box[0] - 1 || z < box[1] - 1 || x > box[2] + 1 || z > box[3] + 1) {
                return -1;
            }
            double d = shape.dist(x, z);
            if (d >= 0) {
                return -1;
            }
            return Math.max(1, Math.min(maxDepth, 1 + (-d) * slope));
        }
    }

    /** Ein Gipfel: Mitte, Höhe, Radius des Fußes. */
    record Peak(double x, double z, double top, double radius) {
    }

    /** Was in einer Säule steht. */
    public record Column(int ground, Water water, int waterTop, int bed) {

        public boolean wet() {
            return water != null;
        }
    }

    private static final List<Peak> PEAKS = List.of(
            new Peak(-62, 122, 196, 62), new Peak(14, 132, 214, 70), new Peak(96, 130, 184, 56),
            new Peak(-150, 104, 178, 64), new Peak(-180, -14, 170, 62), new Peak(-52, -150, 156, 66),
            new Peak(54, -142, 162, 62), new Peak(150, -110, 158, 60), new Peak(182, 24, 174, 64),
            new Peak(166, 128, 182, 64));

    private final List<Carve> carves = new ArrayList<>();
    private final List<Carve> late = new ArrayList<>();
    private final List<Water> waters = new ArrayList<>();
    private final List<Shape.Line> paths = new ArrayList<>();
    private final Shape moor;
    private final Shape kar;
    private final Shape village;
    private final Shape.Ellipse lake;

    public Terrain() {
        // ---------- Aushübe (nur absenken) ----------
        carves.add(new Carve(new Shape.Ellipse(0, 8, 95, 62, 0.05, 1),
                (x, z) -> 63 + 1.3 * Noise.fbm(x / 23, z / 23, 3, 3), 28, Mode.MIN));
        village = new Shape.Rect(-52, -68, 52, -22);
        carves.add(new Carve(village, (x, z) -> 63, 14, Mode.MIN));
        Shape.Line streamValley = line(11, new double[] {-50, 9}, new double[] {-66, 12},
                new double[] {-80, 7}, new double[] {-100, 2});
        carves.add(new Carve(streamValley, (x, z) -> 63 + 0.8 * Noise.fbm(x / 13, z / 13, 5, 2), 12, Mode.MIN));
        carves.add(new Carve(new Shape.Ellipse(-100, 2, 15, 13, 0.06, 4), (x, z) -> 63, 3, Mode.MIN));
        moor = new Shape.Ellipse(98, 4, 32, 26, 0.08, 6);
        carves.add(new Carve(moor, (x, z) -> 63 + 0.9 * Noise.fbm(x / 9, z / 9, 7, 2), 12, Mode.MIN));
        kar = new Shape.Ellipse(78, 100, 28, 20, 0.07, 8);
        carves.add(new Carve(kar, (x, z) -> 83 + 0.7 * Noise.fbm(x / 8, z / 8, 9, 2), 6, Mode.MIN));
        // Felsstufe hinter der Gumpe kommt in height() (scharfe Kante); danach die Klamm oberhalb.
        Shape.Line gorge = line(4, new double[] {-111, 1.5}, new double[] {-122, -1}, new double[] {-136, -7},
                new double[] {-152, -13});
        late.add(new Carve(gorge, (x, z) -> 76, 5, Mode.MIN));

        // ---------- Wege (genau auf ihre Höhe) ----------
        paths.add(path(new double[] {-6, -46, 63}, new double[] {-28, -44, 63}, new double[] {-46, -34, 63},
                new double[] {-60, -18, 63}, new double[] {-76, -12, 63}, new double[] {-92, -10, 63},
                new double[] {-100, -10, 63}));
        paths.add(path(new double[] {6, -46, 63}, new double[] {28, -44, 63}, new double[] {48, -34, 63},
                new double[] {62, -18, 63}, new double[] {70, -6, 63}));
        paths.add(path(new double[] {62, -18, 63}, new double[] {66, 4, 63}, new double[] {66, 30, 63},
                new double[] {62, 48, 63}, new double[] {52, 62, 67}, new double[] {58, 74, 72},
                new double[] {66, 82, 78}, new double[] {70, 89, 83}));
        paths.add(path(new double[] {-60, -18, 63}, new double[] {-66, -2, 63}, new double[] {-68, 14, 63},
                new double[] {-62, 30, 63}, new double[] {-44, 46, 63}, new double[] {-22, 54, 63},
                new double[] {-12, 55, 63}));
        for (Shape.Line p : paths) {
            late.add(new Carve(p, (x, z) -> p.heightAt(x, z), 3.5, Mode.SET));
        }

        // ---------- Gewässer (das erste, das eine Stelle enthält, gilt) ----------
        waters.add(new Water("oberbach", new Shape.ClipWest(line(2.0, new double[] {-111, 1.5},
                new double[] {-122, -1}, new double[] {-136, -7}, new double[] {-150, -13}), -110.5),
                Layout.UPPER_WATER_Y, 2, 1.2, Spot.BACH, false, Bed.STREAM));
        waters.add(new Water("gumpe", new Shape.Union(List.of(new Shape.Ellipse(-100, 2, 10, 8, 0.06, 5),
                blocks(Layout.FALL_X, Layout.FALL_Z1, -105, Layout.FALL_Z2))), Layout.WATER_Y, 5, 0.7, Spot.BACH,
                false, Bed.STREAM));
        lake = new Shape.Ellipse(0, 10, 56, 34, 0.07, 3);
        waters.add(new Water("bergsee", lake, Layout.WATER_Y, 11, 0.33, Spot.BERGSEE, false, Bed.LAKE));
        waters.add(new Water("bach", line(2.2, new double[] {-50, 9}, new double[] {-53, 9.5},
                new double[] {-66, 12}, new double[] {-80, 7}, new double[] {-93, 3}), Layout.WATER_Y, 2.5, 0.9,
                Spot.BACH, false, Bed.STREAM));
        waters.add(new Water("moorweiher", new Shape.Ellipse(100, 6, 13, 10, 0.1, 11), Layout.WATER_Y, 4, 0.6,
                Spot.MOOR, false, Bed.MOOR));
        waters.add(new Water("moor-nord", new Shape.Ellipse(84, -9, 5, 4, 0.1, 12), Layout.WATER_Y, 2, 0.6,
                Spot.MOOR, false, Bed.MOOR));
        waters.add(new Water("moor-sued", new Shape.Ellipse(117, 19, 6, 5, 0.1, 13), Layout.WATER_Y, 3, 0.6,
                Spot.MOOR, false, Bed.MOOR));
        waters.add(new Water("moor-ost", new Shape.Ellipse(111, -11, 4, 3, 0.1, 14), Layout.WATER_Y, 2, 0.6,
                Spot.MOOR, false, Bed.MOOR));
        waters.add(new Water("karsee", new Shape.Ellipse(78, 100, 17, 11, 0.05, 15), Layout.ICE_Y, 6, 0.6, Spot.EIS,
                true, Bed.KAR));
    }

    private static Shape.Line line(double halfWidth, double[]... points) {
        return new Shape.Line(List.of(points), halfWidth);
    }

    private static Shape.Line path(double[]... points) {
        return new Shape.Line(List.of(points), 1.5);
    }

    /** Rechteck in Blockkoordinaten (beide Ecken eingeschlossen). */
    static Shape.Rect blocks(int x1, int z1, int x2, int z2) {
        return new Shape.Rect(Math.min(x1, x2), Math.min(z1, z2), Math.max(x1, x2) + 1, Math.max(z1, z2) + 1);
    }

    private static double[] grow(double[] b, double by) {
        return new double[] {b[0] - by, b[1] - by, b[2] + by, b[3] + by};
    }

    // ---------- Höhe ----------

    /** Berge ringsum: steigen mit dem Abstand vom See, dazu Grate und einzelne Gipfel. */
    double mountains(double x, double z) {
        double dx = x / 112;
        double dz = (z - 6) / 78;
        double r = Math.sqrt(dx * dx + dz * dz);
        double s = Noise.smoothstep(0.70, 1.9, r);
        double h = 63;
        if (s > 0) {
            double n = Noise.fbm(x / 64, z / 64, 11, 4);
            double rg = Noise.ridge(x / 88, z / 88, 23, 3);
            h += s * 128 * (0.18 + 0.57 * rg + 0.25 * n);
        }
        for (Peak p : PEAKS) {
            double d = Math.hypot(x - p.x(), z - p.z());
            if (d < p.radius()) {
                double t = 1 - d / p.radius();
                double n = Noise.fbm(x / 21, z / 21, 17, 3);
                h = Math.max(h, 63 + (p.top() - 63) * Math.pow(t, 1.15) * (0.9 + 0.2 * n));
            }
        }
        return h;
    }

    /** Geländehöhe an einer Stelle (Kommazahl, vor dem Runden). */
    double heightAt(double x, double z) {
        double h = mountains(x, z);
        for (Carve c : carves) {
            h = c.apply(x, z, h);
        }
        h = cliff(x, z, h);
        for (Carve c : late) {
            h = c.apply(x, z, h);
        }
        return h;
    }

    /** Felsstufe hinter der Gumpe: halbrund, oben der Bach, in der Mitte der Spalt für den Wasserfall. */
    private static double cliff(double x, double z, double h) {
        double dz = z - 1.5;
        double ad = Math.abs(dz);
        if (ad >= 17) {
            return h;
        }
        double edge = -109.0 - 0.035 * dz * dz;
        if (x >= edge) {
            return h;
        }
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        if (bx == Layout.FALL_X && bz >= Layout.FALL_Z1 && bz <= Layout.FALL_Z2) {
            return h;
        }
        double w = ad <= 11 ? 1 : 1 - (ad - 11) / 6;
        double target = 77 + 5 * Noise.fbm(x / 9, z / 9, 41, 2) + Math.max(0, edge - x) * 0.25;
        return Math.max(h, h + (target - h) * w);
    }

    /** Oberster fester Block einer Säule (ohne Wasser). */
    public int height(int x, int z) {
        return (int) Math.floor(heightAt(x + 0.5, z + 0.5));
    }

    /** Gewässer an einer Stelle (oder null). */
    public Water waterAt(int x, int z) {
        double cx = x + 0.5;
        double cz = z + 0.5;
        for (Water w : waters) {
            if (w.depth(cx, cz) > 0) {
                return w;
            }
        }
        return null;
    }

    /** Alles über eine Säule. */
    public Column column(int x, int z) {
        int ground = height(x, z);
        double cx = x + 0.5;
        double cz = z + 0.5;
        for (Water w : waters) {
            double d = w.depth(cx, cz);
            if (d > 0) {
                int bed = w.top() - (int) Math.round(d);
                return new Column(ground, w, w.top(), bed);
            }
        }
        return new Column(ground, null, Integer.MIN_VALUE, ground);
    }

    /** Fangplatz eines Wasser-Blocks (oder null, wenn dort kein Gewässer ist). */
    public Spot spotAt(int x, int z) {
        Water w = waterAt(x, z);
        if (w != null) {
            return w.spot();
        }
        if (x == Layout.FALL_X && z >= Layout.FALL_Z1 && z <= Layout.FALL_Z2) {
            return Spot.BACH;
        }
        return null;
    }

    /** Liegt die Stelle auf einem Weg? */
    public boolean onPath(int x, int z) {
        double cx = x + 0.5;
        double cz = z + 0.5;
        for (Shape.Line p : paths) {
            double[] b = p.box();
            if (cx < b[0] || cz < b[1] || cx > b[2] || cz > b[3]) {
                continue;
            }
            if (p.dist(cx, cz) <= 0) {
                return true;
            }
        }
        return false;
    }

    /** Abstand zum nächsten Weg (für Bäume: nicht zu nah). */
    public double pathDistance(int x, int z) {
        double best = Double.MAX_VALUE;
        for (Shape.Line p : paths) {
            double[] b = p.box();
            if (x < b[0] - 8 || z < b[1] - 8 || x > b[2] + 8 || z > b[3] + 8) {
                continue;
            }
            best = Math.min(best, p.dist(x + 0.5, z + 0.5));
        }
        return best;
    }

    public boolean inMoor(int x, int z) {
        return moor.dist(x + 0.5, z + 0.5) < 3;
    }

    public boolean inKar(int x, int z) {
        return kar.dist(x + 0.5, z + 0.5) < 2;
    }

    public boolean inVillage(int x, int z) {
        return village.dist(x + 0.5, z + 0.5) < 0;
    }

    /** Ufernah am Bergsee (für Sand und Schilf). */
    public boolean nearLake(int x, int z, double within) {
        return lake.dist(x + 0.5, z + 0.5) < within;
    }

    public List<Water> waters() {
        return List.copyOf(waters);
    }

    public List<Shape.Line> paths() {
        return List.copyOf(paths);
    }

    /**
     * Biom je Stelle und Höhe (Farben von Gras, Laub, Wasser – und ob Regen oder Schnee fällt): Moor = Sumpf, Kar und
     * Gipfel = Schnee, sonst Bergwiese.
     */
    public String biome(int x, int y, int z) {
        if (kar.dist(x + 0.5, z + 0.5) < 10 && y < 120) {
            return "minecraft:snowy_plains";
        }
        if (y >= 150) {
            return "minecraft:frozen_peaks";
        }
        if (y >= 116) {
            return "minecraft:snowy_slopes";
        }
        if (moor.dist(x + 0.5, z + 0.5) < 6) {
            return "minecraft:swamp";
        }
        return "minecraft:meadow";
    }
}
