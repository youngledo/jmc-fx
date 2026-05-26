package com.youngledo.jmcfx.ui.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.ColorScheme;
import org.junit.jupiter.api.Test;

class AppThemeTest {

    @Test
    void persistedValuesIncludeSystemLightAndDark() {
        assertEquals("system", AppTheme.SYSTEM.persistedValue());
        assertEquals("primer-light", AppTheme.PRIMER_LIGHT.persistedValue());
        assertEquals("primer-dark", AppTheme.PRIMER_DARK.persistedValue());
    }

    @Test
    void invalidPersistedThemeFallsBackToSystem() {
        assertEquals(AppTheme.SYSTEM, AppTheme.fromPersistedValue(null));
        assertEquals(AppTheme.SYSTEM, AppTheme.fromPersistedValue(""));
        assertEquals(AppTheme.SYSTEM, AppTheme.fromPersistedValue("solarized"));
    }

    @Test
    void resolvesSystemThemeFromColorScheme() {
        assertEquals(AppTheme.PRIMER_LIGHT, AppTheme.SYSTEM.resolve(ColorScheme.LIGHT));
        assertEquals(AppTheme.PRIMER_DARK, AppTheme.SYSTEM.resolve(ColorScheme.DARK));
        assertEquals(AppTheme.PRIMER_LIGHT, AppTheme.PRIMER_LIGHT.resolve(ColorScheme.DARK));
        assertEquals(AppTheme.PRIMER_DARK, AppTheme.PRIMER_DARK.resolve(ColorScheme.LIGHT));
    }

    @Test
    void concreteThemesExposeAtlantaFxStylesheets() {
        assertTrue(AppTheme.PRIMER_LIGHT.userAgentStylesheet().contains("primer-light"));
        assertTrue(AppTheme.PRIMER_DARK.userAgentStylesheet().contains("primer-dark"));
    }
}
