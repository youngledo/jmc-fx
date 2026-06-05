package io.github.youngledo.jmcfx.application;

import java.util.Objects;

import io.github.youngledo.jmcfx.domain.service.JmcFxException;

public record LiveJvmUseCases(
        LiveJvmDiscoveryUseCase discovery,
        LiveJvmConnectionUseCase connection,
        LiveJvmRecordingUseCase recording,
        LiveJvmMBeanUseCase mbeans,
        LiveJvmDiagnosticsUseCase diagnostics,
        LiveJvmAgentUseCase agent,
        LiveJvmMonitoringUseCase monitoring,
        LiveJvmPersistenceUseCase persistence) {

    public LiveJvmUseCases {
        Objects.requireNonNull(discovery, "discovery");
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(monitoring, "monitoring");
        Objects.requireNonNull(persistence, "persistence");
    }

    public static LiveJvmUseCases from(LiveJvmApplicationServices services) {
        Objects.requireNonNull(services, "services");
        return new LiveJvmUseCases(
                new LiveJvmDiscoveryUseCase(services.jvmDiscoveryService(), services.jdpDiscoveryService()),
                new LiveJvmConnectionUseCase(services.jmxConnectionService()),
                new LiveJvmRecordingUseCase(services.flightRecordingService()),
                new LiveJvmMBeanUseCase(services.mBeanBrowserService()),
                new LiveJvmDiagnosticsUseCase(services.diagnosticCommandService(), services.liveMetricService()),
                new LiveJvmAgentUseCase(services.jmcAgentService()),
                new LiveJvmMonitoringUseCase(services.jmxMonitoringService(), services.jmxMonitoringRepository()),
                new LiveJvmPersistenceUseCase(services.savedTargetRepository()));
    }

    public boolean expectedApplicationFailure(RuntimeException exception) {
        return exception instanceof JmcFxException;
    }
}
