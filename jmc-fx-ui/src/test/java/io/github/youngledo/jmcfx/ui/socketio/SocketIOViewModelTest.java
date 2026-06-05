package io.github.youngledo.jmcfx.ui.socketio;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.youngledo.jmcfx.application.LoadSocketIOUseCase;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.SocketIOEvent;
import io.github.youngledo.jmcfx.domain.model.SocketIOGrouping;
import io.github.youngledo.jmcfx.domain.model.SocketIOHistogram;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;
import io.github.youngledo.jmcfx.ui.recording.RecordingTimeRange;
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
    void timeRangeFiltersEventLogAndClearsBackToFullRecording() {
        FakeSocketIOService service = new FakeSocketIOService();
        service.addEvent(new SocketIOEvent("jdk.SocketRead", "10.0.0.1", 8080,
                10, 0, 1.0, Instant.parse("2026-06-05T01:02:03Z").toEpochMilli(), "main"));
        service.addEvent(new SocketIOEvent("jdk.SocketWrite", "10.0.0.2", 9090,
                20, 0, 2.0, Instant.parse("2026-06-05T01:02:04Z").toEpochMilli(), "worker"));

        SocketIOViewModel vm = new SocketIOViewModel(new LoadSocketIOUseCase(service));
        vm.load(testRecording());

        assertEquals(2, vm.eventsProperty().size());

        vm.timeRangeProperty().set(new RecordingTimeRange(
                Instant.parse("2026-06-05T01:02:03.500Z").toEpochMilli(),
                Instant.parse("2026-06-05T01:02:04.500Z").toEpochMilli()));

        assertEquals(1, vm.eventsProperty().size());
        assertEquals("10.0.0.2", vm.eventsProperty().getFirst().host());

        vm.timeRangeProperty().set(null);

        assertEquals(2, vm.eventsProperty().size());
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

    @Test
    void controllerUsesSharedTimeRangeForContextAndFiltering() {
        FakeSocketIOService service = new FakeSocketIOService();
        service.addEvent(new SocketIOEvent("jdk.SocketRead", "10.0.0.1", 8080,
                10, 0, 1.0, Instant.parse("2026-06-05T01:02:03Z").toEpochMilli(), "main"));
        service.addEvent(new SocketIOEvent("jdk.SocketWrite", "10.0.0.2", 9090,
                20, 0, 2.0, Instant.parse("2026-06-05T01:02:04Z").toEpochMilli(), "worker"));
        SocketIOViewModel vm = new SocketIOViewModel(new LoadSocketIOUseCase(service));
        vm.load(testRecording());

        SocketIoPaneView pane = new SocketIoPaneView(new VBox());
        I18n i18n = new I18n(Locale.ENGLISH);
        SocketIoPageController controller = new SocketIoPageController(pane.view(), i18n);
        javafx.beans.property.ObjectProperty<RecordingTimeRange> sharedRange =
                new javafx.beans.property.SimpleObjectProperty<>();
        controller.configure();
        controller.bind(vm, sharedRange);

        sharedRange.set(new RecordingTimeRange(
                Instant.parse("2026-06-05T01:02:03.500Z").toEpochMilli(),
                Instant.parse("2026-06-05T01:02:04.500Z").toEpochMilli()));

        assertTrue(pane.view().recordingContextLabel().getText().startsWith("Active window:"));
        assertTrue(pane.view().clearTimeRangeButton().isVisible());
        assertNotNull(pane.view().timelineChart().userSelectedRangeProperty().get());
        assertEquals(sharedRange.get().startEpochMillis(),
                Math.round(pane.view().timelineChart().userSelectedRangeProperty().get().lowerBound()));
        assertEquals(1, vm.eventsProperty().size());

        pane.view().clearTimeRangeButton().fire();

        assertNull(sharedRange.get());
        assertNull(pane.view().timelineChart().userSelectedRangeProperty().get());
        assertEquals(2, vm.eventsProperty().size());
    }

    @Test
    void controllerRestoresSharedSelectionAfterTimelineDataLoads() {
        FakeSocketIOService service = new FakeSocketIOService();
        SocketIOViewModel vm = new SocketIOViewModel(new LoadSocketIOUseCase(service));
        SocketIoPaneView pane = new SocketIoPaneView(new VBox());
        SocketIoPageController controller = new SocketIoPageController(pane.view(), new I18n(Locale.ENGLISH));
        javafx.beans.property.ObjectProperty<RecordingTimeRange> sharedRange =
                new javafx.beans.property.SimpleObjectProperty<>(new RecordingTimeRange(
                        Instant.parse("2026-06-05T01:02:03.250Z").toEpochMilli(),
                        Instant.parse("2026-06-05T01:02:03.750Z").toEpochMilli()));
        controller.configure();
        controller.bind(vm, sharedRange);

        assertNotNull(pane.view().timelineChart().userSelectedRangeProperty().get());

        vm.load(testRecording());

        assertNotNull(pane.view().timelineChart().userSelectedRangeProperty().get());
        assertEquals(sharedRange.get().startEpochMillis(),
                Math.round(pane.view().timelineChart().userSelectedRangeProperty().get().lowerBound()));
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.parse("2026-06-05T01:02:03Z"),
                Instant.parse("2026-06-05T01:02:04Z"),
                1000, 1024);
    }
}
