package com.youngledo.jmcfx.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.JvmCapability;
import com.youngledo.jmcfx.domain.model.JvmCapabilitySnapshot;
import com.youngledo.jmcfx.domain.model.JvmCapabilityStatus;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.JvmConnectionSource;
import com.youngledo.jmcfx.domain.model.JvmConnectionState;
import com.youngledo.jmcfx.domain.model.JvmRuntimeSnapshot;
import com.youngledo.jmcfx.domain.model.JvmSessionSnapshot;
import com.youngledo.jmcfx.domain.model.MBeanAttributeInfo;
import com.youngledo.jmcfx.domain.model.MBeanNode;
import com.youngledo.jmcfx.domain.model.MBeanOperationInfo;
import com.youngledo.jmcfx.domain.model.MBeanOperationParameter;
import com.youngledo.jmcfx.domain.model.MBeanOperationRequest;
import com.youngledo.jmcfx.domain.model.MBeanOperationResult;
import com.youngledo.jmcfx.domain.service.JmcFxException;

class FakeJvmServicesTest {

    @Test
    void fakeDiscoveryCanReplaceSnapshot() {
        FakeJvmDiscoveryService discovery = new FakeJvmDiscoveryService();
        discovery.setConnections(List.of(local("1", "one.Main")));
        discovery.setConnections(List.of(local("2", "two.Main")));

        assertEquals(1, discovery.discoverLocalJvms().size());
        assertEquals("2", discovery.discoverLocalJvms().getFirst().pid());
    }

    @Test
    void fakeJmxServiceConnectsRemoteUrl() {
        FakeJmxConnectionService service = new FakeJmxConnectionService();

        JvmConnection connected = service.connect("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi");

        assertTrue(connected.connected());
        assertEquals(JvmConnectionSource.MANUAL, connected.source());
        assertEquals(JvmConnectionState.CONNECTED, connected.state());
        assertTrue(service.connectedConnections().contains(connected.id()));
    }

    @Test
    void fakeJmxServiceConnectsLocalAttachableJvm() {
        FakeJmxConnectionService service = new FakeJmxConnectionService();

        JvmConnection connected = service.connectLocal(local("42", "demo.Main"));

        assertTrue(connected.connected());
        assertEquals("42", connected.id());
        assertEquals("42", connected.pid());
        assertEquals(JvmConnectionSource.LOCAL, connected.source());
        assertEquals("service:jmx:local://42", connected.connectionUrl());
        assertTrue(service.connectedConnections().contains(connected.id()));
    }

    @Test
    void fakeJmxServiceRejectsUnavailableLocalJvm() {
        FakeJmxConnectionService service = new FakeJmxConnectionService();

        assertThrows(IllegalArgumentException.class,
                () -> service.connectLocal(JvmConnection.local("42", "blocked.Main", "", false)));
    }

    @Test
    void fakeJmxServiceRejectsLocalJvmWithoutPid() {
        FakeJmxConnectionService service = new FakeJmxConnectionService();

        assertThrows(IllegalArgumentException.class,
                () -> service.connectLocal(JvmConnection.local("", "unknown.Main", "26.0.1", true)));
    }

    @Test
    void fakeJmxServiceReturnsRegisteredSessionSnapshot() {
        FakeJmxConnectionService service = new FakeJmxConnectionService();
        JvmConnection connection = service.connect("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi");
        JvmSessionSnapshot snapshot = new JvmSessionSnapshot(connection,
                new JvmRuntimeSnapshot("OpenJDK 64-Bit Server VM", "Eclipse Adoptium",
                        "26.0.1", "26", java.time.Instant.EPOCH, 1000),
                List.of(new JvmCapabilitySnapshot(JvmCapability.MBEAN_SERVER,
                        JvmCapabilityStatus.AVAILABLE, "Available")));

        service.setSessionSnapshot(connection.id(), snapshot);

        assertEquals(snapshot, service.sessionSnapshot(connection));
    }

    @Test
    void fakeJmxServiceRejectsUnknownSessionSnapshot() {
        FakeJmxConnectionService service = new FakeJmxConnectionService();

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.sessionSnapshot(new JvmConnection("missing", "Missing", "", true)));

        assertEquals("No live JVM session for connection: missing", exception.getMessage());
    }

    @Test
    void fakeMBeanServiceReturnsRegisteredData() {
        FakeMBeanBrowserService service = new FakeMBeanBrowserService();
        JvmConnection connection = local("42", "demo.Main").asConnected("service:jmx:local://42");
        MBeanNode runtime = MBeanNode.objectName("java.lang:type=Runtime", "Runtime");
        MBeanAttributeInfo vmName = new MBeanAttributeInfo("VmName", "java.lang.String", true, false,
                "OpenJDK", "");
        MBeanOperationInfo operation = new MBeanOperationInfo("gc", "void", "",
                List.of(new MBeanOperationParameter("verbose", "boolean", "")));

        service.setTree(connection.id(), List.of(MBeanNode.domain("java.lang", List.of(runtime))));
        service.setAttributes(connection.id(), runtime.objectName(), List.of(vmName));
        service.setOperations(connection.id(), runtime.objectName(), List.of(operation));
        service.setOperationResult(connection.id(), runtime.objectName(), "gc", List.of("boolean"),
                new MBeanOperationResult(true, "ok", ""));

        assertEquals(1, service.tree(connection).size());
        assertEquals(vmName, service.attributes(connection, runtime.objectName()).getFirst());
        assertEquals(operation, service.operations(connection, runtime.objectName()).getFirst());
        assertEquals("ok", service.invoke(new MBeanOperationRequest(connection,
                runtime.objectName(), "gc", List.of("boolean"), List.of("true"))).value());
    }

    @Test
    void fakeMBeanServiceRejectsMissingObjectName() {
        FakeMBeanBrowserService service = new FakeMBeanBrowserService();
        JvmConnection connection = local("42", "demo.Main").asConnected("service:jmx:local://42");

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.attributes(connection, "missing:type=Nope"));

        assertEquals("No fake MBean attributes for 42 missing:type=Nope", exception.getMessage());
    }

    @Test
    void fakeMBeanServiceRejectsWrongOperationSignature() {
        FakeMBeanBrowserService service = new FakeMBeanBrowserService();
        JvmConnection connection = local("42", "demo.Main").asConnected("service:jmx:local://42");
        String objectName = "demo:type=Operations";

        service.setOperationResult(connection.id(), objectName, "update", List.of("java.lang.String"),
                new MBeanOperationResult(true, "string", ""));

        JmcFxException exception = assertThrows(JmcFxException.class,
                () -> service.invoke(new MBeanOperationRequest(connection,
                        objectName, "update", List.of("int"), List.of("7"))));

        assertEquals("No fake MBean operation result for update", exception.getMessage());
    }

    @Test
    void fakeMBeanServiceResolvesSameNameOperationsBySignature() {
        FakeMBeanBrowserService service = new FakeMBeanBrowserService();
        JvmConnection connection = local("42", "demo.Main").asConnected("service:jmx:local://42");
        String objectName = "demo:type=Operations";

        service.setOperationResult(connection.id(), objectName, "update", List.of("java.lang.String"),
                new MBeanOperationResult(true, "string", ""));
        service.setOperationResult(connection.id(), objectName, "update", List.of("int"),
                new MBeanOperationResult(true, "int", ""));

        assertEquals("string", service.invoke(new MBeanOperationRequest(connection,
                objectName, "update", List.of("java.lang.String"), List.of("alpha"))).value());
        assertEquals("int", service.invoke(new MBeanOperationRequest(connection,
                objectName, "update", List.of("int"), List.of("7"))).value());
    }

    private static JvmConnection local(String pid, String name) {
        return JvmConnection.local(pid, name, "26.0.1", true);
    }
}
