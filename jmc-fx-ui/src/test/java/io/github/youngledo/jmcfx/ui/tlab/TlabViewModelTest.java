package io.github.youngledo.jmcfx.ui.tlab;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.youngledo.jmcfx.application.LoadTlabUseCase;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.TlabAllocation;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;
import io.github.youngledo.jmcfx.ui.testsupport.FakeTlabService;

import javafx.application.Platform;
import javafx.scene.layout.VBox;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TlabViewModelTest {

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
    void loadPopulatesAllocations() {
        FakeTlabService service = new FakeTlabService();
        service.addAllocation(new TlabAllocation("main", 500, 10, 256.0, 1024.0, 128000, 10240));

        TlabViewModel vm = new TlabViewModel(new LoadTlabUseCase(service));
        vm.load(testRecording());

        assertEquals(1, vm.allocationsProperty().size());
        assertEquals("main", vm.allocationsProperty().getFirst().thread());
        assertEquals("test", vm.currentRecordingProperty().get().id());
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

    @Test
    void controllerShowsLocalizedRecordingContext() {
        FakeTlabService service = new FakeTlabService();
        TlabViewModel vm = new TlabViewModel(new LoadTlabUseCase(service));
        vm.load(testRecording());

        TlabPaneView pane = new TlabPaneView(new VBox());
        I18n i18n = new I18n(Locale.ENGLISH);
        TlabPageController controller = new TlabPageController(pane.view(), i18n);
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
