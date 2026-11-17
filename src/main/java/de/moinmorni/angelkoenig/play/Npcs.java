package de.moinmorni.angelkoenig.play;

import de.moinmorni.angelkoenig.Items;

import de.moinmorni.angelkoenig.fish.Species;

import de.moinmorni.angelkoenig.AngelkoenigPlugin;
import de.moinmorni.angelkoenig.world.Layout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.entity.boat.SpruceBoat;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Leben im Tal: Fischhändler Xaver und Bootsmann Hias, Ruderboote am Steg (kommen zurück, wenn man sie stehen
 * lässt), eine Katze am Fischerhaus, Kühe auf der Wiese, Fische auf Eis an der Theke, Räucherfische und die goldene
 * Forelle über dem Brunnen. Alles nicht dauerhaft – beim Start neu aufgestellt.
 */
public final class Npcs {

    public static final String MERCHANT = "ak_merchant";
    public static final String BOATMAN = "ak_boatman";
    public static final String BOAT = "ak_boat";
    private static final String DECOR = "ak_decor";
    private static final long BOAT_RETURN_MS = 45_000L;

    private final AngelkoenigPlugin pl;
    private final List<Entity> entities = new ArrayList<>();
    private final Map<Integer, Boat> boats = new HashMap<>();
    private final Map<Integer, Long> boatAway = new HashMap<>();
    private ItemDisplay trout;
    private float troutAngle;

    public Npcs(AngelkoenigPlugin pl) {
        this.pl = pl;
    }

    private World w() {
        return pl.world();
    }

    private Location at(Layout.Place p) {
        return new Location(w(), p.x(), p.y(), p.z(), p.yaw(), p.pitch());
    }

    /** Alles aufstellen (alte Reste dieser Art vorher weg). */
    public void spawn() {
        for (Entity e : w().getEntities()) {
            if (e.getScoreboardTags().stream().anyMatch(t -> t.startsWith("ak_"))) {
                e.remove();
            }
        }
        entities.clear();
        boats.clear();
        entities.add(villager(at(Layout.MERCHANT), MERCHANT, Villager.Profession.FISHERMAN, Villager.Type.TAIGA));
        entities.add(villager(at(Layout.BOATMAN), BOATMAN, Villager.Profession.LEATHERWORKER, Villager.Type.SWAMP));
        for (int i = 0; i < Layout.BOATS.length; i++) {
            boat(i);
        }
        entities.add(w().spawn(new Location(w(), 17.5, Layout.STAND_Y, -50.5, 90f, 0f), Cat.class, c -> {
            c.setPersistent(false);
            c.setSitting(true);
            c.setAI(false);
            c.setInvulnerable(true);
            c.setSilent(true);
            c.addScoreboardTag(DECOR);
        }));
        double[][] cows = {{-44.5, -74.5, 30f}, {-50.5, -70.5, 200f}, {44.5, -76.5, 120f}};
        for (double[] c : cows) {
            int gx = (int) Math.floor(c[0]);
            int gz = (int) Math.floor(c[1]);
            double y = pl.terrain().height(gx, gz) + 1;
            entities.add(w().spawn(new Location(w(), c[0], y, c[1], (float) c[2], 0f), Cow.class, cow -> {
                cow.setPersistent(false);
                cow.setAI(false);
                cow.setInvulnerable(true);
                cow.setSilent(true);
                cow.addScoreboardTag(DECOR);
            }));
        }
        // Fische auf Eis an der Theke (Theke bei z = −35, Eis bei x = 10, 11, 13)
        // Eigene Zeichnungen mit dem Fisch-Paket, sonst die Minecraft-Fische
        Species[] counter = {Species.BARSCH, Species.BACHFORELLE, Species.SEESAIBLING, Species.RENKE,
            Species.HUCHEN, Species.ROTAUGE};
        double[][] spots = {{10.3, -34.6}, {10.7, -34.3}, {11.4, -34.6}, {11.7, -34.35}, {13.3, -34.5}, {13.7, -34.6}};
        for (int i = 0; i < spots.length; i++) {
            entities.add(display(new Location(w(), spots[i][0], Layout.STAND_Y + 1.03, spots[i][1]),
                    FishItems.look(counter[i]), 0.45f, true, 25f * i));
        }
        // Räucherfische an der Stange neben dem Fischerhaus
        for (int i = 0; i < 4; i++) {
            entities.add(display(new Location(w(), 15.5, Layout.STAND_Y + 2.45, -55.3 + i * 1.1),
                    FishItems.look(i % 2 == 0 ? Species.BACHFORELLE : Species.RENKE), 0.6f, false, 90f));
        }
        trout = display(new Location(w(), Layout.FOUNTAIN_X + 0.5, Layout.STAND_Y + 3.25, Layout.FOUNTAIN_Z + 0.5),
                Items.glow(FishItems.look(Species.GOLDFORELLE), true), 1.4f, false, 0f);
        trout.setInterpolationDuration(20);
        entities.add(trout);
    }

    private Villager villager(Location l, String tag, Villager.Profession prof, Villager.Type type) {
        return w().spawn(l, Villager.class, v -> {
            v.setPersistent(false);
            v.setAI(false);
            v.setInvulnerable(true);
            v.setSilent(true);
            v.setCollidable(false);
            v.setProfession(prof);
            v.setVillagerType(type);
            v.setVillagerLevel(5);
            v.addScoreboardTag(tag);
            v.setRotation(l.getYaw(), 0f);
        });
    }

    private ItemDisplay display(Location l, ItemStack item, float scale, boolean flat, float yaw) {
        return w().spawn(l, ItemDisplay.class, d -> {
            d.setPersistent(false);
            d.setItemStack(item);
            Quaternionf rot = new Quaternionf();
            rot.rotateY((float) Math.toRadians(yaw));
            if (flat) {
                rot.rotateX((float) Math.toRadians(90));
            } else {
                rot.rotateZ((float) Math.toRadians(-90));
            }
            d.setTransformation(new Transformation(new Vector3f(), rot, new Vector3f(scale, scale, scale),
                    new Quaternionf()));
            d.setBrightness(new Display.Brightness(12, 15));
            d.addScoreboardTag(DECOR);
        });
    }

    private void boat(int i) {
        Location l = at(Layout.BOATS[i]);
        Boat b = w().spawn(l, SpruceBoat.class, boat -> {
            boat.setPersistent(false);
            boat.addScoreboardTag(BOAT);
            boat.addScoreboardTag(BOAT + ":" + i);
        });
        boats.put(i, b);
        entities.add(b);
    }

    /** Alle paar Sekunden: liegengelassene Boote zurück an den Steg, kaputte ersetzen, Forelle dreht sich. */
    public void tick() {
        long now = System.currentTimeMillis();
        for (int i = 0; i < Layout.BOATS.length; i++) {
            Boat b = boats.get(i);
            if (b == null || !b.isValid()) {
                boat(i);
                continue;
            }
            Location home = at(Layout.BOATS[i]);
            boolean away = b.getPassengers().isEmpty() && b.getLocation().distanceSquared(home) > 9;
            if (!away) {
                boatAway.remove(i);
                continue;
            }
            long since = boatAway.computeIfAbsent(i, k -> now);
            if (now - since > BOAT_RETURN_MS) {
                b.teleport(home);
                boatAway.remove(i);
            }
        }
    }

    /** Die goldene Forelle über dem Brunnen dreht sich langsam (jede Sekunde ein Stück, weich gleitend). */
    public void spinTrout() {
        if (trout == null || !trout.isValid()) {
            return;
        }
        troutAngle = (troutAngle + 18f) % 360f;
        Transformation t = trout.getTransformation();
        trout.setInterpolationDelay(0);
        trout.setTransformation(new Transformation(new Vector3f(0, (float) Math.sin(Math.toRadians(troutAngle * 2)) * 0.1f, 0),
                new AxisAngle4f((float) Math.toRadians(troutAngle), 0, 1, 0), t.getScale(), new AxisAngle4f()));
    }

    public void removeAll() {
        entities.forEach(Entity::remove);
        entities.clear();
        boats.clear();
    }

    /** Fehlen Figuren (z. B. nach /kill)? Dann alles neu. */
    public boolean missing() {
        return entities.stream().anyMatch(e -> !e.isValid() && !(e instanceof Boat));
    }
}
