package io.github.youngledo.jmcfx.ui.analysis;

import io.github.youngledo.jmcfx.domain.model.DiagnosticFinding;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/// Code-first view for the recording Automated Analysis page.
public final class AnalysisPaneView {

    private final Label titleLabel = new Label();
    private final Label findingsSummaryLabel = new Label();
    private final Label highestSeverityLabel = new Label();
    private final Label ruleAnalysisStatusLabel = new Label();
    private final Label aiStatusLabel = new Label();
    private final TextField searchField = new TextField();
    private final Label minimumScoreLabel = new Label();
    private final Spinner<Integer> minimumScoreSpinner = new Spinner<>();
    private final CheckBox showOkCheckBox = new CheckBox();
    private final CheckBox showIgnoredCheckBox = new CheckBox();
    private final CheckBox showUnavailableCheckBox = new CheckBox();
    private final TableView<DiagnosticFinding> table = denseTable();
    private final Label detailTitleLabel = new Label();
    private final Label detailMetaLabel = new Label();
    private final Label detailExplanationCaption = new Label();
    private final Label detailExplanationArea = detailBodyLabel();
    private final Label detailRecommendationCaption = new Label();
    private final Label detailRecommendationArea = detailBodyLabel();
    private final Label aiTitleLabel = new Label();
    private final Button aiAnalyzeButton = new Button();
    private AiReportView aiReportView;
    private final TabPane detailTabs = new TabPane();
    private HBox aiActionBar;

    public AnalysisPaneView(VBox pane) {
        configure(pane);
    }

    public AnalysisPageView view() {
        return new AnalysisPageView(titleLabel, findingsSummaryLabel, highestSeverityLabel, ruleAnalysisStatusLabel,
                aiStatusLabel, searchField, minimumScoreLabel,
                minimumScoreSpinner, showOkCheckBox, showIgnoredCheckBox,
                showUnavailableCheckBox, table, detailTitleLabel, detailMetaLabel, detailExplanationCaption,
                detailExplanationArea, detailRecommendationCaption, detailRecommendationArea,
                aiTitleLabel, aiAnalyzeButton,
                aiReportView, aiActionBar, detailTabs);
    }

    private void configure(VBox pane) {
        pane.setSpacing(8);
        styles(pane, "split-table-detail-page");
        styles(titleLabel, "view-title");
        HBox summaryBar = hbox(12, findingsSummaryLabel, highestSeverityLabel,
                ruleAnalysisStatusLabel, aiStatusLabel);
        styles(summaryBar, "page-toolbar", "analysis-summary-bar");
        HBox filterBar = hbox(8, searchField, minimumScoreLabel, minimumScoreSpinner,
                showOkCheckBox, showIgnoredCheckBox, showUnavailableCheckBox);
        styles(filterBar, "page-toolbar", "analysis-filter-bar");
        styles(detailTitleLabel, "detail-panel-title");
        styles(detailMetaLabel, "detail-panel-meta");
        styles(detailExplanationCaption, "detail-section-label");
        styles(detailRecommendationCaption, "detail-section-label");
        styles(detailExplanationArea, "detail-panel-body");
        styles(detailRecommendationArea, "detail-panel-body");
        VBox ruleDetails = vbox(6, detailTitleLabel, detailMetaLabel,
                detailExplanationCaption, detailExplanationArea,
                detailRecommendationCaption, detailRecommendationArea);
        styles(ruleDetails, "detail-panel");
        Node ruleDetailsScroll = scrollingPanel(ruleDetails);
        Node aiAssistant = aiAssistantPanel();
        Tab rulesTab = new Tab();
        rulesTab.textProperty().set("Rules");
        rulesTab.setClosable(false);
        rulesTab.setContent(ruleDetailsScroll);
        Tab aiTab = new Tab();
        aiTab.textProperty().set("AI");
        aiTab.setClosable(false);
        aiTab.setContent(aiAssistant);
        detailTabs.getTabs().setAll(rulesTab, aiTab);
        styles(detailTabs, "page-detail-tabs");
        SplitPane split = new SplitPane(table, detailTabs);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.6);
        VBox.setVgrow(split, Priority.ALWAYS);
        pane.getChildren().setAll(titleLabel, summaryBar, filterBar, split);
    }

    private Node aiAssistantPanel() {
        styles(aiTitleLabel, "detail-panel-title");
        aiReportView = new AiReportView();
        aiActionBar = hbox(8, aiAnalyzeButton);
        styles(aiActionBar, "page-toolbar");
        VBox.setVgrow(aiReportView.node(), Priority.ALWAYS);
        VBox panel = vbox(6, aiTitleLabel, aiActionBar, aiReportView.node());
        panel.setFillWidth(true);
        panel.setMaxHeight(Double.MAX_VALUE);
        styles(panel, "detail-panel", "analysis-ai-panel");
        return panel;
    }

    private static ScrollPane scrollingPanel(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return scroll;
    }

    private static VBox vbox(double spacing, Node... children) {
        return new VBox(spacing, children);
    }

    private static HBox hbox(double spacing, Node... children) {
        return new HBox(spacing, children);
    }

    private static Label detailBodyLabel() {
        Label label = new Label();
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private static <T> TableView<T> denseTable() {
        TableView<T> table = new TableView<>();
        styles(table, "dense-table");
        return table;
    }

    private static void styles(Node node, String... styleClasses) {
        node.getStyleClass().addAll(styleClasses);
    }
}
