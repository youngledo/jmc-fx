package io.github.youngledo.jmcfx.application.ai;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;

public record RecordingAiContext(
        RecordingSummary recording,
        List<RuleResult> ruleResults,
        List<String> limitations) {

    public RecordingAiContext {
        if (recording == null) {
            throw new NullPointerException("recording");
        }
        ruleResults = List.copyOf(ruleResults == null ? List.of() : ruleResults);
        limitations = List.copyOf(limitations == null ? List.of() : limitations);
    }
}
