package com.thejourney.client.config;

/** Colour themes for the recap panel. */
public enum JourneyTheme {
    END_PORTAL("End Portal", new ThemePalette(
            0xFF07040F, 0xFF3A2A78, 0xFFB9F6EE,   // outline, border, highlight
            0xE6150C2B, 0xF0070412,               // panel background top/bottom
            0xE02C1A55, 0xE0150C2B,               // header top/bottom
            0xFF0B0719,                           // tile outer
            0xFF241649, 0xFF150C2E,               // tile body top/bottom
            0xFF33206A, 0xFF1D1142,               // tile hover top/bottom
            0xFF5B3FB0, 0xFF4A3193, 0xFF0A0616, 0xFF120A2A, // bevel top/left/bottom/right
            0xFF160D2E, 0xFF2D1D5C, 0xFF24164A,   // icon slot bg/low/right
            0xFF0C0719, 0xFF160E2F,               // value plaque inner/low
            0xFFE7FFFB, 0xFF9FB6D8, 0xFFB9F6EE,   // title, subtitle, value text
            0x07040F,                             // fade colour (rgb)
            0xFF3FE0C8, 0xFFA66BFF                // accent (teal), accent2 (purple)
    )),
    NETHER("Nether Ember", new ThemePalette(
            0xFF120508, 0xFF7A2430, 0xFFFFC2A0,
            0xE62A0E14, 0xF0120508,
            0xE0521A24, 0xE02A0E14,
            0xFF1A080C,
            0xFF4A1A22, 0xFF2E0F15,
            0xFF66262F, 0xFF42171E,
            0xFFA64A55, 0xFF8E3C47, 0xFF1C0A0E, 0xFF260D12,
            0xFF2C1016, 0xFF5E2A32, 0xFF4A2027,
            0xFF1A080C, 0xFF2C1016,
            0xFFFFE9DC, 0xFFD9A9A0, 0xFFFFC2A0,
            0x120508,
            0xFFFF6A3D, 0xFFFFC24A
    )),
    EMERALD("Emerald Grove", new ThemePalette(
            0xFF050F09, 0xFF2A7A4C, 0xFFC9F7D0,
            0xE60E2418, 0xF0050F09,
            0xE0184A2E, 0xE00E2418,
            0xFF08150E,
            0xFF1C4A30, 0xFF10301F,
            0xFF286642, 0xFF18432C,
            0xFF4A9E6C, 0xFF3C8459, 0xFF08150E, 0xFF0C1E14,
            0xFF12281B, 0xFF2A5C3E, 0xFF214933,
            0xFF08150E, 0xFF12281B,
            0xFFE6FFEA, 0xFFA6CDB0, 0xFFC9F7D0,
            0x050F09,
            0xFF4CE08A, 0xFFE8D86A
    )),
    FROST("Frozen Peaks", new ThemePalette(
            0xFF050E16, 0xFF2F6E94, 0xFFD8F2FF,
            0xE60E2233, 0xF0050E16,
            0xE01C4560, 0xE00E2233,
            0xFF08131D,
            0xFF1E4A66, 0xFF112E43,
            0xFF2A6288, 0xFF194360,
            0xFF4E9BC8, 0xFF4083AB, 0xFF08131D, 0xFF0C1C2A,
            0xFF122A3C, 0xFF2A5B7C, 0xFF204A66,
            0xFF08131D, 0xFF122A3C,
            0xFFEAF8FF, 0xFFA9C7DA, 0xFFD8F2FF,
            0x050E16,
            0xFF6FD3FF, 0xFFB7A5FF
    )),
    SUNSET("Dusk Horizon", new ThemePalette(
            0xFF120810, 0xFF8A3A6A, 0xFFFFE2B8,
            0xE6281020, 0xF0120810,
            0xE0502450, 0xE0281020,
            0xFF190B14,
            0xFF4A2040, 0xFF2E1228,
            0xFF682C58, 0xFF42193A,
            0xFFA85C90, 0xFF904B7A, 0xFF190B14, 0xFF230F1C,
            0xFF2A1224, 0xFF5E2C4E, 0xFF4A223E,
            0xFF190B14, 0xFF2A1224,
            0xFFFFF0DC, 0xFFD3AEC0, 0xFFFFE2B8,
            0x120810,
            0xFFFF9F5A, 0xFFFF6FA8
    )),
    WOOD("Classic Wood", new ThemePalette(
            0xFF120B06, 0xFF8B6339, 0xFFE0BD84,
            0xE62C1E13, 0xF0130C07,
            0xE0573A26, 0xE02F1F13,
            0xFF1E140D,
            0xFF7E5A34, 0xFF553A22,
            0xFF93703F, 0xFF6F4E2D,
            0xFFBE9358, 0xFFA67B49, 0xFF3A2716, 0xFF46301C,
            0xFF3D2C1C, 0xFF6E5236, 0xFF5E452C,
            0xFF2B1D12, 0xFF3A2A1B,
            0xFFFFD56A, 0xFFC9B493, 0xFFFFE7B8,
            0x160E08,
            0xFFE0A84A, 0xFF7BD672
    )),
    NIGHT("Midnight", new ThemePalette(
            0xFF070A12, 0xFF4A5F8A, 0xFF9DB4E0,
            0xE6141B2C, 0xF0080C16,
            0xE0263A5E, 0xE0141F36,
            0xFF0A0F1A,
            0xFF3A4F7A, 0xFF263553,
            0xFF4B6395, 0xFF33466E,
            0xFF7C95C8, 0xFF6A82B2, 0xFF18233A, 0xFF1F2C48,
            0xFF1B2640, 0xFF3C5080, 0xFF31436C,
            0xFF111A2E, 0xFF1C2842,
            0xFFBFD4FF, 0xFF9AA9C8, 0xFFDCE8FF,
            0x070A12,
            0xFF7FA8FF, 0xFFC08CFF
    )),
    END("Purple Dusk", new ThemePalette(
            0xFF0C0614, 0xFF7B4FB3, 0xFFD2A8FF,
            0xE6231238, 0xF00B0616,
            0xE04A2A78, 0xE0251340,
            0xFF120820,
            0xFF5E3A93, 0xFF3A2160,
            0xFF7248AD, 0xFF4B2C7A,
            0xFFA77BE0, 0xFF9468CC, 0xFF241040, 0xFF2E1852,
            0xFF2A1646, 0xFF583593, 0xFF492A7C,
            0xFF1A0E2E, 0xFF281646,
            0xFFE8D0FF, 0xFFB9A3D6, 0xFFF1E3FF,
            0x0C0614,
            0xFFD2A8FF, 0xFF7BD9E8
    ));

    private final String displayName;
    private final ThemePalette palette;

    JourneyTheme(String displayName, ThemePalette palette) {
        this.displayName = displayName;
        this.palette = palette;
    }

    public ThemePalette palette() {
        return palette;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
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
            int fadeRgb,
            int accent, int accent2
    ) {
        /** All colours as a flat array, used to blend smoothly between two themes. */
        public int[] toArray() {
            return new int[]{
                    outline, border, highlight, bgTop, bgBottom, headerTop, headerBottom, tileOuter,
                    tileTop, tileBottom, tileHoverTop, tileHoverBottom, bevelTop, bevelLeft, bevelBottom,
                    bevelRight, slotBg, slotLow, slotRight, plaqueInner, plaqueLow, title, subtitle, value,
                    fadeRgb, accent, accent2
            };
        }

        public static ThemePalette fromArray(int[] c) {
            return new ThemePalette(
                    c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7], c[8], c[9], c[10], c[11], c[12],
                    c[13], c[14], c[15], c[16], c[17], c[18], c[19], c[20], c[21], c[22], c[23], c[24],
                    c[25], c[26]
            );
        }
    }
}
