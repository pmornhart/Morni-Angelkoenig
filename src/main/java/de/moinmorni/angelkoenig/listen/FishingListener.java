package de.moinmorni.angelkoenig.listen;

import de.moinmorni.angelkoenig.AngelkoenigPlugin;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerFishEvent;

/**
 * Angeln: beim Auswerfen die Wartezeit der Angel, beim Fang der eigene Fisch statt der normalen Beute. Der Haken
 * zieht weder Leute noch Figuren heran.
 */
public final class FishingListener implements Listener {

    private final AngelkoenigPlugin plugin;

    public FishingListener(AngelkoenigPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        Player p = e.getPlayer();
        if (p.getWorld() != plugin.world()) {
            return;
        }
        switch (e.getState()) {
            case FISHING -> plugin.angling().cast(p, e.getHook());
            case CAUGHT_FISH -> plugin.angling().caught(e);
            case CAUGHT_ENTITY -> {
                e.setCancelled(true);
                e.getHook().remove();
            }
            default -> {
            }
        }
    }

    /** Der Schwimmer bleibt nicht an Leuten, Figuren, Booten oder Tafeln hängen. */
    @EventHandler(ignoreCancelled = true)
    public void onHit(ProjectileHitEvent e) {
        if (e.getEntity() instanceof FishHook && e.getHitEntity() != null && e.getEntity().getWorld() == plugin.world()) {
            e.setCancelled(true);
        }
    }
}
