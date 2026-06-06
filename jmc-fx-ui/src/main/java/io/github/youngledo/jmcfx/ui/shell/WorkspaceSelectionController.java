package io.github.youngledo.jmcfx.ui.shell;

import java.util.function.Consumer;

import io.github.youngledo.jmcfx.ui.advanced.AdvancedJfrPageController;
import io.github.youngledo.jmcfx.ui.analysis.AnalysisPageController;
import io.github.youngledo.jmcfx.ui.environment.EnvironmentPagesController;
import io.github.youngledo.jmcfx.ui.events.EventsPageController;
import io.github.youngledo.jmcfx.ui.exceptions.ExceptionsPageController;
import io.github.youngledo.jmcfx.ui.fileio.FileIoPageController;
import io.github.youngledo.jmcfx.ui.gc.G1GcPageController;
import io.github.youngledo.jmcfx.ui.heap.HeapPageController;
import io.github.youngledo.jmcfx.ui.heapdump.HeapDumpAnalysisPageController;
import io.github.youngledo.jmcfx.ui.javaapp.JavaApplicationDataPagesController;
import io.github.youngledo.jmcfx.ui.jfx.JavaFxEventsPageController;
import io.github.youngledo.jmcfx.ui.jvm.JvmInternalsPagesController;
import io.github.youngledo.jmcfx.ui.leaks.LeakSuspectsPageController;
import io.github.youngledo.jmcfx.ui.locks.LocksPageController;
import io.github.youngledo.jmcfx.ui.metadata.MetadataPageController;
import io.github.youngledo.jmcfx.ui.overview.OverviewPageController;
import io.github.youngledo.jmcfx.ui.profiling.ProfilingPageController;
import io.github.youngledo.jmcfx.ui.socketio.SocketIoPageController;
import io.github.youngledo.jmcfx.ui.threads.ThreadsPageController;
import io.github.youngledo.jmcfx.ui.tlab.TlabPageController;

final class WorkspaceSelectionController {

    private final AppShellViewModel viewModel;
    private final WorkspaceTabsController workspaceTabsController;
    private final WorkspacePageControllers pages;
    private final RecordingSectionLoader recordingSectionLoader;
    private final Consumer<Boolean> backgroundWorkVisible;
    private RecordingWorkspace loadedWorkspace;

    WorkspaceSelectionController(AppShellViewModel viewModel, WorkspaceTabsController workspaceTabsController,
            WorkspacePageControllers pages, RecordingSectionLoader recordingSectionLoader,
            Consumer<Boolean> backgroundWorkVisible) {
        this.viewModel = viewModel;
        this.workspaceTabsController = workspaceTabsController;
        this.pages = pages;
        this.recordingSectionLoader = recordingSectionLoader;
        this.backgroundWorkVisible = backgroundWorkVisible;
    }

    void configure() {
        viewModel.selectedWorkspaceProperty()
                .addListener((observable, oldValue, newValue) -> showWorkspace(newValue));
        viewModel.selectedHeapDumpWorkspaceProperty()
                .addListener((observable, oldValue, newValue) -> showHeapDumpWorkspace(newValue));
        viewModel.selectedLiveJvmWorkspaceProperty()
                .addListener((observable, oldValue, newValue) -> showLiveJvmWorkspace(newValue));
        viewModel.selectedSectionProperty()
                .addListener((observable, oldValue, newValue) -> loadSelectedWorkspaceSection());
        showWorkspace(viewModel.selectedWorkspaceProperty().get());
        showHeapDumpWorkspace(viewModel.selectedHeapDumpWorkspaceProperty().get());
        showLiveJvmWorkspace(viewModel.selectedLiveJvmWorkspaceProperty().get());
    }

    private void showWorkspace(RecordingWorkspace workspace) {
        loadedWorkspace = null;
        pages.overviewPageController().bind(workspace == null ? null : workspace.overviewViewModel());
        pages.eventsPageController().bind(workspace == null ? null : workspace.eventBrowserViewModel());
        pages.analysisPageController().bind(workspace == null ? null : workspace.ruleResultsViewModel());
        pages.profilingPageController().bind(workspace == null ? null : workspace.profilingViewModel());
        pages.exceptionsPageController().bind(
                workspace == null ? null : workspace.exceptionViewModel(),
                workspace == null ? null : workspace.selectedTimeRangeProperty());
        pages.threadsPageController().bind(workspace == null ? null : workspace.threadViewModel());
        pages.fileIoPageController().bind(
                workspace == null ? null : workspace.fileIOViewModel(),
                workspace == null ? null : workspace.selectedTimeRangeProperty());
        pages.socketIoPageController().bind(
                workspace == null ? null : workspace.socketIOViewModel(),
                workspace == null ? null : workspace.selectedTimeRangeProperty());
        pages.locksPageController().bind(workspace == null ? null : workspace.lockViewModel());
        pages.javaApplicationDataPagesController().bindThreadHistogram(
                workspace == null ? null : workspace.javaAppOverviewViewModel(),
                workspace == null ? null : workspace.selectedTimeRangeProperty());
        pages.javaApplicationDataPagesController().bindSecurity(workspace == null ? null : workspace.securityViewModel());
        pages.javaApplicationDataPagesController().bindNativeLibraries(
                workspace == null ? null : workspace.nativeLibraryViewModel());
        pages.javaApplicationDataPagesController().bindThreadDumps(
                workspace == null ? null : workspace.threadDumpViewModel());
        pages.heapPageController().bind(
                workspace == null ? null : workspace.heapViewModel(),
                workspace == null ? null : workspace.selectedTimeRangeProperty());
        pages.leakSuspectsPageController().bind(workspace == null ? null : workspace.leakSuspectsViewModel());
        pages.tlabPageController().bind(
                workspace == null ? null : workspace.tlabViewModel(),
                workspace == null ? null : workspace.selectedTimeRangeProperty());
        pages.jvmInternalsPagesController().bindJvmInfo(workspace == null ? null : workspace.jvmInfoViewModel());
        pages.jvmInternalsPagesController().bindGcConfig(workspace == null ? null : workspace.gcConfigViewModel());
        pages.jvmInternalsPagesController().bindGcSummary(workspace == null ? null : workspace.gcSummaryViewModel());
        pages.jvmInternalsPagesController().bindGcDetails(
                workspace == null ? null : workspace.gcDetailsViewModel(),
                workspace == null ? null : workspace.selectedTimeRangeProperty());
        pages.g1GcPageController().bind(workspace == null ? null : workspace.g1GcViewModel());
        pages.javaFxEventsPageController().bind(workspace == null ? null : workspace.javaFxEventsViewModel());
        pages.jvmInternalsPagesController().bindCompilations(
                workspace == null ? null : workspace.compilationsViewModel(),
                workspace == null ? null : workspace.selectedTimeRangeProperty());
        pages.jvmInternalsPagesController().bindCodeCache(
                workspace == null ? null : workspace.codeCacheViewModel(),
                workspace == null ? null : workspace.selectedTimeRangeProperty());
        pages.jvmInternalsPagesController().bindClassLoading(
                workspace == null ? null : workspace.classLoadingViewModel(),
                workspace == null ? null : workspace.selectedTimeRangeProperty());
        pages.jvmInternalsPagesController().bindVmOperations(
                workspace == null ? null : workspace.vmOperationsViewModel(),
                workspace == null ? null : workspace.selectedTimeRangeProperty());
        pages.environmentPagesController().bind(workspace == null ? null : workspace.environmentViewModel());
        pages.metadataPageController().bind(workspace == null ? null : workspace.jfrMetadataViewModel());
        pages.advancedJfrPageController().bind(workspace == null ? null : workspace.advancedJfrViewModel());
        loadedWorkspace = workspace;
        loadSelectedWorkspaceSection();
    }

    private void showHeapDumpWorkspace(HeapDumpWorkspace workspace) {
        if (workspace == null) {
            pages.heapDumpAnalysisPageController().bind(null);
            return;
        }
        pages.heapDumpAnalysisPageController().bind(workspace.viewModel());
    }

    private void showLiveJvmWorkspace(LiveJvmWorkspace workspace) {
        if (workspace == null) {
            return;
        }
    }

    void loadSelectedWorkspaceSection() {
        RecordingWorkspace workspace = loadedWorkspace;
        if (workspace == null) {
            return;
        }
        loadWorkspaceSection(workspace, viewModel.selectedSectionProperty().get());
    }

    void preloadRecordingWorkspace(RecordingWorkspace workspace) {
        backgroundWorkVisible.accept(false);
    }

    static java.util.List<String> preloadedWorkspaceSections() {
        return java.util.List.of();
    }

    void loadWorkspaceSection(RecordingWorkspace workspace, String sectionId) {
        recordingSectionLoader.load(workspace, sectionId);
    }
}

record WorkspacePageControllers(
        OverviewPageController overviewPageController,
        EventsPageController eventsPageController,
        AnalysisPageController analysisPageController,
        ProfilingPageController profilingPageController,
        ExceptionsPageController exceptionsPageController,
        ThreadsPageController threadsPageController,
        FileIoPageController fileIoPageController,
        SocketIoPageController socketIoPageController,
        LocksPageController locksPageController,
        JavaApplicationDataPagesController javaApplicationDataPagesController,
        HeapPageController heapPageController,
        LeakSuspectsPageController leakSuspectsPageController,
        TlabPageController tlabPageController,
        JvmInternalsPagesController jvmInternalsPagesController,
        G1GcPageController g1GcPageController,
        JavaFxEventsPageController javaFxEventsPageController,
        EnvironmentPagesController environmentPagesController,
        MetadataPageController metadataPageController,
        AdvancedJfrPageController advancedJfrPageController,
        HeapDumpAnalysisPageController heapDumpAnalysisPageController) {
}
