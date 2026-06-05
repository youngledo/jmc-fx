package io.github.youngledo.jmcfx.ui.advanced;

import io.github.youngledo.jmcfx.domain.model.MemoryIssue;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/// Code-first view for the Advanced JFR tabbed analysis page.
public final class AdvancedJfrPaneView {

    private final Label titleLabel = new Label();
    private final Label summaryLabel = new Label();
    private final TabPane tabs = new TabPane();
    private final Tab heatmapTab = tab();
    private final Tab memoryTab = tab();
    private final VBox heatmapContainer = new VBox();
    private final Label selectionTitleLabel = new Label();
    private final Label selectedEventTypeCaptionLabel = new Label();
    private final Label selectedEventTypeLabel = new Label();
    private final Label selectedCountCaptionLabel = new Label();
    private final Label selectedCountLabel = new Label();
    private final Label memorySummaryLabel = new Label();
    private final TableView<MemoryIssue> memoryTable = denseTable();
    private final Label memoryDetailTitleLabel = new Label();
    private final TextArea memoryDetailArea = textArea();

    public AdvancedJfrPaneView(VBox pane) {
        configure(pane);
    }

    public AdvancedJfrPageView view() {
        return new AdvancedJfrPageView(titleLabel, summaryLabel,
                heatmapTab, memoryTab, heatmapContainer,
                selectionTitleLabel, selectedEventTypeCaptionLabel,
                selectedEventTypeLabel, selectedCountCaptionLabel,
                selectedCountLabel, memorySummaryLabel, memoryTable,
                memoryDetailTitleLabel, memoryDetailArea);
    }

    private void configure(VBox pane) {
        pane.setSpacing(8);
        styles(titleLabel, "view-title");
        styles(summaryLabel, "event-window-status");
        wrap(summaryLabel, selectedEventTypeLabel, memorySummaryLabel);
        styles(heatmapContainer, "advanced-jfr-heatmap-content");
        VBox selection = vbox(6, selectionTitleLabel,
                hbox(16, vbox(2, selectedEventTypeCaptionLabel, selectedEventTypeLabel),
                        vbox(2, selectedCountCaptionLabel, selectedCountLabel)));
        styles(selection, "advanced-jfr-selection-pane");
        ScrollPane heatmapScroll = new ScrollPane(heatmapContainer);
        heatmapScroll.setFitToWidth(true);
        heatmapScroll.setFitToHeight(true);
        styles(heatmapScroll, "advanced-jfr-heatmap-scroll");
        tab(heatmapTab, vbox(8, heatmapScroll, selection));
        styles(memoryDetailTitleLabel, "detail-panel-title");
        styles(memoryDetailArea, "detail-panel-body");
        readonly(memoryDetailArea);
        VBox memoryDetail = vbox(0, memoryDetailTitleLabel, memoryDetailArea);
        styles(memoryDetail, "detail-panel");
        SplitPane memorySplit = verticalSplit(0.62, memoryTable, memoryDetail);
        tab(memoryTab, vbox(8, memorySummaryLabel, memorySplit));
        tabs.getTabs().setAll(heatmapTab, memoryTab);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        pane.getChildren().setAll(titleLabel, summaryLabel, tabs);
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

    private static HBox hbox(double spacing, Node... children) {
        return new HBox(spacing, children);
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

    private static void wrap(Label... labels) {
        for (Label label : labels) {
            label.setWrapText(true);
        }
    }

    private static void styles(Node node, String... styleClasses) {
        node.getStyleClass().addAll(styleClasses);
    }
}
