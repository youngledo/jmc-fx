package com.youngledo.jmcfx.launcher;

import com.youngledo.jmcfx.adapter.jmc.JmcAdvancedJfrAnalysisService;
import com.youngledo.jmcfx.adapter.jmc.JmcAgentManagerService;
import com.youngledo.jmcfx.adapter.jmc.JmcDiagnosticCommandService;
import com.youngledo.jmcfx.adapter.jmc.JmcEnvironmentService;
import com.youngledo.jmcfx.adapter.jmc.JmcEventQueryService;
import com.youngledo.jmcfx.adapter.jmc.JmcExceptionService;
import com.youngledo.jmcfx.adapter.jmc.JmcFileIOService;
import com.youngledo.jmcfx.adapter.jmc.JmcFlightRecordingService;
import com.youngledo.jmcfx.adapter.jmc.JmcG1GcService;
import com.youngledo.jmcfx.adapter.jmc.JmcHeapDumpAnalysisService;
import com.youngledo.jmcfx.adapter.jmc.JmcHeapService;
import com.youngledo.jmcfx.adapter.jmc.JmcJavaAppService;
import com.youngledo.jmcfx.adapter.jmc.JmcJavaFxEventService;
import com.youngledo.jmcfx.adapter.jmc.JmcJdpDiscoveryService;
import com.youngledo.jmcfx.adapter.jmc.JmcJfrMetadataService;
import com.youngledo.jmcfx.adapter.jmc.JmcJmxConnectionService;
import com.youngledo.jmcfx.adapter.jmc.JmcJmxMonitoringService;
import com.youngledo.jmcfx.adapter.jmc.JmcJvmDiscoveryService;
import com.youngledo.jmcfx.adapter.jmc.JmcJvmInternalsService;
import com.youngledo.jmcfx.adapter.jmc.JmcLeakSuspectsService;
import com.youngledo.jmcfx.adapter.jmc.JmcLiveMetricService;
import com.youngledo.jmcfx.adapter.jmc.JmcLockService;
import com.youngledo.jmcfx.adapter.jmc.JmcMBeanBrowserService;
import com.youngledo.jmcfx.adapter.jmc.JmcProfilingService;
import com.youngledo.jmcfx.adapter.jmc.JmcRecordingRepository;
import com.youngledo.jmcfx.adapter.jmc.JmcRuleAnalysisService;
import com.youngledo.jmcfx.adapter.jmc.JmcSocketIOService;
import com.youngledo.jmcfx.adapter.jmc.JmcThreadService;
import com.youngledo.jmcfx.adapter.jmc.JmcTlabService;
import com.youngledo.jmcfx.application.HeapDumpApplicationServices;
import com.youngledo.jmcfx.application.LiveJvmApplicationServices;
import com.youngledo.jmcfx.application.RecordingApplicationServices;
import com.youngledo.jmcfx.ui.preferences.JavaJmxMonitoringRepository;
import com.youngledo.jmcfx.ui.preferences.JavaSavedJvmTargetRepository;

final class JmcFxLauncherServicesFactory {

    JmcFxLauncherServices create() {
        JmcJmxConnectionService jmxConnectionService = new JmcJmxConnectionService();
        return new JmcFxLauncherServices(recordingServices(), liveJvmServices(jmxConnectionService), heapDumpServices());
    }

    private RecordingApplicationServices recordingServices() {
        return new RecordingApplicationServices(new JmcRecordingRepository(),
                new JmcEventQueryService(), new JmcRuleAnalysisService(), new JmcProfilingService(),
                new JmcExceptionService(), new JmcThreadService(), new JmcFileIOService(), new JmcSocketIOService(),
                new JmcLockService(), new JmcHeapService(), new JmcLeakSuspectsService(), new JmcTlabService(),
                new JmcJvmInternalsService(), new JmcEnvironmentService(), new JmcJavaAppService(),
                new JmcJfrMetadataService(), new JmcG1GcService(), new JmcJavaFxEventService(),
                new JmcAdvancedJfrAnalysisService());
    }

    private LiveJvmApplicationServices liveJvmServices(JmcJmxConnectionService jmxConnectionService) {
        return new LiveJvmApplicationServices(new JmcJvmDiscoveryService(), jmxConnectionService,
                new JmcFlightRecordingService(jmxConnectionService), new JmcMBeanBrowserService(jmxConnectionService),
                new JmcDiagnosticCommandService(jmxConnectionService), new JmcLiveMetricService(jmxConnectionService),
                new JmcAgentManagerService(jmxConnectionService), new JmcJmxMonitoringService(jmxConnectionService),
                new JavaJmxMonitoringRepository(), new JavaSavedJvmTargetRepository(), new JmcJdpDiscoveryService());
    }

    private HeapDumpApplicationServices heapDumpServices() {
        return new HeapDumpApplicationServices(new JmcHeapDumpAnalysisService());
    }
}
