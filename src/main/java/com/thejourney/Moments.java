package com.thejourney;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Real moments of a player's journey, taken from their advancement progress.
 * Dates are the moment the advancement was actually completed; if Minecraft
 * has no date for it (for example it was granted by a command) no date is sent.
 * Nothing here is estimated or made up.
 */
final class Moments {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    /** id, advancement path, headline. */
    private record Moment(String id, String path, String headline) {
    }

    private static final List<Moment> MOMENTS = List.of(
            new Moment("m_diamond", "story/mine_diamond", "First diamond"),
            new Moment("m_nether", "story/enter_the_nether", "Entered the Nether"),
            new Moment("m_end", "story/enter_the_end", "Entered the End"),
            new Moment("m_dragon", "end/kill_dragon", "Ender Dragon defeated")
    );

    /**
     * Hardest first. The first one the player has completed becomes their
     * "crowning achievement". (Our own ranking of Minecraft's advancements.)
     */
    private static final List<String> CROWN = List.of(
            "adventure/kill_all_mobs",
            "adventure/adventuring_time",
            "nether/all_effects",
            "husbandry/complete_catalogue",
            "nether/create_full_beacon",
            "husbandry/netherite_hoe",
            "adventure/hero_of_the_village",
            "nether/netherite_armor",
            "end/dragon_egg",
            "end/elytra"
    );

    private Moments() {
    }

    static void addMoments(ServerPlayer player, List<String> rows) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        PlayerAdvancements progress = player.getAdvancements();

        for (Moment moment : MOMENTS) {
            AdvancementHolder holder = find(server, moment.path());
            if (holder == null || !progress.getOrStartProgress(holder).isDone()) {
                continue;
            }

            Instant when = completedAt(progress.getOrStartProgress(holder));
            rows.add(RecapBuilder.fullRow(
                    moment.id(), "moments", moment.headline(),
                    when == null ? "Completed" : format(when),
                    iconOf(holder), epoch(when) + ":d", ""));
        }

        for (String path : CROWN) {
            AdvancementHolder holder = find(server, path);
            if (holder == null || !progress.getOrStartProgress(holder).isDone()
                    || holder.value().display().isEmpty()) {
                continue;
            }

            DisplayInfo display = holder.value().display().get();
            Instant when = completedAt(progress.getOrStartProgress(holder));
            rows.add(RecapBuilder.fullRow(
                    "m_crown", "moments", "Crowning achievement",
                    display.getTitle().getString(), iconOf(holder), epoch(when) + ":d",
                    display.getDescription().getString()));
            break;
        }
    }

    /** Rows may not exceed this many characters in total (the network string limit is 32767). */
    private static final int ROW_BUDGET = 29000;

    private static final List<String> VANILLA_GROUPS = List.of("story", "nether", "end", "adventure", "husbandry");

    private record Candidate(AdvancementHolder holder, String row, boolean done, int groupOrder) {
    }

    /**
     * Every advancement the game knows, including those added by other mods or data packs.
     * Root tabs and hidden advancements that are not yet earned are left out (no spoilers).
     */
    static void addAchievements(ServerPlayer player, List<String> rows, int usedChars) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        PlayerAdvancements progress = player.getAdvancements();
        java.util.List<Candidate> list = new java.util.ArrayList<>();

        for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
            if (holder.value().display().isEmpty()) {
                continue;
            }
            DisplayInfo display = holder.value().display().get();
            AdvancementProgress state = progress.getOrStartProgress(holder);
            boolean done = state.isDone();

            if (display.getBackground().isPresent() || (display.isHidden() && !done)) {
                continue;
            }

            ResourceLocation id = holder.id();
            String rowId = "a_" + id;
            String title = display.getTitle().getString();
            String row;
            if (done) {
                Instant when = completedAt(state);
                row = RecapBuilder.fullRow(rowId, "achievements", title,
                        when == null ? "Completed" : format(when), iconOf(holder), epoch(when) + ":d", "");
            } else {
                row = RecapBuilder.fullRow(rowId, "achievements", title, "Locked", iconOf(holder), "0:b", "");
            }

            int group = id.getNamespace().equals("minecraft")
                    ? Math.max(0, VANILLA_GROUPS.indexOf(id.getPath().split("/")[0]))
                    : 100;
            list.add(new Candidate(holder, row, done, group));
        }

        list.sort(java.util.Comparator
                .comparingInt(Candidate::groupOrder)
                .thenComparing(c -> c.holder().id().getNamespace())
                .thenComparing(c -> c.holder().id().getPath()));

        // Too many for one message (huge modpacks)? Keep everything earned, drop locked ones from the end.
        int total = usedChars;
        for (Candidate c : list) {
            total += c.row().length() + 1;
        }
        int index = list.size() - 1;
        while (total > ROW_BUDGET && index >= 0) {
            if (!list.get(index).done()) {
                total -= list.get(index).row().length() + 1;
                list.remove(index);
            }
            index--;
        }

        for (Candidate c : list) {
            rows.add(c.row());
        }
    }

    // ------------------------------------------------------------------

    private static AdvancementHolder find(MinecraftServer server, String path) {
        return server.getAdvancements().get(ResourceLocation.withDefaultNamespace(path));
    }

    /** The moment the last criterion was completed = when the advancement was earned. */
    private static Instant completedAt(AdvancementProgress progress) {
        Instant latest = null;

        for (String name : progress.getCompletedCriteria()) {
            CriterionProgress criterion = progress.getCriterion(name);
            Instant obtained = criterion == null ? null : criterion.getObtained();

            if (obtained != null && (latest == null || obtained.isAfter(latest))) {
                latest = obtained;
            }
        }

        return latest;
    }

    private static long epoch(Instant when) {
        return when == null ? -1 : when.toEpochMilli();
    }

    private static String format(Instant when) {
        return DATE.format(LocalDate.ofInstant(when, ZoneId.systemDefault()));
    }

    private static String iconOf(AdvancementHolder holder) {
        if (holder.value().display().isEmpty()) {
            return "";
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(holder.value().display().get().getIcon().getItem());
        return key.toString();
    }
}
