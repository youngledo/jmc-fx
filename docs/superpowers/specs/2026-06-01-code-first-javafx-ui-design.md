# Code-First JavaFX UI Migration Design

## Purpose

JMC FX currently uses JavaFX FXML for the application shell and Live JVM pane:

- `app-shell.fxml`
- `live-jvm-pane.fxml`

Recent shell decomposition reduced the central shell's responsibility, but it
also exposed a fragile FXML/controller-factory boundary: every included FXML
controller must be explicitly supported by `AppShellFactory`. The project
direction should now move from a hybrid FXML/controller model to a code-first
JavaFX UI architecture.

This is not a migration to Flutter, Compose Multiplatform, or a declarative
recomposition framework. JavaFX remains a scene graph toolkit with mutable
controls, observable properties, and explicit bindings. The goal is to use
plain Java, JavaFX controls, small view classes, view models, and helper
factories to get stronger typing, better refactoring support, simpler startup
paths, and clearer component boundaries.

## Goals

- Stop adding new FXML files.
- Replace existing FXML with Java view classes over time.
- Keep JavaFX, AtlantaFX, existing CSS, existing view models, and existing
  domain ports.
- Keep UI code in `jmc-fx-ui` and preserve the existing hexagonal boundary:
  UI must not call OpenJDK JMC APIs directly.
- Preserve the current UI/UX system: dense workbench layout, typed workspaces,
  shared CSS contracts, AtlantaFX base theme, reactive i18n, and existing
  workflow semantics.
- Make controller/view ownership explicit through constructors and methods
  instead of FXML injection and `fx:id` reflection.
- Reduce `AppShellFactory` fragility by assembling typed view objects directly
  rather than routing controller creation through `FXMLLoader`.
- Keep migration incremental and always runnable.

## Non-Goals

- Do not introduce Compose Multiplatform, Kotlin, TornadoFX, MigLayout, or a
  separate UI toolkit.
- Do not build a custom virtual DOM or recomposition engine.
- Do not remove AtlantaFX or application CSS.
- Do not rewrite all UI in one change.
- Do not change user-visible layout, labels, navigation, theme behavior, or
  workflow semantics during the first migration phase.
- Do not introduce a dependency injection framework.
- Do not move service calls or JMC API usage into UI views.

## Architectural Direction

JMC FX should use code-first JavaFX components:

- A **view class** builds JavaFX nodes and exposes named nodes or binding hooks
  through typed fields and methods.
- A **controller class** owns actions, bindings, lifecycle, and handoff to view
  models. A controller may create its view, or receive a view in its
  constructor when tests benefit from that seam.
- A **view model** owns state and behavior that can be tested without a JavaFX
  scene.
- `AppShellFactory` assembles dependencies, view models, views, controllers,
  and the root `AppShell`.

The first target structure is:

```text
jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/
  AppShellView.java
  AppShellController.java
  LiveJvmPaneView.java
  LiveJvmPaneController.java
```

`AppShellView` should create the shell root `BorderPane`, sidebar, workspace
tabs, Home page, Settings page, and page containers that are currently defined
in `app-shell.fxml`.

`LiveJvmPaneView` should create the Live JVM workspace root, browser sidebar,
tabs, tables, charts, toolbars, and detail panels currently defined in
`live-jvm-pane.fxml`.

Controllers should no longer use `@FXML` fields after their corresponding view
has migrated. Instead, they should reference view fields or accessors such as
`view.jvmsTable()` or package-private final fields, depending on local style.
The project should prefer simple package-private view fields when both the view
and controller live in the same package and tests are source-contract based.

## JavaFX Code-First Style

The code-first style should be pragmatic JavaFX, not a fake Compose layer.

Use:

- `VBox`, `HBox`, `BorderPane`, `SplitPane`, `TabPane`, `TableView`,
  `TreeView`, `LineChart`, `TextField`, `Button`, and other standard JavaFX
  controls directly.
- Small helper methods for repeated JavaFX setup, such as applying style
  classes, creating toolbars, creating dense tables, and setting grow
  constraints.
- Observable property bindings for dynamic text, enabled state, visibility,
  and selected data.
- Existing CSS class names and AtlantaFX defaults.
- Existing `I18n` bindings for all user-visible labels.

Avoid:

- A generic builder DSL that hides JavaFX node types.
- Reflection-based lookup by string id.
- Recreating FXML-like string ids solely to wire Java code.
- Large anonymous blocks that make view construction harder to scan than FXML.
- One monolithic `build()` method for the whole shell.

View methods should be organized around stable UI regions:

```java
final class LiveJvmPaneView {
    final VBox root = new VBox(8);
    final TableView<JvmConnection> jvmsTable = new TableView<>();
    final TabPane jvmsLiveTabs = new TabPane();

    LiveJvmPaneView() {
        root.getChildren().setAll(titleLabel, workspaceSplit());
    }

    private SplitPane workspaceSplit() {
        SplitPane splitPane = new SplitPane(browserSidebar(), sessionDetailPane());
        splitPane.setDividerPositions(0.28);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        return splitPane;
    }
}
```

This example shows the intended shape only. The implementation plan should
write exact code against the current classes and imports.

## Migration Sequence

The migration should happen in phases so the application stays runnable:

1. **Add code-first test contracts and helper conventions.**
   - Add tests that reject new FXML files once the migration starts.
   - Add tests that verify `AppShellFactory` no longer depends on
     `FXMLLoader` after the shell migration phase.
   - Document view class naming and ownership rules.

2. **Migrate `live-jvm-pane.fxml` to `LiveJvmPaneView`.**
   - Keep `LiveJvmPaneController` behavior as intact as possible.
   - Replace `@FXML` fields with a `LiveJvmPaneView` reference.
   - Preserve all existing Live JVM CSS classes, tabs, tables, chart setup, and
     localized text.
   - Delete `live-jvm-pane.fxml` after tests pass.

3. **Migrate `app-shell.fxml` to `AppShellView`.**
   - Keep `AppShellController` as the shell behavior owner.
   - Replace `FXMLLoader` in `AppShellFactory.create()` with direct Java view
     assembly.
   - Delete `app-shell.fxml` after tests pass.

4. **Remove FXML infrastructure.**
   - Remove FXML imports and plugin/runtime dependency if no longer needed.
   - Remove XML parsing tests that only exist for FXML structure.
   - Replace them with Java source/behavior tests and runtime view-loading
     tests.

5. **Continue controller decomposition on top of code-first views.**
   - Split remaining large shell regions into view/controller pairs only when a
     page is actively being changed.
   - Avoid broad refactors that do not reduce active migration risk.

## UI/UX Contracts

The migration is structural and should not change visual behavior.

Applicable contracts from `docs/ui-ux-system.md` and `docs/ui-guidelines.md`:

- Stable shell with global Home and Settings pages.
- Typed JFR, HPROF, and Live JVM workspaces.
- Dense technical workbench layout.
- `page-toolbar` for compact toolbars.
- `dense-table` for large technical tables.
- `detail-panel`, `detail-panel-title`, `detail-panel-body` for shared detail
  panel semantics.
- `page-detail-tabs` where detail tabs are used.
- `jvms-live-tab-content` and other existing page-scoped classes where they
  still describe product semantics.
- AtlantaFX owns standard control states.
- Application CSS remains scoped to layout, workflow, and product semantics.

The code-first view classes must apply the same CSS classes that the FXML
applied. For controls that previously used FXML attributes such as
`VBox.vgrow`, `HBox.hgrow`, `wrapText`, `editable`, `animated`, or
`legendVisible`, the Java view must set the equivalent JavaFX property.

## Testing Strategy

Tests should move from XML shape checks to Java view and runtime assembly
checks.

During migration, keep both kinds where useful:

- FXML contract tests protect existing files before migration.
- New code-first view tests protect the Java replacement.

After a FXML file is deleted:

- Remove tests that parse that FXML.
- Add tests that instantiate the Java view and assert key node types,
  style classes, grow constraints, tab structure, and important defaults.
- Keep behavior tests in controllers and view models.
- Add source-contract tests only for architectural boundaries that are hard to
  assert at runtime, such as no `FXMLLoader` in `AppShellFactory`.

Minimum verification for each implementation phase:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -v
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -pl jmc-fx-ui -Dtest=AppShellTest,LiveJvmPaneControllerTest test
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw verify
rg -n "<modules>|<module>" pom.xml **/pom.xml
rg -n "org.openjdk.jmc|JfrLoaderToolkit" jmc-fx-ui jmc-fx-app jmc-fx-domain
```

For the phase that removes the final FXML file, add:

```bash
rg -n "FXMLLoader|@FXML|fx:id|\\.fxml" jmc-fx-ui/src/main jmc-fx-ui/src/test
```

Expected result after final migration: no production FXML use remains.

## Roadmap Impact

`docs/roadmap.md` should be updated to make code-first JavaFX the active UI
architecture direction:

- Current capabilities should still describe existing behavior accurately while
  FXML remains in the codebase.
- P1 should include code-first UI migration as the next shell architecture
  priority.
- The existing shell decomposition item should be reframed as part of the
  code-first migration, not as a long-term FXML split strategy.
- `AppShellFactory` dependency cleanup remains valuable, but it should happen
  in support of direct Java view assembly rather than FXML controller factory
  growth.

## Open Decisions Resolved

- JMC FX will not adopt Compose Multiplatform for this migration.
- JMC FX will not keep FXML as the long-term page layout format.
- JMC FX will not attempt a one-shot UI rewrite.
- New UI surfaces should be code-first JavaFX unless a maintainer explicitly
  approves an exception.
- Existing FXML should be deleted only after an equivalent Java view is tested
  and running.
