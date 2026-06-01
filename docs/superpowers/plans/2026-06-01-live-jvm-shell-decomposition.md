# Live JVM Shell Decomposition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract the Live JVM workspace UI from the central shell into `LiveJvmPaneController` and `live-jvm-pane.fxml` while preserving current behavior and UI contracts.

**Architecture:** Keep `AppShellController` as the global shell owner for navigation, workspace tabs, Home, Settings, and open-file flows. Move Live JVM FXML controls, table/chart/tree setup, bindings, actions, and localized text into a dedicated controller loaded through `<fx:include>`. Use the established hybrid JavaFX approach: static page structure in small FXML, dynamic controls and bindings in Java.

**Tech Stack:** Java 26, JavaFX 26/FXML, AtlantaFX, Maven 4, JUnit 5, existing JMC FX ports/view models.

---

## File Structure

- Create `jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/live-jvm-pane.fxml`.
  - Owns the current `jvmsPane` subtree from `app-shell.fxml`.
  - Sets `fx:controller="com.youngledo.jmcfx.ui.shell.LiveJvmPaneController"`.
  - Preserves existing `fx:id` values and style classes.
- Create `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java`.
  - Owns all Live JVM `@FXML` fields.
  - Owns Live JVM table, chart, tree, action, and i18n binding methods.
  - Exposes `configure(I18n i18n, JvmBrowserViewModel viewModel)` and `refresh()`.
- Create `jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneControllerTest.java`.
  - Holds structural and source-contract assertions previously tied to shell ownership.
- Modify `jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/app-shell.fxml`.
  - Replaces the embedded Live JVM subtree with `<fx:include fx:id="jvmsPane" source="live-jvm-pane.fxml"/>`.
- Modify `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java`.
  - Keeps `@FXML private VBox jvmsPane;`.
  - Adds `@FXML private LiveJvmPaneController jvmsPaneController;`.
  - Creates `JvmBrowserViewModel` as today and hands it to `jvmsPaneController`.
  - Removes Live JVM-specific FXML fields and setup/binding/action methods.
- Modify `jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/AppShellTest.java`.
  - Keeps factory, shell, navigation, and shared helper coverage.
  - Changes Live JVM FXML assertions to assert the include boundary.
  - Moves Live JVM controller/source assertions to `LiveJvmPaneControllerTest`.
- Modify `docs/roadmap.md`.
  - Mark the first Live JVM shell decomposition phase complete while keeping broader shell decomposition open.

## UI/UX Contracts

- Affected template and workflow patterns:
  - Live JVM remains a typed workspace under the shell information architecture.
  - MBeans and Diagnostics remain split table/detail workflows.
  - Monitoring, Triggers, Flight Recorder, and JMC Agent remain control-panel workflows.
  - Overview remains an Overview page with current chart-only refresh semantics.
- Shared contracts to preserve:
  - `page-toolbar`
  - `dense-table`
  - `detail-panel`, `detail-panel-title`, `detail-panel-body`
  - `jvms-live-tab-content`
  - shell workspace visibility and tab restoration
- This plan intentionally does not change CSS, spacing, page structure, icons, or labels.

---

### Task 1: Add Split Boundary Tests

**Files:**
- Modify: `jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/AppShellTest.java`
- Create: `jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneControllerTest.java`

- [x] **Step 1: Add AppShell include contract test**

Add or update an `AppShellTest` test so the shell FXML must include the Live JVM pane and must not embed key Live JVM controls directly:

```java
@Test
void appShellIncludesLiveJvmPaneInsteadOfEmbeddingIt() throws Exception {
    Document document = fxmlDocument("app-shell.fxml");
    Element include = elementByFxId(document, "jvmsPane");

    assertEquals("fx:include", include.getTagName());
    assertEquals("live-jvm-pane.fxml", include.getAttribute("source"));
    assertNull(elementByFxIdOrNull(document, "jvmsTable"));
    assertNull(elementByFxIdOrNull(document, "jvmsMonitoringToolbar"));
    assertNull(elementByFxIdOrNull(document, "jvmsAgentTransformsTable"));
}
```

If `AppShellTest` only has `elementByFxId`, add this helper near it:

```java
private Element elementByFxIdOrNull(Document document, String fxId) {
    NodeList nodes = document.getElementsByTagName("*");
    for (int i = 0; i < nodes.getLength(); i++) {
        Node node = nodes.item(i);
        if (node instanceof Element element && fxId.equals(element.getAttribute("fx:id"))) {
            return element;
        }
    }
    return null;
}
```

- [x] **Step 2: Add Live JVM FXML structural tests**

Create `LiveJvmPaneControllerTest` with the XML helper methods copied from `AppShellTest` and assertions for the moved FXML:

```java
package com.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class LiveJvmPaneControllerTest {

    @Test
    void liveJvmPaneOwnsWorkspaceRootAndController() throws Exception {
        Document document = fxmlDocument("live-jvm-pane.fxml");
        Element root = document.getDocumentElement();

        assertEquals("VBox", root.getTagName());
        assertEquals("jvmsPane", root.getAttribute("fx:id"));
        assertEquals("com.youngledo.jmcfx.ui.shell.LiveJvmPaneController",
                root.getAttribute("fx:controller"));
    }

    @Test
    void liveJvmPanePreservesBrowserAndLiveTabs() throws Exception {
        Document document = fxmlDocument("live-jvm-pane.fxml");

        assertEquals("TableView", elementByFxId(document, "jvmsTable").getTagName());
        assertEquals("ALWAYS", elementByFxId(document, "jvmsTable").getAttribute("VBox.vgrow"));
        assertEquals("TabPane", elementByFxId(document, "jvmsLiveTabs").getTagName());
        assertEquals("Tab", elementByFxId(document, "jvmsOverviewTab").getTagName());
        assertEquals("Tab", elementByFxId(document, "jvmsSessionTab").getTagName());
        assertEquals("Tab", elementByFxId(document, "jvmsMBeanTab").getTagName());
        assertEquals("Tab", elementByFxId(document, "jvmsDiagnosticsTab").getTagName());
        assertEquals("Tab", elementByFxId(document, "jvmsTriggersTab").getTagName());
        assertEquals("Tab", elementByFxId(document, "jvmsMonitoringTab").getTagName());
        assertEquals("Tab", elementByFxId(document, "jvmsAgentTab").getTagName());
    }

    @Test
    void liveJvmPanePreservesMonitoringNotificationControls() throws Exception {
        Document document = fxmlDocument("live-jvm-pane.fxml");

        assertTrue(hasStyleClass(elementByFxId(document, "jvmsMonitoringContent"), "jvms-live-tab-content"));
        assertTrue(hasStyleClass(elementByFxId(document, "jvmsMonitoringToolbar"), "page-toolbar"));
        assertEquals("Button", elementByFxId(document, "jvmsAddMonitoringSubscriptionButton").getTagName());
        assertEquals("Button", elementByFxId(document, "jvmsSampleSubscriptionButton").getTagName());
        assertEquals("Button", elementByFxId(document, "jvmsAddNotificationSubscriptionButton").getTagName());
        assertEquals("Button", elementByFxId(document, "jvmsStartNotificationsButton").getTagName());
        assertEquals("Button", elementByFxId(document, "jvmsStopNotificationsButton").getTagName());
        assertEquals("TableView", elementByFxId(document, "jvmsMonitoringSubscriptionsTable").getTagName());
        assertTrue(hasStyleClass(elementByFxId(document, "jvmsMonitoringSubscriptionsTable"), "dense-table"));
        assertEquals("LineChart", elementByFxId(document, "jvmsMonitoringChart").getTagName());
        assertEquals("TableView", elementByFxId(document, "jvmsMonitoringSamplesTable").getTagName());
        assertEquals("TableView", elementByFxId(document, "jvmsMonitoringNotificationsTable").getTagName());
    }

    @Test
    void liveJvmPanePreservesAgentDetailPanelContract() throws Exception {
        Document document = fxmlDocument("live-jvm-pane.fxml");

        assertTrue(hasStyleClass(elementByFxId(document, "jvmsAgentContent"), "jvms-live-tab-content"));
        assertTrue(hasStyleClass(elementByFxId(document, "jvmsAgentToolbar"), "page-toolbar"));
        assertEquals("ComboBox", elementByFxId(document, "jvmsAgentPresetCombo").getTagName());
        assertEquals("TableView", elementByFxId(document, "jvmsAgentTransformsTable").getTagName());
        assertTrue(hasStyleClass(elementByFxId(document, "jvmsAgentTransformsTable"), "dense-table"));
        assertTrue(hasStyleClass(elementByFxId(document, "jvmsAgentConfigurationPane"), "detail-panel"));
        assertTrue(hasStyleClass(elementByFxId(document, "jvmsAgentConfigurationTitleLabel"),
                "detail-panel-title"));
        assertTrue(hasStyleClass(elementByFxId(document, "jvmsAgentConfigurationArea"), "detail-panel-body"));
    }

    @Test
    void liveJvmControllerOwnsLiveJvmBindingsAndActions() throws Exception {
        String source = source("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java");

        assertTrue(source.contains("class LiveJvmPaneController"));
        assertTrue(source.contains("void configure(I18n i18n, JvmBrowserViewModel viewModel)"));
        assertTrue(source.contains("@FXML private TableView<JvmConnection> jvmsTable;"));
        assertTrue(source.contains("@FXML private Button jvmsAddNotificationSubscriptionButton;"));
        assertTrue(source.contains("jvmsAddNotificationSubscriptionButton.setOnAction(event -> addSelectedNotificationSubscription())"));
        assertTrue(source.contains("jvmsStartNotificationsButton.setOnAction(event -> jvmBrowserViewModel.startSelectedNotificationSubscription())"));
        assertTrue(source.contains("jvmsStopNotificationsButton.setOnAction(event -> jvmBrowserViewModel.stopSelectedNotificationSubscription())"));
        assertTrue(source.contains("jvmsAgentTab.textProperty().bind(i18n.text(\"jvms.agent.tab\"))"));
    }

    private Document fxmlDocument(String name) throws Exception {
        try (InputStream stream = LiveJvmPaneController.class.getResourceAsStream(name)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            return factory.newDocumentBuilder().parse(stream);
        }
    }

    private static Element elementByFxId(Document document, String fxId) {
        NodeList nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && fxId.equals(element.getAttribute("fx:id"))) {
                return element;
            }
        }
        throw new AssertionError("Missing fx:id: " + fxId);
    }

    private static boolean hasStyleClass(Element element, String styleClass) {
        String direct = element.getAttribute("styleClass");
        if (direct != null && java.util.Arrays.asList(direct.split("\\s+")).contains(styleClass)) {
            return true;
        }
        NodeList children = element.getElementsByTagName("String");
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element child && styleClass.equals(child.getAttribute("fx:value"))) {
                return true;
            }
        }
        return false;
    }

    private static String source(String path) throws Exception {
        return java.nio.file.Files.readString(java.nio.file.Path.of(path));
    }
}
```

- [x] **Step 3: Run tests and confirm they fail**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -pl jmc-fx-ui -Dtest=AppShellTest,LiveJvmPaneControllerTest test
```

Expected before implementation:

```text
FAILURE: appShellIncludesLiveJvmPaneInsteadOfEmbeddingIt
FAILURE: liveJvmPaneOwnsWorkspaceRootAndController
```

The exact failure text can differ, but at least one failure must prove `live-jvm-pane.fxml` and `LiveJvmPaneController` do not exist yet.

---

### Task 2: Extract Live JVM FXML

**Files:**
- Create: `jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/live-jvm-pane.fxml`
- Modify: `jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/app-shell.fxml`

- [x] **Step 1: Create `live-jvm-pane.fxml` imports and root**

Create `live-jvm-pane.fxml` with these imports and root header:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import java.lang.String?>
<?import javafx.scene.chart.LineChart?>
<?import javafx.scene.chart.NumberAxis?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.control.ComboBox?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.ListView?>
<?import javafx.scene.control.ScrollPane?>
<?import javafx.scene.control.SplitPane?>
<?import javafx.scene.control.Tab?>
<?import javafx.scene.control.TabPane?>
<?import javafx.scene.control.TableView?>
<?import javafx.scene.control.TextArea?>
<?import javafx.scene.control.TextField?>
<?import javafx.scene.control.TreeView?>
<?import javafx.scene.layout.FlowPane?>
<?import javafx.scene.layout.HBox?>
<?import javafx.scene.layout.VBox?>

<VBox xmlns="http://javafx.com/javafx"
      xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.youngledo.jmcfx.ui.shell.LiveJvmPaneController"
      fx:id="jvmsPane"
      spacing="8">
```

Move the current `jvmsPane` children from `app-shell.fxml` into this root, from the `Label fx:id="jvmsTitleLabel"` through the closing `TabPane`/`SplitPane` content. Do not move the outer `profilingPane` or any recording/heap/settings nodes.

- [x] **Step 2: Replace embedded subtree with include**

In `app-shell.fxml`, replace the entire current Live JVM subtree:

```xml
<VBox fx:id="jvmsPane" spacing="8">
    ...
</VBox>
```

with:

```xml
<fx:include fx:id="jvmsPane" source="live-jvm-pane.fxml"/>
```

The `fx:id` on the include intentionally remains `jvmsPane`, so JavaFX injects:

```java
@FXML private VBox jvmsPane;
@FXML private LiveJvmPaneController jvmsPaneController;
```

- [x] **Step 3: Run FXML structural tests**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -pl jmc-fx-ui -Dtest=AppShellTest,LiveJvmPaneControllerTest test
```

Expected:

```text
LiveJvmPaneControllerTest liveJvmPane... structural tests pass
LiveJvmPaneControllerTest liveJvmControllerOwnsLiveJvmBindingsAndActions fails
```

The controller source-contract test should still fail until Task 3 creates the controller.

---

### Task 3: Create LiveJvmPaneController and Move Live JVM Ownership

**Files:**
- Create: `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java`
- Modify: `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java`

- [x] **Step 1: Create controller class skeleton**

Create `LiveJvmPaneController.java` in package `com.youngledo.jmcfx.ui.shell`:

```java
package com.youngledo.jmcfx.ui.shell;

import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.jvms.JvmBrowserViewModel;

public final class LiveJvmPaneController {
    private I18n i18n;
    private JvmBrowserViewModel jvmBrowserViewModel;
    private boolean configured;

    void configure(I18n i18n, JvmBrowserViewModel viewModel) {
        this.i18n = java.util.Objects.requireNonNull(i18n, "i18n");
        this.jvmBrowserViewModel = viewModel;
        if (configured || viewModel == null) {
            return;
        }
        configured = true;
        configureJvmBrowserTable();
        configureJvmRecordingsTable();
        configureMBeanBrowser();
        configureLiveJvmOverview();
        configureDiagnosticCommands();
        configureTriggers();
        configureJmxMonitoring();
        configureJmcAgentManager();
        bindJvmBrowser();
    }

    void refresh() {
        if (jvmBrowserViewModel != null) {
            jvmBrowserViewModel.refresh();
        }
    }
}
```

This skeleton will not compile until the following steps add fields and moved methods.

- [x] **Step 2: Move Live JVM FXML fields**

Move the Live JVM `@FXML` fields from `AppShellController` into `LiveJvmPaneController`. Keep these two fields in `AppShellController` only:

```java
@FXML private VBox jvmsPane;
@FXML private LiveJvmPaneController jvmsPaneController;
```

The moved fields include the browser, session, Overview, MBean, Diagnostics, Triggers, Monitoring, and JMC Agent controls:

```java
@FXML private Label jvmsTitleLabel;
@FXML private Button jvmsRefreshButton;
@FXML private Button jvmsRefreshJdpButton;
@FXML private TextField jvmsManualUrlField;
@FXML private Label jvmsManualUrlHintLabel;
@FXML private TextField jvmsManualNameField;
@FXML private Button jvmsSaveTargetButton;
@FXML private Button jvmsRemoveSavedTargetButton;
@FXML private Button jvmsConnectButton;
@FXML private Button jvmsDisconnectButton;
@FXML private Label jvmsSelectedConnectionStatusLabel;
@FXML private TableView<JvmConnection> jvmsTable;
@FXML private VBox jvmsSessionDetailPane;
@FXML private TabPane jvmsLiveTabs;
@FXML private Tab jvmsOverviewTab;
@FXML private VBox jvmsOverviewContent;
@FXML private Label jvmsOverviewPersistenceTitleLabel;
@FXML private Label jvmsOverviewPersistenceLabel;
@FXML private TableView<LiveJvmOverviewMetric> jvmsOverviewPersistenceTable;
@FXML private Label jvmsOverviewDashboardTitleLabel;
@FXML private TabPane jvmsOverviewDashboardTabs;
@FXML private Tab jvmsOverviewDashboardChartTab;
@FXML private LineChart<Number, Number> jvmsOverviewDashboardChart;
@FXML private FlowPane jvmsOverviewDashboardMetricToggles;
@FXML private Tab jvmsOverviewDashboardTableTab;
@FXML private TableView<LiveJvmOverviewMetric> jvmsOverviewDashboardTable;
@FXML private Label jvmsOverviewProcessorTitleLabel;
@FXML private TabPane jvmsOverviewProcessorTabs;
@FXML private Tab jvmsOverviewProcessorChartTab;
@FXML private LineChart<Number, Number> jvmsOverviewProcessorChart;
@FXML private FlowPane jvmsOverviewProcessorMetricToggles;
@FXML private Tab jvmsOverviewProcessorTableTab;
@FXML private TableView<LiveJvmOverviewMetric> jvmsOverviewProcessorTable;
@FXML private Label jvmsOverviewMemoryTitleLabel;
@FXML private TabPane jvmsOverviewMemoryTabs;
@FXML private Tab jvmsOverviewMemoryChartTab;
@FXML private LineChart<Number, Number> jvmsOverviewMemoryChart;
@FXML private FlowPane jvmsOverviewMemoryMetricToggles;
@FXML private Tab jvmsOverviewMemoryTableTab;
@FXML private TableView<LiveJvmOverviewMetric> jvmsOverviewMemoryTable;
@FXML private Label jvmsOverviewErrorLabel;
@FXML private Tab jvmsSessionTab;
@FXML private Label jvmsSessionTitleLabel;
@FXML private Label jvmsRuntimeSummaryLabel;
@FXML private ListView<JvmCapabilitySnapshot> jvmsCapabilitiesList;
@FXML private Button jvmsStartRecordingButton;
@FXML private Button jvmsStopRecordingButton;
@FXML private Label jvmsRecordingStatusLabel;
@FXML private TableView<FlightRecordingDescriptor> jvmsRecordingsTable;
@FXML private Label jvmsSessionErrorLabel;
@FXML private Tab jvmsMBeanTab;
@FXML private TreeView<MBeanTreeNode> jvmsMBeanTree;
@FXML private TableView<MBeanAttributeInfo> jvmsMBeanAttributesTable;
@FXML private TableView<MBeanOperationInfo> jvmsMBeanOperationsTable;
@FXML private TextField jvmsMBeanOperationArgumentsField;
@FXML private Button jvmsRefreshMBeanButton;
@FXML private Button jvmsInvokeMBeanOperationButton;
@FXML private Label jvmsMBeanResultLabel;
@FXML private Label jvmsMBeanErrorLabel;
@FXML private Tab jvmsDiagnosticsTab;
@FXML private TableView<DiagnosticCommandInfo> jvmsDiagnosticCommandsTable;
@FXML private TextField jvmsDiagnosticArgumentsField;
@FXML private Button jvmsExecuteDiagnosticCommandButton;
@FXML private Button jvmsSaveDiagnosticOutputButton;
@FXML private TextArea jvmsDiagnosticOutputArea;
@FXML private Label jvmsDiagnosticErrorLabel;
@FXML private Tab jvmsTriggersTab;
@FXML private TextField jvmsTriggerNameField;
@FXML private ComboBox<LiveMetricKind> jvmsTriggerMetricCombo;
@FXML private ComboBox<TriggerOperator> jvmsTriggerOperatorCombo;
@FXML private TextField jvmsTriggerThresholdField;
@FXML private ComboBox<TriggerActionType> jvmsTriggerActionCombo;
@FXML private ComboBox<DiagnosticCommandInfo> jvmsTriggerCommandCombo;
@FXML private Button jvmsAddTriggerButton;
@FXML private Button jvmsRemoveTriggerButton;
@FXML private Button jvmsEvaluateTriggersButton;
@FXML private TableView<TriggerRule> jvmsTriggerRulesTable;
@FXML private TableView<TriggerEvent> jvmsTriggerEventsTable;
@FXML private Label jvmsTriggerErrorLabel;
@FXML private Tab jvmsMonitoringTab;
@FXML private Button jvmsAddMonitoringSubscriptionButton;
@FXML private Button jvmsSampleSubscriptionButton;
@FXML private Button jvmsAddNotificationSubscriptionButton;
@FXML private Button jvmsStartNotificationsButton;
@FXML private Button jvmsStopNotificationsButton;
@FXML private TableView<JmxAttributeSubscription> jvmsMonitoringSubscriptionsTable;
@FXML private LineChart<Number, Number> jvmsMonitoringChart;
@FXML private TableView<JmxSubscriptionSample> jvmsMonitoringSamplesTable;
@FXML private TableView<JmxNotificationEvent> jvmsMonitoringNotificationsTable;
@FXML private Label jvmsMonitoringErrorLabel;
@FXML private Tab jvmsAgentTab;
@FXML private ComboBox<JmcAgentPreset> jvmsAgentPresetCombo;
@FXML private Button jvmsRefreshAgentButton;
@FXML private Button jvmsLoadAgentPresetButton;
@FXML private Button jvmsApplyAgentConfigurationButton;
@FXML private TableView<JmcAgentTransform> jvmsAgentTransformsTable;
@FXML private VBox jvmsAgentConfigurationPane;
@FXML private Label jvmsAgentConfigurationTitleLabel;
@FXML private TextArea jvmsAgentConfigurationArea;
@FXML private Label jvmsAgentStatusLabel;
```

Add imports for the domain model and JavaFX types used by these fields.

- [x] **Step 3: Move Live JVM setup and binding methods**

Move these methods from `AppShellController` to `LiveJvmPaneController` unchanged first, then adjust only compile errors caused by helper visibility:

```text
configureJvmBrowserTable
configureJvmRecordingsTable
configureMBeanBrowser
configureLiveJvmOverview
configureOverviewChart
configureOverviewTable
configureDiagnosticCommands
configureTriggers
configureJmxMonitoring
configureJmcAgentManager
bindJvmBrowser
bindMBeanBrowser
bindLiveJvmOverview
bindOverviewGroupTable
formatOverviewPersistence
lastOverviewObservation
updateLiveJvmOverviewRefreshTimer
stopLiveJvmOverviewRefreshTimer
bindDiagnosticCommands
bindTriggers
bindJmxMonitoring
bindJmcAgentManager
rebuildMBeanTree
selectMBeanTreeNode
removeSelectedTriggerRule
addSelectedMonitoringSubscription
addSelectedNotificationSubscription
rebuildLiveJvmOverviewGroups
rebuildLiveJvmOverviewGroup
selectedOverviewMetricKinds
rebuildOverviewMetricToggles
rebuildOverviewChart
overviewSeriesData
updateOverviewTable
liveMetricDisplayValue
liveMetricUnit
liveMetricGroup
overviewMetricOrder
rebuildJmxMonitoringChart
saveSelectedFlightRecording
saveDiagnosticCommandOutput
formatJvmState
formatJvmSource
selectedConnectionStatusText
localizedConnectionStatus
localizedJdpStatus
formatJvmRuntime
formatJvmCapability
displayCapabilityStatus
formatFlightRecordingState
formatMBeanAttributeValue
formatMBeanOperationSignature
displayDiagnosticCommandName
formatDiagnosticCommandParameters
formatTriggerCondition
formatTriggerAction
formatTriggerActionType
formatTriggerEventValue
```

Move the `liveJvmOverviewRefreshTimeline` field with these methods:

```java
private Timeline liveJvmOverviewRefreshTimeline;
```

For shared helpers currently in `AppShellController`, add local copies in `LiveJvmPaneController` rather than a broad utility refactor:

```java
private Label localizedTablePlaceholder(String key) {
    Label label = new Label();
    label.textProperty().bind(i18n.text(key));
    return label;
}

private static Label emptyTablePlaceholder() {
    return new Label();
}

private TableColumn<?, ?> localizedColumn(String key, double width) {
    TableColumn<Object, Object> column = new TableColumn<>();
    column.textProperty().bind(i18n.text(key));
    column.setPrefWidth(width);
    return column;
}
```

If generic type inference makes `localizedColumn` noisy, keep the current explicit column creation code from `AppShellController` instead of forcing a shared helper.

- [x] **Step 4: Keep file chooser work local to Live JVM**

In the moved `saveSelectedFlightRecording()` and `saveDiagnosticCommandOutput()` methods, use the Live JVM root node to find the owner window:

```java
Window owner = jvmsPane.getScene() == null ? null : jvmsPane.getScene().getWindow();
```

The stopped-recording flow should still call:

```java
jvmBrowserViewModel.stopAndSaveSelectedFlightRecording(file.toPath());
```

Do not add a shell callback for this phase; the existing `JvmBrowserViewModel` saved-recording callback continues to call `AppShellController.openRecordingInBackground` because the shell still creates the view model with that handler.

- [x] **Step 5: Update AppShellController handoff**

In `AppShellController.initialize()`, keep view model creation exactly where it is today, then replace the Live JVM configuration block:

```java
configureJvmBrowserTable();
configureJvmRecordingsTable();
configureMBeanBrowser();
configureLiveJvmOverview();
configureDiagnosticCommands();
configureTriggers();
configureJmxMonitoring();
configureJmcAgentManager();
bindJvmBrowser();
```

with:

```java
if (jvmsPaneController != null) {
    jvmsPaneController.configure(i18n, jvmBrowserViewModel);
}
```

Replace the selected-section refresh listener body:

```java
if ("jvms".equals(newValue)) {
    refreshJvmBrowser();
}
```

with:

```java
if ("jvms".equals(newValue) && jvmsPaneController != null) {
    jvmsPaneController.refresh();
}
```

Remove `refreshJvmBrowser()` from `AppShellController` after this handoff.

- [x] **Step 6: Move Live JVM localized text bindings**

Move Live JVM-specific text bindings from `AppShellController.bindLocalizedText()` into a new private method in `LiveJvmPaneController`:

```java
private void bindLocalizedText() {
    jvmsTitleLabel.textProperty().bind(i18n.text("jvms.title"));
    jvmsRefreshButton.textProperty().bind(i18n.text("jvms.refresh"));
    jvmsRefreshJdpButton.textProperty().bind(i18n.text("jvms.refreshJdp"));
    jvmsManualUrlField.promptTextProperty().bind(i18n.text("jvms.manualUrl.prompt"));
    jvmsManualUrlHintLabel.textProperty().bind(i18n.text("jvms.manualUrl.hint"));
    jvmsManualNameField.promptTextProperty().bind(i18n.text("jvms.manualName.prompt"));
    jvmsSaveTargetButton.textProperty().bind(i18n.text("jvms.saveTarget"));
    jvmsRemoveSavedTargetButton.textProperty().bind(i18n.text("jvms.removeSavedTarget"));
    jvmsConnectButton.textProperty().bind(i18n.text("jvms.connect"));
    jvmsDisconnectButton.textProperty().bind(i18n.text("jvms.disconnect"));
    jvmsOverviewTab.textProperty().bind(i18n.text("jvms.overview.tab"));
    jvmsSessionTab.textProperty().bind(i18n.text("jvms.session.tab"));
    jvmsMBeanTab.textProperty().bind(i18n.text("jvms.mbeans.tab"));
    jvmsDiagnosticsTab.textProperty().bind(i18n.text("jvms.diagnostics.tab"));
    jvmsTriggersTab.textProperty().bind(i18n.text("jvms.triggers.tab"));
    jvmsMonitoringTab.textProperty().bind(i18n.text("jvms.monitoring.tab"));
    jvmsAgentTab.textProperty().bind(i18n.text("jvms.agent.tab"));
}
```

Use the existing complete binding list from `AppShellController`; the snippet above is the minimum boundary check, not the complete list.

Call `bindLocalizedText()` inside `configure(...)` before table setup if column labels depend on `i18n`.

- [x] **Step 7: Run targeted compile/test and fix mechanical imports**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -pl jmc-fx-ui -Dtest=AppShellTest,LiveJvmPaneControllerTest test
```

Expected after mechanical fixes:

```text
Tests run: ... Failures: 0, Errors: 0
```

If compilation fails because a moved method used an AppShell-only helper, either:

- move a small private helper with the Live JVM controller, or
- keep the helper in `AppShellController` only if it is still used by non-Live-JVM code and add a local Live JVM copy.

Do not make `AppShellController` package-private methods solely so `LiveJvmPaneController` can call back into the shell.

---

### Task 4: Update Tests and Roadmap Status

**Files:**
- Modify: `jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/AppShellTest.java`
- Modify: `jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneControllerTest.java`
- Modify: `docs/roadmap.md`

- [x] **Step 1: Move Live JVM source-contract tests**

For `AppShellTest` tests that read `AppShellController.java` and assert Live JVM implementation details, change their source path to `LiveJvmPaneController.java` and move them into `LiveJvmPaneControllerTest`.

Examples of assertions that belong in `LiveJvmPaneControllerTest` after extraction:

```java
assertTrue(source.contains("jvmsMBeanTree.setRoot"));
assertTrue(source.contains("jvmsMBeanAttributesTable.setItems"));
assertTrue(source.contains("jvmsDiagnosticCommandsTable.setItems"));
assertTrue(source.contains("jvmsAddNotificationSubscriptionButton.setOnAction"));
assertTrue(source.contains("jvmsAgentPresetCombo.setItems(jvmBrowserViewModel.jmcAgentPresetsProperty())"));
assertTrue(source.contains("jvmsAgentConfigurationArea.textProperty().bindBidirectional("));
```

Assertions that should remain in `AppShellTest`:

```java
assertTrue(controller.contains("new JvmBrowserViewModel("));
assertTrue(controller.contains("this::openRecordingInBackground"));
assertTrue(controller.contains("jvmsPaneController.configure(i18n, jvmBrowserViewModel)"));
assertTrue(controller.contains("jvmsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo(\"jvms\"))"));
```

- [x] **Step 2: Add shell ownership regression test**

Add an `AppShellTest` source-contract test:

```java
@Test
void appShellDoesNotOwnLiveJvmControlFieldsAfterExtraction() throws Exception {
    String source = source("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java");

    assertTrue(source.contains("@FXML private VBox jvmsPane;"));
    assertTrue(source.contains("@FXML private LiveJvmPaneController jvmsPaneController;"));
    assertFalse(source.contains("@FXML private TableView<JvmConnection> jvmsTable;"));
    assertFalse(source.contains("@FXML private Button jvmsAddNotificationSubscriptionButton;"));
    assertFalse(source.contains("private void configureJmxMonitoring()"));
    assertFalse(source.contains("private void bindJmcAgentManager()"));
}
```

Add `assertFalse` static import if missing.

- [x] **Step 3: Update roadmap current capabilities and P1 item**

In `docs/roadmap.md`, add one bullet under `Current Capabilities > UI Shell And Workspace Navigation`:

```markdown
- Loads the Live JVM workspace through a dedicated pane/controller boundary so
  Live JVM controls, tables, charts, and bindings are no longer owned by the
  central shell controller.
```

Update the P1 shell split item to show the completed first phase and remaining scope:

```markdown
- Continue splitting `AppShellController` and `app-shell.fxml` responsibilities.
  - Completed first phase: Live JVM workspace FXML and controller ownership are
    separated from the central shell while preserving shell navigation and
    workspace selection.
  - Move remaining feature-specific binding and table setup out of the central
    shell controller where practical.
  - Keep the shell responsible for navigation, workspace selection, and global
    status.
  - Preserve existing UI/UX contracts and tests during each split.
```

- [x] **Step 4: Run targeted tests**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -pl jmc-fx-ui -Dtest=AppShellTest,LiveJvmPaneControllerTest,JvmBrowserViewModelTest test
```

Expected:

```text
Tests run: ... Failures: 0, Errors: 0
```

---

### Task 5: Full Verification and Commit

**Files:**
- All files changed above

- [x] **Step 1: Run AGENTS verification**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -v
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw verify
rg -n "<modules>|<module>" pom.xml **/pom.xml
rg -n "org.openjdk.jmc|JfrLoaderToolkit" jmc-fx-ui jmc-fx-app jmc-fx-domain
```

Expected:

```text
./mvnw -v reports Apache Maven 4.x and Java 26
./mvnw verify passes
both rg commands exit 1 with no matches
```

- [x] **Step 2: Inspect final diff**

Run:

```bash
git diff --stat
git diff -- jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/app-shell.fxml
git diff -- jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/live-jvm-pane.fxml
git diff -- jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java
git diff -- jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java
git diff -- docs/roadmap.md
```

Confirm:

```text
app-shell.fxml only includes the Live JVM pane.
live-jvm-pane.fxml contains the moved Live JVM subtree.
AppShellController keeps shell handoff only.
LiveJvmPaneController owns Live JVM UI setup and bindings.
docs/roadmap.md records the first decomposition phase.
```

- [x] **Step 3: Commit implementation**

Run:

```bash
git add jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/app-shell.fxml \
        jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/live-jvm-pane.fxml \
        jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java \
        jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java \
        jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/AppShellTest.java \
        jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneControllerTest.java \
        docs/roadmap.md
git commit -m "refactor(shell): extract live jvm pane controller"
```

If the plan file has not already been committed, include it with `git add -f docs/superpowers/plans/2026-06-01-live-jvm-shell-decomposition.md` in a separate docs commit before implementation.

---

## Self-Review

- Spec coverage:
  - Live JVM-only split: Task 2 and Task 3.
  - Hybrid FXML/Java strategy: File Structure and Task 2/3.
  - Shell remains global owner: Task 3 Step 5 and Task 4 Step 2.
  - Live JVM owns tables/charts/actions/i18n: Task 3 Step 2/3/6 and Task 4 Step 1.
  - Preserve UI/UX contracts: UI/UX Contracts and Task 1/4 structural tests.
  - Roadmap update: Task 4 Step 3.
  - Verification: Task 5.
- Placeholder scan:
  - No TBD/TODO/later placeholders.
  - Large method move is explicitly bounded by method names and compile/test checks.
- Type consistency:
  - The include id `jvmsPane` matches `jvmsPaneController` JavaFX include controller injection.
  - `configure(I18n i18n, JvmBrowserViewModel viewModel)` is used consistently in tests and shell handoff.
