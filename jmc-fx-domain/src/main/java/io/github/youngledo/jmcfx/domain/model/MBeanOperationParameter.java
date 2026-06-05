package io.github.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record MBeanOperationParameter(String name, String type, String description) {

    public MBeanOperationParameter {
        name = Objects.requireNonNullElse(name, "");
        type = Objects.requireNonNullElse(type, "");
        description = Objects.requireNonNullElse(description, "");
    }
}
