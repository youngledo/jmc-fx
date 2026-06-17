package io.github.youngledo.jmcfx.ui.shell;

import java.nio.file.Path;

import io.github.youngledo.jmcfx.application.HeapDumpApplicationServices;
import io.github.youngledo.jmcfx.application.LiveJvmApplicationServices;
import io.github.youngledo.jmcfx.application.RecordingApplicationServices;
import io.github.youngledo.jmcfx.application.RecordingPageUseCases;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.jvms.SavedFlightRecording;
import io.github.youngledo.jmcfx.ui.util.WorkbenchFocusSupport;
import io.github.youngledo.jmcfx.ui.util.WorkbenchFocusTarget;

import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

final class ShellRuntimeController {

    private final AppShellView view;
    private final AppShellViewModel viewModel;
    private final I18n i18n;
    private final RecordingPageUseCases recordingUseCases;
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
        this.recordingUseCases = RecordingPageUseCases.from(recordingServices);
        RecordingWorkspaceFactory recordingWorkspaceFactory =
                new RecordingWorkspaceFactory(recordingUseCases, i18n);
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
                shellLifecycleController, this::openSavedFlightRecordingInBackground);
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
        settingsPaneController = new SettingsPaneController(view.settings, viewModel, i18n,
                recordingUseCases.aiSettings());
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
        liveJvmWorkspaceController.installExportMenus(exportMenuInstaller);
        workspaceSelectionController = new WorkspaceSelectionController(viewModel, workspaceTabsController,
                pageControllerRegistry.workspacePageControllers(), recordingSectionLoader,
                backgroundWorkController::setVisible);
        workspaceSelectionController.configure();
        configureWorkbenchAccelerators();
        i18n.localeProperty().addListener((observable, oldValue, newValue) -> pageControllerRegistry.refreshOverviewLocale());
    }

    void close() {
        shellLifecycleController.close();
    }

    void openRecordingInBackground(Path path) {
        workspaceOpenCoordinator.openRecordingInBackground(path);
    }

    void openSavedFlightRecordingInBackground(SavedFlightRecording recording) {
        workspaceOpenCoordinator.openRecordingInBackground(recording.path(), recording.origin());
    }

    private void setRecordingOpening(boolean opening) {
        if (homePaneController != null) {
            homePaneController.setOpening(opening);
        }
    }

    private void configureWorkbenchAccelerators() {
        view.root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            Node focused = view.root.getScene() == null ? null : view.root.getScene().getFocusOwner();
            if (!WorkbenchFocusSupport.shouldHandleNavigationShortcut(focused, event)) {
                return;
            }
            if (WorkbenchFocusSupport.isCommandShortcut(event, KeyCode.DIGIT1)) {
                focus(WorkbenchFocusTarget.GLOBAL_NAVIGATION);
                event.consume();
            } else if (WorkbenchFocusSupport.isCommandShortcut(event, KeyCode.DIGIT2)) {
                focus(WorkbenchFocusTarget.WORKSPACE_TABS);
                event.consume();
            } else if (WorkbenchFocusSupport.isCommandShortcut(event, KeyCode.DIGIT3)) {
                focus(WorkbenchFocusTarget.PAGE_PRIMARY);
                event.consume();
            } else if (WorkbenchFocusSupport.isCommandShortcut(event, KeyCode.F)) {
                focus(WorkbenchFocusTarget.PAGE_FILTER);
                event.consume();
            }
        });
    }

    private void focus(WorkbenchFocusTarget target) {
        WorkbenchFocusSupport.requestFocusWhenReady(view.focusTarget(target));
    }
}
