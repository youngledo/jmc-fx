package com.youngledo.jmcfx.ui.profiling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.shape.Line;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CallGraphViewTest {

    private static final double DELTA = 0.000001;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await(5, TimeUnit.SECONDS);
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized by another test class.
        }
    }

    @Test
    void rendersOneNodePerGraphNodeAndEdges() {
        CallGraphView view = new CallGraphView();

        view.setLayout(sampleLayout());

        assertEquals(2, view.nodeCount());
        assertEquals(1, view.edgeCount());
        assertTrue(view.getStyleClass().contains("call-graph-view"));
        assertEquals(2, countByStyleClass(view, "call-graph-node"));
        assertEquals(1, countByStyleClass(view, "call-graph-edge"));
    }

    @Test
    void nullLayoutShowsEmptyTextWithoutGraphNodes() {
        CallGraphView view = new CallGraphView();
        view.setEmptyText("Select a method to view the call graph.");

        view.setLayout(null);

        assertEquals(0, view.nodeCount());
        Label label = (Label) firstByStyleClass(view, "call-graph-empty");
        assertEquals("Select a method to view the call graph.", label.getText());
        assertEquals(0, countByStyleClass(view, "call-graph-node"));
        assertEquals(0, countByStyleClass(view, "call-graph-edge"));
    }

    @Test
    void nullLayoutShowsEmptyLabelWhenEmptyTextIsBlank() {
        CallGraphView view = new CallGraphView();

        view.setLayout(null);

        Label label = (Label) firstByStyleClass(view, "call-graph-empty");
        assertEquals("", label.getText());
        assertEquals(1, view.getChildren().size());
        assertEquals(0, countByStyleClass(view, "call-graph-node"));
        assertEquals(0, countByStyleClass(view, "call-graph-edge"));
    }

    @Test
    void labelsUseEllipsis() {
        CallGraphView view = new CallGraphView();

        view.setLayout(new CallGraphLayout(List.of(
                new CallGraphNode("selected", "Very long method name that should be clipped",
                        100, 100, 0, 0.5, 0, true)), List.of(), 0));

        Label label = (Label) firstByStyleClass(view, "call-graph-node-label");

        assertEquals(OverrunStyle.ELLIPSIS, label.getTextOverrun());
    }

    @Test
    void primaryNodeUsesPrimaryStyleClass() {
        CallGraphView view = new CallGraphView();

        view.setLayout(sampleLayout());

        Node primary = firstByStyleClass(view, "call-graph-node-primary");
        assertTrue(primary.getStyleClass().contains("call-graph-node"));
        assertEquals(sampleLayout().nodes().getFirst(), primary.getUserData());
    }

    @Test
    void layoutChildrenConnectsEdgeBetweenNodeCenters() {
        CallGraphView view = new CallGraphView();
        view.setLayout(sampleLayout());
        view.resize(400, 260);

        view.layout();

        Line edge = (Line) firstByStyleClass(view, "call-graph-edge");
        Node source = graphNodeById(view, "selected");
        Node target = graphNodeById(view, "node-1");

        assertEquals(centerX(source), edge.getStartX(), DELTA);
        assertEquals(centerY(source), edge.getStartY(), DELTA);
        assertEquals(centerX(target), edge.getEndX(), DELTA);
        assertEquals(centerY(target), edge.getEndY(), DELTA);
    }

    @Test
    void graphLayoutRemovesEmptyLabel() {
        CallGraphView view = new CallGraphView();
        view.setEmptyText("Select a method to view the call graph.");
        view.setLayout(null);

        view.setLayout(sampleLayout());

        assertNull(firstByStyleClass(view, "call-graph-empty"));
        assertEquals(2, countByStyleClass(view, "call-graph-node"));
        assertEquals(1, countByStyleClass(view, "call-graph-edge"));
    }

    @Test
    void cssDefinesCallGraphClasses() throws IOException {
        String css = appCss();

        assertTrue(css.contains(".call-graph-view"));
        assertTrue(css.contains(".call-graph-node"));
        assertTrue(css.contains(".call-graph-node-primary"));
        assertTrue(css.contains(".call-graph-node-label"));
        assertTrue(css.contains(".call-graph-edge"));
        assertTrue(css.contains(".call-graph-empty"));
    }

    private CallGraphLayout sampleLayout() {
        List<CallGraphNode> nodes = List.of(
                new CallGraphNode("selected", "com.example.Service.run", 100, 100, 0, 0.25, 0, true),
                new CallGraphNode("node-1", "com.example.Repository.find", 40, 40, 1, 0.75, 1, false));
        return new CallGraphLayout(nodes, List.of(new CallGraphEdge("selected", "node-1", 40, 40)), 1);
    }

    private int countByStyleClass(Node root, String styleClass) {
        int count = root.getStyleClass().contains(styleClass) ? 1 : 0;
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                count += countByStyleClass(child, styleClass);
            }
        }
        return count;
    }

    private Node firstByStyleClass(Node root, String styleClass) {
        if (root.getStyleClass().contains(styleClass)) {
            return root;
        }
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node match = firstByStyleClass(child, styleClass);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private Node graphNodeById(Node root, String id) {
        if (root.getUserData() instanceof CallGraphNode graphNode && graphNode.id().equals(id)) {
            return root;
        }
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node match = graphNodeById(child, id);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private double centerX(Node node) {
        return node.getLayoutX() + (node.getLayoutBounds().getWidth() / 2);
    }

    private double centerY(Node node) {
        return node.getLayoutY() + (node.getLayoutBounds().getHeight() / 2);
    }

    private String appCss() throws IOException {
        try (InputStream stream = CallGraphViewTest.class.getResourceAsStream("/css/app.css")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
