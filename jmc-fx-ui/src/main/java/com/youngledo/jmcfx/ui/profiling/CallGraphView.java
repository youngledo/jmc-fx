package com.youngledo.jmcfx.ui.profiling;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;

public class CallGraphView extends Pane {

    private static final double MIN_HEIGHT = 220;
    private static final double NODE_WIDTH = 180;
    private static final double NODE_HEIGHT = 34;
    private static final double HORIZONTAL_MARGIN = 32;
    private static final double VERTICAL_MARGIN = 28;
    private static final double DEPTH_WIDTH = 240;
    private static final double ROW_HEIGHT = 72;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 3.0;
    private static final double ZOOM_STEP = 1.2;

    private CallGraphLayout layout = CallGraphLayout.EMPTY;
    private final Label emptyLabel = new Label();
    private final StringProperty emptyText = new SimpleStringProperty(this, "emptyText", "");
    private final DoubleProperty zoomScale = new SimpleDoubleProperty(this, "zoomScale", 1.0);

    public CallGraphView() {
        getStyleClass().add("call-graph-view");
        emptyLabel.getStyleClass().add("call-graph-empty");
        emptyLabel.setManaged(false);
        emptyLabel.setMouseTransparent(true);
        zoomScale.addListener((obs, old, val) -> {
            double zoom = clampZoom(val.doubleValue());
            if (Double.compare(zoom, val.doubleValue()) != 0) {
                zoomScale.set(zoom);
                return;
            }
            updateScaledSize();
            requestLayout();
        });
        emptyText.addListener((obs, old, val) -> {
            rebuildChildren();
            requestLayout();
        });
        setMinHeight(MIN_HEIGHT);
    }

    public void setLayout(CallGraphLayout layout) {
        this.layout = layout == null ? CallGraphLayout.EMPTY : layout;
        rebuildChildren();
        updateScaledSize();
        requestLayout();
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

    public int nodeCount() {
        return layout.nodes().size();
    }

    public int edgeCount() {
        return layout.edges().size();
    }

    public DoubleProperty zoomScaleProperty() {
        return zoomScale;
    }

    public void zoomIn() {
        setZoomScale(zoomScale.get() * ZOOM_STEP);
    }

    public void zoomOut() {
        setZoomScale(zoomScale.get() / ZOOM_STEP);
    }

    public void zoomBy(double factor) {
        if (factor <= 0 || Double.isNaN(factor)) {
            return;
        }
        setZoomScale(zoomScale.get() * factor);
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

    private void setZoomScale(double zoom) {
        zoomScale.set(clampZoom(zoom));
    }

    private void rebuildChildren() {
        getChildren().clear();
        if (layout.nodes().isEmpty()) {
            emptyLabel.setText(getEmptyText());
            getChildren().add(emptyLabel);
            return;
        }
        for (CallGraphEdge edge : layout.edges()) {
            Line line = new Line();
            line.getStyleClass().add("call-graph-edge");
            if (edge.percentage() >= 70) {
                line.getStyleClass().add("call-graph-edge-strong");
            }
            line.setManaged(false);
            line.setMouseTransparent(true);
            line.setUserData(edge);
            getChildren().add(line);
        }
        for (CallGraphNode graphNode : layout.nodes()) {
            getChildren().add(nodeView(graphNode));
        }
    }

    @Override
    protected void layoutChildren() {
        double left = snappedLeftInset();
        double top = snappedTopInset();
        double contentWidth = Math.max(computeScaledBaseWidth(), getWidth() - left - snappedRightInset());
        double contentHeight = Math.max(computeScaledBaseHeight(), getHeight() - top - snappedBottomInset());
        if (layout.nodes().isEmpty()) {
            if (getChildren().contains(emptyLabel)) {
                emptyLabel.resizeRelocate(left, top, contentWidth, snapSizeY(NODE_HEIGHT * zoomScale.get()));
            }
            return;
        }

        Map<String, Bounds> nodeBounds = new HashMap<>();
        double nodeWidth = snapSizeX(NODE_WIDTH * zoomScale.get());
        double nodeHeight = snapSizeY(NODE_HEIGHT * zoomScale.get());
        double xRange = Math.max(0, contentWidth - nodeWidth);
        double yRange = Math.max(0, contentHeight - nodeHeight);

        for (Node child : getChildren()) {
            if (child.getUserData() instanceof CallGraphNode graphNode) {
                double x = left + snapPositionX(graphNode.x() * xRange);
                double y = top + snapPositionY(graphNode.y() * yRange);
                child.resizeRelocate(x, y, nodeWidth, nodeHeight);
                nodeBounds.put(graphNode.id(), new Bounds(
                        x + (nodeWidth / 2),
                        y + (nodeHeight / 2)));
            }
        }

        for (Node child : getChildren()) {
            if (child instanceof Line line && line.getUserData() instanceof CallGraphEdge edge) {
                Bounds source = nodeBounds.get(edge.sourceId());
                Bounds target = nodeBounds.get(edge.targetId());
                if (source == null || target == null) {
                    line.setVisible(false);
                    continue;
                }
                line.setVisible(true);
                line.setStartX(source.centerX());
                line.setStartY(source.centerY());
                line.setEndX(target.centerX());
                line.setEndY(target.centerY());
            }
        }
    }

    @Override
    protected double computePrefWidth(double height) {
        return computeScaledBaseWidth() + snappedLeftInset() + snappedRightInset();
    }

    @Override
    protected double computePrefHeight(double width) {
        return computeScaledBaseHeight() + snappedTopInset() + snappedBottomInset();
    }

    private double computeBaseWidth() {
        if (layout.nodes().isEmpty()) {
            return NODE_WIDTH + (HORIZONTAL_MARGIN * 2);
        }
        return Math.max(NODE_WIDTH + (HORIZONTAL_MARGIN * 2),
                NODE_WIDTH + (HORIZONTAL_MARGIN * 2) + (layout.maxDepth() * DEPTH_WIDTH));
    }

    private double computeBaseHeight() {
        if (layout.nodes().isEmpty()) {
            return MIN_HEIGHT;
        }
        long largestLevel = layout.nodes().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        CallGraphNode::depth,
                        java.util.stream.Collectors.counting()))
                .values()
                .stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(1);
        double levelHeight = VERTICAL_MARGIN * 2 + NODE_HEIGHT + Math.max(0, largestLevel - 1) * ROW_HEIGHT;
        double depthHeight = VERTICAL_MARGIN * 2 + NODE_HEIGHT + Math.max(0, layout.maxDepth()) * ROW_HEIGHT;
        return Math.max(MIN_HEIGHT, Math.max(levelHeight, depthHeight));
    }

    private double computeScaledBaseWidth() {
        return computeBaseWidth() * zoomScale.get();
    }

    private double computeScaledBaseHeight() {
        return computeBaseHeight() * zoomScale.get();
    }

    private void updateScaledSize() {
        setPrefWidth(computePrefWidth(-1));
        setPrefHeight(computePrefHeight(-1));
    }

    private StackPane nodeView(CallGraphNode graphNode) {
        StackPane node = new StackPane();
        node.getStyleClass().add("call-graph-node");
        if (graphNode.primary()) {
            node.getStyleClass().add("call-graph-node-primary");
        }
        node.setUserData(graphNode);

        Label label = new Label(graphNode.label());
        label.getStyleClass().add("call-graph-node-label");
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinWidth(0);
        label.setMouseTransparent(true);
        node.getChildren().add(label);
        return node;
    }

    private double clampZoom(double zoom) {
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
    }

    private record Bounds(double centerX, double centerY) {
    }
}
