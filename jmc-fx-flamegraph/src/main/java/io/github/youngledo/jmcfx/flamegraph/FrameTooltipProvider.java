package io.github.youngledo.jmcfx.flamegraph;

@FunctionalInterface
public interface FrameTooltipProvider<T> {

    String tooltip(FlameGraphFrame<T> frame);

    static <T> FrameTooltipProvider<T> defaultProvider() {
        return frame -> frame == null ? "" : frame.node().label();
    }
}
