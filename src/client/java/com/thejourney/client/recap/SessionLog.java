package com.thejourney.client.recap;

import com.google.gson.Gson;
import com.thejourney.client.shots.EndShots;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Minecraft does not store how long each play session lasted, so The Journey measures it
 * itself from the moment it is installed. "Longest session" is therefore only ever shown
 * for time that was really measured.
 */
public final class SessionLog {
    private static final Gson GSON = new Gson();
    private static final long MIN_REPORT_MS = 5 * 60 * 1000L;

    private static final class Data {
        Map<String, Long> longestMs = new HashMap<>();
    }

    private static Data data = null;
    private static long startMs = -1;
    private static String key = "unknown";

    private SessionLog() {
    }

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            key = EndShots.worldKey();
            startMs = System.currentTimeMillis();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> finish());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> finish());
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("the-journey-sessions.json");
    }

    private static Data data() {
        if (data == null) {
            data = new Data();
            try {
                if (Files.exists(file())) {
                    Data loaded = GSON.fromJson(Files.readString(file()), Data.class);
                    if (loaded != null && loaded.longestMs != null) {
                        data = loaded;
                    }
                }
            } catch (Exception ignored) {
                // Start fresh.
            }
        }
        return data;
    }

    private static void finish() {
        if (startMs < 0) {
            return;
        }
        long length = System.currentTimeMillis() - startMs;
        startMs = -1;

        if (length > data().longestMs.getOrDefault(key, 0L)) {
            data().longestMs.put(key, length);
            try {
                Files.createDirectories(file().getParent());
                Files.writeString(file(), GSON.toJson(data()));
            } catch (Exception ignored) {
                // Not critical.
            }
        }
    }

    /** How long this session has lasted so far, or -1 if unknown. */
    public static long currentMs() {
        return startMs < 0 ? -1 : System.currentTimeMillis() - startMs;
    }

    /** Longest measured session for this world, or -1 if nothing worth showing was measured. */
    public static long longestMs() {
        long best = Math.max(currentMs(), data().longestMs.getOrDefault(EndShots.worldKey(), 0L));
        return best >= MIN_REPORT_MS ? best : -1;
    }
}
