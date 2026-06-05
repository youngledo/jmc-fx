package io.github.youngledo.jmcfx.ui.heap;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.youngledo.jmcfx.application.LoadHeapUseCase;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.HeapClassHistogram;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;
import io.github.youngledo.jmcfx.ui.testsupport.FakeHeapService;

import javafx.application.Platform;
import javafx.scene.layout.VBox;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeapViewModelTest {

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
        FakeHeapService service = new FakeHeapService();
        service.addHistogramRow(new HeapClassHistogram("java.lang.String", 1500, 36000, 0, 45.0));
        service.addHistogramRow(new HeapClassHistogram("byte[]", 800, 25600, 0, 32.0));

        HeapViewModel vm = new HeapViewModel(new LoadHeapUseCase(service));
        vm.load(testRecording());

        assertEquals(2, vm.histogramProperty().size());
        assertEquals("java.lang.String", vm.histogramProperty().getFirst().className());
        assertEquals("test", vm.currentRecordingProperty().get().id());
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

    @Test
    void controllerShowsLocalizedRecordingContext() {
        FakeHeapService service = new FakeHeapService();
        HeapViewModel vm = new HeapViewModel(new LoadHeapUseCase(service));
        vm.load(testRecording());

        HeapPaneView pane = new HeapPaneView(new VBox());
        I18n i18n = new I18n(Locale.ENGLISH);
        HeapPageController controller = new HeapPageController(pane.view(), i18n);
        controller.configure();
        controller.bind(vm);

        assertTrue(pane.view().recordingContextLabel().getText().startsWith("Recording range:"));

        i18n.setLanguageMode(LanguageMode.CHINESE_SIMPLIFIED);

        assertTrue(pane.view().recordingContextLabel().getText().startsWith("录制范围："));
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.parse("2026-06-05T01:02:03Z"),
                Instant.parse("2026-06-05T01:02:04Z"),
                1000, 1024);
    }
}
