package io.github.youngledo.jmcfx.ui.exceptions;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.youngledo.jmcfx.application.LoadExceptionsUseCase;

import io.github.youngledo.jmcfx.domain.model.ExceptionGrouping;
import io.github.youngledo.jmcfx.domain.model.ExceptionSummary;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;
import io.github.youngledo.jmcfx.ui.javaapp.JavaApplicationDataPaneView;
import io.github.youngledo.jmcfx.ui.testsupport.FakeExceptionService;

import javafx.application.Platform;
import javafx.scene.layout.VBox;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionViewModelTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await(5, TimeUnit.SECONDS);
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized by another test class.
        }
    }

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
        assertEquals("test", vm.currentRecordingProperty().get().id());
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

    @Test
    void controllerShowsLocalizedRecordingContext() {
        FakeExceptionService service = new FakeExceptionService();
        ExceptionViewModel vm = new ExceptionViewModel(new LoadExceptionsUseCase(service));
        vm.load(testRecording());

        JavaApplicationDataPaneView pane = new JavaApplicationDataPaneView(
                new VBox(), new VBox(), new VBox(), new VBox(), new VBox(), new VBox());
        I18n i18n = new I18n(Locale.ENGLISH);
        ExceptionsPageController controller = new ExceptionsPageController(pane.exceptionsPage(), i18n);
        controller.configure();
        controller.bind(vm);

        assertTrue(pane.exceptionsPage().recordingContextLabel().getText().startsWith("Recording range:"));

        i18n.setLanguageMode(LanguageMode.CHINESE_SIMPLIFIED);

        assertTrue(pane.exceptionsPage().recordingContextLabel().getText().startsWith("录制范围："));
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.parse("2026-06-05T01:02:03Z"),
                Instant.parse("2026-06-05T01:02:04Z"),
                1000, 1024);
    }
}
