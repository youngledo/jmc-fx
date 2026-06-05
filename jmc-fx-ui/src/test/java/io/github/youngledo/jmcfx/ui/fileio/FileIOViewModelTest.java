package io.github.youngledo.jmcfx.ui.fileio;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.youngledo.jmcfx.application.LoadFileIOUseCase;

import io.github.youngledo.jmcfx.domain.model.FileIOEvent;
import io.github.youngledo.jmcfx.domain.model.FileIOHistogram;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;
import io.github.youngledo.jmcfx.ui.recording.RecordingTimeRange;
import io.github.youngledo.jmcfx.ui.testsupport.FakeFileIOService;

import javafx.application.Platform;
import javafx.scene.layout.VBox;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileIOViewModelTest {

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
    void loadPopulatesHistogramAndEvents() {
        FakeFileIOService service = new FakeFileIOService();
        service.addHistogramRow(new FileIOHistogram("/var/log/app.log",
                10, 5, 4096, 2048, 150, 50, 10.0));

        FileIOViewModel vm = new FileIOViewModel(new LoadFileIOUseCase(service));
        vm.load(testRecording());

        assertEquals(1, vm.histogramProperty().size());
        assertEquals("/var/log/app.log", vm.histogramProperty().getFirst().path());
        assertEquals(10, vm.histogramProperty().getFirst().readCount());
        assertNotNull(vm.timelineProperty().get());
        assertEquals("test", vm.currentRecordingProperty().get().id());
    }

    @Test
    void timeRangeFiltersEventLogAndClearsBackToFullRecording() {
        FakeFileIOService service = new FakeFileIOService();
        service.addEvent(new FileIOEvent("jdk.FileRead", "/tmp/early.log", 10, 1.0,
                Instant.parse("2026-06-05T01:02:03Z").toEpochMilli(), "main"));
        service.addEvent(new FileIOEvent("jdk.FileWrite", "/tmp/late.log", 20, 2.0,
                Instant.parse("2026-06-05T01:02:04Z").toEpochMilli(), "worker"));

        FileIOViewModel vm = new FileIOViewModel(new LoadFileIOUseCase(service));
        vm.load(testRecording());

        assertEquals(2, vm.eventsProperty().size());

        vm.timeRangeProperty().set(new RecordingTimeRange(
                Instant.parse("2026-06-05T01:02:03.500Z").toEpochMilli(),
                Instant.parse("2026-06-05T01:02:04.500Z").toEpochMilli()));

        assertEquals(1, vm.eventsProperty().size());
        assertEquals("/tmp/late.log", vm.eventsProperty().getFirst().path());

        vm.timeRangeProperty().set(null);

        assertEquals(2, vm.eventsProperty().size());
    }

    @Test
    void controllerShowsLocalizedRecordingContext() {
        FakeFileIOService service = new FakeFileIOService();
        FileIOViewModel vm = new FileIOViewModel(new LoadFileIOUseCase(service));
        vm.load(testRecording());

        FileIoPaneView pane = new FileIoPaneView(new VBox());
        I18n i18n = new I18n(Locale.ENGLISH);
        FileIoPageController controller = new FileIoPageController(pane.view(), i18n);
        controller.configure();
        controller.bind(vm);

        assertTrue(pane.view().recordingContextLabel().getText().startsWith("Recording range:"));

        i18n.setLanguageMode(LanguageMode.CHINESE_SIMPLIFIED);

        assertTrue(pane.view().recordingContextLabel().getText().startsWith("录制范围："));
    }

    @Test
    void controllerUsesSharedTimeRangeForContextAndFiltering() {
        FakeFileIOService service = new FakeFileIOService();
        service.addEvent(new FileIOEvent("jdk.FileRead", "/tmp/early.log", 10, 1.0,
                Instant.parse("2026-06-05T01:02:03Z").toEpochMilli(), "main"));
        service.addEvent(new FileIOEvent("jdk.FileWrite", "/tmp/late.log", 20, 2.0,
                Instant.parse("2026-06-05T01:02:04Z").toEpochMilli(), "worker"));
        FileIOViewModel vm = new FileIOViewModel(new LoadFileIOUseCase(service));
        vm.load(testRecording());

        FileIoPaneView pane = new FileIoPaneView(new VBox());
        I18n i18n = new I18n(Locale.ENGLISH);
        FileIoPageController controller = new FileIoPageController(pane.view(), i18n);
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
        FakeFileIOService service = new FakeFileIOService();
        FileIOViewModel vm = new FileIOViewModel(new LoadFileIOUseCase(service));
        FileIoPaneView pane = new FileIoPaneView(new VBox());
        FileIoPageController controller = new FileIoPageController(pane.view(), new I18n(Locale.ENGLISH));
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
