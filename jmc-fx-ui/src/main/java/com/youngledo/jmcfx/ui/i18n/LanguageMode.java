package com.youngledo.jmcfx.ui.i18n;

import java.util.Locale;

public enum LanguageMode {
    ENGLISH("english"),
    CHINESE_SIMPLIFIED("zh-CN"),
    SYSTEM("system");

    private final String persistedValue;

    LanguageMode(String persistedValue) {
        this.persistedValue = persistedValue;
    }

    public String persistedValue() {
        return persistedValue;
    }

    public Locale resolve(Locale systemLocale) {
        return switch (this) {
            case ENGLISH -> Locale.ENGLISH;
            case CHINESE_SIMPLIFIED -> Locale.SIMPLIFIED_CHINESE;
            case SYSTEM -> resolveSystemLocale(systemLocale);
        };
    }

    public static LanguageMode fromPersistedValue(String value) {
        if (value == null || value.isBlank()) {
            return SYSTEM;
        }
        for (LanguageMode mode : values()) {
            if (mode.persistedValue.equals(value)) {
                return mode;
            }
        }
        return SYSTEM;
    }

    private static Locale resolveSystemLocale(Locale systemLocale) {
        if (systemLocale != null && "zh".equals(systemLocale.getLanguage())
                && "CN".equalsIgnoreCase(systemLocale.getCountry())) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        return Locale.ENGLISH;
    }
}
