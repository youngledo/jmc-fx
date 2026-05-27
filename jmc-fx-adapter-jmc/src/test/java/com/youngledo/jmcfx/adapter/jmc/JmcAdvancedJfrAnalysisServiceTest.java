package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.youngledo.jmcfx.domain.model.EventHeatmap;
import com.youngledo.jmcfx.domain.model.EventHeatmapRow;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JmcFxException;

class JmcAdvancedJfrAnalysisServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void missingRecordingFailsClearly() {
        JmcAdvancedJfrAnalysisService service = new JmcAdvancedJfrAnalysisService();

        assertThrows(JmcFxException.class, () -> service.loadEventHeatmap(recording(Path.of("missing.jfr")), 20, 12));
    }

    @Test
    void invalidLimitsAreClamped() {
        JmcAdvancedJfrAnalysisService service = new JmcAdvancedJfrAnalysisService();

        assertEquals(1, service.normalizedBucketCount(0));
        assertEquals(80, service.normalizedBucketCount(500));
        assertEquals(1, service.normalizedMaxEventTypes(0));
        assertEquals(40, service.normalizedMaxEventTypes(500));
    }

    @Test
    void recordingBuildsBoundedHeatmap() throws Exception {
        JmcAdvancedJfrAnalysisService service = new JmcAdvancedJfrAnalysisService();

        EventHeatmap heatmap = service.loadEventHeatmap(customRecording(), 10, 1);

        assertEquals(10, heatmap.bucketCount());
        assertEquals(1, heatmap.rows().size());
        EventHeatmapRow row = heatmap.rows().getFirst();
        assertEquals("com.youngledo.jmcfx.TestHeatmapEvent", row.eventTypeId());
        assertEquals("Test Heatmap Event", row.label());
        assertEquals(List.of("JMC FX", "Tests"), row.categoryPath());
        assertEquals(3, row.totalCount());
        assertEquals(10, row.cells().size());
        assertTrue(heatmap.maxCellCount() >= 1);
    }

    private RecordingSummary customRecording() throws Exception {
        Path recordingPath = tempDir.resolve("heatmap.jfr");
        try (Recording recording = new Recording()) {
            recording.enable(TestHeatmapEvent.class);
            recording.start();
            new TestHeatmapEvent().commit();
            Thread.sleep(2);
            new TestHeatmapEvent().commit();
            Thread.sleep(2);
            new TestHeatmapEvent().commit();
            recording.stop();
            recording.dump(recordingPath);
        }
        return recording(recordingPath);
    }

    private RecordingSummary recording(Path path) throws Exception {
        long size = Files.isRegularFile(path) ? Files.size(path) : 0;
        return new RecordingSummary(path.toString(), path, path.getFileName().toString(),
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, size);
    }

    @Name("com.youngledo.jmcfx.TestHeatmapEvent")
    @Label("Test Heatmap Event")
    @jdk.jfr.Category({"JMC FX", "Tests"})
    static class TestHeatmapEvent extends Event {
    }
}
