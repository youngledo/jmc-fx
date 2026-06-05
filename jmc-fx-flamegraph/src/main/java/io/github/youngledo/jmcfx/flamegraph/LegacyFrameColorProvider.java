package io.github.youngledo.jmcfx.flamegraph;

@FunctionalInterface
public interface LegacyFrameColorProvider<T> {

    FlameGraphFrameColors colors(FlameGraphFrame<T> frame, FlameGraphFrameState state);
}
