package de.moinmorni.angelkoenig.listen;

import de.moinmorni.angelkoenig.AngelkoenigPlugin;
import de.moinmorni.angelkoenig.Items;
import de.moinmorni.angelkoenig.Texts;
import de.moinmorni.angelkoenig.Ui;
import de.moinmorni.angelkoenig.data.PlayerData;
import de.moinmorni.angelkoenig.fish.Species;
import de.moinmorni.angelkoenig.fish.Units;
import de.moinmorni.angelkoenig.play.FishItems;
import de.moinmorni.angelkoenig.play.Hotbar;
import de.moinmorni.angelkoenig.play.Menus;
import de.moinmorni.angelkoenig.play.Npcs;
import java.util.SplittableRandom;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Betreten, Verlassen, Knöpfe der Schnellleiste, Figuren anklicken, Inventar schützen. */
public final class PlayerListener implements Listener {

    private static final int TIPS = 7;
    private static final int GREETS = 4;
    private final AngelkoenigPlugin plugin;
    private final SplittableRandom random = new SplittableRandom();

    public PlayerListener(AngelkoenigPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        e.joinMessage(null);
        PlayerData d = plugin.data(p);
        boolean fresh = !d.introSeen();
        if (fresh) {
            d.earn(plugin.settings().startTaler());
            d.introSeen(true);
        }
        plugin.angling().ensureQuests(d);
        plugin.welcome(p);
        String lang = plugin.lang(p);
        Ui.title(p, Texts.text(lang, "welcome.title"), Texts.text(lang, "welcome.subtitle"), 2500);
        if (fresh) {
            for (String k : new String[] {"intro.1", "intro.2", "intro.3", "intro.4"}) {
                p.sendMessage(Texts.msg(lang, k));
            }
        } else {
            p.sendMessage(Texts.msg(lang, "welcome.back", Placeholder.unparsed("taler", Units.number(d.taler(), lang)),
                    Placeholder.unparsed("species", String.valueOf(d.speciesCount())),
                    Placeholder.unparsed("total", String.valueOf(Species.fishes().size()))));
        }
        plugin.store().save(d);
        plugin.stateChanged();
        // Angelt die Party schon hier? Dann gleich dazu.
        UUID uuid = p.getUniqueId();
        plugin.net().party(uuid).thenAccept(party -> Bukkit.getScheduler().runTask(plugin, () -> {
            Player q = Bukkit.getPlayer(uuid);
            if (party.isEmpty() || q == null) {
                return;
            }
            for (var m : party.get().members()) {
                Player mate = m.uuid().equals(uuid) ? null : Bukkit.getPlayer(m.uuid());
                if (mate != null && mate.getWorld() == plugin.world()) {
                    q.teleport(mate.getLocation());
                    q.sendMessage(Texts.msg(plugin.lang(q), "party.joined", Placeholder.unparsed("name", mate.getName())));
                    return;
                }
            }
        }));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        e.quitMessage(null);
        plugin.tournaments().forget(p);
        plugin.angling().flush(p.getUniqueId());
        plugin.angling().forget(p.getUniqueId());
        plugin.sidebar().forget(p.getUniqueId());
        plugin.forgetLanguage(p.getUniqueId());
        plugin.store().unload(p.getUniqueId());
        plugin.stateChanged();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (e.getPlayer().getWorld() == plugin.world() || plugin.world() != null) {
            Location s = plugin.spawn();
            e.setRespawnLocation(s);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        String tool = Items.tool(item);
        if (FishItems.isFish(item)) {
            e.setCancelled(true);
            return;
        }
        if (tool == null || "rod".equals(tool)) {
            return;
        }
        e.setCancelled(true);
        switch (tool) {
            case "bait" -> Menus.baits(plugin, p);
            case "spots" -> Menus.spots(plugin, p);
            case "book" -> Menus.book(plugin, p);
            case "quests" -> Menus.quests(plugin, p);
            case "wardrobe" -> Menus.wardrobe(plugin, p);
            case "lobby" -> {
                p.sendMessage(Texts.msg(plugin.lang(p), "lobby.going"));
                plugin.angling().flush(p.getUniqueId());
                plugin.net().toLobby(p, Texts.text(plugin.lang(p), "lobby.kick"));
            }
            default -> {
            }
        }
    }

    /** Fischhändler und Bootsmann anklicken. */
    @EventHandler(priority = EventPriority.LOW)
    public void onEntity(PlayerInteractEntityEvent e) {
        var tags = e.getRightClicked().getScoreboardTags();
        Player p = e.getPlayer();
        if (tags.contains(Npcs.MERCHANT)) {
            e.setCancelled(true);
            if (e.getHand() != EquipmentSlot.HAND) {
                return;
            }
            String lang = plugin.lang(p);
            p.sendMessage(Texts.msg(lang, "merchant.greet." + random.nextInt(GREETS)));
            Ui.sound(p, "entity.villager.trade", 1.0f);
            Menus.merchant(plugin, p);
        } else if (tags.contains(Npcs.BOATMAN)) {
            e.setCancelled(true);
            if (e.getHand() == EquipmentSlot.HAND) {
                p.sendMessage(Texts.msg(plugin.lang(p), "boatman.tip." + random.nextInt(TIPS)));
                Ui.sound(p, "entity.villager.ambient", 0.9f);
            }
        } else if (e.getRightClicked().getWorld() == plugin.world()
                && !tags.contains(Npcs.BOAT) && !plugin.builder(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (e.getPlayer().getWorld() == plugin.world() && !plugin.builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (Items.tool(e.getMainHandItem()) != null || Items.tool(e.getOffHandItem()) != null
                || FishItems.isFish(e.getOffHandItem()) || FishItems.isFish(e.getMainHandItem())) {
            e.setCancelled(true);
        }
    }

    /** Werkzeuge und Kleidung bleiben, wo sie sind; Fänge darf man umsortieren (nur im eigenen Inventar). */
    @EventHandler(priority = EventPriority.LOW)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || plugin.builder(p)) {
            return;
        }
        boolean own = e.getView().getTopInventory().getType() == InventoryType.CRAFTING;
        if (Items.tool(e.getCurrentItem()) != null || Items.tool(e.getCursor()) != null
                || e.getSlotType() == InventoryType.SlotType.ARMOR || e.getSlotType() == InventoryType.SlotType.CRAFTING
                || e.getSlotType() == InventoryType.SlotType.RESULT) {
            e.setCancelled(true);
            return;
        }
        if (e.getHotbarButton() >= 0 && Items.tool(p.getInventory().getItem(e.getHotbarButton())) != null) {
            e.setCancelled(true);
            return;
        }
        if (!own && e.getClickedInventory() == p.getInventory() && e.isShiftClick()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player p && !plugin.builder(p)
                && (Items.tool(e.getOldCursor()) != null || e.getRawSlots().stream().anyMatch(s -> s < 9 && s >= 5))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        if (FishItems.isFish(e.getItem())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (e.getEntity().getWorld() == plugin.world()) {
            e.setCancelled(true);
        }
    }

    /** Im Tal tut nichts weh; wer ins Leere fällt, landet wieder im Dorf. */
    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || p.getWorld() != plugin.world()) {
            return;
        }
        e.setCancelled(true);
        if (e.getCause() == EntityDamageEvent.DamageCause.VOID || e.getCause() == EntityDamageEvent.DamageCause.WORLD_BORDER) {
            p.teleport(plugin.spawn());
        }
        if (p.getGameMode() == GameMode.SURVIVAL) {
            p.setGameMode(GameMode.ADVENTURE);
        }
    }
}
