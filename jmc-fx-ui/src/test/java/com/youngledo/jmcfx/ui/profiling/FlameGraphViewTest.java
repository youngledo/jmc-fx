package com.youngledo.jmcfx.ui.profiling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import javafx.scene.transform.Scale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FlameGraphViewTest {

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
    void rendersOneNodePerFrame() {
        FlameGraphView view = new FlameGraphView();
        view.setLayout(sampleLayout());

        assertEquals(2, view.frameCount());
        assertTrue(view.hasFramesProperty().get());
        assertTrue(view.getStyleClass().contains("flame-graph-view"));
    }

    @Test
    void settingLayoutTwiceClearsOldFrameNodes() {
        FlameGraphView view = new FlameGraphView();
        view.setLayout(sampleLayout());

        view.setLayout(new FlameGraphLayout(List.of(
                new FlameGraphFrame("C", 40, 40, 0, 0, 1)), 1));

        assertEquals(1, view.frameCount());
        assertEquals(1, view.getChildren().size());
    }

    @Test
    void nullLayoutClearsFrames() {
        FlameGraphView view = new FlameGraphView();
        view.setLayout(sampleLayout());

        view.setLayout(null);

        assertEquals(0, view.frameCount());
        assertTrue(!view.hasFramesProperty().get());
        assertTrue(view.getChildren().isEmpty());
    }

    @Test
    void emptyLayoutShowsPlaceholderWithoutCountingFrameNodes() {
        FlameGraphView view = new FlameGraphView();
        view.setEmptyText("Select a method to view the flame graph.");

        view.setLayout(null);

        assertEquals(0, view.frameCount());
        assertEquals(1, view.getChildren().size());
        Label label = (Label) firstByStyleClass(view, "flame-graph-empty");
        assertEquals("Select a method to view the flame graph.", label.getText());
        assertTrue(label.getStyleClass().contains("flame-graph-empty"));
    }

    @Test
    void frameLayoutRemovesPlaceholder() {
        FlameGraphView view = new FlameGraphView();
        view.setEmptyText("Select a method to view the flame graph.");
        view.setLayout(null);

        view.setLayout(sampleLayout());

        assertEquals(2, view.frameCount());
        assertEquals(2, view.getChildren().size());
        assertEquals(null, firstByStyleClass(view, "flame-graph-empty"));
    }

    @Test
    void labelsUseEllipsisToAvoidOverflow() {
        FlameGraphView view = new FlameGraphView();
        view.setLayout(new FlameGraphLayout(List.of(
                new FlameGraphFrame("Very long method name that should be clipped", 100, 100, 0, 0, 1)), 1));

        Label label = (Label) firstByStyleClass(view, "flame-graph-frame-label");

        assertEquals(OverrunStyle.ELLIPSIS, label.getTextOverrun());
        assertTrue(label.getStyleClass().contains("flame-graph-frame-label"));
    }

    @Test
    void laysOutFramesFromNormalizedCoordinatesWhenWidthChanges() {
        FlameGraphView view = new FlameGraphView();
        view.setLayout(sampleLayout());
        view.resize(200, view.prefHeight(-1));

        view.layout();

        Node secondFrame = view.getChildren().get(1);
        assertEquals(100, secondFrame.getLayoutX(), 0.000001);
        assertEquals(100, secondFrame.prefWidth(-1), 0.000001);
    }


    @Test
    void zoomControlsScaleFlameGraphCanvas() {
        FlameGraphView view = new FlameGraphView();
        view.setLayout(sampleLayout());

        view.zoomIn();

        assertTrue(view.zoomScaleProperty().get() > 1.0);
        assertEquals(view.zoomScaleProperty().get(), graphScale(view).getX(), 0.000001);

        view.zoomOut();
        view.resetZoom();

        assertEquals(1.0, view.zoomScaleProperty().get(), 0.000001);
    }

    @Test
    void fitToWidthScalesWideFlameGraphWithoutUpscaling() {
        FlameGraphView view = new FlameGraphView();
        view.setLayout(new FlameGraphLayout(List.of(
                new FlameGraphFrame("wide", 100, 100, 0, 0, 1)), 1));

        view.fitToWidth(320);

        assertTrue(view.zoomScaleProperty().get() < 1.0);

        view.fitToWidth(2000);

        assertEquals(1.0, view.zoomScaleProperty().get(), 0.000001);
    }

    @Test
    void canSwitchBetweenIcicleAndFlameOrientation() {
        FlameGraphView view = new FlameGraphView();
        view.setLayout(new FlameGraphLayout(List.of(
                new FlameGraphFrame("root", 100, 100, 0, 0, 1),
                new FlameGraphFrame("child", 50, 50, 1, 0, 0.5)), 2));
        view.resize(400, view.prefHeight(-1));
        view.layout();
        double icicleChildY = view.getChildren().get(1).getLayoutY();

        view.setOrientation(FlameGraphView.Orientation.FLAME);
        view.layout();

        assertTrue(view.getChildren().get(1).getLayoutY() < icicleChildY);
        assertEquals(FlameGraphView.Orientation.FLAME, view.orientationProperty().get());
    }

    @Test
    void framesExposeAccessibleTextWithMethodCountAndPercentage() {
        FlameGraphView view = new FlameGraphView();
        view.setLayout(sampleLayout());

        String accessibleText = view.getChildren().getFirst().getAccessibleText();

        assertNotNull(accessibleText);
        assertTrue(accessibleText.contains("A"));
        assertTrue(accessibleText.contains("60"));
        assertTrue(accessibleText.contains("60.0%"));
    }

    @Test
    void cssDefinesFlameGraphClasses() throws IOException {
        String css = appCss();

        assertTrue(css.contains(".flame-graph-view"));
        assertTrue(css.contains(".flame-graph-frame"));
        assertTrue(css.contains(".flame-graph-frame-label"));
        assertTrue(css.contains(".flame-graph-empty"));
        assertTrue(css.contains(".profiling-graph-tab-content"));
        assertTrue(css.contains(".profiling-graph-toolbar"));
    }

    private FlameGraphLayout sampleLayout() {
        return new FlameGraphLayout(List.of(
                new FlameGraphFrame("A", 60, 60, 0, 0, 0.5),
                new FlameGraphFrame("B", 40, 40, 0, 0.5, 0.5)), 1);
    }


    private Scale graphScale(FlameGraphView view) {
        return view.getTransforms().stream()
                .filter(Scale.class::isInstance)
                .map(Scale.class::cast)
                .findFirst()
                .orElseThrow();
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

    private String appCss() throws IOException {
        try (InputStream stream = FlameGraphViewTest.class.getResourceAsStream("/css/app.css")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
