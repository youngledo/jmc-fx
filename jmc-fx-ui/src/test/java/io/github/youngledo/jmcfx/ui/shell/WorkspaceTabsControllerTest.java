package io.github.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
}
