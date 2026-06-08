package io.github.youngledo.jmcfx.domain.model.ai;

import java.util.List;

public record AiAssistantAnswer(
        String answerMarkdown,
        List<String> followUpQuestions) {

    public AiAssistantAnswer {
        answerMarkdown = answerMarkdown == null ? "" : answerMarkdown;
        followUpQuestions = List.copyOf(followUpQuestions == null ? List.of() : followUpQuestions);
    }
}
