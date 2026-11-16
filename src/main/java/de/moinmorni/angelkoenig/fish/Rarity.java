package de.moinmorni.angelkoenig.fish;

/**
 * Seltenheit eines Fangs. Bessere Angeln ({@link Rod#luck()}) machen seltene Stufen wahrscheinlicher und Krempel
 * unwahrscheinlicher.
 */
public enum Rarity {

    /** Alter Stiefel, Treibholz … – gehört nicht ins Fischbuch der Arten. */
    JUNK("krempel", 0x8A8A8A, 0, 0.0),
    COMMON("gewoehnlich", 0xE8E8E8, 1, 0.0),
    UNCOMMON("ungewoehnlich", 0x55D16E, 2, 0.15),
    RARE("selten", 0x4FA3F7, 3, 0.35),
    EPIC("episch", 0xB46CF0, 4, 0.55),
    LEGENDARY("legendaer", 0xF2B632, 5, 0.80);

    private final String id;
    private final int colour;
    private final int stars;
    private final double luckBonus;

    Rarity(String id, int colour, int stars, double luckBonus) {
        this.id = id;
        this.colour = colour;
        this.stars = stars;
        this.luckBonus = luckBonus;
    }

    public String id() {
        return id;
    }

    /** Farbe im Chat und auf dem Gegenstand (RGB). */
    public int colour() {
        return colour;
    }

    /** Sterne in der Beschreibung (0 bei Krempel). */
    public int stars() {
        return stars;
    }

    /** Zuschlag pro Glückspunkt der Angel: Gewicht × (1 + Glück × Zuschlag). */
    public double luckBonus() {
        return luckBonus;
    }

    /** Mindestens so selten wie {@code other}? Krempel zählt nie. */
    public boolean atLeast(Rarity other) {
        return this != JUNK && ordinal() >= other.ordinal();
    }
}
