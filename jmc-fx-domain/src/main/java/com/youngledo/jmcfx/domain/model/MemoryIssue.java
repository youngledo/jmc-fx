package com.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record MemoryIssue(
        MemoryIssueCategory category,
        MemoryIssueSeverity severity,
        String subject,
        long estimatedBytes,
        long count,
        double score,
        String evidence,
        String recommendation) {

    public MemoryIssue {
        category = Objects.requireNonNull(category, "category");
        severity = Objects.requireNonNull(severity, "severity");
        subject = Objects.requireNonNullElse(subject, "");
        estimatedBytes = Math.max(0, estimatedBytes);
        count = Math.max(0, count);
        score = Math.clamp(score, 0.0, 100.0);
        evidence = Objects.requireNonNullElse(evidence, "");
        recommendation = Objects.requireNonNullElse(recommendation, "");
    }
}
