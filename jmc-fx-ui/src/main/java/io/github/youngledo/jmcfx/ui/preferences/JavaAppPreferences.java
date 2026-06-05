package io.github.youngledo.jmcfx.ui.preferences;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;

import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;

public final class JavaAppPreferences implements AppPreferences {

    private static final String LANGUAGE_MODE = "languageMode";
    private static final String THEME = "theme";
    private static final String LEGACY_UI_PREFERENCES_NODE = "/com/youngledo/jmcfx/ui/preferences";

    private final Preferences preferences;

    public JavaAppPreferences() {
        this(Preferences.userRoot().node(LEGACY_UI_PREFERENCES_NODE));
    }

    private JavaAppPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    public static JavaAppPreferences inMemory() {
        return new JavaAppPreferences(new InMemoryPreferences());
    }

    static String defaultNodePath() {
        return LEGACY_UI_PREFERENCES_NODE;
    }

    @Override
    public LanguageMode languageMode() {
        return LanguageMode.fromPersistedValue(preferences.get(LANGUAGE_MODE, LanguageMode.SYSTEM.persistedValue()));
    }

    @Override
    public void setLanguageMode(LanguageMode mode) {
        preferences.put(LANGUAGE_MODE, (mode == null ? LanguageMode.SYSTEM : mode).persistedValue());
    }

    @Override
    public AppTheme theme() {
        return AppTheme.fromPersistedValue(preferences.get(THEME, AppTheme.SYSTEM.persistedValue()));
    }

    @Override
    public void setTheme(AppTheme theme) {
        preferences.put(THEME, (theme == null ? AppTheme.SYSTEM : theme).persistedValue());
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
