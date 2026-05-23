package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JmcFxException;

class JmcRecordingRepositoryTest {

    @Test
    void reportsMissingRecordingAsDomainException() {
        JmcRecordingRepository repository = new JmcRecordingRepository();

        assertThrows(JmcFxException.class, () -> repository.open(Path.of("missing.jfr")));
    }

    @Test
    void openUsesFileMetadataWithoutParsingRecordingEvents() throws Exception {
        Path recording = Files.createTempFile("jmc-fx-lightweight-open", ".jfr");
        Files.writeString(recording, "not a real jfr");

        RecordingSummary summary = new JmcRecordingRepository().open(recording);

        assertEquals(recording.toAbsolutePath().normalize().toString(), summary.id());
        assertEquals(recording, summary.path());
        assertEquals(recording.getFileName().toString(), summary.name());
        assertEquals(Instant.EPOCH, summary.startTime());
        assertEquals(Instant.EPOCH, summary.endTime());
        assertEquals(0, summary.durationMillis());
        assertTrue(summary.sizeBytes() > 0);
    }

    @Test
    void openUsesRecordingChunkTimeRangeWhenAvailable() throws Exception {
        Path recording = Files.createTempFile("jmc-fx-real-time-range", ".jfr");
        try (jdk.jfr.Recording jfr = new jdk.jfr.Recording()) {
            jfr.start();
            Thread.sleep(50);
            jfr.stop();
            jfr.dump(recording);
        }

        RecordingSummary summary = new JmcRecordingRepository().open(recording);

        assertTrue(summary.startTime().isAfter(Instant.EPOCH), summary.startTime().toString());
        assertTrue(summary.endTime().isAfter(summary.startTime()) || summary.endTime().equals(summary.startTime()),
                summary.endTime().toString());
        assertTrue(summary.durationMillis() >= 0);
    }
}
