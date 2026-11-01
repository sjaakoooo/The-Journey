package com.thejourney.client.recap;

import com.thejourney.client.config.JourneyConfig;
import com.thejourney.client.config.JourneyTheme;
import com.thejourney.client.config.JourneyTheme.ThemePalette;

/** Smoothly fades from the old theme to a newly selected one. */
public final class ThemeBlend {
    private static final long DURATION_MS = 450;

    private static JourneyTheme target = null;
    private static int[] from = null;
    private static long start = 0;
    private static ThemePalette cached = null;
    private static long cachedAt = -1;

    private ThemeBlend() {
    }

    /** The palette to draw with right now. */
    public static ThemePalette current() {
        JourneyTheme wanted = JourneyConfig.get().theme;
        if (wanted == null) {
            wanted = JourneyTheme.END_PORTAL;
        }

        if (target == null) {
            target = wanted;
            return wanted.palette();
        }

        if (wanted != target) {
            // The theme changed: start fading from whatever is on screen now.
            from = (cached != null ? cached : target.palette()).toArray();
            target = wanted;
            start = Anim.now();
        }

        if (from == null) {
            return target.palette();
        }

        long now = Anim.now();
        float t = Anim.on() ? Anim.inOutCubic((now - start) / (float) DURATION_MS) : 1.0F;
        if (t >= 1.0F) {
            from = null;
            cached = target.palette();
            return cached;
        }

        if (cachedAt == now && cached != null) {
            return cached;
        }

        int[] to = target.palette().toArray();
        int[] mixed = new int[to.length];
        for (int i = 0; i < to.length; i++) {
            mixed[i] = Fx.lerp(from[i], to[i], t);
        }
        cached = ThemePalette.fromArray(mixed);
        cachedAt = now;
        return cached;
    }
}
