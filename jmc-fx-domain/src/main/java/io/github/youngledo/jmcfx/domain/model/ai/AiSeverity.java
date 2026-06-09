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
            case "提示", "信息", "一般" -> INFO;
            case "WARNING", "WARN", "警告", "中", "中等" -> WARNING;
            case "CRITICAL", "ERROR", "严重", "高", "高危", "关键" -> CRITICAL;
            default -> UNKNOWN;
        };
    }
}
