package com.youngledo.jmcfx.ui.tlab;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.application.LoadTlabUseCase;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.TlabAllocation;
import com.youngledo.jmcfx.testsupport.FakeTlabService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TlabViewModelTest {

    @Test
    void loadPopulatesAllocations() {
        FakeTlabService service = new FakeTlabService();
        service.addAllocation(new TlabAllocation("main", 500, 10, 256.0, 1024.0, 128000, 10240));

        TlabViewModel vm = new TlabViewModel(new LoadTlabUseCase(service));
        vm.load(testRecording());

        assertEquals(1, vm.allocationsProperty().size());
        assertEquals("main", vm.allocationsProperty().getFirst().thread());
    }

    @Test
    void loadPopulatesTimeline() {
        FakeTlabService service = new FakeTlabService();
        service.setTimeline(new ChartDefinition("Time", "Bytes", List.of()));

        TlabViewModel vm = new TlabViewModel(new LoadTlabUseCase(service));
        vm.load(testRecording());

        assertNotNull(vm.timelineProperty().get());
    }

    @Test
    void startsUnloadedAndBecomesLoadedAfterLoadCompletes() {
        FakeTlabService service = new FakeTlabService();
        TlabViewModel vm = new TlabViewModel(new LoadTlabUseCase(service));

        assertFalse(vm.loadedProperty().get());
        assertFalse(vm.loadingProperty().get());

        vm.load(testRecording());

        assertTrue(vm.loadedProperty().get());
        assertFalse(vm.loadingProperty().get());
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.now(), Instant.now(), 1000, 1024);
    }
}
