package io.github.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.management.Descriptor;
import javax.management.JMException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import io.github.youngledo.jmcfx.domain.model.DiagnosticCommandInfo;
import io.github.youngledo.jmcfx.domain.model.DiagnosticCommandParameter;
import io.github.youngledo.jmcfx.domain.model.DiagnosticCommandRequest;
import io.github.youngledo.jmcfx.domain.model.DiagnosticCommandResult;
import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.service.DiagnosticCommandService;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;

public class JmcDiagnosticCommandService implements DiagnosticCommandService {

    private static final ObjectName DIAGNOSTIC_COMMAND = objectName("com.sun.management:type=DiagnosticCommand");
    private static final List<String> COMMON_COMMANDS = List.of(
            "threadPrint",
            "gcHeapInfo",
            "vmFlags",
            "gcClassHistogram",
            "vmCommandLine",
            "help");
    private static final Map<String, Integer> COMMON_COMMAND_ORDER = commandOrder();

    private final JmxConnectionAccessor connectionAccessor;

    public JmcDiagnosticCommandService(JmxConnectionAccessor connectionAccessor) {
        this.connectionAccessor = Objects.requireNonNull(connectionAccessor, "connectionAccessor");
    }

    @Override
    public List<DiagnosticCommandInfo> commands(JvmConnection connection) {
        try {
            MBeanInfo info = server(connection).getMBeanInfo(DIAGNOSTIC_COMMAND);
            return Arrays.stream(info.getOperations())
                    .sorted(commandComparator())
                    .map(this::commandInfo)
                    .toList();
        } catch (IOException | JMException exception) {
            throw new JmcFxException("Unable to read diagnostic commands: " + exception.getMessage(), exception);
        }
    }

    @Override
    public DiagnosticCommandResult execute(DiagnosticCommandRequest request) {
        try {
            Object output = server(request.connection()).invoke(
                    DIAGNOSTIC_COMMAND,
                    request.commandName(),
                    new Object[] { request.arguments().toArray(String[]::new) },
                    new String[] { "[Ljava.lang.String;" });
            return new DiagnosticCommandResult(true, Objects.requireNonNullElse(output, "").toString(), "");
        } catch (IOException | JMException | RuntimeException exception) {
            return failed(errorMessage(exception));
        }
    }

    private DiagnosticCommandInfo commandInfo(MBeanOperationInfo operation) {
        return new DiagnosticCommandInfo(
                operation.getName(),
                displayName(operation),
                operation.getDescription(),
                Arrays.stream(operation.getSignature())
                        .map(this::parameterInfo)
                        .toList());
    }

    private String displayName(MBeanOperationInfo operation) {
        Descriptor descriptor = operation.getDescriptor();
        Object dcmdName = descriptor == null ? null : descriptor.getFieldValue("dcmd.name");
        String displayName = dcmdName == null ? "" : dcmdName.toString();
        return displayName.isBlank() ? operation.getName() : displayName;
    }

    private DiagnosticCommandParameter parameterInfo(MBeanParameterInfo parameter) {
        return new DiagnosticCommandParameter(
                parameter.getName(),
                parameter.getType(),
                parameter.getDescription(),
                false);
    }

    private Comparator<MBeanOperationInfo> commandComparator() {
        return Comparator
                .comparingInt((MBeanOperationInfo operation) ->
                        COMMON_COMMAND_ORDER.getOrDefault(operation.getName(), Integer.MAX_VALUE))
                .thenComparing(MBeanOperationInfo::getName);
    }

    private DiagnosticCommandResult failed(String error) {
        return new DiagnosticCommandResult(false, "", Objects.requireNonNullElse(error, ""));
    }

    private String errorMessage(Exception exception) {
        String message = exception.getMessage();
        if ((message == null || message.isBlank()) && exception.getCause() != null) {
            message = exception.getCause().getMessage();
        }
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }

    private MBeanServerConnection server(JvmConnection connection) {
        try {
            return connectionAccessor.mBeanServerConnection(connection);
        } catch (IOException exception) {
            throw new JmcFxException("No live JVM session for connection: " + connection.id(), exception);
        }
    }

    private static Map<String, Integer> commandOrder() {
        java.util.HashMap<String, Integer> order = new java.util.HashMap<>();
        for (int index = 0; index < COMMON_COMMANDS.size(); index++) {
            order.put(COMMON_COMMANDS.get(index), index);
        }
        return Map.copyOf(order);
    }

    private static ObjectName objectName(String name) {
        try {
            return new ObjectName(name);
        } catch (JMException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
