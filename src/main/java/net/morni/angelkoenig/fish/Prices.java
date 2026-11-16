package net.morni.angelkoenig.fish;

/** Was der Fischhändler für einen Fang zahlt: Grundpreis der Art, größere Fische bringen mehr. */
public final class Prices {

    /** Wie stark die Größe den Preis hebt (Gewicht relativ zum Durchschnitt hoch diese Zahl). */
    static final double SIZE_EXPONENT = 0.6;
    static final double MIN_FACTOR = 0.5;
    static final double MAX_FACTOR = 3.0;

    private Prices() {
    }

    public static int sale(Species s, int grams) {
        if (s.junk()) {
            return s.basePrice();
        }
        double ratio = Math.max(1, grams) / s.averageGrams();
        double factor = Math.max(MIN_FACTOR, Math.min(MAX_FACTOR, Math.pow(ratio, SIZE_EXPONENT)));
        return Math.max(1, (int) Math.round(s.basePrice() * factor));
    }
}
