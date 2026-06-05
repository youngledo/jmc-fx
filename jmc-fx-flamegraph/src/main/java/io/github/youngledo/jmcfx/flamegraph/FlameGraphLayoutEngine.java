package io.github.youngledo.jmcfx.flamegraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class FlameGraphLayoutEngine<T> {

    public FlameGraphLayout<T> layout(FlameGraphModel<T> model, FlameGraphMode mode) {
        if (model == null || model.isEmpty()) {
            return FlameGraphLayout.empty();
        }
        List<FlameGraphFrame<T>> frames = new ArrayList<>();
        int deepestDepth = appendLevels(
                sortedByWeight(model.root().children()),
                0,
                0,
                1,
                List.of(),
                model,
                frames);
        return layoutWithRows(frames, Math.max(0, deepestDepth + 1), mode);
    }

    public FlameGraphLayout<T> focusedLayout(
            FlameGraphModel<T> model,
            FlameGraphMode mode,
            FlameGraphFrame<T> focusFrame) {
        FlameGraphLayout<T> fullLayout = layout(model, mode);
        if (focusFrame == null || fullLayout.frames().isEmpty() || focusFrame.width() <= 0) {
            return fullLayout;
        }
        FlameGraphFrame<T> originalFocus = fullLayout.frames().stream()
                .filter(frame -> frame.path().equals(focusFrame.path()))
                .findFirst()
                .orElse(null);
        if (originalFocus == null) {
            return fullLayout;
        }

        List<FlameGraphFrame<T>> focusedFrames = fullLayout.frames().stream()
                .filter(frame -> frame.samePath(originalFocus)
                        || frame.isDescendantOf(originalFocus))
                .map(frame -> normalizeFocusedFrame(frame, originalFocus))
                .toList();
        int maxDepth = focusedFrames.stream()
                .mapToInt(FlameGraphFrame::depth)
                .max()
                .orElse(-1) + 1;
        return layoutWithRows(focusedFrames, maxDepth, mode);
    }

    private int appendLevels(
            List<FlameGraphNode<T>> children,
            int depth,
            double parentX,
            double parentWidth,
            List<Integer> parentPath,
            FlameGraphModel<T> model,
            List<FlameGraphFrame<T>> frames) {
        int deepestDepth = depth - 1;
        List<ChildLayout<T>> currentLevel = List.of(new ChildLayout<>(
                children,
                parentX,
                parentWidth,
                parentPath));
        int currentDepth = depth;
        while (!currentLevel.isEmpty()
                && currentDepth < model.maxDepth()
                && frames.size() < model.maxFrames()) {
            List<ChildLayout<T>> nextLevel = new ArrayList<>();
            for (ChildLayout<T> parent : currentLevel) {
                List<FlameGraphNode<T>> sortedChildren = sortedByWeight(parent.children());
                double denominator = childWeightSum(sortedChildren);
                double nextX = parent.x();
                for (int index = 0; index < sortedChildren.size(); index++) {
                    if (frames.size() >= model.maxFrames()) {
                        break;
                    }
                    FlameGraphNode<T> child = sortedChildren.get(index);
                    double childWidth = denominator <= 0 ? 0 : parent.width() * child.weight() / denominator;
                    List<Integer> childPath = appendPath(parent.path(), index);
                    frames.add(new FlameGraphFrame<>(child, currentDepth, currentDepth, nextX, childWidth, childPath));
                    nextLevel.add(new ChildLayout<>(
                            child.children(),
                            nextX,
                            childWidth,
                            childPath));
                    deepestDepth = Math.max(deepestDepth, currentDepth);
                    nextX += childWidth;
                }
                if (frames.size() >= model.maxFrames()) {
                    break;
                }
            }
            currentLevel = nextLevel;
            currentDepth++;
        }
        return deepestDepth;
    }

    private FlameGraphFrame<T> normalizeFocusedFrame(FlameGraphFrame<T> frame, FlameGraphFrame<T> focusFrame) {
        return new FlameGraphFrame<>(
                frame.node(),
                frame.depth() - focusFrame.depth(),
                frame.row(),
                (frame.x() - focusFrame.x()) / focusFrame.width(),
                frame.width() / focusFrame.width(),
                frame.path());
    }

    private FlameGraphLayout<T> layoutWithRows(
            List<FlameGraphFrame<T>> frames,
            int maxDepth,
            FlameGraphMode mode) {
        FlameGraphMode resolvedMode = mode == null ? FlameGraphMode.ICICLE : mode;
        List<FlameGraphFrame<T>> rowFrames = frames.stream()
                .map(frame -> new FlameGraphFrame<>(
                        frame.node(),
                        frame.depth(),
                        rowFor(frame.depth(), maxDepth, resolvedMode),
                        frame.x(),
                        frame.width(),
                        frame.path()))
                .toList();
        return new FlameGraphLayout<>(rowFrames, maxDepth);
    }

    private int rowFor(int depth, int maxDepth, FlameGraphMode mode) {
        if (mode == FlameGraphMode.FLAME) {
            return Math.max(0, maxDepth - depth - 1);
        }
        return depth;
    }

    private List<Integer> appendPath(List<Integer> parentPath, int index) {
        List<Integer> path = new ArrayList<>(parentPath);
        path.add(index);
        return path;
    }

    private List<FlameGraphNode<T>> sortedByWeight(List<FlameGraphNode<T>> children) {
        return children.stream()
                .filter(child -> child != null && child.weight() > 0)
                .sorted(Comparator.comparingDouble(FlameGraphNode<T>::weight).reversed())
                .toList();
    }

    private double childWeightSum(List<FlameGraphNode<T>> children) {
        return children.stream()
                .mapToDouble(FlameGraphNode::weight)
                .sum();
    }

    private record ChildLayout<T>(
            List<FlameGraphNode<T>> children,
            double x,
            double width,
            List<Integer> path) {
    }
}
