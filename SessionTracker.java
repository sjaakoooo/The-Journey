package com.thejourney;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers each player's totals at the moment they joined, so the recap can
 * show what happened during the current session only.
 */
public final class SessionTracker {
    /** The totals that are tracked per session. */
    public record Totals(
            long distanceCm,
            long playTicks,
            long blocksMined,
            long mobsKilled,
            long deaths,
            long itemsCrafted,
            long blocksPlaced,
            long jumps
    ) {
    }

    private static final Map<UUID, Totals> BASELINES = new ConcurrentHashMap<>();

    private SessionTracker() {
    }

    public static void start(UUID player, Totals totals) {
        BASELINES.put(player, totals);
    }

    public static void stop(UUID player) {
        BASELINES.remove(player);
    }

    /** The totals at join time, or {@code fallback} if this player was never recorded. */
    public static Totals baseline(UUID player, Totals fallback) {
        return BASELINES.getOrDefault(player, fallback);
    }
}
