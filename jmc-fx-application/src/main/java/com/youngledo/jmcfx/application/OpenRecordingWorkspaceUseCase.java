package com.youngledo.jmcfx.application;

import java.nio.file.Path;
import java.util.Objects;

public final class OpenRecordingWorkspaceUseCase {

    private final RecordingApplicationServices services;

    public OpenRecordingWorkspaceUseCase(RecordingApplicationServices services) {
        this.services = Objects.requireNonNull(services, "services");
    }

    public RecordingWorkspacePlan open(Path path) {
        Objects.requireNonNull(path, "path");
        return new RecordingWorkspacePlan(
                services.recordingRepository().open(path),
                services.profilingService() != null,
                services.exceptionService() != null,
                services.threadService() != null,
                services.fileIOService() != null,
                services.socketIOService() != null,
                services.lockService() != null,
                services.heapService() != null,
                services.leakSuspectsService() != null,
                services.tlabService() != null,
                services.jvmInternalsService() != null,
                services.environmentService() != null,
                services.javaAppService() != null,
                services.jfrMetadataService() != null,
                services.g1GcService() != null,
                services.javaFxEventService() != null,
                services.advancedJfrAnalysisService() != null);
    }
}
