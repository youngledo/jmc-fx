package io.github.youngledo.jmcfx.flamegraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
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
    void viewUsesSingleCanvasChildForRenderedFrames() {
        FlameGraphView<String> view = new FlameGraphView<>();
        view.setModel(sampleModel());

        assertEquals(1, view.getChildrenUnmodifiable().stream().filter(Canvas.class::isInstance).count());
        assertEquals(5, view.frameCount());
        assertTrue(view.hasFramesProperty().get());
    }

    @Test
    void canvasResizesWithControl() {
        FlameGraphView<String> view = new FlameGraphView<>();
        view.setModel(sampleModel());
        view.resize(640, 180);

        view.layout();

        Canvas canvas = canvas(view);
        assertEquals(640, canvas.getWidth(), 0.000001);
        assertEquals(180, canvas.getHeight(), 0.000001);
    }

    @Test
    void viewCanExpandPastDefaultPreferredWidth() {
        FlameGraphView<String> view = new FlameGraphView<>();
        Pane parent = new Pane(view);
        parent.resize(1280, 240);

        assertEquals(Double.MAX_VALUE, view.getMaxWidth(), 0.000001);
        assertEquals(1280, view.prefWidth(-1), 0.000001);
    }

    @Test
    void canvasUsesScaledBackingStoreWithoutChangingLogicalHitTesting() {
        FlameGraphView<String> view = preparedView();

        view.resizeCanvas(640, 180, 2.0, 1.5);
        view.fireEvent(click(120, 36, 1));

        Canvas canvas = canvas(view);
        Scale scale = canvas.getTransforms().stream()
                .filter(Scale.class::isInstance)
                .map(Scale.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals(1280, canvas.getWidth(), 0.000001);
        assertEquals(270, canvas.getHeight(), 0.000001);
        assertEquals(0.5, scale.getX(), 0.000001);
        assertEquals(2.0 / 3.0, scale.getY(), 0.000001);
        assertEquals("child", view.selectedFrameProperty().get().node().label());
    }

    @Test
    void emptyModelShowsPlaceholder() {
        FlameGraphView<String> view = new FlameGraphView<>();
        view.setEmptyText("Select a method");
        view.setModel(FlameGraphModel.empty());

        Label label = (Label) view.getChildrenUnmodifiable().stream()
                .filter(Label.class::isInstance)
                .findFirst()
                .orElseThrow();
        assertEquals("Select a method", label.getText());
        assertEquals(0, view.frameCount());
        assertTrue(!view.hasFramesProperty().get());
    }

    @Test
    void clickingFrameSelectsDescendantStackAndMutesUnrelatedFrames() {
        FlameGraphView<String> view = preparedView();

        view.fireEvent(click(120, 36, 1));

        assertEquals("child", view.selectedFrameProperty().get().node().label());
        assertEquals(FlameGraphFrameState.SELECTED, view.frameState(view.selectedFrameProperty().get()));
        FlameGraphFrame<String> parent = frame(view, "parent");
        FlameGraphFrame<String> grandchild = frame(view, "grandchild");
        FlameGraphFrame<String> sibling = frame(view, "sibling");
        assertEquals(FlameGraphFrameState.MUTED, view.frameState(parent));
        assertEquals(FlameGraphFrameState.PATH, view.frameState(grandchild));
        assertEquals(FlameGraphFrameState.MUTED, view.frameState(sibling));
    }

    @Test
    void clickingFrameUsesSameSelectionStackInFlameMode() {
        FlameGraphView<String> view = preparedView();
        view.setMode(FlameGraphMode.FLAME);

        view.fireEvent(click(120, 36, 1));

        assertEquals("child", view.selectedFrameProperty().get().node().label());
        FlameGraphFrame<String> parent = frame(view, "parent");
        FlameGraphFrame<String> grandchild = frame(view, "grandchild");
        FlameGraphFrame<String> sibling = frame(view, "sibling");
        assertEquals(FlameGraphFrameState.MUTED, view.frameState(parent));
        assertEquals(FlameGraphFrameState.PATH, view.frameState(grandchild));
        assertEquals(FlameGraphFrameState.MUTED, view.frameState(sibling));
    }

    @Test
    void clickingEmptySpaceClearsSelection() {
        FlameGraphView<String> view = preparedView();
        view.fireEvent(click(120, 36, 1));

        view.fireEvent(click(630, 170, 1));

        assertEquals(null, view.selectedFrameProperty().get());
    }

    @Test
    void doubleClickingFrameFocusesStack() {
        FlameGraphView<String> view = preparedView();
        view.setMode(FlameGraphMode.FLAME);

        view.fireEvent(click(120, 36, 2));

        assertNotNull(view.focusedFrameProperty().get());
        assertEquals("child", view.focusedFrameProperty().get().node().label());
        assertEquals(null, view.selectedFrameProperty().get());
        assertEquals(2, view.frameCount());
        assertEquals(List.of("child", "grandchild"), view.visibleFrames().stream()
                .map(frame -> frame.node().label())
                .toList());
    }

    @Test
    void doubleClickingFrameFocusesSameStackInIcicleMode() {
        FlameGraphView<String> view = preparedView();

        view.fireEvent(click(120, 36, 2));

        assertNotNull(view.focusedFrameProperty().get());
        assertEquals("child", view.focusedFrameProperty().get().node().label());
        assertEquals(2, view.frameCount());
        assertEquals(List.of("child", "grandchild"), view.visibleFrames().stream()
                .map(frame -> frame.node().label())
                .toList());
    }

    @Test
    void doubleClickingFrameKeepsFocusedStackInDefaultColorState() {
        FlameGraphView<String> view = preparedView();

        view.fireEvent(click(120, 36, 2));

        assertEquals(List.of(FlameGraphFrameState.DEFAULT, FlameGraphFrameState.DEFAULT),
                view.visibleFrames().stream()
                        .map(view::frameState)
                        .toList());
    }

    @Test
    void resetZoomClearsFocusAndSelection() {
        FlameGraphView<String> view = preparedView();
        view.fireEvent(click(120, 36, 2));

        view.resetZoom();

        assertEquals(null, view.focusedFrameProperty().get());
        assertEquals(null, view.selectedFrameProperty().get());
        assertEquals(5, view.frameCount());
    }

    @Test
    void viewportStartsAtIdentityScaleAndOffset() {
        FlameGraphView<String> view = preparedView();

        assertEquals(1.0, view.viewportScaleProperty().get(), 0.000001);
        assertEquals(0.0, view.viewportOffsetXProperty().get(), 0.000001);
        assertEquals(1.0, view.visibleWidthRatioProperty().get(), 0.000001);
    }

    @Test
    void viewportOffsetClampsToScaledContent() {
        FlameGraphView<String> view = preparedView();

        view.setViewportScale(2.0);
        view.setViewportOffsetX(0.75);

        assertEquals(0.5, view.viewportOffsetXProperty().get(), 0.000001);
    }

    @Test
    void viewportResetRestoresIdentity() {
        FlameGraphView<String> view = preparedView();
        view.setViewportScale(3.0);
        view.setViewportOffsetX(0.5);

        view.resetViewport();

        assertEquals(1.0, view.viewportScaleProperty().get(), 0.000001);
        assertEquals(0.0, view.viewportOffsetXProperty().get(), 0.000001);
    }

    @Test
    void fitToWidthUsesContentWidthRatioAndClampsOffset() {
        FlameGraphView<String> view = preparedView();
        view.setViewportScale(3.0);
        view.setViewportOffsetX(0.8);

        view.fitToWidth(320);

        assertEquals(2.0, view.viewportScaleProperty().get(), 0.000001);
        assertEquals(0.5, view.viewportOffsetXProperty().get(), 0.000001);
        assertEquals(0.5, view.visibleWidthRatioProperty().get(), 0.000001);
    }

    @Test
    void zoomInAndOutScaleAroundViewportCenter() {
        FlameGraphView<String> view = preparedView();

        view.zoomIn();
        assertEquals(1.25, view.viewportScaleProperty().get(), 0.000001);
        assertEquals(0.1, view.viewportOffsetXProperty().get(), 0.000001);

        view.zoomOut();
        assertEquals(1.0, view.viewportScaleProperty().get(), 0.000001);
        assertEquals(0.0, view.viewportOffsetXProperty().get(), 0.000001);
    }

    @Test
    void zoomByUsesAnchorRatio() {
        FlameGraphView<String> view = preparedView();

        view.zoomBy(2.0, 0.25);

        assertEquals(2.0, view.viewportScaleProperty().get(), 0.000001);
        assertEquals(0.125, view.viewportOffsetXProperty().get(), 0.000001);
    }

    @Test
    void viewportTransformsHitTesting() {
        FlameGraphView<String> view = preparedView();
        view.zoomBy(2.0, 0.0);

        view.fireEvent(click(320, 36, 1));

        assertEquals("child", view.selectedFrameProperty().get().node().label());
    }

    @Test
    void resetZoomClearsViewportFocusAndSelection() {
        FlameGraphView<String> view = preparedView();
        view.fireEvent(click(120, 36, 2));
        view.zoomBy(2.0, 0.25);

        view.resetZoom();

        assertEquals(null, view.focusedFrameProperty().get());
        assertEquals(null, view.selectedFrameProperty().get());
        assertEquals(1.0, view.viewportScaleProperty().get(), 0.000001);
        assertEquals(0.0, view.viewportOffsetXProperty().get(), 0.000001);
    }

    @Test
    void searchUsesTextProviderAndOrdersMatchesByVisibleFrameOrder() {
        FlameGraphView<String> view = preparedView();
        view.setTextProvider(frame -> frame.node().label().replace("-", " "));

        view.search("child");

        assertEquals(3, view.matchCountProperty().get());
        assertEquals(0, view.currentMatchIndexProperty().get());
        assertEquals("child", view.currentMatchProperty().get().node().label());
        assertEquals(List.of("child", "child-sibling", "grandchild"), view.matchingFrames().stream()
                .map(frame -> frame.node().label())
                .toList());
    }

    @Test
    void searchSupportsPayloadPredicate() {
        FlameGraphView<String> view = preparedView();

        view.search(frame -> frame.node().payload().endsWith("sibling"));

        assertEquals(2, view.matchCountProperty().get());
        assertEquals("sibling", view.currentMatchProperty().get().node().label());
    }

    @Test
    void searchNextPreviousAndClearMaintainCurrentMatch() {
        FlameGraphView<String> view = preparedView();
        view.search("child");

        view.nextMatch();
        assertEquals(1, view.currentMatchIndexProperty().get());
        assertEquals("child-sibling", view.currentMatchProperty().get().node().label());

        view.previousMatch();
        assertEquals(0, view.currentMatchIndexProperty().get());

        view.clearSearch();
        assertEquals(0, view.matchCountProperty().get());
        assertEquals(-1, view.currentMatchIndexProperty().get());
        assertEquals(null, view.currentMatchProperty().get());
        assertTrue(view.matchingFrames().isEmpty());
    }

    @Test
    void matchStateComposesWithSelectionAndMutedStates() {
        FlameGraphView<String> view = preparedView();
        view.search("grandchild");
        FlameGraphFrame<String> grandchild = frame(view, "grandchild");
        FlameGraphFrame<String> sibling = frame(view, "sibling");

        assertEquals(FlameGraphFrameState.MATCH, view.frameState(grandchild));

        view.fireEvent(click(120, 36, 1));

        assertEquals(FlameGraphFrameState.PATH, view.frameState(grandchild));
        assertEquals(FlameGraphFrameState.MUTED, view.frameState(sibling));
    }

    @Test
    void tooltipProviderSuppliesHoverText() {
        FlameGraphView<String> view = preparedView();
        view.setTooltipProvider(frame -> "Tooltip: " + frame.node().label());

        view.fireEvent(mouseMove(120, 36));

        Tooltip tooltip = installedTooltip(view);
        assertEquals("Tooltip: child", tooltip.getText());
    }

    @Test
    void modeChangesRows() {
        FlameGraphView<String> view = preparedView();

        view.setMode(FlameGraphMode.FLAME);

        FlameGraphFrame<String> parent = frame(view, "parent");
        FlameGraphFrame<String> grandchild = frame(view, "grandchild");
        assertTrue(parent.row() > grandchild.row());
    }

    @Test
    void colorProviderReceivesRenderModeContext() {
        FlameGraphView<String> view = preparedView();
        AtomicReference<FlameGraphMode> observedMode = new AtomicReference<>();
        view.setMode(FlameGraphMode.FLAME);
        view.setColorProvider((frame, state, context) -> {
            observedMode.set(context.mode());
            return FlameGraphFrameColors.DEFAULT;
        });

        view.layout();

        assertEquals(FlameGraphMode.FLAME, observedMode.get());
    }

    @Test
    void nullColorProviderFallsBackToDefaultProvider() {
        FlameGraphView<String> view = preparedView();
        FrameColorProvider<String> customProvider =
                (frame, state, context) -> new FlameGraphFrameColors(Color.RED, Color.BLACK, Color.WHITE);
        view.setColorProvider(customProvider);

        assertEquals(customProvider, view.getColorProvider());
        view.setColorProvider(null);

        FlameGraphFrame<String> frame = frame(view, "child");
        FlameGraphFrameColors colors = view.getColorProvider()
                .colors(frame, FlameGraphFrameState.DEFAULT, new FlameGraphRenderContext(FlameGraphMode.ICICLE, 3));
        assertEquals(FlameGraphFrameColors.DEFAULT, colors);
    }

    @Test
    void defaultColorProviderDefinesAllFrameStates() {
        FlameGraphFrame<String> frame = frame(preparedView(), "child");
        FlameGraphRenderContext context = new FlameGraphRenderContext(FlameGraphMode.ICICLE, 3);

        for (FlameGraphFrameState state : FlameGraphFrameState.values()) {
            FlameGraphFrameColors colors = FrameColorProvider.<String>defaultProvider().colors(frame, state, context);

            assertNotNull(colors.fill(), state.name());
            assertNotNull(colors.stroke(), state.name());
            assertNotNull(colors.text(), state.name());
        }
    }

    @Test
    void textClippingUsesJavaFxMeasurementForNarrowGlyphs() {
        FlameGraphTextMeasurer measurer = new FlameGraphTextMeasurer();
        Font font = Font.font(11);
        String text = "iiiiiiiiiiiiiiiiiiii";
        double maxWidth = measurer.width(text, font) + 0.5;

        assertTrue(font.getSize() * text.length() * 0.55 > maxWidth);
        assertEquals(text, measurer.clip(text, font, maxWidth));
    }

    @Test
    void textClippingConstrainsRenderedWidth() {
        FlameGraphTextMeasurer measurer = new FlameGraphTextMeasurer();
        Font font = Font.font(11);

        String clipped = measurer.clip("com.example.service.Worker.performVeryExpensiveOperation", font, 80);

        assertTrue(clipped.endsWith("..."));
        assertTrue(measurer.width(clipped, font) <= 80.000001);
    }

    private FlameGraphView<String> preparedView() {
        FlameGraphView<String> view = new FlameGraphView<>();
        view.setModel(sampleModel());
        view.resize(640, 180);
        view.layout();
        return view;
    }

    private FlameGraphModel<String> sampleModel() {
        FlameGraphNode<String> root = node("root", 100,
                node("parent", 80,
                        node("child", 60, node("grandchild", 30)),
                        node("child-sibling", 20)),
                node("sibling", 20));
        return FlameGraphModel.of(root);
    }

    @SafeVarargs
    private static FlameGraphNode<String> node(String label, double weight, FlameGraphNode<String>... children) {
        return new FlameGraphNode<>(label, weight, weight, label, List.of(children));
    }

    private Canvas canvas(FlameGraphView<String> view) {
        return view.getChildrenUnmodifiable().stream()
                .filter(Canvas.class::isInstance)
                .map(Canvas.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private FlameGraphFrame<String> frame(FlameGraphView<String> view, String label) {
        return view.visibleFrames().stream()
                .filter(frame -> frame.node().label().equals(label))
                .findFirst()
                .orElseThrow();
    }

    private Tooltip installedTooltip(Node node) {
        return node.getProperties().values().stream()
                .filter(Tooltip.class::isInstance)
                .map(Tooltip.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private MouseEvent click(double x, double y, int clickCount) {
        return new MouseEvent(
                MouseEvent.MOUSE_CLICKED,
                x,
                y,
                x,
                y,
                MouseButton.PRIMARY,
                clickCount,
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

    private MouseEvent mouseMove(double x, double y) {
        return new MouseEvent(
                MouseEvent.MOUSE_MOVED,
                x,
                y,
                x,
                y,
                MouseButton.NONE,
                0,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                null);
    }
}
