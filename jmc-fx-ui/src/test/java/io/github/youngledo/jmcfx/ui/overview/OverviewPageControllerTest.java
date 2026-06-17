package io.github.youngledo.jmcfx.ui.overview;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.domain.model.JvmConnectionSource;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.jvms.LiveFlightRecordingOrigin;

import javafx.application.Platform;
import javafx.scene.layout.VBox;

class OverviewPageControllerTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                latch.countDown();
            });
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (IllegalStateException alreadyStarted) {
            Platform.setImplicitExit(false);
        }
    }

    @Test
    void hidesLiveOriginSummaryUntilRecordingComesFromLiveJvm() {
        OverviewPaneView pane = new OverviewPaneView(new VBox());
        OverviewPageController controller = new OverviewPageController(pane.view(), new I18n(Locale.ENGLISH));

        controller.configure();

        assertFalse(pane.view().liveOriginPane().isVisible());
        assertFalse(pane.view().liveOriginPane().isManaged());
    }

    @Test
    void formatsLiveOriginDetailsForVisibleOverviewSummary() {
        OverviewPaneView pane = new OverviewPaneView(new VBox());
        OverviewPageController controller = new OverviewPageController(pane.view(), new I18n(Locale.ENGLISH));
        LiveFlightRecordingOrigin origin = new LiveFlightRecordingOrigin("42", "demo.Main",
                "service:jmx:local://42", JvmConnectionSource.LOCAL, "42", "26.0.1", 100, "jmcfx-42");

        String details = controller.formatLiveOriginDetails(origin);

        assertTrue(details.contains("JVM: demo.Main"));
        assertTrue(details.contains("Source: Local"));
        assertTrue(details.contains("Connection: service:jmx:local://42"));
        assertTrue(details.contains("PID: 42"));
        assertTrue(details.contains("Java: 26.0.1"));
        assertTrue(details.contains("Recording: jmcfx-42 (id 100)"));
    }

    @Test
    void showsLiveOriginSummaryWhenBoundViewModelHasOrigin() {
        OverviewPaneView pane = new OverviewPaneView(new VBox());
        OverviewPageController controller = new OverviewPageController(pane.view(), new I18n(Locale.ENGLISH));
        RecordingSummary recording = new RecordingSummary("rec", Path.of("rec.jfr"), "rec.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);
        LiveFlightRecordingOrigin origin = new LiveFlightRecordingOrigin("42", "demo.Main",
                "service:jmx:local://42", JvmConnectionSource.LOCAL, "42", "26.0.1", 100, "jmcfx-42");
        OverviewViewModel viewModel = new OverviewViewModel();
        viewModel.showRecording(recording, "details", origin, controller.formatLiveOriginDetails(origin));

        controller.configure();
        controller.bind(viewModel);

        assertTrue(pane.view().liveOriginPane().isVisible());
        assertTrue(pane.view().liveOriginPane().isManaged());
        assertTrue(pane.view().liveOriginDetailsLabel().getText().contains("demo.Main"));
        assertTrue(pane.view().liveOriginDetailsLabel().getText().contains("service:jmx:local://42"));
    }
}
