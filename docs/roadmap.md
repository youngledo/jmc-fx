# JMC FX Roadmap

This roadmap guides future JMC FX work. It is not a changelog, completion log,
or implementation history.

Keep this document focused on what the project should do next, why it matters,
and what should remain deferred or out of scope. Current behavior belongs in
the README, architecture guides, UI/UX guides, tests, and source code.

## Product Direction

JMC FX should remain a reliable, installable JavaFX workbench for JVM
diagnostics. It should align with JDK Mission Control concepts while keeping an
independent JavaFX architecture, visual language, and release workflow.

Future work should prioritize:

- Dense, inspectable workflows for JFR recordings, HPROF heap dumps, and live
  JVM/JMX diagnostics.
- Predictable interaction patterns for repeated diagnostic work.
- Clear architecture boundaries between domain, application, adapter, UI, and
  launcher code.
- Public-facing documentation that lets users build, run, package, and evaluate
  the project without reading source code.

## Priority Model

- **P0**: Guardrails that every change must preserve.
- **P1**: Highest-value product work that should guide the next focused
  implementation efforts.
- **P2**: Product-depth improvements that make existing workflows more useful.
- **P3**: Strategic exploration that may become important later but should not
  distract from P1/P2.

## P0 Guardrails

- Keep Java 26, JavaFX 26, Maven 4, Maven model `4.1.0`, and Maven 4
  `<subprojects>` as the project baseline.
- Keep the package and Maven coordinate namespace under
  `io.github.youngledo.jmcfx`.
- Keep the hexagonal architecture boundaries documented in
  `docs/hexagonal-boundary-guide.md`.
- Keep UI work aligned with `docs/ui-ux-system.md` and
  `docs/ui-guidelines.md`.
- Keep the project independent from Oracle and OpenJDK endorsement, branding,
  commercial assets, Eclipse RCP/SWT UI code, and Eclipse workbench structure.
- Keep public setup and packaging guidance available in both `README.md` and
  `README_ZH.md`.
- Keep this roadmap future-oriented. Do not add completed implementation
  history here.

## P1 Diagnostic Workflow Depth

### Recording Workflow Context

- Add shared time-range or selection context where recording pages naturally
  describe the same time window.
- Start with page pairs where charts and tables clearly need coordinated
  interpretation.
- Avoid forcing global coupling onto pages that only need local selection.

### Event Browser Usability

- Improve raw event browsing for large recordings with better saved filters,
  reusable column layouts, and clearer paging/window state.
- Keep large recording handling paged, sliced, or summarized so JavaFX tables do
  not eagerly load huge datasets.
- Preserve stable row identity for details and export.

### HPROF And JOverflow Browsing

- Expand heap dump analysis beyond a single issue table and text report.
- Add structured navigation for issue categories, reference paths, object
  groups, or retained-size-oriented inspection when the backing data supports
  it.
- Keep Home as the file-open entry point and keep heap dump workspaces focused
  on analysis.

### Live JVM And JMX Workflows

- Expand JMX notification management only if the product needs multiple
  simultaneous notification sources.
- If expanded, expose subscription selection, listening state per subscription,
  retained event history, and clear start/stop behavior.
- Improve JMC Agent workflows with clearer preset preview, capability failures,
  and applied-configuration feedback.
- Keep live JVM state explicit and separate from offline recording state.

## P2 Workbench Usability

### Export And Reporting

- Make CSV export scope explicit across recording, heap dump, and live JVM
  tables.
- Add report/export workflows only when they preserve the currently visible
  filter, selection, and time-range context.
- Avoid broad report generation before the underlying page semantics are stable.

### Workbench Polish

- Improve keyboard navigation, focus behavior, table density, empty/loading
  states, and accessibility for repeated diagnostic workflows.
- Keep visual changes aligned with a quiet technical workbench, not a marketing
  or showcase interface.
- Prefer targeted UI corrections over broad redesign unless the information
  architecture changes.

## P3 Strategic Exploration

### JPMS Packaging

- Revisit module-launch packaging when JMC dependencies are suitable for a full
  JPMS application-module `jlink` image.
- Keep classpath-based packaging as the supported route until the module path
  provides a clear benefit without breaking JMC integration.

### GraalVM Native Image

- Treat GraalVM native-image as a separate research and packaging path.
- Evaluate JFR availability, JavaFX compatibility, JMC dependencies,
  reflection/resource configuration, startup benefit, and debugging tradeoffs
  before committing to implementation.
- Do not overload the current jpackage/Leyden installer workflow with
  native-image concerns.

### Remote JVM Management

- Design credentials, TLS trust-store handling, and secure remote-connection UX
  as a coherent workflow before adding individual fields or connector-specific
  shortcuts.
- Revisit Jolokia automatic discovery only if remote JVM discovery becomes a
  core product goal.

## Deferred

- Jolokia multicast or automatic discovery.
- Credentials and TLS trust-store UI without a broader secure remote-connection
  design.
- WebSocket integration without a concrete JVM diagnostics workflow.
- JConsole plug-in compatibility without a product decision.
- Eclipse IDE, PDE, RCP, or update-site integration.

## Out Of Scope

- Copying JDK Mission Control Eclipse RCP/SWT UI code.
- Copying Oracle JDK Mission Control commercial binary assets.
- Copying JMC icons, branding, workbench layouts, or product assets.
- Implying Oracle or OpenJDK endorsement.

## Maintenance Rules

- Keep this document short enough to guide prioritization.
- Keep future work, deferred work, and out-of-scope work separate.
- Move implementation evidence into tests, README files, architecture docs, or
  issue/PR descriptions instead of this roadmap.
- Update this document when product direction changes, when a future item is
  promoted, or when a future item is deliberately deferred or removed.
- Link future specs or implementation plans only when they are active and
  useful for deciding what to do next.
