package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.youngledo.jmcfx.domain.model.JavaFxEventReport;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JavaFxEventService;

import jdk.jfr.Recording;

class JmcJavaFxEventServiceTest {

    private final JavaFxEventService service = new JmcJavaFxEventService();

    @Test
    void loadJavaFxEventsReturnsEmptyReportWhenRecordingHasNoJavaFxEvents(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = createMinimalRecording(tempDir);

        JavaFxEventReport report = service.loadJavaFxEvents(recording);

        assertNotNull(report);
        assertEquals(0, report.pulseCount());
        assertEquals(0, report.phaseCount());
        assertEquals(0, report.inputCount());
        assertTrue(report.pulseSummaries().isEmpty());
        assertTrue(report.pulsePhases().isEmpty());
        assertTrue(report.inputEvents().isEmpty());
    }

    @Test
    void javaFxEventTypeIdsStayInAdapter() throws Exception {
        String adapterSource = Files.readString(
                Path.of("src/main/java/com/youngledo/jmcfx/adapter/jmc/JmcJavaFxEventService.java"),
                StandardCharsets.UTF_8);
        String uiSource = Files.readString(
                Path.of("../jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/jfx/JavaFxEventsViewModel.java"),
                StandardCharsets.UTF_8);

        assertTrue(adapterSource.contains("javafx.PulsePhase"));
        assertTrue(adapterSource.contains("javafx.Input"));
        assertFalse(uiSource.contains("javafx.PulsePhase"));
        assertFalse(uiSource.contains("javafx.Input"));
    }

    @Test
    void javaFx8AndJavaFx12AttributesAreSupported() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/youngledo/jmcfx/adapter/jmc/JmcJavaFxEventService.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("pulseNumber"));
        assertTrue(source.contains("pulseId"));
        assertTrue(source.contains("phaseName"));
        assertTrue(source.contains("phase"));
        assertTrue(source.contains("input"));
    }

    private RecordingSummary createMinimalRecording(Path tempDir) throws Exception {
        try (Recording recording = new Recording()) {
            recording.start();
            Thread.sleep(100);
            recording.stop();
            Path file = tempDir.resolve("javafx-events-test.jfr");
            recording.dump(file);
            return new RecordingSummary("test", file, "test",
                    Instant.now(), Instant.now(), 100, 2048);
        }
    }
}
