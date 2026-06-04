package com.youngledo.jmcfx.ui.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import org.junit.jupiter.api.Test;

class I18nBundleTest {

    private static final String BUNDLE_BASE_NAME = "com.youngledo.jmcfx.ui.i18n.messages";

    @Test
    void chineseBundleHasSameKeysAsEnglishFallback() {
        ResourceBundle english = bundle(Locale.ENGLISH);
        ResourceBundle chinese = bundle(Locale.SIMPLIFIED_CHINESE);

        assertEquals(english.keySet(), chinese.keySet());
    }

    @Test
    void bundleContainsCurrentShellKeys() {
        ResourceBundle english = bundle(Locale.ENGLISH);

        Set<String> requiredKeys = Set.of(
                "app.title",
                "home.kicker",
                "home.title",
                "home.subtitle",
                "home.openRecording",
                "home.connectJvm",
                "overview.title",
                "events.title",
                "events.filters.clear",
                "events.columns",
                "events.details.properties",
                "events.details.timing",
                "events.details.thread",
                "events.details.stackTrace",
                "analysis.title",
                "nav.heap",
                "locks.grouping.byClass",
                "locks.grouping.byAddress",
                "locks.grouping.byThread",
                "threadDumps.empty",
                "tlab.column.insideAvgSize",
                "tlab.column.outsideAvgSize",
                "jvms.title",
                "jvms.source.local",
                "jvms.source.manual",
                "jvms.state.attachable",
                "jvms.state.connected",
                "jvms.state.disconnected",
                "jvms.state.discovered",
                "jvms.state.unavailable",
                "settings.title",
                "settings.language");

        for (String key : requiredKeys) {
            assertTrue(english.containsKey(key), "Missing key: " + key);
        }
    }

    @Test
    void englishBundleKeepsMemoryAndLockLabelsReadable() {
        ResourceBundle english = bundle(Locale.ENGLISH);

        assertEquals("Heap Memory", english.getString("nav.heap"));
        assertEquals("No thread dump events found.", english.getString("threadDumps.empty"));
        assertEquals("By Class", english.getString("locks.grouping.byClass"));
        assertEquals("By Address", english.getString("locks.grouping.byAddress"));
        assertEquals("By Thread", english.getString("locks.grouping.byThread"));
    }

    private static ResourceBundle bundle(Locale locale) {
        Locale bundleLocale = Locale.SIMPLIFIED_CHINESE.equals(locale) ? Locale.SIMPLIFIED_CHINESE : Locale.ROOT;
        return ResourceBundle.getBundle(BUNDLE_BASE_NAME, bundleLocale);
    }
}
