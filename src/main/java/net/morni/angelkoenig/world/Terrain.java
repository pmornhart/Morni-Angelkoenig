package net.morni.angelkoenig.world;

import net.morni.angelkoenig.fish.Spot;
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
    /**
     * Ein Gipfel: Mitte, Höhe, Fuß-Radien (länglich, gedreht), Steilheit der Flanken und Stärke der Grate. So sieht
     * jeder Berg anders aus – breite Stöcke, schlanke Hörner, lange Kämme.
     */
    record Peak(double x, double z, double top, double rx, double rz, double angle, double steep, double ridges,
            boolean massive) {

        /** Schroffer Gipfel (Norden, Osten, Westen). */
        Peak(double x, double z, double top, double rx, double rz, double angle, double steep, double ridges) {
            this(x, z, top, rx, rz, angle, steep, ridges, false);
        }

        /** Kräftiger Alpengipfel (Süden): breiter Fuß, massige Grate, keine Nadeln. */
        static Peak massive(double x, double z, double top, double rx, double rz, double angle) {
            return new Peak(x, z, top, rx, rz, angle, 1, 0, true);
        }
    }

    /** Kar: eine Mulde in der Bergflanke (dort sammelt sich Schnee). */
    record Cirque(double x, double z, double radius, double depth) {
    }

    /** Was in einer Säule steht. */
    public record Column(int ground, Water water, int waterTop, int bed) {

        public boolean wet() {
            return water != null;
        }
    }

    private static final List<Peak> PEAKS = List.of(
            // Süden, hinter dem See (Blick vom Dorf): kräftige Stöcke mit breitem Fuß und massigen Graten
            Peak.massive(-64, 142, 184, 74, 56, 0.4), Peak.massive(16, 158, 198, 80, 60, -0.3),
            Peak.massive(98, 150, 178, 76, 52, 0.9), Peak.massive(-152, 124, 174, 64, 50, -0.5),
            Peak.massive(168, 146, 178, 66, 54, 0.3),
            // Vorgipfel davor
            Peak.massive(-28, 116, 138, 38, 30, 0.2), Peak.massive(52, 120, 142, 36, 30, -0.6),
            Peak.massive(-100, 110, 132, 36, 28, 1.1),
            // Westen, Norden, Osten
 new Peak(-182, -16, 172, 52, 70, 0.2, 1.7, 0.4),
            new Peak(-54, -152, 158, 74, 40, 0.1, 1.8, 0.35), new Peak(56, -144, 166, 50, 50, 0, 1.3, 0.5),
            new Peak(10, -120, 128, 36, 26, 0.5, 1.0, 0.4), new Peak(152, -112, 160, 56, 46, 0.7, 1.5, 0.45),
            new Peak(184, 22, 176, 48, 66, -0.2, 1.6, 0.4),
            new Peak(-120, -100, 140, 38, 30, -0.8, 1.0, 0.45));

    private static final List<Cirque> CIRQUES = List.of(new Cirque(-58, 120, 24, 14), new Cirque(10, 130, 26, 16),
            new Cirque(-146, 102, 22, 12), new Cirque(50, -122, 18, 20), new Cirque(150, -92, 16, 18));

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
        waters.add(new Water("oberbach", new Shape.ClipWest(line(2.3, new double[] {-111, 2.0},
                new double[] {-122, -1}, new double[] {-136, -7}, new double[] {-150, -13}), -110.5),
                Layout.UPPER_WATER_Y, 2, 1.2, Spot.BACH, false, Bed.STREAM));
        waters.add(new Water("gumpe", new Shape.Union(List.of(new Shape.Ellipse(-100, 2, 10, 8, 0.06, 5),
                blocks(Layout.FALL_X + 1, Layout.FALL_Z1, -105, Layout.FALL_Z2))), Layout.WATER_Y, 5, 0.7, Spot.BACH,
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
        // leicht verbogene Koordinaten: keine geraden Kanten, keine runden Kegel
        double wx = x + 22 * (Noise.fbm(x / 70, z / 70, 31, 2) - 0.5);
        double wz = z + 22 * (Noise.fbm(x / 70, z / 70, 37, 2) - 0.5);
        double dx = wx / 112;
        double dz = (wz - 6) / 78;
        double r = Math.sqrt(dx * dx + dz * dz);
        double s = Noise.smoothstep(0.70, 1.9, r);
        double h = 63;
        double rg = Noise.ridge(wx / 70, wz / 70, 23, 3);
        if (s > 0) {
            double n = Noise.fbm(wx / 64, wz / 64, 11, 4);
            h += s * 120 * (0.16 + 0.6 * rg + 0.24 * n);
        }
        double massive = 0;
        for (Peak p : PEAKS) {
            h = Math.max(h, peak(p, wx, wz, 0, 0, 1, 1));
            if (p.massive()) {
                massive = Math.max(massive, 1 - Noise.smoothstep(0.75, 1.05, reach(p, wx, wz)));
                // Nebengipfel dicht am Hauptgipfel: dazwischen ein hoher, breiter Grat statt einzelner Spitzen
                double ax = Math.cos(p.angle()) * p.rx();
                double az = Math.sin(p.angle()) * p.rx();
                h = Math.max(h, peak(p, wx, wz, ax * 0.42, az * 0.42, 0.88, 0.78));
                h = Math.max(h, peak(p, wx, wz, -ax * 0.36, -az * 0.36, 0.83, 0.72));
            } else if (p.top() >= 170) {
                // zweiter, etwas niedrigerer Gipfel am Grat: Massiv statt Kegel
                double off = 0.42;
                h = Math.max(h, peak(p, wx, wz, Math.cos(p.angle()) * p.rx() * off,
                        Math.sin(p.angle()) * p.rx() * off, 0.86, 0.6));
                h = Math.max(h, peak(p, wx, wz, -Math.cos(p.angle()) * p.rx() * off * 0.8,
                        -Math.sin(p.angle()) * p.rx() * off * 0.8, 0.8, 0.55));
            }
        }
        // Rinnen: schmale Erosionsfurchen in den Flanken
        if (h > 95) {
            double gully = 1 - Noise.ridge(wx / 13.0 + wz / 40.0, wz / 13.0, 57, 2);
            double depth = Math.min(1, (h - 95) / 40);
            h -= 9 * (1 - 0.65 * massive) * depth * gully * gully;
        }
        for (Cirque k : CIRQUES) {
            double d = Math.hypot(wx - k.x(), wz - k.z()) / k.radius();
            if (d < 1.4) {
                double w = 1 - Noise.smoothstep(0.6, 1.4, d);
                h = Math.max(Math.min(h, 100), h - k.depth() * w);
            }
        }
        return h;
    }

    /**
     * Kräftiger Alpengipfel: Profil zwischen Pyramide und S-Kurve – am Fuß breit auslaufend, oben ein klarer, aber
     * stumpfer Gipfel; Rippen in den Flanken, leichte Wellen im Grat, kaum Ausbuchtungen im Umriss.
     */
    private static double massivePeak(Peak p, double wx, double wz, double ox, double oz, double scale, double size) {
        double dx = wx - p.x() - ox;
        double dz = wz - p.z() - oz;
        double c = Math.cos(p.angle());
        double sn = Math.sin(p.angle());
        double u = (dx * c + dz * sn) / (p.rx() * size * BROAD);
        double v = (-dx * sn + dz * c) / (p.rz() * size * BROAD);
        // drei oder vier Grate laufen vom Gipfel herab, dazwischen breite Flanken (dort sammelt sich Schnee)
        double ang = Math.atan2(v, u);
        int arms = 3 + Noise.pick((int) p.x(), (int) p.z(), 63, 2);
        double arm = 0.5 + 0.5 * Math.cos(arms * (ang - p.angle() * 1.7));
        double lobe = 0.8 + 0.3 * arm * arm + 0.12 * (Noise.fbm(Math.cos(ang) * 1.4 + p.x() * 0.01,
                Math.sin(ang) * 1.4 + p.z() * 0.01, 53, 2) - 0.5);
        double d = Math.sqrt(u * u + v * v) / lobe;
        if (d >= 1) {
            return 63;
        }
        double t = 1 - d;
        // oben ein klarer Gipfelaufbau (keine Kuppe, keine Nadel)
        double summit = Math.pow(Math.max(0, (t - 0.78) / 0.22), 1.6);
        double body = (0.62 * t + 0.38 * t * t * (3 - 2 * t) + 0.07 * summit) / 1.07;
        double swell = 0.93 + 0.14 * Noise.fbm(wx / 45, wz / 45, 59, 2);
        double ribs = 26 * t * (1 - t) * (Noise.ridge(wx / 28, wz / 28, 61, 2) - 0.45);
        double crag = Noise.fbm(wx / 9, wz / 9, 17, 2);
        return 63 + (p.top() - 63) * scale * body * swell + ribs + 5 * (crag - 0.5) * t;
    }

    /** Breite der Gipfel: flachere Flanken, breitere Stöcke statt Nadeln. */
    private static final double BROAD = 1.3;

    /** Höhe eines Gipfels (verschoben um ox/oz, Höhe und Breite skaliert); 63, wenn die Stelle außerhalb liegt. */
    /** Abstand zur Mitte eines Gipfels: 0 in der Mitte, 1 am Fuß (ohne Ausbuchtungen). */
    private static double reach(Peak p, double wx, double wz) {
        double dx = wx - p.x();
        double dz = wz - p.z();
        double c = Math.cos(p.angle());
        double sn = Math.sin(p.angle());
        double u = (dx * c + dz * sn) / (p.rx() * BROAD);
        double v = (-dx * sn + dz * c) / (p.rz() * BROAD);
        return Math.sqrt(u * u + v * v);
    }

    private static double peak(Peak p, double wx, double wz, double ox, double oz, double scale, double size) {
        if (p.massive()) {
            return massivePeak(p, wx, wz, ox, oz, scale, size);
        }
        double dx = wx - p.x() - ox;
        double dz = wz - p.z() - oz;
        double c = Math.cos(p.angle());
        double sn = Math.sin(p.angle());
        double u = (dx * c + dz * sn) / (p.rx() * size * BROAD);
        double v = (-dx * sn + dz * c) / (p.rz() * size * BROAD);
        double ang = Math.atan2(v, u);
        double lobe = 0.74 + 0.55 * Noise.fbm(Math.cos(ang) * 1.6 + p.x() * 0.01, Math.sin(ang) * 1.6 + p.z() * 0.01,
                53, 2);
        double d = Math.sqrt(u * u + v * v) / lobe;
        if (d >= 1) {
            return 63;
        }
        double t = 1 - d;
        double profile = Math.pow(t, p.steep());
        double ridge = Noise.ridge(wx / 26, wz / 26, 19, 2);
        double crag = Noise.fbm(wx / 11, wz / 11, 17, 2);
        return 63 + (p.top() - 63) * scale * profile * (1 - p.ridges() * 0.35 + p.ridges() * 0.7 * ridge)
                + 6 * (crag - 0.5) * t;
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
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        // zerklüftete Kante: springt vor und zurück, nur um den Fall herum gerade
        double jag = ad < 4 ? 0 : 2.6 * (Noise.fbm(z / 3.5, 7, 43, 2) - 0.35);
        double edge = -109.0 - 0.035 * dz * dz - jag;
        if (bx == Layout.FALL_X && bz >= Layout.FALL_Z1 && bz <= Layout.FALL_Z2) {
            return Math.max(h, Layout.FALL_LEDGE_Y);
        }
        if (x >= edge) {
            return h;
        }
        double w = ad <= 11 ? 1 : 1 - (ad - 11) / 6;
        double into = edge - x;
        double full = 77 + 5 * Noise.fbm(x / 9, z / 9, 41, 2) + into * 0.25;
        // Neben dem Fall eine senkrechte Wand (hält den oberen Bach), weiter außen eine zerklüftete Felsflanke mit
        // Stufen, Vorsprüngen und Absätzen statt einer glatten Mauer
        double crag = Noise.fbm(x / 2.5, z / 2.5, 47, 2);
        double tier = 64 + 2.4 * into + 7 * (crag - 0.3) + 3 * Noise.fbm(z / 5.0, x / 5.0, 49, 2);
        double open = Noise.smoothstep(4.5, 7.5, ad);
        double target = Math.min(full, full * (1 - open) + tier * open);
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
        if ((x == Layout.FALL_X || x == Layout.FALL_X + 1) && z >= Layout.FALL_Z1 && z <= Layout.FALL_Z2) {
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
