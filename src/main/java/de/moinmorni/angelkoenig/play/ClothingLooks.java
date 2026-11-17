package de.moinmorni.angelkoenig.play;

import de.moinmorni.angelkoenig.AngelkoenigPlugin;
import de.moinmorni.angelkoenig.data.PlayerData;
import de.moinmorni.angelkoenig.fish.Cosmetic;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.packs.ResourcePack;

/**
 * Eigenes Aussehen der Kleidung am Körper – je Betrachter. Wer das Angelkönig-Paket geladen hat, sieht Hüte als
 * 3D-Modell auf dem Kopf (Stück ohne Rüstungs-Aussehen) und Weste, Jacke, Hosen, Stiefel mit eigenem Muster
 * (Rüstungs-Aussehen {@code moinmorni:kleidung/<stück>}); wer es nicht hat (abgelehnt, ältere Version, Bedrock), sieht
 * die gewohnte Lederkleidung – sonst sähe er einen flachen Helm vor der Stirn bzw. gar nichts.
 *
 * <p>Der Träger selbst bekommt das Stück passend zu seinem Paket ins Inventar ({@link Hotbar#armor}). Allen anderen
 * schickt Angelkönig, wo ihr Paket anders ist als das des Trägers, das passende Stück nur zum Ansehen
 * ({@link Player#sendEquipmentChange}) – beim Sichtbarwerden, nach jedem Umziehen und alle zwei Sekunden.
 */
public final class ClothingLooks implements Listener {

    private static final Set<UUID> LOADED = ConcurrentHashMap.newKeySet();
    private static final long AFTER_BROADCAST = 2L;
    private static final long EVERY = 40L;
    private static ClothingLooks instance;
    /** Kennung des Pakets, das {@link PackOffer} anbietet. */
    static volatile UUID ownPack;
    private final AngelkoenigPlugin plugin;

    public ClothingLooks(AngelkoenigPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, EVERY, EVERY);
    }

    /** Hat der Spieler das Angelkönig-Paket geladen (und spielt Java)? */
    public static boolean sees(Player p) {
        return LOADED.contains(p.getUniqueId()) && !bedrock(p.getUniqueId());
    }

    /** Floodgate gibt Bedrock-Spielern eine UUID mit lauter Nullen vorn (und hat dafür eine eigene Abfrage). */
    static boolean bedrock(UUID id) {
        if (id.getMostSignificantBits() == 0) {
            return true;
        }
        try {
            Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object floodgate = api.getMethod("getInstance").invoke(null);
            return (Boolean) api.getMethod("isFloodgatePlayer", UUID.class).invoke(floodgate, id);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return false;
        }
    }

    /** Nach dem Umziehen: alle Betrachter gleich (nicht erst in zwei Sekunden) richtigstellen. */
    public static void changed(Player wearer) {
        ClothingLooks self = instance;
        if (self != null) {
            Bukkit.getScheduler().runTaskLater(self.plugin, () -> self.showToAll(wearer), AFTER_BROADCAST);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPack(PlayerResourcePackStatusEvent e) {
        if (!ours(e.getID())) {
            return;
        }
        Player p = e.getPlayer();
        boolean before = sees(p);
        switch (e.getStatus()) {
            case SUCCESSFULLY_LOADED -> LOADED.add(p.getUniqueId());
            case DECLINED, FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD, DISCARDED -> LOADED.remove(p.getUniqueId());
            default -> {
                return;
            }
        }
        if (before != sees(p)) {
            redress(p);
        }
    }

    /** Antwort auf das Angebot beim Verbinden ({@link PackOffer}) – vor dem Betreten, darum nur merken. */
    static void answered(UUID player, boolean loaded) {
        if (loaded) {
            LOADED.add(player);
        } else {
            LOADED.remove(player);
        }
    }

    /** Der Spieler sieht jetzt anders: eigene Kleidung tauschen, allen anderen und ihm selbst neu zeigen. */
    private void redress(Player p) {
        if (p.isOnline() && p.getWorld().equals(plugin.world())) {
            Hotbar.armor(p, plugin.data(p), plugin.lang(p));
            Bukkit.getScheduler().runTaskLater(plugin, () -> showAllTo(p), AFTER_BROADCAST);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        LOADED.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrack(PlayerTrackEntityEvent e) {
        if (e.getEntity() instanceof Player wearer) {
            Player viewer = e.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> show(viewer, wearer), AFTER_BROADCAST);
        }
    }

    /** Unser Paket (aus paket.json oder der server.properties)? Ohne beides zählt jedes. */
    private static boolean ours(UUID id) {
        UUID own = ownPack;
        ResourcePack server = Bukkit.getServer().getServerResourcePack();
        if (id == null || (own == null && server == null)) {
            return true;
        }
        return id.equals(own) || (server != null && server.getId().equals(id));
    }

    private void refreshAll() {
        for (Player wearer : plugin.world().getPlayers()) {
            showToAll(wearer);
        }
    }

    private void showToAll(Player wearer) {
        if (!wearer.isOnline()) {
            return;
        }
        for (Player viewer : wearer.getWorld().getPlayers()) {
            show(viewer, wearer);
        }
    }

    private void showAllTo(Player viewer) {
        if (!viewer.isOnline()) {
            return;
        }
        for (Player wearer : viewer.getWorld().getPlayers()) {
            show(viewer, wearer);
        }
    }

    /** Dem Betrachter die Kleidung des Trägers in seiner Fassung schicken (nur wenn sie sich unterscheidet). */
    private void show(Player viewer, Player wearer) {
        if (viewer.equals(wearer) || !viewer.isOnline() || !wearer.isOnline() || !viewer.canSee(wearer)
                || !wearer.getWorld().equals(plugin.world()) || !viewer.getWorld().equals(wearer.getWorld())) {
            return;
        }
        boolean look = sees(viewer);
        if (look == sees(wearer)) {
            return;
        }
        PlayerData d = plugin.data(wearer);
        String lang = plugin.lang(wearer);
        Map<EquipmentSlot, ItemStack> items = new EnumMap<>(EquipmentSlot.class);
        for (Cosmetic.Slot slot : Cosmetic.Slot.values()) {
            Cosmetic c = d.worn(slot);
            if (c != null) {
                items.put(Hotbar.slot(slot), Hotbar.worn(c, lang, look));
            }
        }
        if (!items.isEmpty()) {
            viewer.sendEquipmentChange(wearer, items);
        }
    }
}
