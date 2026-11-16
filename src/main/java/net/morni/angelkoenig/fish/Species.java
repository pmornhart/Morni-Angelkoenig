package net.morni.angelkoenig.fish;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Alle Fische (und Fundstücke) im Tal – eigene Auswahl heimischer Alpen-Fische plus ein paar Legenden. Größen in cm
 * (Normalverteilung, begrenzt), Gewicht aus der Länge ({@code Gewicht = a × Länge³}), Grundpreis in Talern für einen
 * Fisch mittlerer Größe. Wie oft eine Art anbeißt, steht je Fangplatz in {@link #weight(Spot)}, Tageszeit und Wetter
 * wirken als Faktor.
 */
public enum Species {

    // ---------- gewöhnlich ----------
    ROTAUGE(fish("rotauge", Rarity.COMMON, "cod", 22, 4, 12, 38, 0.0120, 6)
            .at(Spot.BERGSEE, 28).at(Spot.MOOR, 22).weather(Weather.REGEN, 0.8)),
    BARSCH(fish("barsch", Rarity.COMMON, "cod", 24, 5, 12, 45, 0.0140, 8)
            .at(Spot.BERGSEE, 22).at(Spot.BACH, 5).at(Spot.MOOR, 8).at(Spot.EIS, 8)
            .time(DayTime.TAG, 1.3).time(DayTime.NACHT, 0.6)),
    BACHFORELLE(fish("bachforelle", Rarity.COMMON, "salmon", 30, 6, 16, 62, 0.0110, 10)
            .at(Spot.BACH, 35).at(Spot.BERGSEE, 5)
            .time(DayTime.MORGEN, 1.5).time(DayTime.ABEND, 1.5).weather(Weather.REGEN, 1.3)),
    KARAUSCHE(fish("karausche", Rarity.COMMON, "pufferfish", 20, 4, 10, 40, 0.0240, 7)
            .at(Spot.MOOR, 26)),

    // ---------- ungewöhnlich ----------
    RENKE(fish("renke", Rarity.UNCOMMON, "cod", 36, 5, 22, 55, 0.0092, 18)
            .at(Spot.BERGSEE, 18).at(Spot.EIS, 22).time(DayTime.MORGEN, 1.3)),
    AESCHE(fish("aesche", Rarity.UNCOMMON, "cod", 34, 5, 20, 55, 0.0088, 18)
            .at(Spot.BACH, 20).time(DayTime.TAG, 1.2)),
    SCHLEIE(fish("schleie", Rarity.UNCOMMON, "cod", 32, 6, 18, 62, 0.0135, 16)
            .at(Spot.MOOR, 18).time(DayTime.ABEND, 1.5).time(DayTime.NACHT, 1.3)),
    STEINKREBS(fish("steinkrebs", Rarity.UNCOMMON, "armadillo_scute", 9, 1.5, 5, 14, 0.0320, 20)
            .at(Spot.BACH, 10).at(Spot.MOOR, 6).time(DayTime.NACHT, 2.0).time(DayTime.TAG, 0.6)),

    // ---------- selten ----------
    SEESAIBLING(fish("seesaibling", Rarity.RARE, "salmon", 35, 6, 20, 65, 0.0102, 40)
            .at(Spot.BERGSEE, 7).at(Spot.EIS, 22).time(DayTime.MORGEN, 1.4)),
    HECHT(fish("hecht", Rarity.RARE, "cod", 66, 15, 35, 130, 0.0055, 45)
            .at(Spot.MOOR, 8).at(Spot.BERGSEE, 4).time(DayTime.TAG, 1.2).time(DayTime.NACHT, 0.7)
            .weather(Weather.REGEN, 1.2)),
    ZANDER(fish("zander", Rarity.RARE, "cod", 55, 10, 30, 95, 0.0075, 42)
            .at(Spot.BERGSEE, 6).time(DayTime.ABEND, 1.8).time(DayTime.NACHT, 1.5).time(DayTime.TAG, 0.6)),
    AAL(fish("aal", Rarity.RARE, "cod", 62, 12, 35, 110, 0.0017, 50)
            .at(Spot.MOOR, 7).at(Spot.BERGSEE, 3)
            .time(DayTime.MORGEN, 0).time(DayTime.TAG, 0).time(DayTime.NACHT, 3.0)
            .weather(Weather.REGEN, 1.4).weather(Weather.GEWITTER, 1.6)),
    RUTTE(fish("rutte", Rarity.RARE, "cod", 42, 8, 25, 75, 0.0065, 45)
            .at(Spot.EIS, 16).at(Spot.BERGSEE, 2).time(DayTime.NACHT, 2.0).time(DayTime.ABEND, 1.2)),

    // ---------- episch ----------
    WALLER(fish("waller", Rarity.EPIC, "cod", 135, 30, 70, 240, 0.0062, 150)
            .at(Spot.BERGSEE, 2.2).at(Spot.MOOR, 1.4)
            .time(DayTime.NACHT, 3.0).time(DayTime.ABEND, 1.5).time(DayTime.TAG, 0.5)
            .weather(Weather.GEWITTER, 2.5).weather(Weather.REGEN, 1.3)),
    HUCHEN(fish("huchen", Rarity.EPIC, "salmon", 85, 15, 50, 140, 0.0088, 160)
            .at(Spot.BACH, 2.8).time(DayTime.MORGEN, 1.6).time(DayTime.ABEND, 1.3).weather(Weather.REGEN, 1.5)),
    SEEFORELLE(fish("seeforelle", Rarity.EPIC, "salmon", 70, 12, 40, 110, 0.0102, 140)
            .at(Spot.BERGSEE, 3).at(Spot.EIS, 2.5).time(DayTime.MORGEN, 1.5)),

    // ---------- legendär ----------
    GOLDFORELLE(fish("goldforelle", Rarity.LEGENDARY, "tropical_fish", 45, 8, 28, 70, 0.0115, 700)
            .at(Spot.BACH, 0.35).at(Spot.BERGSEE, 0.12).time(DayTime.MORGEN, 2.0).time(DayTime.NACHT, 0.5)
            .weather(Weather.REGEN, 1.6).weather(Weather.GEWITTER, 0.5).shine()),
    SEEKOENIG(fish("seekoenig", Rarity.LEGENDARY, "cod", 215, 22, 170, 270, 0.0062, 900)
            .at(Spot.BERGSEE, 0.2).time(DayTime.NACHT, 3.0).time(DayTime.TAG, 0.4)
            .weather(Weather.GEWITTER, 2.5).shine()),
    MOORKOENIG(fish("moorkoenig", Rarity.LEGENDARY, "cod", 125, 14, 100, 160, 0.0056, 800)
            .at(Spot.MOOR, 0.3).time(DayTime.NACHT, 2.0).time(DayTime.TAG, 0.5)
            .weather(Weather.GEWITTER, 3.0).weather(Weather.REGEN, 1.5).shine()),
    KRISTALLSAIBLING(fish("kristallsaibling", Rarity.LEGENDARY, "salmon", 52, 8, 32, 80, 0.0102, 800)
            .at(Spot.EIS, 0.35).time(DayTime.NACHT, 2.5).time(DayTime.TAG, 0.6)
            .weather(Weather.KLAR, 1.4).shine()),

    // ---------- Fundstücke (kein Fisch) ----------
    STIEFEL(junk("stiefel", "leather_boots", 450, 1300, 2)
            .at(Spot.BERGSEE, 5).at(Spot.BACH, 3).at(Spot.MOOR, 6).at(Spot.EIS, 2)
            .weather(Weather.GEWITTER, 1.5)),
    TREIBHOLZ(junk("treibholz", "stick", 80, 900, 1)
            .at(Spot.BERGSEE, 4).at(Spot.BACH, 6).at(Spot.MOOR, 4).at(Spot.EIS, 1)
            .weather(Weather.REGEN, 1.5).weather(Weather.GEWITTER, 2.0)),
    FLASCHENPOST(junk("flaschenpost", "glass_bottle", 300, 600, 40)
            .at(Spot.BERGSEE, 0.8).at(Spot.BACH, 0.3).at(Spot.MOOR, 0.3).at(Spot.EIS, 0.2));

    private final Def d;

    Species(Def d) {
        this.d = d;
    }

    public String id() {
        return d.id;
    }

    public Rarity rarity() {
        return d.rarity;
    }

    /** Fundstück statt Fisch (kein Eintrag bei den Arten, keine Länge). */
    public boolean junk() {
        return d.rarity == Rarity.JUNK;
    }

    /** Minecraft-Gegenstand für den Fang, z. B. „salmon“. */
    public String item() {
        return d.item;
    }

    /** Glänzt der Gegenstand (Legenden)? */
    public boolean shines() {
        return d.shine;
    }

    public double meanCm() {
        return d.mean;
    }

    public double sdCm() {
        return d.sd;
    }

    public double minCm() {
        return d.min;
    }

    public double maxCm() {
        return d.max;
    }

    /** Gewicht in Gramm zu einer Länge (ohne die kleine Streuung „gut genährt“). */
    public double gramsAt(double cm) {
        return d.coefficient * cm * cm * cm;
    }

    /** Gewicht eines mittelgroßen Fangs (für den Preis). */
    public double averageGrams() {
        return junk() ? (d.min + d.max) / 2.0 : gramsAt(d.mean);
    }

    /** Fundstücke: leichtestes und schwerstes Gewicht in Gramm. */
    public int junkMinGrams() {
        return (int) d.min;
    }

    public int junkMaxGrams() {
        return (int) d.max;
    }

    public int basePrice() {
        return d.price;
    }

    /** Grundgewicht an diesem Fangplatz (0 = lebt dort nicht). */
    public double weight(Spot spot) {
        return d.spots.getOrDefault(spot, 0.0);
    }

    public Set<Spot> spots() {
        return Collections.unmodifiableSet(d.spots.keySet());
    }

    public double timeFactor(DayTime t) {
        return d.times.getOrDefault(t, 1.0);
    }

    public double weatherFactor(Weather w) {
        return d.weathers.getOrDefault(w, 1.0);
    }

    /** Beißt diese Art zu dieser Tageszeit überhaupt? */
    public boolean activeAt(DayTime t) {
        return timeFactor(t) > 0;
    }

    /** Alle echten Fischarten (ohne Fundstücke) – das zählt im Fischbuch als „Arten“. */
    public static List<Species> fishes() {
        return java.util.Arrays.stream(values()).filter(s -> !s.junk()).toList();
    }

    public static Optional<Species> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        for (Species s : values()) {
            if (s.id().equalsIgnoreCase(id)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    // ---------- Aufbau ----------

    private static Def fish(String id, Rarity rarity, String item, double mean, double sd, double min, double max,
            double coefficient, int price) {
        return new Def(id, rarity, item, mean, sd, min, max, coefficient, price);
    }

    private static Def junk(String id, String item, int minGrams, int maxGrams, int price) {
        return new Def(id, Rarity.JUNK, item, 0, 0, minGrams, maxGrams, 0, price);
    }

    /** Werte einer Art (nur beim Aufbau der Liste). */
    static final class Def {
        final String id;
        final Rarity rarity;
        final String item;
        final double mean;
        final double sd;
        final double min;
        final double max;
        final double coefficient;
        final int price;
        final Map<Spot, Double> spots = new EnumMap<>(Spot.class);
        final Map<DayTime, Double> times = new EnumMap<>(DayTime.class);
        final Map<Weather, Double> weathers = new EnumMap<>(Weather.class);
        boolean shine;

        Def(String id, Rarity rarity, String item, double mean, double sd, double min, double max, double coefficient,
                int price) {
            this.id = id;
            this.rarity = rarity;
            this.item = item;
            this.mean = mean;
            this.sd = sd;
            this.min = min;
            this.max = max;
            this.coefficient = coefficient;
            this.price = price;
        }

        Def at(Spot spot, double weight) {
            spots.put(spot, weight);
            return this;
        }

        Def time(DayTime t, double factor) {
            times.put(t, factor);
            return this;
        }

        Def weather(Weather w, double factor) {
            weathers.put(w, factor);
            return this;
        }

        Def shine() {
            shine = true;
            return this;
        }
    }

    /** Alle Fangplätze, an denen es mindestens einen echten Fisch gibt (für Tests). */
    static Set<Spot> spotsWithFish() {
        Set<Spot> out = EnumSet.noneOf(Spot.class);
        for (Species s : fishes()) {
            out.addAll(s.spots());
        }
        return out;
    }
}
