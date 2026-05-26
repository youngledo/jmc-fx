# JMC FX UI Guidelines

These rules define the UI contract for JMC FX. AtlantaFX is the base control
theme; this document defines how JMC FX assembles those controls into a
consistent desktop analysis application.

## Theme Boundary

- Use AtlantaFX as the user-agent stylesheet.
- Use the system theme preference by default. Resolve it to `PrimerLight` for
  light mode and `PrimerDark` for dark mode, with `PrimerLight` as the fallback
  when the platform color scheme is unavailable.
- Add JMC FX application CSS after AtlantaFX.
- Treat AtlantaFX as the owner of standard control states: hover, armed,
  pressed, focused, disabled, selected, borders, and control color variables.
- Do not fork or edit AtlantaFX CSS.
- Do not override base selectors such as `.button`, `.table-view`,
  `.text-field`, `.combo-box`, or `.tab-pane` unless a maintainer approves a
  project-wide control rule.
- Before changing a standard control state, compare against AtlantaFX sampler
  behavior or AtlantaFX source and document the reason in the change.

## Application CSS Scope

Application CSS may define layout, workflow, and product semantics. It should
not become a second control theme.

Allowed application CSS:

- Shell layout: navigation, toolbar, status areas, split panes, and workspace
  regions.
- Page templates: page header, page toolbar, page content, chart regions,
  detail regions, and empty states.
- Product semantics: severity, analysis status, recording state, warning
  regions, loading regions, and no-data regions.
- Technical data density: table sizing, column spacing, chart area sizing, and
  compact page spacing.
- Minor composition tweaks: icon/text gaps, section spacing, and alignment for
  app-specific containers.

Avoid application CSS that:

- Recolors or restyles standard controls directly.
- Defines one-off button appearances for a single page.
- Uses hard-coded colors when an AtlantaFX token is available.
- Depends on a background/control token collision to make a state visible.
- Fixes a local visual bug by changing global control behavior.

## Naming

- Use semantic application classes such as `app-shell`, `sidebar`,
  `page-header`, `page-toolbar`, `page-content`, `chart-region`,
  `detail-panel`, `empty-state`, and `metric-grid`.
- Do not name classes after implementation accidents, colors, or temporary
  fixes.
- Prefer page or region classes over direct control selectors.
- Keep custom classes additive. Do not remove the default JavaFX style class
  from controls.

## Layout Patterns

- Use a stable application shell: navigation on the left, shared command/status
  areas where appropriate, and a central workspace for feature pages.
- Use dense layouts for technical analysis pages. Avoid marketing-style hero
  sections inside analysis workflows.
- Prefer tables, split panes, tabs, and detail panels for large datasets.
- Prefer context menus and detail panels over many inline row buttons.
- Keep fixed-format UI stable with explicit sizing constraints where layout
  shift would hurt scanning or interaction.
- Ensure empty, loading, error, and no-data states fit the same page structure
  as the loaded state.

## Data Presentation

- Format large plain numbers with thousands separators.
- Format bytes, durations, timestamps, percentages, and rates consistently
  through shared formatting utilities.
- Keep units visible in column headers or values.
- Right-align numeric table columns when practical.
- Avoid eagerly loading large JFR event data into JavaFX observable lists; page,
  slice, summarize, or aggregate it.

## Chart Interaction

- Charts that support zoom should also support pan when zoomed.
- Trackpad gestures, mouse wheel gestures, and pointer dragging should follow
  one consistent interaction model across pages.
- Provide a predictable reset gesture or command.
- Keep chart controls and labels from shifting layout during interaction.

## Accessibility and Feedback

- Preserve visible focus states from AtlantaFX.
- Do not remove focus rings, pressed states, or disabled states.
- Use clear loading, empty, and error states for long-running work.
- Keep long-running parsing, loading, analysis, JMX, and export work off the FX
  Application Thread.
- Keep UI state mutations on the FX Application Thread.

## Review Checklist

Before merging UI work, verify:

- The change uses AtlantaFX defaults for standard control states.
- Any custom CSS is scoped to app layout or product semantics.
- The UI works with Follow System, `PrimerLight`, and `PrimerDark`.
- Large datasets remain paged, sliced, summarized, or virtualized.
- Numeric values and units use shared formatting rules.
- Empty, loading, error, and no-data states are handled.
- Tests cover view model behavior and any important FXML/CSS contracts.
