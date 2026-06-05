package io.github.youngledo.jmcfx.ui.testsupport;

import java.util.ArrayList;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;
import io.github.youngledo.jmcfx.domain.service.RuleAnalysisService;

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
