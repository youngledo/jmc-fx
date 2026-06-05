package io.github.youngledo.jmcfx.ui.histogram;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/// Tests HistogramView structural and CSS properties without JavaFX toolkit initialization.
class HistogramViewTest {

    @Test
    void appCssContainsHistogramTableStyle() throws IOException {
        String css = appCss();
        assertTrue(css.contains(".histogram-table"),
                "app.css must define .histogram-table style for HistogramView");
        assertTrue(css.contains(".percentage-bar"),
                "app.css must define .percentage-bar style for PercentageBarTableCell");
        assertTrue(css.contains(".percentage-bar .track"),
                "app.css must style the percentage bar track for dense tables");
        assertTrue(css.contains(".percentage-bar .bar"),
                "app.css must style the percentage bar fill for dense tables");

        String barBlock = cssBlock(css, ".percentage-bar");
        assertTrue(barBlock.contains("-fx-pref-height: 6px"),
                "percentage bars should stay compact in dense histogram rows");

        String trackBlock = cssBlock(css, ".percentage-bar .track");
        assertTrue(trackBlock.contains("-fx-background-color: -color-bg-subtle"),
                "percentage bar track should be muted");

        String fillBlock = cssBlock(css, ".percentage-bar .bar");
        assertTrue(fillBlock.contains("-fx-background-color: -color-accent-emphasis"),
                "percentage bar fill should use the app accent token");
    }

    @Test
    void histogramPackageExportsReusableComponents() {
        assertNotNull(PercentageBarTableCell.class);
        assertNotNull(HistogramView.class);
        assertNotNull(PercentageParser.class);
    }

    private static String appCss() throws IOException {
        try (InputStream stream = HistogramViewTest.class.getResourceAsStream("/css/app.css")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String cssBlock(String css, String selector) {
        int start = css.indexOf(selector + " {");
        if (start < 0) {
            return "";
        }
        int end = css.indexOf('}', start);
        return end < 0 ? css.substring(start) : css.substring(start, end);
    }
}
