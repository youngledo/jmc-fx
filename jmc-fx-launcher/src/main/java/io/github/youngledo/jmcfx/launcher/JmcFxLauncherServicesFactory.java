package io.github.youngledo.jmcfx.launcher;

import io.github.youngledo.jmcfx.adapter.jmc.JmcAdvancedJfrAnalysisService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcAgentManagerService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcDiagnosticCommandService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcEnvironmentService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcEventQueryService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcExceptionService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcFileIOService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcFlightRecordingService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcG1GcService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcHeapDumpAnalysisService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcHeapService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcJavaAppService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcJavaFxEventService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcJdpDiscoveryService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcJfrMetadataService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcJmxConnectionService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcJmxMonitoringService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcJvmDiscoveryService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcJvmInternalsService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcLeakSuspectsService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcLiveMetricService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcLockService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcMBeanBrowserService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcProfilingService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcRecordingRepository;
import io.github.youngledo.jmcfx.adapter.jmc.JmcRuleAnalysisService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcSocketIOService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcThreadService;
import io.github.youngledo.jmcfx.adapter.jmc.JmcTlabService;
import io.github.youngledo.jmcfx.adapter.preferences.JavaJmxMonitoringRepository;
import io.github.youngledo.jmcfx.adapter.preferences.JavaSavedJvmTargetRepository;
import io.github.youngledo.jmcfx.application.HeapDumpApplicationServices;
import io.github.youngledo.jmcfx.application.LiveJvmApplicationServices;
import io.github.youngledo.jmcfx.application.RecordingApplicationServices;

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
