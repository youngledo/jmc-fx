package com.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record HeapDumpIssue(
        HeapDumpIssueCategory category,
        String subject,
        long wastedBytes,
        long retainedBytes,
        long objectCount,
        double score,
        String evidence,
        String referenceChain) {

    public HeapDumpIssue {
        Objects.requireNonNull(category, "category");
        subject = normalize(subject);
        wastedBytes = Math.max(0, wastedBytes);
        retainedBytes = Math.max(0, retainedBytes);
        objectCount = Math.max(0, objectCount);
        score = Math.clamp(score, 0.0, 1.0);
        evidence = normalize(evidence);
        referenceChain = normalize(referenceChain);
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }
}
