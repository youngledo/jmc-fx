package io.github.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record JmcAgentTransform(String id, String className, String methodName, String methodDescriptor) {

    public JmcAgentTransform {
        id = Objects.requireNonNullElse(id, "");
        className = Objects.requireNonNullElse(className, "");
        methodName = Objects.requireNonNullElse(methodName, "");
        methodDescriptor = Objects.requireNonNullElse(methodDescriptor, "");
    }
}
