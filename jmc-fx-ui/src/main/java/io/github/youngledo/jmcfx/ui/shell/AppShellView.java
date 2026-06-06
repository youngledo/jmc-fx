package io.github.youngledo.jmcfx.ui.shell;

import io.github.youngledo.jmcfx.ui.advanced.AdvancedJfrPaneView;
import io.github.youngledo.jmcfx.ui.analysis.AnalysisPaneView;
import io.github.youngledo.jmcfx.ui.events.EventsPaneView;
import io.github.youngledo.jmcfx.ui.advanced.AdvancedJfrPageView;
import io.github.youngledo.jmcfx.ui.analysis.AnalysisPageView;
import io.github.youngledo.jmcfx.ui.events.EventsPageView;
import io.github.youngledo.jmcfx.ui.environment.EnvironmentPaneView;
import io.github.youngledo.jmcfx.ui.environment.EnvironmentPagesView;
import io.github.youngledo.jmcfx.ui.exceptions.ExceptionsPageView;
import io.github.youngledo.jmcfx.ui.fileio.FileIoPaneView;
import io.github.youngledo.jmcfx.ui.fileio.FileIoPageView;
import io.github.youngledo.jmcfx.ui.gc.G1GcPaneView;
import io.github.youngledo.jmcfx.ui.gc.G1GcPageView;
import io.github.youngledo.jmcfx.ui.heap.HeapPaneView;
import io.github.youngledo.jmcfx.ui.heap.HeapPageView;
import io.github.youngledo.jmcfx.ui.heapdump.HeapDumpAnalysisPageView;
import io.github.youngledo.jmcfx.ui.heapdump.HeapDumpAnalysisPaneView;
import io.github.youngledo.jmcfx.ui.javaapp.JavaApplicationDataPaneView;
import io.github.youngledo.jmcfx.ui.javaapp.JavaApplicationDataPagesView;
import io.github.youngledo.jmcfx.ui.jfx.JavaFxEventsPaneView;
import io.github.youngledo.jmcfx.ui.jfx.JavaFxEventsPageView;
import io.github.youngledo.jmcfx.ui.jvm.JvmInternalsPaneView;
import io.github.youngledo.jmcfx.ui.jvm.JvmInternalsPagesView;
import io.github.youngledo.jmcfx.ui.leaks.LeakSuspectsPaneView;
import io.github.youngledo.jmcfx.ui.leaks.LeakSuspectsPageView;
import io.github.youngledo.jmcfx.ui.locks.LocksPaneView;
import io.github.youngledo.jmcfx.ui.locks.LocksPageView;
import io.github.youngledo.jmcfx.ui.metadata.MetadataPageView;
import io.github.youngledo.jmcfx.ui.metadata.MetadataPaneView;
import io.github.youngledo.jmcfx.ui.overview.OverviewPaneView;
import io.github.youngledo.jmcfx.ui.overview.OverviewPageView;
import io.github.youngledo.jmcfx.ui.profiling.ProfilingPageView;
import io.github.youngledo.jmcfx.ui.profiling.ProfilingPaneView;
import io.github.youngledo.jmcfx.ui.socketio.SocketIoPaneView;
import io.github.youngledo.jmcfx.ui.socketio.SocketIoPageView;
import io.github.youngledo.jmcfx.ui.threads.ThreadsPageView;
import io.github.youngledo.jmcfx.ui.tlab.TlabPaneView;
import io.github.youngledo.jmcfx.ui.tlab.TlabPageView;
import io.github.youngledo.jmcfx.ui.util.WorkbenchFocusTarget;

import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

final class AppShellView {
    final HomePaneView home = new HomePaneView();
    final SettingsPaneView settings = new SettingsPaneView();
    final ShellWorkspacePanes workspacePanes = new ShellWorkspacePanes(home.pane, settings.pane);
    final StackPane workspaceStack = workspacePanes.stack;
    final ShellRootView shell = new ShellRootView(workspacePanes.stack);
    final BorderPane root = shell.root;
    final AppSidebar sidebar = shell.sidebar;
    final TabPane recordingTabs = shell.recordingTabs;
    final ProgressBar progressBar = shell.progressBar;
    final OverviewPaneView overview = new OverviewPaneView(workspacePanes.overviewPane);
    final EventsPaneView events = new EventsPaneView(workspacePanes.eventsPane);
    final AnalysisPaneView analysis = new AnalysisPaneView(workspacePanes.analysisPane);
    final MetadataPaneView metadata = new MetadataPaneView(workspacePanes.metadataPane);
    final AdvancedJfrPaneView advancedJfr = new AdvancedJfrPaneView(workspacePanes.advancedJfrPane);
    final HeapDumpAnalysisPaneView heapDumpAnalysis = new HeapDumpAnalysisPaneView(workspacePanes.heapDumpAnalysisPane);
    final ProfilingPaneView profiling = new ProfilingPaneView(workspacePanes.profilingPane);
    final RecordingOverviewPaneView recordingOverviewPages =
            new RecordingOverviewPaneView(workspacePanes.javaApplicationPane, workspacePanes.jvmInternalsPane,
                    workspacePanes.environmentPane);
    final JavaApplicationDataPaneView javaApplicationData =
            new JavaApplicationDataPaneView(workspacePanes.exceptionsPane, workspacePanes.threadsPane,
                    workspacePanes.threadHistogramPane, workspacePanes.securityPane,
                    workspacePanes.nativeLibrariesPane, workspacePanes.threadDumpsPane);
    final FileIoPaneView fileIo = new FileIoPaneView(workspacePanes.fileioPane);
    final SocketIoPaneView socketIo = new SocketIoPaneView(workspacePanes.socketioPane);
    final LocksPaneView locks = new LocksPaneView(workspacePanes.locksPane);
    final HeapPaneView heap = new HeapPaneView(workspacePanes.heapPane);
    final LeakSuspectsPaneView leaks = new LeakSuspectsPaneView(workspacePanes.leaksPane);
    final TlabPaneView tlab = new TlabPaneView(workspacePanes.tlabPane);
    final JvmInternalsPaneView jvmInternals =
            new JvmInternalsPaneView(workspacePanes.jvmInfoPane, workspacePanes.gcConfigPane,
                    workspacePanes.gcSummaryPane, workspacePanes.gcDetailsPane, workspacePanes.compilationsPane,
                    workspacePanes.codeCachePane, workspacePanes.classLoadingPane, workspacePanes.vmOperationsPane);
    final G1GcPaneView g1Gc = new G1GcPaneView(workspacePanes.g1GcPane);
    final JavaFxEventsPaneView javaFxEvents = new JavaFxEventsPaneView(workspacePanes.javaFxEventsPane);
    final EnvironmentPaneView environment =
            new EnvironmentPaneView(workspacePanes.processesPane, workspacePanes.envVarsPane,
                    workspacePanes.sysPropsPane, workspacePanes.recordingInfoPane, workspacePanes.agentsPane,
                    workspacePanes.constantPoolsPane);

    AnalysisPageView analysisPage() {
        return analysis.view();
    }

    EventsPageView eventsPage() {
        return events.view();
    }

    MetadataPageView metadataPage() {
        return metadata.view();
    }

    OverviewPageView overviewPage() {
        return overview.view();
    }

    AdvancedJfrPageView advancedJfrPage() {
        return advancedJfr.view();
    }

    HeapDumpAnalysisPageView heapDumpAnalysisPage() {
        return heapDumpAnalysis.view();
    }

    ProfilingPageView profilingPage() {
        return profiling.view();
    }

    RecordingOverviewPagesView recordingOverviewPages() {
        return recordingOverviewPages.view();
    }

    ExceptionsPageView exceptionsPage() {
        return javaApplicationData.exceptionsPage();
    }

    ThreadsPageView threadsPage() {
        return javaApplicationData.threadsPage();
    }

    JavaApplicationDataPagesView javaApplicationDataPages() {
        return javaApplicationData.javaApplicationDataPages();
    }

    FileIoPageView fileIoPage() {
        return fileIo.view();
    }

    SocketIoPageView socketIoPage() {
        return socketIo.view();
    }

    LocksPageView locksPage() {
        return locks.view();
    }

    HeapPageView heapPage() {
        return heap.view();
    }

    LeakSuspectsPageView leakSuspectsPage() {
        return leaks.view();
    }

    TlabPageView tlabPage() {
        return tlab.view();
    }

    JvmInternalsPagesView jvmInternalsPages() {
        return jvmInternals.view();
    }

    G1GcPageView g1GcPage() {
        return g1Gc.view();
    }

    JavaFxEventsPageView javaFxEventsPage() {
        return javaFxEvents.view();
    }

    EnvironmentPagesView environmentPages() {
        return environment.view();
    }

    Node focusTarget(WorkbenchFocusTarget target) {
        return switch (target) {
            case GLOBAL_NAVIGATION -> sidebar;
            case WORKSPACE_TABS -> recordingTabs;
            case PAGE_PRIMARY -> visibleWorkspacePane();
            case PAGE_FILTER -> sidebar.searchField();
            case NONE -> null;
        };
    }

    private Node visibleWorkspacePane() {
        return workspaceStack.getChildren().stream()
                .filter(Node::isVisible)
                .findFirst()
                .orElse(workspaceStack);
    }
}
