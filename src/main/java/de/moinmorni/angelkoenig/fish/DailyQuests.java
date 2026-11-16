package de.moinmorni.angelkoenig.fish;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;

/**
 * Drei Aufgaben je Tag – für alle gleich (aus dem Datum gewürfelt): eine leichte, eine mittlere, eine schwere.
 */
public final class DailyQuests {

    public static final int EASY_REWARD = 40;
    public static final int MEDIUM_REWARD = 90;
    public static final int HARD_REWARD = 180;
    private static final long SALT = 0x5DEECE66DL;

    private DailyQuests() {
    }

    public static List<Quest> forDay(LocalDate day) {
        SplittableRandom r = new SplittableRandom(day.toEpochDay() * 0x9E3779B97F4A7C15L ^ SALT);
        List<Quest> out = new ArrayList<>(3);
        Spot easySpot = pick(r, Spot.values());
        out.add(switch (r.nextInt(3)) {
            case 0 -> new Quest(Quest.Kind.TOTAL, "", 8 + r.nextInt(5), EASY_REWARD);
            case 1 -> new Quest(Quest.Kind.SPOT, easySpot.id(), 5 + r.nextInt(2), EASY_REWARD);
            default -> new Quest(Quest.Kind.SPECIES, pick(r, of(Rarity.COMMON)).id(), 3, EASY_REWARD);
        });
        Quest easy = out.get(0);
        Quest medium;
        do {
            medium = switch (r.nextInt(4)) {
                case 0 -> new Quest(Quest.Kind.SPECIES, pick(r, of(Rarity.UNCOMMON)).id(), 3, MEDIUM_REWARD);
                case 1 -> new Quest(Quest.Kind.SELL, "", 150 + 10 * r.nextInt(11), MEDIUM_REWARD);
                case 2 -> new Quest(Quest.Kind.BAIT, "", 6, MEDIUM_REWARD);
                default -> new Quest(Quest.Kind.SPOT, pick(r, Spot.values()).id(), 8, MEDIUM_REWARD);
            };
        } while (medium.kind() == easy.kind() && medium.target().equals(easy.target()));
        out.add(medium);
        out.add(switch (r.nextInt(3)) {
            case 0 -> new Quest(Quest.Kind.SPECIES, pick(r, of(Rarity.RARE)).id(), 2, HARD_REWARD);
            case 1 -> new Quest(Quest.Kind.RARITY, Rarity.RARE.name().toLowerCase(Locale.ROOT), 3, HARD_REWARD);
            default -> new Quest(Quest.Kind.HEAVY, String.valueOf(1500 + 250 * r.nextInt(7)), 1, HARD_REWARD);
        });
        return List.copyOf(out);
    }

    private static Species[] of(Rarity rarity) {
        return Species.fishes().stream().filter(s -> s.rarity() == rarity).toArray(Species[]::new);
    }

    private static <T> T pick(SplittableRandom r, T[] options) {
        return options[r.nextInt(options.length)];
    }
}
