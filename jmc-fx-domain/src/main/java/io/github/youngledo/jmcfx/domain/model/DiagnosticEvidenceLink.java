package io.github.youngledo.jmcfx.domain.model;

public record DiagnosticEvidenceLink(
        String label,
        String relatedPageId,
        String relatedEntityId,
        String description) {

    public DiagnosticEvidenceLink {
        label = label == null ? "" : label;
        relatedPageId = relatedPageId == null ? "" : relatedPageId;
        relatedEntityId = relatedEntityId == null ? "" : relatedEntityId;
        description = description == null ? "" : description;
    }

    public boolean hasRelatedPage() {
        return !relatedPageId.isBlank();
    }
}
