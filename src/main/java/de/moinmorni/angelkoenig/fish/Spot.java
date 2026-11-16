package de.moinmorni.angelkoenig.fish;

import java.util.Locale;
import java.util.Optional;

/** Die vier Fangplätze im Tal – jeder mit eigenen Fischen. */
public enum Spot {

    /** Großer Bergsee am Dorf: Steg, Ruderboote, Bootshaus. */
    BERGSEE("bergsee"),
    /** Gebirgsbach mit Wasserfall und Gumpe im Westen. */
    BACH("bach"),
    /** Moorweiher im Osten: Schilf, Seerosen, Bohlenweg. */
    MOOR("moor"),
    /** Eisloch auf dem zugefrorenen Karsee hoch oben im Südosten. */
    EIS("eis");

    private final String id;

    Spot(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<Spot> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String s = id.toLowerCase(Locale.ROOT);
        for (Spot spot : values()) {
            if (spot.id.equals(s)) {
                return Optional.of(spot);
            }
        }
        return Optional.empty();
    }
}
