package de.moinmorni.angelkoenig.fish;

/**
 * Ein Fang: Art, Länge in Millimetern (0 bei Fundstücken) und Gewicht in Gramm.
 *
 * @param trophy Prachtexemplar: deutlich größer als üblich (mehr als zwei Streuungen über dem Mittel)
 */
public record Catch(Species species, int lengthMm, int grams, boolean trophy) {

    /** Länge in cm mit einer Nachkommastelle (deutsch mit Komma). */
    public String lengthText(String lang) {
        return Units.cm(lengthMm, lang);
    }

    public String weightText(String lang) {
        return Units.weight(grams, lang);
    }

    /** Wert beim Fischhändler. */
    public int price() {
        return Prices.sale(species, grams);
    }
}
