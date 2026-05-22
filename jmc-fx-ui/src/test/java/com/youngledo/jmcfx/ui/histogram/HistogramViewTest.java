package com.youngledo.jmcfx.ui.histogram;

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
}
