package io.github.youngledo.jmcfx.ui.fileio;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.youngledo.jmcfx.application.LoadFileIOUseCase;

import io.github.youngledo.jmcfx.domain.model.FileIOHistogram;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;
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

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.parse("2026-06-05T01:02:03Z"),
                Instant.parse("2026-06-05T01:02:04Z"),
                1000, 1024);
    }
}
