package com.youngledo.jmcfx.ui.javaapp;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.application.LoadJavaApplicationUseCase;

import com.youngledo.jmcfx.domain.model.NativeLibraryEntry;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.testsupport.FakeJavaAppService;

class NativeLibraryViewModelTest {

    @Test
    void loadPopulatesLibraries() {
        FakeJavaAppService service = new FakeJavaAppService();
        service.addNativeLibrary(new NativeLibraryEntry(Instant.now(), "libjvm.so",
                "/usr/lib/jvm/", "/usr/lib/jvm/lib/libjvm.so"));

        NativeLibraryViewModel vm = new NativeLibraryViewModel(new LoadJavaApplicationUseCase(service));
        vm.load(testRecording());

        assertEquals(1, vm.librariesProperty().size());
        assertEquals("libjvm.so", vm.librariesProperty().getFirst().name());
    }

    @Test
    void loadClearsSelection() {
        FakeJavaAppService service = new FakeJavaAppService();
        service.addNativeLibrary(new NativeLibraryEntry(Instant.now(), "libjvm.so", "", ""));

        NativeLibraryViewModel vm = new NativeLibraryViewModel(new LoadJavaApplicationUseCase(service));
        vm.load(testRecording());
        vm.selectedLibraryProperty().set(vm.librariesProperty().getFirst());

        vm.load(testRecording());
        assertNull(vm.selectedLibraryProperty().get());
    }

    @Test
    void startsWithEmptyDefaults() {
        FakeJavaAppService service = new FakeJavaAppService();
        NativeLibraryViewModel vm = new NativeLibraryViewModel(new LoadJavaApplicationUseCase(service));

        assertEquals(0, vm.librariesProperty().size());
        assertNull(vm.selectedLibraryProperty().get());
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.now(), Instant.now(), 1000, 1024);
    }
}
