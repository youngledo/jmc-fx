package com.youngledo.jmcfx.ui.profiling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    void highWeightEdgesUseStrongStyleClass() {
        CallGraphView view = new CallGraphView();

        view.setLayout(wideLayout());

        assertEquals(2, countByStyleClass(view, "call-graph-edge-strong"));
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
    void graphUsesScrollableCanvasLargerThanViewport() {
        CallGraphView view = new CallGraphView();
        view.setLayout(wideLayout());
        view.resize(320, 160);

        view.layout();

        assertTrue(view.prefWidth(-1) > 320);
        assertTrue(view.prefHeight(-1) > 220);
    }

    @Test
    void zoomControlsScaleGraphCanvas() {
        CallGraphView view = new CallGraphView();
        view.setLayout(sampleLayout());

        view.zoomIn();

        assertTrue(view.zoomScaleProperty().get() > 1.0);
        assertTrue(view.prefWidth(-1) > 0);

        view.zoomOut();
        view.resetZoom();

        assertEquals(1.0, view.zoomScaleProperty().get(), DELTA);
    }

    @Test
    void proportionalZoomScalesGraphCanvasForGestures() {
        CallGraphView view = new CallGraphView();
        view.setLayout(sampleLayout());
        double basePrefWidth = view.prefWidth(-1);
        double basePrefHeight = view.prefHeight(-1);

        view.zoomBy(1.5);

        assertEquals(1.5, view.zoomScaleProperty().get(), DELTA);
        assertEquals(basePrefWidth * 1.5, view.prefWidth(-1), DELTA);
        assertEquals(basePrefHeight * 1.5, view.prefHeight(-1), DELTA);

        view.zoomBy(0);

        assertEquals(1.5, view.zoomScaleProperty().get(), DELTA);
    }

    @Test
    void fitToWidthScalesLargeGraphDownWithoutUpscaling() {
        CallGraphView view = new CallGraphView();
        view.setLayout(wideLayout());

        view.fitToWidth(360);

        assertTrue(view.zoomScaleProperty().get() < 1.0);

        CallGraphView small = new CallGraphView();
        small.setLayout(sampleLayout());
        small.fitToWidth(2000);

        assertEquals(1.0, small.zoomScaleProperty().get(), DELTA);
    }

    @Test
    void cssDefinesCallGraphClasses() throws IOException {
        String css = appCss();

        assertTrue(css.contains(".call-graph-view"));
        assertTrue(css.contains(".call-graph-node"));
        assertTrue(css.contains(".call-graph-node-primary"));
        assertTrue(css.contains(".call-graph-node-label"));
        assertTrue(css.contains(".call-graph-edge"));
        assertTrue(css.contains(".call-graph-edge-strong"));
        assertTrue(css.contains(".call-graph-node:hover"));
        assertTrue(css.contains(".call-graph-empty"));
        assertTrue(css.contains(".profiling-graph-tab-content"));
        assertTrue(css.contains(".profiling-graph-toolbar"));

        String nodeBlock = cssBlock(css, ".call-graph-node");
        assertTrue(nodeBlock.contains("-fx-background-radius: 2px"));
        assertTrue(nodeBlock.contains("-fx-border-width: 1px"));

        String edgeBlock = cssBlock(css, ".call-graph-edge");
        assertTrue(edgeBlock.contains("-fx-opacity: 0.72"));

        String strongEdgeBlock = cssBlock(css, ".call-graph-edge-strong");
        assertTrue(strongEdgeBlock.contains("-fx-stroke-width: 1.75px"));
    }

    private CallGraphLayout sampleLayout() {
        List<CallGraphNode> nodes = List.of(
                new CallGraphNode("selected", "com.example.Service.run", 100, 100, 0, 0.25, 0, true),
                new CallGraphNode("node-1", "com.example.Repository.find", 40, 40, 1, 0.75, 1, false));
        return new CallGraphLayout(nodes, List.of(new CallGraphEdge("selected", "node-1", 40, 40)), 1);
    }


    private CallGraphLayout wideLayout() {
        List<CallGraphNode> nodes = List.of(
                new CallGraphNode("selected", "selected", 100, 100, 0, 0.5, 0, true),
                new CallGraphNode("node-1", "a", 80, 80, 1, 0.1, 0.5, false),
                new CallGraphNode("node-2", "b", 70, 70, 1, 0.5, 0.5, false),
                new CallGraphNode("node-3", "c", 60, 60, 1, 0.9, 0.5, false),
                new CallGraphNode("node-4", "d", 50, 50, 2, 0.2, 1, false),
                new CallGraphNode("node-5", "e", 40, 40, 2, 0.8, 1, false));
        return new CallGraphLayout(nodes, List.of(
                new CallGraphEdge("selected", "node-1", 80, 80),
                new CallGraphEdge("selected", "node-2", 70, 70),
                new CallGraphEdge("selected", "node-3", 60, 60),
                new CallGraphEdge("node-1", "node-4", 50, 50),
                new CallGraphEdge("node-3", "node-5", 40, 40)), 2);
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

    private String cssBlock(String css, String selector) {
        int start = css.indexOf(selector + " {");
        if (start < 0) {
            return "";
        }
        int end = css.indexOf('}', start);
        return end < 0 ? css.substring(start) : css.substring(start, end);
    }
}
