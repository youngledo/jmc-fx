package com.youngledo.jmcfx.ui.environment;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ActiveRecordingInfo;
import com.youngledo.jmcfx.domain.model.ActiveSetting;
import com.youngledo.jmcfx.domain.model.AgentInfo;
import com.youngledo.jmcfx.domain.model.ConstantPoolType;
import com.youngledo.jmcfx.domain.model.EnvironmentVariable;
import com.youngledo.jmcfx.domain.model.ProcessInfo;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.SystemProperty;
import com.youngledo.jmcfx.domain.service.EnvironmentService;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// Unified view model for all six Environment pages.
///
/// Each page loads data from the same EnvironmentService port.
/// Search filtering is provided for Environment Variables and System Properties.
public class EnvironmentViewModel {

    private final EnvironmentService environmentService;
    private final ObservableList<ProcessInfo> processes = FXCollections.observableArrayList();
    private final ObservableList<EnvironmentVariable> environmentVariables = FXCollections.observableArrayList();
    private final ObservableList<EnvironmentVariable> filteredEnvironmentVariables = FXCollections.observableArrayList();
    private final ObservableList<SystemProperty> systemProperties = FXCollections.observableArrayList();
    private final ObservableList<SystemProperty> filteredSystemProperties = FXCollections.observableArrayList();
    private final ObservableList<ActiveRecordingInfo> activeRecordings = FXCollections.observableArrayList();
    private final ObservableList<ActiveSetting> activeSettings = FXCollections.observableArrayList();
    private final ObservableList<AgentInfo> agents = FXCollections.observableArrayList();
    private final ObservableList<ConstantPoolType> constantPools = FXCollections.observableArrayList();
    private final StringProperty environmentSearchFilter = new SimpleStringProperty("");
    private final StringProperty systemPropertySearchFilter = new SimpleStringProperty("");
    private final ObjectProperty<ActiveRecordingInfo> selectedRecording = new SimpleObjectProperty<>();
    private RecordingSummary currentRecording;

    public EnvironmentViewModel(EnvironmentService environmentService) {
        this.environmentService = environmentService;
        environmentSearchFilter.addListener((obs, old, val) -> applyEnvironmentFilter());
        systemPropertySearchFilter.addListener((obs, old, val) -> applySystemPropertyFilter());
    }

    public ObservableList<ProcessInfo> processesProperty() {
        return processes;
    }

    public ObservableList<EnvironmentVariable> environmentVariablesProperty() {
        return environmentVariables;
    }

    public ObservableList<EnvironmentVariable> filteredEnvironmentVariablesProperty() {
        return filteredEnvironmentVariables;
    }

    public ObservableList<SystemProperty> systemPropertiesProperty() {
        return systemProperties;
    }

    public ObservableList<SystemProperty> filteredSystemPropertiesProperty() {
        return filteredSystemProperties;
    }

    public ObservableList<ActiveRecordingInfo> activeRecordingsProperty() {
        return activeRecordings;
    }

    public ObservableList<ActiveSetting> activeSettingsProperty() {
        return activeSettings;
    }

    public ObservableList<AgentInfo> agentsProperty() {
        return agents;
    }

    public ObservableList<ConstantPoolType> constantPoolsProperty() {
        return constantPools;
    }

    public StringProperty environmentSearchFilterProperty() {
        return environmentSearchFilter;
    }

    public StringProperty systemPropertySearchFilterProperty() {
        return systemPropertySearchFilter;
    }

    public ObjectProperty<ActiveRecordingInfo> selectedRecordingProperty() {
        return selectedRecording;
    }

    public void setEnvironmentSearchFilter(String filter) {
        environmentSearchFilter.set(filter);
    }

    public void setSystemPropertySearchFilter(String filter) {
        systemPropertySearchFilter.set(filter);
    }

    public void load(RecordingSummary recording) {
        currentRecording = recording;
        processes.setAll(environmentService.loadProcesses(recording));
        List<EnvironmentVariable> envVars = environmentService.loadEnvironmentVariables(recording);
        environmentVariables.setAll(envVars);
        filteredEnvironmentVariables.setAll(envVars);
        List<SystemProperty> sysProps = environmentService.loadSystemProperties(recording);
        systemProperties.setAll(sysProps);
        filteredSystemProperties.setAll(sysProps);
        activeRecordings.setAll(environmentService.loadActiveRecordings(recording));
        activeSettings.setAll(environmentService.loadActiveSettings(recording));
        agents.setAll(environmentService.loadAgents(recording));
        constantPools.setAll(environmentService.loadConstantPools(recording));
        selectedRecording.set(null);
        environmentSearchFilter.set("");
        systemPropertySearchFilter.set("");
    }

    private void applyEnvironmentFilter() {
        String filter = environmentSearchFilter.get();
        if (filter == null || filter.isBlank()) {
            filteredEnvironmentVariables.setAll(environmentVariables);
            return;
        }
        String lowerFilter = filter.toLowerCase();
        List<EnvironmentVariable> filtered = environmentVariables.stream()
                .filter(ev -> ev.key().toLowerCase().contains(lowerFilter)
                        || ev.value().toLowerCase().contains(lowerFilter))
                .toList();
        filteredEnvironmentVariables.setAll(filtered);
    }

    private void applySystemPropertyFilter() {
        String filter = systemPropertySearchFilter.get();
        if (filter == null || filter.isBlank()) {
            filteredSystemProperties.setAll(systemProperties);
            return;
        }
        String lowerFilter = filter.toLowerCase();
        List<SystemProperty> filtered = systemProperties.stream()
                .filter(sp -> sp.key().toLowerCase().contains(lowerFilter)
                        || sp.value().toLowerCase().contains(lowerFilter))
                .toList();
        filteredSystemProperties.setAll(filtered);
    }
}
