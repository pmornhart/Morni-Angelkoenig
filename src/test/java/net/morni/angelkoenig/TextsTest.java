package net.morni.angelkoenig;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.morni.angelkoenig.fish.Bait;
import net.morni.angelkoenig.fish.Cosmetic;
import net.morni.angelkoenig.fish.DayTime;
import net.morni.angelkoenig.fish.Quest;
import net.morni.angelkoenig.fish.Rarity;
import net.morni.angelkoenig.fish.Rod;
import net.morni.angelkoenig.fish.Species;
import net.morni.angelkoenig.fish.Spot;
import net.morni.angelkoenig.fish.Weather;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.junit.jupiter.api.Test;

class TextsTest {

    /** Formatierungen, die keine Platzhalter sind. */
    private static final Set<String> STYLES = Set.of("gray", "white", "em", "au", "bl", "red", "green", "dark_gray",
            "b", "bold", "gold", "yellow", "c");
    // Tags wie <name> oder </gray> – aber nicht \<spieler> (maskiert).
    // https://regex101.com/?flavor=java&regex=(%3F%3C!%5C%5C)%3C%2F%3F(%5Ba-z_%5D%2B)%3E&testString=%3Cgray%3EHi%20%3Cname%3E%3C%2Fgray%3E%20%5C%3Cspieler%3E
    private static final Pattern TAG = Pattern.compile("(?<!\\\\)</?([a-z_]+)>");
    // Text-Schlüssel im Quelltext, z. B. "catch.first" – Klänge (sound(…, "entity.…")) werden ausgelassen.
    // https://regex101.com/?flavor=java&regex=%22(%5Ba-z%5D%5Ba-z0-9_-%5D*(%3F%3A%5C.%5Ba-z0-9_-%5D%2B)%2B)%22&testString=Texts.msg(lang%2C%20%22catch.first%22)
    private static final Pattern KEY = Pattern.compile("\"([a-z][a-z0-9_-]*(?:\\.[a-z0-9_-]+)+)\"");

    private static Set<String> placeholders(String text) {
        Set<String> out = new TreeSet<>();
        Matcher m = TAG.matcher(text);
        while (m.find()) {
            if (!STYLES.contains(m.group(1))) {
                out.add(m.group(1));
            }
        }
        return out;
    }

    @Test
    void everyTextExistsInBothLanguagesWithTheSamePlaceholders() {
        for (String key : Texts.keys()) {
            String de = Texts.raw("de", key);
            String en = Texts.raw("en", key);
            assertFalse(de.isBlank(), key);
            assertFalse(en.isBlank(), key);
            assertEquals(placeholders(de), placeholders(en), "Platzhalter in " + key);
        }
    }

    @Test
    void everyTextParsesStrictly() {
        MiniMessage strict = MiniMessage.builder().strict(true)
                .editTags(t -> t.resolvers(
                        TagResolver.resolver("em", Tag.styling(TextColor.color(Texts.EMERALD))),
                        TagResolver.resolver("au", Tag.styling(TextColor.color(Texts.GOLD))),
                        TagResolver.resolver("bl", Tag.styling(TextColor.color(Texts.BLUE))),
                        TagResolver.resolver("c", Tag.styling(TextColor.color(0x123456)))))
                .build();
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                TagResolver.Builder resolvers = TagResolver.builder();
                placeholders(text).forEach(p -> resolvers.resolver(Placeholder.unparsed(p, "x")));
                TagResolver all = resolvers.build();
                for (String line : text.split("\n")) {
                    assertDoesNotThrow(() -> strict.deserialize(line, all), lang + " " + key + ": " + line);
                }
            }
        }
    }

    /** Minecraft zeigt nur Zeichen der „Basic Multilingual Plane“ – Emoji wie 🎣 wären ein leeres Kästchen. */
    @Test
    void onlyCharactersMinecraftCanShow() {
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                assertEquals(text.length(), text.codePointCount(0, text.length()), lang + " " + key);
            }
        }
    }

    @Test
    void englishIsReallyEnglish() {
        assertNotEquals(Texts.raw("de", "catch.first"), Texts.raw("en", "catch.first"));
        assertEquals(Texts.raw("de", "spot.moor"), Texts.raw("fr", "spot.moor"), "unbekannte Sprache → Deutsch");
        assertEquals("missing.key", Texts.raw("de", "missing.key"));
    }

    /** Jeder Fisch, Köder, jede Angel, jedes Kleidungsstück, jeder Platz … hat einen Namen in beiden Sprachen. */
    @Test
    void everyGameThingHasAName() {
        List<String> need = new ArrayList<>();
        for (Species s : Species.values()) {
            need.add("species." + s.id());
            need.add("species." + s.id() + ".desc");
        }
        for (Rarity r : Rarity.values()) {
            need.add("rarity." + r.id());
        }
        for (Spot s : Spot.values()) {
            need.add("spot." + s.id());
            need.add("spot." + s.id() + ".desc");
        }
        for (DayTime t : DayTime.values()) {
            need.add("time." + t.id());
            need.add("hint." + t.id());
        }
        for (Weather w : Weather.values()) {
            need.add("weather." + w.id());
            need.add("weather.now." + w.id());
            need.add("hint." + w.id());
        }
        for (Rod r : Rod.values()) {
            need.add("rod." + r.id());
            need.add("rod.speed." + r.id());
        }
        for (Bait b : Bait.values()) {
            need.add("bait." + b.id());
        }
        for (Cosmetic c : Cosmetic.values()) {
            need.add("cosmetic." + c.id());
        }
        for (Quest.Kind k : Quest.Kind.values()) {
            need.add("quest." + k.name().toLowerCase(Locale.ROOT));
        }
        for (int i = 0; i < 7; i++) {
            need.add("boatman.tip." + i);
        }
        for (int i = 0; i < 5; i++) {
            need.add("bottle." + i);
        }
        for (int i = 0; i < 4; i++) {
            need.add("merchant.greet." + i);
        }
        List<String> missing = need.stream().filter(k -> !Texts.has(k)).toList();
        assertTrue(missing.isEmpty(), "Fehlende Texte: " + missing);
    }

    /** Jeder Schlüssel, den der Code benutzt, gibt es auch – sonst stünde im Spiel nur „catch.first“. */
    @Test
    void everyKeyInTheCodeExists() throws IOException {
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String src = Files.readString(f);
                Matcher m = KEY.matcher(src);
                while (m.find()) {
                    String key = m.group(1);
                    String before = src.substring(Math.max(0, m.start() - 60), m.start());
                    String line = before.substring(before.lastIndexOf('\n') + 1);
                    if (line.contains("ound") || line.contains("Key(") || line.contains("createBlockData")
                            || line.contains("info(") || line.contains("warning(") || key.startsWith("minecraft.")
                            || src.contains("\"" + key + "\" +") || !looksLikeText(key)) {
                        continue;
                    }
                    if (!Texts.has(key)) {
                        missing.add(f.getFileName() + ": " + key);
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "Fehlende Texte: " + missing);
    }

    private static boolean looksLikeText(String key) {
        String first = key.substring(0, key.indexOf('.'));
        return Texts.keys().stream().anyMatch(k -> k.startsWith(first + "."));
    }

    @Test
    void numbersUseTheRightDecimalMark() {
        assertEquals("▮▮▮▯▯", Ui.bar(0.6, 5));
        assertEquals("1:05", Ui.clock(65));
        assertEquals("0:00", Ui.clock(-3));
    }
}
