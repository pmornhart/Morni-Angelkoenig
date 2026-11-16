package de.moinmorni.angelkoenig.fish;

import java.util.Optional;

/**
 * Kleidung zum Angeben – nur Aussehen, keine Vorteile. Leder in eigenen Farben; die Königskrone gibt es nicht zu
 * kaufen, nur für einen Turniersieg.
 */
public enum Cosmetic {

    FISCHERHUT("fischerhut", Slot.HEAD, "leather_helmet", 0x6B7B3A, 150),
    STROHHUT("strohhut", Slot.HEAD, "leather_helmet", 0xE2C46C, 200),
    TRACHTENHUT("trachtenhut", Slot.HEAD, "leather_helmet", 0x2E4B33, 400),
    KOENIGSKRONE("koenigskrone", Slot.HEAD, "golden_helmet", -1, 0),
    ANGLERWESTE("anglerweste", Slot.CHEST, "leather_chestplate", 0x8C7A4E, 250),
    REGENJACKE("regenjacke", Slot.CHEST, "leather_chestplate", 0xF2C12E, 300),
    LEDERHOSE("lederhose", Slot.LEGS, "leather_leggings", 0x6E4A2A, 350),
    WATHOSE("wathose", Slot.LEGS, "leather_leggings", 0x3D4A2F, 250),
    GUMMISTIEFEL("gummistiefel", Slot.FEET, "leather_boots", 0x2F3B2F, 150);

    /** Wo man es trägt. */
    public enum Slot {
        HEAD, CHEST, LEGS, FEET
    }

    private final String id;
    private final Slot slot;
    private final String item;
    private final int colour;
    private final int price;

    Cosmetic(String id, Slot slot, String item, int colour, int price) {
        this.id = id;
        this.slot = slot;
        this.item = item;
        this.colour = colour;
        this.price = price;
    }

    public String id() {
        return id;
    }

    public Slot slot() {
        return slot;
    }

    public String item() {
        return item;
    }

    /** Lederfarbe (RGB) oder −1 ohne Farbe. */
    public int colour() {
        return colour;
    }

    public int price() {
        return price;
    }

    /** Kann man es beim Fischhändler kaufen? (Die Krone nicht.) */
    public boolean forSale() {
        return price > 0;
    }

    public static Optional<Cosmetic> byId(String id) {
        for (Cosmetic c : values()) {
            if (c.id.equalsIgnoreCase(id)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
}
