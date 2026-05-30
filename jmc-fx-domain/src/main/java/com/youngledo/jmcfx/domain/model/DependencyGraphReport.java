package com.youngledo.jmcfx.domain.model;

import java.util.List;

public record DependencyGraphReport(
        List<DependencyGraphEdge> edges,
        int totalTransitions,
        int packageDepth) {

    public static final DependencyGraphReport EMPTY = new DependencyGraphReport(List.of(), 0, 0);

    public DependencyGraphReport {
        edges = edges == null ? List.of() : List.copyOf(edges);
        totalTransitions = Math.max(0, totalTransitions);
        packageDepth = Math.max(0, packageDepth);
    }
}
