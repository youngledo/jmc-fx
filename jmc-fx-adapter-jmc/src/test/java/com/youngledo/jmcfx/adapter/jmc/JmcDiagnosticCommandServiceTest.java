package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.DiagnosticCommandInfo;
import com.youngledo.jmcfx.domain.model.DiagnosticCommandRequest;
import com.youngledo.jmcfx.domain.model.DiagnosticCommandResult;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.service.JmcFxException;

class JmcDiagnosticCommandServiceTest {

    private static final ObjectName DIAGNOSTIC_COMMAND = objectName("com.sun.management:type=DiagnosticCommand");
    private static final JvmConnection CONNECTION = new JvmConnection("local", "Local", "", true);

    private MBeanServer server;
    private DiagnosticCommandBean diagnosticCommand;
    private JmcDiagnosticCommandService service;

    @BeforeEach
    void registerDiagnosticCommandBean() throws Exception {
        server = MBeanServerFactory.newMBeanServer();
        diagnosticCommand = new DiagnosticCommandBean();
        server.registerMBean(diagnosticCommand, DIAGNOSTIC_COMMAND);
        service = new JmcDiagnosticCommandService(connection -> server);
    }

    @Test
    void commandsExposeDiagnosticOperationsWithCommonCommandsFirst() {
        List<DiagnosticCommandInfo> commands = service.commands(CONNECTION);

        assertEquals(List.of("threadPrint", "gcHeapInfo", "zCustom"),
                commands.stream().map(DiagnosticCommandInfo::name).toList());
        assertEquals(List.of("threadPrint", "gcHeapInfo", "zCustom"),
                commands.stream().map(DiagnosticCommandInfo::displayName).toList());

        DiagnosticCommandInfo threadPrint = commands.getFirst();
        assertEquals("Print all threads", threadPrint.description());
        assertEquals(1, threadPrint.parameters().size());
        assertEquals("options", threadPrint.parameters().getFirst().name());
        assertEquals("[Ljava.lang.String;", threadPrint.parameters().getFirst().type());
        assertEquals("Thread print options", threadPrint.parameters().getFirst().description());
        assertFalse(threadPrint.parameters().getFirst().required());
    }

    @Test
    void executeInvokesDiagnosticCommandWithStringArrayArguments() {
        DiagnosticCommandResult result = service.execute(new DiagnosticCommandRequest(
                CONNECTION,
                "threadPrint",
                List.of("-l", "-e")));

        assertTrue(result.success());
        assertEquals("thread dump", result.output());
        assertEquals("", result.error());
        assertEquals("threadPrint", diagnosticCommand.operationName);
        assertArrayEquals(new String[] { "-l", "-e" }, diagnosticCommand.arguments);
        assertArrayEquals(new String[] { "[Ljava.lang.String;" }, diagnosticCommand.signature);
    }

    @Test
    void executeUsesEmptyOutputForNullCommandOutput() {
        DiagnosticCommandResult result = service.execute(new DiagnosticCommandRequest(
                CONNECTION,
                "gcHeapInfo",
                List.of()));

        assertTrue(result.success());
        assertEquals("", result.output());
        assertEquals("", result.error());
    }

    @Test
    void executeReturnsFailureResultForCommandErrors() {
        DiagnosticCommandResult result = service.execute(new DiagnosticCommandRequest(
                CONNECTION,
                "zCustom",
                List.of()));

        assertFalse(result.success());
        assertEquals("", result.output());
        assertTrue(result.error().contains("custom failed"));
    }

    @Test
    void executeReturnsFailureResultForUnknownCommands() {
        DiagnosticCommandResult result = service.execute(new DiagnosticCommandRequest(
                CONNECTION,
                "missing",
                List.of()));

        assertFalse(result.success());
        assertEquals("", result.output());
        assertTrue(result.error().contains("missing"));
    }

    @Test
    void commandsWrapMissingLiveSessionAsJmcFxException() {
        JmcDiagnosticCommandService disconnected = new JmcDiagnosticCommandService(connection -> {
            throw new java.io.IOException("session closed");
        });

        JmcFxException exception = assertInstanceOf(JmcFxException.class,
                org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                        () -> disconnected.commands(CONNECTION)));

        assertEquals("No live JVM session for connection: local", exception.getMessage());
    }

    private static ObjectName objectName(String name) {
        try {
            return new ObjectName(name);
        } catch (Exception exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    private static final class DiagnosticCommandBean implements DynamicMBean {
        private String operationName;
        private String[] arguments = new String[0];
        private String[] signature = new String[0];

        @Override
        public Object getAttribute(String attribute) {
            throw new UnsupportedOperationException(attribute);
        }

        @Override
        public void setAttribute(Attribute attribute) {
            throw new UnsupportedOperationException(attribute.getName());
        }

        @Override
        public AttributeList getAttributes(String[] attributes) {
            return new AttributeList();
        }

        @Override
        public AttributeList setAttributes(AttributeList attributes) {
            return new AttributeList();
        }

        @Override
        public Object invoke(String actionName, Object[] params, String[] signature)
                throws MBeanException, ReflectionException {
            this.operationName = actionName;
            this.arguments = params == null || params.length == 0 ? new String[0] : (String[]) params[0];
            this.signature = signature == null ? new String[0] : signature;

            return switch (actionName) {
                case "threadPrint" -> "thread dump";
                case "gcHeapInfo" -> null;
                case "zCustom" -> throw new MBeanException(new IllegalStateException("custom failed"));
                default -> throw new ReflectionException(new NoSuchMethodException(actionName));
            };
        }

        @Override
        public MBeanInfo getMBeanInfo() {
            return new MBeanInfo(
                    getClass().getName(),
                    "Diagnostic command test bean",
                    null,
                    null,
                    new MBeanOperationInfo[] {
                            operation("zCustom", "Custom command"),
                            operation("gcHeapInfo", "Heap information"),
                            new MBeanOperationInfo(
                                    "threadPrint",
                                    "Print all threads",
                                    new MBeanParameterInfo[] {
                                            new MBeanParameterInfo(
                                                    "options",
                                                    "[Ljava.lang.String;",
                                                    "Thread print options")
                                    },
                                    "java.lang.String",
                                    MBeanOperationInfo.ACTION)
                    },
                    null);
        }

        private MBeanOperationInfo operation(String name, String description) {
            return new MBeanOperationInfo(
                    name,
                    description,
                    new MBeanParameterInfo[] {
                            new MBeanParameterInfo("options", "[Ljava.lang.String;", name + " options")
                    },
                    "java.lang.String",
                    MBeanOperationInfo.ACTION);
        }
    }
}
