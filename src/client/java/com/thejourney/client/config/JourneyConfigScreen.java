package com.thejourney.client.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class JourneyConfigScreen {
    public static Screen build(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("The Journey Settings"))
                .setSavingRunnable(JourneyConfig::save);

        ConfigCategory general =
                builder.getOrCreateCategory(Component.literal("General"));
        ConfigCategory experience =
                builder.getOrCreateCategory(Component.literal("Recap experience"));
        ConfigCategory exploration =
                builder.getOrCreateCategory(Component.literal("Exploration"));
        ConfigCategory mining =
                builder.getOrCreateCategory(Component.literal("Mining"));
        ConfigCategory combat =
                builder.getOrCreateCategory(Component.literal("Combat"));
        ConfigCategory world =
                builder.getOrCreateCategory(Component.literal("World"));

        ConfigEntryBuilder entries = builder.entryBuilder();
        JourneyConfig config = JourneyConfig.get();

        general.addEntry(entries.startBooleanToggle(
                        Component.literal("Show recap in End Credits"),
                        config.showRecapInCredits)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.showRecapInCredits = value)
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.literal("Use End screenshots in the credits"),
                        config.captureEndScreenshots)
                .setDefaultValue(true)
                .setTooltip(
                        Component.literal("While you are in the End, a few small screenshots of the game view are saved"),
                        Component.literal("(max 300, in the 'the-journey-temp' game folder). They are shown as the"),
                        Component.literal("credits background and deleted when the credits close. Nothing is uploaded."))
                .setSaveConsumer(value -> config.captureEndScreenshots = value)
                .build());

        general.addEntry(entries.startIntSlider(
                        Component.literal("Seconds between End screenshots (average)"),
                        config.shotIntervalSeconds, 5, 60)
                .setDefaultValue(20)
                .setSaveConsumer(value -> config.shotIntervalSeconds = value)
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.literal("Photos at big moments"),
                        config.captureMilestones)
                .setDefaultValue(true)
                .setTooltip(
                        Component.literal("Takes a photo of the game view at moments like your first Crafting Table or"),
                        Component.literal("entering the Nether. Kept for the recap; see \"Keep photos after the credits\"."))
                .setSaveConsumer(value -> config.captureMilestones = value)
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.literal("Keep photos after the credits"),
                        config.keepPhotos)
                .setDefaultValue(true)
                .setTooltip(
                        Component.literal("Keeps your milestone photos and moment clips so you can view them again"),
                        Component.literal("in the recap (J key). Off = they are deleted when the credits close."),
                        Component.literal("Only on this computer, nothing is uploaded. Can also be deleted from the recap settings."))
                .setSaveConsumer(value -> config.keepPhotos = value)
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.literal("Custom credits music"),
                        config.customCreditsMusic)
                .setDefaultValue(true)
                .setTooltip(
                        Component.literal("Plays assets/the-journey/sounds/credits.ogg during the credits, if it exists."))
                .setSaveConsumer(value -> config.customCreditsMusic = value)
                .build());

        general.addEntry(entries.startEnumSelector(
                        Component.literal("Recap theme"),
                        JourneyTheme.class,
                        config.theme)
                .setDefaultValue(JourneyTheme.END_PORTAL)
                .setSaveConsumer(value -> config.theme = value)
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.literal("Show 'This session' stats"),
                        config.showSession)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.showSession = value)
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.literal("Show highlights (favorites)"),
                        config.showHighlights)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.showHighlights = value)
                .build());

        general.addEntry(entries.startBooleanToggle(
                        Component.literal("Show server leaders (multiplayer)"),
                        config.showLeaders)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.showLeaders = value)
                .build());


        experience.addEntry(entries.startBooleanToggle(
                        Component.literal("Animations"),
                        config.animations)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Turn off for an instant, static recap."))
                .setSaveConsumer(value -> config.animations = value)
                .build());

        experience.addEntry(entries.startIntSlider(
                        Component.literal("Animation speed (%)"),
                        config.animationSpeed, 50, 200)
                .setDefaultValue(100)
                .setSaveConsumer(value -> config.animationSpeed = value)
                .build());

        experience.addEntry(entries.startEnumSelector(
                        Component.literal("Reveal style"),
                        JourneyConfig.RevealStyle.class,
                        config.revealStyle)
                .setDefaultValue(JourneyConfig.RevealStyle.CINEMATIC)
                .setSaveConsumer(value -> config.revealStyle = value)
                .build());

        experience.addEntry(entries.startBooleanToggle(
                        Component.literal("Particles"),
                        config.particles)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.particles = value)
                .build());

        experience.addEntry(entries.startBooleanToggle(
                        Component.literal("Sound effects"),
                        config.soundEffects)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.soundEffects = value)
                .build());

        experience.addEntry(entries.startIntSlider(
                        Component.literal("Sound volume (%)"),
                        config.soundVolume, 0, 100)
                .setDefaultValue(70)
                .setSaveConsumer(value -> config.soundVolume = value)
                .build());

        experience.addEntry(entries.startBooleanToggle(
                        Component.literal("Background music (recap screen)"),
                        config.backgroundMusic)
                .setDefaultValue(false)
                .setTooltip(
                        Component.literal("Loops music while the standalone recap screen is open."),
                        Component.literal("The End Credits keep using their own music."))
                .setSaveConsumer(value -> config.backgroundMusic = value)
                .build());

        experience.addEntry(entries.startBooleanToggle(
                        Component.literal("Hide empty statistics"),
                        config.hideEmptyStats)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.hideEmptyStats = value)
                .build());

        experience.addEntry(entries.startBooleanToggle(
                        Component.literal("Short moment clips"),
                        config.captureClips)
                .setDefaultValue(true)
                .setTooltip(
                        Component.literal("Captures about 3 seconds of frames right after a big moment"),
                        Component.literal("(first death, Wither, Ender Dragon). This is a frame sequence, not a video,"),
                        Component.literal("and only exists if it was recorded while you played."))
                .setSaveConsumer(value -> config.captureClips = value)
                .build());

        toggle(exploration, entries, "Distance traveled", config.showDistance, value -> config.showDistance = value);
        toggle(exploration, entries, "Distance ridden", config.showRidden, value -> config.showRidden = value);
        toggle(exploration, entries, "Flown with elytra", config.showElytra, value -> config.showElytra = value);
        toggle(exploration, entries, "Distance swum", config.showSwum, value -> config.showSwum = value);
        toggle(exploration, entries, "Play time", config.showPlayTime, value -> config.showPlayTime = value);
        toggle(exploration, entries, "Days passed", config.showDays, value -> config.showDays = value);
        toggle(exploration, entries, "Nights slept", config.showSleeps, value -> config.showSleeps = value);
        toggle(exploration, entries, "Jumps", config.showJumps, value -> config.showJumps = value);

        toggle(mining, entries, "Blocks mined", config.showBlocksMined, value -> config.showBlocksMined = value);
        toggle(mining, entries, "Ores mined", config.showOresMined, value -> config.showOresMined = value);
        toggle(mining, entries, "Diamonds mined", config.showDiamondsMined, value -> config.showDiamondsMined = value);
        toggle(mining, entries, "Logs chopped", config.showLogsChopped, value -> config.showLogsChopped = value);
        toggle(mining, entries, "Blocks placed", config.showBlocksPlaced, value -> config.showBlocksPlaced = value);
        toggle(mining, entries, "Items crafted", config.showItemsCrafted, value -> config.showItemsCrafted = value);
        toggle(mining, entries, "Tools broken", config.showToolsBroken, value -> config.showToolsBroken = value);

        toggle(combat, entries, "Mobs killed", config.showMobsKilled, value -> config.showMobsKilled = value);
        toggle(combat, entries, "Deaths", config.showDeaths, value -> config.showDeaths = value);
        toggle(combat, entries, "Ender Dragons defeated", config.showEnderDragonKills, value -> config.showEnderDragonKills = value);
        toggle(combat, entries, "Players defeated", config.showPlayerKills, value -> config.showPlayerKills = value);
        toggle(combat, entries, "Creepers killed", config.showCreepersKilled, value -> config.showCreepersKilled = value);
        toggle(combat, entries, "Endermen killed", config.showEndermenKilled, value -> config.showEndermenKilled = value);
        toggle(combat, entries, "Withers defeated", config.showWithersKilled, value -> config.showWithersKilled = value);
        toggle(combat, entries, "Damage dealt", config.showDamageDealt, value -> config.showDamageDealt = value);
        toggle(combat, entries, "Damage taken", config.showDamageTaken, value -> config.showDamageTaken = value);
        toggle(combat, entries, "Arrows shot", config.showShotsFired, value -> config.showShotsFired = value);
        toggle(combat, entries, "Raids won", config.showRaidsWon, value -> config.showRaidsWon = value);
        toggle(combat, entries, "Alive for", config.showAliveFor, value -> config.showAliveFor = value);

        toggle(world, entries, "Animals bred", config.showAnimalsBred, value -> config.showAnimalsBred = value);
        toggle(world, entries, "Fish caught", config.showFishCaught, value -> config.showFishCaught = value);
        toggle(world, entries, "Chests opened", config.showChestsOpened, value -> config.showChestsOpened = value);
        toggle(world, entries, "Villager trades", config.showVillagerTrades, value -> config.showVillagerTrades = value);
        toggle(world, entries, "Items enchanted", config.showItemsEnchanted, value -> config.showItemsEnchanted = value);
        toggle(world, entries, "Food eaten", config.showFoodEaten, value -> config.showFoodEaten = value);
        toggle(world, entries, "Advancements", config.showAdvancements, value -> config.showAdvancements = value);
        toggle(world, entries, "Cake slices eaten", config.showCakesEaten, value -> config.showCakesEaten = value);
        toggle(world, entries, "Bells rung", config.showBellsRung, value -> config.showBellsRung = value);
        toggle(world, entries, "Flowers potted", config.showFlowersPotted, value -> config.showFlowersPotted = value);

        return builder.build();
    }

    private static void toggle(
            ConfigCategory category,
            ConfigEntryBuilder entries,
            String label,
            boolean current,
            Consumer<Boolean> setter
    ) {
        category.addEntry(entries.startBooleanToggle(Component.literal(label), current)
                .setDefaultValue(true)
                .setSaveConsumer(setter)
                .build());
    }
}
