package com.thejourney.client.shots;

import com.mojang.blaze3d.platform.NativeImage;
import com.thejourney.Milestones;
import com.thejourney.TheJourney;
import com.thejourney.client.config.JourneyConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Collects small pictures of the game view (without HUD) for the End Credits background:
 *
 * - End screenshots: taken at random moments while you are in the End (max 300).
 *   Deleted when you leave the world, quit the game, or when the credits close.
 * - Milestone photos: taken at big moments (first Crafting Table, entering the Nether, ...).
 *   Kept per world until the credits close, so they survive restarts. Then deleted.
 *
 * Everything stays on this computer, in the "the-journey-temp" folder of the game directory.
 */
public final class EndShots {
    public static final int MAX_SHOTS = 300;

    private static final int END_WIDTH = 640;
    private static final int MILESTONE_WIDTH = 960;
    private static final Random RANDOM = new Random();

    private static final ExecutorService IO = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "the-journey-shots");
        thread.setDaemon(true);
        return thread;
    });

    private static int savedSlots = 0;
    private static int countdownTicks = 100;
    private static boolean pendingEndShot = false;
    private static volatile String pendingMilestone = null;

    /** One picture for the credits, with an optional caption. */
    public record Entry(Path path, String caption) {
    }

    private EndShots() {
    }

    // ------------------------------------------------------------------ folders

    private static Path baseDir() {
        return FabricLoader.getInstance().getGameDir().resolve("the-journey-temp");
    }

    private static String worldKey() {
        Minecraft client = Minecraft.getInstance();
        String raw = "unknown";

        if (client.getSingleplayerServer() != null) {
            raw = "sp_" + client.getSingleplayerServer().getWorldData().getLevelName();
        } else if (client.getCurrentServer() != null) {
            raw = "mp_" + client.getCurrentServer().ip;
        }

        return raw.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private static Path worldDir() {
        return baseDir().resolve(worldKey());
    }

    // ------------------------------------------------------------------ setup

    public static void init() {
        // Leftovers from an earlier session (for example after a crash).
        deleteEndShots();

        ClientTickEvents.END_CLIENT_TICK.register(EndShots::tick);
        WorldRenderEvents.END.register(context -> captureIfPending());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ShotSlideshow.release();
            deleteEndShots();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> deleteEndShots());
    }

    // ------------------------------------------------------------------ capturing

    /** The server says a milestone was reached: take a photo on the next frame. */
    public static void requestMilestone(String id) {
        if (JourneyConfig.get().captureMilestones && Milestones.CAPTIONS.containsKey(id)) {
            pendingMilestone = id;
        }
    }

    private static void tick(Minecraft client) {
        if (!JourneyConfig.get().captureEndScreenshots
                || client.player == null
                || client.level == null
                || client.level.dimension() != Level.END) {
            // Outside the End: start over when you come back.
            countdownTicks = 100;
            pendingEndShot = false;
            return;
        }

        // Only during real gameplay, not in menus.
        if (client.screen != null) {
            return;
        }

        countdownTicks--;
        if (countdownTicks <= 0) {
            pendingEndShot = true;

            // Next one after 0.5x to 1.5x the average interval.
            int average = Math.max(5, JourneyConfig.get().shotIntervalSeconds) * 20;
            countdownTicks = average / 2 + RANDOM.nextInt(average);
        }
    }

    /** Called after the world is drawn but before the HUD, so pictures have no interface in them. */
    private static void captureIfPending() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }

        String milestone = pendingMilestone;
        if (milestone != null) {
            pendingMilestone = null;
            captureMilestone(client, milestone);
        }

        if (!pendingEndShot) {
            return;
        }
        pendingEndShot = false;

        if (client.screen != null
                || client.level.dimension() != Level.END
                || !JourneyConfig.get().captureEndScreenshots) {
            return;
        }

        NativeImage full = take(client);
        if (full == null) {
            return;
        }

        int slot;
        if (savedSlots < MAX_SHOTS) {
            slot = savedSlots++;
        } else {
            slot = RANDOM.nextInt(MAX_SHOTS);
        }

        Path target = worldDir().resolve("end").resolve(
                String.format(Locale.ROOT, "shot_%03d.png", slot));
        IO.execute(() -> write(full, target, END_WIDTH));
    }

    private static void captureMilestone(Minecraft client, String id) {
        NativeImage full = take(client);
        if (full == null) {
            return;
        }

        Path target = worldDir().resolve("milestones").resolve("milestone_" + id + ".png");
        IO.execute(() -> write(full, target, MILESTONE_WIDTH));
    }

    private static NativeImage take(Minecraft client) {
        try {
            return Screenshot.takeScreenshot(client.getMainRenderTarget());
        } catch (Throwable throwable) {
            TheJourney.LOGGER.debug("Could not take screenshot", throwable);
            return null;
        }
    }

    private static void write(NativeImage full, Path target, int targetWidth) {
        try (full) {
            int width = Math.min(targetWidth, full.getWidth());
            int height = Math.max(
                    1,
                    Math.round(full.getHeight() * (width / (float) full.getWidth()))
            );

            try (NativeImage small = new NativeImage(width, height, false)) {
                full.resizeSubRectTo(0, 0, full.getWidth(), full.getHeight(), small);
                Files.createDirectories(target.getParent());
                small.writeToFile(target);
            }
        } catch (Exception exception) {
            TheJourney.LOGGER.debug("Could not save screenshot", exception);
        }
    }

    // ------------------------------------------------------------------ for the credits

    /**
     * Pictures for the credits slideshow: milestones in order of progression,
     * mixed in between shuffled End screenshots.
     */
    public static List<Entry> listForCredits() {
        Path world = worldDir();

        List<Entry> milestones = new ArrayList<>();
        for (String id : Milestones.CAPTIONS.keySet()) {
            Path file = world.resolve("milestones").resolve("milestone_" + id + ".png");
            if (Files.isRegularFile(file)) {
                milestones.add(new Entry(file, Milestones.CAPTIONS.get(id)));
            }
        }

        List<Entry> endShots = new ArrayList<>();
        Path endDir = world.resolve("end");
        if (Files.isDirectory(endDir)) {
            try (Stream<Path> stream = Files.list(endDir)) {
                stream.filter(path -> path.getFileName().toString().startsWith("shot_"))
                        .sorted(Comparator.comparing(Path::toString))
                        .forEach(path -> endShots.add(new Entry(path, null)));
            } catch (IOException ignored) {
                // Then there is simply no background.
            }
        }
        Collections.shuffle(endShots, RANDOM);

        List<Entry> result = new ArrayList<>();
        int nextMilestone = 0;

        // One milestone after every three End screenshots, starting with a milestone.
        for (int i = 0; i < endShots.size(); i++) {
            if (i % 3 == 0 && nextMilestone < milestones.size()) {
                result.add(milestones.get(nextMilestone++));
            }
            result.add(endShots.get(i));
        }
        while (nextMilestone < milestones.size()) {
            result.add(milestones.get(nextMilestone++));
        }

        return result;
    }

    // ------------------------------------------------------------------ deleting

    /** Deletes the End screenshots of every world. Milestone photos stay. */
    public static void deleteEndShots() {
        pendingEndShot = false;
        savedSlots = 0;

        runAndWait(() -> {
            Path base = baseDir();
            if (!Files.isDirectory(base)) {
                return;
            }

            try (Stream<Path> worlds = Files.list(base)) {
                worlds.forEach(world -> deleteTree(world.resolve("end")));
            } catch (IOException ignored) {
                // Tried again next time.
            }
        });
    }

    /** Credits are over: delete everything saved for the current world. */
    public static void deleteEverythingForWorld() {
        pendingMilestone = null;
        Path world = worldDir();

        deleteEndShots();
        runAndWait(() -> deleteTree(world));
    }

    /** Runs after any still-pending writes (same thread), waits at most 3 seconds. */
    private static void runAndWait(Runnable task) {
        Future<?> future = IO.submit(task);
        try {
            future.get(3, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // The task continues in the background.
        }
    }

    private static void deleteTree(Path root) {
        if (!Files.exists(root)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Tried again next time.
                }
            });
        } catch (IOException ignored) {
            // Idem.
        }
    }
}
