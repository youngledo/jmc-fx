package io.github.youngledo.jmcfx.domain.service;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ActiveRecordingInfo;
import io.github.youngledo.jmcfx.domain.model.ActiveSetting;
import io.github.youngledo.jmcfx.domain.model.AgentInfo;
import io.github.youngledo.jmcfx.domain.model.ConstantPoolType;
import io.github.youngledo.jmcfx.domain.model.EnvironmentVariable;
import io.github.youngledo.jmcfx.domain.model.ProcessInfo;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.SystemProperty;

/// Service port for loading environment metadata from a JFR recording.
///
/// Provides access to system processes, environment variables, system
/// properties, active recordings/settings, agent information, and
/// constant pool metadata.
public interface EnvironmentService {
    List<ProcessInfo> loadProcesses(RecordingSummary recording);

    List<EnvironmentVariable> loadEnvironmentVariables(RecordingSummary recording);

    List<SystemProperty> loadSystemProperties(RecordingSummary recording);

    List<ActiveRecordingInfo> loadActiveRecordings(RecordingSummary recording);

    List<ActiveSetting> loadActiveSettings(RecordingSummary recording);

    List<AgentInfo> loadAgents(RecordingSummary recording);

    List<ConstantPoolType> loadConstantPools(RecordingSummary recording);
}
