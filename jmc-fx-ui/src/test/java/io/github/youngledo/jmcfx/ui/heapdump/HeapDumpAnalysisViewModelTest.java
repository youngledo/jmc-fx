package io.github.youngledo.jmcfx.ui.heapdump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
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
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectSummary;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferenceEdge;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePath;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePathRequest;
import io.github.youngledo.jmcfx.domain.service.HeapDumpBrowsingService;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;
import io.github.youngledo.jmcfx.ui.testsupport.FakeHeapDumpAnalysisService;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.util.TableExportRegistration;
import io.github.youngledo.jmcfx.ui.util.TableExportRequest;
import io.github.youngledo.jmcfx.ui.util.TableExportScope;

import javafx.scene.layout.VBox;
import javafx.application.Platform;

class HeapDumpAnalysisViewModelTest {

    private final I18n i18n = new I18n(Locale.ENGLISH);

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await(5, TimeUnit.SECONDS);
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized by another test class.
        }
    }

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
        assertEquals(List.of(browsingService.objectSummary),
                vm.selectedObjectGroupDetailProperty().get().objects().rows());
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
        assertEquals(List.of(browsingService.objectSummary), pane.view().objectGroupObjectsTable().getItems());
        assertEquals("Object ID", pane.view().objectGroupObjectsTable().getColumns().getFirst().getText());
    }

    @Test
    void controllerLoadsAndDisplaysReferencePathsForSelectedObjectGroup() {
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

        pane.view().loadReferencePathsButton().fire();

        assertEquals("Reference Paths", pane.view().referencePathsTab().getText());
        assertEquals(List.of(browsingService.referencePath), pane.view().referencePathsTable().getItems());
        assertEquals(1, browsingService.referencePathRequests.size());
        assertEquals("group-1", browsingService.referencePathRequests.getFirst().selectedObjectId());
        assertTrue(pane.view().referencePathsTable().getColumns().size() >= 4);
        assertTrue(pane.view().referencePathsTable().getColumns().getFirst().getText().contains("Selected"));
        assertTrue(vm.objectGroupStatusProperty().get().contains("reference paths"));
    }

    @Test
    void referencePathFallbackReportsUnavailableState() {
        FakeHeapDumpAnalysisService analysisService = new FakeHeapDumpAnalysisService();
        analysisService.setReport(sampleReport(Path.of("demo.hprof")));
        FakeHeapDumpBrowsingService browsingService = new FakeHeapDumpBrowsingService();
        browsingService.referencePathsAvailable = false;
        HeapDumpApplicationServices services = new HeapDumpApplicationServices(analysisService, browsingService);
        HeapDumpAnalysisViewModel vm = new HeapDumpAnalysisViewModel(
                new AnalyzeHeapDumpUseCase(services),
                new BrowseHeapDumpObjectGroupsUseCase(services),
                new LoadHeapDumpObjectGroupDetailUseCase(services),
                new LoadHeapDumpReferencePathsUseCase(services),
                new DirectHeapDumpAnalysisExecutor(),
                i18n);

        vm.analyze(Path.of("demo.hprof"));
        vm.loadReferencePaths(vm.selectedObjectGroupProperty().get());

        assertTrue(vm.referencePaths().isEmpty());
        assertTrue(vm.objectGroupStatusProperty().get().contains("not available"));
    }

    @Test
    void heapDumpExportRegistrationsDescribeHeapDumpScopeWithoutTimeRange() {
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

        List<TableExportRequest> requests = controller.exportRegistrations().stream()
                .map(TableExportRegistration::requestSupplier)
                .map(java.util.function.Supplier::get)
                .toList();

        assertEquals(4, requests.size());
        assertTrue(requests.stream().allMatch(request -> "HPROF Heap Dump".equals(request.context().workspace())));
        assertTrue(requests.stream().allMatch(request -> request.context().timeRange() == null));
        assertTrue(requests.stream().allMatch(request -> TableExportScope.CURRENT_VIEW == request.context().rowScope()));
        assertTrue(requests.stream().allMatch(request -> TableExportScope.VISIBLE_COLUMNS == request.context().columnScope()));
        assertTrue(requests.stream().map(request -> request.context().table()).toList()
                .containsAll(List.of("Issues", "Object Groups", "Object Group Objects", "Reference Paths")));
        assertEquals("demo.hprof", requests.getFirst().context().source());
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
        private final HeapDumpObjectSummary objectSummary = new HeapDumpObjectSummary("0x1",
                "java.lang.String", 24, 128, 0, 0, true);
        private final HeapDumpReferencePath referencePath = new HeapDumpReferencePath("group-1",
                List.of(new HeapDumpReferenceEdge("root", "group-1", "field.value", "field")), 4096, true);
        private final List<HeapDumpBrowseRequest> browseRequests = new ArrayList<>();
        private final List<String> detailGroupIds = new ArrayList<>();
        private final List<HeapDumpReferencePathRequest> referencePathRequests = new ArrayList<>();
        private boolean referencePathsAvailable = true;

        @Override
        public HeapDumpBrowseWindow<HeapDumpObjectGroup> browseObjectGroups(HeapDumpBrowseRequest request) {
            browseRequests.add(request);
            return new HeapDumpBrowseWindow<>(List.of(group), request.offset(), request.limit(), 1, false);
        }

        @Override
        public HeapDumpObjectGroupDetail loadObjectGroupDetail(HeapDumpBrowseRequest request, String groupId) {
            detailGroupIds.add(groupId);
            return new HeapDumpObjectGroupDetail(group,
                    new HeapDumpBrowseWindow<>(List.of(objectSummary), 0, request.limit(), 1, false),
                    "");
        }

        @Override
        public HeapDumpBrowseWindow<HeapDumpReferencePath> loadReferencePaths(HeapDumpReferencePathRequest request) {
            referencePathRequests.add(request);
            if (!referencePathsAvailable) {
                return new HeapDumpBrowseWindow<>(List.of(), request.offset(), request.limit(), 0, true);
            }
            return new HeapDumpBrowseWindow<>(List.of(referencePath), request.offset(), request.limit(), 1, true);
        }
    }
}
