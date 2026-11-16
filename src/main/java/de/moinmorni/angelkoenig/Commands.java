package de.moinmorni.angelkoenig;

import de.moinmorni.angelkoenig.data.PlayerData;
import de.moinmorni.angelkoenig.data.Records;
import de.moinmorni.angelkoenig.fish.DayTime;
import de.moinmorni.angelkoenig.fish.Quest;
import de.moinmorni.angelkoenig.fish.Species;
import de.moinmorni.angelkoenig.fish.Tournament;
import de.moinmorni.angelkoenig.fish.Units;
import de.moinmorni.angelkoenig.fish.Weather;
import de.moinmorni.angelkoenig.play.Menus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** Spieler-Befehle (Fischbuch, Aufgaben, Wegweiser …) und /angeladmin zum Verwalten und Testen. */
public final class Commands implements TabExecutor {

    private final AngelkoenigPlugin pl;

    public Commands(AngelkoenigPlugin pl) {
        this.pl = pl;
    }

    private String lang(CommandSender s) {
        return s instanceof Player p ? pl.lang(p) : "de";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("angeladmin")) {
            admin(sender, args);
            return true;
        }
        if (name.equals("stats")) {
            stats(sender, args);
            return true;
        }
        if (name.equals("top")) {
            top(sender, args);
            return true;
        }
        if (name.equals("turnier")) {
            tournament(sender);
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Texts.text("de", "only-players"));
            return true;
        }
        if (!pl.ready()) {
            p.sendMessage(Texts.msg(pl.lang(p), "not-ready"));
            return true;
        }
        switch (name) {
            case "fischbuch" -> Menus.book(pl, p);
            case "aufgaben" -> Menus.quests(pl, p);
            case "wegweiser" -> Menus.spots(pl, p);
            case "koeder" -> Menus.baits(pl, p);
            case "kleiderschrank" -> Menus.wardrobe(pl, p);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void tournament(CommandSender s) {
        String lang = lang(s);
        Tournament t = pl.tournaments().state();
        long now = System.currentTimeMillis();
        if (t.running()) {
            s.sendMessage(Texts.msg(lang, "tourney.info.running", Placeholder.unparsed("time", Ui.clock((t.endsAt() - now) / 1000))));
        } else if (!t.enabled()) {
            s.sendMessage(Texts.msg(lang, "tourney.info.off"));
        } else {
            s.sendMessage(Texts.msg(lang, "tourney.info.next", Placeholder.unparsed("time", Ui.clock((t.nextStart() - now) / 1000))));
        }
    }

    private void stats(CommandSender s, String[] args) {
        String lang = lang(s);
        Optional<PlayerData> d;
        String who;
        if (args.length > 0) {
            who = args[0];
            Player online = Bukkit.getPlayerExact(who);
            UUID uuid = online != null ? online.getUniqueId() : offline(who);
            d = uuid == null ? Optional.empty() : pl.store().read(uuid);
        } else if (s instanceof Player p) {
            who = p.getName();
            d = Optional.of(pl.data(p));
        } else {
            s.sendMessage(Texts.text(lang, "only-players"));
            return;
        }
        if (d.isEmpty()) {
            s.sendMessage(Texts.msg(lang, "stats.none", Placeholder.unparsed("name", who)));
            return;
        }
        PlayerData x = d.get();
        s.sendMessage(Texts.msg(lang, "stats.head", Placeholder.unparsed("name", x.name())));
        s.sendMessage(Texts.text(lang, "stats.line", Placeholder.unparsed("catches", Units.number(x.catches(), lang)),
                Placeholder.unparsed("species", String.valueOf(x.speciesCount())),
                Placeholder.unparsed("total", String.valueOf(Species.fishes().size())),
                Placeholder.unparsed("legendary", String.valueOf(x.legendary())),
                Placeholder.unparsed("wins", String.valueOf(x.tournamentsWon()))));
        s.sendMessage(Texts.text(lang, "stats.line2", Placeholder.unparsed("heaviest", x.heaviest() <= 0 ? "–"
                : Units.weight(x.heaviest(), lang)), Placeholder.unparsed("taler", Units.number(x.taler(), lang))));
    }

    @SuppressWarnings("deprecation")
    private static UUID offline(String name) {
        OfflinePlayer o = Bukkit.getOfflinePlayerIfCached(name);
        return o == null ? null : o.getUniqueId();
    }

    private void top(CommandSender s, String[] args) {
        String lang = lang(s);
        String what = args.length == 0 ? "faenge" : args[0].toLowerCase(Locale.ROOT);
        Records r = pl.records();
        switch (what) {
            case "faenge", "catches" -> list(s, lang, "top.catches", r.topCatches(10), true);
            case "turniere", "wins" -> list(s, lang, "top.wins", r.topWins(10), false);
            case "rekorde", "records" -> {
                s.sendMessage(Texts.msg(lang, "top.records"));
                boolean any = false;
                for (Species sp : Species.fishes()) {
                    Records.Best b = r.best(sp);
                    if (b != null) {
                        any = true;
                        s.sendMessage(Texts.text(lang, "top.record", Texts.colour(sp.rarity().colour()),
                                Placeholder.unparsed("fish", Texts.plain(lang, "species." + sp.id())),
                                Placeholder.unparsed("weight", Units.weight(b.grams(), lang)),
                                Placeholder.unparsed("name", b.name())));
                    }
                }
                if (!any) {
                    s.sendMessage(Texts.text(lang, "top.empty"));
                }
            }
            default -> s.sendMessage(Texts.msg(lang, "top.usage"));
        }
    }

    private void list(CommandSender s, String lang, String head, List<Records.Totals> rows, boolean catches) {
        s.sendMessage(Texts.msg(lang, head));
        if (rows.isEmpty()) {
            s.sendMessage(Texts.text(lang, "top.empty"));
        }
        for (int i = 0; i < rows.size(); i++) {
            Records.Totals t = rows.get(i);
            s.sendMessage(Texts.text(lang, "top.line", Placeholder.unparsed("n", String.valueOf(i + 1)),
                    Placeholder.unparsed("name", t.name()),
                    Placeholder.unparsed("value", Units.number(catches ? t.catches() : t.wins(), lang))));
        }
    }

    // ---------- Verwaltung ----------

    private void admin(CommandSender s, String[] a) {
        String lang = lang(s);
        if (a.length == 0) {
            s.sendMessage(Texts.text(lang, "admin.usage"));
            return;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "info" -> s.sendMessage(Component.text("Angelkönig " + pl.getPluginMeta().getVersion() + " · Welt "
                    + pl.world().getName() + " · " + pl.world().getPlayers().size() + " Spieler · Turnier "
                    + pl.tournaments().state().phase() + " · Netzwerk " + (pl.net().connected() ? "ja" : "nein")));
            case "status" -> s.sendMessage(Component.text(status()));
            case "turnier" -> {
                String sub = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "";
                switch (sub) {
                    case "start" -> pl.tournaments().startNow();
                    case "ende" -> pl.tournaments().endNow();
                    case "stopp" -> pl.tournaments().stop();
                    default -> {
                        s.sendMessage(Texts.text(lang, "admin.usage"));
                        return;
                    }
                }
                s.sendMessage(Texts.text(lang, "admin.done"));
            }
            case "zeit" -> {
                Optional<DayTime> t = a.length > 1 ? DayTime.byId(a[1]) : Optional.empty();
                if (t.isEmpty()) {
                    s.sendMessage(Texts.text(lang, "admin.usage"));
                    return;
                }
                pl.world().setTime(t.get().sampleTicks());
                pl.boards().refreshSpots();
                s.sendMessage(Texts.text(lang, "admin.done"));
            }
            case "wetter" -> {
                Optional<Weather> w = a.length > 1 ? Weather.byId(a[1]) : Optional.empty();
                if (w.isEmpty()) {
                    s.sendMessage(Texts.text(lang, "admin.usage"));
                    return;
                }
                pl.ambience().setWeather(w.get(), 600);
                s.sendMessage(Texts.text(lang, "admin.done"));
            }
            case "taler" -> {
                Player p = a.length > 2 ? Bukkit.getPlayerExact(a[1]) : null;
                if (p == null) {
                    s.sendMessage(Texts.text(lang, "admin.usage"));
                    return;
                }
                try {
                    pl.data(p).setTaler(Long.parseLong(a[2]));
                } catch (NumberFormatException e) {
                    s.sendMessage(Texts.text(lang, "admin.usage"));
                    return;
                }
                pl.store().save(pl.data(p));
                s.sendMessage(Texts.text(lang, "admin.done"));
            }
            case "naechster" -> {
                Player p = a.length > 2 ? Bukkit.getPlayerExact(a[1]) : null;
                Optional<Species> sp = a.length > 2 ? Species.byId(a[2]) : Optional.empty();
                if (p == null || sp.isEmpty()) {
                    s.sendMessage(Texts.text(lang, "admin.usage"));
                    return;
                }
                Double cm = null;
                if (a.length > 3) {
                    try {
                        cm = Double.parseDouble(a[3].replace(',', '.'));
                    } catch (NumberFormatException e) {
                        cm = null;
                    }
                }
                pl.angling().force(p.getUniqueId(), sp.get(), cm);
                s.sendMessage(Texts.text(lang, "admin.done"));
            }
            case "aufgabe" -> {
                Player p = a.length > 3 ? Bukkit.getPlayerExact(a[1]) : null;
                Optional<Quest> q = a.length > 3 ? Quest.parse(a[3]) : Optional.empty();
                int idx;
                try {
                    idx = a.length > 2 ? Integer.parseInt(a[2]) : -1;
                } catch (NumberFormatException e) {
                    idx = -1;
                }
                if (p == null || q.isEmpty() || idx < 0 || idx > 2) {
                    s.sendMessage(Texts.text(lang, "admin.usage"));
                    return;
                }
                PlayerData d = pl.data(p);
                pl.angling().ensureQuests(d);
                d.setQuest(idx, q.get());
                pl.store().save(d);
                s.sendMessage(Texts.text(lang, "admin.done"));
            }
            case "melden" -> {
                pl.angling().flushAll();
                pl.writeRecordsNow();
                s.sendMessage(Texts.text(lang, "admin.done"));
            }
            case "neubauen" -> {
                try {
                    pl.worldSetup().scheduleRebuild();
                    s.sendMessage(Texts.text(lang, "admin.rebuild"));
                } catch (IOException e) {
                    s.sendMessage(Component.text("Fehler: " + e.getMessage()));
                }
            }
            case "neuladen" -> {
                pl.reloadSettings();
                s.sendMessage(Texts.text(lang, "admin.reloaded"));
            }
            default -> s.sendMessage(Texts.text(lang, "admin.usage"));
        }
    }

    /** Maschinenlesbar für Tests: Turnier, Zeit, Wetter, je Spieler Taler, Fänge, Arten, Angel, Köder, Aufgaben. */
    private String status() {
        Tournament t = pl.tournaments().state();
        StringBuilder sb = new StringBuilder();
        sb.append("turnier=").append(t.phase().name().toLowerCase(Locale.ROOT))
                .append(" zeit=").append(pl.angling().time().id())
                .append(" wetter=").append(pl.angling().weather().id())
                .append(" koenig=").append(pl.records().king() == null ? "-" : pl.records().kingName());
        for (Tournament.Entry e : t.ranking()) {
            sb.append(" t:").append(e.name()).append('=').append(e.grams());
        }
        for (Player p : pl.world().getPlayers()) {
            PlayerData d = pl.data(p);
            sb.append('\n').append(p.getName()).append(" taler=").append(d.taler()).append(" faenge=").append(d.catches())
                    .append(" arten=").append(d.speciesCount()).append(" angel=").append(d.rod().id())
                    .append(" koeder=").append(d.bait() == null ? "-" : d.bait().id() + ":" + d.baits(d.bait()))
                    .append(" siege=").append(d.tournamentsWon()).append(" schwerster=").append(d.heaviest());
            List<Quest> qs = d.quests();
            for (int i = 0; i < qs.size(); i++) {
                sb.append(" a").append(i).append('=').append(qs.get(i).spec()).append('|').append(d.progress(i))
                        .append('|').append(d.done(i));
            }
            sb.append(" ort=").append(pl.angling().spotAt(p.getLocation()).id());
        }
        return sb.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        if (name.equals("angeladmin")) {
            if (args.length == 1) {
                options.addAll(List.of("info", "status", "turnier", "zeit", "wetter", "taler", "naechster", "aufgabe",
                        "melden", "neubauen", "neuladen"));
            } else if (args.length == 2) {
                switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "turnier" -> options.addAll(List.of("start", "ende", "stopp"));
                    case "zeit" -> Arrays.stream(DayTime.values()).forEach(d -> options.add(d.id()));
                    case "wetter" -> Arrays.stream(Weather.values()).forEach(w -> options.add(w.id()));
                    default -> Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("naechster")) {
                Arrays.stream(Species.values()).forEach(sp -> options.add(sp.id()));
            }
        } else if (name.equals("top") && args.length == 1) {
            options.addAll(List.of("faenge", "turniere", "rekorde"));
        } else if (name.equals("stats") && args.length == 1) {
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
        }
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(last)).toList();
    }
}
