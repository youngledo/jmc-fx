package io.github.youngledo.jmcfx.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import io.github.youngledo.jmcfx.domain.model.DiagnosticEvidenceLink;
import io.github.youngledo.jmcfx.domain.model.DiagnosticFinding;
import io.github.youngledo.jmcfx.domain.model.DiagnosticFindingSource;
import io.github.youngledo.jmcfx.domain.model.RuleResult;
import io.github.youngledo.jmcfx.domain.model.Severity;
import io.github.youngledo.jmcfx.domain.model.ai.AiEvidence;
import io.github.youngledo.jmcfx.domain.model.ai.AiFinding;
import io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport;
import io.github.youngledo.jmcfx.domain.model.ai.AiSeverity;

public final class DiagnosticFindingsUseCase {

    private static final Comparator<DiagnosticFinding> FINDING_ORDER = Comparator
            .comparingInt((DiagnosticFinding finding) -> severityRank(finding.severity()))
            .reversed()
            .thenComparingInt(finding -> sourceRank(finding.source()))
            .thenComparing(Comparator.comparingInt(DiagnosticFinding::score).reversed())
            .thenComparing(DiagnosticFinding::title, String.CASE_INSENSITIVE_ORDER);

    public List<DiagnosticFinding> compose(List<RuleResult> ruleResults, Optional<AiRecordingReport> aiReport) {
        List<DiagnosticFinding> findings = new ArrayList<>();
        if (ruleResults != null) {
            ruleResults.stream()
                    .map(this::fromRuleResult)
                    .forEach(findings::add);
        }
        Optional<AiRecordingReport> report = aiReport == null ? Optional.empty() : aiReport;
        report.ifPresent(value -> {
            List<AiFinding> aiFindings = value.findings();
            for (int index = 0; index < aiFindings.size(); index++) {
                findings.add(fromAiFinding(index, aiFindings.get(index)));
            }
        });
        findings.sort(FINDING_ORDER);
        return List.copyOf(findings);
    }

    private DiagnosticFinding fromRuleResult(RuleResult result) {
        return new DiagnosticFinding(
                "rule:" + result.id(),
                DiagnosticFindingSource.RULE,
                result.severity(),
                result.name(),
                result.summary(),
                result.explanation(),
                result.recommendation(),
                result.score(),
                List.of(new DiagnosticEvidenceLink(
                        ruleEvidenceLabel(result),
                        result.relatedPageId(),
                        result.id(),
                        result.evidence())));
    }

    private String ruleEvidenceLabel(RuleResult result) {
        return result.relatedPageId().isBlank() ? result.topic() : result.relatedPageId();
    }

    private DiagnosticFinding fromAiFinding(int index, AiFinding finding) {
        return new DiagnosticFinding(
                "ai:" + index,
                DiagnosticFindingSource.AI,
                fromAiSeverity(finding.severity()),
                finding.title(),
                finding.title(),
                finding.limitationsMarkdown(),
                finding.recommendedNextStepMarkdown(),
                (int) Math.round(finding.confidence() * 100),
                aiEvidenceLinks(finding));
    }

    private List<DiagnosticEvidenceLink> aiEvidenceLinks(AiFinding finding) {
        if (!finding.evidence().isEmpty()) {
            return finding.evidence().stream()
                    .map(this::fromAiEvidence)
                    .toList();
        }
        if (!finding.relatedPageId().isBlank()) {
            return List.of(new DiagnosticEvidenceLink(
                    finding.relatedPageId(),
                    finding.relatedPageId(),
                    "",
                    finding.limitationsMarkdown()));
        }
        return List.of();
    }

    private DiagnosticEvidenceLink fromAiEvidence(AiEvidence evidence) {
        return new DiagnosticEvidenceLink(
                evidence.label(),
                evidence.relatedPageId(),
                evidence.relatedEntityId(),
                evidence.value());
    }

    private Severity fromAiSeverity(AiSeverity severity) {
        return switch (severity) {
            case CRITICAL -> Severity.CRITICAL;
            case WARNING -> Severity.WARNING;
            case INFO -> Severity.INFO;
            case UNKNOWN -> Severity.UNKNOWN;
        };
    }

    private static int severityRank(Severity severity) {
        return switch (severity) {
            case CRITICAL -> 6;
            case WARNING -> 5;
            case INFO -> 4;
            case UNKNOWN -> 3;
            case OK -> 2;
            case UNAVAILABLE -> 1;
            case IGNORED -> 0;
        };
    }

    private static int sourceRank(DiagnosticFindingSource source) {
        return switch (source) {
            case RULE -> 0;
            case AI -> 1;
        };
    }
}
