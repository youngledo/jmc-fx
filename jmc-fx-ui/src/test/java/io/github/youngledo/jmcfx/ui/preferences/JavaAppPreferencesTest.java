package io.github.youngledo.jmcfx.ui.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;
import org.junit.jupiter.api.Test;

class JavaAppPreferencesTest {

    private static final String LEGACY_NODE_PATH = "/com/youngledo/jmcfx/ui/preferences";

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

        assertEquals(AppTheme.SYSTEM, preferences.theme());

        preferences.setTheme(AppTheme.PRIMER_DARK);

        assertEquals(AppTheme.PRIMER_DARK, preferences.theme());
    }

    @Test
    void invalidPersistedThemeFallsBackToSystem() {
        JavaAppPreferences preferences = JavaAppPreferences.inMemory();

        preferences.putRaw("theme", "solarized");

        assertEquals(AppTheme.SYSTEM, preferences.theme());
    }

    @Test
    void defaultPreferencesKeepLegacyNodeAfterPackageRename() {
        assertEquals(LEGACY_NODE_PATH, JavaAppPreferences.defaultNodePath());
    }
}
