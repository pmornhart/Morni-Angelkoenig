package de.moinmorni.angelkoenig.fish;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Angel-Turnier: startet zu festen Zeiten (alle {@code interval}, z. B. zur vollen Viertelstunde), läuft
 * {@code duration} lang, eine Minute vorher wird es angekündigt. Gewertet wird der schwerste Fisch jeder Person – bei
 * Gleichstand zählt, wer ihn früher gefangen hat. Ohne Server (Zeit kommt von außen), damit es sich prüfen lässt.
 */
public final class Tournament {

    public enum Phase {
        IDLE, ANNOUNCED, RUNNING
    }

    /** Bester Fang einer Person im Turnier. */
    public record Entry(UUID uuid, String name, int grams, Species species, long at) {
    }

    /** Was beim Takt passiert ist. */
    public sealed interface Event permits Announce, Start, End, Skip {
    }

    public record Announce(long startsAt) implements Event {
    }

    public record Start(long endsAt) implements Event {
    }

    public record End(List<Entry> ranking) implements Event {
    }

    /** Zu wenige Leute da – dieses Turnier fällt aus. */
    public record Skip() implements Event {
    }

    private static final Comparator<Entry> ORDER = Comparator.comparingInt(Entry::grams).reversed()
            .thenComparingLong(Entry::at);

    private boolean enabled;
    private long interval;
    private long duration;
    private long warn;
    private int minPlayers;
    private Phase phase = Phase.IDLE;
    private long nextStart;
    private long endsAt;
    private final Map<UUID, Entry> best = new HashMap<>();

    public Tournament(boolean enabled, long intervalMs, long durationMs, long warnMs, int minPlayers, long now) {
        configure(enabled, intervalMs, durationMs, warnMs, minPlayers, now);
    }

    /** Neue Einstellungen (nach /angeladmin neuladen); ein laufendes Turnier läuft zu Ende. */
    public void configure(boolean enabled, long intervalMs, long durationMs, long warnMs, int minPlayers, long now) {
        this.enabled = enabled;
        this.interval = Math.max(60_000L, intervalMs);
        this.duration = Math.max(10_000L, durationMs);
        this.warn = Math.max(0, warnMs);
        this.minPlayers = Math.max(1, minPlayers);
        if (phase != Phase.RUNNING) {
            phase = Phase.IDLE;
            nextStart = align(now);
        }
    }

    /** Nächster fester Startzeitpunkt nach {@code now}. */
    long align(long now) {
        return (Math.floorDiv(now, interval) + 1) * interval;
    }

    public Phase phase() {
        return phase;
    }

    public boolean running() {
        return phase == Phase.RUNNING;
    }

    public boolean enabled() {
        return enabled;
    }

    public long nextStart() {
        return nextStart;
    }

    public long endsAt() {
        return endsAt;
    }

    public long duration() {
        return duration;
    }

    /** Takt (jede Sekunde): Ankündigen, Starten, Beenden oder Ausfallen lassen. */
    public List<Event> tick(long now, int players) {
        List<Event> out = new ArrayList<>();
        switch (phase) {
            case RUNNING -> {
                if (now >= endsAt) {
                    out.add(new End(finish(now)));
                }
            }
            case IDLE, ANNOUNCED -> {
                if (!enabled) {
                    phase = Phase.IDLE;
                    return out;
                }
                if (now >= nextStart) {
                    if (players >= minPlayers) {
                        out.add(new Start(begin(now)));
                    } else {
                        phase = Phase.IDLE;
                        nextStart = align(now);
                        out.add(new Skip());
                    }
                } else if (phase == Phase.IDLE && now >= nextStart - warn && players >= minPlayers) {
                    phase = Phase.ANNOUNCED;
                    out.add(new Announce(nextStart));
                }
            }
            default -> {
            }
        }
        return out;
    }

    /** Sofort starten (Verwaltung) – liefert das Ende. */
    public long start(long now) {
        return begin(now);
    }

    /** Sofort beenden und werten (Verwaltung). */
    public List<Entry> end(long now) {
        return phase == Phase.RUNNING ? finish(now) : List.of();
    }

    /** Ohne Wertung abbrechen. */
    public void abort(long now) {
        best.clear();
        phase = Phase.IDLE;
        nextStart = align(now);
    }

    private long begin(long now) {
        best.clear();
        phase = Phase.RUNNING;
        endsAt = now + duration;
        return endsAt;
    }

    private List<Entry> finish(long now) {
        List<Entry> ranking = ranking();
        best.clear();
        phase = Phase.IDLE;
        nextStart = align(now);
        return ranking;
    }

    /**
     * Fang melden; zählt nur im laufenden Turnier und nur echte Fische. Liefert {@code true}, wenn es der beste Fang
     * dieser Person im Turnier ist.
     */
    public boolean record(UUID uuid, String name, Catch c, long now) {
        if (phase != Phase.RUNNING || c.species().junk()) {
            return false;
        }
        Entry old = best.get(uuid);
        if (old != null && old.grams() >= c.grams()) {
            return false;
        }
        best.put(uuid, new Entry(uuid, name, c.grams(), c.species(), now));
        return true;
    }

    public List<Entry> ranking() {
        List<Entry> out = new ArrayList<>(best.values());
        out.sort(ORDER);
        return out;
    }

    /** Platz einer Person (1 = vorne), 0 ohne Fang. */
    public int place(UUID uuid) {
        List<Entry> r = ranking();
        for (int i = 0; i < r.size(); i++) {
            if (r.get(i).uuid().equals(uuid)) {
                return i + 1;
            }
        }
        return 0;
    }

    public Entry entry(UUID uuid) {
        return best.get(uuid);
    }
}
