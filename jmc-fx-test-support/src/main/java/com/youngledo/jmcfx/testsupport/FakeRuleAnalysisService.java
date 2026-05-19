package com.youngledo.jmcfx.testsupport;

import java.util.ArrayList;
import java.util.List;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.RuleResult;
import com.youngledo.jmcfx.domain.service.RuleAnalysisService;

public class FakeRuleAnalysisService implements RuleAnalysisService {

    private final List<RuleResult> results = new ArrayList<>();

    public void addResult(RuleResult result) {
        results.add(result);
    }

    @Override
    public List<RuleResult> analyze(RecordingSummary recording) {
        return List.copyOf(results);
    }
}
