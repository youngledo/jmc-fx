package com.youngledo.jmcfx.application;

import java.nio.file.Path;
import java.util.Objects;

public record HeapDumpWorkspacePlan(Path path, AnalyzeHeapDumpUseCase analyzeHeapDump) {

    public HeapDumpWorkspacePlan {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(analyzeHeapDump, "analyzeHeapDump");
    }
}
