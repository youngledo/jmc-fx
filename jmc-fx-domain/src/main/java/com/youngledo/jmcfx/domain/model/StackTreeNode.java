package com.youngledo.jmcfx.domain.model;

import java.util.List;

public record StackTreeNode(
        String method,
        int count,
        double percentage,
        List<StackTreeNode> children) {

    public static final StackTreeNode EMPTY = new StackTreeNode("", 0, 0, List.of());
}
