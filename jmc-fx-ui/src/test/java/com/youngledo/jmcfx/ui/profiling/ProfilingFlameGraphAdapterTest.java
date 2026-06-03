package com.youngledo.jmcfx.ui.profiling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.youngledo.jmcfx.domain.model.StackFrameInfo;
import com.youngledo.jmcfx.domain.model.StackTreeNode;
import com.youngledo.jmcfx.flamegraph.FlameGraphFrameColors;
import com.youngledo.jmcfx.flamegraph.FlameGraphFrameState;
import com.youngledo.jmcfx.flamegraph.FlameGraphMode;
import com.youngledo.jmcfx.flamegraph.FlameGraphModel;
import com.youngledo.jmcfx.flamegraph.FlameGraphRenderContext;

import javafx.scene.paint.Color;

import org.junit.jupiter.api.Test;

class ProfilingFlameGraphAdapterTest {

    @Test
    void convertsStackTreeChildrenToFlameGraphModel() {
        StackTreeNode root = node("root", 100, 100,
                node("caller", 60, 60),
                node("sibling", 40, 40));

        FlameGraphModel<StackFrameInfo> model = ProfilingFlameGraphAdapter.toModel(root);

        assertEquals("root", model.root().label());
        assertEquals(100, model.root().weight());
        assertEquals(2, model.root().children().size());
        assertEquals("caller", model.root().children().getFirst().label());
        assertEquals(60, model.root().children().getFirst().weight());
    }

    @Test
    void preservesStackFrameInfoAsPayload() {
        StackFrameInfo info = frameInfo();
        StackTreeNode root = new StackTreeNode("root", 100, 100, StackFrameInfo.EMPTY,
                List.of(new StackTreeNode("com.example.Worker.run()", 60, 60, info, List.of())));

        FlameGraphModel<StackFrameInfo> model = ProfilingFlameGraphAdapter.toModel(root);

        assertSame(info, model.root().children().getFirst().payload());
    }

    @Test
    void profilingModelKeepsDeepRealWorldStacksVisible() {
        FlameGraphModel<StackFrameInfo> model = ProfilingFlameGraphAdapter.toModel(nestedStackTree(32));

        assertTrue(model.maxDepth() >= 32);
        assertTrue(model.maxFrames() >= 65_536);
    }

    @Test
    void tooltipIncludesPackageClassWeightTypeByteCodeIndexAndLine() {
        StackFrameInfo info = frameInfo();
        com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> frame =
                frame("com.example.Worker.run()", 42, 12.5, info);

        String tooltip = ProfilingFlameGraphAdapter.tooltipProvider().tooltip(frame);

        assertTrue(tooltip.contains("run"));
        assertTrue(tooltip.contains("Package: com.example"));
        assertTrue(tooltip.contains("Weight: 42"));
        assertTrue(tooltip.contains("Type: Interpreted"));
        assertTrue(tooltip.contains("Byte Code Index: 12"));
        assertTrue(tooltip.contains("Line: 34"));
    }

    @Test
    void textProviderPrefersStructuredLabel() {
        StackFrameInfo info = frameInfo();
        com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> frame =
                frame("com.example.Worker.run()", 42, 12.5, info);

        assertEquals("Worker.run", ProfilingFlameGraphAdapter.textProvider().text(frame));
    }

    @Test
    void colorProviderReturnsReadableMutedText() {
        com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> frame =
                frame("com.example.Worker.run()", 42, 12.5, frameInfo());

        assertTrue(((Color) ProfilingFlameGraphAdapter.colorProvider()
                .colors(frame, FlameGraphFrameState.MUTED, new FlameGraphRenderContext(FlameGraphMode.ICICLE, 1))
                .text())
                .getOpacity() >= 0.75);
    }

    @Test
    void colorProviderUsesWarmFlameAndCoolIciclePalettes() {
        com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> frame =
                frame("com.example.Worker.run()", 42, 12.5, frameInfo());

        Color flameFill = (Color) ProfilingFlameGraphAdapter.colorProvider()
                .colors(frame, FlameGraphFrameState.DEFAULT, new FlameGraphRenderContext(FlameGraphMode.FLAME, 1))
                .fill();
        Color icicleFill = (Color) ProfilingFlameGraphAdapter.colorProvider()
                .colors(frame, FlameGraphFrameState.DEFAULT, new FlameGraphRenderContext(FlameGraphMode.ICICLE, 1))
                .fill();

        assertTrue(saturation(flameFill) >= 0.55);
        assertTrue(saturation(icicleFill) >= 0.55);
        assertTrue(relativeLuminance(flameFill) >= 0.35);
        assertTrue(relativeLuminance(icicleFill) >= 0.35);
    }

    @Test
    void colorProviderUsesMethodStableMulticolorPalette() {
        StackFrameInfo workerInfo = frameInfo("Worker.run", "run", "com.example.Worker");
        StackFrameInfo parserInfo = frameInfo("Parser.parse", "parse", "com.example.Parser");
        com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> workerFrame =
                frame("com.example.Worker.run()", 42, 12.5, workerInfo, 1, List.of(0));
        com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> sameWorkerElsewhere =
                frame("com.example.Worker.run()", 24, 8.5, workerInfo, 4, List.of(0, 1, 2, 3));
        com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> parserFrame =
                frame("com.example.Parser.parse()", 18, 7.5, parserInfo, 4, List.of(0, 1, 2, 4));

        Color workerFill = (Color) ProfilingFlameGraphAdapter.colorProvider()
                .colors(workerFrame, FlameGraphFrameState.DEFAULT, new FlameGraphRenderContext(FlameGraphMode.FLAME, 1))
                .fill();
        Color sameWorkerFill = (Color) ProfilingFlameGraphAdapter.colorProvider()
                .colors(sameWorkerElsewhere, FlameGraphFrameState.DEFAULT, new FlameGraphRenderContext(FlameGraphMode.FLAME, 1))
                .fill();
        Color parserFill = (Color) ProfilingFlameGraphAdapter.colorProvider()
                .colors(parserFrame, FlameGraphFrameState.DEFAULT, new FlameGraphRenderContext(FlameGraphMode.FLAME, 1))
                .fill();

        assertEquals(workerFill, sameWorkerFill);
        assertTrue(!workerFill.equals(parserFill));
    }

    @Test
    void colorProviderUsesManyBrightIcicleColors() {
        java.util.Set<Color> colors = new java.util.HashSet<>();
        for (int index = 0; index < 12; index++) {
            StackFrameInfo info = frameInfo("Method" + index + ".run", "run", "com.example.Method" + index);
            com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> frame =
                    frame("com.example.Method" + index + ".run()", 42, 12.5, info, index % 6, List.of(index));
            Color fill = (Color) ProfilingFlameGraphAdapter.colorProvider()
                    .colors(frame, FlameGraphFrameState.DEFAULT, new FlameGraphRenderContext(FlameGraphMode.ICICLE, 1))
                    .fill();
            colors.add(fill);
            assertTrue(saturation(fill) >= 0.45);
            assertTrue(relativeLuminance(fill) >= 0.30);
        }

        assertTrue(colors.size() >= 8);
    }

    @Test
    void colorProviderKeepsDefaultTextReadableOnBrightPalette() {
        com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> frame =
                frame("com.example.Worker.run()", 42, 12.5, frameInfo());

        FlameGraphFrameColors colors = ProfilingFlameGraphAdapter.colorProvider()
                .colors(frame, FlameGraphFrameState.DEFAULT, new FlameGraphRenderContext(FlameGraphMode.ICICLE, 1));
        Color fill = (Color) colors.fill();
        Color text = (Color) colors.text();

        assertTrue(contrastRatio(fill, text) >= 4.5);
    }

    @Test
    void colorProviderUsesDifferentFlameAndIcicleFamiliesForSameMethod() {
        com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> frame =
                frame("com.example.Worker.run()", 42, 12.5, frameInfo());

        Color flameFill = (Color) ProfilingFlameGraphAdapter.colorProvider()
                .colors(frame, FlameGraphFrameState.DEFAULT, new FlameGraphRenderContext(FlameGraphMode.FLAME, 1))
                .fill();
        Color icicleFill = (Color) ProfilingFlameGraphAdapter.colorProvider()
                .colors(frame, FlameGraphFrameState.DEFAULT, new FlameGraphRenderContext(FlameGraphMode.ICICLE, 1))
                .fill();

        assertTrue(colorDistance(flameFill, icicleFill) >= 0.20);
    }

    @Test
    void colorProviderKeepsSelectionAndPathOnMethodPalette() {
        com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> frame =
                frame("com.example.Worker.run()", 42, 12.5, frameInfo());

        FlameGraphFrameColors defaultColors = ProfilingFlameGraphAdapter.colorProvider()
                .colors(frame, FlameGraphFrameState.DEFAULT, new FlameGraphRenderContext(FlameGraphMode.ICICLE, 1));
        FlameGraphFrameColors selectedColors = ProfilingFlameGraphAdapter.colorProvider()
                .colors(frame, FlameGraphFrameState.SELECTED, new FlameGraphRenderContext(FlameGraphMode.ICICLE, 1));
        FlameGraphFrameColors pathColors = ProfilingFlameGraphAdapter.colorProvider()
                .colors(frame, FlameGraphFrameState.PATH, new FlameGraphRenderContext(FlameGraphMode.ICICLE, 1));

        assertEquals(defaultColors.fill(), selectedColors.fill());
        assertEquals(defaultColors.fill(), pathColors.fill());
        assertTrue(!selectedColors.stroke().equals(defaultColors.stroke()));
        assertTrue(!pathColors.stroke().equals(defaultColors.stroke()));
        assertTrue(contrastRatio((Color) selectedColors.fill(), (Color) selectedColors.text()) >= 4.5);
        assertTrue(contrastRatio((Color) pathColors.fill(), (Color) pathColors.text()) >= 4.5);
    }

    @Test
    void colorProviderKeepsHoverOnMethodPalette() {
        com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> frame =
                frame("com.example.Worker.run()", 42, 12.5, frameInfo());

        FlameGraphFrameColors defaultColors = ProfilingFlameGraphAdapter.colorProvider()
                .colors(frame, FlameGraphFrameState.DEFAULT, new FlameGraphRenderContext(FlameGraphMode.FLAME, 1));
        FlameGraphFrameColors hoveredColors = ProfilingFlameGraphAdapter.colorProvider()
                .colors(frame, FlameGraphFrameState.HOVERED, new FlameGraphRenderContext(FlameGraphMode.FLAME, 1));

        assertEquals(defaultColors.fill(), hoveredColors.fill());
        assertTrue(!hoveredColors.stroke().equals(defaultColors.stroke()));
        assertTrue(contrastRatio((Color) hoveredColors.fill(), (Color) hoveredColors.text()) >= 4.5);
    }

    private StackFrameInfo frameInfo() {
        return frameInfo("Worker.run", "run", "com.example.Worker");
    }

    private StackFrameInfo frameInfo(String label, String methodName, String className) {
        return new StackFrameInfo(
                label,
                methodName,
                "com.example",
                className,
                "Interpreted",
                12,
                34);
    }

    private StackTreeNode node(String method, int count, double percentage, StackTreeNode... children) {
        return new StackTreeNode(method, count, percentage, List.of(children));
    }

    private StackTreeNode nestedStackTree(int depth) {
        StackTreeNode child = node("leaf", 100, 100);
        for (int index = depth; index > 0; index--) {
            child = node("frame-" + index, 100, 100, child);
        }
        return node("root", 100, 100, child);
    }

    private com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> frame(
            String label,
            double weight,
            double percentage,
            StackFrameInfo info) {
        return frame(label, weight, percentage, info, 0, List.of(0));
    }

    private com.youngledo.jmcfx.flamegraph.FlameGraphFrame<StackFrameInfo> frame(
            String label,
            double weight,
            double percentage,
            StackFrameInfo info,
            int depth,
            List<Integer> path) {
        return new com.youngledo.jmcfx.flamegraph.FlameGraphFrame<>(
                new com.youngledo.jmcfx.flamegraph.FlameGraphNode<>(
                        label,
                        weight,
                        percentage,
                        info,
                        List.of()),
                depth,
                depth,
                0,
                1,
                path);
    }

    private static double saturation(Color color) {
        double max = Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue()));
        double min = Math.min(color.getRed(), Math.min(color.getGreen(), color.getBlue()));
        return max == 0 ? 0 : (max - min) / max;
    }

    private static double contrastRatio(Color first, Color second) {
        double firstLuminance = relativeLuminance(first);
        double secondLuminance = relativeLuminance(second);
        double lighter = Math.max(firstLuminance, secondLuminance);
        double darker = Math.min(firstLuminance, secondLuminance);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double colorDistance(Color first, Color second) {
        double red = first.getRed() - second.getRed();
        double green = first.getGreen() - second.getGreen();
        double blue = first.getBlue() - second.getBlue();
        return Math.sqrt(red * red + green * green + blue * blue);
    }

    private static double relativeLuminance(Color color) {
        return 0.2126 * linearChannel(color.getRed())
                + 0.7152 * linearChannel(color.getGreen())
                + 0.0722 * linearChannel(color.getBlue());
    }

    private static double linearChannel(double value) {
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
