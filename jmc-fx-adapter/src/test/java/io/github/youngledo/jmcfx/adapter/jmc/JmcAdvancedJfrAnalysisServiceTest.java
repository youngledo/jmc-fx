package io.github.youngledo.jmcfx.adapter.jmc;

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

import io.github.youngledo.jmcfx.domain.model.EventHeatmap;
import io.github.youngledo.jmcfx.domain.model.EventHeatmapRow;
import io.github.youngledo.jmcfx.domain.model.MemoryAnalysisReport;
import io.github.youngledo.jmcfx.domain.model.MemoryIssue;
import io.github.youngledo.jmcfx.domain.model.MemoryIssueCategory;
import io.github.youngledo.jmcfx.domain.model.MemoryIssueSeverity;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;

class JmcAdvancedJfrAnalysisServiceTest {

    private static volatile Object allocationSink;
    private static final int ALLOCATION_ROUNDS = 24;
    private static final int ALLOCATIONS_PER_ROUND = 512;

    @TempDir
    Path tempDir;

    @Test
    void missingRecordingFailsClearly() {
        JmcAdvancedJfrAnalysisService service = new JmcAdvancedJfrAnalysisService();

        assertThrows(JmcFxException.class, () -> service.loadEventHeatmap(recording(Path.of("missing.jfr")), 20, 12));
        assertThrows(JmcFxException.class, () -> service.loadMemoryAnalysis(recording(Path.of("missing.jfr")), 12));
    }

    @Test
    void invalidLimitsAreClamped() {
        JmcAdvancedJfrAnalysisService service = new JmcAdvancedJfrAnalysisService();

        assertEquals(1, service.normalizedBucketCount(0));
        assertEquals(80, service.normalizedBucketCount(500));
        assertEquals(1, service.normalizedMaxEventTypes(0));
        assertEquals(40, service.normalizedMaxEventTypes(500));
        assertEquals(1, service.normalizedMaxMemoryIssues(0));
        assertEquals(40, service.normalizedMaxMemoryIssues(500));
    }

    @Test
    void recordingBuildsBoundedHeatmap() throws Exception {
        JmcAdvancedJfrAnalysisService service = new JmcAdvancedJfrAnalysisService();

        EventHeatmap heatmap = service.loadEventHeatmap(customRecording(), 10, 10);

        assertEquals(10, heatmap.bucketCount());
        assertTrue(heatmap.rows().size() >= 1);
        EventHeatmapRow row = heatmap.rows().stream()
                .filter(candidate -> candidate.eventTypeId().equals("io.github.youngledo.jmcfx.TestHeatmapEvent"))
                .findFirst()
                .orElseThrow();
        assertEquals("io.github.youngledo.jmcfx.TestHeatmapEvent", row.eventTypeId());
        assertEquals("Test Heatmap Event", row.label());
        assertEquals(List.of("JMC FX", "Tests"), row.categoryPath());
        assertEquals(3, row.totalCount());
        assertEquals(10, row.cells().size());
        assertTrue(heatmap.maxCellCount() >= 1);
    }

    @Test
    void recordingBuildsMemoryAnalysisFromAllocationEvents() throws Exception {
        JmcAdvancedJfrAnalysisService service = new JmcAdvancedJfrAnalysisService();

        MemoryAnalysisReport report = service.loadMemoryAnalysis(customRecording(), 10);

        assertTrue(report.totalEstimatedBytes() > 0);
        assertTrue(report.totalCount() > 0);
        MemoryIssue issue = report.issues().stream()
                .filter(candidate -> candidate.category() == MemoryIssueCategory.ALLOCATION_HOTSPOT)
                .findFirst()
                .orElseThrow();
        assertTrue(issue.count() > 0);
        assertTrue(issue.estimatedBytes() > 0);
        assertTrue(!issue.subject().isBlank());
    }

    @Test
    void memoryAnalysisTotalsExcludeSecondaryOutsideTlabIssues() {
        JmcAdvancedJfrAnalysisService service = new JmcAdvancedJfrAnalysisService();
        MemoryIssue allocationHotspot = new MemoryIssue(MemoryIssueCategory.ALLOCATION_HOTSPOT,
                MemoryIssueSeverity.INFO, "byte[]", 100, 4, 10, "primary", "review");
        MemoryIssue outsideTlab = new MemoryIssue(MemoryIssueCategory.OUTSIDE_TLAB,
                MemoryIssueSeverity.INFO, "main", 100, 4, 10, "secondary", "review");
        MemoryIssue retainedObject = new MemoryIssue(MemoryIssueCategory.RETAINED_OBJECT,
                MemoryIssueSeverity.INFO, "java.lang.Object", 7, 1, 1, "retained", "inspect");

        MemoryAnalysisReport report = service.memoryAnalysisReport(
                List.of(allocationHotspot, outsideTlab, retainedObject), 10);

        assertEquals(107, report.totalEstimatedBytes());
        assertEquals(5, report.totalCount());
        assertEquals(3, report.issues().size());
    }

    @Test
    void retainedObjectIssueWithUnknownBytesUsesSampleEvidenceAndInfoSeverity() {
        JmcAdvancedJfrAnalysisService service = new JmcAdvancedJfrAnalysisService();

        MemoryIssue issue = service.retainedObjectIssue("demo.Retained", 0, 3);

        assertEquals(MemoryIssueCategory.RETAINED_OBJECT, issue.category());
        assertEquals(MemoryIssueSeverity.INFO, issue.severity());
        assertEquals(0, issue.estimatedBytes());
        assertEquals(3, issue.count());
        assertTrue(issue.evidence().contains("3 samples"));
        assertTrue(issue.evidence().contains("estimated bytes unknown"));
    }

    private RecordingSummary customRecording() throws Exception {
        Path recordingPath = tempDir.resolve("heatmap.jfr");
        try (Recording recording = new Recording()) {
            recording.enable("jdk.ObjectAllocationInNewTLAB").withThreshold(java.time.Duration.ZERO);
            recording.enable("jdk.ObjectAllocationOutsideTLAB").withThreshold(java.time.Duration.ZERO);
            recording.enable(TestHeatmapEvent.class);
            recording.start();
            for (int i = 0; i < ALLOCATION_ROUNDS; i++) {
                allocateForJfr();
                if (i < 3) {
                    new TestHeatmapEvent().commit();
                }
                Thread.sleep(2);
            }
            recording.stop();
            recording.dump(recordingPath);
        }
        return recording(recordingPath);
    }

    private void allocateForJfr() {
        Object[] allocations = new Object[ALLOCATIONS_PER_ROUND];
        for (int i = 0; i < allocations.length; i++) {
            allocations[i] = new byte[2048 + i];
        }
        allocationSink = allocations;
    }

    private RecordingSummary recording(Path path) throws Exception {
        long size = Files.isRegularFile(path) ? Files.size(path) : 0;
        return new RecordingSummary(path.toString(), path, path.getFileName().toString(),
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, size);
    }

    @Name("io.github.youngledo.jmcfx.TestHeatmapEvent")
    @Label("Test Heatmap Event")
    @jdk.jfr.Category({"JMC FX", "Tests"})
    static class TestHeatmapEvent extends Event {
    }
}
