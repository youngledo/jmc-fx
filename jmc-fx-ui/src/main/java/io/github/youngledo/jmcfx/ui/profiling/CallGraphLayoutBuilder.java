package io.github.youngledo.jmcfx.ui.profiling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.StackTreeNode;

public final class CallGraphLayoutBuilder {

    public static final int DEFAULT_MAX_DEPTH = 4;
    public static final int DEFAULT_MAX_NODES = 80;

    private final int maxDepth;
    private final int maxNodes;

    public CallGraphLayoutBuilder(int maxDepth, int maxNodes) {
        this.maxDepth = Math.max(1, maxDepth);
        this.maxNodes = Math.max(1, maxNodes);
    }

    public static CallGraphLayoutBuilder defaultBuilder() {
        return new CallGraphLayoutBuilder(DEFAULT_MAX_DEPTH, DEFAULT_MAX_NODES);
    }

    public CallGraphLayout build(String selectedMethod, StackTreeNode root, CallGraphDirection direction) {
        String selectedLabel = selectedLabel(selectedMethod);
        CallGraphDirection resolvedDirection = direction == null ? CallGraphDirection.CALLEES : direction;
        List<CallGraphNode> nodes = new ArrayList<>();
        List<CallGraphEdge> edges = new ArrayList<>();
        nodes.add(new CallGraphNode("selected", selectedLabel, countOf(root), percentageOf(root), 0, 0.5, 0, true));

        appendLevels(List.of(new PendingChild("selected", root, 1)), resolvedDirection, nodes, edges);
        return new CallGraphLayout(nodes, edges, deepestDepth(nodes));
    }

    private void appendLevels(
            List<PendingChild> pending,
            CallGraphDirection direction,
            List<CallGraphNode> nodes,
            List<CallGraphEdge> edges) {
        List<PendingChild> currentLevel = pending;
        int nextId = 1;
        while (!currentLevel.isEmpty() && nodes.size() < maxNodes) {
            List<PendingChild> descendants = new ArrayList<>();
            int siblingIndex = 0;
            int siblingCount = currentLevel.stream()
                    .mapToInt(pendingChild -> sortedChildren(pendingChild.node()).size())
                    .sum();

            for (PendingChild pendingChild : currentLevel) {
                if (pendingChild.depth() > maxDepth || nodes.size() >= maxNodes) {
                    continue;
                }

                for (StackTreeNode child : sortedChildren(pendingChild.node())) {
                    if (nodes.size() >= maxNodes) {
                        break;
                    }

                    String childId = "node-" + nextId++;
                    nodes.add(new CallGraphNode(
                            childId,
                            child.method(),
                            child.count(),
                            child.percentage(),
                            pendingChild.depth(),
                            xForSibling(siblingIndex, siblingCount),
                            yForDepth(pendingChild.depth()),
                            false));
                    edges.add(edge(pendingChild.parentId(), childId, child, direction));
                    descendants.add(new PendingChild(childId, child, pendingChild.depth() + 1));
                    siblingIndex++;
                }
            }
            currentLevel = descendants;
        }
    }

    private static CallGraphEdge edge(
            String parentId,
            String childId,
            StackTreeNode child,
            CallGraphDirection direction) {
        if (direction == CallGraphDirection.CALLERS) {
            return new CallGraphEdge(childId, parentId, child.count(), child.percentage());
        }
        return new CallGraphEdge(parentId, childId, child.count(), child.percentage());
    }

    private static List<StackTreeNode> sortedChildren(StackTreeNode node) {
        if (node == null || node.children() == null) {
            return List.of();
        }
        return node.children().stream()
                .filter(child -> child != null && child.count() > 0)
                .sorted(Comparator.comparingInt(StackTreeNode::count).reversed())
                .toList();
    }

    private static int countOf(StackTreeNode node) {
        return node == null ? 0 : node.count();
    }

    private static double percentageOf(StackTreeNode node) {
        return node == null ? 0 : node.percentage();
    }

    private static String selectedLabel(String selectedMethod) {
        if (selectedMethod == null || selectedMethod.isBlank()) {
            return "<selected>";
        }
        return selectedMethod;
    }

    private double yForDepth(int depth) {
        return Math.min(depth, maxDepth) / (maxDepth + 1.0);
    }

    private static double xForSibling(int siblingIndex, int siblingCount) {
        if (siblingCount <= 1) {
            return 0.5;
        }
        return (double) siblingIndex / (siblingCount - 1);
    }

    private static int deepestDepth(List<CallGraphNode> nodes) {
        return nodes.stream()
                .mapToInt(CallGraphNode::depth)
                .max()
                .orElse(0);
    }

    private record PendingChild(String parentId, StackTreeNode node, int depth) {
    }
}
