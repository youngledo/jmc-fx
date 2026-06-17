package io.github.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class DiagnosticFindingTest {

    @Test
    void normalizesNullTextAndCopiesEvidenceLinks() {
        DiagnosticEvidenceLink link = new DiagnosticEvidenceLink("Events", "events", "event-1", null);
        DiagnosticFinding finding = new DiagnosticFinding(
                "rule.gc.pause",
                DiagnosticFindingSource.RULE,
                Severity.WARNING,
                null,
                null,
                null,
                null,
                87,
                List.of(link));

        assertEquals("", finding.title());
        assertEquals("", finding.summary());
        assertEquals("", finding.explanation());
        assertEquals("", finding.recommendedNextAction());
        assertEquals(1, finding.evidenceLinks().size());
        assertEquals("", finding.evidenceLinks().getFirst().description());
        assertThrows(UnsupportedOperationException.class,
                () -> finding.evidenceLinks().add(link));
    }

    @Test
    void normalizesNullSourceAndSeverity() {
        DiagnosticFinding finding = new DiagnosticFinding(
                null,
                null,
                null,
                "Finding",
                "Summary",
                "Explanation",
                "Next",
                -1,
                null);

        assertEquals("", finding.id());
        assertEquals(DiagnosticFindingSource.RULE, finding.source());
        assertEquals(Severity.UNKNOWN, finding.severity());
        assertEquals(0, finding.score());
        assertEquals(List.of(), finding.evidenceLinks());
    }

    @Test
    void clampsScoreToHundred() {
        DiagnosticFinding finding = new DiagnosticFinding(
                "ai.0",
                DiagnosticFindingSource.AI,
                Severity.CRITICAL,
                "Finding",
                "Summary",
                "Explanation",
                "Next",
                150,
                List.of());

        assertEquals(100, finding.score());
    }
}
