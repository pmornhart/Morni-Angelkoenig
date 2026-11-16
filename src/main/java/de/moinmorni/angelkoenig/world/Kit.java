package de.moinmorni.angelkoenig.world;

/**
 * Bausteine für die Bauten im Tal: Alpenhaus, Bänke, Laternen, Zäune, Blumenkästen, Bäume. Eigene Entwürfe, nur
 * Minecraft-Blöcke. Alle Angaben in Weltkoordinaten; {@code y} ist immer die Höhe, auf der man steht (Boden darunter).
 */
final class Kit {

    static final String LEAF_SPRUCE = "spruce_leaves[persistent=true]";
    static final String LEAF_BIRCH = "birch_leaves[persistent=true]";
    static final String LEAF_OAK = "oak_leaves[persistent=true]";
    static final String[] FLOWERS = {"poppy", "dandelion", "cornflower", "azure_bluet", "oxeye_daisy", "allium",
        "red_tulip", "lily_of_the_valley"};
    static final String[] POTS = {"potted_red_tulip", "potted_poppy", "potted_cornflower", "potted_azure_bluet",
        "potted_allium", "potted_orange_tulip"};

    private Kit() {
    }

    /** Fester Zufall 0 … n−1 je Stelle (die Welt sieht immer gleich aus). */
    static int roll(int x, int y, int z, int n) {
        return Noise.pick(x * 31 + y, z * 17 - y, 77, n);
    }

    static String stairs(String wood, String facing, boolean top) {
        return wood + "_stairs[facing=" + facing + ",half=" + (top ? "top" : "bottom") + ",shape=straight]";
    }

    static String slab(String wood, boolean top) {
        return wood + "_slab[type=" + (top ? "top" : "bottom") + "]";
    }

    static String opposite(String facing) {
        return switch (facing) {
            case "north" -> "south";
            case "south" -> "north";
            case "east" -> "west";
            default -> "east";
        };
    }

    /** Gegenrichtung als Versatz {dx, dz}. */
    static int[] step(String facing) {
        return switch (facing) {
            case "north" -> new int[] {0, -1};
            case "south" -> new int[] {0, 1};
            case "east" -> new int[] {1, 0};
            default -> new int[] {-1, 0};
        };
    }

    /** Laternenpfahl: Zaunpfosten, oben eine Laterne. */
    static void lanternPost(Plan p, int x, int y, int z, int height) {
        for (int i = 0; i < height; i++) {
            p.set(x, y + i, z, "spruce_fence");
        }
        p.set(x, y + height, z, "lantern[hanging=false]");
    }

    /** Hängende Laterne an einer Kette. */
    static void hangingLantern(Plan p, int x, int y, int z, int chain) {
        for (int i = 0; i < chain; i++) {
            p.set(x, y - i, z, "iron_chain[axis=y]");
        }
        p.set(x, y - chain, z, "lantern[hanging=true]");
    }

    /**
     * Holzbank: Sitzfläche aus Treppen (Rücken auf der Seite {@code back}), Armlehnen aus Falltüren. {@code alongX}:
     * Bank läuft in x-Richtung.
     */
    static void bench(Plan p, int x, int y, int z, int length, String back, String wood) {
        boolean alongX = back.equals("north") || back.equals("south");
        for (int i = 0; i < length; i++) {
            int bx = alongX ? x + i : x;
            int bz = alongX ? z : z + i;
            p.set(bx, y, bz, stairs(wood, back, false));
        }
        int[] before = alongX ? new int[] {x - 1, z} : new int[] {x, z - 1};
        int[] after = alongX ? new int[] {x + length, z} : new int[] {x, z + length};
        String armA = alongX ? "west" : "north";
        String armB = alongX ? "east" : "south";
        p.set(before[0], y, before[1], wood + "_trapdoor[facing=" + armA + ",half=bottom,open=true]");
        p.set(after[0], y, after[1], wood + "_trapdoor[facing=" + armB + ",half=bottom,open=true]");
    }

    /** Zaun von a nach b (gerade Linie in x oder z). */
    static void fence(Plan p, int x1, int z1, int x2, int z2, int y, String fence) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                p.set(x, y, z, fence);
            }
        }
    }

    /** Fichte: Stamm und kegelförmige Krone, oben spitz. */
    static void spruce(Plan p, int x, int y, int z, int height, boolean snow) {
        for (int i = 0; i < height; i++) {
            p.set(x, y + i, z, "spruce_log[axis=y]");
        }
        int top = y + height;
        p.set(x, top, z, LEAF_SPRUCE);
        p.set(x, top + 1, z, LEAF_SPRUCE);
        for (int layer = 1; layer < height - 1; layer++) {
            int ly = top - layer;
            int rad = Math.min(3, 1 + layer / 2 - (layer % 2 == 0 ? 0 : 1));
            rad = Math.max(1, rad);
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    if ((dx != 0 || dz != 0) && Math.abs(dx) + Math.abs(dz) <= rad + 1
                            && roll(x + dx, ly, z + dz, 10) > 0) {
                        p.setIfEmpty(x + dx, ly, z + dz, LEAF_SPRUCE);
                        if (snow && p.get(x + dx, ly + 1, z + dz) == null) {
                            p.setIfEmpty(x + dx, ly + 1, z + dz, "snow[layers=1]");
                        }
                    }
                }
            }
        }
        if (snow) {
            p.setIfEmpty(x, top + 2, z, "snow[layers=2]");
        }
    }

    /** Laubbaum mit runder Krone (Linde, Birke …). */
    static void broadTree(Plan p, int x, int y, int z, String log, String leaves, int trunk, int radius) {
        for (int i = 0; i < trunk + radius / 2 + 1; i++) {
            p.set(x, y + i, z, log + "[axis=y]");
        }
        int cy = y + trunk + radius / 2;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius + 1; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy * 1.6 + dz * dz);
                    if (d <= radius + 0.3 - roll(x + dx, cy + dy, z + dz, 10) * 0.06) {
                        p.setIfEmpty(x + dx, cy + dy, z + dz, leaves);
                    }
                }
            }
        }
    }

    /** Blumenbeet: Erde mit Blumen, Rand aus Steinplatten-Stufen. */
    static void flowerBed(Plan p, int x1, int z1, int x2, int z2, int y) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                if (edge) {
                    p.set(x, y, z, "stone_brick_slab[type=bottom]");
                } else {
                    p.set(x, y - 1, z, "rooted_dirt");
                    p.set(x, y, z, FLOWERS[roll(x, y, z, FLOWERS.length)]);
                }
            }
        }
    }

    /**
     * Alpenhaus: Steinsockel, weiß verputztes Erdgeschoss, Holz-Obergeschoss mit Balkon zur Türseite, Satteldach mit
     * Überstand, Fensterläden, Blumenkästen, Kamin. {@code door}: Seite der Tür (north/south/east/west). Steht auf
     * {@code y} (Boden darunter wird Sockel).
     */
    static void house(Plan p, int x1, int z1, int x2, int z2, int y, String door, String wood, String plaster) {
        int h1 = 3;
        int h2 = 3;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
                p.set(x, y - 1, z, edge ? "stone_bricks" : wood + "_planks");
                p.set(x, y - 2, z, "stone_bricks");
                for (int dy = 0; dy < h1 + h2; dy++) {
                    if (!edge) {
                        p.set(x, y + dy, z, dy == h1 ? wood + "_planks" : Plan.AIR);
                        continue;
                    }
                    String block = corner ? wood + "_log[axis=y]" : dy < h1 ? plaster : wood + "_planks";
                    if (dy == 0 && !corner) {
                        block = "stone_bricks";
                    }
                    p.set(x, y + dy, z, block);
                }
            }
        }
        boolean ridgeAlongZ = door.equals("north") || door.equals("south");
        int[] d = step(door);
        int dx = d[0] == 0 ? (x1 + x2) / 2 : d[0] > 0 ? x2 : x1;
        int dz = d[1] == 0 ? (z1 + z2) / 2 : d[1] > 0 ? z2 : z1;
        // Fenster ringsum, zwei Geschosse (nicht direkt neben der Tür)
        for (int level : new int[] {y + 1, y + h1 + 1}) {
            for (int x = x1 + 2; x <= x2 - 2; x += 2) {
                if (!(door.equals("north") && Math.abs(x - dx) <= 1)) {
                    window(p, x, level, z1, "north", wood);
                }
                if (!(door.equals("south") && Math.abs(x - dx) <= 1)) {
                    window(p, x, level, z2, "south", wood);
                }
            }
            for (int z = z1 + 2; z <= z2 - 2; z += 2) {
                if (!(door.equals("west") && Math.abs(z - dz) <= 1)) {
                    window(p, x1, level, z, "west", wood);
                }
                if (!(door.equals("east") && Math.abs(z - dz) <= 1)) {
                    window(p, x2, level, z, "east", wood);
                }
            }
        }
        // Tür (mittig), Stufe davor, Laterne unter dem Balkon
        p.set(dx, y, dz, wood + "_door[facing=" + door + ",half=lower,hinge=left,open=false]");
        p.set(dx, y + 1, dz, wood + "_door[facing=" + door + ",half=upper,hinge=left,open=false]");
        p.set(dx, y + 2, dz, wood + "_planks");
        p.set(dx + d[0], y - 1, dz + d[1], "polished_andesite");
        p.set(dx + d[0] - d[1], y + 2, dz + d[1] + d[0], "lantern[hanging=true]");
        if (ridgeAlongZ) {
            int bz = d[1] > 0 ? z2 + 1 : z1 - 1;
            for (int x = x1 + 1; x <= x2 - 1; x++) {
                p.set(x, y + h1, bz, slab(wood, true));
                p.set(x, y + h1 + 1, bz, wood + "_fence");
                if (roll(x, y, bz, 3) == 0) {
                    p.set(x, y + h1 + 2, bz, POTS[roll(x, y + 1, bz, POTS.length)]);
                }
            }
        } else {
            int bx = d[0] > 0 ? x2 + 1 : x1 - 1;
            for (int z = z1 + 1; z <= z2 - 1; z++) {
                p.set(bx, y + h1, z, slab(wood, true));
                p.set(bx, y + h1 + 1, z, wood + "_fence");
                if (roll(bx, y, z, 3) == 0) {
                    p.set(bx, y + h1 + 2, z, POTS[roll(bx, y + 1, z, POTS.length)]);
                }
            }
        }
        // Balkontür im Obergeschoss
        p.set(dx, y + h1 + 1, dz, wood + "_door[facing=" + door + ",half=lower,hinge=right,open=false]");
        p.set(dx, y + h1 + 2, dz, wood + "_door[facing=" + door + ",half=upper,hinge=right,open=false]");
        roof(p, x1, z1, x2, z2, y + h1 + h2, ridgeAlongZ, wood);
        // Kamin
        int cx = ridgeAlongZ ? x2 - 1 : x1 + 1;
        int cz = ridgeAlongZ ? z1 + 1 : z2 - 1;
        int roofTop = y + h1 + h2 + (ridgeAlongZ ? (x2 - x1) / 2 : (z2 - z1) / 2) + 1;
        for (int cy = y + h1 + h2; cy <= roofTop; cy++) {
            p.set(cx, cy, cz, "bricks");
        }
        p.set(cx, roofTop + 1, cz, "campfire[lit=true,signal_fire=false,facing=north]");
        // Einrichtung: Tisch, Stühle, Ofen, Bett oben
        int mx = (x1 + x2) / 2;
        int mz = (z1 + z2) / 2;
        p.set(mx, y, mz, wood + "_fence");
        p.set(mx, y + 1, mz, "spruce_pressure_plate");
        p.set(x1 + 1, y, z1 + 1, "barrel[facing=up,open=false]");
        p.set(x2 - 1, y, z1 + 1, "smoker[facing=" + (ridgeAlongZ ? "south" : "east") + ",lit=true]");
        p.set(x1 + 1, y + h1 + 1, z2 - 1, "red_bed[facing=north,part=foot,occupied=false]");
        p.set(x1 + 1, y + h1 + 1, z2 - 2, "red_bed[facing=north,part=head,occupied=false]");
        hangingLantern(p, mx, y + h1 - 1, mz, 0);
        p.reserve(x1 - 2, z1 - 2, x2 + 2, z2 + 2);
    }

    /** Vollblock zu einem Holz (bei Ziegeln „bricks“). */
    static String planks(String wood) {
        return "brick".equals(wood) ? "bricks" : wood + "_planks";
    }

    /** Satteldach mit Überstand; First in z-Richtung ({@code ridgeAlongZ}) oder x-Richtung. */
    static void roof(Plan p, int x1, int z1, int x2, int z2, int y, boolean ridgeAlongZ, String wood) {
        if (ridgeAlongZ) {
            for (int i = 0;; i++) {
                int left = x1 - 1 + i;
                int right = x2 + 1 - i;
                int ly = y + i;
                if (left > right) {
                    break;
                }
                for (int z = z1 - 1; z <= z2 + 1; z++) {
                    if (left == right) {
                        p.set(left, ly, z, slab(wood, false));
                        continue;
                    }
                    p.set(left, ly, z, stairs(wood, "east", false));
                    p.set(right, ly, z, stairs(wood, "west", false));
                    if (z == z1 || z == z2) {
                        for (int x = left + 1; x < right; x++) {
                            p.set(x, ly, z, planks(wood));
                        }
                    }
                }
            }
        } else {
            for (int i = 0;; i++) {
                int front = z1 - 1 + i;
                int back = z2 + 1 - i;
                int ly = y + i;
                if (front > back) {
                    break;
                }
                for (int x = x1 - 1; x <= x2 + 1; x++) {
                    if (front == back) {
                        p.set(x, ly, front, slab(wood, false));
                        continue;
                    }
                    p.set(x, ly, front, stairs(wood, "south", false));
                    p.set(x, ly, back, stairs(wood, "north", false));
                    if (x == x1 || x == x2) {
                        for (int z = front + 1; z < back; z++) {
                            p.set(x, ly, z, planks(wood));
                        }
                    }
                }
            }
        }
    }

    /** Fenster mit Läden und Blumenkasten (außen, Richtung {@code face}). */
    static void window(Plan p, int x, int y, int z, String face, String wood) {
        int[] o = step(face);
        p.set(x, y, z, "glass_pane");
        int ox = x + o[0];
        int oz = z + o[1];
        p.set(ox, y - 1, oz, wood + "_trapdoor[facing=" + face + ",half=top,open=false]");
        p.set(ox, y, oz, POTS[roll(x, y, z, POTS.length)]);
        boolean alongX = face.equals("north") || face.equals("south");
        String shutter = "green_terracotta".equals(wood) ? "spruce" : wood;
        if (alongX) {
            p.setIfEmpty(ox - 1, y, oz, shutter + "_trapdoor[facing=west,half=bottom,open=true]");
            p.setIfEmpty(ox + 1, y, oz, shutter + "_trapdoor[facing=east,half=bottom,open=true]");
        } else {
            p.setIfEmpty(ox, y, oz - 1, shutter + "_trapdoor[facing=north,half=bottom,open=true]");
            p.setIfEmpty(ox, y, oz + 1, shutter + "_trapdoor[facing=south,half=bottom,open=true]");
        }
    }
}
