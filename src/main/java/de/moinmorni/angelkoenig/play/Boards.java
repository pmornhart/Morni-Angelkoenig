package de.moinmorni.angelkoenig.play;

import de.moinmorni.angelkoenig.AngelkoenigPlugin;
import de.moinmorni.angelkoenig.Holo;
import de.moinmorni.angelkoenig.Texts;
import de.moinmorni.angelkoenig.data.Records;
import de.moinmorni.angelkoenig.fish.DayTime;
import de.moinmorni.angelkoenig.fish.Species;
import de.moinmorni.angelkoenig.fish.Spot;
import de.moinmorni.angelkoenig.fish.Units;
import de.moinmorni.angelkoenig.world.Layout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;

/**
 * Schwebende Tafeln je Sprache: drei Bestenlisten an der Tafelwand am Dorfplatz (größte Fänge je Art, meiste Fänge,
 * Turniersiege, Angelkönig), eine Tafel „Was beißt gerade?“ an jedem Fangplatz und die Namen der Figuren.
 */
public final class Boards {

    private static final float BOARD_SCALE = 0.62f;
    private static final float SPOT_SCALE = 0.75f;
    private final AngelkoenigPlugin pl;
    private final Holo[] records = new Holo[3];
    private final Map<Spot, Holo> spots = new EnumMap<>(Spot.class);
    private final List<Holo> names = new ArrayList<>();

    public Boards(AngelkoenigPlugin pl) {
        this.pl = pl;
    }

    private Location at(Layout.Place p) {
        return new Location(pl.world(), p.x(), p.y(), p.z());
    }

    /** Alle Tafeln aufstellen (fehlende neu). */
    public void spawn() {
        for (int i = 0; i < records.length; i++) {
            if (records[i] == null || !records[i].valid()) {
                int idx = i;
                records[i] = Holo.spawn(pl, at(Layout.BOARDS[i]), lang -> recordBoard(idx, lang), BOARD_SCALE);
            }
        }
        for (Spot s : Spot.values()) {
            Holo h = spots.get(s);
            if (h == null || !h.valid()) {
                spots.put(s, Holo.spawn(pl, at(Layout.SPOT_BOARDS.get(s)), lang -> spotBoard(s, lang), SPOT_SCALE));
            }
        }
        if (names.isEmpty() || names.stream().anyMatch(h -> !h.valid())) {
            names.forEach(Holo::remove);
            names.clear();
            names.add(name(Layout.MERCHANT, lang -> Texts.text(lang, "merchant.name").append(Component.newline())
                    .append(Texts.text(lang, "merchant.hint"))));
            names.add(name(Layout.BOATMAN, lang -> Texts.text(lang, "boatman.name").append(Component.newline())
                    .append(Texts.text(lang, "merchant.hint"))));
        }
    }

    private Holo name(Layout.Place p, Function<String, Component> text) {
        return Holo.spawn(pl, new Location(pl.world(), p.x(), p.y() + 2.25, p.z()), text, 0.8f);
    }

    public void refreshRecords() {
        for (int i = 0; i < records.length; i++) {
            int idx = i;
            if (records[i] != null) {
                records[i].update(lang -> recordBoard(idx, lang));
            }
        }
    }

    public void refreshSpots() {
        spots.forEach((s, h) -> h.update(lang -> spotBoard(s, lang)));
    }

    public void removeAll() {
        for (Holo h : records) {
            if (h != null) {
                h.remove();
            }
        }
        spots.values().forEach(Holo::remove);
        spots.clear();
        names.forEach(Holo::remove);
        names.clear();
    }

    /** Wo eine Art am häufigsten vorkommt. */
    static Spot home(Species s) {
        Spot best = Spot.BERGSEE;
        double w = -1;
        for (Spot sp : Spot.values()) {
            if (s.weight(sp) > w) {
                w = s.weight(sp);
                best = sp;
            }
        }
        return best;
    }

    private Component recordBoard(int idx, String lang) {
        Records r = pl.records();
        Component out = Texts.text(lang, idx == 2 ? "board.top" : "board.records");
        if (idx < 2) {
            Spot[] parts = idx == 0 ? new Spot[] {Spot.BERGSEE, Spot.BACH} : new Spot[] {Spot.MOOR, Spot.EIS};
            for (Spot sp : parts) {
                out = out.append(Component.newline()).append(Component.newline()).append(Texts.text(lang,
                        "board.records.part", Placeholder.unparsed("spot", Texts.plain(lang, "spot." + sp.id()))));
                for (Species s : Species.fishes()) {
                    if (home(s) != sp) {
                        continue;
                    }
                    Records.Best b = r.best(s);
                    String fish = Texts.plain(lang, "species." + s.id());
                    out = out.append(Component.newline()).append(b == null
                            ? Texts.text(lang, "board.line.empty", Texts.colour(s.rarity().colour()),
                                    Placeholder.unparsed("fish", fish))
                            : Texts.text(lang, "board.line", Texts.colour(s.rarity().colour()),
                                    Placeholder.unparsed("fish", fish), Placeholder.unparsed("weight",
                                            Units.weight(b.grams(), lang)), Placeholder.unparsed("name", b.name())));
                }
            }
            return out;
        }
        out = out.append(Component.newline());
        List<Records.Totals> top = r.topCatches(5);
        if (top.isEmpty()) {
            out = out.append(Component.newline()).append(Texts.text(lang, "board.empty"));
        }
        for (int i = 0; i < top.size(); i++) {
            out = out.append(Component.newline()).append(Texts.text(lang, "board.rank",
                    Placeholder.unparsed("n", String.valueOf(i + 1)), Placeholder.unparsed("name", top.get(i).name()),
                    Placeholder.unparsed("value", Units.number(top.get(i).catches(), lang))));
        }
        out = out.append(Component.newline()).append(Component.newline()).append(Texts.text(lang, "board.wins"))
                .append(Component.newline());
        List<Records.Totals> wins = r.topWins(5);
        if (wins.isEmpty()) {
            out = out.append(Component.newline()).append(Texts.text(lang, "board.empty"));
        }
        for (int i = 0; i < wins.size(); i++) {
            out = out.append(Component.newline()).append(Texts.text(lang, "board.rank",
                    Placeholder.unparsed("n", String.valueOf(i + 1)), Placeholder.unparsed("name", wins.get(i).name()),
                    Placeholder.unparsed("value", String.valueOf(wins.get(i).wins()))));
        }
        out = out.append(Component.newline()).append(Component.newline()).append(r.king() == null
                ? Texts.text(lang, "board.king.none")
                : Texts.text(lang, "board.king", Placeholder.unparsed("name", r.kingName())));
        return out;
    }

    private Component spotBoard(Spot s, String lang) {
        Location at = at(Layout.SPOT_BOARDS.get(s));
        Component out = Texts.text(lang, "spotboard.title", Placeholder.unparsed("spot", Texts.plain(lang, "spot." + s.id())));
        out = out.append(Component.newline()).append(Texts.text(lang, "spotboard.now",
                Placeholder.unparsed("clock", DayTime.clock(pl.world().getTime())),
                Placeholder.unparsed("time", Texts.plain(lang, "time." + pl.angling().time().id())),
                Placeholder.unparsed("weather", pl.angling().weatherName(lang, at))));
        out = out.append(Component.newline()).append(Texts.text(lang, "spotboard.bites"));
        for (Component line : Menus.biteList(pl, lang, s, 4)) {
            out = out.append(Component.newline()).append(line);
        }
        Records.Best best = null;
        Species bestSpecies = null;
        for (Species sp : Species.fishes()) {
            Records.Best b = pl.records().best(sp);
            if (b != null && sp.weight(s) > 0 && (best == null || b.grams() > best.grams())) {
                best = b;
                bestSpecies = sp;
            }
        }
        if (best != null) {
            out = out.append(Component.newline()).append(Texts.text(lang, "spotboard.record",
                    Texts.colour(bestSpecies.rarity().colour()),
                    Placeholder.unparsed("fish", Texts.plain(lang, "species." + bestSpecies.id())),
                    Placeholder.unparsed("weight", Units.weight(best.grams(), lang)), Placeholder.unparsed("name", best.name())));
        }
        return out;
    }
}
