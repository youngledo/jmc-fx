package com.youngledo.jmcfx.ui.profiling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.StackTreeNode;

class FlameGraphLayoutBuilderTest {

    private static final double DELTA = 0.000001;

    @Test
    void convertsStackTreeToNormalizedFrames() {
        StackTreeNode root = node("root", 100, node("A", 60), node("B", 40));

        FlameGraphLayout layout = FlameGraphLayoutBuilder.defaultBuilder().build(root);

        assertEquals(2, layout.frames().size());
        assertEquals(1, layout.maxDepth());

        FlameGraphFrame frameA = layout.frames().getFirst();
        assertEquals("A", frameA.method());
        assertEquals(60, frameA.count());
        assertEquals(0, frameA.depth());
        assertEquals(0, frameA.x(), DELTA);
        assertEquals(0.6, frameA.width(), DELTA);

        FlameGraphFrame frameB = layout.frames().get(1);
        assertEquals("B", frameB.method());
        assertEquals(40, frameB.count());
        assertEquals(0, frameB.depth());
        assertEquals(0.6, frameB.x(), DELTA);
        assertEquals(0.4, frameB.width(), DELTA);
    }

    @Test
    void preservesChildPositionInsideParentWidth() {
        StackTreeNode root = node("root", 100, node("A", 80, node("child", 40)));

        FlameGraphLayout layout = FlameGraphLayoutBuilder.defaultBuilder().build(root);

        FlameGraphFrame child = layout.frames().stream()
                .filter(frame -> frame.method().equals("child"))
                .findFirst()
                .orElseThrow();
        assertEquals(1, child.depth());
        assertEquals(0, child.x(), DELTA);
        assertEquals(0.4, child.width(), DELTA);
    }

    @Test
    void limitsDepthAndFrameCount() {
        StackTreeNode root = nestedTree(12);

        FlameGraphLayout layout = new FlameGraphLayoutBuilder(4, 8).build(root);

        assertTrue(layout.maxDepth() <= 4);
        assertTrue(layout.frames().size() <= 8);
    }

    @Test
    void skipsNonPositiveChildCounts() {
        StackTreeNode root = node("root", 10, node("zero", 0), node("negative", -5), node("positive", 10));

        FlameGraphLayout layout = FlameGraphLayoutBuilder.defaultBuilder().build(root);

        assertEquals(1, layout.frames().size());
        assertEquals("positive", layout.frames().getFirst().method());
    }

    @Test
    void rootWithNullChildrenProducesEmptyLayout() {
        StackTreeNode root = new StackTreeNode("root", 100, 0, null);

        FlameGraphLayout layout = FlameGraphLayoutBuilder.defaultBuilder().build(root);

        assertSame(FlameGraphLayout.EMPTY, layout);
    }

    @Test
    void childWithNullChildrenStillRendersFrame() {
        StackTreeNode root = node("root", 100, new StackTreeNode("child", 40, 0, null));

        FlameGraphLayout layout = FlameGraphLayoutBuilder.defaultBuilder().build(root);

        assertEquals(1, layout.frames().size());
        assertEquals("child", layout.frames().getFirst().method());
    }

    @Test
    void normalizesOverfullSiblingsWithinParentWidth() {
        StackTreeNode root = node("root", 100, node("A", 80), node("B", 80));

        FlameGraphLayout layout = FlameGraphLayoutBuilder.defaultBuilder().build(root);

        assertEquals(2, layout.frames().size());

        FlameGraphFrame frameA = layout.frames().getFirst();
        assertEquals("A", frameA.method());
        assertEquals(0, frameA.x(), DELTA);
        assertEquals(0.5, frameA.width(), DELTA);

        FlameGraphFrame frameB = layout.frames().get(1);
        assertEquals("B", frameB.method());
        assertEquals(0.5, frameB.x(), DELTA);
        assertEquals(0.5, frameB.width(), DELTA);
    }

    @Test
    void maxFramesPreservesCurrentDepthSiblingsBeforeDescendants() {
        StackTreeNode root = node("root", 100,
                node("A", 40, node("A1", 40, node("A2", 40))),
                node("B", 30),
                node("C", 20));

        FlameGraphLayout layout = new FlameGraphLayoutBuilder(8, 3).build(root);

        assertEquals(3, layout.frames().size());
        assertEquals(List.of("A", "B", "C"), layout.frames().stream()
                .map(FlameGraphFrame::method)
                .toList());
        assertTrue(layout.frames().stream().allMatch(frame -> frame.depth() == 0));
    }

    @Test
    void emptyTreeProducesEmptyLayout() {
        FlameGraphLayout layout = FlameGraphLayoutBuilder.defaultBuilder().build(StackTreeNode.EMPTY);

        assertSame(FlameGraphLayout.EMPTY, layout);
    }

    private static StackTreeNode nestedTree(int depth) {
        StackTreeNode child = node("leaf", 100);
        for (int index = depth; index > 0; index--) {
            child = node("node-" + index, 100, child);
        }
        return node("root", 100, child);
    }

    private static StackTreeNode node(String method, int count, StackTreeNode... children) {
        return new StackTreeNode(method, count, 0, List.of(children));
    }
}
