package io.github.youngledo.jmcfx.domain.model;

public record RuleResult(
        String id,
        String name,
        Severity severity,
        int score,
        String topic,
        String summary,
        String explanation,
        String evidence,
        String recommendation,
        String relatedPageId) {

    public RuleResult(String id, String name, Severity severity, int score, String topic, String summary,
            String explanation) {
        this(id, name, severity, score, topic, summary, explanation, "", "", "");
    }

    public RuleResult {
        evidence = evidence == null ? "" : evidence;
        recommendation = recommendation == null ? "" : recommendation;
        relatedPageId = relatedPageId == null ? "" : relatedPageId;
    }
}
