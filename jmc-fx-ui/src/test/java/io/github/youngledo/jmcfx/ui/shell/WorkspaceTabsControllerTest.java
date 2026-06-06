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
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.ui.events.EventBrowserViewModel;
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
    void globalPagesClearTabSelectionButKeepLiveJvmTabSelectable() {
        AppShellViewModel viewModel = new AppShellViewModel();
        TabPane tabs = new TabPane();
        WorkspaceTabsController controller = new WorkspaceTabsController(tabs, viewModel);
        controller.configure();

        viewModel.openLiveJvmWorkspace();
        LiveJvmWorkspace workspace = viewModel.liveJvmWorkspaceProperty().get();
        Tab liveJvmTab = tabs.getTabs().getFirst();

        assertSame(workspace, liveJvmTab.getUserData());
        assertEquals(liveJvmTab, tabs.getSelectionModel().getSelectedItem());

        viewModel.showSection("home");

        assertNull(viewModel.selectedLiveJvmWorkspaceProperty().get());
        assertNull(tabs.getSelectionModel().getSelectedItem());
        assertEquals(1, tabs.getTabs().size());

        tabs.getSelectionModel().select(liveJvmTab);

        assertSame(workspace, viewModel.selectedLiveJvmWorkspaceProperty().get());
        assertEquals("jvms", viewModel.selectedSectionProperty().get());
    }

    @Test
    void openingHeapDumpAfterRecordingAndJvmKeepsHeapDumpTabFocused() {
        AppShellViewModel viewModel = new AppShellViewModel();
        TabPane tabs = new TabPane();
        WorkspaceTabsController controller = new WorkspaceTabsController(tabs, viewModel);
        controller.configure();
        RecordingWorkspace recording = openRecording(viewModel, "demo.jfr");
        viewModel.openLiveJvmWorkspace();
        HeapDumpWorkspace heapDump = new HeapDumpWorkspace(Path.of("demo.hprof"), null);

        viewModel.openHeapDump(heapDump);

        assertEquals(3, tabs.getTabs().size());
        assertSame(heapDump, tabs.getSelectionModel().getSelectedItem().getUserData());
        assertSame(heapDump, viewModel.selectedWorkspaceTabProperty().get());
        assertSame(heapDump, viewModel.selectedHeapDumpWorkspaceProperty().get());
        assertNull(viewModel.selectedWorkspaceProperty().get());
        assertSame(recording, tabs.getTabs().getFirst().getUserData());
        assertFalse(tabs.getTabs().isEmpty());
    }

    @Test
    void selectingRecordingTabAfterMixedWorkspaceOpensKeepsWorkspaceTabsVisible() {
        AppShellViewModel viewModel = new AppShellViewModel();
        TabPane tabs = new TabPane();
        WorkspaceTabsController controller = new WorkspaceTabsController(tabs, viewModel);
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
        assertEquals(3, tabs.getTabs().size());
        assertEquals(true, tabs.isVisible());
        assertEquals(true, tabs.isManaged());
        assertSame(recordingTab, tabs.getSelectionModel().getSelectedItem());
    }

    @Test
    void selectingLiveJvmAfterJfrAndHeapDumpClearsPreviousTabSelectionFirst() {
        AppShellViewModel viewModel = new AppShellViewModel();
        TabPane tabs = new TabPane();
        WorkspaceTabsController controller = new WorkspaceTabsController(tabs, viewModel);
        controller.configure();
        RecordingWorkspace recording = openRecording(viewModel, "app.jfr");
        HeapDumpWorkspace heapDump = new HeapDumpWorkspace(Path.of("jmc-fx.hprof"), null);
        viewModel.openHeapDump(heapDump);
        viewModel.openLiveJvmWorkspace();

        Tab liveJvmTab = tabs.getSelectionModel().getSelectedItem();
        assertEquals(3, tabs.getTabs().size());
        assertSame(recording, tabs.getTabs().get(0).getUserData());
        assertSame(heapDump, tabs.getTabs().get(1).getUserData());
        assertSame(viewModel.liveJvmWorkspaceProperty().get(), tabs.getTabs().get(2).getUserData());
        assertSame(viewModel.liveJvmWorkspaceProperty().get(), liveJvmTab.getUserData());
        assertSame(liveJvmTab, tabs.getTabs().get(2));
    }

    private static RecordingWorkspace openRecording(AppShellViewModel viewModel, String fileName) {
        return viewModel.openRecording(
                new RecordingSummary("rec-" + fileName, Path.of(fileName), fileName,
                        Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128),
                new OverviewViewModel(),
                new EventBrowserViewModel(BrowseEventsUseCase.unavailable()),
                new RuleResultsViewModel(AnalyzeRulesUseCase.empty()));
    }
}
