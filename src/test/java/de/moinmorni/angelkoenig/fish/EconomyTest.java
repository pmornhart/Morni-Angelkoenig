package de.moinmorni.angelkoenig.fish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** Preise, Angeln, Köder, Kleidung, Zahlen und Zeiten. */
class EconomyTest {

    @Test
    void biggerFishPayMore() {
        Catch small = CatchRules.sized(Species.ZANDER, 35, 1.0);
        Catch mid = CatchRules.sized(Species.ZANDER, Species.ZANDER.meanCm(), 1.0);
        Catch big = CatchRules.sized(Species.ZANDER, 90, 1.0);
        assertEquals(Species.ZANDER.basePrice(), mid.price(), 1);
        assertTrue(small.price() < mid.price());
        assertTrue(big.price() > mid.price());
        assertTrue(big.price() <= Species.ZANDER.basePrice() * Prices.MAX_FACTOR);
        assertTrue(small.price() >= Species.ZANDER.basePrice() * Prices.MIN_FACTOR - 1);
        assertEquals(Species.STIEFEL.basePrice(), Prices.sale(Species.STIEFEL, 999), "Fundstücke: fester Preis");
    }

    @Test
    void rarerFishAreWorthMore() {
        for (Rarity r : Rarity.values()) {
            if (r == Rarity.JUNK || r == Rarity.LEGENDARY) {
                continue;
            }
            int maxHere = Arrays.stream(Species.values()).filter(s -> s.rarity() == r).mapToInt(Species::basePrice).max()
                    .orElseThrow();
            Rarity next = Rarity.values()[r.ordinal() + 1];
            int minNext = Arrays.stream(Species.values()).filter(s -> s.rarity() == next).mapToInt(Species::basePrice)
                    .min().orElseThrow();
            assertTrue(maxHere < minNext, r + " < " + next);
        }
    }

    /** Ungefähr: wie lange man für eine Angel fischen muss (mittlerer Wert je Fang mit der vorigen Angel). */
    @Test
    void rodsCostAFewSessions() {
        SplittableRandom r = new SplittableRandom(3);
        double sum = 0;
        int n = 20_000;
        for (int i = 0; i < n; i++) {
            Spot s = Spot.values()[i % 4];
            DayTime t = DayTime.values()[(i / 4) % 4];
            sum += CatchRules.roll(new CatchRules.Conditions(s, t, Weather.KLAR, Rod.HASEL, null), r).price();
        }
        double perCatch = sum / n;
        assertTrue(perCatch > 8 && perCatch < 40, "mittlerer Wert je Fang: " + perCatch);
        double catchesForAsh = Rod.ESCHE.price() / perCatch;
        assertTrue(catchesForAsh > 8 && catchesForAsh < 40, "Fänge bis zur Eschenrute: " + catchesForAsh);
        assertTrue(Rod.KOENIG.price() / perCatch > 150, "Königsrute ist ein großes Ziel");
        for (int i = 1; i < Rod.values().length; i++) {
            assertTrue(Rod.values()[i].price() > Rod.values()[i - 1].price());
            assertTrue(Rod.values()[i].luck() > Rod.values()[i - 1].luck());
            assertTrue(Rod.values()[i].waitFactor() < Rod.values()[i - 1].waitFactor());
        }
    }

    @Test
    void shopThingsHaveSensiblePrices() {
        for (Bait b : Bait.values()) {
            assertTrue(b.price() > 0 && b.pack() > 0, b.name());
        }
        assertFalse(Cosmetic.KOENIGSKRONE.forSale(), "Krone nur für Turniersieger");
        for (Cosmetic c : Cosmetic.values()) {
            if (c != Cosmetic.KOENIGSKRONE) {
                assertTrue(c.forSale() && c.colour() >= 0, c.name());
            }
        }
        assertEquals(0, Rod.HASEL.price(), "Haselrute hat jeder");
    }

    @Test
    void unitsReadNicely() {
        assertEquals("850 g", Units.weight(850, "de"));
        assertEquals("1,23 kg", Units.weight(1234, "de"));
        assertEquals("1.23 kg", Units.weight(1234, "en"));
        assertEquals("61,6 kg", Units.weight(61_600, "de"));
        assertEquals("42,3 cm", Units.cm(423, "de"));
        assertEquals("12.345", Units.number(12_345, "de"));
        assertEquals("12,345", Units.number(12_345, "en"));
    }

    @Test
    void dayTimesFollowTheSun() {
        assertEquals(DayTime.MORGEN, DayTime.of(0));
        assertEquals(DayTime.MORGEN, DayTime.of(23500));
        assertEquals(DayTime.TAG, DayTime.of(6000));
        assertEquals(DayTime.ABEND, DayTime.of(12000));
        assertEquals(DayTime.NACHT, DayTime.of(18000));
        assertEquals(DayTime.MORGEN, DayTime.of(24000 + 100));
        for (DayTime d : DayTime.values()) {
            assertEquals(d, DayTime.of(d.sampleTicks()), d.name());
        }
        assertEquals("6:00", DayTime.clock(0));
        assertEquals("12:00", DayTime.clock(6000));
        assertEquals("0:00", DayTime.clock(18000));
        assertEquals("7:30", DayTime.clock(1500));
    }

    @Test
    void weatherChangesSensibly() {
        SplittableRandom r = new SplittableRandom(1);
        int[] seen = new int[3];
        Weather now = Weather.KLAR;
        for (int i = 0; i < 10_000; i++) {
            Weather.Spell s = Weather.next(now, r, 300, 900);
            if (now != Weather.KLAR) {
                assertEquals(Weather.KLAR, s.weather(), "nach Regen kommt klar");
            }
            assertTrue(s.seconds() >= 150 && s.seconds() <= 900);
            seen[s.weather().ordinal()]++;
            now = s.weather();
        }
        assertTrue(seen[Weather.REGEN.ordinal()] > seen[Weather.GEWITTER.ordinal()]);
        assertTrue(seen[Weather.GEWITTER.ordinal()] > 0);
        assertEquals(Weather.GEWITTER, Weather.of(true, true));
        assertEquals(Weather.KLAR, Weather.of(false, true));
    }
}
