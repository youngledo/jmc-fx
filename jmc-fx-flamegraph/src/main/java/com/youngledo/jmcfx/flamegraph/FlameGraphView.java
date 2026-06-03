package com.youngledo.jmcfx.flamegraph;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

public class FlameGraphView<T> extends Region {

    private static final double FRAME_HEIGHT = 24;
    private static final double FRAME_GAP = 2;
    private static final double MIN_TEXT_WIDTH = 24;
    private static final double TEXT_PADDING = 6;
    private static final double ZOOM_STEP = 1.25;

    private final FlameGraphLayoutEngine<T> layoutEngine = new FlameGraphLayoutEngine<>();
    private final Canvas canvas = new Canvas();
    private final Label emptyLabel = new Label();
    private final Tooltip tooltip = new Tooltip();
    private final FlameGraphTextMeasurer textMeasurer = new FlameGraphTextMeasurer();
    private final StringProperty emptyText = new SimpleStringProperty(this, "emptyText", "");
    private final ObjectProperty<FlameGraphModel<T>> model =
            new SimpleObjectProperty<>(this, "model", FlameGraphModel.empty());
    private final ObjectProperty<FlameGraphMode> mode =
            new SimpleObjectProperty<>(this, "mode", FlameGraphMode.ICICLE);
    private final ObjectProperty<FrameTextProvider<T>> textProvider =
            new SimpleObjectProperty<>(this, "textProvider", FrameTextProvider.defaultProvider());
    private final ObjectProperty<FrameTooltipProvider<T>> tooltipProvider =
            new SimpleObjectProperty<>(this, "tooltipProvider", FrameTooltipProvider.defaultProvider());
    private final ObjectProperty<FrameColorProvider<T>> colorProvider =
            new SimpleObjectProperty<>(this, "colorProvider", FrameColorProvider.defaultProvider());
    private final ReadOnlyBooleanWrapper hasFrames = new ReadOnlyBooleanWrapper(this, "hasFrames", false);
    private final ReadOnlyObjectWrapper<FlameGraphFrame<T>> selectedFrame =
            new ReadOnlyObjectWrapper<>(this, "selectedFrame");
    private final ReadOnlyObjectWrapper<FlameGraphFrame<T>> focusedFrame =
            new ReadOnlyObjectWrapper<>(this, "focusedFrame");
    private final ReadOnlyObjectWrapper<FlameGraphFrame<T>> hoveredFrame =
            new ReadOnlyObjectWrapper<>(this, "hoveredFrame");
    private final DoubleProperty viewportScale = new SimpleDoubleProperty(this, "viewportScale", 1);
    private final DoubleProperty viewportOffsetX = new SimpleDoubleProperty(this, "viewportOffsetX", 0);
    private final ReadOnlyDoubleWrapper visibleWidthRatio =
            new ReadOnlyDoubleWrapper(this, "visibleWidthRatio", 1);
    private final ReadOnlyIntegerWrapper matchCount = new ReadOnlyIntegerWrapper(this, "matchCount", 0);
    private final IntegerProperty currentMatchIndex = new SimpleIntegerProperty(this, "currentMatchIndex", -1);
    private final ReadOnlyObjectWrapper<FlameGraphFrame<T>> currentMatch =
            new ReadOnlyObjectWrapper<>(this, "currentMatch");

    private FlameGraphLayout<T> layout = FlameGraphLayout.empty();
    private Predicate<FlameGraphFrame<T>> searchPredicate;
    private List<FlameGraphFrame<T>> matchingFrames = List.of();
    private double logicalCanvasWidth;
    private double logicalCanvasHeight;
    private double outputScaleX = 1;
    private double outputScaleY = 1;

    public FlameGraphView() {
        getStyleClass().add("flame-graph-view");
        setMaxWidth(Double.MAX_VALUE);
        emptyLabel.getStyleClass().add("flame-graph-empty");
        emptyLabel.setManaged(false);
        emptyLabel.setMouseTransparent(true);
        canvas.setManaged(false);
        Tooltip.install(this, tooltip);
        tooltip.setShowDuration(Duration.INDEFINITE);
        getChildren().setAll(canvas);
        model.addListener((obs, old, val) -> rebuildLayout());
        mode.addListener((obs, old, val) -> rebuildLayout());
        emptyText.addListener((obs, old, val) -> updateChildren());
        textProvider.addListener((obs, old, val) -> draw());
        colorProvider.addListener((obs, old, val) -> draw());
        tooltipProvider.addListener((obs, old, val) -> updateTooltip(hoveredFrame.get()));
        viewportScale.addListener((obs, old, val) -> {
            updateVisibleWidthRatio();
            clampViewportOffset();
            draw();
        });
        viewportOffsetX.addListener((obs, old, val) -> {
            double clamped = clampedViewportOffset(val.doubleValue());
            if (Double.compare(clamped, val.doubleValue()) != 0) {
                viewportOffsetX.set(clamped);
            } else {
                draw();
            }
        });
        addEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouseMoved);
        addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            hoveredFrame.set(null);
            updateTooltip(null);
            draw();
        });
        addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMouseClicked);
        rebuildLayout();
    }

    public ObjectProperty<FlameGraphModel<T>> modelProperty() {
        return model;
    }

    public void setModel(FlameGraphModel<T> model) {
        this.model.set(model == null ? FlameGraphModel.empty() : model);
    }

    public FlameGraphModel<T> getModel() {
        return model.get();
    }

    public ObjectProperty<FlameGraphMode> modeProperty() {
        return mode;
    }

    public void setMode(FlameGraphMode mode) {
        this.mode.set(mode == null ? FlameGraphMode.ICICLE : mode);
    }

    public FlameGraphMode getMode() {
        return mode.get();
    }

    public StringProperty emptyTextProperty() {
        return emptyText;
    }

    public void setEmptyText(String emptyText) {
        this.emptyText.set(emptyText == null ? "" : emptyText);
    }

    public String getEmptyText() {
        return emptyText.get();
    }

    public void setTextProvider(FrameTextProvider<T> textProvider) {
        this.textProvider.set(textProvider == null ? FrameTextProvider.defaultProvider() : textProvider);
    }

    public void setTooltipProvider(FrameTooltipProvider<T> tooltipProvider) {
        this.tooltipProvider.set(tooltipProvider == null
                ? FrameTooltipProvider.defaultProvider()
                : tooltipProvider);
    }

    public void setColorProvider(FrameColorProvider<T> colorProvider) {
        this.colorProvider.set(colorProvider == null ? FrameColorProvider.defaultProvider() : colorProvider);
    }

    public ReadOnlyBooleanProperty hasFramesProperty() {
        return hasFrames.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<FlameGraphFrame<T>> selectedFrameProperty() {
        return selectedFrame.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<FlameGraphFrame<T>> focusedFrameProperty() {
        return focusedFrame.getReadOnlyProperty();
    }

    public ReadOnlyDoubleProperty viewportScaleProperty() {
        return viewportScale;
    }

    public void setViewportScale(double scale) {
        viewportScale.set(Math.max(1, Double.isFinite(scale) ? scale : 1));
    }

    public ReadOnlyDoubleProperty viewportOffsetXProperty() {
        return viewportOffsetX;
    }

    public void setViewportOffsetX(double offset) {
        viewportOffsetX.set(clampedViewportOffset(Double.isFinite(offset) ? offset : 0));
    }

    public ReadOnlyDoubleProperty visibleWidthRatioProperty() {
        return visibleWidthRatio.getReadOnlyProperty();
    }

    public ReadOnlyIntegerProperty matchCountProperty() {
        return matchCount.getReadOnlyProperty();
    }

    public ReadOnlyIntegerProperty currentMatchIndexProperty() {
        return currentMatchIndex;
    }

    public ReadOnlyObjectProperty<FlameGraphFrame<T>> currentMatchProperty() {
        return currentMatch.getReadOnlyProperty();
    }

    public int frameCount() {
        return layout.frames().size();
    }

    public List<FlameGraphFrame<T>> visibleFrames() {
        return layout.frames();
    }

    public List<FlameGraphFrame<T>> matchingFrames() {
        return matchingFrames;
    }

    public FlameGraphFrameState frameState(FlameGraphFrame<T> frame) {
        if (frame == null) {
            return FlameGraphFrameState.DEFAULT;
        }
        FlameGraphFrame<T> selected = selectedFrame.get();
        if (selected == null) {
            if (frame.samePath(hoveredFrame.get())) {
                return FlameGraphFrameState.HOVERED;
            }
            return isMatchedFrame(frame) ? FlameGraphFrameState.MATCH : FlameGraphFrameState.DEFAULT;
        }
        if (frame.samePath(selected)) {
            return FlameGraphFrameState.SELECTED;
        }
        if (frame.isDescendantOf(selected)) {
            return FlameGraphFrameState.PATH;
        }
        return FlameGraphFrameState.MUTED;
    }

    public void search(String query) {
        String normalizedQuery = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (normalizedQuery.isBlank()) {
            clearSearch();
            return;
        }
        search(frame -> {
            String text = textProvider.get().text(frame);
            return text != null && text.toLowerCase(Locale.ROOT).contains(normalizedQuery);
        });
    }

    public void search(Predicate<FlameGraphFrame<T>> predicate) {
        searchPredicate = predicate;
        rebuildSearchMatches(null);
        draw();
    }

    public void nextMatch() {
        if (matchingFrames.isEmpty()) {
            return;
        }
        setCurrentMatchIndex((currentMatchIndex.get() + 1) % matchingFrames.size());
    }

    public void previousMatch() {
        if (matchingFrames.isEmpty()) {
            return;
        }
        setCurrentMatchIndex((currentMatchIndex.get() - 1 + matchingFrames.size()) % matchingFrames.size());
    }

    public void clearSearch() {
        searchPredicate = null;
        matchingFrames = List.of();
        matchCount.set(0);
        currentMatchIndex.set(-1);
        currentMatch.set(null);
        draw();
    }

    public void resetZoom() {
        focusedFrame.set(null);
        selectedFrame.set(null);
        hoveredFrame.set(null);
        resetViewport();
        rebuildLayout();
    }

    public void resetViewport() {
        viewportScale.set(1);
        viewportOffsetX.set(0);
        updateVisibleWidthRatio();
        draw();
    }

    public void zoomIn() {
        zoomBy(ZOOM_STEP, 0.5);
    }

    public void zoomOut() {
        zoomBy(1 / ZOOM_STEP, 0.5);
    }

    public void zoomBy(double factor, double anchorRatio) {
        if (!Double.isFinite(factor) || factor <= 0) {
            return;
        }
        double anchor = Math.clamp(Double.isFinite(anchorRatio) ? anchorRatio : 0.5, 0, 1);
        double oldScale = viewportScale.get();
        double oldVisibleWidth = visibleWidthRatio.get();
        double anchorModelX = viewportOffsetX.get() + anchor * oldVisibleWidth;
        setViewportScale(oldScale * factor);
        double newOffset = anchorModelX - anchor * visibleWidthRatio.get();
        setViewportOffsetX(newOffset);
    }

    public void fitToWidth(double width) {
        double logicalWidth = Math.max(1, logicalCanvasWidth > 0 ? logicalCanvasWidth : getWidth());
        setViewportScale(logicalWidth / Math.max(1, width));
        clampViewportOffset();
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        double width = Math.max(0, getWidth());
        double height = Math.max(0, getHeight());
        resizeCanvas(width, height, outputScaleX(), outputScaleY());
        if (getChildren().contains(emptyLabel)) {
            emptyLabel.resizeRelocate(0, 0, width, Math.max(FRAME_HEIGHT, height));
        }
        draw();
    }

    @Override
    protected double computePrefHeight(double width) {
        if (layout.frames().isEmpty() && !getEmptyText().isBlank()) {
            return FRAME_HEIGHT;
        }
        return layout.maxDepth() <= 0 ? 0 : layout.maxDepth() * FRAME_HEIGHT
                + (layout.maxDepth() - 1) * FRAME_GAP;
    }

    @Override
    protected double computePrefWidth(double height) {
        double parentWidth = getParent() == null ? 0 : getParent().getLayoutBounds().getWidth();
        return Math.max(960, parentWidth);
    }

    private void rebuildLayout() {
        FlameGraphFrame<T> previousMatch = currentMatch.get();
        FlameGraphFrame<T> focus = focusedFrame.get();
        if (focus == null) {
            layout = layoutEngine.layout(getModel(), getMode());
        } else {
            layout = layoutEngine.focusedLayout(getModel(), getMode(), focus);
            focusedFrame.set(layout.frames().stream()
                    .filter(frame -> frame.path().equals(focus.path()))
                    .findFirst()
                    .orElse(null));
        }
        hasFrames.set(!layout.frames().isEmpty());
        rebuildSearchMatches(previousMatch);
        clampViewportOffset();
        updateChildren();
        requestLayout();
        draw();
    }

    private void updateChildren() {
        if (layout.frames().isEmpty() && !getEmptyText().isBlank()) {
            emptyLabel.setText(getEmptyText());
            getChildren().setAll(canvas, emptyLabel);
        } else {
            getChildren().setAll(canvas);
        }
    }

    private void handleMouseMoved(MouseEvent event) {
        FlameGraphFrame<T> frame = frameAt(event.getX(), event.getY());
        hoveredFrame.set(frame);
        updateTooltip(frame);
        draw();
    }

    private void handleMouseClicked(MouseEvent event) {
        FlameGraphFrame<T> frame = frameAt(event.getX(), event.getY());
        if (frame == null) {
            selectedFrame.set(null);
            draw();
            return;
        }
        if (event.getClickCount() >= 2) {
            focusedFrame.set(frame);
            selectedFrame.set(null);
            rebuildLayout();
            draw();
            return;
        }
        selectedFrame.set(frame);
        draw();
    }

    private FlameGraphFrame<T> frameAt(double x, double y) {
        double width = Math.max(1, logicalCanvasWidth);
        int row = (int) Math.floor(y / (FRAME_HEIGHT + FRAME_GAP));
        if (row < 0) {
            return null;
        }
        int depth = depthForRow(row);
        return layout.frameAt(viewportOffsetX.get() + (x / width) * visibleWidthRatio.get(), depth).orElse(null);
    }

    private int depthForRow(int row) {
        if (getMode() == FlameGraphMode.FLAME) {
            return Math.max(0, layout.maxDepth() - row - 1);
        }
        return row;
    }

    private void updateTooltip(FlameGraphFrame<T> frame) {
        tooltip.setText(frame == null ? "" : tooltipProvider.get().tooltip(frame));
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setTransform(1, 0, 0, 1, 0, 0);
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setTransform(outputScaleX, 0, 0, outputScaleY, 0, 0);
        double width = logicalCanvasWidth;
        double height = logicalCanvasHeight;
        if (width <= 0 || height <= 0 || layout.frames().isEmpty()) {
            return;
        }
        gc.setFont(Font.font(11));
        for (FlameGraphFrame<T> frame : layout.frames()) {
            drawFrame(gc, frame, width);
        }
    }

    private void drawFrame(GraphicsContext gc, FlameGraphFrame<T> frame, double contentWidth) {
        double x = (frame.x() - viewportOffsetX.get()) * viewportScale.get() * contentWidth;
        double y = frame.row() * (FRAME_HEIGHT + FRAME_GAP);
        double width = frame.width() * viewportScale.get() * contentWidth;
        if (width <= 0) {
            return;
        }
        if (x + width < 0 || x > contentWidth) {
            return;
        }
        FlameGraphFrameState state = frameState(frame);
        FlameGraphFrameColors colors = colorProvider.get().colors(
                frame,
                state,
                new FlameGraphRenderContext(getMode(), layout.maxDepth()));
        gc.setFill(colors.fill());
        gc.fillRect(x, y, width, FRAME_HEIGHT);
        gc.setStroke(colors.stroke());
        gc.strokeRect(x, y, width, FRAME_HEIGHT);
        if (width < MIN_TEXT_WIDTH) {
            return;
        }
        String text = clippedText(gc.getFont(), textProvider.get().text(frame), width - (TEXT_PADDING * 2));
        if (text.isBlank()) {
            return;
        }
        gc.setFill(colors.text());
        gc.fillText(text, x + TEXT_PADDING, y + 16);
    }

    void resizeCanvas(double width, double height, double scaleX, double scaleY) {
        logicalCanvasWidth = Math.max(0, width);
        logicalCanvasHeight = Math.max(0, height);
        outputScaleX = Math.max(1, scaleX);
        outputScaleY = Math.max(1, scaleY);
        canvas.setWidth(logicalCanvasWidth * outputScaleX);
        canvas.setHeight(logicalCanvasHeight * outputScaleY);
        canvas.relocate(0, 0);
        canvas.getTransforms().setAll(new Scale(1 / outputScaleX, 1 / outputScaleY, 0, 0));
    }

    private double outputScaleX() {
        return getScene() == null || getScene().getWindow() == null
                ? 1
                : getScene().getWindow().getOutputScaleX();
    }

    private double outputScaleY() {
        return getScene() == null || getScene().getWindow() == null
                ? 1
                : getScene().getWindow().getOutputScaleY();
    }

    private String clippedText(Font font, String text, double maxWidth) {
        return textMeasurer.clip(text, font, maxWidth);
    }

    private void rebuildSearchMatches(FlameGraphFrame<T> previousMatch) {
        if (searchPredicate == null) {
            matchingFrames = List.of();
            matchCount.set(0);
            currentMatchIndex.set(-1);
            currentMatch.set(null);
            return;
        }
        matchingFrames = layout.frames().stream()
                .filter(searchPredicate)
                .toList();
        matchCount.set(matchingFrames.size());
        if (matchingFrames.isEmpty()) {
            currentMatchIndex.set(-1);
            currentMatch.set(null);
            return;
        }
        int index = previousMatch == null ? -1 : indexOfPath(matchingFrames, previousMatch);
        setCurrentMatchIndex(index >= 0 ? index : 0);
    }

    private void setCurrentMatchIndex(int index) {
        if (matchingFrames.isEmpty()) {
            currentMatchIndex.set(-1);
            currentMatch.set(null);
            return;
        }
        int clampedIndex = Math.clamp(index, 0, matchingFrames.size() - 1);
        currentMatchIndex.set(clampedIndex);
        currentMatch.set(matchingFrames.get(clampedIndex));
        draw();
    }

    private boolean isMatchedFrame(FlameGraphFrame<T> frame) {
        return indexOfPath(matchingFrames, frame) >= 0;
    }

    private int indexOfPath(List<FlameGraphFrame<T>> frames, FlameGraphFrame<T> target) {
        if (target == null) {
            return -1;
        }
        for (int i = 0; i < frames.size(); i++) {
            if (frames.get(i).samePath(target)) {
                return i;
            }
        }
        return -1;
    }

    private void updateVisibleWidthRatio() {
        visibleWidthRatio.set(1 / viewportScale.get());
    }

    private void clampViewportOffset() {
        setViewportOffsetX(viewportOffsetX.get());
    }

    private double clampedViewportOffset(double offset) {
        return Math.clamp(offset, 0, Math.max(0, 1 - visibleWidthRatio.get()));
    }
}
