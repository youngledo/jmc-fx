package com.youngledo.jmcfx.ui.shell;

import java.io.IOException;

import com.youngledo.jmcfx.domain.service.EventQueryService;
import com.youngledo.jmcfx.domain.service.RecordingRepository;
import com.youngledo.jmcfx.domain.service.RuleAnalysisService;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.preferences.AppPreferences;
import com.youngledo.jmcfx.ui.preferences.JavaAppPreferences;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;

/// Loads the JavaFX shell and injects service-backed view models.
///
/// This keeps FXML construction in the UI layer and avoids direct framework
/// setup in application startup code.
public class AppShellFactory {

    private final RecordingRepository recordingRepository;
    private final EventQueryService eventQueryService;
    private final RuleAnalysisService ruleAnalysisService;
    private final I18n i18n;
    private final AppPreferences preferences;

    public AppShellFactory(RecordingRepository recordingRepository, EventQueryService eventQueryService,
            RuleAnalysisService ruleAnalysisService) {
        this(recordingRepository, eventQueryService, ruleAnalysisService,
                new I18n(java.util.Locale.getDefault()), new JavaAppPreferences());
    }

    public AppShellFactory(RecordingRepository recordingRepository, EventQueryService eventQueryService,
            RuleAnalysisService ruleAnalysisService, I18n i18n) {
        this(recordingRepository, eventQueryService, ruleAnalysisService, i18n, new JavaAppPreferences());
    }

    AppShellFactory(RecordingRepository recordingRepository, EventQueryService eventQueryService,
            RuleAnalysisService ruleAnalysisService, I18n i18n, AppPreferences preferences) {
        this.recordingRepository = recordingRepository;
        this.eventQueryService = eventQueryService;
        this.ruleAnalysisService = ruleAnalysisService;
        this.i18n = i18n;
        this.preferences = preferences;
    }

    public AppShell create() {
        try {
            AppShellViewModel viewModel = new AppShellViewModel();
            viewModel.setLanguageMode(preferences.languageMode());
            i18n.setLanguageMode(viewModel.languageModeProperty().get());
            viewModel.languageModeProperty().addListener(
                    (observable, oldValue, newValue) -> preferences.setLanguageMode(newValue));
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/youngledo/jmcfx/ui/shell/app-shell.fxml"));
            loader.setControllerFactory(type -> controllerFor(type, viewModel));
            BorderPane root = loader.load();
            AppShellController controller = loader.getController();
            return new AppShell(root, i18n.text("app.title"), controller::close);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load app shell", exception);
        }
    }

    Object controllerFor(Class<?> type, AppShellViewModel viewModel) {
        if (type == AppShellController.class) {
            return new AppShellController(viewModel, recordingRepository, eventQueryService, ruleAnalysisService, i18n);
        }
        throw new IllegalArgumentException("Unsupported controller: " + type.getName());
    }
}
