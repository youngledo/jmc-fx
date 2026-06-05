package io.github.youngledo.jmcfx.ui.gc;

import io.github.youngledo.jmcfx.domain.model.G1GcRegionState;
import io.github.youngledo.jmcfx.domain.model.G1GcRegionSummary;
import io.github.youngledo.jmcfx.domain.model.GcEvent;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/// Code-first view for the JFR G1 GC detail page.
public final class G1GcPaneView {

    private final Label titleLabel = new Label();
    private final Label summaryLabel = new Label();
    private final Label regionStatesLabel = new Label();
    private final Label regionSummaryLabel = new Label();
    private final Label pausesLabel = new Label();
    private final TableView<G1GcRegionSummary> regionSummaryTable = denseTable();
    private final TableView<G1GcRegionState> regionStatesTable = denseTable();
    private final TableView<GcEvent> pauseTable = denseTable();
    private final Label detailTitleLabel = new Label();
    private final TextArea detailArea = textArea();

    public G1GcPaneView(VBox pane) {
        configureDetailPage(pane, titleLabel, summaryLabel,
                vbox(8, regionStatesLabel, regionStatesTable),
                detailTitleLabel, detailArea,
                vbox(8, regionSummaryLabel, regionSummaryTable, pausesLabel, pauseTable));
    }

    public G1GcPageView view() {
        return new G1GcPageView(titleLabel, summaryLabel, regionStatesLabel,
                regionStatesTable, detailTitleLabel, detailArea, regionSummaryLabel,
                regionSummaryTable, pausesLabel, pauseTable);
    }

    private void configureDetailPage(VBox pane, Label title, Label summary, Node primary,
            Label detailTitle, TextArea detailArea, Node secondary) {
        styles(pane, "page", "split-table-detail-page");
        styles(title, "view-title");
        styles(summary, "event-window-status");
        wrap(summary);
        styles(detailTitle, "detail-panel-title");
        styles(detailArea, "detail-panel-body");
        readonly(detailArea);
        VBox detail = vbox(0, detailTitle, detailArea);
        styles(detail, "detail-panel");
        SplitPane split = verticalSplit(0.60, primary, detail, secondary);
        styles(split, "page-content");
        VBox header = vbox(0, title, summary);
        styles(header, "page-header");
        pane.getChildren().setAll(header, split);
        VBox.setVgrow(split, Priority.ALWAYS);
    }

    private static SplitPane verticalSplit(double dividerPosition, Node... nodes) {
        SplitPane split = new SplitPane(nodes);
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
