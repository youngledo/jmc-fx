package io.github.youngledo.jmcfx.application;

import io.github.youngledo.jmcfx.domain.service.AdvancedJfrAnalysisService;
import io.github.youngledo.jmcfx.domain.service.EnvironmentService;
import io.github.youngledo.jmcfx.domain.service.EventQueryService;
import io.github.youngledo.jmcfx.domain.service.ExceptionService;
import io.github.youngledo.jmcfx.domain.service.FileIOService;
import io.github.youngledo.jmcfx.domain.service.G1GcService;
import io.github.youngledo.jmcfx.domain.service.HeapService;
import io.github.youngledo.jmcfx.domain.service.JavaAppService;
import io.github.youngledo.jmcfx.domain.service.JavaFxEventService;
import io.github.youngledo.jmcfx.domain.service.JfrMetadataService;
import io.github.youngledo.jmcfx.domain.service.JvmInternalsService;
import io.github.youngledo.jmcfx.domain.service.LeakSuspectsService;
import io.github.youngledo.jmcfx.domain.service.LockService;
import io.github.youngledo.jmcfx.domain.service.ProfilingService;
import io.github.youngledo.jmcfx.domain.service.RecordingRepository;
import io.github.youngledo.jmcfx.domain.service.RuleAnalysisService;
import io.github.youngledo.jmcfx.domain.service.SocketIOService;
import io.github.youngledo.jmcfx.domain.service.ThreadService;
import io.github.youngledo.jmcfx.domain.service.TlabService;
import io.github.youngledo.jmcfx.domain.service.ai.AiCompletionService;
import io.github.youngledo.jmcfx.domain.service.ai.AiSettingsRepository;

public record RecordingApplicationServices(
        RecordingRepository recordingRepository,
        EventQueryService eventQueryService,
        RuleAnalysisService ruleAnalysisService,
        ProfilingService profilingService,
        ExceptionService exceptionService,
        ThreadService threadService,
        FileIOService fileIOService,
        SocketIOService socketIOService,
        LockService lockService,
        HeapService heapService,
        LeakSuspectsService leakSuspectsService,
        TlabService tlabService,
        JvmInternalsService jvmInternalsService,
        EnvironmentService environmentService,
        JavaAppService javaAppService,
        JfrMetadataService jfrMetadataService,
        G1GcService g1GcService,
        JavaFxEventService javaFxEventService,
        AdvancedJfrAnalysisService advancedJfrAnalysisService,
        AiCompletionService aiCompletionService,
        AiSettingsRepository aiSettingsRepository) {

    public RecordingApplicationServices(RecordingRepository recordingRepository,
            EventQueryService eventQueryService,
            RuleAnalysisService ruleAnalysisService,
            ProfilingService profilingService,
            ExceptionService exceptionService,
            ThreadService threadService,
            FileIOService fileIOService,
            SocketIOService socketIOService,
            LockService lockService,
            HeapService heapService,
            LeakSuspectsService leakSuspectsService,
            TlabService tlabService,
            JvmInternalsService jvmInternalsService,
            EnvironmentService environmentService,
            JavaAppService javaAppService,
            JfrMetadataService jfrMetadataService,
            G1GcService g1GcService,
            JavaFxEventService javaFxEventService,
            AdvancedJfrAnalysisService advancedJfrAnalysisService) {
        this(recordingRepository, eventQueryService, ruleAnalysisService, profilingService,
                exceptionService, threadService, fileIOService, socketIOService, lockService,
                heapService, leakSuspectsService, tlabService, jvmInternalsService, environmentService,
                javaAppService, jfrMetadataService, g1GcService, javaFxEventService,
                advancedJfrAnalysisService, null, null);
    }

    public RecordingApplicationServices(RecordingRepository recordingRepository,
            EventQueryService eventQueryService,
            RuleAnalysisService ruleAnalysisService,
            ProfilingService profilingService,
            ExceptionService exceptionService,
            ThreadService threadService,
            FileIOService fileIOService,
            SocketIOService socketIOService,
            LockService lockService,
            HeapService heapService,
            LeakSuspectsService leakSuspectsService,
            TlabService tlabService,
            JvmInternalsService jvmInternalsService,
            EnvironmentService environmentService,
            JavaAppService javaAppService,
            JfrMetadataService jfrMetadataService,
            G1GcService g1GcService,
            JavaFxEventService javaFxEventService,
            AdvancedJfrAnalysisService advancedJfrAnalysisService,
            AiCompletionService aiCompletionService) {
        this(recordingRepository, eventQueryService, ruleAnalysisService, profilingService,
                exceptionService, threadService, fileIOService, socketIOService, lockService,
                heapService, leakSuspectsService, tlabService, jvmInternalsService, environmentService,
                javaAppService, jfrMetadataService, g1GcService, javaFxEventService,
                advancedJfrAnalysisService, aiCompletionService, null);
    }
}
