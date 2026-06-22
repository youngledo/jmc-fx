package io.github.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.application.AnalyzeRulesUseCase;
import io.github.youngledo.jmcfx.application.BrowseEventsUseCase;
import io.github.youngledo.jmcfx.application.DiagnosticFindingsUseCase;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.overview.OverviewViewModel;
import io.github.youngledo.jmcfx.ui.rules.RuleResultsViewModel;

import javafx.application.Platform;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

class WorkspaceTabsControllerTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await(5, TimeUnit.SECONDS);
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized by another test class.
        }
    }

    @Test
    void globalPagesOpenTheirOwnTabsAndKeepLiveJvmTabSelectable() {
        AppShellViewModel viewModel = new AppShellViewModel();
        TabPane tabs = new TabPane();
        WorkspaceTabsController controller = new WorkspaceTabsController(tabs, viewModel, new I18n(java.util.Locale.ENGLISH));
        controller.configure();

        viewModel.openLiveJvmWorkspace();
        LiveJvmWorkspace workspace = viewModel.liveJvmWorkspaceProperty().get();
        Tab liveJvmTab = tabFor(tabs, workspace);

        assertSame(workspace, liveJvmTab.getUserData());
        assertEquals(liveJvmTab, tabs.getSelectionModel().getSelectedItem());

        viewModel.showSection("home");

        assertSame(workspace, viewModel.selectedLiveJvmWorkspaceProperty().get());
        assertSame(GlobalWorkspaceTab.HOME, viewModel.selectedWorkspaceTabProperty().get());
        assertSame(GlobalWorkspaceTab.HOME, tabs.getSelectionModel().getSelectedItem().getUserData());
        assertEquals(2, tabs.getTabs().size());

        liveJvmTab = tabFor(tabs, workspace);
        tabs.getSelectionModel().select(liveJvmTab);

        assertEquals("jvms", viewModel.selectedSectionProperty().get());
        assertSame(workspace, viewModel.selectedWorkspaceTabProperty().get());
        assertSame(liveJvmTab, tabs.getSelectionModel().getSelectedItem());
    }

    @Test
    void globalPagesReuseTheirExistingTabs() {
        AppShellViewModel viewModel = new AppShellViewModel();
        TabPane tabs = new TabPane();
        WorkspaceTabsController controller = new WorkspaceTabsController(tabs, viewModel, new I18n(java.util.Locale.ENGLISH));
        controller.configure();

        viewModel.openLiveJvmWorkspace();
        LiveJvmWorkspace workspace = viewModel.liveJvmWorkspaceProperty().get();

        viewModel.showSection("home");

        assertEquals(2, tabs.getTabs().size());
        assertEquals(true, tabs.isVisible());
        assertEquals(true, tabs.isManaged());
        assertSame(GlobalWorkspaceTab.HOME, viewModel.selectedWorkspaceTabProperty().get());
        assertSame(GlobalWorkspaceTab.HOME, tabs.getSelectionModel().getSelectedItem().getUserData());
        assertSame(workspace, viewModel.selectedLiveJvmWorkspaceProperty().get());

        viewModel.showSection("settings");

        assertEquals(3, tabs.getTabs().size());
        assertSame(GlobalWorkspaceTab.SETTINGS, viewModel.selectedWorkspaceTabProperty().get());
        assertSame(GlobalWorkspaceTab.SETTINGS, tabs.getSelectionModel().getSelectedItem().getUserData());
        assertSame(workspace, viewModel.selectedLiveJvmWorkspaceProperty().get());

        viewModel.showSection("home");

        assertEquals(3, tabs.getTabs().size());
        assertSame(GlobalWorkspaceTab.HOME, viewModel.selectedWorkspaceTabProperty().get());
        assertSame(GlobalWorkspaceTab.HOME, tabs.getSelectionModel().getSelectedItem().getUserData());
    }

    @Test
    void openingHeapDumpAfterRecordingAndJvmKeepsHeapDumpTabFocused() {
        AppShellViewModel viewModel = new AppShellViewModel();
        TabPane tabs = new TabPane();
        WorkspaceTabsController controller = new WorkspaceTabsController(tabs, viewModel, new I18n(java.util.Locale.ENGLISH));
        controller.configure();
        RecordingWorkspace recording = openRecording(viewModel, "demo.jfr");
        viewModel.openLiveJvmWorkspace();
        HeapDumpWorkspace heapDump = new HeapDumpWorkspace(Path.of("demo.hprof"), null);

        viewModel.openHeapDump(heapDump);

        assertEquals(4, tabs.getTabs().size());
        assertSame(heapDump, tabs.getSelectionModel().getSelectedItem().getUserData());
        assertSame(heapDump, viewModel.selectedWorkspaceTabProperty().get());
        assertSame(heapDump, viewModel.selectedHeapDumpWorkspaceProperty().get());
        assertNull(viewModel.selectedWorkspaceProperty().get());
        assertSame(GlobalWorkspaceTab.HOME, tabs.getTabs().getFirst().getUserData());
        assertSame(recording, tabs.getTabs().get(1).getUserData());
        assertFalse(tabs.getTabs().isEmpty());
    }

    @Test
    void selectingRecordingTabAfterMixedWorkspaceOpensKeepsWorkspaceTabsVisible() {
        AppShellViewModel viewModel = new AppShellViewModel();
        TabPane tabs = new TabPane();
        WorkspaceTabsController controller = new WorkspaceTabsController(tabs, viewModel, new I18n(java.util.Locale.ENGLISH));
        controller.configure();
        RecordingWorkspace recording = openRecording(viewModel, "demo.jfr");
        viewModel.openLiveJvmWorkspace();
        viewModel.openHeapDump(new HeapDumpWorkspace(Path.of("demo.hprof"), null));
        Tab recordingTab = tabs.getTabs().stream()
                .filter(tab -> tab.getUserData() == recording)
                .findFirst()
                .orElseThrow();

        tabs.getSelectionModel().select(recordingTab);

        assertSame(recording, viewModel.selectedWorkspaceTabProperty().get());
        assertSame(recording, viewModel.selectedWorkspaceProperty().get());
        assertEquals(AppWorkspaceKind.RECORDING, viewModel.activeWorkspaceKindProperty().get());
        assertEquals("analysis", viewModel.selectedSectionProperty().get());
        assertEquals(4, tabs.getTabs().size());
        assertEquals(true, tabs.isVisible());
        assertEquals(true, tabs.isManaged());
        assertSame(recordingTab, tabs.getSelectionModel().getSelectedItem());
    }

    @Test
    void selectingLiveJvmAfterJfrAndHeapDumpClearsPreviousTabSelectionFirst() {
        AppShellViewModel viewModel = new AppShellViewModel();
        TabPane tabs = new TabPane();
        WorkspaceTabsController controller = new WorkspaceTabsController(tabs, viewModel, new I18n(java.util.Locale.ENGLISH));
        controller.configure();
        RecordingWorkspace recording = openRecording(viewModel, "app.jfr");
        HeapDumpWorkspace heapDump = new HeapDumpWorkspace(Path.of("jmc-fx.hprof"), null);
        viewModel.openHeapDump(heapDump);
        viewModel.openLiveJvmWorkspace();

        Tab liveJvmTab = tabs.getSelectionModel().getSelectedItem();
        assertEquals(4, tabs.getTabs().size());
        assertSame(GlobalWorkspaceTab.HOME, tabs.getTabs().get(0).getUserData());
        assertSame(recording, tabs.getTabs().get(1).getUserData());
        assertSame(heapDump, tabs.getTabs().get(2).getUserData());
        assertSame(viewModel.liveJvmWorkspaceProperty().get(), tabs.getTabs().get(3).getUserData());
        assertSame(viewModel.liveJvmWorkspaceProperty().get(), liveJvmTab.getUserData());
        assertSame(liveJvmTab, tabs.getTabs().get(3));
    }

    @Test
    void selectedClosePolicySelectsNextPreviousThenNone() {
        assertEquals(1, WorkspaceTabsController.nextSelectionIndexAfterClose(1, 3),
                "Closing a middle selected tab should select the next tab now at the same index");
        assertEquals(0, WorkspaceTabsController.nextSelectionIndexAfterClose(1, 2),
                "Closing the selected last tab should select the previous tab");
        assertEquals(-1, WorkspaceTabsController.nextSelectionIndexAfterClose(0, 1),
                "Closing the only selected tab should leave no workspace tab selected");
    }

    private static RecordingWorkspace openRecording(AppShellViewModel viewModel, String fileName) {
        return viewModel.openRecording(
                new RecordingSummary("rec-" + fileName, Path.of(fileName), fileName,
                        Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128),
                new OverviewViewModel(),
                new EventBrowserViewModel(BrowseEventsUseCase.unavailable()),
                new RuleResultsViewModel(AnalyzeRulesUseCase.empty(), new DiagnosticFindingsUseCase()));
    }

    private static Tab tabFor(TabPane tabs, Object userData) {
        return tabs.getTabs().stream()
                .filter(tab -> tab.getUserData() == userData)
                .findFirst()
                .orElseThrow();
    }
}
