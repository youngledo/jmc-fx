package com.youngledo.jmcfx.flamegraph.demo;

import java.util.List;

import com.youngledo.jmcfx.flamegraph.FlameGraphFrame;
import com.youngledo.jmcfx.flamegraph.FlameGraphFrameColors;
import com.youngledo.jmcfx.flamegraph.FlameGraphFrameState;
import com.youngledo.jmcfx.flamegraph.FlameGraphMode;
import com.youngledo.jmcfx.flamegraph.FlameGraphModel;
import com.youngledo.jmcfx.flamegraph.FlameGraphNode;
import com.youngledo.jmcfx.flamegraph.FlameGraphRenderContext;
import com.youngledo.jmcfx.flamegraph.FlameGraphView;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class FlameGraphDemoApplication extends Application {

    private static final Color[] FLAME_COLORS = {
            Color.web("#fb923c"), Color.web("#facc15"), Color.web("#f87171"), Color.web("#f59e0b"),
            Color.web("#fb7185"), Color.web("#f472b6"), Color.web("#fdba74"), Color.web("#fde047")
    };
    private static final Color[] ICICLE_COLORS = {
            Color.web("#38bdf8"), Color.web("#22d3ee"), Color.web("#2dd4bf"), Color.web("#34d399"),
            Color.web("#0ea5e9"), Color.web("#06b6d4"), Color.web("#14b8a6"), Color.web("#22c55e")
    };

    @Override
    public void start(Stage stage) {
        FlameGraphView<DemoFrame> flameGraph = new FlameGraphView<>();
        flameGraph.setModel(new FlameGraphModel<>(sampleTree(), 16, 2_000));
        flameGraph.setTextProvider(frame -> frame.node().payload().label());
        flameGraph.setTooltipProvider(this::tooltip);
        flameGraph.setColorProvider(this::colors);

        Button modeButton = new Button("Flame");
        modeButton.setOnAction(event -> {
            FlameGraphMode next = flameGraph.getMode() == FlameGraphMode.ICICLE
                    ? FlameGraphMode.FLAME
                    : FlameGraphMode.ICICLE;
            flameGraph.setMode(next);
            modeButton.setText(next == FlameGraphMode.FLAME ? "Icicle" : "Flame");
        });

        Button zoomInButton = new Button("+");
        zoomInButton.setOnAction(event -> flameGraph.zoomIn());
        Button zoomOutButton = new Button("-");
        zoomOutButton.setOnAction(event -> flameGraph.zoomOut());
        Button resetButton = new Button("Reset");
        resetButton.setOnAction(event -> flameGraph.resetZoom());

        TextField search = new TextField();
        search.setPromptText("Search frames");
        search.textProperty().addListener((obs, old, value) -> flameGraph.search(value));
        HBox.setHgrow(search, Priority.ALWAYS);

        HBox toolbar = new HBox(8, modeButton, zoomInButton, zoomOutButton, resetButton, search);
        toolbar.setPadding(new Insets(10));

        BorderPane root = new BorderPane(flameGraph, toolbar, null, null, null);
        root.setPadding(new Insets(8));
        Scene scene = new Scene(root, 1000, 420);
        stage.setTitle("JavaFX Flame Graph Demo");
        stage.setScene(scene);
        stage.show();
    }

    private String tooltip(FlameGraphFrame<DemoFrame> frame) {
        DemoFrame payload = frame.node().payload();
        return payload.label() + System.lineSeparator()
                + "Package: " + payload.packageName() + System.lineSeparator()
                + "Weight: " + (long) frame.node().weight() + System.lineSeparator()
                + "Type: " + payload.kind();
    }

    private FlameGraphFrameColors colors(
            FlameGraphFrame<DemoFrame> frame,
            FlameGraphFrameState state,
            FlameGraphRenderContext context) {
        Color fill = palette(context.mode())[Math.floorMod(frame.node().payload().label().hashCode(), palette(context.mode()).length)];
        return switch (state) {
            case SELECTED, HOVERED -> new FlameGraphFrameColors(fill, Color.web("#111827"), readableText(fill));
            case PATH -> new FlameGraphFrameColors(fill, mix(fill, Color.BLACK, 0.35), readableText(fill));
            case MATCH -> new FlameGraphFrameColors(Color.web("#fde68a"), Color.web("#f97316"), Color.web("#111827"));
            case MUTED -> new FlameGraphFrameColors(Color.web("#d1d5db"), Color.web("#9ca3af"), Color.web("#4b5563"));
            case DEFAULT -> new FlameGraphFrameColors(fill, fill.darker(), readableText(fill));
        };
    }

    private Color[] palette(FlameGraphMode mode) {
        return mode == FlameGraphMode.FLAME ? FLAME_COLORS : ICICLE_COLORS;
    }

    private Color readableText(Color fill) {
        return contrastRatio(fill, Color.web("#111827")) >= contrastRatio(fill, Color.WHITE)
                ? Color.web("#111827")
                : Color.WHITE;
    }

    private double contrastRatio(Color first, Color second) {
        double firstLuminance = luminance(first);
        double secondLuminance = luminance(second);
        double lighter = Math.max(firstLuminance, secondLuminance);
        double darker = Math.min(firstLuminance, secondLuminance);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private double luminance(Color color) {
        return 0.2126 * linear(color.getRed()) + 0.7152 * linear(color.getGreen()) + 0.0722 * linear(color.getBlue());
    }

    private double linear(double value) {
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private Color mix(Color base, Color overlay, double overlayOpacity) {
        double opacity = Math.clamp(overlayOpacity, 0, 1);
        return new Color(
                base.getRed() * (1 - opacity) + overlay.getRed() * opacity,
                base.getGreen() * (1 - opacity) + overlay.getGreen() * opacity,
                base.getBlue() * (1 - opacity) + overlay.getBlue() * opacity,
                1);
    }

    private FlameGraphNode<DemoFrame> sampleTree() {
        return node("root", 100,
                node("app.Main.start", 100,
                        node("parser.ConfigLoader.load", 34,
                                node("parser.Tokenizer.scan", 19),
                                node("parser.Validator.check", 15)),
                        node("service.ProfileService.compute", 41,
                                node("service.SymbolTable.resolve", 20),
                                node("service.Aggregator.merge", 21,
                                        node("storage.Buffer.copy", 9),
                                        node("storage.PageCache.find", 12))),
                        node("ui.Renderer.paint", 25,
                                node("ui.LayoutPass.measure", 11),
                                node("ui.CanvasLayer.draw", 14))));
    }

    @SafeVarargs
    private final FlameGraphNode<DemoFrame> node(String label, double weight, FlameGraphNode<DemoFrame>... children) {
        DemoFrame frame = new DemoFrame(label, packageName(label), "demo");
        return new FlameGraphNode<>(label, weight, weight, frame, List.of(children));
    }

    private String packageName(String label) {
        int index = label.lastIndexOf('.');
        return index < 0 ? "" : label.substring(0, index);
    }

    public static void main(String[] args) {
        launch(args);
    }

    private record DemoFrame(String label, String packageName, String kind) {
    }
}
