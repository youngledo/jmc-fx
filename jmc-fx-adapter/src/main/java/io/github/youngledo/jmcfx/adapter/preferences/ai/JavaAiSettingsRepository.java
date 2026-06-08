package io.github.youngledo.jmcfx.adapter.preferences.ai;

import java.util.Optional;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;

import io.github.youngledo.jmcfx.domain.model.ai.AiSettings;
import io.github.youngledo.jmcfx.domain.service.ai.AiSettingsRepository;

public final class JavaAiSettingsRepository implements AiSettingsRepository {

    private static final String LEGACY_UI_PREFERENCES_NODE = "/com/youngledo/jmcfx/ui/preferences/ai";
    private static final String CONFIGURED = "configured";
    private static final String ENABLED = "enabled";
    private static final String BASE_URL = "baseUrl";
    private static final String MODEL = "model";
    private static final String TEMPERATURE = "temperature";
    private static final String MAX_OUTPUT_TOKENS = "maxOutputTokens";
    private static final String SAVE_API_KEY_LOCALLY = "saveApiKeyLocally";

    private final Preferences preferences;

    public JavaAiSettingsRepository() {
        this(Preferences.userRoot().node(LEGACY_UI_PREFERENCES_NODE));
    }

    private JavaAiSettingsRepository(Preferences preferences) {
        this.preferences = preferences;
    }

    public static JavaAiSettingsRepository inMemory() {
        return new JavaAiSettingsRepository(new InMemoryPreferences());
    }

    @Override
    public Optional<AiSettings> load() {
        if (!preferences.getBoolean(CONFIGURED, false)) {
            return Optional.empty();
        }
        return Optional.of(new AiSettings(
                preferences.getBoolean(ENABLED, true),
                preferences.get(BASE_URL, ""),
                preferences.get(MODEL, ""),
                preferences.getDouble(TEMPERATURE, 0.2),
                preferences.getInt(MAX_OUTPUT_TOKENS, 4_096),
                preferences.getBoolean(SAVE_API_KEY_LOCALLY, false)));
    }

    @Override
    public void save(AiSettings settings) {
        preferences.putBoolean(CONFIGURED, true);
        preferences.putBoolean(ENABLED, settings.enabled());
        preferences.put(BASE_URL, settings.baseUrl());
        preferences.put(MODEL, settings.model());
        preferences.putDouble(TEMPERATURE, settings.temperature());
        preferences.putInt(MAX_OUTPUT_TOKENS, settings.maxOutputTokens());
        preferences.putBoolean(SAVE_API_KEY_LOCALLY, settings.saveApiKeyLocally());
    }

    private static final class InMemoryPreferences extends AbstractPreferences {

        private final java.util.Map<String, String> values = new java.util.HashMap<>();

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
