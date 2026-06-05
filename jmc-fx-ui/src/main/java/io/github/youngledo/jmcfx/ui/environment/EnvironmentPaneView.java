package io.github.youngledo.jmcfx.ui.environment;

import io.github.youngledo.jmcfx.domain.model.ActiveRecordingInfo;
import io.github.youngledo.jmcfx.domain.model.ActiveSetting;
import io.github.youngledo.jmcfx.domain.model.AgentInfo;
import io.github.youngledo.jmcfx.domain.model.ConstantPoolType;
import io.github.youngledo.jmcfx.domain.model.EnvironmentVariable;
import io.github.youngledo.jmcfx.domain.model.ProcessInfo;
import io.github.youngledo.jmcfx.domain.model.SystemProperty;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/// Code-first view for Environment recording data pages.
public final class EnvironmentPaneView {

    private final TableView<ProcessInfo> processesTable = denseTable();
    private final TableView<EnvironmentVariable> envVarsTable = denseTable();
    private final TextField envVarsSearchField = new TextField();
    private final TableView<SystemProperty> sysPropsTable = denseTable();
    private final TextField sysPropsSearchField = new TextField();
    private final TableView<ActiveRecordingInfo> recordingsTable = denseTable();
    private final TableView<ActiveSetting> settingsTable = denseTable();
    private final TabPane recordingInfoTabs = new TabPane();
    private final Tab recordingInfoRecordingsTab = tab();
    private final Tab recordingInfoSettingsTab = tab();
    private final TableView<AgentInfo> agentsTable = denseTable();
    private final TableView<ConstantPoolType> constantPoolsTable = denseTable();
    private final Label processesTitleLabel = new Label();
    private final Label envVarsTitleLabel = new Label();
    private final Label sysPropsTitleLabel = new Label();
    private final Label recordingInfoTitleLabel = new Label();
    private final Label agentsTitleLabel = new Label();
    private final Label constantPoolsTitleLabel = new Label();

    public EnvironmentPaneView(VBox processesPane, VBox envVarsPane, VBox sysPropsPane,
            VBox recordingInfoPane, VBox agentsPane, VBox constantPoolsPane) {
        configure(processesPane, envVarsPane, sysPropsPane, recordingInfoPane, agentsPane, constantPoolsPane);
    }

    public EnvironmentPagesView view() {
        return new EnvironmentPagesView(processesTitleLabel, processesTable, envVarsTitleLabel,
                envVarsSearchField, envVarsTable, sysPropsTitleLabel, sysPropsSearchField, sysPropsTable,
                recordingInfoTitleLabel, recordingInfoRecordingsTab, recordingInfoSettingsTab, recordingsTable,
                settingsTable, agentsTitleLabel, agentsTable, constantPoolsTitleLabel, constantPoolsTable);
    }

    private void configure(VBox processesPane, VBox envVarsPane, VBox sysPropsPane,
            VBox recordingInfoPane, VBox agentsPane, VBox constantPoolsPane) {
        configureTablePage(processesPane, processesTitleLabel, processesTable);
        configureTablePage(envVarsPane, envVarsTitleLabel, hbox(8, envVarsSearchField), envVarsTable);
        configureTablePage(sysPropsPane, sysPropsTitleLabel, hbox(8, sysPropsSearchField), sysPropsTable);
        tab(recordingInfoRecordingsTab, recordingsTable);
        tab(recordingInfoSettingsTab, settingsTable);
        recordingInfoTabs.getTabs().setAll(recordingInfoRecordingsTab, recordingInfoSettingsTab);
        configureTablePage(recordingInfoPane, recordingInfoTitleLabel, recordingInfoTabs);
        configureTablePage(agentsPane, agentsTitleLabel, agentsTable);
        configureTablePage(constantPoolsPane, constantPoolsTitleLabel, constantPoolsTable);
    }

    private void configureTablePage(VBox pane, Label title, Node... content) {
        pane.setSpacing(8);
        styles(title, "view-title");
        pane.getChildren().setAll(title);
        pane.getChildren().addAll(content);
        for (Node node : content) {
            if (node instanceof TableView<?> || node instanceof TabPane || node instanceof SplitPane) {
                VBox.setVgrow(node, Priority.ALWAYS);
            }
        }
    }

    private static HBox hbox(double spacing, Node... children) {
        return new HBox(spacing, children);
    }

    private static void tab(Tab tab, Node content) {
        tab.setClosable(false);
        tab.setContent(content);
    }

    private static Tab tab() {
        Tab tab = new Tab();
        tab.setClosable(false);
        return tab;
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
