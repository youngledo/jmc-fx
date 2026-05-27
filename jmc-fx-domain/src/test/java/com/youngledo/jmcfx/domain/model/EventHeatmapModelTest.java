package com.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class EventHeatmapModelTest {

    @Test
    void rowNormalizesLabelsAndCopiesCells() {
        EventHeatmapCell cell = new EventHeatmapCell("jdk.CPULoad", Instant.EPOCH,
                Instant.EPOCH.plusSeconds(1), 4);
        EventHeatmapRow row = new EventHeatmapRow("jdk.CPULoad", null, List.of("Operating System"),
                4, List.of(cell));

        assertEquals("jdk.CPULoad", row.eventTypeId());
        assertEquals("jdk.CPULoad", row.label());
        assertEquals(List.of("Operating System"), row.categoryPath());
        assertEquals(4, row.totalCount());
        assertThrows(UnsupportedOperationException.class, () -> row.cells().add(cell));
    }

    @Test
    void heatmapComputesMaxCountAndCopiesRows() {
        EventHeatmapCell low = new EventHeatmapCell("jdk.CPULoad", Instant.EPOCH,
                Instant.EPOCH.plusSeconds(1), 2);
        EventHeatmapCell high = new EventHeatmapCell("jdk.ThreadSleep", Instant.EPOCH,
                Instant.EPOCH.plusSeconds(1), 8);
        EventHeatmap heatmap = new EventHeatmap(Instant.EPOCH, Instant.EPOCH.plusSeconds(10),
                5, List.of(
                        new EventHeatmapRow("jdk.CPULoad", "CPU Load", List.of("Operating System"), 2, List.of(low)),
                        new EventHeatmapRow("jdk.ThreadSleep", "Thread Sleep", List.of("Java Application"), 8,
                                List.of(high))));

        assertEquals(8, heatmap.maxCellCount());
        assertEquals(5, heatmap.bucketCount());
        assertThrows(UnsupportedOperationException.class, () -> heatmap.rows().clear());
    }

    @Test
    void cellRejectsNegativeCount() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventHeatmapCell("jdk.CPULoad", Instant.EPOCH, Instant.EPOCH.plusSeconds(1), -1));
    }
}
