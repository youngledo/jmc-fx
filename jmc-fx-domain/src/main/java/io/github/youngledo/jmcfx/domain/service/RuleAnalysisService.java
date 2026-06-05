package io.github.youngledo.jmcfx.domain.service;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;

public interface RuleAnalysisService {
    List<RuleResult> analyze(RecordingSummary recording);
}
