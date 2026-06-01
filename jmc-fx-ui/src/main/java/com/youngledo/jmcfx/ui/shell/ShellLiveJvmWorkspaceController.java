package com.youngledo.jmcfx.ui.shell;

import java.nio.file.Path;
import java.util.function.Consumer;

import com.youngledo.jmcfx.domain.service.JdpDiscoveryService;
import com.youngledo.jmcfx.domain.service.JmxMonitoringRepository;
import com.youngledo.jmcfx.domain.service.JmxMonitoringService;
import com.youngledo.jmcfx.domain.service.SavedJvmTargetRepository;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.jvms.JvmBrowserViewModel;

import javafx.application.Platform;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class ShellLiveJvmWorkspaceController {

    private final AppShellView view;
    private final AppShellViewModel viewModel;
    private final LiveJvmServices services;
    private final I18n i18n;
    private final ShellLifecycleController lifecycleController;
    private final Consumer<Path> recordingOpenHandler;
    private JvmBrowserViewModel jvmBrowserViewModel;
    private LiveJvmPaneController jvmsPaneController;

    ShellLiveJvmWorkspaceController(AppShellView view, AppShellViewModel viewModel, LiveJvmServices services, I18n i18n,
            ShellLifecycleController lifecycleController, Consumer<Path> recordingOpenHandler) {
        this.view = view;
        this.viewModel = viewModel;
        this.services = services;
        this.i18n = i18n;
        this.lifecycleController = lifecycleController;
        this.recordingOpenHandler = recordingOpenHandler;
    }

    void configure() {
        jvmBrowserViewModel = createJvmBrowserViewModel();
        lifecycleController.setJvmBrowserViewModel(jvmBrowserViewModel);
        jvmsPaneController = new LiveJvmPaneController();
        lifecycleController.setLiveJvmPaneController(jvmsPaneController);
        VBox liveJvmRoot = jvmsPaneController.root();
        VBox.setVgrow(liveJvmRoot, Priority.ALWAYS);
        view.workspacePanes.jvmsPaneHost.getChildren().setAll(liveJvmRoot);
        jvmsPaneController.configure(i18n, jvmBrowserViewModel);
        viewModel.selectedSectionProperty().addListener((observable, oldValue, newValue) -> {
            if ("jvms".equals(newValue) && jvmsPaneController != null) {
                jvmsPaneController.refresh();
            }
        });
        if ("jvms".equals(viewModel.selectedSectionProperty().get()) && jvmsPaneController != null) {
            jvmsPaneController.refresh();
        }
    }

    private JvmBrowserViewModel createJvmBrowserViewModel() {
        if (services.jvmDiscoveryService() == null || services.jmxConnectionService() == null) {
            return null;
        }
        return new JvmBrowserViewModel(services.jvmDiscoveryService(), services.jmxConnectionService(),
                services.flightRecordingService(), services.mBeanBrowserService(), services.diagnosticCommandService(),
                services.liveMetricService(), services.jmcAgentService(), services.jmxMonitoringService(),
                services.jmxMonitoringRepository(), services.savedTargetRepository(), services.jdpDiscoveryService(),
                new com.youngledo.jmcfx.ui.jvms.VirtualThreadJvmBrowserExecutor(), Platform::runLater,
                recordingOpenHandler);
    }

    SavedJvmTargetRepository savedTargetRepository() {
        return services.savedTargetRepository();
    }

    JdpDiscoveryService jdpDiscoveryService() {
        return services.jdpDiscoveryService();
    }

    JmxMonitoringService jmxMonitoringService() {
        return services.jmxMonitoringService();
    }

    JmxMonitoringRepository jmxMonitoringRepository() {
        return services.jmxMonitoringRepository();
    }
}
