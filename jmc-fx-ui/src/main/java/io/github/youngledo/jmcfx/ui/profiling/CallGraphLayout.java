package io.github.youngledo.jmcfx.ui.profiling;

import java.util.List;

public record CallGraphLayout(
        List<CallGraphNode> nodes,
        List<CallGraphEdge> edges,
        int maxDepth) {

    public static final CallGraphLayout EMPTY = new CallGraphLayout(List.of(), List.of(), 0);

    public CallGraphLayout {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        edges = edges == null ? List.of() : List.copyOf(edges);
        maxDepth = Math.max(0, maxDepth);
    }
}
