package io.github.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

class LiveJvmPaneControllerTest {

    @org.junit.jupiter.api.BeforeAll
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
    void liveJvmPaneViewBuildsWorkspaceRootAndPrimaryRegions() {
        LiveJvmPaneView view = new LiveJvmPaneView();

        assertEquals("jvmsPane", view.root.getId());
        assertTrue(view.root.getChildren().contains(view.jvmsTitleLabel));
        assertEquals("SplitPane", view.jvmsWorkspaceSplit.getClass().getSimpleName());
        assertEquals("TableView", view.jvmsTable.getClass().getSimpleName());
        assertEquals("TabPane", view.jvmsLiveTabs.getClass().getSimpleName());
        assertEquals(7, view.jvmsLiveTabs.getTabs().size());
    }

    @Test
    void liveJvmPaneOwnsWorkspaceRootAndController() {
        LiveJvmPaneView view = new LiveJvmPaneView();
        LiveJvmPaneController controller = new LiveJvmPaneController(view);

        assertEquals("jvmsPane", view.root.getId());
        assertEquals(view.root, controller.root());
    }

    @Test
    void liveJvmPanePreservesBrowserAndLiveTabs() {
        LiveJvmPaneView view = new LiveJvmPaneView();

        assertEquals("TableView", view.jvmsTable.getClass().getSimpleName());
        assertEquals(Priority.ALWAYS, VBox.getVgrow(view.jvmsTable));
        assertEquals("TabPane", view.jvmsLiveTabs.getClass().getSimpleName());
        assertTrue(view.jvmsLiveTabs.getTabs().contains(view.jvmsOverviewTab));
        assertTrue(view.jvmsLiveTabs.getTabs().contains(view.jvmsSessionTab));
        assertTrue(view.jvmsLiveTabs.getTabs().contains(view.jvmsMBeanTab));
        assertTrue(view.jvmsLiveTabs.getTabs().contains(view.jvmsDiagnosticsTab));
        assertTrue(view.jvmsLiveTabs.getTabs().contains(view.jvmsTriggersTab));
        assertTrue(view.jvmsLiveTabs.getTabs().contains(view.jvmsMonitoringTab));
        assertTrue(view.jvmsLiveTabs.getTabs().contains(view.jvmsAgentTab));
    }

    @Test
    void liveJvmPanePreservesMonitoringNotificationControls() {
        LiveJvmPaneView view = new LiveJvmPaneView();

        assertTrue(view.jvmsMonitoringTab.getContent().getStyleClass().contains("jvms-live-tab-content"));
        assertEquals("Button", view.jvmsAddMonitoringSubscriptionButton.getClass().getSimpleName());
        assertEquals("Button", view.jvmsSampleSubscriptionButton.getClass().getSimpleName());
        assertEquals("Button", view.jvmsAddNotificationSubscriptionButton.getClass().getSimpleName());
        assertEquals("Button", view.jvmsStartNotificationsButton.getClass().getSimpleName());
        assertEquals("Button", view.jvmsStopNotificationsButton.getClass().getSimpleName());
        assertEquals("TableView", view.jvmsMonitoringSubscriptionsTable.getClass().getSimpleName());
        assertTrue(view.jvmsMonitoringSubscriptionsTable.getStyleClass().contains("dense-table"));
        assertEquals("LineChart", view.jvmsMonitoringChart.getClass().getSimpleName());
        assertEquals("TableView", view.jvmsMonitoringSamplesTable.getClass().getSimpleName());
        assertEquals("TableView", view.jvmsMonitoringNotificationsTable.getClass().getSimpleName());
    }

    @Test
    void liveJvmPanePreservesAgentDetailPanelContract() {
        LiveJvmPaneView view = new LiveJvmPaneView();

        assertTrue(view.jvmsAgentTab.getContent().getStyleClass().contains("jvms-live-tab-content"));
        assertEquals("ComboBox", view.jvmsAgentPresetCombo.getClass().getSimpleName());
        assertEquals("TableView", view.jvmsAgentTransformsTable.getClass().getSimpleName());
        assertTrue(view.jvmsAgentTransformsTable.getStyleClass().contains("dense-table"));
        assertTrue(view.jvmsAgentPresetDescriptionLabel.getStyleClass().contains("event-window-status"));
        assertTrue(view.jvmsAgentConfigurationTitleLabel.getStyleClass().contains("detail-panel-title"));
        assertTrue(view.jvmsAgentConfigurationArea.getStyleClass().contains("detail-panel-body"));
        assertTrue(view.jvmsAgentApplyStatusLabel.getStyleClass().contains("event-window-status"));
    }

    @Test
    void liveJvmControllerOwnsLiveJvmBindingsAndActions() throws Exception {
        String source = source("src/main/java/io/github/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java");

        assertTrue(source.contains("class LiveJvmPaneController"));
        assertTrue(source.contains("void configure(I18n i18n, JvmBrowserViewModel viewModel)"));
        assertTrue(source.contains("private final LiveJvmPaneView view;"));
        assertTrue(source.contains("private TableView<JvmConnection> jvmsTable;"));
        assertTrue(source.contains("private TableView<JmxMonitoringSubscriptionRow> jvmsMonitoringSubscriptionsTable;"));
        assertTrue(source.contains("jvmBrowserViewModel.jmxMonitoringSubscriptionsProperty()"));
        assertTrue(source.contains("jvmBrowserViewModel.selectJmxMonitoringSubscription(newValue)"));
        assertTrue(source.contains("private Button jvmsAddNotificationSubscriptionButton;"));
        assertTrue(source.contains(
                "jvmsAddNotificationSubscriptionButton.setOnAction(event -> addSelectedNotificationSubscription())"));
        assertTrue(source.contains(
                "jvmsStartNotificationsButton.setOnAction(event -> jvmBrowserViewModel.startSelectedJmxNotifications())"));
        assertTrue(source.contains(
                "jvmsStopNotificationsButton.setOnAction(event -> jvmBrowserViewModel.stopSelectedJmxNotifications())"));
        assertTrue(source.contains("jvmsAgentTab.textProperty().bind(i18n.text(\"jvms.agent.tab\"))"));
    }

    private static String source(String path) throws Exception {
        return java.nio.file.Files.readString(java.nio.file.Path.of(path));
    }
}
