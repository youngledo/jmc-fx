package com.youngledo.jmcfx.ui.profiling;

import java.util.Locale;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
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
import javafx.util.Duration;

import com.youngledo.jmcfx.domain.model.StackFrameInfo;

public class FlameGraphView extends Pane {

    private static final double FRAME_HEIGHT = 24;
    private static final double FRAME_GAP = 2;
    private static final double DEFAULT_WIDTH = 960;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 3.0;
    private static final double ZOOM_STEP = 1.2;
    private static final int PALETTE_DEPTH_COUNT = 8;
    private static final String ORIENTATION_FLAME_CLASS = "flame-graph-orientation-flame";
    private static final String ORIENTATION_ICICLE_CLASS = "flame-graph-orientation-icicle";
    private static final String SELECTED_FRAME_CLASS = "flame-graph-frame-selected";
    private static final String PATH_FRAME_CLASS = "flame-graph-frame-path";
    private static final String MUTED_FRAME_CLASS = "flame-graph-frame-muted";

    private FlameGraphLayout layout = FlameGraphLayout.EMPTY;
    private FlameGraphLayout displayedLayout = FlameGraphLayout.EMPTY;
    private FlameGraphFrame selectedFrame;
    private FlameGraphFrame focusedFrame;
    private FlameGraphFrame lastClickedFrame;
    private long lastClickNanos;
    private final Label emptyLabel = new Label();
    private final StringProperty emptyText = new SimpleStringProperty(this, "emptyText", "");
    private final ReadOnlyBooleanWrapper hasFrames = new ReadOnlyBooleanWrapper(this, "hasFrames", false);
    private final ReadOnlyObjectWrapper<FlameGraphFrame> focusedFrameProperty =
            new ReadOnlyObjectWrapper<>(this, "focusedFrame");
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
        updateOrientationStyleClass();
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
        orientation.addListener((obs, old, val) -> {
            updateOrientationStyleClass();
            requestLayout();
        });
        emptyText.addListener((obs, old, val) -> {
            rebuildChildren();
            requestLayout();
        });
        setMinHeight(0);
    }

    public void setLayout(FlameGraphLayout layout) {
        this.layout = layout == null ? FlameGraphLayout.EMPTY : layout;
        selectedFrame = null;
        focusedFrame = null;
        lastClickedFrame = null;
        lastClickNanos = 0;
        focusedFrameProperty.set(null);
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

    public ReadOnlyObjectProperty<FlameGraphFrame> focusedFrameProperty() {
        return focusedFrameProperty.getReadOnlyProperty();
    }

    public FlameGraphFrame getFocusedFrame() {
        return focusedFrameProperty.get();
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
        focusedFrame = null;
        focusedFrameProperty.set(null);
        clearSelection();
        lastClickedFrame = null;
        lastClickNanos = 0;
        setZoomScale(1.0);
        rebuildChildren();
        requestLayout();
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

    public void clearSelection() {
        selectedFrame = null;
        resetClickTracking();
        applySelectionStyles();
    }

    private void rebuildChildren() {
        getChildren().clear();
        displayedLayout = focusedLayout();
        hasFrames.set(!displayedLayout.frames().isEmpty());
        if (displayedLayout.frames().isEmpty()) {
            if (!getEmptyText().isBlank()) {
                emptyLabel.setText(getEmptyText());
                getChildren().add(emptyLabel);
            }
            return;
        }
        for (FlameGraphFrame frame : this.displayedLayout.frames()) {
            getChildren().add(frameNode(frame));
        }
    }

    public int frameCount() {
        return displayedLayout.frames().size();
    }

    @Override
    protected void layoutChildren() {
        double left = snappedLeftInset();
        double top = snappedTopInset();
        double contentWidth = Math.max(0, getWidth() - left - snappedRightInset());
        double rowHeight = snapSizeY(FRAME_HEIGHT);
        if (displayedLayout.frames().isEmpty()) {
            if (getChildren().contains(emptyLabel)) {
                emptyLabel.resizeRelocate(left, top, contentWidth, rowHeight);
            }
            return;
        }
        for (int index = 0; index < displayedLayout.frames().size(); index++) {
            FlameGraphFrame frame = displayedLayout.frames().get(index);
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
        return heightForDepth(displayedLayout.maxDepth()) + snappedTopInset() + snappedBottomInset();
    }

    private StackPane frameNode(FlameGraphFrame frame) {
        StackPane node = new StackPane();
        node.getStyleClass().add("flame-graph-frame");
        node.getStyleClass().add(depthStyleClass(frame.depth()));
        String details = frameDetails(frame);
        node.setAccessibleText(details);
        Tooltip.install(node, frameTooltip(details));
        node.setOnMouseClicked(event -> {
            if (isDoubleClick(frame, event)) {
                focusFrame(frame);
            } else {
                selectedFrame = frame.equals(selectedFrame) ? null : frame;
                applySelectionStyles();
            }
            if (event != null) {
                event.consume();
            }
        });
        Label label = new Label(frameLabel(frame));
        label.getStyleClass().add("flame-graph-frame-label");
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinWidth(0);
        label.setMouseTransparent(true);
        node.getChildren().add(label);
        return node;
    }

    private boolean isDoubleClick(FlameGraphFrame frame, javafx.scene.input.MouseEvent event) {
        if (event != null && event.getClickCount() >= 2) {
            resetClickTracking();
            return true;
        }
        long now = System.nanoTime();
        boolean doubleClick = frame.equals(lastClickedFrame)
                && now - lastClickNanos <= 500_000_000L;
        lastClickedFrame = frame;
        lastClickNanos = now;
        if (doubleClick) {
            resetClickTracking();
        }
        return doubleClick;
    }

    private void resetClickTracking() {
        lastClickedFrame = null;
        lastClickNanos = 0;
    }

    private FlameGraphLayout focusedLayout() {
        if (focusedFrame == null) {
            return layout;
        }
        double focusStart = focusedFrame.x();
        double focusWidth = focusedFrame.width();
        if (focusWidth <= 0) {
            return layout;
        }
        var focusedFrames = layout.frames().stream()
                .filter(frame -> frame.equals(focusedFrame) || isStackPathFrame(frame, focusedFrame))
                .map(frame -> normalizeFrame(frame, focusStart, focusWidth))
                .toList();
        if (focusedFrames.isEmpty()) {
            return FlameGraphLayout.EMPTY;
        }
        int maxDepth = focusedFrames.stream()
                .mapToInt(FlameGraphFrame::depth)
                .max()
                .orElse(0) + 1;
        return new FlameGraphLayout(focusedFrames, maxDepth);
    }

    private FlameGraphFrame normalizeFrame(FlameGraphFrame frame, double focusStart, double focusWidth) {
        if (frame.depth() < focusedFrame.depth()) {
            return new FlameGraphFrame(
                    frame.method(),
                    frame.count(),
                    frame.percentage(),
                    frame.frameInfo(),
                    frame.depth(),
                    0,
                    1);
        }
        return new FlameGraphFrame(
                frame.method(),
                frame.count(),
                frame.percentage(),
                frame.frameInfo(),
                frame.depth(),
                (frame.x() - focusStart) / focusWidth,
                frame.width() / focusWidth);
    }

    private void focusFrame(FlameGraphFrame frame) {
        FlameGraphFrame originalFrame = originalFrame(frame);
        if (originalFrame == null) {
            return;
        }
        focusedFrame = originalFrame;
        focusedFrameProperty.set(originalFrame);
        setZoomScale(1.0);
        rebuildChildren();
        selectedFrame = displayedFrameFor(originalFrame);
        applySelectionStyles();
        requestLayout();
    }

    private FlameGraphFrame originalFrame(FlameGraphFrame frame) {
        return layout.frames().stream()
                .filter(frame::equals)
                .findFirst()
                .orElseGet(() -> layout.frames().stream()
                        .filter(candidate -> sameFrameIdentity(candidate, frame))
                        .findFirst()
                        .orElse(null));
    }

    private boolean sameFrameIdentity(FlameGraphFrame left, FlameGraphFrame right) {
        return left.method().equals(right.method())
                && left.count() == right.count()
                && left.depth() == right.depth()
                && Double.compare(left.percentage(), right.percentage()) == 0;
    }

    private FlameGraphFrame displayedFrameFor(FlameGraphFrame frame) {
        return displayedLayout.frames().stream()
                .filter(candidate -> sameFrameIdentity(candidate, frame))
                .findFirst()
                .orElse(frame);
    }

    private Tooltip frameTooltip(String details) {
        Tooltip tooltip = new Tooltip(details);
        tooltip.getStyleClass().add("flame-graph-tooltip");
        tooltip.setShowDuration(Duration.INDEFINITE);
        return tooltip;
    }

    private void applySelectionStyles() {
        for (int index = 0; index < displayedLayout.frames().size(); index++) {
            FlameGraphFrame frame = displayedLayout.frames().get(index);
            NodeState state = nodeState(frame);
            StackPane node = (StackPane) getChildren().get(index);
            node.getStyleClass().removeAll(SELECTED_FRAME_CLASS, PATH_FRAME_CLASS, MUTED_FRAME_CLASS);
            if (state != NodeState.DEFAULT) {
                node.getStyleClass().add(state.styleClass());
            }
        }
    }

    private NodeState nodeState(FlameGraphFrame frame) {
        if (selectedFrame == null) {
            return NodeState.DEFAULT;
        }
        if (frame.equals(selectedFrame)) {
            return NodeState.SELECTED;
        }
        if (isStackPathFrame(frame, selectedFrame)) {
            return NodeState.PATH;
        }
        return NodeState.MUTED;
    }

    private boolean isStackPathFrame(FlameGraphFrame candidate, FlameGraphFrame selected) {
        if (candidate.depth() == selected.depth()) {
            return false;
        }
        double candidateStart = candidate.x();
        double candidateEnd = candidate.x() + candidate.width();
        double selectedStart = selected.x();
        double selectedEnd = selected.x() + selected.width();
        double tolerance = 0.000001;
        if (candidate.depth() < selected.depth()) {
            return containsRange(candidateStart, candidateEnd, selectedStart, selectedEnd, tolerance);
        }
        return containsRange(selectedStart, selectedEnd, candidateStart, candidateEnd, tolerance);
    }

    private boolean containsRange(double outerStart, double outerEnd, double innerStart, double innerEnd, double tolerance) {
        return outerStart <= innerStart + tolerance && outerEnd + tolerance >= innerEnd;
    }

    private double heightForDepth(int depth) {
        if (depth <= 0) {
            return 0;
        }
        return (depth * FRAME_HEIGHT) + ((depth - 1) * FRAME_GAP);
    }

    private double yForDepth(int depth) {
        if (getOrientation() == Orientation.FLAME) {
            int row = Math.max(0, displayedLayout.maxDepth() - depth - 1);
            return row * (FRAME_HEIGHT + FRAME_GAP);
        }
        return depth * (FRAME_HEIGHT + FRAME_GAP);
    }

    private String depthStyleClass(int depth) {
        return "flame-graph-depth-" + Math.floorMod(depth, PALETTE_DEPTH_COUNT);
    }

    private void updateOrientationStyleClass() {
        getStyleClass().removeAll(ORIENTATION_FLAME_CLASS, ORIENTATION_ICICLE_CLASS);
        getStyleClass().add(getOrientation() == Orientation.FLAME
                ? ORIENTATION_FLAME_CLASS
                : ORIENTATION_ICICLE_CLASS);
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
        StackFrameInfo info = frame.frameInfo();
        String title = firstPresent(info.label(), info.methodName(), shortMethodLabel(frame.method()), frame.method());
        StringBuilder details = new StringBuilder(title);
        appendDetail(details, "Package", info.packageName());
        appendDetail(details, "Class", info.typeName());
        appendDetail(details, "Weight", String.format(Locale.ROOT, "%,d samples, %.1f%%",
                frame.count(), frame.percentage()));
        appendDetail(details, "Type", info.frameType());
        appendDetail(details, "Byte Code Index", info.bci());
        appendDetail(details, "Line", info.lineNumber());
        return details.toString();
    }

    private String frameLabel(FlameGraphFrame frame) {
        return firstPresent(frame.frameInfo().label(), shortMethodLabel(frame.method()), frame.method());
    }

    private static void appendDetail(StringBuilder details, String label, Object value) {
        if (value == null) {
            return;
        }
        String text = value.toString();
        if (text.isBlank()) {
            return;
        }
        details.append(System.lineSeparator())
                .append(label)
                .append(": ")
                .append(text);
    }

    private static String shortMethodLabel(String formattedFrame) {
        if (formattedFrame == null || formattedFrame.isBlank()) {
            return "";
        }
        int parenIndex = formattedFrame.indexOf('(');
        String prefix = parenIndex >= 0 ? formattedFrame.substring(0, parenIndex) : formattedFrame;
        String suffix = parenIndex >= 0 ? formattedFrame.substring(parenIndex) : "";
        int methodSeparator = prefix.lastIndexOf('.');
        if (methodSeparator <= 0) {
            return formattedFrame;
        }
        int classSeparator = prefix.lastIndexOf('.', methodSeparator - 1);
        return classSeparator < 0 ? prefix.substring(methodSeparator + 1) + suffix : prefix.substring(classSeparator + 1) + suffix;
    }

    private static String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private enum NodeState {
        DEFAULT(""),
        SELECTED(SELECTED_FRAME_CLASS),
        PATH(PATH_FRAME_CLASS),
        MUTED(MUTED_FRAME_CLASS);

        private final String styleClass;

        NodeState(String styleClass) {
            this.styleClass = styleClass;
        }

        private String styleClass() {
            return styleClass;
        }
    }
}
