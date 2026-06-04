package com.youngledo.jmcfx.ui.testsupport;

import java.util.ArrayList;
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

public class FakeEnvironmentService implements EnvironmentService {

    private final List<ProcessInfo> processes = new ArrayList<>();
    private final List<EnvironmentVariable> envVars = new ArrayList<>();
    private final List<SystemProperty> sysProps = new ArrayList<>();
    private final List<ActiveRecordingInfo> recordings = new ArrayList<>();
    private final List<ActiveSetting> settings = new ArrayList<>();
    private final List<AgentInfo> agents = new ArrayList<>();
    private final List<ConstantPoolType> pools = new ArrayList<>();

    public void addProcess(ProcessInfo process) { processes.add(process); }
    public void addEnvVar(EnvironmentVariable envVar) { envVars.add(envVar); }
    public void addSysProp(SystemProperty sysProp) { sysProps.add(sysProp); }
    public void addRecording(ActiveRecordingInfo rec) { recordings.add(rec); }
    public void addSetting(ActiveSetting setting) { settings.add(setting); }
    public void addAgent(AgentInfo agent) { agents.add(agent); }
    public void addPool(ConstantPoolType pool) { pools.add(pool); }

    @Override
    public List<ProcessInfo> loadProcesses(RecordingSummary recording) {
        return List.copyOf(processes);
    }

    @Override
    public List<EnvironmentVariable> loadEnvironmentVariables(RecordingSummary recording) {
        return List.copyOf(envVars);
    }

    @Override
    public List<SystemProperty> loadSystemProperties(RecordingSummary recording) {
        return List.copyOf(sysProps);
    }

    @Override
    public List<ActiveRecordingInfo> loadActiveRecordings(RecordingSummary recording) {
        return List.copyOf(recordings);
    }

    @Override
    public List<ActiveSetting> loadActiveSettings(RecordingSummary recording) {
        return List.copyOf(settings);
    }

    @Override
    public List<AgentInfo> loadAgents(RecordingSummary recording) {
        return List.copyOf(agents);
    }

    @Override
    public List<ConstantPoolType> loadConstantPools(RecordingSummary recording) {
        return List.copyOf(pools);
    }
}
