package de.moinmorni.angelkoenig.data;

import de.moinmorni.angelkoenig.fish.Catch;
import de.moinmorni.angelkoenig.fish.Species;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Bestenlisten: größter (schwerster) Fang je Art, meiste Fänge, meiste Turniersiege, der amtierende Angelkönig
 * (Sieger des letzten Turniers). Gespeichert als {@code bestenliste.yml}.
 */
public final class Records {

    /** Rekord einer Art. */
    public record Best(UUID uuid, String name, int grams, int mm, long day) {
    }

    /** Summen einer Person. */
    public record Totals(UUID uuid, String name, int catches, int wins, int legendary) {
    }

    private final Map<Species, Best> best = new EnumMap<>(Species.class);
    private final Map<UUID, Totals> totals = new HashMap<>();
    private UUID king;
    private String kingName = "";

    /** Fang anbieten; {@code true}, wenn er der neue Rekord seiner Art ist. */
    public boolean offer(UUID uuid, String name, Catch c, long day) {
        Best old = best.get(c.species());
        if (old != null && old.grams() >= c.grams()) {
            return false;
        }
        best.put(c.species(), new Best(uuid, name, c.grams(), c.lengthMm(), day));
        return true;
    }

    public Best best(Species s) {
        return best.get(s);
    }

    /** Stand einer Person übernehmen (nach jedem Fang und Turnier). */
    public void totals(UUID uuid, String name, int catches, int wins, int legendary) {
        totals.put(uuid, new Totals(uuid, name, catches, wins, legendary));
    }

    public List<Totals> topCatches(int n) {
        return top(Comparator.comparingInt(Totals::catches), n, t -> t.catches() > 0);
    }

    public List<Totals> topWins(int n) {
        return top(Comparator.comparingInt(Totals::wins), n, t -> t.wins() > 0);
    }

    private List<Totals> top(Comparator<Totals> by, int n, java.util.function.Predicate<Totals> keep) {
        List<Totals> out = new ArrayList<>(totals.values().stream().filter(keep).toList());
        out.sort(by.reversed().thenComparing(Totals::name, String.CASE_INSENSITIVE_ORDER));
        return out.subList(0, Math.min(n, out.size()));
    }

    public void king(UUID uuid, String name) {
        king = uuid;
        kingName = name == null ? "" : name;
    }

    public UUID king() {
        return king;
    }

    public String kingName() {
        return kingName;
    }

    public String toYaml() {
        YamlConfiguration y = new YamlConfiguration();
        best.forEach((s, b) -> {
            String p = "rekorde." + s.id() + ".";
            y.set(p + "uuid", b.uuid().toString());
            y.set(p + "name", b.name());
            y.set(p + "gramm", b.grams());
            y.set(p + "mm", b.mm());
            y.set(p + "tag", b.day());
        });
        totals.forEach((u, t) -> {
            String p = "spieler." + u + ".";
            y.set(p + "name", t.name());
            y.set(p + "faenge", t.catches());
            y.set(p + "siege", t.wins());
            y.set(p + "legendaer", t.legendary());
        });
        if (king != null) {
            y.set("koenig.uuid", king.toString());
            y.set("koenig.name", kingName);
        }
        return y.saveToString();
    }

    public static Records fromYaml(String text) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        Records r = new Records();
        ConfigurationSection rec = y.getConfigurationSection("rekorde");
        if (rec != null) {
            for (String id : rec.getKeys(false)) {
                ConfigurationSection s = rec.getConfigurationSection(id);
                UUID u = uuid(s == null ? null : s.getString("uuid"));
                Species.byId(id).ifPresent(sp -> {
                    if (u != null) {
                        r.best.put(sp, new Best(u, s.getString("name", "?"), s.getInt("gramm"), s.getInt("mm"),
                                s.getLong("tag")));
                    }
                });
            }
        }
        ConfigurationSection pl = y.getConfigurationSection("spieler");
        if (pl != null) {
            for (String key : pl.getKeys(false)) {
                UUID u = uuid(key);
                ConfigurationSection s = pl.getConfigurationSection(key);
                if (u != null && s != null) {
                    r.totals.put(u, new Totals(u, s.getString("name", "?"), s.getInt("faenge"), s.getInt("siege"),
                            s.getInt("legendaer")));
                }
            }
        }
        r.king = uuid(y.getString("koenig.uuid"));
        r.kingName = r.king == null ? "" : y.getString("koenig.name", "");
        return r;
    }

    private static UUID uuid(String s) {
        if (s == null) {
            return null;
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
