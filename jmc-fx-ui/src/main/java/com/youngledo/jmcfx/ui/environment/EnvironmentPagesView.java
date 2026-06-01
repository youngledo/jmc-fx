package com.youngledo.jmcfx.ui.environment;

import com.youngledo.jmcfx.domain.model.ActiveRecordingInfo;
import com.youngledo.jmcfx.domain.model.ActiveSetting;
import com.youngledo.jmcfx.domain.model.AgentInfo;
import com.youngledo.jmcfx.domain.model.ConstantPoolType;
import com.youngledo.jmcfx.domain.model.EnvironmentVariable;
import com.youngledo.jmcfx.domain.model.ProcessInfo;
import com.youngledo.jmcfx.domain.model.SystemProperty;

import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/// Narrow view boundary for Environment recording data pages.
public record EnvironmentPagesView(
        Label processesTitleLabel,
        TableView<ProcessInfo> processesTable,
        Label envVarsTitleLabel,
        TextField envVarsSearchField,
        TableView<EnvironmentVariable> envVarsTable,
        Label sysPropsTitleLabel,
        TextField sysPropsSearchField,
        TableView<SystemProperty> sysPropsTable,
        Label recordingInfoTitleLabel,
        Tab recordingInfoRecordingsTab,
        Tab recordingInfoSettingsTab,
        TableView<ActiveRecordingInfo> recordingsTable,
        TableView<ActiveSetting> settingsTable,
        Label agentsTitleLabel,
        TableView<AgentInfo> agentsTable,
        Label constantPoolsTitleLabel,
        TableView<ConstantPoolType> constantPoolsTable) {
}
