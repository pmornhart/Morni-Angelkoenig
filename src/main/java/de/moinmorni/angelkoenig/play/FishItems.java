package de.moinmorni.angelkoenig.play;

import de.moinmorni.angelkoenig.Items;
import de.moinmorni.angelkoenig.Texts;
import de.moinmorni.angelkoenig.fish.Catch;
import de.moinmorni.angelkoenig.fish.DayTime;
import de.moinmorni.angelkoenig.fish.FishModels;
import de.moinmorni.angelkoenig.fish.Species;
import de.moinmorni.angelkoenig.fish.Spot;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Fänge als Gegenstände: Name in der Farbe der Seltenheit, Beschreibung mit Länge, Gewicht, Ort, Tageszeit, Tag und
 * Angler. Die Werte stehen im Gegenstand (nicht im Namen) – so lässt er sich in jeder Sprache neu beschriften.
 */
public final class FishItems {

    /** Ein Fang aus dem Inventar. */
    public record Stored(Catch c, Spot spot, DayTime time, long day, String by) {
    }

    private static final DateTimeFormatter DE = DateTimeFormatter.ofPattern("d.M.yyyy", Locale.GERMAN);
    private static final DateTimeFormatter EN = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final AtomicLong SERIAL = new AtomicLong(System.currentTimeMillis());

    private static NamespacedKey species;
    private static NamespacedKey len;
    private static NamespacedKey grams;
    private static NamespacedKey spot;
    private static NamespacedKey time;
    private static NamespacedKey day;
    private static NamespacedKey by;
    private static NamespacedKey id;
    private static NamespacedKey trophy;

    private FishItems() {
    }

    public static void init(Plugin plugin) {
        species = Items.key(plugin, "fish");
        len = Items.key(plugin, "fish_mm");
        grams = Items.key(plugin, "fish_g");
        spot = Items.key(plugin, "fish_spot");
        time = Items.key(plugin, "fish_time");
        day = Items.key(plugin, "fish_day");
        by = Items.key(plugin, "fish_by");
        id = Items.key(plugin, "fish_id");
        trophy = Items.key(plugin, "fish_trophy");
    }

    public static ItemStack create(Catch c, Spot where, DayTime when, LocalDate date, String angler, String lang) {
        Species s = c.species();
        ItemStack item = new ItemStack(Items.material(s.item()));
        item.editMeta(meta -> {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(species, PersistentDataType.STRING, s.id());
            pdc.set(len, PersistentDataType.INTEGER, c.lengthMm());
            pdc.set(grams, PersistentDataType.INTEGER, c.grams());
            pdc.set(spot, PersistentDataType.STRING, where.id());
            pdc.set(time, PersistentDataType.STRING, when.id());
            pdc.set(day, PersistentDataType.LONG, date.toEpochDay());
            pdc.set(by, PersistentDataType.STRING, angler);
            pdc.set(id, PersistentDataType.LONG, SERIAL.incrementAndGet());
            if (c.trophy()) {
                pdc.set(trophy, PersistentDataType.BYTE, (byte) 1);
            }
            meta.setMaxStackSize(1);
            model(meta, s);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            // Legenden schimmern über ihre eigene bewegte Zeichnung (der lila Verzauberungs-Glanz überdeckte die Farben);
            // große Fänge anderer Arten glänzen weiter
            if (c.trophy() && !s.shines()) {
                meta.setEnchantmentGlintOverride(true);
            }
            if (meta instanceof LeatherArmorMeta leather) {
                leather.setColor(Color.fromRGB(0x5A3A22));
            }
        });
        label(item, lang);
        return item;
    }

    /**
     * Eigenes Aussehen der Art: Name als {@code custom_model_data} (siehe {@link FishModels}). Ohne das Fisch-Paket
     * zeigt Minecraft einfach den Gegenstand aus {@link Species#item()}.
     */
    public static void model(ItemMeta meta, Species s) {
        CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
        cmd.setStrings(List.of(FishModels.name(s)));
        meta.setCustomModelDataComponent(cmd);
    }

    /** Gegenstand einer Art ohne Fang-Daten (Anzeigen im Tal, Fischbuch). */
    public static ItemStack look(Species s) {
        ItemStack item = new ItemStack(Items.material(s.item()));
        item.editMeta(meta -> {
            model(meta, s);
            if (meta instanceof LeatherArmorMeta leather) {
                leather.setColor(Color.fromRGB(0x5A3A22));
            }
        });
        return item;
    }

    /** Fang aus einem Gegenstand lesen (leer, wenn es kein Fang ist). */
    public static Optional<Stored> read(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String sid = pdc.get(species, PersistentDataType.STRING);
        Optional<Species> s = Species.byId(sid);
        if (s.isEmpty()) {
            return Optional.empty();
        }
        int mm = pdc.getOrDefault(len, PersistentDataType.INTEGER, 0);
        int g = pdc.getOrDefault(grams, PersistentDataType.INTEGER, 1);
        boolean big = pdc.has(trophy, PersistentDataType.BYTE);
        Spot sp = Spot.byId(pdc.get(spot, PersistentDataType.STRING)).orElse(Spot.BERGSEE);
        DayTime dt = DayTime.byId(pdc.getOrDefault(time, PersistentDataType.STRING, "tag")).orElse(DayTime.TAG);
        long d = pdc.getOrDefault(day, PersistentDataType.LONG, LocalDate.now().toEpochDay());
        String who = pdc.getOrDefault(by, PersistentDataType.STRING, "?");
        return Optional.of(new Stored(new Catch(s.get(), mm, g, big), sp, dt, d, who));
    }

    public static boolean isFish(ItemStack item) {
        return read(item).isPresent();
    }

    /** Name und Beschreibung in einer Sprache (neu) setzen. */
    public static void label(ItemStack item, String lang) {
        Optional<Stored> st = read(item);
        if (st.isEmpty()) {
            return;
        }
        Stored f = st.get();
        Species s = f.c().species();
        TagResolver colour = Texts.colour(s.rarity().colour());
        Component name = Texts.text(lang, "catch.title", colour,
                Placeholder.unparsed("fish", Texts.plain(lang, "species." + s.id())));
        List<Component> lore = new ArrayList<>();
        lore.add(Texts.text(lang, "fish.lore.rarity", colour,
                Placeholder.unparsed("rarity", Texts.plain(lang, "rarity." + s.rarity().id())),
                Placeholder.unparsed("stars", "★".repeat(s.rarity().stars()))));
        lore.add(Texts.text(lang, "species." + s.id() + ".desc").color(net.kyori.adventure.text.format.NamedTextColor.GRAY));
        if (s.junk()) {
            lore.add(Texts.text(lang, "fish.lore.junk", Placeholder.unparsed("weight", f.c().weightText(lang))));
        } else {
            lore.add(Texts.text(lang, "fish.lore.size", Placeholder.unparsed("len", f.c().lengthText(lang)),
                    Placeholder.unparsed("weight", f.c().weightText(lang))));
        }
        lore.add(Texts.text(lang, "fish.lore.where", Placeholder.unparsed("spot", Texts.plain(lang, "spot." + f.spot().id())),
                Placeholder.unparsed("time", Texts.plain(lang, "time." + f.time().id()))));
        String date = LocalDate.ofEpochDay(f.day()).format("en".equals(lang) ? EN : DE);
        lore.add(Texts.text(lang, "fish.lore.when", Placeholder.unparsed("date", date), Placeholder.unparsed("name", f.by())));
        if (f.c().trophy()) {
            lore.add(Texts.text(lang, "fish.lore.trophy"));
        }
        lore.add(Texts.text(lang, "fish.lore.value", Placeholder.unparsed("price", String.valueOf(f.c().price()))));
        item.editMeta(meta -> {
            meta.displayName(name);
            meta.lore(lore);
        });
    }

    /** Alle Fänge im Inventar eines Spielers neu beschriften (nach Sprachwechsel). */
    public static void relabel(Player p, String lang) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (isFish(item)) {
                label(item, lang);
                inv.setItem(i, item);
            }
        }
    }
}
