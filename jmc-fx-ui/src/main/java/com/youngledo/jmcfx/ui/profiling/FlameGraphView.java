package com.youngledo.jmcfx.ui.profiling;

import java.util.Locale;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;

public class FlameGraphView extends Pane {

    private static final double FRAME_HEIGHT = 24;
    private static final double FRAME_GAP = 2;
    private static final double DEFAULT_WIDTH = 960;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 3.0;
    private static final double ZOOM_STEP = 1.2;

    private FlameGraphLayout layout = FlameGraphLayout.EMPTY;
    private final Label emptyLabel = new Label();
    private final StringProperty emptyText = new SimpleStringProperty(this, "emptyText", "");
    private final ReadOnlyBooleanWrapper hasFrames = new ReadOnlyBooleanWrapper(this, "hasFrames", false);
    private final DoubleProperty zoomScale = new SimpleDoubleProperty(this, "zoomScale", 1.0);
    private final ObjectProperty<Orientation> orientation =
            new SimpleObjectProperty<>(this, "orientation", Orientation.ICICLE);
    private final Scale scale = new Scale(1.0, 1.0);

    public enum Orientation {
        ICICLE,
        FLAME
    }

    public FlameGraphView() {
        getStyleClass().add("flame-graph-view");
        getTransforms().add(scale);
        emptyLabel.getStyleClass().add("flame-graph-empty");
        emptyLabel.setManaged(false);
        emptyLabel.setMouseTransparent(true);
        zoomScale.addListener((obs, old, val) -> {
            double zoom = clampZoom(val.doubleValue());
            if (Double.compare(zoom, val.doubleValue()) != 0) {
                zoomScale.set(zoom);
                return;
            }
            scale.setX(zoom);
            scale.setY(zoom);
            requestLayout();
        });
        orientation.addListener((obs, old, val) -> requestLayout());
        emptyText.addListener((obs, old, val) -> {
            rebuildChildren();
            requestLayout();
        });
        setMinHeight(0);
    }

    public void setLayout(FlameGraphLayout layout) {
        this.layout = layout == null ? FlameGraphLayout.EMPTY : layout;
        rebuildChildren();
        requestLayout();
    }

    public StringProperty emptyTextProperty() {
        return emptyText;
    }

    public void setEmptyText(String emptyText) {
        this.emptyText.set(emptyText);
    }

    public String getEmptyText() {
        return emptyText.get();
    }

    public DoubleProperty zoomScaleProperty() {
        return zoomScale;
    }

    public ReadOnlyBooleanProperty hasFramesProperty() {
        return hasFrames.getReadOnlyProperty();
    }

    public ObjectProperty<Orientation> orientationProperty() {
        return orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation.set(orientation == null ? Orientation.ICICLE : orientation);
    }

    public Orientation getOrientation() {
        return orientation.get();
    }

    public void zoomIn() {
        setZoomScale(zoomScale.get() * ZOOM_STEP);
    }

    public void zoomOut() {
        setZoomScale(zoomScale.get() / ZOOM_STEP);
    }

    public void resetZoom() {
        setZoomScale(1.0);
    }

    public void fitToWidth(double viewportWidth) {
        double contentWidth = computeBaseWidth();
        double availableWidth = viewportWidth <= 0 ? 0 : viewportWidth;
        if (contentWidth <= 0 || availableWidth <= 0 || contentWidth <= availableWidth) {
            setZoomScale(1.0);
            return;
        }
        setZoomScale(availableWidth / contentWidth);
    }

    private void rebuildChildren() {
        getChildren().clear();
        hasFrames.set(!layout.frames().isEmpty());
        if (layout.frames().isEmpty()) {
            if (!getEmptyText().isBlank()) {
                emptyLabel.setText(getEmptyText());
                getChildren().add(emptyLabel);
            }
            return;
        }
        for (FlameGraphFrame frame : this.layout.frames()) {
            getChildren().add(frameNode(frame));
        }
    }

    public int frameCount() {
        return layout.frames().size();
    }

    @Override
    protected void layoutChildren() {
        double left = snappedLeftInset();
        double top = snappedTopInset();
        double contentWidth = Math.max(0, getWidth() - left - snappedRightInset());
        double rowHeight = snapSizeY(FRAME_HEIGHT);
        if (layout.frames().isEmpty()) {
            if (getChildren().contains(emptyLabel)) {
                emptyLabel.resizeRelocate(left, top, contentWidth, rowHeight);
            }
            return;
        }
        for (int index = 0; index < layout.frames().size(); index++) {
            FlameGraphFrame frame = layout.frames().get(index);
            double x = left + snapPositionX(frame.x() * contentWidth);
            double y = top + snapPositionY(yForDepth(frame.depth()));
            double width = snapSizeX(frame.width() * contentWidth);
            StackPane frameNode = (StackPane) getChildren().get(index);
            frameNode.setPrefSize(width, rowHeight);
            frameNode.resizeRelocate(x, y, width, rowHeight);
        }
    }

    @Override
    protected double computePrefWidth(double height) {
        return computeBaseWidth() + snappedLeftInset() + snappedRightInset();
    }

    @Override
    protected double computePrefHeight(double width) {
        if (layout.frames().isEmpty() && !getEmptyText().isBlank()) {
            return FRAME_HEIGHT + snappedTopInset() + snappedBottomInset();
        }
        return heightForDepth(layout.maxDepth()) + snappedTopInset() + snappedBottomInset();
    }

    private StackPane frameNode(FlameGraphFrame frame) {
        StackPane node = new StackPane();
        node.getStyleClass().add("flame-graph-frame");
        String details = frameDetails(frame);
        node.setAccessibleText(details);
        Tooltip.install(node, new Tooltip(details));
        Label label = new Label(frame.method());
        label.getStyleClass().add("flame-graph-frame-label");
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinWidth(0);
        label.setMouseTransparent(true);
        node.getChildren().add(label);
        return node;
    }

    private double heightForDepth(int depth) {
        if (depth <= 0) {
            return 0;
        }
        return (depth * FRAME_HEIGHT) + ((depth - 1) * FRAME_GAP);
    }

    private double yForDepth(int depth) {
        if (getOrientation() == Orientation.FLAME) {
            int row = Math.max(0, layout.maxDepth() - depth - 1);
            return row * (FRAME_HEIGHT + FRAME_GAP);
        }
        return depth * (FRAME_HEIGHT + FRAME_GAP);
    }

    private double computeBaseWidth() {
        return DEFAULT_WIDTH;
    }

    private void setZoomScale(double zoom) {
        zoomScale.set(clampZoom(zoom));
    }

    private double clampZoom(double zoom) {
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
    }

    private String frameDetails(FlameGraphFrame frame) {
        return String.format(Locale.ROOT, "%s, %,d samples, %.1f%%",
                frame.method(),
                frame.count(),
                frame.percentage());
    }
}
