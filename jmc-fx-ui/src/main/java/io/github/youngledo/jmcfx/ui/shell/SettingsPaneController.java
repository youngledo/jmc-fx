package io.github.youngledo.jmcfx.ui.shell;

import io.github.youngledo.jmcfx.application.ai.AiSettingsUseCase;
import io.github.youngledo.jmcfx.domain.model.ai.AiSettings;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;
import io.github.youngledo.jmcfx.ui.preferences.AppTheme;

import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Toggle;

/// Controller for Settings page bindings and preference selectors.
final class SettingsPaneController {

    private final SettingsPaneView view;
    private final AppShellViewModel viewModel;
    private final I18n i18n;
    private final AiSettingsUseCase aiSettingsUseCase;

    SettingsPaneController(SettingsPaneView view, AppShellViewModel viewModel, I18n i18n,
            AiSettingsUseCase aiSettingsUseCase) {
        this.view = view;
        this.viewModel = viewModel;
        this.i18n = i18n;
        this.aiSettingsUseCase = aiSettingsUseCase;
    }

    void configure() {
        bindLocalizedText();
        configureLanguageSelector();
        configureThemeSelector();
        configureAiSettings();
    }

    private void bindLocalizedText() {
        view.titleLabel.textProperty().bind(i18n.text("settings.title"));
        view.languageLabel.textProperty().bind(i18n.text("settings.language"));
        view.themeLabel.textProperty().bind(i18n.text("settings.theme"));
        view.aiLabel.textProperty().bind(i18n.text("settings.ai"));
        view.aiEnabledCheckBox.textProperty().bind(i18n.text("settings.ai.enabled"));
        view.aiBaseUrlLabel.textProperty().bind(i18n.text("settings.ai.baseUrl"));
        view.aiBaseUrlField.promptTextProperty().bind(i18n.text("settings.ai.baseUrl.prompt"));
        view.aiModelLabel.textProperty().bind(i18n.text("settings.ai.model"));
        view.aiModelField.promptTextProperty().bind(i18n.text("settings.ai.model.prompt"));
        view.aiTemperatureLabel.textProperty().bind(i18n.text("settings.ai.temperature"));
        view.aiMaxOutputTokensLabel.textProperty().bind(i18n.text("settings.ai.maxOutputTokens"));
        view.aiSaveButton.textProperty().bind(i18n.text("settings.ai.save"));
        view.aiResetButton.textProperty().bind(i18n.text("settings.ai.reset"));
    }

    private void configureLanguageSelector() {
        view.languageFollowSystemRadio.setUserData(LanguageMode.SYSTEM);
        view.languageEnglishRadio.setUserData(LanguageMode.ENGLISH);
        view.languageChineseRadio.setUserData(LanguageMode.CHINESE_SIMPLIFIED);

        view.languageFollowSystemRadio.textProperty().bind(i18n.text("settings.language.followSystem"));
        view.languageEnglishRadio.textProperty().bind(i18n.text("settings.language.english"));
        view.languageChineseRadio.textProperty().bind(i18n.text("settings.language.chineseSimplified"));

        view.languageToggleGroup.selectToggle(modeToToggle(viewModel.languageModeProperty().get()));

        view.languageToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.getUserData() instanceof LanguageMode mode) {
                viewModel.setLanguageMode(mode);
                i18n.setLanguageMode(mode);
            }
        });
    }

    private Toggle modeToToggle(LanguageMode mode) {
        if (mode == LanguageMode.ENGLISH) {
            return view.languageEnglishRadio;
        }
        if (mode == LanguageMode.CHINESE_SIMPLIFIED) {
            return view.languageChineseRadio;
        }
        return view.languageFollowSystemRadio;
    }

    private void configureThemeSelector() {
        view.themeFollowSystemRadio.setUserData(AppTheme.SYSTEM);
        view.themeLightRadio.setUserData(AppTheme.PRIMER_LIGHT);
        view.themeDarkRadio.setUserData(AppTheme.PRIMER_DARK);

        view.themeFollowSystemRadio.textProperty().bind(i18n.text("settings.theme.followSystem"));
        view.themeLightRadio.textProperty().bind(i18n.text("settings.theme.light"));
        view.themeDarkRadio.textProperty().bind(i18n.text("settings.theme.dark"));

        view.themeToggleGroup.selectToggle(themeToToggle(viewModel.themeProperty().get()));

        view.themeToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.getUserData() instanceof AppTheme theme) {
                viewModel.setTheme(theme);
            }
        });
    }

    private Toggle themeToToggle(AppTheme theme) {
        if (theme == AppTheme.PRIMER_LIGHT) {
            return view.themeLightRadio;
        }
        if (theme == AppTheme.PRIMER_DARK) {
            return view.themeDarkRadio;
        }
        return view.themeFollowSystemRadio;
    }

    private void configureAiSettings() {
        view.aiTemperatureSpinner.setValueFactory(
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 2.0,
                        AiSettingsUseCase.DEFAULT_TEMPERATURE, 0.1));
        view.aiMaxOutputTokensSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 128_000,
                        AiSettingsUseCase.DEFAULT_MAX_OUTPUT_TOKENS, 256));
        if (aiSettingsUseCase == null) {
            setAiSettingsDisabled();
            return;
        }
        loadAiSettings(aiSettingsUseCase.load());
        updateApiKeyStatus();
        view.aiSaveButton.setOnAction(event -> {
            aiSettingsUseCase.save(readAiSettings());
            updateApiKeyStatus();
        });
        view.aiResetButton.setOnAction(event -> loadAiSettings(AiSettingsUseCase.defaults()));
    }

    private void setAiSettingsDisabled() {
        view.aiEnabledCheckBox.setDisable(true);
        view.aiBaseUrlField.setDisable(true);
        view.aiModelField.setDisable(true);
        view.aiTemperatureSpinner.setDisable(true);
        view.aiMaxOutputTokensSpinner.setDisable(true);
        view.aiSaveButton.setDisable(true);
        view.aiResetButton.setDisable(true);
        view.aiApiKeyStatusLabel.textProperty().bind(i18n.text("settings.ai.unavailable"));
    }

    private void loadAiSettings(AiSettings settings) {
        view.aiEnabledCheckBox.setSelected(settings.enabled());
        view.aiBaseUrlField.setText(settings.baseUrl());
        view.aiModelField.setText(settings.model());
        view.aiTemperatureSpinner.getValueFactory().setValue(settings.temperature());
        view.aiMaxOutputTokensSpinner.getValueFactory().setValue(settings.maxOutputTokens());
    }

    private AiSettings readAiSettings() {
        return new AiSettings(
                view.aiEnabledCheckBox.isSelected(),
                view.aiBaseUrlField.getText(),
                view.aiModelField.getText(),
                view.aiTemperatureSpinner.getValue(),
                view.aiMaxOutputTokensSpinner.getValue(),
                false);
    }

    private void updateApiKeyStatus() {
        String key = aiSettingsUseCase.apiKeyEnvironmentConfigured()
                ? "settings.ai.apiKey.present"
                : "settings.ai.apiKey.missing";
        view.aiApiKeyStatusLabel.textProperty().unbind();
        view.aiApiKeyStatusLabel.textProperty().bind(i18n.text(key));
    }
}
