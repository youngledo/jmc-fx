package io.github.youngledo.jmcfx.ui.jfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.application.LoadJavaFxEventsUseCase;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.ui.testsupport.FakeJavaFxEventService;

class JavaFxEventsViewModelTest {

    @Test
    void loadPublishesJavaFxRowsAndSummary() {
        JavaFxEventsViewModel viewModel =
                new JavaFxEventsViewModel(new LoadJavaFxEventsUseCase(new FakeJavaFxEventService()));

        viewModel.load(recording());

        assertEquals(1, viewModel.pulseSummariesProperty().size());
        assertEquals(2, viewModel.pulsePhasesProperty().size());
        assertEquals(2, viewModel.inputEventsProperty().size());
        assertEquals("1 pulses, 2 phases, 2 input events, 1 slow phases", viewModel.summaryProperty().get());
        assertFalse(viewModel.loadingProperty().get());
        assertFalse(viewModel.errorProperty().get());
    }

    @Test
    void selectingPulsePhaseBuildsDetailText() {
        JavaFxEventsViewModel viewModel =
                new JavaFxEventsViewModel(new LoadJavaFxEventsUseCase(new FakeJavaFxEventService()));
        viewModel.load(recording());

        viewModel.selectedPulsePhaseProperty().set(viewModel.pulsePhasesProperty().get(1));

        assertTrue(viewModel.selectedDetailProperty().get().contains("Pulse 42"));
        assertTrue(viewModel.selectedDetailProperty().get().contains("Rendering"));
        assertTrue(viewModel.selectedDetailProperty().get().contains("QuantumRenderer-0"));
    }

    @Test
    void loadFailurePublishesErrorState() {
        JavaFxEventsViewModel viewModel = new JavaFxEventsViewModel(new LoadJavaFxEventsUseCase(recording -> {
            throw new IllegalStateException("broken javafx report");
        }));

        viewModel.load(recording());

        assertTrue(viewModel.errorProperty().get());
        assertEquals("broken javafx report", viewModel.errorMessageProperty().get());
        assertTrue(viewModel.pulseSummariesProperty().isEmpty());
        assertFalse(viewModel.loadingProperty().get());
    }

    private static RecordingSummary recording() {
        return new RecordingSummary("sample", Path.of("sample.jfr"), "sample.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 1024);
    }
}
