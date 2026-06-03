package com.youngledo.jmcfx.ui.profiling;

import java.util.ArrayList;
import java.util.List;

import com.youngledo.jmcfx.domain.model.StackFrameInfo;
import com.youngledo.jmcfx.domain.model.StackTreeNode;
import com.youngledo.jmcfx.flamegraph.FlameGraphFrameColors;
import com.youngledo.jmcfx.flamegraph.FlameGraphFrameState;
import com.youngledo.jmcfx.flamegraph.FlameGraphMode;
import com.youngledo.jmcfx.flamegraph.FlameGraphModel;
import com.youngledo.jmcfx.flamegraph.FlameGraphNode;
import com.youngledo.jmcfx.flamegraph.FlameGraphRenderContext;
import com.youngledo.jmcfx.flamegraph.FrameColorProvider;
import com.youngledo.jmcfx.flamegraph.FrameTextProvider;
import com.youngledo.jmcfx.flamegraph.FrameTooltipProvider;

import javafx.scene.paint.Color;

public final class ProfilingFlameGraphAdapter {

    private static final int MAX_DEPTH = 64;
    private static final int MAX_FRAMES = 65_536;
    private static final Color[] FLAME_FILLS = {
            Color.web("#fb923c"),
            Color.web("#facc15"),
            Color.web("#f87171"),
            Color.web("#f59e0b"),
            Color.web("#fb7185"),
            Color.web("#f472b6"),
            Color.web("#fdba74"),
            Color.web("#fde047"),
            Color.web("#c084fc"),
            Color.web("#38bdf8"),
            Color.web("#34d399"),
            Color.web("#f97316")
    };
    private static final Color[] ICICLE_FILLS = {
            Color.web("#38bdf8"),
            Color.web("#22d3ee"),
            Color.web("#2dd4bf"),
            Color.web("#34d399"),
            Color.web("#0ea5e9"),
            Color.web("#06b6d4"),
            Color.web("#14b8a6"),
            Color.web("#22c55e"),
            Color.web("#f472b6"),
            Color.web("#f59e0b"),
            Color.web("#a3e635"),
            Color.web("#84cc16")
    };
    private static final FlameGraphFrameColors FLAME_MATCH_COLORS =
            new FlameGraphFrameColors(Color.web("#fde68a"), Color.web("#f97316"), Color.web("#111827"));
    private static final FlameGraphFrameColors MUTED_COLORS =
            new FlameGraphFrameColors(Color.web("#d1d5db"), Color.web("#9ca3af"), Color.web("#4b5563"));
    private static final FlameGraphFrameColors ICICLE_MATCH_COLORS =
            new FlameGraphFrameColors(Color.web("#bfdbfe"), Color.web("#38bdf8"), Color.web("#0f172a"));

    private ProfilingFlameGraphAdapter() {
    }

    public static FlameGraphModel<StackFrameInfo> toModel(StackTreeNode root) {
        if (root == null || root == StackTreeNode.EMPTY || root.count() <= 0 || childrenOf(root).isEmpty()) {
            return FlameGraphModel.empty();
        }
        return new FlameGraphModel<>(toNode(root), MAX_DEPTH, MAX_FRAMES);
    }

    public static FrameTextProvider<StackFrameInfo> textProvider() {
        return frame -> {
            if (frame == null) {
                return "";
            }
            StackFrameInfo info = frame.node().payload();
            if (info != null && !info.label().isBlank()) {
                return info.label();
            }
            if (info != null && !info.methodName().isBlank()) {
                return info.methodName();
            }
            return frame.node().label();
        };
    }

    public static FrameTooltipProvider<StackFrameInfo> tooltipProvider() {
        return frame -> {
            if (frame == null) {
                return "";
            }
            StackFrameInfo info = frame.node().payload();
            String title = textProvider().text(frame);
            List<String> lines = new ArrayList<>();
            lines.add(title.isBlank() ? frame.node().label() : title);
            if (info != null && !info.packageName().isBlank()) {
                lines.add("Package: " + info.packageName());
            }
            lines.add("Weight: " + formatWeight(frame.node().weight()));
            lines.add("Percentage: " + formatPercentage(frame.node().percentage()));
            if (info != null && !info.frameType().isBlank()) {
                lines.add("Type: " + info.frameType());
            }
            if (info != null && info.bci() != null) {
                lines.add("Byte Code Index: " + info.bci());
            }
            if (info != null && info.lineNumber() != null) {
                lines.add("Line: " + info.lineNumber());
            }
            return String.join(System.lineSeparator(), lines);
        };
    }

    public static FrameColorProvider<StackFrameInfo> colorProvider() {
        return ProfilingFlameGraphAdapter::colorsFor;
    }

    private static FlameGraphFrameColors colorsFor(
            com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> frame,
            FlameGraphFrameState state,
            FlameGraphRenderContext context) {
        FlameGraphFrameState resolvedState = state == null ? FlameGraphFrameState.DEFAULT : state;
        FlameGraphMode mode = context == null ? FlameGraphMode.ICICLE : context.mode();
        return switch (resolvedState) {
            case SELECTED -> selectedColors(frame, mode);
            case PATH -> pathColors(frame, mode);
            case MATCH -> mode == FlameGraphMode.FLAME ? FLAME_MATCH_COLORS : ICICLE_MATCH_COLORS;
            case MUTED -> MUTED_COLORS;
            case HOVERED -> hoveredColors(frame, mode);
            case DEFAULT -> defaultColors(frame, mode);
        };
    }

    private static FlameGraphFrameColors defaultColors(
            com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> frame,
            FlameGraphMode mode) {
        int seed = methodSeed(frame);
        Color fill = mode == FlameGraphMode.FLAME
                ? paletteColor(FLAME_FILLS, seed)
                : paletteColor(ICICLE_FILLS, seed);
        return new FlameGraphFrameColors(fill, fill.darker(), readableText(fill));
    }

    private static FlameGraphFrameColors selectedColors(
            com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> frame,
            FlameGraphMode mode) {
        FlameGraphFrameColors colors = defaultColors(frame, mode);
        Color fill = (Color) colors.fill();
        return new FlameGraphFrameColors(fill, Color.web("#111827"), readableText(fill));
    }

    private static FlameGraphFrameColors pathColors(
            com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> frame,
            FlameGraphMode mode) {
        FlameGraphFrameColors colors = defaultColors(frame, mode);
        Color fill = (Color) colors.fill();
        return new FlameGraphFrameColors(fill, mix(fill, Color.BLACK, 0.35), readableText(fill));
    }

    private static FlameGraphFrameColors hoveredColors(
            com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> frame,
            FlameGraphMode mode) {
        FlameGraphFrameColors colors = defaultColors(frame, mode);
        Color fill = (Color) colors.fill();
        return new FlameGraphFrameColors(fill, Color.web("#111827"), readableText(fill));
    }

    private static Color paletteColor(Color[] colors, int seed) {
        if (colors.length == 0) {
            return Color.web("#f59e0b");
        }
        int index = Math.floorMod(seed, colors.length);
        Color base = colors[index];
        double opacity = 0.05 + (Math.floorMod(seed / colors.length, 4) * 0.04);
        return mix(base, Color.WHITE, opacity);
    }

    private static Color mix(Color base, Color overlay, double overlayOpacity) {
        double opacity = Math.clamp(overlayOpacity, 0, 1);
        return new Color(
                base.getRed() * (1 - opacity) + overlay.getRed() * opacity,
                base.getGreen() * (1 - opacity) + overlay.getGreen() * opacity,
                base.getBlue() * (1 - opacity) + overlay.getBlue() * opacity,
                1);
    }

    private static int methodSeed(com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> frame) {
        if (frame == null) {
            return 0;
        }
        StackFrameInfo info = frame.node().payload();
        if (info != null) {
            String key = !info.typeName().isBlank() || !info.methodName().isBlank()
                    ? info.typeName() + "#" + info.methodName()
                    : info.label();
            if (!key.isBlank()) {
                return key.hashCode();
            }
        }
        return frame.node().label().hashCode();
    }

    private static Color readableText(Color fill) {
        Color darkText = Color.web("#111827");
        return contrastRatio(fill, darkText) >= contrastRatio(fill, Color.WHITE) ? darkText : Color.WHITE;
    }

    private static double contrastRatio(Color first, Color second) {
        double firstLuminance = relativeLuminance(first);
        double secondLuminance = relativeLuminance(second);
        double lighter = Math.max(firstLuminance, secondLuminance);
        double darker = Math.min(firstLuminance, secondLuminance);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double relativeLuminance(Color color) {
        return 0.2126 * linearChannel(color.getRed())
                + 0.7152 * linearChannel(color.getGreen())
                + 0.0722 * linearChannel(color.getBlue());
    }

    private static double linearChannel(double value) {
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static FlameGraphNode<StackFrameInfo> toNode(StackTreeNode node) {
        return new FlameGraphNode<>(
                node.method(),
                node.count(),
                node.percentage(),
                node.frameInfo(),
                childrenOf(node).stream()
                        .map(ProfilingFlameGraphAdapter::toNode)
                        .toList());
    }

    private static List<StackTreeNode> childrenOf(StackTreeNode node) {
        return node.children() == null ? List.of() : node.children();
    }

    private static String formatWeight(double value) {
        if (value == Math.rint(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static String formatPercentage(double value) {
        return "%.1f%%".formatted(value);
    }
}
