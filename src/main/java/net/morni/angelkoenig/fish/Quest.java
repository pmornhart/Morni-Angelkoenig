package net.morni.angelkoenig.fish;

import java.util.Locale;
import java.util.Optional;

/**
 * Eine Aufgabe des Tages, z. B. „Fang 3 Seesaiblinge“. Als Text gespeichert ({@link #spec()}), damit eine einmal
 * gestellte Aufgabe gleich bleibt.
 *
 * @param kind   Art der Aufgabe
 * @param target Fangplatz, Fischart, Seltenheit oder Mindestgewicht in Gramm (je nach Art, sonst leer)
 * @param amount wie oft (bei SELL: Taler)
 * @param reward Belohnung in Talern
 */
public record Quest(Kind kind, String target, int amount, int reward) {

    public enum Kind {
        /** Beliebige Fänge (ohne Fundstücke). */
        TOTAL,
        /** Fische an einem Fangplatz. */
        SPOT,
        /** Eine bestimmte Art. */
        SPECIES,
        /** Fische mindestens dieser Seltenheit. */
        RARITY,
        /** Ein Fisch mit mindestens so vielen Gramm. */
        HEAVY,
        /** Für so viele Taler verkaufen. */
        SELL,
        /** Fische mit Köder. */
        BAIT
    }

    /** Was gerade passiert ist: ein Fang (species ≠ null) oder ein Verkauf (sold &gt; 0). */
    public record Event(Species species, Spot spot, int grams, boolean bait, int sold) {

        public static Event caught(Catch c, Spot spot, boolean bait) {
            return new Event(c.species(), spot, c.grams(), bait, 0);
        }

        public static Event sold(int taler) {
            return new Event(null, null, 0, false, taler);
        }
    }

    /** Wie weit dieses Ereignis die Aufgabe bringt (0 = gar nicht). */
    public int advance(Event e) {
        if (kind == Kind.SELL) {
            return Math.max(0, e.sold());
        }
        Species s = e.species();
        if (s == null || s.junk()) {
            return 0;
        }
        return switch (kind) {
            case TOTAL -> 1;
            case SPOT -> e.spot() != null && e.spot().id().equals(target) ? 1 : 0;
            case SPECIES -> s.id().equals(target) ? 1 : 0;
            case RARITY -> Rarity.valueOf(target.toUpperCase(Locale.ROOT)).ordinal() <= s.rarity().ordinal() ? 1 : 0;
            case HEAVY -> e.grams() >= Integer.parseInt(target) ? 1 : 0;
            case BAIT -> e.bait() ? 1 : 0;
            case SELL -> 0;
        };
    }

    /** „species:seesaibling:3:180“. */
    public String spec() {
        return kind.name().toLowerCase(Locale.ROOT) + ":" + target + ":" + amount + ":" + reward;
    }

    public static Optional<Quest> parse(String spec) {
        if (spec == null) {
            return Optional.empty();
        }
        String[] p = spec.trim().split(":", -1);
        if (p.length != 4) {
            return Optional.empty();
        }
        try {
            Kind k = Kind.valueOf(p[0].toUpperCase(Locale.ROOT));
            int amount = Integer.parseInt(p[2]);
            int reward = Integer.parseInt(p[3]);
            String target = p[1];
            boolean ok = switch (k) {
                case SPOT -> Spot.byId(target).isPresent();
                case SPECIES -> Species.byId(target).filter(s -> !s.junk()).isPresent();
                case RARITY -> !target.isEmpty() && java.util.Arrays.stream(Rarity.values())
                        .anyMatch(r -> r.name().equalsIgnoreCase(target) && r != Rarity.JUNK);
                case HEAVY -> target.matches("\\d{1,7}");
                default -> true;
            };
            if (!ok || amount <= 0 || reward < 0) {
                return Optional.empty();
            }
            return Optional.of(new Quest(k, target, amount, reward));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
