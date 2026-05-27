package com.youngledo.jmcfx.ui.profiling;

public record CallGraphNode(
        String id,
        String label,
        int count,
        double percentage,
        int depth,
        double x,
        double y,
        boolean primary) {

    public CallGraphNode {
        id = id == null ? "" : id;
        label = label == null ? "" : label;
        count = Math.max(0, count);
        percentage = nonNegative(percentage);
        depth = Math.max(0, depth);
        x = clampUnit(x);
        y = clampUnit(y);
    }

    private static double clampUnit(double value) {
        if (!Double.isFinite(value)) {
            return 0;
        }
        return Math.clamp(value, 0, 1);
    }

    private static double nonNegative(double value) {
        if (!Double.isFinite(value)) {
            return 0;
        }
        return Math.max(0, value);
    }
}
