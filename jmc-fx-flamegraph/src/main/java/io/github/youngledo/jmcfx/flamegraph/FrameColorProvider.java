package io.github.youngledo.jmcfx.flamegraph;

@FunctionalInterface
public interface FrameColorProvider<T> {

    FlameGraphFrameColors colors(
            FlameGraphFrame<T> frame,
            FlameGraphFrameState state,
            FlameGraphRenderContext context);

    static <T> FrameColorProvider<T> of(LegacyFrameColorProvider<T> provider) {
        LegacyFrameColorProvider<T> resolved = provider == null
                ? (frame, state) -> defaultColors(state)
                : provider;
        return (frame, state, context) -> resolved.colors(frame, state);
    }

    static <T> FrameColorProvider<T> defaultProvider() {
        return (frame, state, context) -> defaultColors(state);
    }

    private static FlameGraphFrameColors defaultColors(FlameGraphFrameState state) {
        return switch (state == null ? FlameGraphFrameState.DEFAULT : state) {
            case SELECTED -> FlameGraphFrameColors.SELECTED;
            case MATCH -> FlameGraphFrameColors.MATCH;
            case PATH -> FlameGraphFrameColors.PATH;
            case MUTED -> FlameGraphFrameColors.MUTED;
            case HOVERED -> FlameGraphFrameColors.HOVERED;
            case DEFAULT -> FlameGraphFrameColors.DEFAULT;
        };
    }
}
