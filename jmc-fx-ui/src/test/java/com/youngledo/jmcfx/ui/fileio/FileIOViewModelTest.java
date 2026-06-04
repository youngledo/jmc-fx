package com.youngledo.jmcfx.ui.fileio;

import java.nio.file.Path;
import java.time.Instant;

import com.youngledo.jmcfx.application.LoadFileIOUseCase;

import com.youngledo.jmcfx.domain.model.FileIOHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.testsupport.FakeFileIOService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileIOViewModelTest {

    @Test
    void loadPopulatesHistogramAndEvents() {
        FakeFileIOService service = new FakeFileIOService();
        service.addHistogramRow(new FileIOHistogram("/var/log/app.log",
                10, 5, 4096, 2048, 150, 50, 10.0));

        FileIOViewModel vm = new FileIOViewModel(new LoadFileIOUseCase(service));
        vm.load(testRecording());

        assertEquals(1, vm.histogramProperty().size());
        assertEquals("/var/log/app.log", vm.histogramProperty().getFirst().path());
        assertEquals(10, vm.histogramProperty().getFirst().readCount());
        assertNotNull(vm.timelineProperty().get());
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.now(), Instant.now(), 1000, 1024);
    }
}
