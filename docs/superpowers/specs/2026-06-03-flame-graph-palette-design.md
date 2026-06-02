# Flame Graph Palette Design

## Context

JMC FX already has a reusable `FlameGraphView` for profiling callers and
callees. The view supports both flame and icicle orientation, but the frames
currently share one accent-colored style. That makes the result read like a
generic block chart instead of a flame graph or icicle graph.

## Decision

Use orientation-specific palettes:

- Flame orientation uses a warm red, orange, and yellow temperature scale.
- Icicle orientation uses a cool blue and purple scale.
- Frame labels must remain readable on every palette stop. Dark frame colors
  use light label text; light frame colors use dark label text.

This aligns the visual metaphor with JMC-style flame and icicle graphs without
copying JMC assets or implementation code.

## UI Contract

`FlameGraphView` will expose palette semantics through style classes instead of
inline styles:

- The root view has `flame-graph-orientation-flame` when orientation is
  `FLAME`.
- The root view has `flame-graph-orientation-icicle` when orientation is
  `ICICLE`.
- Each frame has a deterministic depth style class:
  `flame-graph-depth-0` through `flame-graph-depth-7`.
- Depths greater than seven wrap modulo eight so large graphs keep a bounded
  CSS contract.

The existing `flame-graph-frame`, `flame-graph-frame-label`,
`flame-graph-frame:hover`, and `flame-graph-empty` classes remain in place.

## Styling Rules

CSS defines the complete palette using scoped selectors such as:

```css
.flame-graph-orientation-flame .flame-graph-depth-0 { ... }
.flame-graph-orientation-icicle .flame-graph-depth-0 { ... }
.flame-graph-depth-0 .flame-graph-frame-label { ... }
```

Frame background, border, and label text colors must be paired deliberately.
The implementation must not rely on one global label color for all depth
levels.

## Implementation Boundary

This change is limited to `FlameGraphView`, `app.css`, and focused tests. It
does not change profiling data, flame graph layout calculations, zoom, pan,
toolbars, call graph styling, or histogram styling.

## Testing

Add or update tests for:

- Orientation style classes update when toggling between icicle and flame.
- Frame nodes receive deterministic depth style classes.
- CSS contains warm flame palette selectors.
- CSS contains cool icicle palette selectors.
- CSS contains label text color rules for both light and dark frame colors.

Run the focused FlameGraph tests first, then the project verification required
by `AGENTS.md`.
