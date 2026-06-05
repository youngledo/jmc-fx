package io.github.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.model.LiveMetricDefinition;
import io.github.youngledo.jmcfx.domain.model.LiveMetricKind;
import io.github.youngledo.jmcfx.domain.model.LiveMetricSnapshot;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;
import io.github.youngledo.jmcfx.domain.service.LiveMetricService;

public class JmcLiveMetricService implements LiveMetricService {

    private static final ObjectName MEMORY = objectName("java.lang:type=Memory");
    private static final ObjectName THREADING = objectName("java.lang:type=Threading");
    private static final ObjectName CLASS_LOADING = objectName("java.lang:type=ClassLoading");
    private static final ObjectName OPERATING_SYSTEM = objectName("java.lang:type=OperatingSystem");

    private static final List<LiveMetricDefinition> DEFINITIONS = List.of(
            new LiveMetricDefinition(LiveMetricKind.HEAP_USED_PERCENT, "Heap Used", "%", 80.0),
            new LiveMetricDefinition(LiveMetricKind.HEAP_USED_BYTES, "Heap Used", "bytes", 0.0),
            new LiveMetricDefinition(LiveMetricKind.HEAP_COMMITTED_BYTES, "Heap Committed", "bytes", 0.0),
            new LiveMetricDefinition(LiveMetricKind.HEAP_MAX_BYTES, "Heap Max", "bytes", 0.0),
            new LiveMetricDefinition(LiveMetricKind.NON_HEAP_USED_BYTES, "Non-Heap Used", "bytes", 0.0),
            new LiveMetricDefinition(LiveMetricKind.NON_HEAP_COMMITTED_BYTES, "Non-Heap Committed", "bytes", 0.0),
            new LiveMetricDefinition(LiveMetricKind.THREAD_COUNT, "Thread Count", "threads", 200.0),
            new LiveMetricDefinition(LiveMetricKind.PEAK_THREAD_COUNT, "Peak Thread Count", "threads", 200.0),
            new LiveMetricDefinition(LiveMetricKind.DAEMON_THREAD_COUNT, "Daemon Thread Count", "threads", 200.0),
            new LiveMetricDefinition(LiveMetricKind.LOADED_CLASS_COUNT, "Loaded Classes", "classes", 20000.0),
            new LiveMetricDefinition(LiveMetricKind.TOTAL_LOADED_CLASS_COUNT, "Total Loaded Classes", "classes", 20000.0),
            new LiveMetricDefinition(LiveMetricKind.UNLOADED_CLASS_COUNT, "Unloaded Classes", "classes", 20000.0),
            new LiveMetricDefinition(LiveMetricKind.PROCESS_CPU_LOAD_PERCENT, "Process CPU", "%", 80.0),
            new LiveMetricDefinition(LiveMetricKind.SYSTEM_CPU_LOAD_PERCENT, "System CPU", "%", 80.0),
            new LiveMetricDefinition(LiveMetricKind.AVAILABLE_PROCESSORS, "Available Processors", "processors", 0.0),
            new LiveMetricDefinition(LiveMetricKind.SYSTEM_LOAD_AVERAGE, "System Load Average", "load", 0.0));

    private final JmxConnectionAccessor connectionAccessor;
    private final Clock clock;

    public JmcLiveMetricService(JmxConnectionAccessor connectionAccessor) {
        this(connectionAccessor, Clock.systemUTC());
    }

    JmcLiveMetricService(JmxConnectionAccessor connectionAccessor, Clock clock) {
        this.connectionAccessor = Objects.requireNonNull(connectionAccessor, "connectionAccessor");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public List<LiveMetricDefinition> definitions(JvmConnection connection) {
        return DEFINITIONS;
    }

    @Override
    public List<LiveMetricSnapshot> snapshot(JvmConnection connection) {
        try {
            MBeanServerConnection server = server(connection);
            Instant observedAt = clock.instant();
            Object heapMemoryUsage = server.getAttribute(MEMORY, "HeapMemoryUsage");
            Object nonHeapMemoryUsage = server.getAttribute(MEMORY, "NonHeapMemoryUsage");

            return List.of(
                    new LiveMetricSnapshot(
                            LiveMetricKind.HEAP_USED_PERCENT,
                            heapUsedPercent(heapMemoryUsage),
                            "%",
                            observedAt),
                    new LiveMetricSnapshot(
                            LiveMetricKind.HEAP_USED_BYTES,
                            memoryUsageValue(heapMemoryUsage, "used"),
                            "bytes",
                            observedAt),
                    new LiveMetricSnapshot(
                            LiveMetricKind.HEAP_COMMITTED_BYTES,
                            memoryUsageValue(heapMemoryUsage, "committed"),
                            "bytes",
                            observedAt),
                    new LiveMetricSnapshot(
                            LiveMetricKind.HEAP_MAX_BYTES,
                            memoryUsageValue(heapMemoryUsage, "max"),
                            "bytes",
                            observedAt),
                    new LiveMetricSnapshot(
                            LiveMetricKind.NON_HEAP_USED_BYTES,
                            memoryUsageValue(nonHeapMemoryUsage, "used"),
                            "bytes",
                            observedAt),
                    new LiveMetricSnapshot(
                            LiveMetricKind.NON_HEAP_COMMITTED_BYTES,
                            memoryUsageValue(nonHeapMemoryUsage, "committed"),
                            "bytes",
                            observedAt),
                    new LiveMetricSnapshot(
                            LiveMetricKind.THREAD_COUNT,
                            numericValue(server.getAttribute(THREADING, "ThreadCount")),
                            "threads",
                            observedAt),
                    new LiveMetricSnapshot(
                            LiveMetricKind.PEAK_THREAD_COUNT,
                            numericValue(server.getAttribute(THREADING, "PeakThreadCount")),
                            "threads",
                            observedAt),
                    new LiveMetricSnapshot(
                            LiveMetricKind.DAEMON_THREAD_COUNT,
                            numericValue(server.getAttribute(THREADING, "DaemonThreadCount")),
                            "threads",
                            observedAt),
                    new LiveMetricSnapshot(
                            LiveMetricKind.LOADED_CLASS_COUNT,
                            numericValue(server.getAttribute(CLASS_LOADING, "LoadedClassCount")),
                            "classes",
                            observedAt),
                    new LiveMetricSnapshot(
                            LiveMetricKind.TOTAL_LOADED_CLASS_COUNT,
                            numericValue(server.getAttribute(CLASS_LOADING, "TotalLoadedClassCount")),
                            "classes",
                            observedAt),
                    new LiveMetricSnapshot(
                            LiveMetricKind.UNLOADED_CLASS_COUNT,
                            numericValue(server.getAttribute(CLASS_LOADING, "UnloadedClassCount")),
                            "classes",
                            observedAt),
                    new LiveMetricSnapshot(
                            LiveMetricKind.PROCESS_CPU_LOAD_PERCENT,
                            processCpuPercent(server),
                            "%",
                            observedAt),
                    new LiveMetricSnapshot(
                            LiveMetricKind.SYSTEM_CPU_LOAD_PERCENT,
                            percentAttribute(server, "SystemCpuLoad"),
                            "%",
                            observedAt),
                    new LiveMetricSnapshot(
                            LiveMetricKind.AVAILABLE_PROCESSORS,
                            numericValue(server.getAttribute(OPERATING_SYSTEM, "AvailableProcessors")),
                            "processors",
                            observedAt),
                    new LiveMetricSnapshot(
                            LiveMetricKind.SYSTEM_LOAD_AVERAGE,
                            nonNegativeNumericValue(server.getAttribute(OPERATING_SYSTEM, "SystemLoadAverage")),
                            "load",
                            observedAt));
        } catch (JmcFxException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JmcFxException("Unable to read live JVM metrics: " + errorMessage(exception), exception);
        }
    }

    private static double heapUsedPercent(Object heapMemoryUsage) {
        double used = memoryUsageValue(heapMemoryUsage, "used");
        double max = memoryUsageValue(heapMemoryUsage, "max");
        if (max <= 0) {
            return 0.0;
        }
        return used * 100.0 / max;
    }

    private static double memoryUsageValue(Object memoryUsage, String key) {
        if (!(memoryUsage instanceof CompositeData data)) {
            return 0.0;
        }
        Object value;
        try {
            value = data.get(key);
        } catch (IllegalArgumentException exception) {
            return 0.0;
        }
        if (!(value instanceof Number number)) {
            return 0.0;
        }
        return Math.max(0.0, number.doubleValue());
    }

    private static double percentAttribute(MBeanServerConnection server, String attributeName) {
        Object value;
        try {
            value = server.getAttribute(OPERATING_SYSTEM, attributeName);
        } catch (Exception exception) {
            return 0.0;
        }
        if (!(value instanceof Number number)) {
            return 0.0;
        }
        double load = number.doubleValue();
        if (load < 0.0 || Double.isNaN(load) || Double.isInfinite(load)) {
            return 0.0;
        }
        return load * 100.0;
    }

    private static double processCpuPercent(MBeanServerConnection server) {
        return percentAttribute(server, "ProcessCpuLoad");
    }

    private static double numericValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    private static double nonNegativeNumericValue(Object value) {
        double numeric = numericValue(value);
        if (numeric < 0.0 || Double.isNaN(numeric) || Double.isInfinite(numeric)) {
            return 0.0;
        }
        return numeric;
    }

    private MBeanServerConnection server(JvmConnection connection) {
        try {
            return connectionAccessor.mBeanServerConnection(connection);
        } catch (IOException exception) {
            throw new JmcFxException("No live JVM session for connection: " + connection.id(), exception);
        }
    }

    private static ObjectName objectName(String name) {
        try {
            return new ObjectName(name);
        } catch (Exception exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    private static String errorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }
}
