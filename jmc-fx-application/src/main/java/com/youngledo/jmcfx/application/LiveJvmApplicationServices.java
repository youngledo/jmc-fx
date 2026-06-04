package com.youngledo.jmcfx.application;

import com.youngledo.jmcfx.domain.service.DiagnosticCommandService;
import com.youngledo.jmcfx.domain.service.FlightRecordingService;
import com.youngledo.jmcfx.domain.service.JdpDiscoveryService;
import com.youngledo.jmcfx.domain.service.JmcAgentService;
import com.youngledo.jmcfx.domain.service.JmxConnectionService;
import com.youngledo.jmcfx.domain.service.JmxMonitoringRepository;
import com.youngledo.jmcfx.domain.service.JmxMonitoringService;
import com.youngledo.jmcfx.domain.service.JvmDiscoveryService;
import com.youngledo.jmcfx.domain.service.LiveMetricService;
import com.youngledo.jmcfx.domain.service.MBeanBrowserService;
import com.youngledo.jmcfx.domain.service.SavedJvmTargetRepository;

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
