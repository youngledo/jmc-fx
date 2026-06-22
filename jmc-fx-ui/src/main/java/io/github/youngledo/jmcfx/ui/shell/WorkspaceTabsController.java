package io.github.youngledo.jmcfx.ui.shell;

import io.github.youngledo.jmcfx.ui.i18n.I18n;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

final class WorkspaceTabsController {

    private final TabPane tabs;
    private final AppShellViewModel viewModel;
    private final I18n i18n;
    private boolean updatingTabs;

    WorkspaceTabsController(TabPane tabs, AppShellViewModel viewModel, I18n i18n) {
        this.tabs = tabs;
        this.viewModel = viewModel;
        this.i18n = i18n;
    }

    void configure() {
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        tabs.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingTabs || newValue == null) {
                return;
            }
            switch (newValue.getUserData()) {
                case RecordingWorkspace workspace -> viewModel.selectWorkspace(workspace);
                case HeapDumpWorkspace workspace -> viewModel.selectHeapDumpWorkspace(workspace);
                case LiveJvmWorkspace ignored -> viewModel.selectLiveJvmWorkspace();
                case GlobalWorkspaceTab tab -> viewModel.selectWorkspaceTab(tab);
                default -> {
                }
            }
        });
        viewModel.workspaceTabsProperty().addListener((ListChangeListener<Object>) change -> rebuild());
        viewModel.selectedWorkspaceTabProperty().addListener((observable, oldValue, newValue) -> select(newValue));
        i18n.localeProperty().addListener((observable, oldValue, newValue) -> rebuild());
        rebuild();
    }

    void select(RecordingWorkspace recordingWorkspace, HeapDumpWorkspace heapDumpWorkspace,
            LiveJvmWorkspace liveJvmWorkspace) {
        Object workspace = liveJvmWorkspace != null ? liveJvmWorkspace
                : heapDumpWorkspace != null ? heapDumpWorkspace : recordingWorkspace;
        select(workspace);
    }

    void select(Object workspace) {
        if (workspace == null) {
            tabs.getSelectionModel().clearSelection();
            return;
        }
        tabs.getTabs().stream()
                .filter(tab -> tab.getUserData() == workspace)
                .findFirst()
                .ifPresent(tabs.getSelectionModel()::select);
    }

    private void rebuild() {
        updatingTabs = true;
        try {
            java.util.List<Tab> workspaceTabs = viewModel.workspaceTabsProperty().stream()
                    .map(this::toWorkspaceTab)
                    .toList();
            tabs.getTabs().setAll(workspaceTabs);
            boolean showTabs = !viewModel.workspaceTabsProperty().isEmpty();
            tabs.setVisible(showTabs);
            tabs.setManaged(showTabs);
            Object selectedWorkspace = viewModel.selectedWorkspaceTabProperty().get();
            if (selectedWorkspace != null || tabs.getTabs().isEmpty()) {
                select(selectedWorkspace);
            }
        } finally {
            updatingTabs = false;
        }
    }

    private Tab toWorkspaceTab(Object workspace) {
        return switch (workspace) {
            case RecordingWorkspace recordingWorkspace -> toRecordingTab(recordingWorkspace);
            case HeapDumpWorkspace heapDumpWorkspace -> toHeapDumpTab(heapDumpWorkspace);
            case LiveJvmWorkspace liveJvmWorkspace -> toLiveJvmTab(liveJvmWorkspace);
            case GlobalWorkspaceTab globalWorkspaceTab -> toGlobalTab(globalWorkspaceTab);
            default -> throw new IllegalArgumentException("Unsupported workspace tab: " + workspace);
        };
    }

    private Tab toGlobalTab(GlobalWorkspaceTab tabModel) {
        Tab tab = new Tab(tabTitleFor(tabModel, i18n));
        tab.setUserData(tabModel);
        tab.setClosable(true);
        tab.setOnClosed(event -> viewModel.closeGlobalTab(tabModel));
        return tab;
    }

    private Tab toRecordingTab(RecordingWorkspace workspace) {
        Tab tab = new Tab(tabTitleFor(workspace));
        tab.setUserData(workspace);
        tab.setClosable(true);
        tab.setOnClosed(event -> viewModel.closeWorkspace(workspace));
        return tab;
    }

    private Tab toHeapDumpTab(HeapDumpWorkspace workspace) {
        Tab tab = new Tab(tabTitleFor(workspace));
        tab.setUserData(workspace);
        tab.setClosable(true);
        tab.setOnClosed(event -> viewModel.closeHeapDumpWorkspace(workspace));
        return tab;
    }

    private Tab toLiveJvmTab(LiveJvmWorkspace workspace) {
        Tab tab = new Tab(tabTitleFor(workspace));
        tab.setUserData(workspace);
        tab.setClosable(true);
        tab.setOnClosed(event -> viewModel.closeLiveJvmWorkspace());
        return tab;
    }

    static String tabTitleFor(RecordingWorkspace workspace) {
        return workspace.recording().name();
    }

    static String tabTitleFor(HeapDumpWorkspace workspace) {
        return workspace.name();
    }

    static String tabTitleFor(LiveJvmWorkspace workspace) {
        return workspace.name();
    }

    static String tabTitleFor(GlobalWorkspaceTab tab, I18n i18n) {
        return i18n.get(tab.titleKey());
    }

    static boolean shouldShowRecordingTabs(int workspaceCount) {
        return shouldShowWorkspaceTabs(workspaceCount, 0, false);
    }

    static boolean shouldShowWorkspaceTabs(int recordingWorkspaceCount, int heapDumpWorkspaceCount) {
        return shouldShowWorkspaceTabs(recordingWorkspaceCount, heapDumpWorkspaceCount, false);
    }

    static boolean shouldShowWorkspaceTabs(int recordingWorkspaceCount, int heapDumpWorkspaceCount,
            boolean liveJvmWorkspaceOpen) {
        return recordingWorkspaceCount + heapDumpWorkspaceCount > 0 || liveJvmWorkspaceOpen;
    }

    static int nextSelectionIndexAfterClose(int closedIndex, int sizeBeforeClose) {
        int sizeAfterClose = sizeBeforeClose - 1;
        if (sizeAfterClose <= 0) {
            return -1;
        }
        return Math.min(Math.max(closedIndex, 0), sizeAfterClose - 1);
    }
}
