package net.morni.angelkoenig.play;

import net.morni.angelkoenig.AngelkoenigPlugin;
import net.morni.angelkoenig.Settings;
import net.morni.angelkoenig.Texts;
import net.morni.angelkoenig.Ui;
import net.morni.angelkoenig.data.PlayerData;
import net.morni.angelkoenig.fish.Catch;
import net.morni.angelkoenig.fish.Tournament;
import net.morni.angelkoenig.fish.Units;
import net.morni.angelkoenig.world.Layout;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Das Angel-Turnier im Spiel: Ankündigung, Start mit Titel, Leiste oben mit Restzeit und Führendem, Wertung am Ende
 * mit Talern, Königskrone und neuem Angelkönig. Die Regeln selbst stehen in {@link Tournament}.
 */
public final class Tournaments {

    private static final long SECOND = 1000L;
    private final AngelkoenigPlugin pl;
    private final Tournament t;
    private final BossBar barDe = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.YELLOW, BossBar.Overlay.NOTCHED_10);
    private final BossBar barEn = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.YELLOW, BossBar.Overlay.NOTCHED_10);
    private final Set<UUID> seeing = new HashSet<>();
    private UUID leader;

    public Tournaments(AngelkoenigPlugin pl) {
        this.pl = pl;
        Settings s = pl.settings();
        t = new Tournament(s.tournaments(), s.tournamentEvery() * 60_000L, s.tournamentMinutes() * 60_000L,
                s.tournamentWarn() * SECOND, s.tournamentMinPlayers(), System.currentTimeMillis());
    }

    public void reconfigure() {
        Settings s = pl.settings();
        t.configure(s.tournaments(), s.tournamentEvery() * 60_000L, s.tournamentMinutes() * 60_000L,
                s.tournamentWarn() * SECOND, s.tournamentMinPlayers(), System.currentTimeMillis());
    }

    public Tournament state() {
        return t;
    }

    private List<Player> players() {
        return pl.world().getPlayers();
    }

    /** Jede Sekunde. */
    public void tick() {
        long now = System.currentTimeMillis();
        for (Tournament.Event e : t.tick(now, players().size())) {
            switch (e) {
                case Tournament.Announce a -> announce(a.startsAt() - now);
                case Tournament.Start s -> started();
                case Tournament.End end -> finished(end.ranking());
                case Tournament.Skip skip -> players().forEach(p -> p.sendMessage(Texts.msg(pl.lang(p), "tourney.skip")));
            }
        }
        updateBar(now);
    }

    private void announce(long inMs) {
        long secs = Math.max(1, Math.round(inMs / 1000.0));
        for (Player p : players()) {
            String lang = pl.lang(p);
            p.sendMessage(Texts.msg(lang, "tourney.announce", Placeholder.unparsed("secs", String.valueOf(secs)),
                    Placeholder.unparsed("mins", String.valueOf(t.duration() / 60_000L))));
            Ui.sound(p, "block.note_block.bell", 1.2f);
        }
    }

    private void started() {
        leader = null;
        String mins = String.valueOf(Math.max(1, t.duration() / 60_000L));
        for (Player p : players()) {
            String lang = pl.lang(p);
            p.sendMessage(Texts.msg(lang, "tourney.start", Placeholder.unparsed("mins", mins)));
            Ui.title(p, Texts.text(lang, "tourney.start.title"), Texts.text(lang, "tourney.start.sub"), 2500);
            Ui.sound(p, "block.bell.use", 1.0f);
            Ui.sound(p, "item.goat_horn.sound.0", 1.0f);
        }
        pl.stateChanged();
    }

    /** Admin: sofort starten. */
    public void startNow() {
        t.start(System.currentTimeMillis());
        started();
    }

    /** Admin: sofort beenden und werten. */
    public void endNow() {
        if (t.running()) {
            finished(t.end(System.currentTimeMillis()));
        }
    }

    /** Admin: abbrechen ohne Wertung. */
    public void stop() {
        if (t.running()) {
            t.abort(System.currentTimeMillis());
            players().forEach(p -> p.sendMessage(Texts.msg(pl.lang(p), "tourney.stopped")));
            hideBars();
            pl.stateChanged();
        }
    }

    /** Fang ins Turnier eintragen. */
    public void record(Player p, Catch c) {
        long now = System.currentTimeMillis();
        if (!t.record(p.getUniqueId(), p.getName(), c, now)) {
            return;
        }
        String lang = pl.lang(p);
        p.sendMessage(Texts.msg(lang, "tourney.best", Placeholder.unparsed("weight", Units.weight(c.grams(), lang)),
                Placeholder.unparsed("n", String.valueOf(t.place(p.getUniqueId())))));
        Tournament.Entry top = t.ranking().get(0);
        if (!top.uuid().equals(leader)) {
            leader = top.uuid();
            for (Player o : players()) {
                String ol = pl.lang(o);
                o.sendMessage(Texts.msg(ol, "tourney.lead", Placeholder.unparsed("name", top.name()),
                        Placeholder.unparsed("fish", Texts.plain(ol, "species." + top.species().id())),
                        Placeholder.unparsed("weight", Units.weight(top.grams(), ol))));
            }
        }
    }

    private void finished(List<Tournament.Entry> ranking) {
        hideBars();
        List<Integer> prizes = pl.settings().prizes();
        int thanks = pl.settings().participation();
        if (ranking.isEmpty()) {
            players().forEach(p -> p.sendMessage(Texts.msg(pl.lang(p), "tourney.end.none")));
            pl.stateChanged();
            return;
        }
        Tournament.Entry win = ranking.get(0);
        for (int i = 0; i < ranking.size(); i++) {
            Tournament.Entry e = ranking.get(i);
            int prize = i < prizes.size() ? prizes.get(i) : thanks;
            PlayerData d = pl.store().get(e.uuid(), e.name());
            d.earn(prize);
            d.tournamentPlayed(i == 0);
            pl.records().totals(e.uuid(), e.name(), d.catches(), d.tournamentsWon(), d.legendary());
            if (i == 0) {
                pl.angling().addStat(e.uuid(), "tournaments_won", 1);
            }
            pl.store().save(d);
            Player p = Bukkit.getPlayer(e.uuid());
            if (p == null) {
                pl.store().unload(e.uuid());
            } else if (i >= prizes.size()) {
                p.sendMessage(Texts.msg(pl.lang(p), "tourney.thanks", Placeholder.unparsed("n", String.valueOf(prize))));
            }
        }
        pl.records().king(win.uuid(), win.name());
        pl.saveRecords();
        for (Player p : players()) {
            String lang = pl.lang(p);
            p.sendMessage(Texts.msg(lang, "tourney.result.head"));
            for (int i = 0; i < Math.min(3, ranking.size()); i++) {
                Tournament.Entry e = ranking.get(i);
                p.sendMessage(Texts.text(lang, "tourney.result.line", Placeholder.unparsed("n", String.valueOf(i + 1)),
                        Placeholder.unparsed("name", e.name()),
                        Placeholder.unparsed("fish", Texts.plain(lang, "species." + e.species().id())),
                        Placeholder.unparsed("weight", Units.weight(e.grams(), lang)),
                        Placeholder.unparsed("prize", String.valueOf(i < prizes.size() ? prizes.get(i) : thanks))));
            }
            p.sendMessage(Texts.msg(lang, "tourney.king", Placeholder.unparsed("name", win.name())));
            Ui.title(p, Texts.text(lang, "tourney.end.title", Placeholder.unparsed("name", win.name())),
                    Texts.text(lang, "tourney.end.sub", Placeholder.unparsed("fish", Texts.plain(lang, "species."
                            + win.species().id())), Placeholder.unparsed("weight", Units.weight(win.grams(), lang))), 3500);
            Ui.sound(p, "ui.toast.challenge_complete", 1.0f);
            if (p.getUniqueId().equals(win.uuid())) {
                p.sendMessage(Texts.msg(lang, "tourney.crown"));
            }
        }
        // über dem See vor dem Steg (nicht zwischen den Anglern)
        Location lake = new Location(pl.world(), 0.5, Layout.WATER_Y + 1, 6.5);
        pl.angling().celebrate(lake);
        Player winner = Bukkit.getPlayer(win.uuid());
        if (winner != null) {
            // ein paar Schritte vor dem Sieger, damit er Titel und Feuerwerk sieht
            Location front = winner.getLocation();
            front.add(front.getDirection().setY(0).normalize().multiply(7)).add(0, 1, 0);
            pl.angling().celebrate(front);
        }
        ranking.forEach(e -> pl.angling().flush(e.uuid()));
        pl.boards().refreshRecords();
        pl.stateChanged();
    }

    private void updateBar(long now) {
        if (!t.running()) {
            return;
        }
        long left = Math.max(0, t.endsAt() - now);
        float progress = (float) Math.max(0, Math.min(1, left / (double) t.duration()));
        List<Tournament.Entry> r = t.ranking();
        for (BossBar bar : new BossBar[] {barDe, barEn}) {
            String lang = bar == barEn ? "en" : "de";
            String lead = r.isEmpty() ? Texts.plain(lang, "tourney.bar.nobody") : Texts.raw(lang, "tourney.bar.leader")
                    .replace("<name>", r.get(0).name()).replace("<weight>", Units.weight(r.get(0).grams(), lang));
            bar.name(Texts.text(lang, "tourney.bar", Placeholder.unparsed("time", Ui.clock(left / 1000)),
                    Placeholder.unparsed("leader", lead)));
            bar.progress(progress);
        }
        for (Player p : players()) {
            boolean en = "en".equals(pl.lang(p));
            p.hideBossBar(en ? barDe : barEn);
            p.showBossBar(en ? barEn : barDe);
            seeing.add(p.getUniqueId());
        }
    }

    private void hideBars() {
        for (UUID u : seeing) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                p.hideBossBar(barDe);
                p.hideBossBar(barEn);
            }
        }
        seeing.clear();
    }

    public void forget(Player p) {
        p.hideBossBar(barDe);
        p.hideBossBar(barEn);
        seeing.remove(p.getUniqueId());
    }
}
