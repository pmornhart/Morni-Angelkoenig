package de.moinmorni.angelkoenig.fish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TournamentTest {

    private static final long MIN = 60_000L;
    private static final UUID ANNA = UUID.randomUUID();
    private static final UUID BERT = UUID.randomUUID();

    private static Tournament make(long now) {
        return new Tournament(true, 15 * MIN, 5 * MIN, MIN, 2, now);
    }

    @Test
    void startsAtTheQuarterHourAfterAWarning() {
        long now = 3 * MIN + 1234;
        Tournament t = make(now);
        assertEquals(15 * MIN, t.nextStart(), "volle Viertelstunde");
        assertTrue(t.tick(13 * MIN, 3).isEmpty());
        List<Tournament.Event> e = t.tick(14 * MIN, 3);
        assertInstanceOf(Tournament.Announce.class, e.get(0));
        assertEquals(15 * MIN, ((Tournament.Announce) e.get(0)).startsAt());
        assertTrue(t.tick(14 * MIN + 1000, 3).isEmpty(), "nur einmal angekündigt");
        e = t.tick(15 * MIN, 3);
        assertInstanceOf(Tournament.Start.class, e.get(0));
        assertTrue(t.running());
        assertEquals(20 * MIN, t.endsAt());
    }

    @Test
    void heaviestFishWinsEarlierCatchBreaksTies() {
        Tournament t = make(0);
        t.start(0);
        assertTrue(t.record(ANNA, "Anna", CatchRules.sized(Species.HECHT, 60, 1), 1000));
        assertTrue(t.record(BERT, "Bert", CatchRules.sized(Species.HECHT, 60, 1), 2000));
        assertFalse(t.record(ANNA, "Anna", CatchRules.sized(Species.ROTAUGE, 20, 1), 3000), "kleinerer zählt nicht");
        assertFalse(t.record(BERT, "Bert", CatchRules.sized(Species.STIEFEL, 0, 1), 3000), "Krempel zählt nicht");
        assertEquals("Anna", t.ranking().get(0).name(), "gleich schwer: Anna war früher");
        assertTrue(t.record(BERT, "Bert", CatchRules.sized(Species.WALLER, 120, 1), 4000));
        assertEquals(1, t.place(BERT));
        assertEquals(2, t.place(ANNA));
        List<Tournament.Event> e = t.tick(5 * MIN, 2);
        Tournament.End end = assertInstanceOf(Tournament.End.class, e.get(0));
        assertEquals("Bert", end.ranking().get(0).name());
        assertFalse(t.running());
        assertEquals(15 * MIN, t.nextStart());
        assertTrue(t.ranking().isEmpty(), "danach leer");
    }

    @Test
    void tooFewAnglersSkipTheContest() {
        Tournament t = make(0);
        assertTrue(t.tick(14 * MIN, 1).isEmpty(), "ohne genug Leute keine Ankündigung");
        List<Tournament.Event> e = t.tick(15 * MIN, 1);
        assertInstanceOf(Tournament.Skip.class, e.get(0));
        assertEquals(30 * MIN, t.nextStart());
        assertFalse(t.running());
    }

    @Test
    void catchesOutsideTheContestDoNotCount() {
        Tournament t = make(0);
        assertFalse(t.record(ANNA, "Anna", CatchRules.sized(Species.HECHT, 60, 1), 1000));
        assertTrue(t.ranking().isEmpty());
    }

    @Test
    void canBeSwitchedOffAndControlledByHand() {
        Tournament t = new Tournament(false, 15 * MIN, 5 * MIN, MIN, 2, 0);
        assertTrue(t.tick(15 * MIN, 10).isEmpty(), "aus = nie von selbst");
        t.start(16 * MIN);
        assertTrue(t.running());
        t.record(ANNA, "Anna", CatchRules.sized(Species.HECHT, 60, 1), 16 * MIN + 1);
        List<Tournament.Entry> r = t.end(17 * MIN);
        assertEquals(1, r.size());
        assertFalse(t.running());
        t.start(18 * MIN);
        t.abort(18 * MIN + 1);
        assertFalse(t.running());
        assertTrue(t.end(19 * MIN).isEmpty());
    }

    /** Zurück ins Spiel: Wer im Turnier die Verbindung verliert, behält seinen Platz eine Weile. */
    @Test
    void placeIsKeptForAWhileAfterTheConnectionDrops() {
        Tournament t = make(0);
        assertFalse(t.away(ANNA, 0, 90_000), "ohne Turnier wird nichts freigehalten");
        t.start(0);
        t.record(ANNA, "Anna", CatchRules.sized(Species.HECHT, 80, 1), 1000);
        t.record(BERT, "Bert", CatchRules.sized(Species.ROTAUGE, 20, 1), 1000);
        assertTrue(t.away(ANNA, 10_000, 20_000));
        assertTrue(t.waitingFor(ANNA));
        assertEquals(1, t.place(ANNA), "der Platz zählt, solange gewartet wird");
        assertEquals(List.of(), t.expired(30_000), "genau zur Frist zählt noch");
        assertEquals(Tournament.Return.IN_TIME, t.back(ANNA, 30_000));
        assertFalse(t.waitingFor(ANNA));
        assertEquals(1, t.place(ANNA), "rechtzeitig zurück: Platz und Fang bleiben");
        assertEquals(Tournament.Return.NONE, t.back(ANNA, 31_000), "niemand wartet mehr");
    }

    @Test
    void tooLateFreesThePlace() {
        Tournament t = make(0);
        t.start(0);
        t.record(ANNA, "Anna", CatchRules.sized(Species.HECHT, 80, 1), 1000);
        t.record(BERT, "Bert", CatchRules.sized(Species.ROTAUGE, 20, 1), 1000);
        t.away(ANNA, 10_000, 20_000);
        t.away(BERT, 12_000, 20_000);
        assertEquals(List.of(ANNA), t.expired(30_001));
        assertEquals(0, t.place(ANNA), "Platz frei");
        assertEquals(1, t.place(BERT), "Bert wartet noch – sein Platz zählt");
        assertEquals(Tournament.Return.TOO_LATE, t.back(BERT, 40_000), "kommt nach der Frist, bevor sie abgelaufen gemeldet wurde");
        assertTrue(t.ranking().isEmpty());
    }

    @Test
    void withoutGraceTimeLeavingGivesUpThePlace() {
        Tournament t = make(0);
        t.start(0);
        t.record(ANNA, "Anna", CatchRules.sized(Species.HECHT, 80, 1), 1000);
        assertFalse(t.away(ANNA, 2000, 0), "0 = aus");
        assertFalse(t.waitingFor(ANNA));
        assertEquals(0, t.place(ANNA));
    }

    @Test
    void endOfContestCountsHeldPlacesAndReleasesThem() {
        Tournament t = make(0);
        t.start(0);
        t.record(ANNA, "Anna", CatchRules.sized(Species.HECHT, 80, 1), 1000);
        t.away(ANNA, 2000, 90_000);
        List<Tournament.Entry> ranking = t.end(3000);
        assertEquals("Anna", ranking.get(0).name(), "wer gerade weg ist, zählt mit seinem Platz");
        assertEquals(List.of(ANNA), t.releaseAway());
        assertEquals(List.of(), t.releaseAway());
        assertEquals(Tournament.Return.NONE, t.back(ANNA, 4000));
    }
}
