package de.moinmorni.angelkoenig.play;

import de.moinmorni.angelkoenig.AngelkoenigPlugin;
import de.moinmorni.angelkoenig.Texts;
import de.moinmorni.angelkoenig.fish.DayTime;
import de.moinmorni.angelkoenig.fish.Weather;
import de.moinmorni.angelkoenig.world.Layout;
import de.moinmorni.angelkoenig.world.Terrain;
import java.util.SplittableRandom;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Stimmung im Tal: wechselndes Wetter (klar, Regen, Gewitter), Morgennebel über dem Wasser, springende Fische,
 * Gischt am Wasserfall, Irrlichter im Moor bei Nacht, Schneeflocken am Kar. Alles nur zum Anschauen – jeder sieht die
 * Teilchen um sich herum.
 */
public final class Ambience {

    private final AngelkoenigPlugin pl;
    private final SplittableRandom r = new SplittableRandom();
    private long nextWeather;

    public Ambience(AngelkoenigPlugin pl) {
        this.pl = pl;
    }

    /** Wetter jetzt setzen (Verwaltung) – hält eine Weile. */
    public void setWeather(Weather w, int seconds) {
        World world = pl.world();
        boolean was = world.hasStorm();
        boolean wasThunder = world.isThundering();
        world.setStorm(w != Weather.KLAR);
        world.setThundering(w == Weather.GEWITTER);
        world.setWeatherDuration(Math.max(20, seconds * 20 + 200));
        world.setThunderDuration(Math.max(20, seconds * 20 + 200));
        nextWeather = System.currentTimeMillis() + seconds * 1000L;
        if (was != world.hasStorm() || wasThunder != world.isThundering()) {
            for (Player p : world.getPlayers()) {
                p.sendMessage(Texts.msg(pl.lang(p), "weather.now." + w.id()));
            }
            pl.boards().refreshSpots();
        }
    }

    /** Jede Sekunde: Wetter weiterschalten, wenn es Zeit ist. */
    public void second() {
        if (!pl.settings().weatherChanges()) {
            return;
        }
        if (nextWeather == 0) {
            nextWeather = System.currentTimeMillis() + pl.settings().weatherMin() * 1000L;
            return;
        }
        if (System.currentTimeMillis() >= nextWeather) {
            Weather.Spell s = Weather.next(pl.angling().weather(), r, pl.settings().weatherMin(), pl.settings().weatherMax());
            setWeather(s.weather(), s.seconds());
        }
    }

    /** Alle halbe Sekunde: Teilchen um jeden Spieler. */
    public void pulse() {
        DayTime time = pl.angling().time();
        Terrain t = pl.terrain();
        for (Player p : pl.world().getPlayers()) {
            Location l = p.getLocation();
            if (time == DayTime.MORGEN) {
                fog(p, l, t);
            }
            if (r.nextInt(6) == 0) {
                jump(p, l, t);
            }
            if (Math.abs(l.getX() - Layout.FALL_X) < 48 && Math.abs(l.getZ()) < 48) {
                waterfall(p);
            }
            if (time == DayTime.NACHT && t.inMoor(l.getBlockX(), l.getBlockZ())) {
                wisps(p, l, t);
            }
            if (t.inKar(l.getBlockX(), l.getBlockZ())) {
                p.spawnParticle(Particle.SNOWFLAKE, l.clone().add(0, 3, 0), 8, 6, 3, 6, 0.01);
            }
        }
    }

    private Location water(Location l, Terrain t, int radius) {
        for (int tries = 0; tries < 6; tries++) {
            int x = l.getBlockX() + r.nextInt(radius * 2 + 1) - radius;
            int z = l.getBlockZ() + r.nextInt(radius * 2 + 1) - radius;
            Terrain.Column c = t.column(x, z);
            if (c.wet() && !c.water().frozen()) {
                return new Location(l.getWorld(), x + r.nextDouble(), c.waterTop() + 1.05, z + r.nextDouble());
            }
        }
        return null;
    }

    private void fog(Player p, Location l, Terrain t) {
        for (int i = 0; i < 10; i++) {
            Location w = water(l, t, 22);
            if (w != null) {
                p.spawnParticle(Particle.CLOUD, w.add(0, r.nextDouble() * 0.6, 0), 0, 0.02, 0.003, 0.0, 1);
            }
        }
    }

    private void jump(Player p, Location l, Terrain t) {
        Location w = water(l, t, 18);
        if (w == null) {
            return;
        }
        p.spawnParticle(Particle.SPLASH, w, 14, 0.2, 0.1, 0.2, 0.1);
        p.spawnParticle(Particle.BUBBLE_POP, w, 4, 0.15, 0.05, 0.15, 0.02);
        p.playSound(w, Sound.ENTITY_FISH_SWIM, SoundCategory.AMBIENT, 0.5f, 0.8f + r.nextFloat() * 0.4f);
    }

    private void waterfall(Player p) {
        Location base = new Location(pl.world(), Layout.FALL_X + 0.9, Layout.WATER_Y + 1.1,
                (Layout.FALL_Z1 + Layout.FALL_Z2 + 1) / 2.0);
        p.spawnParticle(Particle.CLOUD, base, 3, 0.6, 0.3, 1.2, 0.02);
        p.spawnParticle(Particle.SPLASH, base, 20, 0.8, 0.2, 1.4, 0.3);
        if (r.nextInt(4) == 0) {
            p.playSound(base, Sound.BLOCK_WATER_AMBIENT, SoundCategory.AMBIENT, 0.8f, 0.7f);
        }
    }

    private void wisps(Player p, Location l, Terrain t) {
        Location w = water(l, t, 16);
        if (w != null) {
            p.spawnParticle(Particle.SOUL_FIRE_FLAME, w.add(0, 0.6 + r.nextDouble(), 0), 0, r.nextDouble() * 0.02 - 0.01,
                    0.01, r.nextDouble() * 0.02 - 0.01, 1);
        }
    }
}
