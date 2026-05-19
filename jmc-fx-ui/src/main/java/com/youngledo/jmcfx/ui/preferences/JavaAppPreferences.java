package com.youngledo.jmcfx.ui.preferences;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;

import com.youngledo.jmcfx.ui.i18n.LanguageMode;

public final class JavaAppPreferences implements AppPreferences {

    private static final String LANGUAGE_MODE = "languageMode";

    private final Preferences preferences;

    public JavaAppPreferences() {
        this(Preferences.userNodeForPackage(JavaAppPreferences.class));
    }

    private JavaAppPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    public static JavaAppPreferences inMemory() {
        return new JavaAppPreferences(new InMemoryPreferences());
    }

    @Override
    public LanguageMode languageMode() {
        return LanguageMode.fromPersistedValue(preferences.get(LANGUAGE_MODE, LanguageMode.SYSTEM.persistedValue()));
    }

    @Override
    public void setLanguageMode(LanguageMode mode) {
        preferences.put(LANGUAGE_MODE, (mode == null ? LanguageMode.SYSTEM : mode).persistedValue());
    }

    void putRaw(String key, String value) {
        preferences.put(key, value);
    }

    private static final class InMemoryPreferences extends AbstractPreferences {

        private final Map<String, String> values = new HashMap<>();

        private InMemoryPreferences() {
            super(null, "");
        }

        @Override
        protected void putSpi(String key, String value) {
            values.put(key, value);
        }

        @Override
        protected String getSpi(String key) {
            return values.get(key);
        }

        @Override
        protected void removeSpi(String key) {
            values.remove(key);
        }

        @Override
        protected void removeNodeSpi() {
            values.clear();
        }

        @Override
        protected String[] keysSpi() {
            return values.keySet().toArray(String[]::new);
        }

        @Override
        protected String[] childrenNamesSpi() {
            return new String[0];
        }

        @Override
        protected AbstractPreferences childSpi(String name) {
            return new InMemoryPreferences();
        }

        @Override
        protected void syncSpi() {
        }

        @Override
        protected void flushSpi() {
        }
    }
}
