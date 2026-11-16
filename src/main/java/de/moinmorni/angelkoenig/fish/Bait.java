package de.moinmorni.angelkoenig.fish;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Köder: locken bestimmte Arten (×{@link #TARGET_FACTOR}) und halten Krempel fern. Gekauft wird eine Packung, jeder
 * Fang verbraucht einen Köder. Der Goldspinner lockt Legenden und Epische.
 */
public enum Bait {

    WURM("wurm", "pink_dye", 10, 20, EnumSet.of(Species.BARSCH, Species.STEINKREBS, Species.AAL, Species.SCHLEIE,
            Species.BACHFORELLE)),
    MADEN("maden", "wheat_seeds", 10, 30, EnumSet.of(Species.RENKE, Species.AESCHE, Species.SEESAIBLING,
            Species.ROTAUGE, Species.RUTTE)),
    TEIG("teig", "bread", 10, 15, EnumSet.of(Species.ROTAUGE, Species.KARAUSCHE, Species.SCHLEIE)),
    FLIEGE("fliege", "feather", 10, 40, EnumSet.of(Species.BACHFORELLE, Species.AESCHE, Species.HUCHEN,
            Species.SEEFORELLE)),
    KOEDERFISCH("koederfisch", "tropical_fish_bucket", 10, 60, EnumSet.of(Species.HECHT, Species.ZANDER,
            Species.WALLER, Species.HUCHEN, Species.SEEFORELLE, Species.RUTTE)),
    GOLDSPINNER("goldspinner", "gold_nugget", 5, 300, EnumSet.noneOf(Species.class));

    /** So viel öfter beißen die Arten, für die der Köder gedacht ist. */
    public static final double TARGET_FACTOR = 3.0;
    /** Mit Köder kommt seltener Krempel an den Haken. */
    public static final double JUNK_FACTOR = 0.5;
    /** Goldspinner: Legenden ×2,5, Epische ×1,5. */
    public static final double GOLD_LEGENDARY = 2.5;
    public static final double GOLD_EPIC = 1.5;

    private final String id;
    private final String item;
    private final int pack;
    private final int price;
    private final Set<Species> targets;

    Bait(String id, String item, int pack, int price, Set<Species> targets) {
        this.id = id;
        this.item = item;
        this.pack = pack;
        this.price = price;
        this.targets = targets;
    }

    public String id() {
        return id;
    }

    /** Minecraft-Gegenstand als Bild im Menü. */
    public String item() {
        return item;
    }

    /** So viele Köder sind in einer Packung. */
    public int pack() {
        return pack;
    }

    /** Preis einer Packung in Talern. */
    public int price() {
        return price;
    }

    public Set<Species> targets() {
        return targets;
    }

    /** Faktor dieses Köders für eine Art. */
    public double factor(Species s) {
        if (s.junk()) {
            return JUNK_FACTOR;
        }
        if (this == GOLDSPINNER) {
            return s.rarity() == Rarity.LEGENDARY ? GOLD_LEGENDARY : s.rarity() == Rarity.EPIC ? GOLD_EPIC : 1.0;
        }
        return targets.contains(s) ? TARGET_FACTOR : 1.0;
    }

    public static Optional<Bait> byId(String id) {
        for (Bait b : values()) {
            if (b.id.equalsIgnoreCase(id)) {
                return Optional.of(b);
            }
        }
        return Optional.empty();
    }
}
