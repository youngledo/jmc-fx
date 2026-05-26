package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.youngledo.jmcfx.domain.model.JvmCapability;
import com.youngledo.jmcfx.domain.model.JvmCapabilitySnapshot;
import com.youngledo.jmcfx.domain.model.JvmCapabilityStatus;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.JvmConnectionSource;
import com.youngledo.jmcfx.domain.model.JvmConnectionState;
import com.youngledo.jmcfx.domain.model.JvmRuntimeSnapshot;
import com.youngledo.jmcfx.domain.model.JvmSessionSnapshot;
import com.youngledo.jmcfx.domain.service.JmcFxException;
import com.youngledo.jmcfx.domain.service.JmxConnectionService;

public class JmcJmxConnectionService implements JmxConnectionService {

    static final String LOCAL_CONNECTOR_ADDRESS_PROPERTY =
            "com.sun.management.jmxremote.localConnectorAddress";
    private static final String FLIGHT_RECORDER_MBEAN = "jdk.management.jfr:type=FlightRecorder";
    private static final String DIAGNOSTIC_COMMAND_MBEAN = "com.sun.management:type=DiagnosticCommand";

    private final Map<String, JMXConnector> connectors = new ConcurrentHashMap<>();
    private final LocalConnectorAddressResolver localConnectorAddressResolver;
    private final JmxConnectorFactory connectorFactory;

    public JmcJmxConnectionService() {
        this(new AttachLocalConnectorAddressResolver(), JMXConnectorFactory::connect);
    }

    JmcJmxConnectionService(LocalConnectorAddressResolver localConnectorAddressResolver,
            JmxConnectorFactory connectorFactory) {
        this.localConnectorAddressResolver = Objects.requireNonNull(localConnectorAddressResolver,
                "localConnectorAddressResolver");
        this.connectorFactory = Objects.requireNonNull(connectorFactory, "connectorFactory");
    }

    @Override
    public JvmConnection connect(String connectionUrl) {
        try {
            JMXServiceURL serviceUrl = new JMXServiceURL(connectionUrl);
            JMXConnector connector = connectorFactory.connect(serviceUrl);
            try {
                String id = UUID.randomUUID().toString();
                JvmConnection connected = new JvmConnection(id, connectionUrl, connectionUrl, true,
                        JvmConnectionSource.MANUAL, JvmConnectionState.CONNECTED, "Connected");
                registerConnector(connected.id(), connector);
                return connected;
            } catch (RuntimeException exception) {
                closeQuietly(connector);
                throw exception;
            }
        } catch (IOException | RuntimeException exception) {
            throw new JmcFxException("Unable to connect to JVM: " + exception.getMessage(), exception);
        }
    }

    @Override
    public JvmConnection connectLocal(JvmConnection localConnection) {
        if (!canAttach(localConnection)) {
            String pid = localConnection == null ? "" : localConnection.pid();
            throw new JmcFxException("Local JVM is not attachable: " + pid);
        }
        try {
            JMXServiceURL serviceUrl = localConnectorAddressResolver.resolve(localConnection.pid());
            JMXConnector connector = connectorFactory.connect(serviceUrl);
            try {
                JvmConnection connected = localConnection.asConnected(serviceUrl.toString());
                registerConnector(connected.id(), connector);
                return connected;
            } catch (RuntimeException exception) {
                closeQuietly(connector);
                throw exception;
            }
        } catch (IOException | RuntimeException exception) {
            throw new JmcFxException("Unable to connect to local JVM " + localConnection.pid()
                    + ": " + exception.getMessage(), exception);
        }
    }

    @Override
    public void disconnect(JvmConnection connection) {
        if (connection == null) {
            return;
        }
        JMXConnector connector = connectors.remove(connection.id());
        if (connector == null) {
            return;
        }
        try {
            connector.close();
        } catch (IOException exception) {
            throw new JmcFxException("Unable to disconnect JVM: " + exception.getMessage(), exception);
        }
    }

    @Override
    public JvmSessionSnapshot sessionSnapshot(JvmConnection connection) {
        String id = connection == null ? "" : connection.id();
        JMXConnector connector = connectors.get(id);
        if (connector == null) {
            throw new JmcFxException("No live JVM session for connection: " + id);
        }
        try {
            MBeanServerConnection server = connector.getMBeanServerConnection();
            return new JvmSessionSnapshot(connection, runtimeSnapshot(server), capabilitySnapshots(server));
        } catch (IOException | RuntimeException exception) {
            throw new JmcFxException("Unable to inspect JVM session " + id + ": "
                    + exception.getMessage(), exception);
        }
    }

    private void registerConnector(String id, JMXConnector connector) {
        JMXConnector previous = connectors.put(id, connector);
        if (previous != null && previous != connector) {
            closeQuietly(previous);
        }
    }

    private static void closeQuietly(JMXConnector connector) {
        try {
            connector.close();
        } catch (IOException exception) {
            // The caller is already handling the primary operation; close failures are best-effort cleanup.
        }
    }

    private static boolean canAttach(JvmConnection connection) {
        return connection != null
                && connection.source() == JvmConnectionSource.LOCAL
                && connection.attachable()
                && !connection.pid().isBlank();
    }

    private static JvmRuntimeSnapshot runtimeSnapshot(MBeanServerConnection server) throws IOException {
        ObjectName runtime = objectName(ManagementFactory.RUNTIME_MXBEAN_NAME);
        return new JvmRuntimeSnapshot(
                attributeAsString(server, runtime, "VmName"),
                attributeAsString(server, runtime, "VmVendor"),
                attributeAsString(server, runtime, "VmVersion"),
                attributeAsString(server, runtime, "SpecVersion"),
                Instant.ofEpochMilli(attributeAsLong(server, runtime, "StartTime")),
                attributeAsLong(server, runtime, "Uptime"));
    }

    private static List<JvmCapabilitySnapshot> capabilitySnapshots(MBeanServerConnection server) throws IOException {
        return List.of(
                available(JvmCapability.MBEAN_SERVER, "Connected to MBean server."),
                registered(server, JvmCapability.RUNTIME_MXBEAN, ManagementFactory.RUNTIME_MXBEAN_NAME),
                registered(server, JvmCapability.MEMORY_MXBEAN, ManagementFactory.MEMORY_MXBEAN_NAME),
                registered(server, JvmCapability.THREAD_MXBEAN, ManagementFactory.THREAD_MXBEAN_NAME),
                registered(server, JvmCapability.FLIGHT_RECORDER, FLIGHT_RECORDER_MBEAN),
                registered(server, JvmCapability.DIAGNOSTIC_COMMANDS, DIAGNOSTIC_COMMAND_MBEAN));
    }

    private static JvmCapabilitySnapshot available(JvmCapability capability, String message) {
        return new JvmCapabilitySnapshot(capability, JvmCapabilityStatus.AVAILABLE, message);
    }

    private static JvmCapabilitySnapshot registered(MBeanServerConnection server, JvmCapability capability,
            String objectName) throws IOException {
        boolean available = server.isRegistered(objectName(objectName));
        return new JvmCapabilitySnapshot(capability,
                available ? JvmCapabilityStatus.AVAILABLE : JvmCapabilityStatus.UNAVAILABLE,
                available ? "MBean is registered." : "MBean is not registered.");
    }

    private static ObjectName objectName(String name) {
        try {
            return new ObjectName(name);
        } catch (javax.management.MalformedObjectNameException exception) {
            throw new IllegalArgumentException("Invalid ObjectName: " + name, exception);
        }
    }

    private static String attributeAsString(MBeanServerConnection server, ObjectName name, String attribute)
            throws IOException {
        try {
            Object value = server.getAttribute(name, attribute);
            return value == null ? "" : value.toString();
        } catch (javax.management.JMException exception) {
            throw new IOException("Unable to read " + attribute, exception);
        }
    }

    private static long attributeAsLong(MBeanServerConnection server, ObjectName name, String attribute)
            throws IOException {
        String value = attributeAsString(server, name, attribute);
        if (value.isBlank()) {
            return 0;
        }
        return Long.parseLong(value);
    }

    interface LocalConnectorAddressResolver {
        JMXServiceURL resolve(String pid) throws IOException;
    }

    interface JmxConnectorFactory {
        JMXConnector connect(JMXServiceURL serviceUrl) throws IOException;
    }

    interface AttachVirtualMachineFactory {
        AttachedVirtualMachine attach(String pid) throws IOException;
    }

    interface AttachedVirtualMachine {
        String localConnectorAddress() throws IOException;

        String startLocalManagementAgent(String pid) throws IOException;

        void detach(String pid) throws IOException;
    }

    static final class AttachLocalConnectorAddressResolver implements LocalConnectorAddressResolver {
        private final AttachVirtualMachineFactory virtualMachineFactory;

        AttachLocalConnectorAddressResolver() {
            this(AttachVirtualMachine::attach);
        }

        AttachLocalConnectorAddressResolver(AttachVirtualMachineFactory virtualMachineFactory) {
            this.virtualMachineFactory = Objects.requireNonNull(virtualMachineFactory, "virtualMachineFactory");
        }

        @Override
        public JMXServiceURL resolve(String pid) throws IOException {
            AttachedVirtualMachine vm = virtualMachineFactory.attach(pid);
            try {
                String address = vm.localConnectorAddress();
                if (address == null || address.isBlank()) {
                    address = vm.startLocalManagementAgent(pid);
                }
                return new JMXServiceURL(address);
            } finally {
                vm.detach(pid);
            }
        }
    }

    private static final class AttachVirtualMachine implements AttachedVirtualMachine {
        private final VirtualMachine delegate;

        private AttachVirtualMachine(VirtualMachine delegate) {
            this.delegate = delegate;
        }

        private static AttachedVirtualMachine attach(String pid) throws IOException {
            try {
                return new AttachVirtualMachine(VirtualMachine.attach(pid));
            } catch (AttachNotSupportedException exception) {
                throw new IOException("Attach failed for PID " + pid, exception);
            }
        }

        @Override
        public String localConnectorAddress() throws IOException {
            Properties agentProperties = delegate.getAgentProperties();
            return agentProperties.getProperty(LOCAL_CONNECTOR_ADDRESS_PROPERTY, "");
        }

        @Override
        public String startLocalManagementAgent(String pid) throws IOException {
            return delegate.startLocalManagementAgent();
        }

        @Override
        public void detach(String pid) throws IOException {
            delegate.detach();
        }
    }
}
