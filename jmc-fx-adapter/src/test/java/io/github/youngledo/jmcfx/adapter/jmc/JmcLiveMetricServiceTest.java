package io.github.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.SimpleType;

import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.model.LiveMetricDefinition;
import io.github.youngledo.jmcfx.domain.model.LiveMetricKind;
import io.github.youngledo.jmcfx.domain.model.LiveMetricSnapshot;

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
                new LiveMetricDefinition(LiveMetricKind.SYSTEM_LOAD_AVERAGE, "System Load Average", "load", 0.0)),
                definitions);
    }

    @Test
    void snapshotConvertsMBeanAttributesToNumericMetrics() throws Exception {
        MBeanServer controlledServer = MBeanServerFactory.newMBeanServer();
        controlledServer.registerMBean(
                new AttributesMBean(Map.of(
                        "HeapMemoryUsage", memoryUsage(0L, 25L, 75L, 100L),
                        "NonHeapMemoryUsage", memoryUsage(0L, 12L, 24L, -1L))),
                objectName("java.lang:type=Memory"));
        controlledServer.registerMBean(
                new AttributesMBean(Map.of(
                        "ThreadCount", 7,
                        "PeakThreadCount", 11,
                        "DaemonThreadCount", 3)),
                objectName("java.lang:type=Threading"));
        controlledServer.registerMBean(
                new AttributesMBean(Map.of(
                        "LoadedClassCount", 42,
                        "TotalLoadedClassCount", 50L,
                        "UnloadedClassCount", 8L)),
                objectName("java.lang:type=ClassLoading"));
        controlledServer.registerMBean(
                new AttributesMBean(Map.of(
                        "ProcessCpuLoad", 0.5,
                        "SystemCpuLoad", 0.75,
                        "AvailableProcessors", 8,
                        "SystemLoadAverage", 2.25)),
                objectName("java.lang:type=OperatingSystem"));

        JmcLiveMetricService controlledService = new JmcLiveMetricService(
                connection -> controlledServer,
                Clock.fixed(OBSERVED_AT, ZoneOffset.UTC));

        List<LiveMetricSnapshot> snapshots = controlledService.snapshot(CONNECTION);

        assertEquals(List.of(
                LiveMetricKind.HEAP_USED_PERCENT,
                LiveMetricKind.HEAP_USED_BYTES,
                LiveMetricKind.HEAP_COMMITTED_BYTES,
                LiveMetricKind.HEAP_MAX_BYTES,
                LiveMetricKind.NON_HEAP_USED_BYTES,
                LiveMetricKind.NON_HEAP_COMMITTED_BYTES,
                LiveMetricKind.THREAD_COUNT,
                LiveMetricKind.PEAK_THREAD_COUNT,
                LiveMetricKind.DAEMON_THREAD_COUNT,
                LiveMetricKind.LOADED_CLASS_COUNT,
                LiveMetricKind.TOTAL_LOADED_CLASS_COUNT,
                LiveMetricKind.UNLOADED_CLASS_COUNT,
                LiveMetricKind.PROCESS_CPU_LOAD_PERCENT,
                LiveMetricKind.SYSTEM_CPU_LOAD_PERCENT,
                LiveMetricKind.AVAILABLE_PROCESSORS,
                LiveMetricKind.SYSTEM_LOAD_AVERAGE),
                snapshots.stream().map(LiveMetricSnapshot::kind).toList());
        assertEquals(List.of("%", "bytes", "bytes", "bytes", "bytes", "bytes",
                        "threads", "threads", "threads", "classes", "classes", "classes",
                        "%", "%", "processors", "load"),
                snapshots.stream().map(LiveMetricSnapshot::unit).toList());
        assertEquals(16, snapshots.stream()
                .filter(snapshot -> OBSERVED_AT.equals(snapshot.observedAt()))
                .count());
        assertEquals(List.of(25.0, 25.0, 75.0, 100.0, 12.0, 24.0,
                        7.0, 11.0, 3.0, 42.0, 50.0, 8.0, 50.0, 75.0, 8.0, 2.25),
                snapshots.stream().map(LiveMetricSnapshot::value).toList());
    }

    @Test
    void platformSnapshotReturnsFiniteNumericMetrics() {
        List<LiveMetricSnapshot> snapshots = service.snapshot(CONNECTION);

        assertFalse(snapshots.stream().mapToDouble(LiveMetricSnapshot::value).anyMatch(Double::isNaN));
        assertFalse(snapshots.stream().mapToDouble(LiveMetricSnapshot::value).anyMatch(Double::isInfinite));
    }

    private static CompositeData memoryUsage(long init, long used, long committed, long max) throws OpenDataException {
        CompositeType type = new CompositeType(
                "MemoryUsage",
                "Memory usage",
                new String[] { "init", "used", "committed", "max" },
                new String[] { "init", "used", "committed", "max" },
                new SimpleType<?>[] { SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG });
        return new CompositeDataSupport(type, Map.of(
                "init", init,
                "used", used,
                "committed", committed,
                "max", max));
    }

    private static ObjectName objectName(String name) throws Exception {
        return new ObjectName(name);
    }

    private static final class AttributesMBean implements DynamicMBean {
        private final Map<String, Object> attributes;

        private AttributesMBean(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        @Override
        public Object getAttribute(String attribute) throws AttributeNotFoundException {
            if (!attributes.containsKey(attribute)) {
                throw new AttributeNotFoundException(attribute);
            }
            return attributes.get(attribute);
        }

        @Override
        public void setAttribute(Attribute attribute)
                throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
            throw new AttributeNotFoundException(attribute == null ? null : attribute.getName());
        }

        @Override
        public AttributeList getAttributes(String[] names) {
            AttributeList list = new AttributeList();
            for (String name : names) {
                if (attributes.containsKey(name)) {
                    list.add(new Attribute(name, attributes.get(name)));
                }
            }
            return list;
        }

        @Override
        public AttributeList setAttributes(AttributeList attributes) {
            return new AttributeList();
        }

        @Override
        public Object invoke(String actionName, Object[] params, String[] signature)
                throws MBeanException, ReflectionException {
            throw new ReflectionException(new NoSuchMethodException(actionName));
        }

        @Override
        public MBeanInfo getMBeanInfo() {
            MBeanAttributeInfo[] infos = attributes.entrySet().stream()
                    .map(entry -> new MBeanAttributeInfo(
                            entry.getKey(),
                            attributeType(entry.getValue()),
                            entry.getKey(),
                            true,
                            false,
                            false))
                    .toArray(MBeanAttributeInfo[]::new);
            return new MBeanInfo(getClass().getName(), "Test attributes", infos, null, null, null);
        }

        private static String attributeType(Object value) {
            if (value instanceof CompositeData) {
                return CompositeData.class.getName();
            }
            return value.getClass().getName();
        }
    }
}
