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

import com.youngledo.jmcfx.domain.model.G1GcReport;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.G1GcService;

import jdk.jfr.Recording;

class JmcG1GcServiceTest {

    private final G1GcService service = new JmcG1GcService();

    @Test
    void loadG1GcReportReturnsEmptyReportWhenRecordingHasNoG1RegionEvents(@TempDir Path tempDir) throws Exception {
        RecordingSummary recording = createMinimalRecording(tempDir);

        G1GcReport report = service.loadG1GcReport(recording);

        assertNotNull(report);
        assertEquals(0, report.snapshotCount());
        assertEquals(0, report.transitionCount());
        assertTrue(report.regionSummaries().isEmpty());
        assertTrue(report.recentRegionStates().isEmpty());
        assertNotNull(report.gcPauses());
    }

    @Test
    void g1RegionByteFieldsAreReadAsBytes() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/com/youngledo/jmcfx/adapter/jmc/JmcG1GcService.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("readLongBytes(REGION_USED, item)"));
        assertTrue(source.contains("readLongBytes(REGION_CAPACITY, item)"));
        assertFalse(source.contains("readLong(REGION_USED, item)"));
        assertFalse(source.contains("readLong(REGION_CAPACITY, item)"));
    }

    @Test
    void g1TypeIdsStayInAdapter() throws Exception {
        String uiSource = Files.readString(
                Path.of("../jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/gc/G1GcViewModel.java"),
                StandardCharsets.UTF_8);

        assertFalse(uiSource.contains("GC_G1_HEAP_REGION_INFORMATION"));
        assertFalse(uiSource.contains("GC_G1_HEAP_REGION_TYPE_CHANGE"));
    }

    private RecordingSummary createMinimalRecording(Path tempDir) throws Exception {
        try (Recording recording = new Recording()) {
            recording.start();
            Thread.sleep(100);
            recording.stop();
            Path file = tempDir.resolve("g1-gc-test.jfr");
            recording.dump(file);
            return new RecordingSummary("test", file, "test",
                    Instant.now(), Instant.now(), 100, 2048);
        }
    }
}
