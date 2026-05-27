package com.youngledo.jmcfx.ui.profiling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.StackTreeNode;

class CallGraphLayoutBuilderTest {

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
