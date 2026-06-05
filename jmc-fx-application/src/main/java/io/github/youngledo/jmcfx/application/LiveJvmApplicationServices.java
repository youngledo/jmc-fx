package io.github.youngledo.jmcfx.application;

import io.github.youngledo.jmcfx.domain.service.DiagnosticCommandService;
import io.github.youngledo.jmcfx.domain.service.FlightRecordingService;
import io.github.youngledo.jmcfx.domain.service.JdpDiscoveryService;
import io.github.youngledo.jmcfx.domain.service.JmcAgentService;
import io.github.youngledo.jmcfx.domain.service.JmxConnectionService;
import io.github.youngledo.jmcfx.domain.service.JmxMonitoringRepository;
import io.github.youngledo.jmcfx.domain.service.JmxMonitoringService;
import io.github.youngledo.jmcfx.domain.service.JvmDiscoveryService;
import io.github.youngledo.jmcfx.domain.service.LiveMetricService;
import io.github.youngledo.jmcfx.domain.service.MBeanBrowserService;
import io.github.youngledo.jmcfx.domain.service.SavedJvmTargetRepository;

public record LiveJvmApplicationServices(
        JvmDiscoveryService jvmDiscoveryService,
        JmxConnectionService jmxConnectionService,
        FlightRecordingService flightRecordingService,
        MBeanBrowserService mBeanBrowserService,
        DiagnosticCommandService diagnosticCommandService,
        LiveMetricService liveMetricService,
        JmcAgentService jmcAgentService,
        JmxMonitoringService jmxMonitoringService,
        JmxMonitoringRepository jmxMonitoringRepository,
        SavedJvmTargetRepository savedTargetRepository,
        JdpDiscoveryService jdpDiscoveryService) {
}
