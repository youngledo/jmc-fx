package com.youngledo.jmcfx.domain.model;

import java.util.List;
import java.util.Objects;

public record DiagnosticCommandRequest(
        JvmConnection connection,
        String commandName,
        List<String> arguments) {

    public DiagnosticCommandRequest {
        connection = Objects.requireNonNull(connection, "connection");
        commandName = Objects.requireNonNullElse(commandName, "");
        arguments = List.copyOf(Objects.requireNonNullElse(arguments, List.of()));
    }
}
