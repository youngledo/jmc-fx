package io.github.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record DiagnosticCommandParameter(String name, String type, String description, boolean required) {

    public DiagnosticCommandParameter {
        name = Objects.requireNonNullElse(name, "");
        type = Objects.requireNonNullElse(type, "");
        description = Objects.requireNonNullElse(description, "");
    }
}
