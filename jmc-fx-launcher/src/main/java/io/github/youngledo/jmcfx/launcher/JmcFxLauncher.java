package io.github.youngledo.jmcfx.launcher;

import java.util.Locale;

import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.preferences.JavaAppPreferences;
import io.github.youngledo.jmcfx.ui.shell.AppShell;
import io.github.youngledo.jmcfx.ui.shell.AppShellFactory;
import io.github.youngledo.jmcfx.ui.shell.AppShellFactoryDependencies;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// JavaFX entry point for JMC FX.
///
/// Startup is limited to theme selection, service assembly, shell creation,
/// stylesheet ordering, and primary stage lifecycle.
public class JmcFxLauncher extends Application {

    private static final String LEYDEN_TRAINING_PROPERTY = "jmcfx.leyden.training";
    private static final String APPLICATION_ICON = "/icons/jmc-fx-icon.png";
    private static final Logger LOGGER = LogManager.getLogger(JmcFxLauncher.class);

    private AppShell shell;

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) ->
                LOGGER.atError()
                        .withThrowable(exception)
                        .log("Uncaught exception on thread {}", thread.getName()));
        Locale systemLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
        JmcFxLauncherServices services = new JmcFxLauncherServicesFactory().create();
        AppShellFactoryDependencies dependencies = new AppShellFactoryDependencies(services.recording(), services.liveJvm(),
                services.heapDump(), new I18n(systemLocale), new JavaAppPreferences());
        shell = new AppShellFactory(dependencies).create();
        Scene scene = new Scene(shell.root(), 1280, 800);
        scene.getStylesheets().add(AppShell.class.getResource("/css/app.css").toExternalForm());
        stage.titleProperty().bind(shell.titleBinding());
        stage.getIcons().add(new Image(JmcFxLauncher.class.getResource(APPLICATION_ICON).toExternalForm()));
        stage.setScene(scene);
        stage.show();
        if (Boolean.getBoolean(LEYDEN_TRAINING_PROPERTY)) {
            Platform.runLater(Platform::exit);
        }
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
