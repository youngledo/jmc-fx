package io.github.youngledo.jmcfx.ui.shell;

import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;
import io.github.youngledo.jmcfx.ui.preferences.AppTheme;

import javafx.scene.control.Toggle;

/// Controller for Settings page bindings and preference selectors.
final class SettingsPaneController {

    private final SettingsPaneView view;
    private final AppShellViewModel viewModel;
    private final I18n i18n;

    SettingsPaneController(SettingsPaneView view, AppShellViewModel viewModel, I18n i18n) {
        this.view = view;
        this.viewModel = viewModel;
        this.i18n = i18n;
    }

    void configure() {
        bindLocalizedText();
        configureLanguageSelector();
        configureThemeSelector();
    }

    private void bindLocalizedText() {
        view.titleLabel.textProperty().bind(i18n.text("settings.title"));
        view.languageLabel.textProperty().bind(i18n.text("settings.language"));
        view.themeLabel.textProperty().bind(i18n.text("settings.theme"));
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
}
