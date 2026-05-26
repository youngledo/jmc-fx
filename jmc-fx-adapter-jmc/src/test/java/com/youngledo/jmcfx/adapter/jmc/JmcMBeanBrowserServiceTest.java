package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.MBeanAttributeInfo;
import com.youngledo.jmcfx.domain.model.MBeanNode;
import com.youngledo.jmcfx.domain.model.MBeanOperationInfo;
import com.youngledo.jmcfx.domain.model.MBeanOperationRequest;
import com.youngledo.jmcfx.domain.model.MBeanOperationResult;

class JmcMBeanBrowserServiceTest {

    private static final ObjectName ECHO_NAME = objectName("jmcfx.test:type=Echo");
    private static final JvmConnection CONNECTION = new JvmConnection("local", "Local", "", true);

    private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    private final JmcMBeanBrowserService service = new JmcMBeanBrowserService(connection -> server);

    @BeforeEach
    void registerEchoBean() throws Exception {
        unregisterEchoBean();
        server.registerMBean(new Echo(), ECHO_NAME);
    }

    @AfterEach
    void unregisterEchoBean() throws Exception {
        if (server.isRegistered(ECHO_NAME)) {
            server.unregisterMBean(ECHO_NAME);
        }
    }

    @Test
    void treeGroupsObjectNamesByDomain() {
        List<MBeanNode> tree = service.tree(CONNECTION);

        MBeanNode domain = tree.stream()
                .filter(node -> node.domain() && node.name().equals("jmcfx.test"))
                .findFirst()
                .orElseThrow();

        assertTrue(domain.children().stream()
                .anyMatch(node -> node.objectName().equals("jmcfx.test:type=Echo")
                        && node.name().equals("Echo")));
    }

    @Test
    void attributesIncludeReadableValuesAndWriteFlags() {
        List<MBeanAttributeInfo> attributes = service.attributes(CONNECTION, ECHO_NAME.getCanonicalName());

        MBeanAttributeInfo message = attributes.stream()
                .filter(attribute -> attribute.name().equals("Message"))
                .findFirst()
                .orElseThrow();

        assertEquals("java.lang.String", message.type());
        assertTrue(message.readable());
        assertTrue(message.writable());
        assertEquals("hello", message.value());
        assertEquals("", message.error());
    }

    @Test
    void operationsExposeMetadataAndInvokeSimpleScalars() {
        List<MBeanOperationInfo> operations = service.operations(CONNECTION, ECHO_NAME.getCanonicalName());

        MBeanOperationInfo echo = operations.stream()
                .filter(operation -> operation.name().equals("echo"))
                .filter(operation -> operation.parameters().size() == 1)
                .filter(operation -> operation.parameters().getFirst().type().equals("java.lang.String"))
                .findFirst()
                .orElseThrow();

        assertEquals("java.lang.String", echo.returnType());
        assertEquals(1, echo.parameters().size());
        assertEquals("java.lang.String", echo.parameters().getFirst().type());

        MBeanOperationResult result = service.invoke(new MBeanOperationRequest(
                CONNECTION,
                ECHO_NAME.getCanonicalName(),
                "echo",
                List.of("java.lang.String"),
                List.of("live")));

        assertTrue(result.success());
        assertEquals("echo: live", result.value());
        assertEquals("", result.error());
    }

    @Test
    void failedAttributeReadReturnsErrorTextForThatAttribute() {
        List<MBeanAttributeInfo> attributes = service.attributes(CONNECTION, ECHO_NAME.getCanonicalName());

        MBeanAttributeInfo broken = attributes.stream()
                .filter(attribute -> attribute.name().equals("Broken"))
                .findFirst()
                .orElseThrow();

        assertEquals("", broken.value());
        assertTrue(broken.error().contains("broken"));
    }

    @Test
    void invokeUsesExactParameterTypesForOverloadedOperations() {
        MBeanOperationResult result = service.invoke(new MBeanOperationRequest(
                CONNECTION,
                ECHO_NAME.getCanonicalName(),
                "echo",
                List.of("int"),
                List.of("7")));

        assertTrue(result.success());
        assertEquals("echo int: 7", result.value());
        assertEquals("", result.error());
    }

    private static ObjectName objectName(String name) {
        try {
            return new ObjectName(name);
        } catch (Exception exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    public interface EchoMBean {
        String getMessage();

        void setMessage(String message);

        String getBroken();

        String echo(String value);

        String echo(int value);
    }

    public static final class Echo implements EchoMBean {
        private String message = "hello";

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public String getBroken() {
            throw new IllegalStateException("broken");
        }

        @Override
        public String echo(String value) {
            return "echo: " + value;
        }

        @Override
        public String echo(int value) {
            return "echo int: " + value;
        }
    }
}
