# Roadmap Gap Matrix Design

## Purpose

JMC FX needs a durable roadmap document that records the current product
capabilities, active gaps, deferred work, and out-of-scope areas in one place.
The existing `docs/gap-status.md` has been removed from the working tree, and
the remaining documentation does not provide a current project-wide roadmap.

This design creates a long-lived `docs/roadmap.md` as the single fact source
for roadmap and gap status. The roadmap will complement `docs/ui-ux-system.md`
and `docs/ui-guidelines.md`; it will not replace UI/UX contracts or detailed
implementation plans.

## Goals

- Add `docs/roadmap.md` as the current roadmap and gap matrix.
- Make the roadmap evidence-based by recording the latest validation baseline.
- Separate completed capabilities, active priorities, deferred work, and
  out-of-scope items.
- Make explicit that `docs/roadmap.md` supersedes the deleted
  `docs/gap-status.md`.
- Provide clear follow-on work items for future Superpowers specs and plans.

## Non-Goals

- Do not implement Live JVM monitoring changes in this work.
- Do not split `AppShellController` or `app-shell.fxml` in this work.
- Do not restore `docs/gap-status.md`.
- Do not change Java, FXML, CSS, Maven, or runtime behavior.
- Do not create a detailed implementation plan for every roadmap item in this
  design. Each major roadmap item should receive its own spec and plan when it
  becomes active work.

## Document Structure

The new `docs/roadmap.md` will use these sections.

### Purpose

Explain that the roadmap is the project-wide current fact source for product
capabilities, gaps, priorities, deferred decisions, and out-of-scope items.
State that it replaces the narrower historical gap-status note.

### Validation Baseline

Record the current verification evidence used to classify the roadmap:

- `sdk env`
- `./mvnw -v`
- `./mvnw verify`
- `rg -n "<modules>|<module>" pom.xml **/pom.xml`
- `rg -n "org.openjdk.jmc|JfrLoaderToolkit" jmc-fx-ui jmc-fx-app jmc-fx-domain`

The baseline should include a date and concise result. It should not paste full
test logs.

### Current Capabilities

Group implemented capabilities by product area:

- JFR recording workspaces
- Live JVM and JMX workspaces
- HPROF heap dump analysis
- UI shell and workspace navigation
- Packaging and release infrastructure
- Documentation, licensing, and repository workflow

Each capability entry should describe user-visible behavior and the primary
code area or document that backs it.

### Priority Roadmap

Use priority levels rather than a strict calendar:

- P0: must happen before the roadmap can guide reliable future work.
- P1: high-impact product or architecture work that should be planned next.
- P2: important improvements that can wait behind P1 work.

The initial roadmap should include these items:

- P0: maintain `docs/roadmap.md` as the roadmap and gap matrix.
- P1: complete the Live JVM JMX monitoring notification workflow.
- P1: split `AppShellController` and `app-shell.fxml` responsibilities.
- P1: reduce constructor and dependency-chain growth in `AppShellFactory` and
  workspace creation.
- P2: add shared time-range or selection context where recording pages need
  cross-page consistency.
- P2: improve HPROF/JOverflow browsing beyond a single issue table and text
  report.
- P2: define release packaging, native-package, license, and notice
  verification as a release checklist.
- P2: add a small desktop smoke-test layer for key user journeys.

### Deferred

List known deferred work separately from active gaps. The initial deferred list
should include:

- Jolokia multicast or automatic discovery.
- Secure remote connection design for credentials and TLS trust stores.
- WebSocket integration.
- JConsole plug-in compatibility.

Deferred entries should say why they are not active now and what decision would
make them active.

### Out Of Scope

List areas that should not be treated as missing JMC FX features:

- Eclipse IDE integration.
- Eclipse PDE or RCP product integration.
- Eclipse update-site packaging.
- Copying JDK Mission Control RCP/SWT UI, branding, icons, layouts, or
  commercial binary assets.

### Maintenance Rules

The roadmap should be updated when:

- A roadmap item is implemented.
- A gap is deliberately deferred.
- A deferred item becomes active work.
- A verification baseline materially changes.
- A feature is removed, renamed, or moved to a different product area.

Changes should preserve the distinction between completed, active, deferred,
and out-of-scope work.

## Follow-On Planning

After `docs/roadmap.md` exists, future work should be split into independent
Superpowers specs and plans:

- Live JVM JMX monitoring notification workflow.
- Shell controller and FXML decomposition.
- App shell dependency bundle cleanup.
- HPROF/JOverflow browsing improvements.
- Release checklist and packaging validation.
- Desktop smoke-test coverage.

The roadmap itself should point to those specs or plans once they exist, but it
should remain readable without requiring readers to open every linked document.

## Testing And Verification

Because the implementation is documentation-only, verification should include:

- Spell and consistency review of the new roadmap.
- Confirm the roadmap does not contradict `docs/ui-ux-system.md` or
  `docs/ui-guidelines.md`.
- Run the project verification required by `AGENTS.md`, or document why it
  could not run.
- Confirm no Maven 3 module syntax appears in POMs.
- Confirm JMC APIs remain outside `jmc-fx-ui`, `jmc-fx-app`, and
  `jmc-fx-domain`.

## Approval Status

The user approved the roadmap-first direction and the durable
`docs/roadmap.md` approach on 2026-05-31.
