package com.youngledo.jmcfx.flamegraph;

import javafx.scene.text.Font;
import javafx.scene.text.Text;

final class FlameGraphTextMeasurer {

    private static final String ELLIPSIS = "...";
    private final Text textNode = new Text();

    String clip(String text, Font font, double maxWidth) {
        if (text == null || text.isBlank() || maxWidth <= 0) {
            return "";
        }
        if (width(text, font) <= maxWidth) {
            return text;
        }
        double ellipsisWidth = width(ELLIPSIS, font);
        if (ellipsisWidth > maxWidth) {
            return "";
        }
        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (width(text.substring(0, mid), font) + ellipsisWidth <= maxWidth) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low == 0 ? "" : text.substring(0, low) + ELLIPSIS;
    }

    double width(String text, Font font) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        textNode.setFont(font);
        textNode.setText(text);
        return textNode.getLayoutBounds().getWidth();
    }
}
