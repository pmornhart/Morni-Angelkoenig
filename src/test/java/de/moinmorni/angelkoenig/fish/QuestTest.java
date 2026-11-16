package de.moinmorni.angelkoenig.fish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QuestTest {

    @Test
    void sameDaySameTasksForEveryone() {
        LocalDate d = LocalDate.of(2026, 11, 16);
        assertEquals(DailyQuests.forDay(d), DailyQuests.forDay(d));
        Set<List<Quest>> different = new HashSet<>();
        for (int i = 0; i < 30; i++) {
            different.add(DailyQuests.forDay(d.plusDays(i)));
        }
        assertTrue(different.size() > 20, "jeden Tag andere Aufgaben");
    }

    /** Ein Jahr lang: drei Aufgaben, leicht → schwer, gültig, schaffbar, gespeichert und gelesen gleich. */
    @Test
    void tasksAreValidAndDoable() {
        LocalDate d = LocalDate.of(2026, 1, 1);
        for (int i = 0; i < 366; i++) {
            List<Quest> qs = DailyQuests.forDay(d.plusDays(i));
            assertEquals(3, qs.size());
            assertEquals(DailyQuests.EASY_REWARD, qs.get(0).reward());
            assertEquals(DailyQuests.MEDIUM_REWARD, qs.get(1).reward());
            assertEquals(DailyQuests.HARD_REWARD, qs.get(2).reward());
            assertNotEquals(qs.get(0), qs.get(1));
            for (Quest q : qs) {
                assertEquals(q, Quest.parse(q.spec()).orElseThrow(), q.spec());
                assertTrue(q.amount() > 0);
                if (q.kind() == Quest.Kind.SPECIES) {
                    Species s = Species.byId(q.target()).orElseThrow();
                    assertFalse(s.junk());
                    assertFalse(s.spots().isEmpty());
                    assertTrue(s.rarity().ordinal() <= Rarity.RARE.ordinal(), "keine Legende als Tagesaufgabe");
                }
            }
        }
    }

    @Test
    void progressCountsTheRightThings() {
        Catch saibling = CatchRules.sized(Species.SEESAIBLING, 35, 1.0);
        Catch boot = CatchRules.sized(Species.STIEFEL, 0, 1.0);
        Catch pike = CatchRules.sized(Species.HECHT, 100, 1.0);
        Quest species = Quest.parse("species:seesaibling:3:180").orElseThrow();
        Quest spot = Quest.parse("spot:eis:5:40").orElseThrow();
        Quest rare = Quest.parse("rarity:rare:3:180").orElseThrow();
        Quest heavy = Quest.parse("heavy:2000:1:180").orElseThrow();
        Quest sell = Quest.parse("sell::150:90").orElseThrow();
        Quest bait = Quest.parse("bait::6:90").orElseThrow();
        Quest total = Quest.parse("total::8:40").orElseThrow();
        assertEquals(1, species.advance(Quest.Event.caught(saibling, Spot.EIS, false)));
        assertEquals(0, species.advance(Quest.Event.caught(pike, Spot.EIS, false)));
        assertEquals(1, spot.advance(Quest.Event.caught(saibling, Spot.EIS, false)));
        assertEquals(0, spot.advance(Quest.Event.caught(saibling, Spot.BERGSEE, false)));
        assertEquals(1, rare.advance(Quest.Event.caught(pike, Spot.MOOR, false)));
        assertEquals(0, rare.advance(Quest.Event.caught(CatchRules.sized(Species.RENKE, 30, 1), Spot.EIS, false)));
        assertEquals(1, heavy.advance(Quest.Event.caught(pike, Spot.MOOR, false)), "Hecht 100 cm > 2 kg");
        assertEquals(0, heavy.advance(Quest.Event.caught(saibling, Spot.MOOR, false)));
        assertEquals(37, sell.advance(Quest.Event.sold(37)));
        assertEquals(0, sell.advance(Quest.Event.caught(pike, Spot.MOOR, true)));
        assertEquals(1, bait.advance(Quest.Event.caught(pike, Spot.MOOR, true)));
        assertEquals(0, bait.advance(Quest.Event.caught(pike, Spot.MOOR, false)));
        assertEquals(0, total.advance(Quest.Event.caught(boot, Spot.MOOR, false)), "Krempel zählt nicht");
        assertEquals(1, total.advance(Quest.Event.caught(saibling, Spot.MOOR, false)));
    }

    @Test
    void badSpecsAreRejected() {
        for (String bad : new String[] {"", "nix", "species:stiefel:1:1", "species:unbekannt:2:5", "spot:mond:2:2",
            "heavy:viel:1:1", "total::0:5", "rarity:junk:1:1", "total::2"}) {
            assertTrue(Quest.parse(bad).isEmpty(), bad);
        }
    }
}
