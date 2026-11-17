package de.moinmorni.angelkoenig.play;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.packs.ResourcePack;

/**
 * Das Angelkönig-Paket (eigene Fische, Hüte und Kleidung) bietet Angelkönig selbst an – beim Verbinden, bevor man das
 * Tal betritt, genau einmal. So weiß Angelkönig, wer es geladen hat ({@link ClothingLooks}); beim Paket aus der
 * server.properties erfährt ein Plugin das nicht.
 *
 * <p>Woher: {@code plugins/MorniAngelkoenig/paket.json} mit {@code url}, {@code sha1}, {@code id} (das schreibt das
 * MoinMorni-Netzwerk) – oder von Hand. Angelkönig legt beim Start {@code paket-angebot} an: daran erkennt das
 * Netzwerk, dass es das Paket nicht mehr in die server.properties schreiben muss. Steht dasselbe Paket trotzdem in der
 * server.properties, bietet Angelkönig es nicht noch einmal an (sonst lädt der Client es zweimal).
 */
public final class PackOffer implements Listener {

    /** Datei mit dem Paket (vom Netzwerk). */
    public static final String FILE = "paket.json";
    /** Zeichen fürs Netzwerk: diese Fassung bietet das Paket selbst an. */
    public static final String MARKER = "paket-angebot";
    /** So lange wartet das Verbinden höchstens auf Herunterladen und Laden. */
    private static final long WAIT_SECONDS = 90;
    private static final int SHA1_HEX = 40;

    /** Paket: Kennung, Adresse, SHA-1 (hex) und Frage an die Spieler. */
    public record Pack(UUID id, URI url, String sha1, Component prompt) {
    }

    private final Path folder;
    private final Logger log;
    private volatile Pack pack;

    public PackOffer(Path folder, Logger log) {
        this.folder = folder;
        this.log = log;
        writeMarker();
        reload();
    }

    /** Paket aus {@code paket.json} lesen (fehlt die Datei oder ist sie leer: kein Paket). */
    public void reload() {
        pack = read(folder.resolve(FILE));
        ClothingLooks.ownPack = pack == null ? null : pack.id();
        if (pack != null) {
            log.info("Angelkönig-Paket: " + pack.url() + " (biete ich beim Verbinden an)");
        }
    }

    public Pack pack() {
        return pack;
    }

    static Pack read(Path file) {
        try {
            if (!Files.isRegularFile(file)) {
                return null;
            }
            JsonObject o = new Gson().fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            return o == null ? null : parse(o);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    static Pack parse(JsonObject o) {
        String url = o.has("url") ? o.get("url").getAsString().trim() : "";
        String sha1 = o.has("sha1") ? o.get("sha1").getAsString().trim().toLowerCase(java.util.Locale.ROOT) : "";
        String id = o.has("id") ? o.get("id").getAsString().trim() : "";
        if (url.isEmpty() || !(url.startsWith("https://") || url.startsWith("http://"))
                || !sha1.matches("[0-9a-f]{" + SHA1_HEX + "}")) {
            return null;
        }
        UUID uuid = id.isEmpty() ? UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)) : UUID.fromString(id);
        String prompt = o.has("prompt") ? o.get("prompt").getAsString() : "";
        Component text = prompt.isBlank() ? null : Component.text(prompt, NamedTextColor.AQUA);
        return new Pack(uuid, URI.create(url), sha1, text);
    }

    private void writeMarker() {
        try {
            Files.createDirectories(folder);
            Files.writeString(folder.resolve(MARKER),
                    "Angelkönig bietet das Paket aus paket.json selbst an (beim Verbinden).\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warning("Konnte " + MARKER + " nicht schreiben: " + e.getMessage());
        }
    }

    /** Läuft beim Verbinden (eigener Faden, das Verbinden wartet): Paket anbieten und auf die Antwort warten. */
    @EventHandler
    public void onConfigure(AsyncPlayerConnectionConfigureEvent e) {
        Pack p = pack;
        PlayerConfigurationConnection c = e.getConnection();
        UUID player = c.getProfile().getId();
        if (p == null || player == null || ClothingLooks.bedrock(player) || sameAsServer(p)) {
            return;
        }
        CompletableFuture<ResourcePackStatus> answer = new CompletableFuture<>();
        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(ResourcePackInfo.resourcePackInfo(p.id(), p.url(), p.sha1()))
                .replace(false)
                .required(false)
                .prompt(p.prompt())
                .callback((id, status, audience) -> {
                    if (!status.intermediate()) {
                        answer.complete(status);
                    }
                })
                .build();
        c.getAudience().sendResourcePacks(request);
        try {
            ResourcePackStatus status = answer.get(WAIT_SECONDS, TimeUnit.SECONDS);
            ClothingLooks.answered(player, status == ResourcePackStatus.SUCCESSFULLY_LOADED);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException ex) {
            ClothingLooks.answered(player, false);
        }
    }

    /** Steht genau dieses Paket schon in der server.properties, schickt Paper es selbst. */
    private static boolean sameAsServer(Pack p) {
        ResourcePack server = Bukkit.getServer().getServerResourcePack();
        return server != null && p.id().equals(server.getId());
    }
}
