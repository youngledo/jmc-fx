package io.github.youngledo.jmcfx.ui.analysis;

import io.github.youngledo.jmcfx.domain.model.DiagnosticEvidenceLink;
import io.github.youngledo.jmcfx.domain.model.DiagnosticFinding;
import io.github.youngledo.jmcfx.ui.util.HtmlToTextFlow;

public record DiagnosticFindingDetail(
        String findingId,
        String title,
        String meta,
        String summary,
        String explanation,
        String evidence,
        String recommendation,
        String relatedPageId) {

    public DiagnosticFindingDetail {
        findingId = findingId == null ? "" : findingId;
        title = title == null ? "" : title;
        meta = meta == null ? "" : meta;
        summary = summary == null ? "" : summary;
        explanation = explanation == null ? "" : explanation;
        evidence = evidence == null ? "" : evidence;
        recommendation = recommendation == null ? "" : recommendation;
        relatedPageId = relatedPageId == null ? "" : relatedPageId;
    }

    public boolean hasRelatedPage() {
        return !relatedPageId.isBlank();
    }

    public static DiagnosticFindingDetail from(DiagnosticFinding finding) {
        if (finding == null) {
            return null;
        }
        DiagnosticEvidenceLink evidence = finding.primaryEvidenceLink();
        return new DiagnosticFindingDetail(
                finding.id(),
                finding.title(),
                "%s | %s | Score %d".formatted(finding.source(), finding.severity(), finding.score()),
                HtmlToTextFlow.toPlainText(finding.summary()),
                HtmlToTextFlow.toPlainText(finding.explanation()),
                HtmlToTextFlow.toPlainText(evidence.description()),
                HtmlToTextFlow.toPlainText(finding.recommendedNextAction()),
                evidence.relatedPageId());
    }
}
