package com.thejourney.client.recap;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.thejourney.client.config.JourneyConfig;
import com.thejourney.client.config.JourneyTheme.ThemePalette;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * "Export as image": draws a clean, static poster of the recap (no buttons, no settings) in
 * the middle of the screen for a single frame, crops exactly that area from the framebuffer
 * and saves it as a PNG in the screenshots folder.
 */
public final class ExportCard {
    private static final ResourceLocation LOGO =
            ResourceLocation.fromNamespaceAndPath("the-journey", "textures/gui/journey_credits_logo.png");

    /** The stats offered for the poster, best first. Only valid, enabled and non-empty ones are used. */
    private static final String[] PRIORITY = {
            "play_time", "distance", "blocks_mined", "mobs_killed", "diamonds_mined",
            "ender_dragon_kills", "advancements", "items_crafted", "days", "deaths"
    };

    private static int pendingFrames = 0;
    private static volatile String message = null;
    private static volatile long messageUntil = 0;

    private ExportCard() {
    }

    public static void request() {
        if (!RecapModel.loaded()) {
            show("Nothing to export yet.");
            return;
        }
        // Two frames: the first one lets a photo finish loading.
        pendingFrames = 2;
    }

    public static String message() {
        return message != null && System.currentTimeMillis() < messageUntil ? message : null;
    }

    private static void show(String text) {
        message = text;
        messageUntil = System.currentTimeMillis() + 5000;
    }

    static void captureIfRequested(RecapView view) {
        if (pendingFrames <= 0) {
            return;
        }
        pendingFrames--;

        Minecraft client = Minecraft.getInstance();
        int sw = client.getWindow().getGuiScaledWidth();
        int sh = client.getWindow().getGuiScaledHeight();
        int cw = Math.min(sw - 8, 440);
        int ch = Math.min(sh - 8, 270);
        int cx = (sw - cw) / 2;
        int cy = (sh - ch) / 2;

        List<Gallery.Item> items = Gallery.items();
        Gallery.Tex photo = null;
        for (Gallery.Item item : items) {
            if (!item.clip()) {
                photo = Gallery.texture(item.path());
                break;
            }
        }

        if (pendingFrames > 0) {
            return; // Preload frame.
        }

        float old = Fx.alpha;
        Fx.alpha = 1.0F;
        view.g.fill(0, 0, sw, sh, 0xFF000000);
        draw(view.g, view.font, view.pal, cx, cy, cw, ch, photo);
        Fx.alpha = old;
        view.g.flush();

        save(cx, cy, cw, ch);
        view.flashAt = Anim.now();
        Sfx.play(Sfx.Kind.EXPORT);
    }

    // ------------------------------------------------------------------ drawing

    private static void draw(GuiGraphics g, Font font, ThemePalette pal, int x, int y, int w, int h, Gallery.Tex photo) {
        Fx.fill(g, x, y, x + w, y + h, 0xFF000000);
        Fx.grad(g, x, y, x + w, y + h, Fx.withAlpha(pal.bgTop(), 255), Fx.withAlpha(pal.bgBottom(), 255));

        // A deterministic field of stars, so the poster looks the same every time.
        long seed = 1234567;
        for (int i = 0; i < 70; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int sx = x + 4 + (int) ((seed >>> 33) % Math.max(1, w - 8));
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int sy = y + 4 + (int) ((seed >>> 33) % Math.max(1, h - 8));
            float b = 0.15F + ((seed >>> 20) & 7) / 20.0F;
            Fx.fill(g, sx, sy, sx + 1, sy + 1, Fx.rgba((i % 3 == 0) ? pal.accent2() : pal.accent(), b));
        }

        Fx.frame(g, x, y, w, h, pal.border());
        Fx.frame(g, x + 2, y + 2, w - 4, h - 4, Fx.rgba(pal.accent(), 0.45F));

        // ---- title ----
        String player = Minecraft.getInstance().getUser().getName();
        String rank = RecapModel.rank();
        Fx.textScaled(g, font, "Your Journey, " + RecapModel.year(), x + w / 2, y + 9, pal.title(), w >= 330 ? 1.7F : 1.3F, true);
        Fx.textC(g, font, rank != null ? player + "  -  " + rank : player, x + w / 2, y + 9 + (w >= 330 ? 20 : 16), pal.accent());
        int lineY = y + (w >= 330 ? 41 : 36);
        Fx.grad(g, x + 20, lineY, x + w - 20, lineY + 1, Fx.rgba(pal.accent(), 0.0F), Fx.rgba(pal.accent(), 0.9F));

        int bodyTop = lineY + 6;
        int bodyBottom = y + h - 24;
        int bodyH = bodyBottom - bodyTop;
        int pad = 10;

        // ---- screenshot (own space, never behind statistics) ----
        int leftX = x + pad;
        int rightX = x + pad;
        int rightW = w - pad * 2;
        if (photo != null && w >= 300) {
            int pw = (int) (w * 0.38F);
            int ph = Math.min(bodyH, (int) (pw * 0.75F));
            Pages.cover(g, photo, leftX, bodyTop, pw, ph, 0.0F, 1.0F);
            Fx.frame(g, leftX - 1, bodyTop - 1, pw + 2, ph + 2, pal.border());
            rightX = leftX + pw + 12;
            rightW = x + w - pad - rightX;
        }

        // ---- highlights ----
        List<RecapModel.Highlight> highlights = RecapModel.highlights();
        int shown = Math.min(highlights.size(), bodyH >= 120 ? 3 : 2);
        int hy = bodyTop;
        if (shown > 0) {
            Fx.text(g, font, "HIGHLIGHTS", rightX, hy, pal.accent2());
            hy += 11;
            for (int i = 0; i < shown; i++) {
                RecapModel.Highlight highlight = highlights.get(i);
                Fx.fill(g, rightX, hy, rightX + rightW, hy + 19, Fx.rgba(pal.tileTop(), 0.85F));
                Fx.fill(g, rightX, hy, rightX + 2, hy + 19, pal.accent());
                Pages.icon(g, highlight.icon(), rightX + 5, hy + 1, 1.0F);
                Fx.text(g, font, Fx.ellipsize(font, highlight.kicker(), rightW - 28), rightX + 25, hy + 2, pal.accent());
                Fx.text(g, font, Fx.ellipsize(font, highlight.title(), rightW - 28), rightX + 25, hy + 10, pal.title());
                hy += 22;
            }
            hy += 2;
        }

        // ---- statistics ----
        List<RecapModel.Stat> picks = new ArrayList<>();
        JourneyConfig config = JourneyConfig.get();
        for (String id : PRIORITY) {
            RecapModel.Stat stat = RecapModel.get(id);
            if (stat != null && config.isEnabled(id) && (stat.num() != 0 || stat.unit().equals("f"))) {
                picks.add(stat);
            }
        }
        int maxStats = bodyBottom - hy >= 70 ? 6 : 4;
        int cols = 3;
        int cellW = (rightW - (cols - 1) * 4) / cols;
        int rows = (Math.min(maxStats, picks.size()) + cols - 1) / cols;
        int statsTop = Math.max(hy, bodyBottom - rows * 28 - 2);
        for (int i = 0; i < Math.min(maxStats, picks.size()); i++) {
            RecapModel.Stat stat = picks.get(i);
            int cellX = rightX + (i % cols) * (cellW + 4);
            int cellY = statsTop + (i / cols) * 28;
            Fx.panel(g, cellX, cellY, cellW, 25, pal.tileTop(), pal.tileBottom(), pal.border());
            Fx.fill(g, cellX + 2, cellY + 1, cellX + cellW - 2, cellY + 2, pal.accent());
            Fx.textFit(g, font, stat.value(), cellX + cellW / 2, cellY + 6, pal.value(), 1.2F, cellW - 6);
            Fx.textFit(g, font, stat.label(), cellX + cellW / 2, cellY + 16, pal.subtitle(), 0.8F, cellW - 6);
        }

        // ---- footer ----
        int logoW = 72;
        RenderSystem.enableBlend();
        g.blit(LOGO, x + pad, y + h - 21, logoW, logoW / 4, 0.0F, 0.0F, 1024, 256, 1024, 256);
        String date = Fmt.date(System.currentTimeMillis());
        Fx.textR(g, font, date, x + w - pad, y + h - 15, pal.subtitle());
    }

    // ------------------------------------------------------------------ saving

    private static void save(int guiX, int guiY, int guiWidth, int guiHeight) {
        Minecraft client = Minecraft.getInstance();

        try {
            NativeImage full = Screenshot.takeScreenshot(client.getMainRenderTarget());
            double scale = client.getWindow().getGuiScale();

            int x = Math.max(0, (int) Math.floor(guiX * scale));
            int y = Math.max(0, (int) Math.floor(guiY * scale));
            int width = Math.min(full.getWidth() - x, (int) Math.ceil(guiWidth * scale));
            int height = Math.min(full.getHeight() - y, (int) Math.ceil(guiHeight * scale));

            if (width <= 0 || height <= 0) {
                full.close();
                show("Could not save the image.");
                return;
            }

            NativeImage card = new NativeImage(width, height, false);
            full.resizeSubRectTo(x, y, width, height, card);
            full.close();

            String name = "the-journey-recap-"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss")) + ".png";
            Path target = FabricLoader.getInstance().getGameDir().resolve("screenshots").resolve(name);

            Thread writer = new Thread(() -> {
                try (card) {
                    Files.createDirectories(target.getParent());
                    card.writeToFile(target);
                    show("Saved to screenshots/" + name);
                } catch (Exception exception) {
                    show("Could not save the image.");
                }
            }, "the-journey-card");
            writer.setDaemon(true);
            writer.start();
        } catch (Throwable throwable) {
            show("Could not save the image.");
        }
    }
}
