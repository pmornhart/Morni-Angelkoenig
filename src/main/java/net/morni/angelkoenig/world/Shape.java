package net.morni.angelkoenig.world;

import java.util.List;

/**
 * Flächen im Gelände (waagrecht). {@link #dist} ist ungefähr der Abstand zum Rand in Blöcken: negativ innen, positiv
 * außen.
 */
public interface Shape {

    double dist(double x, double z);

    default boolean contains(double x, double z) {
        return dist(x, z) < 0;
    }

    /** Begrenzungsrechteck (minX, minZ, maxX, maxZ) – für schnelles Aussortieren. */
    double[] box();

    /** Ellipse mit leicht welligem Rand (Seeufer sehen so natürlicher aus). */
    record Ellipse(double cx, double cz, double rx, double rz, double wobble, int seed) implements Shape {

        /** Radius-Faktor in Richtung {@code angle} (1 ± wobble). */
        double radiusFactor(double angle) {
            if (wobble == 0) {
                return 1;
            }
            double p1 = Noise.hash(seed, 1, 7) * Math.PI * 2;
            double p2 = Noise.hash(seed, 2, 7) * Math.PI * 2;
            double p3 = Noise.hash(seed, 3, 7) * Math.PI * 2;
            return 1 + wobble * (0.55 * Math.sin(3 * angle + p1) + 0.3 * Math.sin(5 * angle + p2)
                    + 0.15 * Math.sin(8 * angle + p3));
        }

        /** 0 in der Mitte, 1 am Rand. */
        double norm(double x, double z) {
            double dx = (x - cx) / rx;
            double dz = (z - cz) / rz;
            return Math.sqrt(dx * dx + dz * dz) / radiusFactor(Math.atan2(dz, dx));
        }

        @Override
        public double dist(double x, double z) {
            return (norm(x, z) - 1) * Math.min(rx, rz);
        }

        @Override
        public double[] box() {
            double f = 1 + wobble;
            return new double[] {cx - rx * f, cz - rz * f, cx + rx * f, cz + rz * f};
        }
    }

    /** Rechteck (Ecken eingeschlossen). */
    record Rect(double x1, double z1, double x2, double z2) implements Shape {

        @Override
        public double dist(double x, double z) {
            double dx = Math.max(x1 - x, x - x2);
            double dz = Math.max(z1 - z, z - z2);
            if (dx <= 0 && dz <= 0) {
                return Math.max(dx, dz);
            }
            return Math.hypot(Math.max(0, dx), Math.max(0, dz));
        }

        @Override
        public double[] box() {
            return new double[] {x1, z1, x2, z2};
        }
    }

    /** Linienzug mit Breite (Bach, Weg). Punkte als {x, z} oder {x, z, Höhe}. */
    record Line(List<double[]> points, double halfWidth) implements Shape {

        /** Abstand zur Mittellinie und Anteil entlang des Zugs (0 … 1) für die Höhe. */
        double[] nearest(double x, double z) {
            double total = 0;
            double[] lengths = new double[points.size()];
            for (int i = 1; i < points.size(); i++) {
                double[] a = points.get(i - 1);
                double[] b = points.get(i);
                total += Math.hypot(b[0] - a[0], b[1] - a[1]);
                lengths[i] = total;
            }
            double best = Double.MAX_VALUE;
            double along = 0;
            int segment = 1;
            double segT = 0;
            for (int i = 1; i < points.size(); i++) {
                double[] a = points.get(i - 1);
                double[] b = points.get(i);
                double vx = b[0] - a[0];
                double vz = b[1] - a[1];
                double len2 = vx * vx + vz * vz;
                double t = len2 == 0 ? 0 : ((x - a[0]) * vx + (z - a[1]) * vz) / len2;
                t = Math.max(0, Math.min(1, t));
                double d = Math.hypot(x - (a[0] + t * vx), z - (a[1] + t * vz));
                if (d < best) {
                    best = d;
                    along = total == 0 ? 0 : (lengths[i - 1] + t * Math.sqrt(len2)) / total;
                    segment = i;
                    segT = t;
                }
            }
            return new double[] {best, along, segment, segT};
        }

        /** Höhe auf der Mittellinie (dritter Wert der Punkte), zwischen den Punkten gerade verbunden. */
        double heightAt(double x, double z) {
            double[] n = nearest(x, z);
            int i = (int) n[2];
            double[] a = points.get(i - 1);
            double[] b = points.get(i);
            if (a.length < 3 || b.length < 3) {
                return 0;
            }
            return a[2] + (b[2] - a[2]) * n[3];
        }

        @Override
        public double dist(double x, double z) {
            return nearest(x, z)[0] - halfWidth;
        }

        @Override
        public double[] box() {
            double minX = Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;
            for (double[] p : points) {
                minX = Math.min(minX, p[0]);
                minZ = Math.min(minZ, p[1]);
                maxX = Math.max(maxX, p[0]);
                maxZ = Math.max(maxZ, p[1]);
            }
            return new double[] {minX - halfWidth, minZ - halfWidth, maxX + halfWidth, maxZ + halfWidth};
        }
    }

    /** Vereinigung mehrerer Flächen. */
    record Union(List<Shape> parts) implements Shape {

        @Override
        public double dist(double x, double z) {
            double best = Double.MAX_VALUE;
            for (Shape s : parts) {
                best = Math.min(best, s.dist(x, z));
            }
            return best;
        }

        @Override
        public double[] box() {
            double[] out = {Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
            for (Shape s : parts) {
                double[] b = s.box();
                out[0] = Math.min(out[0], b[0]);
                out[1] = Math.min(out[1], b[1]);
                out[2] = Math.max(out[2], b[2]);
                out[3] = Math.max(out[3], b[3]);
            }
            return out;
        }
    }

    /** Nur der Teil westlich von {@code maxX} (z. B. Bach oberhalb des Wasserfalls). */
    record ClipWest(Shape inner, double maxX) implements Shape {

        @Override
        public double dist(double x, double z) {
            return Math.max(inner.dist(x, z), x - maxX);
        }

        @Override
        public double[] box() {
            double[] b = inner.box();
            return new double[] {b[0], b[1], Math.min(b[2], maxX), b[3]};
        }
    }
}
