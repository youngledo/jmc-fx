package com.youngledo.jmcfx.ui.preferences;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.ColorScheme;

public enum AppTheme {
    SYSTEM("system"),
    PRIMER_LIGHT("primer-light"),
    PRIMER_DARK("primer-dark");

    private final String persistedValue;

    AppTheme(String persistedValue) {
        this.persistedValue = persistedValue;
    }

    public String persistedValue() {
        return persistedValue;
    }

    public String userAgentStylesheet() {
        return switch (this) {
            case SYSTEM, PRIMER_LIGHT -> new PrimerLight().getUserAgentStylesheet();
            case PRIMER_DARK -> new PrimerDark().getUserAgentStylesheet();
        };
    }

    public AppTheme resolve(ColorScheme colorScheme) {
        return switch (this) {
            case SYSTEM -> colorScheme == ColorScheme.DARK ? PRIMER_DARK : PRIMER_LIGHT;
            case PRIMER_LIGHT -> PRIMER_LIGHT;
            case PRIMER_DARK -> PRIMER_DARK;
        };
    }

    public static AppTheme fromPersistedValue(String value) {
        if (value == null || value.isBlank()) {
            return SYSTEM;
        }
        for (AppTheme theme : values()) {
            if (theme.persistedValue.equals(value)) {
                return theme;
            }
        }
        return SYSTEM;
    }
}
