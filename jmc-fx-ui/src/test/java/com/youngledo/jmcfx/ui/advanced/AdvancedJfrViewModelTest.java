package com.youngledo.jmcfx.ui.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.domain.model.EventHeatmap;
import com.youngledo.jmcfx.domain.model.EventHeatmapCell;
import com.youngledo.jmcfx.domain.model.EventHeatmapRow;
import com.youngledo.jmcfx.domain.model.MemoryAnalysisReport;
import com.youngledo.jmcfx.domain.model.MemoryIssue;
import com.youngledo.jmcfx.domain.model.MemoryIssueCategory;
import com.youngledo.jmcfx.domain.model.MemoryIssueSeverity;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.testsupport.FakeAdvancedJfrAnalysisService;

import org.junit.jupiter.api.Test;

class AdvancedJfrViewModelTest {

    @Test
    void loadPopulatesHeatmapAndSummary() {
        FakeAdvancedJfrAnalysisService service = new FakeAdvancedJfrAnalysisService();
        service.setHeatmap(sampleHeatmap());

        AdvancedJfrViewModel viewModel = new AdvancedJfrViewModel(service);
        viewModel.load(recording());

        assertEquals(1, viewModel.heatmapProperty().get().rows().size());
        assertEquals("1 event types, 4 events", viewModel.summaryProperty().get());
        assertEquals(20, service.lastBucketCount());
        assertEquals(12, service.lastMaxEventTypes());
    }

    @Test
    void loadPopulatesMemoryReportSummaryAndIssues() {
        FakeAdvancedJfrAnalysisService service = new FakeAdvancedJfrAnalysisService();
        service.setHeatmap(sampleHeatmap());
        MemoryIssue first = memoryIssue(MemoryIssueSeverity.WARNING, "java.lang.String",
                8 * 1024 * 1024, 10, 82.5);
        MemoryIssue second = memoryIssue(MemoryIssueSeverity.INFO, "byte[]", 2 * 1024 * 1024, 2, 24.0);
        service.setMemoryAnalysisReport(new MemoryAnalysisReport(10 * 1024 * 1024, 12, List.of(first, second)));

        AdvancedJfrViewModel viewModel = new AdvancedJfrViewModel(service);
        viewModel.load(recording());

        assertEquals(10 * 1024 * 1024, viewModel.memoryReportProperty().get().totalEstimatedBytes());
        assertEquals(List.of(first, second), viewModel.memoryIssues().stream().toList());
        assertEquals("2 issues, 10.0 MB estimated, 12 events", viewModel.memorySummaryProperty().get());
        assertEquals(AdvancedJfrViewModel.DEFAULT_MAX_MEMORY_ISSUES, service.lastMaxMemoryIssues());
    }

    @Test
    void selectingCellUpdatesSelectionProperties() {
        AdvancedJfrViewModel viewModel = new AdvancedJfrViewModel(new FakeAdvancedJfrAnalysisService());
        EventHeatmapCell cell = new EventHeatmapCell("jdk.CPULoad", Instant.EPOCH,
                Instant.EPOCH.plusSeconds(1), 4);

        viewModel.selectCell(cell);

        assertEquals(cell, viewModel.selectedCellProperty().get());
        assertEquals("jdk.CPULoad", viewModel.selectedEventTypeProperty().get());
        assertEquals("4", viewModel.selectedCountProperty().get());
    }

    @Test
    void loadClearsPreviousSelection() {
        FakeAdvancedJfrAnalysisService service = new FakeAdvancedJfrAnalysisService();
        service.setHeatmap(sampleHeatmap());
        AdvancedJfrViewModel viewModel = new AdvancedJfrViewModel(service);
        viewModel.selectCell(new EventHeatmapCell("jdk.CPULoad", Instant.EPOCH,
                Instant.EPOCH.plusSeconds(1), 4));

        viewModel.load(recording());

        assertNull(viewModel.selectedCellProperty().get());
        assertEquals("", viewModel.selectedEventTypeProperty().get());
        assertEquals("", viewModel.selectedCountProperty().get());
    }

    @Test
    void selectingMemoryIssuePopulatesTitleAndDetails() {
        AdvancedJfrViewModel viewModel = new AdvancedJfrViewModel(new FakeAdvancedJfrAnalysisService());
        MemoryIssue issue = memoryIssue(MemoryIssueSeverity.WARNING, "java.lang.String",
                10 * 1024 * 1024, 12, 87.5);

        viewModel.selectMemoryIssue(issue);

        assertEquals(issue, viewModel.selectedMemoryIssueProperty().get());
        assertEquals("WARNING - java.lang.String", viewModel.selectedMemoryIssueTitleProperty().get());
        String details = viewModel.selectedMemoryIssueDetailsProperty().get();
        assertEquals("""
                Category: ALLOCATION_HOTSPOT
                Estimated bytes: 10.0 MB
                Count: 12
                Score: 87.5%
                Evidence: Allocation stack dominated by java.lang.String
                Recommendation: Inspect allocation pressure""", details);
    }

    @Test
    void nullMemoryIssueSelectionClearsTitleAndDetails() {
        AdvancedJfrViewModel viewModel = new AdvancedJfrViewModel(new FakeAdvancedJfrAnalysisService());
        viewModel.selectMemoryIssue(memoryIssue(MemoryIssueSeverity.WARNING, "java.lang.String",
                10 * 1024 * 1024, 12, 87.5));

        viewModel.selectMemoryIssue(null);

        assertNull(viewModel.selectedMemoryIssueProperty().get());
        assertEquals("", viewModel.selectedMemoryIssueTitleProperty().get());
        assertEquals("", viewModel.selectedMemoryIssueDetailsProperty().get());
    }

    @Test
    void loadClearsPreviousSelectedMemoryIssue() {
        FakeAdvancedJfrAnalysisService service = new FakeAdvancedJfrAnalysisService();
        service.setHeatmap(sampleHeatmap());
        service.setMemoryAnalysisReport(new MemoryAnalysisReport(0, 0, List.of()));
        AdvancedJfrViewModel viewModel = new AdvancedJfrViewModel(service);
        viewModel.selectMemoryIssue(memoryIssue(MemoryIssueSeverity.WARNING, "java.lang.String",
                10 * 1024 * 1024, 12, 87.5));

        viewModel.load(recording());

        assertNull(viewModel.selectedMemoryIssueProperty().get());
        assertEquals("", viewModel.selectedMemoryIssueTitleProperty().get());
        assertEquals("", viewModel.selectedMemoryIssueDetailsProperty().get());
    }

    private EventHeatmap sampleHeatmap() {
        EventHeatmapCell cell = new EventHeatmapCell("jdk.CPULoad", Instant.EPOCH,
                Instant.EPOCH.plusSeconds(1), 4);
        return new EventHeatmap(Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 20,
                List.of(new EventHeatmapRow("jdk.CPULoad", "CPU Load",
                        List.of("Operating System"), 4, List.of(cell))));
    }

    private RecordingSummary recording() {
        return new RecordingSummary("rec", Path.of("sample.jfr"), "sample.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 1024);
    }

    private MemoryIssue memoryIssue(MemoryIssueSeverity severity, String subject, long estimatedBytes,
            long count, double score) {
        return new MemoryIssue(MemoryIssueCategory.ALLOCATION_HOTSPOT, severity, subject, estimatedBytes,
                count, score, "Allocation stack dominated by " + subject, "Inspect allocation pressure");
    }
}
