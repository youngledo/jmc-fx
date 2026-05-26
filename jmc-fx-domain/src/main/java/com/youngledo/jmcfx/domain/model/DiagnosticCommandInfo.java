package com.youngledo.jmcfx.domain.model;

import java.util.List;
import java.util.Objects;

public record DiagnosticCommandInfo(
        String name,
        String displayName,
        String description,
        List<DiagnosticCommandParameter> parameters) {

    public DiagnosticCommandInfo {
        name = Objects.requireNonNullElse(name, "");
        displayName = Objects.requireNonNullElse(displayName, "");
        description = Objects.requireNonNullElse(description, "");
        parameters = List.copyOf(Objects.requireNonNullElse(parameters, List.of()));
    }
}
