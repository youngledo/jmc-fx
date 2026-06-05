package io.github.youngledo.jmcfx.ui.histogram;

/// Pure-logic percentage parsing utility, testable without JavaFX toolkit.
final class PercentageParser {

    private PercentageParser() {}

    static double parsePercentage(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        try {
            double parsed = Double.parseDouble(value.replaceAll("[+%\\s]", ""));
            return Math.max(0.0, Math.min(1.0, parsed / 100.0));
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }
}
