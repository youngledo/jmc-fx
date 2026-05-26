# JMC FX UI/UX System

This document defines the product-level user experience system for JMC FX.
It is a long-lived design contract, not a feature-specific design note.

Every UI/UX change must use this document together with `docs/ui-guidelines.md`:

- `docs/ui-ux-system.md` defines information architecture, workflow patterns,
  page templates, density, and interaction semantics.
- `docs/ui-guidelines.md` defines AtlantaFX, CSS, control styling, and
  implementation-level UI rules.

Feature-specific design specs in `docs/superpowers/specs/` may add detail for
one feature, but they must not silently contradict this system.

## Product Experience Goal

JMC FX is a technical desktop workbench for JVM diagnostics. The interface must
feel calm, dense, predictable, and inspection-oriented. It should help users
compare data, move between recordings and live JVMs, and run diagnostic actions
without hiding system state behind decorative layouts.

The application is not a marketing site, a wizard-first utility, or a visual
showcase. Prefer explicit data, durable navigation, clear state, and compact
controls over large illustrative panels or one-off visual treatments.

## Information Architecture

The shell has four stable top-level areas:

- Home: entry point for opening recordings and connecting JVMs.
- JVMs: live JVM discovery, connection, session detail, and future live tools.
- Recording workspace: offline JFR analysis pages for the selected recording.
- Settings: application preferences such as language and theme.

Navigation rules:

- Left navigation is the durable map of workflows.
- Recording-specific pages belong to the selected recording workspace.
- Global pages such as Home, JVMs, and Settings are not recording-specific.
- Live JVM state and offline recording state must not be merged into one hidden
  context. If a future page can use both, the current source must be explicit.
- Avoid creating top-level navigation items for small tools. Prefer tabs,
  detail panels, or context menus inside an existing workflow.

## JMC Alignment Strategy

JMC FX should align with JDK Mission Control's diagnostic concepts without
copying Eclipse RCP/SWT UI structure, icons, layouts, branding, or assets.

Follow JMC conceptually:

- JVM Browser and JVM Console map to live JVM discovery and connected sessions.
- Flight Recorder control maps to recordings on a live JVM.
- MBean Browser maps to a browse/select/inspect/operate workflow.
- Diagnostic Commands map to command discovery, parameter entry, execution,
  and text output.
- Offline JFR analysis maps to recording-scoped pages.

Do not copy JMC literally:

- Do not reproduce Eclipse workbench layouts.
- Do not copy JMC icons or product assets.
- Do not expose implementation terms such as RCP, SWT, or internal service
  names in the UI.

## Page Templates

Use a small set of repeatable page templates. New pages must identify which
template they use before implementation.

### Data Table Page

Use for large flat technical datasets.

Structure:

- Page title.
- Optional compact toolbar for filters, refresh, export, and mode controls.
- Dense `TableView` as the primary surface.
- Optional status line only when it communicates paging/window state or an
  error that cannot live in the table placeholder.

Rules:

- Tables should carry the main information load.
- Avoid inline row buttons for repeated actions; use selection plus toolbar,
  context menu, or detail panel.
- Empty, loading, and error placeholders must fit inside the table area.

### Split Table Detail Page

Use when users select an item and inspect details.

Structure:

- Page title and optional toolbar.
- Primary list/table/tree on one side or top.
- Detail panel on the other side or bottom.

Rules:

- Selection drives detail.
- Detail must preserve context: selected item name, state, and key metadata.
- Refreshing the list must not destroy connected or selected state unless the
  underlying entity is genuinely gone and no longer usable.

### Control Panel Page

Use for live JVM actions such as starting recordings, running diagnostic
commands, or editing future trigger rules.

Structure:

- Page title.
- Current target/session summary.
- Compact controls grouped by purpose.
- Result or log output area.

Rules:

- Actions must be disabled when prerequisites are missing.
- Long-running actions must show progress or busy state.
- Dangerous or expensive actions need explicit labels and clear outcome text.
- Control pages should avoid modal-first workflows unless the action requires
  multi-step input.

### Settings Page

Use a quiet form layout.

Structure:

- Page title.
- Vertical groups.
- Each group has a short label and controls directly below or beside it.

Rules:

- Do not use cards for each setting group.
- Use radio buttons for mutually exclusive small sets, such as language and
  theme.
- Use checkboxes or toggles for binary preferences.
- Use combo boxes only when the option set is large or dynamic.
- Group spacing should be wider than control spacing.

### Overview Page

Use when summarizing a selected recording or connected JVM.

Structure:

- Page title.
- Small number of summary groups.
- Links or actions into deeper pages.

Rules:

- Overview is for orientation, not decoration.
- Avoid hero-style composition inside the workbench.
- Summary groups should lead users to specific next actions.

## Workflow Patterns

### Discover And Connect

Use for local JVM discovery and remote JMX connection.

- Discovery lists candidates; it does not imply connection.
- Connect is an explicit action.
- Disconnect is enabled only for connected rows.
- Refresh must not override connected rows or live session state.
- Manual remote connection must remain available even when local discovery is
  empty.

### Selection-Driven Detail

Use for tables, trees, JVM sessions, MBeans, recordings, events, and rules.

- A selected item should have a visible detail area when details matter.
- If no item is selected, show an empty state in the detail area.
- If detail loading fails, preserve the selection and show the failure in the
  detail area.

### Long-Running Work

Use for loading recordings, querying JMX, reading large event windows, exporting
data, and diagnostic actions.

- Never block the FX Application Thread.
- Disable only controls that conflict with the active operation.
- Keep existing data visible during refresh unless it is misleading.
- Prefer scoped busy state over full-page blanking.
- Error states should say what failed and leave the user with a clear next
  action.

### Status And Feedback

Use status text sparingly.

- Prefer table placeholders for empty/no-data states.
- Prefer detail-panel messages for detail loading failures.
- Prefer inline command output areas for diagnostic command results.
- Do not duplicate obvious table state in a persistent bottom label.
- Use transient status only for action completion or scoped errors.

## Visual Density And Layout

JMC FX is a dense desktop tool. Spacing should make information scannable
without turning pages into sparse marketing layouts.

Spacing guidance:

- Page outer spacing: 12-18 px.
- Toolbar control spacing: 6-8 px.
- Form group spacing: 16-24 px.
- Label-to-control spacing inside a group: 6-8 px.
- Split pane detail areas should have stable preferred sizes.

Typography guidance:

- Use page-scale titles only for page titles.
- Use compact section labels inside panels and settings groups.
- Do not use oversized headings inside technical work surfaces.
- Keep button text short and action-oriented.

Panels and cards:

- Use unframed layouts for page structure.
- Use cards only for repeated items, modal content, or genuinely framed tools.
- Do not nest cards inside cards.
- Prefer split panes and detail panels for technical inspection.

## Control Selection

Use familiar controls consistently:

- Buttons for commands.
- Icon buttons for compact tool commands when the icon is familiar.
- Radio buttons for 2-5 mutually exclusive settings.
- Checkboxes/toggles for binary settings.
- Text fields for filters and direct input.
- Tables for large flat datasets.
- Trees for hierarchical data.
- Tabs for sibling views inside one workflow.
- Context menus for row/object actions that are not primary toolbar actions.

Avoid:

- Decorative controls that do not add workflow value.
- One-off custom controls when JavaFX or AtlantaFX controls are sufficient.
- Inline action buttons repeated across many table rows.

## Content And Localization

Text must be concise, stable, and localizable.

Rules:

- No hardcoded visible text in FXML.
- Button labels should be verbs or short verb phrases.
- State labels should be nouns or adjectives, not sentences.
- Empty states should explain the condition and the next useful action when
  one exists.
- Error messages should be specific enough to debug but not expose stack traces.
- Chinese and English labels must both fit the intended control width.

## Live JVM Experience

Live JVM features should be organized around the connected session.

Recommended progression:

- JVM Browser: discover, connect, disconnect, show connection status.
- Live JVM session: runtime summary and capability status.
- Flight Recorder control: list recordings, start, stop, dump, open saved JFR.
- MBean Browser: tree/list, attributes, operations, notifications later.
- Diagnostic Commands: list, parameter input, execute, output, save.
- Triggers: rules based on live metrics and actions.

Rules:

- A live session must expose capability state before a feature depends on it.
- Unsupported capabilities should be visible and disabled, not hidden when the
  absence helps explain behavior.
- Connected sessions should survive local discovery refreshes.

## Recording Workspace Experience

Offline recording pages are scoped to the selected recording.

Rules:

- Keep selected recording visible in shell context.
- Pages must handle large recordings with pagination, slicing, summarization,
  or lazy loading.
- Tables and charts should agree on selected time range when a page supports
  time filtering.
- Export actions should export the current view or clearly state their scope.

## Theme And Appearance

Theme is an application preference in Settings.

Rules:

- Available modes are Follow System, Light, and Dark.
- Follow System uses JavaFX platform color scheme preferences and updates while
  the app is running.
- Light resolves to AtlantaFX `PrimerLight`.
- Dark resolves to AtlantaFX `PrimerDark`.
- Application CSS must work in both resolved themes.
- Do not add page-specific colors that only work in one theme.

## Design Review Checklist

Before implementing or reviewing a UI/UX change, answer:

- Which information architecture area does this change belong to?
- Which page template does it use?
- What is the loading, empty, error, and success state?
- What user action starts long-running work, and what stays enabled?
- Does selection drive detail, navigation, or both?
- Does this preserve connected/live or selected/recording state across refresh?
- Does this use existing JavaFX/AtlantaFX controls?
- Does it avoid custom control-state CSS?
- Does it work with Follow System, Light, and Dark themes?
- Are visible strings localizable and short enough for English and Chinese?
- Are tests covering ViewModel behavior and important FXML/CSS contracts?
