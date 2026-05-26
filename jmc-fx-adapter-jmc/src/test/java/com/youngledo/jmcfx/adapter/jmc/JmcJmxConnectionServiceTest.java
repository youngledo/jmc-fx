package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.JvmCapability;
import com.youngledo.jmcfx.domain.model.JvmCapabilityStatus;
import com.youngledo.jmcfx.domain.model.JvmConnectionSource;
import com.youngledo.jmcfx.domain.model.JvmSessionSnapshot;
import com.youngledo.jmcfx.domain.service.JmcFxException;

class JmcJmxConnectionServiceTest {

    @Test
    void rejectsMalformedJmxUrl() {
        JmcJmxConnectionService service = new JmcJmxConnectionService();

        assertThrows(JmcFxException.class, () -> service.connect("not-a-jmx-url"));
    }

    @Test
    void disconnectIgnoresUnknownConnections() {
        JmcJmxConnectionService service = new JmcJmxConnectionService();

        assertDoesNotThrow(() -> service.disconnect(new JvmConnection("missing", "Missing", "", false)));
    }

    @Test
    void rejectsLocalConnectionThatIsNotAttachable() {
        JmcJmxConnectionService service = new JmcJmxConnectionService();

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.connectLocal(JvmConnection.local("42", "blocked.Main", "", false)));

        assertEquals("Local JVM is not attachable: 42", exception.getMessage());
    }

    @Test
    void rejectsManualConnectionForLocalAttach() {
        JmcJmxConnectionService service = new JmcJmxConnectionService();

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.connectLocal(new JvmConnection("manual", "Manual", "", false)));

        assertEquals("Local JVM is not attachable: ", exception.getMessage());
    }

    @Test
    void rejectsLocalConnectionWithoutPid() {
        JmcJmxConnectionService service = new JmcJmxConnectionService();

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.connectLocal(new JvmConnection("id", "No PID", "", false,
                        JvmConnectionSource.LOCAL, null, "", "", "", true)));

        assertEquals("Local JVM is not attachable: ", exception.getMessage());
    }

    @Test
    void connectsAttachableLocalConnectionThroughResolverAndConnectorFactory() {
        RecordingLocalConnectorResolver resolver = new RecordingLocalConnectorResolver("service:jmx:local://42");
        RecordingJmxConnectorFactory connectorFactory = new RecordingJmxConnectorFactory();
        JmcJmxConnectionService service = new JmcJmxConnectionService(resolver, connectorFactory);

        JvmConnection connected = service.connectLocal(JvmConnection.local("42", "demo.Main", "26.0.1", true));

        assertEquals("42", resolver.resolvedPid);
        assertEquals("service:jmx:local://42", connectorFactory.connectedUrl);
        assertEquals("42", connected.id());
        assertEquals("42", connected.pid());
        assertEquals("service:jmx:local://42", connected.connectionUrl());
        assertEquals("26.0.1", connected.javaVersion());
    }

    @Test
    void repeatedLocalConnectClosesPreviousConnectorForSamePid() {
        RecordingLocalConnectorResolver resolver = new RecordingLocalConnectorResolver("service:jmx:local://42");
        RecordingJmxConnectorFactory connectorFactory = new RecordingJmxConnectorFactory();
        JmcJmxConnectionService service = new JmcJmxConnectionService(resolver, connectorFactory);

        service.connectLocal(JvmConnection.local("42", "demo.Main", "26.0.1", true));
        service.connectLocal(JvmConnection.local("42", "demo.Main", "26.0.1", true));

        assertEquals(2, connectorFactory.createdConnectors.size());
        assertEquals(1, connectorFactory.createdConnectors.getFirst().closeCount);
        assertEquals(0, connectorFactory.createdConnectors.get(1).closeCount);
    }

    @Test
    void localConnectorResolverStartsAgentWhenAddressIsMissing() throws IOException {
        RecordingAttachVirtualMachine vm = new RecordingAttachVirtualMachine("", "service:jmx:local://42");
        JmcJmxConnectionService.AttachLocalConnectorAddressResolver resolver =
                new JmcJmxConnectionService.AttachLocalConnectorAddressResolver(pid -> vm);

        JMXServiceURL url = resolver.resolve("42");

        assertEquals("42", vm.attachedPid);
        assertEquals("42", vm.startedPid);
        assertEquals("42", vm.detachedPid);
        assertEquals("service:jmx:local://42", url.toString());
    }

    @Test
    void localConnectorResolverUsesExistingAddressWhenAvailable() throws IOException {
        RecordingAttachVirtualMachine vm = new RecordingAttachVirtualMachine(
                "service:jmx:rmi:///jndi/rmi://127.0.0.1:0/jmxrmi", "service:jmx:local://42");
        JmcJmxConnectionService.AttachLocalConnectorAddressResolver resolver =
                new JmcJmxConnectionService.AttachLocalConnectorAddressResolver(pid -> vm);

        JMXServiceURL url = resolver.resolve("42");

        assertEquals("42", vm.attachedPid);
        assertEquals("", vm.startedPid);
        assertEquals("42", vm.detachedPid);
        assertEquals("service:jmx:rmi:///jndi/rmi://127.0.0.1:0/jmxrmi", url.toString());
    }

    @Test
    void sessionSnapshotReadsRuntimeAndCapabilitiesFromRegisteredConnector() throws Exception {
        RecordingLocalConnectorResolver resolver = new RecordingLocalConnectorResolver("service:jmx:local://42");
        RecordingJmxConnectorFactory connectorFactory = new RecordingJmxConnectorFactory();
        connectorFactory.nextMBeanServer = MBeanServers.runtimeServer(true, true);
        JmcJmxConnectionService service = new JmcJmxConnectionService(resolver, connectorFactory);
        JvmConnection connected = service.connectLocal(JvmConnection.local("42", "demo.Main", "26.0.1", true));

        JvmSessionSnapshot snapshot = service.sessionSnapshot(connected);

        assertEquals("OpenJDK 64-Bit Server VM", snapshot.runtime().vmName());
        assertEquals("Eclipse Adoptium", snapshot.runtime().vmVendor());
        assertEquals("26.0.1", snapshot.runtime().vmVersion());
        assertEquals(JvmCapabilityStatus.AVAILABLE, snapshot.statusOf(JvmCapability.MBEAN_SERVER));
        assertEquals(JvmCapabilityStatus.AVAILABLE, snapshot.statusOf(JvmCapability.RUNTIME_MXBEAN));
        assertEquals(JvmCapabilityStatus.AVAILABLE, snapshot.statusOf(JvmCapability.FLIGHT_RECORDER));
        assertEquals(JvmCapabilityStatus.AVAILABLE, snapshot.statusOf(JvmCapability.DIAGNOSTIC_COMMANDS));
    }

    @Test
    void sessionSnapshotMarksOptionalCapabilitiesUnavailableWhenMBeansAreMissing() throws Exception {
        RecordingLocalConnectorResolver resolver = new RecordingLocalConnectorResolver("service:jmx:local://42");
        RecordingJmxConnectorFactory connectorFactory = new RecordingJmxConnectorFactory();
        connectorFactory.nextMBeanServer = MBeanServers.runtimeServer(false, false);
        JmcJmxConnectionService service = new JmcJmxConnectionService(resolver, connectorFactory);
        JvmConnection connected = service.connectLocal(JvmConnection.local("42", "demo.Main", "26.0.1", true));

        JvmSessionSnapshot snapshot = service.sessionSnapshot(connected);

        assertEquals(JvmCapabilityStatus.UNAVAILABLE, snapshot.statusOf(JvmCapability.FLIGHT_RECORDER));
        assertEquals(JvmCapabilityStatus.UNAVAILABLE, snapshot.statusOf(JvmCapability.DIAGNOSTIC_COMMANDS));
    }

    @Test
    void sessionSnapshotRejectsDisconnectedConnection() {
        JmcJmxConnectionService service = new JmcJmxConnectionService();

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.sessionSnapshot(new JvmConnection("missing", "Missing", "", true)));

        assertEquals("No live JVM session for connection: missing", exception.getMessage());
    }

    private static final class RecordingLocalConnectorResolver
            implements JmcJmxConnectionService.LocalConnectorAddressResolver {
        private final String address;
        private String resolvedPid = "";

        private RecordingLocalConnectorResolver(String address) {
            this.address = address;
        }

        @Override
        public JMXServiceURL resolve(String pid) throws IOException {
            resolvedPid = pid;
            return new JMXServiceURL(address);
        }
    }

    private static final class RecordingJmxConnectorFactory implements JmcJmxConnectionService.JmxConnectorFactory {
        private final java.util.List<NoopJmxConnector> createdConnectors = new java.util.ArrayList<>();
        private MBeanServerConnection nextMBeanServer = MBeanServers.runtimeServer(true, true);
        private String connectedUrl = "";

        @Override
        public javax.management.remote.JMXConnector connect(JMXServiceURL serviceUrl) {
            connectedUrl = serviceUrl.toString();
            NoopJmxConnector connector = new NoopJmxConnector(nextMBeanServer);
            createdConnectors.add(connector);
            return connector;
        }
    }

    private static final class NoopJmxConnector implements javax.management.remote.JMXConnector {
        private final MBeanServerConnection mBeanServer;
        private int closeCount;

        private NoopJmxConnector(MBeanServerConnection mBeanServer) {
            this.mBeanServer = mBeanServer;
        }

        @Override
        public void connect() {
        }

        @Override
        public void connect(java.util.Map<String, ?> environment) {
        }

        @Override
        public javax.management.MBeanServerConnection getMBeanServerConnection() {
            return mBeanServer;
        }

        @Override
        @SuppressWarnings("removal")
        public javax.management.MBeanServerConnection getMBeanServerConnection(javax.security.auth.Subject delegationSubject) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            closeCount++;
        }

        @Override
        public void addConnectionNotificationListener(javax.management.NotificationListener listener,
                javax.management.NotificationFilter filter, Object handback) {
        }

        @Override
        public void removeConnectionNotificationListener(javax.management.NotificationListener listener) {
        }

        @Override
        public void removeConnectionNotificationListener(javax.management.NotificationListener listener,
                javax.management.NotificationFilter filter, Object handback) {
        }

        @Override
        public String getConnectionId() {
            return "noop";
        }
    }

    private static final class RecordingAttachVirtualMachine
            implements JmcJmxConnectionService.AttachedVirtualMachine {
        private final String existingAddress;
        private final String startedAddress;
        private String attachedPid = "";
        private String startedPid = "";
        private String detachedPid = "";

        private RecordingAttachVirtualMachine(String existingAddress, String startedAddress) {
            this.existingAddress = existingAddress;
            this.startedAddress = startedAddress;
        }

        @Override
        public String localConnectorAddress() {
            return existingAddress;
        }

        @Override
        public String startLocalManagementAgent(String pid) {
            startedPid = pid;
            return startedAddress;
        }

        @Override
        public void detach(String pid) {
            attachedPid = pid;
            detachedPid = pid;
        }
    }

    private static final class MBeanServers {
        private static MBeanServerConnection runtimeServer(boolean flightRecorder, boolean diagnosticCommands) {
            Set<String> registered = new java.util.HashSet<>();
            registered.add(ManagementFactory.RUNTIME_MXBEAN_NAME);
            registered.add(ManagementFactory.MEMORY_MXBEAN_NAME);
            registered.add(ManagementFactory.THREAD_MXBEAN_NAME);
            if (flightRecorder) {
                registered.add("jdk.management.jfr:type=FlightRecorder");
            }
            if (diagnosticCommands) {
                registered.add("com.sun.management:type=DiagnosticCommand");
            }
            return (MBeanServerConnection) java.lang.reflect.Proxy.newProxyInstance(
                    MBeanServerConnection.class.getClassLoader(),
                    new Class<?>[] { MBeanServerConnection.class },
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getAttribute" -> runtimeAttribute((String) args[1]);
                        case "isRegistered" -> registered.contains(((ObjectName) args[0]).getCanonicalName());
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private static Object runtimeAttribute(String attribute) {
            return switch (attribute) {
                case "VmName" -> "OpenJDK 64-Bit Server VM";
                case "VmVendor" -> "Eclipse Adoptium";
                case "VmVersion" -> "26.0.1";
                case "SpecVersion" -> "26";
                case "StartTime" -> Instant.EPOCH.toEpochMilli();
                case "Uptime" -> 1234L;
                default -> throw new UnsupportedOperationException(attribute);
            };
        }
    }
}
