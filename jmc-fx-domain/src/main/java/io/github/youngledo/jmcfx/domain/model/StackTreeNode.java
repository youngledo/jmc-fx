package io.github.youngledo.jmcfx.domain.model;

import java.util.List;

public record StackTreeNode(
        String method,
        int count,
        double percentage,
        StackFrameInfo frameInfo,
        List<StackTreeNode> children) {

    public static final StackTreeNode EMPTY = new StackTreeNode("", 0, 0, StackFrameInfo.EMPTY, List.of());

    public StackTreeNode(String method, int count, double percentage, List<StackTreeNode> children) {
        this(method, count, percentage, StackFrameInfo.EMPTY, children);
    }

    public StackTreeNode {
        method = method == null ? "" : method;
        frameInfo = frameInfo == null ? StackFrameInfo.EMPTY : frameInfo;
    }
}
