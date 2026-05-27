package com.youngledo.jmcfx.ui.profiling;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.SimpleStringProperty;
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

    private CallGraphLayout layout = CallGraphLayout.EMPTY;
    private final Label emptyLabel = new Label();
    private final StringProperty emptyText = new SimpleStringProperty(this, "emptyText", "");

    public CallGraphView() {
        getStyleClass().add("call-graph-view");
        emptyLabel.getStyleClass().add("call-graph-empty");
        emptyLabel.setManaged(false);
        emptyLabel.setMouseTransparent(true);
        emptyText.addListener((obs, old, val) -> {
            rebuildChildren();
            requestLayout();
        });
        setMinHeight(MIN_HEIGHT);
    }

    public void setLayout(CallGraphLayout layout) {
        this.layout = layout == null ? CallGraphLayout.EMPTY : layout;
        rebuildChildren();
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
        double contentWidth = Math.max(0, getWidth() - left - snappedRightInset());
        double contentHeight = Math.max(MIN_HEIGHT, getHeight() - top - snappedBottomInset());
        if (layout.nodes().isEmpty()) {
            if (getChildren().contains(emptyLabel)) {
                emptyLabel.resizeRelocate(left, top, contentWidth, snapSizeY(NODE_HEIGHT));
            }
            return;
        }

        Map<String, Bounds> nodeBounds = new HashMap<>();
        double nodeWidth = snapSizeX(NODE_WIDTH);
        double nodeHeight = snapSizeY(NODE_HEIGHT);
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
    protected double computePrefHeight(double width) {
        return MIN_HEIGHT + snappedTopInset() + snappedBottomInset();
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

    private record Bounds(double centerX, double centerY) {
    }
}
