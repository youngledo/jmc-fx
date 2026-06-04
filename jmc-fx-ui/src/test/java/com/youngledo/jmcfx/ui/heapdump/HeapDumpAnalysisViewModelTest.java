package com.youngledo.jmcfx.ui.heapdump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.application.AnalyzeHeapDumpUseCase;
import com.youngledo.jmcfx.application.HeapDumpApplicationServices;
import com.youngledo.jmcfx.domain.model.HeapDumpAnalysisReport;
import com.youngledo.jmcfx.domain.model.HeapDumpAnalysisState;
import com.youngledo.jmcfx.domain.model.HeapDumpIssue;
import com.youngledo.jmcfx.domain.model.HeapDumpIssueCategory;
import com.youngledo.jmcfx.domain.service.JmcFxException;
import com.youngledo.jmcfx.testsupport.FakeHeapDumpAnalysisService;
import com.youngledo.jmcfx.ui.i18n.I18n;

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
