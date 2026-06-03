package com.youngledo.jmcfx.flamegraph;

@FunctionalInterface
public interface FrameTextProvider<T> {

    String text(FlameGraphFrame<T> frame);

    static <T> FrameTextProvider<T> defaultProvider() {
        return frame -> frame == null ? "" : frame.node().label();
    }
}
