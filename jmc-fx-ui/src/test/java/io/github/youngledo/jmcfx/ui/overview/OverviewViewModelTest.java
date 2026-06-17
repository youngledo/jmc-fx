package io.github.youngledo.jmcfx.ui.overview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.JvmConnectionSource;
import io.github.youngledo.jmcfx.ui.jvms.LiveFlightRecordingOrigin;

class OverviewViewModelTest {

    @Test
    void startsWithEmptyDefaults() {
        OverviewViewModel viewModel = new OverviewViewModel();

        assertEquals("", viewModel.recordingNameProperty().get());
        assertEquals("", viewModel.recordingDetailsProperty().get());
        assertEquals("", viewModel.liveOriginDetailsProperty().get());
        assertEquals(false, viewModel.liveOriginVisibleProperty().get());
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

    @Test
    void exposesLiveOriginSummaryWhenRecordingComesFromLiveJvm() {
        OverviewViewModel viewModel = new OverviewViewModel();
        RecordingSummary recording = new RecordingSummary("rec", Path.of("rec.jfr"), "rec.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);
        LiveFlightRecordingOrigin origin = new LiveFlightRecordingOrigin("42", "demo.Main",
                "service:jmx:local://42", JvmConnectionSource.LOCAL, "42", "26.0.1", 100, "jmcfx-42");

        viewModel.showRecording(recording, "details", origin, "JVM: demo.Main");

        assertEquals(origin, viewModel.liveOriginProperty().get());
        assertEquals("JVM: demo.Main", viewModel.liveOriginDetailsProperty().get());
        assertEquals(true, viewModel.liveOriginVisibleProperty().get());
    }
}
