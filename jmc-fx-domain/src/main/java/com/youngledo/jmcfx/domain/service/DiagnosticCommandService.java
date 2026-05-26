package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.DiagnosticCommandInfo;
import com.youngledo.jmcfx.domain.model.DiagnosticCommandRequest;
import com.youngledo.jmcfx.domain.model.DiagnosticCommandResult;
import com.youngledo.jmcfx.domain.model.JvmConnection;

public interface DiagnosticCommandService {
    default List<DiagnosticCommandInfo> commands(JvmConnection connection) {
        throw new JmcFxException("Diagnostic Commands are not supported by this service.");
    }

    default DiagnosticCommandResult execute(DiagnosticCommandRequest request) {
        throw new JmcFxException("Diagnostic Command execution is not supported by this service.");
    }
}
