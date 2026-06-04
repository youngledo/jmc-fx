package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.ChartXAxisType;
import com.youngledo.jmcfx.domain.model.HeapClassHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;

class JmcHeapServiceTest {

    private final JmcHeapService service = new JmcHeapService();

    @Test
    void loadHeapClassHistogramFallsBackToHeapSummaryWhenObjectCountsAreAbsentInStartupRecording()
            throws Exception {
        RecordingSummary recording = startupRecording();
        ChartDefinition timeline = service.loadHeapUsageTimeline(recording);
        assertTrue(timeline.series().stream().flatMap(series -> series.points().stream()).findAny().isPresent(),
                "test recording must contain heap summary events");
        assertEquals(ChartXAxisType.EPOCH_MILLIS, timeline.xAxisType());

        List<HeapClassHistogram> histogram = service.loadHeapClassHistogram(recording);

        assertFalse(histogram.isEmpty());
        HeapClassHistogram row = histogram.getFirst();
        assertTrue(row.className().contains("Heap"));
        assertTrue(row.size() > 0);
    }

    @Test
    void loadHeapUsageTimeline_marksXAxisAsEpochMillis(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = minimalRecording(tempDir);

        ChartDefinition timeline = service.loadHeapUsageTimeline(recording);

        assertEquals(ChartXAxisType.EPOCH_MILLIS, timeline.xAxisType());
    }

    private RecordingSummary startupRecording() throws Exception {
        Path path = startupRecordingPath();
        assumeTrue(Files.isRegularFile(path), "startup.jfr is only used for local regression coverage");
        return new RecordingSummary("startup", path, "startup.jfr",
                Instant.EPOCH, Instant.EPOCH, 0, Files.size(path));
    }

    private RecordingSummary minimalRecording(Path tempDir) throws Exception {
        try (jdk.jfr.Recording recording = new jdk.jfr.Recording()) {
            recording.start();
            Thread.sleep(50);
            recording.stop();
            Path file = tempDir.resolve("heap-test.jfr");
            recording.dump(file);
            return new RecordingSummary("test", file, "heap-test.jfr",
                    Instant.EPOCH, Instant.EPOCH, 0, Files.size(file));
        }
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
