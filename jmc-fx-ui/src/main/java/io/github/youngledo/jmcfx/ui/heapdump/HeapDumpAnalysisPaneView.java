package io.github.youngledo.jmcfx.ui.heapdump;

import io.github.youngledo.jmcfx.domain.model.HeapDumpIssue;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/// Code-first view for the HPROF Heap Dump Analysis split table/detail page.
public final class HeapDumpAnalysisPaneView {

    private final Label titleLabel = new Label();
    private final TableView<HeapDumpIssue> issuesTable = denseTable();
    private final TabPane detailsTabs = new TabPane();
    private final Tab issueDetailTab = tab();
    private final Tab textReportTab = tab();
    private final Label issueDetailTitleLabel = new Label();
    private final TextArea issueDetailArea = textArea();
    private final TextArea textReportArea = textArea();

    public HeapDumpAnalysisPaneView(VBox pane) {
        configure(pane);
    }

    public HeapDumpAnalysisPageView view() {
        return new HeapDumpAnalysisPageView(titleLabel, issuesTable, detailsTabs,
                issueDetailTab, textReportTab, issueDetailTitleLabel,
                issueDetailArea, textReportArea);
    }

    private void configure(VBox pane) {
        styles(pane, "page", "split-table-detail-page", "heap-dump-page");
        styles(titleLabel, "view-title");
        VBox header = vbox(0, titleLabel);
        styles(header, "page-header");
        styles(issueDetailTitleLabel, "detail-panel-title");
        styles(issueDetailArea, "detail-panel-body");
        styles(textReportArea, "dump-text-area", "detail-panel-body");
        readonly(issueDetailArea, textReportArea);
        VBox issueDetail = vbox(0, issueDetailTitleLabel, issueDetailArea);
        styles(issueDetail, "detail-panel");
        VBox textReport = vbox(0, textReportArea);
        styles(textReport, "detail-panel");
        tab(issueDetailTab, issueDetail);
        tab(textReportTab, textReport);
        styles(detailsTabs, "page-detail-tabs");
        detailsTabs.getTabs().setAll(issueDetailTab, textReportTab);
        SplitPane content = verticalSplit(0.62, issuesTable, detailsTabs);
        styles(content, "page-content");
        pane.getChildren().setAll(header, content);
        VBox.setVgrow(content, Priority.ALWAYS);
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

    private static Tab tab() {
        Tab tab = new Tab();
        tab.setClosable(false);
        return tab;
    }

    private static void tab(Tab tab, Node content) {
        tab.setContent(content);
        tab.setClosable(false);
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

    private static void styles(Node node, String... styleClasses) {
        node.getStyleClass().addAll(styleClasses);
    }
}
