package com.thejourney.client;

/**
 * One place that decides how the End Credits screen is divided.
 * <p>
 * Collapsed: the credits are centred, like in the normal game.
 * Expanded on a wide screen: the recap takes the left side and the credits glide to the middle of
 * the remaining right side, so they stay fully readable.
 * Expanded on a narrow screen: there is no room for both, so the recap is drawn over the (dimmed) credits.
 */
public final class CreditsLayout {
    private static final int SPLIT_MIN_SCREEN_WIDTH = 620;
    private static final int TAB_WIDTH = 14;

    private CreditsLayout() {
    }

    public record Panel(int x, int y, int w, int h) {
    }

    /** True when the recap has to cover the credits because the screen is too narrow for both. */
    public static boolean overlay(int screenWidth) {
        return screenWidth < SPLIT_MIN_SCREEN_WIDTH;
    }

    /** Width of the left column (recap) in split mode. */
    public static int columnWidth(int screenWidth) {
        return Math.max(300, Math.min(520, screenWidth / 2));
    }

    public static int tabWidth() {
        return TAB_WIDTH;
    }

    public static Panel panel(int screenWidth, int screenHeight) {
        int y = 8;
        int h = Math.max(120, screenHeight - 16);

        if (overlay(screenWidth)) {
            // Leave room on the right for the arrow tab.
            int w = Math.max(200, Math.min(430, screenWidth - 8 - TAB_WIDTH - 6));
            return new Panel(8, y, w, h);
        }

        int column = columnWidth(screenWidth);
        return new Panel(8, y, column - 12, h);
    }

    /**
     * How far the normal credits are moved to the right. Vanilla centres them on width / 2;
     * moving by half a column centres them in the space that is left over.
     */
    public static int creditsShift(int screenWidth, float eased) {
        if (overlay(screenWidth)) {
            return 0;
        }
        return Math.round(columnWidth(screenWidth) / 2.0F * eased);
    }
}
