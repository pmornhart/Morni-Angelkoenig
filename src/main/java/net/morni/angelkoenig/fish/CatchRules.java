package net.morni.angelkoenig.fish;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Was an den Haken geht: Gewichte je Art aus Fangplatz, Tageszeit, Wetter, Angel und Köder – und dann die Größe.
 * Ohne Server, damit es sich prüfen lässt.
 */
public final class CatchRules {

    /** Krempel wird mit jedem Glückspunkt der Angel um diesen Anteil seltener (höchstens bis {@link #JUNK_FLOOR}). */
    static final double JUNK_PER_LUCK = 0.22;
    static final double JUNK_FLOOR = 0.2;
    /** Jeder Glückspunkt macht Fische im Schnitt etwas größer. */
    static final double SIZE_PER_LUCK = 0.03;
    /** „Gut genährt“: Gewicht streut um ±10 % um den Wert aus der Länge. */
    static final double CONDITION_SPREAD = 0.10;
    static final double TROPHY_SIGMA = 2.0;

    private CatchRules() {
    }

    /** Bedingungen beim Anbiss. {@code bait} kann null sein. */
    public record Conditions(Spot spot, DayTime time, Weather weather, Rod rod, Bait bait) {
    }

    /** Gewicht jeder Art, die jetzt anbeißen kann (nur Arten mit Gewicht über 0). */
    public static Map<Species, Double> weights(Conditions c) {
        Map<Species, Double> out = new EnumMap<>(Species.class);
        int luck = c.rod().luck();
        for (Species s : Species.values()) {
            double w = s.weight(c.spot());
            if (w <= 0) {
                continue;
            }
            w *= s.timeFactor(c.time()) * s.weatherFactor(c.weather());
            if (s.junk()) {
                w *= Math.max(JUNK_FLOOR, 1 - JUNK_PER_LUCK * luck);
            } else {
                w *= 1 + luck * s.rarity().luckBonus();
            }
            if (c.bait() != null) {
                w *= c.bait().factor(s);
            }
            if (w > 0) {
                out.put(s, w);
            }
        }
        return out;
    }

    /** Wahrscheinlichkeit je Art (Summe 1), nach Wahrscheinlichkeit sortiert – für „Was beißt gerade?“. */
    public static List<Map.Entry<Species, Double>> chances(Conditions c) {
        Map<Species, Double> w = weights(c);
        double sum = w.values().stream().mapToDouble(Double::doubleValue).sum();
        List<Map.Entry<Species, Double>> out = new ArrayList<>();
        w.forEach((s, v) -> out.add(Map.entry(s, v / sum)));
        out.sort(Map.Entry.<Species, Double>comparingByValue(Comparator.reverseOrder()));
        return out;
    }

    /** Eine Art ziehen. */
    public static Species pick(Conditions c, RandomGenerator r) {
        Map<Species, Double> w = weights(c);
        double sum = w.values().stream().mapToDouble(Double::doubleValue).sum();
        double roll = r.nextDouble() * sum;
        Species last = null;
        for (Map.Entry<Species, Double> e : w.entrySet()) {
            last = e.getKey();
            roll -= e.getValue();
            if (roll < 0) {
                return last;
            }
        }
        return last == null ? Species.ROTAUGE : last;
    }

    /** Größe und Gewicht für eine Art (Angel-Glück macht Fische etwas größer). */
    public static Catch measure(Species s, int luck, RandomGenerator r) {
        if (s.junk()) {
            int g = s.junkMinGrams() + r.nextInt(Math.max(1, s.junkMaxGrams() - s.junkMinGrams() + 1));
            return new Catch(s, 0, g, false);
        }
        double mean = s.meanCm() * (1 + SIZE_PER_LUCK * luck);
        double cm = mean + r.nextGaussian() * s.sdCm();
        cm = Math.max(s.minCm(), Math.min(s.maxCm(), cm));
        return sized(s, cm, 1 + (r.nextDouble() * 2 - 1) * CONDITION_SPREAD);
    }

    /** Fang mit fester Länge (für Tests und /angeladmin naechster). */
    public static Catch sized(Species s, double cm, double condition) {
        if (s.junk()) {
            return new Catch(s, 0, (s.junkMinGrams() + s.junkMaxGrams()) / 2, false);
        }
        double len = Math.max(s.minCm(), Math.min(s.maxCm(), cm));
        int grams = Math.max(1, (int) Math.round(s.gramsAt(len) * condition));
        boolean trophy = len >= s.meanCm() + TROPHY_SIGMA * s.sdCm();
        return new Catch(s, (int) Math.round(len * 10), grams, trophy);
    }

    /** Ziehen und messen in einem. */
    public static Catch roll(Conditions c, RandomGenerator r) {
        return measure(pick(c, r), c.rod().luck(), r);
    }
}
