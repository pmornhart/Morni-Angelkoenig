package net.morni.angelkoenig;

import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;

/** Werte aus der config.yml (Erklärungen stehen dort). */
public record Settings(
        String world,
        boolean timeRuns,
        boolean weatherChanges,
        int weatherMin,
        int weatherMax,
        int biteMin,
        int biteMax,
        int startTaler,
        boolean tournaments,
        int tournamentEvery,
        int tournamentMinutes,
        int tournamentWarn,
        int tournamentMinPlayers,
        List<Integer> prizes,
        int participation) {

    static Settings load(FileConfiguration c) {
        List<Integer> prizes = c.getIntegerList("turnier.preise").stream().map(p -> clamp(p, 0, 1_000_000)).toList();
        int biteMin = clamp(c.getInt("angeln.biss-min-sekunden", 5), 1, 120);
        return new Settings(
                c.getString("welt", "angelkoenig"),
                c.getBoolean("welt-ablauf.tageszeit-laeuft", true),
                c.getBoolean("welt-ablauf.wetter-wechselt", true),
                clamp(c.getInt("welt-ablauf.wetter-min-sekunden", 360), 10, 36000),
                clamp(c.getInt("welt-ablauf.wetter-max-sekunden", 900), 10, 36000),
                biteMin,
                Math.max(biteMin, clamp(c.getInt("angeln.biss-max-sekunden", 18), 1, 300)),
                clamp(c.getInt("angeln.startgeld", 25), 0, 1_000_000),
                c.getBoolean("turnier.an", true),
                clamp(c.getInt("turnier.alle-minuten", 15), 1, 1440),
                clamp(c.getInt("turnier.dauer-minuten", 5), 1, 120),
                clamp(c.getInt("turnier.vorwarnung-sekunden", 60), 0, 600),
                clamp(c.getInt("turnier.mindestens-spieler", 2), 1, 100),
                prizes.isEmpty() ? List.of(250, 120, 60) : prizes,
                clamp(c.getInt("turnier.teilnahme", 15), 0, 1_000_000));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
