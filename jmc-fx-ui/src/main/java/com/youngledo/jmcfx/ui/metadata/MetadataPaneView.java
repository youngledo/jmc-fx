package com.youngledo.jmcfx.ui.metadata;

import com.youngledo.jmcfx.domain.model.JfrMetadataEventType;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/// Code-first view for the JFR Metadata split table/detail page.
public final class MetadataPaneView {

    private final Label titleLabel = new Label();
    private final Label summaryLabel = new Label();
    private final TableView<JfrMetadataEventType> eventTypesTable = denseTable();
    private final Label detailTitleLabel = new Label();
    private final TextArea detailArea = textArea();

    public MetadataPaneView(VBox pane) {
        configure(pane);
    }

    public MetadataPageView view() {
        return new MetadataPageView(titleLabel, summaryLabel, eventTypesTable,
                detailTitleLabel, detailArea);
    }

    private void configure(VBox pane) {
        styles(pane, "page", "split-table-detail-page");
        styles(titleLabel, "view-title");
        styles(summaryLabel, "event-window-status");
        wrap(summaryLabel);
        styles(detailTitleLabel, "detail-panel-title");
        styles(detailArea, "detail-panel-body");
        readonly(detailArea);
        VBox detail = vbox(0, detailTitleLabel, detailArea);
        styles(detail, "detail-panel");
        SplitPane split = verticalSplit(0.62, eventTypesTable, detail);
        styles(split, "page-content");
        VBox header = vbox(0, titleLabel, summaryLabel);
        styles(header, "page-header");
        pane.getChildren().setAll(header, split);
        VBox.setVgrow(split, Priority.ALWAYS);
    }

    private static SplitPane verticalSplit(double dividerPosition, Node... children) {
        SplitPane split = new SplitPane(children);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(dividerPosition);
        return split;
    }

    private static VBox vbox(double spacing, Node... children) {
        return new VBox(spacing, children);
    }

    private static TextArea textArea() {
        TextArea area = new TextArea();
        area.setWrapText(true);
        return area;
    }

    private static <T> TableView<T> denseTable() {
        TableView<T> table = new TableView<>();
        styles(table, "dense-table");
        return table;
    }

    private static void readonly(TextArea... areas) {
        for (TextArea area : areas) {
            area.setEditable(false);
        }
    }

    private static void wrap(Label... labels) {
        for (Label label : labels) {
            label.setWrapText(true);
        }
    }

    private static void styles(Node node, String... styleClasses) {
        node.getStyleClass().addAll(styleClasses);
    }
}
