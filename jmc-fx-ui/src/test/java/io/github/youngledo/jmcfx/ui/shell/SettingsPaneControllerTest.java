package io.github.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.youngledo.jmcfx.application.ai.AiSettingsUseCase;
import io.github.youngledo.jmcfx.domain.model.ai.AiSettings;
import io.github.youngledo.jmcfx.domain.service.ai.AiSettingsRepository;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;

class SettingsPaneControllerTest {

    @org.junit.jupiter.api.BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            assertTrue(latch.await(30, TimeUnit.SECONDS), "JavaFX toolkit did not start in time.");
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized by another test class.
        }
    }

    @Test
    void savesAiSettingsWithoutApiKey() {
        FakeAiSettingsRepository repository = new FakeAiSettingsRepository(Optional.empty());
        SettingsPaneView view = new SettingsPaneView();
        SettingsPaneController controller = new SettingsPaneController(view, new AppShellViewModel(),
                new I18n(Locale.ENGLISH),
                new AiSettingsUseCase(repository, Map.of(AiSettingsUseCase.API_KEY_ENVIRONMENT_VARIABLE, "key")));

        controller.configure();
        view.aiEnabledCheckBox.setSelected(false);
        view.aiBaseUrlField.setText("https://ai.example/v1");
        view.aiModelField.setText("jfr-model");
        view.aiTemperatureSpinner.getValueFactory().setValue(0.8);
        view.aiMaxOutputTokensSpinner.getValueFactory().setValue(8_192);
        view.aiSaveButton.fire();

        AiSettings saved = repository.load().orElseThrow();
        assertFalse(saved.enabled());
        assertEquals("https://ai.example/v1", saved.baseUrl());
        assertEquals("jfr-model", saved.model());
        assertEquals(0.8, saved.temperature());
        assertEquals(8_192, saved.maxOutputTokens());
        assertFalse(saved.saveApiKeyLocally());
        assertTrue(view.aiApiKeyStatusLabel.getText().contains("OPENAI_API_KEY"));
    }

    @Test
    void resetsAiFormToDefaultsWithoutSaving() {
        FakeAiSettingsRepository repository = new FakeAiSettingsRepository(Optional.of(
                new AiSettings(false, "https://saved.example/v1", "saved-model", 1.1, 1_024, false)));
        SettingsPaneView view = new SettingsPaneView();
        SettingsPaneController controller = new SettingsPaneController(view, new AppShellViewModel(),
                new I18n(Locale.ENGLISH), new AiSettingsUseCase(repository, Map.of()));

        controller.configure();
        view.aiResetButton.fire();

        assertTrue(view.aiEnabledCheckBox.isSelected());
        assertEquals(AiSettingsUseCase.DEFAULT_BASE_URL, view.aiBaseUrlField.getText());
        assertEquals("", view.aiModelField.getText());
        assertEquals(AiSettingsUseCase.DEFAULT_TEMPERATURE, view.aiTemperatureSpinner.getValue());
        assertEquals(AiSettingsUseCase.DEFAULT_MAX_OUTPUT_TOKENS, view.aiMaxOutputTokensSpinner.getValue());
        assertEquals("saved-model", repository.load().orElseThrow().model());
    }

    private static final class FakeAiSettingsRepository implements AiSettingsRepository {

        private Optional<AiSettings> settings;

        private FakeAiSettingsRepository(Optional<AiSettings> settings) {
            this.settings = settings;
        }

        @Override
        public Optional<AiSettings> load() {
            return settings;
        }

        @Override
        public void save(AiSettings settings) {
            this.settings = Optional.of(settings);
        }
    }
}
