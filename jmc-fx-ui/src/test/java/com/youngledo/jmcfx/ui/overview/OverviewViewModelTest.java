package com.youngledo.jmcfx.ui.overview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.RecordingSummary;

class OverviewViewModelTest {

    @Test
    void startsWithEmptyDefaults() {
        OverviewViewModel viewModel = new OverviewViewModel();

        assertEquals("", viewModel.recordingNameProperty().get());
        assertEquals("", viewModel.recordingDetailsProperty().get());
        assertEquals("", viewModel.analysisStatusProperty().get());
        assertEquals("", viewModel.jvmStatusProperty().get());
    }

    @Test
    void exposesRecordingSummaryAfterOpen() {
        OverviewViewModel viewModel = new OverviewViewModel();
        RecordingSummary recording = new RecordingSummary("rec", Path.of("rec.jfr"), "rec.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);

        String details = "Path: rec.jfr | Duration: 1000 ms | Size: 128 bytes";
        viewModel.showRecording(recording, details);

        assertEquals("rec.jfr", viewModel.recordingNameProperty().get());
        assertTrue(viewModel.recordingDetailsProperty().get().contains("Duration: 1000 ms"));
        assertTrue(viewModel.recordingDetailsProperty().get().contains("Size: 128 bytes"));
    }
}
