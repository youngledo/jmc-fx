package com.youngledo.jmcfx.ui.aggregate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.youngledo.jmcfx.domain.model.KeyValueEntry;
import com.youngledo.jmcfx.domain.model.KeyValueSection;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/// Tests AggregateViewer structural and CSS properties without JavaFX toolkit initialization.
class AggregateViewerTest {

    @Test
    void appCssContainsAggregateViewerStyle() throws IOException {
        String css = appCss();
        assertTrue(css.contains(".aggregate-viewer"),
                "app.css must define .aggregate-viewer style");
        assertTrue(css.contains(".aggregate-section-title"),
                "app.css must define .aggregate-section-title style");
        assertTrue(css.contains(".aggregate-key"),
                "app.css must define .aggregate-key style");
        assertTrue(css.contains(".aggregate-value"),
                "app.css must define .aggregate-value style");
    }

    @Test
    void keyValueDomainRecordsSupportSectionStructure() {
        KeyValueEntry entry = new KeyValueEntry("Version", "25");
        assertEquals("Version", entry.label());
        assertEquals("25", entry.value());

        KeyValueSection section = new KeyValueSection("JVM Info", List.of(entry));
        assertEquals("JVM Info", section.title());
        assertEquals(1, section.entries().size());
        assertEquals("Version", section.entries().getFirst().label());
    }

    @Test
    void multipleSectionsSupported() {
        KeyValueSection section1 = new KeyValueSection("JVM Info",
                List.of(new KeyValueEntry("Version", "25")));
        KeyValueSection section2 = new KeyValueSection("Recording",
                List.of(new KeyValueEntry("Events", "1000")));
        List<KeyValueSection> sections = List.of(section1, section2);
        assertEquals(2, sections.size());
        assertEquals("JVM Info", sections.getFirst().title());
        assertEquals("Recording", sections.getLast().title());
    }

    private static String appCss() throws IOException {
        try (InputStream stream = AggregateViewerTest.class.getResourceAsStream("/css/app.css")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
