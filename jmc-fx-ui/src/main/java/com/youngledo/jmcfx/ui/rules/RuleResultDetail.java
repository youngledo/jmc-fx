package com.youngledo.jmcfx.ui.rules;

import com.youngledo.jmcfx.domain.model.RuleResult;
import com.youngledo.jmcfx.ui.util.HtmlToTextFlow;

public record RuleResultDetail(
        String resultId,
        String title,
        String meta,
        String summary,
        String explanation,
        String evidence,
        String recommendation,
        String relatedPageId) {

    public RuleResultDetail {
        resultId = resultId == null ? "" : resultId;
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

    public static RuleResultDetail from(RuleResult result) {
        if (result == null) {
            return null;
        }
        return new RuleResultDetail(
                result.id(),
                result.name(),
                "%s | Score %d | %s".formatted(result.severity(), result.score(), result.topic()),
                HtmlToTextFlow.toPlainText(result.summary()),
                HtmlToTextFlow.toPlainText(result.explanation()),
                HtmlToTextFlow.toPlainText(result.evidence()),
                HtmlToTextFlow.toPlainText(result.recommendation()),
                result.relatedPageId());
    }
}
