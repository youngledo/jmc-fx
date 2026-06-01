# Code-First JavaFX UI Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate JMC FX from FXML-based shell layout to code-first JavaFX view classes while preserving current UI behavior and contracts.

**Architecture:** Replace `live-jvm-pane.fxml` with `LiveJvmPaneView`, then replace `app-shell.fxml` with `AppShellView`, and finally remove production FXML infrastructure. Keep JavaFX, AtlantaFX, CSS, i18n bindings, view models, and domain ports; do not introduce Compose, Kotlin, or a DI framework.

**Tech Stack:** Java 26, JavaFX 26 code-first views, AtlantaFX, Maven 4, JUnit 5, existing JMC FX UI contracts.

---

## Execution Order

This plan is ready for future execution, but `docs/roadmap.md` remains the
execution-order source. Implement this code-first migration only after earlier
P1 roadmap items ahead of it are complete or explicitly deferred.

## File Structure

- Create `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneView.java`.
  - Builds the current `live-jvm-pane.fxml` node tree in Java.
  - Owns JavaFX controls as package-private final fields.
  - Applies all existing Live JVM CSS classes and layout constraints.
- Modify `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java`.
  - Replaces `@FXML` fields with a `LiveJvmPaneView view` field.
  - Keeps existing Live JVM behavior, table setup, action wiring, i18n binding,
    and lifecycle.
  - Exposes `Region root()` or `VBox root()` for shell assembly.
- Modify `jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneControllerTest.java`.
  - Adds Java view structure tests.
  - Removes FXML parsing tests after `live-jvm-pane.fxml` is deleted.
- Delete `jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/live-jvm-pane.fxml`.
- Create `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java`.
  - Builds the current `app-shell.fxml` root layout in Java.
  - Owns shell, Home, Settings, workspace container, and feature pane nodes.
  - Creates and embeds `LiveJvmPaneController.root()` after Live JVM migration.
- Modify `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java`.
  - Replaces `@FXML` fields with an `AppShellView view` field.
  - Keeps shell behavior and existing view model coordination.
  - Uses typed view fields instead of FXML injection.
- Modify `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellFactory.java`.
  - Replaces `FXMLLoader` assembly with direct Java view/controller assembly.
  - Removes `controllerFor(...)` when no tests or FXML loaders need it.
- Modify `jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/AppShellTest.java`.
  - Adds direct Java view assembly tests.
  - Removes `app-shell.fxml` XML structure tests after shell migration.
  - Adds source-contract tests for no production `FXMLLoader` / `@FXML`.
- Delete `jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/app-shell.fxml`.
- Modify `jmc-fx-ui/pom.xml` only if the final FXML removal leaves
  `javafx-fxml` unused.
- Modify `docs/roadmap.md`.
  - Track the active code-first JavaFX migration direction and phase status.

## UI/UX Contracts

- Preserve existing visual layout, labels, actions, and navigation.
- Preserve all currently meaningful CSS classes:
  - `app-shell`
  - `sidebar`
  - `page-toolbar`
  - `dense-table`
  - `detail-panel`
  - `detail-panel-title`
  - `detail-panel-body`
  - `page-detail-tabs`
  - `jvms-live-tab-content`
- Preserve JavaFX layout constraints currently expressed in FXML:
  - `VBox.vgrow="ALWAYS"`
  - `HBox.hgrow="ALWAYS"`
  - `DividerPositions`
  - `wrapText`
  - `editable`
  - chart `animated` and `legendVisible`
- Do not change CSS colors, spacing, typography, icons, page ordering, or text.

---

### Task 1: Add Code-First Migration Guardrails

**Files:**
- Modify: `jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/AppShellTest.java`
- Modify: `jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneControllerTest.java`

- [x] **Step 1: Add source-contract test for no new FXML files after migration starts**

In `AppShellTest`, add this test near the FXML/source contract tests:

```java
@Test
void codeFirstMigrationTracksRemainingFxmlFilesExplicitly() throws Exception {
    List<String> fxmlFiles = java.nio.file.Files.walk(java.nio.file.Path.of("src/main/resources"))
            .filter(path -> path.toString().endsWith(".fxml"))
            .map(path -> java.nio.file.Path.of("src/main/resources").relativize(path).toString())
            .sorted()
            .toList();

    assertEquals(List.of(
            "com/youngledo/jmcfx/ui/shell/app-shell.fxml",
            "com/youngledo/jmcfx/ui/shell/live-jvm-pane.fxml"), fxmlFiles);
}
```

This test intentionally allows only the two known FXML files at the start of
the migration. Later tasks update it as each file is removed.

- [x] **Step 2: Add source-contract test for current FXML loader boundary**

In `AppShellTest`, add:

```java
@Test
void appShellFactoryStillUsesFxmlLoaderUntilShellViewMigration() throws Exception {
    String source = java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellFactory.java"));

    assertTrue(source.contains("new FXMLLoader("));
    assertTrue(source.contains("controllerFor("));
}
```

This test documents the current boundary before it is removed. It will be
changed in Task 3.

- [x] **Step 3: Run the guardrail tests**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -pl jmc-fx-ui -Dtest=AppShellTest#codeFirstMigrationTracksRemainingFxmlFilesExplicitly+appShellFactoryStillUsesFxmlLoaderUntilShellViewMigration test
```

Expected:

```text
Tests run: 2, Failures: 0, Errors: 0
```

- [x] **Step 4: Commit guardrails**

Run:

```bash
git add jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/AppShellTest.java
git commit -m "test(ui): track code-first javafx migration boundary"
```

---

### Task 2: Migrate Live JVM Pane From FXML To Java View

**Files:**
- Create: `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneView.java`
- Modify: `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java`
- Modify: `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java`
- Modify: `jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/app-shell.fxml`
- Modify: `jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneControllerTest.java`
- Modify: `jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/AppShellTest.java`
- Delete: `jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/live-jvm-pane.fxml`

- [ ] **Step 1: Add failing Live JVM Java view structure test**

In `LiveJvmPaneControllerTest`, add:

```java
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
```

This should fail because `LiveJvmPaneView` does not exist yet.

- [ ] **Step 2: Run the failing Live JVM Java view test**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -pl jmc-fx-ui -Dtest=LiveJvmPaneControllerTest#liveJvmPaneViewBuildsWorkspaceRootAndPrimaryRegions test
```

Expected:

```text
Compilation failure mentioning LiveJvmPaneView
```

- [ ] **Step 3: Create `LiveJvmPaneView` skeleton**

Create `LiveJvmPaneView.java`:

```java
package com.youngledo.jmcfx.ui.shell;

import com.youngledo.jmcfx.domain.model.JvmConnection;

import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

final class LiveJvmPaneView {
    final VBox root = new VBox(8);
    final Label jvmsTitleLabel = new Label();
    final SplitPane jvmsWorkspaceSplit = new SplitPane();
    final TableView<JvmConnection> jvmsTable = new TableView<>();
    final TabPane jvmsLiveTabs = new TabPane();

    LiveJvmPaneView() {
        root.setId("jvmsPane");
        root.getChildren().setAll(jvmsTitleLabel, jvmsWorkspaceSplit);
    }
}
```

This is intentionally incomplete; it makes the first view test compile and
drives the next tests.

- [ ] **Step 4: Run the Live JVM Java view test and confirm it fails on incomplete tabs**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -pl jmc-fx-ui -Dtest=LiveJvmPaneControllerTest#liveJvmPaneViewBuildsWorkspaceRootAndPrimaryRegions test
```

Expected:

```text
FAILURE on expected tab count or missing child structure
```

- [ ] **Step 5: Port the full Live JVM FXML node tree to `LiveJvmPaneView`**

Replace the skeleton with a complete Java view that declares package-private
final fields matching every control currently injected by
`LiveJvmPaneController`.

Use these implementation rules:

```java
final VBox root = new VBox(8);
final Label jvmsTitleLabel = new Label();
final SplitPane jvmsWorkspaceSplit = new SplitPane();
final VBox jvmsBrowserSidebar = new VBox(8);
final Button jvmsRefreshButton = new Button();
final Button jvmsRefreshJdpButton = new Button();
final TextField jvmsManualUrlField = new TextField();
final Label jvmsManualUrlHintLabel = new Label();
final TextField jvmsManualNameField = new TextField();
final Button jvmsSaveTargetButton = new Button();
final Button jvmsRemoveSavedTargetButton = new Button();
final Button jvmsConnectButton = new Button();
final Button jvmsDisconnectButton = new Button();
final Label jvmsSelectedConnectionStatusLabel = new Label();
final TableView<JvmConnection> jvmsTable = new TableView<>();
final VBox jvmsSessionDetailPane = new VBox(6);
final TabPane jvmsLiveTabs = new TabPane();
```

Continue declaring the remaining Overview, Session, MBeans, Diagnostics,
Triggers, Monitoring, and Agent controls with the same names currently used in
`LiveJvmPaneController`.

Use helper methods:

```java
private static void addStyle(Node node, String... styleClasses) {
    node.getStyleClass().addAll(styleClasses);
}

private static FlowPane toolbar(Node... children) {
    FlowPane toolbar = new FlowPane(8, 8, children);
    toolbar.getStyleClass().add("page-toolbar");
    return toolbar;
}

private static <T> TableView<T> denseTable() {
    TableView<T> table = new TableView<>();
    table.getStyleClass().add("dense-table");
    return table;
}
```

The view must set JavaFX properties equivalent to the old FXML:

```java
VBox.setVgrow(jvmsWorkspaceSplit, Priority.ALWAYS);
VBox.setVgrow(jvmsTable, Priority.ALWAYS);
jvmsWorkspaceSplit.setDividerPositions(0.28);
jvmsManualUrlHintLabel.setWrapText(true);
jvmsSelectedConnectionStatusLabel.setWrapText(true);
jvmsDiagnosticOutputArea.setEditable(false);
jvmsDiagnosticOutputArea.setWrapText(false);
jvmsAgentConfigurationArea.setEditable(true);
jvmsAgentConfigurationArea.setWrapText(true);
jvmsOverviewDashboardChart.setAnimated(false);
jvmsOverviewDashboardChart.setLegendVisible(false);
jvmsMonitoringChart.setAnimated(false);
jvmsMonitoringChart.setLegendVisible(true);
```

- [ ] **Step 6: Update `LiveJvmPaneController` to use `LiveJvmPaneView`**

Change the controller shape from FXML injection to view ownership:

```java
public final class LiveJvmPaneController {
    private final LiveJvmPaneView view;

    LiveJvmPaneController() {
        this(new LiveJvmPaneView());
    }

    LiveJvmPaneController(I18n i18n) {
        this(new LiveJvmPaneView());
        this.i18n = java.util.Objects.requireNonNull(i18n, "i18n");
    }

    LiveJvmPaneController(LiveJvmPaneView view) {
        this.view = java.util.Objects.requireNonNull(view, "view");
    }

    VBox root() {
        return view.root;
    }
}
```

Then replace each direct field access in the controller with `view.<field>`.
For example:

```java
view.jvmsTable.setPlaceholder(emptyTablePlaceholder());
view.jvmsRefreshButton.setOnAction(event -> refresh());
view.jvmsAgentTab.textProperty().bind(i18n.text("jvms.agent.tab"));
```

Do not change `JvmBrowserViewModel` behavior in this task.

- [ ] **Step 7: Replace shell include with Java-created Live JVM root**

In `app-shell.fxml`, replace:

```xml
<fx:include fx:id="jvmsPane" source="live-jvm-pane.fxml"/>
```

with:

```xml
<VBox fx:id="jvmsPaneHost" spacing="0"/>
```

In `AppShellController`, replace:

```java
@FXML private VBox jvmsPane;
@FXML private LiveJvmPaneController jvmsPaneController;
```

with:

```java
@FXML private VBox jvmsPaneHost;
private LiveJvmPaneController jvmsPaneController;
```

After FXML injection, create and attach the Live JVM controller:

```java
jvmsPaneController = new LiveJvmPaneController();
jvmsPaneHost.getChildren().setAll(jvmsPaneController.root());
VBox.setVgrow(jvmsPaneController.root(), Priority.ALWAYS);
```

Update shell visibility bindings from `jvmsPane` to `jvmsPaneHost`.

- [ ] **Step 8: Update tests for Live JVM Java view migration**

Update `AppShellTest.appShellIncludesLiveJvmPaneInsteadOfEmbeddingIt` so it now
asserts the host boundary:

```java
Element host = elementByFxId(document, "jvmsPaneHost");
assertEquals("VBox", host.getTagName());
assertNull(elementByFxIdOrNull(document, "jvmsTable"));
assertNull(elementByFxIdOrNull(document, "jvmsMonitoringToolbar"));
assertNull(elementByFxIdOrNull(document, "jvmsAgentTransformsTable"));
```

Update source-contract tests from:

```java
assertTrue(source.contains("@FXML private VBox jvmsPane;"));
assertTrue(source.contains("@FXML private LiveJvmPaneController jvmsPaneController;"));
```

to:

```java
assertTrue(source.contains("@FXML private VBox jvmsPaneHost;"));
assertTrue(source.contains("private LiveJvmPaneController jvmsPaneController;"));
assertTrue(source.contains("new LiveJvmPaneController()"));
```

- [ ] **Step 9: Delete `live-jvm-pane.fxml` and update FXML guardrail**

Delete:

```bash
rm jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/live-jvm-pane.fxml
```

Then update the guardrail test expected list to:

```java
assertEquals(List.of("com/youngledo/jmcfx/ui/shell/app-shell.fxml"), fxmlFiles);
```

- [ ] **Step 10: Run targeted Live JVM migration tests**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -pl jmc-fx-ui -Dtest=AppShellTest,LiveJvmPaneControllerTest,JvmBrowserViewModelTest test
```

Expected:

```text
Tests run: ... Failures: 0, Errors: 0
```

- [ ] **Step 11: Run app startup smoke check**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -pl jmc-fx-app -am org.openjfx:javafx-maven-plugin:0.0.8:run
```

Expected:

```text
Application starts without Unable to load app shell or LoadException
```

If the process keeps running after the window opens, stop it with `Ctrl-C` or
kill the Maven/Java child process after confirming startup.

- [ ] **Step 12: Commit Live JVM Java view migration**

Run:

```bash
git add \
  jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneView.java \
  jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java \
  jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java \
  jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/app-shell.fxml \
  jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/AppShellTest.java \
  jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneControllerTest.java
git add -u jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/live-jvm-pane.fxml
git commit -m "refactor(ui): migrate live jvm pane to java view"
```

---

### Task 3: Migrate App Shell From FXML To Java View

**Files:**
- Create: `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java`
- Modify: `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java`
- Modify: `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellFactory.java`
- Modify: `jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/AppShellTest.java`
- Delete: `jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/app-shell.fxml`

- [ ] **Step 1: Add failing `AppShellView` structure test**

In `AppShellTest`, add:

```java
@Test
void appShellViewBuildsRootShellAndWorkspaceRegions() {
    AppShellView view = new AppShellView();

    assertEquals("BorderPane", view.root.getClass().getSimpleName());
    assertTrue(view.root.getStyleClass().contains("app-shell"));
    assertEquals("StackPane", view.workspaceStack.getClass().getSimpleName());
    assertNotNull(view.homePane);
    assertNotNull(view.settingsPane);
    assertNotNull(view.jvmsPaneHost);
}
```

Expected before implementation: compilation failure because `AppShellView`
does not exist.

- [ ] **Step 2: Run the failing shell view test**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -pl jmc-fx-ui -Dtest=AppShellTest#appShellViewBuildsRootShellAndWorkspaceRegions test
```

Expected:

```text
Compilation failure mentioning AppShellView
```

- [ ] **Step 3: Create `AppShellView` skeleton**

Create `AppShellView.java`:

```java
package com.youngledo.jmcfx.ui.shell;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

final class AppShellView {
    final BorderPane root = new BorderPane();
    final StackPane workspaceStack = new StackPane();
    final VBox homePane = new VBox(12);
    final VBox settingsPane = new VBox(12);
    final VBox jvmsPaneHost = new VBox();

    AppShellView() {
        root.getStyleClass().add("app-shell");
        workspaceStack.getChildren().setAll(homePane, settingsPane, jvmsPaneHost);
        root.setCenter(workspaceStack);
    }
}
```

- [ ] **Step 4: Port `app-shell.fxml` node tree to `AppShellView`**

Move the current FXML shell nodes into Java code, preserving field names used by
`AppShellController`.

Use package-private final fields for all nodes that the controller currently
accesses through `@FXML`. Preserve all CSS classes and key layout constraints.

The view should create:

- global shell root
- sidebar container and navigation controls
- workspace tab area
- Home page
- Settings page
- recording page containers
- heap dump page containers
- Live JVM host
- profiling, exceptions, threads, file I/O, socket I/O, locks, GC, TLAB,
  metadata, Java application, JVM internals, environment, and advanced page
  containers currently present in `app-shell.fxml`

Do not change page order or text keys.

- [ ] **Step 5: Update `AppShellController` to use `AppShellView`**

Change constructor shape by adding a view seam:

```java
private final AppShellView view;

AppShellController(AppShellView view, AppShellViewModel viewModel, RecordingRepository recordingRepository, ...) {
    this.view = java.util.Objects.requireNonNull(view, "view");
    this.viewModel = java.util.Objects.requireNonNull(viewModel, "viewModel");
    ...
}
```

Replace `@FXML` fields with references to `view.<field>`.

Keep a compatibility constructor only if tests still instantiate
`AppShellController` directly:

```java
public AppShellController(AppShellViewModel viewModel, RecordingRepository recordingRepository, ...) {
    this(new AppShellView(), viewModel, recordingRepository, ...);
}
```

- [ ] **Step 6: Replace `AppShellFactory.create()` FXML loading**

Replace:

```java
FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/youngledo/jmcfx/ui/shell/app-shell.fxml"));
loader.setControllerFactory(type -> controllerFor(type, viewModel));
BorderPane root = loader.load();
AppShellController controller = loader.getController();
return new AppShell(root, i18n.text("app.title"), controller::close);
```

with:

```java
AppShellView view = new AppShellView();
AppShellController controller = new AppShellController(view, viewModel, recordingRepository, eventQueryService,
        ruleAnalysisService, profilingService, exceptionService, threadService,
        fileIOService, socketIOService, lockService,
        heapService, leakSuspectsService, tlabService,
        jvmInternalsService, environmentService, javaAppService,
        jvmDiscoveryService, jmxConnectionService, flightRecordingService, mBeanBrowserService,
        diagnosticCommandService, liveMetricService, jmcAgentService,
        jmxMonitoringService, jmxMonitoringRepository, jfrMetadataService, g1GcService,
        javaFxEventService, advancedJfrAnalysisService,
        savedTargetRepository, jdpDiscoveryService, heapDumpAnalysisService, i18n);
controller.initialize();
return new AppShell(view.root, i18n.text("app.title"), controller::close);
```

If `initialize()` is currently private or FXML-only, make a package-private
startup method such as `void initializeView()` and call that from the factory.
Do not keep `FXMLLoader` solely to call `initialize`.

- [ ] **Step 7: Delete `app-shell.fxml` and update guardrails**

Delete:

```bash
rm jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/app-shell.fxml
```

Update `codeFirstMigrationTracksRemainingFxmlFilesExplicitly`:

```java
assertEquals(List.of(), fxmlFiles);
```

Replace `appShellFactoryStillUsesFxmlLoaderUntilShellViewMigration` with:

```java
@Test
void appShellFactoryUsesDirectJavaViewAssembly() throws Exception {
    String source = java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellFactory.java"));

    assertFalse(source.contains("new FXMLLoader("));
    assertFalse(source.contains("controllerFor("));
    assertTrue(source.contains("new AppShellView()"));
    assertTrue(source.contains("new AppShellController("));
}
```

- [ ] **Step 8: Remove FXML test helpers and XML imports**

In `AppShellTest` and `LiveJvmPaneControllerTest`, remove helpers that only
parse FXML:

```java
DocumentBuilderFactory
Document
Element
Node
NodeList
appShellFxml()
fxml(...)
elementByFxId(...)
elementByFxIdOrNull(...)
hasStyleClass(Element ...)
```

Keep Java view tests that inspect actual JavaFX nodes and style class lists.

- [ ] **Step 9: Run shell Java view targeted tests**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -pl jmc-fx-ui -Dtest=AppShellTest,LiveJvmPaneControllerTest,AppShellViewModelTest test
```

Expected:

```text
Tests run: ... Failures: 0, Errors: 0
```

- [ ] **Step 10: Run app startup smoke check**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -pl jmc-fx-app -am org.openjfx:javafx-maven-plugin:0.0.8:run
```

Expected:

```text
Application starts without FXML LoadException
```

- [ ] **Step 11: Commit shell Java view migration**

Run:

```bash
git add \
  jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java \
  jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java \
  jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellFactory.java \
  jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/AppShellTest.java \
  jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneControllerTest.java
git add -u jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/app-shell.fxml
git commit -m "refactor(ui): migrate app shell to java view"
```

---

### Task 4: Remove Production FXML Infrastructure

**Files:**
- Modify: `jmc-fx-ui/pom.xml`
- Modify: `jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/AppShellTest.java`
- Modify: `docs/roadmap.md`

- [ ] **Step 1: Add final no-production-FXML test**

In `AppShellTest`, add:

```java
@Test
void productionUiNoLongerUsesFxml() throws Exception {
    List<String> matches = java.nio.file.Files.walk(java.nio.file.Path.of("src/main"))
            .filter(java.nio.file.Files::isRegularFile)
            .filter(path -> path.toString().endsWith(".java")
                    || path.toString().endsWith(".fxml"))
            .flatMap(path -> {
                try {
                    String text = java.nio.file.Files.readString(path);
                    if (text.contains("FXMLLoader") || text.contains("@FXML")
                            || path.toString().endsWith(".fxml")) {
                        return java.util.stream.Stream.of(path.toString());
                    }
                    return java.util.stream.Stream.empty();
                } catch (java.io.IOException exception) {
                    throw new java.io.UncheckedIOException(exception);
                }
            })
            .sorted()
            .toList();

    assertEquals(List.of(), matches);
}
```

- [ ] **Step 2: Run the final FXML usage test**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -pl jmc-fx-ui -Dtest=AppShellTest#productionUiNoLongerUsesFxml test
```

Expected:

```text
Tests run: 1, Failures: 0, Errors: 0
```

- [ ] **Step 3: Remove `javafx-fxml` dependency if unused**

Check usage:

```bash
rg -n "javafx-fxml|javafx.fxml|FXMLLoader|@FXML" jmc-fx-ui jmc-fx-app pom.xml **/pom.xml
```

If only POM dependency entries remain, remove `javafx-fxml` from
`jmc-fx-ui/pom.xml` and any app module dependency that exists solely for FXML.

Do not remove `javafx-fxml` if a transitive dependency or plugin still requires
it for a documented runtime reason.

- [ ] **Step 4: Update roadmap final code-first status**

In `docs/roadmap.md`, update UI Shell current capabilities:

```markdown
- Uses code-first JavaFX view classes for shell and Live JVM workspace layout;
  production UI no longer depends on FXML loading.
```

Update P1 to remove completed code-first migration work and keep any remaining
controller decomposition or dependency cleanup as follow-on items.

- [ ] **Step 5: Run final verification**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -v
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw verify
rg -n "<modules>|<module>" pom.xml **/pom.xml
rg -n "org.openjdk.jmc|JfrLoaderToolkit" jmc-fx-ui jmc-fx-app jmc-fx-domain
rg -n "FXMLLoader|@FXML|fx:id|\\.fxml" jmc-fx-ui/src/main jmc-fx-ui/src/test
```

Expected:

```text
./mvnw -v reports Maven 4.x and Java 26
./mvnw verify passes
Maven 3 module syntax search has no matches
JMC API boundary search has no matches outside adapter
FXML production usage search has no production matches
```

- [ ] **Step 6: Commit final FXML removal**

Run:

```bash
git add jmc-fx-ui/pom.xml \
        jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/AppShellTest.java \
        docs/roadmap.md
git commit -m "refactor(ui): remove fxml infrastructure"
```

---

## Self-Review

- Spec coverage:
  - Stop adding FXML: Task 1 guardrail and Task 4 final test.
  - Live JVM migration: Task 2.
  - App shell migration: Task 3.
  - Remove FXML infrastructure: Task 4.
  - Preserve UI/UX contracts: UI/UX Contracts and targeted tests.
  - Roadmap update: Task 4 Step 4, with initial roadmap update expected before
    execution starts.
- Placeholder scan:
  - No incomplete placeholder markers.
  - Steps that require code include exact target files and representative code.
- Type consistency:
  - `LiveJvmPaneView`, `AppShellView`, `root`, `jvmsPaneHost`, and
    `workspaceStack` names are used consistently.
  - FXML removal tests move from two allowed files, to one allowed file, to no
    allowed files.
