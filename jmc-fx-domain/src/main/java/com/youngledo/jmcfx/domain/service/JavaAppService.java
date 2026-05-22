package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.NativeLibraryEntry;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.ThreadDumpEntry;
import com.youngledo.jmcfx.domain.model.ThreadHistogramRow;
import com.youngledo.jmcfx.domain.model.X509CertificateEntry;

/// Port for Java Application remaining pages.
///
/// Provides thread histogram aggregation, certificate, native library,
/// and thread dump data extracted from JFR events.
public interface JavaAppService {

    /// Loads the per-thread histogram with aggregated metrics.
    ///
    /// @param recording the flight recording to analyze
    /// @return list of thread histogram rows ordered by profiling count descending
    List<ThreadHistogramRow> loadThreadHistogram(RecordingSummary recording);

    /// Loads the XY chart definition for overlay visualization.
    ///
    /// Returns a chart with multiple series: profiling samples, IO duration,
    /// blocked duration, allocation, and exceptions over time.
    ///
    /// @param recording the flight recording to analyze
    /// @return chart definition with switchable overlay series
    ChartDefinition loadOverviewChart(RecordingSummary recording);

    /// Loads X.509 certificate events from jdk.X509Certificate.
    ///
    /// @param recording the flight recording to analyze
    /// @return list of certificate entries ordered by start time
    List<X509CertificateEntry> loadCertificates(RecordingSummary recording);

    /// Loads native library events from jdk.NativeLibrary.
    ///
    /// @param recording the flight recording to analyze
    /// @return list of native library entries ordered by start time
    List<NativeLibraryEntry> loadNativeLibraries(RecordingSummary recording);

    /// Loads thread dump events from jdk.ThreadDump.
    ///
    /// @param recording the flight recording to analyze
    /// @return list of thread dump entries ordered by start time
    List<ThreadDumpEntry> loadThreadDumps(RecordingSummary recording);
}
