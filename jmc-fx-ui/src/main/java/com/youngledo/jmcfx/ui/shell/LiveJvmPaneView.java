package com.youngledo.jmcfx.ui.shell;

import com.youngledo.jmcfx.domain.model.DiagnosticCommandInfo;
import com.youngledo.jmcfx.domain.model.FlightRecordingInfo;
import com.youngledo.jmcfx.domain.model.JmcAgentPreset;
import com.youngledo.jmcfx.domain.model.JmcAgentTransform;
import com.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import com.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import com.youngledo.jmcfx.domain.model.JmxSubscriptionSample;
import com.youngledo.jmcfx.domain.model.JvmCapabilitySnapshot;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.LiveMetricDefinition;
import com.youngledo.jmcfx.domain.model.MBeanAttributeInfo;
import com.youngledo.jmcfx.domain.model.MBeanNode;
import com.youngledo.jmcfx.domain.model.MBeanOperationInfo;
import com.youngledo.jmcfx.domain.model.TriggerActionType;
import com.youngledo.jmcfx.domain.model.TriggerEvent;
import com.youngledo.jmcfx.domain.model.TriggerOperator;
import com.youngledo.jmcfx.domain.model.TriggerRule;
import com.youngledo.jmcfx.ui.jvms.LiveJvmOverviewMetric;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class LiveJvmPaneView {
    final VBox root = new VBox(8);
    final Label jvmsTitleLabel = new Label();
    final SplitPane jvmsWorkspaceSplit = new SplitPane();
    final VBox jvmsBrowserSidebar = new VBox(8);
    final Button jvmsRefreshButton = new Button();
    final TextField jvmsManualUrlField = new TextField();
    final Label jvmsManualUrlHintLabel = new Label();
    final TextField jvmsManualNameField = new TextField();
    final Button jvmsSaveTargetButton = new Button();
    final Button jvmsRemoveSavedTargetButton = new Button();
    final Button jvmsRefreshJdpButton = new Button();
    final Button jvmsConnectButton = new Button();
    final Button jvmsDisconnectButton = new Button();
    final Label jvmsSelectedConnectionStatusLabel = new Label();
    final TableView<JvmConnection> jvmsTable = denseTable();
    final VBox jvmsSessionDetailPane = new VBox(6);
    final TabPane jvmsLiveTabs = new TabPane();
    final Tab jvmsOverviewTab = tab();
    final Tab jvmsSessionTab = tab();
    final Tab jvmsMBeanTab = tab();
    final Tab jvmsDiagnosticsTab = tab();
    final Tab jvmsTriggersTab = tab();
    final Label jvmsSessionTitleLabel = new Label();
    final Label jvmsOverviewPersistenceTitleLabel = new Label();
    final Label jvmsOverviewPersistenceLabel = new Label();
    final TableView<LiveJvmOverviewMetric> jvmsOverviewPersistenceTable = denseTable();
    final Label jvmsOverviewDashboardTitleLabel = new Label();
    final TabPane jvmsOverviewDashboardTabs = new TabPane();
    final Tab jvmsOverviewDashboardChartTab = tab();
    final Tab jvmsOverviewDashboardTableTab = tab();
    final FlowPane jvmsOverviewDashboardMetricToggles = new FlowPane(0, 4);
    final LineChart<Number, Number> jvmsOverviewDashboardChart = lineChart(false);
    final TableView<LiveJvmOverviewMetric> jvmsOverviewDashboardTable = denseTable();
    final Label jvmsOverviewProcessorTitleLabel = new Label();
    final TabPane jvmsOverviewProcessorTabs = new TabPane();
    final Tab jvmsOverviewProcessorChartTab = tab();
    final Tab jvmsOverviewProcessorTableTab = tab();
    final FlowPane jvmsOverviewProcessorMetricToggles = new FlowPane(0, 4);
    final LineChart<Number, Number> jvmsOverviewProcessorChart = lineChart(false);
    final TableView<LiveJvmOverviewMetric> jvmsOverviewProcessorTable = denseTable();
    final Label jvmsOverviewMemoryTitleLabel = new Label();
    final TabPane jvmsOverviewMemoryTabs = new TabPane();
    final Tab jvmsOverviewMemoryChartTab = tab();
    final Tab jvmsOverviewMemoryTableTab = tab();
    final FlowPane jvmsOverviewMemoryMetricToggles = new FlowPane(0, 4);
    final LineChart<Number, Number> jvmsOverviewMemoryChart = lineChart(false);
    final TableView<LiveJvmOverviewMetric> jvmsOverviewMemoryTable = denseTable();
    final Label jvmsOverviewErrorLabel = new Label();
    final Label jvmsRuntimeSummaryLabel = new Label();
    final ListView<JvmCapabilitySnapshot> jvmsCapabilitiesList = new ListView<>();
    final Button jvmsStartRecordingButton = new Button();
    final Button jvmsStopRecordingButton = new Button();
    final TableView<FlightRecordingInfo> jvmsRecordingsTable = denseTable();
    final Label jvmsRecordingStatusLabel = new Label();
    final Label jvmsSessionErrorLabel = new Label();
    final TreeView<MBeanNode> jvmsMBeanTree = new TreeView<>();
    final TableView<MBeanAttributeInfo> jvmsMBeanAttributesTable = denseTable();
    final TableView<MBeanOperationInfo> jvmsMBeanOperationsTable = denseTable();
    final TextField jvmsMBeanOperationArgumentsField = new TextField();
    final Button jvmsRefreshMBeanButton = new Button();
    final Button jvmsInvokeMBeanOperationButton = new Button();
    final Label jvmsMBeanResultLabel = new Label();
    final Label jvmsMBeanErrorLabel = new Label();
    final TableView<DiagnosticCommandInfo> jvmsDiagnosticCommandsTable = denseTable();
    final TextField jvmsDiagnosticArgumentsField = new TextField();
    final Button jvmsExecuteDiagnosticCommandButton = new Button();
    final Button jvmsSaveDiagnosticOutputButton = new Button();
    final TextArea jvmsDiagnosticOutputArea = new TextArea();
    final Label jvmsDiagnosticErrorLabel = new Label();
    final TextField jvmsTriggerNameField = new TextField();
    final ComboBox<LiveMetricDefinition> jvmsTriggerMetricCombo = new ComboBox<>();
    final ComboBox<TriggerOperator> jvmsTriggerOperatorCombo = new ComboBox<>();
    final TextField jvmsTriggerThresholdField = new TextField();
    final ComboBox<TriggerActionType> jvmsTriggerActionCombo = new ComboBox<>();
    final ComboBox<DiagnosticCommandInfo> jvmsTriggerCommandCombo = new ComboBox<>();
    final Button jvmsAddTriggerButton = new Button();
    final Button jvmsRemoveTriggerButton = new Button();
    final Button jvmsEvaluateTriggersButton = new Button();
    final TableView<TriggerRule> jvmsTriggerRulesTable = denseTable();
    final TableView<TriggerEvent> jvmsTriggerEventsTable = denseTable();
    final Label jvmsTriggerErrorLabel = new Label();
    final Tab jvmsMonitoringTab = tab();
    final Button jvmsAddMonitoringSubscriptionButton = new Button();
    final Button jvmsSampleSubscriptionButton = new Button();
    final Button jvmsAddNotificationSubscriptionButton = new Button();
    final Button jvmsStartNotificationsButton = new Button();
    final Button jvmsStopNotificationsButton = new Button();
    final TableView<JmxAttributeSubscription> jvmsMonitoringSubscriptionsTable = denseTable();
    final LineChart<Number, Number> jvmsMonitoringChart = lineChart(true);
    final TableView<JmxSubscriptionSample> jvmsMonitoringSamplesTable = denseTable();
    final TableView<JmxNotificationEvent> jvmsMonitoringNotificationsTable = denseTable();
    final Label jvmsMonitoringErrorLabel = new Label();
    final Tab jvmsAgentTab = tab();
    final ComboBox<JmcAgentPreset> jvmsAgentPresetCombo = new ComboBox<>();
    final Button jvmsRefreshAgentButton = new Button();
    final Button jvmsLoadAgentPresetButton = new Button();
    final Button jvmsApplyAgentConfigurationButton = new Button();
    final TableView<JmcAgentTransform> jvmsAgentTransformsTable = denseTable();
    final Label jvmsAgentConfigurationTitleLabel = new Label();
    final TextArea jvmsAgentConfigurationArea = new TextArea();
    final Label jvmsAgentStatusLabel = new Label();

    LiveJvmPaneView() {
        root.setId("jvmsPane");
        addStyle(jvmsTitleLabel, "view-title");
        root.getChildren().setAll(jvmsTitleLabel, jvmsWorkspaceSplit);
        VBox.setVgrow(jvmsWorkspaceSplit, Priority.ALWAYS);

        configureBrowserSidebar();
        configureOverviewTab();
        configureSessionTab();
        configureMBeanTab();
        configureDiagnosticsTab();
        configureTriggersTab();
        configureMonitoringTab();
        configureAgentTab();

        addStyle(jvmsSessionDetailPane, "summary-panel", "jvms-session-detail");
        VBox.setVgrow(jvmsLiveTabs, Priority.ALWAYS);
        jvmsLiveTabs.getTabs().setAll(jvmsOverviewTab, jvmsSessionTab, jvmsMBeanTab, jvmsDiagnosticsTab,
                jvmsTriggersTab, jvmsMonitoringTab, jvmsAgentTab);
        jvmsSessionDetailPane.getChildren().setAll(jvmsLiveTabs);

        jvmsWorkspaceSplit.getItems().setAll(jvmsBrowserSidebar, jvmsSessionDetailPane);
        jvmsWorkspaceSplit.setDividerPositions(0.28);
    }

    private void configureBrowserSidebar() {
        jvmsBrowserSidebar.setMinWidth(280);
        jvmsBrowserSidebar.setPrefWidth(340);
        addStyle(jvmsBrowserSidebar, "summary-panel", "jvms-browser-sidebar");
        addStyle(jvmsManualUrlHintLabel, "event-window-status");
        jvmsManualUrlHintLabel.setWrapText(true);
        addStyle(jvmsSelectedConnectionStatusLabel, "event-window-status");
        jvmsSelectedConnectionStatusLabel.setWrapText(true);
        VBox.setVgrow(jvmsTable, Priority.ALWAYS);
        jvmsBrowserSidebar.getChildren().setAll(
                toolbar(jvmsRefreshButton, jvmsRefreshJdpButton),
                jvmsManualUrlField,
                jvmsManualUrlHintLabel,
                jvmsManualNameField,
                toolbar(jvmsSaveTargetButton, jvmsRemoveSavedTargetButton),
                toolbar(jvmsConnectButton, jvmsDisconnectButton),
                jvmsSelectedConnectionStatusLabel,
                jvmsTable);
    }

    private void configureOverviewTab() {
        VBox content = tabContent();
        SplitPane groups = new SplitPane(
                overviewGroup(jvmsOverviewPersistenceTitleLabel, jvmsOverviewPersistenceLabel,
                        jvmsOverviewPersistenceTable),
                overviewChartGroup(jvmsOverviewDashboardTitleLabel, jvmsOverviewDashboardTabs,
                        jvmsOverviewDashboardChartTab, jvmsOverviewDashboardChart, jvmsOverviewDashboardMetricToggles,
                        jvmsOverviewDashboardTableTab, jvmsOverviewDashboardTable),
                overviewChartGroup(jvmsOverviewProcessorTitleLabel, jvmsOverviewProcessorTabs,
                        jvmsOverviewProcessorChartTab, jvmsOverviewProcessorChart, jvmsOverviewProcessorMetricToggles,
                        jvmsOverviewProcessorTableTab, jvmsOverviewProcessorTable),
                overviewChartGroup(jvmsOverviewMemoryTitleLabel, jvmsOverviewMemoryTabs,
                        jvmsOverviewMemoryChartTab, jvmsOverviewMemoryChart, jvmsOverviewMemoryMetricToggles,
                        jvmsOverviewMemoryTableTab, jvmsOverviewMemoryTable));
        groups.setOrientation(Orientation.VERTICAL);
        groups.setDividerPositions(0.24, 0.49, 0.74);
        VBox.setVgrow(groups, Priority.ALWAYS);
        addStyle(jvmsOverviewErrorLabel, "unavailable-state");
        jvmsOverviewErrorLabel.setWrapText(true);
        content.getChildren().setAll(groups, jvmsOverviewErrorLabel);
        jvmsOverviewTab.setContent(content);
    }

    private void configureSessionTab() {
        VBox content = tabContent();
        addStyle(jvmsSessionTitleLabel, "detail-title");
        jvmsRuntimeSummaryLabel.setWrapText(true);
        jvmsRecordingStatusLabel.setWrapText(true);
        addStyle(jvmsSessionErrorLabel, "unavailable-state");
        jvmsSessionErrorLabel.setWrapText(true);
        VBox.setVgrow(jvmsRecordingsTable, Priority.ALWAYS);
        content.getChildren().setAll(jvmsSessionTitleLabel, jvmsRuntimeSummaryLabel, jvmsCapabilitiesList,
                hboxToolbar(jvmsStartRecordingButton, jvmsStopRecordingButton, jvmsRecordingStatusLabel),
                jvmsRecordingsTable, jvmsSessionErrorLabel);
        jvmsSessionTab.setContent(content);
    }

    private void configureMBeanTab() {
        SplitPane content = new SplitPane();
        addStyle(content, "jvms-live-tab-content");
        content.setDividerPositions(0.32);
        jvmsMBeanTree.setMinWidth(160);
        jvmsMBeanTree.setPrefWidth(260);
        HBox toolbar = hboxToolbar(jvmsRefreshMBeanButton, jvmsMBeanOperationArgumentsField,
                jvmsInvokeMBeanOperationButton);
        HBox.setHgrow(jvmsMBeanOperationArgumentsField, Priority.ALWAYS);
        VBox detail = new VBox(10, toolbar, jvmsMBeanAttributesTable, jvmsMBeanOperationsTable,
                jvmsMBeanResultLabel, jvmsMBeanErrorLabel);
        addStyle(detail, "jvms-live-workspace");
        VBox.setVgrow(jvmsMBeanAttributesTable, Priority.ALWAYS);
        VBox.setVgrow(jvmsMBeanOperationsTable, Priority.ALWAYS);
        jvmsMBeanResultLabel.setWrapText(true);
        addStyle(jvmsMBeanErrorLabel, "unavailable-state");
        jvmsMBeanErrorLabel.setWrapText(true);
        content.getItems().setAll(jvmsMBeanTree, detail);
        jvmsMBeanTab.setContent(content);
    }

    private void configureDiagnosticsTab() {
        SplitPane content = new SplitPane();
        addStyle(content, "jvms-live-tab-content");
        content.setDividerPositions(0.42);
        VBox.setVgrow(content, Priority.ALWAYS);
        HBox toolbar = hboxToolbar(jvmsDiagnosticArgumentsField, jvmsExecuteDiagnosticCommandButton,
                jvmsSaveDiagnosticOutputButton);
        HBox.setHgrow(jvmsDiagnosticArgumentsField, Priority.ALWAYS);
        jvmsDiagnosticOutputArea.setEditable(false);
        jvmsDiagnosticOutputArea.setWrapText(false);
        VBox.setVgrow(jvmsDiagnosticOutputArea, Priority.ALWAYS);
        addStyle(jvmsDiagnosticErrorLabel, "unavailable-state");
        jvmsDiagnosticErrorLabel.setWrapText(true);
        VBox detail = new VBox(10, toolbar, jvmsDiagnosticOutputArea, jvmsDiagnosticErrorLabel);
        addStyle(detail, "jvms-live-workspace");
        content.getItems().setAll(jvmsDiagnosticCommandsTable, detail);
        jvmsDiagnosticsTab.setContent(content);
    }

    private void configureTriggersTab() {
        VBox content = tabContent();
        jvmsTriggerNameField.setPrefWidth(180);
        jvmsTriggerMetricCombo.setPrefWidth(180);
        jvmsTriggerOperatorCombo.setPrefWidth(90);
        jvmsTriggerThresholdField.setPrefWidth(110);
        jvmsTriggerActionCombo.setPrefWidth(160);
        jvmsTriggerCommandCombo.setPrefWidth(180);
        FlowPane editor = toolbar(jvmsTriggerNameField, jvmsTriggerMetricCombo, jvmsTriggerOperatorCombo,
                jvmsTriggerThresholdField, jvmsTriggerActionCombo, jvmsTriggerCommandCombo, jvmsAddTriggerButton,
                jvmsRemoveTriggerButton, jvmsEvaluateTriggersButton);
        SplitPane tables = new SplitPane(jvmsTriggerRulesTable, jvmsTriggerEventsTable);
        tables.setOrientation(Orientation.VERTICAL);
        tables.setDividerPositions(0.5);
        VBox.setVgrow(tables, Priority.ALWAYS);
        addStyle(jvmsTriggerErrorLabel, "unavailable-state");
        jvmsTriggerErrorLabel.setWrapText(true);
        content.getChildren().setAll(editor, tables, jvmsTriggerErrorLabel);
        jvmsTriggersTab.setContent(content);
    }

    private void configureMonitoringTab() {
        VBox content = tabContent();
        SplitPane split = new SplitPane();
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.42);
        VBox.setVgrow(split, Priority.ALWAYS);
        VBox detail = new VBox(8);
        addStyle(detail, "jvms-monitoring-detail");
        SplitPane tables = new SplitPane(jvmsMonitoringSamplesTable, jvmsMonitoringNotificationsTable);
        tables.setOrientation(Orientation.VERTICAL);
        tables.setDividerPositions(0.5);
        VBox.setVgrow(tables, Priority.ALWAYS);
        detail.getChildren().setAll(jvmsMonitoringChart, tables);
        split.getItems().setAll(jvmsMonitoringSubscriptionsTable, detail);
        addStyle(jvmsMonitoringErrorLabel, "unavailable-state");
        jvmsMonitoringErrorLabel.setWrapText(true);
        content.getChildren().setAll(toolbar(jvmsAddMonitoringSubscriptionButton, jvmsSampleSubscriptionButton,
                jvmsAddNotificationSubscriptionButton, jvmsStartNotificationsButton, jvmsStopNotificationsButton),
                split, jvmsMonitoringErrorLabel);
        jvmsMonitoringTab.setContent(content);
    }

    private void configureAgentTab() {
        VBox content = tabContent();
        jvmsAgentPresetCombo.setPrefWidth(220);
        VBox configurationPane = new VBox();
        addStyle(configurationPane, "detail-panel");
        addStyle(jvmsAgentConfigurationTitleLabel, "detail-panel-title");
        jvmsAgentConfigurationArea.setEditable(true);
        jvmsAgentConfigurationArea.setWrapText(true);
        addStyle(jvmsAgentConfigurationArea, "detail-panel-body");
        VBox.setVgrow(jvmsAgentConfigurationArea, Priority.ALWAYS);
        configurationPane.getChildren().setAll(jvmsAgentConfigurationTitleLabel, jvmsAgentConfigurationArea);
        SplitPane split = new SplitPane(jvmsAgentTransformsTable, configurationPane);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.45);
        VBox.setVgrow(split, Priority.ALWAYS);
        addStyle(jvmsAgentStatusLabel, "unavailable-state");
        jvmsAgentStatusLabel.setWrapText(true);
        content.getChildren().setAll(toolbar(jvmsAgentPresetCombo, jvmsRefreshAgentButton,
                jvmsLoadAgentPresetButton, jvmsApplyAgentConfigurationButton), split, jvmsAgentStatusLabel);
        jvmsAgentTab.setContent(content);
    }

    private static VBox overviewGroup(Label title, Label summary, TableView<LiveJvmOverviewMetric> table) {
        VBox group = new VBox(6, title, summary, table);
        addStyle(group, "jvms-overview-group");
        addStyle(title, "detail-title");
        summary.setWrapText(true);
        return group;
    }

    private static VBox overviewChartGroup(Label title, TabPane tabs, Tab chartTab, LineChart<Number, Number> chart,
            FlowPane toggles, Tab tableTab, TableView<LiveJvmOverviewMetric> table) {
        VBox group = new VBox(6, title, tabs);
        addStyle(group, "jvms-overview-group");
        addStyle(title, "detail-title");
        addStyle(tabs, "page-detail-tabs");
        VBox.setVgrow(tabs, Priority.ALWAYS);
        addStyle(toggles, "jvms-overview-metric-toggles");
        toggles.setOrientation(Orientation.VERTICAL);
        ScrollPane togglesScroll = new ScrollPane(toggles);
        togglesScroll.setFitToWidth(true);
        togglesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        togglesScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        HBox chartPanel = new HBox(8, chart, togglesScroll);
        addStyle(chartPanel, "jvms-overview-chart-panel");
        HBox.setHgrow(chart, Priority.ALWAYS);
        chartTab.setContent(chartPanel);
        tableTab.setContent(table);
        tabs.getTabs().setAll(chartTab, tableTab);
        return group;
    }

    private static VBox tabContent() {
        VBox content = new VBox(10);
        addStyle(content, "jvms-live-tab-content");
        return content;
    }

    private static Tab tab() {
        Tab tab = new Tab();
        tab.setClosable(false);
        return tab;
    }

    private static FlowPane toolbar(Node... children) {
        FlowPane toolbar = new FlowPane(8, 8, children);
        addStyle(toolbar, "page-toolbar");
        return toolbar;
    }

    private static HBox hboxToolbar(Node... children) {
        HBox toolbar = new HBox(8, children);
        addStyle(toolbar, "page-toolbar");
        return toolbar;
    }

    private static <T> TableView<T> denseTable() {
        TableView<T> table = new TableView<>();
        addStyle(table, "dense-table");
        return table;
    }

    private static LineChart<Number, Number> lineChart(boolean legendVisible) {
        LineChart<Number, Number> chart = new LineChart<>(new NumberAxis(), new NumberAxis());
        addStyle(chart, "diagnostic-chart");
        chart.setAnimated(false);
        chart.setLegendVisible(legendVisible);
        chart.setCreateSymbols(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        chart.setAlternativeRowFillVisible(false);
        chart.setAlternativeColumnFillVisible(false);
        return chart;
    }

    private static void addStyle(Node node, String... styleClasses) {
        node.getStyleClass().addAll(styleClasses);
    }
}
