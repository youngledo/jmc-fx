package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import javax.management.MBeanServer;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.LiveMetricDefinition;
import com.youngledo.jmcfx.domain.model.LiveMetricKind;
import com.youngledo.jmcfx.domain.model.LiveMetricSnapshot;

class JmcLiveMetricServiceTest {

    private static final JvmConnection CONNECTION = new JvmConnection("local", "Local", "", true);
    private static final Instant OBSERVED_AT = Instant.parse("2026-05-27T00:00:00Z");

    private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    private final JmcLiveMetricService service = new JmcLiveMetricService(
            connection -> server,
            Clock.fixed(OBSERVED_AT, ZoneOffset.UTC));

    @Test
    void definitionsExposeV1Metrics() {
        List<LiveMetricDefinition> definitions = service.definitions(CONNECTION);

        assertEquals(List.of(
                new LiveMetricDefinition(LiveMetricKind.HEAP_USED_PERCENT, "Heap Used", "%", 80.0),
                new LiveMetricDefinition(LiveMetricKind.THREAD_COUNT, "Thread Count", "threads", 200.0),
                new LiveMetricDefinition(LiveMetricKind.LOADED_CLASS_COUNT, "Loaded Classes", "classes", 20000.0),
                new LiveMetricDefinition(LiveMetricKind.PROCESS_CPU_LOAD_PERCENT, "Process CPU", "%", 80.0)),
                definitions);
    }

    @Test
    void snapshotReturnsNumericMetrics() {
        List<LiveMetricSnapshot> snapshots = service.snapshot(CONNECTION);

        assertEquals(List.of(
                LiveMetricKind.HEAP_USED_PERCENT,
                LiveMetricKind.THREAD_COUNT,
                LiveMetricKind.LOADED_CLASS_COUNT,
                LiveMetricKind.PROCESS_CPU_LOAD_PERCENT),
                snapshots.stream().map(LiveMetricSnapshot::kind).toList());
        assertEquals(List.of("%", "threads", "classes", "%"),
                snapshots.stream().map(LiveMetricSnapshot::unit).toList());
        assertEquals(List.of(OBSERVED_AT, OBSERVED_AT, OBSERVED_AT, OBSERVED_AT),
                snapshots.stream().map(LiveMetricSnapshot::observedAt).toList());
        assertFalse(snapshots.stream().mapToDouble(LiveMetricSnapshot::value).anyMatch(Double::isNaN));
        assertFalse(snapshots.stream().mapToDouble(LiveMetricSnapshot::value).anyMatch(Double::isInfinite));
    }
}
