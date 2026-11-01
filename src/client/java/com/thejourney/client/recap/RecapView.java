package com.thejourney.client.recap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thejourney.client.config.JourneyConfig;
import com.thejourney.client.config.JourneyTheme.ThemePalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The recap interface: title intro, tabs, pages, footer and final screen.
 * One instance lives in the End Credits menu and another in the standalone screen;
 * both draw into any rectangle they are given.
 */
public final class RecapView {
    public interface Host {
        boolean standalone();

        /** The "Done" button on the final screen. */
        void done();
    }

    public enum Page {
        HIGHLIGHTS("Highlights"), STATS("Stats"), PHOTOS("Photos"),
        ACHIEVEMENTS("Achievements"), PLAYTIME("Playtime"), SETTINGS("Settings"), FINAL("Finale");

        final String label;

        Page(String label) {
            this.label = label;
        }
    }

    /** Pages in the order of the guided tour (Settings and Finale are not part of it). */
    static final Page[] TOUR = {Page.HIGHLIGHTS, Page.STATS, Page.PHOTOS, Page.ACHIEVEMENTS, Page.PLAYTIME};

    private static final ItemStack[] TAB_ICONS = {
            new ItemStack(Items.NETHER_STAR), new ItemStack(Items.WRITABLE_BOOK), new ItemStack(Items.PAINTING),
            new ItemStack(Items.TOTEM_OF_UNDYING), new ItemStack(Items.CLOCK)
    };

    private record Hit(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    // ---- shared per-frame state, read by the pages ----
    final Host host;
    GuiGraphics g;
    Font font;
    ThemePalette pal;
    int mx, my;
    int vx, vy, vw, vh;      // content viewport
    float scroll;            // current scroll offset of the visible page
    boolean inContent = false;
    float dt = 0.016F;

    // ---- view state ----
    Page page = Page.HIGHLIGHTS;
    private Page previousPage = Page.HIGHLIGHTS;
    private long pageChangedAt = -1;
    private long introStart = -1;
    private boolean introDone = false;
    private boolean introBurst = false;
    long finalStart = -1;
    final EnumSet<Page> visited = EnumSet.noneOf(Page.class);
    private final float[] scrollTarget = new float[Page.values().length];
    private final float[] scrollCurrent = new float[Page.values().length];
    private int lastContentHeight = 0;

    final Map<String, Long> seen = new HashMap<>();
    final Set<String> fired = new HashSet<>();
    private final Map<String, Float> hover = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    int newThisFrame = 0;
    private long lastNanos = 0;
    private float tabX = -1, tabW = 0;
    private long ambientAt = 0;
    private int lastDataVersion = -1;

    // photos page
    int photoIndex = 0;
    int photoPrevIndex = -1;
    long photoSince = 0;
    long photoSwitchedAt = -1;

    // export feedback
    long flashAt = -1;
    int panelX, panelY, panelW, panelH;

    public RecapView(Host host) {
        this.host = host;
    }

    // ------------------------------------------------------------------ lifecycle

    /** Starts the experience from the beginning (title intro, then the first page). */
    public void open() {
        seen.clear();
        fired.clear();
        visited.clear();
        Fx.Particles.clear();
        Gallery.refresh();
        page = Page.HIGHLIGHTS;
        previousPage = page;
        finalStart = -1;
        photoIndex = 0;
        photoPrevIndex = -1;
        photoSince = Anim.now();
        java.util.Arrays.fill(scrollTarget, 0);
        java.util.Arrays.fill(scrollCurrent, 0);
        introBurst = false;

        if (Anim.on()) {
            introStart = Anim.now();
            introDone = false;
            Sfx.play(Sfx.Kind.TITLE);
        } else {
            introStart = -1;
            introDone = true;
            pageChangedAt = Anim.now();
        }
    }

    public void replay() {
        open();
    }

    public boolean intro() {
        return !introDone && introStart >= 0;
    }

    void go(Page target, boolean sound) {
        if (target == page) {
            return;
        }
        previousPage = page;
        page = target;
        pageChangedAt = Anim.now();
        if (sound) {
            Sfx.play(target == Page.FINAL ? Sfx.Kind.FINALE : Sfx.Kind.TAB);
        }
        if (target == Page.FINAL) {
            finalStart = Anim.now();
            Fx.Particles.fountain(panelX, panelY + panelH, panelW, 26, pal.accent(), pal.accent2());
        }
        if (target == Page.PHOTOS) {
            photoSince = Anim.now();
        }
        newThisFrame = 0;
    }

    void next() {
        int index = tourIndex();
        if (index < 0) {
            go(Page.HIGHLIGHTS, true);
        } else if (index + 1 < TOUR.length) {
            go(TOUR[index + 1], true);
        } else {
            go(Page.FINAL, true);
        }
    }

    void back() {
        int index = tourIndex();
        if (page == Page.FINAL) {
            go(TOUR[TOUR.length - 1], true);
        } else if (index > 0) {
            go(TOUR[index - 1], true);
        } else if (page == Page.SETTINGS) {
            go(previousPage == Page.SETTINGS ? Page.HIGHLIGHTS : previousPage, true);
        }
    }

    private int tourIndex() {
        for (int i = 0; i < TOUR.length; i++) {
            if (TOUR[i] == page) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ helpers for pages

    long reveal(String key) {
        Long start = seen.get(key);
        if (start == null) {
            start = Anim.now() + Anim.scaled(45L * Math.min(newThisFrame++, 10));
            seen.put(key, start);
        }
        return start;
    }

    boolean visible(int y, int h) {
        return y + h > vy && y < vy + vh;
    }

    /** True when the mouse is over the rectangle (and, inside the content area, inside the viewport). */
    boolean over(int x, int y, int w, int h) {
        if (mx < x || mx >= x + w || my < y || my >= y + h) {
            return false;
        }
        return !inContent || (my >= vy && my < vy + vh && mx >= vx && mx < vx + vw);
    }

    float hov(String key, boolean target) {
        float current = hover.getOrDefault(key, 0.0F);
        if (!Anim.on()) {
            current = target ? 1.0F : 0.0F;
        } else {
            current += ((target ? 1.0F : 0.0F) - current) * Math.min(1.0F, dt * 14.0F);
        }
        hover.put(key, current);
        return current;
    }

    void hit(int x, int y, int w, int h, Runnable action) {
        if (inContent) {
            int y1 = Math.max(y, vy);
            int y2 = Math.min(y + h, vy + vh);
            if (y2 <= y1) {
                return;
            }
            hits.add(new Hit(x, y1, w, y2 - y1, action));
        } else {
            hits.add(new Hit(x, y, w, h, action));
        }
    }

    /** A small bordered button. Returns true when hovered. */
    boolean button(String id, int x, int y, int w, int h, String label, boolean primary, Runnable action) {
        boolean hovered = over(x, y, w, h);
        float t = hov("b_" + id, hovered);
        int base = primary ? pal.accent() : pal.border();
        int top = Fx.lerp(Fx.withAlpha(Fx.mix(pal.tileTop(), base, primary ? 0.45F : 0.15F), 255),
                Fx.mix(pal.tileHoverTop(), base, primary ? 0.7F : 0.35F), t);
        int bottom = Fx.lerp(Fx.mix(pal.tileBottom(), base, primary ? 0.25F : 0.05F),
                Fx.mix(pal.tileHoverBottom(), base, primary ? 0.5F : 0.2F), t);

        if (t > 0.05F) {
            Fx.glow(g, x, y, w, h, pal.accent(), t * (primary ? 1.0F : 0.6F), 3);
        }
        int lift = t > 0.5F ? 1 : 0;
        Fx.panel(g, x, y - lift, w, h, top, bottom, Fx.mix(pal.border(), pal.accent(), 0.3F + 0.5F * t));
        Fx.textC(g, font, label, x + w / 2, y - lift + (h - 8) / 2 + 1,
                Fx.mix(pal.subtitle(), pal.title(), 0.4F + 0.6F * t));
        hit(x, y, w, h, () -> {
            Sfx.play(Sfx.Kind.CLICK);
            action.run();
        });
        return hovered;
    }

    // ------------------------------------------------------------------ rendering

    /**
     * Draws the whole recap into the rectangle. {@code open} (0..1) is the opening animation
     * of the panel that contains it (1 = fully open).
     */
    public void render(GuiGraphics graphics, int x, int y, int w, int h, int mouseX, int mouseY, float open) {
        long nanos = System.nanoTime();
        dt = lastNanos == 0 ? 0.016F : Math.min(0.05F, (nanos - lastNanos) / 1_000_000_000.0F);
        lastNanos = nanos;

        this.g = graphics;
        this.font = Minecraft.getInstance().font;
        this.pal = ThemeBlend.current();
        this.mx = mouseX;
        this.my = mouseY;
        this.panelX = x;
        this.panelY = y;
        this.panelW = w;
        this.panelH = h;
        this.hits.clear();
        this.newThisFrame = 0;
        this.inContent = false;

        Gallery.newFrame();
        Sfx.tick();

        if (RecapModel.version() != lastDataVersion) {
            // New data arrived from the server: let everything animate in with the real numbers.
            lastDataVersion = RecapModel.version();
            if (RecapModel.loaded()) {
                seen.clear();
                fired.clear();
            }
        }

        Fx.alpha = Math.max(0.0F, Math.min(1.0F, open));

        // Panel background, border and a soft glow that breathes.
        float breath = Anim.pulse(4200);
        Fx.glow(g, x, y, w, h, pal.accent(), 0.35F + 0.35F * breath, 4);
        Fx.panel(g, x, y, w, h, Fx.withAlpha(pal.bgTop(), 0xF0), Fx.withAlpha(pal.bgBottom(), 0xF4), pal.border());
        Fx.frame(g, x + 1, y + 1, w - 2, h - 2, Fx.rgba(pal.outline(), 0.6F));

        // Ambient motes drifting up through the panel.
        if (Fx.Particles.enabled() && Anim.now() > ambientAt) {
            ambientAt = Anim.now() + 220;
            Fx.Particles.ambient(x + 4, y + 4, w - 8, h - 8, Anim.now() % 2 == 0 ? pal.accent() : pal.accent2());
        }
        Fx.Particles.update(dt);

        if (intro()) {
            renderIntro(x, y, w, h);
        } else {
            renderMain(x, y, w, h);
        }

        Fx.Particles.draw(g, x + 2, y + 2, w - 4, h - 4);

        // Camera flash after exporting an image.
        if (flashAt >= 0) {
            float t = Anim.clamp((Anim.now() - flashAt) / 450.0F);
            if (t >= 1.0F) {
                flashAt = -1;
            } else {
                Fx.fill(g, x, y, x + w, y + h, Fx.rgba(0xFFFFFF, (1.0F - t) * 0.55F));
            }
        }

        Fx.alpha = 1.0F;

        // The one-frame capture happens after everything else is drawn.
        ExportCard.captureIfRequested(this);
    }

    private void renderIntro(int x, int y, int w, int h) {
        long elapsed = Anim.now() - introStart;
        String title = "Your Journey, " + RecapModel.year();
        float scale = w >= 330 ? 2.0F : 1.5F;
        int cx = x + w / 2;
        int cy = y + h / 2 - 14;

        // Expanding portal light.
        float line = Anim.outCubic(Anim.prog(introStart, 150, 900));
        int half = (int) (line * Math.min(w / 2 - 14, 150));
        Fx.fill(g, cx - half, cy + 14, cx + half, cy + 15, Fx.rgba(pal.accent(), 0.9F));
        Fx.fill(g, cx - half, cy + 13, cx + half, cy + 16, Fx.rgba(pal.accent(), 0.18F));
        Fx.fill(g, cx - half, cy + 10, cx + half, cy + 19, Fx.rgba(pal.accent2(), 0.08F));

        // Letter by letter entrance.
        int total = (int) (font.width(title) * scale);
        int pen = cx - total / 2;
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            float p = Anim.prog(introStart, 350 + i * 55L, 480);
            int cw = (int) (font.width(ch) * scale);
            if (p > 0.0F) {
                float rise = (1.0F - Anim.outCubic(p)) * 14.0F;
                float old = Fx.alpha;
                Fx.alpha = old * p;
                Fx.textScaled(g, font, ch, pen, (int) (cy - 6 + rise), Fx.mix(pal.title(), pal.accent(), 1.0F - p), scale, false);
                Fx.alpha = old;
            }
            pen += cw;
        }

        // Light sweep across the finished title.
        float sweep = Anim.prog(introStart, 350 + title.length() * 55L + 100, 700);
        if (sweep > 0.0F && sweep < 1.0F) {
            int sx = cx - total / 2 + (int) (sweep * total);
            Fx.fill(g, sx - 6, cy - 8, sx + 6, cy + 10, Fx.rgba(0xFFFFFF, 0.10F));
            Fx.fill(g, sx - 1, cy - 8, sx + 1, cy + 10, Fx.rgba(0xFFFFFF, 0.25F));
        }

        float sub = Anim.prog(introStart, 350 + title.length() * 55L + 300, 500);
        String rank = RecapModel.rank();
        String line2 = rank != null ? rank : "Your Minecraft year in review";
        float old = Fx.alpha;
        Fx.alpha = old * sub;
        Fx.textC(g, font, line2, cx, cy + 26, pal.accent());
        Fx.alpha = old;

        if (!introBurst && elapsed * Anim.speed() > 700) {
            introBurst = true;
            Fx.Particles.burst(cx, cy + 14, 28, pal.accent(), pal.accent2());
        }

        long length = 350 + title.length() * 55L + 1500;
        if (elapsed * Anim.speed() > length) {
            introDone = true;
            pageChangedAt = Anim.now();
            Sfx.play(Sfx.Kind.WHOOSH);
        }

        // Click anywhere to skip.
        hits.add(new Hit(x, y, w, h, () -> {
            introDone = true;
            pageChangedAt = Anim.now();
        }));
    }

    private void renderMain(int x, int y, int w, int h) {
        if (page == Page.FINAL) {
            PagesB.finale(this, x, y, w, h);
            return;
        }

        boolean compact = h < 200 || w < 260;
        int headerH = compact ? 28 : 40;
        int tabsH = 17;
        int footerH = 24;

        // ---- header ----
        Fx.grad(g, x + 2, y + 2, x + w - 2, y + headerH, pal.headerTop(), pal.headerBottom());
        Fx.fill(g, x + 2, y + headerH, x + w - 2, y + headerH + 1, pal.border());

        float titleScale = compact ? 1.0F : (w >= 330 ? 1.5F : 1.25F);
        String title = "Your Journey, " + RecapModel.year();
        Fx.textScaled(g, font, title, x + w / 2, y + (compact ? 5 : 6), pal.title(), titleScale, true);
        String rank = RecapModel.rank();
        if (rank != null && !compact) {
            int ry = y + 8 + (int) (8 * titleScale) + 3;
            int rw = font.width(rank);
            Fx.textC(g, font, rank, x + w / 2, ry, pal.accent());
            Fx.fill(g, x + w / 2 - rw / 2 - 12, ry + 4, x + w / 2 - rw / 2 - 4, ry + 5, pal.border());
            Fx.fill(g, x + w / 2 + rw / 2 + 4, ry + 4, x + w / 2 + rw / 2 + 12, ry + 5, pal.border());
        }

        // Settings gear.
        int gearX = x + w - 18;
        int gearY = y + 6;
        boolean gearHover = over(gearX - 3, gearY - 3, 15, 13);
        float gt = hov("gear", gearHover);
        boolean onSettings = page == Page.SETTINGS;
        Fx.pixels(g, gearX, gearY, Fx.GEAR, onSettings ? pal.accent() : Fx.mix(pal.subtitle(), pal.title(), gt), false);
        if (gt > 0.1F) {
            Fx.glow(g, gearX - 2, gearY - 2, 13, 11, pal.accent(), gt * 0.8F, 2);
        }
        hit(gearX - 3, gearY - 3, 15, 13, () -> {
            Sfx.play(Sfx.Kind.CLICK);
            if (page == Page.SETTINGS) {
                go(previousPage == Page.SETTINGS ? Page.HIGHLIGHTS : previousPage, false);
            } else {
                go(Page.SETTINGS, false);
            }
        });

        // ---- tabs ----
        renderTabs(x, y + headerH + 1, w, tabsH);

        // ---- content ----
        vx = x + 3;
        vy = y + headerH + 1 + tabsH + 2;
        vw = w - 6;
        vh = h - (vy - y) - footerH - 2;

        int index = page.ordinal();
        scrollCurrent[index] += (scrollTarget[index] - scrollCurrent[index]) * Math.min(1.0F, dt * (Anim.on() ? 16.0F : 100.0F));
        scroll = scrollCurrent[index];

        float enter = Anim.prog(pageChangedAt, 0, 260);
        float oldAlpha = Fx.alpha;
        Fx.alpha = oldAlpha * Anim.outCubic(enter);

        g.enableScissor(vx, vy, vx + vw, vy + vh);
        g.pose().pushPose();
        g.pose().translate(0.0F, (1.0F - Anim.outCubic(enter)) * 8.0F, 0.0F);
        inContent = true;
        visited.add(page);

        int contentHeight = switch (page) {
            case HIGHLIGHTS -> PagesA.highlights(this);
            case STATS -> PagesA.stats(this);
            case PHOTOS -> PagesA.photos(this);
            case ACHIEVEMENTS -> PagesA.achievements(this);
            case PLAYTIME -> PagesA.playtime(this);
            case SETTINGS -> PagesB.settings(this);
            default -> 0;
        };

        inContent = false;
        g.pose().popPose();
        g.disableScissor();
        Fx.alpha = oldAlpha;

        lastContentHeight = contentHeight;
        float max = Math.max(0, contentHeight - vh);
        scrollTarget[index] = Math.max(0, Math.min(scrollTarget[index], max));

        // Soft fades and scrollbar.
        int fade = Fx.withAlpha(pal.bgTop(), 0xF0) & 0xFFFFFF;
        if (scroll > 1) {
            Fx.grad(g, vx, vy, vx + vw, vy + 8, 0xF0000000 | fade, fade);
        }
        if (scroll < max - 1) {
            Fx.grad(g, vx, vy + vh - 8, vx + vw, vy + vh, fade, 0xF0000000 | fade);
        }
        if (max > 0) {
            int thumb = Math.max(16, (int) (vh * (vh / (float) contentHeight)));
            int thumbY = vy + (int) ((vh - thumb) * (scroll / max));
            Fx.fill(g, x + w - 6, vy, x + w - 3, vy + vh, Fx.rgba(0x000000, 0.35F));
            Fx.fill(g, x + w - 6, thumbY, x + w - 3, thumbY + thumb, pal.accent());
        }

        // ---- footer ----
        renderFooter(x, y + h - footerH, w, footerH);
    }

    private void renderTabs(int x, int y, int w, int h) {
        boolean icons = false;
        int total = 0;
        for (Page p : TOUR) {
            total += font.width(p.label) + 14;
        }
        if (total > w - 24) {
            icons = true;
        }

        int count = TOUR.length;
        int slot = (w - 12) / count;
        float targetX = -1, targetW = 0;

        for (int i = 0; i < count; i++) {
            Page p = TOUR[i];
            int tx = x + 6 + i * slot;
            boolean active = page == p;
            boolean hovered = over(tx, y, slot, h);
            float t = hov("tab" + i, hovered);

            if (icons) {
                int ix = tx + slot / 2 - 8;
                g.pose().pushPose();
                g.pose().translate(0, active ? -1 : 0, 0);
                if (Fx.alpha > 0.85F) {
                    g.renderItem(TAB_ICONS[i], ix, y);
                }
                g.pose().popPose();
            } else {
                int color = active ? pal.title() : Fx.mix(pal.subtitle(), pal.title(), t);
                Fx.textC(g, font, p.label, tx + slot / 2, y + 4, color);
            }

            // A small dot marks pages that have been viewed.
            if (visited.contains(p) && !active) {
                Fx.fill(g, tx + slot / 2 - 1, y + h - 3, tx + slot / 2 + 1, y + h - 1, Fx.rgba(pal.accent(), 0.6F));
            }

            if (active) {
                targetX = tx + 4;
                targetW = slot - 8;
            }
            hit(tx, y, slot, h, () -> {
                if (page != p) {
                    go(p, true);
                }
            });
        }

        // Sliding underline.
        if (targetX >= 0) {
            if (tabX < 0 || !Anim.on()) {
                tabX = targetX;
                tabW = targetW;
            } else {
                float k = Math.min(1.0F, dt * 14.0F);
                tabX += (targetX - tabX) * k;
                tabW += (targetW - tabW) * k;
            }
        }
        if (tabX >= 0 && (page != Page.SETTINGS)) {
            Fx.fill(g, (int) tabX, y + h - 2, (int) (tabX + tabW), y + h - 1, pal.accent());
            Fx.fill(g, (int) tabX - 2, y + h - 3, (int) (tabX + tabW) + 2, y + h, Fx.rgba(pal.accent(), 0.18F));
        }
        Fx.fill(g, x + 3, y + h, x + w - 3, y + h + 1, Fx.rgba(pal.border(), 0.6F));
    }

    private void renderFooter(int x, int y, int w, int h) {
        Fx.fill(g, x + 3, y, x + w - 3, y + 1, Fx.rgba(pal.border(), 0.6F));
        int by = y + 4;
        int bh = 16;

        if (page == Page.SETTINGS) {
            button("settings_back", x + w / 2 - 50, by, 100, bh, "Back to recap", true,
                    () -> go(previousPage == Page.SETTINGS ? Page.HIGHLIGHTS : previousPage, false));
            toast(x, y, w);
            return;
        }

        int index = tourIndex();
        int bw = w >= 300 ? 86 : 64;

        if (index > 0) {
            button("back", x + 8, by, bw, bh, "< Back", false, this::back);
        }

        button("export", x + w / 2 - 44, by, 88, bh, "Export image", false, ExportCard::request);

        boolean last = index == TOUR.length - 1;
        button("next", x + w - 8 - bw, by, bw, bh, last ? "Finish" : "Next >", true, this::next);

        toast(x, y, w);
    }

    private void toast(int x, int y, int w) {
        String message = ExportCard.message();
        if (message != null) {
            int tw = font.width(message) + 12;
            int tx = x + w / 2 - tw / 2;
            int ty = y - 16;
            Fx.panel(g, tx, ty, tw, 13, Fx.rgba(pal.outline() & 0xFFFFFF, 0.95F), Fx.rgba(pal.outline() & 0xFFFFFF, 0.95F), pal.accent());
            Fx.textC(g, font, message, x + w / 2, ty + 3, pal.title());
        }
    }

    // ------------------------------------------------------------------ input

    public boolean mouseClicked(double x, double y, int button) {
        if (button != 0) {
            return false;
        }
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (hit.contains(x, y)) {
                hit.action().run();
                return true;
            }
        }
        // Clicks on empty panel space are swallowed so the credits behind do not react.
        return x >= panelX && x < panelX + panelW && y >= panelY && y < panelY + panelH;
    }

    public boolean mouseScrolled(double x, double y, double amount) {
        if (x < panelX || x >= panelX + panelW || y < panelY || y >= panelY + panelH) {
            return false;
        }
        if (intro() || page == Page.FINAL) {
            return true;
        }
        int index = page.ordinal();
        float max = Math.max(0, lastContentHeight - vh);
        scrollTarget[index] = Math.max(0, Math.min(max, scrollTarget[index] - (float) amount * 22.0F));
        return true;
    }

    /** Scrolls the current page regardless of where the mouse is. */
    public void scrollBy(double amount) {
        if (intro() || page == Page.FINAL) {
            return;
        }
        int index = page.ordinal();
        float max = Math.max(0, lastContentHeight - vh);
        scrollTarget[index] = Math.max(0, Math.min(max, scrollTarget[index] - (float) amount * 22.0F));
    }

    public boolean contains(double x, double y) {
        return x >= panelX && x < panelX + panelW && y >= panelY && y < panelY + panelH;
    }

    /** Left / right arrow keys in the standalone screen. */
    public void step(int direction) {
        if (intro() || page == Page.FINAL || page == Page.SETTINGS) {
            return;
        }
        if (direction > 0) {
            next();
        } else {
            back();
        }
    }

    static PoseStack pose(GuiGraphics g) {
        return g.pose();
    }
}
