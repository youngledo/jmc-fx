package io.github.youngledo.jmcfx.application.ai;

import java.util.List;
import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.ai.AiAssistantAnswer;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionRequest;
import io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport;
import io.github.youngledo.jmcfx.domain.service.ai.AiCompletionService;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;

public final class AskRecordingAssistantUseCase {

    private final AiCompletionService completionService;
    private final AiReportJsonParser parser = new AiReportJsonParser();

    public AskRecordingAssistantUseCase(AiCompletionService completionService) {
        this.completionService = Objects.requireNonNull(completionService, "completionService");
    }

    public AiAssistantAnswer ask(RecordingAiContext context, AiRecordingReport report, String question,
            String languageTag) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(report, "report");
        String prompt = """
                Answer the user's follow-up question using only the existing recording AI report and context.
                Return JSON with fields answerMarkdown and followUpQuestions.
                Language: %s
                Question: %s
                Report summary: %s
                Context:
                %s
                Context limitations: %s
                """.formatted(languageTag == null || languageTag.isBlank() ? "en" : languageTag,
                question == null ? "" : question,
                report.summaryMarkdown(),
                contextText(context),
                context.limitations());
        var response = completionService.complete(new AiCompletionRequest(context.recording(), languageTag, prompt));
        try {
            return parser.parseAnswer(response.text());
        } catch (IllegalArgumentException e) {
            throw new JmcFxException("AI returned an invalid assistant answer.", e);
        }
    }

    private static String contextText(RecordingAiContext context) {
        StringBuilder text = new StringBuilder();
        text.append("Rule results: ").append(context.ruleResults().size()).append('\n');
        for (RecordingAiContextSection section : context.sections()) {
            text.append(section.title()).append(":\n");
            for (String row : section.rows()) {
                text.append("- ").append(row).append('\n');
            }
        }
        return text.toString();
    }
}
