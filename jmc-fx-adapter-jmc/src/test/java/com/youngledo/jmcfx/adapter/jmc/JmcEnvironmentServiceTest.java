package com.youngledo.jmcfx.adapter.jmc;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.domain.model.ActiveRecordingInfo;
import com.youngledo.jmcfx.domain.model.ActiveSetting;
import com.youngledo.jmcfx.domain.model.AgentInfo;
import com.youngledo.jmcfx.domain.model.ConstantPoolType;
import com.youngledo.jmcfx.domain.model.EnvironmentVariable;
import com.youngledo.jmcfx.domain.model.ProcessInfo;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.SystemProperty;

import jdk.jfr.Recording;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class JmcEnvironmentServiceTest {

    private final JmcEnvironmentService service = new JmcEnvironmentService();

    @Test
    void loadProcesses_returnsResults(@TempDir Path tempDir) throws Exception {
        Path jfrFile = createMinimalRecording(tempDir);
        RecordingSummary recording = testRecording(jfrFile);
        List<ProcessInfo> processes = service.loadProcesses(recording);
        assertNotNull(processes);
    }

    @Test
    void loadEnvironmentVariables_returnsResults(@TempDir Path tempDir) throws Exception {
        Path jfrFile = createMinimalRecording(tempDir);
        RecordingSummary recording = testRecording(jfrFile);
        List<EnvironmentVariable> vars = service.loadEnvironmentVariables(recording);
        assertNotNull(vars);
    }

    @Test
    void loadSystemProperties_returnsResults(@TempDir Path tempDir) throws Exception {
        Path jfrFile = createMinimalRecording(tempDir);
        RecordingSummary recording = testRecording(jfrFile);
        List<SystemProperty> props = service.loadSystemProperties(recording);
        assertNotNull(props);
        // Minimal recording may not include system property events;
        // the adapter must still return a valid (possibly empty) list
    }

    @Test
    void loadActiveRecordings_returnsResults(@TempDir Path tempDir) throws Exception {
        Path jfrFile = createMinimalRecording(tempDir);
        RecordingSummary recording = testRecording(jfrFile);
        List<ActiveRecordingInfo> recs = service.loadActiveRecordings(recording);
        assertNotNull(recs);
    }

    @Test
    void loadActiveSettings_returnsResults(@TempDir Path tempDir) throws Exception {
        Path jfrFile = createMinimalRecording(tempDir);
        RecordingSummary recording = testRecording(jfrFile);
        List<ActiveSetting> settings = service.loadActiveSettings(recording);
        assertNotNull(settings);
    }

    @Test
    void loadAgents_returnsResults(@TempDir Path tempDir) throws Exception {
        Path jfrFile = createMinimalRecording(tempDir);
        RecordingSummary recording = testRecording(jfrFile);
        List<AgentInfo> agents = service.loadAgents(recording);
        assertNotNull(agents);
    }

    @Test
    void loadConstantPools_returnsResults(@TempDir Path tempDir) throws Exception {
        Path jfrFile = createMinimalRecording(tempDir);
        RecordingSummary recording = testRecording(jfrFile);
        List<ConstantPoolType> pools = service.loadConstantPools(recording);
        assertNotNull(pools);
    }

    private Path createMinimalRecording(Path tempDir) throws Exception {
        try (Recording recording = new Recording()) {
            recording.start();
            Thread.sleep(50);
            recording.stop();
            Path file = tempDir.resolve("environment-test.jfr");
            recording.dump(file);
            return file;
        }
    }

    private RecordingSummary testRecording(Path jfrFile) {
        return new RecordingSummary("test", jfrFile, "test",
                Instant.now(), Instant.now(), 1000, 1024);
    }
}
