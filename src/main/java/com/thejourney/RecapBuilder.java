package com.thejourney;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the recap as plain text: one line per stat, fields separated by tabs:
 * id, category, label, value and (optionally) an item id used as the icon.
 * The client turns these lines into tiles.
 */
public final class RecapBuilder {
    private static final int TICKS_PER_DAY = 24000;

    private RecapBuilder() {
    }

    /** Called when a player joins: remember the totals so "This session" can show the difference. */
    public static void markSessionStart(ServerPlayer player) {
        SessionTracker.start(player.getUUID(), totalsOf(player));
    }

    /** Called when a player leaves. */
    public static void clearSession(ServerPlayer player) {
        SessionTracker.stop(player.getUUID());
    }

    /** Snapshot of the session totals. */
    public static SessionTracker.Totals totalsOf(ServerPlayer player) {
        Scan scan = scan(player.getStats());
        return totals(player.getStats(), scan);
    }

    public static String create(ServerPlayer player) {
        ServerStatsCounter stats = player.getStats();
        Scan scan = scan(stats);
        List<String> rows = new ArrayList<>();

        long rideCm = custom(stats, Stats.MINECART_ONE_CM)
                + custom(stats, Stats.BOAT_ONE_CM)
                + custom(stats, Stats.PIG_ONE_CM)
                + custom(stats, Stats.HORSE_ONE_CM)
                + custom(stats, Stats.STRIDER_ONE_CM);

        long dragons = stats.getValue(Stats.ENTITY_KILLED, EntityType.ENDER_DRAGON);

        // ---- Rank title ----
        rows.add(row("title", "meta", "Title", rankTitle(stats, scan, dragons)));

        // ---- Highlights ----
        if (scan.favBlockCount > 0) {
            rows.add(row("hl_block", "highlights", scan.favBlock.getName().getString(),
                    number(scan.favBlockCount) + " mined", itemKey(scan.favBlock.asItem())));
        }
        if (scan.favCraftCount > 0) {
            rows.add(row("hl_craft", "highlights", scan.favCraft.getDescription().getString(),
                    number(scan.favCraftCount) + " crafted", itemKey(scan.favCraft)));
        }
        if (scan.favUsedCount > 0) {
            rows.add(row("hl_item", "highlights", scan.favUsed.getDescription().getString(),
                    number(scan.favUsedCount) + " used", itemKey(scan.favUsed)));
        }
        if (scan.topKillCount > 0) {
            SpawnEggItem egg = SpawnEggItem.byId(scan.topKill);
            rows.add(row("hl_kill", "highlights", scan.topKill.getDescription().getString(),
                    number(scan.topKillCount) + " defeated",
                    egg != null ? itemKey(egg) : "minecraft:bone"));
        }

        long fallCm = custom(stats, Stats.FALL_ONE_CM);
        if (fallCm > 0) {
            rows.add(drow("hl_fall", "highlights", "Distance fallen", fallCm));
        }

        // ---- Exploration ----
        rows.add(drow("distance", "exploration", "Distance traveled", totalDistanceCm(stats)));
        rows.add(drow("ridden", "exploration", "Distance ridden", rideCm));
        rows.add(drow("elytra", "exploration", "Flown with elytra", custom(stats, Stats.AVIATE_ONE_CM)));
        rows.add(drow("swum", "exploration", "Distance swum", custom(stats, Stats.SWIM_ONE_CM)));
        rows.add(trow("play_time", "exploration", "Play time", custom(stats, Stats.PLAY_TIME)));
        rows.add(nrow("days", "exploration", "Days passed", custom(stats, Stats.TOTAL_WORLD_TIME) / TICKS_PER_DAY));
        rows.add(nrow("sleeps", "exploration", "Nights slept", custom(stats, Stats.SLEEP_IN_BED)));
        rows.add(nrow("jumps", "exploration", "Jumps", custom(stats, Stats.JUMP)));

        // ---- Mining ----
        rows.add(nrow("blocks_mined", "mining", "Blocks mined", scan.blocksMined));
        rows.add(nrow("ores_mined", "mining", "Ores mined", scan.oresMined));
        rows.add(nrow("diamonds_mined", "mining", "Diamonds mined", scan.diamondsMined));
        rows.add(nrow("logs_chopped", "mining", "Logs chopped", scan.logsChopped));
        rows.add(nrow("blocks_placed", "mining", "Blocks placed", scan.blocksPlaced));
        rows.add(nrow("items_crafted", "mining", "Items crafted", scan.itemsCrafted));
        rows.add(nrow("tools_broken", "mining", "Tools broken", scan.toolsBroken));

        // ---- Combat ----
        long shots = stats.getValue(Stats.ITEM_USED, Items.BOW)
                + stats.getValue(Stats.ITEM_USED, Items.CROSSBOW);

        rows.add(nrow("mobs_killed", "combat", "Mobs killed", custom(stats, Stats.MOB_KILLS)));
        rows.add(nrow("deaths", "combat", "Deaths", custom(stats, Stats.DEATHS)));
        rows.add(nrow("ender_dragon_kills", "combat", "Ender Dragons defeated", dragons));
        rows.add(nrow("player_kills", "combat", "Players defeated", custom(stats, Stats.PLAYER_KILLS)));
        rows.add(nrow("creepers_killed", "combat", "Creepers killed", stats.getValue(Stats.ENTITY_KILLED, EntityType.CREEPER)));
        rows.add(nrow("endermen_killed", "combat", "Endermen killed", stats.getValue(Stats.ENTITY_KILLED, EntityType.ENDERMAN)));
        rows.add(nrow("withers_killed", "combat", "Withers defeated", stats.getValue(Stats.ENTITY_KILLED, EntityType.WITHER)));
        // Minecraft stores damage in tenths.
        rows.add(nrow("damage_dealt", "combat", "Damage dealt", custom(stats, Stats.DAMAGE_DEALT) / 10));
        rows.add(nrow("damage_taken", "combat", "Damage taken", custom(stats, Stats.DAMAGE_TAKEN) / 10));
        rows.add(nrow("shots_fired", "combat", "Arrows shot", shots));
        rows.add(nrow("raids_won", "combat", "Raids won", custom(stats, Stats.RAID_WIN)));
        rows.add(trow("alive_for", "combat", "Alive for", custom(stats, Stats.TIME_SINCE_DEATH)));

        // ---- World ----
        rows.add(nrow("animals_bred", "world", "Animals bred", custom(stats, Stats.ANIMALS_BRED)));
        rows.add(nrow("fish_caught", "world", "Fish caught", custom(stats, Stats.FISH_CAUGHT)));
        rows.add(nrow("chests_opened", "world", "Chests opened", custom(stats, Stats.OPEN_CHEST)));
        rows.add(nrow("villager_trades", "world", "Villager trades", custom(stats, Stats.TRADED_WITH_VILLAGER)));
        rows.add(nrow("items_enchanted", "world", "Items enchanted", custom(stats, Stats.ENCHANT_ITEM)));
        rows.add(nrow("food_eaten", "world", "Food eaten", scan.foodEaten));
        rows.add(advancementsRow(player));
        rows.add(nrow("cakes_eaten", "world", "Cake slices eaten", custom(stats, Stats.EAT_CAKE_SLICE)));
        rows.add(nrow("bells_rung", "world", "Bells rung", custom(stats, Stats.BELL_RING)));
        rows.add(nrow("flowers_potted", "world", "Flowers potted", custom(stats, Stats.POT_FLOWER)));

        // ---- This session ----
        SessionTracker.Totals now = totals(stats, scan);
        SessionTracker.Totals start = SessionTracker.baseline(player.getUUID(), now);

        rows.add(trow("s_play_time", "session", "Play time", now.playTicks() - start.playTicks()));
        rows.add(drow("s_distance", "session", "Distance traveled", now.distanceCm() - start.distanceCm()));
        rows.add(nrow("s_blocks_mined", "session", "Blocks mined", now.blocksMined() - start.blocksMined()));
        rows.add(nrow("s_blocks_placed", "session", "Blocks placed", now.blocksPlaced() - start.blocksPlaced()));
        rows.add(nrow("s_items_crafted", "session", "Items crafted", now.itemsCrafted() - start.itemsCrafted()));
        rows.add(nrow("s_mobs_killed", "session", "Mobs killed", now.mobsKilled() - start.mobsKilled()));
        rows.add(nrow("s_deaths", "session", "Deaths", now.deaths() - start.deaths()));
        rows.add(nrow("s_jumps", "session", "Jumps", now.jumps() - start.jumps()));

        // ---- Moments and achievements (real dates from the player's advancement progress) ----
        Moments.addMoments(player, rows);
        addMoreStats(stats, rows);
        Moments.addAchievements(player, rows, rows.stream().mapToInt(String::length).sum() + rows.size() + 2500);

        // ---- Server leaders (only when more than one player is online) ----
        addLeaders(player, rows);

        return String.join("\n", rows);
    }

    // ------------------------------------------------------------------
    // Every other custom statistic (including those added by other mods)
    // ------------------------------------------------------------------

    private static final java.util.Set<ResourceLocation> SHOWN_ELSEWHERE = java.util.Set.of(
            Stats.MINECART_ONE_CM, Stats.BOAT_ONE_CM, Stats.PIG_ONE_CM, Stats.HORSE_ONE_CM, Stats.STRIDER_ONE_CM,
            Stats.WALK_ONE_CM, Stats.SPRINT_ONE_CM, Stats.CROUCH_ONE_CM, Stats.WALK_ON_WATER_ONE_CM,
            Stats.WALK_UNDER_WATER_ONE_CM, Stats.SWIM_ONE_CM, Stats.CLIMB_ONE_CM, Stats.FLY_ONE_CM,
            Stats.AVIATE_ONE_CM, Stats.FALL_ONE_CM, Stats.PLAY_TIME, Stats.TOTAL_WORLD_TIME, Stats.SLEEP_IN_BED,
            Stats.JUMP, Stats.MOB_KILLS, Stats.DEATHS, Stats.PLAYER_KILLS, Stats.DAMAGE_DEALT, Stats.DAMAGE_TAKEN,
            Stats.RAID_WIN, Stats.TIME_SINCE_DEATH, Stats.ANIMALS_BRED, Stats.FISH_CAUGHT, Stats.OPEN_CHEST,
            Stats.TRADED_WITH_VILLAGER, Stats.ENCHANT_ITEM, Stats.EAT_CAKE_SLICE, Stats.BELL_RING, Stats.POT_FLOWER
    );

    private static void addMoreStats(ServerStatsCounter stats, List<String> rows) {
        int added = 0;
        for (ResourceLocation key : BuiltInRegistries.CUSTOM_STAT) {
            if (SHOWN_ELSEWHERE.contains(key) || added >= 60) {
                continue;
            }
            long value = stats.getValue(Stats.CUSTOM, key);
            if (value <= 0) {
                continue;
            }

            String path = key.getPath();
            String translated = net.minecraft.network.chat.Component
                    .translatable("stat." + key.toString().replace(':', '.')).getString();
            String label = translated.startsWith("stat.") ? prettify(path, key.getNamespace()) : translated;
            String id = "x_" + key.toString().replace(':', '.');

            if (path.endsWith("_one_cm")) {
                rows.add(drow(id, "more", label, value));
            } else if (path.contains("time") || path.startsWith("play_")) {
                rows.add(trow(id, "more", label, value));
            } else if (path.contains("damage")) {
                rows.add(nrow(id, "more", label, value / 10));
            } else {
                rows.add(nrow(id, "more", label, value));
            }
            added++;
        }
    }

    /** "inspect_hopper" from mod "foo" becomes "Inspect Hopper" (or "Foo: ..." for other mods). */
    private static String prettify(String path, String namespace) {
        StringBuilder out = new StringBuilder();
        for (String word : path.split("[_/]")) {
            if (!word.isEmpty()) {
                out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
            }
        }
        return out.toString().trim();
    }

    // ------------------------------------------------------------------
    // Scanning
    // ------------------------------------------------------------------

    private static final class Scan {
        long blocksMined;
        long oresMined;
        long diamondsMined;
        long logsChopped;
        long blocksPlaced;
        long foodEaten;
        long itemsCrafted;
        long toolsBroken;

        Block favBlock;
        int favBlockCount;
        Item favCraft;
        int favCraftCount;
        Item favUsed;
        int favUsedCount;
        EntityType<?> topKill;
        int topKillCount;
    }

    private static Scan scan(ServerStatsCounter stats) {
        Scan scan = new Scan();

        for (Block block : BuiltInRegistries.BLOCK) {
            int mined = stats.getValue(Stats.BLOCK_MINED, block);
            if (mined <= 0) {
                continue;
            }

            scan.blocksMined += mined;
            BlockState state = block.defaultBlockState();

            if (isOre(state, block)) {
                scan.oresMined += mined;
            }
            if (state.is(BlockTags.DIAMOND_ORES)) {
                scan.diamondsMined += mined;
            }
            if (state.is(BlockTags.LOGS)) {
                scan.logsChopped += mined;
            }
            if (mined > scan.favBlockCount) {
                scan.favBlockCount = mined;
                scan.favBlock = block;
            }
        }

        for (Item item : BuiltInRegistries.ITEM) {
            int crafted = stats.getValue(Stats.ITEM_CRAFTED, item);
            scan.itemsCrafted += crafted;
            scan.toolsBroken += stats.getValue(Stats.ITEM_BROKEN, item);

            if (crafted > scan.favCraftCount) {
                scan.favCraftCount = crafted;
                scan.favCraft = item;
            }

            int used = stats.getValue(Stats.ITEM_USED, item);
            if (used > 0) {
                if (item instanceof BlockItem) {
                    scan.blocksPlaced += used;
                }
                if (item.components().has(DataComponents.FOOD)) {
                    scan.foodEaten += used;
                }
                if (used > scan.favUsedCount) {
                    scan.favUsedCount = used;
                    scan.favUsed = item;
                }
            }
        }

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            int killed = stats.getValue(Stats.ENTITY_KILLED, type);
            if (killed > scan.topKillCount) {
                scan.topKillCount = killed;
                scan.topKill = type;
            }
        }

        return scan;
    }

    private static SessionTracker.Totals totals(ServerStatsCounter stats, Scan scan) {
        return new SessionTracker.Totals(
                totalDistanceCm(stats),
                custom(stats, Stats.PLAY_TIME),
                scan.blocksMined,
                custom(stats, Stats.MOB_KILLS),
                custom(stats, Stats.DEATHS),
                scan.itemsCrafted,
                scan.blocksPlaced,
                custom(stats, Stats.JUMP)
        );
    }

    private static long totalDistanceCm(ServerStatsCounter stats) {
        return custom(stats, Stats.MINECART_ONE_CM)
                + custom(stats, Stats.BOAT_ONE_CM)
                + custom(stats, Stats.PIG_ONE_CM)
                + custom(stats, Stats.HORSE_ONE_CM)
                + custom(stats, Stats.STRIDER_ONE_CM)
                + custom(stats, Stats.WALK_ONE_CM)
                + custom(stats, Stats.SPRINT_ONE_CM)
                + custom(stats, Stats.CROUCH_ONE_CM)
                + custom(stats, Stats.WALK_ON_WATER_ONE_CM)
                + custom(stats, Stats.WALK_UNDER_WATER_ONE_CM)
                + custom(stats, Stats.SWIM_ONE_CM)
                + custom(stats, Stats.CLIMB_ONE_CM)
                + custom(stats, Stats.FLY_ONE_CM)
                + custom(stats, Stats.AVIATE_ONE_CM);
    }

    // ------------------------------------------------------------------
    // Rank title
    // ------------------------------------------------------------------

    private record Candidate(String name, double score) {
    }

    private static String rankTitle(ServerStatsCounter stats, Scan scan, long dragons) {
        if (dragons > 0) {
            return "Dragonslayer";
        }

        List<Candidate> candidates = List.of(
                new Candidate("Deep Delver", scan.blocksMined / 12000.0),
                new Candidate("Lumberjack", scan.logsChopped / 1500.0),
                new Candidate("Master Builder", scan.blocksPlaced / 10000.0),
                new Candidate("Wayfarer", totalDistanceCm(stats) / 100.0 / 150000.0),
                new Candidate("Skyrider", custom(stats, Stats.AVIATE_ONE_CM) / 100.0 / 30000.0),
                new Candidate("Monster Hunter", custom(stats, Stats.MOB_KILLS) / 1500.0),
                new Candidate("Shepherd", custom(stats, Stats.ANIMALS_BRED) / 150.0),
                new Candidate("Angler", custom(stats, Stats.FISH_CAUGHT) / 200.0),
                new Candidate("Merchant", custom(stats, Stats.TRADED_WITH_VILLAGER) / 300.0),
                new Candidate("Gem Hunter", scan.diamondsMined / 300.0),
                new Candidate("Survivor", custom(stats, Stats.TIME_SINCE_DEATH) / 20.0 / 3600.0 / 30.0)
        );

        Candidate best = candidates.get(0);
        for (Candidate candidate : candidates) {
            if (candidate.score() > best.score()) {
                best = candidate;
            }
        }

        if (best.score() < 0.1) {
            return "Fresh Adventurer";
        }
        if (best.score() < 0.5) {
            return "Apprentice " + best.name();
        }
        if (best.score() >= 2.0) {
            return "Legendary " + best.name();
        }
        return best.name();
    }

    // ------------------------------------------------------------------
    // Server leaders
    // ------------------------------------------------------------------

    private static void addLeaders(ServerPlayer viewer, List<String> rows) {
        MinecraftServer server = viewer.getServer();
        if (server == null || server.getPlayerList().getPlayerCount() < 2) {
            return;
        }

        ServerPlayer topKiller = null;
        ServerPlayer mostDeaths = null;
        ServerPlayer longestPlay = null;
        ServerPlayer farthest = null;
        long killer = -1;
        long deaths = -1;
        long play = -1;
        long far = -1;

        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            ServerStatsCounter stats = other.getStats();

            long kills = custom(stats, Stats.MOB_KILLS);
            if (kills > killer) {
                killer = kills;
                topKiller = other;
            }
            long died = custom(stats, Stats.DEATHS);
            if (died > deaths) {
                deaths = died;
                mostDeaths = other;
            }
            long time = custom(stats, Stats.PLAY_TIME);
            if (time > play) {
                play = time;
                longestPlay = other;
            }
            long distance = totalDistanceCm(stats);
            if (distance > far) {
                far = distance;
                farthest = other;
            }
        }

        if (topKiller != null && killer > 0) {
            rows.add(row("top_killer", "server", "Top killer: " + name(topKiller), number(killer), "minecraft:iron_sword"));
        }
        if (mostDeaths != null && deaths > 0) {
            rows.add(row("top_deaths", "server", "Most deaths: " + name(mostDeaths), number(deaths), "minecraft:skeleton_skull"));
        }
        if (longestPlay != null) {
            rows.add(row("top_playtime", "server", "Longest play: " + name(longestPlay), duration(play), "minecraft:clock"));
        }
        if (farthest != null) {
            rows.add(row("top_traveler", "server", "Top traveler: " + name(farthest), distance(far), "minecraft:iron_boots"));
        }
    }

    private static String name(ServerPlayer player) {
        return player.getName().getString();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static boolean isOre(BlockState state, Block block) {
        return state.is(BlockTags.COAL_ORES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.COPPER_ORES)
                || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.REDSTONE_ORES)
                || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.EMERALD_ORES)
                || block == Blocks.NETHER_GOLD_ORE
                || block == Blocks.NETHER_QUARTZ_ORE
                || block == Blocks.ANCIENT_DEBRIS;
    }

    private static String advancementsRow(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return row("advancements", "world", "Advancements", "-");
        }

        PlayerAdvancements progress = player.getAdvancements();
        int done = 0;
        int total = 0;

        for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
            // Only advancements that appear in the menu count (recipes do not).
            if (holder.value().display().isEmpty()) {
                continue;
            }

            total++;
            if (progress.getOrStartProgress(holder).isDone()) {
                done++;
            }
        }

        return fullRow("advancements", "world", "Advancements", done + " / " + total, "",
                done + "/" + total + ":f", "");
    }

    private static String itemKey(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return key.toString();
    }

    private static long custom(ServerStatsCounter stats, ResourceLocation stat) {
        return stats.getValue(Stats.CUSTOM, stat);
    }

    private static String number(long value) {
        return String.format(Locale.ROOT, "%,d", Math.max(0, value));
    }

    private static String distance(long centimeters) {
        double meters = Math.max(0, centimeters) / 100.0;
        if (meters >= 1000.0) {
            return String.format(Locale.ROOT, "%.1f km", meters / 1000.0);
        }
        return String.format(Locale.ROOT, "%d m", Math.round(meters));
    }

    private static String duration(long ticks) {
        long totalMinutes = Math.max(0, ticks) / 20 / 60;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours > 0 ? hours + " h " + minutes + " min" : minutes + " min";
    }

    /** Stat row with a raw count, so the client can animate it and draw bars. */
    private static String nrow(String id, String category, String label, long value) {
        long safe = Math.max(0, value);
        return fullRow(id, category, label, number(safe), "", safe + ":n", "");
    }

    /** Stat row with a raw distance in centimetres. */
    private static String drow(String id, String category, String label, long centimeters) {
        long safe = Math.max(0, centimeters);
        return fullRow(id, category, label, distance(safe), "", safe + ":cm", "");
    }

    /** Stat row with a raw duration in ticks. */
    private static String trow(String id, String category, String label, long ticks) {
        long safe = Math.max(0, ticks);
        return fullRow(id, category, label, duration(safe), "", safe + ":t", "");
    }

    /**
     * id, category, label, value, icon, raw ("number:unit"), extra.
     * Older clients only read the first five fields; the others are ignored.
     */
    static String fullRow(String id, String category, String label, String value,
                          String icon, String raw, String extra) {
        return id + "\t" + category + "\t" + clean(label) + "\t" + clean(value)
                + "\t" + icon + "\t" + raw + "\t" + clean(extra);
    }

    private static String clean(String text) {
        return text.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    private static String row(String id, String category, String label, String value) {
        return id + "\t" + category + "\t" + label + "\t" + value;
    }

    private static String row(String id, String category, String label, String value, String icon) {
        return row(id, category, label, value) + "\t" + icon;
    }
}
