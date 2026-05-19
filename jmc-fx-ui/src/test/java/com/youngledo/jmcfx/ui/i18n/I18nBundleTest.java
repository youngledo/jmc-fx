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
        ResourceBundle english = ResourceBundle.getBundle(BUNDLE_BASE_NAME, Locale.ENGLISH);
        ResourceBundle chinese = ResourceBundle.getBundle(BUNDLE_BASE_NAME, Locale.SIMPLIFIED_CHINESE);

        assertEquals(english.keySet(), chinese.keySet());
    }

    @Test
    void bundleContainsCurrentShellKeys() {
        ResourceBundle english = ResourceBundle.getBundle(BUNDLE_BASE_NAME, Locale.ENGLISH);

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
                "jvms.title",
                "settings.title",
                "settings.language");

        for (String key : requiredKeys) {
            assertTrue(english.containsKey(key), "Missing key: " + key);
        }
    }
}
