package com.youngledo.jmcfx.ui.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.domain.model.EventHeatmap;
import com.youngledo.jmcfx.domain.model.EventHeatmapCell;
import com.youngledo.jmcfx.domain.model.EventHeatmapRow;
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
}
