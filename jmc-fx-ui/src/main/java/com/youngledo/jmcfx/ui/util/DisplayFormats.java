package com.youngledo.jmcfx.ui.util;

public final class DisplayFormats {

    private static final long SECOND_MS = 1000;
    private static final long MINUTE_MS = 60 * SECOND_MS;
    private static final long HOUR_MS = 60 * MINUTE_MS;

    private DisplayFormats() {
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
}
