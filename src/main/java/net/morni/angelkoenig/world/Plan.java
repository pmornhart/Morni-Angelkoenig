package net.morni.angelkoenig.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Bauplan der Bauten im Tal (Dorf, Stege, Hütten, Brücke, Kapelle …) in Weltkoordinaten, Blöcke als Text wie
 * {@code "spruce_stairs[facing=north]"}. Ohne Server: der Chunk-Erzeuger macht daraus beim Erzeugen echte Blöcke.
 * {@code "air"} räumt das Gelände an dieser Stelle frei. Säulen mit {@link #reserve} bekommen keine Bäume und
 * Blumen vom Gelände.
 */
public final class Plan {

    /** Schild mit Text auf der Vorderseite (bekommt seinen Text, wenn der Chunk geladen wird). */
    public record Sign(int x, int y, int z, List<String> lines) {
    }

    /** Banner mit Mustern wie {@code "light_blue:rhombus"}. */
    public record Banner(int x, int y, int z, List<String> patterns) {
    }

    public static final String AIR = "air";
    private static final int OFFSET = 4096;
    private static final int BITS = 16;
    private static final int MASK = 0xFFFF;
    private static final String[] SIDES = {"north", "east", "south", "west"};
    private static final int[][] DIRS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    private final Map<Long, String> blocks = new LinkedHashMap<>();
    private final Set<Long> reserved = new HashSet<>();
    private final Map<Long, Integer> tops = new HashMap<>();
    private final List<Sign> signs = new ArrayList<>();
    private final List<Banner> banners = new ArrayList<>();

    public static long key(int x, int y, int z) {
        return ((long) (x + OFFSET) << (2 * BITS)) | ((long) (y + OFFSET) << BITS) | (z + OFFSET);
    }

    public static int keyX(long k) {
        return (int) ((k >>> (2 * BITS)) & MASK) - OFFSET;
    }

    public static int keyY(long k) {
        return (int) ((k >>> BITS) & MASK) - OFFSET;
    }

    public static int keyZ(long k) {
        return (int) (k & MASK) - OFFSET;
    }

    private static long column(int x, int z) {
        return ((long) (x + OFFSET) << BITS) | (z + OFFSET);
    }

    public void set(int x, int y, int z, String spec) {
        long k = key(x, y, z);
        blocks.remove(k);
        blocks.put(k, spec);
        if (!AIR.equals(spec)) {
            tops.merge(column(x, z), y, Math::max);
        }
    }

    /** Nur setzen, wenn der Plan dort noch nichts hat. */
    public void setIfEmpty(int x, int y, int z, String spec) {
        if (!blocks.containsKey(key(x, y, z))) {
            set(x, y, z, spec);
        }
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, String spec) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, spec);
                }
            }
        }
    }

    /** Freiräumen (auch Gelände, das dort entstehen würde). */
    public void clear(int x1, int y1, int z1, int x2, int y2, int z2) {
        fill(x1, y1, z1, x2, y2, z2, AIR);
    }

    public String get(int x, int y, int z) {
        return blocks.get(key(x, y, z));
    }

    /** Säulen ohne Bäume und Blumen vom Gelände (unter und um Bauten). */
    public void reserve(int x1, int z1, int x2, int z2) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                reserved.add(column(x, z));
            }
        }
    }

    public boolean reserved(int x, int z) {
        return reserved.contains(column(x, z));
    }

    /** Höchster Block des Plans in einer Säule (Vorschau) oder null. */
    public String topAt(int x, int z) {
        Integer y = tops.get(column(x, z));
        return y == null ? null : blocks.get(key(x, y, z));
    }

    public void sign(int x, int y, int z, String block, String... lines) {
        set(x, y, z, block);
        signs.add(new Sign(x, y, z, List.of(lines)));
    }

    public void banner(int x, int y, int z, String block, String... patterns) {
        set(x, y, z, block);
        banners.add(new Banner(x, y, z, List.of(patterns)));
    }

    public List<Sign> signs() {
        return signs;
    }

    public List<Banner> banners() {
        return banners;
    }

    public void forEach(BiConsumer<Long, String> action) {
        blocks.forEach(action);
    }

    public int size() {
        return blocks.size();
    }

    /** Alle verwendeten Blocknamen ohne Zustand (für Tests: gibt es die in 26.x?). */
    public Set<String> materials() {
        Set<String> out = new HashSet<>();
        for (String s : blocks.values()) {
            int i = s.indexOf('[');
            out.add(i < 0 ? s : s.substring(0, i));
        }
        return out;
    }

    // ---------- Zäune, Mauern, Scheiben verbinden ----------

    /**
     * Zäune, Gitter, Scheiben und Mauern mit ihren Nachbarn verbinden – die Welt entsteht ohne Physik, sonst blieben
     * sie einzelne Pfosten. Nur Einträge ohne eigene Angaben ({@code "spruce_fence"}) werden verbunden.
     */
    public void connect() {
        Map<Long, String> changed = new HashMap<>();
        blocks.forEach((k, spec) -> {
            if (spec.indexOf('[') >= 0) {
                return;
            }
            Kind kind = kind(spec);
            if (kind == Kind.OTHER) {
                return;
            }
            int x = keyX(k);
            int y = keyY(k);
            int z = keyZ(k);
            StringBuilder sb = new StringBuilder(spec).append('[');
            boolean any = false;
            for (int i = 0; i < 4; i++) {
                String n = blocks.get(key(x + DIRS[i][0], y, z + DIRS[i][1]));
                boolean joins = n != null && joins(kind, n);
                if (i > 0) {
                    sb.append(',');
                }
                any |= joins;
                sb.append(SIDES[i]).append('=');
                sb.append(kind == Kind.WALL ? (joins ? "low" : "none") : String.valueOf(joins));
            }
            if (kind == Kind.WALL) {
                String above = blocks.get(key(x, y + 1, z));
                boolean straight = straight(sb.toString());
                sb.append(",up=").append(above != null && !AIR.equals(above) || !straight || !any);
            }
            sb.append(']');
            changed.put(k, sb.toString());
        });
        blocks.putAll(changed);
    }

    private static boolean straight(String s) {
        boolean n = s.contains("north=low");
        boolean e = s.contains("east=low");
        boolean so = s.contains("south=low");
        boolean w = s.contains("west=low");
        return (n && so && !e && !w) || (e && w && !n && !so);
    }

    private enum Kind {
        FENCE, PANE, WALL, OTHER
    }

    private static Kind kind(String spec) {
        String s = base(spec);
        if (s.endsWith("_fence") && !s.endsWith("_fence_gate")) {
            return Kind.FENCE;
        }
        if (s.endsWith("glass_pane") || s.equals("iron_bars")) {
            return Kind.PANE;
        }
        if (s.endsWith("_wall") && !s.endsWith("_sign") && !s.contains("wall_")) {
            return Kind.WALL;
        }
        return Kind.OTHER;
    }

    private static String base(String spec) {
        int i = spec.indexOf('[');
        return (i < 0 ? spec : spec.substring(0, i)).toLowerCase(Locale.ROOT);
    }

    private static boolean joins(Kind self, String neighbour) {
        String n = base(neighbour);
        Kind other = kind(n);
        if (self == Kind.FENCE) {
            return other == Kind.FENCE || n.endsWith("_fence_gate") || full(n);
        }
        if (self == Kind.PANE) {
            return other == Kind.PANE || other == Kind.WALL || full(n);
        }
        return other == Kind.WALL || other == Kind.PANE || n.endsWith("_fence_gate") || full(n);
    }

    private static final String[] NOT_FULL = {"slab", "stairs", "fence", "wall", "pane", "door", "lantern", "torch",
        "flower", "carpet", "sign", "banner", "button", "pressure", "bars", "chain", "air", "water", "campfire",
        "potted", "candle", "ladder", "rail", "sapling", "short_grass", "tall_grass", "fern", "lily", "cane", "vine",
        "head", "skull", "_bed", "anvil", "cake", "lever", "barrier", "bush", "poppy", "dandelion", "tulip", "daisy",
        "bluet", "allium", "orchid", "cornflower", "lily_of", "roots", "snow[", "rod", "grindstone", "bell",
        "hopper", "brewing", "lectern", "composter", "cauldron", "trapdoor", "light", "petals", "sunflower", "lilac",
        "peony", "rose_bush"};

    private static boolean full(String n) {
        if (n.equals("snow")) {
            return false;
        }
        for (String part : NOT_FULL) {
            if (n.contains(part)) {
                return false;
            }
        }
        return true;
    }
}
