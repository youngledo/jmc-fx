# Roadmap Gap Matrix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create `docs/roadmap.md` as the current JMC FX roadmap and gap matrix, following the already-removed historical `docs/gap-status.md` note.

**Architecture:** This is a documentation-only change. The implementation adds one durable roadmap document, records that the obsolete gap-status document is absent, and verifies that the roadmap matches the current build and architecture baseline.

**Tech Stack:** Markdown documentation, Git, Maven 4 wrapper, Java 26, ripgrep.

---

### Task 1: Add The Roadmap Document

**Files:**
- Create: `docs/roadmap.md`
- Reference: `docs/superpowers/specs/2026-05-31-roadmap-gap-matrix-design.md`
- Reference: `docs/ui-ux-system.md`
- Reference: `docs/ui-guidelines.md`

- [ ] **Step 1: Create `docs/roadmap.md`**

Use this exact document content:

```markdown
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
```

- [ ] **Step 2: Review the new document for unfinished markers**

Run:

```bash
rg -n 'T''BD|TO''DO|FIX''ME|implement ''later|fill ''in|place''holder|\?\?' docs/roadmap.md
```

Expected: no matches and exit code `1`.

### Task 2: Confirm The Obsolete Gap Status Document Is Absent

**Files:**
- Verify: `docs/gap-status.md`
- Modify: none

- [ ] **Step 1: Confirm `docs/gap-status.md` is absent from the working tree**

Run:

```bash
test ! -e docs/gap-status.md
```

Expected: exit code `0`.

- [ ] **Step 2: Verify Git sees the roadmap addition and no pending gap-status deletion**

Run:

```bash
git status --short docs/roadmap.md docs/gap-status.md
```

Expected output includes an untracked `docs/roadmap.md` entry and no
`docs/gap-status.md` entry. If `docs/roadmap.md` is already staged, the
expected status may show `A  docs/roadmap.md`.

### Task 3: Verify Documentation Consistency And Project Baseline

**Files:**
- Verify: `docs/roadmap.md`
- Verify: `docs/ui-ux-system.md`
- Verify: `docs/ui-guidelines.md`
- Verify: `pom.xml`
- Verify: `jmc-fx-ui`
- Verify: `jmc-fx-app`
- Verify: `jmc-fx-domain`

- [ ] **Step 1: Check roadmap references to the existing UI contract files**

Run:

```bash
rg -n "docs/ui-ux-system.md|docs/ui-guidelines.md|AGENTS.md|docs/gap-status.md" docs/roadmap.md
```

Expected output includes references to all four names.

- [ ] **Step 2: Run SDK and Maven version verification**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -v
```

Expected output includes:

```text
Using java version 26.0.1-tem
Apache Maven 4.0.0-rc-5
Java version: 26.0.1
```

- [ ] **Step 3: Run full project verification**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw verify
```

Expected output includes:

```text
BUILD SUCCESS
```

- [ ] **Step 4: Verify Maven 3 module syntax is absent**

Run:

```bash
rg -n "<modules>|<module>" pom.xml **/pom.xml
```

Expected: no matches and exit code `1`.

- [ ] **Step 5: Verify JMC API usage stays outside UI, app, and domain**

Run:

```bash
rg -n "org.openjdk.jmc|JfrLoaderToolkit" jmc-fx-ui jmc-fx-app jmc-fx-domain
```

Expected: no matches and exit code `1`.

### Task 4: Commit The Roadmap Implementation

**Files:**
- Add: `docs/roadmap.md`
- Add: `docs/superpowers/plans/2026-05-31-roadmap-gap-matrix.md`

- [ ] **Step 1: Review final diff**

Run:

```bash
git diff -- docs/roadmap.md
git diff -- docs/superpowers/plans/2026-05-31-roadmap-gap-matrix.md
```

Expected:

- `docs/roadmap.md` is new and matches Task 1.
- This implementation plan exists under `docs/superpowers/plans/`.

- [ ] **Step 2: Stage roadmap and plan**

Run:

```bash
git add docs/roadmap.md
git add -f docs/superpowers/plans/2026-05-31-roadmap-gap-matrix.md
```

Expected:

```bash
git diff --cached --name-status
```

includes:

```text
A	docs/roadmap.md
A	docs/superpowers/plans/2026-05-31-roadmap-gap-matrix.md
```

- [ ] **Step 3: Commit**

Run:

```bash
git commit -m "docs: add project roadmap"
```

Expected: commit succeeds.

## Self-Review

- Spec coverage: the plan creates `docs/roadmap.md`, records validation
  evidence, separates current capabilities, active priorities, deferred work,
  out-of-scope work, and maintenance rules, and treats the already-removed
  `docs/gap-status.md` as superseded.
- Completion-marker scan: this plan intentionally contains no unfinished
  implementation instructions.
- Scope check: this plan is documentation-only and does not implement Live JVM,
  shell decomposition, HPROF, release, or smoke-test feature work.
