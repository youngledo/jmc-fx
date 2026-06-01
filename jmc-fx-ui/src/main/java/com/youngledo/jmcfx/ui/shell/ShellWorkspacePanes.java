package com.youngledo.jmcfx.ui.shell;

import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

final class ShellWorkspacePanes {

    final StackPane stack = new StackPane();
    final VBox homePane;
    final VBox overviewPane = new VBox();
    final VBox eventsPane = new VBox();
    final VBox analysisPane = new VBox();
    final VBox metadataPane = new VBox();
    final VBox advancedJfrPane = new VBox();
    final VBox heapDumpAnalysisPane = new VBox();
    final VBox jvmsPaneHost = new VBox();
    final VBox javaApplicationPane = new VBox();
    final VBox jvmInternalsPane = new VBox();
    final VBox environmentPane = new VBox();
    final VBox profilingPane = new VBox();
    final VBox exceptionsPane = new VBox();
    final VBox threadsPane = new VBox();
    final VBox fileioPane = new VBox();
    final VBox socketioPane = new VBox();
    final VBox locksPane = new VBox();
    final VBox threadHistogramPane = new VBox();
    final VBox securityPane = new VBox();
    final VBox nativeLibrariesPane = new VBox();
    final VBox threadDumpsPane = new VBox();
    final VBox heapPane = new VBox();
    final VBox leaksPane = new VBox();
    final VBox tlabPane = new VBox();
    final VBox jvmInfoPane = new VBox();
    final VBox gcConfigPane = new VBox();
    final VBox gcSummaryPane = new VBox();
    final VBox gcDetailsPane = new VBox();
    final VBox g1GcPane = new VBox();
    final VBox javaFxEventsPane = new VBox();
    final VBox compilationsPane = new VBox();
    final VBox codeCachePane = new VBox();
    final VBox classLoadingPane = new VBox();
    final VBox vmOperationsPane = new VBox();
    final VBox processesPane = new VBox();
    final VBox envVarsPane = new VBox();
    final VBox sysPropsPane = new VBox();
    final VBox recordingInfoPane = new VBox();
    final VBox agentsPane = new VBox();
    final VBox constantPoolsPane = new VBox();
    final VBox settingsPane;

    ShellWorkspacePanes(VBox homePane, VBox settingsPane) {
        this.homePane = homePane;
        this.settingsPane = settingsPane;
        install();
    }

    void install() {
        stack.getChildren().setAll(
                homePane, overviewPane, eventsPane, analysisPane, metadataPane, advancedJfrPane,
                heapDumpAnalysisPane, jvmsPaneHost, javaApplicationPane, jvmInternalsPane, environmentPane,
                profilingPane, exceptionsPane, threadsPane, fileioPane, socketioPane, locksPane,
                threadHistogramPane, securityPane, nativeLibrariesPane, threadDumpsPane, heapPane, leaksPane,
                tlabPane, jvmInfoPane, gcConfigPane, gcSummaryPane, gcDetailsPane, g1GcPane,
                javaFxEventsPane, compilationsPane, codeCachePane, classLoadingPane, vmOperationsPane,
                processesPane, envVarsPane, sysPropsPane, recordingInfoPane, agentsPane, constantPoolsPane,
                settingsPane);
    }
}
