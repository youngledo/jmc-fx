package io.github.youngledo.jmcfx.ui.heapdump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.application.AnalyzeHeapDumpUseCase;
import io.github.youngledo.jmcfx.application.BrowseHeapDumpObjectGroupsUseCase;
import io.github.youngledo.jmcfx.application.HeapDumpApplicationServices;
import io.github.youngledo.jmcfx.application.LoadHeapDumpObjectGroupDetailUseCase;
import io.github.youngledo.jmcfx.application.LoadHeapDumpReferencePathsUseCase;
import io.github.youngledo.jmcfx.domain.model.HeapDumpAnalysisReport;
import io.github.youngledo.jmcfx.domain.model.HeapDumpAnalysisState;
import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseRequest;
import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseSort;
import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseWindow;
import io.github.youngledo.jmcfx.domain.model.HeapDumpIssue;
import io.github.youngledo.jmcfx.domain.model.HeapDumpIssueCategory;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroup;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroupDetail;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroupKind;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePath;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePathRequest;
import io.github.youngledo.jmcfx.domain.service.HeapDumpBrowsingService;
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
    void analyzeLoadsObjectGroupsAndSelectedGroupDetailWhenBrowsingUseCasesAreAvailable() {
        FakeHeapDumpAnalysisService analysisService = new FakeHeapDumpAnalysisService();
        analysisService.setReport(sampleReport(Path.of("demo.hprof")));
        FakeHeapDumpBrowsingService browsingService = new FakeHeapDumpBrowsingService();
        HeapDumpApplicationServices services = new HeapDumpApplicationServices(analysisService, browsingService);
        HeapDumpAnalysisViewModel vm = new HeapDumpAnalysisViewModel(
                new AnalyzeHeapDumpUseCase(services),
                new BrowseHeapDumpObjectGroupsUseCase(services),
                new LoadHeapDumpObjectGroupDetailUseCase(services),
                new LoadHeapDumpReferencePathsUseCase(services),
                new DirectHeapDumpAnalysisExecutor(),
                i18n);

        vm.analyze(Path.of("demo.hprof"));

        assertEquals(1, browsingService.browseRequests.size());
        HeapDumpBrowseRequest request = browsingService.browseRequests.getFirst();
        assertEquals(Path.of("demo.hprof"), request.path());
        assertEquals(HeapDumpObjectGroupKind.CLASS, request.groupKind());
        assertEquals(HeapDumpBrowseSort.RETAINED_SIZE_BYTES, request.sort());
        assertEquals(100, request.limit());
        assertEquals(List.of(browsingService.group), vm.objectGroups());
        assertSame(browsingService.group, vm.selectedObjectGroupProperty().get());
        assertEquals("group-1", vm.selectedObjectGroupDetailProperty().get().group().id());
        assertTrue(vm.objectGroupStatusProperty().get().contains("not available"));
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

    @Test
    void controllerBindsObjectGroupsTableAndDetailTab() {
        FakeHeapDumpAnalysisService analysisService = new FakeHeapDumpAnalysisService();
        analysisService.setReport(sampleReport(Path.of("demo.hprof")));
        FakeHeapDumpBrowsingService browsingService = new FakeHeapDumpBrowsingService();
        HeapDumpApplicationServices services = new HeapDumpApplicationServices(analysisService, browsingService);
        HeapDumpAnalysisViewModel vm = new HeapDumpAnalysisViewModel(
                new AnalyzeHeapDumpUseCase(services),
                new BrowseHeapDumpObjectGroupsUseCase(services),
                new LoadHeapDumpObjectGroupDetailUseCase(services),
                new LoadHeapDumpReferencePathsUseCase(services),
                new DirectHeapDumpAnalysisExecutor(),
                i18n);
        vm.analyze(Path.of("demo.hprof"));
        HeapDumpAnalysisPaneView pane = new HeapDumpAnalysisPaneView(new VBox());
        HeapDumpAnalysisPageController controller = new HeapDumpAnalysisPageController(pane.view(), i18n);
        controller.configure();

        controller.bind(vm);

        assertEquals("Object Groups", pane.view().objectGroupsTab().getText());
        assertEquals(List.of(browsingService.group), pane.view().objectGroupsTable().getItems());
        assertSame(browsingService.group, pane.view().objectGroupsTable().getSelectionModel().getSelectedItem());
        assertEquals("java.lang.String", pane.view().objectGroupDetailTitleLabel().getText());
        assertTrue(pane.view().objectGroupMetaLabel().getText().contains("Objects: 42"));
        assertTrue(pane.view().objectGroupDetailArea().getText().contains("not available"));
    }

    private HeapDumpAnalysisReport sampleReport(Path path) {
        return new HeapDumpAnalysisReport(path, 4096, 2048, 10, 8, 1, 1,
                List.of(sampleIssue()), "raw report");
    }

    private AnalyzeHeapDumpUseCase useCase(FakeHeapDumpAnalysisService service) {
        return new AnalyzeHeapDumpUseCase(new HeapDumpApplicationServices(service, new FakeHeapDumpBrowsingService()));
    }

    private HeapDumpIssue sampleIssue() {
        return new HeapDumpIssue(HeapDumpIssueCategory.DUPLICATE_STRING, "java.lang.String",
                512 * 1024, 1024 * 1024, 42, 0.8, "duplicate candidates", "root -> string");
    }

    private static final class FakeHeapDumpBrowsingService implements HeapDumpBrowsingService {
        private final HeapDumpObjectGroup group = new HeapDumpObjectGroup("group-1", "java.lang.String",
                HeapDumpObjectGroupKind.CLASS, 42, 1024, 4096, 512, true);
        private final List<HeapDumpBrowseRequest> browseRequests = new ArrayList<>();
        private final List<String> detailGroupIds = new ArrayList<>();

        @Override
        public HeapDumpBrowseWindow<HeapDumpObjectGroup> browseObjectGroups(HeapDumpBrowseRequest request) {
            browseRequests.add(request);
            return new HeapDumpBrowseWindow<>(List.of(group), request.offset(), request.limit(), 1, false);
        }

        @Override
        public HeapDumpObjectGroupDetail loadObjectGroupDetail(HeapDumpBrowseRequest request, String groupId) {
            detailGroupIds.add(groupId);
            return new HeapDumpObjectGroupDetail(group,
                    new HeapDumpBrowseWindow<>(List.of(), 0, request.limit(), 0, false),
                    "Heap dump browsing data is not available yet.");
        }

        @Override
        public HeapDumpBrowseWindow<HeapDumpReferencePath> loadReferencePaths(HeapDumpReferencePathRequest request) {
            return new HeapDumpBrowseWindow<>(List.of(), request.offset(), request.limit(), 0, false);
        }
    }
}
