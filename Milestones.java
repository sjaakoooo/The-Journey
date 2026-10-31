package com.thejourney;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects big moments in a player's journey. When a NEW milestone is reached
 * (not one reached in an earlier session) the client is told to take a photo.
 */
public final class Milestones {
    /** In rough order of progression. The client shows these captions in the credits. */
    public static final Map<String, String> CAPTIONS = new LinkedHashMap<>();

    static {
        CAPTIONS.put("crafting_table", "Your first Crafting Table");
        CAPTIONS.put("iron", "Your first Iron Ingot");
        CAPTIONS.put("diamond", "Your first Diamond");
        CAPTIONS.put("enchant", "Your first Enchanted Item");
        CAPTIONS.put("death", "Your first Death");
        CAPTIONS.put("nether", "Entering the Nether");
        CAPTIONS.put("stronghold", "Finding the Stronghold");
        CAPTIONS.put("beacon", "Building a Beacon");
        CAPTIONS.put("wither", "Defeating the Wither");
        CAPTIONS.put("end", "Entering the End");
        CAPTIONS.put("elytra", "Finding Elytra");
    }

    private static final Map<UUID, Set<String>> KNOWN = new ConcurrentHashMap<>();

    private Milestones() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            RecapBuilder.markSessionStart(player);
            // Everything already reached before this session does not get a new photo.
            KNOWN.put(player.getUUID(), reachedNow(player));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            RecapBuilder.clearSession(player);
            KNOWN.remove(player.getUUID());
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 40 != 0) {
                return;
            }

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                check(player);
            }
        });
    }

    private static void check(ServerPlayer player) {
        Set<String> known = KNOWN.get(player.getUUID());
        if (known == null) {
            return;
        }

        for (String id : CAPTIONS.keySet()) {
            if (known.contains(id) || !reached(player, id)) {
                continue;
            }

            known.add(id);

            if (ServerPlayNetworking.canSend(player, MilestonePayload.TYPE)) {
                ServerPlayNetworking.send(player, new MilestonePayload(id));
            }
        }
    }

    private static Set<String> reachedNow(ServerPlayer player) {
        Set<String> result = new HashSet<>();
        for (String id : CAPTIONS.keySet()) {
            if (reached(player, id)) {
                result.add(id);
            }
        }
        return result;
    }

    private static boolean reached(ServerPlayer player, String id) {
        ServerStatsCounter stats = player.getStats();

        return switch (id) {
            case "crafting_table" -> stats.getValue(Stats.ITEM_CRAFTED, Items.CRAFTING_TABLE) > 0;
            case "iron" -> advancementDone(player, "story/smelt_iron");
            case "diamond" -> stats.getValue(Stats.ITEM_PICKED_UP, Items.DIAMOND) > 0;
            case "enchant" -> advancementDone(player, "story/enchant_item");
            case "death" -> stats.getValue(Stats.CUSTOM, Stats.DEATHS) > 0;
            case "nether" -> advancementDone(player, "story/enter_the_nether");
            case "stronghold" -> advancementDone(player, "story/follow_ender_eye");
            case "beacon" -> advancementDone(player, "nether/create_beacon");
            case "wither" -> stats.getValue(Stats.ENTITY_KILLED, EntityType.WITHER) > 0;
            case "end" -> advancementDone(player, "story/enter_the_end");
            case "elytra" -> advancementDone(player, "end/elytra");
            default -> false;
        };
    }

    private static boolean advancementDone(ServerPlayer player, String path) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        AdvancementHolder holder =
                server.getAdvancements().get(ResourceLocation.withDefaultNamespace(path));

        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
