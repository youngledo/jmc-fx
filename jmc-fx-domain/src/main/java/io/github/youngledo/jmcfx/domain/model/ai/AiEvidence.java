package io.github.youngledo.jmcfx.domain.model.ai;

public record AiEvidence(
        String label,
        String value,
        String source,
        String relatedPageId,
        String relatedEntityId) {

    public AiEvidence {
        label = label == null ? "" : label;
        value = value == null ? "" : value;
        source = source == null ? "" : source;
        relatedPageId = relatedPageId == null ? "" : relatedPageId;
        relatedEntityId = relatedEntityId == null ? "" : relatedEntityId;
    }
}
