package com.youngledo.jmcfx.app;

import java.util.Locale;

import atlantafx.base.theme.PrimerLight;
import com.youngledo.jmcfx.adapter.jmc.JmcEventQueryService;
import com.youngledo.jmcfx.adapter.jmc.JmcExceptionService;
import com.youngledo.jmcfx.adapter.jmc.JmcProfilingService;
import com.youngledo.jmcfx.adapter.jmc.JmcRecordingRepository;
import com.youngledo.jmcfx.adapter.jmc.JmcRuleAnalysisService;
import com.youngledo.jmcfx.adapter.jmc.JmcThreadService;
import com.youngledo.jmcfx.ui.shell.AppShell;
import com.youngledo.jmcfx.ui.shell.AppShellFactory;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/// JavaFX entry point for JMC FX.
///
/// Startup is limited to theme selection, service assembly, shell creation,
/// stylesheet ordering, and primary stage lifecycle.
public class JmcFxApplication extends Application {

    private AppShell shell;

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        Locale systemLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
        shell = new AppShellFactory(new JmcRecordingRepository(),
                new JmcEventQueryService(), new JmcRuleAnalysisService(),
                new JmcProfilingService(), new JmcExceptionService(),
                new JmcThreadService(),
                new com.youngledo.jmcfx.ui.i18n.I18n(systemLocale)).create();
        Scene scene = new Scene(shell.root(), 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        stage.titleProperty().bind(shell.titleBinding());
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        if (shell != null) {
            shell.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
