package com.thejourney.client.recap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thejourney.client.config.JourneyConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * Small drawing toolkit for the recap: colours, glows, chamfered panels, text helpers,
 * pixel icons and a tiny particle system. All helpers multiply colours by {@link #alpha},
 * so a whole card can fade in by setting that one value.
 */
public final class Fx {
    /** Global alpha multiplier for everything drawn through these helpers. */
    public static float alpha = 1.0F;

    private Fx() {
    }

    // ------------------------------------------------------------------ colours

    /** Applies the global alpha (and an extra multiplier) to a colour. */
    public static int a(int argb, float mul) {
        int base = argb >>> 24;
        int out = Math.round(base * mul * alpha);
        return (Math.max(0, Math.min(255, out)) << 24) | (argb & 0xFFFFFF);
    }

    public static int a(int argb) {
        return a(argb, 1.0F);
    }

    public static int rgba(int rgb, float alphaValue) {
        return (Math.max(0, Math.min(255, Math.round(alphaValue * 255.0F))) << 24) | (rgb & 0xFFFFFF);
    }

    public static int lerp(int c1, int c2, float t) {
        t = Anim.clamp(t);
        int a = Math.round(((c1 >>> 24) & 255) * (1 - t) + ((c2 >>> 24) & 255) * t);
        int r = Math.round(((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = Math.round(((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = Math.round((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Mixes two colours, always opaque. */
    public static int mix(int c1, int c2, float t) {
        return 0xFF000000 | (lerp(c1, c2, t) & 0xFFFFFF);
    }

    public static int withAlpha(int argb, int alpha255) {
        return (alpha255 << 24) | (argb & 0xFFFFFF);
    }

    // ------------------------------------------------------------------ shapes

    public static void fill(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int c = a(color);
        if ((c >>> 24) == 0 || x2 <= x1 || y2 <= y1) {
            return;
        }
        g.fill(x1, y1, x2, y2, c);
    }

    public static void grad(GuiGraphics g, int x1, int y1, int x2, int y2, int top, int bottom) {
        int t = a(top);
        int b = a(bottom);
        if (((t | b) >>> 24) == 0 || x2 <= x1 || y2 <= y1) {
            return;
        }
        g.fillGradient(x1, y1, x2, y2, t, b);
    }

    public static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
        fill(g, x, y, x + w, y + 1, color);
        fill(g, x, y + h - 1, x + w, y + h, color);
        fill(g, x, y + 1, x + 1, y + h - 1, color);
        fill(g, x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** Panel with cut corners (a pixel-art "rounded" rectangle). */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int top, int bottom, int border) {
        grad(g, x + 1, y, x + w - 1, y + h, top, bottom);
        grad(g, x, y + 1, x + 1, y + h - 1, top, bottom);
        grad(g, x + w - 1, y + 1, x + w, y + h - 1, top, bottom);

        fill(g, x + 1, y, x + w - 1, y + 1, border);
        fill(g, x + 1, y + h - 1, x + w - 1, y + h, border);
        fill(g, x, y + 1, x + 1, y + h - 1, border);
        fill(g, x + w - 1, y + 1, x + w, y + h - 1, border);
    }

    /** Soft glow around a rectangle: a few outlines that fade out. */
    public static void glow(GuiGraphics g, int x, int y, int w, int h, int rgb, float strength, int layers) {
        for (int i = 1; i <= layers; i++) {
            float fade = strength * (1.0F - (i - 1) / (float) layers) * 0.35F;
            int c = rgba(rgb, fade);
            frame(g, x - i, y - i, w + i * 2, h + i * 2, c);
        }
    }

    /** Filled circle, drawn as horizontal bars. */
    public static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            fill(g, cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    /** Circle outline of the given thickness. */
    public static void ring(GuiGraphics g, int cx, int cy, int r, int thickness, int color) {
        if (r <= 0) {
            return;
        }
        for (int dy = -r; dy <= r; dy++) {
            int outer = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            int ri = r - thickness;
            int inner = Math.abs(dy) < ri ? (int) Math.round(Math.sqrt((double) ri * ri - (double) dy * dy)) : 0;
            if (inner == 0) {
                fill(g, cx - outer, cy + dy, cx + outer + 1, cy + dy + 1, color);
            } else {
                fill(g, cx - outer, cy + dy, cx - inner, cy + dy + 1, color);
                fill(g, cx + inner + 1, cy + dy, cx + outer + 1, cy + dy + 1, color);
            }
        }
    }

    // ------------------------------------------------------------------ pixel icons

    public static final String[] GEAR = {
            "..#.#.#..",
            ".#######.",
            "###...###",
            "##.....##",
            "###...###",
            ".#######.",
            "..#.#.#.."
    };

    /** Points right. Flip it for a left arrow. */
    public static final String[] CHEVRON = {
            "#..",
            ".#.",
            "..#",
            ".#.",
            "#.."
    };

    public static final String[] LOCK = {
            ".###.",
            "#...#",
            "#...#",
            "#####",
            "##.##",
            "#####"
    };

    public static final String[] PLAY = {
            "#...",
            "##..",
            "###.",
            "####",
            "###.",
            "##..",
            "#..."
    };

    public static void pixels(GuiGraphics g, int x, int y, String[] rows, int color, boolean flip) {
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            int width = row.length();
            int c = 0;
            while (c < width) {
                if (row.charAt(c) != '#') {
                    c++;
                    continue;
                }
                int start = c;
                while (c < width && row.charAt(c) == '#') {
                    c++;
                }
                int x1 = flip ? width - c : start;
                int x2 = flip ? width - start : c;
                fill(g, x + x1, y + r, x + x2, y + r + 1, color);
            }
        }
    }

    // ------------------------------------------------------------------ text

    /** Minecraft draws fully transparent text as opaque, so skip text that is (nearly) invisible. */
    private static boolean visible(int color) {
        return (a(color) >>> 24) > 6;
    }

    public static void text(GuiGraphics g, Font font, String s, int x, int y, int color) {
        if (visible(color)) {
            g.drawString(font, s, x, y, a(color), true);
        }
    }

    public static void textFlat(GuiGraphics g, Font font, String s, int x, int y, int color) {
        if (visible(color)) {
            g.drawString(font, s, x, y, a(color), false);
        }
    }

    public static void textC(GuiGraphics g, Font font, String s, int cx, int y, int color) {
        text(g, font, s, cx - font.width(s) / 2, y, color);
    }

    public static void textR(GuiGraphics g, Font font, String s, int right, int y, int color) {
        text(g, font, s, right - font.width(s), y, color);
    }

    /** Scaled text, centred on cx (or left aligned at cx when {@code centered} is false). */
    public static void textScaled(GuiGraphics g, Font font, String s, int cx, int y, int color,
                                  float scale, boolean centered) {
        if (!visible(color)) {
            return;
        }
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(cx, y, 0.0F);
        pose.scale(scale, scale, 1.0F);
        int width = font.width(s);
        g.drawString(font, s, centered ? -width / 2 : 0, 0, a(color), true);
        pose.popPose();
    }

    /** Draws a value so it always fits {@code maxWidth}, shrinking it when needed. */
    public static void textFit(GuiGraphics g, Font font, String s, int cx, int y, int color,
                               float preferredScale, int maxWidth) {
        float scale = preferredScale;
        int width = font.width(s);
        if (width * scale > maxWidth) {
            scale = maxWidth / (float) width;
        }
        textScaled(g, font, s, cx, y, color, scale, true);
    }

    /** Word wrap with an ellipsis if the text does not fit in {@code maxLines}. */
    public static List<String> wrap(Font font, String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty() || maxWidth <= 0 || maxLines <= 0) {
            return lines;
        }

        String[] words = text.split(" ");
        String line = "";

        for (int i = 0; i < words.length; i++) {
            String candidate = line.isEmpty() ? words[i] : line + " " + words[i];

            if (line.isEmpty() || font.width(candidate) <= maxWidth) {
                line = candidate;
                continue;
            }

            if (lines.size() == maxLines - 1) {
                // Last line we are allowed to use: it gets everything that is left, cut with "...".
                String rest = line + " " + String.join(" ", java.util.Arrays.copyOfRange(words, i, words.length));
                lines.add(ellipsize(font, rest, maxWidth));
                return lines;
            }

            lines.add(line);
            line = words[i];
        }

        if (!line.isEmpty()) {
            lines.add(ellipsize(font, line, maxWidth));
        }
        return lines;
    }

    public static String ellipsize(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String dots = "...";
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + dots) > maxWidth) {
            end--;
        }
        return text.substring(0, Math.max(0, end)).stripTrailing() + dots;
    }

    // ------------------------------------------------------------------ particles

    /** A tiny pool of glowing motes. Capped, cheap, and silent when particles are switched off. */
    public static final class Particles {
        private static final int MAX = 110;
        private static final float[] px = new float[MAX];
        private static final float[] py = new float[MAX];
        private static final float[] vx = new float[MAX];
        private static final float[] vy = new float[MAX];
        private static final float[] life = new float[MAX];
        private static final float[] max = new float[MAX];
        private static final float[] size = new float[MAX];
        private static final float[] gravity = new float[MAX];
        private static final int[] color = new int[MAX];
        private static int count = 0;
        private static final java.util.Random RANDOM = new java.util.Random();

        private Particles() {
        }

        public static boolean enabled() {
            return JourneyConfig.get().particles && Anim.on();
        }

        public static void clear() {
            count = 0;
        }

        private static void add(float x, float y, float dx, float dy, float seconds, float sz, int rgb, float grav) {
            if (!enabled() || count >= MAX) {
                return;
            }
            px[count] = x;
            py[count] = y;
            vx[count] = dx;
            vy[count] = dy;
            life[count] = seconds;
            max[count] = seconds;
            size[count] = sz;
            gravity[count] = grav;
            color[count] = rgb;
            count++;
        }

        /** A slow rising mote somewhere in the rectangle. */
        public static void ambient(int x, int y, int w, int h, int rgb) {
            if (!enabled() || w <= 0 || h <= 0) {
                return;
            }
            add(x + RANDOM.nextInt(w), y + h - RANDOM.nextInt(Math.max(1, h / 3)),
                    (RANDOM.nextFloat() - 0.5F) * 6.0F, -(6.0F + RANDOM.nextFloat() * 10.0F),
                    3.0F + RANDOM.nextFloat() * 3.0F, 1.0F + RANDOM.nextInt(2), rgb, 0.0F);
        }

        /** Sparks flying out of a point. */
        public static void burst(float x, float y, int amount, int rgb1, int rgb2) {
            if (!enabled()) {
                return;
            }
            for (int i = 0; i < amount; i++) {
                double angle = RANDOM.nextDouble() * Math.PI * 2.0;
                float speed = 25.0F + RANDOM.nextFloat() * 55.0F;
                add(x, y, (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed - 12.0F,
                        0.6F + RANDOM.nextFloat() * 0.7F, 1.0F + RANDOM.nextInt(2),
                        RANDOM.nextBoolean() ? rgb1 : rgb2, 70.0F);
            }
        }

        /** Rising sparkles from the bottom edge (used on the final screen). */
        public static void fountain(int x, int y, int w, int amount, int rgb1, int rgb2) {
            if (!enabled()) {
                return;
            }
            for (int i = 0; i < amount; i++) {
                add(x + RANDOM.nextInt(Math.max(1, w)), y,
                        (RANDOM.nextFloat() - 0.5F) * 30.0F, -(50.0F + RANDOM.nextFloat() * 70.0F),
                        1.2F + RANDOM.nextFloat() * 1.2F, 1.0F + RANDOM.nextInt(2),
                        RANDOM.nextBoolean() ? rgb1 : rgb2, 40.0F);
            }
        }

        public static void update(float seconds) {
            for (int i = count - 1; i >= 0; i--) {
                life[i] -= seconds;
                if (life[i] <= 0.0F) {
                    // Swap-remove.
                    count--;
                    px[i] = px[count];
                    py[i] = py[count];
                    vx[i] = vx[count];
                    vy[i] = vy[count];
                    life[i] = life[count];
                    max[i] = max[count];
                    size[i] = size[count];
                    gravity[i] = gravity[count];
                    color[i] = color[count];
                    continue;
                }
                vy[i] += gravity[i] * seconds;
                px[i] += vx[i] * seconds;
                py[i] += vy[i] * seconds;
            }
        }

        public static int count() {
            return count;
        }

        public static void draw(GuiGraphics g, int x, int y, int w, int h) {
            if (count == 0) {
                return;
            }
            g.enableScissor(x, y, x + w, y + h);
            for (int i = 0; i < count; i++) {
                float t = life[i] / max[i];
                // Fade in quickly, fade out slowly, with a little twinkle.
                float fade = Math.min(1.0F, t * 2.5F) * Math.min(1.0F, (1.0F - t) * 8.0F + 0.2F);
                float twinkle = 0.75F + 0.25F * (float) Math.sin((life[i] + i) * 18.0F);
                float intensity = fade * twinkle;

                int s = Math.max(1, Math.round(size[i]));
                int ix = Math.round(px[i]);
                int iy = Math.round(py[i]);
                fill(g, ix - 1, iy - 1, ix + s + 1, iy + s + 1, rgba(color[i], intensity * 0.22F));
                fill(g, ix, iy, ix + s, iy + s, rgba(color[i], intensity * 0.95F));
            }
            g.disableScissor();
        }
    }
}
