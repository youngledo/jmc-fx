package com.youngledo.jmcfx.ui.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.RuleResult;
import com.youngledo.jmcfx.domain.model.Severity;
import com.youngledo.jmcfx.testsupport.FakeRuleAnalysisService;

class RuleResultsViewModelTest {

    @Test
    void analyzesRecordingAndSelectsFirstResult() {
        FakeRuleAnalysisService service = new FakeRuleAnalysisService();
        service.addResult(new RuleResult("r1", "Rule 1", Severity.WARNING, 50, "Memory", "Summary", "Explanation"));
        RuleResultsViewModel viewModel = new RuleResultsViewModel(service);
        RecordingSummary recording = new RecordingSummary("rec", Path.of("rec.jfr"), "rec.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);

        viewModel.analyze(recording);

        assertEquals(1, viewModel.resultsProperty().size());
        assertEquals("r1", viewModel.selectedResultProperty().get().id());
    }
}
