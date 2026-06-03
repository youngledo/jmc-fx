# JMC FX Flame Graph

`jmc-fx-flamegraph` is a reusable JavaFX control for flame graphs and icicle
graphs. It is developed inside JMC FX, but the module is intentionally
data-neutral: consumers provide tree data, labels, tooltips, and colors.

The module does not depend on JMC FX domain/UI/adapter classes or OpenJDK JMC
APIs.

## Requirements

- Java 26
- JavaFX 26
- Maven 4 when building inside the JMC FX repository

## Core Types

- `FlameGraphNode<T>`: immutable tree node with label, weight, percentage,
  payload, and children.
- `FlameGraphModel<T>`: root node plus max depth and max frame limits.
- `FlameGraphView<T>`: JavaFX control that renders the model.
- `FlameGraphMode`: `ICICLE` or `FLAME`.
- `FrameTextProvider<T>`: supplies frame labels.
- `FrameTooltipProvider<T>`: supplies tooltip text.
- `FrameColorProvider<T>`: supplies frame colors for default, hover, selected,
  path, match, and muted states.

## Minimal Usage

```java
FlameGraphNode<String> root = new FlameGraphNode<>("root", 100, 100, "root", List.of(
        new FlameGraphNode<>("parse", 45, 45, "parse", List.of()),
        new FlameGraphNode<>("render", 55, 55, "render", List.of())));

FlameGraphView<String> view = new FlameGraphView<>();
view.setModel(FlameGraphModel.of(root));
view.setMode(FlameGraphMode.FLAME);
view.setTooltipProvider(frame -> frame.node().label() + " - " + frame.node().weight());
```

## Color Providers

The reusable module includes a safe default color provider. Applications should
usually provide their own palette:

```java
view.setColorProvider((frame, state, context) -> {
    Color fill = context.mode() == FlameGraphMode.FLAME
            ? Color.web("#f97316")
            : Color.web("#38bdf8");
    return new FlameGraphFrameColors(fill, fill.darker(), Color.WHITE);
});
```

The provider receives:

- the rendered frame,
- the current `FlameGraphFrameState`,
- `FlameGraphRenderContext`, including current mode and max depth.

## Interactions

- Single click selects a frame.
- Double click focuses a frame subtree.
- Search can be driven through `search(String)` or `search(Predicate)`.
- `zoomIn`, `zoomOut`, `zoomBy`, `fitToWidth`, and `resetZoom` are available
  for host controls.

## Host Application Boundary

`jmc-fx-flamegraph` owns the reusable graph model, layout, rendering, selection,
focus, zoom, search, and per-frame provider APIs. Host applications own
application-specific buttons, menus, keyboard shortcuts, page text, status
lines, data loading, and domain adapters.

This keeps the control usable outside JMC FX while still allowing JMC FX to
provide JMC-style labels, tooltips, colors, and profiling data.

## Demo

Run the local demo from the repository root:

```bash
sdk env
./mvnw -pl jmc-fx-flamegraph-demo -am org.openjfx:javafx-maven-plugin:0.0.8:run
```

The demo is intentionally separate from `jmc-fx-flamegraph` so the reusable
module stays free of application-specific code.

## Extraction Notes

Before publishing this as an independent library:

- keep `jmc-fx-flamegraph` free of JMC FX and OpenJDK JMC dependencies,
- move this module, the demo, tests, license, and notices into a standalone
  repository,
- choose independent coordinates such as `io.github.<owner>:javafx-flamegraph`,
- add Maven Central publishing metadata,
- add standalone `LICENSE`, `NOTICE`, and third-party dependency notices,
- keep `FrameColorProvider`, `FrameTextProvider`, `FrameTooltipProvider`,
  `FlameGraphModel`, `FlameGraphNode`, and `FlameGraphView` as the public API.
