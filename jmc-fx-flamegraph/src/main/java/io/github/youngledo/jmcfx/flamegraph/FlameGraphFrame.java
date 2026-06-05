package io.github.youngledo.jmcfx.flamegraph;

import java.util.List;

public record FlameGraphFrame<T>(
        FlameGraphNode<T> node,
        int depth,
        int row,
        double x,
        double width,
        List<Integer> path) {

    public FlameGraphFrame {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }
        depth = Math.max(0, depth);
        row = Math.max(0, row);
        x = clampUnit(x);
        width = clampUnit(width);
        path = path == null ? List.of() : List.copyOf(path);
    }

    boolean samePath(FlameGraphFrame<T> other) {
        return other != null && path.equals(other.path);
    }

    boolean isAncestorOf(FlameGraphFrame<T> other) {
        return other != null
                && path.size() < other.path.size()
                && other.path.subList(0, path.size()).equals(path);
    }

    boolean isDescendantOf(FlameGraphFrame<T> other) {
        return other != null && other.isAncestorOf(this);
    }

    private static double clampUnit(double value) {
        if (Double.isNaN(value)) {
            return 0;
        }
        return Math.clamp(value, 0, 1);
    }
}
