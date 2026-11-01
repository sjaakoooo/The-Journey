package com.thejourney.client.recap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Which item stands for which statistic. (Moved here from the old recap screen.) */
final class StatIcons {
    private StatIcons() {
    }

    private static final java.util.Map<String, ItemStack> CACHE = new java.util.HashMap<>();

    static ItemStack iconFor(RecapModel.Stat stat) {
        return CACHE.computeIfAbsent(stat.id(), id -> build(stat));
    }

    private static ItemStack build(RecapModel.Stat stat) {
        if (stat.icon() != null) {
            ResourceLocation id = ResourceLocation.tryParse(stat.icon());
            if (id != null) {
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item != Items.AIR) {
                    return new ItemStack(item);
                }
            }
        }

        return getIcon(stat.id());
    }

    private static ItemStack getIcon(String statId) {
        return switch (statId) {
            case "distance" -> new ItemStack(Items.IRON_BOOTS);
            case "ridden" -> new ItemStack(Items.SADDLE);
            case "elytra" -> new ItemStack(Items.ELYTRA);
            case "swum" -> new ItemStack(Items.WATER_BUCKET);
            case "play_time" -> new ItemStack(Items.CLOCK);
            case "days" -> new ItemStack(Items.SUNFLOWER);
            case "sleeps" -> new ItemStack(Items.RED_BED);
            case "jumps" -> new ItemStack(Items.FEATHER);
            case "blocks_mined" -> new ItemStack(Items.IRON_PICKAXE);
            case "ores_mined" -> new ItemStack(Items.RAW_IRON);
            case "diamonds_mined" -> new ItemStack(Items.DIAMOND);
            case "logs_chopped" -> new ItemStack(Items.OAK_LOG);
            case "blocks_placed" -> new ItemStack(Items.BRICKS);
            case "items_crafted" -> new ItemStack(Items.CRAFTING_TABLE);
            case "tools_broken" -> new ItemStack(Items.ANVIL);
            case "mobs_killed" -> new ItemStack(Items.IRON_SWORD);
            case "deaths" -> new ItemStack(Items.SKELETON_SKULL);
            case "ender_dragon_kills" -> new ItemStack(Items.DRAGON_HEAD);
            case "player_kills" -> new ItemStack(Items.PLAYER_HEAD);
            case "creepers_killed" -> new ItemStack(Items.GUNPOWDER);
            case "endermen_killed" -> new ItemStack(Items.ENDER_PEARL);
            case "withers_killed" -> new ItemStack(Items.NETHER_STAR);
            case "damage_dealt" -> new ItemStack(Items.DIAMOND_SWORD);
            case "damage_taken" -> new ItemStack(Items.SHIELD);
            case "shots_fired" -> new ItemStack(Items.BOW);
            case "raids_won" -> new ItemStack(Items.WHITE_BANNER);
            case "alive_for" -> new ItemStack(Items.GOLDEN_APPLE);
            case "animals_bred" -> new ItemStack(Items.WHEAT);
            case "fish_caught" -> new ItemStack(Items.COD);
            case "chests_opened" -> new ItemStack(Items.CHEST);
            case "villager_trades" -> new ItemStack(Items.EMERALD);
            case "items_enchanted" -> new ItemStack(Items.ENCHANTING_TABLE);
            case "food_eaten" -> new ItemStack(Items.COOKED_BEEF);
            case "advancements" -> new ItemStack(Items.WRITABLE_BOOK);
            case "cakes_eaten" -> new ItemStack(Items.CAKE);
            case "bells_rung" -> new ItemStack(Items.BELL);
            case "flowers_potted" -> new ItemStack(Items.FLOWER_POT);
            case "hl_fall" -> new ItemStack(Items.LEATHER_BOOTS);
            case "s_play_time" -> new ItemStack(Items.CLOCK);
            case "s_distance" -> new ItemStack(Items.IRON_BOOTS);
            case "s_blocks_mined" -> new ItemStack(Items.IRON_PICKAXE);
            case "s_blocks_placed" -> new ItemStack(Items.BRICKS);
            case "s_items_crafted" -> new ItemStack(Items.CRAFTING_TABLE);
            case "s_mobs_killed" -> new ItemStack(Items.IRON_SWORD);
            case "s_deaths" -> new ItemStack(Items.SKELETON_SKULL);
            default -> new ItemStack(Items.PAPER);
        };
    }
}
