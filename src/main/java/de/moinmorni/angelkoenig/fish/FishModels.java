package de.moinmorni.angelkoenig.fish;

import java.util.Arrays;
import java.util.List;

/**
 * Eigenes Aussehen der Fische: Jeder Fang trägt den Namen seiner Art als {@code custom_model_data} (Text), z. B.
 * {@code moinmorni:fisch/hecht}. Das Angelkönig-Paket des MoinMorni-Netzwerks zeigt für genau diese Namen die eigene
 * Zeichnung; ohne Paket (ohne Netzwerk, abgelehnt, ältere Clients, Bedrock) bleibt der Minecraft-Gegenstand aus
 * {@link Species#item()} – nichts sieht kaputt aus.
 *
 * <p>Die Liste steht auch in {@code docs/fisch-modelle.txt} (Name und Gegenstand je Zeile) und im Netzwerk-Repo
 * ({@code packages/shared/src/angel-fische.ts}); Tests in beiden Repos halten sie gleich.
 */
public final class FishModels {

    public static final String PREFIX = "moinmorni:fisch/";

    private FishModels() {
    }

    /** Modell-Name einer Art. */
    public static String name(Species s) {
        return PREFIX + s.id();
    }

    /** Zeilen der Liste: „Name Gegenstand“, in der Reihenfolge der Arten. */
    public static List<String> lines() {
        return Arrays.stream(Species.values()).map(s -> name(s) + " " + s.item()).toList();
    }
}
