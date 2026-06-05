package io.github.youngledo.jmcfx.flamegraph;

import java.util.List;
import java.util.Optional;

public record FlameGraphLayout<T>(
        List<FlameGraphFrame<T>> frames,
        int maxDepth) {

    public FlameGraphLayout {
        frames = frames == null ? List.of() : List.copyOf(frames);
        maxDepth = Math.max(0, maxDepth);
    }

    public static <T> FlameGraphLayout<T> empty() {
        return new FlameGraphLayout<>(List.of(), 0);
    }

    public Optional<FlameGraphFrame<T>> frameAt(double normalizedX, int depth) {
        double x = Math.clamp(normalizedX, 0, 1);
        double tolerance = 0.000001;
        return frames.stream()
                .filter(frame -> frame.depth() == depth)
                .filter(frame -> frame.x() <= x + tolerance
                        && frame.x() + frame.width() + tolerance >= x)
                .findFirst();
    }
}
