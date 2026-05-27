package com.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class MemoryAnalysisModelTest {

    @Test
    void issueNormalizesStringsAndClampsNumbers() {
        MemoryIssue issue = new MemoryIssue(
                MemoryIssueCategory.ALLOCATION_HOTSPOT,
                MemoryIssueSeverity.WARNING,
                null,
                -1,
                -2,
                120.5,
                null,
                null);

        assertEquals(MemoryIssueCategory.ALLOCATION_HOTSPOT, issue.category());
        assertEquals(MemoryIssueSeverity.WARNING, issue.severity());
        assertEquals("", issue.subject());
        assertEquals(0, issue.estimatedBytes());
        assertEquals(0, issue.count());
        assertEquals(100.0, issue.score());
        assertEquals("", issue.evidence());
        assertEquals("", issue.recommendation());
    }

    @Test
    void issueClampsScoreFloor() {
        MemoryIssue issue = new MemoryIssue(
                MemoryIssueCategory.OUTSIDE_TLAB,
                MemoryIssueSeverity.INFO,
                "java.lang.String",
                42,
                3,
                -0.1,
                "allocation pressure",
                "inspect allocation stack");

        assertEquals(0.0, issue.score());
    }

    @Test
    void issueRejectsNullCategoryAndSeverity() {
        assertThrows(NullPointerException.class, () -> new MemoryIssue(
                null,
                MemoryIssueSeverity.CRITICAL,
                "subject",
                1,
                1,
                1.0,
                "evidence",
                "recommendation"));
        assertThrows(NullPointerException.class, () -> new MemoryIssue(
                MemoryIssueCategory.RETAINED_OBJECT,
                null,
                "subject",
                1,
                1,
                1.0,
                "evidence",
                "recommendation"));
    }

    @Test
    void reportClampsTotalsRejectsNullListAndCopiesIssues() {
        MemoryIssue issue = sampleIssue("first");
        List<MemoryIssue> issues = new ArrayList<>();
        issues.add(issue);

        MemoryAnalysisReport report = new MemoryAnalysisReport(-10, -20, issues);
        issues.clear();

        assertEquals(0, report.totalEstimatedBytes());
        assertEquals(0, report.totalCount());
        assertEquals(List.of(issue), report.issues());
        assertThrows(UnsupportedOperationException.class, () -> report.issues().clear());
        assertThrows(NullPointerException.class, () -> new MemoryAnalysisReport(1, 1, null));
    }

    @Test
    void emptyReportHasNoTopIssue() {
        MemoryAnalysisReport report = new MemoryAnalysisReport(0, 0, List.of());

        assertFalse(report.hasIssues());
        assertNull(report.topIssue());
    }

    @Test
    void nonEmptyReportHasFirstTopIssue() {
        MemoryIssue first = sampleIssue("first");
        MemoryIssue second = sampleIssue("second");
        MemoryAnalysisReport report = new MemoryAnalysisReport(10, 2, List.of(first, second));

        assertTrue(report.hasIssues());
        assertSame(first, report.topIssue());
    }

    private static MemoryIssue sampleIssue(String subject) {
        return new MemoryIssue(
                MemoryIssueCategory.ALLOCATION_HOTSPOT,
                MemoryIssueSeverity.WARNING,
                subject,
                10,
                1,
                25.0,
                "evidence",
                "recommendation");
    }
}
