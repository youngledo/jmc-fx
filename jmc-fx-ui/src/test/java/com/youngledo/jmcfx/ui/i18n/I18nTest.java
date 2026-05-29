package com.youngledo.jmcfx.ui.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import javafx.beans.binding.StringBinding;
import org.junit.jupiter.api.Test;

class I18nTest {

    @Test
    void defaultsToEnglishWithoutUsingSystemLocale() {
        I18n i18n = new I18n(Locale.SIMPLIFIED_CHINESE);

        assertEquals(LanguageMode.ENGLISH, i18n.languageModeProperty().get());
        assertEquals(Locale.ENGLISH, i18n.localeProperty().get());
        assertEquals("Open JFR", i18n.get("home.openRecording"));
    }

    @Test
    void switchesLanguageAndUpdatesBindings() {
        I18n i18n = new I18n(Locale.ENGLISH);
        StringBinding binding = i18n.text("settings.language");

        assertEquals("Language", binding.get());

        i18n.setLanguageMode(LanguageMode.CHINESE_SIMPLIFIED);

        assertEquals(Locale.SIMPLIFIED_CHINESE, i18n.localeProperty().get());
        assertEquals("语言", binding.get());
    }

    @Test
    void followsSupportedSystemLocale() {
        I18n i18n = new I18n(Locale.SIMPLIFIED_CHINESE);

        i18n.setLanguageMode(LanguageMode.SYSTEM);

        assertEquals(Locale.SIMPLIFIED_CHINESE, i18n.localeProperty().get());
        assertEquals("打开JFR", i18n.get("home.openRecording"));
    }

    @Test
    void formatsArguments() {
        I18n i18n = new I18n(Locale.ENGLISH);

        assertEquals("Category: demo", i18n.format("heapDump.detail.category", "demo"));
    }

    @Test
    void marksMissingKeys() {
        I18n i18n = new I18n(Locale.ENGLISH);

        assertEquals("!missing.key!", i18n.get("missing.key"));
    }
}
