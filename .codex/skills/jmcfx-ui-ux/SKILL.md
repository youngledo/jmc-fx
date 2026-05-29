---
name: jmcfx-ui-ux
description: Use when changing JMC FX JavaFX UI, FXML, CSS, layout, visual density, page templates, detail panels, tabs, toolbars, or user-facing interaction flows.
---

# JMC FX UI/UX

Use this before editing any UI/UX surface in this repository.

## Required Workflow

1. Read `docs/ui-ux-system.md`.
2. Read `docs/ui-guidelines.md`.
3. Identify the affected page template and workflow pattern from `docs/ui-ux-system.md`.
4. State which shared UI contracts apply, such as `detail-panel`, `page-toolbar`, `page-detail-tabs`, dense `TableView`, or shell status/progress.
5. If the change deviates from the system, document the reason before implementation.
6. Add or update contract tests for reusable UI/UX rules.
7. Run targeted UI tests and the project verification required by `AGENTS.md`.

## Non-Negotiables

- Do not introduce feature-specific detail panel aliases such as `analysis-detail`, `heap-dump-detail`, or `memory-detail`.
- Put page-specific layout on page containers, not shared detail components.
- Keep split table detail content aligned with the primary table/list/tree edge unless `docs/ui-ux-system.md` explicitly allows otherwise.
- Do not use FXML `styleClass="a b"` for multiple classes. Use JavaFX list syntax with `<styleClass><String fx:value="..."/></styleClass>`.

## Verification

At minimum, run the relevant UI tests. For reusable UX contracts, prefer `UiUxSystemContractTest` over one-off page tests.
