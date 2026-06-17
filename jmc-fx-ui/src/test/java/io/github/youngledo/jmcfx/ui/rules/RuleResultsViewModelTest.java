package io.github.youngledo.jmcfx.ui.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.application.AnalyzeRulesUseCase;
import io.github.youngledo.jmcfx.application.DiagnosticFindingsUseCase;
import io.github.youngledo.jmcfx.domain.model.DiagnosticFindingSource;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;
import io.github.youngledo.jmcfx.domain.model.Severity;
import io.github.youngledo.jmcfx.domain.model.ai.AiFinding;
import io.github.youngledo.jmcfx.domain.model.ai.AiRecordingReport;
import io.github.youngledo.jmcfx.domain.model.ai.AiSeverity;
import io.github.youngledo.jmcfx.ui.testsupport.FakeRuleAnalysisService;

class RuleResultsViewModelTest {

    @Test
    void analyzesRecordingAndSelectsFirstResult() {
        FakeRuleAnalysisService service = new FakeRuleAnalysisService();
        service.addResult(new RuleResult("r1", "Rule 1", Severity.WARNING, 50, "Memory", "Summary", "Explanation"));
        RuleResultsViewModel viewModel = viewModel(service);
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
    void selectedRulePublishesSharedDetailSelection() {
        FakeRuleAnalysisService service = new FakeRuleAnalysisService();
        service.addResult(new RuleResult("r1", "Rule 1", Severity.WARNING, 50, "Memory", "Summary", "Explanation"));
        service.addResult(new RuleResult("r2", "Rule 2", Severity.CRITICAL, 90, "Threads", "Blocked",
                "Thread contention", "Blocked for 2 s", "Inspect contended locks", "locks"));
        RuleResultsViewModel viewModel = viewModel(service);
        RecordingSummary recording = new RecordingSummary("rec", Path.of("rec.jfr"), "rec.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);

        viewModel.analyze(recording);
        viewModel.selectedResultProperty().set(viewModel.resultsProperty().get(1));

        var detail = viewModel.detailSelectionProperty().get();
        assertEquals("analysis", detail.pageId());
        assertEquals("r2", detail.selectionId());
        assertEquals("Rule 2", detail.title());
        assertTrue(detail.meta().contains("CRITICAL"));
        assertTrue(detail.meta().contains("90"));
        assertTrue(detail.meta().contains("locks"));
        assertTrue(detail.body().contains("Thread contention"));
        assertTrue(detail.body().contains("Blocked for 2 s"));
        assertTrue(detail.body().contains("Inspect contended locks"));
    }

    @Test
    void selectedRulePublishesStructuredDetailForAnalysisPane() {
        FakeRuleAnalysisService service = new FakeRuleAnalysisService();
        service.addResult(new RuleResult("r1", "Rule 1", Severity.CRITICAL, 90, "Threads", "Blocked summary",
                "<p>Thread contention</p>", "<p>Blocked for 2 s</p>", "<p>Inspect contended locks</p>", "locks"));
        RuleResultsViewModel viewModel = viewModel(service);
        RecordingSummary recording = new RecordingSummary("rec", Path.of("rec.jfr"), "rec.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);

        viewModel.analyze(recording);

        RuleResultDetail detail = viewModel.selectedDetailProperty().get();
        assertEquals("r1", detail.resultId());
        assertEquals("Rule 1", detail.title());
        assertEquals("Blocked summary", detail.summary());
        assertEquals("Thread contention", detail.explanation());
        assertEquals("Blocked for 2 s", detail.evidence());
        assertEquals("Inspect contended locks", detail.recommendation());
        assertEquals("locks", detail.relatedPageId());
        assertTrue(detail.hasRelatedPage());

        viewModel.selectedResultProperty().set(null);

        assertEquals(null, viewModel.selectedDetailProperty().get());
    }

    @Test
    void filtersByResultOverviewControls() {
        FakeRuleAnalysisService service = new FakeRuleAnalysisService();
        service.addResult(new RuleResult("ok", "OK Rule", Severity.OK, 0, "General", "Everything is fine",
                "No action needed"));
        service.addResult(new RuleResult("info", "Info Rule", Severity.INFO, 25, "General", "Small signal",
                "Inspect if relevant"));
        service.addResult(new RuleResult("warning", "Allocation Rule", Severity.WARNING, 75, "Memory",
                "High allocation", "Review allocation pressure"));
        service.addResult(new RuleResult("ignored", "Ignored Rule", Severity.IGNORED, -3, "Rules",
                "Disabled by preferences", "Enable rule to evaluate"));
        service.addResult(new RuleResult("unavailable", "Unavailable Rule", Severity.UNAVAILABLE, -1, "Events",
                "Required events missing", "Record required events"));
        RuleResultsViewModel viewModel = viewModel(service);
        RecordingSummary recording = new RecordingSummary("rec", Path.of("rec.jfr"), "rec.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);

        viewModel.analyze(recording);

        assertEquals(List.of("info", "warning"), resultIds(viewModel));

        viewModel.minimumScoreProperty().set(50);
        assertEquals(List.of("warning"), resultIds(viewModel));

        viewModel.searchTextProperty().set("allocation");
        assertEquals(List.of("warning"), resultIds(viewModel));

        viewModel.showOkResultsProperty().set(true);
        viewModel.showIgnoredResultsProperty().set(true);
        viewModel.showUnavailableResultsProperty().set(true);
        viewModel.searchTextProperty().set("");
        assertEquals(List.of("ok", "warning", "ignored", "unavailable"), resultIds(viewModel));
    }

    @Test
    void filtersVisibleDiagnosticFindingsWithAnalysisControls() {
        FakeRuleAnalysisService service = new FakeRuleAnalysisService();
        service.addResult(new RuleResult("ok", "OK Rule", Severity.OK, 0, "General", "Everything is fine",
                "No action needed"));
        service.addResult(new RuleResult("info", "Info Rule", Severity.INFO, 25, "General", "Small signal",
                "Inspect if relevant"));
        service.addResult(new RuleResult("warning", "Allocation Rule", Severity.WARNING, 75, "Memory",
                "High allocation", "Review allocation pressure"));
        service.addResult(new RuleResult("ignored", "Ignored Rule", Severity.IGNORED, -3, "Rules",
                "Disabled by preferences", "Enable rule to evaluate"));
        service.addResult(new RuleResult("unavailable", "Unavailable Rule", Severity.UNAVAILABLE, -1, "Events",
                "Required events missing", "Record required events"));
        RuleResultsViewModel viewModel = viewModel(service);

        viewModel.analyze(recording());
        viewModel.updateAiReport(new AiRecordingReport("", List.of(
                new AiFinding("AI allocation finding", AiSeverity.WARNING, 0.82,
                        "memory", "Inspect allocations.", "AI evidence mentions allocations", List.of())),
                List.of(), List.of()));

        assertEquals(List.of("rule:warning", "ai:0", "rule:info"), findingIds(viewModel));

        viewModel.minimumScoreProperty().set(80);
        assertEquals(List.of("ai:0"), findingIds(viewModel));
        assertEquals("ai:0", viewModel.selectedFindingProperty().get().id());

        viewModel.searchTextProperty().set("allocation");
        assertEquals(List.of("ai:0"), findingIds(viewModel));

        viewModel.minimumScoreProperty().set(50);
        assertEquals(List.of("rule:warning", "ai:0"), findingIds(viewModel));

        viewModel.showOkResultsProperty().set(true);
        viewModel.showIgnoredResultsProperty().set(true);
        viewModel.showUnavailableResultsProperty().set(true);
        viewModel.searchTextProperty().set("");
        assertEquals(List.of("rule:warning", "ai:0", "rule:ok", "rule:unavailable", "rule:ignored"),
                findingIds(viewModel));
    }

    @Test
    void loadingStateIsVisibleWhileAnalysisRuns() {
        LoadingProbeRuleAnalysisService service = new LoadingProbeRuleAnalysisService();
        RuleResultsViewModel viewModel = new RuleResultsViewModel(
                new AnalyzeRulesUseCase(service), new DiagnosticFindingsUseCase());
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
        RuleResultsViewModel viewModel = viewModel(service);
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

    @Test
    void exposesDiagnosticFindingsForLoadedRuleResults() {
        FakeRuleAnalysisService service = new FakeRuleAnalysisService();
        service.addResult(new RuleResult("gc", "GC Pressure", Severity.WARNING, 70, "GC",
                "GC pressure is elevated.", "Explanation", "Evidence", "Inspect GC.", "gcDetails"));
        RuleResultsViewModel viewModel = viewModel(service);

        viewModel.analyze(recording());

        assertEquals(1, viewModel.findingsProperty().size());
        assertEquals("rule:gc", viewModel.findingsProperty().getFirst().id());
        assertEquals("GC Pressure", viewModel.selectedFindingDetailProperty().get().title());
        assertEquals("gcDetails", viewModel.selectedFindingDetailProperty().get().relatedPageId());
    }

    @Test
    void mergesAiReportFindingsWithoutCallingAiAutomatically() {
        FakeRuleAnalysisService service = new FakeRuleAnalysisService();
        service.addResult(new RuleResult("threads", "Thread Activity", Severity.INFO, 20, "Threads",
                "Thread activity was recorded.", "", "", "", "threads"));
        RuleResultsViewModel viewModel = viewModel(service);
        viewModel.analyze(recording());

        viewModel.updateAiReport(new AiRecordingReport("", List.of(
                new AiFinding("Blocked threads look suspicious", AiSeverity.CRITICAL, 0.88,
                        "threads", "Inspect thread states.", "", List.of())),
                List.of(), List.of()));

        assertEquals(2, viewModel.findingsProperty().size());
        assertEquals(DiagnosticFindingSource.AI, viewModel.findingsProperty().getFirst().source());
        assertEquals("threads", viewModel.selectedFindingDetailProperty().get().relatedPageId());
    }

    private static final class LoadingProbeRuleAnalysisService
            implements io.github.youngledo.jmcfx.domain.service.RuleAnalysisService {
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

    private static List<String> resultIds(RuleResultsViewModel viewModel) {
        return viewModel.resultsProperty().stream()
                .map(RuleResult::id)
                .toList();
    }

    private static List<String> findingIds(RuleResultsViewModel viewModel) {
        return viewModel.findingsProperty().stream()
                .map(finding -> finding.id())
                .toList();
    }

    private static RuleResultsViewModel viewModel(FakeRuleAnalysisService service) {
        return new RuleResultsViewModel(new AnalyzeRulesUseCase(service), new DiagnosticFindingsUseCase());
    }

    private static RecordingSummary recording() {
        return new RecordingSummary("rec", Path.of("rec.jfr"), "rec.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);
    }
}
