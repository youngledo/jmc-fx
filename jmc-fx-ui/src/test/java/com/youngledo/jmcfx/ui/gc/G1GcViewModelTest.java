package com.youngledo.jmcfx.ui.gc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.application.LoadG1GcUseCase;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.ui.testsupport.FakeG1GcService;

class G1GcViewModelTest {

    @Test
    void loadPublishesG1RowsAndSummary() {
        G1GcViewModel viewModel = new G1GcViewModel(new LoadG1GcUseCase(new FakeG1GcService()));

        viewModel.load(recording());

        assertEquals(2, viewModel.regionSummariesProperty().size());
        assertEquals(3, viewModel.recentRegionStatesProperty().size());
        assertEquals(2, viewModel.gcPausesProperty().size());
        assertEquals("2 snapshots, 1 transitions, 2 GC pauses, 3 regions", viewModel.summaryProperty().get());
        assertFalse(viewModel.loadingProperty().get());
        assertFalse(viewModel.errorProperty().get());
    }

    @Test
    void selectingRegionStateBuildsDetailText() {
        G1GcViewModel viewModel = new G1GcViewModel(new LoadG1GcUseCase(new FakeG1GcService()));
        viewModel.load(recording());

        viewModel.selectedRegionStateProperty().set(viewModel.recentRegionStatesProperty().get(1));

        assertTrue(viewModel.selectedDetailProperty().get().contains("Region 1"));
        assertTrue(viewModel.selectedDetailProperty().get().contains("Old -> Humongous"));
    }

    @Test
    void loadFailurePublishesErrorState() {
        G1GcViewModel viewModel = new G1GcViewModel(new LoadG1GcUseCase(recording -> {
            throw new IllegalStateException("broken g1 report");
        }));

        viewModel.load(recording());

        assertTrue(viewModel.errorProperty().get());
        assertEquals("broken g1 report", viewModel.errorMessageProperty().get());
        assertTrue(viewModel.regionSummariesProperty().isEmpty());
        assertFalse(viewModel.loadingProperty().get());
    }

    private static RecordingSummary recording() {
        return new RecordingSummary("sample", Path.of("sample.jfr"), "sample.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 1024);
    }
}
