package com.youngledo.jmcfx.testsupport;

import java.util.ArrayList;
import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.NativeLibraryEntry;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.ThreadDumpEntry;
import com.youngledo.jmcfx.domain.model.ThreadHistogramRow;
import com.youngledo.jmcfx.domain.model.X509CertificateEntry;
import com.youngledo.jmcfx.domain.service.JavaAppService;

/// Fake implementation of {@link JavaAppService} for testing.
///
/// Pre-populated with test data via add* methods. All queries return the same
/// data regardless of the recording argument.
public class FakeJavaAppService implements JavaAppService {

    private final List<ThreadHistogramRow> histogramRows = new ArrayList<>();
    private final List<X509CertificateEntry> certificates = new ArrayList<>();
    private final List<NativeLibraryEntry> nativeLibraries = new ArrayList<>();
    private final List<ThreadDumpEntry> threadDumps = new ArrayList<>();

    public void addHistogramRow(ThreadHistogramRow row) {
        histogramRows.add(row);
    }

    public void addCertificate(X509CertificateEntry entry) {
        certificates.add(entry);
    }

    public void addNativeLibrary(NativeLibraryEntry entry) {
        nativeLibraries.add(entry);
    }

    public void addThreadDump(ThreadDumpEntry entry) {
        threadDumps.add(entry);
    }

    @Override
    public List<ThreadHistogramRow> loadThreadHistogram(RecordingSummary recording) {
        return List.copyOf(histogramRows);
    }

    @Override
    public ChartDefinition loadOverviewChart(RecordingSummary recording) {
        return new ChartDefinition("Time", "Value", List.of());
    }

    @Override
    public List<X509CertificateEntry> loadCertificates(RecordingSummary recording) {
        return List.copyOf(certificates);
    }

    @Override
    public List<NativeLibraryEntry> loadNativeLibraries(RecordingSummary recording) {
        return List.copyOf(nativeLibraries);
    }

    @Override
    public List<ThreadDumpEntry> loadThreadDumps(RecordingSummary recording) {
        return List.copyOf(threadDumps);
    }
}
