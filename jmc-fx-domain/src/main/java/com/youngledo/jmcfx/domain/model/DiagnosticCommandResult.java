package com.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record DiagnosticCommandResult(boolean success, String output, String error) {

    public DiagnosticCommandResult {
        output = Objects.requireNonNullElse(output, "");
        error = Objects.requireNonNullElse(error, "");
    }
}
