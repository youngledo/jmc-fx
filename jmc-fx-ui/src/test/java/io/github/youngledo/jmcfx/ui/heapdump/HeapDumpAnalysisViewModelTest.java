package io.github.youngledo.jmcfx.ui.heapdump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.application.AnalyzeHeapDumpUseCase;
import io.github.youngledo.jmcfx.application.HeapDumpApplicationServices;
import io.github.youngledo.jmcfx.domain.model.HeapDumpAnalysisReport;
import io.github.youngledo.jmcfx.domain.model.HeapDumpAnalysisState;
import io.github.youngledo.jmcfx.domain.model.HeapDumpIssue;
import io.github.youngledo.jmcfx.domain.model.HeapDumpIssueCategory;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;
import io.github.youngledo.jmcfx.ui.testsupport.FakeHeapDumpAnalysisService;
import io.github.youngledo.jmcfx.ui.i18n.I18n;

import javafx.scene.layout.VBox;

class HeapDumpAnalysisViewModelTest {

    private final I18n i18n = new I18n(Locale.ENGLISH);

    @Test
    void analyzeUpdatesReportAndStatus() {
        FakeHeapDumpAnalysisService service = new FakeHeapDumpAnalysisService();
        service.setReport(sampleReport(Path.of("demo.hprof")));
        HeapDumpAnalysisViewModel vm = new HeapDumpAnalysisViewModel(useCase(service),
                new DirectHeapDumpAnalysisExecutor(), i18n);

        vm.analyze(Path.of("demo.hprof"));

        assertEquals(HeapDumpAnalysisState.SUCCEEDED, vm.stateProperty().get());
        assertEquals("demo.hprof", vm.heapDumpNameProperty().get());
        assertEquals(1, vm.issues().size());
        assertSame(vm.issues().getFirst(), vm.selectedIssueProperty().get());
        assertEquals("raw report", vm.textReportProperty().get());
    }

    @Test
    void issueCategoryFilterNarrowsIssuesAndClearsBackToAll() {
        HeapDumpIssue duplicateString = sampleIssue();
        HeapDumpIssue arrayWaste = new HeapDumpIssue(HeapDumpIssueCategory.DUPLICATE_ARRAY, "byte[]",
                256 * 1024, 512 * 1024, 12, 0.6, "duplicate arrays", "root -> array");
        FakeHeapDumpAnalysisService service = new FakeHeapDumpAnalysisService();
        service.setReport(new HeapDumpAnalysisReport(Path.of("demo.hprof"), 4096, 2048, 10, 8, 1, 1,
                List.of(duplicateString, arrayWaste), "raw report"));
        HeapDumpAnalysisViewModel vm = new HeapDumpAnalysisViewModel(useCase(service),
                new DirectHeapDumpAnalysisExecutor(), i18n);

        vm.analyze(Path.of("demo.hprof"));

        assertEquals(List.of(HeapDumpIssueCategory.DUPLICATE_STRING, HeapDumpIssueCategory.DUPLICATE_ARRAY),
                vm.issueCategories());
        assertEquals(List.of(duplicateString, arrayWaste), vm.issues());

        vm.selectIssueCategory(HeapDumpIssueCategory.DUPLICATE_ARRAY);

        assertEquals(List.of(arrayWaste), vm.issues());
        assertSame(arrayWaste, vm.selectedIssueProperty().get());

        vm.selectIssueCategory(null);

        assertEquals(List.of(duplicateString, arrayWaste), vm.issues());
        assertSame(duplicateString, vm.selectedIssueProperty().get());
    }

    @Test
    void analyzeFailureSetsFailedStateAndMessage() {
        FakeHeapDumpAnalysisService service = new FakeHeapDumpAnalysisService();
        service.setException(new JmcFxException("boom"));
        HeapDumpAnalysisViewModel vm = new HeapDumpAnalysisViewModel(useCase(service),
                new DirectHeapDumpAnalysisExecutor(), i18n);

        vm.analyze(Path.of("bad.hprof"));

        assertEquals(HeapDumpAnalysisState.FAILED, vm.stateProperty().get());
        assertTrue(vm.statusMessageProperty().get().contains("boom"));
        assertTrue(vm.issues().isEmpty());
    }

    @Test
    void selectingIssueUpdatesDetailProperties() {
        HeapDumpIssue issue = sampleIssue();
        HeapDumpAnalysisViewModel vm = new HeapDumpAnalysisViewModel(useCase(new FakeHeapDumpAnalysisService()),
                new DirectHeapDumpAnalysisExecutor(), i18n);

        vm.selectIssue(issue);

        assertSame(issue, vm.selectedIssueProperty().get());
        assertTrue(vm.selectedIssueDetailsProperty().get().contains("duplicate"));
        assertTrue(vm.selectedIssueDetailsProperty().get().contains("512.0 KB"));
    }

    @Test
    void controllerCategoryComboFiltersIssueTable() {
        HeapDumpIssue duplicateString = sampleIssue();
        HeapDumpIssue arrayWaste = new HeapDumpIssue(HeapDumpIssueCategory.DUPLICATE_ARRAY, "byte[]",
                256 * 1024, 512 * 1024, 12, 0.6, "duplicate arrays", "root -> array");
        FakeHeapDumpAnalysisService service = new FakeHeapDumpAnalysisService();
        service.setReport(new HeapDumpAnalysisReport(Path.of("demo.hprof"), 4096, 2048, 10, 8, 1, 1,
                List.of(duplicateString, arrayWaste), "raw report"));
        HeapDumpAnalysisViewModel vm = new HeapDumpAnalysisViewModel(useCase(service),
                new DirectHeapDumpAnalysisExecutor(), i18n);
        vm.analyze(Path.of("demo.hprof"));
        HeapDumpAnalysisPaneView pane = new HeapDumpAnalysisPaneView(new VBox());
        HeapDumpAnalysisPageController controller = new HeapDumpAnalysisPageController(pane.view(), i18n);
        controller.configure();

        controller.bind(vm);
        pane.view().categoryFilterCombo().getSelectionModel().select(HeapDumpIssueCategory.DUPLICATE_ARRAY);

        assertEquals(List.of(arrayWaste), pane.view().issuesTable().getItems());
        assertSame(arrayWaste, vm.selectedIssueProperty().get());

        pane.view().clearCategoryFilterButton().fire();

        assertEquals(List.of(duplicateString, arrayWaste), pane.view().issuesTable().getItems());
        assertSame(duplicateString, vm.selectedIssueProperty().get());
    }

    private HeapDumpAnalysisReport sampleReport(Path path) {
        return new HeapDumpAnalysisReport(path, 4096, 2048, 10, 8, 1, 1,
                List.of(sampleIssue()), "raw report");
    }

    private AnalyzeHeapDumpUseCase useCase(FakeHeapDumpAnalysisService service) {
        return new AnalyzeHeapDumpUseCase(new HeapDumpApplicationServices(service));
    }

    private HeapDumpIssue sampleIssue() {
        return new HeapDumpIssue(HeapDumpIssueCategory.DUPLICATE_STRING, "java.lang.String",
                512 * 1024, 1024 * 1024, 42, 0.8, "duplicate candidates", "root -> string");
    }
}
