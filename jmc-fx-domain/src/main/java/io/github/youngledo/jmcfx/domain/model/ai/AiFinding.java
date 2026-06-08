package io.github.youngledo.jmcfx.domain.model.ai;

import java.util.List;

public record AiFinding(
        String title,
        AiSeverity severity,
        double confidence,
        String relatedPageId,
        String recommendedNextStepMarkdown,
        String limitationsMarkdown,
        List<AiEvidence> evidence) {

    public AiFinding {
        title = title == null ? "" : title;
        severity = severity == null ? AiSeverity.UNKNOWN : severity;
        confidence = Math.clamp(confidence, 0.0, 1.0);
        relatedPageId = relatedPageId == null ? "" : relatedPageId;
        recommendedNextStepMarkdown = recommendedNextStepMarkdown == null ? "" : recommendedNextStepMarkdown;
        limitationsMarkdown = limitationsMarkdown == null ? "" : limitationsMarkdown;
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
    }
}
