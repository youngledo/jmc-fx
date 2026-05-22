package com.youngledo.jmcfx.ui.threads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.ThreadSummary;
import com.youngledo.jmcfx.testsupport.FakeThreadService;

class ThreadViewModelTest {

    @Test
    void loadPopulatesThreadSummaries() {
        FakeThreadService service = new FakeThreadService();
        service.addThread(new ThreadSummary("main", 1, "main", false, 100, 50, List.of()));
        service.addThread(new ThreadSummary("worker-1", 2, "pool", true, 80, 10, List.of()));

        ThreadViewModel vm = new ThreadViewModel(service);
        vm.load(testRecording());

        assertEquals(2, vm.threadSummariesProperty().size());
        assertEquals("main", vm.threadSummariesProperty().getFirst().threadName());
    }

    @Test
    void loadClearsSelection() {
        FakeThreadService service = new FakeThreadService();
        service.addThread(new ThreadSummary("main", 1, "main", false, 100, 50, List.of()));

        ThreadViewModel vm = new ThreadViewModel(service);
        vm.load(testRecording());
        vm.selectedThreadProperty().set(vm.threadSummariesProperty().getFirst());

        // Load again — selection should be cleared
        vm.load(testRecording());
        assertNull(vm.selectedThreadProperty().get());
    }

    @Test
    void startsWithEmptyDefaults() {
        FakeThreadService service = new FakeThreadService();
        ThreadViewModel vm = new ThreadViewModel(service);

        assertEquals(0, vm.threadSummariesProperty().size());
        assertNull(vm.selectedThreadProperty().get());
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.now(), Instant.now(), 1000, 1024);
    }
}
