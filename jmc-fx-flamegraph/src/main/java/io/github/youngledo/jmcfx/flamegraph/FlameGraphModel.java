package io.github.youngledo.jmcfx.flamegraph;

public record FlameGraphModel<T>(
        FlameGraphNode<T> root,
        int maxDepth,
        int maxFrames) {

    public static final int DEFAULT_MAX_DEPTH = 8;
    public static final int DEFAULT_MAX_FRAMES = 240;

    public FlameGraphModel {
        maxDepth = Math.max(1, maxDepth);
        maxFrames = Math.max(1, maxFrames);
    }

    public static <T> FlameGraphModel<T> empty() {
        return new FlameGraphModel<>(null, DEFAULT_MAX_DEPTH, DEFAULT_MAX_FRAMES);
    }

    public static <T> FlameGraphModel<T> of(FlameGraphNode<T> root) {
        return new FlameGraphModel<>(root, DEFAULT_MAX_DEPTH, DEFAULT_MAX_FRAMES);
    }

    public boolean isEmpty() {
        return root == null || root.weight() <= 0 || root.children().isEmpty();
    }
}
