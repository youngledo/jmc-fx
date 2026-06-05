package io.github.youngledo.jmcfx.ui.socketio;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.youngledo.jmcfx.application.LoadSocketIOUseCase;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.SocketIOGrouping;
import io.github.youngledo.jmcfx.domain.model.SocketIOHistogram;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;
import io.github.youngledo.jmcfx.ui.testsupport.FakeSocketIOService;

import javafx.application.Platform;
import javafx.scene.layout.VBox;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SocketIOViewModelTest {

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
        FakeSocketIOService service = new FakeSocketIOService();
        service.addHistogramRow(new SocketIOHistogram("10.0.0.1:8080",
                "10.0.0.1", 8080, 50, 30, 102400, 51200, 300, 25, 3.75));

        SocketIOViewModel vm = new SocketIOViewModel(new LoadSocketIOUseCase(service));
        vm.load(testRecording());

        assertEquals(1, vm.histogramProperty().size());
        assertEquals("10.0.0.1:8080", vm.histogramProperty().getFirst().key());
        assertEquals("test", vm.currentRecordingProperty().get().id());
    }

    @Test
    void setGroupingReloadsHistogram() {
        FakeSocketIOService service = new FakeSocketIOService();
        service.addHistogramRow(new SocketIOHistogram("10.0.0.1:8080",
                "10.0.0.1", 8080, 50, 30, 102400, 51200, 300, 25, 3.75));

        SocketIOViewModel vm = new SocketIOViewModel(new LoadSocketIOUseCase(service));
        vm.load(testRecording());

        assertEquals(SocketIOGrouping.BY_HOST_AND_PORT, vm.groupingProperty().get());

        vm.setGrouping(SocketIOGrouping.BY_HOST);

        assertEquals(SocketIOGrouping.BY_HOST, vm.groupingProperty().get());
        assertEquals(1, vm.histogramProperty().size());
    }

    @Test
    void controllerShowsLocalizedRecordingContext() {
        FakeSocketIOService service = new FakeSocketIOService();
        SocketIOViewModel vm = new SocketIOViewModel(new LoadSocketIOUseCase(service));
        vm.load(testRecording());

        SocketIoPaneView pane = new SocketIoPaneView(new VBox());
        I18n i18n = new I18n(Locale.ENGLISH);
        SocketIoPageController controller = new SocketIoPageController(pane.view(), i18n);
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
