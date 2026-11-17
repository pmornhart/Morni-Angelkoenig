package de.moinmorni.angelkoenig.fish;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Eigenes Aussehen der Kleidung: Jedes Stück trägt seinen Namen als {@code custom_model_data} (Text), z. B.
 * {@code moinmorni:kleidung/fischerhut}. Das Angelkönig-Paket des MoinMorni-Netzwerks zeigt dafür im Inventar das
 * eigene Modell; am Körper zeigt Minecraft Hüte als 3D-Modell, wenn das Stück kein Rüstungs-Aussehen hat, und die
 * übrigen Stücke mit dem Ausrüstungs-Aussehen gleichen Namens (siehe {@code play/ClothingLooks}). Ohne Paket bleibt der
 * Minecraft-Gegenstand aus {@link Cosmetic#item()}.
 *
 * <p>Die Liste steht auch in {@code docs/kleidung-modelle.txt} (Name, Gegenstand, Platz je Zeile) und im Netzwerk-Repo
 * ({@code packages/shared/src/angel-kleidung.ts}); Tests in beiden Repos halten sie gleich.
 */
public final class ClothingModels {

    public static final String NAMESPACE = "moinmorni";
    public static final String PREFIX = NAMESPACE + ":kleidung/";

    private ClothingModels() {
    }

    /** Modell-Name eines Stücks (auch Name des Ausrüstungs-Aussehens am Körper). */
    public static String name(Cosmetic c) {
        return PREFIX + c.id();
    }

    /** Pfad des Ausrüstungs-Aussehens (ohne Namensraum), z. B. {@code kleidung/anglerweste}. */
    public static String assetPath(Cosmetic c) {
        return "kleidung/" + c.id();
    }

    /** Hüte zeigt Minecraft als 3D-Modell auf dem Kopf (ohne Rüstungs-Aussehen); alles andere trägt ein eigenes Muster. */
    public static boolean hat(Cosmetic c) {
        return c.slot() == Cosmetic.Slot.HEAD;
    }

    /** Zeilen der Liste: „Name Gegenstand Platz“, in der Reihenfolge des Kleiderschranks. */
    public static List<String> lines() {
        return Arrays.stream(Cosmetic.values())
                .map(c -> name(c) + " " + c.item() + " " + c.slot().name().toLowerCase(Locale.ROOT))
                .toList();
    }
}
