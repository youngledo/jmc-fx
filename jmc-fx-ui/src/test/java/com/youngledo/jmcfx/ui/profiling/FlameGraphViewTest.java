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
import javafx.scene.control.Tooltip;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.StackFrameInfo;

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
    void framesReceiveBoundedDepthStyleClasses() {
        FlameGraphView view = new FlameGraphView();
        view.setLayout(new FlameGraphLayout(List.of(
                new FlameGraphFrame("root", 100, 100, 0, 0, 1),
                new FlameGraphFrame("child", 60, 60, 1, 0, 0.6),
                new FlameGraphFrame("wrapped", 10, 10, 9, 0, 0.1)), 10));

        assertTrue(view.getChildren().get(0).getStyleClass().contains("flame-graph-depth-0"));
        assertTrue(view.getChildren().get(1).getStyleClass().contains("flame-graph-depth-1"));
        assertTrue(view.getChildren().get(2).getStyleClass().contains("flame-graph-depth-1"));
    }

    @Test
    void cssDefinesReadableLabelContrastByOrientationAndDepth() throws IOException {
        String css = appCss();

        assertCssTextFill(css,
                ".flame-graph-orientation-flame .flame-graph-depth-0 .flame-graph-frame-label",
                "#ffffff");
        assertCssTextFill(css,
                ".flame-graph-orientation-flame .flame-graph-depth-2 .flame-graph-frame-label",
                "#1f2328");
        assertCssTextFill(css,
                ".flame-graph-orientation-icicle .flame-graph-depth-0 .flame-graph-frame-label",
                "#ffffff");
        assertCssTextFill(css,
                ".flame-graph-orientation-icicle .flame-graph-depth-6 .flame-graph-frame-label",
                "#1f2328");
    }

    @Test
    void framesUseDepthClassesWithoutLabelContrastClasses() {
        FlameGraphView view = new FlameGraphView();
        view.setLayout(new FlameGraphLayout(List.of(
                new FlameGraphFrame("dark", 100, 100, 0, 0, 0.5),
                new FlameGraphFrame("light", 40, 40, 7, 0.5, 0.5)), 8));

        assertTrue(view.getChildren().get(0).getStyleClass().contains("flame-graph-depth-0"));
        assertTrue(view.getChildren().get(1).getStyleClass().contains("flame-graph-depth-7"));
        assertTrue(!view.getChildren().get(0).getStyleClass().contains("flame-graph-label-light"));
        assertTrue(!view.getChildren().get(0).getStyleClass().contains("flame-graph-label-dark"));
        assertTrue(!view.getChildren().get(1).getStyleClass().contains("flame-graph-label-light"));
        assertTrue(!view.getChildren().get(1).getStyleClass().contains("flame-graph-label-dark"));
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
    void frameLabelsUseReadableMethodNamesInsteadOfFullPackages() {
        FlameGraphView view = new FlameGraphView();
        view.setLayout(new FlameGraphLayout(List.of(
                new FlameGraphFrame("com.example.deep.Service.process(java.lang.String)", 100, 100, 0, 0, 1)), 1));

        Label label = (Label) firstByStyleClass(view, "flame-graph-frame-label");

        assertEquals("Service.process(java.lang.String)", label.getText());
    }

    @Test
    void frameLabelsPreferStructuredFrameInfo() {
        FlameGraphView view = new FlameGraphView();
        StackFrameInfo info = new StackFrameInfo(
                "Worker.run",
                "run",
                "com.example",
                "com.example.Worker",
                "JIT compiled",
                42,
                128);
        view.setLayout(new FlameGraphLayout(List.of(
                new FlameGraphFrame("com.example.Worker.run()", 80, 80, info, 0, 0, 1)), 1));

        Label label = (Label) firstByStyleClass(view, "flame-graph-frame-label");

        assertEquals("Worker.run", label.getText());
    }

    @Test
    void frameTooltipShowsStructuredJmcFrameDetails() {
        FlameGraphView view = new FlameGraphView();
        StackFrameInfo info = new StackFrameInfo(
                "Worker.run",
                "run",
                "com.example",
                "com.example.Worker",
                "JIT compiled",
                42,
                128);
        view.setLayout(new FlameGraphLayout(List.of(
                new FlameGraphFrame("com.example.Worker.run()", 80, 80, info, 0, 0, 1)), 1));

        Node frameNode = view.getChildren().getFirst();
        String details = installedTooltip(frameNode).getText();

        assertEquals(details, frameNode.getAccessibleText());
        assertTrue(details.startsWith("Worker.run"));
        assertTrue(details.contains("Package: com.example"));
        assertTrue(details.contains("Class: com.example.Worker"));
        assertTrue(details.contains("Weight: 80 samples, 80.0%"));
        assertTrue(details.contains("Type: JIT compiled"));
        assertTrue(details.contains("Byte Code Index: 42"));
        assertTrue(details.contains("Line: 128"));
    }

    @Test
    void frameTooltipDoesNotAutoHideWhileHoveringAndUsesScopedSmallText() {
        FlameGraphView view = new FlameGraphView();
        view.setLayout(sampleLayout());

        Tooltip tooltip = installedTooltip(view.getChildren().getFirst());

        assertEquals(Duration.INDEFINITE, tooltip.getShowDuration());
        assertTrue(tooltip.getStyleClass().contains("flame-graph-tooltip"));
    }

    @Test
    void clickingFrameDimsUnrelatedFramesAndKeepsStackPathActive() {
        FlameGraphView view = new FlameGraphView();
        view.setLayout(new FlameGraphLayout(List.of(
                new FlameGraphFrame("parent", 80, 80, 0, 0, 0.8),
                new FlameGraphFrame("sibling", 20, 20, 0, 0.8, 0.2),
                new FlameGraphFrame("child", 60, 60, 1, 0, 0.6),
                new FlameGraphFrame("child-sibling", 20, 20, 1, 0.6, 0.2),
                new FlameGraphFrame("grandchild", 30, 30, 2, 0.1, 0.3)), 3));

        view.getChildren().get(2).getOnMouseClicked().handle(null);

        assertTrue(view.getChildren().get(0).getStyleClass().contains("flame-graph-frame-path"));
        assertTrue(view.getChildren().get(1).getStyleClass().contains("flame-graph-frame-muted"));
        assertTrue(view.getChildren().get(2).getStyleClass().contains("flame-graph-frame-selected"));
        assertTrue(view.getChildren().get(3).getStyleClass().contains("flame-graph-frame-muted"));
        assertTrue(view.getChildren().get(4).getStyleClass().contains("flame-graph-frame-path"));
        assertTrue(!view.getChildren().get(0).getStyleClass().contains("flame-graph-frame-muted"));
        assertTrue(!view.getChildren().get(2).getStyleClass().contains("flame-graph-frame-muted"));
        assertTrue(!view.getChildren().get(4).getStyleClass().contains("flame-graph-frame-muted"));
    }

    @Test
    void clearSelectionClearsFrameDimming() {
        FlameGraphView view = new FlameGraphView();
        view.setLayout(new FlameGraphLayout(List.of(
                new FlameGraphFrame("parent", 80, 80, 0, 0, 0.8),
                new FlameGraphFrame("sibling", 20, 20, 0, 0.8, 0.2),
                new FlameGraphFrame("child", 60, 60, 1, 0, 0.6)), 2));

        view.getChildren().get(2).getOnMouseClicked().handle(null);
        view.clearSelection();

        assertTrue(view.getChildren().stream()
                .noneMatch(node -> node.getStyleClass().contains("flame-graph-frame-selected")));
        assertTrue(view.getChildren().stream()
                .noneMatch(node -> node.getStyleClass().contains("flame-graph-frame-path")));
        assertTrue(view.getChildren().stream()
                .noneMatch(node -> node.getStyleClass().contains("flame-graph-frame-muted")));
    }

    @Test
    void doubleClickingFrameFocusesStackInsteadOfClearingSelection() {
        FlameGraphView view = new FlameGraphView();
        FlameGraphFrame selected = new FlameGraphFrame("child", 60, 60, 1, 0.2, 0.6);
        view.setLayout(new FlameGraphLayout(List.of(
                new FlameGraphFrame("parent", 80, 80, 0, 0, 0.8),
                new FlameGraphFrame("sibling", 20, 20, 0, 0.8, 0.2),
                selected,
                new FlameGraphFrame("child-sibling", 20, 20, 1, 0.8, 0.2),
                new FlameGraphFrame("grandchild", 30, 30, 2, 0.3, 0.3)), 3));

        view.getChildren().get(2).getOnMouseClicked().handle(doubleClickEvent());

        assertEquals(selected, view.getFocusedFrame());
        assertEquals(3, view.frameCount());
        assertEquals("parent", frameLabelAt(view, 0));
        assertEquals("child", frameLabelAt(view, 1));
        assertEquals("grandchild", frameLabelAt(view, 2));
        view.resize(600, view.prefHeight(-1));
        view.layout();
        assertEquals(0, view.getChildren().get(0).getLayoutX(), 0.000001);
        assertEquals(600, view.getChildren().get(0).prefWidth(-1), 0.000001);
        assertEquals(0, view.getChildren().get(1).getLayoutX(), 0.000001);
        assertEquals(600, view.getChildren().get(1).prefWidth(-1), 0.000001);
        assertEquals(300, view.getChildren().get(2).prefWidth(-1), 0.000001);
        assertTrue(view.getChildren().get(1).getStyleClass().contains("flame-graph-frame-selected"));
        assertTrue(!view.getChildren().get(1).getStyleClass().contains("flame-graph-frame-muted"));
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
    void orientationUpdatesPaletteStyleClass() {
        FlameGraphView view = new FlameGraphView();

        assertTrue(view.getStyleClass().contains("flame-graph-orientation-icicle"));
        assertTrue(!view.getStyleClass().contains("flame-graph-orientation-flame"));

        view.setOrientation(FlameGraphView.Orientation.FLAME);

        assertTrue(view.getStyleClass().contains("flame-graph-orientation-flame"));
        assertTrue(!view.getStyleClass().contains("flame-graph-orientation-icicle"));

        view.setOrientation(FlameGraphView.Orientation.ICICLE);

        assertTrue(view.getStyleClass().contains("flame-graph-orientation-icicle"));
        assertTrue(!view.getStyleClass().contains("flame-graph-orientation-flame"));
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
        assertTrue(css.contains(".flame-graph-frame:hover"));
        assertTrue(css.contains(".flame-graph-frame-selected"));
        assertTrue(css.contains(".flame-graph-frame-path"));
        assertTrue(css.contains(".flame-graph-frame-muted"));
        assertTrue(css.contains(".flame-graph-frame-label"));
        assertTrue(css.contains(".flame-graph-tooltip"));
        assertTrue(css.contains("-fx-font-size: 11px"));
        assertTrue(css.contains(".flame-graph-empty"));
        assertTrue(css.contains(".profiling-graph-tab-content"));
        assertTrue(css.contains(".profiling-graph-toolbar"));
    }

    @Test
    void cssDefinesOrientationPalettesAndReadableFrameLabels() throws IOException {
        String css = appCss();

        assertTrue(css.contains(".flame-graph-orientation-flame .flame-graph-depth-0"));
        assertTrue(css.contains(".flame-graph-orientation-flame .flame-graph-depth-7"));
        assertTrue(css.contains(".flame-graph-orientation-icicle .flame-graph-depth-0"));
        assertTrue(css.contains(".flame-graph-orientation-icicle .flame-graph-depth-7"));

        assertTrue(css.contains("-fx-background-color: #b42318"));
        assertTrue(css.contains("-fx-background-color: #ffd166"));
        assertTrue(css.contains("-fx-background-color: #1f4e79"));
        assertTrue(css.contains("-fx-background-color: #b197fc"));

        assertTrue(css.contains(
                ".flame-graph-orientation-flame .flame-graph-depth-0 .flame-graph-frame-label"));
        assertTrue(css.contains(
                ".flame-graph-orientation-flame .flame-graph-depth-7 .flame-graph-frame-label"));
        assertTrue(css.contains(
                ".flame-graph-orientation-icicle .flame-graph-depth-0 .flame-graph-frame-label"));
        assertTrue(css.contains(
                ".flame-graph-orientation-icicle .flame-graph-depth-7 .flame-graph-frame-label"));
        assertTrue(css.contains("-fx-text-fill: #ffffff"));
        assertTrue(css.contains("-fx-text-fill: #1f2328"));

        int hoverIndex = css.indexOf(".flame-graph-frame:hover");
        int lastPaletteIndex = css.indexOf(".flame-graph-orientation-icicle .flame-graph-depth-7");
        assertTrue(hoverIndex > lastPaletteIndex,
                "flame graph hover border must come after palette rules so it wins the cascade");
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

    private Tooltip installedTooltip(Node node) {
        return node.getProperties().values().stream()
                .filter(Tooltip.class::isInstance)
                .map(Tooltip.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private javafx.scene.input.MouseEvent doubleClickEvent() {
        return new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                0,
                0,
                0,
                0,
                javafx.scene.input.MouseButton.PRIMARY,
                2,
                false,
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                null);
    }

    private String frameLabelAt(FlameGraphView view, int index) {
        Label label = (Label) firstByStyleClass(view.getChildren().get(index), "flame-graph-frame-label");
        return label.getText();
    }

    private String appCss() throws IOException {
        try (InputStream stream = FlameGraphViewTest.class.getResourceAsStream("/css/app.css")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void assertCssTextFill(String css, String selector, String color) {
        String block = cssBlock(css, selector);
        assertTrue(block.contains("-fx-text-fill: " + color),
                selector + " should define -fx-text-fill: " + color);
    }

    private String cssBlock(String css, String selector) {
        int selectorIndex = css.indexOf(selector);
        assertTrue(selectorIndex >= 0, selector + " should exist");
        int blockStart = css.indexOf('{', selectorIndex);
        int blockEnd = css.indexOf('}', blockStart);
        assertTrue(blockStart > selectorIndex && blockEnd > blockStart, selector + " should have a CSS block");
        return css.substring(blockStart + 1, blockEnd);
    }
}
