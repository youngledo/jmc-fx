package com.youngledo.jmcfx.flamegraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class FlameGraphLayoutEngineTest {

    private static final double DELTA = 0.000001;

    @Test
    void convertsTreeToNormalizedFrames() {
        FlameGraphNode<String> root = node("root", 100, node("A", 60), node("B", 40));

        FlameGraphLayout<String> layout = new FlameGraphLayoutEngine<String>()
                .layout(FlameGraphModel.of(root), FlameGraphMode.ICICLE);

        assertEquals(2, layout.frames().size());
        assertEquals(1, layout.maxDepth());

        FlameGraphFrame<String> frameA = layout.frames().getFirst();
        assertEquals("A", frameA.node().label());
        assertEquals(60, frameA.node().weight(), DELTA);
        assertEquals(0, frameA.depth());
        assertEquals(0, frameA.x(), DELTA);
        assertEquals(0.6, frameA.width(), DELTA);
        assertEquals(0, frameA.row());

        FlameGraphFrame<String> frameB = layout.frames().get(1);
        assertEquals("B", frameB.node().label());
        assertEquals(0.6, frameB.x(), DELTA);
        assertEquals(0.4, frameB.width(), DELTA);
    }

    @Test
    void sortsSiblingsByDescendingWeight() {
        FlameGraphNode<String> root = node("root", 100,
                node("small", 20),
                node("large", 70),
                node("medium", 30));

        FlameGraphLayout<String> layout = new FlameGraphLayoutEngine<String>()
                .layout(FlameGraphModel.of(root), FlameGraphMode.ICICLE);

        assertEquals(List.of("large", "medium", "small"), layout.frames().stream()
                .map(frame -> frame.node().label())
                .toList());
    }

    @Test
    void normalizesOverfullSiblingsWithinParentWidth() {
        FlameGraphNode<String> root = node("root", 100, node("A", 80), node("B", 80));

        FlameGraphLayout<String> layout = new FlameGraphLayoutEngine<String>()
                .layout(FlameGraphModel.of(root), FlameGraphMode.ICICLE);

        FlameGraphFrame<String> frameA = layout.frames().getFirst();
        assertEquals("A", frameA.node().label());
        assertEquals(0, frameA.x(), DELTA);
        assertEquals(0.5, frameA.width(), DELTA);

        FlameGraphFrame<String> frameB = layout.frames().get(1);
        assertEquals("B", frameB.node().label());
        assertEquals(0.5, frameB.x(), DELTA);
        assertEquals(0.5, frameB.width(), DELTA);
    }

    @Test
    void singleChildStackFillsAvailableWidthAtEveryDepth() {
        FlameGraphNode<String> root = node("root", 100,
                node("parent", 80,
                        node("child", 60,
                                node("grandchild", 30))));

        FlameGraphLayout<String> layout = new FlameGraphLayoutEngine<String>()
                .layout(FlameGraphModel.of(root), FlameGraphMode.ICICLE);

        assertEquals(List.of("parent", "child", "grandchild"), layout.frames().stream()
                .map(frame -> frame.node().label())
                .toList());
        for (FlameGraphFrame<String> frame : layout.frames()) {
            assertEquals(0, frame.x(), DELTA, frame.node().label());
            assertEquals(1, frame.width(), DELTA, frame.node().label());
        }
    }

    @Test
    void siblingChildrenFillTheirParentWidthWithoutSelfTimeGap() {
        FlameGraphNode<String> root = node("root", 100,
                node("parent", 80,
                        node("child", 30),
                        node("sibling", 10)));

        FlameGraphLayout<String> layout = new FlameGraphLayoutEngine<String>()
                .layout(FlameGraphModel.of(root), FlameGraphMode.ICICLE);

        FlameGraphFrame<String> parent = layout.frames().stream()
                .filter(frame -> frame.node().label().equals("parent"))
                .findFirst()
                .orElseThrow();
        FlameGraphFrame<String> child = layout.frames().stream()
                .filter(frame -> frame.node().label().equals("child"))
                .findFirst()
                .orElseThrow();
        FlameGraphFrame<String> sibling = layout.frames().stream()
                .filter(frame -> frame.node().label().equals("sibling"))
                .findFirst()
                .orElseThrow();

        assertEquals(1, parent.width(), DELTA);
        assertEquals(0, child.x(), DELTA);
        assertEquals(0.75, child.width(), DELTA);
        assertEquals(0.75, sibling.x(), DELTA);
        assertEquals(0.25, sibling.width(), DELTA);
    }

    @Test
    void limitsDepthAndFrameCount() {
        FlameGraphNode<String> root = nestedTree(12);
        FlameGraphModel<String> model = new FlameGraphModel<>(root, 4, 8);

        FlameGraphLayout<String> layout = new FlameGraphLayoutEngine<String>()
                .layout(model, FlameGraphMode.ICICLE);

        assertTrue(layout.maxDepth() <= 4);
        assertTrue(layout.frames().size() <= 8);
    }

    @Test
    void frameLimitKeepsUpperFramesAcrossWideBranches() {
        FlameGraphNode<String> root = node("root", 100,
                node("left", 50,
                        node("left-child", 50,
                                node("left-grandchild", 50))),
                node("right", 50,
                        node("right-child", 50,
                                node("right-grandchild", 50))));
        FlameGraphModel<String> model = new FlameGraphModel<>(root, 8, 4);

        FlameGraphLayout<String> layout = new FlameGraphLayoutEngine<String>()
                .layout(model, FlameGraphMode.ICICLE);

        assertEquals(List.of("left", "right", "left-child", "right-child"), layout.frames().stream()
                .map(frame -> frame.node().label())
                .toList());
        FlameGraphFrame<String> rightChild = layout.frames().stream()
                .filter(frame -> frame.node().label().equals("right-child"))
                .findFirst()
                .orElseThrow();
        assertEquals(0.5, rightChild.x(), DELTA);
        assertEquals(0.5, rightChild.width(), DELTA);
    }

    @Test
    void skipsNonPositiveChildren() {
        FlameGraphNode<String> root = node("root", 10,
                node("zero", 0),
                node("negative", -5),
                node("positive", 10));

        FlameGraphLayout<String> layout = new FlameGraphLayoutEngine<String>()
                .layout(FlameGraphModel.of(root), FlameGraphMode.ICICLE);

        assertEquals(1, layout.frames().size());
        assertEquals("positive", layout.frames().getFirst().node().label());
    }

    @Test
    void emptyTreeProducesEmptyLayout() {
        FlameGraphLayout<String> layout = new FlameGraphLayoutEngine<String>()
                .layout(FlameGraphModel.empty(), FlameGraphMode.ICICLE);

        assertTrue(layout.frames().isEmpty());
        assertEquals(0, layout.maxDepth());
    }

    @Test
    void positionsFlameRowsFromBottom() {
        FlameGraphNode<String> root = node("root", 100, node("A", 80, node("child", 40)));

        FlameGraphLayout<String> icicle = new FlameGraphLayoutEngine<String>()
                .layout(FlameGraphModel.of(root), FlameGraphMode.ICICLE);
        FlameGraphLayout<String> flame = new FlameGraphLayoutEngine<String>()
                .layout(FlameGraphModel.of(root), FlameGraphMode.FLAME);

        FlameGraphFrame<String> icicleParent = icicle.frames().getFirst();
        FlameGraphFrame<String> icicleChild = icicle.frames().get(1);
        FlameGraphFrame<String> flameParent = flame.frames().getFirst();
        FlameGraphFrame<String> flameChild = flame.frames().get(1);

        assertEquals(0, icicleParent.row());
        assertEquals(1, icicleChild.row());
        assertEquals(1, flameParent.row());
        assertEquals(0, flameChild.row());
    }

    @Test
    void frameAtFindsFrameByNormalizedCoordinateAndDepth() {
        FlameGraphNode<String> root = node("root", 100, node("A", 60), node("B", 40));
        FlameGraphLayout<String> layout = new FlameGraphLayoutEngine<String>()
                .layout(FlameGraphModel.of(root), FlameGraphMode.ICICLE);

        assertEquals("A", layout.frameAt(0.3, 0).orElseThrow().node().label());
        assertEquals("B", layout.frameAt(0.7, 0).orElseThrow().node().label());
        assertTrue(layout.frameAt(0.7, 1).isEmpty());
    }

    @Test
    void icicleFocusKeepsFocusedFrameAndDescendantsOnly() {
        FlameGraphNode<String> root = node("root", 100,
                node("parent", 80,
                        node("child", 60, node("grandchild", 30)),
                        node("child-sibling", 20)),
                node("sibling", 20));
        FlameGraphLayoutEngine<String> engine = new FlameGraphLayoutEngine<>();
        FlameGraphLayout<String> layout = engine.layout(FlameGraphModel.of(root), FlameGraphMode.ICICLE);
        FlameGraphFrame<String> child = layout.frames().stream()
                .filter(frame -> frame.node().label().equals("child"))
                .findFirst()
                .orElseThrow();

        FlameGraphLayout<String> focused = engine.focusedLayout(
                FlameGraphModel.of(root),
                FlameGraphMode.ICICLE,
                child);

        assertEquals(List.of("child", "grandchild"), focused.frames().stream()
                .map(frame -> frame.node().label())
                .toList());
        FlameGraphFrame<String> focusedChild = focused.frames().get(0);
        FlameGraphFrame<String> focusedGrandchild = focused.frames().get(1);
        assertEquals(0, focusedChild.depth());
        assertEquals(1, focusedGrandchild.depth());
        assertEquals(0, focusedChild.row());
        assertEquals(1, focusedGrandchild.row());
        assertEquals(0, focusedChild.x(), DELTA);
        assertEquals(1, focusedChild.width(), DELTA);
        assertEquals(0, focusedGrandchild.x(), DELTA);
        assertEquals(1, focusedGrandchild.width(), DELTA);
    }

    @Test
    void flameFocusKeepsFocusedFrameAndDescendantsOnly() {
        FlameGraphNode<String> root = node("root", 100,
                node("parent", 80,
                        node("child", 60, node("grandchild", 30)),
                        node("child-sibling", 20)),
                node("sibling", 20));
        FlameGraphLayoutEngine<String> engine = new FlameGraphLayoutEngine<>();
        FlameGraphLayout<String> layout = engine.layout(FlameGraphModel.of(root), FlameGraphMode.FLAME);
        FlameGraphFrame<String> child = layout.frames().stream()
                .filter(frame -> frame.node().label().equals("child"))
                .findFirst()
                .orElseThrow();

        FlameGraphLayout<String> focused = engine.focusedLayout(
                FlameGraphModel.of(root),
                FlameGraphMode.FLAME,
                child);

        assertEquals(List.of("child", "grandchild"), focused.frames().stream()
                .map(frame -> frame.node().label())
                .toList());
        FlameGraphFrame<String> focusedChild = focused.frames().get(0);
        FlameGraphFrame<String> focusedGrandchild = focused.frames().get(1);
        assertEquals(0, focusedChild.depth());
        assertEquals(1, focusedGrandchild.depth());
        assertEquals(1, focusedChild.row());
        assertEquals(0, focusedGrandchild.row());
        assertEquals(0, focusedChild.x(), DELTA);
        assertEquals(1, focusedChild.width(), DELTA);
        assertEquals(0, focusedGrandchild.x(), DELTA);
        assertEquals(1, focusedGrandchild.width(), DELTA);
    }

    @Test
    void focusedFrameResolvesFromOriginalPath() {
        FlameGraphNode<String> root = node("root", 100, node("A", 80, node("child", 40)));
        FlameGraphLayoutEngine<String> engine = new FlameGraphLayoutEngine<>();
        FlameGraphLayout<String> layout = engine.layout(FlameGraphModel.of(root), FlameGraphMode.ICICLE);
        FlameGraphFrame<String> child = layout.frames().get(1);

        FlameGraphLayout<String> focused = engine.focusedLayout(FlameGraphModel.of(root), FlameGraphMode.ICICLE, child);

        assertSame(child.node(), focused.frames().stream()
                .filter(frame -> frame.path().equals(child.path()))
                .findFirst()
                .orElseThrow()
                .node());
    }

    private static FlameGraphNode<String> nestedTree(int depth) {
        FlameGraphNode<String> child = node("leaf", 100);
        for (int index = depth; index > 0; index--) {
            child = node("node-" + index, 100, child);
        }
        return node("root", 100, child);
    }

    @SafeVarargs
    private static FlameGraphNode<String> node(String label, double weight, FlameGraphNode<String>... children) {
        return new FlameGraphNode<>(label, weight, 0, label, List.of(children));
    }
}
