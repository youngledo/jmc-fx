package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.LiveMetricDefinition;
import com.youngledo.jmcfx.domain.model.LiveMetricKind;
import com.youngledo.jmcfx.domain.model.LiveMetricSnapshot;
import com.youngledo.jmcfx.domain.service.JmcFxException;
import com.youngledo.jmcfx.domain.service.LiveMetricService;

public class JmcLiveMetricService implements LiveMetricService {

    private static final ObjectName MEMORY = objectName("java.lang:type=Memory");
    private static final ObjectName THREADING = objectName("java.lang:type=Threading");
    private static final ObjectName CLASS_LOADING = objectName("java.lang:type=ClassLoading");
    private static final ObjectName OPERATING_SYSTEM = objectName("java.lang:type=OperatingSystem");

    private static final List<LiveMetricDefinition> DEFINITIONS = List.of(
            new LiveMetricDefinition(LiveMetricKind.HEAP_USED_PERCENT, "Heap Used", "%", 80.0),
            new LiveMetricDefinition(LiveMetricKind.THREAD_COUNT, "Thread Count", "threads", 200.0),
            new LiveMetricDefinition(LiveMetricKind.LOADED_CLASS_COUNT, "Loaded Classes", "classes", 20000.0),
            new LiveMetricDefinition(LiveMetricKind.PROCESS_CPU_LOAD_PERCENT, "Process CPU", "%", 80.0));

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

            return List.of(
                    new LiveMetricSnapshot(
                            LiveMetricKind.HEAP_USED_PERCENT,
                            heapUsedPercent(server.getAttribute(MEMORY, "HeapMemoryUsage")),
                            "%",
                            observedAt),
                    new LiveMetricSnapshot(
                            LiveMetricKind.THREAD_COUNT,
                            numericValue(server.getAttribute(THREADING, "ThreadCount")),
                            "threads",
                            observedAt),
                    new LiveMetricSnapshot(
                            LiveMetricKind.LOADED_CLASS_COUNT,
                            numericValue(server.getAttribute(CLASS_LOADING, "LoadedClassCount")),
                            "classes",
                            observedAt),
                    new LiveMetricSnapshot(
                            LiveMetricKind.PROCESS_CPU_LOAD_PERCENT,
                            processCpuPercent(server),
                            "%",
                            observedAt));
        } catch (JmcFxException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JmcFxException("Unable to read live JVM metrics: " + errorMessage(exception), exception);
        }
    }

    private static double heapUsedPercent(Object heapMemoryUsage) {
        if (!(heapMemoryUsage instanceof CompositeData data)) {
            return 0.0;
        }
        Object usedValue;
        Object maxValue;
        try {
            usedValue = data.get("used");
            maxValue = data.get("max");
        } catch (IllegalArgumentException exception) {
            return 0.0;
        }
        if (!(usedValue instanceof Number used) || !(maxValue instanceof Number max)) {
            return 0.0;
        }
        long maxBytes = max.longValue();
        if (maxBytes <= 0) {
            return 0.0;
        }
        return used.doubleValue() * 100.0 / maxBytes;
    }

    private static double processCpuPercent(MBeanServerConnection server) {
        Object processCpuLoad;
        try {
            processCpuLoad = server.getAttribute(OPERATING_SYSTEM, "ProcessCpuLoad");
        } catch (Exception exception) {
            return 0.0;
        }
        if (!(processCpuLoad instanceof Number load)) {
            return 0.0;
        }
        double value = load.doubleValue();
        if (value < 0.0 || Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return value * 100.0;
    }

    private static double numericValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
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
