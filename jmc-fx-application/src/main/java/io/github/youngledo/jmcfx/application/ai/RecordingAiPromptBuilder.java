package io.github.youngledo.jmcfx.application.ai;

import java.util.Comparator;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;

final class RecordingAiPromptBuilder {

    private static final int MAX_RULE_RESULTS = 20;
    private static final int MAX_PROMPT_CHARS = 60_000;
    private static final int PROMPT_TAIL_RESERVED_CHARS = 2_000;
    private static final int MAX_CONTEXT_ROW_CHARS = 500;

    String build(RecordingAiContext context, String languageTag) {
        return build(context.recording(), context.ruleResults(), context.sections(), context.limitations(), languageTag);
    }

    String build(RecordingSummary recording, List<RuleResult> ruleResults, String languageTag) {
        return build(recording, ruleResults, List.of(), List.of(), languageTag);
    }

    private String build(RecordingSummary recording, List<RuleResult> ruleResults,
            List<RecordingAiContextSection> sections, List<String> limitations, String languageTag) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert Java performance analyst working with an offline JFR recording.\n");
        prompt.append("Do not invent evidence. Use only the context below.\n");
        prompt.append("Stream a user-visible Markdown report first. Do not show raw JSON in that Markdown.\n");
        prompt.append("Finish with one fenced machine-readable JSON block using this exact fence:\n");
        prompt.append("```jmcfx-report-json\n");
        prompt.append("{...}\n");
        prompt.append("```\n");
        prompt.append("Do not output anything after the closing fence. The JSON block must match the requested schema.\n");
        prompt.append("Use restricted Markdown in markdown fields.\n");
        prompt.append("Response language: ").append(languageTag == null || languageTag.isBlank() ? "en" : languageTag)
                .append('\n');
        prompt.append("Every user-visible text field must be written in the response language, including ")
                .append("summaryMarkdown, finding title, recommendedNextStepMarkdown, limitationsMarkdown, ")
                .append("followUpQuestions, contextLimitations, evidence label, and evidence value.\n");
        prompt.append("Do not localize machine-readable fields: severity must be exactly one of info, warning, ")
                .append("or critical; confidence must be a number between 0.0 and 1.0; relatedPageId must stay ")
                .append("as the page id from the context.\n");
        prompt.append("The deterministic context below may be in English. Do not copy context limitation text verbatim ")
                .append("when the response language is not English; translate or summarize it in the response language.\n\n");
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
        boolean contextCapped = appendSections(prompt, sections);
        if (contextCapped) {
            prompt.append("\nContext budget limitation:\n");
            prompt.append("- Additional deterministic context was capped to keep the prompt below ")
                    .append(MAX_PROMPT_CHARS)
                    .append(" characters. Some lower-priority rows or sections were omitted.\n");
        }
        appendLimitations(prompt, limitations);
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

    private static boolean appendSections(StringBuilder prompt, List<RecordingAiContextSection> sections) {
        if (sections == null || sections.isEmpty()) {
            return false;
        }
        boolean capped = false;
        prompt.append("\nAdditional deterministic context:\n");
        for (RecordingAiContextSection section : sections) {
            String header = section.title() + " (" + section.rows().size() + " of " + section.totalCount() + "):\n";
            if (!appendWithinBudget(prompt, header)) {
                return true;
            }
            for (String row : section.rows()) {
                String line = "- " + truncate(row, MAX_CONTEXT_ROW_CHARS) + '\n';
                if (!appendWithinBudget(prompt, line)) {
                    return true;
                }
                capped = capped || row.length() > MAX_CONTEXT_ROW_CHARS;
            }
        }
        return capped;
    }

    private static void appendLimitations(StringBuilder prompt, List<String> limitations) {
        if (limitations == null || limitations.isEmpty()) {
            return;
        }
        prompt.append("\nContext limitations:\n");
        for (String limitation : limitations) {
            prompt.append("- ").append(limitation).append('\n');
        }
    }

    private static boolean appendWithinBudget(StringBuilder prompt, String value) {
        if (prompt.length() + value.length() > MAX_PROMPT_CHARS - PROMPT_TAIL_RESERVED_CHARS) {
            return false;
        }
        prompt.append(value);
        return true;
    }

    private static String truncate(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(0, maxChars - 15)) + "... [truncated]";
    }
}
