package io.github.youngledo.jmcfx.ui.shell;

import java.nio.file.Path;
import java.util.function.Consumer;

import io.github.youngledo.jmcfx.application.LiveJvmApplicationServices;
import io.github.youngledo.jmcfx.application.LiveJvmUseCases;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.jvms.JvmBrowserViewModel;

import javafx.application.Platform;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class ShellLiveJvmWorkspaceController {

    private final AppShellView view;
    private final AppShellViewModel viewModel;
    private final LiveJvmApplicationServices services;
    private final I18n i18n;
    private final ShellLifecycleController lifecycleController;
    private final Consumer<Path> recordingOpenHandler;
    private JvmBrowserViewModel jvmBrowserViewModel;
    private LiveJvmPaneController jvmsPaneController;

    ShellLiveJvmWorkspaceController(AppShellView view, AppShellViewModel viewModel, LiveJvmApplicationServices services, I18n i18n,
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

    void installExportMenus(ExportMenuInstaller installer) {
        if (jvmsPaneController == null) {
            return;
        }
        jvmsPaneController.exportRegistrations().forEach(installer::install);
    }

    private JvmBrowserViewModel createJvmBrowserViewModel() {
        if (services.jvmDiscoveryService() == null || services.jmxConnectionService() == null) {
            return null;
        }
        return new JvmBrowserViewModel(LiveJvmUseCases.from(services),
                new io.github.youngledo.jmcfx.ui.jvms.VirtualThreadJvmBrowserExecutor(), Platform::runLater,
                recordingOpenHandler);
    }
}
