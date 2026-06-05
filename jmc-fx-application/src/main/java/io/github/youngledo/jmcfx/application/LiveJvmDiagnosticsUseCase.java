package io.github.youngledo.jmcfx.application;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.DiagnosticCommandInfo;
import io.github.youngledo.jmcfx.domain.model.DiagnosticCommandRequest;
import io.github.youngledo.jmcfx.domain.model.DiagnosticCommandResult;
import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.model.LiveMetricDefinition;
import io.github.youngledo.jmcfx.domain.model.LiveMetricSnapshot;
import io.github.youngledo.jmcfx.domain.service.DiagnosticCommandService;
import io.github.youngledo.jmcfx.domain.service.LiveMetricService;

public final class LiveJvmDiagnosticsUseCase {

    private final DiagnosticCommandService diagnosticCommandService;
    private final LiveMetricService liveMetricService;

    public LiveJvmDiagnosticsUseCase(
            DiagnosticCommandService diagnosticCommandService, LiveMetricService liveMetricService) {
        this.diagnosticCommandService = diagnosticCommandService;
        this.liveMetricService = liveMetricService;
    }

    public boolean diagnosticCommandsAvailable() {
        return diagnosticCommandService != null;
    }

    public List<DiagnosticCommandInfo> commands(JvmConnection connection) {
        return diagnosticCommandService.commands(connection);
    }

    public DiagnosticCommandResult execute(DiagnosticCommandRequest request) {
        return diagnosticCommandService.execute(request);
    }

    public boolean liveMetricsAvailable() {
        return liveMetricService != null;
    }

    public List<LiveMetricDefinition> definitions(JvmConnection connection) {
        return liveMetricService.definitions(connection);
    }

    public List<LiveMetricSnapshot> snapshot(JvmConnection connection) {
        return liveMetricService.snapshot(connection);
    }
}
