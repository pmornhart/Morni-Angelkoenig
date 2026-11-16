package net.morni.angelkoenig.fish;

import java.util.Optional;
import java.util.random.RandomGenerator;

/** Wetter im Tal. Am Eisloch fällt bei „Regen“ Schnee – für die Fische ist das dasselbe. */
public enum Weather {

    KLAR("klar"),
    REGEN("regen"),
    GEWITTER("gewitter");

    private final String id;

    Weather(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Weather of(boolean storm, boolean thunder) {
        if (storm && thunder) {
            return GEWITTER;
        }
        return storm ? REGEN : KLAR;
    }

    public static Optional<Weather> byId(String id) {
        for (Weather w : values()) {
            if (w.id.equalsIgnoreCase(id)) {
                return Optional.of(w);
            }
        }
        return Optional.empty();
    }

    /** Ein Abschnitt Wetter: wie lange er dauert (Sekunden). */
    public record Spell(Weather weather, int seconds) {
    }

    /**
     * Nächstes Wetter: meistens klar, manchmal Regen, selten Gewitter – nie zweimal hintereinander dasselbe
     * Schlechtwetter. Dauer in Sekunden zwischen {@code minSeconds} und {@code maxSeconds} (Schlechtwetter halb so lang).
     */
    public static Spell next(Weather now, RandomGenerator r, int minSeconds, int maxSeconds) {
        Weather w;
        if (now != KLAR) {
            w = KLAR;
        } else {
            int roll = r.nextInt(100);
            w = roll < 55 ? KLAR : roll < 88 ? REGEN : GEWITTER;
        }
        int lo = Math.max(1, minSeconds);
        int hi = Math.max(lo, maxSeconds);
        int seconds = lo + r.nextInt(hi - lo + 1);
        if (w != KLAR) {
            seconds = Math.max(lo / 2, seconds / 2);
        }
        return new Spell(w, Math.max(1, seconds));
    }
}
