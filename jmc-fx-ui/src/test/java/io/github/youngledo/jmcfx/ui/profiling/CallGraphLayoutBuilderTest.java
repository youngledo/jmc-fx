package io.github.youngledo.jmcfx.ui.profiling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.domain.model.StackTreeNode;

class CallGraphLayoutBuilderTest {

    @Test
    void callGraphNodeNormalizesNullDefaultsAndBounds() {
        CallGraphNode node = new CallGraphNode(null, null, -1, -2, -3, -0.5, 1.5, false);

        assertEquals("", node.id());
        assertEquals("", node.label());
        assertEquals(0, node.count());
        assertEquals(0, node.percentage());
        assertEquals(0, node.depth());
        assertEquals(0, node.x());
        assertEquals(1, node.y());
    }

    @Test
    void callGraphEdgeNormalizesNullDefaultsAndNonNegativeValues() {
        CallGraphEdge edge = new CallGraphEdge(null, null, -1, -2);

        assertEquals("", edge.sourceId());
        assertEquals("", edge.targetId());
        assertEquals(0, edge.count());
        assertEquals(0, edge.percentage());
    }

    @Test
    void callGraphLayoutNormalizesNullListsDefensivelyCopiesAndClampsDepth() {
        CallGraphLayout nullLayout = new CallGraphLayout(null, null, -1);

        assertTrue(nullLayout.nodes().isEmpty());
        assertTrue(nullLayout.edges().isEmpty());
        assertEquals(0, nullLayout.maxDepth());

        List<CallGraphNode> nodes = new ArrayList<>();
        nodes.add(new CallGraphNode("selected", "selected", 1, 1, 0, 0.5, 0.5, true));
        List<CallGraphEdge> edges = new ArrayList<>();
        edges.add(new CallGraphEdge("selected", "node-1", 1, 1));
        CallGraphLayout copiedLayout = new CallGraphLayout(nodes, edges, 2);

        nodes.clear();
        edges.clear();

        assertEquals(1, copiedLayout.nodes().size());
        assertEquals(1, copiedLayout.edges().size());
        assertEquals(2, copiedLayout.maxDepth());
        assertSame(CallGraphLayout.EMPTY, CallGraphLayout.EMPTY);
        assertTrue(CallGraphLayout.EMPTY.nodes().isEmpty());
        assertTrue(CallGraphLayout.EMPTY.edges().isEmpty());
    }

    @Test
    void callersGraphConnectsCallerToSelectedMethod() {
        StackTreeNode root = node("root", 100, node("caller", 70));

        CallGraphLayout layout = CallGraphLayoutBuilder.defaultBuilder()
                .build("com.example.Service.run", root, CallGraphDirection.CALLERS);

        assertEquals(List.of("com.example.Service.run", "caller"), labels(layout));
        assertEquals(1, layout.edges().size());
        CallGraphEdge edge = layout.edges().getFirst();
        assertEquals("node-1", edge.sourceId());
        assertEquals("selected", edge.targetId());
        assertEquals(70, edge.count());
    }

    @Test
    void calleesGraphConnectsSelectedMethodToCallee() {
        StackTreeNode root = node("root", 100, node("callee", 60));

        CallGraphLayout layout = CallGraphLayoutBuilder.defaultBuilder()
                .build("com.example.Service.run", root, CallGraphDirection.CALLEES);

        assertEquals(List.of("com.example.Service.run", "callee"), labels(layout));
        assertEquals(1, layout.edges().size());
        CallGraphEdge edge = layout.edges().getFirst();
        assertEquals("selected", edge.sourceId());
        assertEquals("node-1", edge.targetId());
        assertEquals(60, edge.count());
    }

    @Test
    void blankOrNullSelectedMethodUsesSelectedPlaceholder() {
        StackTreeNode root = node("root", 100);

        CallGraphLayout nullSelected = CallGraphLayoutBuilder.defaultBuilder()
                .build(null, root, CallGraphDirection.CALLEES);
        CallGraphLayout blankSelected = CallGraphLayoutBuilder.defaultBuilder()
                .build("  ", root, CallGraphDirection.CALLEES);

        assertEquals("<selected>", nullSelected.nodes().getFirst().label());
        assertEquals("<selected>", blankSelected.nodes().getFirst().label());
    }

    @Test
    void nullDirectionDefaultsToCallees() {
        StackTreeNode root = node("root", 100, node("callee", 60));

        CallGraphLayout layout = CallGraphLayoutBuilder.defaultBuilder()
                .build("selected method", root, null);

        CallGraphEdge edge = layout.edges().getFirst();
        assertEquals("selected", edge.sourceId());
        assertEquals("node-1", edge.targetId());
    }

    @Test
    void constructorClampsMinimumDepthAndNodeCount() {
        StackTreeNode root = node("root", 100, node("child", 50));

        CallGraphLayout layout = new CallGraphLayoutBuilder(0, 0)
                .build("selected", root, CallGraphDirection.CALLEES);

        assertEquals(List.of("selected"), labels(layout));
        assertEquals(0, layout.maxDepth());
        assertTrue(layout.edges().isEmpty());
    }

    @Test
    void sortsChildrenByCountDescending() {
        StackTreeNode root = node("root", 100,
                node("b", 20),
                node("a", 40),
                node("c", 30));

        CallGraphLayout layout = CallGraphLayoutBuilder.defaultBuilder()
                .build("selected", root, CallGraphDirection.CALLEES);

        assertEquals(List.of("selected", "a", "c", "b"), labels(layout));
    }

    @Test
    void keepsNodeCoordinatesWithinUnitBounds() {
        StackTreeNode root = node("root", 100,
                node("a", 40, node("a1", 20)),
                node("b", 30),
                node("c", 20));

        CallGraphLayout layout = CallGraphLayoutBuilder.defaultBuilder()
                .build("selected", root, CallGraphDirection.CALLEES);

        assertTrue(layout.nodes().stream()
                .allMatch(node -> node.x() >= 0 && node.x() <= 1 && node.y() >= 0 && node.y() <= 1));
    }

    @Test
    void separatesSelectedNodeYFromChildAndDescendantRows() {
        StackTreeNode root = node("root", 100,
                node("child", 50, node("descendant", 25)));

        CallGraphLayout layout = new CallGraphLayoutBuilder(2, 8)
                .build("selected", root, CallGraphDirection.CALLEES);

        CallGraphNode selected = layout.nodes().stream()
                .filter(CallGraphNode::primary)
                .findFirst()
                .orElseThrow();

        assertEquals(0, selected.y());
        assertTrue(layout.nodes().stream()
                .filter(node -> !node.primary())
                .noneMatch(node -> node.y() == selected.y()));
        assertTrue(layout.nodes().stream()
                .allMatch(node -> node.x() >= 0 && node.x() <= 1 && node.y() >= 0 && node.y() <= 1));
    }

    @Test
    void skipsNullAndNonPositiveChildren() {
        StackTreeNode root = new StackTreeNode("root", 100, 0,
                Arrays.asList(null, node("zero", 0), node("negative", -5), node("positive", 10)));

        CallGraphLayout layout = CallGraphLayoutBuilder.defaultBuilder()
                .build("selected method", root, CallGraphDirection.CALLEES);

        assertEquals(List.of("selected method", "positive"), labels(layout));
        assertEquals(1, layout.edges().size());
    }

    @Test
    void maxDepthAndMaxNodesLimitGraph() {
        StackTreeNode root = node("root", 100,
                node("a", 50, node("a1", 40, node("a2", 30))),
                node("b", 30),
                node("c", 20));

        CallGraphLayout layout = new CallGraphLayoutBuilder(2, 3)
                .build("selected", root, CallGraphDirection.CALLEES);

        assertEquals(3, layout.nodes().size());
        assertEquals(2, layout.edges().size());
        assertTrue(layout.nodes().stream().allMatch(node -> node.depth() <= 1));
        assertEquals(1, layout.maxDepth());
    }

    @Test
    void emptyStackTreeProducesOnlySelectedNode() {
        CallGraphLayout layout = CallGraphLayoutBuilder.defaultBuilder()
                .build("selected method", StackTreeNode.EMPTY, CallGraphDirection.CALLEES);

        assertEquals(List.of("selected method"), labels(layout));
        assertTrue(layout.nodes().getFirst().primary());
        assertTrue(layout.edges().isEmpty());
    }

    @Test
    void maxNodesPreservesCurrentLevelSiblingsBeforeDescendants() {
        StackTreeNode root = node("root", 100,
                node("sibling-a", 40, node("child-a", 40)),
                node("sibling-b", 30),
                node("sibling-c", 20));

        CallGraphLayout layout = new CallGraphLayoutBuilder(8, 4)
                .build("selected", root, CallGraphDirection.CALLEES);

        assertEquals(List.of("selected", "sibling-a", "sibling-b", "sibling-c"), labels(layout));
        assertTrue(layout.nodes().stream().noneMatch(node -> node.label().equals("child-a")));
    }

    private static List<String> labels(CallGraphLayout layout) {
        return layout.nodes().stream()
                .map(CallGraphNode::label)
                .toList();
    }

    private static StackTreeNode node(String method, int count, StackTreeNode... children) {
        return new StackTreeNode(method, count, 0, List.of(children));
    }
}
