package com.thejourney.client.recap;

import com.thejourney.client.config.JourneyConfig;

/** Animation timing. Everything respects the "animations" and "speed" settings. */
public final class Anim {
    private static final long T0 = System.nanoTime();

    private Anim() {
    }

    /** Milliseconds since the game started drawing recap screens (monotonic). */
    public static long now() {
        return (System.nanoTime() - T0) / 1_000_000L;
    }

    public static boolean on() {
        return JourneyConfig.get().animations;
    }

    public static float speed() {
        return Math.max(0.25F, Math.min(3.0F, JourneyConfig.get().animationSpeed / 100.0F));
    }

    /** Converts a real-time delay into the delay for the current speed setting. */
    public static long scaled(long ms) {
        return Math.round(ms / speed());
    }

    /**
     * Progress 0..1 of an animation that started at {@code start}.
     * With animations off this is always 1, so everything is simply "there".
     * A negative start means "not started".
     */
    public static float prog(long start, long delayMs, long durationMs) {
        if (!on()) {
            return 1.0F;
        }
        if (start < 0) {
            return 0.0F;
        }
        float elapsed = (now() - start) * speed() - delayMs;
        return clamp(elapsed / Math.max(1, durationMs));
    }

    public static float clamp(float t) {
        return t < 0.0F ? 0.0F : Math.min(t, 1.0F);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float outCubic(float t) {
        float i = 1.0F - clamp(t);
        return 1.0F - i * i * i;
    }

    public static float outExpo(float t) {
        t = clamp(t);
        return t >= 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0, -10.0 * t);
    }

    public static float inOutCubic(float t) {
        t = clamp(t);
        return t < 0.5F ? 4 * t * t * t : 1.0F - (float) Math.pow(-2 * t + 2, 3) / 2.0F;
    }

    /** Overshoots a little, then settles: good for "pop" entrances. */
    public static float outBack(float t) {
        t = clamp(t);
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float u = t - 1.0F;
        return 1.0F + c3 * u * u * u + c1 * u * u;
    }

    /** Slow breathing 0..1, not affected by the speed setting (ambient motion). */
    public static float pulse(long periodMs) {
        if (!on()) {
            return 0.5F;
        }
        return 0.5F + 0.5F * (float) Math.sin(now() * (Math.PI * 2.0) / periodMs);
    }
}
