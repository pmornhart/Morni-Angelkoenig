package de.moinmorni.angelkoenig.fish;

import java.util.Optional;

/**
 * Angeln: bessere beißen schneller an ({@link #waitFactor()}, Faktor auf die Wartezeit) und holen öfter Seltenes
 * heraus ({@link #luck()}). Die Haselrute hat jeder von Anfang an.
 */
public enum Rod {

    HASEL("hasel", 0, 1.00, 0, false),
    ESCHE("esche", 250, 0.85, 1, false),
    MEISTER("meister", 1200, 0.72, 2, false),
    KOENIG("koenig", 5000, 0.60, 3, true);

    private final String id;
    private final int price;
    private final double waitFactor;
    private final int luck;
    private final boolean shine;

    Rod(String id, int price, double waitFactor, int luck, boolean shine) {
        this.id = id;
        this.price = price;
        this.waitFactor = waitFactor;
        this.luck = luck;
        this.shine = shine;
    }

    public String id() {
        return id;
    }

    public int price() {
        return price;
    }

    public double waitFactor() {
        return waitFactor;
    }

    public int luck() {
        return luck;
    }

    public boolean shines() {
        return shine;
    }

    /** Wartezeit auf den Biss in Ticks: Grundwert × Faktor der Angel, mindestens 1 Sekunde. */
    public int waitTicks(int baseTicks) {
        return Math.max(20, (int) Math.round(baseTicks * waitFactor));
    }

    public static Optional<Rod> byId(String id) {
        for (Rod r : values()) {
            if (r.id.equalsIgnoreCase(id)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }
}
