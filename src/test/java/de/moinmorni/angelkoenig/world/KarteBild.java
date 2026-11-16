package de.moinmorni.angelkoenig.world;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Blick von oben auf das Tal als Bild ({@code target/karte.png}) – zum schnellen Prüfen beim Bauen. Läuft nur mit
 * {@code mvn test -Dkarte=true -Dtest=KarteBild}.
 */
class KarteBild {

    @Test
    void draw() throws IOException {
        if (!Boolean.getBoolean("karte")) {
            return;
        }
        Terrain t = new Terrain();
        Plan plan = Valley.build(t);
        int ext = Layout.EXTENT;
        int size = ext * 2;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int x = -ext; x < ext; x++) {
            for (int z = -ext; z < ext; z++) {
                Terrain.Column c = t.column(x, z);
                int rgb;
                String top = plan.topAt(x, z);
                if (top != null) {
                    rgb = 0x8B5A2B;
                    if (top.contains("water")) {
                        rgb = 0x3F76E4;
                    } else if (top.contains("cobble") || top.contains("andesite") || top.contains("stone")) {
                        rgb = 0x9A9A9A;
                    } else if (top.contains("dirt_path") || top.contains("gravel")) {
                        rgb = 0xB49A6A;
                    }
                } else if (c.wet()) {
                    int depth = c.waterTop() - c.bed();
                    rgb = c.water().frozen() ? 0xA6D0F5 : shade(0x2E64C8, 1.15 - depth * 0.05);
                } else if (t.onPath(x, z)) {
                    rgb = 0xC8A870;
                } else {
                    int h = c.ground();
                    int base = h >= 150 ? 0xF4F4F4 : h >= 118 ? 0x8C8C8C : t.inMoor(x, z) ? 0x5E6B3A
                            : t.inKar(x, z) ? 0xE8EEF2 : 0x5DA03C;
                    rgb = shade(base, 0.55 + 0.45 * Math.min(1, (h - 60) / 130.0) + slopeShade(t, x, z));
                }
                if (Math.abs(x) == Layout.BORDER || Math.abs(z) == Layout.BORDER) {
                    rgb = 0xFF0000;
                }
                img.setRGB(x + ext, z + ext, rgb);
            }
        }
        File out = new File("target/karte.png");
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }

    private static double slopeShade(Terrain t, int x, int z) {
        return (t.height(x - 1, z - 1) - t.height(x + 1, z + 1)) * 0.03;
    }

    private static int shade(int rgb, double f) {
        int r = clamp((int) (((rgb >> 16) & 255) * f));
        int g = clamp((int) (((rgb >> 8) & 255) * f));
        int b = clamp((int) ((rgb & 255) * f));
        return (r << 16) | (g << 8) | b;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
