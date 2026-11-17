package de.moinmorni.angelkoenig.fish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class FishModelsTest {

    /** Liste im Repo = Arten im Code (das Netzwerk-Repo hat eine Kopie davon). */
    @Test
    void listMatchesSpecies() throws IOException {
        List<String> file = Files.readAllLines(Path.of("docs/fisch-modelle.txt")).stream()
                .map(String::trim).filter(l -> !l.isEmpty() && !l.startsWith("#")).toList();
        assertEquals(FishModels.lines(), file, "docs/fisch-modelle.txt nachziehen (und die Kopie im Netzwerk-Repo)");
    }

    @Test
    void namesAreUniqueAndValid() {
        var seen = new HashSet<String>();
        for (Species s : Species.values()) {
            String n = FishModels.name(s);
            assertTrue(seen.add(n), n);
            // Namensraum:Pfad wie ein Minecraft-Schlüssel (klein, ohne Leerzeichen)
            assertTrue(n.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"), n);
        }
    }
}
