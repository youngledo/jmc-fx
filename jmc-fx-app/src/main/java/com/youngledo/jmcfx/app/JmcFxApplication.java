package com.youngledo.jmcfx.app;

import java.util.Locale;

import com.youngledo.jmcfx.adapter.jmc.JmcAdvancedJfrAnalysisService;
import com.youngledo.jmcfx.adapter.jmc.JmcDiagnosticCommandService;
import com.youngledo.jmcfx.adapter.jmc.JmcEnvironmentService;
import com.youngledo.jmcfx.adapter.jmc.JmcEventQueryService;
import com.youngledo.jmcfx.adapter.jmc.JmcJavaAppService;
import com.youngledo.jmcfx.adapter.jmc.JmcExceptionService;
import com.youngledo.jmcfx.adapter.jmc.JmcFileIOService;
import com.youngledo.jmcfx.adapter.jmc.JmcFlightRecordingService;
import com.youngledo.jmcfx.adapter.jmc.JmcHeapDumpAnalysisService;
import com.youngledo.jmcfx.adapter.jmc.JmcHeapService;
import com.youngledo.jmcfx.adapter.jmc.JmcAgentManagerService;
import com.youngledo.jmcfx.adapter.jmc.JmcJvmInternalsService;
import com.youngledo.jmcfx.adapter.jmc.JmcJfrMetadataService;
import com.youngledo.jmcfx.adapter.jmc.JmcJmxMonitoringService;
import com.youngledo.jmcfx.adapter.jmc.JmcJmxConnectionService;
import com.youngledo.jmcfx.adapter.jmc.JmcJvmDiscoveryService;
import com.youngledo.jmcfx.adapter.jmc.JmcJdpDiscoveryService;
import com.youngledo.jmcfx.adapter.jmc.JmcLeakSuspectsService;
import com.youngledo.jmcfx.adapter.jmc.JmcLiveMetricService;
import com.youngledo.jmcfx.adapter.jmc.JmcLockService;
import com.youngledo.jmcfx.adapter.jmc.JmcMBeanBrowserService;
import com.youngledo.jmcfx.adapter.jmc.JmcProfilingService;
import com.youngledo.jmcfx.adapter.jmc.JmcRecordingRepository;
import com.youngledo.jmcfx.adapter.jmc.JmcRuleAnalysisService;
import com.youngledo.jmcfx.adapter.jmc.JmcSocketIOService;
import com.youngledo.jmcfx.adapter.jmc.JmcThreadService;
import com.youngledo.jmcfx.adapter.jmc.JmcTlabService;
import com.youngledo.jmcfx.ui.preferences.JavaJmxMonitoringRepository;
import com.youngledo.jmcfx.ui.preferences.JavaSavedJvmTargetRepository;
import com.youngledo.jmcfx.ui.shell.AppShell;
import com.youngledo.jmcfx.ui.shell.AppShellFactory;
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
        JmcJmxConnectionService jmxConnectionService = new JmcJmxConnectionService();
        shell = new AppShellFactory(new JmcRecordingRepository(),
                new JmcEventQueryService(), new JmcRuleAnalysisService(),
                new JmcProfilingService(), new JmcExceptionService(),
                new JmcThreadService(),
                new JmcFileIOService(), new JmcSocketIOService(),
                new JmcLockService(),
                new JmcHeapService(), new JmcLeakSuspectsService(),
                new JmcTlabService(),
                new JmcJvmInternalsService(),
                new JmcEnvironmentService(),
                new JmcJavaAppService(),
                new JmcJvmDiscoveryService(),
                jmxConnectionService,
                new JmcFlightRecordingService(jmxConnectionService),
                new JmcMBeanBrowserService(jmxConnectionService),
                new JmcDiagnosticCommandService(jmxConnectionService),
                new JmcLiveMetricService(jmxConnectionService),
                new JmcAgentManagerService(jmxConnectionService),
                new JmcJmxMonitoringService(jmxConnectionService),
                new JavaJmxMonitoringRepository(),
                new JmcJfrMetadataService(),
                new JmcAdvancedJfrAnalysisService(),
                new JavaSavedJvmTargetRepository(),
                new JmcJdpDiscoveryService(),
                new JmcHeapDumpAnalysisService(),
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
