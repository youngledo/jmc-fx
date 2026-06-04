package com.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.NativeLibraryEntry;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.ThreadDumpEntry;
import com.youngledo.jmcfx.domain.model.ThreadHistogramRow;
import com.youngledo.jmcfx.domain.model.X509CertificateEntry;
import com.youngledo.jmcfx.domain.service.JavaAppService;

public final class LoadJavaApplicationUseCase {

    private final JavaAppService service;

    public LoadJavaApplicationUseCase(JavaAppService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public List<ThreadHistogramRow> loadThreadHistogram(RecordingSummary recording) {
        return service.loadThreadHistogram(recording);
    }

    public ChartDefinition loadOverviewChart(RecordingSummary recording) {
        return service.loadOverviewChart(recording);
    }

    public List<X509CertificateEntry> loadCertificates(RecordingSummary recording) {
        return service.loadCertificates(recording);
    }

    public List<NativeLibraryEntry> loadNativeLibraries(RecordingSummary recording) {
        return service.loadNativeLibraries(recording);
    }

    public List<ThreadDumpEntry> loadThreadDumps(RecordingSummary recording) {
        return service.loadThreadDumps(recording);
    }
}
