package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
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
                recordingData(17L, "Startup", "RUNNING", 12_000L, 4096L)
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
    void startRecordingCreatesRecordingWithTemplateAndName() {
        RecordingMBeanServer server = new RecordingMBeanServer();
        JmcFlightRecordingService service = new JmcFlightRecordingService(connection -> server.proxy());

        FlightRecordingInfo info = service.startRecording(new FlightRecordingStartRequest(connection(),
                "CPU capture", new FlightRecordingTemplate("profile", "Profile", "")));

        assertEquals(77, info.id());
        assertEquals(List.of("newRecording", "setPredefinedConfiguration", "setRecordingOptions",
                "startRecording", "getRecordings"), server.invocations);
        assertEquals("profile", server.lastPredefinedConfiguration);
        assertEquals("CPU capture", server.lastOptions.get("name"));
    }

    @Test
    void stopAndSaveRecordingCopiesAndClosesRecording() {
        RecordingMBeanServer server = new RecordingMBeanServer();
        JmcFlightRecordingService service = new JmcFlightRecordingService(connection -> server.proxy());
        Path destination = Path.of("target/live-capture.jfr");

        Path saved = service.stopAndSaveRecording(new FlightRecordingStopRequest(connection(), 17, destination));

        assertEquals(destination, saved);
        assertEquals(List.of("stopRecording", "copyTo", "closeRecording"), server.invocations);
        assertEquals(17L, server.lastRecordingId);
        assertEquals(destination.toAbsolutePath().toString(), server.lastCopyDestination);
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

    private static CompositeData recordingData(long id, String name, String state, long duration, long size)
            throws Exception {
        String[] names = { "id", "name", "state", "duration", "size" };
        OpenType<?>[] types = { SimpleType.LONG, SimpleType.STRING, SimpleType.STRING,
                SimpleType.LONG, SimpleType.LONG };
        CompositeType type = new CompositeType("RecordingInfo", "RecordingInfo", names, names, types);
        return new CompositeDataSupport(type, names, new Object[] { id, name, state, duration, size });
    }

    private static final class RecordingMBeanServer {
        private boolean registered = true;
        private Object[] recordings = new Object[0];
        private final List<String> invocations = new ArrayList<>();
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
                        case "invoke" -> invoke((String) args[1], (Object[]) args[2]);
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private Object invoke(String operation, Object[] args) throws Exception {
            invocations.add(operation);
            return switch (operation) {
                case "newRecording" -> 77L;
                case "setPredefinedConfiguration" -> {
                    lastPredefinedConfiguration = (String) args[1];
                    yield null;
                }
                case "setRecordingOptions" -> {
                    @SuppressWarnings("unchecked")
                    Map<String, String> options = (Map<String, String>) args[1];
                    lastOptions = options;
                    yield null;
                }
                case "startRecording" -> null;
                case "getRecordings" -> recordings;
                case "stopRecording" -> {
                    lastRecordingId = (Long) args[0];
                    yield null;
                }
                case "copyTo" -> {
                    lastRecordingId = (Long) args[0];
                    lastCopyDestination = (String) args[1];
                    yield null;
                }
                case "closeRecording" -> {
                    lastRecordingId = (Long) args[0];
                    yield null;
                }
                default -> throw new UnsupportedOperationException(operation);
            };
        }
    }
}
