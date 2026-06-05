package io.github.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.model.MBeanNode;
import io.github.youngledo.jmcfx.domain.model.MBeanOperationParameter;
import io.github.youngledo.jmcfx.domain.model.MBeanOperationRequest;
import io.github.youngledo.jmcfx.domain.model.MBeanOperationResult;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;
import io.github.youngledo.jmcfx.domain.service.MBeanBrowserService;

public class JmcMBeanBrowserService implements MBeanBrowserService {

    private final JmxConnectionAccessor connectionAccessor;

    public JmcMBeanBrowserService(JmxConnectionAccessor connectionAccessor) {
        this.connectionAccessor = Objects.requireNonNull(connectionAccessor, "connectionAccessor");
    }

    @Override
    public List<MBeanNode> tree(JvmConnection connection) {
        try {
            Map<String, List<MBeanNode>> nodesByDomain = server(connection).queryNames(null, null).stream()
                    .sorted(Comparator.comparing(ObjectName::getCanonicalName))
                    .collect(TreeMap::new,
                            (domains, objectName) -> domains.computeIfAbsent(objectName.getDomain(), key -> new java.util.ArrayList<>())
                                    .add(MBeanNode.objectName(objectName.getCanonicalName(), displayName(objectName))),
                            Map::putAll);

            return nodesByDomain.entrySet().stream()
                    .map(entry -> MBeanNode.domain(entry.getKey(), entry.getValue()))
                    .toList();
        } catch (IOException exception) {
            throw new JmcFxException("Unable to browse MBeans: " + exception.getMessage(), exception);
        }
    }

    @Override
    public List<io.github.youngledo.jmcfx.domain.model.MBeanAttributeInfo> attributes(
            JvmConnection connection, String objectName) {
        try {
            MBeanServerConnection server = server(connection);
            ObjectName name = new ObjectName(objectName);
            MBeanInfo info = server.getMBeanInfo(name);

            return Arrays.stream(info.getAttributes())
                    .sorted(Comparator.comparing(MBeanAttributeInfo::getName))
                    .map(attribute -> attributeInfo(server, name, attribute))
                    .toList();
        } catch (IOException | JMException exception) {
            throw new JmcFxException("Unable to read MBean attributes: " + exception.getMessage(), exception);
        }
    }

    @Override
    public List<io.github.youngledo.jmcfx.domain.model.MBeanOperationInfo> operations(
            JvmConnection connection, String objectName) {
        try {
            MBeanInfo info = server(connection).getMBeanInfo(new ObjectName(objectName));

            return Arrays.stream(info.getOperations())
                    .sorted(Comparator.comparing(MBeanOperationInfo::getName)
                            .thenComparing(operation -> Arrays.stream(operation.getSignature())
                                    .map(MBeanParameterInfo::getType)
                                    .reduce("", (left, right) -> left + "\u0000" + right)))
                    .map(this::operationInfo)
                    .toList();
        } catch (IOException | JMException exception) {
            throw new JmcFxException("Unable to read MBean operations: " + exception.getMessage(), exception);
        }
    }

    @Override
    public MBeanOperationResult invoke(MBeanOperationRequest request) {
        if (request.parameterTypes().size() != request.arguments().size()) {
            return failed("Operation signature has %d parameter type(s) but %d argument(s)."
                    .formatted(request.parameterTypes().size(), request.arguments().size()));
        }

        Object[] arguments;
        try {
            arguments = convertArguments(request.parameterTypes(), request.arguments());
        } catch (IllegalArgumentException exception) {
            return failed(exception.getMessage());
        }

        try {
            Object result = server(request.connection()).invoke(
                    new ObjectName(request.objectName()),
                    request.operationName(),
                    arguments,
                    request.parameterTypes().toArray(String[]::new));
            return new MBeanOperationResult(true, displayValue(result), "");
        } catch (IOException | JMException | RuntimeException exception) {
            return failed(errorMessage(exception));
        }
    }

    private io.github.youngledo.jmcfx.domain.model.MBeanAttributeInfo attributeInfo(
            MBeanServerConnection server, ObjectName objectName, MBeanAttributeInfo attribute) {
        String value = "";
        String error = "";
        if (attribute.isReadable()) {
            try {
                value = displayValue(server.getAttribute(objectName, attribute.getName()));
            } catch (Exception exception) {
                error = errorMessage(exception);
            }
        }
        return new io.github.youngledo.jmcfx.domain.model.MBeanAttributeInfo(
                attribute.getName(),
                attribute.getType(),
                attribute.isReadable(),
                attribute.isWritable(),
                value,
                error);
    }

    private io.github.youngledo.jmcfx.domain.model.MBeanOperationInfo operationInfo(MBeanOperationInfo operation) {
        List<MBeanOperationParameter> parameters = Arrays.stream(operation.getSignature())
                .map(parameter -> new MBeanOperationParameter(
                        parameter.getName(),
                        parameter.getType(),
                        parameter.getDescription()))
                .toList();
        return new io.github.youngledo.jmcfx.domain.model.MBeanOperationInfo(
                operation.getName(),
                operation.getReturnType(),
                operation.getDescription(),
                parameters);
    }

    private Object[] convertArguments(List<String> parameterTypes, List<String> arguments) {
        Object[] converted = new Object[arguments.size()];
        for (int index = 0; index < arguments.size(); index++) {
            converted[index] = convertArgument(parameterTypes.get(index), arguments.get(index));
        }
        return converted;
    }

    private Object convertArgument(String type, String argument) {
        return switch (type) {
            case "java.lang.String" -> argument;
            case "boolean", "java.lang.Boolean" -> parseBoolean(argument);
            case "byte", "java.lang.Byte" -> Byte.parseByte(argument);
            case "short", "java.lang.Short" -> Short.parseShort(argument);
            case "int", "java.lang.Integer" -> Integer.parseInt(argument);
            case "long", "java.lang.Long" -> Long.parseLong(argument);
            case "float", "java.lang.Float" -> Float.parseFloat(argument);
            case "double", "java.lang.Double" -> Double.parseDouble(argument);
            default -> throw new IllegalArgumentException("Unsupported parameter type: " + type);
        };
    }

    private boolean parseBoolean(String argument) {
        if ("true".equalsIgnoreCase(argument)) {
            return true;
        }
        if ("false".equalsIgnoreCase(argument)) {
            return false;
        }
        throw new IllegalArgumentException("Invalid boolean value: " + argument + ". Expected true or false.");
    }

    private String displayName(ObjectName objectName) {
        String type = objectName.getKeyProperty("type");
        String name = objectName.getKeyProperty("name");
        if (type != null && name != null) {
            return type + "/" + name;
        }
        if (type != null) {
            return type;
        }
        return objectName.getKeyPropertyListString();
    }

    private String displayValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Object[] array) {
            return Arrays.deepToString(array);
        }
        if (value instanceof boolean[] array) {
            return Arrays.toString(array);
        }
        if (value instanceof byte[] array) {
            return Arrays.toString(array);
        }
        if (value instanceof short[] array) {
            return Arrays.toString(array);
        }
        if (value instanceof int[] array) {
            return Arrays.toString(array);
        }
        if (value instanceof long[] array) {
            return Arrays.toString(array);
        }
        if (value instanceof float[] array) {
            return Arrays.toString(array);
        }
        if (value instanceof double[] array) {
            return Arrays.toString(array);
        }
        if (value instanceof char[] array) {
            return Arrays.toString(array);
        }
        return String.valueOf(value);
    }

    private MBeanOperationResult failed(String error) {
        return new MBeanOperationResult(false, "", Objects.requireNonNullElse(error, ""));
    }

    private String errorMessage(Exception exception) {
        String message = exception.getMessage();
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
}
