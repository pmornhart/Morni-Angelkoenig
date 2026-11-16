package net.morni.angelkoenig.fish;

import java.util.Locale;
import java.util.Optional;

/** Tageszeit im Spiel (aus den Minecraft-Ticks eines Tages, 0 … 23 999). */
public enum DayTime {

    MORGEN("morgen", 0L),
    TAG("tag", 6000L),
    ABEND("abend", 12000L),
    NACHT("nacht", 18000L);

    /** Morgen beginnt kurz vor Sonnenaufgang (5 Uhr). */
    static final long MORNING_FROM = 23000L;
    /** Tag ab 9 Uhr. */
    static final long DAY_FROM = 3000L;
    /** Abend ab 17 Uhr. */
    static final long EVENING_FROM = 11000L;
    /** Nacht ab 19:30 Uhr. */
    static final long NIGHT_FROM = 13500L;
    private static final long DAY_TICKS = 24000L;

    private final String id;
    private final long sample;

    DayTime(String id, long sample) {
        this.id = id;
        this.sample = sample;
    }

    public String id() {
        return id;
    }

    /** Eine typische Uhrzeit dieses Abschnitts (für /angeladmin zeit). */
    public long sampleTicks() {
        return sample;
    }

    public static DayTime of(long ticks) {
        long t = Math.floorMod(ticks, DAY_TICKS);
        if (t >= MORNING_FROM || t < DAY_FROM) {
            return MORGEN;
        }
        if (t < EVENING_FROM) {
            return TAG;
        }
        if (t < NIGHT_FROM) {
            return ABEND;
        }
        return NACHT;
    }

    /** Minecraft-Ticks als Uhrzeit „7:30“ (Tick 0 = 6 Uhr). */
    public static String clock(long ticks) {
        long t = Math.floorMod(ticks + 6000L, DAY_TICKS);
        long hours = t / 1000L;
        long minutes = (t % 1000L) * 60L / 1000L;
        return String.format(Locale.ROOT, "%d:%02d", hours, minutes);
    }

    public static Optional<DayTime> byId(String id) {
        for (DayTime d : values()) {
            if (d.id.equalsIgnoreCase(id)) {
                return Optional.of(d);
            }
        }
        return Optional.empty();
    }
}
