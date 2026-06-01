package com.youngledo.jmcfx.ui.shell;

import com.youngledo.jmcfx.domain.service.AdvancedJfrAnalysisService;
import com.youngledo.jmcfx.domain.service.EnvironmentService;
import com.youngledo.jmcfx.domain.service.EventQueryService;
import com.youngledo.jmcfx.domain.service.ExceptionService;
import com.youngledo.jmcfx.domain.service.FileIOService;
import com.youngledo.jmcfx.domain.service.G1GcService;
import com.youngledo.jmcfx.domain.service.HeapService;
import com.youngledo.jmcfx.domain.service.JavaAppService;
import com.youngledo.jmcfx.domain.service.JavaFxEventService;
import com.youngledo.jmcfx.domain.service.JfrMetadataService;
import com.youngledo.jmcfx.domain.service.JvmInternalsService;
import com.youngledo.jmcfx.domain.service.LeakSuspectsService;
import com.youngledo.jmcfx.domain.service.LockService;
import com.youngledo.jmcfx.domain.service.ProfilingService;
import com.youngledo.jmcfx.domain.service.RecordingRepository;
import com.youngledo.jmcfx.domain.service.RuleAnalysisService;
import com.youngledo.jmcfx.domain.service.SocketIOService;
import com.youngledo.jmcfx.domain.service.ThreadService;
import com.youngledo.jmcfx.domain.service.TlabService;

public record RecordingServices(
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
        AdvancedJfrAnalysisService advancedJfrAnalysisService) {
}
