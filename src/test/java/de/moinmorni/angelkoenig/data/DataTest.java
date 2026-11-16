package de.moinmorni.angelkoenig.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.angelkoenig.fish.Bait;
import de.moinmorni.angelkoenig.fish.CatchRules;
import de.moinmorni.angelkoenig.fish.Cosmetic;
import de.moinmorni.angelkoenig.fish.DailyQuests;
import de.moinmorni.angelkoenig.fish.Quest;
import de.moinmorni.angelkoenig.fish.Rod;
import de.moinmorni.angelkoenig.fish.Species;
import de.moinmorni.angelkoenig.fish.Spot;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DataTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 16);

    @Test
    void fishBookCountsAndKeepsTheBiggest() {
        PlayerData d = new PlayerData(UUID.randomUUID(), "Anna");
        assertTrue(d.addCatch(CatchRules.sized(Species.HECHT, 50, 1), DAY), "erster Fang");
        assertFalse(d.addCatch(CatchRules.sized(Species.HECHT, 40, 1), DAY), "kleiner");
        assertTrue(d.addCatch(CatchRules.sized(Species.HECHT, 90, 1), DAY), "größer");
        d.addCatch(CatchRules.sized(Species.STIEFEL, 0, 1), DAY);
        d.addCatch(CatchRules.sized(Species.GOLDFORELLE, 45, 1), DAY);
        assertEquals(3, d.book(Species.HECHT).count());
        assertEquals(900, d.book(Species.HECHT).bestMm());
        assertEquals(4, d.catches(), "Stiefel zählt als Fundstück");
        assertEquals(1, d.finds());
        assertEquals(1, d.legendary());
        assertEquals(2, d.speciesCount(), "Arten ohne Fundstücke");
        assertEquals("hecht", d.heaviestSpecies());
    }

    @Test
    void moneyBaitRodsAndClothes() {
        PlayerData d = new PlayerData(UUID.randomUUID(), "Bert");
        assertFalse(d.spend(10));
        d.sold(300);
        assertEquals(300, d.earnedTotal());
        d.earn(25);
        assertEquals(325, d.earnedTotal(), "Belohnungen zählen auch als verdient");
        assertTrue(d.spend(25));
        assertTrue(d.spend(250));
        assertEquals(50, d.taler());
        assertFalse(d.useRod(Rod.ESCHE), "nicht gekauft");
        d.addRod(Rod.ESCHE);
        assertTrue(d.useRod(Rod.ESCHE));
        d.addBaits(Bait.MADEN, 2);
        d.bait(Bait.MADEN);
        assertEquals(Bait.MADEN, d.useBait());
        assertEquals(Bait.MADEN, d.useBait());
        assertNull(d.bait(), "leer = ausgehängt");
        assertNull(d.useBait());
        assertFalse(d.toggleWear(Cosmetic.STROHHUT), "nicht im Schrank");
        d.addCosmetic(Cosmetic.STROHHUT);
        assertTrue(d.toggleWear(Cosmetic.STROHHUT));
        assertEquals(Cosmetic.STROHHUT, d.worn(Cosmetic.Slot.HEAD));
        assertFalse(d.toggleWear(Cosmetic.STROHHUT));
        assertNull(d.worn(Cosmetic.Slot.HEAD));
        d.tournamentPlayed(true);
        assertTrue(d.owns(Cosmetic.KOENIGSKRONE), "Sieg bringt die Krone");
    }

    @Test
    void questsFinishOnceAndResetNextDay() {
        PlayerData d = new PlayerData(UUID.randomUUID(), "Carl");
        assertTrue(d.ensureQuests(DAY, DailyQuests.forDay(DAY)));
        assertFalse(d.ensureQuests(DAY, DailyQuests.forDay(DAY)));
        d.setQuest(0, Quest.parse("total::2:40").orElseThrow());
        Quest.Event e = Quest.Event.caught(CatchRules.sized(Species.ROTAUGE, 20, 1), Spot.BERGSEE, false);
        assertTrue(d.advance(e).isEmpty());
        assertEquals(List.of(0), d.advance(e));
        assertTrue(d.advance(e).isEmpty(), "nur einmal fertig");
        assertTrue(d.done(0));
        assertTrue(d.ensureQuests(DAY.plusDays(1), DailyQuests.forDay(DAY.plusDays(1))));
        assertFalse(d.done(0));
        assertEquals(0, d.progress(0));
    }

    @Test
    void savesAndLoadsEverything() {
        UUID u = UUID.randomUUID();
        PlayerData d = new PlayerData(u, "Dora");
        d.addCatch(CatchRules.sized(Species.WALLER, 150, 1), DAY);
        d.addCatch(CatchRules.sized(Species.TREIBHOLZ, 0, 1), DAY);
        d.sold(1234);
        d.addRod(Rod.MEISTER);
        d.useRod(Rod.MEISTER);
        d.addBaits(Bait.KOEDERFISCH, 7);
        d.bait(Bait.KOEDERFISCH);
        d.addCosmetic(Cosmetic.LEDERHOSE);
        d.toggleWear(Cosmetic.LEDERHOSE);
        d.tournamentPlayed(true);
        d.introSeen(true);
        d.ensureQuests(DAY, DailyQuests.forDay(DAY));
        d.setQuest(1, Quest.parse("spot:moor:5:90").orElseThrow());
        d.advance(Quest.Event.caught(CatchRules.sized(Species.HECHT, 60, 1), Spot.MOOR, true));
        PlayerData back = PlayerData.fromYaml(u, d.toYaml());
        assertEquals("Dora", back.name());
        assertEquals(1234, back.taler());
        assertEquals(1, back.catches());
        assertEquals(1, back.finds());
        assertEquals(d.heaviest(), back.heaviest());
        assertEquals(d.book(Species.WALLER), back.book(Species.WALLER));
        assertEquals(Rod.MEISTER, back.rod());
        assertEquals(7, back.baits(Bait.KOEDERFISCH));
        assertEquals(Bait.KOEDERFISCH, back.bait());
        assertEquals(Cosmetic.LEDERHOSE, back.worn(Cosmetic.Slot.LEGS));
        assertTrue(back.owns(Cosmetic.KOENIGSKRONE));
        assertEquals(1, back.tournamentsWon());
        assertTrue(back.introSeen());
        assertEquals(d.quests(), back.quests());
        assertEquals(1, back.progress(1));
        assertEquals(DAY.toString(), back.questDay());
    }

    @Test
    void recordsKeepTheHeaviestAndRankPeople() {
        Records r = new Records();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertTrue(r.offer(a, "Anna", CatchRules.sized(Species.HECHT, 60, 1), 1));
        assertFalse(r.offer(b, "Bert", CatchRules.sized(Species.HECHT, 50, 1), 2));
        assertTrue(r.offer(b, "Bert", CatchRules.sized(Species.HECHT, 80, 1), 3));
        assertEquals("Bert", r.best(Species.HECHT).name());
        r.totals(a, "Anna", 30, 1, 0);
        r.totals(b, "Bert", 12, 3, 1);
        r.totals(UUID.randomUUID(), "Neu", 0, 0, 0);
        assertEquals("Anna", r.topCatches(5).get(0).name());
        assertEquals("Bert", r.topWins(5).get(0).name());
        assertEquals(2, r.topCatches(5).size(), "ohne Fang nicht in der Liste");
        r.king(b, "Bert");
        Records back = Records.fromYaml(r.toYaml());
        assertEquals("Bert", back.kingName());
        assertEquals(b, back.king());
        assertEquals(r.best(Species.HECHT), back.best(Species.HECHT));
        assertEquals(30, back.topCatches(1).get(0).catches());
    }
}
