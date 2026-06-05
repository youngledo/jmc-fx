package io.github.youngledo.jmcfx.ui.shell;

import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.preferences.AppPreferences;
import io.github.youngledo.jmcfx.ui.preferences.AppTheme;

import javafx.application.Application;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;

/// Builds the JavaFX shell and injects service-backed view models.
///
/// This keeps view construction in the UI layer and avoids direct framework
/// setup in application startup code.
public class AppShellFactory {

    private final AppShellFactoryDependencies dependencies;

    public AppShellFactory(AppShellFactoryDependencies dependencies) {
        this.dependencies = dependencies;
    }

    public AppShell create() {
        AppPreferences preferences = dependencies.preferences();
        I18n i18n = dependencies.i18n();
        AppShellViewModel viewModel = new AppShellViewModel();
        viewModel.setLanguageMode(preferences.languageMode());
        viewModel.setTheme(preferences.theme());
        i18n.setLanguageMode(viewModel.languageModeProperty().get());
        viewModel.languageModeProperty().addListener(
                (observable, oldValue, newValue) -> preferences.setLanguageMode(newValue));
        viewModel.themeProperty().addListener((observable, oldValue, newValue) -> {
            preferences.setTheme(newValue);
            applyTheme(newValue, systemColorScheme());
        });
        ChangeListener<ColorScheme> colorSchemeListener = (observable, oldValue, newValue) ->
                applyTheme(viewModel.themeProperty().get(), newValue);
        Platform.getPreferences().colorSchemeProperty().addListener(colorSchemeListener);
        applyTheme(viewModel.themeProperty().get(), systemColorScheme());
        AppShellView view = new AppShellView();
        AppShellController controller = new AppShellController(view, viewModel, dependencies.recordingServices(),
                dependencies.liveJvmServices(), dependencies.heapDumpServices(), i18n,
                new VirtualThreadRecordingOpenExecutor());
        controller.initialize();
        return new AppShell(view.root, i18n.text("app.title"), controller::close, controller);
    }

    private static void applyTheme(AppTheme theme, ColorScheme colorScheme) {
        AppTheme selected = theme == null ? AppTheme.SYSTEM : theme;
        Application.setUserAgentStylesheet(selected.resolve(colorScheme).userAgentStylesheet());
    }

    private static ColorScheme systemColorScheme() {
        ColorScheme colorScheme = Platform.getPreferences().getColorScheme();
        return colorScheme == null ? ColorScheme.LIGHT : colorScheme;
    }
}
