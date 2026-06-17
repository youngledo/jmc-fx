package io.github.youngledo.jmcfx.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import io.github.youngledo.jmcfx.domain.model.DiagnosticFinding;
import io.github.youngledo.jmcfx.domain.model.DiagnosticFindingSource;
import io.github.youngledo.jmcfx.domain.model.RuleResult;
import io.github.youngledo.jmcfx.domain.model.Severity;
import io.github.youngledo.jmcfx.domain.model.ai.AiEvidence;
import io.github.youngledo.jmcfx.domain.model.ai.AiFinding;
import io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport;
import io.github.youngledo.jmcfx.domain.model.ai.AiSeverity;
import org.junit.jupiter.api.Test;

class DiagnosticFindingsUseCaseTest {

    private final DiagnosticFindingsUseCase useCase = new DiagnosticFindingsUseCase();

    @Test
    void mapsRuleResultsToDiagnosticFindings() {
        RuleResult result = new RuleResult(
                "rule.gc.pause",
                "Long GC Pause",
                Severity.WARNING,
                80,
                "Garbage Collection",
                "Pause time is elevated.",
                "Several pauses exceeded the target.",
                "GC pause event at 12s.",
                "Inspect GC details.",
                "gcDetails");

        List<DiagnosticFinding> findings = useCase.compose(List.of(result), Optional.empty());

        assertEquals(1, findings.size());
        DiagnosticFinding finding = findings.getFirst();
        assertEquals("rule:rule.gc.pause", finding.id());
        assertEquals(DiagnosticFindingSource.RULE, finding.source());
        assertEquals(Severity.WARNING, finding.severity());
        assertEquals("Long GC Pause", finding.title());
        assertEquals("Pause time is elevated.", finding.summary());
        assertEquals("Several pauses exceeded the target.", finding.explanation());
        assertEquals("Inspect GC details.", finding.recommendedNextAction());
        assertEquals(80, finding.score());
        assertEquals("gcDetails", finding.primaryEvidenceLink().relatedPageId());
        assertEquals("GC pause event at 12s.", finding.primaryEvidenceLink().description());
    }

    @Test
    void mapsAiFindingsWithoutReplacingRuleFindings() {
        RuleResult result = new RuleResult(
                "rule.exceptions",
                "Exception Count",
                Severity.INFO,
                20,
                "Exceptions",
                "Exceptions were recorded.",
                "Some exceptions appear repeatedly.",
                "",
                "Inspect exception groups.",
                "exceptions");
        AiFinding aiFinding = new AiFinding(
                "Possible exception spike",
                AiSeverity.CRITICAL,
                0.92,
                "exceptions",
                "Open the Exceptions page.",
                "Context is summary-only.",
                List.of(new AiEvidence("Exception group", "java.lang.IllegalStateException", "AI", "exceptions", "")));
        AiRecordingReport report = new AiRecordingReport("Summary", List.of(aiFinding), List.of(), List.of());

        List<DiagnosticFinding> findings = useCase.compose(List.of(result), Optional.of(report));

        assertEquals(2, findings.size());
        assertEquals(DiagnosticFindingSource.AI, findings.getFirst().source());
        assertEquals(Severity.CRITICAL, findings.getFirst().severity());
        assertEquals(92, findings.getFirst().score());
        assertEquals("Open the Exceptions page.", findings.getFirst().recommendedNextAction());
        assertEquals("exceptions", findings.getFirst().primaryEvidenceLink().relatedPageId());
        assertTrue(findings.stream().anyMatch(f -> f.source() == DiagnosticFindingSource.RULE));
    }

    @Test
    void keepsDeterministicRuleFindingsWhenAiReportIsUnavailable() {
        RuleResult warning = new RuleResult(
                "warning",
                "Warning",
                Severity.WARNING,
                60,
                "Topic",
                "Warning summary",
                "",
                "",
                "",
                "");
        RuleResult critical = new RuleResult(
                "critical",
                "Critical",
                Severity.CRITICAL,
                40,
                "Topic",
                "Critical summary",
                "",
                "",
                "",
                "");
        RuleResult info = new RuleResult(
                "info",
                "Info",
                Severity.INFO,
                95,
                "Topic",
                "Info summary",
                "",
                "",
                "",
                "");

        List<DiagnosticFinding> findings = useCase.compose(List.of(warning, critical, info), Optional.empty());

        assertEquals(3, findings.size());
        assertTrue(findings.stream().allMatch(finding -> finding.source() == DiagnosticFindingSource.RULE));
        assertEquals("rule:critical", findings.get(0).id());
        assertEquals("rule:warning", findings.get(1).id());
        assertEquals("rule:info", findings.get(2).id());
    }

    @Test
    void sortsBySeverityThenSourceThenScore() {
        RuleResult infoHigh = new RuleResult("info", "Info", Severity.INFO, 99, "Topic", "Info", "", "", "", "");
        RuleResult warningLow = new RuleResult("warning", "Warning", Severity.WARNING, 10, "Topic", "Warning", "", "", "", "");
        AiFinding aiWarningHigh = new AiFinding("AI warning", AiSeverity.WARNING, 0.95, "", "", "", List.of());
        AiRecordingReport report = new AiRecordingReport("", List.of(aiWarningHigh), List.of(), List.of());

        List<DiagnosticFinding> findings = useCase.compose(List.of(infoHigh, warningLow), Optional.of(report));

        assertEquals("rule:warning", findings.get(0).id());
        assertEquals("ai:0", findings.get(1).id());
        assertEquals("rule:info", findings.get(2).id());
    }

    @Test
    void keepsRuleFindingsAheadOfAiFindingsWhenSeverityAndScoreTie() {
        RuleResult ruleWarning = new RuleResult(
                "warning",
                "Rule warning",
                Severity.WARNING,
                80,
                "Topic",
                "Rule warning",
                "",
                "",
                "",
                "");
        AiFinding aiWarning = new AiFinding("AI warning", AiSeverity.WARNING, 0.80, "", "", "", List.of());
        AiRecordingReport report = new AiRecordingReport("", List.of(aiWarning), List.of(), List.of());

        List<DiagnosticFinding> findings = useCase.compose(List.of(ruleWarning), Optional.of(report));

        assertEquals("rule:warning", findings.getFirst().id());
        assertEquals("ai:0", findings.get(1).id());
    }
}
