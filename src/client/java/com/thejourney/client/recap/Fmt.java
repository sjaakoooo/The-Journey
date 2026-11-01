package com.thejourney.client.recap;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Number formatting. Mirrors the server's formatting so counting up ends on the exact same text. */
public final class Fmt {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private Fmt() {
    }

    public static String number(long value) {
        return String.format(Locale.ROOT, "%,d", Math.max(0, value));
    }

    public static String distance(long centimeters) {
        double meters = Math.max(0, centimeters) / 100.0;
        if (meters >= 1000.0) {
            return String.format(Locale.ROOT, "%.1f km", meters / 1000.0);
        }
        return String.format(Locale.ROOT, "%d m", Math.round(meters));
    }

    public static String duration(long ticks) {
        long totalMinutes = Math.max(0, ticks) / 20 / 60;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours > 0 ? hours + " h " + minutes + " min" : minutes + " min";
    }

    public static String durationMs(long millis) {
        return duration(Math.max(0, millis) / 50);
    }

    public static String date(long epochMillis) {
        return DATE.format(LocalDate.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()));
    }

    public static String percent(double fraction) {
        return Math.round(Math.max(0, Math.min(1, fraction)) * 100) + "%";
    }
}
