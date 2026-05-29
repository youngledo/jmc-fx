package com.youngledo.jmcfx.domain.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record HeapDumpAnalysisReport(
        Path path,
        long fileSizeBytes,
        long totalObjectSizeBytes,
        long objectCount,
        long instanceCount,
        long objectArrayCount,
        long primitiveArrayCount,
        List<HeapDumpIssue> issues,
        String textReport) {

    public HeapDumpAnalysisReport {
        Objects.requireNonNull(path, "path");
        issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
        fileSizeBytes = Math.max(0, fileSizeBytes);
        totalObjectSizeBytes = Math.max(0, totalObjectSizeBytes);
        objectCount = Math.max(0, objectCount);
        instanceCount = Math.max(0, instanceCount);
        objectArrayCount = Math.max(0, objectArrayCount);
        primitiveArrayCount = Math.max(0, primitiveArrayCount);
        textReport = textReport == null ? "" : textReport;
    }
}
