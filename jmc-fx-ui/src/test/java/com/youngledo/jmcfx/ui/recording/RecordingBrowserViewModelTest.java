package com.youngledo.jmcfx.ui.recording;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.application.OpenRecordingUseCase;

import com.youngledo.jmcfx.testsupport.FakeRecordingRepository;

class RecordingBrowserViewModelTest {

    @Test
    void opensRecordingAndKeepsSummary() {
        RecordingBrowserViewModel viewModel = new RecordingBrowserViewModel(new OpenRecordingUseCase(new FakeRecordingRepository()));

        viewModel.openRecording(Path.of("sample.jfr"));

        assertEquals(1, viewModel.recordingsProperty().size());
        assertEquals("sample.jfr", viewModel.selectedRecordingProperty().get().name());
    }
}
