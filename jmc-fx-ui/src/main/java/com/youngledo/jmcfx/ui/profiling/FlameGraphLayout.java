package com.youngledo.jmcfx.ui.profiling;

import java.util.List;

public record FlameGraphLayout(List<FlameGraphFrame> frames, int maxDepth) {

    public static final FlameGraphLayout EMPTY = new FlameGraphLayout(List.of(), 0);

    public FlameGraphLayout {
        frames = frames == null ? List.of() : List.copyOf(frames);
        maxDepth = Math.max(0, maxDepth);
    }
}
