package io.github.youngledo.jmcfx.domain.model;

import java.util.List;

public record DiagnosticFinding(
        String id,
        DiagnosticFindingSource source,
        Severity severity,
        String title,
        String summary,
        String explanation,
        String recommendedNextAction,
        int score,
        List<DiagnosticEvidenceLink> evidenceLinks) {

    public DiagnosticFinding {
        id = id == null ? "" : id;
        source = source == null ? DiagnosticFindingSource.RULE : source;
        severity = severity == null ? Severity.UNKNOWN : severity;
        title = title == null ? "" : title;
        summary = summary == null ? "" : summary;
        explanation = explanation == null ? "" : explanation;
        recommendedNextAction = recommendedNextAction == null ? "" : recommendedNextAction;
        score = Math.clamp(score, 0, 100);
        evidenceLinks = List.copyOf(evidenceLinks == null ? List.of() : evidenceLinks);
    }

    public DiagnosticEvidenceLink primaryEvidenceLink() {
        return evidenceLinks.stream()
                .filter(DiagnosticEvidenceLink::hasRelatedPage)
                .findFirst()
                .orElse(evidenceLinks.isEmpty()
                        ? new DiagnosticEvidenceLink("", "", "", "")
                        : evidenceLinks.getFirst());
    }
}
