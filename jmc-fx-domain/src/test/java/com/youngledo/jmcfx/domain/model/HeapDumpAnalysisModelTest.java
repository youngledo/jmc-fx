package com.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class HeapDumpAnalysisModelTest {

    @Test
    void reportCopiesIssues() {
        List<HeapDumpIssue> issues = new ArrayList<>();
        issues.add(sampleIssue("java.util.HashMap"));
        HeapDumpAnalysisReport report = new HeapDumpAnalysisReport(Path.of("dump.hprof"),
                100, 200, 3, 2, 1, 0, issues, "report");

        issues.clear();

        assertEquals(1, report.issues().size());
        assertThrows(UnsupportedOperationException.class, () -> report.issues().add(sampleIssue("x")));
    }

    @Test
    void issueNormalizesNullStrings() {
        HeapDumpIssue issue = new HeapDumpIssue(HeapDumpIssueCategory.DUPLICATE_STRING,
                null, 1, 2, 3, 0.4, null, null);

        assertEquals("", issue.subject());
        assertEquals("", issue.evidence());
        assertEquals("", issue.referenceChain());
    }

    @Test
    void reportRejectsNullPathAndIssues() {
        assertThrows(NullPointerException.class, () -> new HeapDumpAnalysisReport(null,
                1, 1, 1, 1, 1, 1, List.of(), ""));
        assertThrows(NullPointerException.class, () -> new HeapDumpAnalysisReport(Path.of("dump.hprof"),
                1, 1, 1, 1, 1, 1, null, ""));
    }

    private static HeapDumpIssue sampleIssue(String subject) {
        return new HeapDumpIssue(HeapDumpIssueCategory.COLLECTION_OVERHEAD,
                subject, 100, 120, 2, 0.5, "empty collection", "root -> field");
    }
}
