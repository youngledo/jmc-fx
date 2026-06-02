package com.youngledo.jmcfx.ui.profiling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.youngledo.jmcfx.domain.model.StackTreeNode;

public final class FlameGraphLayoutBuilder {

    public static final int DEFAULT_MAX_DEPTH = 8;
    public static final int DEFAULT_MAX_FRAMES = 240;

    private final int maxDepth;
    private final int maxFrames;

    public FlameGraphLayoutBuilder(int maxDepth, int maxFrames) {
        this.maxDepth = Math.max(1, maxDepth);
        this.maxFrames = Math.max(1, maxFrames);
    }

    public static FlameGraphLayoutBuilder defaultBuilder() {
        return new FlameGraphLayoutBuilder(DEFAULT_MAX_DEPTH, DEFAULT_MAX_FRAMES);
    }

    public FlameGraphLayout build(StackTreeNode root) {
        if (root == null || root.count() <= 0 || childrenOf(root).isEmpty()) {
            return FlameGraphLayout.EMPTY;
        }

        List<FlameGraphFrame> frames = new ArrayList<>();
        int deepestDepth = appendChildren(childrenOf(root), root.count(), 0, 0, 1, frames);
        if (frames.isEmpty()) {
            return FlameGraphLayout.EMPTY;
        }
        return new FlameGraphLayout(frames, deepestDepth + 1);
    }

    private int appendChildren(
            List<StackTreeNode> children,
            int parentCount,
            int depth,
            double parentX,
            double parentWidth,
            List<FlameGraphFrame> frames) {
        if (depth >= maxDepth || parentCount <= 0 || frames.size() >= maxFrames) {
            return depth - 1;
        }

        int deepestDepth = depth - 1;
        double nextX = parentX;
        List<StackTreeNode> sortedChildren = sortedByCount(children);
        int widthDenominator = Math.max(parentCount, childCountSum(sortedChildren));
        List<ChildLayout> descendants = new ArrayList<>();
        for (StackTreeNode child : sortedChildren) {
            if (frames.size() >= maxFrames) {
                break;
            }

            double childWidth = parentWidth * child.count() / widthDenominator;
            FlameGraphFrame frame = new FlameGraphFrame(
                    child.method(),
                    child.count(),
                    child.percentage(),
                    child.frameInfo(),
                    depth,
                    nextX,
                    childWidth);
            frames.add(frame);
            descendants.add(new ChildLayout(child, nextX, childWidth));
            deepestDepth = Math.max(deepestDepth, depth);
            nextX += childWidth;
        }

        for (ChildLayout descendant : descendants) {
            if (frames.size() >= maxFrames) {
                break;
            }
            deepestDepth = Math.max(deepestDepth, appendChildren(
                    childrenOf(descendant.node()),
                    descendant.node().count(),
                    depth + 1,
                    descendant.x(),
                    descendant.width(),
                    frames));
        }
        return deepestDepth;
    }

    private static List<StackTreeNode> sortedByCount(List<StackTreeNode> children) {
        return children.stream()
                .filter(child -> child != null && child.count() > 0)
                .sorted(Comparator.comparingInt(StackTreeNode::count).reversed())
                .toList();
    }

    private static int childCountSum(List<StackTreeNode> children) {
        return children.stream()
                .mapToInt(StackTreeNode::count)
                .sum();
    }

    private static List<StackTreeNode> childrenOf(StackTreeNode node) {
        return node.children() == null ? List.of() : node.children();
    }

    private record ChildLayout(StackTreeNode node, double x, double width) {
    }
}
