package com.youngledo.jmcfx.flamegraph;

import java.util.List;

public record FlameGraphNode<T>(
        String label,
        double weight,
        double percentage,
        T payload,
        List<FlameGraphNode<T>> children) {

    public FlameGraphNode {
        label = label == null ? "" : label;
        weight = Math.max(0, weight);
        children = children == null ? List.of() : List.copyOf(children);
    }
}
