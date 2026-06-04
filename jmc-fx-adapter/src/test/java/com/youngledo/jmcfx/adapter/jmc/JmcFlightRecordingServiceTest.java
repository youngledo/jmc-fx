package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.FlightRecordingInfo;
import com.youngledo.jmcfx.domain.model.FlightRecordingStartRequest;
import com.youngledo.jmcfx.domain.model.FlightRecordingState;
import com.youngledo.jmcfx.domain.model.FlightRecordingStopRequest;
import com.youngledo.jmcfx.domain.model.FlightRecordingTemplate;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.service.JmcFxException;

class JmcFlightRecordingServiceTest {

    @Test
    void recordingControlIsAvailableWhenFlightRecorderMBeanIsRegistered() {
        RecordingMBeanServer server = new RecordingMBeanServer();
        JmcFlightRecordingService service = new JmcFlightRecordingService(connection -> server.proxy());

        assertTrue(service.isRecordingControlAvailable(connection()));

        server.registered = false;
        assertFalse(service.isRecordingControlAvailable(connection()));
    }

    @Test
    void recordingsMapsCompositeDataRows() throws Exception {
        RecordingMBeanServer server = new RecordingMBeanServer();
        server.recordings = new Object[] {
                recordingData(17L, "Startup", "RUNNING", 12_000L, 4096L, 0L, 0L)
        };
        JmcFlightRecordingService service = new JmcFlightRecordingService(connection -> server.proxy());

        List<FlightRecordingInfo> recordings = service.recordings(connection());

        assertEquals(1, recordings.size());
        assertEquals(17, recordings.getFirst().id());
        assertEquals("Startup", recordings.getFirst().name());
        assertEquals(FlightRecordingState.RUNNING, recordings.getFirst().state());
        assertEquals(12_000, recordings.getFirst().durationMillis());
        assertEquals(4096, recordings.getFirst().sizeBytes());
    }

    @Test
    void runningRecordingUsesElapsedTimeWhenDurationAttributeIsZero() throws Exception {
        RecordingMBeanServer server = new RecordingMBeanServer();
        server.recordings = new Object[] {
                recordingData(17L, "Startup", "RUNNING", 0L, 4096L, 1_700_000_000_000L, 0L)
        };
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1_700_000_012_345L), ZoneOffset.UTC);
        JmcFlightRecordingService service = new JmcFlightRecordingService(connection -> server.proxy(), clock);

        List<FlightRecordingInfo> recordings = service.recordings(connection());

        assertEquals(12_345, recordings.getFirst().durationMillis());
    }

    @Test
    void startRecordingCreatesRecordingWithTemplateAndName() {
        RecordingMBeanServer server = new RecordingMBeanServer();
        JmcFlightRecordingService service = new JmcFlightRecordingService(connection -> server.proxy());

        FlightRecordingInfo info = service.startRecording(new FlightRecordingStartRequest(connection(),
                "CPU capture", new FlightRecordingTemplate("profile", "Profile", "")));

        assertEquals(77, info.id());
        assertEquals(List.of("newRecording", "setPredefinedConfiguration", "setRecordingOptions",
                "startRecording"), server.invocations);
        assertEquals(List.of("Recordings"), server.attributes);
        assertEquals("profile", server.lastPredefinedConfiguration);
        assertEquals("CPU capture", server.lastOptions.get("name"));
    }

    @Test
    void stopAndSaveRecordingCopiesAndClosesRecording() throws Exception {
        RecordingMBeanServer server = new RecordingMBeanServer();
        server.recordings = new Object[] {
                recordingData(17L, "Startup", "RUNNING", 12_000L, 4096L, 0L, 0L)
        };
        JmcFlightRecordingService service = new JmcFlightRecordingService(connection -> server.proxy());
        Path destination = Path.of("target/live-capture.jfr");

        Path saved = service.stopAndSaveRecording(new FlightRecordingStopRequest(connection(), 17, destination));

        assertEquals(destination, saved);
        assertEquals(List.of("stopRecording", "copyTo", "closeRecording"), server.invocations);
        assertEquals(List.of("Recordings"), server.attributes);
        assertEquals(17L, server.lastRecordingId);
        assertEquals(destination.toAbsolutePath().toString(), server.lastCopyDestination);
    }

    @Test
    void stopAndSaveRecordingCopiesAndClosesStoppedRecordingWithoutStoppingAgain() throws Exception {
        RecordingMBeanServer server = new RecordingMBeanServer();
        server.recordings = new Object[] {
                recordingData(17L, "Startup", "STOPPED", 12_000L, 4096L, 0L, 0L)
        };
        JmcFlightRecordingService service = new JmcFlightRecordingService(connection -> server.proxy());
        Path destination = Path.of("target/live-capture.jfr");

        Path saved = service.stopAndSaveRecording(new FlightRecordingStopRequest(connection(), 17, destination));

        assertEquals(destination, saved);
        assertEquals(List.of("copyTo", "closeRecording"), server.invocations);
        assertEquals(List.of("Recordings"), server.attributes);
        assertEquals(17L, server.lastRecordingId);
        assertEquals(destination.toAbsolutePath().toString(), server.lastCopyDestination);
    }

    @Test
    void stopAndDiscardRecordingStopsAndClosesWithoutCopying() throws Exception {
        RecordingMBeanServer server = new RecordingMBeanServer();
        server.recordings = new Object[] {
                recordingData(17L, "Startup", "RUNNING", 12_000L, 4096L, 0L, 0L)
        };
        JmcFlightRecordingService service = new JmcFlightRecordingService(connection -> server.proxy());

        service.stopAndDiscardRecording(connection(), 17);

        assertEquals(List.of("stopRecording", "closeRecording"), server.invocations);
        assertEquals(List.of("Recordings"), server.attributes);
        assertEquals(17L, server.lastRecordingId);
        assertEquals("", server.lastCopyDestination);
    }


    @Test
    void missingLiveSessionThrowsDomainException() {
        JmcFlightRecordingService service = new JmcFlightRecordingService(connection -> {
            throw new JmcFxException("No live JVM session for connection: 42");
        });

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.recordings(connection()));

        assertEquals("No live JVM session for connection: 42", exception.getMessage());
    }

    private static JvmConnection connection() {
        return JvmConnection.local("42", "demo.Main", "26.0.1", true)
                .asConnected("service:jmx:local://42");
    }

    private static CompositeData recordingData(long id, String name, String state, long duration, long size,
            long startTime, long stopTime) throws Exception {
        String[] names = { "id", "name", "state", "duration", "size", "startTime", "stopTime" };
        OpenType<?>[] types = { SimpleType.LONG, SimpleType.STRING, SimpleType.STRING,
                SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG };
        CompositeType type = new CompositeType("RecordingInfo", "RecordingInfo", names, names, types);
        return new CompositeDataSupport(type, names, new Object[] { id, name, state, duration, size,
                startTime, stopTime });
    }

    private static final class RecordingMBeanServer {
        private boolean registered = true;
        private Object[] recordings = new Object[0];
        private final List<String> invocations = new ArrayList<>();
        private final List<String> attributes = new ArrayList<>();
        private String lastPredefinedConfiguration = "";
        private Map<String, String> lastOptions = Map.of();
        private long lastRecordingId;
        private String lastCopyDestination = "";

        private MBeanServerConnection proxy() {
            return (MBeanServerConnection) java.lang.reflect.Proxy.newProxyInstance(
                    MBeanServerConnection.class.getClassLoader(),
                    new Class<?>[] { MBeanServerConnection.class },
                    (proxy, method, args) -> switch (method.getName()) {
                        case "isRegistered" -> registered;
                        case "getAttribute" -> attribute((String) args[1]);
                        case "invoke" -> invoke((String) args[1], (Object[]) args[2], (String[]) args[3]);
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private Object attribute(String name) {
            attributes.add(name);
            return switch (name) {
                case "Recordings" -> recordings;
                default -> throw new UnsupportedOperationException(name);
            };
        }

        private Object invoke(String operation, Object[] args, String[] signature) throws Exception {
            invocations.add(operation);
            return switch (operation) {
                case "newRecording" -> 77L;
                case "setPredefinedConfiguration" -> {
                    assertEquals(List.of("long", "java.lang.String"), List.of(signature));
                    lastPredefinedConfiguration = (String) args[1];
                    yield null;
                }
                case "setRecordingOptions" -> {
                    assertEquals(List.of("long", "javax.management.openmbean.TabularData"), List.of(signature));
                    lastOptions = options((TabularData) args[1]);
                    yield null;
                }
                case "startRecording" -> {
                    assertEquals(List.of("long"), List.of(signature));
                    yield null;
                }
                case "stopRecording" -> {
                    assertEquals(List.of("long"), List.of(signature));
                    lastRecordingId = (Long) args[0];
                    yield null;
                }
                case "copyTo" -> {
                    assertEquals(List.of("long", "java.lang.String"), List.of(signature));
                    lastRecordingId = (Long) args[0];
                    lastCopyDestination = (String) args[1];
                    yield null;
                }
                case "closeRecording" -> {
                    assertEquals(List.of("long"), List.of(signature));
                    lastRecordingId = (Long) args[0];
                    yield null;
                }
                default -> throw new UnsupportedOperationException(operation);
            };
        }

        private static Map<String, String> options(TabularData data) {
            return data.values().stream()
                    .map(CompositeData.class::cast)
                    .collect(java.util.stream.Collectors.toMap(
                            row -> row.get("key").toString(),
                            row -> row.get("value").toString()));
        }
    }
}
