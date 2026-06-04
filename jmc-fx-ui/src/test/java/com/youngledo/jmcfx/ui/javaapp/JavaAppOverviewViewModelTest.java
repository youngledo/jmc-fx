package com.youngledo.jmcfx.ui.javaapp;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.application.LoadJavaApplicationUseCase;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.ThreadHistogramRow;
import com.youngledo.jmcfx.ui.testsupport.FakeJavaAppService;

class JavaAppOverviewViewModelTest {

    @Test
    void loadPopulatesHistogramRows() {
        FakeJavaAppService service = new FakeJavaAppService();
        service.addHistogramRow(new ThreadHistogramRow("main", 100, 50, 20, 4096, 3));
        service.addHistogramRow(new ThreadHistogramRow("worker-1", 80, 200, 10, 8192, 1));

        JavaAppOverviewViewModel vm = new JavaAppOverviewViewModel(new LoadJavaApplicationUseCase(service));
        vm.load(testRecording());

        assertEquals(2, vm.histogramRowsProperty().size());
        assertEquals("main", vm.histogramRowsProperty().getFirst().threadName());
        assertEquals(100, vm.histogramRowsProperty().getFirst().profilingCount());
    }

    @Test
    void loadClearsSelection() {
        FakeJavaAppService service = new FakeJavaAppService();
        service.addHistogramRow(new ThreadHistogramRow("main", 100, 50, 20, 4096, 3));

        JavaAppOverviewViewModel vm = new JavaAppOverviewViewModel(new LoadJavaApplicationUseCase(service));
        vm.load(testRecording());
        vm.selectedRowProperty().set(vm.histogramRowsProperty().getFirst());

        vm.load(testRecording());
        assertNull(vm.selectedRowProperty().get());
    }

    @Test
    void loadSetsChart() {
        FakeJavaAppService service = new FakeJavaAppService();
        JavaAppOverviewViewModel vm = new JavaAppOverviewViewModel(new LoadJavaApplicationUseCase(service));
        vm.load(testRecording());

        assertNotNull(vm.chartProperty().get());
    }

    @Test
    void startsWithEmptyDefaults() {
        FakeJavaAppService service = new FakeJavaAppService();
        JavaAppOverviewViewModel vm = new JavaAppOverviewViewModel(new LoadJavaApplicationUseCase(service));

        assertEquals(0, vm.histogramRowsProperty().size());
        assertNull(vm.selectedRowProperty().get());
        assertNull(vm.chartProperty().get());
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.now(), Instant.now(), 1000, 1024);
    }
}
