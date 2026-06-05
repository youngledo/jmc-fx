package io.github.youngledo.jmcfx.ui.environment;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ActiveRecordingInfo;
import io.github.youngledo.jmcfx.domain.model.ActiveSetting;
import io.github.youngledo.jmcfx.domain.model.AgentInfo;
import io.github.youngledo.jmcfx.domain.model.ConstantPoolType;
import io.github.youngledo.jmcfx.domain.model.EnvironmentVariable;
import io.github.youngledo.jmcfx.domain.model.ProcessInfo;
import io.github.youngledo.jmcfx.domain.model.SystemProperty;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/// Controller for Environment data table pages.
public final class EnvironmentPagesController {

    private final EnvironmentPagesView view;
    private final I18n i18n;
    private final ChangeListener<String> environmentSearchListener;
    private final ChangeListener<String> systemPropertySearchListener;
    private EnvironmentViewModel viewModel;

    public EnvironmentPagesController(EnvironmentPagesView view, I18n i18n) {
        this.view = view;
        this.i18n = i18n;
        environmentSearchListener = (observable, oldValue, newValue) -> {
            if (viewModel != null) {
                viewModel.setEnvironmentSearchFilter(newValue);
            }
        };
        systemPropertySearchListener = (observable, oldValue, newValue) -> {
            if (viewModel != null) {
                viewModel.setSystemPropertySearchFilter(newValue);
            }
        };
    }

    public void configure() {
        bindLocalizedText();
        configureProcessesTable();
        configureEnvVarsTable();
        configureSysPropsTable();
        configureRecordingsTable();
        configureSettingsTable();
        configureAgentsTable();
        configureConstantPoolsTable();
        view.envVarsSearchField().textProperty().addListener(environmentSearchListener);
        view.sysPropsSearchField().textProperty().addListener(systemPropertySearchListener);
        bind(null);
    }

    public List<TableView<?>> exportTables() {
        return List.of(view.processesTable(), view.envVarsTable(), view.sysPropsTable(), view.recordingsTable(),
                view.settingsTable(), view.agentsTable(), view.constantPoolsTable());
    }

    public void bind(EnvironmentViewModel nextViewModel) {
        view.processesTable().setItems(FXCollections.emptyObservableList());
        view.envVarsTable().setItems(FXCollections.emptyObservableList());
        view.sysPropsTable().setItems(FXCollections.emptyObservableList());
        view.recordingsTable().setItems(FXCollections.emptyObservableList());
        view.settingsTable().setItems(FXCollections.emptyObservableList());
        view.agentsTable().setItems(FXCollections.emptyObservableList());
        view.constantPoolsTable().setItems(FXCollections.emptyObservableList());
        viewModel = nextViewModel;
        if (nextViewModel == null) {
            view.envVarsSearchField().clear();
            view.sysPropsSearchField().clear();
            return;
        }
        view.processesTable().setItems(nextViewModel.processesProperty());
        view.envVarsTable().setItems(nextViewModel.filteredEnvironmentVariablesProperty());
        view.sysPropsTable().setItems(nextViewModel.filteredSystemPropertiesProperty());
        view.recordingsTable().setItems(nextViewModel.activeRecordingsProperty());
        view.settingsTable().setItems(nextViewModel.activeSettingsProperty());
        view.agentsTable().setItems(nextViewModel.agentsProperty());
        view.constantPoolsTable().setItems(nextViewModel.constantPoolsProperty());
        view.envVarsSearchField().setText(nextViewModel.environmentSearchFilterProperty().get());
        view.sysPropsSearchField().setText(nextViewModel.systemPropertySearchFilterProperty().get());
    }

    private void bindLocalizedText() {
        view.processesTitleLabel().textProperty().bind(i18n.text("processes.title"));
        view.envVarsTitleLabel().textProperty().bind(i18n.text("envVars.title"));
        view.sysPropsTitleLabel().textProperty().bind(i18n.text("sysProps.title"));
        view.recordingInfoTitleLabel().textProperty().bind(i18n.text("recordingInfo.title"));
        view.recordingInfoRecordingsTab().textProperty().bind(i18n.text("recordingInfo.tab.recordings"));
        view.recordingInfoSettingsTab().textProperty().bind(i18n.text("recordingInfo.tab.settings"));
        view.agentsTitleLabel().textProperty().bind(i18n.text("agents.title"));
        view.constantPoolsTitleLabel().textProperty().bind(i18n.text("constantPools.title"));
        view.envVarsSearchField().promptTextProperty().bind(i18n.text("envVars.search.prompt"));
        view.sysPropsSearchField().promptTextProperty().bind(i18n.text("sysProps.search.prompt"));
    }

    private void configureProcessesTable() {
        view.processesTable().setPlaceholder(localizedTablePlaceholder("processes.empty"));
        TableColumn<ProcessInfo, String> pidCol = localizedColumn("processes.column.pid");
        pidCol.setPrefWidth(80);
        pidCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().pid()));
        TableColumn<ProcessInfo, String> cmdCol = localizedColumn("processes.column.commandLine");
        cmdCol.setPrefWidth(600);
        cmdCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().commandLine()));
        TableColumn<ProcessInfo, String> firstCol = localizedColumn("processes.column.firstSample");
        firstCol.setPrefWidth(180);
        firstCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().startTime()));
        TableColumn<ProcessInfo, String> lastCol = localizedColumn("processes.column.lastSample");
        lastCol.setPrefWidth(180);
        lastCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().lastSample()));
        view.processesTable().getColumns().setAll(List.of(pidCol, cmdCol, firstCol, lastCol));
    }

    private void configureEnvVarsTable() {
        view.envVarsTable().setPlaceholder(localizedTablePlaceholder("envVars.empty"));
        TableColumn<EnvironmentVariable, String> keyCol = localizedColumn("envVars.column.key");
        keyCol.setPrefWidth(300);
        keyCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().key()));
        TableColumn<EnvironmentVariable, String> valCol = localizedColumn("envVars.column.value");
        valCol.setPrefWidth(700);
        valCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().value()));
        view.envVarsTable().getColumns().setAll(List.of(keyCol, valCol));
    }

    private void configureSysPropsTable() {
        view.sysPropsTable().setPlaceholder(localizedTablePlaceholder("sysProps.empty"));
        TableColumn<SystemProperty, String> keyCol = localizedColumn("sysProps.column.key");
        keyCol.setPrefWidth(350);
        keyCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().key()));
        TableColumn<SystemProperty, String> valCol = localizedColumn("sysProps.column.value");
        valCol.setPrefWidth(650);
        valCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().value()));
        view.sysPropsTable().getColumns().setAll(List.of(keyCol, valCol));
    }

    private void configureRecordingsTable() {
        view.recordingsTable().setPlaceholder(localizedTablePlaceholder("recordingInfo.empty"));
        TableColumn<ActiveRecordingInfo, String> idCol = localizedColumn("recordingInfo.column.id");
        idCol.setPrefWidth(80);
        idCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().id()));
        TableColumn<ActiveRecordingInfo, String> nameCol = localizedColumn("recordingInfo.column.name");
        nameCol.setPrefWidth(260);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
        TableColumn<ActiveRecordingInfo, String> destCol = localizedColumn("recordingInfo.column.destination");
        destCol.setPrefWidth(360);
        destCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().destination()));
        TableColumn<ActiveRecordingInfo, String> startCol = localizedColumn("recordingInfo.column.startTime");
        startCol.setPrefWidth(180);
        startCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().startTime()));
        TableColumn<ActiveRecordingInfo, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("recordingInfo.column.eventCount"));
        countCol.setPrefWidth(120);
        countCol.setCellValueFactory(cell -> new SimpleLongProperty(cell.getValue().eventCount()));
        useFormattedIntegerCells(countCol);
        view.recordingsTable().getColumns().setAll(List.of(idCol, nameCol, destCol, startCol, countCol));
    }

    private void configureSettingsTable() {
        view.settingsTable().setPlaceholder(localizedTablePlaceholder("recordingInfo.settings.empty"));
        TableColumn<ActiveSetting, String> eventCol = localizedColumn("recordingInfo.settings.column.eventId");
        eventCol.setPrefWidth(420);
        eventCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().eventId()));
        TableColumn<ActiveSetting, String> nameCol = localizedColumn("recordingInfo.settings.column.name");
        nameCol.setPrefWidth(200);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().settingName()));
        TableColumn<ActiveSetting, String> valCol = localizedColumn("recordingInfo.settings.column.value");
        valCol.setPrefWidth(360);
        valCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().settingValue()));
        view.settingsTable().getColumns().setAll(List.of(eventCol, nameCol, valCol));
    }

    private void configureAgentsTable() {
        view.agentsTable().setPlaceholder(localizedTablePlaceholder("agents.empty"));
        TableColumn<AgentInfo, String> nameCol = localizedColumn("agents.column.name");
        nameCol.setPrefWidth(360);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
        TableColumn<AgentInfo, String> optCol = localizedColumn("agents.column.options");
        optCol.setPrefWidth(420);
        optCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().options()));
        TableColumn<AgentInfo, String> initCol = localizedColumn("agents.column.initTime");
        initCol.setPrefWidth(180);
        initCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().initTime()));
        TableColumn<AgentInfo, String> dynCol = localizedColumn("agents.column.dynamic");
        dynCol.setPrefWidth(90);
        dynCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatBoolean(cell.getValue().dynamic())));
        TableColumn<AgentInfo, String> kindCol = localizedColumn("agents.column.kind");
        kindCol.setPrefWidth(120);
        kindCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().kind()));
        view.agentsTable().getColumns().setAll(List.of(nameCol, optCol, initCol, dynCol, kindCol));
    }

    private void configureConstantPoolsTable() {
        view.constantPoolsTable().setPlaceholder(localizedTablePlaceholder("constantPools.empty"));
        TableColumn<ConstantPoolType, String> nameCol = localizedColumn("constantPools.column.typeName");
        nameCol.setPrefWidth(620);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().typeName()));
        TableColumn<ConstantPoolType, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("constantPools.column.entryCount"));
        countCol.setPrefWidth(130);
        countCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().entryCount()));
        useFormattedIntegerCells(countCol);
        view.constantPoolsTable().getColumns().setAll(List.of(nameCol, countCol));
    }

    private Label localizedTablePlaceholder(String key) {
        Label label = new Label();
        label.textProperty().bind(i18n.text(key));
        return label;
    }

    private <T> TableColumn<T, String> localizedColumn(String key) {
        TableColumn<T, String> column = new TableColumn<>();
        column.textProperty().bind(i18n.text(key));
        return column;
    }

    private static <T> void useFormattedIntegerCells(TableColumn<T, Number> column) {
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(DisplayFormats.formatInteger(item.longValue()));
                }
            }
        });
    }
}
