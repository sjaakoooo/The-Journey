package com.thejourney.client.recap;

import com.thejourney.client.config.JourneyConfig;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The recap data, parsed once when it arrives from the server.
 * Highlights are derived only from values that really exist.
 */
public final class RecapModel {
    /** One row from the server. {@code raw} is "number:unit" (n, cm, t, f = done/total, d = date, b = flag). */
    public record Stat(String id, String category, String label, String value,
                       String icon, String raw, String extra) {
        public long num() {
            if (raw == null) {
                return -1;
            }
            String number = raw.contains(":") ? raw.substring(0, raw.indexOf(':')) : raw;
            if (number.contains("/")) {
                number = number.substring(0, number.indexOf('/'));
            }
            try {
                return Long.parseLong(number.trim());
            } catch (NumberFormatException exception) {
                return -1;
            }
        }

        public String unit() {
            return raw != null && raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : "";
        }

        /** done / total for unit "f", otherwise -1. */
        public double fraction() {
            if (!"f".equals(unit()) || !raw.contains("/")) {
                return -1;
            }
            try {
                String[] parts = raw.substring(0, raw.indexOf(':')).split("/");
                double total = Double.parseDouble(parts[1]);
                return total <= 0 ? -1 : Double.parseDouble(parts[0]) / total;
            } catch (RuntimeException exception) {
                return -1;
            }
        }

        public long total() {
            try {
                return Long.parseLong(raw.substring(raw.indexOf('/') + 1, raw.indexOf(':')));
            } catch (RuntimeException exception) {
                return 0;
            }
        }

        public boolean isZero() {
            return num() == 0 && !unit().equals("f") && !unit().equals("b") && !unit().equals("d");
        }
    }

    /** A card on the Highlights page. {@code photoId} links it to a milestone photo, if one exists. */
    public record Highlight(String id, String kicker, String title, String text, String icon,
                            String photoId, int tier, String badge) {
    }

    private static List<Stat> all = List.of();
    private static final Map<String, Stat> BY_ID = new HashMap<>();
    private static String message = "Join a world to load your recap.";
    private static boolean loaded = false;
    private static int version = 0;
    private static List<Highlight> highlightCache = null;
    private static int highlightVersion = -1;

    private RecapModel() {
    }

    public static void set(String text) {
        version++;
        highlightCache = null;

        if (text == null || !text.contains("\t")) {
            loaded = false;
            message = text == null ? "" : text;
            all = List.of();
            BY_ID.clear();
            return;
        }

        List<Stat> parsed = new ArrayList<>();
        BY_ID.clear();

        for (String row : text.split("\\R")) {
            String[] f = row.split("\t", 7);
            if (f.length < 4) {
                continue;
            }
            Stat stat = new Stat(f[0], f[1], f[2], f[3],
                    f.length > 4 && !f[4].isBlank() ? f[4] : null,
                    f.length > 5 && !f[5].isBlank() ? f[5] : null,
                    f.length > 6 && !f[6].isBlank() ? f[6] : null);
            parsed.add(stat);
            BY_ID.put(stat.id(), stat);
        }

        all = parsed;
        loaded = true;
    }

    /** Rebuilds the highlights (for example after photos were deleted). */
    public static void refreshHighlights() {
        highlightCache = null;
    }

    public static boolean loaded() {
        return loaded;
    }

    public static String message() {
        return message;
    }

    public static int version() {
        return version;
    }

    public static Stat get(String id) {
        return BY_ID.get(id);
    }

    public static long num(String id) {
        Stat stat = BY_ID.get(id);
        return stat == null ? -1 : stat.num();
    }

    public static String rank() {
        Stat stat = BY_ID.get("title");
        return stat == null ? null : stat.value();
    }

    /** Stats of a category that the player has not switched off (and, optionally, that are not empty). */
    public static List<Stat> category(String category) {
        JourneyConfig config = JourneyConfig.get();
        List<Stat> result = new ArrayList<>();
        for (Stat stat : all) {
            if (!stat.category().equals(category) || !config.isEnabled(stat.id())) {
                continue;
            }
            if (config.hideEmptyStats && stat.isZero()) {
                continue;
            }
            result.add(stat);
        }
        return result;
    }

    // ------------------------------------------------------------------ highlights

    public static List<Highlight> highlights() {
        if (highlightCache == null || highlightVersion != version) {
            highlightCache = buildHighlights();
            highlightVersion = version;
        }
        return highlightCache;
    }

    private static String when(Stat moment) {
        return moment.num() > 0 ? Fmt.date(moment.num()) : null;
    }

    private static List<Highlight> buildHighlights() {
        List<Highlight> list = new ArrayList<>();
        JourneyConfig config = JourneyConfig.get();

        Stat crown = BY_ID.get("m_crown");
        if (crown != null) {
            String date = when(crown);
            String text = (crown.extra() != null ? crown.extra() : "A rare achievement")
                    + (date != null ? " (" + date + ")" : "");
            list.add(new Highlight("crown", "CROWNING ACHIEVEMENT", crown.value(), text, crown.icon(), null, 2, null));
        }

        Stat diamond = BY_ID.get("m_diamond");
        if (diamond != null) {
            list.add(new Highlight("diamond", "FIRST DIAMOND", "First diamond discovered",
                    diamond.num() > 0 ? "Found on " + when(diamond) : "Found", "minecraft:diamond",
                    "diamond", 1, null));
        }

        Stat nether = BY_ID.get("m_nether");
        if (nether != null) {
            list.add(new Highlight("nether", "THE NETHER", "First time in the Nether",
                    nether.num() > 0 ? "Entered on " + when(nether) : "Entered", "minecraft:flint_and_steel",
                    "nether", 1, null));
        }

        Stat end = BY_ID.get("m_end");
        if (end != null) {
            list.add(new Highlight("end", "THE END", "Entered the End",
                    end.num() > 0 ? "Arrived on " + when(end) : "Arrived", "minecraft:ender_eye",
                    "end", 1, null));
        }

        Stat dragon = BY_ID.get("m_dragon");
        if (dragon != null) {
            long kills = num("ender_dragon_kills");
            String text = (dragon.num() > 0 ? "First victory on " + when(dragon) : "Victory")
                    + (kills > 1 ? " - " + Fmt.number(kills) + " defeated in total" : "");
            list.add(new Highlight("dragon", "ENDER DRAGON", "Ender Dragon defeated", text,
                    "minecraft:dragon_head", "dragon", 2, null));
        }

        long deaths = num("deaths");
        if (deaths > 0 && Gallery.photoFile("death") != null) {
            list.add(new Highlight("death", "FIRST DEATH", "Your first death",
                    "You have respawned " + Fmt.number(deaths) + (deaths == 1 ? " time" : " times") + " so far",
                    "minecraft:skeleton_skull", "death", 1, null));
        }

        long playTicks = num("play_time");
        if (deaths == 0 && playTicks >= 72000) {
            list.add(new Highlight("untouched", "UNTOUCHED", "Not a single death",
                    "Zero deaths in " + Fmt.duration(playTicks) + " of play", "minecraft:totem_of_undying",
                    null, playTicks >= 720000 ? 2 : 1, null));
        }

        Stat kill = BY_ID.get("hl_kill");
        if (kill != null && config.showHighlights) {
            long total = num("mobs_killed");
            list.add(new Highlight("kill", "MOST DEFEATED", kill.label(), kill.value() + " - your most defeated mob",
                    kill.icon(), null, 1, total > 0 ? Fmt.number(total) + " mobs total" : null));
        }

        Stat distance = BY_ID.get("distance");
        if (distance != null && distance.num() > 0) {
            list.add(new Highlight("distance", "MOST EXPLORED", "Distance traveled: " + distance.value(),
                    travelDetail(distance.num()), "minecraft:iron_boots", null,
                    distance.num() >= 10_000_000L ? 2 : 1, null));
        }

        long session = SessionLog.longestMs();
        if (session > 0) {
            list.add(new Highlight("session", "LONGEST SESSION", Fmt.durationMs(session),
                    "Longest single session measured by The Journey", "minecraft:clock", null, 1, null));
        }

        if (config.showHighlights) {
            addFavorite(list, "hl_block", "FAVORITE BLOCK");
            addFavorite(list, "hl_craft", "FAVORITE CRAFT");
            addFavorite(list, "hl_item", "MOST USED ITEM");
        }

        Stat fall = BY_ID.get("hl_fall");
        if (fall != null && fall.num() > 0 && config.showHighlights) {
            list.add(new Highlight("fall", "GRAVITY CHECK", fall.value() + " fallen",
                    "Total distance fallen", "minecraft:feather", null, 1, null));
        }

        return list;
    }

    private static void addFavorite(List<Highlight> list, String id, String kicker) {
        Stat stat = BY_ID.get(id);
        if (stat != null) {
            list.add(new Highlight(id, kicker, stat.label(), stat.value(), stat.icon(), null, 1, null));
        }
    }

    /** Splits the travelled distance into modes and names the biggest, using real stats only. */
    private static String travelDetail(long totalCm) {
        long ridden = Math.max(0, num("ridden"));
        long elytra = Math.max(0, num("elytra"));
        long swum = Math.max(0, num("swum"));
        long onFoot = Math.max(0, totalCm - ridden - elytra - swum);

        long best = Math.max(Math.max(onFoot, ridden), Math.max(elytra, swum));
        String mode = best == onFoot ? "on foot" : best == ridden ? "riding" : best == elytra ? "gliding" : "swimming";
        return "Mostly " + mode + " (" + Fmt.percent(best / (double) totalCm) + ")";
    }

    /** Travel modes for the bar graph; null when there is nothing meaningful to compare. */
    public static long[] travelModes() {
        Stat distance = BY_ID.get("distance");
        if (distance == null || distance.num() <= 0) {
            return null;
        }
        long ridden = Math.max(0, num("ridden"));
        long elytra = Math.max(0, num("elytra"));
        long swum = Math.max(0, num("swum"));
        long onFoot = Math.max(0, distance.num() - ridden - elytra - swum);
        return new long[]{onFoot, ridden, elytra, swum};
    }

    public static int year() {
        return LocalDate.now().getYear();
    }

    /** A stat this good deserves a special reveal. Thresholds are ours; the numbers are real. */
    public static boolean impressive(Stat stat) {
        long n = stat.num();
        return switch (stat.id()) {
            case "ender_dragon_kills", "withers_killed" -> n >= 1;
            case "diamonds_mined" -> n >= 100;
            case "mobs_killed" -> n >= 1000;
            case "blocks_mined" -> n >= 10000;
            case "distance" -> n >= 10_000_000L;
            case "play_time" -> n >= 7_200_000L;
            case "advancements" -> stat.fraction() >= 0.75;
            default -> false;
        };
    }
}
