package net.morni.angelkoenig;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Gegenstände bauen und markieren (Markierung im Gegenstand, nicht am Namen – Namen hängen an der Sprache). */
public final class Items {

    /** Werkzeug in der Schnellleiste: „rod“, „bait“, „spots“, „book“, „quests“, „wardrobe“, „lobby“, „cosmetic“. */
    private static NamespacedKey toolKey;

    private Items() {
    }

    static void init(Plugin plugin) {
        toolKey = new NamespacedKey(plugin, "tool");
    }

    public static NamespacedKey key(Plugin plugin, String name) {
        return new NamespacedKey(plugin, name);
    }

    public static Material material(String id) {
        Material m = Material.matchMaterial(id);
        return m == null ? Material.COD : m;
    }

    public static ItemStack named(Material material, int amount, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        item.editMeta(meta -> {
            if (name != null) {
                meta.displayName(name);
            }
            if (lore != null && !lore.isEmpty()) {
                // Zeilen ohne eigene Farbe grau (Minecraft malt sie sonst lila)
                meta.lore(lore.stream().map(l -> l.colorIfAbsent(NamedTextColor.GRAY)).toList());
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE, ItemFlag.HIDE_UNBREAKABLE,
                    ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        });
        return item;
    }

    public static ItemStack glow(ItemStack item, boolean glow) {
        item.editMeta(meta -> meta.setEnchantmentGlintOverride(glow ? Boolean.TRUE : null));
        return item;
    }

    public static ItemStack tool(ItemStack item, String id) {
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, id);
            meta.setUnbreakable(true);
            meta.setMaxStackSize(1);
        });
        return item;
    }

    public static String tool(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
    }

    /** Leere Glasscheibe als Rahmen in Menüs. */
    public static ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> meta.setHideTooltip(true));
        return item;
    }
}
