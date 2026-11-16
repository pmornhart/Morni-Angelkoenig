package net.morni.angelkoenig.world;

/**
 * Alle Bauten im Tal als ein Bauplan: Dorf am Nordufer (Dorfplatz mit Forellenbrunnen, Steg mit Plattform,
 * Fischhändler-Stand, Bootshaus, Fischerhaus, Alpenhäuser, Tafelwand, Lagerfeuer), Plattform an der Gumpe mit
 * Wasserfall und Marterl, Bohlenweg und Torfhütte im Moor, Eisfischer-Hütte auf dem Karsee, Brücke, Seekapelle am
 * Südufer und ein Gipfelkreuz. Eigene Entwürfe.
 */
public final class Valley {

    static final int G = Layout.GROUND_Y;
    static final int S = Layout.STAND_Y;

    private Valley() {
    }

    public static Plan build(Terrain t) {
        Plan p = new Plan();
        plaza(p);
        signpost(p);
        steg(p, t);
        merchant(p);
        boardWall(p);
        campfire(p);
        boathouse(p, t);
        houses(p);
        villageGreen(p);
        bach(p, t);
        bridge(p);
        moor(p);
        eis(p);
        chapel(p, t);
        summitCross(p, t);
        p.connect();
        return p;
    }

    // ---------- Dorfplatz ----------

    private static void plaza(Plan p) {
        int cx = Layout.FOUNTAIN_X;
        int cz = Layout.FOUNTAIN_Z;
        for (int x = cx - 9; x <= cx + 9; x++) {
            for (int z = cz - 9; z <= cz + 9; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d > 8.6) {
                    continue;
                }
                String b;
                if (d > 7.6) {
                    b = "polished_andesite";
                } else if ((int) Math.floor(d) % 3 == 0) {
                    b = "stone_bricks";
                } else {
                    int r = Kit.roll(x, G, z, 10);
                    b = r < 5 ? "cobblestone" : r < 8 ? "andesite" : r < 9 ? "mossy_cobblestone" : "gravel";
                }
                p.set(x, G, z, b);
            }
        }
        // Weg zum Steg (Pflaster) bis zur Uferkante
        for (int z = cz + 8; z <= -30; z++) {
            for (int x = -2; x <= 2; x++) {
                p.set(x, G, z, Math.abs(x) == 2 ? "polished_andesite" : Kit.roll(x, G, z, 3) == 0 ? "andesite"
                        : "cobblestone");
            }
        }
        // Brunnen: Becken mit Wasser, Rand aus Steinziegeln, in der Mitte ein Sockel für die goldene Forelle
        for (int x = cx - 3; x <= cx + 3; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d > 3.2) {
                    continue;
                }
                p.set(x, G, z, "polished_andesite");
                if (d > 2.2) {
                    p.set(x, S, z, "polished_andesite");
                } else {
                    p.set(x, S, z, "water");
                }
            }
        }
        p.set(cx, S, cz, "chiseled_stone_bricks");
        p.set(cx, S + 1, cz, "stone_brick_wall");
        p.set(cx, S + 2, cz, "polished_andesite_slab[type=bottom]");
        p.reserve(cx - 10, cz - 10, cx + 10, -28);
        // Laternen rund um den Platz
        int[][] posts = {{-7, -45}, {7, -45}, {-8, -36}, {8, -36}, {-3, -31}, {3, -31}};
        for (int[] l : posts) {
            Kit.lanternPost(p, l[0], S, l[1], 3);
        }
    }

    /** Wegweiser mit Schildern zu den Fangplätzen. */
    private static void signpost(Plan p) {
        int x = 6;
        int z = -47;
        p.set(x, G, z, "polished_andesite");
        for (int y = S; y <= S + 2; y++) {
            p.set(x, y, z, "spruce_log[axis=y]");
        }
        p.set(x, S + 3, z, "spruce_fence");
        p.set(x, S + 4, z, "lantern[hanging=false]");
        p.sign(x - 1, S + 2, z, "spruce_wall_sign[facing=west]", "", "← Gebirgsbach", "Mountain stream", "");
        p.sign(x + 1, S + 2, z, "spruce_wall_sign[facing=east]", "", "Moorweiher →", "Bog pond", "");
        p.sign(x + 1, S + 1, z, "spruce_wall_sign[facing=east]", "", "Eisloch →", "Ice hole", "");
        p.sign(x, S + 1, z + 1, "spruce_wall_sign[facing=south]", "", "Steg · Bergsee", "Jetty · Lake", "");
        p.reserve(x - 2, z - 2, x + 2, z + 2);
    }

    // ---------- Steg ----------

    private static void steg(Plan p, Terrain t) {
        for (int z = Layout.STEG_Z1; z <= Layout.STEG_Z2; z++) {
            for (int x = Layout.STEG_X1; x <= Layout.STEG_X2; x++) {
                p.set(x, G, z, x == 0 ? "spruce_planks" : "stripped_spruce_log[axis=z]");
            }
        }
        for (int x = Layout.DECK_X1; x <= Layout.DECK_X2; x++) {
            for (int z = Layout.DECK_Z1; z <= Layout.DECK_Z2; z++) {
                boolean edge = x == Layout.DECK_X1 || x == Layout.DECK_X2 || z == Layout.DECK_Z2;
                p.set(x, G, z, edge ? "stripped_spruce_log[axis=" + (z == Layout.DECK_Z2 ? "x" : "z") + "]"
                        : "spruce_planks");
            }
        }
        // Pfähle seitlich am Steg (Poller für die Boote), manche mit Laterne
        for (int z = Layout.STEG_Z1 + 2; z <= Layout.STEG_Z2; z += 4) {
            for (int x : new int[] {Layout.STEG_X1 - 1, Layout.STEG_X2 + 1}) {
                pile(p, t, x, z, G);
                p.set(x, S, z, "spruce_fence");
                if ((z - Layout.STEG_Z1) % 8 == 2) {
                    p.set(x, S + 1, z, "lantern[hanging=false]");
                }
            }
        }
        // Pfähle unter der Plattform
        for (int x : new int[] {Layout.DECK_X1, 0, Layout.DECK_X2}) {
            for (int z : new int[] {Layout.DECK_Z1, Layout.DECK_Z2}) {
                pile(p, t, x, z, G - 1);
            }
        }
        // Geländer seitlich, vorn offen (dort wird geangelt)
        for (int z = Layout.DECK_Z1; z <= Layout.DECK_Z2 - 2; z++) {
            p.set(Layout.DECK_X1, S, z, "spruce_fence");
            p.set(Layout.DECK_X2, S, z, "spruce_fence");
        }
        Kit.lanternPost(p, Layout.DECK_X1, S, Layout.DECK_Z2 - 1, 2);
        Kit.lanternPost(p, Layout.DECK_X2, S, Layout.DECK_Z2 - 1, 2);
        Kit.bench(p, Layout.DECK_X1 + 1, S, Layout.DECK_Z1, 2, "north", "spruce");
        Kit.bench(p, Layout.DECK_X2 - 2, S, Layout.DECK_Z1, 2, "north", "spruce");
        // Fahnenmast mit weiß-blauem Wimpel
        for (int y = S; y <= S + 5; y++) {
            p.set(Layout.DECK_X2 + 1, y, Layout.DECK_Z1, "spruce_fence");
        }
        pile(p, t, Layout.DECK_X2 + 1, Layout.DECK_Z1, G);
        p.banner(Layout.DECK_X2 + 1, S + 6, Layout.DECK_Z1, "white_banner[rotation=0]", "light_blue:rhombus",
                "light_blue:triangles_top", "light_blue:triangles_bottom", "yellow:border");
        p.reserve(Layout.DECK_X1 - 3, Layout.STEG_Z1 - 1, Layout.DECK_X2 + 3, Layout.DECK_Z2 + 2);
    }

    /** Holzpfahl von {@code top} bis zum Grund des Gewässers. */
    private static void pile(Plan p, Terrain t, int x, int z, int top) {
        Terrain.Column c = t.column(x, z);
        int bottom = c.wet() ? c.bed() : c.ground();
        for (int y = bottom + 1; y <= top; y++) {
            p.set(x, y, z, "spruce_log[axis=y]");
        }
    }

    // ---------- Fischhändler ----------

    private static void merchant(Plan p) {
        int x1 = 8;
        int x2 = 15;
        int z1 = -36;
        int z2 = -31;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                p.set(x, G, z, "spruce_planks");
                for (int y = S; y <= S + 6; y++) {
                    p.set(x, y, z, Plan.AIR);
                }
            }
        }
        // Rückwand mit Regalen, Seitenwände halbhoch
        for (int x = x1; x <= x2; x++) {
            for (int y = S; y <= S + 3; y++) {
                boolean post = x == x1 || x == x2;
                p.set(x, y, z2, post ? "spruce_log[axis=y]" : y == S ? "barrel[facing=north,open=false]"
                        : y == S + 1 ? "spruce_planks" : y == S + 2 ? "barrel[facing=north,open=false]" : "spruce_planks");
            }
        }
        for (int z = z1 + 1; z < z2; z++) {
            p.set(x1, S, z, "spruce_planks");
            p.set(x2, S, z, "spruce_planks");
            p.set(x1, S + 1, z, "spruce_fence");
            p.set(x2, S + 1, z, "spruce_fence");
        }
        for (int y = S; y <= S + 2; y++) {
            p.set(x1, y, z1, "spruce_log[axis=y]");
            p.set(x2, y, z1, "spruce_log[axis=y]");
        }
        // Theke: Fässer, Eis mit Fischen (kommen als Anzeige dazu), Räucherofen
        int cz = z1 + 1;
        String[] counter = {"barrel[facing=up,open=false]", "packed_ice", "packed_ice", "smoker[facing=north,lit=true]",
            "packed_ice", "barrel[facing=up,open=false]"};
        for (int i = 0; i < counter.length; i++) {
            p.set(x1 + 1 + i, S, cz, counter[i]);
        }
        // gestreiftes Vordach (weiß-blau), nach hinten ansteigend
        for (int z = z1 - 1; z <= z2; z++) {
            int y = S + 3 + (z - z1 + 1) / 2;
            for (int x = x1 - 1; x <= x2 + 1; x++) {
                p.set(x, y, z, Math.floorMod(x, 2) == 0 ? "white_wool" : "light_blue_wool");
            }
        }
        Kit.hangingLantern(p, x1 + 1, S + 2, z1 + 1, 0);
        Kit.hangingLantern(p, x2 - 1, S + 2, z1 + 1, 0);
        p.sign(x1 + 2, S + 2, z1 - 1, "spruce_hanging_sign[rotation=8,attached=false,waterlogged=false]",
                "Xaver", "Fische", "Fish", "");
        p.reserve(x1 - 2, z1 - 2, x2 + 2, z2 + 1);
    }

    // ---------- Tafelwand ----------

    private static void boardWall(Plan p) {
        int x = Layout.BOARD_WALL_X;
        for (int z = Layout.BOARD_WALL_Z1; z <= Layout.BOARD_WALL_Z2; z++) {
            boolean post = z == Layout.BOARD_WALL_Z1 || z == Layout.BOARD_WALL_Z2
                    || (z - Layout.BOARD_WALL_Z1) % 4 == 3 && z != Layout.BOARD_WALL_Z2 - 0;
            p.set(x, G, z, "stone_bricks");
            for (int y = S; y <= S + 4; y++) {
                p.set(x, y, z, post ? "spruce_log[axis=y]" : y == S ? "stone_bricks" : "dark_oak_planks");
            }
            p.set(x, S + 5, z, Kit.stairs("spruce", "west", false));
            p.set(x + 1, S + 5, z, Kit.stairs("spruce", "east", true));
            p.set(x - 1, S + 5, z, Kit.stairs("spruce", "west", true));
            p.set(x + 1, G, z, "polished_andesite");
            if (!post) {
                p.set(x + 1, S + 4, z, "spruce_trapdoor[facing=east,half=top,open=false]");
            }
        }
        p.set(x + 1, S, Layout.BOARD_WALL_Z1, "potted_cornflower");
        p.set(x + 1, S, Layout.BOARD_WALL_Z2, "potted_red_tulip");
        p.reserve(x - 2, Layout.BOARD_WALL_Z1 - 1, x + 3, Layout.BOARD_WALL_Z2 + 1);
    }

    // ---------- Lagerfeuer ----------

    private static void campfire(Plan p) {
        int fx = Layout.FIRE_X;
        int fz = Layout.FIRE_Z;
        for (int x = fx - 4; x <= fx + 4; x++) {
            for (int z = fz - 4; z <= fz + 4; z++) {
                double d = Math.hypot(x - fx, z - fz);
                if (d <= 4.4) {
                    p.set(x, G, z, d < 1.6 ? "cobblestone" : Kit.roll(x, G, z, 4) == 0 ? "coarse_dirt" : "gravel");
                }
            }
        }
        p.set(fx, S, fz, "campfire[lit=true,signal_fire=false,facing=north]");
        for (int[] s : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            p.set(fx + s[0], S, fz + s[1], "cobblestone_wall");
        }
        Kit.bench(p, fx - 1, S, fz - 3, 3, "north", "spruce");
        Kit.bench(p, fx - 1, S, fz + 3, 3, "south", "spruce");
        Kit.bench(p, fx - 3, S, fz - 1, 3, "west", "spruce");
        p.set(fx + 3, S, fz, "spruce_log[axis=y]");
        p.set(fx + 3, S, fz - 1, "spruce_log[axis=y]");
        p.set(fx + 3, S, fz + 1, "oak_log[axis=y]");
        p.reserve(fx - 5, fz - 5, fx + 5, fz + 5);
    }

    // ---------- Bootshaus ----------

    private static void boathouse(Plan p, Terrain t) {
        int x1 = -26;
        int x2 = -18;
        int z1 = -33;
        int z2 = -23;
        int sx1 = -23;
        int sx2 = -21;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2 + 1; z++) {
                boolean slip = x >= sx1 && x <= sx2 && z > z1;
                Terrain.Column c = t.column(x, z);
                int bottom = Math.min(c.wet() ? c.bed() : c.ground(), Layout.WATER_Y - 4);
                for (int y = bottom; y <= G; y++) {
                    if (slip) {
                        p.set(x, y, z, y <= Layout.WATER_Y - 4 ? "gravel" : y <= Layout.WATER_Y ? "water" : Plan.AIR);
                    } else if (z <= z2) {
                        p.set(x, y, z, y == G ? "spruce_planks" : "stone_bricks");
                    }
                }
                if (z <= z2) {
                    for (int y = S; y <= S + 9; y++) {
                        p.set(x, y, z, Plan.AIR);
                    }
                }
            }
        }
        // Wände (unten Stein, oben Holz), Bootstor nach Süden zum See
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                if (!edge) {
                    continue;
                }
                boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
                for (int y = S; y <= S + 4; y++) {
                    boolean gate = z == z2 && x >= sx1 - 1 && x <= sx2 + 1 && y <= S + 2;
                    if (gate) {
                        continue;
                    }
                    p.set(x, y, z, corner ? "spruce_log[axis=y]" : y == S + 4 ? "stripped_spruce_log[axis="
                            + (z == z1 || z == z2 ? "x" : "z") + "]" : "spruce_planks");
                }
            }
        }
        for (int z = z1 + 2; z <= z2 - 2; z += 3) {
            p.set(x1, S + 2, z, "glass_pane");
            p.set(x2, S + 2, z, "glass_pane");
        }
        p.set(x2, S, z1 + 3, "spruce_door[facing=east,half=lower,hinge=left,open=false]");
        p.set(x2, S + 1, z1 + 3, "spruce_door[facing=east,half=upper,hinge=left,open=false]");
        Kit.roof(p, x1, z1, x2, z2, S + 5, true, "dark_oak");
        // Netze, Fässer, Laterne
        for (int x = x1 + 1; x <= x1 + 2; x++) {
            p.set(x, S + 3, z1 + 1, "cobweb");
        }
        p.set(x2 - 1, S + 3, z1 + 1, "cobweb");
        p.set(x1 + 1, S, z1 + 1, "barrel[facing=up,open=false]");
        p.set(x1 + 1, S, z1 + 2, "barrel[facing=up,open=false]");
        p.set(x1 + 1, S + 1, z1 + 1, "barrel[facing=up,open=false]");
        p.set(x2 - 1, S, z1 + 1, "chest[facing=south,type=single,waterlogged=false]");
        p.set(x2 - 1, S, z2 - 1, "hay_block[axis=y]");
        Kit.hangingLantern(p, (sx1 + sx2) / 2, S + 4, (z1 + z2) / 2, 1);
        p.reserve(x1 - 2, z1 - 2, x2 + 2, z2 + 2);
    }

    // ---------- Häuser ----------

    private static void houses(Plan p) {
        // Fischerhaus (Tür nach Westen zum Platz), daneben die Räucherei
        Kit.house(p, 18, -54, 26, -47, S, "west", "spruce", "white_terracotta");
        for (int y = S; y <= S + 2; y++) {
            p.set(15, y, -56, "spruce_fence");
            p.set(15, y, -52, "spruce_fence");
        }
        for (int z = -56; z <= -52; z++) {
            p.set(15, S + 3, z, "stripped_spruce_log[axis=z]");
        }
        p.set(14, S, -54, "campfire[lit=true,signal_fire=false,facing=north]");
        p.reserve(12, -58, 16, -50);
        Kit.house(p, -40, -66, -32, -59, S, "south", "dark_oak", "smooth_quartz");
        Kit.house(p, -6, -68, 3, -61, S, "south", "spruce", "calcite");
        Kit.house(p, 30, -68, 38, -61, S, "south", "spruce", "white_terracotta");
        // Gartenzäune mit Blumen
        Kit.fence(p, -42, -57, -30, -57, S, "spruce_fence");
        Kit.flowerBed(p, -39, -58, -34, -58, S);
        Kit.fence(p, 28, -59, 40, -59, S, "spruce_fence");
        Kit.flowerBed(p, 31, -60, 37, -60, S);
    }

    /** Dorflinde, Bänke am Ufer, Heu, Holzstapel. */
    private static void villageGreen(Plan p) {
        Kit.broadTree(p, 14, S, -44, "oak_log", Kit.LEAF_OAK, 4, 4);
        p.reserve(10, -48, 18, -40);
        Kit.bench(p, 13, S, -41, 2, "north", "spruce");
        Kit.bench(p, -12, S, -29, 3, "north", "spruce");
        Kit.bench(p, 18, S, -28, 3, "north", "spruce");
        Kit.broadTree(p, -34, S, -40, "birch_log", Kit.LEAF_BIRCH, 5, 3);
        Kit.broadTree(p, 34, S, -36, "birch_log", Kit.LEAF_BIRCH, 5, 3);
        Kit.broadTree(p, 26, S, -30, "birch_log", Kit.LEAF_BIRCH, 4, 3);
        p.reserve(-37, -43, -31, -37);
        p.reserve(31, -39, 37, -33);
        p.reserve(23, -33, 29, -27);
        // Holzstapel und Heuballen
        for (int z = -60; z <= -57; z++) {
            for (int y = S; y <= S + 1; y++) {
                p.set(22, y, z, "spruce_log[axis=x]");
                p.set(23, y, z, "oak_log[axis=x]");
            }
        }
        p.set(-30, S, -50, "hay_block[axis=y]");
        p.set(-30, S + 1, -50, "hay_block[axis=x]");
        p.set(-31, S, -50, "hay_block[axis=z]");
        p.reserve(-33, -52, -28, -48);
    }

    // ---------- Gebirgsbach ----------

    private static void bach(Plan p, Terrain t) {
        int y = G;
        for (int x = Layout.POOL_DECK_X1; x <= Layout.POOL_DECK_X2; x++) {
            for (int z = Layout.POOL_DECK_Z1; z <= Layout.POOL_DECK_Z2; z++) {
                p.set(x, y, z, z == Layout.POOL_DECK_Z2 ? "stripped_spruce_log[axis=x]" : "spruce_planks");
                for (int up = S; up <= S + 3; up++) {
                    p.set(x, up, z, Plan.AIR);
                }
                pile(p, t, x, z, G - 1);
            }
        }
        for (int z = Layout.POOL_DECK_Z1; z <= Layout.POOL_DECK_Z2 - 1; z++) {
            p.set(Layout.POOL_DECK_X1, S, z, "spruce_fence");
            p.set(Layout.POOL_DECK_X2, S, z, "spruce_fence");
        }
        Kit.lanternPost(p, Layout.POOL_DECK_X1, S, Layout.POOL_DECK_Z2, 2);
        Kit.lanternPost(p, Layout.POOL_DECK_X2, S, Layout.POOL_DECK_Z2, 2);
        Kit.bench(p, -102, S, Layout.POOL_DECK_Z1, 2, "north", "spruce");
        Kit.bench(p, -98, S, Layout.POOL_DECK_Z1, 2, "north", "spruce");
        p.reserve(Layout.POOL_DECK_X1 - 2, Layout.POOL_DECK_Z1 - 2, Layout.POOL_DECK_X2 + 2,
                Layout.POOL_DECK_Z2 + 1);
        // Wasserfall in zwei Stufen: oben aus dem Bach auf die Felsstufe, dann eine Spalte weiter vorn in die Gumpe
        for (int z = Layout.FALL_Z1; z <= Layout.FALL_Z2; z++) {
            for (int fy = Layout.FALL_LEDGE_Y + 1; fy <= Layout.UPPER_WATER_Y; fy++) {
                p.set(Layout.FALL_X, fy, z, "water[level=8]");
            }
            for (int fy = Layout.WATER_Y + 1; fy <= Layout.FALL_LEDGE_Y + 1; fy++) {
                p.set(Layout.FALL_X + 1, fy, z, "water[level=8]");
            }
            p.set(Layout.FALL_X, Layout.FALL_LEDGE_Y, z, z % 2 == 0 ? "mossy_cobblestone" : "moss_block");
        }
        // zerklüftete Wand: Moos, Farn, Ranken, Vorsprünge
        for (int z = Layout.FALL_Z1 - 9; z <= Layout.FALL_Z2 + 9; z++) {
            for (int x = Layout.FALL_X - 4; x <= Layout.FALL_X; x++) {
                if (x == Layout.FALL_X && z >= Layout.FALL_Z1 - 1 && z <= Layout.FALL_Z2 + 1) {
                    continue;
                }
                int top = t.height(x, z);
                if (top <= Layout.GROUND_Y + 1 || t.waterAt(x, z) != null) {
                    continue;
                }
                for (int fy = Layout.WATER_Y + 1; fy <= Math.min(top, Layout.UPPER_WATER_Y + 4); fy++) {
                    int r = Kit.roll(x, fy, z, 7);
                    if (r < 3) {
                        p.set(x, fy, z, r == 0 ? "moss_block" : r == 1 ? "mossy_cobblestone" : "tuff");
                    }
                }
                boolean east = t.height(x + 1, z) < top - 1;
                if (east && Kit.roll(x, top, z, 4) == 0) {
                    p.set(x + 1, top, z, "vine[west=true]");
                    p.set(x + 1, top - 1, z, "vine[west=true]");
                }
                if (t.height(x + 1, z) <= top - 3 && Kit.roll(x, top + 1, z, 3) == 0) {
                    p.set(x, top + 1, z, Kit.roll(z, top, x, 2) == 0 ? "fern" : "moss_carpet");
                }
            }
        }
        // Gumpe: bemooste Felsbrocken am Rand, Farn dazwischen (Wurfbahn vom Steg bleibt frei)
        int[][] stones = {{-108, -5, 2}, {-107, -6, 1}, {-108, 7, 2}, {-106, 9, 1}, {-101, 11, 2}, {-95, 10, 1},
            {-91, 6, 1}};
        for (int[] st : stones) {
            int top = t.height(st[0], st[1]);
            if (t.waterAt(st[0], st[1]) != null) {
                continue;
            }
            for (int dx = 0; dx < st[2]; dx++) {
                for (int dzz = 0; dzz < st[2]; dzz++) {
                    p.set(st[0] + dx, top + 1, st[1] + dzz, (dx + dzz) % 2 == 0 ? "mossy_cobblestone" : "andesite");
                }
            }
            p.set(st[0], top + 1 + (st[2] > 1 ? 1 : 0), st[1], "moss_block");
            p.set(st[0] + st[2], top + 1, st[1], "fern");
        }
        // Marterl (Wegkreuz mit Dach) am Weg zur Gumpe
        int mx = -91;
        int mz = -13;
        p.set(mx, S, mz, "cobblestone_wall");
        p.set(mx, S + 1, mz, "spruce_log[axis=y]");
        p.set(mx, S + 2, mz, "spruce_planks");
        p.set(mx, S + 3, mz, "spruce_slab[type=bottom]");
        p.set(mx - 1, S + 3, mz, Kit.stairs("spruce", "east", false));
        p.set(mx + 1, S + 3, mz, Kit.stairs("spruce", "west", false));
        p.set(mx, S + 2, mz + 1, "spruce_trapdoor[facing=south,half=bottom,open=true]");
        p.set(mx, S + 1, mz + 1, "potted_cornflower");
        p.set(mx, S + 1, mz - 1, "lantern[hanging=false]");
        p.reserve(mx - 1, mz - 1, mx + 1, mz + 1);
    }

    /** Holzbrücke über den Bach (Rundweg ums Westufer). */
    private static void bridge(Plan p) {
        int x1 = -70;
        int x2 = -66;
        int z1 = 7;
        int z2 = 16;
        for (int z = z1; z <= z2; z++) {
            int rise = Math.min(z - z1, z2 - z);
            for (int x = x1 + 1; x <= x2 - 1; x++) {
                if (rise == 0) {
                    p.set(x, S, z, "spruce_slab[type=bottom]");
                } else if (rise == 1) {
                    p.set(x, S, z, "spruce_planks");
                } else {
                    p.set(x, S, z, "spruce_planks");
                    p.set(x, S + 1, z, "spruce_slab[type=bottom]");
                }
            }
            if (rise >= 1) {
                int ry = rise >= 2 ? S + 2 : S + 1;
                p.set(x1, ry, z, "spruce_fence");
                p.set(x2, ry, z, "spruce_fence");
                p.set(x1, ry - 1, z, rise >= 2 ? "spruce_planks" : "spruce_fence");
                p.set(x2, ry - 1, z, rise >= 2 ? "spruce_planks" : "spruce_fence");
            }
        }
        p.reserve(x1 - 1, z1 - 1, x2 + 1, z2 + 1);
    }

    // ---------- Moor ----------

    private static void moor(Plan p) {
        int[][] walk = {{70, -6}, {76, -2}, {80, 2}, {83, 5}};
        for (int i = 1; i < walk.length; i++) {
            int[] a = walk[i - 1];
            int[] b = walk[i];
            int steps = Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1])) * 2;
            for (int s = 0; s <= steps; s++) {
                double fx = a[0] + (b[0] - a[0]) * (double) s / steps;
                double fz = a[1] + (b[1] - a[1]) * (double) s / steps;
                for (int dx = 0; dx <= 1; dx++) {
                    for (int dz = 0; dz <= 1; dz++) {
                        int x = (int) Math.floor(fx) + dx;
                        int z = (int) Math.floor(fz) + dz;
                        p.set(x, S, z, "spruce_slab[type=bottom]");
                        p.set(x, S + 1, z, Plan.AIR);
                        p.set(x, S + 2, z, Plan.AIR);
                    }
                }
            }
        }
        for (int[] w : walk) {
            p.reserve(w[0] - 2, w[1] - 2, w[0] + 3, w[1] + 3);
        }
        for (int i = 0; i < 6; i++) {
            p.reserve(70 + i * 2, -6 + i * 2, 72 + i * 2, -4 + i * 2);
        }
        // Plattform am Weiher
        for (int x = Layout.MOOR_DECK_X1; x <= Layout.MOOR_DECK_X2; x++) {
            for (int z = Layout.MOOR_DECK_Z1; z <= Layout.MOOR_DECK_Z2; z++) {
                p.set(x, S, z, "spruce_slab[type=bottom]");
                p.set(x, G, z, "spruce_fence");
                p.set(x, S + 1, z, Plan.AIR);
                p.set(x, S + 2, z, Plan.AIR);
            }
        }
        for (int x = Layout.MOOR_DECK_X1; x <= Layout.MOOR_DECK_X2 - 1; x++) {
            p.set(x, S + 1, Layout.MOOR_DECK_Z1, "spruce_fence");
            p.set(x, S + 1, Layout.MOOR_DECK_Z2, "spruce_fence");
        }
        Kit.lanternPost(p, Layout.MOOR_DECK_X2, S + 1, Layout.MOOR_DECK_Z1, 2);
        Kit.lanternPost(p, Layout.MOOR_DECK_X2, S + 1, Layout.MOOR_DECK_Z2, 2);
        p.set(Layout.MOOR_DECK_X1, S + 1, Layout.MOOR_DECK_Z1 + 3, Kit.stairs("spruce", "west", false));
        p.set(Layout.MOOR_DECK_X1, S + 1, Layout.MOOR_DECK_Z1 + 4, Kit.stairs("spruce", "west", false));
        p.reserve(Layout.MOOR_DECK_X1 - 1, Layout.MOOR_DECK_Z1 - 2, Layout.MOOR_DECK_X2 + 6, Layout.MOOR_DECK_Z2 + 2);
        // Torfhütte mit Torfstapel
        int x1 = 72;
        int x2 = 77;
        int z1 = 12;
        int z2 = 16;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                p.set(x, G, z, edge ? "mud_bricks" : "packed_mud");
                for (int y = S; y <= S + 2; y++) {
                    p.set(x, y, z, edge ? ((x == x1 || x == x2) && (z == z1 || z == z2) ? "spruce_log[axis=y]"
                            : "mud_bricks") : Plan.AIR);
                }
            }
        }
        p.set(x1 + 2, S, z1, "spruce_door[facing=north,half=lower,hinge=left,open=false]");
        p.set(x1 + 2, S + 1, z1, "spruce_door[facing=north,half=upper,hinge=left,open=false]");
        p.set(x2, S + 1, z1 + 2, "glass_pane");
        Kit.roof(p, x1, z1, x2, z2, S + 3, false, "spruce");
        for (int x = x1 - 1; x <= x2 + 1; x++) {
            if (Kit.roll(x, S + 3, z1 - 1, 2) == 0) {
                p.set(x, S + 4, z1 - 1, "moss_carpet");
            }
        }
        for (int i = 0; i < 4; i++) {
            for (int h = 0; h <= (i < 2 ? 1 : 0); h++) {
                p.set(x2 + 2, S + h, z1 + i, "packed_mud");
            }
        }
        p.reserve(x1 - 2, z1 - 2, x2 + 4, z2 + 2);
    }

    // ---------- Eisloch ----------

    private static void eis(Plan p) {
        int y = Layout.KAR_Y;
        int x1 = Layout.HUT_X1;
        int x2 = Layout.HUT_X2;
        int z1 = Layout.HUT_Z1;
        int z2 = Layout.HUT_Z2;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
                p.set(x, y - 1, z, edge ? "spruce_planks" : "spruce_planks");
                for (int h = 0; h <= 2; h++) {
                    p.set(x, y + h, z, !edge ? Plan.AIR : corner ? "stripped_spruce_log[axis=y]"
                            : h == 2 ? "white_terracotta" : "mangrove_planks");
                }
            }
        }
        p.set(x1, y, z1 + 2, "spruce_door[facing=west,half=lower,hinge=left,open=false]");
        p.set(x1, y + 1, z1 + 2, "spruce_door[facing=west,half=upper,hinge=left,open=false]");
        p.set(x2, y + 1, z1 + 2, "glass_pane");
        p.set(x1 + 2, y + 1, z1, "glass_pane");
        Kit.roof(p, x1, z1, x2, z2, y + 3, true, "spruce");
        for (int z = z1 - 1; z <= z2 + 1; z++) {
            p.set(x1 - 1, y + 4, z, "snow[layers=1]");
            p.set(x2 + 1, y + 4, z, "snow[layers=1]");
        }
        p.set(x2 - 1, y + 3, z2 - 1, "bricks");
        p.set(x2 - 1, y + 4, z2 - 1, "bricks");
        p.set(x2 - 1, y + 5, z2 - 1, "bricks");
        p.set(x2 - 1, y + 6, z2 - 1, "campfire[lit=true,signal_fire=false,facing=north]");
        p.set(x2 - 1, y, z2 - 1, "smoker[facing=west,lit=true]");
        Kit.bench(p, x1 + 1, y, z2 - 1, 2, "south", "spruce");
        Kit.hangingLantern(p, x1 + 2, y + 2, z1 + 2, 0);
        p.reserve(x1 - 2, z1 - 2, x2 + 2, z2 + 2);
        // Hocker und Laternen an den Löchern
        for (int[] h : Layout.ICE_HOLES) {
            p.set(h[0] + 2, y, h[1] - 2, "spruce_log[axis=y]");
            Kit.lanternPost(p, h[0] - 2, y, h[1] - 2, 1);
            p.reserve(h[0] - 3, h[1] - 3, h[0] + 3, h[1] + 3);
        }
        // Eisfall an der Karwand (gefrorener Wasserfall)
        int fx = 92;
        int fz = 117;
        for (int fy = y; fy <= y + 12; fy++) {
            p.set(fx, fy, fz, fy % 3 == 0 ? "blue_ice" : "packed_ice");
            p.set(fx + 1, fy, fz, "packed_ice");
            if (fy < y + 8) {
                p.set(fx - 1, fy, fz, "ice");
            }
        }
    }

    // ---------- Seekapelle ----------

    private static void chapel(Plan p, Terrain t) {
        int x1 = -16;
        int x2 = -8;
        int z1 = 57;
        int z2 = 66;
        int y = S;
        String wall = "white_concrete";
        for (int x = x1 - 1; x <= x2 + 1; x++) {
            for (int z = z1 - 4; z <= z2 + 1; z++) {
                for (int fy = Math.min(t.height(x, z), G) - 2; fy <= G; fy++) {
                    p.set(x, fy, z, fy == G ? "polished_andesite" : "stone_bricks");
                }
                for (int fy = y; fy <= y + 4; fy++) {
                    p.set(x, fy, z, Plan.AIR);
                }
            }
        }
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                for (int h = 0; h <= 5; h++) {
                    if (edge) {
                        p.set(x, y + h, z, h == 0 ? "stone_bricks" : wall);
                    }
                }
            }
        }
        for (int z = z1 + 2; z <= z2 - 2; z += 3) {
            for (int h = 2; h <= 3; h++) {
                p.set(x1, y + h, z, "light_blue_stained_glass_pane");
                p.set(x2, y + h, z, "light_blue_stained_glass_pane");
            }
        }
        Kit.roof(p, x1, z1, x2, z2, y + 6, true, "brick");
        // Turm vorn mit Zwiebelhaube
        int tx1 = -14;
        int tx2 = -10;
        int tz1 = 53;
        int tz2 = 57;
        for (int x = tx1; x <= tx2; x++) {
            for (int z = tz1; z <= tz2; z++) {
                boolean edge = x == tx1 || x == tx2 || z == tz1 || z == tz2;
                for (int h = 0; h <= 10; h++) {
                    if (edge) {
                        p.set(x, y + h, z, h == 0 ? "stone_bricks" : h == 10 ? "stone_brick_slab[type=top]" : wall);
                    } else {
                        p.set(x, y + h, z, h == 10 ? "bricks" : Plan.AIR);
                    }
                }
            }
        }
        p.set(-12, y, tz1, "spruce_door[facing=north,half=lower,hinge=left,open=false]");
        p.set(-12, y + 1, tz1, "spruce_door[facing=north,half=upper,hinge=left,open=false]");
        p.set(-12, y + 2, tz1, "spruce_planks");
        for (int h = 7; h <= 8; h++) {
            p.set(-12, y + h, tz1, "spruce_fence");
            p.set(tx1, y + h, 55, "spruce_fence");
            p.set(tx2, y + h, 55, "spruce_fence");
        }
        p.set(-12, y + 8, 55, "bell[facing=north,attachment=ceiling]");
        int cx = -12;
        int cz = 55;
        int dome = y + 11;
        int[] radius = {2, 2, 2, 1, 1, 0, 0};
        for (int i = 0; i < radius.length; i++) {
            int r = radius[i];
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (r == 2 && Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                        continue;
                    }
                    p.set(cx + dx, dome + i, cz + dz, i == 1 ? "bricks" : i == 6 ? "brick_wall" : "bricks");
                }
            }
        }
        p.set(cx, dome + 7, cz, "gold_block");
        p.set(cx, dome + 8, cz, "lightning_rod[facing=up]");
        // innen: Bänke, Altar, Kerzen
        for (int z = z1 + 2; z <= z2 - 3; z += 2) {
            Kit.bench(p, x1 + 2, y, z, 2, "south", "spruce");
            Kit.bench(p, x2 - 3, y, z, 2, "south", "spruce");
        }
        p.set(-12, y, z2 - 1, "quartz_block");
        p.set(-13, y, z2 - 1, "quartz_stairs[facing=east,half=bottom,shape=straight]");
        p.set(-11, y, z2 - 1, "quartz_stairs[facing=west,half=bottom,shape=straight]");
        p.set(-12, y + 1, z2 - 1, "candle[candles=3,lit=true]");
        Kit.hangingLantern(p, -12, y + 5, 61, 1);
        p.reserve(x1 - 3, tz1 - 3, x2 + 3, z2 + 3);
        // Blumenwiese vor der Kapelle
        Kit.flowerBed(p, -17, 49, -15, 52, y);
        Kit.flowerBed(p, -9, 49, -7, 52, y);
    }

    // ---------- Gipfelkreuz ----------

    private static void summitCross(Plan p, Terrain t) {
        // höchster Punkt rund um den Hauptgipfel im Süden (16/158, durch die Verbiegung etwas verschoben)
        int bx = 16;
        int bz = 158;
        int by = 0;
        for (int x = -12; x <= 44; x++) {
            for (int z = 130; z <= 186; z++) {
                int h = t.height(x, z);
                if (h > by) {
                    by = h;
                    bx = x;
                    bz = z;
                }
            }
        }
        for (int h = 1; h <= 6; h++) {
            p.set(bx, by + h, bz, "dark_oak_log[axis=y]");
        }
        p.set(bx - 1, by + 4, bz, "dark_oak_log[axis=x]");
        p.set(bx + 1, by + 4, bz, "dark_oak_log[axis=x]");
        p.set(bx, by + 7, bz, "dark_oak_slab[type=bottom]");
        p.reserve(bx - 2, bz - 2, bx + 2, bz + 2);
    }
}
