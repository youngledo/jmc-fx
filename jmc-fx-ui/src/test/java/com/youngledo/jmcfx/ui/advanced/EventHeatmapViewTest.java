package com.youngledo.jmcfx.ui.advanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.domain.model.EventHeatmap;
import com.youngledo.jmcfx.domain.model.EventHeatmapCell;
import com.youngledo.jmcfx.domain.model.EventHeatmapRow;

import org.junit.jupiter.api.Test;

class EventHeatmapViewTest {

    @Test
    void rendersOneRegionPerHeatmapCell() {
        EventHeatmapView view = new EventHeatmapView();
        view.setHeatmap(sampleHeatmap());

        assertEquals(2, view.cellCount());
        assertTrue(view.getStyleClass().contains("event-heatmap-view"));
    }

    @Test
    void cssDefinesHeatmapClasses() throws IOException {
        String css = appCss();

        assertTrue(css.contains(".event-heatmap-view"));
        assertTrue(css.contains(".event-heatmap-cell"));
        assertTrue(css.contains(".event-heatmap-cell-selected"));
    }

    private EventHeatmap sampleHeatmap() {
        return new EventHeatmap(Instant.EPOCH, Instant.EPOCH.plusSeconds(2), 2,
                List.of(new EventHeatmapRow("jdk.CPULoad", "CPU Load", List.of("Operating System"), 4,
                        List.of(
                                new EventHeatmapCell("jdk.CPULoad", Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1),
                                new EventHeatmapCell("jdk.CPULoad", Instant.EPOCH.plusSeconds(1),
                                        Instant.EPOCH.plusSeconds(2), 3)))));
    }

    private String appCss() throws IOException {
        try (InputStream stream = EventHeatmapViewTest.class.getResourceAsStream("/css/app.css")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
