package com.youngledo.jmcfx.ui.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

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
        assertFalse(viewModel.loadingProperty().get());
        assertTrue(viewModel.loadedProperty().get());
        assertFalse(viewModel.errorProperty().get());
    }

    @Test
    void loadingStateIsVisibleWhileAnalysisRuns() {
        LoadingProbeRuleAnalysisService service = new LoadingProbeRuleAnalysisService();
        RuleResultsViewModel viewModel = new RuleResultsViewModel(service);
        service.viewModel = viewModel;
        RecordingSummary recording = new RecordingSummary("rec", Path.of("rec.jfr"), "rec.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);

        viewModel.analyze(recording);

        assertTrue(service.loadingDuringAnalyze);
        assertFalse(viewModel.loadingProperty().get());
        assertTrue(viewModel.loadedProperty().get());
    }

    @Test
    void failedAnalysisSetsErrorState() {
        FakeRuleAnalysisService service = new FakeRuleAnalysisService() {
            @Override
            public java.util.List<RuleResult> analyze(RecordingSummary recording) {
                throw new IllegalStateException("Rules failed");
            }
        };
        RuleResultsViewModel viewModel = new RuleResultsViewModel(service);
        RecordingSummary recording = new RecordingSummary("rec", Path.of("rec.jfr"), "rec.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);

        try {
            viewModel.analyze(recording);
        } catch (IllegalStateException ignored) {
            // Existing callers handle section-load failures outside this view model.
        }

        assertFalse(viewModel.loadingProperty().get());
        assertFalse(viewModel.loadedProperty().get());
        assertTrue(viewModel.errorProperty().get());
        assertEquals("Rules failed", viewModel.errorMessageProperty().get());
    }

    private static final class LoadingProbeRuleAnalysisService
            implements com.youngledo.jmcfx.domain.service.RuleAnalysisService {
        private RuleResultsViewModel viewModel;
        private boolean loadingDuringAnalyze;

        @Override
        public java.util.List<RuleResult> analyze(RecordingSummary recording) {
            loadingDuringAnalyze = viewModel.loadingProperty().get()
                    && !viewModel.loadedProperty().get()
                    && !viewModel.errorProperty().get();
            return List.of();
        }
    }
}
