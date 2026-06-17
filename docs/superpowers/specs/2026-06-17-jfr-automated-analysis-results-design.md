# JFR Automated Analysis Results Design

Date: 2026-06-17

## Product Direction Guardrail

JMC FX is not a pixel-by-pixel clone of the Eclipse RCP/SWT JDK Mission Control application, and it is not an AI-first chat diagnostic tool. The project direction is:

- Preserve JMC-compatible diagnostic concepts: JFR recordings, automated rules, event browsing, JVM discovery, MBeans, diagnostic commands, and Flight Recorder control.
- Build a JavaFX-native workbench: dense analysis pages, predictable navigation, clear state feedback, theme integration, exportable tables, and responsive background work.
- Add guided diagnostics as the product-level differentiator: keep JMC-compatible automated analysis results central, help users inspect rule explanations and solutions, navigate to existing related pages, and use AI as an optional explanatory layer without replacing deterministic analysis.

This means JMC FX should feel familiar to JMC users at the conceptual level, but it does not need to reproduce Eclipse workbench structure, SWT layouts, icons, or branding. It should earn trust as a serious JVM diagnostics tool first, then use modern JavaFX and optional AI to reduce investigation friction.

## Current Stage: Offline JFR Productization

The current stage focuses on making the first minute after opening a `.jfr` recording feel professional, guided, and trustworthy.

The main feature for this stage is **JFR Automated Analysis Results v1**. It upgrades the existing Analysis page while keeping the user-visible concept aligned with JDK Mission Control's Automated Analysis Results.

### Goals

- Give users an immediate summary of recording health and important automated analysis results.
- Keep deterministic JMC rule results as the primary surface while allowing AI report findings to appear as an optional source.
- Keep deterministic analysis usable when AI is disabled or unavailable.
- Make important results actionable through detail text and existing page navigation.
- Reuse existing JFR pages instead of rebuilding the entire analysis workspace.

### Non-Goals

- Do not rewrite all Analysis pages.
- Do not add new deep JMC adapter analysis algorithms in this stage.
- Do not make AI the primary source of truth.
- Do not replace JMC-compatible navigation concepts.
- Do not redesign the Live JVM workbench in this stage.
- Do not build recording comparison, full report generation, or a general evidence graph yet.
- Do not generalize Live JVM origin metadata into a cross-page recording context model in this stage; keep that as a planned bridge/future-diagnostics concern.

## JFR Automated Analysis Results v1

### Model

Add a lightweight UI-neutral diagnostic aggregation model in the domain/application boundary. Internal model names may use "finding" for aggregation, but user-visible page names, labels, and docs should stay aligned with JMC's "Automated Analysis Results" concept. Candidate records:

- `DiagnosticFinding`
- `DiagnosticFindingSource`
- `DiagnosticEvidenceLink`
- `DiagnosticFindingSeverity`

Each finding should express:

- stable id
- source, such as rule analysis or AI
- severity
- title
- summary
- optional explanation
- optional recommended next action
- optional links to existing recording pages

The model should not depend on JavaFX. It should be suitable for future export, Live-to-JFR handoff, and reporting.

### Application Flow

Add an application-level use case that composes existing sources into unified findings:

- Map JMC rule results into deterministic findings.
- Map AI report findings into assistant-sourced findings when available.
- Preserve source metadata so the UI can show whether a finding came from rules, AI, or a future source.
- Sort by severity and product relevance.
- Avoid calling external AI automatically as part of ordinary rule loading.

The use case should remain an orchestration layer. Existing rule analysis and AI context/report behavior stay in their current services.

### UI Flow

The Analysis page becomes a richer Automated Analysis Results page while preserving existing functionality:

- Top summary area: highest severity, result count, rule analysis status, AI availability/status.
- Main results table/list: severity, source, score, title, and summary.
- Shared detail panel: JMC-aligned rule details, especially explanation and solution.
- AI panel remains available as an enhancement, not the default source of truth.
- Double-click navigation may reuse existing page navigation, such as Events, Profiling, GC, Threads, Locks, File I/O, Socket I/O, Heap, and Advanced JFR pages, but should not introduce a separate "Evidence" column or action unless JMC parity review explicitly approves it.

The UI should follow the existing `Split Table Detail Page` pattern from `docs/ui-ux-system.md`, using shared `detail-panel` classes and dense table conventions.

### Error Handling

- If rule analysis fails, show the failure in the Automated Analysis Results page without breaking unrelated pages.
- If AI is disabled, unconfigured, or fails, deterministic findings remain available.
- If an optional related-page link cannot resolve to a page, keep the result visible and avoid exposing unresolved implementation targets in the table or detail text.
- Long-running work must stay off the FX Application Thread.

### Testing

Add focused tests for:

- rule result to diagnostic finding mapping
- AI report finding to diagnostic finding mapping
- deterministic behavior when AI is unavailable
- finding sorting by severity/source relevance
- Analysis page detail-panel contract and optional related-page navigation behavior

Prefer view model and application tests for behavior. Use UI contract tests only for reusable layout or CSS semantics.

## Roadmap

### Next Stage: Live to JFR Bridge

Goal: connect the Live JVM workbench to the offline analyzer.

- Start, stop, save, and open a JFR recording from a live JVM session.
- Preserve source JVM, session, and time-range context when opening the resulting recording workspace.
- Let users trace from a live session to the generated recording and back to the originating target.
- Introduce a lightweight recording context concept for Live-to-JFR handoff only after the source metadata has proven useful in the Overview page.
- Keep the bridge context scoped to source attribution and traceability; do not turn it into report generation, AI context expansion, or a general evidence graph in this stage.
- Make the bridge context reusable by future pages that need source attribution, while preserving the boundary between live JVM state and offline recording state.

### Later Stage: Live JVM Operations Polish

Goal: make the Live JVM area feel like a production operations workbench.

- Session health overview.
- Operation history.
- Diagnostic command presets.
- Trigger rule lifecycle polish.
- Monitoring profile save and restore.
- Better connection failure explanations and recovery actions.

### Future Stage: Advanced Guided Diagnostics

Goal: make JMC FX a stronger next-generation JVM diagnostics tool while staying compatible with JMC concepts.

- Multi-source evidence graph.
- Recording comparison.
- Issue timeline.
- Exportable diagnosis reports.
- Recording context reuse across Overview, findings, AI explanations, and exported reports.
- Explainable AI follow-up questions.
- Custom rules, triggers, or analysis profiles.

## Approval Status

This design captures the agreed direction: professional, restrained diagnostics by default, with AI as an optional explanatory enhancement. Implementation should begin only after this written spec is reviewed and approved.
