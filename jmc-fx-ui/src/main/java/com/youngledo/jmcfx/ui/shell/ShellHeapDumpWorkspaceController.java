package com.youngledo.jmcfx.ui.shell;

import com.youngledo.jmcfx.ui.heapdump.HeapDumpAnalysisViewModel;
import com.youngledo.jmcfx.ui.heapdump.VirtualThreadHeapDumpAnalysisExecutor;
import com.youngledo.jmcfx.ui.i18n.I18n;

final class ShellHeapDumpWorkspaceController {

    private final HeapDumpServices services;
    private final I18n i18n;
    private final ShellLifecycleController lifecycleController;
    private HeapDumpAnalysisViewModel heapDumpAnalysisViewModel;

    ShellHeapDumpWorkspaceController(HeapDumpServices services, I18n i18n,
            ShellLifecycleController lifecycleController) {
        this.services = services;
        this.i18n = i18n;
        this.lifecycleController = lifecycleController;
    }

    void configure() {
        heapDumpAnalysisViewModel = services.heapDumpAnalysisService() == null ? null
                : new HeapDumpAnalysisViewModel(services.heapDumpAnalysisService(),
                        new VirtualThreadHeapDumpAnalysisExecutor(), i18n);
        lifecycleController.setHeapDumpAnalysisViewModel(heapDumpAnalysisViewModel);
    }
}
