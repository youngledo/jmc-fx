package io.github.youngledo.jmcfx.flamegraph;

public record FlameGraphRenderContext(
        FlameGraphMode mode,
        int maxDepth) {

    public FlameGraphRenderContext {
        mode = mode == null ? FlameGraphMode.ICICLE : mode;
        maxDepth = Math.max(0, maxDepth);
    }
}
