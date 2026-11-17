package de.moinmorni.angelkoenig.fish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClothingModelsTest {

    /** Liste im Repo = Kleidung im Code (das Netzwerk-Repo hat eine Kopie davon). */
    @Test
    void listMatchesCosmetics() throws IOException {
        List<String> file = Files.readAllLines(Path.of("docs/kleidung-modelle.txt")).stream()
                .map(String::trim).filter(l -> !l.isEmpty() && !l.startsWith("#")).toList();
        assertEquals(ClothingModels.lines(), file,
                "docs/kleidung-modelle.txt nachziehen (und die Kopie im Netzwerk-Repo)");
    }

    @Test
    void namesAreUniqueAndValid() {
        var seen = new HashSet<String>();
        for (Cosmetic c : Cosmetic.values()) {
            String n = ClothingModels.name(c);
            assertTrue(seen.add(n), n);
            assertTrue(n.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"), n);
            assertEquals(ClothingModels.NAMESPACE + ":" + ClothingModels.assetPath(c), n);
        }
    }

    @Test
    void hatsAreTheHeadPieces() {
        assertTrue(ClothingModels.hat(Cosmetic.FISCHERHUT));
        assertTrue(ClothingModels.hat(Cosmetic.KOENIGSKRONE));
        assertFalse(ClothingModels.hat(Cosmetic.ANGLERWESTE));
        assertFalse(ClothingModels.hat(Cosmetic.GUMMISTIEFEL));
    }
}
