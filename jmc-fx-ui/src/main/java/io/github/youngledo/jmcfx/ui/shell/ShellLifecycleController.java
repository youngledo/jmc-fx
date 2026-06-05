package io.github.youngledo.jmcfx.ui.shell;

import java.util.List;

import io.github.youngledo.jmcfx.ui.heapdump.HeapDumpAnalysisViewModel;
import io.github.youngledo.jmcfx.ui.jvms.JvmBrowserViewModel;

final class ShellLifecycleController {

    private final AppShellViewModel viewModel;
    private final RecordingOpenExecutor recordingOpenExecutor;
    private LiveJvmPaneController jvmsPaneController;
    private JvmBrowserViewModel jvmBrowserViewModel;
    private HeapDumpAnalysisViewModel heapDumpAnalysisViewModel;

    ShellLifecycleController(AppShellViewModel viewModel, RecordingOpenExecutor recordingOpenExecutor) {
        this.viewModel = viewModel;
        this.recordingOpenExecutor = recordingOpenExecutor;
    }

    void setLiveJvmPaneController(LiveJvmPaneController jvmsPaneController) {
        this.jvmsPaneController = jvmsPaneController;
    }

    void setJvmBrowserViewModel(JvmBrowserViewModel jvmBrowserViewModel) {
        this.jvmBrowserViewModel = jvmBrowserViewModel;
    }

    void setHeapDumpAnalysisViewModel(HeapDumpAnalysisViewModel heapDumpAnalysisViewModel) {
        this.heapDumpAnalysisViewModel = heapDumpAnalysisViewModel;
    }

    void close() {
        if (jvmsPaneController != null) {
            jvmsPaneController.close();
        }
        List.copyOf(viewModel.recordingWorkspacesProperty()).forEach(viewModel::closeWorkspace);
        List.copyOf(viewModel.heapDumpWorkspacesProperty()).forEach(viewModel::closeHeapDumpWorkspace);
        if (jvmBrowserViewModel != null) {
            jvmBrowserViewModel.close();
        }
        if (viewModel.heapDumpWorkspacesProperty().isEmpty() && heapDumpAnalysisViewModel != null) {
            heapDumpAnalysisViewModel.close();
        }
        recordingOpenExecutor.close();
    }
}
