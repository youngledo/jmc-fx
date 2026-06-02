package com.youngledo.jmcfx.ui.profiling;

import com.youngledo.jmcfx.domain.model.StackFrameInfo;

public record FlameGraphFrame(
        String method,
        int count,
        double percentage,
        StackFrameInfo frameInfo,
        int depth,
        double x,
        double width) {

    public FlameGraphFrame(String method, int count, double percentage, int depth, double x, double width) {
        this(method, count, percentage, StackFrameInfo.EMPTY, depth, x, width);
    }

    public FlameGraphFrame {
        method = method == null ? "" : method;
        count = Math.max(0, count);
        frameInfo = frameInfo == null ? StackFrameInfo.EMPTY : frameInfo;
        depth = Math.max(0, depth);
        x = clampUnit(x);
        width = clampUnit(width);
    }

    private static double clampUnit(double value) {
        if (Double.isNaN(value)) {
            return 0;
        }
        return Math.clamp(value, 0, 1);
    }
}
