package io.github.youngledo.jmcfx.domain.service;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.DiagnosticCommandInfo;
import io.github.youngledo.jmcfx.domain.model.DiagnosticCommandRequest;
import io.github.youngledo.jmcfx.domain.model.DiagnosticCommandResult;
import io.github.youngledo.jmcfx.domain.model.JvmConnection;

public interface DiagnosticCommandService {
    default List<DiagnosticCommandInfo> commands(JvmConnection connection) {
        throw new JmcFxException("Diagnostic Commands are not supported by this service.");
    }

    default DiagnosticCommandResult execute(DiagnosticCommandRequest request) {
        throw new JmcFxException("Diagnostic Command execution is not supported by this service.");
    }
}
