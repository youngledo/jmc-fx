package com.youngledo.jmcfx.ui.locks;

import java.nio.file.Path;
import java.time.Instant;

import com.youngledo.jmcfx.application.LoadLocksUseCase;

import com.youngledo.jmcfx.domain.model.LockGrouping;
import com.youngledo.jmcfx.domain.model.LockHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.testsupport.FakeLockService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LockViewModelTest {

    @Test
    void loadPopulatesAllHistograms() {
        FakeLockService service = new FakeLockService();
        service.addHistogramRow(new LockHistogram("java.lang.Object",
                100, 5000, 200, 50.0, 3, 5, 12));

        LockViewModel vm = new LockViewModel(new LoadLocksUseCase(service));
        vm.load(testRecording());

        assertEquals(1, vm.classHistogramProperty().size());
        assertEquals(1, vm.addressHistogramProperty().size());
        assertEquals(1, vm.threadHistogramProperty().size());
        assertEquals("java.lang.Object", vm.classHistogramProperty().getFirst().key());
    }

    @Test
    void setPrimaryGroupingUpdatesProperty() {
        FakeLockService service = new FakeLockService();
        LockViewModel vm = new LockViewModel(new LoadLocksUseCase(service));
        vm.load(testRecording());

        assertEquals(LockGrouping.BY_CLASS, vm.primaryGroupingProperty().get());

        vm.setPrimaryGrouping(LockGrouping.BY_ADDRESS);

        assertEquals(LockGrouping.BY_ADDRESS, vm.primaryGroupingProperty().get());
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.now(), Instant.now(), 1000, 1024);
    }
}
