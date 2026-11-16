package net.morni.angelkoenig.world;

/**
 * Eigenes, festes Rauschen für das Gelände (keine Zufallszahlen beim Start: die Welt sieht immer gleich aus). Werte
 * zwischen 0 und 1.
 */
public final class Noise {

    private static final long M1 = 0x9E3779B97F4A7C15L;
    private static final long M2 = 0xC2B2AE3D27D4EB4FL;
    private static final double TO_UNIT = 1.0 / (1L << 53);

    private Noise() {
    }

    /** Fester Zufallswert 0 … 1 für eine ganze Stelle. */
    public static double hash(int x, int z, int seed) {
        long h = x * M1 + z * M2 + seed * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * TO_UNIT;
    }

    /** Ganzzahliger Zufallswert 0 … bound−1 für eine Stelle. */
    public static int pick(int x, int z, int seed, int bound) {
        return (int) Math.min(bound - 1, Math.floor(hash(x, z, seed) * bound));
    }

    private static double fade(double t) {
        return t * t * (3 - 2 * t);
    }

    /** Weiches Rauschen (Werte an Gitterpunkten, dazwischen geglättet). */
    public static double value(double x, double z, int seed) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double fx = fade(x - x0);
        double fz = fade(z - z0);
        double a = hash(x0, z0, seed);
        double b = hash(x0 + 1, z0, seed);
        double c = hash(x0, z0 + 1, seed);
        double d = hash(x0 + 1, z0 + 1, seed);
        double top = a + (b - a) * fx;
        double bottom = c + (d - c) * fx;
        return top + (bottom - top) * fz;
    }

    /** Mehrere Lagen übereinander (grob + fein), 0 … 1. */
    public static double fbm(double x, double z, int seed, int octaves) {
        double sum = 0;
        double amp = 1;
        double norm = 0;
        double f = 1;
        for (int i = 0; i < octaves; i++) {
            sum += amp * value(x * f, z * f, seed + i * 101);
            norm += amp;
            amp *= 0.5;
            f *= 2.03;
        }
        return sum / norm;
    }

    /** Grate: scharfe Kämme statt runder Hügel, 0 … 1. */
    public static double ridge(double x, double z, int seed, int octaves) {
        double sum = 0;
        double amp = 1;
        double norm = 0;
        double f = 1;
        for (int i = 0; i < octaves; i++) {
            double v = 1 - Math.abs(2 * value(x * f, z * f, seed + i * 211) - 1);
            sum += amp * v * v;
            norm += amp;
            amp *= 0.5;
            f *= 2.11;
        }
        return sum / norm;
    }

    public static double smoothstep(double edge0, double edge1, double v) {
        double t = Math.max(0, Math.min(1, (v - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }
}
