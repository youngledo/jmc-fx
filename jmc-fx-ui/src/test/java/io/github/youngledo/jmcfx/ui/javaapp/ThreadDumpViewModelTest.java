package io.github.youngledo.jmcfx.ui.javaapp;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.application.LoadJavaApplicationUseCase;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.ThreadDumpEntry;
import io.github.youngledo.jmcfx.ui.testsupport.FakeJavaAppService;

class ThreadDumpViewModelTest {

    @Test
    void loadPopulatesDumps() {
        FakeJavaAppService service = new FakeJavaAppService();
        service.addThreadDump(new ThreadDumpEntry(Instant.now(), "Full thread dump ..."));

        ThreadDumpViewModel vm = new ThreadDumpViewModel(new LoadJavaApplicationUseCase(service));
        vm.load(testRecording());

        assertEquals(1, vm.dumpsProperty().size());
    }

    @Test
    void loadClearsSelectionAndText() {
        FakeJavaAppService service = new FakeJavaAppService();
        service.addThreadDump(new ThreadDumpEntry(Instant.now(), "Full thread dump ..."));

        ThreadDumpViewModel vm = new ThreadDumpViewModel(new LoadJavaApplicationUseCase(service));
        vm.load(testRecording());
        vm.selectedDumpProperty().set(vm.dumpsProperty().getFirst());
        assertFalse(vm.dumpTextProperty().get().isEmpty());

        vm.load(testRecording());
        assertNull(vm.selectedDumpProperty().get());
        assertTrue(vm.dumpTextProperty().get().isEmpty());
    }

    @Test
    void selectingDumpShowsText() {
        FakeJavaAppService service = new FakeJavaAppService();
        String dumpText = "main tid=1 RUNNABLE\n  at foo.Bar.run(Bar.java:42)";
        service.addThreadDump(new ThreadDumpEntry(Instant.now(), dumpText));

        ThreadDumpViewModel vm = new ThreadDumpViewModel(new LoadJavaApplicationUseCase(service));
        vm.load(testRecording());
        vm.selectedDumpProperty().set(vm.dumpsProperty().getFirst());

        assertEquals(dumpText, vm.dumpTextProperty().get());
    }

    @Test
    void deselectingDumpClearsText() {
        FakeJavaAppService service = new FakeJavaAppService();
        service.addThreadDump(new ThreadDumpEntry(Instant.now(), "Full thread dump ..."));

        ThreadDumpViewModel vm = new ThreadDumpViewModel(new LoadJavaApplicationUseCase(service));
        vm.load(testRecording());
        vm.selectedDumpProperty().set(vm.dumpsProperty().getFirst());
        vm.selectedDumpProperty().set(null);

        assertTrue(vm.dumpTextProperty().get().isEmpty());
    }

    @Test
    void startsWithEmptyDefaults() {
        FakeJavaAppService service = new FakeJavaAppService();
        ThreadDumpViewModel vm = new ThreadDumpViewModel(new LoadJavaApplicationUseCase(service));

        assertEquals(0, vm.dumpsProperty().size());
        assertNull(vm.selectedDumpProperty().get());
        assertTrue(vm.dumpTextProperty().get().isEmpty());
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.now(), Instant.now(), 1000, 1024);
    }
}
