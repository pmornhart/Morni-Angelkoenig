package de.moinmorni.angelkoenig.world;

import de.moinmorni.angelkoenig.fish.Spot;
import java.util.EnumMap;
import java.util.Map;

/**
 * Feste Stellen im Tal (x nach Osten, z nach Süden): Dorf am Nordufer des Bergsees, Gebirgsbach im Westen,
 * Moorweiher im Osten, Eisloch auf dem Karsee im Südosten. Höhe: Wasser der Seen auf y = 62, Boden auf 63, man steht
 * auf 64.
 */
public final class Layout {

    /** Wasseroberfläche von Bergsee, Bach-Gumpe und Moorweihern. */
    public static final int WATER_Y = 62;
    /** Oberster Bodenblock im Dorf. */
    public static final int GROUND_Y = 63;
    /** Hier stehen die Füße im Dorf. */
    public static final int STAND_Y = 64;
    /** Bach oberhalb des Wasserfalls. */
    public static final int UPPER_WATER_Y = 75;
    /** Eis auf dem Karsee (oberster Block). */
    public static final int ICE_Y = 82;
    /** Boden auf der Hochebene am Karsee. */
    public static final int KAR_Y = 83;
    /** Bis hierhin dürfen Spieler (Weltgrenze ± so viel um 0/0). */
    public static final int BORDER = 200;
    /** So weit reicht das Gelände (hinter der Grenze stehen noch Berge). */
    public static final int EXTENT = 240;

    /** Wasserfall: Spalte x, von z bis z (fällt von UPPER_WATER_Y in die Gumpe). */
    public static final int FALL_X = -110;
    public static final int FALL_Z1 = 0;
    public static final int FALL_Z2 = 2;

    /** Ein Ort mit Blickrichtung (Minecraft: Gierwinkel 0 = Süden, −90 = Osten, 90 = Westen, 180 = Norden). */
    public record Place(double x, double y, double z, float yaw, float pitch) {
    }

    /** Ankunft im Dorf: Blick nach Süden über Steg und See auf die Berge. */
    public static final Place SPAWN = new Place(0.5, STAND_Y, -46.5, 0f, 0f);

    /** Wo der Wegweiser einen hinbringt. */
    public static final Map<Spot, Place> SPOTS = new EnumMap<>(Spot.class);

    static {
        SPOTS.put(Spot.BERGSEE, new Place(0.5, STAND_Y, -7.5, 0f, 20f));
        SPOTS.put(Spot.BACH, new Place(-99.5, STAND_Y, -7.5, 0f, 15f));
        SPOTS.put(Spot.MOOR, new Place(85.5, STAND_Y + 0.5, 6.5, -90f, 15f));
        SPOTS.put(Spot.EIS, new Place(69.5, KAR_Y, 93.5, 0f, 30f));
    }

    // ---------- Dorf ----------

    /** Steg: von der Uferkante in den See, 3 breit. */
    public static final int STEG_X1 = -1;
    public static final int STEG_X2 = 1;
    public static final int STEG_Z1 = -31;
    public static final int STEG_Z2 = -12;
    /** Plattform am Ende des Stegs. */
    public static final int DECK_X1 = -5;
    public static final int DECK_X2 = 5;
    public static final int DECK_Z1 = -12;
    public static final int DECK_Z2 = -7;

    /** Fischhändler hinter seiner Theke (schaut nach Norden auf den Dorfplatz). */
    public static final Place MERCHANT = new Place(11.5, STAND_Y, -33.5, 180f, 0f);
    /** Bootsmann am Bootshaus (gibt Tipps). */
    public static final Place BOATMAN = new Place(-16.5, STAND_Y, -35.5, -120f, 0f);
    /** Tafelwand mit den Bestenlisten am Dorfplatz (Wand bei x = −15, Tafeln davor). */
    public static final int BOARD_WALL_X = -15;
    public static final int BOARD_WALL_Z1 = -52;
    public static final int BOARD_WALL_Z2 = -39;
    /** Mitte der drei Tafeln (vor der Wand, Blick nach Osten). */
    public static final Place[] BOARDS = {
        new Place(-13.6, STAND_Y + 2.6, -49.0, -90f, 0f),
        new Place(-13.6, STAND_Y + 2.6, -45.5, -90f, 0f),
        new Place(-13.6, STAND_Y + 2.6, -42.0, -90f, 0f),
    };
    /** Lagerfeuer mit Bänken. */
    public static final int FIRE_X = -27;
    public static final int FIRE_Z = -47;
    /** Brunnen mit der goldenen Forelle mitten auf dem Dorfplatz. */
    public static final int FOUNTAIN_X = 0;
    public static final int FOUNTAIN_Z = -39;
    /** Liegeplätze der Ruderboote (am Steg). */
    public static final Place[] BOATS = {
        new Place(-3.0, WATER_Y + 0.5, -24.5, 0f, 0f),
        new Place(4.0, WATER_Y + 0.5, -24.5, 0f, 0f),
        new Place(-3.0, WATER_Y + 0.5, -18.5, 0f, 0f),
        new Place(4.0, WATER_Y + 0.5, -18.5, 0f, 0f),
    };

    // ---------- Fangplätze ----------

    /** Plattform an der Gumpe (Nordufer). */
    public static final int POOL_DECK_X1 = -104;
    public static final int POOL_DECK_X2 = -96;
    public static final int POOL_DECK_Z1 = -10;
    public static final int POOL_DECK_Z2 = -7;
    /** Holzplattform am Moorweiher (Ende des Bohlenwegs). */
    public static final int MOOR_DECK_X1 = 83;
    public static final int MOOR_DECK_X2 = 86;
    public static final int MOOR_DECK_Z1 = 3;
    public static final int MOOR_DECK_Z2 = 10;
    /** Eislöcher (Mitte, 3 × 3). */
    public static final int[][] ICE_HOLES = {{69, 96}, {76, 104}, {85, 98}, {80, 93}};
    /** Eisfischer-Hütte auf dem See. */
    public static final int HUT_X1 = 86;
    public static final int HUT_X2 = 90;
    public static final int HUT_Z1 = 103;
    public static final int HUT_Z2 = 107;

    /** Tafel „Was beißt gerade?“ an jedem Fangplatz. */
    public static final Map<Spot, Place> SPOT_BOARDS = new EnumMap<>(Spot.class);

    static {
        SPOT_BOARDS.put(Spot.BERGSEE, new Place(4.5, STAND_Y + 2.6, -10.5, 0f, 0f));
        SPOT_BOARDS.put(Spot.BACH, new Place(-104.5, STAND_Y + 2.6, -9.5, 0f, 0f));
        SPOT_BOARDS.put(Spot.MOOR, new Place(83.5, STAND_Y + 3.0, 2.5, 0f, 0f));
        SPOT_BOARDS.put(Spot.EIS, new Place(65.5, KAR_Y + 2.6, 92.5, 0f, 0f));
    }

    private Layout() {
    }
}
