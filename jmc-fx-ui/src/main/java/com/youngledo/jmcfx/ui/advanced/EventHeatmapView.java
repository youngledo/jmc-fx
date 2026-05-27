package com.youngledo.jmcfx.ui.advanced;

import java.util.function.Consumer;

import com.youngledo.jmcfx.domain.model.EventHeatmap;
import com.youngledo.jmcfx.domain.model.EventHeatmapCell;
import com.youngledo.jmcfx.domain.model.EventHeatmapRow;

import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class EventHeatmapView extends VBox {

    private static final double ROW_LABEL_MIN_WIDTH = 220;
    private static final double ROW_LABEL_PREF_WIDTH = 320;
    private static final double ROW_LABEL_MAX_WIDTH = 380;
    private static final double CELL_WIDTH = 28;

    private final GridPane grid = new GridPane();
    private Consumer<EventHeatmapCell> selectionHandler = cell -> { };
    private Region selectedRegion;
    private int cellCount;

    public EventHeatmapView() {
        getStyleClass().add("event-heatmap-view");
        grid.getStyleClass().add("event-heatmap-grid");
        getChildren().add(grid);
    }

    public void setOnCellSelected(Consumer<EventHeatmapCell> handler) {
        selectionHandler = handler == null ? cell -> { } : handler;
    }

    public void setHeatmap(EventHeatmap heatmap) {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        grid.getRowConstraints().clear();
        selectedRegion = null;
        cellCount = 0;
        if (heatmap == null || heatmap.rows().isEmpty()) {
            return;
        }
        grid.add(header("Event Type"), 0, 0);
        grid.getColumnConstraints().add(new ColumnConstraints(
                ROW_LABEL_MIN_WIDTH, ROW_LABEL_PREF_WIDTH, ROW_LABEL_MAX_WIDTH));
        for (int column = 0; column < heatmap.bucketCount(); column++) {
            grid.add(header(Integer.toString(column + 1)), column + 1, 0);
            grid.getColumnConstraints().add(new ColumnConstraints(CELL_WIDTH, CELL_WIDTH, CELL_WIDTH));
        }
        long max = Math.max(1, heatmap.maxCellCount());
        int rowIndex = 1;
        for (EventHeatmapRow row : heatmap.rows()) {
            grid.add(rowLabel(row), 0, rowIndex);
            grid.getRowConstraints().add(new RowConstraints(28));
            for (int column = 0; column < row.cells().size(); column++) {
                Region region = cellRegion(row.cells().get(column), max);
                grid.add(region, column + 1, rowIndex);
                cellCount++;
            }
            rowIndex++;
        }
    }

    public int cellCount() {
        return cellCount;
    }

    private Text header(String text) {
        Text label = new Text(text);
        label.getStyleClass().add("event-heatmap-header");
        return label;
    }

    private Label rowLabel(EventHeatmapRow row) {
        Label label = new Label(row.label());
        label.getStyleClass().add("event-heatmap-row-label");
        label.setMinWidth(ROW_LABEL_MIN_WIDTH);
        label.setPrefWidth(ROW_LABEL_PREF_WIDTH);
        label.setMaxWidth(ROW_LABEL_MAX_WIDTH);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        return label;
    }

    private Region cellRegion(EventHeatmapCell cell, long max) {
        Region region = new Region();
        region.getStyleClass().add("event-heatmap-cell");
        region.setMinSize(CELL_WIDTH, 20);
        region.setPrefSize(CELL_WIDTH, 22);
        region.setMaxSize(CELL_WIDTH, 22);
        double intensity = cell.count() <= 0 ? 0 : Math.max(0.18, Math.min(1.0, cell.count() / (double) max));
        region.setOpacity(0.35 + (0.65 * intensity));
        region.setOnMouseClicked(event -> select(region, cell));
        return region;
    }

    private void select(Region region, EventHeatmapCell cell) {
        if (selectedRegion != null) {
            selectedRegion.getStyleClass().remove("event-heatmap-cell-selected");
        }
        selectedRegion = region;
        if (!region.getStyleClass().contains("event-heatmap-cell-selected")) {
            region.getStyleClass().add("event-heatmap-cell-selected");
        }
        selectionHandler.accept(cell);
    }
}
