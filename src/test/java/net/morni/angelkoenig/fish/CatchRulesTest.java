package net.morni.angelkoenig.fish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class CatchRulesTest {

    private static CatchRules.Conditions at(Spot s, DayTime t, Weather w, Rod r, Bait b) {
        return new CatchRules.Conditions(s, t, w, r, b);
    }

    /** An jedem Platz und zu jeder Zeit beißt irgendwas – und jede echte Art lebt irgendwo. */
    @Test
    void somethingAlwaysBites() {
        for (Spot s : Spot.values()) {
            for (DayTime t : DayTime.values()) {
                for (Weather w : Weather.values()) {
                    Map<Species, Double> weights = CatchRules.weights(at(s, t, w, Rod.HASEL, null));
                    assertTrue(weights.keySet().stream().anyMatch(sp -> !sp.junk()), s + " " + t + " " + w);
                }
            }
        }
        for (Species sp : Species.values()) {
            assertFalse(sp.spots().isEmpty(), sp + " lebt nirgends");
        }
        assertEquals(java.util.EnumSet.allOf(Spot.class), Species.spotsWithFish());
    }

    @Test
    void eachSpotHasItsOwnFish() {
        assertEquals(0, CatchRules.weights(at(Spot.EIS, DayTime.TAG, Weather.KLAR, Rod.HASEL, null))
                .getOrDefault(Species.BACHFORELLE, 0.0));
        assertTrue(CatchRules.weights(at(Spot.BACH, DayTime.TAG, Weather.KLAR, Rod.HASEL, null))
                .containsKey(Species.BACHFORELLE));
        assertTrue(CatchRules.weights(at(Spot.MOOR, DayTime.TAG, Weather.KLAR, Rod.HASEL, null))
                .containsKey(Species.KARAUSCHE));
        assertFalse(CatchRules.weights(at(Spot.BERGSEE, DayTime.TAG, Weather.KLAR, Rod.HASEL, null))
                .containsKey(Species.KARAUSCHE));
    }

    @Test
    void eelsOnlyBiteInTheDark() {
        assertFalse(CatchRules.weights(at(Spot.MOOR, DayTime.TAG, Weather.KLAR, Rod.HASEL, null)).containsKey(Species.AAL));
        assertFalse(CatchRules.weights(at(Spot.MOOR, DayTime.MORGEN, Weather.KLAR, Rod.HASEL, null))
                .containsKey(Species.AAL));
        double night = CatchRules.weights(at(Spot.MOOR, DayTime.NACHT, Weather.KLAR, Rod.HASEL, null)).get(Species.AAL);
        double evening = CatchRules.weights(at(Spot.MOOR, DayTime.ABEND, Weather.KLAR, Rod.HASEL, null)).get(Species.AAL);
        assertTrue(night > evening);
    }

    @Test
    void stormsBringOutTheGiants() {
        double clear = CatchRules.weights(at(Spot.MOOR, DayTime.NACHT, Weather.KLAR, Rod.HASEL, null))
                .get(Species.MOORKOENIG);
        double storm = CatchRules.weights(at(Spot.MOOR, DayTime.NACHT, Weather.GEWITTER, Rod.HASEL, null))
                .get(Species.MOORKOENIG);
        assertEquals(3.0, storm / clear, 1e-9);
    }

    @Test
    void betterRodsMeanRarerFishAndLessJunk() {
        Map<Species, Double> basic = CatchRules.weights(at(Spot.BERGSEE, DayTime.TAG, Weather.KLAR, Rod.HASEL, null));
        Map<Species, Double> king = CatchRules.weights(at(Spot.BERGSEE, DayTime.TAG, Weather.KLAR, Rod.KOENIG, null));
        assertEquals(basic.get(Species.ROTAUGE), king.get(Species.ROTAUGE), 1e-9, "Gewöhnliche bleiben gleich");
        assertTrue(king.get(Species.SEESAIBLING) > basic.get(Species.SEESAIBLING));
        assertTrue(king.get(Species.SEEKOENIG) / basic.get(Species.SEEKOENIG)
                > king.get(Species.SEESAIBLING) / basic.get(Species.SEESAIBLING), "Legenden profitieren am meisten");
        assertTrue(king.get(Species.STIEFEL) < basic.get(Species.STIEFEL));
        assertTrue(Rod.KOENIG.waitTicks(400) < Rod.HASEL.waitTicks(400));
        assertEquals(20, Rod.KOENIG.waitTicks(1), "mindestens eine Sekunde");
    }

    @Test
    void baitLuresItsTargets() {
        Map<Species, Double> none = CatchRules.weights(at(Spot.BERGSEE, DayTime.TAG, Weather.KLAR, Rod.HASEL, null));
        Map<Species, Double> fishBait = CatchRules.weights(at(Spot.BERGSEE, DayTime.TAG, Weather.KLAR, Rod.HASEL,
                Bait.KOEDERFISCH));
        assertEquals(Bait.TARGET_FACTOR, fishBait.get(Species.HECHT) / none.get(Species.HECHT), 1e-9);
        assertEquals(1.0, fishBait.get(Species.ROTAUGE) / none.get(Species.ROTAUGE), 1e-9);
        assertEquals(Bait.JUNK_FACTOR, fishBait.get(Species.TREIBHOLZ) / none.get(Species.TREIBHOLZ), 1e-9);
        Map<Species, Double> gold = CatchRules.weights(at(Spot.BERGSEE, DayTime.TAG, Weather.KLAR, Rod.HASEL,
                Bait.GOLDSPINNER));
        assertEquals(Bait.GOLD_LEGENDARY, gold.get(Species.SEEKOENIG) / none.get(Species.SEEKOENIG), 1e-9);
        assertEquals(Bait.GOLD_EPIC, gold.get(Species.WALLER) / none.get(Species.WALLER), 1e-9);
        for (Bait b : Bait.values()) {
            for (Species s : b.targets()) {
                assertFalse(s.junk(), b + " lockt Krempel");
            }
        }
    }

    /** Viele Würfe: Häufigkeiten passen ungefähr zu den Gewichten, Legenden sind selten. */
    @Test
    void rollsFollowTheWeights() {
        SplittableRandom r = new SplittableRandom(42);
        CatchRules.Conditions c = at(Spot.BACH, DayTime.MORGEN, Weather.KLAR, Rod.HASEL, null);
        Map<Species, Integer> count = new EnumMap<>(Species.class);
        int n = 200_000;
        for (int i = 0; i < n; i++) {
            count.merge(CatchRules.pick(c, r), 1, Integer::sum);
        }
        for (var e : CatchRules.chances(c)) {
            double seen = count.getOrDefault(e.getKey(), 0) / (double) n;
            assertEquals(e.getValue(), seen, 0.01, e.getKey().name());
        }
        double gold = count.getOrDefault(Species.GOLDFORELLE, 0) / (double) n;
        assertTrue(gold > 0.002 && gold < 0.02, "Goldforelle selten, aber möglich: " + gold);
    }

    @Test
    void sizesStayInRangeAndWeightGrowsWithLength() {
        SplittableRandom r = new SplittableRandom(7);
        for (Species s : Species.values()) {
            for (int i = 0; i < 2000; i++) {
                Catch c = CatchRules.measure(s, 3, r);
                assertTrue(c.grams() > 0, s.name());
                if (s.junk()) {
                    assertEquals(0, c.lengthMm());
                    assertTrue(c.grams() >= s.junkMinGrams() && c.grams() <= s.junkMaxGrams());
                } else {
                    assertTrue(c.lengthMm() >= Math.round(s.minCm() * 10) && c.lengthMm() <= Math.round(s.maxCm() * 10),
                            s + " " + c.lengthMm());
                }
            }
        }
        Catch small = CatchRules.sized(Species.HECHT, 40, 1.0);
        Catch big = CatchRules.sized(Species.HECHT, 110, 1.0);
        assertTrue(big.grams() > small.grams() * 10);
        assertTrue(big.trophy(), "über Mittel + 2 Streuungen = Prachtexemplar");
        assertFalse(small.trophy());
        Catch giant = CatchRules.sized(Species.SEEKOENIG, 999, 1.0);
        assertEquals(2700, giant.lengthMm(), "auf die größte Länge begrenzt");
    }

    @Test
    void typicalWeightsAreBelievable() {
        assertEquals(297, Math.round(Species.BACHFORELLE.averageGrams()), 5);
        assertTrue(Species.WALLER.averageGrams() > 10_000);
        assertTrue(Species.STEINKREBS.averageGrams() < 50);
        assertTrue(Species.SEEKOENIG.averageGrams() > 50_000);
    }
}
