package com.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record MBeanOperationResult(boolean success, String value, String error) {

    public MBeanOperationResult {
        value = Objects.requireNonNullElse(value, "");
        error = Objects.requireNonNullElse(error, "");
    }
}
