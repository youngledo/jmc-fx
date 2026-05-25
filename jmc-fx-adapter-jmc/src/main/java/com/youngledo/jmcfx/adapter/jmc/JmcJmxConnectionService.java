package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.JvmConnectionSource;
import com.youngledo.jmcfx.domain.model.JvmConnectionState;
import com.youngledo.jmcfx.domain.service.JmcFxException;
import com.youngledo.jmcfx.domain.service.JmxConnectionService;

public class JmcJmxConnectionService implements JmxConnectionService {

    static final String LOCAL_CONNECTOR_ADDRESS_PROPERTY =
            "com.sun.management.jmxremote.localConnectorAddress";

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
