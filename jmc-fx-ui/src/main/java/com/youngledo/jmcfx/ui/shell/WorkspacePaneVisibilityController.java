package com.youngledo.jmcfx.ui.shell;

import javafx.scene.layout.Pane;

final class WorkspacePaneVisibilityController {

    private final AppShellView view;
    private final AppShellViewModel viewModel;

    WorkspacePaneVisibilityController(AppShellView view, AppShellViewModel viewModel) {
        this.view = view;
        this.viewModel = viewModel;
    }

    void configure() {
        ShellWorkspacePanes panes = view.workspacePanes;
        bind(panes.homePane, "home");
        bind(panes.overviewPane, "overview");
        bind(panes.eventsPane, "events");
        bind(panes.analysisPane, "analysis");
        bind(panes.metadataPane, "metadata");
        bind(panes.advancedJfrPane, "advancedJfr");
        bind(panes.heapDumpAnalysisPane, "heapDumpAnalysis");
        bind(panes.jvmsPaneHost, "jvms");
        bind(panes.javaApplicationPane, "javaApplication");
        bind(panes.jvmInternalsPane, "jvmInternals");
        bind(panes.environmentPane, "environment");
        bind(panes.profilingPane, "profiling");
        bind(panes.exceptionsPane, "exceptions");
        bind(panes.threadsPane, "threads");
        bind(panes.fileioPane, "fileio");
        bind(panes.socketioPane, "socketio");
        bind(panes.locksPane, "locks");
        bind(panes.threadHistogramPane, "threadHistogram");
        bind(panes.securityPane, "security");
        bind(panes.nativeLibrariesPane, "nativeLibraries");
        bind(panes.threadDumpsPane, "threadDumps");
        bind(panes.heapPane, "heap");
        bind(panes.leaksPane, "leaks");
        bind(panes.tlabPane, "tlab");
        bind(panes.jvmInfoPane, "jvmInfo");
        bind(panes.gcConfigPane, "gcConfig");
        bind(panes.gcSummaryPane, "gcSummary");
        bind(panes.gcDetailsPane, "gcDetails");
        bind(panes.g1GcPane, "g1Gc");
        bind(panes.javaFxEventsPane, "javaFxEvents");
        bind(panes.compilationsPane, "compilations");
        bind(panes.codeCachePane, "codeCache");
        bind(panes.classLoadingPane, "classLoading");
        bind(panes.vmOperationsPane, "vmOperations");
        bind(panes.processesPane, "processes");
        bind(panes.envVarsPane, "envVars");
        bind(panes.sysPropsPane, "sysProps");
        bind(panes.recordingInfoPane, "recordingInfo");
        bind(panes.agentsPane, "agents");
        bind(panes.constantPoolsPane, "constantPools");
        bind(panes.settingsPane, "settings");
    }

    private void bind(Pane pane, String sectionId) {
        pane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo(sectionId));
        pane.managedProperty().bind(pane.visibleProperty());
    }
}
