package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.RuleResult;

public interface RuleAnalysisService {
    List<RuleResult> analyze(RecordingSummary recording);
}
