package de.moinmorni.angelkoenig.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackOfferTest {

    private static JsonObject json(String text) {
        return new Gson().fromJson(text, JsonObject.class);
    }

    @Test
    void readsUrlHashAndId() {
        var p = PackOffer.parse(json("""
                {"url": "https://moinmorni.net/api/public/packs/angelkoenig.zip",
                 "sha1": "0BA2C5B0AB7A5C43D665FBF26E2C68B83BF1DDEE",
                 "id": "6d6f696e-6d6f-726e-692d-616e67656c6b", "prompt": "Unser Paket"}"""));
        assertNotNull(p);
        assertEquals(UUID.fromString("6d6f696e-6d6f-726e-692d-616e67656c6b"), p.id());
        assertEquals("0ba2c5b0ab7a5c43d665fbf26e2c68b83bf1ddee", p.sha1());
        assertNotNull(p.prompt());
    }

    @Test
    void withoutUrlOrWithBadHashThereIsNoPack() {
        assertNull(PackOffer.parse(json("{}")));
        assertNull(PackOffer.parse(json("{\"url\": \"\", \"sha1\": \"0ba2c5b0ab7a5c43d665fbf26e2c68b83bf1ddee\"}")));
        assertNull(PackOffer.parse(json("{\"url\": \"https://x.de/p.zip\", \"sha1\": \"kaputt\"}")));
        assertNull(PackOffer.parse(json(
                "{\"url\": \"file:///etc/passwd\", \"sha1\": \"0ba2c5b0ab7a5c43d665fbf26e2c68b83bf1ddee\"}")));
    }

    @Test
    void missingOrBrokenFileMeansNoPack(@TempDir Path dir) throws IOException {
        assertNull(PackOffer.read(dir.resolve(PackOffer.FILE)));
        Files.writeString(dir.resolve(PackOffer.FILE), "kein json");
        assertNull(PackOffer.read(dir.resolve(PackOffer.FILE)));
    }

    @Test
    void floodgatePlayersAreBedrock() {
        assertTrue(ClothingLooks.bedrock(new UUID(0, 42)));
        assertFalse(ClothingLooks.bedrock(UUID.fromString("180f3b9a-eaa0-3d4d-bf3d-e043a181f23f")));
    }
}
