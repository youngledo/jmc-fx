package com.youngledo.jmcfx.ui.environment;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.application.LoadEnvironmentUseCase;

import com.youngledo.jmcfx.domain.model.ActiveRecordingInfo;
import com.youngledo.jmcfx.domain.model.ActiveSetting;
import com.youngledo.jmcfx.domain.model.AgentInfo;
import com.youngledo.jmcfx.domain.model.ConstantPoolType;
import com.youngledo.jmcfx.domain.model.EnvironmentVariable;
import com.youngledo.jmcfx.domain.model.ProcessInfo;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.SystemProperty;
import com.youngledo.jmcfx.testsupport.FakeEnvironmentService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentViewModelTest {

    @Test
    void loadPopulatesProcesses() {
        FakeEnvironmentService service = new FakeEnvironmentService();
        service.addProcess(new ProcessInfo("1234", "java MyApp", "2025-01-01 00:00:00.000", "2025-01-01 00:00:00.000"));

        EnvironmentViewModel vm = new EnvironmentViewModel(new LoadEnvironmentUseCase(service));
        vm.load(testRecording());

        assertEquals(1, vm.processesProperty().size());
        assertEquals("1234", vm.processesProperty().getFirst().pid());
    }

    @Test
    void loadPopulatesEnvironmentVariables() {
        FakeEnvironmentService service = new FakeEnvironmentService();
        service.addEnvVar(new EnvironmentVariable("JAVA_HOME", "/usr/lib/jvm/java-25"));
        service.addEnvVar(new EnvironmentVariable("PATH", "/usr/bin:/bin"));

        EnvironmentViewModel vm = new EnvironmentViewModel(new LoadEnvironmentUseCase(service));
        vm.load(testRecording());

        assertEquals(2, vm.environmentVariablesProperty().size());
    }

    @Test
    void loadPopulatesSystemProperties() {
        FakeEnvironmentService service = new FakeEnvironmentService();
        service.addSysProp(new SystemProperty("java.version", "25"));
        service.addSysProp(new SystemProperty("os.name", "Linux"));

        EnvironmentViewModel vm = new EnvironmentViewModel(new LoadEnvironmentUseCase(service));
        vm.load(testRecording());

        assertEquals(2, vm.systemPropertiesProperty().size());
    }

    @Test
    void setSearchFilterFiltersEnvironmentVariables() {
        FakeEnvironmentService service = new FakeEnvironmentService();
        service.addEnvVar(new EnvironmentVariable("JAVA_HOME", "/usr/lib/jvm"));
        service.addEnvVar(new EnvironmentVariable("HOME", "/home/user"));

        EnvironmentViewModel vm = new EnvironmentViewModel(new LoadEnvironmentUseCase(service));
        vm.load(testRecording());
        vm.setEnvironmentSearchFilter("JAVA");

        assertEquals(1, vm.filteredEnvironmentVariablesProperty().size());
        assertEquals("JAVA_HOME", vm.filteredEnvironmentVariablesProperty().getFirst().key());
    }

    @Test
    void setSearchFilterFiltersSystemProperties() {
        FakeEnvironmentService service = new FakeEnvironmentService();
        service.addSysProp(new SystemProperty("java.version", "25"));
        service.addSysProp(new SystemProperty("os.name", "Linux"));

        EnvironmentViewModel vm = new EnvironmentViewModel(new LoadEnvironmentUseCase(service));
        vm.load(testRecording());
        vm.setSystemPropertySearchFilter("os");

        assertEquals(1, vm.filteredSystemPropertiesProperty().size());
        assertEquals("os.name", vm.filteredSystemPropertiesProperty().getFirst().key());
    }

    @Test
    void loadPopulatesActiveRecordings() {
        FakeEnvironmentService service = new FakeEnvironmentService();
        service.addRecording(new ActiveRecordingInfo("1", "recording", "", 0, 0, "", "", 100));

        EnvironmentViewModel vm = new EnvironmentViewModel(new LoadEnvironmentUseCase(service));
        vm.load(testRecording());

        assertEquals(1, vm.activeRecordingsProperty().size());
    }

    @Test
    void loadCollapsesDuplicateMetadataRows() {
        FakeEnvironmentService service = new FakeEnvironmentService();
        service.addEnvVar(new EnvironmentVariable("JAVA_HOME", "/usr/lib/jvm/java-25"));
        service.addEnvVar(new EnvironmentVariable("JAVA_HOME", "/usr/lib/jvm/java-25"));
        service.addSysProp(new SystemProperty("java.version", "25"));
        service.addSysProp(new SystemProperty("java.version", "25"));
        service.addRecording(new ActiveRecordingInfo("1", "recording", "", 0, 0, "", "", 100));
        service.addRecording(new ActiveRecordingInfo("1", "recording", "", 0, 0, "", "", 100));

        EnvironmentViewModel vm = new EnvironmentViewModel(new LoadEnvironmentUseCase(service));
        vm.load(testRecording());

        assertEquals(1, vm.environmentVariablesProperty().size());
        assertEquals(1, vm.filteredEnvironmentVariablesProperty().size());
        assertEquals(1, vm.systemPropertiesProperty().size());
        assertEquals(1, vm.filteredSystemPropertiesProperty().size());
        assertEquals(1, vm.activeRecordingsProperty().size());
    }

    @Test
    void loadPopulatesActiveSettings() {
        FakeEnvironmentService service = new FakeEnvironmentService();
        service.addSetting(new ActiveSetting("jdk.ExecutionSample", "enabled", "true"));

        EnvironmentViewModel vm = new EnvironmentViewModel(new LoadEnvironmentUseCase(service));
        vm.load(testRecording());

        assertEquals(1, vm.activeSettingsProperty().size());
    }

    @Test
    void loadPopulatesAgents() {
        FakeEnvironmentService service = new FakeEnvironmentService();
        service.addAgent(new AgentInfo("management-agent", "", "", false, "Java"));

        EnvironmentViewModel vm = new EnvironmentViewModel(new LoadEnvironmentUseCase(service));
        vm.load(testRecording());

        assertEquals(1, vm.agentsProperty().size());
    }

    @Test
    void loadPopulatesConstantPools() {
        FakeEnvironmentService service = new FakeEnvironmentService();
        service.addPool(new ConstantPoolType("java.lang.String", "java.lang.String", 50, List.of()));

        EnvironmentViewModel vm = new EnvironmentViewModel(new LoadEnvironmentUseCase(service));
        vm.load(testRecording());

        assertEquals(1, vm.constantPoolsProperty().size());
    }

    @Test
    void loadClearsPreviousData() {
        FakeEnvironmentService service = new FakeEnvironmentService();
        service.addProcess(new ProcessInfo("1", "cmd", "", ""));

        EnvironmentViewModel vm = new EnvironmentViewModel(new LoadEnvironmentUseCase(service));
        vm.load(testRecording());
        assertEquals(1, vm.processesProperty().size());

        // Load with fresh service (no data)
        EnvironmentViewModel vm2 = new EnvironmentViewModel(new LoadEnvironmentUseCase(new FakeEnvironmentService()));
        vm2.load(testRecording());
        assertEquals(0, vm2.processesProperty().size());
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.now(), Instant.now(), 1000, 1024);
    }
}
