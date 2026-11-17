package de.moinmorni.angelkoenig.data;

import de.moinmorni.angelkoenig.fish.Bait;
import de.moinmorni.angelkoenig.fish.Catch;
import de.moinmorni.angelkoenig.fish.Cosmetic;
import de.moinmorni.angelkoenig.fish.Quest;
import de.moinmorni.angelkoenig.fish.Rarity;
import de.moinmorni.angelkoenig.fish.Rod;
import de.moinmorni.angelkoenig.fish.Species;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Alles, was sich Angelkönig über eine Person merkt: Taler, Fänge, Fischbuch, Angeln, Köder, Kleidung, Aufgaben des
 * Tages und Turniersiege. Gespeichert als {@code spieler/<uuid>.yml}.
 */
public final class PlayerData {

    /** Ein Eintrag im Fischbuch. */
    public record BookEntry(int count, int bestGrams, int bestMm, long firstDay) {
    }

    private final UUID uuid;
    private String name;
    private long taler;
    private int catches;
    private int finds;
    private int legendary;
    private int heaviest;
    private String heaviestSpecies = "";
    private int tournamentsWon;
    private int tournamentsPlayed;
    private long earned;
    private boolean introSeen;
    private final Map<Species, BookEntry> book = new EnumMap<>(Species.class);
    private final Set<Rod> rods = EnumSet.of(Rod.HASEL);
    private Rod rod = Rod.HASEL;
    private final Map<Bait, Integer> baits = new EnumMap<>(Bait.class);
    private Bait bait;
    private final Set<Cosmetic> wardrobe = EnumSet.noneOf(Cosmetic.class);
    private final Map<Cosmetic.Slot, Cosmetic> worn = new EnumMap<>(Cosmetic.Slot.class);
    private String questDay = "";
    private final List<Quest> quests = new ArrayList<>();
    private final List<Integer> progress = new ArrayList<>();
    private final List<Boolean> done = new ArrayList<>();

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String n) {
        if (n != null && !n.isBlank()) {
            name = n;
        }
    }

    // ---------- Taler ----------

    public long taler() {
        return taler;
    }

    /** Taler gutschreiben (Verkauf, Fischbuch, Aufgaben, Turnier) – zählt in „verdient“. */
    public void earn(long amount) {
        taler += Math.max(0, amount);
        earned += Math.max(0, amount);
    }

    /** Taler für einen Verkauf gutschreiben. */
    public void sold(long amount) {
        earn(amount);
    }

    public long earnedTotal() {
        return earned;
    }

    /** Bezahlen; {@code false}, wenn es nicht reicht (dann ändert sich nichts). */
    public boolean spend(long amount) {
        if (amount < 0 || taler < amount) {
            return false;
        }
        taler -= amount;
        return true;
    }

    public void setTaler(long amount) {
        taler = Math.max(0, amount);
    }

    // ---------- Fänge und Fischbuch ----------

    /**
     * Fang eintragen. Liefert {@code true}, wenn es der größte (schwerste) Fang dieser Art bisher ist – auch beim
     * allerersten.
     */
    public boolean addCatch(Catch c, LocalDate day) {
        Species s = c.species();
        if (s.junk()) {
            finds++;
        } else {
            catches++;
            if (s.rarity() == Rarity.LEGENDARY) {
                legendary++;
            }
            if (c.grams() > heaviest) {
                heaviest = c.grams();
                heaviestSpecies = s.id();
            }
        }
        BookEntry old = book.get(s);
        if (old == null) {
            book.put(s, new BookEntry(1, c.grams(), c.lengthMm(), day.toEpochDay()));
            return true;
        }
        boolean record = c.grams() > old.bestGrams();
        book.put(s, new BookEntry(old.count() + 1, record ? c.grams() : old.bestGrams(),
                record ? c.lengthMm() : old.bestMm(), old.firstDay()));
        return record;
    }

    public BookEntry book(Species s) {
        return book.get(s);
    }

    /** Verwaltung: Fischbuch und Fangzähler leeren (Taler, Angeln, Siege bleiben). */
    public void resetBook() {
        book.clear();
        catches = 0;
        finds = 0;
        legendary = 0;
        heaviest = 0;
        heaviestSpecies = "";
    }

    public boolean knows(Species s) {
        return book.containsKey(s);
    }

    /** Wie viele echte Fischarten schon im Fischbuch stehen. */
    public int speciesCount() {
        return (int) book.keySet().stream().filter(s -> !s.junk()).count();
    }

    public int catches() {
        return catches;
    }

    public int finds() {
        return finds;
    }

    public int legendary() {
        return legendary;
    }

    public int heaviest() {
        return heaviest;
    }

    public String heaviestSpecies() {
        return heaviestSpecies;
    }

    // ---------- Turniere ----------

    public int tournamentsWon() {
        return tournamentsWon;
    }

    public int tournamentsPlayed() {
        return tournamentsPlayed;
    }

    public void tournamentPlayed(boolean won) {
        tournamentsPlayed++;
        if (won) {
            tournamentsWon++;
            wardrobe.add(Cosmetic.KOENIGSKRONE);
        }
    }

    // ---------- Angeln ----------

    public Rod rod() {
        return rod;
    }

    public boolean owns(Rod r) {
        return rods.contains(r);
    }

    public void addRod(Rod r) {
        rods.add(r);
    }

    /** Angel wechseln (nur eine, die man hat). */
    public boolean useRod(Rod r) {
        if (!rods.contains(r)) {
            return false;
        }
        rod = r;
        return true;
    }

    // ---------- Köder ----------

    public int baits(Bait b) {
        return baits.getOrDefault(b, 0);
    }

    public void addBaits(Bait b, int n) {
        baits.merge(b, Math.max(0, n), Integer::sum);
    }

    /** Eingehängter Köder (null = keiner). */
    public Bait bait() {
        return bait != null && baits(bait) > 0 ? bait : null;
    }

    public void bait(Bait b) {
        bait = b;
    }

    /** Einen Köder verbrauchen; ist die Dose leer, wird er ausgehängt. Liefert den verbrauchten Köder oder null. */
    public Bait useBait() {
        Bait b = bait();
        if (b == null) {
            return null;
        }
        int left = baits(b) - 1;
        if (left <= 0) {
            baits.remove(b);
            bait = null;
        } else {
            baits.put(b, left);
        }
        return b;
    }

    // ---------- Kleidung ----------

    public boolean owns(Cosmetic c) {
        return wardrobe.contains(c);
    }

    public void addCosmetic(Cosmetic c) {
        wardrobe.add(c);
    }

    public Cosmetic worn(Cosmetic.Slot slot) {
        Cosmetic c = worn.get(slot);
        return c != null && wardrobe.contains(c) ? c : null;
    }

    /** Anziehen bzw. (wenn schon an) ausziehen. Liefert {@code true}, wenn es jetzt getragen wird. */
    /** Anziehen (nur, was im Kleiderschrank ist); ersetzt das Stück am selben Platz. */
    public void wear(Cosmetic c) {
        if (wardrobe.contains(c)) {
            worn.put(c.slot(), c);
        }
    }

    /** Alles ausziehen. */
    public void undress() {
        worn.clear();
    }

    public boolean toggleWear(Cosmetic c) {
        if (!wardrobe.contains(c)) {
            return false;
        }
        if (worn.get(c.slot()) == c) {
            worn.remove(c.slot());
            return false;
        }
        worn.put(c.slot(), c);
        return true;
    }

    // ---------- Aufgaben ----------

    /** Aufgaben für diesen Tag bereitstellen (neuer Tag = neue Aufgaben, Fortschritt bei 0). */
    public boolean ensureQuests(LocalDate day, List<Quest> generated) {
        String d = day.toString();
        if (d.equals(questDay) && quests.size() == generated.size()) {
            return false;
        }
        questDay = d;
        quests.clear();
        quests.addAll(generated);
        progress.clear();
        done.clear();
        for (int i = 0; i < quests.size(); i++) {
            progress.add(0);
            done.add(false);
        }
        return true;
    }

    /** Eine Aufgabe ersetzen (Verwaltung/Test). */
    public void setQuest(int index, Quest q) {
        if (index >= 0 && index < quests.size()) {
            quests.set(index, q);
            progress.set(index, 0);
            done.set(index, false);
        }
    }

    public List<Quest> quests() {
        return List.copyOf(quests);
    }

    public int progress(int i) {
        return progress.get(i);
    }

    public boolean done(int i) {
        return done.get(i);
    }

    public String questDay() {
        return questDay;
    }

    /** Ereignis auf die Aufgaben anwenden; liefert die Nummern der Aufgaben, die dadurch fertig wurden. */
    public List<Integer> advance(Quest.Event e) {
        List<Integer> finished = new ArrayList<>();
        for (int i = 0; i < quests.size(); i++) {
            if (done.get(i)) {
                continue;
            }
            Quest q = quests.get(i);
            int add = q.advance(e);
            if (add <= 0) {
                continue;
            }
            int p = Math.min(q.amount(), progress.get(i) + add);
            progress.set(i, p);
            if (p >= q.amount()) {
                done.set(i, true);
                finished.add(i);
            }
        }
        return finished;
    }

    // ---------- Sonstiges ----------

    public boolean introSeen() {
        return introSeen;
    }

    public void introSeen(boolean seen) {
        introSeen = seen;
    }

    // ---------- Speichern ----------

    public String toYaml() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("name", name);
        y.set("taler", taler);
        y.set("faenge", catches);
        y.set("fundstuecke", finds);
        y.set("legendaer", legendary);
        y.set("schwerster.gramm", heaviest);
        y.set("schwerster.art", heaviestSpecies);
        y.set("turniere.gewonnen", tournamentsWon);
        y.set("turniere.gespielt", tournamentsPlayed);
        y.set("verdient", earned);
        y.set("einfuehrung", introSeen);
        book.forEach((s, e) -> y.set("fischbuch." + s.id(), e.count() + " " + e.bestGrams() + " " + e.bestMm() + " "
                + e.firstDay()));
        y.set("angeln", rods.stream().map(Rod::id).toList());
        y.set("angel", rod.id());
        baits.forEach((b, n) -> y.set("koeder." + b.id(), n));
        y.set("koeder-aktiv", bait == null ? "" : bait.id());
        y.set("kleidung", wardrobe.stream().map(Cosmetic::id).toList());
        worn.forEach((slot, c) -> y.set("getragen." + slot.name().toLowerCase(Locale.ROOT), c.id()));
        y.set("aufgaben.tag", questDay);
        List<String> qs = new ArrayList<>();
        for (int i = 0; i < quests.size(); i++) {
            qs.add(quests.get(i).spec() + "|" + progress.get(i) + "|" + done.get(i));
        }
        y.set("aufgaben.liste", qs);
        return y.saveToString();
    }

    public static PlayerData fromYaml(UUID uuid, String text) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        PlayerData d = new PlayerData(uuid, y.getString("name", "?"));
        d.taler = Math.max(0, y.getLong("taler"));
        d.catches = y.getInt("faenge");
        d.finds = y.getInt("fundstuecke");
        d.legendary = y.getInt("legendaer");
        d.heaviest = y.getInt("schwerster.gramm");
        d.heaviestSpecies = y.getString("schwerster.art", "");
        d.tournamentsWon = y.getInt("turniere.gewonnen");
        d.tournamentsPlayed = y.getInt("turniere.gespielt");
        d.earned = y.getLong("verdient");
        d.introSeen = y.getBoolean("einfuehrung");
        ConfigurationSection b = y.getConfigurationSection("fischbuch");
        if (b != null) {
            for (String k : b.getKeys(false)) {
                Species.byId(k).ifPresent(s -> {
                    String[] p = String.valueOf(b.get(k)).trim().split("\\s+");
                    if (p.length == 4) {
                        try {
                            d.book.put(s, new BookEntry(Integer.parseInt(p[0]), Integer.parseInt(p[1]),
                                    Integer.parseInt(p[2]), Long.parseLong(p[3])));
                        } catch (NumberFormatException ignored) {
                            // kaputter Eintrag: weglassen
                        }
                    }
                });
            }
        }
        for (String r : y.getStringList("angeln")) {
            Rod.byId(r).ifPresent(d.rods::add);
        }
        Rod.byId(y.getString("angel", "hasel")).filter(d.rods::contains).ifPresent(r -> d.rod = r);
        ConfigurationSection k = y.getConfigurationSection("koeder");
        if (k != null) {
            for (String id : k.getKeys(false)) {
                Bait.byId(id).ifPresent(bait -> {
                    int n = k.getInt(id);
                    if (n > 0) {
                        d.baits.put(bait, n);
                    }
                });
            }
        }
        d.bait = Bait.byId(y.getString("koeder-aktiv", "")).orElse(null);
        for (String c : y.getStringList("kleidung")) {
            Cosmetic.byId(c).ifPresent(d.wardrobe::add);
        }
        ConfigurationSection w = y.getConfigurationSection("getragen");
        if (w != null) {
            for (Cosmetic.Slot slot : Cosmetic.Slot.values()) {
                Cosmetic.byId(w.getString(slot.name().toLowerCase(Locale.ROOT), ""))
                        .filter(c -> c.slot() == slot).ifPresent(c -> d.worn.put(slot, c));
            }
        }
        d.questDay = y.getString("aufgaben.tag", "");
        for (String line : y.getStringList("aufgaben.liste")) {
            String[] p = line.split("\\|");
            if (p.length != 3) {
                continue;
            }
            Quest.parse(p[0]).ifPresent(q -> {
                d.quests.add(q);
                int prog;
                try {
                    prog = Math.max(0, Math.min(q.amount(), Integer.parseInt(p[1])));
                } catch (NumberFormatException e) {
                    prog = 0;
                }
                d.progress.add(prog);
                d.done.add(Boolean.parseBoolean(p[2]));
            });
        }
        return d;
    }
}
