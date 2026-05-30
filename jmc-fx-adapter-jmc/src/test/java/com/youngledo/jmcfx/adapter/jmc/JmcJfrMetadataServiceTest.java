package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.RecordingSummary;

class JmcJfrMetadataServiceTest {

    @Test
    void loadsEventTypeAndFieldMetadataFromRecording() throws Exception {
        JmcJfrMetadataService service = new JmcJfrMetadataService();

        var report = service.loadMetadata(startupRecording());

        assertFalse(report.eventTypes().isEmpty());
        assertTrue(report.eventTypeCount() > 0);
        assertTrue(report.eventCount() > 0);
        assertTrue(report.eventTypes().stream().anyMatch(type -> !type.fields().isEmpty()));
        assertTrue(report.eventTypes().stream().allMatch(type -> !type.id().isBlank()));
    }

    private RecordingSummary startupRecording() throws Exception {
        Path path = startupRecordingPath();
        assumeTrue(Files.isRegularFile(path), "startup.jfr is only used for local regression coverage");
        return new RecordingSummary("startup", path, "startup.jfr",
                Instant.EPOCH, Instant.EPOCH, 0, Files.size(path));
    }

    private Path startupRecordingPath() {
        String configuredPath = System.getProperty("jmcfx.realJfr", "");
        if (!configuredPath.isBlank()) {
            return Path.of(configuredPath);
        }
        Path modulePath = Path.of("startup.jfr");
        if (Files.isRegularFile(modulePath)) {
            return modulePath;
        }
        return Path.of("..", "startup.jfr");
    }
}
