package com.youngledo.jmcfx.domain.model;

import java.util.List;
import java.util.Objects;

public record MemoryAnalysisReport(
        long totalEstimatedBytes,
        long totalCount,
        List<MemoryIssue> issues) {

    public MemoryAnalysisReport {
        totalEstimatedBytes = Math.max(0, totalEstimatedBytes);
        totalCount = Math.max(0, totalCount);
        issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
    }

    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    public MemoryIssue topIssue() {
        return hasIssues() ? issues.getFirst() : null;
    }
}
