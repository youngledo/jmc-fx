package io.github.youngledo.jmcfx.ui.javaapp;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.scene.layout.VBox;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.application.LoadJavaApplicationUseCase;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.ThreadHistogramRow;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;
import io.github.youngledo.jmcfx.ui.testsupport.FakeJavaAppService;

class JavaAppOverviewViewModelTest {

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
    void loadPopulatesHistogramRows() {
        FakeJavaAppService service = new FakeJavaAppService();
        service.addHistogramRow(new ThreadHistogramRow("main", 100, 50, 20, 4096, 3));
        service.addHistogramRow(new ThreadHistogramRow("worker-1", 80, 200, 10, 8192, 1));

        JavaAppOverviewViewModel vm = new JavaAppOverviewViewModel(new LoadJavaApplicationUseCase(service));
        vm.load(testRecording());

        assertEquals(2, vm.histogramRowsProperty().size());
        assertEquals("main", vm.histogramRowsProperty().getFirst().threadName());
        assertEquals(100, vm.histogramRowsProperty().getFirst().profilingCount());
        assertEquals("test", vm.currentRecordingProperty().get().id());
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
        assertNull(vm.currentRecordingProperty().get());
    }

    @Test
    void controllerShowsLocalizedThreadHistogramRecordingContext() {
        FakeJavaAppService service = new FakeJavaAppService();
        JavaAppOverviewViewModel vm = new JavaAppOverviewViewModel(new LoadJavaApplicationUseCase(service));
        vm.load(testRecording());

        JavaApplicationDataPaneView pane = new JavaApplicationDataPaneView(
                new VBox(), new VBox(), new VBox(), new VBox(), new VBox(), new VBox());
        I18n i18n = new I18n(Locale.ENGLISH);
        JavaApplicationDataPagesController controller =
                new JavaApplicationDataPagesController(pane.javaApplicationDataPages(), i18n);
        controller.configure();
        controller.bindThreadHistogram(vm);

        assertTrue(pane.javaApplicationDataPages().threadHistogramRecordingContextLabel().getText()
                .startsWith("Recording range:"));

        i18n.setLanguageMode(LanguageMode.CHINESE_SIMPLIFIED);

        assertTrue(pane.javaApplicationDataPages().threadHistogramRecordingContextLabel().getText()
                .startsWith("录制范围："));
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.parse("2026-06-05T01:02:03Z"),
                Instant.parse("2026-06-05T01:02:04Z"),
                1000, 1024);
    }
}
