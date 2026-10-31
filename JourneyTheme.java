package com.thejourney.client.config;

/** Colour themes for the recap panel. */
public enum JourneyTheme {
    WOOD(new ThemePalette(
            0xFF120B06, 0xFF8B6339, 0xFFE0BD84,   // outline, border, highlight
            0xCC2C1E13, 0xCC130C07,               // panel background top/bottom
            0xE0573A26, 0xE02F1F13,               // header top/bottom
            0xFF1E140D,                           // tile outer
            0xFF7E5A34, 0xFF553A22,               // tile body top/bottom
            0xFF93703F, 0xFF6F4E2D,               // tile hover top/bottom
            0xFFBE9358, 0xFFA67B49, 0xFF3A2716, 0xFF46301C, // bevel top/left/bottom/right
            0xFF3D2C1C, 0xFF6E5236, 0xFF5E452C,   // icon slot bg/low/right
            0xFF2B1D12, 0xFF3A2A1B,               // value plaque inner/low
            0xFFFFD56A, 0xFFC9B493, 0xFFFFE7B8,   // title, subtitle, value text
            0x160E08                              // fade colour (rgb)
    )),
    NIGHT(new ThemePalette(
            0xFF070A12, 0xFF4A5F8A, 0xFF9DB4E0,
            0xCC141B2C, 0xCC080C16,
            0xE0263A5E, 0xE0141F36,
            0xFF0A0F1A,
            0xFF3A4F7A, 0xFF263553,
            0xFF4B6395, 0xFF33466E,
            0xFF7C95C8, 0xFF6A82B2, 0xFF18233A, 0xFF1F2C48,
            0xFF1B2640, 0xFF3C5080, 0xFF31436C,
            0xFF111A2E, 0xFF1C2842,
            0xFFBFD4FF, 0xFF9AA9C8, 0xFFDCE8FF,
            0x070A12
    )),
    END(new ThemePalette(
            0xFF0C0614, 0xFF7B4FB3, 0xFFD2A8FF,
            0xCC231238, 0xCC0B0616,
            0xE04A2A78, 0xE0251340,
            0xFF120820,
            0xFF5E3A93, 0xFF3A2160,
            0xFF7248AD, 0xFF4B2C7A,
            0xFFA77BE0, 0xFF9468CC, 0xFF241040, 0xFF2E1852,
            0xFF2A1646, 0xFF583593, 0xFF492A7C,
            0xFF1A0E2E, 0xFF281646,
            0xFFE8D0FF, 0xFFB9A3D6, 0xFFF1E3FF,
            0x0C0614
    ));

    private final ThemePalette palette;

    JourneyTheme(ThemePalette palette) {
        this.palette = palette;
    }

    public ThemePalette palette() {
        return palette;
    }

    public record ThemePalette(
            int outline, int border, int highlight,
            int bgTop, int bgBottom,
            int headerTop, int headerBottom,
            int tileOuter,
            int tileTop, int tileBottom,
            int tileHoverTop, int tileHoverBottom,
            int bevelTop, int bevelLeft, int bevelBottom, int bevelRight,
            int slotBg, int slotLow, int slotRight,
            int plaqueInner, int plaqueLow,
            int title, int subtitle, int value,
            int fadeRgb
    ) {
    }
}
