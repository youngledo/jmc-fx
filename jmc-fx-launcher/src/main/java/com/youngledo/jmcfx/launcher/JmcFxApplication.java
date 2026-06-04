package com.youngledo.jmcfx.launcher;

import java.util.Locale;

import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.preferences.JavaAppPreferences;
import com.youngledo.jmcfx.ui.shell.AppShell;
import com.youngledo.jmcfx.ui.shell.AppShellFactory;
import com.youngledo.jmcfx.ui.shell.AppShellFactoryDependencies;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// JavaFX entry point for JMC FX.
///
/// Startup is limited to theme selection, service assembly, shell creation,
/// stylesheet ordering, and primary stage lifecycle.
public class JmcFxApplication extends Application {

    private static final Logger LOGGER = LogManager.getLogger(JmcFxApplication.class);

    private AppShell shell;

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) ->
                LOGGER.atError()
                        .withThrowable(exception)
                        .log("Uncaught exception on thread {}", thread.getName()));
        Locale systemLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
        JmcApplicationServices services = new JmcApplicationServicesFactory().create();
        AppShellFactoryDependencies dependencies = new AppShellFactoryDependencies(services.recording(), services.liveJvm(),
                services.heapDump(), new I18n(systemLocale), new JavaAppPreferences());
        shell = new AppShellFactory(dependencies).create();
        Scene scene = new Scene(shell.root(), 1280, 800);
        scene.getStylesheets().add(AppShell.class.getResource("/css/app.css").toExternalForm());
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
