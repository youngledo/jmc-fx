package com.youngledo.jmcfx.ui.profiling;

import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class FlameGraphView extends Pane {

    private static final double FRAME_HEIGHT = 24;
    private static final double FRAME_GAP = 2;

    private FlameGraphLayout layout = FlameGraphLayout.EMPTY;

    public FlameGraphView() {
        getStyleClass().add("flame-graph-view");
        setMinHeight(0);
    }

    public void setLayout(FlameGraphLayout layout) {
        this.layout = layout == null ? FlameGraphLayout.EMPTY : layout;
        getChildren().clear();
        for (FlameGraphFrame frame : this.layout.frames()) {
            getChildren().add(frameNode(frame));
        }
        requestLayout();
    }

    public int frameCount() {
        return getChildren().size();
    }

    @Override
    protected void layoutChildren() {
        double left = snappedLeftInset();
        double top = snappedTopInset();
        double contentWidth = Math.max(0, getWidth() - left - snappedRightInset());
        double rowHeight = snapSizeY(FRAME_HEIGHT);
        for (int index = 0; index < layout.frames().size(); index++) {
            FlameGraphFrame frame = layout.frames().get(index);
            double x = left + snapPositionX(frame.x() * contentWidth);
            double y = top + snapPositionY(frame.depth() * (FRAME_HEIGHT + FRAME_GAP));
            double width = snapSizeX(frame.width() * contentWidth);
            StackPane frameNode = (StackPane) getChildren().get(index);
            frameNode.setPrefSize(width, rowHeight);
            frameNode.resizeRelocate(x, y, width, rowHeight);
        }
    }

    @Override
    protected double computePrefHeight(double width) {
        return heightForDepth(layout.maxDepth()) + snappedTopInset() + snappedBottomInset();
    }

    private StackPane frameNode(FlameGraphFrame frame) {
        StackPane node = new StackPane();
        node.getStyleClass().add("flame-graph-frame");
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
}
