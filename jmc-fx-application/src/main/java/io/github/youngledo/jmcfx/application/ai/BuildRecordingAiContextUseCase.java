package io.github.youngledo.jmcfx.application.ai;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import io.github.youngledo.jmcfx.application.AnalyzeRulesUseCase;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;

public final class BuildRecordingAiContextUseCase {

    private static final int MAX_RULE_RESULTS = 20;

    private final AnalyzeRulesUseCase analyzeRules;

    public BuildRecordingAiContextUseCase(AnalyzeRulesUseCase analyzeRules) {
        this.analyzeRules = Objects.requireNonNull(analyzeRules, "analyzeRules");
    }

    public RecordingAiContext build(RecordingSummary recording) {
        Objects.requireNonNull(recording, "recording");
        List<RuleResult> allRules = analyzeRules.analyze(recording);
        List<RuleResult> rules = allRules.stream()
                .sorted(Comparator.comparingInt(RuleResult::score).reversed())
                .limit(MAX_RULE_RESULTS)
                .toList();
        List<String> limitations = allRules.size() > MAX_RULE_RESULTS
                ? List.of("Rule results were capped to top 20 by score.")
                : List.of();
        return new RecordingAiContext(recording, rules, limitations);
    }
}
