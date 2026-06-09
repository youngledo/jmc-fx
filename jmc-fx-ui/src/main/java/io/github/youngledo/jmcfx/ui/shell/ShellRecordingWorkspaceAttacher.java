package io.github.youngledo.jmcfx.ui.shell;

import java.util.Objects;

import io.github.youngledo.jmcfx.ui.i18n.I18n;

final class ShellRecordingWorkspaceAttacher {

    private final AppShellViewModel viewModel;
    private final ShellPageControllerRegistry pageControllerRegistry;
    private final I18n i18n;

    ShellRecordingWorkspaceAttacher(AppShellViewModel viewModel,
            ShellPageControllerRegistry pageControllerRegistry, I18n i18n) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        this.pageControllerRegistry = Objects.requireNonNull(pageControllerRegistry, "pageControllerRegistry");
        this.i18n = Objects.requireNonNull(i18n, "i18n");
    }

    void attach(PreparedRecordingWorkspace prepared, long openRequestGeneration) {
        prepared.overview().showRecording(prepared.recording(),
                pageControllerRegistry.formatRecordingDetails(prepared.recording()));
        viewModel.openRecording(prepared.recording(), prepared.overview(), prepared.events(), prepared.analysis(),
                prepared.profiling(), prepared.exceptions(), prepared.threads(), prepared.fileio(),
                prepared.socketio(), prepared.locks(), prepared.heap(), prepared.leakSuspects(), prepared.tlab(),
                prepared.jvmInfo(), prepared.gcConfig(), prepared.gcSummary(), prepared.gcDetails(),
                prepared.compilations(), prepared.codeCache(), prepared.classLoading(), prepared.vmOperations(),
                prepared.environment(), prepared.javaAppOverview(), prepared.security(), prepared.nativeLibraries(),
                prepared.threadDumps(), prepared.metadata(), prepared.g1Gc(), prepared.javaFxEvents(),
                prepared.advancedJfr(), prepared.aiAssistant(), openRequestGeneration);
        viewModel.showStatus(i18n.format("status.openedRecording", prepared.recording().name()));
    }
}
