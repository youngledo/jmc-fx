package com.youngledo.jmcfx.ui.heap;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.application.LoadHeapUseCase;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.HeapClassHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.ui.testsupport.FakeHeapService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeapViewModelTest {

    @Test
    void loadPopulatesHistogram() {
        FakeHeapService service = new FakeHeapService();
        service.addHistogramRow(new HeapClassHistogram("java.lang.String", 1500, 36000, 0, 45.0));
        service.addHistogramRow(new HeapClassHistogram("byte[]", 800, 25600, 0, 32.0));

        HeapViewModel vm = new HeapViewModel(new LoadHeapUseCase(service));
        vm.load(testRecording());

        assertEquals(2, vm.histogramProperty().size());
        assertEquals("java.lang.String", vm.histogramProperty().getFirst().className());
    }

    @Test
    void loadPopulatesTimeline() {
        FakeHeapService service = new FakeHeapService();
        service.setTimeline(new ChartDefinition("Time", "Bytes", List.of()));

        HeapViewModel vm = new HeapViewModel(new LoadHeapUseCase(service));
        vm.load(testRecording());

        assertNotNull(vm.timelineProperty().get());
        assertEquals("Time", vm.timelineProperty().get().xLabel());
    }

    @Test
    void loadClearsPreviousData() {
        FakeHeapService service = new FakeHeapService();
        service.addHistogramRow(new HeapClassHistogram("java.lang.Object", 100, 800, 0, 10.0));

        HeapViewModel vm = new HeapViewModel(new LoadHeapUseCase(service));
        vm.load(testRecording());
        assertEquals(1, vm.histogramProperty().size());

        FakeHeapService emptyService = new FakeHeapService();
        HeapViewModel vm2 = new HeapViewModel(new LoadHeapUseCase(emptyService));
        vm2.load(testRecording());
        assertTrue(vm2.histogramProperty().isEmpty());
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.now(), Instant.now(), 1000, 1024);
    }
}
