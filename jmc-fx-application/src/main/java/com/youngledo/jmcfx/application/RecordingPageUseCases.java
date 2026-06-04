package com.youngledo.jmcfx.application;

import java.util.Objects;

public record RecordingPageUseCases(
        OpenRecordingUseCase openRecording,
        OpenRecordingWorkspaceUseCase openRecordingWorkspace,
        BrowseEventsUseCase browseEvents,
        AnalyzeRulesUseCase analyzeRules,
        LoadProfilingUseCase profiling,
        LoadExceptionsUseCase exceptions,
        LoadThreadsUseCase threads,
        LoadFileIOUseCase fileIO,
        LoadSocketIOUseCase socketIO,
        LoadLocksUseCase locks,
        LoadHeapUseCase heap,
        LoadLeakSuspectsUseCase leakSuspects,
        LoadTlabUseCase tlab,
        LoadJvmInternalsUseCase jvmInternals,
        LoadEnvironmentUseCase environment,
        LoadJavaApplicationUseCase javaApplication,
        LoadJfrMetadataUseCase jfrMetadata,
        LoadG1GcUseCase g1Gc,
        LoadJavaFxEventsUseCase javaFxEvents,
        LoadAdvancedJfrUseCase advancedJfr) {

    public RecordingPageUseCases {
        Objects.requireNonNull(openRecording, "openRecording");
        Objects.requireNonNull(openRecordingWorkspace, "openRecordingWorkspace");
        Objects.requireNonNull(browseEvents, "browseEvents");
        Objects.requireNonNull(analyzeRules, "analyzeRules");
    }

    public static RecordingPageUseCases from(RecordingApplicationServices services) {
        Objects.requireNonNull(services, "services");
        return new RecordingPageUseCases(
                new OpenRecordingUseCase(services.recordingRepository()),
                new OpenRecordingWorkspaceUseCase(services),
                new BrowseEventsUseCase(services.eventQueryService()),
                new AnalyzeRulesUseCase(services.ruleAnalysisService()),
                services.profilingService() == null ? null : new LoadProfilingUseCase(services.profilingService()),
                services.exceptionService() == null ? null : new LoadExceptionsUseCase(services.exceptionService()),
                services.threadService() == null ? null : new LoadThreadsUseCase(services.threadService()),
                services.fileIOService() == null ? null : new LoadFileIOUseCase(services.fileIOService()),
                services.socketIOService() == null ? null : new LoadSocketIOUseCase(services.socketIOService()),
                services.lockService() == null ? null : new LoadLocksUseCase(services.lockService()),
                services.heapService() == null ? null : new LoadHeapUseCase(services.heapService()),
                services.leakSuspectsService() == null ? null : new LoadLeakSuspectsUseCase(services.leakSuspectsService()),
                services.tlabService() == null ? null : new LoadTlabUseCase(services.tlabService()),
                services.jvmInternalsService() == null ? null : new LoadJvmInternalsUseCase(services.jvmInternalsService()),
                services.environmentService() == null ? null : new LoadEnvironmentUseCase(services.environmentService()),
                services.javaAppService() == null ? null : new LoadJavaApplicationUseCase(services.javaAppService()),
                services.jfrMetadataService() == null ? null : new LoadJfrMetadataUseCase(services.jfrMetadataService()),
                services.g1GcService() == null ? null : new LoadG1GcUseCase(services.g1GcService()),
                services.javaFxEventService() == null ? null : new LoadJavaFxEventsUseCase(services.javaFxEventService()),
                services.advancedJfrAnalysisService() == null
                        ? null
                        : new LoadAdvancedJfrUseCase(services.advancedJfrAnalysisService()));
    }
}
