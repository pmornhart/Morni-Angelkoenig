package net.morni.angelkoenig.listen;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import net.morni.angelkoenig.AngelkoenigPlugin;
import java.util.Set;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

/**
 * Schutz des Tals: niemand baut oder zerstört etwas, nichts brennt, wächst, schmilzt oder verfällt, Wasser bleibt, wo
 * es ist, keine fremden Tiere oder Monster, Boote bleiben ganz, Blitze nur zum Anschauen. Türen und Tore darf man
 * öffnen. Ausnahme: Admins im Kreativmodus.
 */
public final class GuardListener implements Listener {

    private static final Set<CreatureSpawnEvent.SpawnReason> ALLOWED = Set.of(CreatureSpawnEvent.SpawnReason.CUSTOM,
            CreatureSpawnEvent.SpawnReason.COMMAND, CreatureSpawnEvent.SpawnReason.SPAWNER_EGG,
            CreatureSpawnEvent.SpawnReason.DEFAULT);

    private final AngelkoenigPlugin plugin;

    public GuardListener(AngelkoenigPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean ours(World w) {
        return w == plugin.world();
    }

    /** Im Tal wird nicht gekocht: keine „Rezept freigeschaltet“-Hinweise, wenn ein Fisch im Korb landet. */
    @EventHandler(ignoreCancelled = true)
    public void onRecipe(PlayerRecipeDiscoverEvent e) {
        if (ours(e.getPlayer().getWorld())) {
            e.setCancelled(true);
        }
    }

    /** Keine Minecraft-Fortschritte im Tal (z. B. „Anglerglück“ beim ersten Fang) – das Fischbuch zählt hier. */
    @EventHandler(ignoreCancelled = true)
    public void onAdvancement(PlayerAdvancementCriterionGrantEvent e) {
        if (ours(e.getPlayer().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (ours(e.getBlock().getWorld()) && !plugin.builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (ours(e.getBlock().getWorld()) && !plugin.builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!ours(p.getWorld()) || plugin.builder(p)) {
            return;
        }
        if (e.getAction() == Action.PHYSICAL) {
            e.setCancelled(true);
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null) {
            org.bukkit.Material m = e.getClickedBlock().getType();
            boolean open = org.bukkit.Tag.DOORS.isTagged(m) || org.bukkit.Tag.FENCE_GATES.isTagged(m)
                    || m == org.bukkit.Material.BELL;
            if (!open) {
                e.setUseInteractedBlock(Event.Result.DENY);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent e) {
        if (ours(e.getBlock().getWorld()) && !plugin.builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucket(PlayerBucketFillEvent e) {
        if (ours(e.getBlock().getWorld()) && !plugin.builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        if (ours(e.getBlock().getWorld()) && e.getCause() != BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFade(BlockFadeEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onForm(BlockFormEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        if (ours(e.getEntity().getWorld())) {
            e.blockList().clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.blockList().clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onChange(EntityChangeBlockEvent e) {
        if (ours(e.getBlock().getWorld()) && !(e.getEntity() instanceof Player p && plugin.builder(p))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHanging(HangingBreakEvent e) {
        if (ours(e.getEntity().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (ours(e.getLocation().getWorld()) && !ALLOWED.contains(e.getSpawnReason())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFluid(org.bukkit.event.block.FluidLevelChangeEvent e) {
        if (ours(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPhysics(org.bukkit.event.block.BlockPhysicsEvent e) {
        if (ours(e.getBlock().getWorld()) && e.getChangedType() == org.bukkit.Material.SUGAR_CANE) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicle(org.bukkit.event.vehicle.VehicleDamageEvent e) {
        if (ours(e.getVehicle().getWorld()) && !(e.getAttacker() instanceof Player p && plugin.builder(p))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicle(org.bukkit.event.vehicle.VehicleDestroyEvent e) {
        if (ours(e.getVehicle().getWorld()) && !(e.getAttacker() instanceof Player p && plugin.builder(p))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLightning(org.bukkit.event.weather.LightningStrikeEvent e) {
        if (ours(e.getWorld()) && e.getCause() == org.bukkit.event.weather.LightningStrikeEvent.Cause.WEATHER) {
            e.setCancelled(true);
            org.bukkit.Location l = e.getLightning().getLocation();
            if (Math.hypot(l.getX(), l.getZ() + 20) > 90) {
                e.getWorld().strikeLightningEffect(l);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTransform(org.bukkit.event.entity.EntityTransformEvent e) {
        if (ours(e.getEntity().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onWeather(WeatherChangeEvent e) {
        if (ours(e.getWorld()) && e.getCause() != WeatherChangeEvent.Cause.PLUGIN
                && e.getCause() != WeatherChangeEvent.Cause.COMMAND) {
            e.setCancelled(true);
        }
    }
}
