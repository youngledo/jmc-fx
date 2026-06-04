package com.youngledo.jmcfx.ui.shell;

import com.youngledo.jmcfx.application.AnalyzeHeapDumpUseCase;
import com.youngledo.jmcfx.application.HeapDumpApplicationServices;
import com.youngledo.jmcfx.ui.heapdump.HeapDumpAnalysisViewModel;
import com.youngledo.jmcfx.ui.heapdump.VirtualThreadHeapDumpAnalysisExecutor;
import com.youngledo.jmcfx.ui.i18n.I18n;

final class ShellHeapDumpWorkspaceController {

    private final HeapDumpApplicationServices services;
    private final I18n i18n;
    private final ShellLifecycleController lifecycleController;
    private HeapDumpAnalysisViewModel heapDumpAnalysisViewModel;

    ShellHeapDumpWorkspaceController(HeapDumpApplicationServices services, I18n i18n,
            ShellLifecycleController lifecycleController) {
        this.services = services;
        this.i18n = i18n;
        this.lifecycleController = lifecycleController;
    }

    void configure() {
        heapDumpAnalysisViewModel = services.heapDumpAnalysisService() == null ? null
                : new HeapDumpAnalysisViewModel(new AnalyzeHeapDumpUseCase(services),
                        new VirtualThreadHeapDumpAnalysisExecutor(), i18n);
        lifecycleController.setHeapDumpAnalysisViewModel(heapDumpAnalysisViewModel);
    }
}
