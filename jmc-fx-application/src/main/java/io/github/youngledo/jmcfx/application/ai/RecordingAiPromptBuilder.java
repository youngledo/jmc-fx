package io.github.youngledo.jmcfx.application.ai;

import java.util.Comparator;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;

final class RecordingAiPromptBuilder {

    private static final int MAX_RULE_RESULTS = 20;

    String build(RecordingSummary recording, List<RuleResult> ruleResults, String languageTag) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert Java performance analyst working with an offline JFR recording.\n");
        prompt.append("Return only JSON matching the requested schema. Use restricted Markdown in markdown fields.\n");
        prompt.append("Do not invent evidence. Use only the context below.\n");
        prompt.append("Response language: ").append(languageTag == null || languageTag.isBlank() ? "en" : languageTag)
                .append("\n\n");
        prompt.append("Recording:\n");
        prompt.append("- id: ").append(recording.id()).append('\n');
        prompt.append("- name: ").append(recording.name()).append('\n');
        prompt.append("- durationMillis: ").append(recording.durationMillis()).append('\n');
        prompt.append("- sizeBytes: ").append(recording.sizeBytes()).append("\n\n");
        prompt.append("Rule results are capped to top ").append(MAX_RULE_RESULTS).append(".\n");
        ruleResults.stream()
                .sorted(Comparator.comparingInt(RuleResult::score).reversed())
                .limit(MAX_RULE_RESULTS)
                .forEach(rule -> appendRule(prompt, rule));
        prompt.append("\nSchema fields: summaryMarkdown, findings, followUpQuestions, contextLimitations.\n");
        prompt.append("Each finding: title, severity, confidence, relatedPageId, recommendedNextStepMarkdown, ")
                .append("limitationsMarkdown, evidence.\n");
        return prompt.toString();
    }

    private static void appendRule(StringBuilder prompt, RuleResult rule) {
        prompt.append("- rule id=").append(rule.id())
                .append(", name=").append(rule.name())
                .append(", severity=").append(rule.severity())
                .append(", score=").append(rule.score())
                .append(", topic=").append(rule.topic())
                .append(", summary=").append(rule.summary())
                .append(", relatedPageId=").append(rule.relatedPageId())
                .append('\n');
    }
}
