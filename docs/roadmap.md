# JMC FX Roadmap

This document is the current project-wide fact source for JMC FX capabilities,
active gaps, deferred work, and out-of-scope areas. It supersedes the narrower
historical `docs/gap-status.md` note.

It complements `docs/ui-ux-system.md` and `docs/ui-guidelines.md`; those files
remain the source of truth for UI/UX contracts and implementation-level UI
rules.

## Validation Baseline

Baseline date: 2026-05-31.

- `sdk env`: selected Java `26.0.1-tem`.
- `./mvnw -v`: Apache Maven `4.0.0-rc-5`, Java `26.0.1`.
- `./mvnw verify`: passed for the full Maven reactor.
- `rg -n "<modules>|<module>" pom.xml **/pom.xml`: no Maven 3 module syntax
  found.
- `rg -n "org.openjdk.jmc|JfrLoaderToolkit" jmc-fx-ui jmc-fx-launcher jmc-fx-application jmc-fx-domain`:
  no JMC API usage found outside the adapter boundary.

## Current Capabilities

### JFR Recording Workspaces

- Opens JFR recordings as typed recording workspaces.
- Reuses OpenJDK JMC core/headless APIs through `jmc-fx-adapter-jmc`.
- Starts users in Automated Analysis Results, with Overview and Events as
  recording-scoped follow-up pages.
- Provides recording pages for analysis, overview, events, metadata, advanced
  JFR views, Java application diagnostics, JVM internals, and environment data.
- Uses lazy section loading so expensive recording pages can load on demand.
- Prevents duplicate workspaces for the same normalized recording path.
- Keeps raw event browsing paged or sliced through the event query service
  instead of eagerly loading large recordings into JavaFX observable lists.

### Live JVM And JMX Workspaces

- Provides a Live JVM workspace for discovery, saved/manual targets, connection
  status, and connected-session tools.
- Supports local JVM discovery and manual/saved JMX service URLs.
- Supports Jolokia-style remote connector URLs through the existing manual and
  saved target workflow.
- Preserves connected sessions across discovery refreshes.
- Provides live session capability state before capability-specific actions.
- Provides Flight Recorder controls for listing, starting, stopping, saving,
  and opening recordings from a connected JVM.
- Provides MBean tree browsing, attribute inspection, operation invocation, and
  diagnostic command execution.
- Provides live Overview charts and tables for JVM metrics, with chart-only
  periodic refresh after the page is initialized.
- Provides trigger rules based on live metric samples and actions.
- Provides JMX attribute monitoring, retained samples, and persisted monitoring
  preferences.
- Provides the completed first-phase JMX notification workflow from the
  Monitoring tab: create a subscription from the selected MBean, start or stop
  listening, and retain observed events.
- Provides JMC Agent status, preset loading, and configuration application when
  the target supports it.

### HPROF Heap Dump Analysis

- Opens HPROF files from Home into typed heap dump workspaces.
- Prevents duplicate heap dump workspaces for the same normalized file path.
- Runs heap dump analysis off the FX Application Thread.
- Shows issue summaries and text report detail for JOverflow-backed analysis.
- Keeps the opened HPROF workspace focused on analysis rather than repeating
  file-open chrome inside the workspace.

### UI Shell And Workspace Navigation

- Uses a stable shell with global Home and Settings pages.
- Represents opened JFR, HPROF, and Live JVM targets as typed workspace tabs.
- Keeps Home and Settings global; selecting them does not close or clear active
  workspaces.
- Restores each workspace's typed navigation context when users select an
  existing workspace.
- Uses AtlantaFX as the base theme and application CSS afterward.
- Supports Follow System, Light, and Dark theme preferences.
- Supports English and Simplified Chinese resource bundles.
- Uses code-first JavaFX view classes for shell and Live JVM workspace layout;
  production UI no longer depends on FXML loading.
- Loads the Live JVM workspace through a dedicated pane/controller boundary so
  Live JVM controls, tables, charts, and bindings are no longer owned by the
  central shell controller.
- Includes UI/UX contract tests for JavaFX view classes, CSS, navigation,
  localized strings, and important shell invariants.

### Packaging And Release Infrastructure

- Uses Java 26, JavaFX 26, Maven 4, Maven model `4.1.0`, and Maven 4
  `<subprojects>`.
- Provides a Maven Wrapper pinned to Maven `4.0.0-rc-5`.
- Provides native packaging profiles for platform installers.
- Provides a trimmed-runtime native package path with a full-JDK fallback for
  troubleshooting.
- Includes packaging tests for the application module's native package
  configuration.

### Documentation, Licensing, And Repository Workflow

- Documents Java, Maven, build, run, and native-package commands in `README.md`.
- Documents module boundary maintenance in `docs/hexagonal-boundary-guide.md`.
- Defines project-wide UI/UX contracts in `docs/ui-ux-system.md`.
- Defines AtlantaFX and JavaFX styling rules in `docs/ui-guidelines.md`.
- Keeps JMC FX positioned as an independent JavaFX application, not an
  OpenJDK JMC Eclipse RCP/SWT fork.
- Keeps public-facing docs under the independent-project disclaimer.
- Uses a non-Node commit-message validation workflow.

## Priority Roadmap

### P0

- Keep this roadmap as the project-wide roadmap and gap matrix.
- Update this document whenever a roadmap item is completed, deliberately
  deferred, promoted to active work, removed, renamed, or materially affected
  by verification results.

### P1

P1 items should be implemented in the order listed here unless a subsequent
verification result or product decision explicitly changes the order.

- Expand Live JVM JMX notification management only if the product needs
  second-phase multi-subscription management beyond the completed first-phase
  toolbar workflow.
  - Current implementation already has persisted notification subscriptions,
    start/stop actions, and retained notification events.
  - It does not yet expose a dedicated notification-subscription list for users
    to switch among multiple simultaneous notification sources.
  - It does not yet surface active/listening state per subscription.

- Continue splitting `AppShellController` responsibilities as part of the
  code-first JavaFX architecture.
  - Completed first phase: Live JVM workspace view and controller ownership are
    separated from the central shell while preserving shell navigation and
    workspace selection.
  - Completed global-page phase: Home page actions/text/icons and Settings page
    language/theme binding now live in focused code-first controllers instead
    of the central shell controller.
  - Completed global-page view phase: Home and Settings page node ownership and
    layout construction now live in focused code-first view classes instead of
    the monolithic shell view.
  - Completed recording-overview phase: Java Application, JVM Internals, and
    Environment overview page text/actions now live in a focused controller
    instead of the central shell controller.
  - Completed recording-overview view phase: Java Application, JVM Internals,
    and Environment overview page node ownership, summary panels, metric
    grids, and page layout construction now live in `RecordingOverviewPaneView`
    instead of the monolithic shell view.
  - Completed Java Application data view phase: Exceptions, Threads, Thread
    Histogram, Security, Native Libraries, and Thread Dumps node ownership and
    page layout construction now live in `ui.javaapp.JavaApplicationDataPaneView`
    instead of the monolithic shell view.
  - Completed I/O and Locks view phase: File I/O, Socket I/O, and Locks node
    ownership, grouping bars, tab panes, charts, and dense tables now live in
    focused package pane views instead of the monolithic shell view.
  - Completed Memory view phase: Heap, Leak Suspects, and TLAB node ownership,
    split panes, timeline chart containers, trees, and dense tables now live in
    focused package pane views instead of the monolithic shell view.
  - Completed JVM/GC view phase: JVM Info, GC Config/Summary/Details,
    Compilations, Code Cache, Class Loading, VM Operations, G1 GC, and JavaFX
    Events node ownership and page layout construction now live in focused
    package pane views instead of the monolithic shell view.
  - Completed Environment view phase: Processes, Environment Variables, System
    Properties, Recording Info, Agents, and Constant Pools node ownership,
    search fields, tabs, and dense tables now live in
    `ui.environment.EnvironmentPaneView` instead of the monolithic shell view.
  - Completed final AppShellView cleanup phase: `AppShellView` now only
    aggregates shell/workspace pane views and exposes narrow page view records;
    page-family node ownership and layout construction live in focused
    code-first views.
  - Completed AppShellView host-alias cleanup phase: workspace page host panes
    stay owned by `ShellWorkspacePanes`, and `AppShellView` now wires focused
    pane views through `workspacePanes` instead of mirroring every page host as
    a separate `VBox` field.
  - Completed main Overview page package phase: recording summary binding,
    unavailable-state text, localized recording detail formatting, and locale
    refresh handling now live in `ui.overview` behind a narrow page view
    record.
  - Completed main Overview page view phase: recording overview label node
    ownership and summary-card layout construction now live in
    `ui.overview.OverviewPaneView` instead of the monolithic shell view.
  - Completed Analysis page package phase: Analysis table setup, filters,
    detail rendering, placeholder state, and related-page navigation now live
    in `ui.analysis` behind a narrow page view record.
  - Completed Analysis page view phase: Analysis filter controls, rule table,
    detail panel, split pane, and page layout construction now live in
    `ui.analysis.AnalysisPaneView` instead of the monolithic shell view.
  - Completed Event Browser page package phase: Event type tree wiring,
    dynamic columns, filters, selection-driven details, and split divider
    behavior now live in `ui.events` behind a narrow page view record.
  - Completed Event Browser page view phase: Event Browser controls, dense
    tables, detail tabs, filter bar, split pane, and page layout construction
    now live in `ui.events.EventsPaneView` instead of the monolithic shell
    view.
  - Completed Metadata page package phase: JFR metadata table setup, localized
    text, selection synchronization, summary binding, and detail binding now
    live in `ui.metadata` behind a narrow page view record.
  - Completed Metadata page view phase: JFR metadata title, summary,
    event-type table, detail panel, split pane, and page layout construction
    now live in `ui.metadata.MetadataPaneView` instead of the monolithic shell
    view.
  - Completed Advanced JFR page package phase: heatmap binding, tab text,
    selected heatmap labels, memory issue table setup, memory detail binding,
    and memory summary formatting now live in `ui.advanced` behind a narrow
    page view record.
  - Completed Advanced JFR page view phase: heatmap tab, memory tab,
    selection labels, memory table/detail panel, tab pane, and page layout
    construction now live in `ui.advanced.AdvancedJfrPaneView` instead of the
    monolithic shell view.
  - Completed Heap Dump Analysis page package phase: HPROF issue table setup,
    localized tabs, selection synchronization, detail/report binding, and
    rebind listener cleanup now live in `ui.heapdump` behind a narrow page
    view record.
  - Completed Heap Dump Analysis page view phase: HPROF issue table, detail
    tabs, issue detail panel, text report panel, split pane, and page layout
    construction now live in `ui.heapdump.HeapDumpAnalysisPaneView` instead of
    the monolithic shell view.
  - Completed Profiling page package phase: hot-method/dependency table setup,
    call graph and flame graph controls, zoom/pan gestures, stack-tree
    rendering, and profiling view-model binding now live in `ui.profiling`
    behind a narrow page view record.
  - Completed Profiling page view phase: profiling title, hot-method table,
    graph tabs, graph toolbars, scroll panes, graph containers, dependency
    table, stack trees, split pane, and page layout construction now live in
    `ui.profiling.ProfilingPaneView` instead of the monolithic shell view.
  - Completed recording workspace attach phase: recording-open success
    attachment, status update, and prepared workspace transfer now live in a
    focused shell attacher instead of the central shell controller.
  - Completed constructor entry cleanup phase: `AppShellFactory` now uses the
    bundle-based controller entry point directly, keeping positional service
    conversion out of the central shell constructor path.
  - Completed shell background-work phase: progress-bar visibility and
    FX-thread dispatch now live in a focused shell controller instead of the
    central shell controller.
  - Completed workspace-selection test-hook cleanup phase: section-loading
    tests now target the owning controllers/loaders directly, and the central
    shell no longer exposes workspace-selection wrapper methods.
  - Completed shell lazy-helper cleanup phase: lifecycle and workspace
    selection collaborators are used directly instead of retained lazy helper
    methods in the central shell.
  - Completed Live JVM accessor cleanup phase: saved-target, JDP discovery,
    and JMX monitoring dependency assertions now target `LiveJvmServices` and
    `ShellLiveJvmWorkspaceController`, and the central shell no longer exposes
    Live JVM service wrapper methods.
  - Completed I18n accessor cleanup phase: locale default behavior is verified
    against `I18n` directly, and the central shell no longer exposes its
    internal i18n dependency as a test accessor.
  - Completed recording workspace test-hook cleanup phase: lazy-loading tests
    now prepare workspaces through `RecordingWorkspaceFactory`, and the
    central shell no longer exposes a recording workspace preparation wrapper.
  - Completed constructor compatibility cleanup phase: the central shell now
    exposes only the bundle-based controller entry point and no longer imports
    every domain service type for obsolete positional constructors.
  - Completed stale page-remnant cleanup phase: Event Browser sizing constants
    and Profiling graph zoom math are verified at their owning page controllers
    instead of being exposed through the central shell controller.
  - Completed constructor-state cleanup phase: root/sidebar stay owned by
    `AppShellView`, and constructor-only recording collaborators are local
    wiring variables instead of retained shell controller state.
  - Completed shell runtime controller phase: runtime collaborator
    construction, initialization ordering, lifecycle close, and recording-open
    callback wiring now live in `ShellRuntimeController`, leaving
    `AppShellController` as a thin facade over the code-first shell view.
  - Completed Java Application data pages package phase: Exceptions, Threads,
    Thread Histogram, Security, Native Libraries, and Thread Dumps table/chart
    setup and view-model binding now live in `ui.exceptions`, `ui.threads`,
    and `ui.javaapp` behind narrow page view records.
  - Completed I/O and Locks pages package phase: File I/O, Socket I/O, and
    Locks tab text, grouping actions, table setup, timeline binding, and
    view-model binding now live in `ui.fileio`, `ui.socketio`, and `ui.locks`
    behind narrow page view records.
  - Completed Memory pages package phase: Heap, Leak Suspects, and TLAB title
    binding, table setup, timeline binding, leak reference tree rendering,
    TLAB placeholder state, selection handling, and rebind cleanup now live in
    `ui.heap`, `ui.leaks`, and `ui.tlab` behind narrow page view records.
  - Completed JVM Internals pages package phase: JVM information, GC
    configuration, GC summary/details, compilations, code cache, class loading,
    VM operations, G1 GC, and JavaFX Events title binding, table/chart setup,
    selection-driven detail binding, and rebind cleanup now live in `ui.jvm`,
    `ui.gc`, and `ui.jfx` behind narrow page view records.
  - Completed Environment pages package phase: Processes, Environment
    Variables, System Properties, Recording Info, Agents, and Constant Pools
    title binding, search-field filtering, table setup, and view-model binding
    now live in `ui.environment` behind a narrow page view record.
  - Completed recording workspace lifecycle phase: recording summary opening,
    recording-scoped view-model construction, prepared workspace transfer, and
    lazy section loading now live behind focused shell package services instead
    of the central shell controller.
  - Completed workspace tabs phase: opened JFR, HPROF, and Live JVM workspace
    tab construction, visibility, close handling, and selection synchronization
    now live in a focused shell controller.
  - Completed reusable export menu phase: CSV context-menu installation,
    save-file chooser setup, export invocation, and export status handling now
    live behind a focused shell installer.
  - Completed workspace open coordinator phase: JFR/HPROF file chooser handling,
    duplicate-open short-circuiting, background open work, and open progress
    state now live behind a focused shell coordinator.
  - Completed workspace pane visibility phase: selected-section visible and
    managed bindings for global, recording, heap dump, and live JVM panes now
    live behind a focused shell controller.
  - Completed workspace pane host view phase: workspace section pane ownership
    and workspace stack installation now live in `ShellWorkspacePanes` instead
    of the monolithic shell view.
  - Completed shell root view phase: root shell frame ownership, sidebar/tabs
    node ownership, status bar progress node ownership, and shell frame layout
    construction now live in `ShellRootView` instead of the monolithic shell
    view.
  - Completed workspace selection phase: selected workspace listeners, tab
    selection, page-controller rebinding, and selected-section lazy loading now
    live behind a focused shell controller.
  - Completed shell lifecycle phase: Live JVM controller cleanup, workspace
    close loops, live JVM view-model cleanup, heap dump analysis cleanup, and
    recording executor shutdown now live behind a focused shell lifecycle
    controller.
  - Completed pane-field ownership cleanup: page-pane fields already owned by
    `AppShellView` and `WorkspacePaneVisibilityController` are no longer
    mirrored in the central shell controller.
  - Completed shell page controller registry phase: recording and heap-dump page
    controller construction, workspace page-controller assembly, overview
    locale refresh, and page export-table enumeration now live behind a focused
    shell registry.
  - Completed Live JVM workspace controller phase: JVM browser view-model
    construction, Live JVM pane installation/configuration, section-open
    refresh wiring, and lifecycle registration now live behind a focused shell
    controller.
  - Completed heap dump workspace controller phase: heap dump analysis
    view-model construction and lifecycle registration now live behind a
    focused shell controller.
  - Removed migrated Live JVM helper remnants from the central shell
    controller; Live JVM table helpers and recording filename formatting now
    stay with `LiveJvmPaneController`, event timestamp formatting with
    `EventsPageController`, and settings language labels with settings/i18n
    bindings.
  - Removed long-lived service bundle state from the central shell controller;
    recording, Live JVM, and heap dump service bundles are now constructor
    wiring inputs handed to focused collaborators.
  - Move remaining feature-specific binding and table setup out of the central
    shell controller when a code-first view migration or active feature change
    touches that area.
  - Keep the shell responsible for navigation, workspace selection, and global
    status.
  - Preserve existing UI/UX contracts and tests during each split.

- Reduce constructor and dependency-chain growth in `AppShellFactory` and
  workspace creation.
  - Completed first dependency-bundle phase: recording, live JVM, and heap dump
    shell services now flow through explicit service records instead of being
    retained as long parallel field chains in the shell factory and controller.
  - Completed factory dependency-entry phase: `AppShellFactory` now uses a
    single explicit dependency record as its canonical construction path, and
    application startup assembles recording, live JVM, and heap dump service
    bundles before creating the shell.
  - Completed factory positional-constructor cleanup phase: obsolete long
    `AppShellFactory` constructors were removed, leaving factory creation on
    the bundled dependency entry point while adapter/service assembly stays in
    the app module and service records.
  - Introduce focused dependency bundles such as recording, live JVM, and heap
    dump service groups.
  - Avoid adding a dependency injection framework unless the project has a
    separate product reason to do so.
  - Keep tests able to provide fakes without long positional constructor chains.
  - Prefer cleanup that supports direct Java view assembly and keeps startup
    wiring explicit.

### P2

- Add shared time-range or selection context where recording pages need
  cross-page consistency.
  - Start with pages where tables and charts naturally describe the same time
    range.
  - Do not force global coupling onto pages that only need local selection.

- Improve HPROF/JOverflow browsing beyond a single issue table and text report.
  - Add more structured navigation for issue categories, reference paths, or
    retained-size-oriented inspection when the underlying analysis data allows
    it.
  - Keep Home as the file-open entry point.

- Define a release checklist for packaging and distribution.
  - Include native package verification, startup smoke checks, jlink module
    review, license and notice review, and platform-specific release notes.
  - Keep the independent-project disclaimer visible in public-facing release
    material.

- Add a small desktop smoke-test layer for key user journeys.
  - Cover opening JFR, switching recording pages, opening HPROF, basic Live JVM
    failure paths, theme switching, and CSV export.
  - Keep smoke tests focused; do not duplicate all unit and contract tests.

## Deferred

- Jolokia multicast or automatic discovery.
  - Manual and saved Jolokia URLs are the supported workflow for now.
  - Promote this only if discovery becomes a core remote-connection goal.

- Credentials and TLS trust-store UI for remote connections.
  - Defer until the product has a broader secure remote-connection design.
  - Do not add one-off credential fields for a single connector path.

- WebSocket integration.
  - Defer until a concrete JVM diagnostics workflow requires it.

- JConsole plug-in compatibility.
  - Defer pending a product decision.
  - Do not assume JConsole plug-in parity is required for JMC FX.

## Out Of Scope

- Eclipse IDE integration.
- Eclipse PDE integration.
- Eclipse RCP product integration.
- Eclipse update-site packaging.
- Copying JDK Mission Control Eclipse RCP/SWT UI code, icons, branding,
  workbench layouts, product assets, or Oracle commercial binary assets.
- Implying Oracle or OpenJDK endorsement.

## Maintenance Rules

- Keep completed, active, deferred, and out-of-scope work in separate sections.
- Keep entries evidence-based and tied to current repository behavior.
- Update the validation baseline when verification materially changes.
- Link future Superpowers specs or implementation plans from roadmap items once
  they exist.
- Keep the roadmap readable without requiring readers to open every linked
  document.
- Do not use this document to bypass `docs/ui-ux-system.md`,
  `docs/ui-guidelines.md`, or `AGENTS.md`.

## Follow-On Planning

Future work should be split into independent Superpowers specs and plans:

- Live JVM JMX notification management expansion.
- Shell controller decomposition on top of code-first views.
- App shell dependency bundle cleanup.
- HPROF/JOverflow browsing improvements.
- Release checklist and packaging validation.
- Desktop smoke-test coverage.
