package io.github.youngledo.jmcfx.flamegraph;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public record FlameGraphFrameColors(
        Paint fill,
        Paint stroke,
        Paint text) {

    public static final FlameGraphFrameColors DEFAULT =
            new FlameGraphFrameColors(Color.web("#d97706"), Color.web("#92400e"), Color.WHITE);
    public static final FlameGraphFrameColors SELECTED =
            new FlameGraphFrameColors(Color.web("#f59e0b"), Color.web("#111827"), Color.web("#111827"));
    public static final FlameGraphFrameColors MATCH =
            new FlameGraphFrameColors(Color.web("#fde68a"), Color.web("#f59e0b"), Color.web("#111827"));
    public static final FlameGraphFrameColors PATH =
            new FlameGraphFrameColors(Color.web("#fbbf24"), Color.web("#b45309"), Color.web("#111827"));
    public static final FlameGraphFrameColors MUTED =
            new FlameGraphFrameColors(Color.web("#d1d5db"), Color.web("#9ca3af"), Color.web("#6b7280"));
    public static final FlameGraphFrameColors HOVERED =
            new FlameGraphFrameColors(Color.web("#f97316"), Color.web("#111827"), Color.WHITE);

    public FlameGraphFrameColors {
        fill = fill == null ? Color.TRANSPARENT : fill;
        stroke = stroke == null ? Color.TRANSPARENT : stroke;
        text = text == null ? Color.BLACK : text;
    }
}
