package com.thejourney.client;

/**
 * One place that decides how the End Credits screen is divided:
 * on the left the recap (logo + panel, centered together in their column),
 * on the right the normal credits (centered in their own column).
 */
public final class CreditsLayout {
    /** Vanilla breekt credits-regels af op 256 breed. */
    private static final int CREDITS_TEXT_WIDTH = 256;
    private static final int COLUMN_MARGIN = 14;
    private static final int MAX_PANEL_WIDTH = 380;
    private static final int MAX_LOGO_WIDTH = 240;
    private static final int LOGO_TOP = 10;

    private CreditsLayout() {
    }

    public record Layout(
            int logoX,
            int logoY,
            int logoWidth,
            int logoHeight,
            int panelX,
            int panelY,
            int panelWidth,
            int panelHeight,
            int creditsShift,
            int creditsColumnCenterX,
            int dividerX
    ) {
    }

    /** Width of the column with the normal credits. */
    private static int creditsColumnWidth(int screenWidth) {
        return Math.min(CREDITS_TEXT_WIDTH + COLUMN_MARGIN * 2, screenWidth * 6 / 10);
    }

    /**
     * How far the normal credits must move to the right so that they are exactly
     * in the middle of their column (vanilla centers on width / 2).
     */
    public static int creditsShift(int screenWidth) {
        return (screenWidth - creditsColumnWidth(screenWidth)) / 2;
    }

    public static Layout compute(int screenWidth, int screenHeight) {
        int creditsColumn = creditsColumnWidth(screenWidth);
        int recapColumn = screenWidth - creditsColumn;

        int panelWidth = Math.max(90, Math.min(MAX_PANEL_WIDTH, recapColumn - COLUMN_MARGIN * 2));
        int panelX = (recapColumn - panelWidth) / 2;

        int logoWidth = Math.max(60, Math.min(MAX_LOGO_WIDTH, panelWidth - 16));
        int logoHeight = logoWidth / 4;
        int logoX = (recapColumn - logoWidth) / 2;

        int panelY = LOGO_TOP + logoHeight + 6;
        int panelHeight = Math.max(40, screenHeight - panelY - 12);

        return new Layout(
                logoX,
                LOGO_TOP,
                logoWidth,
                logoHeight,
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                creditsShift(screenWidth),
                recapColumn + creditsColumn / 2,
                recapColumn
        );
    }
}
