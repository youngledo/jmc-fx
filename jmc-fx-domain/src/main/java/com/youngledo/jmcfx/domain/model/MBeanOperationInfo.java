package com.youngledo.jmcfx.domain.model;

import java.util.List;
import java.util.Objects;

public record MBeanOperationInfo(
        String name,
        String returnType,
        String description,
        List<MBeanOperationParameter> parameters) {

    public MBeanOperationInfo {
        name = Objects.requireNonNullElse(name, "");
        returnType = Objects.requireNonNullElse(returnType, "");
        description = Objects.requireNonNullElse(description, "");
        parameters = List.copyOf(Objects.requireNonNullElse(parameters, List.of()));
    }
}
