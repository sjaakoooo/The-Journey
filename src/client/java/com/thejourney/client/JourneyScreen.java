package com.thejourney.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.thejourney.client.config.JourneyConfig;
import com.thejourney.client.config.JourneyTheme.ThemePalette;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class JourneyScreen extends Screen {
    private static String recapText = "Join a world to load your recap.";
    private static double scrollAmount = 0;

    private final Screen parent;

    private static final int TILE_SIZE = 76;
    private static final int TILE_GAP = 5;
    private static final int HEADER_HEIGHT = 42;
    private static final int PANEL_PADDING = 4;
    private static final int CATEGORY_HEADING_HEIGHT = 15;
    private static final int CATEGORY_BOTTOM_GAP = 8;
    private static final int MAX_STANDALONE_PANEL_WIDTH = 400;

    private static final String[][] CATEGORIES = {
            {"exploration", "EXPLORATION"},
            {"mining", "MINING"},
            {"combat", "COMBAT"},
            {"world", "WORLD"},
            {"highlights", "HIGHLIGHTS"},
            {"session", "THIS SESSION"},
            {"server", "SERVER LEADERS"}
    };

    // Accent colour per category (same order as CATEGORIES).
    private static final int[] ACCENTS = {
            0xFF7BD672, // exploration
            0xFF8FB7D9, // mining
            0xFFE0645C, // combat
            0xFFE8C24A, // world
            0xFFB98CFF, // highlights
            0xFF5ED0E6, // this session
            0xFFF08CC0  // server leaders
    };

    private static final ResourceLocation LOGO =
            ResourceLocation.fromNamespaceAndPath(
                    "the-journey",
                    "textures/gui/journey_credits_logo.png"
            );

    // Mouse position for hover effects (-1 = no mouse, like in the credits).
    private static int hoverX = -1;
    private static int hoverY = -1;

    // Size of the last drawn card list, so scrolling is clamped with exactly
    // the same numbers as drawing.
    private static int lastContentWidth = 200;
    private static int lastViewportHeight = 200;

    // Reveal animation.
    private static long animStart = 0;
    private static boolean soundPending = false;

    // "Save as image".
    private static boolean captureRequested = false;
    private static boolean cardMode = false;
    private static volatile String savedMessage = null;
    private static volatile long savedUntil = 0;

    public JourneyScreen() {
        this(null);
    }

    public JourneyScreen(Screen parent) {
        super(Component.literal("The Journey"));
        this.parent = parent;
        restartAnimation();
    }

    public static void setRecapText(String text) {
        recapText = text;
        scrollAmount = 0;

        // Real data arrived: play the reveal animation.
        if (text.contains("\t")) {
            restartAnimation();
        }
    }

    private static void restartAnimation() {
        animStart = System.currentTimeMillis();
        soundPending = true;
    }

    private static ThemePalette palette() {
        return JourneyConfig.get().theme.palette();
    }

    /** A "meta" row such as the title (rank), or null. */
    private static String getMeta(String id) {
        for (String row : recapText.split("\\R")) {
            String[] fields = row.split("\\t", 5);
            if (fields.length >= 4 && fields[1].equals("meta") && fields[0].equals(id)) {
                return fields[3];
            }
        }
        return null;
    }

    private static List<StatCard> getVisibleStats(String categoryId) {
        List<StatCard> visibleStats = new ArrayList<>();
        JourneyConfig config = JourneyConfig.get();

        for (String row : recapText.split("\\R")) {
            String[] fields = row.split("\\t", 5);

            if (fields.length < 4 || !fields[1].equals(categoryId)) {
                continue;
            }

            if (config.isEnabled(fields[0])) {
                visibleStats.add(new StatCard(
                        fields[0],
                        fields[2],
                        fields[3],
                        fields.length > 4 ? fields[4] : null
                ));
            }
        }

        return visibleStats;
    }

    /**
     * Used by the End Credits. The credits background stays visible; this only
     * draws the recap panel and the tiles.
     */
    public static void renderCreditCards(
            GuiGraphics graphics,
            int x,
            int y,
            int areaWidth,
            int areaHeight
    ) {
        hoverX = -1;
        hoverY = -1;
        renderCardGrid(graphics, x, y, areaWidth, areaHeight);
    }

    /** Called by the End Credits when the mouse wheel is used. */
    public static void scrollCreditCards(double verticalAmount) {
        scrollAmount -= verticalAmount * 18;
        clampScroll();
    }

    private static int columnsFor(int cardAreaWidth) {
        return Math.max(1, (cardAreaWidth + TILE_GAP) / (TILE_SIZE + TILE_GAP));
    }

    private static int tileSizeFor(int cardAreaWidth, int columns) {
        return Math.min(
                TILE_SIZE,
                Math.max(50, (cardAreaWidth - (columns - 1) * TILE_GAP) / columns)
        );
    }

    private static void border(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** Small plus-shaped ornament. */
    private static void ornament(GuiGraphics g, int cx, int cy, int color) {
        g.fill(cx - 1, cy, cx + 2, cy + 1, color);
        g.fill(cx, cy - 1, cx + 1, cy + 2, color);
    }

    private static void drawPanelFrame(GuiGraphics g, ThemePalette pal, int x, int y, int w, int h) {
        g.fillGradient(x, y, x + w, y + h, pal.bgTop(), pal.bgBottom());

        border(g, x, y, w, h, pal.outline());
        border(g, x + 1, y + 1, w - 2, h - 2, pal.border());
        border(g, x + 2, y + 2, w - 4, h - 4, 0x66000000);

        int[][] corners = {{x + 1, y + 1}, {x + w - 4, y + 1}, {x + 1, y + h - 4}, {x + w - 4, y + h - 4}};
        for (int[] c : corners) {
            g.fill(c[0], c[1], c[0] + 3, c[1] + 3, pal.highlight());
            g.fill(c[0] + 1, c[1] + 1, c[0] + 2, c[1] + 2, pal.tileTop());
        }
    }

    private static float ease(float t) {
        float clamped = Math.min(1.0F, Math.max(0.0F, t));
        float inverse = 1.0F - clamped;
        return 1.0F - inverse * inverse * inverse;
    }

    private static void renderCardGrid(
            GuiGraphics graphics,
            int x,
            int y,
            int areaWidth,
            int areaHeight
    ) {
        Font font = Minecraft.getInstance().font;
        ThemePalette pal = palette();
        int centerX = x + areaWidth / 2;
        long now = System.currentTimeMillis();

        if (soundPending && recapText.contains("\t")) {
            soundPending = false;
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_IN, 1.0F, 0.5F));
        }

        drawPanelFrame(graphics, pal, x, y, areaWidth, areaHeight);

        // Header with gradient: title, rank between two ornaments, and a hint.
        graphics.fillGradient(x + 2, y + 2, x + areaWidth - 2, y + HEADER_HEIGHT - 2, pal.headerTop(), pal.headerBottom());
        graphics.fill(x + 2, y + HEADER_HEIGHT - 2, x + areaWidth - 2, y + HEADER_HEIGHT - 1, pal.border());
        graphics.fill(x + 2, y + HEADER_HEIGHT - 1, x + areaWidth - 2, y + HEADER_HEIGHT, pal.outline());

        String titleText = cardMode
                ? Minecraft.getInstance().getUser().getName() + "'s Journey"
                : "Your Journey";
        Component title = Component.literal(titleText);
        int titleWidth = font.width(title);
        graphics.drawCenteredString(font, title, centerX, y + 6, pal.title());
        ornament(graphics, centerX - titleWidth / 2 - 8, y + 10, pal.highlight());
        ornament(graphics, centerX + titleWidth / 2 + 8, y + 10, pal.highlight());

        String rank = getMeta("title");
        if (rank != null) {
            Component rankText = Component.literal(rank);
            int rankWidth = font.width(rankText);
            graphics.drawCenteredString(font, rankText, centerX, y + 18, pal.highlight());
            graphics.fill(centerX - rankWidth / 2 - 10, y + 22, centerX - rankWidth / 2 - 3, y + 23, pal.border());
            graphics.fill(centerX + rankWidth / 2 + 3, y + 22, centerX + rankWidth / 2 + 10, y + 23, pal.border());
        }

        String hint = "Scroll to explore your stats";
        if (font.width(hint) > areaWidth - 12) {
            hint = "Scroll to explore";
        }
        if (!cardMode) {
            graphics.drawCenteredString(font, Component.literal(hint), centerX, y + 30, pal.subtitle());
        }

        int cardAreaX = x + PANEL_PADDING + 2;
        int cardAreaWidth = areaWidth - (PANEL_PADDING + 2) * 2;
        int cardAreaY = y + HEADER_HEIGHT + 4;
        int cardAreaHeight = Math.max(0, areaHeight - HEADER_HEIGHT - 4 - 4);

        int columns = columnsFor(cardAreaWidth);
        int tileSize = tileSizeFor(cardAreaWidth, columns);
        int gridWidth = columns * tileSize + (columns - 1) * TILE_GAP;
        int gridX = cardAreaX + (cardAreaWidth - gridWidth) / 2;
        int cardCenterX = cardAreaX + cardAreaWidth / 2;

        lastContentWidth = cardAreaWidth;
        lastViewportHeight = cardAreaHeight;
        clampScroll();

        boolean mouseInArea = hoverX >= cardAreaX && hoverX < cardAreaX + cardAreaWidth
                && hoverY >= cardAreaY && hoverY < cardAreaY + cardAreaHeight;

        graphics.enableScissor(
                cardAreaX,
                cardAreaY,
                cardAreaX + cardAreaWidth,
                cardAreaY + cardAreaHeight
        );

        int currentY = cardAreaY - (int) scrollAmount + 2;
        boolean anyStats = false;
        int tileIndex = 0;

        for (int categoryIndex = 0; categoryIndex < CATEGORIES.length; categoryIndex++) {
            String[] category = CATEGORIES[categoryIndex];
            int accent = ACCENTS[categoryIndex];
            List<StatCard> stats = getVisibleStats(category[0]);

            if (stats.isEmpty()) {
                continue;
            }

            anyStats = true;

            // Centered heading in the accent colour, with lines and ornaments.
            Component heading = Component.literal(category[1]);
            int headingWidth = font.width(heading);
            int lineY = currentY + 5;
            int leftLineEnd = cardCenterX - headingWidth / 2 - 8;
            int rightLineStart = cardCenterX + headingWidth / 2 + 8;
            int lineColor = (accent & 0x00FFFFFF) | 0x99000000;

            if (leftLineEnd > gridX + 6) {
                graphics.fill(gridX + 5, lineY, leftLineEnd, lineY + 1, lineColor);
                ornament(graphics, gridX + 2, lineY, accent);
            }
            if (gridX + gridWidth - 6 > rightLineStart) {
                graphics.fill(rightLineStart, lineY, gridX + gridWidth - 5, lineY + 1, lineColor);
                ornament(graphics, gridX + gridWidth - 3, lineY, accent);
            }

            graphics.drawCenteredString(font, heading, cardCenterX, currentY + 1, accent);

            currentY += CATEGORY_HEADING_HEIGHT;

            int rows = (stats.size() + columns - 1) / columns;

            for (int row = 0; row < rows; row++) {
                int inThisRow = Math.min(columns, stats.size() - row * columns);
                int rowWidth = inThisRow * tileSize + (inThisRow - 1) * TILE_GAP;
                // Every row is centered on its own, including a half-full last row.
                int rowX = cardAreaX + (cardAreaWidth - rowWidth) / 2;
                int rowY = currentY + row * (tileSize + TILE_GAP);

                for (int column = 0; column < inThisRow; column++) {
                    int tileX = rowX + column * (tileSize + TILE_GAP);

                    // Staggered reveal: each tile slides up a little after the previous one.
                    float appear = ease((now - animStart - tileIndex * 30L) / 280.0F);
                    float count = ease((now - animStart - tileIndex * 30L) / 800.0F);
                    tileIndex++;

                    if (appear <= 0.0F) {
                        continue;
                    }

                    int slide = Math.round((1.0F - appear) * 10.0F);
                    boolean hovered = mouseInArea
                            && hoverX >= tileX && hoverX < tileX + tileSize
                            && hoverY >= rowY && hoverY < rowY + tileSize;

                    drawCard(
                            graphics,
                            font,
                            pal,
                            stats.get(row * columns + column),
                            tileX,
                            rowY + slide,
                            tileSize,
                            accent,
                            hovered,
                            count
                    );
                }
            }

            currentY += rows * (tileSize + TILE_GAP) + CATEGORY_BOTTOM_GAP;
        }

        if (!anyStats) {
            String message = recapText.contains("\t")
                    ? "No stats are enabled."
                    : recapText;

            var lines = font.split(Component.literal(message), Math.max(40, cardAreaWidth - 8));
            int textY = cardAreaY + Math.max(0, (cardAreaHeight - lines.size() * 9) / 2);

            for (int i = 0; i < lines.size(); i++) {
                graphics.drawCenteredString(font, lines.get(i), cardCenterX, textY + i * 9, 0xFFFFFFFF);
            }
        }

        graphics.disableScissor();

        // Soft fade at the top/bottom so tiles are not cut off hard.
        int contentHeight = getContentHeight(cardAreaWidth);
        int maxScroll = Math.max(0, contentHeight - cardAreaHeight);
        int fadeSolid = 0xE0000000 | pal.fadeRgb();
        int fadeClear = pal.fadeRgb();

        if (scrollAmount > 0) {
            graphics.fillGradient(cardAreaX, cardAreaY, cardAreaX + cardAreaWidth, cardAreaY + 8,
                    fadeSolid, fadeClear);
        }
        if (scrollAmount < maxScroll) {
            graphics.fillGradient(cardAreaX, cardAreaY + cardAreaHeight - 8,
                    cardAreaX + cardAreaWidth, cardAreaY + cardAreaHeight,
                    fadeClear, fadeSolid);
        }

        drawScrollbar(graphics, pal, x + areaWidth - 7, cardAreaY, cardAreaHeight, contentHeight);
    }

    private static String animatedValue(String value, float progress) {
        if (progress >= 1.0F || !value.matches("[0-9,]+")) {
            return value;
        }

        try {
            long target = Long.parseLong(value.replace(",", ""));
            return String.format(Locale.ROOT, "%,d", (long) (target * progress));
        } catch (NumberFormatException exception) {
            return value;
        }
    }

    private static void drawCard(
            GuiGraphics graphics,
            Font font,
            ThemePalette pal,
            StatCard stat,
            int x,
            int y,
            int size,
            int accent,
            boolean hovered,
            float countProgress
    ) {
        // Drop shadow.
        graphics.fill(x + 1, y + 2, x + size + 1, y + size + 2, 0x55000000);

        // Dark outer edge and a gradient body.
        graphics.fill(x, y, x + size, y + size, pal.tileOuter());
        graphics.fillGradient(
                x + 1, y + 1, x + size - 1, y + size - 1,
                hovered ? pal.tileHoverTop() : pal.tileTop(),
                hovered ? pal.tileHoverBottom() : pal.tileBottom()
        );

        // Bevel: light top/left, dark bottom/right.
        graphics.fill(x + 1, y + 1, x + size - 1, y + 2, pal.bevelTop());
        graphics.fill(x + 1, y + 1, x + 2, y + size - 1, pal.bevelLeft());
        graphics.fill(x + 1, y + size - 2, x + size - 1, y + size - 1, pal.bevelBottom());
        graphics.fill(x + size - 2, y + 1, x + size - 1, y + size - 1, pal.bevelRight());

        // Accent strip in the category colour.
        graphics.fill(x + 3, y + 3, x + size - 3, y + 5, accent);
        graphics.fill(x + 3, y + 5, x + size - 3, y + 6, 0x66000000);

        // Icon in an inventory-like slot.
        int slot = 20;
        int slotX = x + (size - slot) / 2;
        int slotY = y + 9;
        graphics.fill(slotX, slotY, slotX + slot, slotY + slot, pal.tileOuter());
        graphics.fill(slotX + 1, slotY + 1, slotX + slot - 1, slotY + slot - 1, pal.slotBg());
        graphics.fill(slotX + 1, slotY + slot - 2, slotX + slot - 1, slotY + slot - 1, pal.slotLow());
        graphics.fill(slotX + slot - 2, slotY + 1, slotX + slot - 1, slotY + slot - 1, pal.slotRight());

        graphics.renderItem(iconFor(stat), slotX + 2, slotY + 2);

        // Label, at most two lines, centered.
        var titleLines = font.split(Component.literal(stat.label()), size - 8);
        int titleY = y + 33;
        for (int i = 0; i < Math.min(titleLines.size(), 2); i++) {
            graphics.drawCenteredString(font, titleLines.get(i), x + size / 2, titleY + i * 9, 0xFFFFFFFF);
        }

        // Value on a dark plaque; long values are scaled down to fit.
        int plaqueX = x + 5;
        int plaqueW = size - 10;
        int plaqueH = 14;
        int plaqueY = y + size - plaqueH - 5;

        graphics.fill(plaqueX, plaqueY, plaqueX + plaqueW, plaqueY + plaqueH, pal.tileOuter());
        graphics.fill(plaqueX + 1, plaqueY + 1, plaqueX + plaqueW - 1, plaqueY + plaqueH - 1, pal.plaqueInner());
        graphics.fill(plaqueX + 1, plaqueY + plaqueH - 2, plaqueX + plaqueW - 1, plaqueY + plaqueH - 1, pal.plaqueLow());

        Component value = Component.literal(animatedValue(stat.value(), countProgress));
        int valueWidth = font.width(value);
        float scale = valueWidth > plaqueW - 4
                ? (float) (plaqueW - 4) / valueWidth
                : 1.0F;

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + size / 2.0F, plaqueY + 3.0F + (1.0F - scale) * 4.0F, 0.0F);
        pose.scale(scale, scale, 1.0F);
        graphics.drawCenteredString(font, value, 0, 0, pal.value());
        pose.popPose();

        // Subtle highlight on hover (only in the standalone recap screen).
        if (hovered) {
            graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0x22FFFFFF);
        }
    }

    private static ItemStack iconFor(StatCard stat) {
        if (stat.iconId() != null) {
            ResourceLocation id = ResourceLocation.tryParse(stat.iconId());
            if (id != null) {
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item != Items.AIR) {
                    return new ItemStack(item);
                }
            }
        }

        return getIcon(stat.id());
    }

    private static ItemStack getIcon(String statId) {
        return switch (statId) {
            case "distance" -> new ItemStack(Items.IRON_BOOTS);
            case "ridden" -> new ItemStack(Items.SADDLE);
            case "elytra" -> new ItemStack(Items.ELYTRA);
            case "swum" -> new ItemStack(Items.WATER_BUCKET);
            case "play_time" -> new ItemStack(Items.CLOCK);
            case "days" -> new ItemStack(Items.SUNFLOWER);
            case "sleeps" -> new ItemStack(Items.RED_BED);
            case "jumps" -> new ItemStack(Items.FEATHER);
            case "blocks_mined" -> new ItemStack(Items.IRON_PICKAXE);
            case "ores_mined" -> new ItemStack(Items.RAW_IRON);
            case "diamonds_mined" -> new ItemStack(Items.DIAMOND);
            case "logs_chopped" -> new ItemStack(Items.OAK_LOG);
            case "blocks_placed" -> new ItemStack(Items.BRICKS);
            case "items_crafted" -> new ItemStack(Items.CRAFTING_TABLE);
            case "tools_broken" -> new ItemStack(Items.ANVIL);
            case "mobs_killed" -> new ItemStack(Items.IRON_SWORD);
            case "deaths" -> new ItemStack(Items.SKELETON_SKULL);
            case "ender_dragon_kills" -> new ItemStack(Items.DRAGON_HEAD);
            case "player_kills" -> new ItemStack(Items.PLAYER_HEAD);
            case "creepers_killed" -> new ItemStack(Items.GUNPOWDER);
            case "endermen_killed" -> new ItemStack(Items.ENDER_PEARL);
            case "withers_killed" -> new ItemStack(Items.NETHER_STAR);
            case "damage_dealt" -> new ItemStack(Items.DIAMOND_SWORD);
            case "damage_taken" -> new ItemStack(Items.SHIELD);
            case "shots_fired" -> new ItemStack(Items.BOW);
            case "raids_won" -> new ItemStack(Items.WHITE_BANNER);
            case "alive_for" -> new ItemStack(Items.GOLDEN_APPLE);
            case "animals_bred" -> new ItemStack(Items.WHEAT);
            case "fish_caught" -> new ItemStack(Items.COD);
            case "chests_opened" -> new ItemStack(Items.CHEST);
            case "villager_trades" -> new ItemStack(Items.EMERALD);
            case "items_enchanted" -> new ItemStack(Items.ENCHANTING_TABLE);
            case "food_eaten" -> new ItemStack(Items.COOKED_BEEF);
            case "advancements" -> new ItemStack(Items.WRITABLE_BOOK);
            case "cakes_eaten" -> new ItemStack(Items.CAKE);
            case "bells_rung" -> new ItemStack(Items.BELL);
            case "flowers_potted" -> new ItemStack(Items.FLOWER_POT);
            case "hl_fall" -> new ItemStack(Items.LEATHER_BOOTS);
            case "s_play_time" -> new ItemStack(Items.CLOCK);
            case "s_distance" -> new ItemStack(Items.IRON_BOOTS);
            case "s_blocks_mined" -> new ItemStack(Items.IRON_PICKAXE);
            case "s_blocks_placed" -> new ItemStack(Items.BRICKS);
            case "s_items_crafted" -> new ItemStack(Items.CRAFTING_TABLE);
            case "s_mobs_killed" -> new ItemStack(Items.IRON_SWORD);
            case "s_deaths" -> new ItemStack(Items.SKELETON_SKULL);
            default -> new ItemStack(Items.PAPER);
        };
    }

    private static void drawScrollbar(
            GuiGraphics graphics,
            ThemePalette pal,
            int x,
            int y,
            int viewportHeight,
            int contentHeight
    ) {
        if (contentHeight <= viewportHeight || viewportHeight <= 0) {
            return;
        }

        graphics.fill(x - 1, y, x + 4, y + viewportHeight, 0xAA000000 | pal.fadeRgb());

        int thumbHeight = Math.max(18, viewportHeight * viewportHeight / contentHeight);
        int maxScroll = Math.max(1, contentHeight - viewportHeight);
        int thumbY = y + (int) ((viewportHeight - thumbHeight) * scrollAmount / maxScroll);

        graphics.fill(x, thumbY, x + 3, thumbY + thumbHeight, pal.highlight());
        graphics.fill(x + 2, thumbY, x + 3, thumbY + thumbHeight, pal.border());
    }

    private static int getContentHeight(int cardAreaWidth) {
        int columns = columnsFor(cardAreaWidth);
        int tileSize = tileSizeFor(cardAreaWidth, columns);
        int height = 0;

        for (String[] category : CATEGORIES) {
            List<StatCard> stats = getVisibleStats(category[0]);
            if (!stats.isEmpty()) {
                int rows = (stats.size() + columns - 1) / columns;
                height += CATEGORY_HEADING_HEIGHT
                        + rows * (tileSize + TILE_GAP)
                        + CATEGORY_BOTTOM_GAP;
            }
        }

        return height;
    }

    private static void clampScroll() {
        int maxScroll = Math.max(0, getContentHeight(lastContentWidth) - lastViewportHeight);
        scrollAmount = Math.max(0, Math.min(scrollAmount, maxScroll));
    }

    // ------------------------------------------------------------------ the standalone screen

    @Override
    protected void init() {
        int buttonY = this.height - 24;

        this.addRenderableWidget(
                Button.builder(Component.literal("Save as image"), button -> captureRequested = true)
                        .bounds(this.width / 2 - 104, buttonY, 100, 20)
                        .tooltip(Tooltip.create(Component.literal(
                                "Saves your recap as a picture in the 'screenshots' folder")))
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("Done"), button -> this.onClose())
                        .bounds(this.width / 2 + 4, buttonY, 100, 20)
                        .build()
        );
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xE0080808);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        scrollCreditCards(verticalAmount);
        return true;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        // Background and buttons first; logo and panel go on top.
        super.render(graphics, mouseX, mouseY, partialTick);

        // Logo and panel are centered on the screen, one above the other.
        int panelWidth = Math.min(MAX_STANDALONE_PANEL_WIDTH, this.width - 36);
        int panelX = (this.width - panelWidth) / 2;

        int logoWidth = Math.max(60, Math.min(240, panelWidth - 16));
        int logoHeight = logoWidth / 4;
        int logoX = (this.width - logoWidth) / 2;
        int logoY = 8;

        graphics.blit(
                LOGO,
                logoX,
                logoY,
                logoWidth,
                logoHeight,
                0.0F,
                0.0F,
                1024,
                256,
                1024,
                256
        );

        int panelY = logoY + logoHeight + 6;
        int panelHeight = Math.max(60, this.height - panelY - 32);

        boolean capturing = captureRequested;
        cardMode = capturing;
        hoverX = capturing ? -1 : mouseX;
        hoverY = capturing ? -1 : mouseY;

        renderCardGrid(graphics, panelX, panelY, panelWidth, panelHeight);

        hoverX = -1;
        hoverY = -1;
        cardMode = false;

        if (capturing) {
            captureRequested = false;
            graphics.flush();
            saveCard(
                    panelX - 6,
                    logoY - 4,
                    panelWidth + 12,
                    panelY + panelHeight + 6 - (logoY - 4)
            );
        }

        String message = savedMessage;
        if (message != null && System.currentTimeMillis() < savedUntil) {
            graphics.drawCenteredString(
                    this.font,
                    Component.literal(message),
                    this.width / 2,
                    this.height - 38,
                    0xFF9CFF9C
            );
        }
    }

    /** Saves the part of the screen with logo and panel as a PNG in the screenshots folder. */
    private void saveCard(int guiX, int guiY, int guiWidth, int guiHeight) {
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
                return;
            }

            NativeImage card = new NativeImage(width, height, false);
            full.resizeSubRectTo(x, y, width, height, card);
            full.close();

            String name = "the-journey-recap-"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss"))
                    + ".png";
            Path target = FabricLoader.getInstance().getGameDir().resolve("screenshots").resolve(name);

            Thread writer = new Thread(() -> {
                try (card) {
                    Files.createDirectories(target.getParent());
                    card.writeToFile(target);
                    savedMessage = "Saved to screenshots/" + name;
                } catch (Exception exception) {
                    savedMessage = "Could not save the image.";
                }
                savedUntil = System.currentTimeMillis() + 5000;
            }, "the-journey-card");
            writer.setDaemon(true);
            writer.start();
        } catch (Throwable throwable) {
            savedMessage = "Could not save the image.";
            savedUntil = System.currentTimeMillis() + 5000;
        }
    }

    private record StatCard(String id, String label, String value, String iconId) {
    }
}
