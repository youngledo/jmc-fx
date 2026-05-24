package com.youngledo.jmcfx.ui.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.youngledo.jmcfx.ui.i18n.LanguageMode;
import org.junit.jupiter.api.Test;

class JavaAppPreferencesTest {

    @Test
    void storesLanguageMode() {
        JavaAppPreferences preferences = JavaAppPreferences.inMemory();

        assertEquals(LanguageMode.SYSTEM, preferences.languageMode());

        preferences.setLanguageMode(LanguageMode.CHINESE_SIMPLIFIED);

        assertEquals(LanguageMode.CHINESE_SIMPLIFIED, preferences.languageMode());
    }

    @Test
    void invalidPersistedLanguageFallsBackToSystem() {
        JavaAppPreferences preferences = JavaAppPreferences.inMemory();

        preferences.putRaw("languageMode", "fr-FR");

        assertEquals(LanguageMode.SYSTEM, preferences.languageMode());
    }

    @Test
    void storesTheme() {
        JavaAppPreferences preferences = JavaAppPreferences.inMemory();

        assertEquals(AppTheme.PRIMER_LIGHT, preferences.theme());

        preferences.setTheme(AppTheme.PRIMER_DARK);

        assertEquals(AppTheme.PRIMER_DARK, preferences.theme());
    }

    @Test
    void invalidPersistedThemeFallsBackToLight() {
        JavaAppPreferences preferences = JavaAppPreferences.inMemory();

        preferences.putRaw("theme", "solarized");

        assertEquals(AppTheme.PRIMER_LIGHT, preferences.theme());
    }
}
