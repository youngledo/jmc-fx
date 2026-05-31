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
- `rg -n "org.openjdk.jmc|JfrLoaderToolkit" jmc-fx-ui jmc-fx-app jmc-fx-domain`:
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
- Provides JMX notification model, storage, and service support, but the
  notification UI workflow is not yet complete.
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
- Includes UI/UX contract tests for FXML, CSS, navigation, localized strings,
  and important shell invariants.

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

- Complete the Live JVM JMX monitoring notification workflow.
  - Add user-facing controls for creating/selecting notification
    subscriptions.
  - Add start and stop actions for notification listeners.
  - Surface active/listening state and failure state in the monitoring page.
  - Preserve persisted notification subscriptions and retained events.

- Split `AppShellController` and `app-shell.fxml` responsibilities.
  - Move feature-specific binding and table setup out of the central shell
    controller where practical.
  - Keep the shell responsible for navigation, workspace selection, and global
    status.
  - Preserve existing UI/UX contracts and tests during the split.

- Reduce constructor and dependency-chain growth in `AppShellFactory` and
  workspace creation.
  - Introduce focused dependency bundles such as recording, live JVM, and heap
    dump service groups.
  - Avoid adding a dependency injection framework unless the project has a
    separate product reason to do so.
  - Keep tests able to provide fakes without long positional constructor chains.

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

- Live JVM JMX monitoring notification workflow.
- Shell controller and FXML decomposition.
- App shell dependency bundle cleanup.
- HPROF/JOverflow browsing improvements.
- Release checklist and packaging validation.
- Desktop smoke-test coverage.
