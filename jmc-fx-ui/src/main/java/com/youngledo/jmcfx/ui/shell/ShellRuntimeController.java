package com.youngledo.jmcfx.ui.shell;

import java.nio.file.Path;

import com.youngledo.jmcfx.application.HeapDumpApplicationServices;
import com.youngledo.jmcfx.application.LiveJvmApplicationServices;
import com.youngledo.jmcfx.application.RecordingApplicationServices;
import com.youngledo.jmcfx.application.RecordingPageUseCases;
import com.youngledo.jmcfx.ui.i18n.I18n;

final class ShellRuntimeController {

    private final AppShellView view;
    private final AppShellViewModel viewModel;
    private final I18n i18n;
    private final RecordingSectionLoader recordingSectionLoader;
    private final ShellBackgroundWorkController backgroundWorkController;
    private HomePaneController homePaneController;
    private ShellPageControllerRegistry pageControllerRegistry;
    private ShellLiveJvmWorkspaceController liveJvmWorkspaceController;
    private ShellHeapDumpWorkspaceController heapDumpWorkspaceController;
    private WorkspaceTabsController workspaceTabsController;
    private ExportMenuInstaller exportMenuInstaller;
    private WorkspaceOpenCoordinator workspaceOpenCoordinator;
    private WorkspacePaneVisibilityController workspacePaneVisibilityController;
    private WorkspaceSelectionController workspaceSelectionController;
    private ShellLifecycleController shellLifecycleController;
    private SettingsPaneController settingsPaneController;

    ShellRuntimeController(AppShellView view, AppShellViewModel viewModel, RecordingApplicationServices recordingServices,
            LiveJvmApplicationServices liveJvmServices, HeapDumpApplicationServices heapDumpServices, I18n i18n,
            RecordingOpenExecutor recordingOpenExecutor) {
        this.view = view;
        this.viewModel = viewModel;
        this.i18n = i18n;
        this.backgroundWorkController = new ShellBackgroundWorkController(view.progressBar);
        RecordingWorkspaceFactory recordingWorkspaceFactory =
                new RecordingWorkspaceFactory(RecordingPageUseCases.from(recordingServices), i18n);
        this.recordingSectionLoader = new RecordingSectionLoader(recordingOpenExecutor, i18n,
                backgroundWorkController::setVisible, viewModel::showTaskSummary, backgroundWorkController::onFxThread);
        this.pageControllerRegistry = new ShellPageControllerRegistry(view, viewModel, i18n);
        ShellRecordingWorkspaceAttacher recordingWorkspaceAttacher =
                new ShellRecordingWorkspaceAttacher(viewModel, pageControllerRegistry, i18n);
        this.workspaceOpenCoordinator = new WorkspaceOpenCoordinator(view.root, viewModel, recordingWorkspaceFactory,
                heapDumpServices, i18n, recordingOpenExecutor, recordingWorkspaceAttacher::attach,
                this::setRecordingOpening, backgroundWorkController::setVisible);
        this.shellLifecycleController = new ShellLifecycleController(viewModel, recordingOpenExecutor);
        this.liveJvmWorkspaceController = new ShellLiveJvmWorkspaceController(view, viewModel, liveJvmServices, i18n,
                shellLifecycleController, this::openRecordingInBackground);
        this.heapDumpWorkspaceController = new ShellHeapDumpWorkspaceController(heapDumpServices, i18n,
                shellLifecycleController);
    }

    void initialize() {
        backgroundWorkController.configure();
        view.sidebar.bind(viewModel);
        view.sidebar.setNavigationHandler(viewModel::showSection);
        view.sidebar.setI18n(i18n);
        homePaneController = new HomePaneController(view.home, i18n, workspaceOpenCoordinator::openRecording,
                workspaceOpenCoordinator::showOpenHeapDumpChooser, viewModel::openLiveJvmWorkspace);
        homePaneController.configure();
        settingsPaneController = new SettingsPaneController(view.settings, viewModel, i18n);
        settingsPaneController.configure();
        pageControllerRegistry.configure();
        workspaceTabsController = new WorkspaceTabsController(view.recordingTabs, viewModel);
        workspaceTabsController.configure();
        exportMenuInstaller = new ExportMenuInstaller(view.root, viewModel, i18n);
        heapDumpWorkspaceController.configure();
        liveJvmWorkspaceController.configure();
        workspacePaneVisibilityController = new WorkspacePaneVisibilityController(view, viewModel);
        workspacePaneVisibilityController.configure();
        pageControllerRegistry.installExportMenus(exportMenuInstaller);
        workspaceSelectionController = new WorkspaceSelectionController(viewModel, workspaceTabsController,
                pageControllerRegistry.workspacePageControllers(), recordingSectionLoader,
                backgroundWorkController::setVisible);
        workspaceSelectionController.configure();
        i18n.localeProperty().addListener((observable, oldValue, newValue) -> pageControllerRegistry.refreshOverviewLocale());
    }

    void close() {
        shellLifecycleController.close();
    }

    void openRecordingInBackground(Path path) {
        workspaceOpenCoordinator.openRecordingInBackground(path);
    }

    private void setRecordingOpening(boolean opening) {
        if (homePaneController != null) {
            homePaneController.setOpening(opening);
        }
    }
}
