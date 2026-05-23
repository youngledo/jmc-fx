package com.youngledo.jmcfx.ui.util;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DisplayFormats {

    private static final long SECOND_MS = 1000;
    private static final long MINUTE_MS = 60 * SECOND_MS;
    private static final long HOUR_MS = 60 * MINUTE_MS;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    private DisplayFormats() {
    }

    public static String formatInteger(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(value);
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public static String formatDuration(long millis) {
        if (millis <= 0) {
            return "0 ms";
        }
        if (millis < SECOND_MS) {
            return millis + " ms";
        }
        if (millis < MINUTE_MS) {
            return String.format("%.1f s", millis / 1000.0);
        }
        if (millis < HOUR_MS) {
            long minutes = millis / MINUTE_MS;
            long remainingSeconds = (millis % MINUTE_MS) / SECOND_MS;
            return remainingSeconds == 0
                    ? minutes + " min"
                    : minutes + " min " + remainingSeconds + " s";
        }
        long hours = millis / HOUR_MS;
        long remainingMinutes = (millis % HOUR_MS) / MINUTE_MS;
        return remainingMinutes == 0
                ? hours + " h"
                : hours + " h " + remainingMinutes + " min";
    }

    public static String formatDurationMillis(double millis) {
        return String.format(Locale.US, "%.2f ms", millis);
    }

    public static String formatMicros(long micros) {
        if (micros <= 0) {
            return "0 us";
        }
        if (micros < 1000) {
            return formatInteger(micros) + " us";
        }
        if (micros < 1_000_000) {
            return String.format(Locale.US, "%.1f ms", micros / 1000.0);
        }
        return String.format(Locale.US, "%.1f s", micros / 1_000_000.0);
    }

    public static String formatPercent(double percent) {
        return String.format(Locale.US, "%.1f%%", percent);
    }

    public static String formatBoolean(boolean value) {
        return value ? "Yes" : "No";
    }

    public static String formatTimestamp(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return "";
        }
        ZoneId displayZone = zoneId == null ? ZoneId.systemDefault() : zoneId;
        return TIMESTAMP_FORMATTER.withZone(displayZone).format(instant);
    }
}
