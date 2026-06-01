package com.youngledo.jmcfx.ui.shell;

import com.youngledo.jmcfx.ui.advanced.AdvancedJfrPaneView;
import com.youngledo.jmcfx.ui.analysis.AnalysisPaneView;
import com.youngledo.jmcfx.ui.events.EventsPaneView;
import com.youngledo.jmcfx.ui.advanced.AdvancedJfrPageView;
import com.youngledo.jmcfx.ui.analysis.AnalysisPageView;
import com.youngledo.jmcfx.ui.events.EventsPageView;
import com.youngledo.jmcfx.ui.environment.EnvironmentPaneView;
import com.youngledo.jmcfx.ui.environment.EnvironmentPagesView;
import com.youngledo.jmcfx.ui.exceptions.ExceptionsPageView;
import com.youngledo.jmcfx.ui.fileio.FileIoPaneView;
import com.youngledo.jmcfx.ui.fileio.FileIoPageView;
import com.youngledo.jmcfx.ui.gc.G1GcPaneView;
import com.youngledo.jmcfx.ui.gc.G1GcPageView;
import com.youngledo.jmcfx.ui.heap.HeapPaneView;
import com.youngledo.jmcfx.ui.heap.HeapPageView;
import com.youngledo.jmcfx.ui.heapdump.HeapDumpAnalysisPageView;
import com.youngledo.jmcfx.ui.heapdump.HeapDumpAnalysisPaneView;
import com.youngledo.jmcfx.ui.javaapp.JavaApplicationDataPaneView;
import com.youngledo.jmcfx.ui.javaapp.JavaApplicationDataPagesView;
import com.youngledo.jmcfx.ui.jfx.JavaFxEventsPaneView;
import com.youngledo.jmcfx.ui.jfx.JavaFxEventsPageView;
import com.youngledo.jmcfx.ui.jvm.JvmInternalsPaneView;
import com.youngledo.jmcfx.ui.jvm.JvmInternalsPagesView;
import com.youngledo.jmcfx.ui.leaks.LeakSuspectsPaneView;
import com.youngledo.jmcfx.ui.leaks.LeakSuspectsPageView;
import com.youngledo.jmcfx.ui.locks.LocksPaneView;
import com.youngledo.jmcfx.ui.locks.LocksPageView;
import com.youngledo.jmcfx.ui.metadata.MetadataPageView;
import com.youngledo.jmcfx.ui.metadata.MetadataPaneView;
import com.youngledo.jmcfx.ui.overview.OverviewPaneView;
import com.youngledo.jmcfx.ui.overview.OverviewPageView;
import com.youngledo.jmcfx.ui.profiling.ProfilingPageView;
import com.youngledo.jmcfx.ui.profiling.ProfilingPaneView;
import com.youngledo.jmcfx.ui.socketio.SocketIoPaneView;
import com.youngledo.jmcfx.ui.socketio.SocketIoPageView;
import com.youngledo.jmcfx.ui.threads.ThreadsPageView;
import com.youngledo.jmcfx.ui.tlab.TlabPaneView;
import com.youngledo.jmcfx.ui.tlab.TlabPageView;

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
}
