package net.morni.angelkoenig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Alle Texte für Spieler, auf Deutsch und Englisch, als MiniMessage. Eigene Farben: {@code <em>} Smaragd,
 * {@code <au>} Gold, {@code <bl>} Seeblau; {@code <c>} ist die Farbe der Seltenheit (kommt beim Aufruf mit). Namen
 * von Spielern kommen immer als {@code Placeholder.unparsed} hinein.
 */
public final class Texts {

    public static final int EMERALD = 0x2BB673;
    public static final int GOLD = 0xF2B632;
    public static final int BLUE = 0x5DA9E9;

    public static final MiniMessage MM = MiniMessage.builder()
            .editTags(t -> t.resolvers(
                    TagResolver.resolver("em", Tag.styling(TextColor.color(EMERALD))),
                    TagResolver.resolver("au", Tag.styling(TextColor.color(GOLD))),
                    TagResolver.resolver("bl", Tag.styling(TextColor.color(BLUE)))))
            .build();

    private static final Map<String, String[]> TEXTS = new LinkedHashMap<>();

    private Texts() {
    }

    private static void t(String key, String de, String en) {
        if (TEXTS.put(key, new String[] {de, en}) != null) {
            throw new IllegalStateException("Text doppelt: " + key);
        }
    }

    static {
        t("prefix", "<au>⚓</au> <em>Angelkönig</em> <dark_gray>»</dark_gray> ",
                "<au>⚓</au> <em>Angler King</em> <dark_gray>»</dark_gray> ");
        t("mode", "Angelkönig", "Angler King");

        // ---------- Allgemein ----------
        t("only-players", "Das geht nur im Spiel.", "Only possible in-game.");
        t("not-ready", "<gray>Das Tal wird gerade vorbereitet – gleich geht's los!</gray>",
                "<gray>The valley is being prepared – just a moment!</gray>");
        t("unknown-player", "<gray>Niemanden namens <white><name></white> gefunden.</gray>",
                "<gray>Nobody called <white><name></white> found.</gray>");
        t("lobby.kick", "<au>morni.net</au>\n<gray>Zurück in die Lobby: Bitte verbinde dich neu mit morni.net.</gray>",
                "<au>morni.net</au>\n<gray>Back to the lobby: please reconnect to morni.net.</gray>");
        t("lobby.going", "<gray>Ab in die Lobby – Petri Heil und bis bald!</gray>",
                "<gray>Off to the lobby – tight lines and see you soon!</gray>");
        t("welcome.title", "<au>⚓ Angelkönig</au>", "<au>⚓ Angler King</au>");
        t("welcome.subtitle", "<gray>Gemütlich angeln im Alpen-Tal</gray>",
                "<gray>Relaxed fishing in an alpine valley</gray>");
        t("welcome.back", "<gray>Schön, dass du wieder da bist! Du hast <au><taler> Taler</au> und <white><species>/<total></white> Arten im Fischbuch.</gray>",
                "<gray>Good to see you again! You have <au><taler> thaler</au> and <white><species>/<total></white> species in your fish book.</gray>");
        t("intro.1", "<gray>Willkommen im Tal! Nimm die <white>Angel</white> in die Hand und wirf sie mit <white>Rechtsklick</white> ins Wasser. Taucht der Schwimmer unter, holst du mit <white>Rechtsklick</white> ein.</gray>",
                "<gray>Welcome to the valley! Hold your <white>rod</white> and cast it into the water with <white>right-click</white>. When the float dips, <white>right-click</white> again to reel in.</gray>");
        t("intro.2", "<gray>Deine Fänge bringst du zum <em>Fischhändler Xaver</em> am Steg – er zahlt in <au>Talern</au>. Dafür gibt es bessere Angeln, Köder und Kleidung.</gray>",
                "<gray>Take your catches to <em>Xaver the fishmonger</em> by the jetty – he pays in <au>thaler</au>. Spend them on better rods, bait and clothes.</gray>");
        t("intro.3", "<gray>Der <white>Wegweiser</white> (Kompass) bringt dich zu den vier Fangplätzen. Jeder hat eigene Fische – und Tageszeit und Wetter ändern, was beißt.</gray>",
                "<gray>The <white>signpost</white> (compass) takes you to the four fishing spots. Each has its own fish – and the time of day and the weather change what bites.</gray>");
        t("intro.4", "<gray>Alle 15 Minuten gibt es ein <au>Angel-Turnier</au>: Wer den schwersten Fisch fängt, gewinnt – und wird <au>Angelkönig</au>!</gray>",
                "<gray>Every 15 minutes there's a <au>fishing contest</au>: whoever lands the heaviest fish wins – and becomes <au>Angler King</au>!</gray>");
        t("party.joined", "<gray>Deine Party angelt schon hier – du stehst gleich bei <white><name></white>.</gray>",
                "<gray>Your party is already fishing here – you're right next to <white><name></white>.</gray>");

        // ---------- Gegenstände ----------
        t("item.rod.lore", "<gray>Rechtsklick: auswerfen und einholen.</gray>\n<gray>Biss: <white><speed></white> · Glück: <au><luck></au></gray>",
                "<gray>Right-click: cast and reel in.</gray>\n<gray>Bite: <white><speed></white> · Luck: <au><luck></au></gray>");
        t("item.bait", "<em>Köderdose</em> <gray>· <bait></gray>", "<em>Bait tin</em> <gray>· <bait></gray>");
        t("item.bait.lore", "<gray>Rechtsklick: Köder einhängen oder aushängen.</gray>",
                "<gray>Right-click: put bait on or take it off.</gray>");
        t("item.spots", "<em>Wegweiser</em>", "<em>Signpost</em>");
        t("item.spots.lore", "<gray>Rechtsklick: zu den Fangplätzen reisen.</gray>",
                "<gray>Right-click: travel to the fishing spots.</gray>");
        t("item.book", "<em>Fischbuch</em>", "<em>Fish book</em>");
        t("item.book.lore", "<gray>Rechtsklick: deine Sammlung, Rekorde und Werte.</gray>",
                "<gray>Right-click: your collection, records and stats.</gray>");
        t("item.quests", "<em>Aufgaben des Tages</em>", "<em>Daily tasks</em>");
        t("item.quests.lore", "<gray>Rechtsklick: drei Aufgaben – jeden Tag neu.</gray>",
                "<gray>Right-click: three tasks – new every day.</gray>");
        t("item.wardrobe", "<em>Kleiderschrank</em>", "<em>Wardrobe</em>");
        t("item.wardrobe.lore", "<gray>Rechtsklick: Hut, Weste und Hose anziehen.</gray>",
                "<gray>Right-click: put on hats, vests and trousers.</gray>");
        t("item.lobby", "<em>Zur Lobby</em>", "<em>To the lobby</em>");
        t("item.lobby.lore", "<gray>Rechtsklick: zurück in die Lobby von morni.net.</gray>",
                "<gray>Right-click: back to the morni.net lobby.</gray>");

        // ---------- Angeln ----------
        t("rod.hasel", "Haselrute", "Hazel rod");
        t("rod.esche", "Eschenrute", "Ash rod");
        t("rod.meister", "Meisterrute", "Master rod");
        t("rod.koenig", "Königsrute", "King's rod");
        t("rod.speed.hasel", "gemütlich", "relaxed");
        t("rod.speed.esche", "flott", "brisk");
        t("rod.speed.meister", "schnell", "quick");
        t("rod.speed.koenig", "blitzschnell", "lightning-fast");

        // ---------- Seltenheit, Orte, Zeit, Wetter ----------
        t("rarity.krempel", "Fundstück", "Find");
        t("rarity.gewoehnlich", "Gewöhnlich", "Common");
        t("rarity.ungewoehnlich", "Ungewöhnlich", "Uncommon");
        t("rarity.selten", "Selten", "Rare");
        t("rarity.episch", "Episch", "Epic");
        t("rarity.legendaer", "Legendär", "Legendary");
        t("spot.bergsee", "Bergsee", "Mountain lake");
        t("spot.bach", "Gebirgsbach", "Mountain stream");
        t("spot.moor", "Moorweiher", "Bog pond");
        t("spot.eis", "Eisloch", "Ice hole");
        t("spot.bergsee.desc", "Der große See am Dorf: Steg, Ruderboote, tiefes klares Wasser.",
                "The big lake by the village: jetty, rowing boats, deep clear water.");
        t("spot.bach.desc", "Kaltes, schnelles Wasser unter dem Wasserfall – hier stehen die Forellen.",
                "Cold, fast water below the waterfall – the trout's favourite place.");
        t("spot.moor.desc", "Dunkles, stilles Wasser zwischen Schilf und Seerosen. Nachts leuchten Irrlichter.",
                "Dark, still water among reeds and water lilies. Will-o'-the-wisps glow at night.");
        t("spot.eis.desc", "Ein zugefrorener Karsee hoch oben – man angelt durch Löcher im Eis.",
                "A frozen mountain lake high up – you fish through holes in the ice.");
        t("time.morgen", "Morgen", "Morning");
        t("time.tag", "Tag", "Day");
        t("time.abend", "Abend", "Evening");
        t("time.nacht", "Nacht", "Night");
        t("weather.klar", "Klar", "Clear");
        t("weather.regen", "Regen", "Rain");
        t("weather.gewitter", "Gewitter", "Thunderstorm");
        t("weather.snow", "Schneefall", "Snowfall");
        t("weather.now.klar", "<gray>Die Wolken ziehen weiter – die Sonne kommt raus.</gray>",
                "<gray>The clouds move on – here comes the sun.</gray>");
        t("weather.now.regen", "<gray>Es fängt an zu regnen – jetzt werden die Forellen munter.</gray>",
                "<gray>It's starting to rain – the trout are waking up.</gray>");
        t("weather.now.gewitter", "<gray>Ein Gewitter zieht auf – jetzt kommen die ganz Großen aus der Tiefe …</gray>",
                "<gray>A thunderstorm is rolling in – now the really big ones come up from the deep …</gray>");
        t("place.village", "Dorf am See", "Lakeside village");
        t("place.wild", "Unterwegs", "Out and about");
        t("hint.always", "jederzeit", "any time");
        t("hint.morgen", "morgens", "in the morning");
        t("hint.tag", "tagsüber", "during the day");
        t("hint.abend", "abends", "in the evening");
        t("hint.nacht", "nachts", "at night");
        t("hint.klar", "bei klarem Himmel", "under clear skies");
        t("hint.regen", "bei Regen", "in the rain");
        t("hint.gewitter", "bei Gewitter", "in thunderstorms");
        t("hint.only-night", "nur abends und nachts", "only in the evening and at night");

        // ---------- Fische ----------
        t("species.rotauge", "Rotauge", "Roach");
        t("species.barsch", "Flussbarsch", "Perch");
        t("species.bachforelle", "Bachforelle", "Brown trout");
        t("species.karausche", "Karausche", "Crucian carp");
        t("species.renke", "Renke", "Whitefish");
        t("species.aesche", "Äsche", "Grayling");
        t("species.schleie", "Schleie", "Tench");
        t("species.steinkrebs", "Steinkrebs", "Stone crayfish");
        t("species.seesaibling", "Seesaibling", "Arctic char");
        t("species.hecht", "Hecht", "Pike");
        t("species.zander", "Zander", "Zander");
        t("species.aal", "Aal", "Eel");
        t("species.rutte", "Rutte", "Burbot");
        t("species.waller", "Waller", "Wels catfish");
        t("species.huchen", "Huchen", "Huchen");
        t("species.seeforelle", "Seeforelle", "Lake trout");
        t("species.goldforelle", "Goldforelle", "Golden trout");
        t("species.seekoenig", "Seekönig", "Lake King");
        t("species.moorkoenig", "Moorkönig", "Bog King");
        t("species.kristallsaibling", "Kristallsaibling", "Crystal char");
        t("species.stiefel", "Alter Stiefel", "Old boot");
        t("species.treibholz", "Treibholz", "Driftwood");
        t("species.flaschenpost", "Flaschenpost", "Message in a bottle");
        t("species.rotauge.desc", "Silbriger Schwarmfisch mit roten Augen.", "Silvery shoal fish with red eyes.");
        t("species.barsch.desc", "Gestreift, stachelig und immer hungrig.", "Striped, spiny and always hungry.");
        t("species.bachforelle.desc", "Rote Tupfen, flinke Flossen – liebt kaltes Wasser.",
                "Red spots, quick fins – loves cold water.");
        t("species.karausche.desc", "Rund und goldbraun, fühlt sich im Moor wohl.",
                "Round and golden brown, happy in the bog.");
        t("species.renke.desc", "Schlanker Silberfisch der klaren Bergseen.", "Slender silver fish of clear mountain lakes.");
        t("species.aesche.desc", "Trägt eine Fahne als Rückenflosse.", "Carries a flag for a dorsal fin.");
        t("species.schleie.desc", "Olivgrün und gemütlich, gründelt im Schlamm.",
                "Olive green and easy-going, rummages in the mud.");
        t("species.steinkrebs.desc", "Kleiner Krebs mit großen Scheren – nachts unterwegs.",
                "A small crayfish with big claws – out and about at night.");
        t("species.seesaibling.desc", "Leuchtend roter Bauch, lebt tief und kalt.", "Bright red belly, lives deep and cold.");
        t("species.hecht.desc", "Lauert reglos im Kraut – und schlägt blitzschnell zu.",
                "Lurks motionless in the weeds – then strikes in a flash.");
        t("species.zander.desc", "Glasige Augen, jägt in der Dämmerung.", "Glassy eyes, hunts at dusk.");
        t("species.aal.desc", "Schlangenförmig und geheimnisvoll – nur im Dunkeln zu fangen.",
                "Snake-like and mysterious – only caught in the dark.");
        t("species.rutte.desc", "Der einzige Dorsch im Süßwasser – mag es eiskalt.",
                "The only freshwater cod – likes it ice-cold.");
        t("species.waller.desc", "Riesig, mit langen Barteln. Kommt nachts aus der Tiefe.",
                "Huge, with long barbels. Comes up from the deep at night.");
        t("species.huchen.desc", "Der König der Bergbäche – kräftig und selten.",
                "King of the mountain streams – strong and rare.");
        t("species.seeforelle.desc", "Groß und silbern, zieht weit hinaus in den See.",
                "Big and silvery, roams far out into the lake.");
        t("species.goldforelle.desc", "Glänzt wie ein Goldstück. Man sieht sie fast nur am frühen Morgen.",
                "Gleams like a gold coin. Almost only seen early in the morning.");
        t("species.seekoenig.desc", "Ein uralter Riesenwaller – im Dorf erzählt man sich Geschichten über ihn.",
                "An ancient giant catfish – the villagers tell stories about him.");
        t("species.moorkoenig.desc", "Ein Hecht so lang wie ein Ruder. Zeigt sich bei Gewitter.",
                "A pike as long as an oar. Shows itself in thunderstorms.");
        t("species.kristallsaibling.desc", "Fast durchsichtig wie Eis – nur in klaren Nächten am Eisloch.",
                "Almost as clear as ice – only on clear nights at the ice hole.");
        t("species.stiefel.desc", "Jemand hat ihn wohl im See verloren. Größe 44.", "Someone lost it in the lake. Size 11.");
        t("species.treibholz.desc", "Vom Wasser glattgeschliffen.", "Polished smooth by the water.");
        t("species.flaschenpost.desc", "In der Flasche steckt ein Zettel …", "There's a note inside …");
        t("bottle.0", "Wer das liest, schuldet mir eine Brezn. – Sepp", "Whoever reads this owes me a pretzel. – Sepp");
        t("bottle.1", "Der Seekönig beißt nur bei Gewitter. Glaub mir.", "The Lake King only bites in thunderstorms. Trust me.");
        t("bottle.2", "Grüße vom anderen Ufer! Hier ist es auch schön.", "Greetings from the other shore! It's lovely here too.");
        t("bottle.3", "Am Eisloch, wenn die Sterne funkeln, glitzert es unter dem Eis …",
                "At the ice hole, when the stars sparkle, something glitters beneath the ice …");
        t("bottle.4", "Morgens, wenn der Nebel über dem Bach liegt, ist das Gold nicht weit.",
                "In the morning, when mist lies over the stream, gold is not far away.");

        // ---------- Fisch-Gegenstand ----------
        t("fish.lore.rarity", "<c><rarity></c> <au><stars></au>", "<c><rarity></c> <au><stars></au>");
        t("fish.lore.size", "<gray>Länge: <white><len></white> · Gewicht: <white><weight></white></gray>",
                "<gray>Length: <white><len></white> · Weight: <white><weight></white></gray>");
        t("fish.lore.junk", "<gray>Gewicht: <white><weight></white></gray>", "<gray>Weight: <white><weight></white></gray>");
        t("fish.lore.where", "<gray>Gefangen: <white><spot></white> · <white><time></white></gray>",
                "<gray>Caught: <white><spot></white> · <white><time></white></gray>");
        t("fish.lore.when", "<gray>Am <white><date></white> von <white><name></white></gray>",
                "<gray>On <white><date></white> by <white><name></white></gray>");
        t("fish.lore.value", "<gray>Wert: <au><price> Taler</au></gray>", "<gray>Value: <au><price> thaler</au></gray>");
        t("fish.lore.trophy", "<au>★ Prachtexemplar</au>", "<au>★ Trophy fish</au>");

        // ---------- Fang ----------
        t("catch.msg", "<c><fish></c> gefangen! <gray><size></gray>", "You caught <c><fish></c>! <gray><size></gray>");
        t("catch.find", "<c><fish></c> herausgezogen. <gray><size></gray>", "You pulled out <c><fish></c>. <gray><size></gray>");
        t("catch.size", "<len> · <weight>", "<len> · <weight>");
        t("catch.first", "<em>Neu im Fischbuch!</em> <au>+<n> Taler</au>", "<em>New in your fish book!</em> <au>+<n> thaler</au>");
        t("catch.book-done", "<au>\u2726 Fischbuch vollständig!</au> <gray>Alle Arten des Tals gefangen –</gray> <au>+<n> Taler</au><gray>!</gray>",
                "<au>\u2726 Fish book complete!</au> <gray>Every species in the valley caught –</gray> <au>+<n> thaler</au><gray>!</gray>");
        t("catch.record", "<au>Dein bisher größter Fang dieser Art!</au>", "<au>Your biggest catch of this kind so far!</au>");
        t("catch.world-record", "<au>✦ Talrekord!</au> <gray>Kein Fang dieser Art war je schwerer.</gray>",
                "<au>✦ Valley record!</au> <gray>No catch of this kind was ever heavier.</gray>");
        t("catch.trophy", "<au>Ein Prachtexemplar!</au>", "<au>A trophy fish!</au>");
        t("catch.title", "<c><fish></c>", "<c><fish></c>");
        t("catch.subtitle", "<gray><size></gray>", "<gray><size></gray>");
        t("catch.legendary.sub", "<au>✦ Ein legendärer Fang! ✦</au>", "<au>✦ A legendary catch! ✦</au>");
        t("catch.legendary.all", "<au><name></au> <gray>hat</gray> <au><fish></au> <gray>gefangen –</gray> <white><weight></white><gray>!</gray>",
                "<au><name></au> <gray>caught</gray> <au><fish></au> <gray>–</gray> <white><weight></white><gray>!</gray>");
        t("catch.bite", "<au><b>❗ Biss!</b></au> <gray>Schnell einholen!</gray>", "<au><b>❗ Bite!</b></au> <gray>Reel it in!</gray>");
        t("catch.full", "<gray>Dein Korb ist voll – der Fang schwimmt wieder davon. Verkauf deine Fänge beim <em>Fischhändler</em>!</gray>",
                "<gray>Your basket is full – the catch swims away again. Sell your catches at the <em>fishmonger</em>!</gray>");
        t("catch.bait", "<gray>Köder: <white><bait></white> · noch <white><n></white></gray>",
                "<gray>Bait: <white><bait></white> · <white><n></white> left</gray>");
        t("catch.bait-empty", "<gray>Deine Köderdose ist leer – neue Köder gibt's beim Fischhändler.</gray>",
                "<gray>Your bait tin is empty – the fishmonger sells new bait.</gray>");
        t("catch.bottle", "<gray>Auf dem Zettel steht: <white>„<note>“</white></gray>",
                "<gray>The note says: <white>“<note>”</white></gray>");
        t("catch.bar", "<c><fish></c> <gray>· <size></gray>", "<c><fish></c> <gray>· <size></gray>");

        // ---------- Köder ----------
        t("bait.wurm", "Regenwurm", "Earthworm");
        t("bait.maden", "Maden", "Maggots");
        t("bait.teig", "Teigkugel", "Dough ball");
        t("bait.fliege", "Kunstfliege", "Artificial fly");
        t("bait.koederfisch", "Köderfisch", "Bait fish");
        t("bait.goldspinner", "Goldspinner", "Golden spinner");
        t("bait.none", "kein Köder", "no bait");
        t("bait.legends", "Legenden und Epische", "legendary and epic fish");
        t("baits.title", "Köderdose", "Bait tin");
        t("baits.lore", "<gray>Lockt: <white><targets></white></gray>\n<gray>In der Dose: <white><n></white></gray>",
                "<gray>Attracts: <white><targets></white></gray>\n<gray>In the tin: <white><n></white></gray>");
        t("baits.click", "<em>Klick: einhängen</em>", "<em>Click: put it on</em>");
        t("baits.active", "<au>Eingehängt</au> <gray>– Klick: aushängen</gray>", "<au>On the hook</au> <gray>– click: take it off</gray>");
        t("baits.empty", "<gray>Leer – gibt's beim Fischhändler.</gray>", "<gray>Empty – the fishmonger sells it.</gray>");
        t("baits.on", "Köder eingehängt: <white><bait></white>.", "Bait on the hook: <white><bait></white>.");
        t("baits.off", "Köder ausgehängt.", "Bait taken off.");
        t("baits.info", "<gray>Jeder Fang verbraucht einen Köder. Köder locken bestimmte Fische und halten Krempel fern.</gray>",
                "<gray>Every catch uses up one bait. Bait attracts certain fish and keeps junk away.</gray>");

        // ---------- Kleidung ----------
        t("cosmetic.fischerhut", "Fischerhut", "Fisher's hat");
        t("cosmetic.strohhut", "Strohhut", "Straw hat");
        t("cosmetic.trachtenhut", "Trachtenhut", "Alpine hat");
        t("cosmetic.koenigskrone", "Königskrone", "King's crown");
        t("cosmetic.anglerweste", "Anglerweste", "Fishing vest");
        t("cosmetic.regenjacke", "Gelbe Regenjacke", "Yellow raincoat");
        t("cosmetic.lederhose", "Lederhose", "Lederhosen");
        t("cosmetic.wathose", "Wathose", "Waders");
        t("cosmetic.gummistiefel", "Gummistiefel", "Rubber boots");
        t("wardrobe.title", "Kleiderschrank", "Wardrobe");
        t("wardrobe.wear", "<em>Klick: anziehen</em>", "<em>Click: put on</em>");
        t("wardrobe.worn", "<au>Angezogen</au> <gray>– Klick: ausziehen</gray>", "<au>Wearing</au> <gray>– click: take off</gray>");
        t("wardrobe.empty", "<gray>Noch leer – Kleidung gibt's beim Fischhändler.</gray>",
                "<gray>Still empty – the fishmonger sells clothes.</gray>");
        t("wardrobe.on", "Du trägst jetzt: <white><what></white>.", "You're now wearing: <white><what></white>.");
        t("wardrobe.off", "Ausgezogen: <white><what></white>.", "Taken off: <white><what></white>.");

        // ---------- Fischhändler ----------
        t("merchant.name", "<em>Fischhändler Xaver</em>", "<em>Xaver the Fishmonger</em>");
        t("merchant.title", "Fischhändler Xaver", "Xaver the Fishmonger");
        t("merchant.hint", "<gray>Klick mich an!</gray>", "<gray>Click me!</gray>");
        t("merchant.greet.0", "<em>Xaver:</em> <gray>Servus! Was hast denn heut Schönes gefangen?</gray>",
                "<em>Xaver:</em> <gray>Hello there! What lovely fish have you caught today?</gray>");
        t("merchant.greet.1", "<em>Xaver:</em> <gray>Grüß dich! Frischer Fisch? Her damit!</gray>",
                "<em>Xaver:</em> <gray>Good day! Fresh fish? Let's have it!</gray>");
        t("merchant.greet.2", "<em>Xaver:</em> <gray>Ah, mein liebster Angler! Zeig her, zeig her.</gray>",
                "<em>Xaver:</em> <gray>Ah, my favourite angler! Show me, show me.</gray>");
        t("merchant.greet.3", "<em>Xaver:</em> <gray>Petri Heil! Die großen zahl ich besonders gut.</gray>",
                "<em>Xaver:</em> <gray>Tight lines! I pay extra for the big ones.</gray>");
        t("merchant.taler", "<au>Deine Taler: <n></au>", "<au>Your thaler: <n></au>");
        t("merchant.taler.lore", "<gray>Taler gibt es nur hier im Tal – für Fänge, Aufgaben und Turniere.</gray>",
                "<gray>Thaler only exist here in the valley – for catches, tasks and contests.</gray>");
        t("merchant.sell-all", "<em>Alle Fänge verkaufen</em>", "<em>Sell all catches</em>");
        t("merchant.sell-all.lore", "<gray><n> Fänge für zusammen <au><price> Taler</au></gray>",
                "<gray><n> catches for <au><price> thaler</au> in total</gray>");
        t("merchant.nothing", "<gray>Du hast gerade nichts zum Verkaufen.</gray>", "<gray>You have nothing to sell right now.</gray>");
        t("merchant.fish.click", "<em>Klick: verkaufen</em>", "<em>Click: sell</em>");
        t("merchant.sold", "Verkauft: <white><what></white> für <au><price> Taler</au>.", "Sold: <white><what></white> for <au><price> thaler</au>.");
        t("merchant.sold-all", "Verkauft: <white><n> Fänge</white> für <au><price> Taler</au>. Du hast jetzt <au><taler> Taler</au>.",
                "Sold: <white><n> catches</white> for <au><price> thaler</au>. You now have <au><taler> thaler</au>.");
        t("merchant.tab.rods", "<em>Angeln</em>", "<em>Rods</em>");
        t("merchant.tab.rods.lore", "<gray>Bessere Angeln beißen schneller und holen öfter Seltenes heraus.</gray>",
                "<gray>Better rods get bites faster and land rare fish more often.</gray>");
        t("merchant.tab.baits", "<em>Köder</em>", "<em>Bait</em>");
        t("merchant.tab.baits.lore", "<gray>Köder locken bestimmte Fische an den Haken.</gray>",
                "<gray>Bait lures certain fish onto your hook.</gray>");
        t("merchant.tab.clothes", "<em>Kleidung</em>", "<em>Clothes</em>");
        t("merchant.tab.clothes.lore", "<gray>Hüte, Westen und Hosen zum Angeben.</gray>",
                "<gray>Hats, vests and trousers to show off.</gray>");
        t("merchant.back", "<gray>← Zurück</gray>", "<gray>← Back</gray>");
        t("merchant.rods.title", "Xaver · Angeln", "Xaver · Rods");
        t("merchant.baits.title", "Xaver · Köder", "Xaver · Bait");
        t("merchant.clothes.title", "Xaver · Kleidung", "Xaver · Clothes");
        t("shop.price", "<gray>Preis: <au><price> Taler</au></gray>", "<gray>Price: <au><price> thaler</au></gray>");
        t("shop.buy", "<em>Klick: kaufen</em>", "<em>Click: buy</em>");
        t("shop.owned", "<em>Gehört dir</em> <gray>– Klick: benutzen</gray>", "<em>Yours</em> <gray>– click: use it</gray>");
        t("shop.owned-cosmetic", "<em>Gehört dir</em> <gray>– im Kleiderschrank</gray>", "<em>Yours</em> <gray>– in your wardrobe</gray>");
        t("shop.active", "<au>In deiner Hand</au>", "<au>In your hand</au>");
        t("shop.crown", "<gray>Nur für Turniersieger.</gray>", "<gray>Only for contest winners.</gray>");
        t("shop.bait.lore", "<gray>Packung mit <white><n></white> Stück</gray>\n<gray>Lockt: <white><targets></white></gray>\n<gray>Du hast: <white><have></white></gray>",
                "<gray>Pack of <white><n></white></gray>\n<gray>Attracts: <white><targets></white></gray>\n<gray>You have: <white><have></white></gray>");
        t("shop.bought", "Gekauft: <white><what></white> für <au><price> Taler</au>.", "Bought: <white><what></white> for <au><price> thaler</au>.");
        t("shop.too-expensive", "<gray>Dafür fehlen dir noch <au><n> Taler</au>.</gray>", "<gray>You still need <au><n> more thaler</au>.</gray>");
        t("shop.rod-used", "Du angelst jetzt mit der <white><what></white>.", "You're now fishing with the <white><what></white>.");

        // ---------- Fischbuch ----------
        t("book.title", "Fischbuch", "Fish book");
        t("book.summary", "<au>Dein Fischbuch</au>", "<au>Your fish book</au>");
        t("book.summary.lore", "<gray>Arten: <white><n>/<total></white></gray>\n<gray>Fänge: <white><catches></white> · Fundstücke: <white><finds></white></gray>\n<gray>Legendär: <white><legendary></white></gray>\n<gray>Schwerster Fisch: <white><heaviest></white></gray>\n<gray>Turniersiege: <white><wins></white></gray>\n<gray>Verdient: <au><earned> Taler</au></gray>",
                "<gray>Species: <white><n>/<total></white></gray>\n<gray>Catches: <white><catches></white> · Finds: <white><finds></white></gray>\n<gray>Legendary: <white><legendary></white></gray>\n<gray>Heaviest fish: <white><heaviest></white></gray>\n<gray>Contests won: <white><wins></white></gray>\n<gray>Earned: <au><earned> thaler</au></gray>");
        t("book.unknown", "<dark_gray>???</dark_gray>", "<dark_gray>???</dark_gray>");
        t("book.unknown.lore", "<gray>Noch nicht gefangen.</gray>\n<gray>Lebt: <white><spots></white></gray>",
                "<gray>Not caught yet.</gray>\n<gray>Lives in: <white><spots></white></gray>");
        t("book.entry.lore", "<gray>Gefangen: <white><count>×</white></gray>\n<gray>Größter: <white><best></white></gray>\n<gray>Lebt: <white><spots></white></gray>\n<gray>Beißt gern: <white><hint></white></gray>\n<gray>Talrekord: <white><record></white></gray>",
                "<gray>Caught: <white><count>×</white></gray>\n<gray>Biggest: <white><best></white></gray>\n<gray>Lives in: <white><spots></white></gray>\n<gray>Bites best: <white><hint></white></gray>\n<gray>Valley record: <white><record></white></gray>");
        t("book.record", "<weight> (<name>)", "<weight> (<name>)");
        t("book.no-record", "noch keiner", "none yet");

        // ---------- Aufgaben ----------
        t("quests.title", "Aufgaben des Tages", "Daily tasks");
        t("quest.total", "Fang <n> Fische", "Catch <n> fish");
        t("quest.spot", "Fang <n> Fische am Fangplatz „<spot>“", "Catch <n> fish at the “<spot>”");
        t("quest.species", "Fang <n>× <fish>", "Catch <n>× <fish>");
        t("quest.rarity", "Fang <n> seltene Fische (oder besser)", "Catch <n> rare fish (or better)");
        t("quest.heavy", "Fang einen Fisch über <weight>", "Catch a fish over <weight>");
        t("quest.sell", "Verkaufe Fische für <n> Taler", "Sell fish for <n> thaler");
        t("quest.bait", "Fang <n> Fische mit Köder", "Catch <n> fish using bait");
        t("quest.progress", "<gray>Fortschritt: <white><done>/<n></white> <em><bar></em></gray>",
                "<gray>Progress: <white><done>/<n></white> <em><bar></em></gray>");
        t("quest.reward", "<gray>Belohnung: <au><n> Taler</au></gray>", "<gray>Reward: <au><n> thaler</au></gray>");
        t("quest.done", "<em>✔ Erledigt</em>", "<em>✔ Done</em>");
        t("quest.reset", "<gray>Neue Aufgaben gibt es jeden Tag um Mitternacht.</gray>", "<gray>New tasks arrive every day at midnight.</gray>");
        t("quest.finished", "<em>Aufgabe erledigt:</em> <white><quest></white> <au>+<n> Taler</au>",
                "<em>Task done:</em> <white><quest></white> <au>+<n> thaler</au>");
        t("quest.easy", "<em>Leicht</em>", "<em>Easy</em>");
        t("quest.medium", "<au>Mittel</au>", "<au>Medium</au>");
        t("quest.hard", "<red>Schwer</red>", "<red>Hard</red>");

        // ---------- Wegweiser ----------
        t("spots.title", "Wegweiser", "Signpost");
        t("spots.village", "<em>Dorf am See</em>", "<em>Lakeside village</em>");
        t("spots.village.lore", "<gray>Fischhändler, Steg, Lagerfeuer und Bestenlisten.</gray>",
                "<gray>Fishmonger, jetty, campfire and leaderboards.</gray>");
        t("spots.now", "<gray>Jetzt (<white><time></white>, <white><weather></white>) beißen gern:</gray>",
                "<gray>Right now (<white><time></white>, <white><weather></white>) biting well:</gray>");
        t("spots.fish", "<gray>·</gray> <c><fish></c> <gray><chance></gray>", "<gray>·</gray> <c><fish></c> <gray><chance></gray>");
        t("spots.go", "<em>Klick: hinreisen</em>", "<em>Click: travel there</em>");
        t("spots.arrived", "<gray>Du bist jetzt am <white><spot></white>.</gray>", "<gray>You're now at the <white><spot></white>.</gray>");
        t("spots.arrived.village", "<gray>Zurück im <white>Dorf am See</white>.</gray>", "<gray>Back in the <white>lakeside village</white>.</gray>");

        // ---------- Tafeln ----------
        t("board.records", "<au><b>Größte Fänge</b></au>", "<au><b>Biggest catches</b></au>");
        t("board.records.part", "<em><spot></em>", "<em><spot></em>");
        t("board.line", "<c><fish></c> <white><weight></white> <gray><name></gray>", "<c><fish></c> <white><weight></white> <gray><name></gray>");
        t("board.line.empty", "<c><fish></c> <dark_gray>–</dark_gray>", "<c><fish></c> <dark_gray>–</dark_gray>");
        t("board.top", "<au><b>Meiste Fänge</b></au>", "<au><b>Most catches</b></au>");
        t("board.wins", "<au><b>Turniersiege</b></au>", "<au><b>Contest wins</b></au>");
        t("board.rank", "<gray><n>.</gray> <white><name></white> <au><value></au>", "<gray><n>.</gray> <white><name></white> <au><value></au>");
        t("board.empty", "<gray>Noch niemand – vielleicht du?</gray>", "<gray>Nobody yet – maybe you?</gray>");
        t("board.king", "<au>♛ Angelkönig:</au> <white><name></white>", "<au>♛ Angler King:</au> <white><name></white>");
        t("board.king.none", "<gray>Noch kein Angelkönig – gewinn das nächste Turnier!</gray>",
                "<gray>No Angler King yet – win the next contest!</gray>");
        t("spotboard.title", "<em><b><spot></b></em>", "<em><b><spot></b></em>");
        t("spotboard.now", "<gray><clock> · <time> · <weather></gray>", "<gray><clock> · <time> · <weather></gray>");
        t("spotboard.bites", "<gray>Beißt gerade gern:</gray>", "<gray>Biting well right now:</gray>");
        t("spotboard.record", "<gray>Rekord hier: <c><fish></c> <au><weight></au> <gray>(<name>)</gray></gray>",
                "<gray>Record here: <c><fish></c> <au><weight></au> <gray>(<name>)</gray></gray>");

        // ---------- Leiste ----------
        t("sidebar.title", "<au><b>⚓ Angelkönig</b></au>", "<au><b>⚓ Angler King</b></au>");
        t("sidebar.taler", "Taler: <au><n></au>", "Thaler: <au><n></au>");
        t("sidebar.catches", "Fänge: <white><n></white> <gray>· Arten <species>/<total></gray>",
                "Catches: <white><n></white> <gray>· species <species>/<total></gray>");
        t("sidebar.place", "Ort: <white><place></white>", "Place: <white><place></white>");
        t("sidebar.time", "<white><clock></white> <gray>·</gray> <white><time></white> <gray>·</gray> <white><weather></white>",
                "<white><clock></white> <gray>·</gray> <white><time></white> <gray>·</gray> <white><weather></white>");
        t("sidebar.bait", "Köder: <white><bait></white>", "Bait: <white><bait></white>");
        t("sidebar.next", "Turnier in <au><time></au>", "Contest in <au><time></au>");
        t("sidebar.soon", "<au>Turnier gleich! <time></au>", "<au>Contest soon! <time></au>");
        t("sidebar.running", "<au><b>Turnier</b> · <time></au>", "<au><b>Contest</b> · <time></au>");
        t("sidebar.rank", "<gray><n>.</gray> <white><name></white> <au><weight></au>", "<gray><n>.</gray> <white><name></white> <au><weight></au>");
        t("sidebar.you", "<gray>Du:</gray> <white><n>.</white> <au><weight></au>", "<gray>You:</gray> <white><n>.</white> <au><weight></au>");
        t("sidebar.no-catch", "<gray>Noch kein Fang – los!</gray>", "<gray>No catch yet – go!</gray>");
        t("sidebar.off", "<gray>Turniere: aus</gray>", "<gray>Contests: off</gray>");
        t("sidebar.king", "<au>♛</au> <white><name></white>", "<au>♛</au> <white><name></white>");
        t("sidebar.address", "<gray>morni.net</gray>", "<gray>morni.net</gray>");

        // ---------- Turnier ----------
        t("tourney.announce", "<au>In <secs> Sekunden beginnt das Angel-Turnier!</au> <gray>Wer in <mins> Minuten den schwersten Fisch fängt, gewinnt.</gray>",
                "<au>The fishing contest starts in <secs> seconds!</au> <gray>Whoever lands the heaviest fish in <mins> minutes wins.</gray>");
        t("tourney.start", "<au>Das Angel-Turnier läuft!</au> <gray><mins> Minuten – der schwerste Fisch gewinnt. Petri Heil!</gray>",
                "<au>The fishing contest is on!</au> <gray><mins> minutes – the heaviest fish wins. Tight lines!</gray>");
        t("tourney.start.title", "<au>Angel-Turnier</au>", "<au>Fishing contest</au>");
        t("tourney.start.sub", "<gray>Der schwerste Fisch gewinnt</gray>", "<gray>The heaviest fish wins</gray>");
        t("tourney.bar", "Angel-Turnier · <time> · <leader>", "Fishing contest · <time> · <leader>");
        t("tourney.bar.leader", "vorn: <name> (<weight>)", "leading: <name> (<weight>)");
        t("tourney.bar.nobody", "noch kein Fang", "no catch yet");
        t("tourney.lead", "<au><name></au> <gray>führt jetzt mit <white><fish></white> (<au><weight></au>).</gray>",
                "<au><name></au> <gray>now leads with <white><fish></white> (<au><weight></au>).</gray>");
        t("tourney.best", "<gray>Dein bester Turnier-Fang: <white><weight></white> – Platz <white><n></white>.</gray>",
                "<gray>Your best contest catch: <white><weight></white> – place <white><n></white>.</gray>");
        t("tourney.end.title", "<au>♛ <name></au>", "<au>♛ <name></au>");
        t("tourney.end.sub", "<gray>gewinnt mit <white><fish></white> (<weight>)</gray>", "<gray>wins with <white><fish></white> (<weight>)</gray>");
        t("tourney.end.none", "<gray>Das Turnier ist vorbei – diesmal hat niemand etwas gefangen.</gray>",
                "<gray>The contest is over – nobody caught anything this time.</gray>");
        t("tourney.result.head", "<au>Ergebnis des Angel-Turniers</au>", "<au>Fishing contest results</au>");
        t("tourney.result.line", "<gray><n>.</gray> <white><name></white> <gray>–</gray> <fish> <au><weight></au> <gray>(+<prize> Taler)</gray>",
                "<gray><n>.</gray> <white><name></white> <gray>–</gray> <fish> <au><weight></au> <gray>(+<prize> thaler)</gray>");
        t("tourney.king", "<au><name></au> <gray>ist der neue</gray> <au>♛ Angelkönig</au><gray>!</gray>",
                "<au><name></au> <gray>is the new</gray> <au>♛ Angler King</au><gray>!</gray>");
        t("tourney.crown", "<au>Du hast die Königskrone bekommen!</au> <gray>Zieh sie im Kleiderschrank an.</gray>",
                "<au>You've earned the King's crown!</au> <gray>Put it on in your wardrobe.</gray>");
        t("tourney.thanks", "<gray>Danke fürs Mitmachen: <au>+<n> Taler</au>.</gray>", "<gray>Thanks for taking part: <au>+<n> thaler</au>.</gray>");
        t("tourney.skip", "<gray>Für ein Turnier sind gerade zu wenige Angler da – vielleicht beim nächsten Mal!</gray>",
                "<gray>Too few anglers for a contest right now – maybe next time!</gray>");
        t("tourney.stopped", "<gray>Das Turnier wurde abgebrochen.</gray>", "<gray>The contest was called off.</gray>");
        t("tourney.info.next", "<gray>Das nächste Angel-Turnier beginnt in <au><time></au>.</gray>",
                "<gray>The next fishing contest starts in <au><time></au>.</gray>");
        t("tourney.info.running", "<gray>Das Angel-Turnier läuft noch <au><time></au>.</gray>",
                "<gray>The fishing contest has <au><time></au> left.</gray>");
        t("tourney.info.off", "<gray>Turniere sind gerade ausgeschaltet.</gray>", "<gray>Contests are switched off right now.</gray>");

        // ---------- Figuren ----------
        t("boatman.name", "<em>Bootsmann Hias</em>", "<em>Hias the Boatman</em>");
        t("boatman.tip.0", "<em>Hias:</em> <gray>Die Ruderboote am Steg sind frei – steig ein und angel mitten auf dem See!</gray>",
                "<em>Hias:</em> <gray>The rowing boats by the jetty are free – hop in and fish in the middle of the lake!</gray>");
        t("boatman.tip.1", "<em>Hias:</em> <gray>Nachts beißen im Moorweiher die Aale. Unheimlich, aber lohnend!</gray>",
                "<em>Hias:</em> <gray>At night the eels bite in the bog pond. Spooky, but worth it!</gray>");
        t("boatman.tip.2", "<em>Hias:</em> <gray>Am Eisloch fangen sich Saiblinge und Rutten – zieh dich warm an!</gray>",
                "<em>Hias:</em> <gray>Char and burbot bite at the ice hole – wrap up warm!</gray>");
        t("boatman.tip.3", "<em>Hias:</em> <gray>Bei Regen werden die Forellen im Bach richtig munter.</gray>",
                "<em>Hias:</em> <gray>When it rains, the trout in the stream get really lively.</gray>");
        t("boatman.tip.4", "<em>Hias:</em> <gray>Köderfische locken die großen Räuber – Hecht, Zander, Waller.</gray>",
                "<em>Hias:</em> <gray>Bait fish lure the big predators – pike, zander, catfish.</gray>");
        t("boatman.tip.5", "<em>Hias:</em> <gray>Man erzählt sich von einem riesigen Waller im Bergsee – dem Seekönig …</gray>",
                "<em>Hias:</em> <gray>They say there's a giant catfish in the mountain lake – the Lake King …</gray>");
        t("boatman.tip.6", "<em>Hias:</em> <gray>Früh am Morgen glitzert manchmal eine goldene Forelle im Bach.</gray>",
                "<em>Hias:</em> <gray>Early in the morning a golden trout sometimes glitters in the stream.</gray>");

        // ---------- Statistik ----------
        t("stats.head", "<au>Angelkönig – <name></au>", "<au>Angler King – <name></au>");
        t("stats.none", "<gray>Von <white><name></white> gibt es noch keine Werte.</gray>", "<gray>No stats for <white><name></white> yet.</gray>");
        t("stats.line", "<gray>Fänge <white><catches></white> · Arten <white><species>/<total></white> · Legendär <white><legendary></white> · Turniersiege <white><wins></white></gray>",
                "<gray>Catches <white><catches></white> · species <white><species>/<total></white> · legendary <white><legendary></white> · contest wins <white><wins></white></gray>");
        t("stats.line2", "<gray>Schwerster Fisch <white><heaviest></white> · Taler <au><taler></au></gray>",
                "<gray>Heaviest fish <white><heaviest></white> · thaler <au><taler></au></gray>");
        t("top.catches", "<au>Meiste Fänge</au>", "<au>Most catches</au>");
        t("top.wins", "<au>Turniersiege</au>", "<au>Contest wins</au>");
        t("top.records", "<au>Größte Fänge im Tal</au>", "<au>Biggest catches in the valley</au>");
        t("top.line", "<gray><n>.</gray> <white><name></white> <gray>–</gray> <au><value></au>", "<gray><n>.</gray> <white><name></white> <gray>–</gray> <au><value></au>");
        t("top.record", "<c><fish></c><gray>:</gray> <au><weight></au> <gray>(<name>)</gray>", "<c><fish></c><gray>:</gray> <au><weight></au> <gray>(<name>)</gray>");
        t("top.empty", "<gray>Noch niemand.</gray>", "<gray>Nobody yet.</gray>");
        t("top.usage", "<gray>/top [faenge|turniere|rekorde]</gray>", "<gray>/top [catches|wins|records]</gray>");

        // ---------- Verwaltung ----------
        t("admin.usage", "<gray>/angeladmin info|status|turnier <start|ende|stopp>|zeit <morgen|tag|abend|nacht>|wetter <klar|regen|gewitter>|taler <spieler> <betrag>|naechster <spieler> <art> [cm]|fisch <spieler> <art> [cm]|fischbuch <spieler> <alle|leer|art> [cm]|aufgabe <spieler> <0-2> <art:ziel:anzahl:belohnung>|melden|neubauen|neuladen</gray>",
                "<gray>/angeladmin info|status|turnier <start|ende|stopp>|zeit <morgen|tag|abend|nacht>|wetter <klar|regen|gewitter>|taler <spieler> <betrag>|naechster <spieler> <art> [cm]|fisch <spieler> <art> [cm]|fischbuch <spieler> <alle|leer|art> [cm]|aufgabe <spieler> <0-2> <art:ziel:anzahl:belohnung>|melden|neubauen|neuladen</gray>");
        t("admin.done", "<em>Erledigt.</em>", "<em>Done.</em>");
        t("admin.rebuild", "<gray>Beim nächsten Start entsteht das Tal neu.</gray>", "<gray>The valley will be rebuilt on the next start.</gray>");
        t("admin.reloaded", "<em>Einstellungen neu geladen.</em>", "<em>Settings reloaded.</em>");
    }

    /** Alle Schlüssel (für Tests). */
    public static Set<String> keys() {
        return Collections.unmodifiableSet(TEXTS.keySet());
    }

    public static boolean has(String key) {
        return TEXTS.containsKey(key);
    }

    /** Roher MiniMessage-Text. Unbekannte Sprache → Deutsch. */
    public static String raw(String lang, String key) {
        String[] t = TEXTS.get(key);
        if (t == null) {
            return key;
        }
        return "en".equals(lang) ? t[1] : t[0];
    }

    /** Text ohne Vorsilbe; nie kursiv (für Gegenstände). */
    public static Component text(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, key), resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** Nachricht für den Chat, mit „⚓ Angelkönig »“ davor. */
    public static Component msg(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, "prefix")).append(MM.deserialize(raw(lang, key), resolvers));
    }

    /** Mehrzeiliger Text als einzelne Zeilen (für Beschreibungen von Gegenständen). */
    public static List<Component> lines(String lang, String key, TagResolver... resolvers) {
        List<Component> out = new ArrayList<>();
        for (String line : raw(lang, key).split("\n")) {
            out.add(MM.deserialize(line, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        return out;
    }

    /** Reiner Text ohne Formatierung (z. B. Fischnamen in Listen). */
    public static String plain(String lang, String key) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(MM.deserialize(raw(lang, key)));
    }

    /** Farbe für {@code <c>} (Seltenheit). */
    public static TagResolver colour(int rgb) {
        return TagResolver.resolver("c", Tag.styling(TextColor.color(rgb)));
    }
}
