package com.youngledo.jmcfx.ui.profiling;

public record CallGraphEdge(
        String sourceId,
        String targetId,
        int count,
        double percentage) {

    public CallGraphEdge {
        sourceId = sourceId == null ? "" : sourceId;
        targetId = targetId == null ? "" : targetId;
        count = Math.max(0, count);
        percentage = nonNegative(percentage);
    }

    private static double nonNegative(double value) {
        if (!Double.isFinite(value)) {
            return 0;
        }
        return Math.max(0, value);
    }
}
