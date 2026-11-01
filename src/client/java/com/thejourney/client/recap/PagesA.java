package com.thejourney.client.recap;

import com.thejourney.client.config.JourneyTheme.ThemePalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongFunction;

/** The content pages: Highlights, Stats, Photos, Achievements and Playtime. Each returns its content height. */
final class PagesA {
    private static final String[][] CATEGORIES = {
            {"exploration", "EXPLORATION"}, {"mining", "MINING"}, {"combat", "COMBAT"},
            {"world", "WORLD"}, {"more", "MORE STATISTICS"}, {"session", "THIS SESSION"}, {"server", "SERVER LEADERS"}
    };
    private static final int[] ACCENTS = {
            0xFF7BD672, 0xFF8FB7D9, 0xFFE0645C, 0xFFE8C24A, 0xFFB9A3D6, 0xFF5ED0E6, 0xFFF08CC0
    };

    private PagesA() {
    }

    // ================================================================== highlights

    static int highlights(RecapView v) {
        if (!RecapModel.loaded()) {
            return Pages.emptyState(v, "Loading your journey...", RecapModel.message());
        }
        List<RecapModel.Highlight> list = RecapModel.highlights();
        if (list.isEmpty()) {
            return Pages.emptyState(v, "No highlights yet",
                    "Highlights appear once there is something real to show: a first diamond, a trip to the Nether, a defeated dragon. Keep playing and come back.");
        }

        GuiGraphics g = v.g;
        ThemePalette pal = v.pal;
        int pad = 6;
        int x = v.vx + pad;
        int w = v.vw - pad * 2 - 4;
        int top = v.vy - (int) v.scroll;
        int y = top + 4;

        Fx.text(g, v.font, "HIGHLIGHTS", x, y, pal.accent2());
        Fx.textR(g, v.font, list.size() + (list.size() == 1 ? " moment" : " moments"), x + w, y, pal.subtitle());
        y += 13;

        boolean narrow = w < 250;
        int cardH = narrow ? 50 : 58;
        int media = narrow ? 48 : 78;

        for (int i = 0; i < list.size(); i++) {
            if (v.visible(y - 4, cardH + 8)) {
                highlightCard(v, list.get(i), i, x, y, w, cardH, media);
            }
            y += cardH + 5;
        }
        return y + 4 - top;
    }

    private static void highlightCard(RecapView v, RecapModel.Highlight hl, int index,
                                      int x, int y, int w, int h, int media) {
        GuiGraphics g = v.g;
        Font font = v.font;
        ThemePalette pal = v.pal;

        long start = v.reveal("h_" + hl.id());
        float p = Anim.prog(start, 0, 520);
        if (p > 0.0F && v.fired.add("hs_" + hl.id())) {
            Sfx.play(hl.tier() >= 2 ? Sfx.Kind.ACHIEVE : Sfx.Kind.STAT);
            if (hl.tier() >= 2) {
                Fx.Particles.burst(x + w / 2.0F, y + h / 2.0F, 22, pal.accent(), pal.accent2());
            }
        }
        if (p <= 0.0F) {
            return;
        }

        boolean hovered = v.over(x, y, w, h);
        float ht = v.hov("h_" + hl.id(), hovered);
        Pages.Xf xf = Pages.xf(p, index);
        float old = Pages.begin(g, xf, x, y, w, h);

        boolean epic = hl.tier() >= 2;
        int border = epic ? Fx.mix(pal.border(), pal.accent2(), 0.4F + 0.6F * Anim.pulse(2200)) : Fx.mix(pal.border(), pal.accent(), ht);
        if (epic || ht > 0.05F) {
            Fx.glow(g, x, y, w, h, epic ? pal.accent2() : pal.accent(), epic ? 0.8F : ht, 3);
        }
        Fx.panel(g, x, y, w, h, Fx.lerp(pal.tileTop(), pal.tileHoverTop(), ht), Fx.lerp(pal.tileBottom(), pal.tileHoverBottom(), ht), border);
        Fx.fill(g, x + 1, y + 2, x + 3, y + h - 2, epic ? pal.accent2() : pal.accent());

        // Media area: its own space, text never goes on top of it.
        int mx = x + 7;
        int my = y + 5;
        int mh = h - 10;
        Fx.fill(g, mx - 1, my - 1, mx + media + 1, my + mh + 1, pal.tileOuter());
        Gallery.Tex tex = hl.photoId() != null ? Gallery.texture(Gallery.photoFile(hl.photoId())) : null;
        if (tex != null) {
            Pages.cover(g, tex, mx, my, media, mh, 0.05F * ht, 1.0F);
        } else {
            Fx.grad(g, mx, my, mx + media, my + mh, pal.slotBg(), pal.slotLow());
            if (mh >= 40 && media >= 40) {
                Pages.icon(g, hl.icon(), mx + media / 2 - 16, my + mh / 2 - 16, 2.0F);
            } else {
                Pages.icon(g, hl.icon(), mx + media / 2 - 8, my + mh / 2 - 8, 1.0F);
            }
        }

        int tx = mx + media + 8;
        int tw = x + w - tx - 6;
        Fx.textScaled(g, font, hl.kicker(), tx, y + 6, epic ? pal.accent2() : pal.accent(), 0.8F, false);
        if (hl.badge() != null && tw > 130) {
            Fx.textScaled(g, font, hl.badge(), x + w - 6 - (int) (font.width(hl.badge()) * 0.8F), y + 6, pal.subtitle(), 0.8F, false);
        }
        Fx.text(g, font, Fx.ellipsize(font, hl.title(), tw), tx, y + 16, pal.title());
        int lines = h >= 56 ? 2 : 1;
        int ty = y + 28;
        for (String line : Fx.wrap(font, hl.text(), tw, lines)) {
            Fx.text(g, font, line, tx, ty, pal.subtitle());
            ty += 10;
        }

        if (epic && Anim.on()) {
            int gx = x + (int) ((Anim.now() / 10 + index * 53) % (w + 40)) - 20;
            int x1 = Math.max(x + 2, gx);
            int x2 = Math.min(x + w - 2, gx + 14);
            if (x2 > x1) {
                Fx.fill(g, x1, y, x2, y + 1, Fx.rgba(0xFFFFFF, 0.8F));
            }
        }
        Pages.end(g, old);
    }

    // ================================================================== stats

    static int stats(RecapView v) {
        if (!RecapModel.loaded()) {
            return Pages.emptyState(v, "Loading your journey...", RecapModel.message());
        }
        GuiGraphics g = v.g;
        int pad = 6;
        int x0 = v.vx + pad;
        int w = v.vw - pad * 2 - 4;
        int top = v.vy - (int) v.scroll;
        int y = top + 4;

        int cols = w >= 330 ? 3 : (w >= 210 ? 2 : 1);
        int gap = 4;
        int cardW = (w - gap * (cols - 1)) / cols;
        int cardH = 46;
        boolean any = false;

        for (int ci = 0; ci < CATEGORIES.length; ci++) {
            String id = CATEGORIES[ci][0];
            List<RecapModel.Stat> list = RecapModel.category(id);
            if (list.isEmpty()) {
                continue;
            }
            any = true;
            int accent = Fx.mix(ACCENTS[ci], v.pal.accent(), 0.3F);

            heading(v, CATEGORIES[ci][1], x0, y, w, accent);
            y += 16;

            if (id.equals("exploration")) {
                long[] modes = RecapModel.travelModes();
                if (modes != null) {
                    y += barChart(v, "chart_travel", "HOW YOU GOT AROUND", x0, y, w,
                            new String[]{"On foot", "Riding", "Elytra", "Swimming"}, modes,
                            new int[]{0xFF7BD672, 0xFFE8C24A, 0xFFB98CFF, 0xFF5ED0E6}, Fmt::distance) + 6;
                }
            }
            if (id.equals("combat")) {
                long dealt = Math.max(0, RecapModel.num("damage_dealt"));
                long taken = Math.max(0, RecapModel.num("damage_taken"));
                if (dealt + taken > 0) {
                    y += barChart(v, "chart_damage", "DAMAGE", x0, y, w,
                            new String[]{"Dealt", "Taken"}, new long[]{dealt, taken},
                            new int[]{0xFFE0645C, 0xFF8FB7D9}, Fmt::number) + 6;
                }
            }

            long maxCm = 0;
            for (RecapModel.Stat stat : list) {
                if (stat.unit().equals("cm")) {
                    maxCm = Math.max(maxCm, stat.num());
                }
            }

            int rows = (list.size() + cols - 1) / cols;
            for (int i = 0; i < list.size(); i++) {
                int cx = x0 + (i % cols) * (cardW + gap);
                int cy = y + (i / cols) * (cardH + gap);
                if (v.visible(cy - 4, cardH + 8)) {
                    statCard(v, list.get(i), cx, cy, cardW, cardH, accent, maxCm, i);
                }
            }
            y += rows * (cardH + gap) + 6;
        }

        if (!any) {
            return Pages.emptyState(v, "No statistics to show",
                    "Every statistic is either switched off in the settings or still empty.");
        }
        return y + 4 - top;
    }

    private static void heading(RecapView v, String label, int x, int y, int w, int accent) {
        GuiGraphics g = v.g;
        int cx = x + w / 2;
        int lw = v.font.width(label);
        int lineY = y + 4;
        int lineColor = Fx.rgba(accent, 0.6F);
        if (cx - lw / 2 - 8 > x + 8) {
            Fx.grad(g, x, lineY, cx - lw / 2 - 8, lineY + 1, Fx.rgba(accent, 0.0F), lineColor);
        }
        if (x + w > cx + lw / 2 + 8 + 8) {
            Fx.grad(g, cx + lw / 2 + 8, lineY, x + w, lineY + 1, lineColor, Fx.rgba(accent, 0.0F));
        }
        Fx.textC(g, v.font, label, cx, y, accent);
    }

    /** Horizontal bars that grow into place. Returns the height used. */
    private static int barChart(RecapView v, String key, String title, int x, int y, int w,
                                String[] labels, long[] values, int[] colors, LongFunction<String> format) {
        int rowH = 12;
        int height = 13 + labels.length * rowH + 2;
        if (!v.visible(y - 4, height + 8)) {
            return height;
        }
        GuiGraphics g = v.g;
        ThemePalette pal = v.pal;
        long start = v.reveal(key);
        float p = Anim.prog(start, 0, 400);
        if (p <= 0.0F) {
            return height;
        }

        float old = Pages.begin(g, Pages.xf(p, 0), x, y, w, height);
        Fx.panel(g, x, y, w, height, pal.tileTop(), pal.tileBottom(), pal.border());
        Fx.textScaled(g, v.font, title, x + 6, y + 3, pal.subtitle(), 0.8F, false);

        long max = 1;
        for (long value : values) {
            max = Math.max(max, value);
        }
        int labelW = 52;
        int valueW = 54;
        int trackX = x + 6 + labelW;
        int trackW = w - 12 - labelW - valueW;

        for (int i = 0; i < labels.length; i++) {
            int ry = y + 13 + i * rowH;
            Fx.text(g, v.font, labels[i], x + 6, ry + 1, pal.title());
            Fx.fill(g, trackX, ry + 2, trackX + trackW, ry + 8, Fx.rgba(0x000000, 0.35F));
            float grow = Anim.outCubic(Anim.prog(start, 150 + i * 120L, 900));
            int fill = (int) (trackW * (values[i] / (float) max) * grow);
            if (fill > 0) {
                Fx.grad(g, trackX, ry + 2, trackX + fill, ry + 8, Fx.mix(colors[i], 0xFFFFFFFF, 0.25F), colors[i]);
                Fx.fill(g, trackX + fill - 1, ry + 2, trackX + fill, ry + 8, Fx.rgba(0xFFFFFF, 0.6F));
            }
            Fx.textR(g, v.font, format.apply((long) (values[i] * grow)), x + w - 6, ry + 1, pal.value());
        }
        Pages.end(g, old);
        return height;
    }

    static String animated(RecapModel.Stat stat, float p) {
        if (p >= 1.0F || stat.raw() == null) {
            return stat.value();
        }
        return switch (stat.unit()) {
            case "n" -> Fmt.number((long) (stat.num() * p));
            case "cm" -> Fmt.distance((long) (stat.num() * p));
            case "t" -> Fmt.duration((long) (stat.num() * p));
            case "f" -> ((long) (stat.num() * p)) + " / " + stat.total();
            default -> stat.value();
        };
    }

    private static void statCard(RecapView v, RecapModel.Stat stat, int x, int y, int w, int h,
                                 int accent, long maxCm, int index) {
        GuiGraphics g = v.g;
        Font font = v.font;
        ThemePalette pal = v.pal;

        String key = "s_" + stat.id();
        long start = v.reveal(key);
        float p = Anim.prog(start, 0, 520);
        boolean special = RecapModel.impressive(stat);

        if (p > 0.0F && v.fired.add("ss_" + stat.id())) {
            if (special) {
                Sfx.play(Sfx.Kind.ACHIEVE);
                Fx.Particles.burst(x + w / 2.0F, y + h / 2.0F, 20, 0xFFFFD56A, pal.accent());
            } else {
                Sfx.play(Sfx.Kind.STAT);
            }
        }
        if (p <= 0.0F) {
            return;
        }

        boolean hovered = v.over(x, y, w, h);
        float ht = v.hov(key, hovered);
        float old = Pages.begin(g, Pages.xf(p, stat.id().hashCode()), x, y, w, h);

        int gold = 0xFFFFD56A;
        int border = special ? Fx.mix(pal.border(), gold, 0.45F + 0.55F * Anim.pulse(1800)) : Fx.mix(pal.border(), accent, ht * 0.8F);
        if (special) {
            Fx.glow(g, x, y, w, h, gold, 0.7F, 3);
        } else if (ht > 0.05F) {
            Fx.glow(g, x, y, w, h, accent, ht * 0.7F, 2);
        }
        Fx.panel(g, x, y, w, h, Fx.lerp(pal.tileTop(), pal.tileHoverTop(), ht), Fx.lerp(pal.tileBottom(), pal.tileHoverBottom(), ht), border);
        Fx.fill(g, x + 3, y + 1, x + w - 3, y + 3, special ? gold : accent);

        // Icon slot.
        int sx = x + 5;
        int sy = y + 7;
        Fx.fill(g, sx, sy, sx + 20, sy + 20, pal.tileOuter());
        Fx.fill(g, sx + 1, sy + 1, sx + 19, sy + 19, pal.slotBg());
        Fx.fill(g, sx + 1, sy + 18, sx + 19, sy + 19, pal.slotLow());
        ItemStack icon = StatIcons.iconFor(stat);
        Pages.icon(g, icon, sx + 2, sy + 2);

        // Label (up to two lines) to the right of the icon.
        int lx = sx + 24;
        List<String> label = Fx.wrap(font, stat.label(), x + w - lx - 4, 2);
        for (int i = 0; i < label.size(); i++) {
            Fx.text(g, font, label.get(i), lx, y + 8 + i * 9, pal.title());
        }

        // Value with a count-up.
        float count = Anim.outExpo(Anim.prog(start, 100, 900));
        String value = animated(stat, count);
        Fx.textFit(g, font, value, x + w / 2, y + 29, special ? gold : pal.value(), 1.2F, w - 12);

        // Bar for stats that have a meaningful scale (distance relative to the longest, or done/total).
        float fraction = -1;
        if (stat.unit().equals("cm") && maxCm > 0) {
            fraction = stat.num() / (float) maxCm;
        } else if (stat.unit().equals("f")) {
            fraction = (float) stat.fraction();
        }
        if (fraction >= 0) {
            float grow = Anim.outCubic(Anim.prog(start, 250, 900));
            int bx = x + 6;
            int bw = w - 12;
            Fx.fill(g, bx, y + h - 5, bx + bw, y + h - 3, Fx.rgba(0x000000, 0.4F));
            Fx.fill(g, bx, y + h - 5, bx + (int) (bw * Math.min(1.0F, fraction) * grow), y + h - 3, special ? gold : accent);
        }
        Pages.end(g, old);
    }

    // ================================================================== photos

    private static void switchPhoto(RecapView v, int to, int size) {
        v.photoPrevIndex = v.photoIndex;
        v.photoIndex = Math.floorMod(to, size);
        v.photoSwitchedAt = Anim.now();
        v.photoSince = Anim.now();
        Sfx.play(Sfx.Kind.WHOOSH);
    }

    static int photos(RecapView v) {
        List<Gallery.Item> items = Gallery.items();
        if (items.isEmpty()) {
            return Pages.emptyState(v, "No photos in this journey",
                    "Photos are taken at big moments (a first diamond, entering the Nether, ...) while \"Photos at big moments\" is switched on. None are saved for this world (they may have been deleted, or \"Keep photos after the credits\" was off).");
        }

        GuiGraphics g = v.g;
        Font font = v.font;
        ThemePalette pal = v.pal;
        v.photoIndex = Math.floorMod(v.photoIndex, items.size());
        Gallery.Item item = items.get(v.photoIndex);

        int top = v.vy - (int) v.scroll;
        int pad = 8;
        int arrowW = 14;
        int captionH = 28;
        int dotsH = 12;
        int availW = v.vw - pad * 2;
        int availH = Math.max(60, v.vh - captionH - dotsH - 16);
        boolean arrowsOutside = availW >= 260;
        int maxW = availW - (arrowsOutside ? arrowW * 2 + 12 : 0);

        int imgW = Math.min(maxW, availH * 16 / 9);
        int imgH = imgW * 9 / 16;
        int imgX = v.vx + (v.vw - imgW) / 2;
        int imgY = top + 6;

        long start = v.reveal("photos");
        float enter = Anim.prog(start, 0, 600);
        float old = Pages.begin(g, Pages.xf(enter, 2), imgX, imgY, imgW, imgH);

        Fx.glow(g, imgX, imgY, imgW, imgH, pal.accent(), 0.6F, 3);
        Fx.fill(g, imgX - 1, imgY - 1, imgX + imgW + 1, imgY + imgH + 1, pal.border());
        Fx.fill(g, imgX, imgY, imgX + imgW, imgY + imgH, 0xFF000000);

        boolean hoveringImage = v.over(imgX, imgY, imgW, imgH);
        if (Anim.on() && items.size() > 1 && !hoveringImage
                && Anim.now() - v.photoSince > Anim.scaled(6500)) {
            switchPhoto(v, v.photoIndex + 1, items.size());
            item = items.get(v.photoIndex);
        }

        float fade = v.photoSwitchedAt < 0 ? 1.0F : Anim.prog(v.photoSwitchedAt, 0, 600);
        float zoom = Anim.on() ? 0.06F * Anim.clamp((Anim.now() - v.photoSince) / 9000.0F) : 0.0F;

        g.enableScissor(imgX, imgY, imgX + imgW, imgY + imgH);
        if (fade < 1.0F && v.photoPrevIndex >= 0 && v.photoPrevIndex < items.size()) {
            drawItem(g, items.get(v.photoPrevIndex), imgX, imgY, imgW, imgH, 1.0F, 0.06F);
        }
        drawItem(g, item, imgX, imgY, imgW, imgH, fade, zoom);
        g.disableScissor();

        if (item.clip()) {
            Fx.fill(g, imgX + 4, imgY + 4, imgX + 44, imgY + 15, Fx.rgba(0x000000, 0.65F));
            Fx.pixels(g, imgX + 7, imgY + 6, Fx.PLAY, pal.accent(), false);
            Fx.text(g, font, "CLIP", imgX + 15, imgY + 6, pal.title());
        }

        // Arrows.
        int ay = imgY + imgH / 2 - 13;
        int leftX = arrowsOutside ? imgX - arrowW - 5 : imgX + 3;
        int rightX = arrowsOutside ? imgX + imgW + 5 : imgX + imgW - arrowW - 3;
        if (items.size() > 1) {
            arrow(v, "pl", leftX, ay, arrowW, true, () -> switchPhoto(v, v.photoIndex - 1, items.size()));
            arrow(v, "pr", rightX, ay, arrowW, false, () -> switchPhoto(v, v.photoIndex + 1, items.size()));
        }
        Pages.end(g, old);

        // Caption in its own space under the picture.
        int capY = imgY + imgH + 6;
        Fx.panel(g, imgX, capY, imgW, captionH, pal.tileTop(), pal.tileBottom(), pal.border());
        Fx.textScaled(g, font, item.tag().toUpperCase(java.util.Locale.ROOT), imgX + 8, capY + 5, pal.accent(), 0.8F, false);
        Fx.text(g, font, Fx.ellipsize(font, item.caption(), imgW - 70), imgX + 8, capY + 15, pal.title());
        Fx.textR(g, font, (v.photoIndex + 1) + " / " + items.size(), imgX + imgW - 8, capY + 10, pal.subtitle());

        // Dots.
        int dotsY = capY + captionH + 5;
        if (items.size() <= 18) {
            int totalW = items.size() * 8 - 2;
            int dx = v.vx + (v.vw - totalW) / 2;
            for (int i = 0; i < items.size(); i++) {
                int index = i;
                boolean current = i == v.photoIndex;
                Fx.fill(g, dx + i * 8, dotsY, dx + i * 8 + 6, dotsY + 4, current ? pal.accent() : Fx.rgba(pal.border(), 0.8F));
                v.hit(dx + i * 8 - 1, dotsY - 3, 8, 10, () -> {
                    if (index != v.photoIndex) {
                        switchPhoto(v, index, items.size());
                    }
                });
            }
        }
        return dotsY + 10 - top;
    }

    private static void arrow(RecapView v, String id, int x, int y, int w, boolean left, Runnable action) {
        boolean hovered = v.over(x, y, w, 26);
        float t = v.hov("arrow_" + id, hovered);
        Fx.panel(v.g, x, y, w, 26, Fx.rgba(0x000000, 0.45F + 0.25F * t), Fx.rgba(0x000000, 0.45F + 0.25F * t),
                Fx.mix(v.pal.border(), v.pal.accent(), t));
        Fx.pixels(v.g, x + w / 2 - 1, y + 10, Fx.CHEVRON, Fx.mix(v.pal.subtitle(), v.pal.title(), t), left);
        v.hit(x, y, w, 26, () -> {
            Sfx.play(Sfx.Kind.CLICK);
            action.run();
        });
    }

    private static void drawItem(GuiGraphics g, Gallery.Item item, int x, int y, int w, int h, float alpha, float zoom) {
        java.nio.file.Path path = item.path();
        if (item.clip()) {
            int frame = (int) ((Anim.now() / 250) % item.frames().size());
            path = item.frames().get(frame);
            // Until a frame is loaded, keep showing the first one.
            if (Gallery.texture(path) == null) {
                path = item.frames().get(0);
            }
        }
        Gallery.Tex tex = Gallery.texture(path);
        if (tex == null) {
            return;
        }
        Pages.cover(g, tex, x, y, w, h, item.clip() ? 0.0F : zoom, alpha);
    }

    // ================================================================== achievements

    static int achievements(RecapView v) {
        if (!RecapModel.loaded()) {
            return Pages.emptyState(v, "Loading your journey...", RecapModel.message());
        }
        List<RecapModel.Stat> list = RecapModel.category("achievements");
        RecapModel.Stat advancements = RecapModel.get("advancements");
        if (list.isEmpty() && advancements == null) {
            return Pages.emptyState(v, "No achievements found", "The server did not send any advancement data.");
        }

        GuiGraphics g = v.g;
        Font font = v.font;
        ThemePalette pal = v.pal;
        int pad = 6;
        int x = v.vx + pad;
        int w = v.vw - pad * 2 - 4;
        int top = v.vy - (int) v.scroll;
        int y = top + 4;

        if (advancements != null && advancements.fraction() >= 0) {
            long start = v.reveal("adv");
            float p = Anim.prog(start, 0, 450);
            if (p > 0.0F) {
                float old = Pages.begin(g, Pages.xf(p, 0), x, y, w, 36);
                Fx.panel(g, x, y, w, 36, pal.tileTop(), pal.tileBottom(), pal.border());
                Fx.text(g, font, "Advancements", x + 8, y + 6, pal.title());
                float count = Anim.outExpo(Anim.prog(start, 100, 1100));
                Fx.textR(g, font, animated(advancements, count), x + w - 8, y + 6, pal.value());

                int bx = x + 8;
                int bw = w - 16;
                float grow = Anim.outCubic(Anim.prog(start, 200, 1300));
                int fill = (int) (bw * (float) advancements.fraction() * grow);
                Fx.fill(g, bx, y + 20, bx + bw, y + 29, Fx.rgba(0x000000, 0.4F));
                if (fill > 0) {
                    Fx.grad(g, bx, y + 20, bx + fill, y + 29, Fx.mix(pal.accent(), 0xFFFFFFFF, 0.3F), pal.accent());
                    float shine = 0.5F + 0.5F * Anim.pulse(1200);
                    Fx.fill(g, bx + fill - 2, y + 20, bx + fill, y + 29, Fx.rgba(0xFFFFFF, 0.4F + 0.4F * shine));
                }
                Fx.textScaled(g, font, Fmt.percent(advancements.fraction() * grow), bx + bw / 2, y + 22, pal.title(), 0.8F, true);
                if (advancements.fraction() >= 1.0 && grow >= 1.0F && v.fired.add("adv_full")) {
                    Fx.Particles.burst(x + w / 2.0F, y + 24, 26, 0xFFFFD56A, pal.accent());
                    Sfx.play(Sfx.Kind.ACHIEVE);
                }
                Pages.end(g, old);
            }
            y += 42;
        }

        if (!list.isEmpty()) {
            int done = 0;
            for (RecapModel.Stat stat : list) {
                if (stat.unit().equals("d")) {
                    done++;
                }
            }
            heading(v, "ACHIEVEMENTS  " + done + " / " + list.size(), x, y, w, pal.accent2());
            y += 16;

            int cols = w >= 300 ? 2 : 1;
            int gap = 4;
            int cw = (w - gap * (cols - 1)) / cols;
            int rh = 28;

            int i = 0;
            while (i < list.size()) {
                String group = groupOf(list.get(i).id());
                int end = i;
                int groupDone = 0;
                while (end < list.size() && groupOf(list.get(end).id()).equals(group)) {
                    if (list.get(end).unit().equals("d")) {
                        groupDone++;
                    }
                    end++;
                }

                Fx.textScaled(g, font, group.toUpperCase(java.util.Locale.ROOT) + "  " + groupDone + " / " + (end - i),
                        x + 2, y, pal.accent(), 0.85F, false);
                y += 12;

                for (int k = i; k < end; k++) {
                    int cx = x + ((k - i) % cols) * (cw + gap);
                    int cy = y + ((k - i) / cols) * (rh + gap);
                    if (v.visible(cy - 4, rh + 8)) {
                        achievementRow(v, list.get(k), cx, cy, cw, rh, k);
                    }
                }
                y += (((end - i) + cols - 1) / cols) * (rh + gap) + 6;
                i = end;
            }
        }
        return y + 4 - top;
    }

    /** Tab or mod an advancement belongs to, from its id ("a_minecraft:story/mine_stone"). */
    private static String groupOf(String id) {
        String full = id.startsWith("a_") ? id.substring(2) : id;
        int colon = full.indexOf(':');
        String namespace = colon > 0 ? full.substring(0, colon) : "minecraft";
        String path = colon > 0 ? full.substring(colon + 1) : full;

        if (namespace.equals("minecraft")) {
            String tab = path.contains("/") ? path.substring(0, path.indexOf('/')) : path;
            return switch (tab) {
                case "story" -> "Minecraft";
                case "nether" -> "Nether";
                case "end" -> "The End";
                case "adventure" -> "Adventure";
                case "husbandry" -> "Husbandry";
                default -> pretty(tab);
            };
        }
        return pretty(namespace);
    }

    private static String pretty(String text) {
        StringBuilder out = new StringBuilder();
        for (String word : text.split("[_\\-]")) {
            if (!word.isEmpty()) {
                out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
            }
        }
        return out.toString().trim();
    }

    private static void achievementRow(RecapView v, RecapModel.Stat stat, int x, int y, int w, int h, int index) {
        GuiGraphics g = v.g;
        Font font = v.font;
        ThemePalette pal = v.pal;
        boolean done = stat.unit().equals("d");

        String key = "a_" + stat.id();
        long start = v.reveal(key);
        float p = Anim.prog(start, 0, 420);
        if (p > 0.0F && v.fired.add("as_" + stat.id()) && done) {
            Sfx.play(Sfx.Kind.SPARKLE);
        }
        if (p <= 0.0F) {
            return;
        }

        boolean hovered = v.over(x, y, w, h);
        float ht = v.hov(key, hovered);
        float old = Pages.begin(g, Pages.xf(p, index + 1), x, y, w, h);
        int border = done ? Fx.mix(pal.border(), pal.accent(), 0.4F + 0.4F * ht) : Fx.rgba(pal.border(), 0.5F);
        Fx.panel(g, x, y, w, h, Fx.lerp(pal.tileTop(), pal.tileHoverTop(), ht), Fx.lerp(pal.tileBottom(), pal.tileHoverBottom(), ht), border);
        if (done) {
            Fx.fill(g, x + 1, y + 2, x + 3, y + h - 2, pal.accent());
        }

        int sx = x + 7;
        int sy = y + 5;
        Fx.fill(g, sx, sy, sx + 18, sy + 18, pal.tileOuter());
        Fx.fill(g, sx + 1, sy + 1, sx + 17, sy + 17, pal.slotBg());
        Pages.icon(g, stat.icon(), sx + 1, sy + 1, 1.0F);
        if (!done) {
            Fx.fill(g, sx + 1, sy + 1, sx + 17, sy + 17, Fx.rgba(pal.bgBottom() & 0xFFFFFF, 0.72F));
            Fx.pixels(g, sx + 6, sy + 6, Fx.LOCK, pal.subtitle(), false);
        }

        int tx = sx + 24;
        int tw = x + w - tx - 5;
        Fx.text(g, font, Fx.ellipsize(font, stat.label(), tw), tx, y + 5, done ? pal.title() : pal.subtitle());
        String sub = done ? (stat.num() > 0 ? Fmt.date(stat.num()) : "Completed") : "Locked";
        Fx.textScaled(g, font, sub, tx, y + 16, done ? pal.accent() : Fx.rgba(pal.subtitle(), 0.7F), 0.85F, false);
        Pages.end(g, old);
    }

    // ================================================================== playtime

    static int playtime(RecapView v) {
        if (!RecapModel.loaded()) {
            return Pages.emptyState(v, "Loading your journey...", RecapModel.message());
        }
        RecapModel.Stat total = RecapModel.get("play_time");
        if (total == null) {
            return Pages.emptyState(v, "No play time data", "The server did not send any play time.");
        }

        GuiGraphics g = v.g;
        Font font = v.font;
        ThemePalette pal = v.pal;
        int pad = 6;
        int x = v.vx + pad;
        int w = v.vw - pad * 2 - 4;
        int top = v.vy - (int) v.scroll;
        int y = top + 4;

        // Big number.
        long start = v.reveal("pt_big");
        float p = Anim.prog(start, 0, 500);
        int bigH = 62;
        if (p > 0.0F) {
            float old = Pages.begin(g, Pages.xf(p, 0), x, y, w, bigH);
            Fx.glow(g, x, y, w, bigH, pal.accent(), 0.4F + 0.3F * Anim.pulse(3000), 3);
            Fx.panel(g, x, y, w, bigH, pal.headerTop(), pal.headerBottom(), pal.border());
            Fx.textC(g, font, "TOTAL PLAY TIME", x + w / 2, y + 6, pal.accent());
            float count = Anim.outExpo(Anim.prog(start, 150, 1400));
            Fx.textFit(g, font, animated(total, count), x + w / 2, y + 19, pal.title(), 2.2F, w - 20);

            RecapModel.Stat session = RecapModel.get("s_play_time");
            if (session != null && total.num() > 0 && session.num() >= 0) {
                float fraction = Math.min(1.0F, session.num() / (float) total.num());
                int bx = x + 12;
                int bw = w - 24;
                float grow = Anim.outCubic(Anim.prog(start, 400, 1100));
                Fx.fill(g, bx, y + 45, bx + bw, y + 49, Fx.rgba(0x000000, 0.4F));
                Fx.fill(g, bx, y + 45, bx + Math.max(1, (int) (bw * fraction * grow)), y + 49, pal.accent());
                Fx.textScaled(g, font, "This session: " + session.value() + "  (" + Fmt.percent(fraction) + " of your total)",
                        x + w / 2, y + 51, pal.subtitle(), 0.8F, true);
            }
            Pages.end(g, old);
        }
        y += bigH + 8;

        // Facts. Every line only appears when the data really exists.
        List<String[]> rows = new ArrayList<>();
        addRow(rows, "Days passed", RecapModel.get("days"));
        addRow(rows, "Nights slept", RecapModel.get("sleeps"));
        addRow(rows, "Alive for (since last death)", RecapModel.get("alive_for"));
        long longest = SessionLog.longestMs();
        if (longest > 0) {
            rows.add(new String[]{"Longest session measured", Fmt.durationMs(longest)});
        }
        long current = SessionLog.currentMs();
        if (current > 0) {
            rows.add(new String[]{"This session so far", Fmt.durationMs(current)});
        }
        Minecraft client = Minecraft.getInstance();
        rows.add(new String[]{"Player", client.getUser().getName()});
        String world = worldName(client);
        if (world != null) {
            rows.add(new String[]{"World", world});
        }

        Fx.textScaled(g, font, "THE DETAILS", x, y, pal.accent2(), 0.9F, false);
        y += 12;
        for (int i = 0; i < rows.size(); i++) {
            int ry = y + i * 15;
            if (!v.visible(ry - 2, 16)) {
                continue;
            }
            long rs = v.reveal("pt_row" + i);
            float rp = Anim.prog(rs, 0, 380);
            if (rp <= 0.0F) {
                continue;
            }
            float old = Pages.begin(g, Pages.xf(rp, 1), x, ry, w, 14);
            if (i % 2 == 0) {
                Fx.fill(g, x, ry, x + w, ry + 14, Fx.rgba(pal.tileTop(), 0.55F));
            }
            Fx.text(g, font, rows.get(i)[0], x + 6, ry + 3, pal.subtitle());
            Fx.textR(g, font, Fx.ellipsize(font, rows.get(i)[1], w / 2), x + w - 6, ry + 3, pal.title());
            Pages.end(g, old);
        }
        y += rows.size() * 15 + 4;
        return y + 4 - top;
    }

    private static void addRow(List<String[]> rows, String label, RecapModel.Stat stat) {
        if (stat != null) {
            rows.add(new String[]{label, stat.value()});
        }
    }

    private static String worldName(Minecraft client) {
        if (client.getSingleplayerServer() != null) {
            return client.getSingleplayerServer().getWorldData().getLevelName();
        }
        if (client.getCurrentServer() != null) {
            return client.getCurrentServer().name;
        }
        return null;
    }
}
