package io.github.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.util.TableExportRegistration;
import io.github.youngledo.jmcfx.ui.util.TableExportRequest;
import io.github.youngledo.jmcfx.ui.util.TableExportScope;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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
        assertEquals("Button", view.jvmsClearNotificationHistoryButton.getClass().getSimpleName());
        assertEquals("TextField", view.jvmsNotificationHistoryFilterField.getClass().getSimpleName());
        assertEquals("TableView", view.jvmsMonitoringSubscriptionsTable.getClass().getSimpleName());
        assertTrue(view.jvmsMonitoringSubscriptionsTable.getStyleClass().contains("dense-table"));
        assertEquals("LineChart", view.jvmsMonitoringChart.getClass().getSimpleName());
        assertEquals("TableView", view.jvmsMonitoringSamplesTable.getClass().getSimpleName());
        assertEquals("TableView", view.jvmsMonitoringNotificationsTable.getClass().getSimpleName());
    }

    @Test
    void liveJvmExportRegistrationsDescribeActiveSessionTablesWithoutTimeRange() {
        LiveJvmPaneView view = new LiveJvmPaneView();
        LiveJvmPaneController controller = new LiveJvmPaneController(view);

        List<TableExportRequest> requests = controller.exportRegistrations().stream()
                .map(TableExportRegistration::requestSupplier)
                .map(java.util.function.Supplier::get)
                .toList();

        assertEquals(4, requests.size());
        assertTrue(requests.stream().allMatch(request -> "Live JVM session".equals(request.context().workspace())));
        assertTrue(requests.stream().allMatch(request -> TableExportScope.CURRENT_VIEW == request.context().rowScope()));
        assertTrue(requests.stream().allMatch(request -> TableExportScope.VISIBLE_COLUMNS == request.context().columnScope()));
        assertTrue(requests.stream().allMatch(request -> request.context().table() != null
                && !request.context().table().isBlank()));
        assertNull(requests.getFirst().context().timeRange());
        assertTrue(requests.stream().map(request -> request.context().table()).toList()
                .containsAll(List.of("Discovered JVMs", "JMX Monitoring Subscriptions",
                        "JMX Samples", "JMX Notifications")));
    }

    @Test
    void exportContextMenuSecondaryClickDoesNotChangeTableSelection() throws Exception {
        TableView<String> table = new TableView<>();
        TableColumn<String, String> column = new TableColumn<>("Name");
        column.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyStringWrapper(cell.getValue()));
        table.getColumns().add(column);
        table.setItems(FXCollections.observableArrayList("first", "second"));
        table.getSelectionModel().select(0);
        AppShellViewModel viewModel = new AppShellViewModel();
        I18n i18n = new I18n(java.util.Locale.ENGLISH);

        runFxAndWait(() -> {
            BorderPane root = new BorderPane(table);
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 320, 240));
            new ExportMenuInstaller(root, viewModel, i18n).install(table);
            stage.show();
            table.fireEvent(new MouseEvent(MouseEvent.MOUSE_PRESSED,
                    12, 36, 12, 36,
                    MouseButton.SECONDARY, 1,
                    false, false, false, false,
                    false, false, true, false,
                    false, false, null));
            stage.close();
        });

        assertEquals("first", table.getSelectionModel().getSelectedItem());
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
        assertTrue(source.contains("localizedColumn(\"jvms.monitoring.subscription.state\")"));
        assertTrue(source.contains("formatJmxNotificationListeningState"));
        assertTrue(source.contains("private Button jvmsAddNotificationSubscriptionButton;"));
        assertTrue(source.contains(
                "jvmsAddNotificationSubscriptionButton.setOnAction(event -> addSelectedNotificationSubscription())"));
        assertTrue(source.contains(
                "jvmsStartNotificationsButton.setOnAction(event -> jvmBrowserViewModel.startSelectedJmxNotifications())"));
        assertTrue(source.contains(
                "jvmsStopNotificationsButton.setOnAction(event -> jvmBrowserViewModel.stopSelectedJmxNotifications())"));
        assertTrue(source.contains("jvmsClearNotificationHistoryButton.setOnAction"));
        assertTrue(source.contains("clearSelectedJmxNotificationHistory"));
        assertTrue(source.contains("jvmsClearNotificationHistoryButton.textProperty().bind(i18n.text(\"jvms.monitoring.clearHistory\"))"));
        assertTrue(source.contains("jvmsNotificationHistoryFilterField.textProperty().bindBidirectional"));
        assertTrue(source.contains("jmxNotificationHistoryFilterProperty"));
        assertTrue(source.contains("jvmsNotificationHistoryFilterField.promptTextProperty().bind(i18n.text(\"jvms.monitoring.filterHistory\"))"));
        assertTrue(source.contains("java.util.List<TableExportRegistration> exportRegistrations()"));
        assertTrue(source.contains("\"Live JVM session\""));
        assertTrue(source.contains("\"JMX Monitoring Subscriptions\""));
        assertTrue(source.contains("\"JMX Samples\""));
        assertTrue(source.contains("\"JMX Notifications\""));
        assertTrue(source.contains("this::notificationFilterSummary"));
        assertTrue(source.contains("localizedColumn(\"jvms.monitoring.notification.source\")"));
        assertTrue(source.contains("jmxNotificationEventSource"));
        assertTrue(source.contains("jvmsAgentTab.textProperty().bind(i18n.text(\"jvms.agent.tab\"))"));
    }

    private static String source(String path) throws Exception {
        return java.nio.file.Files.readString(java.nio.file.Path.of(path));
    }

    private static void runFxAndWait(Runnable action) throws InterruptedException {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> failure =
                new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (failure.get() instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure.get() instanceof Error error) {
            throw error;
        }
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }
}
