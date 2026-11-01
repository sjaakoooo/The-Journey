package com.thejourney.client.recap;

import com.thejourney.client.config.JourneyConfig;
import com.thejourney.client.config.JourneyTheme;
import com.thejourney.client.config.JourneyTheme.ThemePalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** The settings page and the final screen. */
final class PagesB {
    private static final String[] FINALE_STATS = {
            "play_time", "distance", "blocks_mined", "mobs_killed", "diamonds_mined",
            "ender_dragon_kills", "advancements", "items_crafted", "days"
    };

    private static long deleteArmedAt = 0;

    private PagesB() {
    }

    // ================================================================== settings

    static int settings(RecapView v) {
        GuiGraphics g = v.g;
        Font font = v.font;
        ThemePalette pal = v.pal;
        JourneyConfig config = JourneyConfig.get();
        int pad = 8;
        int x = v.vx + pad;
        int w = v.vw - pad * 2 - 4;
        int top = v.vy - (int) v.scroll;
        int y = top + 4;

        // ---- themes ----
        section(v, "THEME", x, y, w);
        y += 13;
        JourneyTheme[] themes = JourneyTheme.values();
        int cols = w >= 300 ? 4 : 2;
        int gap = 4;
        int chipW = (w - gap * (cols - 1)) / cols;
        int chipH = 32;
        for (int i = 0; i < themes.length; i++) {
            int cx = x + (i % cols) * (chipW + gap);
            int cy = y + (i / cols) * (chipH + gap);
            if (v.visible(cy - 4, chipH + 8)) {
                themeChip(v, themes[i], cx, cy, chipW, chipH, config.theme == themes[i]);
            }
        }
        y += ((themes.length + cols - 1) / cols) * (chipH + gap) + 6;

        // ---- animations ----
        section(v, "ANIMATIONS", x, y, w);
        y += 13;
        toggle(v, "anim", "Animations", config.animations, x, y, w, value -> config.animations = value);
        y += 18;
        segmented(v, "speed", "Animation speed", new String[]{"0.5x", "1x", "1.5x", "2x"},
                speedIndex(config.animationSpeed), x, y, w, i -> config.animationSpeed = new int[]{50, 100, 150, 200}[i]);
        y += 31;
        JourneyConfig.RevealStyle[] styles = JourneyConfig.RevealStyle.values();
        segmented(v, "style", "Reveal style", new String[]{"Cinematic", "Slide", "Fade", "Pop"},
                config.revealStyle.ordinal(), x, y, w, i -> config.revealStyle = styles[i]);
        y += 31;
        toggle(v, "particles", "Particles", config.particles, x, y, w, value -> config.particles = value);
        y += 22;

        // ---- audio ----
        section(v, "AUDIO", x, y, w);
        y += 13;
        toggle(v, "sfx", "Sound effects", config.soundEffects, x, y, w, value -> config.soundEffects = value);
        y += 18;
        volume(v, x, y, w, config);
        y += 31;
        toggle(v, "music", v.host.standalone() ? "Background music" : "Background music (recap screen only)",
                config.backgroundMusic, x, y, w, value -> {
                    config.backgroundMusic = value;
                    if (v.host.standalone()) {
                        if (value) {
                            Sfx.startMusic();
                        } else {
                            Sfx.stopMusic();
                        }
                    }
                });
        y += 22;

        // ---- content ----
        section(v, "CONTENT", x, y, w);
        y += 13;
        toggle(v, "empty", "Hide empty statistics", config.hideEmptyStats, x, y, w, value -> config.hideEmptyStats = value);
        y += 24;

        toggle(v, "keep", "Keep photos after the credits", config.keepPhotos, x, y, w, value -> config.keepPhotos = value);
        y += 22;

        boolean armed = v.fired.contains("del_armed") && Anim.now() - deleteArmedAt < 3500;
        v.button("delete", x + 6, y, w - 12, 16, armed ? "Click again to delete all photos" : "Delete saved photos now", false, () -> {
            if (armed) {
                com.thejourney.client.shots.EndShots.deletePhotosNow();
                Gallery.release();
                Gallery.refresh();
                RecapModel.refreshHighlights();
                v.fired.remove("del_armed");
                v.photoIndex = 0;
                v.photoPrevIndex = -1;
            } else {
                v.fired.add("del_armed");
                deleteArmedAt = Anim.now();
            }
        });
        y += 22;

        v.button("replay", x + w / 2 - 55, y, 110, 16, "Replay recap", true, v::replay);
        y += 24;
        return y - top;
    }

    private static int speedIndex(int percent) {
        int[] values = {50, 100, 150, 200};
        int best = 1;
        for (int i = 0; i < values.length; i++) {
            if (Math.abs(values[i] - percent) < Math.abs(values[best] - percent)) {
                best = i;
            }
        }
        return best;
    }

    private static void section(RecapView v, String label, int x, int y, int w) {
        Fx.textScaled(v.g, v.font, label, x, y, v.pal.accent2(), 0.9F, false);
        Fx.grad(v.g, x + 60, y + 4, x + w, y + 5, Fx.rgba(v.pal.accent2(), 0.6F), Fx.rgba(v.pal.accent2(), 0.0F));
    }

    private static void changed() {
        JourneyConfig.saveQuietly();
    }

    private static void themeChip(RecapView v, JourneyTheme theme, int x, int y, int w, int h, boolean selected) {
        GuiGraphics g = v.g;
        ThemePalette p = theme.palette();
        boolean hovered = v.over(x, y, w, h);
        float t = v.hov("theme_" + theme.name(), hovered);

        if (selected || t > 0.05F) {
            Fx.glow(g, x, y, w, h, p.accent(), selected ? 0.9F : t * 0.7F, 3);
        }
        Fx.panel(g, x, y, w, h, Fx.withAlpha(p.bgTop(), 255), Fx.withAlpha(p.bgBottom(), 255),
                selected ? p.accent() : Fx.mix(p.border(), p.highlight(), t * 0.6F));

        // Swatch strip: three of the theme's colours.
        int sx = x + 4;
        int sw = (w - 8) / 3;
        Fx.fill(g, sx, y + 4, sx + sw, y + 13, Fx.withAlpha(p.tileTop(), 255));
        Fx.fill(g, sx + sw, y + 4, sx + sw * 2, y + 13, p.accent());
        Fx.fill(g, sx + sw * 2, y + 4, x + w - 4, y + 13, p.accent2());

        String name = Fx.ellipsize(v.font, theme.displayName(), (int) ((w - 6) / 0.8F));
        Fx.textScaled(g, v.font, name, x + w / 2, y + 18, selected ? p.title() : p.subtitle(), 0.8F, true);
        if (selected) {
            Fx.fill(g, x + w / 2 - 6, y + h - 4, x + w / 2 + 6, y + h - 3, p.accent());
        }

        v.hit(x, y, w, h, () -> {
            if (JourneyConfig.get().theme != theme) {
                JourneyConfig.get().theme = theme;
                changed();
                Sfx.play(Sfx.Kind.TOGGLE);
            }
        });
    }

    private static void toggle(RecapView v, String key, String label, boolean value,
                               int x, int y, int w, Consumer<Boolean> setter) {
        GuiGraphics g = v.g;
        ThemePalette pal = v.pal;
        boolean hovered = v.over(x, y, w, 18);
        float hv = v.hov("th_" + key, hovered);
        float on = v.hov("tg_" + key, value);

        if (hv > 0.05F) {
            Fx.fill(g, x, y, x + w, y + 18, Fx.rgba(pal.tileHoverTop(), 0.5F * hv));
        }
        Fx.text(g, v.font, Fx.ellipsize(v.font, label, w - 44), x + 6, y + 5, Fx.mix(pal.subtitle(), pal.title(), hv));

        int px = x + w - 32;
        int py = y + 4;
        Fx.fill(g, px - 1, py - 1, px + 27, py + 11, pal.outline());
        Fx.fill(g, px, py, px + 26, py + 10, Fx.lerp(0xFF22222C, Fx.withAlpha(pal.accent(), 255), on));
        int knob = px + 1 + (int) (on * 16);
        Fx.fill(g, knob, py + 1, knob + 9, py + 9, Fx.lerp(0xFF8A8A98, 0xFFFFFFFF, on));

        v.hit(x, y, w, 18, () -> {
            setter.accept(!value);
            changed();
            Sfx.play(Sfx.Kind.TOGGLE, value ? 0.85F : 1.15F);
        });
    }

    private static void segmented(RecapView v, String key, String label, String[] options, int selected,
                                  int x, int y, int w, IntConsumer setter) {
        GuiGraphics g = v.g;
        ThemePalette pal = v.pal;
        Fx.text(g, v.font, label, x + 6, y + 2, pal.subtitle());

        int count = options.length;
        int segW = (w - 12) / count;
        for (int i = 0; i < count; i++) {
            int sx = x + 6 + i * segW;
            boolean active = i == selected;
            boolean hovered = v.over(sx, y + 13, segW, 15);
            float t = v.hov("seg_" + key + i, hovered);
            float on = v.hov("segon_" + key + i, active);

            int fill = Fx.lerp(Fx.lerp(pal.tileTop(), pal.tileHoverTop(), t), Fx.mix(pal.tileTop(), pal.accent(), 0.55F), on);
            Fx.fill(g, sx, y + 13, sx + segW - 1, y + 28, fill);
            Fx.frame(g, sx, y + 13, segW - 1, 15, active ? pal.accent() : Fx.rgba(pal.border(), 0.8F));
            Fx.textC(g, v.font, Fx.ellipsize(v.font, options[i], segW - 4), sx + (segW - 1) / 2, y + 17,
                    active ? pal.title() : Fx.mix(pal.subtitle(), pal.title(), t));

            int index = i;
            v.hit(sx, y + 13, segW, 15, () -> {
                setter.accept(index);
                changed();
                Sfx.play(Sfx.Kind.TOGGLE);
            });
        }
    }

    private static void volume(RecapView v, int x, int y, int w, JourneyConfig config) {
        GuiGraphics g = v.g;
        ThemePalette pal = v.pal;
        Fx.text(g, v.font, "Sound volume", x + 6, y + 2, pal.subtitle());
        Fx.textR(g, v.font, config.soundVolume + "%", x + w - 6, y + 2, pal.value());

        int bx = x + 6;
        int bw = w - 12;
        int by = y + 15;
        boolean hovered = v.over(bx, by - 3, bw, 14);
        float t = v.hov("vol", hovered);
        Fx.fill(g, bx, by, bx + bw, by + 8, Fx.rgba(0x000000, 0.45F));
        int fill = (int) (bw * config.soundVolume / 100.0F);
        Fx.grad(g, bx, by, bx + fill, by + 8, Fx.mix(pal.accent(), 0xFFFFFFFF, 0.25F), pal.accent());
        Fx.frame(g, bx - 1, by - 1, bw + 2, 10, Fx.mix(pal.border(), pal.accent(), t));
        Fx.fill(g, bx + fill - 1, by - 2, bx + fill + 1, by + 10, pal.title());

        v.hit(bx, by - 3, bw, 14, () -> {
            float fraction = Math.max(0.0F, Math.min(1.0F, (v.mx - bx) / (float) bw));
            config.soundVolume = Math.round(fraction * 20.0F) * 5;
            changed();
            Sfx.refreshMusicVolume();
            Sfx.play(Sfx.Kind.STAT);
        });
    }

    // ================================================================== finale

    static void finale(RecapView v, int x, int y, int w, int h) {
        GuiGraphics g = v.g;
        Font font = v.font;
        ThemePalette pal = v.pal;
        long start = v.finalStart;
        int cx = x + w / 2;

        // Portal rings expanding from the middle, then slowly breathing light.
        for (int k = 0; k < 3; k++) {
            float t = Anim.prog(start, k * 260L, 1500);
            if (t > 0.0F && t < 1.0F) {
                int radius = (int) (Anim.outCubic(t) * Math.max(w, h) * 0.65F);
                int clipped = Math.min(radius, Math.max(w, h));
                Fx.ring(g, cx, y + h / 2 - 10, clipped, 2, Fx.rgba(k == 1 ? pal.accent2() : pal.accent(), (1.0F - t) * 0.55F));
            }
        }
        Fx.grad(g, x + 2, y + 2, x + w - 2, y + h / 2, Fx.rgba(pal.accent(), 0.0F), Fx.rgba(pal.accent2(), 0.10F + 0.05F * Anim.pulse(3500)));

        // Title.
        float titleP = Anim.prog(start, 250, 700);
        float old = Fx.alpha;
        Fx.alpha = old * titleP;
        float rise = (1.0F - Anim.outCubic(titleP)) * 10.0F;
        Fx.textScaled(g, font, "Your Journey, " + RecapModel.year(), cx, (int) (y + 14 + rise), pal.title(), w >= 330 ? 2.0F : 1.5F, true);
        String name = Minecraft.getInstance().getUser().getName();
        String rank = RecapModel.rank();
        Fx.textC(g, font, rank != null ? name + "  -  " + rank : name, cx, (int) (y + 14 + (w >= 330 ? 22 : 18) + rise), pal.accent());
        Fx.alpha = old;

        // Summary tiles with count-up.
        List<RecapModel.Stat> picks = new ArrayList<>();
        for (String id : FINALE_STATS) {
            RecapModel.Stat stat = RecapModel.get(id);
            if (stat != null && JourneyConfig.get().isEnabled(id) && (stat.num() > 0 || stat.fraction() > 0)) {
                picks.add(stat);
            }
        }
        int shown = Math.min(picks.size(), w >= 330 ? 4 : 3);
        if (shown > 0) {
            int gap = 6;
            int tileW = Math.min(100, (w - 30 - gap * (shown - 1)) / shown);
            int tileH = 44;
            int rowW = shown * tileW + (shown - 1) * gap;
            int tx0 = cx - rowW / 2;
            int ty = y + h / 2 - 16;

            for (int i = 0; i < shown; i++) {
                RecapModel.Stat stat = picks.get(i);
                float p = Anim.prog(start, 900 + i * 260L, 600);
                if (p <= 0.0F) {
                    continue;
                }
                if (v.fired.add("fin_" + i)) {
                    Sfx.play(Sfx.Kind.SPARKLE);
                    Fx.Particles.burst(tx0 + i * (tileW + gap) + tileW / 2.0F, ty + tileH / 2.0F, 12, pal.accent(), pal.accent2());
                }
                int tx = tx0 + i * (tileW + gap);
                float o = Pages.begin(g, Pages.xf(p, i + 2), tx, ty, tileW, tileH);
                Fx.glow(g, tx, ty, tileW, tileH, pal.accent(), 0.5F, 2);
                Fx.panel(g, tx, ty, tileW, tileH, pal.tileTop(), pal.tileBottom(), pal.border());
                Fx.fill(g, tx + 3, ty + 1, tx + tileW - 3, ty + 3, pal.accent());
                float count = Anim.outExpo(Anim.prog(start, 1000 + i * 260L, 1200));
                Fx.textFit(g, font, PagesA.animated(stat, count), tx + tileW / 2, ty + 11, pal.value(), 1.4F, tileW - 8);
                Fx.textFit(g, font, stat.label(), tx + tileW / 2, ty + 30, pal.subtitle(), 0.85F, tileW - 8);
                Pages.end(g, o);
            }
        }

        float closing = Anim.prog(start, 900 + Math.max(1, shown) * 260L + 300, 700);
        Fx.alpha = old * closing;
        Fx.textC(g, font, "Thank you for playing. Your journey continues.", cx, y + h - 54, pal.subtitle());
        Fx.alpha = old;

        // Gentle sparkles for the first few seconds.
        if (Anim.on() && Anim.now() - start < 4500 && Anim.now() % 3 == 0) {
            Fx.Particles.fountain(x + 8, y + h - 6, w - 16, 1, pal.accent(), pal.accent2());
        }

        // Buttons (only clickable once visible).
        float buttons = Anim.prog(start, 900 + Math.max(1, shown) * 260L + 500, 500);
        if (buttons > 0.5F) {
            Fx.alpha = old * buttons;
            int by = y + h - 30;
            int bw = w >= 330 ? 96 : 76;
            int total = bw * 3 + 12;
            int bx = cx - total / 2;
            v.button("fin_export", bx, by, bw, 18, "Export image", false, ExportCard::request);
            v.button("fin_replay", bx + bw + 6, by, bw, 18, "Replay", false, v::replay);
            v.button("fin_done", bx + (bw + 6) * 2, by, bw, 18, v.host.standalone() ? "Done" : "Back to credits", true, v.host::done);
            Fx.alpha = old;
        }

        if (buttons > 0.5F) {
            v.button("fin_back", x + 6, y + 6, 40, 14, "< Back", false, v::back);
        }

        String toast = ExportCard.message();
        if (toast != null) {
            Fx.textC(g, font, toast, cx, y + h - 42, pal.accent());
        }
    }
}
