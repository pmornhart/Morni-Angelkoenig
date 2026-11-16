package net.morni.angelkoenig.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Alle Blöcke, die in der Welt vorkommen, mit einer Nummer (0 = Luft). Wird einmal beim Start gefüllt (Gelände,
 * Bäume, Bauplan) und danach nur noch gelesen – auch von den Erzeuger-Threads.
 */
public final class Palette {

    private final Map<String, Short> ids = new HashMap<>();
    private final List<String> specs = new ArrayList<>();
    private boolean frozen;

    public Palette() {
        id(Plan.AIR);
    }

    /** Nummer eines Blocks (legt ihn an, solange die Palette noch nicht fest ist). */
    public short id(String spec) {
        Short id = ids.get(spec);
        if (id != null) {
            return id;
        }
        if (frozen) {
            throw new IllegalStateException("Block nicht in der Palette: " + spec);
        }
        if (specs.size() >= Short.MAX_VALUE) {
            throw new IllegalStateException("Zu viele verschiedene Blöcke");
        }
        short n = (short) specs.size();
        ids.put(spec, n);
        specs.add(spec);
        return n;
    }

    /** Ab jetzt keine neuen Blöcke mehr (die Erzeuger-Threads lesen nur). */
    public void freeze() {
        frozen = true;
    }

    public String spec(short id) {
        return specs.get(id);
    }

    public List<String> specs() {
        return List.copyOf(specs);
    }

    public int size() {
        return specs.size();
    }
}
