package com.youngledo.jmcfx.ui.profiling;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class FlameGraphView extends Pane {

    private static final double FRAME_HEIGHT = 24;
    private static final double FRAME_GAP = 2;

    private FlameGraphLayout layout = FlameGraphLayout.EMPTY;
    private final Label emptyLabel = new Label();
    private final StringProperty emptyText = new SimpleStringProperty(this, "emptyText", "");

    public FlameGraphView() {
        getStyleClass().add("flame-graph-view");
        emptyLabel.getStyleClass().add("flame-graph-empty");
        emptyLabel.setManaged(false);
        emptyLabel.setMouseTransparent(true);
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

    private void rebuildChildren() {
        getChildren().clear();
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
            double y = top + snapPositionY(frame.depth() * (FRAME_HEIGHT + FRAME_GAP));
            double width = snapSizeX(frame.width() * contentWidth);
            StackPane frameNode = (StackPane) getChildren().get(index);
            frameNode.setPrefSize(width, rowHeight);
            frameNode.resizeRelocate(x, y, width, rowHeight);
        }
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
