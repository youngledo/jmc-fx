package io.github.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record MBeanAttributeInfo(
        String name,
        String type,
        boolean readable,
        boolean writable,
        String value,
        String error) {

    public MBeanAttributeInfo {
        name = Objects.requireNonNullElse(name, "");
        type = Objects.requireNonNullElse(type, "");
        value = Objects.requireNonNullElse(value, "");
        error = Objects.requireNonNullElse(error, "");
    }
}
