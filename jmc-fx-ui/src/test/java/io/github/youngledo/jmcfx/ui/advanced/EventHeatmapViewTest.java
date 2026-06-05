package io.github.youngledo.jmcfx.ui.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.youngledo.jmcfx.domain.model.EventHeatmap;
import io.github.youngledo.jmcfx.domain.model.EventHeatmapCell;
import io.github.youngledo.jmcfx.domain.model.EventHeatmapRow;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EventHeatmapViewTest {

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
    void rendersOneRegionPerHeatmapCell() {
        EventHeatmapView view = new EventHeatmapView();
        view.setHeatmap(sampleHeatmap());

        assertEquals(2, view.cellCount());
        assertTrue(view.getStyleClass().contains("event-heatmap-view"));
    }

    @Test
    void constrainsRowLabelsSoLongEventNamesDoNotOverflowLeftEdge() {
        EventHeatmapView view = new EventHeatmapView();
        view.setHeatmap(heatmapWithLongEventName());

        Label label = (Label) firstByStyleClass(view, "event-heatmap-row-label");

        assertEquals(OverrunStyle.ELLIPSIS, label.getTextOverrun());
        assertTrue(label.getMaxWidth() <= 380);
        assertTrue(label.getPrefWidth() >= 280);
    }

    @Test
    void cssDefinesHeatmapClasses() throws IOException {
        String css = appCss();

        assertTrue(css.contains(".event-heatmap-view"));
        assertTrue(css.contains(".advanced-jfr-heatmap-scroll"));
        assertTrue(css.contains(".advanced-jfr-heatmap-content"));
        assertTrue(css.contains(".event-heatmap-cell"));
        assertTrue(css.contains(".event-heatmap-cell:hover"));
        assertTrue(css.contains(".event-heatmap-cell-selected"));
        assertTrue(css.contains("-fx-background-radius: 2px"));
        assertTrue(css.contains(".event-heatmap-row-label"));
        assertTrue(css.contains("-fx-text-fill: -color-fg-muted"));
    }

    private EventHeatmap sampleHeatmap() {
        return new EventHeatmap(Instant.EPOCH, Instant.EPOCH.plusSeconds(2), 2,
                List.of(new EventHeatmapRow("jdk.CPULoad", "CPU Load", List.of("Operating System"), 4,
                        List.of(
                                new EventHeatmapCell("jdk.CPULoad", Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1),
                                new EventHeatmapCell("jdk.CPULoad", Instant.EPOCH.plusSeconds(1),
                                        Instant.EPOCH.plusSeconds(2), 3)))));
    }

    private EventHeatmap heatmapWithLongEventName() {
        return new EventHeatmap(Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1,
                List.of(new EventHeatmapRow("jdk.LongEventName",
                        "Very Long JFR Event Type Name That Should Not Overflow The Heatmap Edge",
                        List.of("Custom"), 1,
                        List.of(new EventHeatmapCell("jdk.LongEventName", Instant.EPOCH,
                                Instant.EPOCH.plusSeconds(1), 1)))));
    }

    private Node firstByStyleClass(Node root, String styleClass) {
        if (root.getStyleClass().contains(styleClass)) {
            return root;
        }
        if (root instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node match = firstByStyleClass(child, styleClass);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private String appCss() throws IOException {
        try (InputStream stream = EventHeatmapViewTest.class.getResourceAsStream("/css/app.css")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
