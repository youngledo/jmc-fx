package com.youngledo.jmcfx.ui.exceptions;

import java.nio.file.Path;
import java.time.Instant;

import com.youngledo.jmcfx.application.LoadExceptionsUseCase;

import com.youngledo.jmcfx.domain.model.ExceptionGrouping;
import com.youngledo.jmcfx.domain.model.ExceptionSummary;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.ui.testsupport.FakeExceptionService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionViewModelTest {

    @Test
    void loadPopulatesHistogram() {
        FakeExceptionService service = new FakeExceptionService();
        service.addException(new ExceptionSummary("java.lang.NullPointerException",
                "java.lang.NullPointerException", null, 42, 60.0));

        ExceptionViewModel vm = new ExceptionViewModel(new LoadExceptionsUseCase(service));
        vm.load(testRecording());

        assertEquals(1, vm.histogramProperty().size());
        assertEquals("java.lang.NullPointerException", vm.histogramProperty().getFirst().className());
        assertEquals(42, vm.histogramProperty().getFirst().count());
    }

    @Test
    void setGroupingReloadsHistogram() {
        FakeExceptionService service = new FakeExceptionService();
        service.addException(new ExceptionSummary("java.io.IOException",
                "java.io.IOException", "Connection reset", 10, 100.0));

        ExceptionViewModel vm = new ExceptionViewModel(new LoadExceptionsUseCase(service));
        vm.load(testRecording());

        assertEquals(ExceptionGrouping.BY_CLASS, vm.groupingProperty().get());

        vm.setGrouping(ExceptionGrouping.BY_MESSAGE);

        assertEquals(ExceptionGrouping.BY_MESSAGE, vm.groupingProperty().get());
        // FakeExceptionService returns the same histogram regardless of grouping,
        // so the histogram should still have 1 entry after reload
        assertEquals(1, vm.histogramProperty().size());
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.now(), Instant.now(), 1000, 1024);
    }
}
