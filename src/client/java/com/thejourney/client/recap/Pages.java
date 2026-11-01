package com.thejourney.client.recap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.thejourney.client.config.JourneyConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/** Shared drawing helpers for the pages. */
final class Pages {
    private static final Map<String, ItemStack> STACKS = new HashMap<>();

    private Pages() {
    }

    /** Entrance transform: alpha, x/y offset and scale for a progress value 0..1. */
    record Xf(float alpha, float dx, float dy, float scale) {
    }

    static Xf xf(float p, int seed) {
        if (p >= 1.0F) {
            return new Xf(1, 0, 0, 1);
        }
        JourneyConfig.RevealStyle style = JourneyConfig.get().revealStyle;
        int variant = switch (style) {
            case SLIDE -> 0;
            case FADE -> 3;
            case POP -> 2;
            default -> Math.floorMod(seed, 3);
        };
        float e = Anim.outCubic(p);
        float a = Anim.clamp(p * 1.8F);
        return switch (variant) {
            case 0 -> new Xf(a, 0, (1 - e) * 14, 1);
            case 1 -> new Xf(a, (1 - e) * -26, 0, 1);
            case 2 -> new Xf(a, 0, 0, 0.82F + 0.18F * Anim.outBack(p));
            default -> new Xf(a, 0, 0, 1);
        };
    }

    /** Applies the transform around the centre of the box. Pair with {@link #end}. */
    static float begin(GuiGraphics g, Xf t, int x, int y, int w, int h) {
        float old = Fx.alpha;
        Fx.alpha = old * t.alpha();
        g.pose().pushPose();
        g.pose().translate(x + w / 2.0F + t.dx(), y + h / 2.0F + t.dy(), 0.0F);
        g.pose().scale(t.scale(), t.scale(), 1.0F);
        g.pose().translate(-(x + w / 2.0F), -(y + h / 2.0F), 0.0F);
        return old;
    }

    static void end(GuiGraphics g, float oldAlpha) {
        g.pose().popPose();
        Fx.alpha = oldAlpha;
    }

    static ItemStack stack(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        ItemStack cached = STACKS.get(id);
        if (cached != null) {
            return cached;
        }
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(key);
        if (item == Items.AIR) {
            return null;
        }
        ItemStack stack = new ItemStack(item);
        STACKS.put(id, stack);
        return stack;
    }

    /** Item icon. Items cannot be drawn half transparent, so they appear once the card is (almost) solid. */
    static void icon(GuiGraphics g, String id, int x, int y, float scale) {
        ItemStack stack = stack(id);
        if (stack == null || Fx.alpha < 0.85F) {
            return;
        }
        if (scale == 1.0F) {
            g.renderItem(stack, x, y);
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    static void icon(GuiGraphics g, ItemStack stack, int x, int y) {
        if (stack != null && Fx.alpha >= 0.85F) {
            g.renderItem(stack, x, y);
        }
    }

    /** Draws a picture so it fills the box (cropping the overflow), with an optional slow zoom. */
    static void cover(GuiGraphics g, Gallery.Tex tex, int x, int y, int w, int h, float zoom, float alpha) {
        if (tex == null || w <= 0 || h <= 0) {
            return;
        }
        float boxAspect = w / (float) h;
        float texAspect = tex.width() / (float) tex.height();
        int uw;
        int vh;
        if (texAspect > boxAspect) {
            vh = tex.height();
            uw = Math.round(vh * boxAspect);
        } else {
            uw = tex.width();
            vh = Math.round(uw / boxAspect);
        }
        float shrink = 1.0F - Anim.clamp(zoom);
        int cu = Math.max(1, Math.round(uw * shrink));
        int cv = Math.max(1, Math.round(vh * shrink));
        float u = (tex.width() - cu) / 2.0F;
        float v = (tex.height() - cv) / 2.0F;

        RenderSystem.enableBlend();
        g.setColor(1.0F, 1.0F, 1.0F, Math.max(0.0F, Math.min(1.0F, alpha * Fx.alpha)));
        g.blit(tex.id(), x, y, w, h, u, v, cu, cv, tex.width(), tex.height());
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    static String message(RecapView v, String fallback) {
        return RecapModel.loaded() ? fallback : RecapModel.message();
    }

    /** Centred, wrapped text used for empty states. */
    static int emptyState(RecapView v, String title, String text) {
        int cx = v.vx + v.vw / 2;
        int y = v.vy + Math.max(10, v.vh / 2 - 30);
        Fx.textC(v.g, v.font, title, cx, y, v.pal.title());
        int ty = y + 14;
        for (String line : Fx.wrap(v.font, text, Math.min(v.vw - 30, 260), 6)) {
            Fx.textC(v.g, v.font, line, cx, ty, v.pal.subtitle());
            ty += 10;
        }
        return v.vh;
    }
}
