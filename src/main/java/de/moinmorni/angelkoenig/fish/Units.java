package de.moinmorni.angelkoenig.fish;

import java.util.Locale;

/** Zahlen für Menschen: Gewicht, Länge, Taler – deutsch mit Komma und Punkt als Tausendertrenner. */
public final class Units {

    private static final int KILO = 1000;

    private Units() {
    }

    /** 850 → „850 g“, 1234 → „1,23 kg“, 61 600 → „61,6 kg“. */
    public static String weight(int grams, String lang) {
        if (grams < KILO) {
            return grams + " g";
        }
        double kg = grams / (double) KILO;
        String s = kg >= 10 ? String.format(Locale.ROOT, "%.1f", kg) : String.format(Locale.ROOT, "%.2f", kg);
        return decimal(s, lang) + " kg";
    }

    /** Millimeter als „42,3 cm“. */
    public static String cm(int mm, String lang) {
        return decimal(String.format(Locale.ROOT, "%.1f", mm / 10.0), lang) + " cm";
    }

    /** Ganze Zahl mit Tausendertrenner: „12.345“ bzw. „12,345“. */
    public static String number(long n, String lang) {
        String s = String.format(Locale.ROOT, "%,d", n);
        return "en".equals(lang) ? s : s.replace(',', '.');
    }

    private static String decimal(String s, String lang) {
        return "en".equals(lang) ? s : s.replace('.', ',');
    }
}
