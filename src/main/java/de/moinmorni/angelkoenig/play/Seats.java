package de.moinmorni.angelkoenig.play;

import de.moinmorni.angelkoenig.AngelkoenigPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Sitzen auf Bänken (Treppen und Stufen): Rechtsklick mit leerer Hand. Der Sitz ist eine leere Block-Anzeige (kein
 * Tier – keine Herzen über der Leiste), aufstehen mit Schleichen. Geht auch mit Bedrock (dort Schleichen/Aussteigen).
 */
public final class Seats implements Listener {

    public static final String TAG = "ak_seat";
    private static final double SEAT_Y = 0.35;
    private final AngelkoenigPlugin pl;

    public Seats(AngelkoenigPlugin pl) {
        this.pl = pl;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        if (b == null || b.getWorld() != pl.world() || p.isSneaking() || p.isInsideVehicle()
                || p.getInventory().getItemInMainHand().getType() != Material.AIR) {
            return;
        }
        boolean seat = b.getBlockData() instanceof Stairs st && st.getHalf() == Bisected.Half.BOTTOM
                || b.getBlockData() instanceof Slab sl && sl.getType() == Slab.Type.BOTTOM;
        if (!seat || !b.getRelative(0, 1, 0).isPassable() || p.getLocation().distanceSquared(b.getLocation()) > 9) {
            return;
        }
        e.setCancelled(true);
        float yaw = p.getLocation().getYaw();
        if (b.getBlockData() instanceof Stairs st) {
            yaw = switch (st.getFacing()) {
                case NORTH -> 0f;
                case SOUTH -> 180f;
                case EAST -> 90f;
                default -> -90f;
            };
        }
        Location at = b.getLocation().add(0.5, SEAT_Y, 0.5);
        at.setYaw(yaw);
        BlockDisplay d = b.getWorld().spawn(at, BlockDisplay.class, disp -> {
            disp.setPersistent(false);
            disp.addScoreboardTag(TAG);
        });
        d.addPassenger(p);
    }

    @EventHandler
    public void onDismount(EntityDismountEvent e) {
        Entity seat = e.getDismounted();
        if (seat.getScoreboardTags().contains(TAG)) {
            seat.remove();
            if (e.getEntity() instanceof Player p) {
                Location up = p.getLocation().add(0, 0.6, 0);
                pl.getServer().getScheduler().runTask(pl, () -> p.teleport(up));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Entity v = e.getPlayer().getVehicle();
        if (v != null && v.getScoreboardTags().contains(TAG)) {
            v.remove();
        }
    }

    /** Beim Abschalten alle Sitze weg. */
    public void clear() {
        for (Entity e : pl.world().getEntities()) {
            if (e.getScoreboardTags().contains(TAG)) {
                e.remove();
            }
        }
    }
}
