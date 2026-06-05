package io.github.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.ActiveRecordingInfo;
import io.github.youngledo.jmcfx.domain.model.ActiveSetting;
import io.github.youngledo.jmcfx.domain.model.AgentInfo;
import io.github.youngledo.jmcfx.domain.model.ConstantPoolType;
import io.github.youngledo.jmcfx.domain.model.EnvironmentVariable;
import io.github.youngledo.jmcfx.domain.model.ProcessInfo;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.SystemProperty;
import io.github.youngledo.jmcfx.domain.service.EnvironmentService;

public final class LoadEnvironmentUseCase {

    private final EnvironmentService service;

    public LoadEnvironmentUseCase(EnvironmentService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public List<ProcessInfo> loadProcesses(RecordingSummary recording) {
        return service.loadProcesses(recording);
    }

    public List<EnvironmentVariable> loadEnvironmentVariables(RecordingSummary recording) {
        return service.loadEnvironmentVariables(recording);
    }

    public List<SystemProperty> loadSystemProperties(RecordingSummary recording) {
        return service.loadSystemProperties(recording);
    }

    public List<ActiveRecordingInfo> loadActiveRecordings(RecordingSummary recording) {
        return service.loadActiveRecordings(recording);
    }

    public List<ActiveSetting> loadActiveSettings(RecordingSummary recording) {
        return service.loadActiveSettings(recording);
    }

    public List<AgentInfo> loadAgents(RecordingSummary recording) {
        return service.loadAgents(recording);
    }

    public List<ConstantPoolType> loadConstantPools(RecordingSummary recording) {
        return service.loadConstantPools(recording);
    }
}
