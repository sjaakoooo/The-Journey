package com.thejourney.client.shots;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.thejourney.client.config.JourneyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Slideshow of the collected pictures as End Credits background: slow zoom,
 * cross-fade, and a caption on milestone photos.
 * Keeps at most two pictures in memory at a time.
 */
public final class ShotSlideshow {
    private static final long SLIDE_MS = 8000;
    private static final long FADE_MS = 2000;
    private static final float ZOOM = 0.10F;

    private static boolean initialised = false;
    private static List<EndShots.Entry> order = new ArrayList<>();
    private static int index = 0;
    private static long slideStart = 0;
    private static Slide current = null;
    private static Slide next = null;
    private static int textureCounter = 0;

    private record Slide(ResourceLocation id, int width, int height, String caption) {
    }

    private ShotSlideshow() {
    }

    /**
     * Draws the background. Returns false when there are no pictures, so the
     * normal Minecraft background can be used instead.
     */
    public static boolean render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (!initialised) {
            start();
        }

        if (current == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - slideStart;
        boolean multiple = order.size() > 1;

        // Prepare the next picture just before the cross-fade begins.
        if (multiple && next == null && elapsed >= SLIDE_MS - FADE_MS - 500) {
            next = loadAt(index + 1);
        }

        if (multiple && next != null && elapsed >= SLIDE_MS) {
            releaseSlide(current);
            current = next;
            next = null;
            index++;
            slideStart += SLIDE_MS - FADE_MS;
            elapsed = now - slideStart;
        } else if ((!multiple || next == null) && elapsed >= SLIDE_MS) {
            // No successor available: keep repeating this one.
            slideStart = now;
            elapsed = 0;
        }

        float nextAlpha = 0.0F;
        if (next != null && elapsed >= SLIDE_MS - FADE_MS) {
            long age = elapsed - (SLIDE_MS - FADE_MS);
            nextAlpha = Math.min(1.0F, Math.max(0.0F, age / (float) FADE_MS));
        }

        drawSlide(graphics, current, screenWidth, screenHeight, elapsed / (float) SLIDE_MS, 1.0F);

        if (nextAlpha > 0.0F) {
            long age = elapsed - (SLIDE_MS - FADE_MS);
            drawSlide(graphics, next, screenWidth, screenHeight, age / (float) SLIDE_MS, nextAlpha);
        }

        // Darken so the credits stay readable, plus a soft vignette.
        graphics.fill(0, 0, screenWidth, screenHeight, 0x88000000);
        graphics.fillGradient(0, 0, screenWidth, screenHeight / 4, 0x99000000, 0x00000000);
        graphics.fillGradient(0, screenHeight * 3 / 4, screenWidth, screenHeight, 0x00000000, 0x99000000);

        drawCaption(graphics, current, screenWidth, screenHeight, 1.0F - nextAlpha);
        if (nextAlpha > 0.0F) {
            drawCaption(graphics, next, screenWidth, screenHeight, nextAlpha);
        }

        return true;
    }

    private static void drawSlide(
            GuiGraphics graphics,
            Slide slide,
            int screenWidth,
            int screenHeight,
            float progress,
            float alpha
    ) {
        // "Cover": fill the whole screen, crop the rest, zoom in slowly.
        float scale = Math.max(
                screenWidth / (float) slide.width(),
                screenHeight / (float) slide.height()
        ) * (1.0F + ZOOM * Math.min(1.0F, Math.max(0.0F, progress)));

        int drawWidth = Math.round(slide.width() * scale);
        int drawHeight = Math.round(slide.height() * scale);

        RenderSystem.enableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(
                slide.id(),
                (screenWidth - drawWidth) / 2,
                (screenHeight - drawHeight) / 2,
                drawWidth,
                drawHeight,
                0.0F,
                0.0F,
                slide.width(),
                slide.height(),
                slide.width(),
                slide.height()
        );
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawCaption(
            GuiGraphics graphics,
            Slide slide,
            int screenWidth,
            int screenHeight,
            float alpha
    ) {
        if (slide.caption() == null || alpha < 0.05F) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        Component text = Component.literal(slide.caption());
        int textWidth = font.width(text);
        int x = screenWidth / 2;
        int y = screenHeight - 40;

        int a = Math.min(255, Math.round(alpha * 255.0F));
        int backing = (Math.round(alpha * 150.0F) << 24);
        int gold = (a << 24) | 0xFFE7B8;
        int line = (a << 24) | 0xC49A62;

        graphics.fill(x - textWidth / 2 - 10, y - 4, x + textWidth / 2 + 10, y + 13, backing);
        graphics.fill(x - textWidth / 2 - 10, y + 13, x + textWidth / 2 + 10, y + 14, line);
        graphics.drawCenteredString(font, text, x, y, gold);
    }

    private static void start() {
        initialised = true;
        order = EndShots.listForCredits();
        index = 0;
        slideStart = System.currentTimeMillis();
        current = null;
        next = null;

        // First picture that can be read.
        for (int attempt = 0; attempt < order.size() && current == null; attempt++) {
            current = load(order.get(attempt));
            index = attempt;
        }
    }

    private static Slide loadAt(int position) {
        if (order.isEmpty()) {
            return null;
        }

        // Try a few files in a row, in case one is broken.
        for (int offset = 0; offset < Math.min(5, order.size()); offset++) {
            Slide slide = load(order.get((position + offset) % order.size()));
            if (slide != null) {
                return slide;
            }
        }

        return null;
    }

    private static Slide load(EndShots.Entry entry) {
        try (InputStream stream = Files.newInputStream(entry.path())) {
            NativeImage image = NativeImage.read(stream);
            int width = image.getWidth();
            int height = image.getHeight();

            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    "the-journey",
                    "end_shot_" + (textureCounter++)
            );

            DynamicTexture texture = new DynamicTexture(image);
            texture.setFilter(true, false);
            Minecraft.getInstance().getTextureManager().register(id, texture);

            return new Slide(id, width, height, entry.caption());
        } catch (Exception exception) {
            return null;
        }
    }

    private static void releaseSlide(Slide slide) {
        if (slide != null) {
            Minecraft.getInstance().getTextureManager().release(slide.id());
        }
    }

    /** Frees all textures and resets the slideshow. */
    public static void release() {
        releaseSlide(current);
        releaseSlide(next);
        current = null;
        next = null;
        order = new ArrayList<>();
        initialised = false;
    }
}
