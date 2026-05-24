package com.youngledo.jmcfx.ui.preferences;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;

public enum AppTheme {
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
            case PRIMER_LIGHT -> new PrimerLight().getUserAgentStylesheet();
            case PRIMER_DARK -> new PrimerDark().getUserAgentStylesheet();
        };
    }

    public AppTheme toggle() {
        return switch (this) {
            case PRIMER_LIGHT -> PRIMER_DARK;
            case PRIMER_DARK -> PRIMER_LIGHT;
        };
    }

    public static AppTheme fromPersistedValue(String value) {
        if (value == null || value.isBlank()) {
            return PRIMER_LIGHT;
        }
        for (AppTheme theme : values()) {
            if (theme.persistedValue.equals(value)) {
                return theme;
            }
        }
        return PRIMER_LIGHT;
    }
}
