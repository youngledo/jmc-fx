# Live JVM Shell Decomposition Design

## Purpose

`AppShellController` and `app-shell.fxml` have grown into central ownership of
shell navigation, workspace lifecycle, recording pages, heap dump pages, Live
JVM pages, table setup, chart setup, localized text, and feature-specific
actions. This makes every new Live JVM change risky because the work lands in a
large shell file rather than a bounded Live JVM surface.

This design starts the shell decomposition with the Live JVM workspace. It uses
a hybrid JavaFX approach: small FXML files keep stable static layout readable,
while dynamic tables, charts, trees, and toolbar behavior stay in Java
controllers/helpers where they are easier to test and refactor.

## Goals

- Move Live JVM page ownership out of `AppShellController`.
- Move the Live JVM FXML subtree out of `app-shell.fxml`.
- Keep the application shell responsible for global navigation, workspace
  selection, opened workspace tabs, Home, Settings, and cross-workspace status.
- Keep Live JVM controller responsibilities local to Live JVM UI setup,
  bindings, table columns, chart updates, MBean tree rendering, Monitoring
  actions, Triggers, Diagnostic Commands, Flight Recorder controls, and JMC
  Agent controls.
- Preserve existing user-visible behavior, fx:id contracts, localized text,
  CSS classes, page templates, and tests.
- Establish a decomposition path that allows complex Live JVM subregions to
  become Java view/helper classes later without a full UI rewrite.

## Non-Goals

- Do not redesign the Live JVM UI.
- Do not rewrite all FXML as Java code.
- Do not split every recording, heap dump, or settings page in this phase.
- Do not introduce a dependency injection framework.
- Do not change `JvmBrowserViewModel` behavior unless a binding seam requires
  a narrowly scoped accessor.
- Do not move JMC API usage outside `jmc-fx-adapter-jmc`.
- Do not change roadmap priorities beyond marking this decomposition phase
  complete or refined.

## Architectural Direction

The target structure is:

- `AppShellController`: global shell controller.
  - Owns Home, Settings, sidebar, workspace tabs, open-file flows, active
    workspace selection, and high-level view model handoff.
  - Creates or wires the Live JVM view model and passes it to the Live JVM
    controller.
  - Does not configure Live JVM tables, charts, buttons, tabs, MBean tree, or
    Monitoring behavior.

- `LiveJvmPaneController`: Live JVM page controller.
  - Owns Live JVM FXML controls and localized Live JVM text.
  - Configures and binds the Live JVM table, session tab, MBeans tab,
    Diagnostics tab, Triggers tab, Monitoring tab, Overview tab, and JMC Agent
    tab.
  - Exposes a small public API for shell handoff, such as
    `setViewModel(JvmBrowserViewModel viewModel)` and `dispose()`.
  - Receives shell callbacks only for shell-owned actions, such as opening a
    saved Flight Recording in a recording workspace or saving Diagnostic Command
    output through a file chooser if that remains shell-owned.

- `live-jvm-pane.fxml`: static Live JVM layout.
  - Contains the current `jvmsPane` subtree and its existing fx:id values.
  - Keeps `jvms-live-tab-content`, `page-toolbar`, `dense-table`, and existing
    page template classes unchanged.
  - Does not introduce nested cards or feature-specific detail-panel aliases.

The first implementation phase should keep layout in FXML because the Live JVM
surface is mostly static page structure: tab pane, split panes, tables, text
fields, labels, and buttons. Dynamic table columns, chart series, MBean tree
items, Overview metric toggles, graph/table updates, and action wiring remain
in Java. This is the preferred hybrid path for this codebase.

## FXML Strategy

`app-shell.fxml` should include the Live JVM pane with `<fx:include>`.

The included pane should preserve the root fx:id `jvmsPane` so existing shell
workspace visibility code can continue to target a single Live JVM node. If
JavaFX include semantics require a different controller injection pattern, the
shell may hold both:

- the included root node, for show/hide workspace behavior
- the included controller, for view model handoff and disposal

The split should not move FXML nodes across product boundaries. Only the Live
JVM workspace subtree moves to `live-jvm-pane.fxml`. Recording, HPROF, Home,
Settings, and shell nodes stay in `app-shell.fxml`.

## Controller Handoff

`AppShellController` should initialize and hand off Live JVM state after FXML
loading:

- The shell creates or receives the existing `JvmBrowserViewModel` using the
  same services as today.
- The shell passes the view model and needed callbacks into
  `LiveJvmPaneController`.
- The Live JVM controller performs configuration and binding once it has both
  FXML controls and the view model.
- When the selected workspace changes away from a Live JVM workspace, existing
  refresh timers or background UI timers owned by the Live JVM controller must
  stop if they currently stop in the shell.

The callback boundary should stay narrow. Valid callbacks include:

- opening a JFR path produced by stopping a live Flight Recording
- saving Diagnostic Command output if the file chooser remains shell-owned
- reporting shell status only if an existing user-facing status line requires
  it

Avoid passing the whole `AppShellController` into the Live JVM controller.

## UI/UX Contracts

This change is structural, not visual. Applicable contracts:

- Live JVM workspace remains a typed workspace under the shell.
- MBeans tab remains a Split Table Detail workflow.
- Monitoring and Triggers remain Control Panel style pages.
- Overview remains an Overview page with chart-only refresh behavior.
- Toolbar controls continue using `page-toolbar`.
- Large technical datasets remain dense `TableView` controls.
- Existing CSS classes and localized labels remain stable.
- No new cards, no global right-side detail panel, and no feature-specific
  detail-panel aliases are introduced.

## Testing

Add or update tests to prove the split is real and behavior stays protected:

- `AppShellTest` should assert `app-shell.fxml` uses an include for the Live JVM
  pane and no longer embeds the full Live JVM subtree directly.
- A new or updated shell contract test should assert Live JVM-specific fields
  and methods are no longer owned by `AppShellController`.
- A new `LiveJvmPaneControllerTest` should assert the new controller owns the
  Live JVM FXML fields, action bindings, i18n bindings, and important
  structural contracts previously checked through `AppShellTest`.
- Existing Live JVM behavior tests in `JvmBrowserViewModelTest` remain
  unchanged because ViewModel behavior is not moving.
- Existing `AppShellTest` coverage for workspace selection and global shell
  behavior should remain in place.

Run targeted tests:

- `./mvnw -pl jmc-fx-ui -Dtest=AppShellTest test`
- `./mvnw -pl jmc-fx-ui -Dtest=LiveJvmPaneControllerTest test`
- `./mvnw -pl jmc-fx-ui -Dtest=JvmBrowserViewModelTest test`

Run project verification required by `AGENTS.md`:

- `sdk env`
- `./mvnw -v`
- `./mvnw verify`
- `rg -n "<modules>|<module>" pom.xml **/pom.xml`
- `rg -n "org.openjdk.jmc|JfrLoaderToolkit" jmc-fx-ui jmc-fx-app jmc-fx-domain`

## Rollout Notes

After implementation, update `docs/roadmap.md` to mark the first Live JVM shell
decomposition phase complete. Keep the broader shell decomposition roadmap item
open if recording pages, heap dump pages, or settings remain inside the central
shell FXML/controller.

The implementation should be committed as a structural refactor, for example:

```text
refactor(shell): extract live jvm pane controller
```

## Open Decisions Resolved

- The project will not adopt an all-code JavaFX layout rewrite in this phase.
- The project will not keep growing one monolithic FXML/controller.
- The project will use small FXML for stable page structure and Java code for
  dynamic, test-heavy UI behavior.
- The first split target is Live JVM because it is the most recently active and
  largest changing area.
