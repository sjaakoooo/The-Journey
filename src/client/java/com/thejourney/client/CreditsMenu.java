package com.thejourney.client;

import com.thejourney.client.recap.Anim;
import com.thejourney.client.recap.Fx;
import com.thejourney.client.recap.RecapView;
import com.thejourney.client.recap.Sfx;
import com.thejourney.client.recap.ThemeBlend;
import com.thejourney.client.config.JourneyTheme.ThemePalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The expandable recap menu of the End Credits: a small arrow tab on the left that smoothly
 * opens the complete recap experience. The credits stay on screen and keep running.
 */
public final class CreditsMenu {
    private static final RecapView.Host HOST = new RecapView.Host() {
        @Override
        public boolean standalone() {
            return false;
        }

        @Override
        public void done() {
            setExpanded(false);
        }
    };

    private static final RecapView VIEW = new RecapView(HOST);

    private static Object attached = null;
    private static boolean expanded = false;
    private static boolean everOpened = false;
    private static float progress = 0.0F;
    private static long lastNanos = 0;
    private static float tabHover = 0.0F;

    // Where the arrow tab was drawn this frame (for clicking).
    private static int tabX, tabY, tabW, tabH;

    private CreditsMenu() {
    }

    /** Called when a credits screen is created or re-initialised (for example on window resize). */
    public static void attach(Object screen) {
        if (attached != screen) {
            attached = screen;
            expanded = false;
            everOpened = false;
            progress = 0.0F;
            lastNanos = 0;
        }
    }

    public static boolean expanded() {
        return expanded;
    }

    /** Credits should wait at their end instead of closing while the recap is being used. */
    public static boolean holdCredits() {
        return expanded || progress > 0.02F;
    }

    private static float eased() {
        return Anim.inOutCubic(progress);
    }

    public static int creditsShift(int screenWidth) {
        return CreditsLayout.creditsShift(screenWidth, eased());
    }

    public static void setExpanded(boolean value) {
        if (value == expanded) {
            return;
        }
        expanded = value;
        Sfx.play(value ? Sfx.Kind.OPEN : Sfx.Kind.CLOSE);
        if (value && !everOpened) {
            everOpened = true;
            VIEW.open();
        }
    }

    public static void render(GuiGraphics g, int mouseX, int mouseY) {
        Minecraft client = Minecraft.getInstance();
        int sw = client.getWindow().getGuiScaledWidth();
        int sh = client.getWindow().getGuiScaledHeight();

        long nanos = System.nanoTime();
        float dt = lastNanos == 0 ? 0.016F : Math.min(0.05F, (nanos - lastNanos) / 1_000_000_000.0F);
        lastNanos = nanos;

        float target = expanded ? 1.0F : 0.0F;
        if (!Anim.on()) {
            progress = target;
        } else {
            float step = dt * 3.2F * Anim.speed();
            progress = progress < target ? Math.min(target, progress + step) : Math.max(target, progress - step);
        }

        float e = eased();
        CreditsLayout.Panel panel = CreditsLayout.panel(sw, sh);
        ThemePalette pal = ThemeBlend.current();

        int panelX = panel.x() - Math.round((1.0F - e) * (panel.w() + 24));

        if (progress > 0.001F) {
            // Dim whatever is behind the recap so the panel reads well.
            Fx.alpha = 1.0F;
            int scrimWidth = CreditsLayout.overlay(sw) ? sw : CreditsLayout.columnWidth(sw);
            g.fill(0, 0, scrimWidth, sh, Fx.rgba(0x000000, 0.4F * e));

            VIEW.render(g, panelX, panel.y(), panel.w(), panel.h(), mouseX, mouseY, Math.min(1.0F, e * 1.5F));
        }

        renderTab(g, client.font, pal, Math.max(0, panelX + panel.w() - 1), sh / 2 - 22, e, mouseX, mouseY, dt);
        Fx.alpha = 1.0F;
    }

    private static void renderTab(GuiGraphics g, Font font, ThemePalette pal, int x, int y, float e,
                                  int mouseX, int mouseY, float dt) {
        int w = CreditsLayout.tabWidth();
        int h = 44;
        tabX = x;
        tabY = y;
        tabW = w;
        tabH = h;

        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        tabHover += ((hovered ? 1.0F : 0.0F) - tabHover) * Math.min(1.0F, dt * (Anim.on() ? 14.0F : 100.0F));

        Fx.alpha = 1.0F;
        // While the menu has never been opened, the tab breathes gently so it is easy to find.
        float attention = (!everOpened && !expanded) ? 0.35F + 0.65F * Anim.pulse(2200) : 0.0F;
        float glowStrength = Math.max(tabHover, attention);
        if (glowStrength > 0.02F) {
            Fx.glow(g, x, y, w, h, pal.accent(), glowStrength, 4);
        }

        int fill = Fx.lerp(Fx.withAlpha(pal.bgTop(), 0xF0), Fx.withAlpha(pal.headerTop(), 0xF8), tabHover);
        Fx.panel(g, x, y, w, h, fill, Fx.withAlpha(pal.bgBottom(), 0xF4), Fx.mix(pal.border(), pal.accent(), 0.35F + 0.65F * Math.max(tabHover, attention * 0.7F)));

        // Chevron that turns around: points right when collapsed, left when expanded.
        float scaleX = 1.0F - 2.0F * e;
        int cx = x + w / 2;
        int cy = y + h / 2;
        float nudge = (Anim.on() && !expanded && !everOpened) ? (float) Math.sin(Anim.now() / 260.0) * 1.2F : 0.0F;
        g.pose().pushPose();
        g.pose().translate(cx + nudge, cy, 0.0F);
        g.pose().scale(Math.max(0.05F, Math.abs(scaleX)), 1.0F, 1.0F);
        Fx.pixels(g, -1, -2, Fx.CHEVRON, Fx.mix(pal.subtitle(), pal.title(), tabHover), scaleX < 0);
        g.pose().popPose();

        // Small label on hover (collapsed only): says what the arrow opens.
        if (!expanded && tabHover > 0.05F && e < 0.05F) {
            String label = "Your Journey";
            int lw = font.width(label) + 10;
            Fx.alpha = tabHover;
            Fx.panel(g, x + w + 2, cy - 8, lw, 16, Fx.withAlpha(pal.bgTop(), 0xF0), Fx.withAlpha(pal.bgBottom(), 0xF4), pal.border());
            Fx.textC(g, font, label, x + w + 2 + lw / 2, cy - 4, pal.title());
            Fx.alpha = 1.0F;
        }
    }

    /** Returns true when the click was used by the menu (so the credits must not react). */
    public static boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && x >= tabX && x < tabX + tabW && y >= tabY && y < tabY + tabH) {
            setExpanded(!expanded);
            return true;
        }
        return expanded && VIEW.mouseClicked(x, y, button);
    }

    /** Returns true when the wheel scrolled the recap. Otherwise the credits scroll as usual. */
    public static boolean mouseScrolled(double x, double y, double amount) {
        return expanded && VIEW.mouseScrolled(x, y, amount);
    }

    public static void scrollBy(double amount) {
        if (expanded) {
            VIEW.scrollBy(amount);
        }
    }

    /** The credits are being closed: free pictures and start fresh next time. */
    public static void reset() {
        attached = null;
        expanded = false;
        progress = 0.0F;
        everOpened = false;
    }
}
