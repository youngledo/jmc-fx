package io.github.youngledo.jmcfx.domain.model.ai;

import java.util.List;

public record AiRecordingReport(
        String summaryMarkdown,
        List<AiFinding> findings,
        List<String> followUpQuestions,
        List<String> contextLimitations) {

    public AiRecordingReport {
        summaryMarkdown = summaryMarkdown == null ? "" : summaryMarkdown;
        findings = List.copyOf(findings == null ? List.of() : findings);
        followUpQuestions = List.copyOf(followUpQuestions == null ? List.of() : followUpQuestions);
        contextLimitations = List.copyOf(contextLimitations == null ? List.of() : contextLimitations);
    }

    public static AiRecordingReport empty(String summaryMarkdown) {
        return new AiRecordingReport(summaryMarkdown, List.of(), List.of(), List.of());
    }
}
