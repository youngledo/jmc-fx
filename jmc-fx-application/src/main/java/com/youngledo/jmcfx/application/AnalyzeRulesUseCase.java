package com.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.RuleResult;
import com.youngledo.jmcfx.domain.service.RuleAnalysisService;

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
