package com.youngledo.jmcfx.domain.model;

import java.util.List;
import java.util.Objects;

public record MBeanOperationRequest(
        JvmConnection connection,
        String objectName,
        String operationName,
        List<String> arguments) {

    public MBeanOperationRequest {
        connection = Objects.requireNonNull(connection, "connection");
        objectName = Objects.requireNonNullElse(objectName, "");
        operationName = Objects.requireNonNullElse(operationName, "");
        arguments = List.copyOf(Objects.requireNonNullElse(arguments, List.of()));
    }
}
