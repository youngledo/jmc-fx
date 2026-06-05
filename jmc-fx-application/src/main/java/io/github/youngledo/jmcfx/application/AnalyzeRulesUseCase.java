package io.github.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;
import io.github.youngledo.jmcfx.domain.service.RuleAnalysisService;

public final class AnalyzeRulesUseCase {

    private final RuleAnalysisService ruleAnalysisService;

    public AnalyzeRulesUseCase(RuleAnalysisService ruleAnalysisService) {
        this.ruleAnalysisService = Objects.requireNonNull(ruleAnalysisService, "ruleAnalysisService");
    }

    public List<RuleResult> analyze(RecordingSummary recording) {
        Objects.requireNonNull(recording, "recording");
        return ruleAnalysisService.analyze(recording);
    }

    public static AnalyzeRulesUseCase empty() {
        return new AnalyzeRulesUseCase(recording -> List.of());
    }
}
