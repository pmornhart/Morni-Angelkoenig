package net.morni.angelkoenig.play;

import net.morni.angelkoenig.AngelkoenigPlugin;
import net.morni.angelkoenig.Items;
import net.morni.angelkoenig.Texts;
import net.morni.angelkoenig.data.PlayerData;
import net.morni.angelkoenig.fish.Bait;
import net.morni.angelkoenig.fish.Cosmetic;
import net.morni.angelkoenig.fish.Rod;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;

/**
 * Die Schnellleiste: Angel, Köderdose, Wegweiser, Fischbuch, Aufgaben, Kleiderschrank, zur Lobby – und die Kleidung,
 * die man trägt. Alles andere Platz bleibt für die Fänge.
 */
public final class Hotbar {

    public static final int ROD = 0;
    public static final int BAIT = 1;
    public static final int SPOTS = 4;
    public static final int BOOK = 5;
    public static final int QUESTS = 6;
    public static final int WARDROBE = 7;
    public static final int LOBBY = 8;
    private static final int[] ROD_COLOURS = {0xE8E8E8, 0x55D16E, 0x4FA3F7, 0xF2B632};

    private Hotbar() {
    }

    /** Alles neu geben (beim Betreten, nach Sprachwechsel); Fänge bleiben, Fremdes fliegt raus. */
    public static void give(AngelkoenigPlugin pl, Player p) {
        String lang = pl.lang(p);
        PlayerData d = pl.data(p);
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !FishItems.isFish(item)) {
                inv.setItem(i, null);
            }
        }
        for (int slot : new int[] {ROD, BAIT, SPOTS, BOOK, QUESTS, WARDROBE, LOBBY}) {
            ItemStack old = inv.getItem(slot);
            if (old != null && FishItems.isFish(old)) {
                inv.setItem(slot, null);
                if (!inv.addItem(old).isEmpty()) {
                    continue;
                }
            }
        }
        inv.setItem(ROD, rod(d.rod(), lang));
        inv.setItem(BAIT, bait(d, lang));
        inv.setItem(SPOTS, tool(Material.COMPASS, lang, "spots", "item.spots", "item.spots.lore"));
        inv.setItem(BOOK, tool(Material.BOOK, lang, "book", "item.book", "item.book.lore"));
        inv.setItem(QUESTS, tool(Material.PAPER, lang, "quests", "item.quests", "item.quests.lore"));
        inv.setItem(WARDROBE, tool(Material.ARMOR_STAND, lang, "wardrobe", "item.wardrobe", "item.wardrobe.lore"));
        inv.setItem(LOBBY, tool(Material.RED_BED, lang, "lobby", "item.lobby", "item.lobby.lore"));
        armor(p, d, lang);
        FishItems.relabel(p, lang);
        if (inv.getHeldItemSlot() != ROD && inv.getItem(inv.getHeldItemSlot()) == null) {
            inv.setHeldItemSlot(ROD);
        }
    }

    private static ItemStack tool(Material m, String lang, String id, String name, String lore) {
        return Items.tool(Items.named(m, 1, Texts.text(lang, name), Texts.lines(lang, lore)), id);
    }

    public static ItemStack rod(Rod r, String lang) {
        Component name = Texts.text(lang, "rod." + r.id()).color(TextColor.color(ROD_COLOURS[r.ordinal()]));
        ItemStack item = Items.named(Material.FISHING_ROD, 1, name, Texts.lines(lang, "item.rod.lore",
                Placeholder.unparsed("speed", Texts.plain(lang, "rod.speed." + r.id())),
                Placeholder.unparsed("luck", r.luck() == 0 ? "–" : "★".repeat(r.luck()))));
        Items.glow(item, r.shines());
        return Items.tool(item, "rod");
    }

    public static ItemStack bait(PlayerData d, String lang) {
        Bait b = d.bait();
        Material m = b == null ? Material.BOWL : Items.material(b.item());
        String what = b == null ? Texts.plain(lang, "bait.none") : Texts.plain(lang, "bait." + b.id()) + " ×"
                + d.baits(b);
        return tool(m, lang, "bait", "item.bait", "item.bait.lore", what);
    }

    private static ItemStack tool(Material m, String lang, String id, String name, String lore, String bait) {
        ItemStack item = Items.named(m, 1, Texts.text(lang, name, Placeholder.unparsed("bait", bait)),
                Texts.lines(lang, lore));
        return Items.tool(item, id);
    }

    public static void refreshRod(AngelkoenigPlugin pl, Player p) {
        p.getInventory().setItem(ROD, rod(pl.data(p).rod(), pl.lang(p)));
    }

    public static void refreshBait(AngelkoenigPlugin pl, Player p) {
        p.getInventory().setItem(BAIT, bait(pl.data(p), pl.lang(p)));
    }

    /** Getragene Kleidung anlegen (nur zum Aussehen, abnehmen geht über den Kleiderschrank). */
    public static void armor(Player p, PlayerData d, String lang) {
        PlayerInventory inv = p.getInventory();
        for (Cosmetic.Slot slot : Cosmetic.Slot.values()) {
            Cosmetic c = d.worn(slot);
            EquipmentSlot es = switch (slot) {
                case HEAD -> EquipmentSlot.HEAD;
                case CHEST -> EquipmentSlot.CHEST;
                case LEGS -> EquipmentSlot.LEGS;
                case FEET -> EquipmentSlot.FEET;
            };
            inv.setItem(es, c == null ? null : cosmetic(c, lang));
        }
    }

    public static ItemStack cosmetic(Cosmetic c, String lang) {
        ItemStack item = Items.named(Items.material(c.item()), 1, Texts.text(lang, "cosmetic." + c.id()), null);
        item.editMeta(meta -> {
            if (meta instanceof LeatherArmorMeta leather && c.colour() >= 0) {
                leather.setColor(Color.fromRGB(c.colour()));
            }
        });
        Items.glow(item, c == Cosmetic.KOENIGSKRONE);
        ItemStack out = Items.tool(item, "cosmetic");
        // Kleidung ist nur Schmuck: keine Rüstungspunkte (sonst steht über den Herzen eine Rüstungsleiste)
        out.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.itemAttributes().build());
        return out;
    }
}
