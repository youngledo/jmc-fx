package io.github.youngledo.jmcfx.application;

import java.nio.file.Path;
import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.HeapDumpAnalysisReport;

public final class AnalyzeHeapDumpUseCase {

    private final HeapDumpApplicationServices services;

    public AnalyzeHeapDumpUseCase(HeapDumpApplicationServices services) {
        this.services = Objects.requireNonNull(services, "services");
    }

    public HeapDumpAnalysisReport analyze(Path path) {
        Objects.requireNonNull(path, "path");
        return services.heapDumpAnalysisService().analyze(path);
    }
}
