package io.github.youngledo.jmcfx.ui.jvm;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.youngledo.jmcfx.application.LoadJvmInternalsUseCase;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;
import io.github.youngledo.jmcfx.ui.recording.RecordingTimeRange;
import io.github.youngledo.jmcfx.ui.testsupport.FakeJvmInternalsService;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.VBox;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JvmInternalsRecordingContextTest {

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
    void chartBackedViewModelsRememberCurrentRecording() {
        LoadJvmInternalsUseCase useCase = new LoadJvmInternalsUseCase(new FakeJvmInternalsService());
        RecordingSummary recording = testRecording();

        GcDetailsViewModel gc = new GcDetailsViewModel(useCase);
        CompilationsViewModel compilations = new CompilationsViewModel(useCase);
        CodeCacheViewModel codeCache = new CodeCacheViewModel(useCase);
        ClassLoadingViewModel classLoading = new ClassLoadingViewModel(useCase);

        gc.load(recording);
        compilations.load(recording);
        codeCache.load(recording);
        classLoading.load(recording);

        assertEquals("test", gc.currentRecordingProperty().get().id());
        assertEquals("test", compilations.currentRecordingProperty().get().id());
        assertEquals("test", codeCache.currentRecordingProperty().get().id());
        assertEquals("test", classLoading.currentRecordingProperty().get().id());
    }

    @Test
    void controllerShowsLocalizedRecordingContextForChartBackedPages() {
        LoadJvmInternalsUseCase useCase = new LoadJvmInternalsUseCase(new FakeJvmInternalsService());
        GcDetailsViewModel gc = new GcDetailsViewModel(useCase);
        CompilationsViewModel compilations = new CompilationsViewModel(useCase);
        CodeCacheViewModel codeCache = new CodeCacheViewModel(useCase);
        ClassLoadingViewModel classLoading = new ClassLoadingViewModel(useCase);
        VmOperationsViewModel vmOperations = new VmOperationsViewModel(useCase);
        RecordingSummary recording = testRecording();
        gc.load(recording);
        compilations.load(recording);
        codeCache.load(recording);
        classLoading.load(recording);
        vmOperations.load(recording);

        JvmInternalsPaneView pane = new JvmInternalsPaneView(
                new VBox(), new VBox(), new VBox(), new VBox(), new VBox(), new VBox(), new VBox(), new VBox());
        I18n i18n = new I18n(Locale.ENGLISH);
        JvmInternalsPagesController controller = new JvmInternalsPagesController(pane.view(), i18n);
        controller.configure();

        controller.bindGcDetails(gc);
        controller.bindCompilations(compilations);
        controller.bindCodeCache(codeCache);
        controller.bindClassLoading(classLoading);

        assertTrue(pane.view().gcDetailsRecordingContextLabel().getText().startsWith("Recording range:"));
        assertTrue(pane.view().compilationsRecordingContextLabel().getText().startsWith("Recording range:"));
        assertTrue(pane.view().codeCacheRecordingContextLabel().getText().startsWith("Recording range:"));
        assertTrue(pane.view().classLoadingRecordingContextLabel().getText().startsWith("Recording range:"));

        i18n.setLanguageMode(LanguageMode.CHINESE_SIMPLIFIED);

        assertTrue(pane.view().gcDetailsRecordingContextLabel().getText().startsWith("录制范围："));
        assertTrue(pane.view().compilationsRecordingContextLabel().getText().startsWith("录制范围："));
        assertTrue(pane.view().codeCacheRecordingContextLabel().getText().startsWith("录制范围："));
        assertTrue(pane.view().classLoadingRecordingContextLabel().getText().startsWith("录制范围："));
    }

    @Test
    void chartBackedPagesApplySharedEpochMillisRangeToEpochSecondsCharts() {
        LoadJvmInternalsUseCase useCase = new LoadJvmInternalsUseCase(new FakeJvmInternalsService());
        GcDetailsViewModel gc = new GcDetailsViewModel(useCase);
        CompilationsViewModel compilations = new CompilationsViewModel(useCase);
        CodeCacheViewModel codeCache = new CodeCacheViewModel(useCase);
        ClassLoadingViewModel classLoading = new ClassLoadingViewModel(useCase);
        VmOperationsViewModel vmOperations = new VmOperationsViewModel(useCase);
        RecordingSummary recording = testRecording();
        gc.load(recording);
        compilations.load(recording);
        codeCache.load(recording);
        classLoading.load(recording);
        vmOperations.load(recording);

        JvmInternalsPaneView pane = new JvmInternalsPaneView(
                new VBox(), new VBox(), new VBox(), new VBox(), new VBox(), new VBox(), new VBox(), new VBox());
        JvmInternalsPagesController controller = new JvmInternalsPagesController(pane.view(), new I18n(Locale.ENGLISH));
        controller.configure();
        SimpleObjectProperty<RecordingTimeRange> sharedRange =
                new SimpleObjectProperty<>(new RecordingTimeRange(1_600_000, 1_700_000));

        controller.bindGcDetails(gc, sharedRange);
        controller.bindCompilations(compilations, sharedRange);
        controller.bindCodeCache(codeCache, sharedRange);
        controller.bindClassLoading(classLoading, sharedRange);
        controller.bindVmOperations(vmOperations, sharedRange);

        assertEquals(1_600.0, pane.view().gcHeapChart().userSelectedRangeProperty().get().lowerBound());
        assertEquals(1_600.0, pane.view().compilationDurationChart().userSelectedRangeProperty().get().lowerBound());
        assertEquals(1_600.0, pane.view().codeCacheEntriesChart().userSelectedRangeProperty().get().lowerBound());
        assertEquals(1_600.0, pane.view().classLoadingChart().userSelectedRangeProperty().get().lowerBound());
        assertTrue(pane.view().gcDetailsClearTimeRangeButton().isVisible());
        assertTrue(pane.view().compilationsClearTimeRangeButton().isVisible());
        assertTrue(pane.view().codeCacheClearTimeRangeButton().isVisible());
        assertTrue(pane.view().classLoadingClearTimeRangeButton().isVisible());
        assertTrue(pane.view().vmOperationsClearTimeRangeButton().isVisible());

        pane.view().vmOperationsClearTimeRangeButton().fire();

        assertNull(sharedRange.get());
        assertNull(pane.view().gcHeapChart().userSelectedRangeProperty().get());
        assertNull(pane.view().compilationDurationChart().userSelectedRangeProperty().get());
        assertNull(pane.view().codeCacheEntriesChart().userSelectedRangeProperty().get());
        assertNull(pane.view().classLoadingChart().userSelectedRangeProperty().get());
        assertFalse(pane.view().gcDetailsClearTimeRangeButton().isVisible());
        assertFalse(pane.view().compilationsClearTimeRangeButton().isVisible());
        assertFalse(pane.view().codeCacheClearTimeRangeButton().isVisible());
        assertFalse(pane.view().classLoadingClearTimeRangeButton().isVisible());
        assertFalse(pane.view().vmOperationsClearTimeRangeButton().isVisible());
    }

    @Test
    void eventTablesUseSharedTimeRangeForRealRowFiltering() {
        LoadJvmInternalsUseCase useCase = new LoadJvmInternalsUseCase(new FakeJvmInternalsService());
        GcDetailsViewModel gc = new GcDetailsViewModel(useCase);
        CompilationsViewModel compilations = new CompilationsViewModel(useCase);
        CodeCacheViewModel codeCache = new CodeCacheViewModel(useCase);
        ClassLoadingViewModel classLoading = new ClassLoadingViewModel(useCase);
        VmOperationsViewModel vmOperations = new VmOperationsViewModel(useCase);
        RecordingSummary recording = testRecording();
        gc.load(recording);
        compilations.load(recording);
        codeCache.load(recording);
        classLoading.load(recording);
        vmOperations.load(recording);

        JvmInternalsPaneView pane = new JvmInternalsPaneView(
                new VBox(), new VBox(), new VBox(), new VBox(), new VBox(), new VBox(), new VBox(), new VBox());
        JvmInternalsPagesController controller = new JvmInternalsPagesController(pane.view(), new I18n(Locale.ENGLISH));
        controller.configure();
        SimpleObjectProperty<RecordingTimeRange> sharedRange = new SimpleObjectProperty<>();

        controller.bindGcDetails(gc, sharedRange);
        controller.bindCompilations(compilations, sharedRange);
        controller.bindCodeCache(codeCache, sharedRange);
        controller.bindClassLoading(classLoading, sharedRange);
        controller.bindVmOperations(vmOperations, sharedRange);

        assertEquals(2, pane.view().gcEventsTable().getItems().size());
        assertEquals(3, pane.view().compilationsTable().getItems().size());
        assertEquals(2, pane.view().compilationFailuresTable().getItems().size());
        assertEquals(2, pane.view().codeCacheSweepsTable().getItems().size());
        assertEquals(3, pane.view().classLoadingEventsTable().getItems().size());
        assertEquals(3, pane.view().vmOperationEventsTable().getItems().size());

        sharedRange.set(new RecordingTimeRange(
                Instant.parse("2026-01-01T00:00:02Z").toEpochMilli(),
                Instant.parse("2026-01-01T00:00:06Z").toEpochMilli()));

        assertEquals(0, pane.view().gcEventsTable().getItems().size());
        assertEquals(2, pane.view().compilationsTable().getItems().size());
        assertEquals(1, pane.view().compilationFailuresTable().getItems().size());
        assertEquals(1, pane.view().codeCacheSweepsTable().getItems().size());
        assertEquals(1, pane.view().classLoadingEventsTable().getItems().size());
        assertEquals(1, pane.view().vmOperationEventsTable().getItems().size());
        assertEquals(2, pane.view().gcReferenceStatsTable().getItems().size(),
                "aggregate/reference tables stay unfiltered until the service can recompute them");
        assertEquals(1, pane.view().codeCacheStatsTable().getItems().size(),
                "state/statistics tables stay unfiltered until their sampling semantics are defined");

        sharedRange.set(null);

        assertEquals(2, pane.view().gcEventsTable().getItems().size());
        assertEquals(3, pane.view().compilationsTable().getItems().size());
        assertEquals(2, pane.view().compilationFailuresTable().getItems().size());
        assertEquals(2, pane.view().codeCacheSweepsTable().getItems().size());
        assertEquals(3, pane.view().classLoadingEventsTable().getItems().size());
        assertEquals(3, pane.view().vmOperationEventsTable().getItems().size());
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.parse("2026-06-05T01:02:03Z"),
                Instant.parse("2026-06-05T01:02:04Z"),
                1000, 1024);
    }
}
