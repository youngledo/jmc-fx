package io.github.youngledo.jmcfx.ui.shell;

import javafx.collections.ListChangeListener;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

final class WorkspaceTabsController {

    private final TabPane tabs;
    private final AppShellViewModel viewModel;
    private boolean updatingTabs;

    WorkspaceTabsController(TabPane tabs, AppShellViewModel viewModel) {
        this.tabs = tabs;
        this.viewModel = viewModel;
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
                default -> {
                }
            }
        });
        viewModel.workspaceTabsProperty().addListener((ListChangeListener<Object>) change -> rebuild());
        viewModel.selectedWorkspaceTabProperty().addListener((observable, oldValue, newValue) -> select(newValue));
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
            default -> throw new IllegalArgumentException("Unsupported workspace tab: " + workspace);
        };
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
}
