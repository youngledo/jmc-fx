package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.youngledo.jmcfx.domain.model.FlightRecordingInfo;
import com.youngledo.jmcfx.domain.model.FlightRecordingStartRequest;
import com.youngledo.jmcfx.domain.model.FlightRecordingState;
import com.youngledo.jmcfx.domain.model.FlightRecordingStopRequest;
import com.youngledo.jmcfx.domain.model.FlightRecordingTemplate;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.service.FlightRecordingService;
import com.youngledo.jmcfx.domain.service.JmcFxException;

/// JMX-backed flight recording control boundary.
///
/// The implementation deliberately uses dynamic MBean calls so the rest of the
/// application does not require compile-time access to jdk.management.jfr APIs.
public class JmcFlightRecordingService implements FlightRecordingService {

    private static final ObjectName FLIGHT_RECORDER = objectName("jdk.management.jfr:type=FlightRecorder");
    private static final Logger LOGGER = LoggerFactory.getLogger(JmcFlightRecordingService.class);

    private final JmxConnectionAccessor connectionAccessor;
    private final Clock clock;

    public JmcFlightRecordingService(JmxConnectionAccessor connectionAccessor) {
        this(connectionAccessor, Clock.systemUTC());
    }

    JmcFlightRecordingService(JmxConnectionAccessor connectionAccessor, Clock clock) {
        this.connectionAccessor = Objects.requireNonNull(connectionAccessor, "connectionAccessor");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public boolean isRecordingControlAvailable(JvmConnection connection) {
        try {
            return server(connection).isRegistered(FLIGHT_RECORDER);
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    @Override
    public List<FlightRecordingTemplate> templates(JvmConnection connection) {
        return FlightRecordingTemplate.predefined();
    }

    @Override
    public List<FlightRecordingInfo> recordings(JvmConnection connection) {
        MBeanServerConnection server = server(connection);
        Object value = attribute(server, "Recordings");
        if (value instanceof Object[] array) {
            return Arrays.stream(array)
                    .map(this::recordingInfo)
                    .toList();
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(this::recordingInfo)
                    .toList();
        }
        return List.of();
    }

    @Override
    public FlightRecordingInfo startRecording(FlightRecordingStartRequest request) {
        MBeanServerConnection server = server(request.connection());
        long id = ((Number) invoke(server, "newRecording", new Object[0], new String[0])).longValue();
        invoke(server, "setPredefinedConfiguration",
                new Object[] { id, request.template().name() },
                new String[] { "long", "java.lang.String" });
        invoke(server, "setRecordingOptions",
                new Object[] { id, optionTable("name", request.name()) },
                new String[] { "long", "javax.management.openmbean.TabularData" });
        invoke(server, "startRecording", new Object[] { id }, new String[] { "long" });
        return recordings(request.connection()).stream()
                .filter(recording -> recording.id() == id)
                .findFirst()
                .orElse(new FlightRecordingInfo(id, request.name(), FlightRecordingState.RUNNING, 0, 0));
    }

    @Override
    public Path stopAndSaveRecording(FlightRecordingStopRequest request) {
        MBeanServerConnection server = server(request.connection());
        long id = request.recordingId();
        Path destination = request.destinationFile().toAbsolutePath();
        if (recordingState(request.connection(), id) == FlightRecordingState.RUNNING) {
            invoke(server, "stopRecording", new Object[] { id }, new String[] { "long" });
        }
        invoke(server, "copyTo", new Object[] { id, destination.toString() },
                new String[] { "long", "java.lang.String" });
        invoke(server, "closeRecording", new Object[] { id }, new String[] { "long" });
        return request.destinationFile();
    }

    @Override
    public void stopAndDiscardRecording(JvmConnection connection, long recordingId) {
        MBeanServerConnection server = server(connection);
        if (recordingState(connection, recordingId) == FlightRecordingState.RUNNING) {
            invoke(server, "stopRecording", new Object[] { recordingId }, new String[] { "long" });
        }
        invoke(server, "closeRecording", new Object[] { recordingId }, new String[] { "long" });
    }

    private FlightRecordingState recordingState(JvmConnection connection, long recordingId) {
        return recordings(connection).stream()
                .filter(recording -> recording.id() == recordingId)
                .findFirst()
                .map(FlightRecordingInfo::state)
                .orElse(FlightRecordingState.UNKNOWN);
    }

    private MBeanServerConnection server(JvmConnection connection) {
        try {
            return connectionAccessor.mBeanServerConnection(connection);
        } catch (JmcFxException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new JmcFxException("Unable to access live JVM session: " + exception.getMessage(), exception);
        }
    }

    private static Object invoke(MBeanServerConnection server, String operation, Object[] params, String[] signature) {
        try {
            return server.invoke(FLIGHT_RECORDER, operation, params, signature);
        } catch (JMException | IOException | RuntimeException exception) {
            LOGGER.error("Unable to invoke Flight Recorder operation {} with signature {}",
                    operation, Arrays.toString(signature), exception);
            throw new JmcFxException("Unable to invoke Flight Recorder operation " + operation
                    + ": " + exception.getMessage(), exception);
        }
    }

    private static Object attribute(MBeanServerConnection server, String attribute) {
        try {
            return server.getAttribute(FLIGHT_RECORDER, attribute);
        } catch (JMException | IOException | RuntimeException exception) {
            LOGGER.error("Unable to read Flight Recorder attribute {}", attribute, exception);
            throw new JmcFxException("Unable to read Flight Recorder attribute " + attribute
                    + ": " + exception.getMessage(), exception);
        }
    }

    private static TabularData optionTable(String key, String value) {
        try {
            String[] names = { "key", "value" };
            OpenType<?>[] types = { SimpleType.STRING, SimpleType.STRING };
            CompositeType rowType = new CompositeType("java.util.Map<java.lang.String, java.lang.String>",
                    "Flight Recorder recording option", names, names, types);
            TabularType tableType = new TabularType("java.util.Map<java.lang.String, java.lang.String>",
                    "Flight Recorder recording options", rowType, new String[] { "key" });
            TabularDataSupport table = new TabularDataSupport(tableType);
            table.put(new CompositeDataSupport(rowType, names, new Object[] { key, value }));
            return table;
        } catch (OpenDataException exception) {
            throw new JmcFxException("Unable to create Flight Recorder options: " + exception.getMessage(), exception);
        }
    }

    private FlightRecordingInfo recordingInfo(Object value) {
        if (value instanceof CompositeData data) {
            return new FlightRecordingInfo(
                    longValue(data, "id"),
                    stringValue(data, "name"),
                    stateValue(stringValue(data, "state")),
                    durationMillis(data),
                    longValue(data, "size"));
        }
        throw new JmcFxException("Unsupported Flight Recorder recording row: " + value);
    }

    private long durationMillis(CompositeData data) {
        return durationMillis(data, clock);
    }

    private static long durationMillis(CompositeData data, Clock clock) {
        long duration = longValue(data, "duration");
        if (duration > 0 || stateValue(stringValue(data, "state")) != FlightRecordingState.RUNNING) {
            return duration;
        }
        long startTime = longValue(data, "startTime");
        if (startTime <= 0) {
            return 0;
        }
        return Math.max(0, clock.millis() - startTime);
    }

    private static String stringValue(CompositeData data, String key) {
        Object value = data.get(key);
        return value == null ? "" : value.toString();
    }

    private static long longValue(CompositeData data, String key) {
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || value.toString().isBlank()) {
            return 0;
        }
        return Long.parseLong(value.toString());
    }

    private static FlightRecordingState stateValue(String value) {
        try {
            return FlightRecordingState.valueOf(value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return FlightRecordingState.UNKNOWN;
        }
    }

    private static ObjectName objectName(String name) {
        try {
            return new ObjectName(name);
        } catch (MalformedObjectNameException exception) {
            throw new IllegalArgumentException("Invalid ObjectName: " + name, exception);
        }
    }
}
