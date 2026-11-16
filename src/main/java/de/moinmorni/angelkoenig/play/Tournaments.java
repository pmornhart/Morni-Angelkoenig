package de.moinmorni.angelkoenig.play;

import de.moinmorni.angelkoenig.AngelkoenigPlugin;
import de.moinmorni.angelkoenig.Settings;
import de.moinmorni.angelkoenig.Texts;
import de.moinmorni.angelkoenig.Ui;
import de.moinmorni.angelkoenig.data.PlayerData;
import de.moinmorni.angelkoenig.fish.Catch;
import de.moinmorni.angelkoenig.fish.Tournament;
import de.moinmorni.angelkoenig.fish.Units;
import de.moinmorni.angelkoenig.world.Layout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
 *
 * <p>Wer im Tal ist, macht mit und „spielt gerade“ (dann fragt „/lobby“ im Netzwerk erst nach). Verliert jemand
 * während des Turniers die Verbindung, bleibt sein Platz {@code rueckkehr-sekunden} lang frei – so lange bietet die
 * Lobby „Zurück ins Spiel“ an.
 */
public final class Tournaments {

    private static final long SECOND = 1000L;
    private final AngelkoenigPlugin pl;
    private final Tournament t;
    private final BossBar barDe = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.YELLOW, BossBar.Overlay.NOTCHED_10);
    private final BossBar barEn = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.YELLOW, BossBar.Overlay.NOTCHED_10);
    private final Set<UUID> seeing = new HashSet<>();
    /** Wem das Netzwerk gerade „spielt“ glaubt (nur Änderungen werden gemeldet). */
    private final Set<UUID> told = new HashSet<>();
    /** Namen derer, die gerade weg sind und zurückkommen dürfen (für die Meldungen). */
    private final Map<UUID, String> awayNames = new HashMap<>();
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
        for (UUID u : t.expired(now)) {
            late(u);
        }
        syncPlaying();
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
        syncPlaying();
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
            release();
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
        announceLeader();
    }

    /** Wer jetzt vorn liegt, erfahren alle – aber nur, wenn sich die Führung geändert hat. */
    private void announceLeader() {
        List<Tournament.Entry> ranking = t.ranking();
        if (ranking.isEmpty()) {
            leader = null;
            return;
        }
        Tournament.Entry top = ranking.get(0);
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

    // ---------- Zurück ins Spiel ----------

    /**
     * Jemand verlässt den Server (Verbindung weg, Lobby). Im laufenden Turnier bleibt sein Platz samt bestem Fang
     * {@code rueckkehr-sekunden} lang frei ({@code holdSlot}); ohne Frist ist er gleich weg.
     */
    public void quit(Player p) {
        UUID u = p.getUniqueId();
        forget(p);
        // Das Netzwerk vergisst „spielt gerade“, sobald jemand den Server verlässt.
        told.remove(u);
        if (!t.running()) {
            return;
        }
        int seconds = pl.settings().comebackSeconds();
        boolean placed = t.entry(u) != null;
        String name = p.getName();
        if (t.away(u, System.currentTimeMillis(), seconds * SECOND)) {
            awayNames.put(u, name);
            pl.net().holdSlot(u, seconds);
            for (Player o : players()) {
                if (!o.equals(p)) {
                    o.sendMessage(Texts.msg(pl.lang(o), "tourney.away", Placeholder.unparsed("name", name),
                            Placeholder.unparsed("secs", String.valueOf(seconds))));
                }
            }
        } else if (placed) {
            gone(name);
        }
    }

    /**
     * Wiederkommen: Läuft das Turnier noch und ist der Platz frei geblieben, geht es weiter (true) – Minecraft hat Ort
     * und Fänge gespeichert. Zu spät: der Platz ist weg (false – man kommt wie sonst ins Dorf).
     */
    public boolean rejoin(Player p) {
        UUID u = p.getUniqueId();
        switch (t.back(u, System.currentTimeMillis())) {
            case IN_TIME -> {
                awayNames.remove(u);
                pl.net().holdSlot(u, 0);
                tell(p, true);
                return true;
            }
            case TOO_LATE -> {
                late(u);
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    /** Nach {@link #rejoin}: Willkommen zurück – mit dem Stand im Turnier. */
    public void greetBack(Player p) {
        String lang = pl.lang(p);
        Ui.title(p, Texts.text(lang, "tourney.back.title"), Texts.text(lang, "tourney.back.sub"), 2500);
        Ui.sound(p, "entity.player.levelup", 1.2f);
        Tournament.Entry e = t.entry(p.getUniqueId());
        if (e == null) {
            p.sendMessage(Texts.msg(lang, "tourney.back"));
        } else {
            p.sendMessage(Texts.msg(lang, "tourney.back.placed", Placeholder.unparsed("n",
                    String.valueOf(t.place(p.getUniqueId()))), Placeholder.unparsed("weight", Units.weight(e.grams(), lang)),
                    Placeholder.unparsed("fish", Texts.plain(lang, "species." + e.species().id()))));
        }
        for (Player o : players()) {
            if (!o.equals(p)) {
                o.sendMessage(Texts.msg(pl.lang(o), "tourney.back.all", Placeholder.unparsed("name", p.getName())));
            }
        }
    }

    /** Nicht rechtzeitig zurück: Platz frei, Angebot zurückgenommen. */
    private void late(UUID u) {
        pl.net().holdSlot(u, 0);
        String name = awayNames.remove(u);
        if (name == null) {
            return;
        }
        for (Player o : players()) {
            o.sendMessage(Texts.msg(pl.lang(o), "tourney.away.late", Placeholder.unparsed("name", name)));
        }
        if (u.equals(leader)) {
            announceLeader();
        }
    }

    /** Ohne Rückkehr-Frist (0): Wer geht, gibt seinen Platz gleich auf. */
    private void gone(String name) {
        for (Player o : players()) {
            o.sendMessage(Texts.msg(pl.lang(o), "tourney.left", Placeholder.unparsed("name", name)));
        }
        if (leader != null && t.entry(leader) == null) {
            announceLeader();
        }
    }

    /** Wie viele gerade weg sind und zurückkommen dürfen (für /angeladmin status). */
    public int awayCount() {
        return awayNames.size();
    }

    /** Turnier vorbei oder abgebrochen: niemand muss mehr zurück, niemand spielt mehr. */
    private void release() {
        for (UUID u : t.releaseAway()) {
            pl.net().holdSlot(u, 0);
        }
        awayNames.clear();
        syncPlaying();
    }

    /** Wer im Tal ist, spielt, solange das Turnier läuft – sonst niemand. */
    private void syncPlaying() {
        boolean on = t.running();
        Set<UUID> here = new HashSet<>();
        for (Player p : players()) {
            here.add(p.getUniqueId());
            tell(p, on);
        }
        for (UUID u : new ArrayList<>(told)) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) {
                told.remove(u);
            } else if (!here.contains(u)) {
                tell(p, false);
            }
        }
    }

    /** Dem Netzwerk sagen, ob jemand gerade spielt (dann fragt „/lobby“ erst nach) – nur bei Änderungen. */
    private void tell(Player p, boolean playing) {
        if (playing ? told.add(p.getUniqueId()) : told.remove(p.getUniqueId())) {
            pl.net().playing(p, playing);
        }
    }

    private void finished(List<Tournament.Entry> ranking) {
        hideBars();
        // Wer gerade weg war, zählt mit seinem Platz – das Angebot „Zurück ins Spiel“ ist aber vorbei.
        release();
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
