package com.youngledo.jmcfx.domain.model;

import java.util.List;

public record LeakReferenceNode(
        String object,
        String description,
        String referencePath,
        List<LeakReferenceNode> children) {

    public static final LeakReferenceNode EMPTY = new LeakReferenceNode("", "", "", List.of());
}
