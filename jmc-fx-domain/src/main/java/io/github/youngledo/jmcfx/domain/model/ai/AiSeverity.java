package io.github.youngledo.jmcfx.domain.model.ai;

public enum AiSeverity {
    INFO,
    WARNING,
    CRITICAL,
    UNKNOWN;

    public static AiSeverity fromText(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return switch (value.trim().toUpperCase()) {
            case "INFO" -> INFO;
            case "WARNING", "WARN" -> WARNING;
            case "CRITICAL", "ERROR" -> CRITICAL;
            default -> UNKNOWN;
        };
    }
}
