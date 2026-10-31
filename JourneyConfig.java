package com.thejourney.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JourneyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE =
            FabricLoader.getInstance().getConfigDir().resolve("the-journey.json");

    private static JourneyConfig instance = new JourneyConfig();

    public boolean showRecapInCredits = true;

    /** Take a few small screenshots while in the End, for the credits background. */
    public boolean captureEndScreenshots = true;

    /** Take a photo at big moments (first Crafting Table, entering the Nether, ...). */
    public boolean captureMilestones = true;

    /** Play your own music (sounds/credits.ogg) during the credits, if the file exists. */
    public boolean customCreditsMusic = true;

    /** Average number of seconds between End screenshots. */
    public int shotIntervalSeconds = 20;

    public JourneyTheme theme = JourneyTheme.WOOD;

    public boolean showSession = true;
    public boolean showHighlights = true;
    public boolean showLeaders = true;

    public boolean showDistance = true;
    public boolean showRidden = true;
    public boolean showElytra = true;
    public boolean showSwum = true;
    public boolean showPlayTime = true;
    public boolean showDays = true;
    public boolean showSleeps = true;
    public boolean showJumps = true;
    public boolean showBlocksMined = true;
    public boolean showOresMined = true;
    public boolean showDiamondsMined = true;
    public boolean showLogsChopped = true;
    public boolean showBlocksPlaced = true;
    public boolean showItemsCrafted = true;
    public boolean showToolsBroken = true;
    public boolean showMobsKilled = true;
    public boolean showDeaths = true;
    public boolean showEnderDragonKills = true;
    public boolean showPlayerKills = true;
    public boolean showCreepersKilled = true;
    public boolean showEndermenKilled = true;
    public boolean showWithersKilled = true;
    public boolean showDamageDealt = true;
    public boolean showDamageTaken = true;
    public boolean showShotsFired = true;
    public boolean showRaidsWon = true;
    public boolean showAliveFor = true;
    public boolean showAnimalsBred = true;
    public boolean showFishCaught = true;
    public boolean showChestsOpened = true;
    public boolean showVillagerTrades = true;
    public boolean showItemsEnchanted = true;
    public boolean showFoodEaten = true;
    public boolean showAdvancements = true;
    public boolean showCakesEaten = true;
    public boolean showBellsRung = true;
    public boolean showFlowersPotted = true;

    /** Whether the stat with this id may be shown in the recap (unknown ids always are). */
    public boolean isEnabled(String statId) {
        if (statId.startsWith("s_")) {
            return showSession;
        }
        if (statId.startsWith("hl_")) {
            return showHighlights;
        }
        if (statId.startsWith("top_")) {
            return showLeaders;
        }

        return switch (statId) {
            case "distance" -> showDistance;
            case "ridden" -> showRidden;
            case "elytra" -> showElytra;
            case "swum" -> showSwum;
            case "play_time" -> showPlayTime;
            case "days" -> showDays;
            case "sleeps" -> showSleeps;
            case "jumps" -> showJumps;
            case "blocks_mined" -> showBlocksMined;
            case "ores_mined" -> showOresMined;
            case "diamonds_mined" -> showDiamondsMined;
            case "logs_chopped" -> showLogsChopped;
            case "blocks_placed" -> showBlocksPlaced;
            case "items_crafted" -> showItemsCrafted;
            case "tools_broken" -> showToolsBroken;
            case "mobs_killed" -> showMobsKilled;
            case "deaths" -> showDeaths;
            case "ender_dragon_kills" -> showEnderDragonKills;
            case "player_kills" -> showPlayerKills;
            case "creepers_killed" -> showCreepersKilled;
            case "endermen_killed" -> showEndermenKilled;
            case "withers_killed" -> showWithersKilled;
            case "damage_dealt" -> showDamageDealt;
            case "damage_taken" -> showDamageTaken;
            case "shots_fired" -> showShotsFired;
            case "raids_won" -> showRaidsWon;
            case "alive_for" -> showAliveFor;
            case "animals_bred" -> showAnimalsBred;
            case "fish_caught" -> showFishCaught;
            case "chests_opened" -> showChestsOpened;
            case "villager_trades" -> showVillagerTrades;
            case "items_enchanted" -> showItemsEnchanted;
            case "food_eaten" -> showFoodEaten;
            case "advancements" -> showAdvancements;
            case "cakes_eaten" -> showCakesEaten;
            case "bells_rung" -> showBellsRung;
            case "flowers_potted" -> showFlowersPotted;
            default -> true;
        };
    }

    public static JourneyConfig get() {
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                JourneyConfig loaded =
                        GSON.fromJson(Files.readString(FILE), JourneyConfig.class);

                if (loaded != null) {
                    instance = loaded;
                }
            }
        } catch (Exception exception) {
            instance = new JourneyConfig();
        }

        save();
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(instance));
        } catch (IOException exception) {
            throw new RuntimeException("Could not save The Journey config", exception);
        }
    }
}
