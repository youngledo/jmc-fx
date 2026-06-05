package io.github.youngledo.jmcfx.ui.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.application.LoadJfrMetadataUseCase;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.ui.testsupport.FakeJfrMetadataService;

class JfrMetadataViewModelTest {

    @Test
    void loadPublishesMetadataRowsAndSummary() {
        JfrMetadataViewModel viewModel = new JfrMetadataViewModel(new LoadJfrMetadataUseCase(new FakeJfrMetadataService()));

        viewModel.load(recording());

        assertEquals(2, viewModel.eventTypesProperty().size());
        assertEquals("2 event types, 3 events, 2 fields", viewModel.summaryProperty().get());
        assertFalse(viewModel.loadingProperty().get());
        assertFalse(viewModel.errorProperty().get());
    }

    @Test
    void selectingEventTypeBuildsFieldDetailText() {
        JfrMetadataViewModel viewModel = new JfrMetadataViewModel(new LoadJfrMetadataUseCase(new FakeJfrMetadataService()));
        viewModel.load(recording());

        viewModel.selectedEventTypeProperty().set(viewModel.eventTypesProperty().stream()
                .filter(type -> "jdk.CPULoad".equals(type.id()))
                .findFirst()
                .orElseThrow());

        assertTrue(viewModel.selectedDetailProperty().get().contains("jdk.CPULoad"));
        assertTrue(viewModel.selectedDetailProperty().get().contains("jvmUser"));
        assertTrue(viewModel.selectedDetailProperty().get().contains("Type: NUMBER"));
        assertTrue(viewModel.selectedDetailProperty().get().contains("Filter value type: NUMBER"));
        assertTrue(viewModel.selectedDetailProperty().get().contains("Unit: %"));
    }

    private static RecordingSummary recording() {
        return new RecordingSummary("sample", Path.of("sample.jfr"), "sample.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 1024);
    }
}
