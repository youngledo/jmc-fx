package com.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.youngledo.jmcfx.application.LiveJvmApplicationServices;
import com.youngledo.jmcfx.application.RecordingApplicationServices;
import com.youngledo.jmcfx.ui.util.DisplayFormats;
import com.youngledo.jmcfx.domain.model.EventTypeSelection;
import com.youngledo.jmcfx.domain.model.JdpJvmAdvertisement;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.JvmConnectionSource;
import com.youngledo.jmcfx.domain.model.SavedJvmTarget;
import com.youngledo.jmcfx.domain.model.StackTreeNode;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.ProfilingService;
import com.youngledo.jmcfx.domain.service.RuleAnalysisService;
import com.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import com.youngledo.jmcfx.ui.events.EventsPageController;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.i18n.LanguageMode;
import com.youngledo.jmcfx.ui.overview.OverviewViewModel;
import com.youngledo.jmcfx.ui.profiling.ProfilingPageController;
import com.youngledo.jmcfx.ui.rules.RuleResultsViewModel;
import com.youngledo.jmcfx.testsupport.FakeEventQueryService;
import com.youngledo.jmcfx.testsupport.FakeJdpDiscoveryService;
import com.youngledo.jmcfx.testsupport.FakeJmxConnectionService;
import com.youngledo.jmcfx.testsupport.FakeJmxMonitoringRepository;
import com.youngledo.jmcfx.testsupport.FakeJmxMonitoringService;
import com.youngledo.jmcfx.testsupport.FakeJvmDiscoveryService;
import com.youngledo.jmcfx.testsupport.FakeRecordingRepository;
import com.youngledo.jmcfx.testsupport.FakeRuleAnalysisService;
import com.youngledo.jmcfx.testsupport.FakeSavedJvmTargetRepository;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.Region;

class AppShellTest {

    @org.junit.jupiter.api.BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await(5, TimeUnit.SECONDS);
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized by another test class.
        }
    }

    @Test
    void closeDelegatesToCloseHandle() throws Exception {
        AtomicInteger closeCount = new AtomicInteger();
        AppShell shell = new AppShell(null, null, closeCount::incrementAndGet);

        shell.close();

        assertEquals(1, closeCount.get());
    }

    @Test
    void appShellViewBuildsRootShellAndWorkspaceRegions() {
        AppShellView view = new AppShellView();

        assertEquals("BorderPane", view.root.getClass().getSimpleName());
        assertTrue(view.root.getStyleClass().contains("app-shell"));
        assertEquals("StackPane", view.workspaceStack.getClass().getSimpleName());
        assertNotNull(view.workspacePanes.homePane);
        assertNotNull(view.workspacePanes.settingsPane);
        assertNotNull(view.workspacePanes.jvmsPaneHost);
    }

    @Test
    void shellRootFrameIsSplitOutOfAppShellView() throws Exception {
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String rootView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/ShellRootView.java"));

        assertTrue(appShellView.contains("final ShellRootView shell = new ShellRootView(workspacePanes.stack);"));
        assertFalse(appShellView.contains("final BorderPane root = new BorderPane();"));
        assertFalse(appShellView.contains("final AppSidebar sidebar = new AppSidebar();"));
        assertFalse(appShellView.contains("final TabPane recordingTabs = new TabPane();"));
        assertFalse(appShellView.contains("final ProgressBar progressBar = new ProgressBar(0);"));
        assertFalse(appShellView.contains("private void configureShell()"));

        assertTrue(rootView.contains("final class ShellRootView"));
        assertTrue(rootView.contains("final BorderPane root = new BorderPane();"));
        assertTrue(rootView.contains("final AppSidebar sidebar = new AppSidebar();"));
        assertTrue(rootView.contains("final TabPane recordingTabs = new TabPane();"));
        assertTrue(rootView.contains("final ProgressBar progressBar = new ProgressBar(0);"));
        assertTrue(rootView.contains("ShellRootView(StackPane workspaceStack)"));
        assertTrue(rootView.contains("void configure(StackPane workspaceStack)"));
        assertTrue(rootView.contains("styles(root, \"enterprise-shell\", \"app-shell\")"));
        assertTrue(rootView.contains("workspaceShell.getChildren().setAll(recordingTabs, workspaceStack)"));
    }

    @Test
    void globalPageViewConstructionIsSplitOutOfAppShellView() throws Exception {
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String homeView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/HomePaneView.java"));
        String settingsView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/SettingsPaneView.java"));
        String homeController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/HomePaneController.java"));
        String settingsController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/SettingsPaneController.java"));

        assertTrue(appShellView.contains("final HomePaneView home = new HomePaneView();"));
        assertTrue(appShellView.contains("final SettingsPaneView settings = new SettingsPaneView();"));
        assertFalse(appShellView.contains("final Button homeOpenRecordingButton"));
        assertFalse(appShellView.contains("final Label homeKickerLabel"));
        assertFalse(appShellView.contains("private void configureHome()"));
        assertFalse(appShellView.contains("final Label settingsLanguageLabel"));
        assertFalse(appShellView.contains("final ToggleGroup languageToggleGroup"));
        assertFalse(appShellView.contains("private void configureSettings()"));

        assertTrue(homeView.contains("final class HomePaneView"));
        assertTrue(homeView.contains("final VBox pane = new VBox();"));
        assertTrue(homeView.contains("final Button openRecordingButton = new Button();"));
        assertTrue(homeView.contains("styles(pane, \"welcome-pane\")"));

        assertTrue(settingsView.contains("final class SettingsPaneView"));
        assertTrue(settingsView.contains("final VBox pane = new VBox();"));
        assertTrue(settingsView.contains("final ToggleGroup languageToggleGroup = new ToggleGroup();"));
        assertTrue(settingsView.contains("pane.setSpacing(36)"));

        assertTrue(homeController.contains("private final HomePaneView view;"));
        assertTrue(settingsController.contains("private final SettingsPaneView view;"));
    }

    @Test
    void workspacePaneHostsAreSplitOutOfAppShellView() throws Exception {
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String workspacePanes = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/ShellWorkspacePanes.java"));
        String visibility = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/WorkspacePaneVisibilityController.java"));
        String liveJvm = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/ShellLiveJvmWorkspaceController.java"));

        assertTrue(appShellView.contains("final ShellWorkspacePanes workspacePanes = new ShellWorkspacePanes(home.pane, settings.pane);"));
        assertFalse(appShellView.contains("final StackPane workspaceStack = new StackPane();"));
        assertFalse(appShellView.contains("final VBox overviewPane = new VBox();"));
        assertFalse(appShellView.contains("final VBox jvmsPaneHost = new VBox();"));
        assertFalse(appShellView.contains("workspaceStack.getChildren().setAll("));

        assertTrue(workspacePanes.contains("final class ShellWorkspacePanes"));
        assertTrue(workspacePanes.contains("final StackPane stack = new StackPane();"));
        assertTrue(workspacePanes.contains("final VBox overviewPane = new VBox();"));
        assertTrue(workspacePanes.contains("final VBox jvmsPaneHost = new VBox();"));
        assertTrue(workspacePanes.contains("void install()"));
        assertTrue(workspacePanes.contains("stack.getChildren().setAll("));

        assertTrue(visibility.contains("ShellWorkspacePanes panes = view.workspacePanes;"));
        assertTrue(visibility.contains("bind(panes.homePane, \"home\")"));
        assertTrue(visibility.contains("bind(panes.settingsPane, \"settings\")"));
        assertTrue(liveJvm.contains("view.workspacePanes.jvmsPaneHost.getChildren().setAll("));
    }

    @Test
    void appShellViewDoesNotMirrorWorkspacePaneHosts() throws Exception {
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));

        assertFalse(appShellView.contains("import javafx.scene.layout.VBox;"));
        for (String paneField : List.of("homePane", "overviewPane", "eventsPane", "analysisPane", "metadataPane",
                "advancedJfrPane", "heapDumpAnalysisPane", "jvmsPaneHost", "javaApplicationPane",
                "jvmInternalsPane", "environmentPane", "profilingPane", "exceptionsPane", "threadsPane",
                "fileioPane", "socketioPane", "locksPane", "threadHistogramPane", "securityPane",
                "nativeLibrariesPane", "threadDumpsPane", "heapPane", "leaksPane", "tlabPane",
                "jvmInfoPane", "gcConfigPane", "gcSummaryPane", "gcDetailsPane", "g1GcPane",
                "javaFxEventsPane", "compilationsPane", "codeCachePane", "classLoadingPane",
                "vmOperationsPane", "processesPane", "envVarsPane", "sysPropsPane", "recordingInfoPane",
                "agentsPane", "constantPoolsPane", "settingsPane")) {
            assertFalse(appShellView.contains("final VBox " + paneField + " = workspacePanes." + paneField + ";"),
                    () -> paneField + " should be accessed through workspacePanes");
        }

        assertTrue(appShellView.contains("new OverviewPaneView(workspacePanes.overviewPane)"));
        assertTrue(appShellView.contains("new RecordingOverviewPaneView(workspacePanes.javaApplicationPane,"));
        assertTrue(appShellView.contains("new JavaApplicationDataPaneView(workspacePanes.exceptionsPane,"));
        assertTrue(appShellView.contains("new EnvironmentPaneView(workspacePanes.processesPane,"));
    }


    @Test
    void appShellFactoryUsesDirectJavaViewAssembly() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellFactory.java"));

        assertFalse(source.contains("new FXMLLoader("));
        assertFalse(source.contains("controllerFor("));
        assertTrue(source.contains("new AppShellView()"));
        assertTrue(source.contains("new AppShellController("));
    }

    @Test
    void productionUiNoLongerUsesFxml() throws Exception {
        List<String> matches = java.nio.file.Files.walk(java.nio.file.Path.of("src/main"))
                .filter(java.nio.file.Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java")
                        || path.toString().endsWith(".fxml"))
                .flatMap(path -> {
                    try {
                        String text = java.nio.file.Files.readString(path);
                        if (text.contains("FXMLLoader") || text.contains("@FXML")
                                || path.toString().endsWith(".fxml")) {
                            return java.util.stream.Stream.of(path.toString());
                        }
                        return java.util.stream.Stream.empty();
                    } catch (java.io.IOException exception) {
                        throw new java.io.UncheckedIOException(exception);
                    }
                })
                .sorted()
                .toList();

        assertEquals(List.of(), matches);
    }

    @Test
    void recordingWorkspaceLifecycleIsSplitOutOfShellController() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String runtime = shellRuntimeSource();
        String factory = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/RecordingWorkspaceFactory.java"));
        String loader = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/RecordingSectionLoader.java"));
        String prepared = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/PreparedRecordingWorkspace.java"));

        assertTrue(runtime.contains("private final RecordingSectionLoader recordingSectionLoader;"));
        assertTrue(runtime.contains("RecordingWorkspaceFactory recordingWorkspaceFactory = new RecordingWorkspaceFactory("));
        assertFalse(shell.contains("record PreparedRecordingWorkspace("),
                "The prepared recording workspace data carrier belongs in its own source file");
        assertFalse(shell.contains("private void loadWorkspaceSectionNow("),
                "Per-section load dispatch belongs in RecordingSectionLoader");
        assertFalse(shell.contains("private void loadIfPresent("),
                "Feature-specific nullable load helpers belong in RecordingSectionLoader");

        assertTrue(factory.contains("final class RecordingWorkspaceFactory"));
        assertTrue(factory.contains("private final RecordingApplicationServices services;"));
        assertTrue(factory.contains("private final OpenRecordingWorkspaceUseCase openRecordingWorkspace;"));
        assertTrue(factory.contains("RecordingWorkspaceFactory(RecordingApplicationServices services, I18n i18n)"));
        assertTrue(factory.contains("PreparedRecordingWorkspace prepare(Path path)"));
        assertTrue(factory.contains("RecordingWorkspacePlan plan = openRecordingWorkspace.open(path)"));
        assertFalse(factory.contains("services.recordingRepository().open(path)"),
                "Opening the recording repository belongs in the application use case");

        assertTrue(loader.contains("final class RecordingSectionLoader"));
        assertTrue(loader.contains("void load(RecordingWorkspace workspace, String sectionId)"));
        assertTrue(loader.contains("private void loadWorkspaceSectionNow("));

        assertTrue(prepared.contains("record PreparedRecordingWorkspace("));
    }

    @Test
    void shellDependenciesAreGroupedIntoFocusedBundles() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String factory = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellFactory.java"));
        String recordingServices = java.nio.file.Files.readString(
                java.nio.file.Path.of("../jmc-fx-application/src/main/java/com/youngledo/jmcfx/application/RecordingApplicationServices.java"));
        String liveJvmServices = java.nio.file.Files.readString(
                java.nio.file.Path.of("../jmc-fx-application/src/main/java/com/youngledo/jmcfx/application/LiveJvmApplicationServices.java"));
        String heapDumpServices = java.nio.file.Files.readString(
                java.nio.file.Path.of("../jmc-fx-application/src/main/java/com/youngledo/jmcfx/application/HeapDumpApplicationServices.java"));

        assertTrue(shell.contains("AppShellController(AppShellView view, AppShellViewModel viewModel, RecordingApplicationServices recordingServices,"));
        assertTrue(shell.contains("LiveJvmApplicationServices liveJvmServices, HeapDumpApplicationServices heapDumpServices, I18n i18n,"));
        assertFalse(shell.contains("private final RecordingApplicationServices recordingServices;"));
        assertFalse(shell.contains("private final LiveJvmApplicationServices liveJvmServices;"));
        assertFalse(shell.contains("private final HeapDumpApplicationServices heapDumpServices;"));
        assertFalse(shell.contains("private final RecordingRepository recordingRepository;"));
        assertFalse(shell.contains("private final JvmDiscoveryService jvmDiscoveryService;"));
        assertFalse(shell.contains("private final HeapDumpAnalysisService heapDumpAnalysisService;"));

        assertTrue(factory.contains("private final AppShellFactoryDependencies dependencies;"));
        assertFalse(factory.contains("private final RecordingApplicationServices recordingServices;"));
        assertFalse(factory.contains("private final LiveJvmApplicationServices liveJvmServices;"));
        assertFalse(factory.contains("private final HeapDumpApplicationServices heapDumpServices;"));

        assertTrue(recordingServices.contains("record RecordingApplicationServices("));
        assertTrue(recordingServices.contains("RecordingRepository recordingRepository"));
        assertTrue(recordingServices.contains("AdvancedJfrAnalysisService advancedJfrAnalysisService"));
        assertTrue(liveJvmServices.contains("record LiveJvmApplicationServices("));
        assertTrue(liveJvmServices.contains("JvmDiscoveryService jvmDiscoveryService"));
        assertTrue(liveJvmServices.contains("JmxMonitoringRepository jmxMonitoringRepository"));
        assertTrue(heapDumpServices.contains("record HeapDumpApplicationServices("));
        assertTrue(heapDumpServices.contains("HeapDumpAnalysisService heapDumpAnalysisService"));
    }

    @Test
    void appShellFactoryUsesBundledDependencyEntryPoint() throws Exception {
        String factory = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellFactory.java"));
        String dependencies = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellFactoryDependencies.java"));
        String app = java.nio.file.Files.readString(
                java.nio.file.Path.of("../jmc-fx-launcher/src/main/java/com/youngledo/jmcfx/launcher/JmcFxLauncher.java"));

        assertTrue(factory.contains("private final AppShellFactoryDependencies dependencies;"));
        assertTrue(factory.contains("public AppShellFactory(AppShellFactoryDependencies dependencies)"));
        assertFalse(factory.contains("private final RecordingApplicationServices recordingServices;"));
        assertFalse(factory.contains("private final LiveJvmApplicationServices liveJvmServices;"));
        assertFalse(factory.contains("private final HeapDumpApplicationServices heapDumpServices;"));
        assertFalse(factory.contains("private final I18n i18n;"));
        assertFalse(factory.contains("private final AppPreferences preferences;"));

        assertTrue(dependencies.contains("public record AppShellFactoryDependencies("));
        assertTrue(dependencies.contains("RecordingApplicationServices recordingServices"));
        assertTrue(dependencies.contains("LiveJvmApplicationServices liveJvmServices"));
        assertTrue(dependencies.contains("HeapDumpApplicationServices heapDumpServices"));
        assertTrue(dependencies.contains("I18n i18n"));
        assertTrue(dependencies.contains("AppPreferences preferences"));

        assertFalse(factory.contains("new AppShellFactoryDependencies("));
        assertTrue(factory.contains("dependencies.preferences()"));
        assertTrue(factory.contains("dependencies.i18n()"));
        assertTrue(factory.contains("dependencies.recordingServices()"));
        assertTrue(app.contains("new AppShellFactoryDependencies("));
        assertFalse(app.contains("new AppShellFactory(new JmcRecordingRepository(),"));
    }

    @Test
    void appShellFactoryHasSingleBundledEntryPoint() throws Exception {
        String factory = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellFactory.java"));

        assertTrue(factory.contains("public AppShellFactory(AppShellFactoryDependencies dependencies)"));
        assertFalse(factory.contains("AppShellFactory(RecordingRepository"));
        assertFalse(factory.contains("private AppShellFactory(RecordingRepository"));
        assertFalse(factory.contains("import com.youngledo.jmcfx.domain.service."));
        assertFalse(factory.contains("new RecordingApplicationServices("));
        assertFalse(factory.contains("new LiveJvmApplicationServices("));
        assertFalse(factory.contains("new HeapDumpApplicationServices("));
        assertFalse(factory.contains("new JavaAppPreferences()"));
    }

    @Test
    void workspaceTabsAreSplitOutOfShellController() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String runtime = shellRuntimeSource();
        String tabs = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/WorkspaceTabsController.java"));

        assertTrue(runtime.contains("private WorkspaceTabsController workspaceTabsController;"));
        assertTrue(runtime.contains("workspaceTabsController = new WorkspaceTabsController(view.recordingTabs, viewModel);"));
        assertFalse(shell.contains("private boolean updatingRecordingTabs;"));
        assertFalse(shell.contains("private TabPane recordingTabs;"));
        assertFalse(shell.contains("private void configureRecordingTabs("));
        assertFalse(shell.contains("private void rebuildRecordingTabs("));
        assertFalse(shell.contains("private Tab toWorkspaceTab("));
        assertFalse(shell.contains("private void selectWorkspaceTab("));

        assertTrue(tabs.contains("final class WorkspaceTabsController"));
        assertTrue(tabs.contains("private final TabPane tabs;"));
        assertTrue(tabs.contains("private boolean updatingTabs;"));
        assertTrue(tabs.contains("void configure()"));
        assertTrue(tabs.contains("void select(RecordingWorkspace recordingWorkspace, HeapDumpWorkspace heapDumpWorkspace,"));
        assertTrue(tabs.contains("static String tabTitleFor(RecordingWorkspace workspace)"));
        assertTrue(tabs.contains("static boolean shouldShowWorkspaceTabs(int recordingWorkspaceCount, int heapDumpWorkspaceCount,"));
    }

    @Test
    void exportMenuInstallationIsSplitOutOfShellController() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String runtime = shellRuntimeSource();
        String installer = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/ExportMenuInstaller.java"));
        String registry = shellPageControllerRegistrySource();

        assertTrue(runtime.contains("private ExportMenuInstaller exportMenuInstaller;"));
        assertTrue(runtime.contains("exportMenuInstaller = new ExportMenuInstaller(view.root, viewModel, i18n);"));
        assertTrue(runtime.contains("pageControllerRegistry.installExportMenus(exportMenuInstaller)"));
        assertTrue(registry.contains("installer.install(analysisPageController.table())"));
        assertFalse(shell.contains("private void attachExportMenu("));
        assertFalse(shell.contains("CsvExport.export(table, target.toPath())"));
        assertFalse(shell.contains("new MenuItem(i18n.get(\"context.exportCsv\"))"));

        assertTrue(installer.contains("final class ExportMenuInstaller"));
        assertTrue(installer.contains("void install(TableView<?> table)"));
        assertTrue(installer.contains("new MenuItem(i18n.get(\"context.exportCsv\"))"));
        assertTrue(installer.contains("chooser.setTitle(i18n.get(\"fileChooser.saveCsv.title\"))"));
        assertTrue(installer.contains("CsvExport.export(table, target.toPath())"));
        assertTrue(installer.contains("viewModel.showStatus(i18n.format(\"status.exported\", target.getName()))"));
    }

    @Test
    void workspaceOpenFlowIsSplitOutOfShellController() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String runtime = shellRuntimeSource();
        String coordinator = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/WorkspaceOpenCoordinator.java"));

        assertTrue(runtime.contains("private WorkspaceOpenCoordinator workspaceOpenCoordinator;"));
        assertTrue(runtime.contains("this.workspaceOpenCoordinator = new WorkspaceOpenCoordinator("));
        assertTrue(runtime.contains("workspaceOpenCoordinator::openRecording"));
        assertTrue(runtime.contains("workspaceOpenCoordinator::showOpenHeapDumpChooser"));
        assertTrue(runtime.contains("workspaceOpenCoordinator.openRecordingInBackground(path)"));
        assertFalse(shell.contains("private void showOpenRecordingChooser("));
        assertFalse(shell.contains("private void openRecordingInBackground("));
        assertFalse(shell.contains("private void openHeapDumpInBackground("));
        assertFalse(shell.contains("private boolean selectExistingRecordingWorkspace("));
        assertFalse(shell.contains("private void showOpenRecordingFailure("));
        assertFalse(shell.contains("PreparedRecordingWorkspace prepareRecordingWorkspace(Path path)"));

        assertTrue(coordinator.contains("final class WorkspaceOpenCoordinator"));
        assertTrue(coordinator.contains("void openRecording()"));
        assertTrue(coordinator.contains("void showOpenHeapDumpChooser()"));
        assertTrue(coordinator.contains("void openRecordingInBackground(Path path)"));
        assertTrue(coordinator.contains("PreparedRecordingWorkspace prepareRecordingWorkspace(Path path)"));
        assertTrue(coordinator.contains("private void openHeapDumpInBackground(Path path)"));
        assertTrue(coordinator.contains("selectExistingRecordingWorkspace(path)"));
        assertTrue(coordinator.contains("selectExistingHeapDumpWorkspace(path)"));
        assertTrue(coordinator.contains("recordingOpenExecutor.execute("));
        assertTrue(coordinator.contains("HeapDumpAnalysisViewModel nextViewModel"));
    }

    @Test
    void recordingWorkspaceAttachHandlingIsSplitOutOfShellController() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String runtime = shellRuntimeSource();
        String attacher = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/ShellRecordingWorkspaceAttacher.java"));
        String coordinator = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/WorkspaceOpenCoordinator.java"));

        assertTrue(runtime.contains("ShellRecordingWorkspaceAttacher recordingWorkspaceAttacher ="));
        assertTrue(runtime.contains("recordingWorkspaceAttacher::attach"));
        assertFalse(shell.contains("private void attachPreparedRecordingWorkspace("));
        assertFalse(shell.contains("prepared.overview().showRecording("));
        assertFalse(shell.contains("viewModel.openRecording(prepared.recording()"));
        assertFalse(shell.contains("workspaceOpenCoordinator.finishRecordingOpen();"));

        assertTrue(attacher.contains("final class ShellRecordingWorkspaceAttacher"));
        assertTrue(attacher.contains("void attach(PreparedRecordingWorkspace prepared)"));
        assertTrue(attacher.contains("prepared.overview().showRecording("));
        assertTrue(attacher.contains("pageControllerRegistry.formatRecordingDetails(prepared.recording())"));
        assertTrue(attacher.contains("viewModel.openRecording(prepared.recording()"));
        assertTrue(attacher.contains("viewModel.showStatus(i18n.format(\"status.openedRecording\""));

        assertTrue(coordinator.contains("finishRecordingOpen();"));
        assertTrue(coordinator.contains("recordingWorkspaceConsumer.accept(preparedWorkspace);"));
    }

    @Test
    void shellFactoryUsesBundleBasedControllerEntryPoint() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String factory = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellFactory.java"));

        assertTrue(shell.contains("AppShellController(AppShellView view, AppShellViewModel viewModel,"));
        assertTrue(shell.contains("RecordingApplicationServices recordingServices"));
        assertTrue(shell.contains("LiveJvmApplicationServices liveJvmServices"));
        assertTrue(shell.contains("HeapDumpApplicationServices heapDumpServices"));
        assertFalse(shell.contains(
                "AppShellController(AppShellView view, AppShellViewModel viewModel, RecordingRepository recordingRepository"));
        assertFalse(shell.contains("public AppShellController(AppShellViewModel viewModel"));
        assertFalse(shell.contains("private AppShellController(AppShellViewModel viewModel"));
        assertFalse(shell.contains("import com.youngledo.jmcfx.domain.service.RecordingRepository;"));
        assertFalse(shell.contains("import com.youngledo.jmcfx.domain.service.JmxConnectionService;"));
        assertFalse(shell.contains("import com.youngledo.jmcfx.domain.service.HeapDumpAnalysisService;"));

        assertTrue(factory.contains("new AppShellController(view, viewModel, dependencies.recordingServices(),"));
        assertTrue(factory.contains("dependencies.liveJvmServices(), dependencies.heapDumpServices(), i18n,"));
        assertFalse(factory.contains("new AppShellController(view, viewModel, recordingServices.recordingRepository()"));
    }

    @Test
    void backgroundWorkHandlingIsSplitOutOfShellController() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String runtime = shellRuntimeSource();
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/ShellBackgroundWorkController.java"));

        assertTrue(runtime.contains("private final ShellBackgroundWorkController backgroundWorkController;"));
        assertTrue(runtime.contains("backgroundWorkController::setVisible"));
        assertTrue(runtime.contains("backgroundWorkController::onFxThread"));
        assertTrue(runtime.contains("backgroundWorkController.configure()"));
        assertFalse(shell.contains("private ProgressBar progressBar;"));
        assertFalse(shell.contains("private void setBackgroundWorkVisible("));
        assertFalse(shell.contains("private void onFxThread(Runnable runnable)"));
        assertFalse(shell.contains("progressBar.setProgress("));
        assertFalse(shell.contains("Platform.isFxApplicationThread()"));

        assertTrue(controller.contains("final class ShellBackgroundWorkController"));
        assertTrue(controller.contains("ShellBackgroundWorkController(ProgressBar progressBar)"));
        assertTrue(controller.contains("void configure()"));
        assertTrue(controller.contains("void setVisible(boolean visible)"));
        assertTrue(controller.contains("void onFxThread(Runnable runnable)"));
        assertTrue(controller.contains("ProgressBar.INDETERMINATE_PROGRESS"));
        assertTrue(controller.contains("Platform.runLater(runnable)"));
    }

    @Test
    void workspacePaneVisibilityIsSplitOutOfShellController() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String runtime = shellRuntimeSource();
        String visibility = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/WorkspacePaneVisibilityController.java"));

        assertTrue(runtime.contains("private WorkspacePaneVisibilityController workspacePaneVisibilityController;"));
        assertTrue(runtime.contains("workspacePaneVisibilityController = new WorkspacePaneVisibilityController(view, viewModel);"));
        assertTrue(runtime.contains("workspacePaneVisibilityController.configure();"));
        assertFalse(shell.contains("homePane.visibleProperty().bind("));
        assertFalse(shell.contains("settingsPane.managedProperty().bind("));
        assertFalse(shell.contains("jvmsPaneHost.visibleProperty().bind("));

        assertTrue(visibility.contains("final class WorkspacePaneVisibilityController"));
        assertTrue(visibility.contains("void configure()"));
        assertTrue(visibility.contains("ShellWorkspacePanes panes = view.workspacePanes;"));
        assertTrue(visibility.contains("bind(panes.homePane, \"home\")"));
        assertTrue(visibility.contains("bind(panes.jvmsPaneHost, \"jvms\")"));
        assertTrue(visibility.contains("bind(panes.settingsPane, \"settings\")"));
        assertTrue(visibility.contains("pane.managedProperty().bind(pane.visibleProperty())"));
    }

    @Test
    void workspaceSelectionIsSplitOutOfShellController() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String runtime = shellRuntimeSource();
        String selection = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/WorkspaceSelectionController.java"));

        assertTrue(runtime.contains("private WorkspaceSelectionController workspaceSelectionController;"));
        assertTrue(runtime.contains("workspaceSelectionController = new WorkspaceSelectionController("));
        assertTrue(runtime.contains("workspaceSelectionController.configure();"));
        assertFalse(shell.contains("private WorkspaceSelectionController workspaceSelectionController()"));
        assertFalse(shell.contains("workspaceSelectionController().loadSelectedWorkspaceSection();"));
        assertFalse(shell.contains("workspaceSelectionController().loadWorkspaceSection(workspace, sectionId);"));
        assertFalse(shell.contains("void preloadRecordingWorkspace(RecordingWorkspace workspace)"));
        assertFalse(shell.contains("void loadWorkspaceSection(RecordingWorkspace workspace, String sectionId)"));
        assertFalse(shell.contains("static List<String> preloadedWorkspaceSections()"));
        assertFalse(shell.contains("private RecordingWorkspace loadedWorkspace;"));
        assertFalse(shell.contains("private void bindWorkspaceSelection("));
        assertFalse(shell.contains("private void showWorkspace("));
        assertFalse(shell.contains("private void showHeapDumpWorkspace("));
        assertFalse(shell.contains("private void showLiveJvmWorkspace("));

        assertTrue(selection.contains("final class WorkspaceSelectionController"));
        assertTrue(selection.contains("private RecordingWorkspace loadedWorkspace;"));
        assertTrue(selection.contains("void configure()"));
        assertTrue(selection.contains("private void showWorkspace(RecordingWorkspace workspace)"));
        assertTrue(selection.contains("private void showHeapDumpWorkspace(HeapDumpWorkspace workspace)"));
        assertTrue(selection.contains("private void showLiveJvmWorkspace(LiveJvmWorkspace workspace)"));
        assertTrue(selection.contains("void loadSelectedWorkspaceSection()"));
        assertTrue(selection.contains("void preloadRecordingWorkspace(RecordingWorkspace workspace)"));
        assertTrue(selection.contains("static java.util.List<String> preloadedWorkspaceSections()"));
        assertTrue(selection.contains("void loadWorkspaceSection(RecordingWorkspace workspace, String sectionId)"));
    }

    @Test
    void shellLifecycleIsSplitOutOfShellController() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String runtime = shellRuntimeSource();
        String lifecycle = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/ShellLifecycleController.java"));

        assertTrue(runtime.contains("private ShellLifecycleController shellLifecycleController;"));
        assertFalse(shell.contains("private ShellLifecycleController shellLifecycleController()"));
        assertTrue(runtime.contains("shellLifecycleController.close();"));
        assertFalse(shell.contains("List.copyOf(viewModel.recordingWorkspacesProperty()).forEach(viewModel::closeWorkspace);"));
        assertFalse(shell.contains("recordingOpenExecutor.close();"));

        assertTrue(lifecycle.contains("final class ShellLifecycleController"));
        assertTrue(lifecycle.contains("void close()"));
        assertTrue(lifecycle.contains("jvmsPaneController.close();"));
        assertTrue(lifecycle.contains("List.copyOf(viewModel.recordingWorkspacesProperty()).forEach(viewModel::closeWorkspace);"));
        assertTrue(lifecycle.contains("List.copyOf(viewModel.heapDumpWorkspacesProperty()).forEach(viewModel::closeHeapDumpWorkspace);"));
        assertTrue(lifecycle.contains("jvmBrowserViewModel.close();"));
        assertTrue(lifecycle.contains("heapDumpAnalysisViewModel.close();"));
        assertTrue(lifecycle.contains("recordingOpenExecutor.close();"));
    }

    @Test
    void shellControllerDoesNotMirrorPagePaneFields() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));

        for (String paneField : List.of("homePane", "overviewPane", "eventsPane", "analysisPane", "metadataPane",
                "advancedJfrPane", "heapDumpAnalysisPane", "javaApplicationPane", "jvmInternalsPane",
                "environmentPane", "profilingPane", "exceptionsPane", "threadsPane", "fileioPane", "socketioPane",
                "locksPane", "threadHistogramPane", "securityPane", "nativeLibrariesPane", "threadDumpsPane",
                "heapPane", "leaksPane", "tlabPane", "jvmInfoPane", "gcConfigPane", "gcSummaryPane",
                "gcDetailsPane", "g1GcPane", "javaFxEventsPane", "compilationsPane", "codeCachePane",
                "classLoadingPane", "vmOperationsPane", "processesPane", "envVarsPane", "sysPropsPane",
                "recordingInfoPane", "agentsPane", "constantPoolsPane", "settingsPane")) {
            assertFalse(shell.contains("private VBox " + paneField + ";"),
                    () -> paneField + " should stay owned by AppShellView");
            assertFalse(shell.contains("this." + paneField + " = view." + paneField + ";"),
                    () -> paneField + " should not be mirrored in AppShellController");
        }

        assertFalse(shell.contains("private VBox jvmsPaneHost;"),
                "Live JVM host ownership belongs in ShellLiveJvmWorkspaceController");
    }

    @Test
    void shellControllerDoesNotRetainViewMirrorsOrConstructorOnlyCollaborators() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String runtime = shellRuntimeSource();

        assertFalse(shell.contains("private BorderPane root;"));
        assertFalse(shell.contains("private AppSidebar sidebar;"));
        assertFalse(shell.contains("private final RecordingOpenExecutor recordingOpenExecutor;"));
        assertFalse(shell.contains("private final RecordingWorkspaceFactory recordingWorkspaceFactory;"));
        assertFalse(shell.contains("private final ShellRecordingWorkspaceAttacher recordingWorkspaceAttacher;"));
        assertFalse(shell.contains("assignViewFields("));
        assertFalse(shell.contains("this.root = view.root;"));
        assertFalse(shell.contains("this.sidebar = view.sidebar;"));
        assertTrue(shell.contains("return view.root;"));
        assertTrue(runtime.contains("view.sidebar.bind(viewModel);"));
        assertTrue(runtime.contains("RecordingWorkspaceFactory recordingWorkspaceFactory = new RecordingWorkspaceFactory("));
        assertTrue(runtime.contains("ShellRecordingWorkspaceAttacher recordingWorkspaceAttacher ="));
        assertTrue(runtime.contains("new ShellRecordingWorkspaceAttacher(viewModel, pageControllerRegistry, i18n)"));
    }

    @Test
    void appShellControllerIsThinFacadeOverRuntimeController() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        java.nio.file.Path runtimePath = java.nio.file.Path.of(
                "src/main/java/com/youngledo/jmcfx/ui/shell/ShellRuntimeController.java");

        assertTrue(java.nio.file.Files.exists(runtimePath));
        String runtime = java.nio.file.Files.readString(runtimePath);

        assertTrue(shell.contains("private final ShellRuntimeController runtimeController;"));
        assertTrue(shell.contains("this.runtimeController = new ShellRuntimeController("));
        assertTrue(shell.contains("runtimeController.initialize();"));
        assertTrue(shell.contains("runtimeController.close();"));
        assertTrue(shell.contains("runtimeController.openRecordingInBackground(path);"));
        assertFalse(shell.contains("new HomePaneController("));
        assertFalse(shell.contains("new SettingsPaneController("));
        assertFalse(shell.contains("new WorkspaceTabsController("));
        assertFalse(shell.contains("new WorkspacePaneVisibilityController("));
        assertFalse(shell.contains("new WorkspaceSelectionController("));
        assertFalse(shell.contains("new ExportMenuInstaller("));
        assertFalse(shell.contains("new ShellPageControllerRegistry("));
        assertFalse(shell.contains("new ShellLiveJvmWorkspaceController("));
        assertFalse(shell.contains("new ShellHeapDumpWorkspaceController("));
        assertFalse(shell.contains("new WorkspaceOpenCoordinator("));
        assertFalse(shell.contains("new RecordingSectionLoader("));
        assertFalse(shell.contains("new ShellBackgroundWorkController("));
        assertFalse(shell.contains("private HomePaneController"));
        assertFalse(shell.contains("private ShellPageControllerRegistry"));
        assertFalse(shell.contains("private WorkspaceTabsController"));
        assertFalse(shell.contains("private WorkspaceOpenCoordinator"));
        assertFalse(shell.contains("private ShellLifecycleController"));

        assertTrue(runtime.contains("final class ShellRuntimeController"));
        assertTrue(runtime.contains("new HomePaneController("));
        assertTrue(runtime.contains("new SettingsPaneController("));
        assertTrue(runtime.contains("new WorkspaceTabsController("));
        assertTrue(runtime.contains("new WorkspacePaneVisibilityController("));
        assertTrue(runtime.contains("new WorkspaceSelectionController("));
        assertTrue(runtime.contains("new WorkspaceOpenCoordinator("));
        assertTrue(runtime.contains("void openRecordingInBackground(Path path)"));
        assertTrue(runtime.contains("void close()"));
    }

    @Test
    void shellControllerDelegatesPageControllerRegistry() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String runtime = shellRuntimeSource();
        java.nio.file.Path registryPath = java.nio.file.Path.of(
                "src/main/java/com/youngledo/jmcfx/ui/shell/ShellPageControllerRegistry.java");
        assertTrue(java.nio.file.Files.exists(registryPath), "ShellPageControllerRegistry should own page controllers");
        String registry = java.nio.file.Files.readString(registryPath);

        assertTrue(runtime.contains("private ShellPageControllerRegistry pageControllerRegistry;"));
        assertTrue(runtime.contains("pageControllerRegistry = new ShellPageControllerRegistry("));
        assertTrue(runtime.contains("pageControllerRegistry.configure();"));
        assertTrue(runtime.contains("pageControllerRegistry.workspacePageControllers()"));
        assertTrue(runtime.contains("pageControllerRegistry.installExportMenus(exportMenuInstaller)"));

        for (String field : List.of("overviewPageController", "analysisPageController", "eventsPageController",
                "metadataPageController", "advancedJfrPageController", "heapDumpAnalysisPageController",
                "profilingPageController", "exceptionsPageController", "threadsPageController",
                "javaApplicationDataPagesController", "fileIoPageController", "socketIoPageController",
                "locksPageController", "heapPageController", "leakSuspectsPageController", "tlabPageController",
                "jvmInternalsPagesController", "g1GcPageController", "javaFxEventsPageController",
                "environmentPagesController")) {
            assertFalse(shell.contains("private " + field), () -> field + " should be owned by the registry");
        }
        assertFalse(shell.contains("private void attachExportMenus()"),
                "Page export table enumeration belongs in the page controller registry");

        assertTrue(registry.contains("final class ShellPageControllerRegistry"));
        assertTrue(registry.contains("WorkspacePageControllers workspacePageControllers()"));
        assertTrue(registry.contains("void installExportMenus(ExportMenuInstaller installer)"));
    }

    @Test
    void liveJvmServicesCarrySavedTargetsAndJdpDiscoveryDependencies() {
        FakeSavedJvmTargetRepository savedTargets = new FakeSavedJvmTargetRepository();
        FakeJdpDiscoveryService jdpDiscovery = new FakeJdpDiscoveryService();
        LiveJvmApplicationServices services = new LiveJvmApplicationServices(null, null, null, null, null, null, null, null, null,
                savedTargets, jdpDiscovery);

        assertEquals(savedTargets, services.savedTargetRepository());
        assertEquals(jdpDiscovery, services.jdpDiscoveryService());
    }

    @Test
    void liveJvmServicesCarryMonitoringDependencies() {
        FakeJmxMonitoringService monitoringService = new FakeJmxMonitoringService();
        FakeJmxMonitoringRepository monitoringRepository = new FakeJmxMonitoringRepository();
        LiveJvmApplicationServices services = new LiveJvmApplicationServices(null, null, null, null, null, null, null,
                monitoringService, monitoringRepository, null, null);

        assertEquals(monitoringService, services.jmxMonitoringService());
        assertEquals(monitoringRepository, services.jmxMonitoringRepository());
    }

    @Test
    void shellControllerDoesNotExposeLiveJvmServiceAccessors() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String liveJvmController = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/youngledo/jmcfx/ui/shell/ShellLiveJvmWorkspaceController.java"));

        for (String method : List.of("savedTargetRepository()", "jdpDiscoveryService()",
                "jmxMonitoringService()", "jmxMonitoringRepository()")) {
            assertFalse(shell.contains(method), () -> method + " should not be exposed by AppShellController");
            assertTrue(liveJvmController.contains(method), () -> method + " should stay with the Live JVM controller");
        }
    }

    @Test
    void i18nDefaultsToEnglishUiLocale() {
        I18n i18n = new I18n(java.util.Locale.SIMPLIFIED_CHINESE);

        assertEquals(java.util.Locale.ENGLISH, i18n.localeProperty().get());
    }

    @Test
    void shellControllerDoesNotExposeI18nAccessor() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));

        assertFalse(shell.contains("I18n i18n()"));
    }

    @Test
    void sourceLabelsCoverEveryJvmConnectionSource() {
        ResourceBundle english = ResourceBundle.getBundle("com.youngledo.jmcfx.ui.i18n.messages", Locale.ENGLISH);
        ResourceBundle chinese = ResourceBundle.getBundle("com.youngledo.jmcfx.ui.i18n.messages", Locale.SIMPLIFIED_CHINESE);

        for (JvmConnectionSource source : JvmConnectionSource.values()) {
            String key = "jvms.source." + source.name().toLowerCase(Locale.ROOT);

            assertTrue(english.containsKey(key), () -> "Missing English source label: " + key);
            assertTrue(chinese.containsKey(key), () -> "Missing Simplified Chinese source label: " + key);
        }
    }

    @Test
    void callGraphZoomKeepsViewportAnchorStable() {
        assertEquals(0.5, ProfilingPageController.scrollValueAfterZoom(
                0.5, 1000, 2000, 200, 100), 0.000001);
        assertEquals(0, ProfilingPageController.scrollValueAfterZoom(
                0, 1000, 2000, 200, 0), 0.000001);
        assertEquals(1, ProfilingPageController.scrollValueAfterZoom(
                1, 1000, 2000, 200, 200), 0.000001);
        assertEquals(0, ProfilingPageController.scrollValueAfterZoom(
                0.5, 1000, 150, 200, 100), 0.000001);
    }

    @Test
    void shellControllerDoesNotExposePageOwnedConstantsOrUtilityWrappers() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String events = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/events/EventsPageController.java"));
        String profiling = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/profiling/ProfilingPageController.java"));

        for (String constant : List.of("MIN_EVENT_TYPES_WIDTH", "DEFAULT_EVENT_TYPES_WIDTH",
                "MAX_EVENT_TYPES_WIDTH", "DEFAULT_EVENT_TYPES_DIVIDER_POSITION")) {
            assertFalse(shell.contains(constant), () -> constant + " should not be owned by AppShellController");
            assertTrue(events.contains(constant), () -> constant + " should stay with EventsPageController");
        }
        assertFalse(shell.contains("scrollValueAfterZoom("));
        assertTrue(profiling.contains("scrollValueAfterZoom("));
    }

    @Test
    void workspaceTabsIncludeSingletonLiveJvmWorkspace() {
        assertFalse(WorkspaceTabsController.shouldShowWorkspaceTabs(0, 0, false));
        assertTrue(WorkspaceTabsController.shouldShowWorkspaceTabs(0, 0, true));
        assertTrue(WorkspaceTabsController.shouldShowWorkspaceTabs(1, 0, false));
        assertTrue(WorkspaceTabsController.shouldShowWorkspaceTabs(0, 1, false));
        assertEquals("JVM", WorkspaceTabsController.tabTitleFor(new LiveJvmWorkspace("JVM")));
    }

    @Test
    void uiPomIncludesMaterialIconPack() throws Exception {
        Document document = pom("pom.xml");

        assertEquals(true, elements(document).values().stream()
                .anyMatch(element -> "artifactId".equals(element.getTagName())
                        && "ikonli-material2-pack".equals(element.getTextContent())));
    }


    @Test
    void appShellDoesNotOwnLiveJvmControlFieldsAfterExtraction() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String liveJvmWorkspace = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/ShellLiveJvmWorkspaceController.java"));

        assertFalse(source.contains("private VBox jvmsPaneHost;"));
        assertFalse(source.contains("private LiveJvmPaneController jvmsPaneController;"));
        assertFalse(source.contains("new LiveJvmPaneController()"));
        assertTrue(liveJvmWorkspace.contains("private LiveJvmPaneController jvmsPaneController;"));
        assertTrue(liveJvmWorkspace.contains("new LiveJvmPaneController()"));
        assertFalse(source.contains("private TableView<JvmConnection> jvmsTable;"));
        assertFalse(source.contains("private Button jvmsAddNotificationSubscriptionButton;"));
        assertFalse(source.contains("private void configureJmxMonitoring()"));
        assertFalse(source.contains("private void bindJmcAgentManager()"));
    }

    @Test
    void shellControllerDelegatesLiveJvmWorkspaceController() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String runtime = shellRuntimeSource();
        java.nio.file.Path controllerPath = java.nio.file.Path.of(
                "src/main/java/com/youngledo/jmcfx/ui/shell/ShellLiveJvmWorkspaceController.java");
        assertTrue(java.nio.file.Files.exists(controllerPath),
                "ShellLiveJvmWorkspaceController should own Live JVM workspace setup");
        String liveJvmWorkspace = java.nio.file.Files.readString(controllerPath);

        assertTrue(runtime.contains("private ShellLiveJvmWorkspaceController liveJvmWorkspaceController;"));
        assertTrue(runtime.contains("liveJvmWorkspaceController = new ShellLiveJvmWorkspaceController("));
        assertTrue(runtime.contains("liveJvmWorkspaceController.configure();"));
        assertFalse(shell.contains("private VBox jvmsPaneHost;"));
        assertFalse(shell.contains("private JvmBrowserViewModel jvmBrowserViewModel;"));
        assertFalse(shell.contains("private LiveJvmPaneController jvmsPaneController;"));
        assertFalse(shell.contains("new LiveJvmPaneController()"));
        assertFalse(shell.contains("jvmsPaneHost.getChildren().setAll("));
        assertFalse(shell.contains("\"jvms\".equals(newValue) && jvmsPaneController != null"));

        assertTrue(liveJvmWorkspace.contains("final class ShellLiveJvmWorkspaceController"));
        assertTrue(liveJvmWorkspace.contains("new JvmBrowserViewModel("));
        assertTrue(liveJvmWorkspace.contains("new LiveJvmPaneController()"));
        assertTrue(liveJvmWorkspace.contains("view.workspacePanes.jvmsPaneHost.getChildren().setAll("));
        assertTrue(liveJvmWorkspace.contains("\"jvms\".equals(newValue) && jvmsPaneController != null"));
    }

    @Test
    void shellControllerDelegatesHeapDumpWorkspaceController() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String runtime = shellRuntimeSource();
        java.nio.file.Path controllerPath = java.nio.file.Path.of(
                "src/main/java/com/youngledo/jmcfx/ui/shell/ShellHeapDumpWorkspaceController.java");
        assertTrue(java.nio.file.Files.exists(controllerPath),
                "ShellHeapDumpWorkspaceController should own heap dump analysis lifecycle setup");
        String heapDumpWorkspace = java.nio.file.Files.readString(controllerPath);
        String heapDumpViewModel = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/heapdump/HeapDumpAnalysisViewModel.java"));

        assertTrue(runtime.contains("private ShellHeapDumpWorkspaceController heapDumpWorkspaceController;"));
        assertTrue(runtime.contains("heapDumpWorkspaceController = new ShellHeapDumpWorkspaceController("));
        assertTrue(runtime.contains("heapDumpWorkspaceController.configure();"));
        assertFalse(shell.contains("private HeapDumpAnalysisViewModel heapDumpAnalysisViewModel;"));
        assertFalse(shell.contains("new HeapDumpAnalysisViewModel("));
        assertFalse(shell.contains("new VirtualThreadHeapDumpAnalysisExecutor()"));
        assertFalse(shell.contains("setHeapDumpAnalysisViewModel(heapDumpAnalysisViewModel)"));

        assertTrue(heapDumpWorkspace.contains("final class ShellHeapDumpWorkspaceController"));
        assertTrue(heapDumpWorkspace.contains("private HeapDumpAnalysisViewModel heapDumpAnalysisViewModel;"));
        assertTrue(heapDumpWorkspace.contains("new HeapDumpAnalysisViewModel(new AnalyzeHeapDumpUseCase(services),"));
        assertTrue(heapDumpWorkspace.contains("new VirtualThreadHeapDumpAnalysisExecutor()"));
        assertTrue(heapDumpWorkspace.contains("lifecycleController.setHeapDumpAnalysisViewModel(heapDumpAnalysisViewModel)"));
        assertTrue(heapDumpViewModel.contains("private final AnalyzeHeapDumpUseCase analyzeHeapDump;"));
        assertFalse(heapDumpViewModel.contains("HeapDumpAnalysisService"),
                "Heap dump analysis service calls belong in the application use case");
    }

    @Test
    void shellControllerDoesNotRetainLiveJvmHelperRemnants() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String liveJvm = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java"));
        String events = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/events/EventsPageController.java"));
        String settings = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/SettingsPaneController.java"));

        for (String helper : List.of("useFormattedIntegerCells", "localizedTablePlaceholder", "localizedColumn",
                "bindLocalizedText", "emptyTablePlaceholder", "formatEventTime(", "formatEventTimeForDisplay",
                "saveRecordingInitialFileName", "languageModeDisplayName", "canDisconnectJvm")) {
            assertFalse(shell.contains(helper), () -> helper + " should not remain in AppShellController");
        }
        assertFalse(shell.contains("liveJvmOverviewRefreshTimeline"));
        assertFalse(shell.contains("JfrMetadataViewModel"));
        assertFalse(shell.contains("AdvancedJfrViewModel"));

        assertTrue(liveJvm.contains("private static Region emptyTablePlaceholder()"));
        assertTrue(liveJvm.contains("private static String saveRecordingInitialFileName("));
        assertTrue(liveJvm.contains("private static boolean canDisconnectJvm("));
        assertTrue(liveJvm.contains("private static String formatEventTimeForDisplay("));
        assertTrue(events.contains("static String formatEventTimeForDisplay("));
        assertTrue(settings.contains("view.languageFollowSystemRadio.textProperty().bind(i18n.text(\"settings.language.followSystem\"))"));
        assertTrue(settings.contains("view.languageEnglishRadio.textProperty().bind(i18n.text(\"settings.language.english\"))"));
        assertTrue(settings.contains("view.languageChineseRadio.textProperty().bind(i18n.text(\"settings.language.chineseSimplified\"))"));
    }

    @Test
    void appShellDelegatesAnalysisPageToDomainPackagedController() throws Exception {
        java.nio.file.Path controllerPath = java.nio.file.Path.of(
                "src/main/java/com/youngledo/jmcfx/ui/analysis/AnalysisPageController.java");
        java.nio.file.Path viewPath = java.nio.file.Path.of(
                "src/main/java/com/youngledo/jmcfx/ui/analysis/AnalysisPageView.java");
        assertTrue(java.nio.file.Files.exists(controllerPath));
        assertTrue(java.nio.file.Files.exists(viewPath));

        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String registry = shellPageControllerRegistrySource();
        String selection = workspaceSelectionSource();
        String controller = java.nio.file.Files.readString(controllerPath);
        String pageView = java.nio.file.Files.readString(viewPath);

        assertTrue(registry.contains("private AnalysisPageController analysisPageController;"));
        assertTrue(registry.contains("new AnalysisPageController("));
        assertTrue(registry.contains("view.analysisPage()"));
        assertTrue(selection.contains("pages.analysisPageController().bind("));
        assertFalse(shell.contains("private TableView<RuleResult> analysisTable;"));
        assertFalse(shell.contains("private Label analysisDetailExplanationCaption;"));
        assertFalse(shell.contains("private void configureAnalysisTable()"));
        assertFalse(shell.contains("private void bindAnalysis("));
        assertFalse(shell.contains("private void showAnalysisDetail("));
        assertFalse(shell.contains("private void openAnalysisRelatedPage("));

        assertTrue(controller.contains("package com.youngledo.jmcfx.ui.analysis;"));
        assertTrue(controller.contains("class AnalysisPageController"));
        assertTrue(controller.contains("TableColumn<RuleResult, Severity>"));
        assertTrue(controller.contains("new AnalysisSeverityCell<>()"));
        assertTrue(controller.contains("analysis.column.severity"));
        assertTrue(controller.contains("analysis.filter.search"));
        assertTrue(controller.contains("analysis.detail.recommendation"));
        assertTrue(controller.contains("analysisPlaceholder("));
        assertTrue(controller.contains("relatedPageNavigator.accept(detail.relatedPageId())"));
        assertTrue(pageView.contains("public record AnalysisPageView("));
        assertTrue(pageView.contains("TableView<RuleResult> table"));
    }

    @Test
    void analysisPaneNodeOwnershipIsSplitOutOfAppShellView() throws Exception {
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String analysisPaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/analysis/AnalysisPaneView.java"));

        assertTrue(appShellView.contains("final AnalysisPaneView analysis = new AnalysisPaneView(workspacePanes.analysisPane);"));
        assertTrue(appShellView.contains("AnalysisPageView analysisPage()"));
        assertTrue(appShellView.contains("return analysis.view();"));
        assertFalse(appShellView.contains("final Label analysisTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final TextField analysisSearchField = new TextField();"));
        assertFalse(appShellView.contains("final Spinner<Integer> analysisMinimumScoreSpinner = new Spinner<>();"));
        assertFalse(appShellView.contains("final CheckBox analysisShowOkCheckBox = new CheckBox();"));
        assertFalse(appShellView.contains("final TableView<RuleResult> analysisTable = denseTable();"));
        assertFalse(appShellView.contains("final TextArea analysisDetailExplanationArea = textArea();"));
        assertFalse(appShellView.contains("private void configureAnalysis()"));

        assertTrue(analysisPaneView.contains("package com.youngledo.jmcfx.ui.analysis;"));
        assertTrue(analysisPaneView.contains("public final class AnalysisPaneView"));
        assertTrue(analysisPaneView.contains("private final TableView<RuleResult> table = denseTable();"));
        assertTrue(analysisPaneView.contains("private final TextArea detailExplanationArea = textArea();"));
        assertTrue(analysisPaneView.contains("public AnalysisPaneView(VBox pane)"));
        assertTrue(analysisPaneView.contains("public AnalysisPageView view()"));
        assertTrue(analysisPaneView.contains("styles(pane, \"split-table-detail-page\")"));
        assertTrue(analysisPaneView.contains("pane.getChildren().setAll(titleLabel, filterBar, split);"));
    }

    @Test
    void metadataPaneNodeOwnershipIsSplitOutOfAppShellView() throws Exception {
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String metadataPaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/metadata/MetadataPaneView.java"));

        assertTrue(appShellView.contains("final MetadataPaneView metadata = new MetadataPaneView(workspacePanes.metadataPane);"));
        assertTrue(appShellView.contains("MetadataPageView metadataPage()"));
        assertTrue(appShellView.contains("return metadata.view();"));
        assertFalse(appShellView.contains("final Label metadataTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final Label metadataSummaryLabel = new Label();"));
        assertFalse(appShellView.contains("final TableView<JfrMetadataEventType> metadataEventTypesTable = denseTable();"));
        assertFalse(appShellView.contains("final Label metadataDetailTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final TextArea metadataDetailArea = textArea();"));
        assertFalse(appShellView.contains("private void configureMetadata()"));

        assertTrue(metadataPaneView.contains("package com.youngledo.jmcfx.ui.metadata;"));
        assertTrue(metadataPaneView.contains("public final class MetadataPaneView"));
        assertTrue(metadataPaneView.contains("private final TableView<JfrMetadataEventType> eventTypesTable = denseTable();"));
        assertTrue(metadataPaneView.contains("private final TextArea detailArea = textArea();"));
        assertTrue(metadataPaneView.contains("public MetadataPaneView(VBox pane)"));
        assertTrue(metadataPaneView.contains("public MetadataPageView view()"));
        assertTrue(metadataPaneView.contains("styles(pane, \"page\", \"split-table-detail-page\")"));
        assertTrue(metadataPaneView.contains("pane.getChildren().setAll(header, split);"));
    }

    @Test
    void advancedJfrPaneNodeOwnershipIsSplitOutOfAppShellView() throws Exception {
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String advancedJfrPaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/advanced/AdvancedJfrPaneView.java"));

        assertTrue(appShellView.contains("final AdvancedJfrPaneView advancedJfr = new AdvancedJfrPaneView(workspacePanes.advancedJfrPane);"));
        assertTrue(appShellView.contains("AdvancedJfrPageView advancedJfrPage()"));
        assertTrue(appShellView.contains("return advancedJfr.view();"));
        assertFalse(appShellView.contains("final Label advancedJfrTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final TabPane advancedJfrTabs = new TabPane();"));
        assertFalse(appShellView.contains("final Tab advancedJfrHeatmapTab = tab();"));
        assertFalse(appShellView.contains("final VBox advancedJfrHeatmapContainer = new VBox();"));
        assertFalse(appShellView.contains("final TableView<MemoryIssue> advancedJfrMemoryTable = denseTable();"));
        assertFalse(appShellView.contains("final TextArea advancedJfrMemoryDetailArea = textArea();"));
        assertFalse(appShellView.contains("private void configureAdvancedJfr()"));

        assertTrue(advancedJfrPaneView.contains("package com.youngledo.jmcfx.ui.advanced;"));
        assertTrue(advancedJfrPaneView.contains("public final class AdvancedJfrPaneView"));
        assertTrue(advancedJfrPaneView.contains("private final TabPane tabs = new TabPane();"));
        assertTrue(advancedJfrPaneView.contains("private final VBox heatmapContainer = new VBox();"));
        assertTrue(advancedJfrPaneView.contains("private final TableView<MemoryIssue> memoryTable = denseTable();"));
        assertTrue(advancedJfrPaneView.contains("private final TextArea memoryDetailArea = textArea();"));
        assertTrue(advancedJfrPaneView.contains("public AdvancedJfrPaneView(VBox pane)"));
        assertTrue(advancedJfrPaneView.contains("public AdvancedJfrPageView view()"));
        assertTrue(advancedJfrPaneView.contains("tabs.getTabs().setAll(heatmapTab, memoryTab);"));
        assertTrue(advancedJfrPaneView.contains("pane.getChildren().setAll(titleLabel, summaryLabel, tabs);"));
    }

    @Test
    void heapDumpAnalysisPaneNodeOwnershipIsSplitOutOfAppShellView() throws Exception {
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String heapDumpPaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/heapdump/HeapDumpAnalysisPaneView.java"));

        assertTrue(appShellView.contains("final HeapDumpAnalysisPaneView heapDumpAnalysis = new HeapDumpAnalysisPaneView(workspacePanes.heapDumpAnalysisPane);"));
        assertTrue(appShellView.contains("HeapDumpAnalysisPageView heapDumpAnalysisPage()"));
        assertTrue(appShellView.contains("return heapDumpAnalysis.view();"));
        assertFalse(appShellView.contains("final Label heapDumpAnalysisTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final TableView<HeapDumpIssue> heapDumpIssuesTable = denseTable();"));
        assertFalse(appShellView.contains("final TabPane heapDumpDetailsTabs = new TabPane();"));
        assertFalse(appShellView.contains("final Tab heapDumpIssueDetailTab = tab();"));
        assertFalse(appShellView.contains("final TextArea heapDumpIssueDetailArea = textArea();"));
        assertFalse(appShellView.contains("final TextArea heapDumpTextReportArea = textArea();"));
        assertFalse(appShellView.contains("private void configureHeapDump()"));

        assertTrue(heapDumpPaneView.contains("package com.youngledo.jmcfx.ui.heapdump;"));
        assertTrue(heapDumpPaneView.contains("public final class HeapDumpAnalysisPaneView"));
        assertTrue(heapDumpPaneView.contains("private final TableView<HeapDumpIssue> issuesTable = denseTable();"));
        assertTrue(heapDumpPaneView.contains("private final TabPane detailsTabs = new TabPane();"));
        assertTrue(heapDumpPaneView.contains("private final TextArea issueDetailArea = textArea();"));
        assertTrue(heapDumpPaneView.contains("private final TextArea textReportArea = textArea();"));
        assertTrue(heapDumpPaneView.contains("public HeapDumpAnalysisPaneView(VBox pane)"));
        assertTrue(heapDumpPaneView.contains("public HeapDumpAnalysisPageView view()"));
        assertTrue(heapDumpPaneView.contains("styles(pane, \"page\", \"split-table-detail-page\", \"heap-dump-page\")"));
        assertTrue(heapDumpPaneView.contains("pane.getChildren().setAll(header, content);"));
    }

    @Test
    void profilingPaneNodeOwnershipIsSplitOutOfAppShellView() throws Exception {
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String profilingPaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/profiling/ProfilingPaneView.java"));

        assertTrue(appShellView.contains("final ProfilingPaneView profiling = new ProfilingPaneView(workspacePanes.profilingPane);"));
        assertTrue(appShellView.contains("ProfilingPageView profilingPage()"));
        assertTrue(appShellView.contains("return profiling.view();"));
        assertFalse(appShellView.contains("final Label profilingTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final TableView<HotMethod> profilingTable = denseTable();"));
        assertFalse(appShellView.contains("final TabPane profilingTreeTabs = new TabPane();"));
        assertFalse(appShellView.contains("final HBox profilingCallGraphToolbar = new HBox();"));
        assertFalse(appShellView.contains("final TableView<DependencyGraphEdge> profilingDependencyTable = denseTable();"));
        assertFalse(appShellView.contains("final TreeView<StackTreeNode> profilingCallersTree = new TreeView<>();"));
        assertFalse(appShellView.contains("private void configureProfiling()"));
        assertFalse(appShellView.contains("private void configureProfilingTab("));
        assertFalse(appShellView.contains("private void configureFlameTab("));

        assertTrue(profilingPaneView.contains("package com.youngledo.jmcfx.ui.profiling;"));
        assertTrue(profilingPaneView.contains("public final class ProfilingPaneView"));
        assertTrue(profilingPaneView.contains("private final TableView<HotMethod> hotMethodsTable = denseTable();"));
        assertTrue(profilingPaneView.contains("private final TabPane detailTabs = new TabPane();"));
        assertTrue(profilingPaneView.contains("private final HBox callGraphToolbar = new HBox();"));
        assertTrue(profilingPaneView.contains("private final TableView<DependencyGraphEdge> dependencyTable = denseTable();"));
        assertTrue(profilingPaneView.contains("private final TreeView<StackTreeNode> callersTree = new TreeView<>();"));
        assertTrue(profilingPaneView.contains("public ProfilingPaneView(VBox pane)"));
        assertTrue(profilingPaneView.contains("public ProfilingPageView view()"));
        assertTrue(profilingPaneView.contains("detailTabs.getTabs().setAll(callersFlameTab, calleesFlameTab,"));
        assertTrue(profilingPaneView.contains("profilingSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);"));
        assertTrue(profilingPaneView.contains("pane.getChildren().setAll(titleLabel, profilingSplit);"));
        assertTrue(profilingPaneView.contains("scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);"),
                "Deep profiling stacks should be vertically scrollable instead of clipped by the visible tab height");
    }

    @Test
    void recordingOverviewPaneNodeOwnershipIsSplitOutOfAppShellView() throws Exception {
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String overviewPaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/RecordingOverviewPaneView.java"));
        String overviewPagesView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/RecordingOverviewPagesView.java"));
        String overviewController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/RecordingOverviewPagesController.java"));
        String registry = shellPageControllerRegistrySource();

        assertTrue(appShellView.contains("final RecordingOverviewPaneView recordingOverviewPages ="));
        assertTrue(appShellView.contains("new RecordingOverviewPaneView(workspacePanes.javaApplicationPane,"));
        assertTrue(appShellView.contains("RecordingOverviewPagesView recordingOverviewPages()"));
        assertTrue(appShellView.contains("return recordingOverviewPages.view();"));
        assertFalse(appShellView.contains("final Label javaApplicationTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final Button javaApplicationProfilingButton = new Button();"));
        assertFalse(appShellView.contains("final Label jvmInternalsTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final Button jvmInternalsGcButton = new Button();"));
        assertFalse(appShellView.contains("final Label environmentTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final Button environmentProcessesButton = new Button();"));
        assertFalse(appShellView.contains("private void configureOverviewPages()"));
        assertFalse(appShellView.contains("private void configureActionOverview("));
        assertFalse(appShellView.contains("private VBox summaryAction("));

        assertTrue(overviewPaneView.contains("final class RecordingOverviewPaneView"));
        assertTrue(overviewPaneView.contains("RecordingOverviewPaneView(VBox javaApplicationPane,"));
        assertTrue(overviewPaneView.contains("RecordingOverviewPagesView view()"));
        assertTrue(overviewPaneView.contains("configureActionOverview(javaApplicationPane, javaApplicationTitleLabel"));
        assertTrue(overviewPaneView.contains("styles(pane, \"page\", \"overview-page\", pageClass)"));
        assertTrue(overviewPaneView.contains("styles(panel, \"summary-panel\")"));

        assertTrue(overviewPagesView.contains("record RecordingOverviewPagesView("));
        assertTrue(overviewPagesView.contains("Label javaApplicationTitleLabel"));
        assertTrue(overviewPagesView.contains("Button jvmInternalsGcButton"));
        assertTrue(overviewPagesView.contains("Button environmentProcessesButton"));

        assertTrue(overviewController.contains("private final RecordingOverviewPagesView view;"));
        assertTrue(overviewController.contains("RecordingOverviewPagesController(RecordingOverviewPagesView view,"));
        assertTrue(registry.contains("new RecordingOverviewPagesController(view.recordingOverviewPages(), viewModel, i18n)"));
    }

    @Test
    void javaApplicationDataPaneNodeOwnershipIsSplitOutOfAppShellView() throws Exception {
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String javaApplicationPaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/javaapp/JavaApplicationDataPaneView.java"));

        assertTrue(appShellView.contains("final JavaApplicationDataPaneView javaApplicationData ="));
        assertTrue(appShellView.contains("new JavaApplicationDataPaneView(workspacePanes.exceptionsPane,"));
        assertTrue(appShellView.contains("ExceptionsPageView exceptionsPage()"));
        assertTrue(appShellView.contains("return javaApplicationData.exceptionsPage();"));
        assertTrue(appShellView.contains("ThreadsPageView threadsPage()"));
        assertTrue(appShellView.contains("return javaApplicationData.threadsPage();"));
        assertTrue(appShellView.contains("JavaApplicationDataPagesView javaApplicationDataPages()"));
        assertTrue(appShellView.contains("return javaApplicationData.javaApplicationDataPages();"));

        assertFalse(appShellView.contains("final Label exceptionsTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final TableView<ExceptionSummary> exceptionsTable = denseTable();"));
        assertFalse(appShellView.contains("final Label threadsTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final TableView<ThreadSummary> threadsTable = denseTable();"));
        assertFalse(appShellView.contains("final Label threadHistogramTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final TableView<ThreadDumpEntry> threadDumpsTable = denseTable();"));
        assertFalse(appShellView.contains("configureTablePage(exceptionsPane, exceptionsTitleLabel"));
        assertFalse(appShellView.contains("configureTablePage(threadsPane, threadsTitleLabel"));
        assertFalse(appShellView.contains("configureTablePage(threadHistogramPane, threadHistogramTitleLabel"));
        assertFalse(appShellView.contains("configureTablePage(securityPane, securityTitleLabel"));
        assertFalse(appShellView.contains("configureTablePage(nativeLibrariesPane, nativeLibrariesTitleLabel"));
        assertFalse(appShellView.contains("configureTablePage(threadDumpsPane, threadDumpsTitleLabel"));

        assertTrue(javaApplicationPaneView.contains("package com.youngledo.jmcfx.ui.javaapp;"));
        assertTrue(javaApplicationPaneView.contains("public final class JavaApplicationDataPaneView"));
        assertTrue(javaApplicationPaneView.contains("public JavaApplicationDataPaneView(VBox exceptionsPane,"));
        assertTrue(javaApplicationPaneView.contains("public ExceptionsPageView exceptionsPage()"));
        assertTrue(javaApplicationPaneView.contains("public ThreadsPageView threadsPage()"));
        assertTrue(javaApplicationPaneView.contains("public JavaApplicationDataPagesView javaApplicationDataPages()"));
        assertTrue(javaApplicationPaneView.contains("configureTablePage(exceptionsPane, exceptionsTitleLabel"));
        assertTrue(javaApplicationPaneView.contains("configureTablePage(threadDumpsPane, threadDumpsTitleLabel"));
        assertTrue(javaApplicationPaneView.contains("styles(threadDumpTextArea, \"dump-text-area\")"));
    }

    @Test
    void ioAndLocksPaneNodeOwnershipIsSplitOutOfAppShellView() throws Exception {
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String fileIoPaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/fileio/FileIoPaneView.java"));
        String socketIoPaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/socketio/SocketIoPaneView.java"));
        String locksPaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/locks/LocksPaneView.java"));

        assertTrue(appShellView.contains("final FileIoPaneView fileIo = new FileIoPaneView(workspacePanes.fileioPane);"));
        assertTrue(appShellView.contains("final SocketIoPaneView socketIo = new SocketIoPaneView(workspacePanes.socketioPane);"));
        assertTrue(appShellView.contains("final LocksPaneView locks = new LocksPaneView(workspacePanes.locksPane);"));
        assertTrue(appShellView.contains("return fileIo.view();"));
        assertTrue(appShellView.contains("return socketIo.view();"));
        assertTrue(appShellView.contains("return locks.view();"));

        assertFalse(appShellView.contains("final Label fileioTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final TableView<FileIOHistogram> fileioHistogramTable = denseTable();"));
        assertFalse(appShellView.contains("final Label socketioTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final TableView<SocketIOEvent> socketioEventTable = denseTable();"));
        assertFalse(appShellView.contains("final Label locksTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final TableView<LockHistogram> locksByClassTable = denseTable();"));
        assertFalse(appShellView.contains("configureTablePage(fileioPane, fileioTitleLabel"));
        assertFalse(appShellView.contains("configureTablePage(socketioPane, socketioTitleLabel"));
        assertFalse(appShellView.contains("configureTablePage(locksPane, locksTitleLabel"));

        assertTrue(fileIoPaneView.contains("package com.youngledo.jmcfx.ui.fileio;"));
        assertTrue(fileIoPaneView.contains("public final class FileIoPaneView"));
        assertTrue(fileIoPaneView.contains("public FileIoPaneView(VBox pane)"));
        assertTrue(fileIoPaneView.contains("public FileIoPageView view()"));
        assertTrue(fileIoPaneView.contains("tab(timelineTab, timelineContainer);"));
        assertTrue(fileIoPaneView.contains("configureTablePage(pane, titleLabel, tabs);"));

        assertTrue(socketIoPaneView.contains("package com.youngledo.jmcfx.ui.socketio;"));
        assertTrue(socketIoPaneView.contains("public final class SocketIoPaneView"));
        assertTrue(socketIoPaneView.contains("public SocketIoPaneView(VBox pane)"));
        assertTrue(socketIoPaneView.contains("public SocketIoPageView view()"));
        assertTrue(socketIoPaneView.contains("styles(groupingBar, \"socketio-grouping-bar\")"));
        assertTrue(socketIoPaneView.contains("configureTablePage(pane, titleLabel, groupingBar, tabs);"));

        assertTrue(locksPaneView.contains("package com.youngledo.jmcfx.ui.locks;"));
        assertTrue(locksPaneView.contains("public final class LocksPaneView"));
        assertTrue(locksPaneView.contains("public LocksPaneView(VBox pane)"));
        assertTrue(locksPaneView.contains("public LocksPageView view()"));
        assertTrue(locksPaneView.contains("styles(groupingBar, \"locks-grouping-bar\")"));
        assertTrue(locksPaneView.contains("configureTablePage(pane, titleLabel, groupingBar, tabs);"));
    }

    @Test
    void memoryPaneNodeOwnershipIsSplitOutOfAppShellView() throws Exception {
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String heapPaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/heap/HeapPaneView.java"));
        String leaksPaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/leaks/LeakSuspectsPaneView.java"));
        String tlabPaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/tlab/TlabPaneView.java"));

        assertTrue(appShellView.contains("final HeapPaneView heap = new HeapPaneView(workspacePanes.heapPane);"));
        assertTrue(appShellView.contains("final LeakSuspectsPaneView leaks = new LeakSuspectsPaneView(workspacePanes.leaksPane);"));
        assertTrue(appShellView.contains("final TlabPaneView tlab = new TlabPaneView(workspacePanes.tlabPane);"));
        assertTrue(appShellView.contains("return heap.view();"));
        assertTrue(appShellView.contains("return leaks.view();"));
        assertTrue(appShellView.contains("return tlab.view();"));

        assertFalse(appShellView.contains("final Label heapTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final TableView<HeapClassHistogram> heapTable = denseTable();"));
        assertFalse(appShellView.contains("final Label leaksTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final TreeView<LeakReferenceNode> leaksReferenceTree = new TreeView<>();"));
        assertFalse(appShellView.contains("final Label tlabTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final TableView<TlabAllocation> tlabTable = denseTable();"));
        assertFalse(appShellView.contains("configureTablePage(heapPane, heapTitleLabel"));
        assertFalse(appShellView.contains("configureTablePage(leaksPane, leaksTitleLabel"));
        assertFalse(appShellView.contains("configureTablePage(tlabPane, tlabTitleLabel"));

        assertTrue(heapPaneView.contains("package com.youngledo.jmcfx.ui.heap;"));
        assertTrue(heapPaneView.contains("public final class HeapPaneView"));
        assertTrue(heapPaneView.contains("public HeapPaneView(VBox pane)"));
        assertTrue(heapPaneView.contains("public HeapPageView view()"));
        assertTrue(heapPaneView.contains("timelineContainer.getChildren().setAll(timelineChart);"));
        assertTrue(heapPaneView.contains("configureTablePage(pane, titleLabel, new SplitPane(table, timelineContainer));"));

        assertTrue(leaksPaneView.contains("package com.youngledo.jmcfx.ui.leaks;"));
        assertTrue(leaksPaneView.contains("public final class LeakSuspectsPaneView"));
        assertTrue(leaksPaneView.contains("public LeakSuspectsPaneView(VBox pane)"));
        assertTrue(leaksPaneView.contains("public LeakSuspectsPageView view()"));
        assertTrue(leaksPaneView.contains("configureTablePage(pane, titleLabel, new SplitPane(table, referenceTree));"));

        assertTrue(tlabPaneView.contains("package com.youngledo.jmcfx.ui.tlab;"));
        assertTrue(tlabPaneView.contains("public final class TlabPaneView"));
        assertTrue(tlabPaneView.contains("public TlabPaneView(VBox pane)"));
        assertTrue(tlabPaneView.contains("public TlabPageView view()"));
        assertTrue(tlabPaneView.contains("timelineContainer.getChildren().setAll(timelineChart);"));
        assertTrue(tlabPaneView.contains("configureTablePage(pane, titleLabel, new SplitPane(table, timelineContainer));"));
    }

    @Test
    void jvmInternalsPaneNodeOwnershipIsSplitOutOfAppShellView() throws Exception {
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String jvmPaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/jvm/JvmInternalsPaneView.java"));
        String g1PaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/gc/G1GcPaneView.java"));
        String javaFxPaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/jfx/JavaFxEventsPaneView.java"));

        assertTrue(appShellView.contains("final JvmInternalsPaneView jvmInternals ="));
        assertTrue(appShellView.contains("new JvmInternalsPaneView(workspacePanes.jvmInfoPane,"));
        assertTrue(appShellView.contains("final G1GcPaneView g1Gc = new G1GcPaneView(workspacePanes.g1GcPane);"));
        assertTrue(appShellView.contains("final JavaFxEventsPaneView javaFxEvents = new JavaFxEventsPaneView(workspacePanes.javaFxEventsPane);"));
        assertTrue(appShellView.contains("return jvmInternals.view();"));
        assertTrue(appShellView.contains("return g1Gc.view();"));
        assertTrue(appShellView.contains("return javaFxEvents.view();"));

        assertFalse(appShellView.contains("final TableView<JvmFlag> jvmFlagsTable = denseTable();"));
        assertFalse(appShellView.contains("final TableView<GcSummary> gcSummaryTable = denseTable();"));
        assertFalse(appShellView.contains("final TableView<CompilationEvent> compilationsTable = denseTable();"));
        assertFalse(appShellView.contains("final TableView<CodeCacheSweep> codeCacheSweepsTable = denseTable();"));
        assertFalse(appShellView.contains("final TableView<ClassloaderSummary> classLoadingHistogramTable = denseTable();"));
        assertFalse(appShellView.contains("final TableView<VmOperationSummary> vmOperationSummaryTable = denseTable();"));
        assertFalse(appShellView.contains("final Label g1GcTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final TableView<G1GcRegionState> g1GcRegionStatesTable = denseTable();"));
        assertFalse(appShellView.contains("final Label javaFxEventsTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final TableView<JavaFxPulsePhase> javaFxEventsPhaseTable = denseTable();"));
        assertFalse(appShellView.contains("private void configureGcPages()"));

        assertTrue(jvmPaneView.contains("package com.youngledo.jmcfx.ui.jvm;"));
        assertTrue(jvmPaneView.contains("public final class JvmInternalsPaneView"));
        assertTrue(jvmPaneView.contains("public JvmInternalsPaneView(VBox jvmInfoPane,"));
        assertTrue(jvmPaneView.contains("public JvmInternalsPagesView view()"));
        assertTrue(jvmPaneView.contains("configureTablePage(jvmInfoPane, jvmInfoTitleLabel"));
        assertTrue(jvmPaneView.contains("configureTablePage(gcDetailsPane, gcDetailsTitleLabel"));
        assertTrue(jvmPaneView.contains("configureTablePage(vmOperationsPane, vmOperationsTitleLabel"));

        assertTrue(g1PaneView.contains("package com.youngledo.jmcfx.ui.gc;"));
        assertTrue(g1PaneView.contains("public final class G1GcPaneView"));
        assertTrue(g1PaneView.contains("public G1GcPaneView(VBox pane)"));
        assertTrue(g1PaneView.contains("public G1GcPageView view()"));
        assertTrue(g1PaneView.contains("configureDetailPage(pane, titleLabel, summaryLabel"));

        assertTrue(javaFxPaneView.contains("package com.youngledo.jmcfx.ui.jfx;"));
        assertTrue(javaFxPaneView.contains("public final class JavaFxEventsPaneView"));
        assertTrue(javaFxPaneView.contains("public JavaFxEventsPaneView(VBox pane)"));
        assertTrue(javaFxPaneView.contains("public JavaFxEventsPageView view()"));
        assertTrue(javaFxPaneView.contains("configureDetailPage(pane, titleLabel, summaryLabel"));
    }

    @Test
    void environmentPaneNodeOwnershipIsSplitOutOfAppShellView() throws Exception {
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String environmentPaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/environment/EnvironmentPaneView.java"));

        assertTrue(appShellView.contains("final EnvironmentPaneView environment ="));
        assertTrue(appShellView.contains("new EnvironmentPaneView(workspacePanes.processesPane,"));
        assertTrue(appShellView.contains("EnvironmentPagesView environmentPages()"));
        assertTrue(appShellView.contains("return environment.view();"));

        assertFalse(appShellView.contains("final TableView<ProcessInfo> processesTable = denseTable();"));
        assertFalse(appShellView.contains("final TextField envVarsSearchField = new TextField();"));
        assertFalse(appShellView.contains("final TableView<SystemProperty> sysPropsTable = denseTable();"));
        assertFalse(appShellView.contains("final TabPane recordingInfoTabs = new TabPane();"));
        assertFalse(appShellView.contains("final TableView<AgentInfo> agentsTable = denseTable();"));
        assertFalse(appShellView.contains("final Label constantPoolsTitleLabel = new Label();"));
        assertFalse(appShellView.contains("private void configureEnvironmentPages()"));
        assertFalse(appShellView.contains("private void configureTablePage("));

        assertTrue(environmentPaneView.contains("package com.youngledo.jmcfx.ui.environment;"));
        assertTrue(environmentPaneView.contains("public final class EnvironmentPaneView"));
        assertTrue(environmentPaneView.contains("public EnvironmentPaneView(VBox processesPane,"));
        assertTrue(environmentPaneView.contains("public EnvironmentPagesView view()"));
        assertTrue(environmentPaneView.contains("configureTablePage(processesPane, processesTitleLabel, processesTable);"));
        assertTrue(environmentPaneView.contains("tab(recordingInfoRecordingsTab, recordingsTable);"));
        assertTrue(environmentPaneView.contains("configureTablePage(constantPoolsPane, constantPoolsTitleLabel, constantPoolsTable);"));
    }

    @Test
    void appShellDelegatesEventsPageToDomainPackagedController() throws Exception {
        java.nio.file.Path controllerPath = java.nio.file.Path.of(
                "src/main/java/com/youngledo/jmcfx/ui/events/EventsPageController.java");
        java.nio.file.Path viewPath = java.nio.file.Path.of(
                "src/main/java/com/youngledo/jmcfx/ui/events/EventsPageView.java");
        assertTrue(java.nio.file.Files.exists(controllerPath));
        assertTrue(java.nio.file.Files.exists(viewPath));

        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String registry = shellPageControllerRegistrySource();
        String selection = workspaceSelectionSource();
        String controller = java.nio.file.Files.readString(controllerPath);
        String pageView = java.nio.file.Files.readString(viewPath);

        assertTrue(registry.contains("private EventsPageController eventsPageController;"));
        assertTrue(registry.contains("new EventsPageController("));
        assertTrue(registry.contains("view.eventsPage()"));
        assertTrue(selection.contains("pages.eventsPageController().bind("));
        assertFalse(shell.contains("private TreeView<EventTypeNode> eventTypesTree;"));
        assertFalse(shell.contains("private TableView<EventRow> eventsTable;"));
        assertFalse(shell.contains("private MenuButton columnsButton;"));
        assertFalse(shell.contains("private void bindEvents()"));
        assertFalse(shell.contains("private void bindEventBrowser("));
        assertFalse(shell.contains("private void rebuildEventTypeTree()"));
        assertFalse(shell.contains("private void rebuildEventColumns()"));
        assertFalse(shell.contains("private void selectEventRow("));
        assertFalse(shell.contains("private void clearEventFilters()"));
        assertFalse(shell.contains("private List<EventFieldCondition> fieldConditions("));
        assertFalse(shell.contains("private void showEventDetails("));
        assertFalse(shell.contains("private void showSelectionProperties("));

        assertTrue(controller.contains("package com.youngledo.jmcfx.ui.events;"));
        assertTrue(controller.contains("public final class EventsPageController"));
        assertTrue(controller.contains("DEFAULT_EVENT_TYPES_DIVIDER_POSITION"));
        assertTrue(controller.contains("eventTypesTree().setCellFactory"));
        assertTrue(controller.contains("selectEventType(newValue)"));
        assertTrue(controller.contains("columnsButton().getItems().setAll"));
        assertTrue(controller.contains("new CheckMenuItem(field.label())"));
        assertTrue(controller.contains("new EventFilter("));
        assertTrue(controller.contains("fieldConditions("));
        assertTrue(controller.contains("showEventDetails("));
        assertTrue(controller.contains("showSelectionProperties("));
        assertTrue(controller.contains("shouldClearEventTypesTreeSelection"));
        assertTrue(pageView.contains("public record EventsPageView("));
        assertTrue(pageView.contains("TreeView<EventTypeNode> eventTypesTree"));
        assertTrue(pageView.contains("TableView<EventRow> eventsTable"));
    }

    @Test
    void eventsPaneNodeOwnershipIsSplitOutOfAppShellView() throws Exception {
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String eventsPaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/events/EventsPaneView.java"));

        assertTrue(appShellView.contains("final EventsPaneView events = new EventsPaneView(workspacePanes.eventsPane);"));
        assertTrue(appShellView.contains("EventsPageView eventsPage()"));
        assertTrue(appShellView.contains("return events.view();"));
        assertFalse(appShellView.contains("final Label eventsTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final TreeView<EventTypeNode> eventTypesTree = new TreeView<>();"));
        assertFalse(appShellView.contains("final TextField eventSearchField = new TextField();"));
        assertFalse(appShellView.contains("final MenuButton columnsButton = new MenuButton();"));
        assertFalse(appShellView.contains("final SplitPane eventsSplitPane = new SplitPane();"));
        assertFalse(appShellView.contains("final TableView<EventRow> eventsTable = denseTable();"));
        assertFalse(appShellView.contains("final TabPane eventDetailsTabs = new TabPane();"));
        assertFalse(appShellView.contains("final TableView<EventProperty> eventPropertiesTable = denseTable();"));
        assertFalse(appShellView.contains("private void configureEvents()"));

        assertTrue(eventsPaneView.contains("package com.youngledo.jmcfx.ui.events;"));
        assertTrue(eventsPaneView.contains("public final class EventsPaneView"));
        assertTrue(eventsPaneView.contains("private final TreeView<EventTypeNode> eventTypesTree = new TreeView<>();"));
        assertTrue(eventsPaneView.contains("private final TableView<EventRow> eventsTable = denseTable();"));
        assertTrue(eventsPaneView.contains("public EventsPaneView(VBox pane)"));
        assertTrue(eventsPaneView.contains("public EventsPageView view()"));
        assertTrue(eventsPaneView.contains("eventsSplitPane.getItems().setAll(eventTypesTree, vbox(6, eventsTable, eventWindowStatusLabel));"));
        assertTrue(eventsPaneView.contains("pane.getChildren().setAll(eventsTitleLabel, filters, eventsSplitPane, eventDetailsTabs);"));
    }


    @Test
    void profilingGraphShellWiringUsesViewModelLayoutsAndI18n() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String registry = shellPageControllerRegistrySource();
        String selectionController = workspaceSelectionSource();
        String pageController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/profiling/ProfilingPageController.java"));
        assertTrue(java.nio.file.Files.exists(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/profiling/ProfilingPageView.java")));
        String english = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages.properties"));
        String chinese = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages_zh_CN.properties"));
        String css = appCss();

        assertTrue(registry.contains("import com.youngledo.jmcfx.ui.profiling.ProfilingPageController;"));
        assertTrue(registry.contains("private ProfilingPageController profilingPageController;"));
        assertTrue(registry.contains("profilingPageController = new ProfilingPageController(view.profilingPage(), i18n);"));
        assertTrue(registry.contains("profilingPageController.configure();"));
        assertTrue(selectionController.contains("pages.profilingPageController().bind(workspace == null ? null : workspace.profilingViewModel())"));
        assertFalse(shell.contains("private TableView<HotMethod> profilingTable;"));
        assertFalse(shell.contains("private CallGraphView profilingCallGraphView;"));
        assertFalse(shell.contains("private CallGraphView profilingDependencyGraphView;"));
        assertFalse(shell.contains("private FlameGraphView profilingCallersFlameGraphView;"));
        assertFalse(shell.contains("private FlameGraphView profilingCalleesFlameGraphView;"));
        assertFalse(shell.contains("private void configureProfilingTable()"));
        assertFalse(shell.contains("private void bindProfiling("));
        assertFalse(shell.contains("private void selectProfilingMethod("));
        assertFalse(shell.contains("private void rebuildStackTree("));
        assertFalse(shell.contains("private String formatCallGraphDirection("));
        assertFalse(shell.contains("private void refreshProfilingCallGraphDirectionLabel("));
        assertFalse(shell.contains("private ListCell<CallGraphDirection> callGraphDirectionCell("));
        assertFalse(shell.contains("private void configureGraphZoomButtons("));
        assertFalse(shell.contains("private void configureCallGraphGestures("));
        assertFalse(shell.contains("private void zoomCallGraphAt("));
        assertFalse(shell.contains("private void panCallGraphViewport("));
        assertFalse(shell.contains("private void configureFlameGraphButtons("));
        assertFalse(shell.contains("private void bindFlameGraphToolbarVisibility("));
        assertFalse(shell.contains("private void toggleFlameGraphOrientation("));
        assertFalse(shell.contains("private double graphViewportWidth("));
        assertTrue(pageController.contains("private CallGraphView profilingCallGraphView;"));
        assertTrue(pageController.contains("private CallGraphView profilingDependencyGraphView;"));
        assertTrue(pageController.contains("private com.youngledo.jmcfx.flamegraph.FlameGraphView<StackFrameInfo> profilingCallersFlameGraphView;"));
        assertTrue(pageController.contains("private com.youngledo.jmcfx.flamegraph.FlameGraphView<StackFrameInfo> profilingCalleesFlameGraphView;"));
        assertTrue(pageController.contains("profilingCallGraphView.emptyTextProperty().bind(i18n.text(\"profiling.callGraph.empty\"))"));
        assertTrue(pageController.contains("profilingDependencyGraphView.emptyTextProperty().bind(i18n.text(\"profiling.dependency.empty\"))"));
        assertTrue(pageController.contains("view.callGraphContainer().getChildren().setAll(profilingCallGraphView)"));
        assertTrue(pageController.contains("view.dependencyGraphContainer().getChildren().setAll(profilingDependencyGraphView)"));
        assertTrue(pageController.contains("profilingCallGraphView.setLayout(null)"));
        assertTrue(pageController.contains("profilingDependencyGraphView.setLayout(null)"));
        assertTrue(pageController.contains("view.callersFlameContainer().getChildren().setAll(profilingCallersFlameGraphView)"));
        assertTrue(pageController.contains("view.calleesFlameContainer().getChildren().setAll(profilingCalleesFlameGraphView)"));
        assertTrue(pageController.contains("bindFlameGraphSummaryLabels(nextViewModel)"));
        assertTrue(pageController.contains("view.callersFlameSummaryLabel().textProperty().bind(summaryBinding)"));
        assertTrue(pageController.contains("view.calleesFlameSummaryLabel().textProperty().bind(summaryBinding)"));
        assertTrue(pageController.contains("view.callersFlameSummaryLabel().textProperty().unbind()"));
        assertTrue(pageController.contains("view.calleesFlameSummaryLabel().textProperty().unbind()"));
        assertTrue(pageController.contains("label.visibleProperty().bind(label.textProperty().isNotEmpty())"));
        assertTrue(pageController.contains("profiling.flame.summary.methodProfilingSample"));
        assertTrue(pageController.contains("profilingCallersFlameGraphView.setModel(null)"));
        assertTrue(pageController.contains("profilingCalleesFlameGraphView.setModel(null)"));
        assertTrue(pageController.contains("currentProfilingViewModel.callGraphProperty().removeListener(callGraphListener)"));
        assertTrue(pageController.contains("currentProfilingViewModel.dependencyGraphProperty().removeListener(dependencyGraphListener)"));
        assertTrue(pageController.contains("currentProfilingViewModel.callersTreeProperty().removeListener(callersTreeListener)"));
        assertTrue(pageController.contains("currentProfilingViewModel.calleesTreeProperty().removeListener(calleesTreeListener)"));
        assertTrue(pageController.contains("currentProfilingViewModel.callersFlameGraphProperty().removeListener(callersFlameGraphListener)"));
        assertTrue(pageController.contains("currentProfilingViewModel.calleesFlameGraphProperty().removeListener(calleesFlameGraphListener)"));
        assertTrue(pageController.contains("nextViewModel.callGraphProperty().addListener(callGraphListener)"));
        assertTrue(pageController.contains("nextViewModel.dependencyGraphProperty().addListener(dependencyGraphListener)"));
        assertTrue(pageController.contains("nextViewModel.callersTreeProperty().addListener(callersTreeListener)"));
        assertTrue(pageController.contains("nextViewModel.calleesTreeProperty().addListener(calleesTreeListener)"));
        assertTrue(pageController.contains("nextViewModel.callersFlameGraphProperty().addListener(callersFlameGraphListener)"));
        assertTrue(pageController.contains("nextViewModel.calleesFlameGraphProperty().addListener(calleesFlameGraphListener)"));
        assertTrue(pageController.contains("nextViewModel.callGraphProperty().get()"));
        assertTrue(pageController.contains("nextViewModel.dependencyGraphProperty().get()"));
        assertTrue(pageController.contains("nextViewModel.callersFlameGraphProperty().get()"));
        assertTrue(pageController.contains("nextViewModel.calleesFlameGraphProperty().get()"));
        assertTrue(pageController.contains("setCallGraphDirection"));
        assertTrue(pageController.contains("setCallGraphMaxDepth"));
        assertTrue(pageController.contains("profiling.callGraph.direction.callers"));
        assertTrue(pageController.contains("profiling.callGraph.direction.callees"));
        assertTrue(pageController.contains("i18n.localeProperty().addListener"));
        assertTrue(pageController.contains("refreshProfilingCallGraphDirectionLabel"));
        assertTrue(pageController.contains("CallGraphDirection selectedDirection = view.callGraphDirectionCombo().getSelectionModel().getSelectedItem()"));
        assertTrue(pageController.contains("view.callGraphDirectionCombo().getSelectionModel().select(selectedDirection)"));
        assertTrue(pageController.contains("view.callGraphTab().textProperty().bind(i18n.text(\"profiling.tab.callGraph\"))"));
        assertTrue(pageController.contains("view.dependencyGraphTab().textProperty().bind(i18n.text(\"profiling.tab.dependencyGraph\"))"));
        assertTrue(pageController.contains("view.callGraphDirectionCombo().promptTextProperty().bind(i18n.text(\"profiling.callGraph.direction\"))"));
        assertTrue(pageController.contains("view.callGraphDepthLabel().textProperty().bind(i18n.text(\"profiling.callGraph.depth\"))"));
        assertTrue(pageController.contains("view.dependencyDepthLabel().textProperty().bind(i18n.text(\"profiling.dependency.depth\"))"));
        assertTrue(pageController.contains("view.callersFlameTab().textProperty().bind(i18n.text(\"profiling.tab.callersFlame\"))"));
        assertTrue(pageController.contains("view.calleesFlameTab().textProperty().bind(i18n.text(\"profiling.tab.calleesFlame\"))"));
        assertTrue(pageController.contains("view.callersFlameSearchField().promptTextProperty().bind(i18n.text(\"profiling.flame.search.prompt\"))"));
        assertTrue(pageController.contains("view.calleesFlameSearchField().promptTextProperty().bind(i18n.text(\"profiling.flame.search.prompt\"))"));
        assertTrue(pageController.contains("profilingCallersFlameGraphView.emptyTextProperty().bind(i18n.text(\"profiling.flame.empty\"))"));
        assertTrue(pageController.contains("profilingCalleesFlameGraphView.emptyTextProperty().bind(i18n.text(\"profiling.flame.empty\"))"));
        assertTrue(pageController.contains("graphView.setTextProvider(ProfilingFlameGraphAdapter.textProvider())"));
        assertTrue(pageController.contains("graphView.setTooltipProvider(ProfilingFlameGraphAdapter.tooltipProvider())"));
        assertTrue(pageController.contains("graphView.setColorProvider(ProfilingFlameGraphAdapter.colorProvider())"));
        assertTrue(pageController.contains("configureGraphZoomButtons(profilingCallGraphView"));
        assertTrue(pageController.contains("configureGraphZoomButtons(profilingDependencyGraphView"));
        assertTrue(pageController.contains("configureFlameGraphButtons(profilingCallersFlameGraphView"));
        assertTrue(pageController.contains("toggleFlameGraphOrientation"));
        assertTrue(pageController.contains("bindFlameGraphToolbarVisibility(view.callersFlameToolbar()"));
        assertTrue(pageController.contains("bindFlameGraphToolbarVisibility(view.calleesFlameToolbar()"));
        assertTrue(pageController.contains("toolbar.visibleProperty().bind(graphView.hasFramesProperty())"));
        assertTrue(pageController.contains("toolbar.managedProperty().bind(toolbar.visibleProperty())"));
        assertTrue(pageController.contains("graphView.fitToWidth(graphViewportWidth(graphView))"));
        assertTrue(pageController.contains("graphView.setMode(graphView.getMode() == FlameGraphMode.ICICLE"));
        assertTrue(pageController.contains("configureFlameGraphGestures(profilingCallersFlameGraphView"));
        assertTrue(pageController.contains("configureFlameGraphGestures(profilingCalleesFlameGraphView"));
        assertTrue(pageController.contains("configureFlameGraphSearch(profilingCallersFlameGraphView"));
        assertTrue(pageController.contains("configureFlameGraphSearch(profilingCalleesFlameGraphView"));
        assertTrue(pageController.contains("searchField.textProperty().addListener"));
        assertTrue(pageController.contains("graphView.search(query)"));
        assertTrue(pageController.contains("flameGraphSearchStatus(searchField, graphView)"));
        assertTrue(pageController.contains("profiling.flame.search.noMatches"));
        assertTrue(pageController.contains("profiling.flame.search.matchStatus"));
        assertTrue(pageController.contains("previousButton.setOnAction(event -> graphView.previousMatch())"));
        assertTrue(pageController.contains("nextButton.setOnAction(event -> graphView.nextMatch())"));
        assertTrue(pageController.contains("clearButton.setOnAction(event -> searchField.clear())"));
        assertTrue(pageController.contains("graphView.addEventFilter(KeyEvent.KEY_PRESSED"));
        assertTrue(pageController.contains("event.isShortcutDown() && event.getCode() == KeyCode.F"));
        assertTrue(pageController.contains("searchField.requestFocus()"));
        assertTrue(pageController.contains("searchField.selectAll()"));
        assertTrue(pageController.contains("private void configureFlameGraphGestures("));
        assertTrue(pageController.contains("graphView.zoomBy(event.getDeltaY() > 0 ? 1.1 : 1 / 1.1"));
        assertTrue(pageController.contains("shouldPanFlameGraphHorizontally(event)"));
        assertTrue(pageController.contains("return Math.abs(event.getDeltaX()) > Math.abs(event.getDeltaY())"),
                "Vertical trackpad scroll should bubble to the surrounding ScrollPane");
        assertTrue(pageController.contains("event.isShiftDown()"),
                "Shift-scroll should remain available for horizontal flame graph panning");
        assertTrue(pageController.contains("graphView.setViewportOffsetX(graphView.viewportOffsetXProperty().get()"));
        assertTrue(pageController.contains("configureCallGraphGestures"));
        assertTrue(pageController.contains("addEventFilter(ScrollEvent.SCROLL"));
        assertTrue(pageController.contains("addEventFilter(ZoomEvent.ZOOM_STARTED"));
        assertTrue(pageController.contains("zoomCallGraphAt"));
        assertTrue(pageController.contains("scrollValueAfterZoom"));
        assertTrue(pageController.contains("addEventFilter(ZoomEvent.ZOOM_FINISHED"));
        assertFalse(shell.contains("PauseTransition"));
        assertTrue(pageController.contains("panCallGraphViewport"));
        assertTrue(pageController.contains("scrollPane.setHvalue"));
        assertTrue(pageController.contains("scrollPane.setVvalue"));
        assertTrue(pageController.contains("addEventFilter(ZoomEvent.ZOOM"));
        assertTrue(pageController.contains("addEventFilter(MouseEvent.MOUSE_CLICKED"));
        assertTrue(pageController.contains("event.isShortcutDown()"));
        assertTrue(pageController.contains("graphView.zoomBy"));

        assertTrue(css.contains(".profiling-call-graph-container"));
        assertTrue(css.contains(".profiling-graph-tab-content"));
        assertTrue(css.contains(".profiling-graph-toolbar"));
        assertTrue(css.contains(".profiling-graph-tool-button"));
        assertTrue(english.contains("profiling.tab.callGraph=Call Graph"));
        assertTrue(english.contains("profiling.tab.dependencyGraph=Dependency Graph"));
        assertTrue(english.contains("profiling.tab.callersFlame=Flame Graph"));
        assertTrue(english.contains("profiling.tab.calleesFlame=Inverted Flame Graph"));
        assertTrue(english.contains("profiling.callGraph.empty=Select a method to view the call graph."));
        assertTrue(english.contains("profiling.callGraph.direction=Direction"));
        assertTrue(english.contains("profiling.callGraph.direction.callers=Callers"));
        assertTrue(english.contains("profiling.callGraph.direction.callees=Callees"));
        assertTrue(english.contains("profiling.callGraph.depth=Depth"));
        assertTrue(english.contains("profiling.dependency.empty=No package dependencies found in execution samples."));
        assertTrue(english.contains("profiling.dependency.depth=Package Depth"));
        assertTrue(english.contains("profiling.dependency.column.source=Source"));
        assertTrue(english.contains("profiling.dependency.column.target=Target"));
        assertTrue(english.contains("profiling.dependency.column.count=Count"));
        assertTrue(english.contains("profiling.dependency.column.percentage=Percentage"));
        assertTrue(english.contains("profiling.graph.zoomIn=Zoom in"));
        assertTrue(english.contains("profiling.graph.zoomOut=Zoom out"));
        assertTrue(english.contains("profiling.graph.resetZoom=Reset zoom"));
        assertTrue(english.contains("profiling.graph.fit=Fit to width"));
        assertTrue(english.contains("profiling.flame.empty=Select a method to view the flame graph."));
        assertTrue(english.contains("profiling.flame.orientation=Switch flame/icicle orientation"));
        assertTrue(english.contains("profiling.flame.orientation.icicle=Icicle"));
        assertTrue(english.contains("profiling.flame.orientation.flame=Flame"));
        assertTrue(english.contains("profiling.flame.search.prompt=Find method"));
        assertTrue(english.contains("profiling.flame.search.previous=Previous match"));
        assertTrue(english.contains("profiling.flame.search.next=Next match"));
        assertTrue(english.contains("profiling.flame.search.clear=Clear search"));
        assertTrue(english.contains("profiling.flame.search.noMatches=No matches"));
        assertTrue(english.contains("profiling.flame.search.matchStatus={0}/{1}"));
        assertTrue(english.contains("profiling.flame.summary.methodProfilingSample={0} event(s) of 1 type(s): Method Profiling Sample[{0}]"));
        assertTrue(chinese.contains("profiling.tab.callGraph=调用图"));
        assertTrue(chinese.contains("profiling.tab.dependencyGraph=依赖图"));
        assertTrue(chinese.contains("profiling.tab.callersFlame=火焰图"));
        assertTrue(chinese.contains("profiling.tab.calleesFlame=反向火焰图"));
        assertTrue(chinese.contains("profiling.callGraph.empty=选择一个方法查看调用图。"));
        assertTrue(chinese.contains("profiling.callGraph.direction=方向"));
        assertTrue(chinese.contains("profiling.callGraph.direction.callers=调用者"));
        assertTrue(chinese.contains("profiling.callGraph.direction.callees=被调用者"));
        assertTrue(chinese.contains("profiling.callGraph.depth=深度"));
        assertTrue(chinese.contains("profiling.dependency.empty=执行采样中未找到包依赖。"));
        assertTrue(chinese.contains("profiling.dependency.depth=包深度"));
        assertTrue(chinese.contains("profiling.dependency.column.source=来源"));
        assertTrue(chinese.contains("profiling.dependency.column.target=目标"));
        assertTrue(chinese.contains("profiling.dependency.column.count=次数"));
        assertTrue(chinese.contains("profiling.dependency.column.percentage=百分比"));
        assertTrue(chinese.contains("profiling.graph.zoomIn=放大"));
        assertTrue(chinese.contains("profiling.graph.zoomOut=缩小"));
        assertTrue(chinese.contains("profiling.graph.resetZoom=重置缩放"));
        assertTrue(chinese.contains("profiling.graph.fit=适应宽度"));
        assertTrue(chinese.contains("profiling.flame.empty=选择一个方法查看火焰图。"));
        assertTrue(chinese.contains("profiling.flame.orientation=切换火焰图/冰柱图方向"));
        assertTrue(chinese.contains("profiling.flame.orientation.icicle=冰柱图"));
        assertTrue(chinese.contains("profiling.flame.orientation.flame=火焰图"));
        assertTrue(chinese.contains("profiling.flame.search.prompt=查找方法"));
        assertTrue(chinese.contains("profiling.flame.search.previous=上一个匹配"));
        assertTrue(chinese.contains("profiling.flame.search.next=下一个匹配"));
        assertTrue(chinese.contains("profiling.flame.search.clear=清除搜索"));
        assertTrue(chinese.contains("profiling.flame.search.noMatches=无匹配"));
        assertTrue(chinese.contains("profiling.flame.search.matchStatus={0}/{1}"));
        assertTrue(chinese.contains("profiling.flame.summary.methodProfilingSample={0}个事件，1种类型：方法分析采样[{0}]"));
    }

    @Test
    void workspaceShellKeepsGlobalLauncherInLeftNavigation() throws Exception {
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String css = appCss();

        assertFalse(controller.contains("workspaceHomeButton"));
        assertFalse(controller.contains("workspaceTopbar"));
        assertFalse(controller.contains("workspaceContextLabel"));
        assertFalse(css.contains(".workspace-topbar"));
        assertFalse(css.contains(".workspace-context-label"));
        assertFalse(css.contains(".workspace-home-button"));
    }

    @Test
    void liveJvmOverviewProvidesDefaultChartAndTableViews() throws Exception {
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java"));
        String english = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages.properties"));
        String chinese = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages_zh_CN.properties"));
        String css = appCss();

        assertTrue(controller.contains("private Tab jvmsOverviewTab;"));
        assertTrue(controller.contains("private TableView<LiveJvmOverviewMetric> jvmsOverviewPersistenceTable;"));
        assertTrue(controller.contains("private LineChart<Number, Number> jvmsOverviewDashboardChart;"));
        assertTrue(controller.contains("private FlowPane jvmsOverviewDashboardMetricToggles;"));
        assertTrue(controller.contains("private TableView<LiveJvmOverviewMetric> jvmsOverviewDashboardTable;"));
        assertTrue(controller.contains("private LineChart<Number, Number> jvmsOverviewProcessorChart;"));
        assertTrue(controller.contains("private FlowPane jvmsOverviewProcessorMetricToggles;"));
        assertTrue(controller.contains("private TableView<LiveJvmOverviewMetric> jvmsOverviewProcessorTable;"));
        assertTrue(controller.contains("private LineChart<Number, Number> jvmsOverviewMemoryChart;"));
        assertTrue(controller.contains("private FlowPane jvmsOverviewMemoryMetricToggles;"));
        assertTrue(controller.contains("private TableView<LiveJvmOverviewMetric> jvmsOverviewMemoryTable;"));
        assertTrue(controller.contains("DEFAULT_OVERVIEW_CHART_METRICS"));
        assertTrue(controller.contains("overviewChartSeries"));
        assertTrue(controller.contains("new CheckBox()"));
        assertTrue(controller.contains("checkBox.setGraphic(overviewMetricToggleGraphic(group, metric))"));
        assertTrue(controller.contains("selectOverviewMetricAxis(group, kind)"));
        assertTrue(controller.contains("updatingOverviewMetricSelection"));
        assertTrue(controller.contains("refreshOverviewMetricToggleGraphics(group)"));
        assertTrue(controller.contains("updateOverviewMetricToggle(CheckBox checkBox, String group, LiveJvmOverviewMetric metric)"));
        assertTrue(controller.contains("overviewMetricColorStyle(String group, LiveMetricKind kind)"));
        assertTrue(controller.contains("graphic.setPadding(new Insets(0, 0, 0, 8))"));
        assertFalse(controller.contains("series.getNode()"));
        assertTrue(controller.contains("updateLiveJvmOverviewChart"));
        assertTrue(controller.contains(".filter(metric -> group.equals(metric.group()))"));
        assertTrue(controller.contains("List<XYChart.Series<Number, Number>> orderedSeries = new ArrayList<>()"));
        assertTrue(controller.contains("chart.getData().setAll(orderedSeries)"));
        assertTrue(controller.contains("updateOverviewChartAxis"));
        assertTrue(controller.contains("updateOverviewChartAxis(LineChart<Number, Number> chart, String group, List<LiveMetricKind> kinds)"));
        assertTrue(controller.contains("overviewChartAxis(String group, List<LiveMetricKind> kinds)"));
        assertFalse(controller.contains("OverviewChartAxis.MIXED"));
        assertTrue(controller.contains("overviewChartValue"));
        assertTrue(controller.contains("DisplayFormats.formatFileSize(Math.round(numeric))"));
        assertTrue(controller.contains("OverviewValueTickFormatter"));
        assertTrue(controller.contains("chart.setLegendVisible(false);"));
        assertTrue(controller.contains("configureLiveJvmOverview();"));
        assertTrue(controller.contains("bindLiveJvmOverview();"));
        assertTrue(controller.contains("rebuildLiveJvmOverviewGroups();"));
        assertTrue(controller.contains(".addListener((ListChangeListener<LiveJvmOverviewMetric>) change -> updateLiveJvmOverviewCharts())"));
        assertTrue(controller.contains("overviewMetricTogglesUninitialized()"));
        assertTrue(controller.contains("jvmsOverviewDashboardMetricToggles.getChildren().isEmpty()"));
        assertTrue(controller.contains("event -> refreshLiveJvmOverviewCharts()"));
        assertFalse(controller.contains("jvmsRefreshOverviewButton"));
        assertFalse(controller.contains("jvmsOverviewUpdatedLabel"));
        assertTrue(controller.contains("jvmsOverviewDashboardTabs.getSelectionModel().select(jvmsOverviewDashboardChartTab)"));
        assertTrue(controller.contains("jvmsOverviewProcessorTabs.getSelectionModel().select(jvmsOverviewProcessorChartTab)"));
        assertTrue(controller.contains("jvmsOverviewMemoryTabs.getSelectionModel().select(jvmsOverviewMemoryChartTab)"));
        assertTrue(controller.contains("jvmsOverviewTab.textProperty().bind(i18n.text(\"jvms.overview.tab\"))"));
        assertTrue(controller.contains("jvmsOverviewPersistenceTitleLabel.textProperty().bind(i18n.text(\"jvms.overview.persistence.title\"))"));
        assertTrue(controller.contains("jvmsOverviewDashboardTitleLabel.textProperty().bind(i18n.text(\"jvms.overview.dashboard.title\"))"));
        assertTrue(controller.contains("jvmsOverviewProcessorTitleLabel.textProperty().bind(i18n.text(\"jvms.overview.processor.title\"))"));
        assertTrue(controller.contains("jvmsOverviewMemoryTitleLabel.textProperty().bind(i18n.text(\"jvms.overview.memory.title\"))"));
        assertTrue(controller.contains("new Timeline(new KeyFrame(Duration.seconds(2)"));
        assertTrue(controller.contains("localizedColumn(\"jvms.overview.metric.name\")"));
        assertTrue(controller.contains("localizedColumn(\"jvms.overview.metric.value\")"));
        assertTrue(controller.contains("localizedColumn(\"jvms.overview.metric.observed\")"));

        assertTrue(css.contains(".jvms-live-tab-content .chart"));
        assertTrue(css.contains(".jvms-live-tab-content .diagnostic-chart"));
        assertFalse(css.contains(".jvms-live-tab-content .chart:refreshing"));
        assertFalse(css.contains(".jvms-live-tab-content .table-view:refreshing"));
        assertTrue(css.contains(".jvms-overview-group"));
        assertTrue(css.contains(".jvms-overview-chart-panel .scroll-pane"));
        assertTrue(css.contains(".jvms-overview-metric-toggles"));
        assertTrue(css.contains(".jvms-overview-metric-toggle-content"));
        assertTrue(css.contains("-fx-spacing: 6px"));
        assertTrue(css.contains(".jvms-overview-metric-swatch.default-color0"));
        assertTrue(css.contains("-fx-background-color: #f3622d"));

        String view = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneView.java"));
        assertTrue(view.contains("addStyle(chart, \"diagnostic-chart\");"));
        assertTrue(view.contains("chart.setCreateSymbols(false);"));
        assertTrue(view.contains("chart.setHorizontalGridLinesVisible(true);"));
        assertTrue(view.contains("chart.setVerticalGridLinesVisible(false);"));

        assertTrue(english.contains("jvms.overview.tab=Overview"));
        assertTrue(english.contains("jvms.overview.chart=Chart"));
        assertTrue(english.contains("jvms.overview.table=Table"));
        assertFalse(english.contains("jvms.overview.refresh="));
        assertFalse(english.contains("jvms.overview.updated="));
        assertFalse(english.contains("jvms.overview.updated.empty="));
        assertTrue(english.contains("jvms.overview.persistence.title=JMX Data Persistence Settings"));
        assertTrue(english.contains("jvms.overview.dashboard.title=Dashboard"));
        assertTrue(english.contains("jvms.overview.processor.title=Processor"));
        assertTrue(english.contains("jvms.overview.memory.title=Memory"));
        assertTrue(english.contains("jvms.overview.persistence.summary=Persistence enabled for"));
        assertTrue(english.contains("jvms.overview.metrics.empty=No overview metrics sampled yet."));
        assertTrue(controller.contains("case \"bytes\" -> \"\""));
        assertTrue(english.contains("jvms.overview.axis.memory=Memory"));
        assertTrue(chinese.contains("jvms.overview.tab=总览"));
        assertTrue(chinese.contains("jvms.overview.chart=图表"));
        assertTrue(chinese.contains("jvms.overview.table=表格"));
        assertFalse(chinese.contains("jvms.overview.refresh="));
        assertFalse(chinese.contains("jvms.overview.updated="));
        assertFalse(chinese.contains("jvms.overview.updated.empty="));
        assertTrue(chinese.contains("jvms.overview.persistence.title=JMX数据持久化设置"));
        assertTrue(chinese.contains("jvms.overview.dashboard.title=Dashboard"));
        assertTrue(chinese.contains("jvms.overview.processor.title=Processor"));
        assertTrue(chinese.contains("jvms.overview.memory.title=Memory"));
        assertTrue(chinese.contains("jvms.overview.persistence.summary=已为"));
        assertTrue(chinese.contains("jvms.overview.metrics.empty=尚未采样总览指标。"));
        assertTrue(chinese.contains("jvms.overview.axis.memory=内存"));
    }

    @Test
    void liveJvmMonitoringWiresNotificationToolbarActions() throws Exception {
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java"));
        String english = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages.properties"));
        String chinese = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages_zh_CN.properties"));

        assertTrue(controller.contains("private Button jvmsAddNotificationSubscriptionButton;"));
        assertTrue(controller.contains("private Button jvmsStartNotificationsButton;"));
        assertTrue(controller.contains("private Button jvmsStopNotificationsButton;"));
        assertTrue(controller.contains(
                "jvmsAddNotificationSubscriptionButton.setOnAction(event -> addSelectedNotificationSubscription())"));
        assertTrue(controller.contains(
                "jvmsStartNotificationsButton.setOnAction(event -> jvmBrowserViewModel.startSelectedJmxNotifications())"));
        assertTrue(controller.contains(
                "jvmsStopNotificationsButton.setOnAction(event -> jvmBrowserViewModel.stopSelectedJmxNotifications())"));
        assertTrue(controller.contains(
                "jvmsAddNotificationSubscriptionButton.textProperty().bind(i18n.text(\"jvms.monitoring.addNotification\"))"));
        assertTrue(controller.contains(
                "jvmsStartNotificationsButton.textProperty().bind(i18n.text(\"jvms.monitoring.startNotifications\"))"));
        assertTrue(controller.contains(
                "jvmsStopNotificationsButton.textProperty().bind(i18n.text(\"jvms.monitoring.stopNotifications\"))"));
        assertTrue(english.contains("jvms.monitoring.addSubscription=Add Attribute"));
        assertTrue(english.contains("jvms.monitoring.addNotification=Add Notification"));
        assertTrue(english.contains("jvms.monitoring.startNotifications=Start Notifications"));
        assertTrue(english.contains("jvms.monitoring.stopNotifications=Stop Notifications"));
        assertTrue(chinese.contains("jvms.monitoring.addSubscription=添加属性"));
        assertTrue(chinese.contains("jvms.monitoring.addNotification=添加通知"));
        assertTrue(chinese.contains("jvms.monitoring.startNotifications=开始通知"));
        assertTrue(chinese.contains("jvms.monitoring.stopNotifications=停止通知"));
    }

    @Test
    void advancedJfrShellUsesTabbedHeatmapAndMemoryBindings() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String registry = shellPageControllerRegistrySource();
        String selectionController = workspaceSelectionSource();
        String advancedController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/advanced/AdvancedJfrPageController.java"));
        assertTrue(java.nio.file.Files.exists(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/advanced/AdvancedJfrPageView.java")));
        String analysisController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/analysis/AnalysisPageController.java"));
        String english = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages.properties"));
        String chinese = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages_zh_CN.properties"));
        String css = appCss();

        assertFalse(shell.contains("import com.youngledo.jmcfx.domain.model.MemoryIssue;"));
        assertTrue(registry.contains("import com.youngledo.jmcfx.ui.advanced.AdvancedJfrPageController;"));
        assertTrue(registry.contains("private AdvancedJfrPageController advancedJfrPageController;"));
        assertTrue(registry.contains("advancedJfrPageController = new AdvancedJfrPageController(view.advancedJfrPage(), i18n);"));
        assertTrue(registry.contains("advancedJfrPageController.configure();"));
        assertTrue(selectionController.contains("pages.advancedJfrPageController().bind(workspace == null ? null : workspace.advancedJfrViewModel());"));
        assertFalse(shell.contains("private TabPane advancedJfrTabs;"));
        assertFalse(shell.contains("private Tab advancedJfrHeatmapTab;"));
        assertFalse(shell.contains("private Tab advancedJfrMemoryTab;"));
        assertFalse(shell.contains("private TableView<MemoryIssue> advancedJfrMemoryTable;"));
        assertFalse(shell.contains("advancedHeatmapListener"));
        assertFalse(shell.contains("private boolean rebindingAdvancedJfrMemory;"));
        assertFalse(shell.contains("private void configureAdvancedJfrMemoryTable()"));
        assertFalse(shell.contains("private void bindAdvancedJfr("));
        assertFalse(shell.contains("private void bindAdvancedJfrMemoryText("));
        assertFalse(shell.contains("private String formatAdvancedJfrMemorySummary("));
        assertFalse(shell.contains("private String formatAdvancedJfrMemoryIssueTitle("));
        assertFalse(shell.contains("private String formatAdvancedJfrMemoryIssueDetails("));
        assertTrue(advancedController.contains("view.heatmapTab().textProperty().bind(i18n.text(\"advancedJfr.heatmap.tab\"))"));
        assertTrue(advancedController.contains("view.memoryTab().textProperty().bind(i18n.text(\"advancedJfr.memory.tab\"))"));
        assertTrue(advancedController.contains("view.memoryTable().setPlaceholder(localizedTablePlaceholder(\"advancedJfr.memory.empty\"))"));
        assertTrue(advancedController.contains("localizedColumn(\"advancedJfr.memory.column.severity\")"));
        assertTrue(advancedController.contains("localizedColumn(\"advancedJfr.memory.column.category\")"));
        assertTrue(advancedController.contains("localizedColumn(\"advancedJfr.memory.column.subject\")"));
        assertTrue(advancedController.contains("localizedColumn(\"advancedJfr.memory.column.estimatedBytes\")"));
        assertTrue(advancedController.contains("localizedColumn(\"advancedJfr.memory.column.count\")"));
        assertTrue(advancedController.contains("localizedColumn(\"advancedJfr.memory.column.score\")"));
        assertTrue(advancedController.contains("view.memoryTable().setItems(nextViewModel.memoryIssues())"));
        assertTrue(advancedController.contains("if (!rebindingMemorySelection && viewModel != null)"));
        assertTrue(advancedController.contains("viewModel.selectMemoryIssue(issue)"));
        assertTrue(advancedController.contains("rebindingMemorySelection = true;"));
        assertTrue(advancedController.contains("rebindingMemorySelection = false;"));
        assertTrue(advancedController.contains("bindMemoryText(nextViewModel)"));
        assertTrue(advancedController.contains("formatMemorySummary"));
        assertTrue(advancedController.contains("formatMemoryIssueTitle"));
        assertTrue(advancedController.contains("formatMemoryIssueDetails"));
        assertTrue(advancedController.contains("i18n.format(\"advancedJfr.memory.summary.format\""));
        assertTrue(advancedController.contains("i18n.format(\"advancedJfr.memory.detail.category\""));
        assertTrue(advancedController.contains("i18n.format(\"advancedJfr.memory.detail.recommendation\""));
        assertFalse(shell.contains("memorySummaryProperty()"));
        assertFalse(shell.contains("selectedMemoryIssueTitleProperty()"));
        assertFalse(shell.contains("selectedMemoryIssueDetailsProperty()"));
        assertTrue(advancedController.contains("view.memoryTable().setItems(FXCollections.emptyObservableList())"));
        assertTrue(advancedController.contains("view.memoryTable().getSelectionModel().clearSelection()"));

        assertTrue(css.contains(".advanced-jfr-memory-content"));
        assertTrue(css.contains(".detail-panel"));
        assertTrue(css.contains(".analysis-filter-bar"));

        assertTrue(analysisController.contains("view.searchField().textProperty().bindBidirectional"));
        assertTrue(analysisController.contains("view.minimumScoreSpinner().getValueFactory().valueProperty().bindBidirectional"));
        assertTrue(analysisController.contains("view.showOkCheckBox().selectedProperty().bindBidirectional"));
        assertTrue(analysisController.contains("view.showIgnoredCheckBox().selectedProperty().bindBidirectional"));
        assertTrue(analysisController.contains("view.showUnavailableCheckBox().selectedProperty().bindBidirectional"));
        assertTrue(analysisController.contains("localizedColumn(\"analysis.column.resultId\")"));
        assertTrue(analysisController.contains("localizedColumn(\"analysis.column.rulePage\")"));
        assertTrue(analysisController.contains("viewModel.selectedResultProperty().set(val)"));
        assertTrue(analysisController.contains("view.table().setRowFactory(table ->"));
        assertTrue(analysisController.contains("event.getButton() == MouseButton.PRIMARY"));
        assertTrue(analysisController.contains("event.getClickCount() == 2"));
        assertTrue(analysisController.contains("openRelatedPage(row.getItem())"));
        assertTrue(analysisController.contains("relatedPageNavigator.accept(detail.relatedPageId())"));
        assertFalse(analysisController.contains("analysisRelatedPageButton"));
        assertFalse(analysisController.contains("analysisDetailTitle"));
        assertFalse(analysisController.contains("analysisDetailSummaryArea"));
        assertFalse(analysisController.contains("analysisDetailResultIdLabel"));
        assertFalse(analysisController.contains("analysisDetailMeta"));
        assertTrue(analysisController.contains("view.detailEvidenceArea().setText(detail.evidence())"));
        assertTrue(analysisController.contains("view.detailRecommendationArea().setText(detail.recommendation())"));

        assertTrue(english.contains("analysis.filter.search=Search"));
        assertTrue(english.contains("analysis.filter.minimumScore=Min score"));
        assertTrue(english.contains("analysis.filter.showOk=OK"));
        assertTrue(english.contains("analysis.filter.showIgnored=Ignored"));
        assertTrue(english.contains("analysis.filter.showUnavailable=Unavailable"));
        assertTrue(english.contains("analysis.column.resultId=Result ID"));
        assertTrue(english.contains("analysis.column.rulePage=Page"));
        assertFalse(english.contains("analysis.detail.title="));
        assertFalse(english.contains("analysis.detail.summary="));
        assertFalse(english.contains("analysis.detail.resultId="));
        assertFalse(english.contains("analysis.detail.openRelatedPage="));
        assertTrue(english.contains("analysis.detail.evidence=Evidence"));
        assertTrue(english.contains("analysis.detail.recommendation=Recommendation"));
        assertTrue(english.contains("events.metadata.title=Events / Metadata"));
        assertTrue(english.contains("advancedJfr.heatmap.tab=Heatmap"));
        assertTrue(english.contains("advancedJfr.memory.tab=Memory Analysis"));
        assertTrue(english.contains("advancedJfr.memory.summary=Open a recording to load memory analysis."));
        assertTrue(english.contains("advancedJfr.memory.summary.format={0} {0,choice,1#issue|1<issues}, {1} estimated, {2} events"));
        assertTrue(english.contains("advancedJfr.memory.empty=No memory issues for this recording."));
        assertTrue(english.contains("advancedJfr.memory.column.severity=Severity"));
        assertTrue(english.contains("advancedJfr.memory.column.category=Category"));
        assertTrue(english.contains("advancedJfr.memory.column.subject=Subject"));
        assertTrue(english.contains("advancedJfr.memory.column.estimatedBytes=Estimated Bytes"));
        assertTrue(english.contains("advancedJfr.memory.column.count=Count"));
        assertTrue(english.contains("advancedJfr.memory.column.score=Score"));
        assertTrue(english.contains("advancedJfr.memory.detail.title={0} - {1}"));
        assertTrue(english.contains("advancedJfr.memory.detail.category=Category: {0}"));
        assertTrue(english.contains("advancedJfr.memory.detail.estimatedBytes=Estimated bytes: {0}"));
        assertTrue(english.contains("advancedJfr.memory.detail.count=Count: {0}"));
        assertTrue(english.contains("advancedJfr.memory.detail.score=Score: {0}"));
        assertTrue(english.contains("advancedJfr.memory.detail.evidence=Evidence: {0}"));
        assertTrue(english.contains("advancedJfr.memory.detail.recommendation=Recommendation: {0}"));

        assertTrue(chinese.contains("analysis.filter.search=搜索"));
        assertTrue(chinese.contains("analysis.filter.minimumScore=最低分数"));
        assertTrue(chinese.contains("analysis.filter.showOk=OK"));
        assertTrue(chinese.contains("analysis.filter.showIgnored=已忽略"));
        assertTrue(chinese.contains("analysis.filter.showUnavailable=不可用"));
        assertTrue(chinese.contains("analysis.column.resultId=结果ID"));
        assertTrue(chinese.contains("analysis.column.rulePage=页面"));
        assertFalse(chinese.contains("analysis.detail.title="));
        assertFalse(chinese.contains("analysis.detail.summary="));
        assertFalse(chinese.contains("analysis.detail.resultId="));
        assertFalse(chinese.contains("analysis.detail.openRelatedPage="));
        assertTrue(chinese.contains("analysis.detail.evidence=证据"));
        assertTrue(chinese.contains("analysis.detail.recommendation=建议"));
        assertTrue(chinese.contains("events.metadata.title=事件 / 元数据"));
        assertTrue(chinese.contains("advancedJfr.heatmap.tab=热力图"));
        assertTrue(chinese.contains("advancedJfr.memory.tab=内存分析"));
        assertTrue(chinese.contains("advancedJfr.memory.summary=打开JFR记录后加载内存分析。"));
        assertTrue(chinese.contains("advancedJfr.memory.summary.format={0} 个问题，估算 {1}，{2} 个事件"));
        assertTrue(chinese.contains("advancedJfr.memory.empty=此记录没有内存问题。"));
        assertTrue(chinese.contains("advancedJfr.memory.column.severity=严重程度"));
        assertTrue(chinese.contains("advancedJfr.memory.column.category=类别"));
        assertTrue(chinese.contains("advancedJfr.memory.column.subject=对象"));
        assertTrue(chinese.contains("advancedJfr.memory.column.estimatedBytes=估算字节"));
        assertTrue(chinese.contains("advancedJfr.memory.column.count=数量"));
        assertTrue(chinese.contains("advancedJfr.memory.column.score=分数"));
        assertTrue(chinese.contains("advancedJfr.memory.detail.title={0} - {1}"));
        assertTrue(chinese.contains("advancedJfr.memory.detail.category=类别：{0}"));
        assertTrue(chinese.contains("advancedJfr.memory.detail.estimatedBytes=估算字节：{0}"));
        assertTrue(chinese.contains("advancedJfr.memory.detail.count=数量：{0}"));
        assertTrue(chinese.contains("advancedJfr.memory.detail.score=分数：{0}"));
        assertTrue(chinese.contains("advancedJfr.memory.detail.evidence=证据：{0}"));
        assertTrue(chinese.contains("advancedJfr.memory.detail.recommendation=建议：{0}"));
    }

    @Test
    void metadataShellUsesSplitTableDetailBindingsAndI18n() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String registry = shellPageControllerRegistrySource();
        String visibilityController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/WorkspacePaneVisibilityController.java"));
        String selectionController = workspaceSelectionSource();
        String metadataController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/metadata/MetadataPageController.java"));
        String sectionLoader = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/RecordingSectionLoader.java"));
        assertTrue(java.nio.file.Files.exists(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/metadata/MetadataPageView.java")));
        String english = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages.properties"));
        String chinese = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages_zh_CN.properties"));

        assertFalse(shell.contains("import com.youngledo.jmcfx.domain.model.JfrMetadataEventType;"));
        assertTrue(registry.contains("import com.youngledo.jmcfx.ui.metadata.MetadataPageController;"));
        assertTrue(registry.contains("private MetadataPageController metadataPageController;"));
        assertTrue(visibilityController.contains("bind(panes.metadataPane, \"metadata\")"));
        assertTrue(registry.contains("metadataPageController = new MetadataPageController(view.metadataPage(), i18n);"));
        assertTrue(registry.contains("metadataPageController.configure();"));
        assertTrue(selectionController.contains("pages.metadataPageController().bind(workspace == null ? null : workspace.jfrMetadataViewModel());"));
        assertFalse(shell.contains("private void configureMetadataTable()"));
        assertFalse(shell.contains("private void bindJfrMetadata("));
        assertFalse(shell.contains("metadataSelectedEventTypeListener"));
        assertFalse(shell.contains("private TableView<JfrMetadataEventType> metadataEventTypesTable;"));
        assertTrue(sectionLoader.contains("case \"metadata\" -> loadIfPresent(workspace.jfrMetadataViewModel(), recording);"));
        assertTrue(metadataController.contains("view.titleLabel().textProperty().bind(i18n.text(\"metadata.title\"))"));
        assertTrue(metadataController.contains("view.detailTitleLabel().textProperty().bind(i18n.text(\"metadata.detail.title\"))"));
        assertTrue(metadataController.contains("view.eventTypesTable().setPlaceholder(localizedTablePlaceholder(\"metadata.empty\"))"));
        assertTrue(metadataController.contains("localizedColumn(\"metadata.column.category\")"));
        assertTrue(metadataController.contains("localizedColumn(\"metadata.column.name\")"));
        assertTrue(metadataController.contains("localizedColumn(\"metadata.column.id\")"));
        assertTrue(metadataController.contains("localizedColumn(\"metadata.column.eventCount\")"));
        assertTrue(metadataController.contains("localizedColumn(\"metadata.column.fieldCount\")"));
        assertTrue(metadataController.contains("view.eventTypesTable().setItems(nextViewModel.eventTypesProperty())"));
        assertTrue(metadataController.contains("view.detailArea().textProperty().bind(nextViewModel.selectedDetailProperty())"));
        assertTrue(metadataController.contains("selectedEventTypeProperty().set(eventType)"));
        assertTrue(metadataController.contains("useFormattedIntegerCells"));

        assertTrue(english.contains("metadata.title=JFR Metadata"));
        assertTrue(english.contains("metadata.empty=No event metadata for this recording."));
        assertTrue(english.contains("metadata.detail.title=Event Type Details"));
        assertTrue(english.contains("metadata.column.category=Category"));
        assertTrue(english.contains("metadata.column.name=Name"));
        assertTrue(english.contains("metadata.column.id=Event Type"));
        assertTrue(english.contains("metadata.column.eventCount=Events"));
        assertTrue(english.contains("metadata.column.fieldCount=Fields"));
        assertTrue(chinese.contains("metadata.title=JFR元数据"));
        assertTrue(chinese.contains("metadata.empty=此记录没有事件元数据。"));
        assertTrue(chinese.contains("metadata.detail.title=事件类型详情"));
        assertTrue(chinese.contains("metadata.column.category=类别"));
        assertTrue(chinese.contains("metadata.column.name=名称"));
        assertTrue(chinese.contains("metadata.column.id=事件类型"));
        assertTrue(chinese.contains("metadata.column.eventCount=事件数"));
        assertTrue(chinese.contains("metadata.column.fieldCount=字段数"));
    }


    @Test
    void cssDefinesSamplerStyleSidebarHooks() throws Exception {
        String css = appCss();

        assertTrue(css.contains(".app-sidebar"));
        assertTrue(css.contains("-fx-pref-width: 270px"));
        assertTrue(css.contains("-color-bg-inset"));
        assertTrue(css.contains(".sidebar-header"));
        assertTrue(css.contains(".sidebar-product-mark"));
        assertTrue(css.contains(".sidebar-search"));
        assertFalse(css.contains(".sidebar-recording-card"));
        assertFalse(css.contains(".recording-card-label"));
        assertFalse(css.contains(".recording-card-title"));
        assertTrue(css.contains(".app-nav-tree"));
        assertTrue(css.contains(".app-nav-tree-cell"));
        assertTrue(css.contains(".nav-icon-recording"));
        assertTrue(css.contains(".nav-icon-java"));
        assertTrue(css.contains(".nav-icon-memory"));
        assertTrue(css.contains(".nav-icon-environment"));
        assertTrue(css.contains(".nav-icon-application"));
        assertTrue(css.contains(":group"));
        assertTrue(css.contains(":unavailable"));
        assertTrue(css.contains(".sidebar-footer"));
        assertTrue(css.contains(".toolbar-primary"));
        assertTrue(css.contains(".recording-tabs"));
        assertTrue(css.contains(".home-hero"));
        assertTrue(css.contains(".workflow-tile"));
    }

    @Test
    void toolbarPrimaryDoesNotOverrideThemeButtonColors() throws Exception {
        String css = appCss();
        String toolbarPrimary = cssBlock(css, ".toolbar-primary");

        assertFalse(toolbarPrimary.contains("-color-button-bg:"));
        assertFalse(toolbarPrimary.contains("-color-button-bg-hover:"));
        assertFalse(toolbarPrimary.contains("-color-button-bg-focused:"));
        assertFalse(toolbarPrimary.contains("-color-button-bg-pressed:"));
        assertFalse(toolbarPrimary.contains("-color-button-border:"));
        assertFalse(toolbarPrimary.contains("-color-button-border-hover:"));
        assertFalse(toolbarPrimary.contains("-color-button-border-focused:"));
        assertFalse(toolbarPrimary.contains("-color-button-border-pressed:"));
        assertFalse(css.contains(".toolbar-primary:disabled"));
        assertFalse(toolbarPrimary.contains("-fx-opacity: 1.0"));
    }

    @Test
    void appSpecificButtonHooksDoNotOverrideAtlantaFxButtonStateTokens() throws Exception {
        String css = appCss();

        for (String selector : List.of(".toolbar-primary", ".toolbar-secondary", ".sidebar-search")) {
            String block = cssBlock(css, selector);
            assertFalse(block.contains("-color-button-"),
                    selector + " must not override AtlantaFX button state color tokens");
        }
    }

    @Test
    void homeHeroUsesDefaultBackgroundSoAtlantaFxButtonPressRemainsVisible() throws Exception {
        String css = appCss();
        String homeHero = cssBlock(css, ".home-hero");

        assertTrue(homeHero.contains("-fx-background-color: -color-bg-default"),
                "AtlantaFX buttons press to -color-bg-subtle, so the hero must not use the same fill");
        assertFalse(css.contains(".home-actions .button {"),
                "home action buttons should use AtlantaFX button styles instead of local pressed-state overrides");
    }

    @Test
    void navSelectionDoesNotChangeTextWeightAndCauseLayoutJitter() throws Exception {
        String css = appCss();
        String selectedNavTitle = cssBlock(css, ".app-nav-tree-cell:selected > .nav-cell-container > .nav-title");

        assertFalse(selectedNavTitle.contains("-fx-font-weight:"),
                "selected navigation items should not change text weight because it relayouts TreeCells on click");
    }

    @Test
    void jvmsLiveTabsUseUnifiedContentSpacing() throws Exception {
        String css = appCss();
        String sessionDetail = cssBlock(css, ".jvms-session-detail");
        String browserSidebar = cssBlock(css, ".jvms-browser-sidebar");
        String tabContent = cssBlock(css, ".jvms-live-tab-content");
        String workspace = cssBlock(css, ".jvms-live-workspace");

        assertTrue(browserSidebar.contains("-fx-pref-width: 340px"));
        assertTrue(sessionDetail.contains("-fx-min-height: 360px"));
        assertFalse(sessionDetail.contains("-fx-pref-height: 320px"));
        assertTrue(tabContent.contains("-fx-padding: 10px 10px 0 0"));
        assertTrue(workspace.contains("-fx-padding: 0 0 0 8px"));
    }

    @Test
    void detailPanelTitlesUseSharedVerticalSpacingContract() throws Exception {
        String css = appCss();
        String detailPanel = cssBlock(css, ".detail-panel");
        String detailPanelTitle = cssBlock(css, ".detail-panel-title");

        assertFalse(detailPanel.contains("-fx-padding"),
                "Generic detail panel must not add area-level padding; callers own their page layout");
        assertFalse(detailPanel.contains("-fx-spacing"),
                "Generic detail panel must not add area-level spacing; only titles get shared vertical spacing");
        assertTrue(detailPanelTitle.contains("-fx-padding: 4px 0 10px 0"));
    }


    @Test
    void workspaceTabsUseDistinctSelectedIndicator() throws Exception {
        String css = appCss();
        String selectedTab = cssBlock(css, ".recording-tabs .tab:selected");
        String selectedTabContainer = cssBlock(css, ".recording-tabs .tab:selected .tab-container");
        String selectedTabLabel = cssBlock(css, ".recording-tabs .tab:selected .tab-label");

        assertFalse(selectedTab.contains("-fx-border-width"),
                "Selected workspace tabs must not change tab border size because it can clip adjacent labels");
        assertTrue(selectedTabContainer.contains("-fx-border-width: 0 0 3px 0"));
        assertTrue(selectedTabContainer.contains("-fx-border-color: -color-accent-emphasis"));
        assertTrue(selectedTabLabel.contains("-fx-text-fill: -color-fg-default"));
    }

    @Test
    void heapDumpPageUsesSplitTableDetailTemplateSpacing() throws Exception {
        String css = appCss();
        String page = cssBlock(css, ".page");
        String header = cssBlock(css, ".page-header");
        String toolbar = cssBlock(css, ".page-toolbar");
        String heapDumpPage = cssBlock(css, ".heap-dump-page");
        String content = cssBlock(css, ".heap-dump-page .page-content");
        String table = cssBlock(css, ".heap-dump-page .dense-table");
        String detailTabs = cssBlock(css, ".heap-dump-page .page-detail-tabs");
        String detailPanelTitle = cssBlock(css, ".heap-dump-page .detail-panel-title");
        String textReport = cssBlock(css, ".heap-dump-page .dump-text-area");

        assertTrue(page.contains("-fx-padding: 0 0 2px 0"));
        assertTrue(page.contains("-fx-spacing: 12px"));
        assertTrue(header.contains("-fx-spacing: 8px"));
        assertTrue(toolbar.contains("-fx-padding: 2px 0 6px 0"));
        assertTrue(toolbar.contains("-fx-alignment: center-left"));
        assertTrue(heapDumpPage.contains("-fx-spacing: 12px"));
        assertTrue(heapDumpPage.contains("-fx-min-width: 0"));
        assertTrue(content.contains("-fx-padding: 6px 0 0 0"));
        assertTrue(table.contains("-fx-min-height: 96px"));
        assertTrue(detailTabs.contains("-fx-min-height: 120px"));
        assertTrue(detailTabs.contains("-fx-pref-height: 220px"));
        assertTrue(detailTabs.contains("-fx-padding: 6px 0 0 0"));
        assertTrue(detailPanelTitle.contains("-fx-padding: 4px 0 10px 0"));
        assertTrue(textReport.contains("-fx-padding: 8px"));
    }

    @Test
    void closingLastHeapDumpWorkspaceClearsBoundAnalysisViewModel() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String registry = shellPageControllerRegistrySource();
        String selectionController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/WorkspaceSelectionController.java"));
        String pageController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/heapdump/HeapDumpAnalysisPageController.java"));
        assertTrue(java.nio.file.Files.exists(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/heapdump/HeapDumpAnalysisPageView.java")));
        String method = selectionController.substring(selectionController.indexOf("private void showHeapDumpWorkspace"));
        method = method.substring(0, method.indexOf("private void showLiveJvmWorkspace"));

        assertTrue(registry.contains("import com.youngledo.jmcfx.ui.heapdump.HeapDumpAnalysisPageController;"));
        assertTrue(registry.contains("private HeapDumpAnalysisPageController heapDumpAnalysisPageController;"));
        assertTrue(registry.contains("heapDumpAnalysisPageController = new HeapDumpAnalysisPageController(view.heapDumpAnalysisPage(), i18n);"));
        assertTrue(registry.contains("heapDumpAnalysisPageController.configure();"));
        assertTrue(method.contains("pages.heapDumpAnalysisPageController().bind(null)"),
                "Closing the last HPROF workspace must clear the previously bound analysis view model");
        assertTrue(method.contains("pages.heapDumpAnalysisPageController().bind(workspace.viewModel())"),
                "Selecting an HPROF workspace must bind the selected analysis view model");
        assertFalse(source.contains("private void configureHeapDumpAnalysis()"));
        assertFalse(source.contains("private void bindHeapDumpAnalysis("));
        assertFalse(source.contains("private void selectHeapDumpIssue("));
        assertFalse(source.contains("private void selectHeapDumpIssueInTable("));
        assertTrue(pageController.contains("viewModel = null"),
                "Clearing HPROF binding must also clear the controller-level view model reference");
    }

    @Test
    void fileBackedWorkspaceOpenShortCircuitsWhenPathIsAlreadyOpen() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/WorkspaceOpenCoordinator.java"));
        String openRecording = source.substring(source.indexOf("void openRecordingInBackground"),
                source.indexOf("PreparedRecordingWorkspace prepareRecordingWorkspace"));
        String openHeapDump = source.substring(source.indexOf("private void openHeapDumpInBackground"),
                source.indexOf("private boolean selectExistingRecordingWorkspace"));

        assertTrue(openRecording.contains("selectExistingRecordingWorkspace(path)"),
                "Opening an already-open JFR should select the existing workspace before parsing");
        assertTrue(openRecording.indexOf("selectExistingRecordingWorkspace(path)")
                        < openRecording.indexOf("setRecordingOpening(true)"),
                "JFR duplicate detection must happen before showing parse progress");
        assertTrue(openHeapDump.contains("selectExistingHeapDumpWorkspace(path)"),
                "Opening an already-open HPROF should select the existing workspace before analysis");
        assertTrue(openHeapDump.indexOf("selectExistingHeapDumpWorkspace(path)")
                        < openHeapDump.indexOf("backgroundWorkVisibleConsumer.accept(true)"),
                "HPROF duplicate detection must happen before starting analysis progress");
    }

    @Test
    void heapDumpRebindingRemovesOldSelectionListenersBeforeClearingViewModel() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String pageController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/heapdump/HeapDumpAnalysisPageController.java"));
        String bindMethod = pageController.substring(pageController.indexOf("public void bind"));
        bindMethod = bindMethod.substring(0, bindMethod.indexOf("private void selectHeapDumpIssue"));

        assertFalse(source.contains("heapDumpTableSelectionListener"));
        assertFalse(source.contains("heapDumpSelectedIssueListener"));
        assertTrue(pageController.contains("heapDumpTableSelectionListener"));
        assertTrue(pageController.contains("heapDumpSelectedIssueListener"));
        assertTrue(pageController.contains("view.titleLabel().textProperty().bind(i18n.text(\"heapDump.title\"))"));
        assertTrue(pageController.contains("view.issueDetailTab().textProperty().bind(i18n.text(\"heapDump.detail.tab\"))"));
        assertTrue(pageController.contains("view.textReportTab().textProperty().bind(i18n.text(\"heapDump.report.tab\"))"));
        assertTrue(pageController.contains("view.issuesTable().setPlaceholder(localizedTablePlaceholder(\"heapDump.openPrompt\"))"));
        assertTrue(pageController.contains("localizedColumn(\"heapDump.column.category\")"));
        assertTrue(pageController.contains("localizedColumn(\"heapDump.column.subject\")"));
        assertTrue(pageController.contains("localizedColumn(\"heapDump.column.wastedBytes\")"));
        assertTrue(pageController.contains("i18n.text(\"heapDump.column.objectCount\")"));
        assertTrue(pageController.contains("localizedColumn(\"heapDump.column.score\")"));
        assertTrue(pageController.contains("view.issueDetailArea().textProperty().bind(viewModel.selectedIssueDetailsProperty())"));
        assertTrue(pageController.contains("view.textReportArea().textProperty().bind(viewModel.textReportProperty())"));
        assertTrue(pageController.contains("view.issuesTable().setItems(viewModel.issues())"));
        assertTrue(pageController.contains("viewModel.selectIssue(issue)"));
        assertTrue(pageController.contains("view.issuesTable().getSelectionModel().select(issue)"));
        assertTrue(bindMethod.indexOf("removeListener(heapDumpTableSelectionListener)")
                        < bindMethod.indexOf("viewModel = null"),
                "Table selection listener must be removed before the HPROF view model reference is cleared");
        assertTrue(bindMethod.contains("removeListener(heapDumpSelectedIssueListener)"),
                "Selected issue listener must be removed when rebinding HPROF workspaces");
        assertFalse(bindMethod.contains("addListener((observable, oldValue, newValue) -> viewModel"),
                "HPROF table selection listener must not capture the mutable controller-level view model field");
    }

    @Test
    void profilingFlameGraphContainerUsesScopedPadding() throws Exception {
        String css = appCss();
        String tabContent = cssBlock(css, ".profiling-graph-tab-content");
        String toolbar = cssBlock(css, ".profiling-graph-toolbar");
        String flameContainer = cssBlock(css, ".profiling-flame-container");
        String callGraphContainer = cssBlock(css, ".profiling-call-graph-container");

        assertTrue(tabContent.contains("-fx-padding: 12px 14px 14px 14px"));
        assertTrue(toolbar.contains("-fx-spacing: 8px"));
        assertTrue(flameContainer.contains("-fx-padding: 8px 0 0 0"));
        assertTrue(callGraphContainer.contains("-fx-padding: 8px 0 0 0"));
        assertTrue(css.contains(".profiling-flame-summary"));
    }

    @Test
    void openRecordingDialogIsDeferredUntilButtonActionCompletes() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/WorkspaceOpenCoordinator.java"));

        assertTrue(source.contains("Platform.runLater(this::showOpenRecordingChooser)"),
                "native file chooser should open after the button action finishes so pressed styling can clear");
    }

    @Test
    void clearingProfilingSelectionPassesNullThroughToViewModel() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/profiling/ProfilingPageController.java"));

        assertFalse(source.contains("profilingViewModel == null || method == null"),
                "Clearing table selection must still clear profiling stack details");
        assertTrue(source.contains("profilingViewModel.selectMethod(method)"),
                "Profiling page controller must pass null selection through to ProfilingViewModel");
    }

    @Test
    void homeActionButtonsBothUseLeadingIcons() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/HomePaneController.java"));
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String runtime = shellRuntimeSource();

        assertTrue(runtime.contains("new HomePaneController("),
                "runtime should delegate Home page behavior to a focused controller");
        assertFalse(shell.contains("private Button homeOpenRecordingButton;"));
        assertFalse(shell.contains("private Button homeOpenHeapDumpButton;"));
        assertFalse(shell.contains("private void configureActionIcons()"));
        assertTrue(source.contains("configureActionButton(view.openRecordingButton"),
                "Open recording home action should keep its leading icon");
        assertTrue(source.contains("configureActionButton(view.connectJvmButton"),
                "Connect JVM home action should use the same leading icon treatment");
    }

    @Test
    void settingsBehaviorLivesOutsideShellController() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String runtime = shellRuntimeSource();
        String settings = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/SettingsPaneController.java"));

        assertTrue(runtime.contains("new SettingsPaneController("),
                "runtime should delegate Settings page behavior to a focused controller");
        assertFalse(shell.contains("private ToggleGroup languageToggleGroup;"));
        assertFalse(shell.contains("private ToggleGroup themeToggleGroup;"));
        assertFalse(shell.contains("private void configureLanguageSelector()"));
        assertFalse(shell.contains("private void configureThemeSelector()"));
        assertTrue(settings.contains("class SettingsPaneController"));
        assertTrue(settings.contains("void configure()"));
        assertTrue(settings.contains("view.languageToggleGroup.selectToggle(modeToToggle("));
        assertTrue(settings.contains("view.themeToggleGroup.selectToggle(themeToToggle("));
        assertTrue(settings.contains("i18n.setLanguageMode(mode)"));
    }

    @Test
    void recordingOverviewPageWiringLivesOutsideShellController() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String registry = shellPageControllerRegistrySource();
        String controller = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/youngledo/jmcfx/ui/shell/RecordingOverviewPagesController.java"));

        assertTrue(registry.contains("new RecordingOverviewPagesController("),
                "registry should delegate recording overview page wiring to a focused controller");
        assertFalse(shell.contains("private Label javaApplicationTitleLabel;"));
        assertFalse(shell.contains("private Button jvmInternalsGcButton;"));
        assertFalse(shell.contains("private Label environmentProcessesTitleLabel;"));
        assertFalse(shell.contains("private void configureJavaApplicationOverviewActions()"));
        assertFalse(shell.contains("private void configureJvmInternalsOverviewActions()"));
        assertFalse(shell.contains("private void configureEnvironmentOverviewActions()"));
        assertTrue(controller.contains("class RecordingOverviewPagesController"));
        assertTrue(controller.contains("void configure()"));
        assertTrue(controller.contains("view.javaApplicationTitleLabel().textProperty().bind(i18n.text(\"javaApplication.title\"))"));
        assertTrue(controller.contains("view.jvmInternalsGcButton().setOnAction(event -> viewModel.showSection(\"gcSummary\"))"));
        assertTrue(controller.contains("view.environmentProcessesButton().setOnAction(event -> viewModel.showSection(\"processes\"))"));
    }

    @Test
    void appShellDelegatesOverviewPageToDomainPackagedController() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String runtime = shellRuntimeSource();
        String registry = shellPageControllerRegistrySource();
        String selection = workspaceSelectionSource();
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String overviewController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/overview/OverviewPageController.java"));
        String overviewView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/overview/OverviewPageView.java"));

        assertTrue(registry.contains("private OverviewPageController overviewPageController;"));
        assertTrue(registry.contains("overviewPageController = new OverviewPageController(view.overviewPage(), i18n);"));
        assertTrue(registry.contains("overviewPageController.configure();"));
        assertTrue(selection.contains("pages.overviewPageController().bind(workspace == null ? null : workspace.overviewViewModel())"));
        assertTrue(runtime.contains("i18n.localeProperty().addListener((observable, oldValue, newValue) -> pageControllerRegistry.refreshOverviewLocale())"));
        assertTrue(appShellView.contains("OverviewPageView overviewPage()"));

        assertFalse(shell.contains("private OverviewViewModel overviewViewModel;"));
        assertFalse(shell.contains("private Label overviewTitleLabel;"));
        assertFalse(shell.contains("private Label overviewRecordingNameLabel;"));
        assertFalse(shell.contains("private Label overviewRecordingDetailsLabel;"));
        assertFalse(shell.contains("private Label overviewAnalysisTitleLabel;"));
        assertFalse(shell.contains("private Label overviewAnalysisStatusLabel;"));
        assertFalse(shell.contains("private Label overviewJvmsTitleLabel;"));
        assertFalse(shell.contains("private Label overviewJvmStatusLabel;"));
        assertFalse(shell.contains("private void bindOverview("));
        assertFalse(shell.contains("private void refreshOverviewOnLocaleChange()"));

        assertTrue(overviewController.contains("package com.youngledo.jmcfx.ui.overview;"));
        assertTrue(overviewController.contains("public final class OverviewPageController"));
        assertTrue(overviewController.contains("public void configure()"));
        assertTrue(overviewController.contains("public void bind(OverviewViewModel nextViewModel)"));
        assertTrue(overviewController.contains("public void refreshLocale()"));
        assertTrue(overviewController.contains("DisplayFormats.formatDuration(recording.durationMillis())"));
        assertTrue(overviewView.contains("public record OverviewPageView("));
    }

    @Test
    void overviewPaneNodeOwnershipIsSplitOutOfAppShellView() throws Exception {
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String overviewPaneView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/overview/OverviewPaneView.java"));

        assertTrue(appShellView.contains("final OverviewPaneView overview = new OverviewPaneView(workspacePanes.overviewPane);"));
        assertTrue(appShellView.contains("OverviewPageView overviewPage()"));
        assertTrue(appShellView.contains("return overview.view();"));
        assertFalse(appShellView.contains("final Label overviewTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final Label overviewRecordingNameLabel = new Label();"));
        assertFalse(appShellView.contains("final Label overviewRecordingDetailsLabel = new Label();"));
        assertFalse(appShellView.contains("final Label overviewAnalysisTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final Label overviewAnalysisStatusLabel = new Label();"));
        assertFalse(appShellView.contains("final Label overviewJvmsTitleLabel = new Label();"));
        assertFalse(appShellView.contains("final Label overviewJvmStatusLabel = new Label();"));
        assertFalse(appShellView.contains("private void configureOverview()"));

        assertTrue(overviewPaneView.contains("package com.youngledo.jmcfx.ui.overview;"));
        assertTrue(overviewPaneView.contains("public final class OverviewPaneView"));
        assertTrue(overviewPaneView.contains("private final Label titleLabel = new Label();"));
        assertTrue(overviewPaneView.contains("public OverviewPaneView(VBox pane)"));
        assertTrue(overviewPaneView.contains("public OverviewPageView view()"));
        assertTrue(overviewPaneView.contains("pane.getChildren().setAll(titleLabel, recording, row);"));
    }

    @Test
    void appShellDelegatesJavaApplicationDataPagesToDomainPackagedControllers() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String registry = shellPageControllerRegistrySource();
        String selection = workspaceSelectionSource();
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String exceptionsController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/exceptions/ExceptionsPageController.java"));
        String exceptionsView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/exceptions/ExceptionsPageView.java"));
        String threadsController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/threads/ThreadsPageController.java"));
        String threadsView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/threads/ThreadsPageView.java"));
        String javaAppController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/javaapp/JavaApplicationDataPagesController.java"));
        String javaAppView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/javaapp/JavaApplicationDataPagesView.java"));

        assertTrue(registry.contains("private ExceptionsPageController exceptionsPageController;"));
        assertTrue(registry.contains("private ThreadsPageController threadsPageController;"));
        assertTrue(registry.contains("private JavaApplicationDataPagesController javaApplicationDataPagesController;"));
        assertTrue(registry.contains("exceptionsPageController = new ExceptionsPageController(view.exceptionsPage(), i18n);"));
        assertTrue(registry.contains("threadsPageController = new ThreadsPageController(view.threadsPage(), i18n);"));
        assertTrue(registry.contains("javaApplicationDataPagesController = new JavaApplicationDataPagesController(view.javaApplicationDataPages(), i18n);"));
        assertTrue(selection.contains("pages.exceptionsPageController().bind(workspace == null ? null : workspace.exceptionViewModel())"));
        assertTrue(selection.contains("pages.threadsPageController().bind(workspace == null ? null : workspace.threadViewModel())"));
        assertTrue(selection.contains("pages.javaApplicationDataPagesController().bindThreadHistogram("));
        assertTrue(selection.contains("workspace == null ? null : workspace.javaAppOverviewViewModel()"));
        assertTrue(selection.contains("pages.javaApplicationDataPagesController().bindSecurity(workspace == null ? null : workspace.securityViewModel())"));
        assertTrue(selection.contains("pages.javaApplicationDataPagesController().bindNativeLibraries("));
        assertTrue(selection.contains("workspace == null ? null : workspace.nativeLibraryViewModel()"));
        assertTrue(selection.contains("pages.javaApplicationDataPagesController().bindThreadDumps("));
        assertTrue(selection.contains("workspace == null ? null : workspace.threadDumpViewModel()"));
        assertTrue(registry.contains("installer.install(exceptionsPageController.table())"));
        assertTrue(registry.contains("installer.install(threadsPageController.table())"));
        assertTrue(registry.contains("javaApplicationDataPagesController.exportTables().forEach(installer::install)"));

        assertFalse(shell.contains("private ExceptionViewModel exceptionViewModel;"));
        assertFalse(shell.contains("private ThreadViewModel threadViewModel;"));
        assertFalse(shell.contains("private JavaAppOverviewViewModel javaAppOverviewViewModel;"));
        assertFalse(shell.contains("private SecurityViewModel securityViewModel;"));
        assertFalse(shell.contains("private NativeLibraryViewModel nativeLibraryViewModel;"));
        assertFalse(shell.contains("private ThreadDumpViewModel threadDumpViewModel;"));
        assertFalse(shell.contains("private TableView<ExceptionSummary> exceptionsTable;"));
        assertFalse(shell.contains("private TableView<ThreadSummary> threadsTable;"));
        assertFalse(shell.contains("private TableView<ThreadHistogramRow> threadHistogramTable;"));
        assertFalse(shell.contains("private TableView<X509CertificateEntry> securityTable;"));
        assertFalse(shell.contains("private TableView<NativeLibraryEntry> nativeLibrariesTable;"));
        assertFalse(shell.contains("private TableView<ThreadDumpEntry> threadDumpsTable;"));
        assertFalse(shell.contains("private void configureExceptionTable()"));
        assertFalse(shell.contains("private void configureThreadTable()"));
        assertFalse(shell.contains("private void configureThreadHistogramTable()"));
        assertFalse(shell.contains("private void configureSecurityTable()"));
        assertFalse(shell.contains("private void configureNativeLibrariesTable()"));
        assertFalse(shell.contains("private void configureThreadDumpsTable()"));
        assertFalse(shell.contains("private void bindExceptions("));
        assertFalse(shell.contains("private void bindThreads("));
        assertFalse(shell.contains("private void bindThreadHistogram("));
        assertFalse(shell.contains("private void bindSecurity("));
        assertFalse(shell.contains("private void bindNativeLibraries("));
        assertFalse(shell.contains("private void bindThreadDumps("));
        assertFalse(shell.contains("private void setExceptionGrouping("));

        assertTrue(appShellView.contains("ExceptionsPageView exceptionsPage()"));
        assertTrue(appShellView.contains("ThreadsPageView threadsPage()"));
        assertTrue(appShellView.contains("JavaApplicationDataPagesView javaApplicationDataPages()"));
        assertTrue(exceptionsView.contains("public record ExceptionsPageView("));
        assertTrue(threadsView.contains("public record ThreadsPageView("));
        assertTrue(javaAppView.contains("public record JavaApplicationDataPagesView("));

        assertTrue(exceptionsController.contains("package com.youngledo.jmcfx.ui.exceptions;"));
        assertTrue(exceptionsController.contains("public final class ExceptionsPageController"));
        assertTrue(exceptionsController.contains("view.titleLabel().textProperty().bind(i18n.text(\"exceptions.title\"))"));
        assertTrue(exceptionsController.contains("view.groupByClassButton().setOnAction(event -> setExceptionGrouping(ExceptionGrouping.BY_CLASS))"));
        assertTrue(exceptionsController.contains("view.table().setItems(nextViewModel.histogramProperty())"));
        assertTrue(exceptionsController.contains("currentViewModel.timelineProperty().removeListener(timelineListener)"));
        assertTrue(exceptionsController.contains("public TableView<ExceptionSummary> table()"));

        assertTrue(threadsController.contains("package com.youngledo.jmcfx.ui.threads;"));
        assertTrue(threadsController.contains("public final class ThreadsPageController"));
        assertTrue(threadsController.contains("view.titleLabel().textProperty().bind(i18n.text(\"threads.title\"))"));
        assertTrue(threadsController.contains("view.table().setItems(nextViewModel.threadSummariesProperty())"));
        assertTrue(threadsController.contains("public TableView<ThreadSummary> table()"));

        assertTrue(javaAppController.contains("package com.youngledo.jmcfx.ui.javaapp;"));
        assertTrue(javaAppController.contains("public final class JavaApplicationDataPagesController"));
        assertTrue(javaAppController.contains("view.threadHistogramTitleLabel().textProperty().bind(i18n.text(\"threadHistogram.title\"))"));
        assertTrue(javaAppController.contains("view.securityTitleLabel().textProperty().bind(i18n.text(\"security.title\"))"));
        assertTrue(javaAppController.contains("view.nativeLibrariesTitleLabel().textProperty().bind(i18n.text(\"nativeLibraries.title\"))"));
        assertTrue(javaAppController.contains("view.threadDumpsTitleLabel().textProperty().bind(i18n.text(\"threadDumps.title\"))"));
        assertTrue(javaAppController.contains("currentThreadHistogramViewModel.chartProperty().removeListener(threadHistogramChartListener)"));
        assertTrue(javaAppController.contains("view.threadDumpsTable().getSelectionModel().selectedItemProperty().addListener"));
        assertTrue(javaAppController.contains("public List<TableView<?>> exportTables()"));
    }

    @Test
    void appShellDelegatesIoAndLocksPagesToDomainPackagedControllers() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String registry = shellPageControllerRegistrySource();
        String selection = workspaceSelectionSource();
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String fileIoController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/fileio/FileIoPageController.java"));
        String fileIoView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/fileio/FileIoPageView.java"));
        String socketIoController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/socketio/SocketIoPageController.java"));
        String socketIoView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/socketio/SocketIoPageView.java"));
        String locksController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/locks/LocksPageController.java"));
        String locksView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/locks/LocksPageView.java"));

        assertTrue(registry.contains("private FileIoPageController fileIoPageController;"));
        assertTrue(registry.contains("private SocketIoPageController socketIoPageController;"));
        assertTrue(registry.contains("private LocksPageController locksPageController;"));
        assertTrue(registry.contains("fileIoPageController = new FileIoPageController(view.fileIoPage(), i18n);"));
        assertTrue(registry.contains("socketIoPageController = new SocketIoPageController(view.socketIoPage(), i18n);"));
        assertTrue(registry.contains("locksPageController = new LocksPageController(view.locksPage(), i18n);"));
        assertTrue(selection.contains("pages.fileIoPageController().bind(workspace == null ? null : workspace.fileIOViewModel())"));
        assertTrue(selection.contains("pages.socketIoPageController().bind(workspace == null ? null : workspace.socketIOViewModel())"));
        assertTrue(selection.contains("pages.locksPageController().bind(workspace == null ? null : workspace.lockViewModel())"));
        assertTrue(registry.contains("fileIoPageController.exportTables().forEach(installer::install)"));
        assertTrue(registry.contains("socketIoPageController.exportTables().forEach(installer::install)"));
        assertTrue(registry.contains("locksPageController.exportTables().forEach(installer::install)"));

        assertFalse(shell.contains("private FileIOViewModel fileIOViewModel;"));
        assertFalse(shell.contains("private SocketIOViewModel socketIOViewModel;"));
        assertFalse(shell.contains("private LockViewModel lockViewModel;"));
        assertFalse(shell.contains("private TableView<FileIOHistogram> fileioHistogramTable;"));
        assertFalse(shell.contains("private TableView<FileIOEvent> fileioEventTable;"));
        assertFalse(shell.contains("private TableView<SocketIOHistogram> socketioHistogramTable;"));
        assertFalse(shell.contains("private TableView<SocketIOEvent> socketioEventTable;"));
        assertFalse(shell.contains("private TableView<LockHistogram> locksByClassTable;"));
        assertFalse(shell.contains("private TableView<LockHistogram> locksByAddressTable;"));
        assertFalse(shell.contains("private TableView<LockHistogram> locksByThreadTable;"));
        assertFalse(shell.contains("private void configureFileIOTable()"));
        assertFalse(shell.contains("private void configureSocketIOTable()"));
        assertFalse(shell.contains("private void configureLockTables()"));
        assertFalse(shell.contains("private void configureSingleLockTable("));
        assertFalse(shell.contains("private void bindFileIO("));
        assertFalse(shell.contains("private void bindSocketIO("));
        assertFalse(shell.contains("private void bindLocks("));
        assertFalse(shell.contains("private void setSocketIOGrouping("));

        assertTrue(appShellView.contains("FileIoPageView fileIoPage()"));
        assertTrue(appShellView.contains("SocketIoPageView socketIoPage()"));
        assertTrue(appShellView.contains("LocksPageView locksPage()"));
        assertTrue(fileIoView.contains("public record FileIoPageView("));
        assertTrue(socketIoView.contains("public record SocketIoPageView("));
        assertTrue(locksView.contains("public record LocksPageView("));

        assertTrue(fileIoController.contains("package com.youngledo.jmcfx.ui.fileio;"));
        assertTrue(fileIoController.contains("public final class FileIoPageController"));
        assertTrue(fileIoController.contains("view.titleLabel().textProperty().bind(i18n.text(\"fileio.title\"))"));
        assertTrue(fileIoController.contains("view.timelineTab().textProperty().bind(i18n.text(\"fileio.tab.timeline\"))"));
        assertTrue(fileIoController.contains("view.histogramTable().setItems(nextViewModel.histogramProperty())"));
        assertTrue(fileIoController.contains("currentViewModel.timelineProperty().removeListener(timelineListener)"));
        assertTrue(fileIoController.contains("public List<TableView<?>> exportTables()"));

        assertTrue(socketIoController.contains("package com.youngledo.jmcfx.ui.socketio;"));
        assertTrue(socketIoController.contains("public final class SocketIoPageController"));
        assertTrue(socketIoController.contains("view.groupByHostAndPortButton().setOnAction(event -> setGrouping(SocketIOGrouping.BY_HOST_AND_PORT))"));
        assertTrue(socketIoController.contains("view.histogramTable().setItems(nextViewModel.histogramProperty())"));
        assertTrue(socketIoController.contains("currentViewModel.timelineProperty().removeListener(timelineListener)"));
        assertTrue(socketIoController.contains("public List<TableView<?>> exportTables()"));

        assertTrue(locksController.contains("package com.youngledo.jmcfx.ui.locks;"));
        assertTrue(locksController.contains("public final class LocksPageController"));
        assertTrue(locksController.contains("view.groupByClassButton().setOnAction(event -> setPrimaryGrouping(LockGrouping.BY_CLASS))"));
        assertTrue(locksController.contains("view.byClassTable().setItems(nextViewModel.classHistogramProperty())"));
        assertTrue(locksController.contains("public List<TableView<?>> exportTables()"));
    }

    @Test
    void appShellDelegatesMemoryPagesToDomainPackagedControllers() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String registry = shellPageControllerRegistrySource();
        String selection = workspaceSelectionSource();
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String heapController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/heap/HeapPageController.java"));
        String heapView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/heap/HeapPageView.java"));
        String leaksController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/leaks/LeakSuspectsPageController.java"));
        String leaksView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/leaks/LeakSuspectsPageView.java"));
        String tlabController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/tlab/TlabPageController.java"));
        String tlabView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/tlab/TlabPageView.java"));

        assertTrue(registry.contains("private HeapPageController heapPageController;"));
        assertTrue(registry.contains("private LeakSuspectsPageController leakSuspectsPageController;"));
        assertTrue(registry.contains("private TlabPageController tlabPageController;"));
        assertTrue(registry.contains("heapPageController = new HeapPageController(view.heapPage(), i18n);"));
        assertTrue(registry.contains("leakSuspectsPageController = new LeakSuspectsPageController(view.leakSuspectsPage(), i18n);"));
        assertTrue(registry.contains("tlabPageController = new TlabPageController(view.tlabPage(), i18n);"));
        assertTrue(selection.contains("pages.heapPageController().bind(workspace == null ? null : workspace.heapViewModel())"));
        assertTrue(selection.contains("pages.leakSuspectsPageController().bind(workspace == null ? null : workspace.leakSuspectsViewModel())"));
        assertTrue(selection.contains("pages.tlabPageController().bind(workspace == null ? null : workspace.tlabViewModel())"));
        assertTrue(registry.contains("installer.install(heapPageController.table())"));
        assertTrue(registry.contains("installer.install(leakSuspectsPageController.table())"));
        assertTrue(registry.contains("installer.install(tlabPageController.table())"));

        assertFalse(shell.contains("private HeapViewModel heapViewModel;"));
        assertFalse(shell.contains("private LeakSuspectsViewModel leaksViewModel;"));
        assertFalse(shell.contains("private TlabViewModel tlabViewModel;"));
        assertFalse(shell.contains("private TableView<HeapClassHistogram> heapTable;"));
        assertFalse(shell.contains("private TableView<LeakCandidate> leaksTable;"));
        assertFalse(shell.contains("private TableView<TlabAllocation> tlabTable;"));
        assertFalse(shell.contains("private TreeView<LeakReferenceNode> leaksReferenceTree;"));
        assertFalse(shell.contains("private void configureHeapTable()"));
        assertFalse(shell.contains("private void configureLeaksTable()"));
        assertFalse(shell.contains("private void configureTlabTable()"));
        assertFalse(shell.contains("private void bindHeap("));
        assertFalse(shell.contains("private void bindLeaks("));
        assertFalse(shell.contains("private void bindTlab("));
        assertFalse(shell.contains("private void updateTlabTablePlaceholder("));
        assertFalse(shell.contains("private void updateLeakReferenceTree("));
        assertFalse(shell.contains("private TreeItem<LeakReferenceNode> buildReferenceTreeItem("));

        assertTrue(appShellView.contains("HeapPageView heapPage()"));
        assertTrue(appShellView.contains("LeakSuspectsPageView leakSuspectsPage()"));
        assertTrue(appShellView.contains("TlabPageView tlabPage()"));
        assertTrue(heapView.contains("public record HeapPageView("));
        assertTrue(leaksView.contains("public record LeakSuspectsPageView("));
        assertTrue(tlabView.contains("public record TlabPageView("));

        assertTrue(heapController.contains("package com.youngledo.jmcfx.ui.heap;"));
        assertTrue(heapController.contains("public final class HeapPageController"));
        assertTrue(heapController.contains("view.titleLabel().textProperty().bind(i18n.text(\"heap.title\"))"));
        assertTrue(heapController.contains("view.table().setItems(nextViewModel.histogramProperty())"));
        assertTrue(heapController.contains("currentViewModel.timelineProperty().removeListener(timelineListener)"));
        assertTrue(heapController.contains("public TableView<HeapClassHistogram> table()"));

        assertTrue(leaksController.contains("package com.youngledo.jmcfx.ui.leaks;"));
        assertTrue(leaksController.contains("public final class LeakSuspectsPageController"));
        assertTrue(leaksController.contains("view.titleLabel().textProperty().bind(i18n.text(\"leaks.title\"))"));
        assertTrue(leaksController.contains("view.table().setItems(nextViewModel.candidatesProperty())"));
        assertTrue(leaksController.contains("currentViewModel.referenceTreeProperty().removeListener(referenceTreeListener)"));
        assertTrue(leaksController.contains("viewModel.selectCandidate(idx)"));
        assertTrue(leaksController.contains("private TreeItem<LeakReferenceNode> buildReferenceTreeItem("));

        assertTrue(tlabController.contains("package com.youngledo.jmcfx.ui.tlab;"));
        assertTrue(tlabController.contains("public final class TlabPageController"));
        assertTrue(tlabController.contains("view.titleLabel().textProperty().bind(i18n.text(\"tlab.title\"))"));
        assertTrue(tlabController.contains("view.table().setItems(nextViewModel.allocationsProperty())"));
        assertTrue(tlabController.contains("currentViewModel.timelineProperty().removeListener(timelineListener)"));
        assertTrue(tlabController.contains("currentViewModel.loadingProperty().removeListener(placeholderListener)"));
        assertTrue(tlabController.contains("currentViewModel.loadedProperty().removeListener(placeholderListener)"));
        assertTrue(tlabController.contains("public TableView<TlabAllocation> table()"));
    }

    @Test
    void appShellDelegatesJvmInternalsPagesToDomainPackagedControllers() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String registry = shellPageControllerRegistrySource();
        String selection = workspaceSelectionSource();
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String jvmController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/jvm/JvmInternalsPagesController.java"));
        String jvmView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/jvm/JvmInternalsPagesView.java"));
        String g1Controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/gc/G1GcPageController.java"));
        String g1View = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/gc/G1GcPageView.java"));
        String javaFxController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/jfx/JavaFxEventsPageController.java"));
        String javaFxView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/jfx/JavaFxEventsPageView.java"));

        assertTrue(registry.contains("private JvmInternalsPagesController jvmInternalsPagesController;"));
        assertTrue(registry.contains("private G1GcPageController g1GcPageController;"));
        assertTrue(registry.contains("private JavaFxEventsPageController javaFxEventsPageController;"));
        assertTrue(registry.contains("jvmInternalsPagesController = new JvmInternalsPagesController(view.jvmInternalsPages(), i18n);"));
        assertTrue(registry.contains("g1GcPageController = new G1GcPageController(view.g1GcPage(), i18n);"));
        assertTrue(registry.contains("javaFxEventsPageController = new JavaFxEventsPageController(view.javaFxEventsPage(), i18n);"));
        assertTrue(selection.contains("pages.jvmInternalsPagesController().bindJvmInfo(workspace == null ? null : workspace.jvmInfoViewModel())"));
        assertTrue(selection.contains("pages.jvmInternalsPagesController().bindGcConfig(workspace == null ? null : workspace.gcConfigViewModel())"));
        assertTrue(selection.contains("pages.jvmInternalsPagesController().bindGcSummary(workspace == null ? null : workspace.gcSummaryViewModel())"));
        assertTrue(selection.contains("pages.jvmInternalsPagesController().bindGcDetails(workspace == null ? null : workspace.gcDetailsViewModel())"));
        assertTrue(selection.contains("pages.g1GcPageController().bind(workspace == null ? null : workspace.g1GcViewModel())"));
        assertTrue(selection.contains("pages.javaFxEventsPageController().bind(workspace == null ? null : workspace.javaFxEventsViewModel())"));
        assertTrue(selection.contains("pages.jvmInternalsPagesController().bindCompilations(workspace == null ? null : workspace.compilationsViewModel())"));
        assertTrue(selection.contains("pages.jvmInternalsPagesController().bindCodeCache(workspace == null ? null : workspace.codeCacheViewModel())"));
        assertTrue(selection.contains("pages.jvmInternalsPagesController().bindClassLoading(workspace == null ? null : workspace.classLoadingViewModel())"));
        assertTrue(selection.contains("pages.jvmInternalsPagesController().bindVmOperations(workspace == null ? null : workspace.vmOperationsViewModel())"));
        assertTrue(registry.contains("jvmInternalsPagesController.exportTables().forEach(installer::install)"));
        assertTrue(registry.contains("g1GcPageController.exportTables().forEach(installer::install)"));
        assertTrue(registry.contains("javaFxEventsPageController.exportTables().forEach(installer::install)"));

        assertFalse(shell.contains("private TableView<JvmFlag> jvmFlagsTable;"));
        assertFalse(shell.contains("private G1GcViewModel g1GcViewModel;"));
        assertFalse(shell.contains("private JavaFxEventsViewModel javaFxEventsViewModel;"));
        assertFalse(shell.contains("private JvmInfoViewModel jvmInfoViewModel;"));
        assertFalse(shell.contains("private void configureJvmFlagsTable()"));
        assertFalse(shell.contains("private void configureG1GcTables()"));
        assertFalse(shell.contains("private void configureJavaFxEventsTables()"));
        assertFalse(shell.contains("private void bindJvmInfo("));
        assertFalse(shell.contains("private void bindG1Gc("));
        assertFalse(shell.contains("private void bindJavaFxEvents("));
        assertFalse(shell.contains("private void bindCodeCache("));

        assertTrue(appShellView.contains("JvmInternalsPagesView jvmInternalsPages()"));
        assertTrue(appShellView.contains("G1GcPageView g1GcPage()"));
        assertTrue(appShellView.contains("JavaFxEventsPageView javaFxEventsPage()"));
        assertTrue(jvmView.contains("public record JvmInternalsPagesView("));
        assertTrue(g1View.contains("public record G1GcPageView("));
        assertTrue(javaFxView.contains("public record JavaFxEventsPageView("));

        assertTrue(jvmController.contains("package com.youngledo.jmcfx.ui.jvm;"));
        assertTrue(jvmController.contains("public final class JvmInternalsPagesController"));
        assertTrue(jvmController.contains("public void bindJvmInfo(JvmInfoViewModel nextViewModel)"));
        assertTrue(jvmController.contains("public void bindGcDetails(GcDetailsViewModel nextViewModel)"));
        assertTrue(jvmController.contains("currentViewModel.heapChartProperty().removeListener(heapChartListener)"));
        assertTrue(jvmController.contains("public List<TableView<?>> exportTables()"));

        assertTrue(g1Controller.contains("package com.youngledo.jmcfx.ui.gc;"));
        assertTrue(g1Controller.contains("public final class G1GcPageController"));
        assertTrue(g1Controller.contains("currentViewModel.selectedRegionStateProperty().removeListener(selectedRegionStateListener)"));
        assertTrue(g1Controller.contains("view.regionStatesTable().getSelectionModel().selectedItemProperty()"));
        assertTrue(g1Controller.contains("public List<TableView<?>> exportTables()"));

        assertTrue(javaFxController.contains("package com.youngledo.jmcfx.ui.jfx;"));
        assertTrue(javaFxController.contains("public final class JavaFxEventsPageController"));
        assertTrue(javaFxController.contains("currentViewModel.selectedPulsePhaseProperty().removeListener(selectedPulsePhaseListener)"));
        assertTrue(javaFxController.contains("view.phaseTable().getSelectionModel().selectedItemProperty()"));
        assertTrue(javaFxController.contains("public List<TableView<?>> exportTables()"));
    }

    @Test
    void appShellDelegatesEnvironmentPagesToDomainPackagedController() throws Exception {
        String shell = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String registry = shellPageControllerRegistrySource();
        String selection = workspaceSelectionSource();
        String appShellView = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));
        String environmentController = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/youngledo/jmcfx/ui/environment/EnvironmentPagesController.java"));
        String environmentView = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/youngledo/jmcfx/ui/environment/EnvironmentPagesView.java"));

        assertTrue(registry.contains("private EnvironmentPagesController environmentPagesController;"));
        assertTrue(registry.contains("environmentPagesController = new EnvironmentPagesController(view.environmentPages(), i18n);"));
        assertTrue(registry.contains("environmentPagesController.configure();"));
        assertTrue(selection.contains("pages.environmentPagesController().bind(workspace == null ? null : workspace.environmentViewModel())"));
        assertTrue(registry.contains("environmentPagesController.exportTables().forEach(installer::install);"));
        assertTrue(appShellView.contains("EnvironmentPagesView environmentPages()"));

        assertFalse(shell.contains("private EnvironmentViewModel environmentViewModel;"));
        assertFalse(shell.contains("private TableView<ProcessInfo> processesTable;"));
        assertFalse(shell.contains("private TableView<EnvironmentVariable> envVarsTable;"));
        assertFalse(shell.contains("private TableView<SystemProperty> sysPropsTable;"));
        assertFalse(shell.contains("private TableView<ActiveRecordingInfo> recordingsTable;"));
        assertFalse(shell.contains("private TableView<ActiveSetting> settingsTable;"));
        assertFalse(shell.contains("private TableView<AgentInfo> agentsTable;"));
        assertFalse(shell.contains("private TableView<ConstantPoolType> constantPoolsTable;"));
        assertFalse(shell.contains("private TextField envVarsSearchField;"));
        assertFalse(shell.contains("private TextField sysPropsSearchField;"));
        assertFalse(shell.contains("private Label processesTitleLabel;"));
        assertFalse(shell.contains("private Label constantPoolsTitleLabel;"));
        assertFalse(shell.contains("private void configureProcessesTable()"));
        assertFalse(shell.contains("private void configureEnvVarsTable()"));
        assertFalse(shell.contains("private void configureSysPropsTable()"));
        assertFalse(shell.contains("private void configureRecordingsTable()"));
        assertFalse(shell.contains("private void configureSettingsTable()"));
        assertFalse(shell.contains("private void configureAgentsTable()"));
        assertFalse(shell.contains("private void configureConstantPoolsTable()"));
        assertFalse(shell.contains("private void bindEnvironment("));

        assertTrue(environmentController.contains("package com.youngledo.jmcfx.ui.environment;"));
        assertTrue(environmentController.contains("public final class EnvironmentPagesController"));
        assertTrue(environmentController.contains("public void configure()"));
        assertTrue(environmentController.contains("public void bind(EnvironmentViewModel nextViewModel)"));
        assertTrue(environmentController.contains("view.envVarsSearchField().textProperty().addListener"));
        assertTrue(environmentController.contains("view.sysPropsSearchField().textProperty().addListener"));
        assertTrue(environmentController.contains("public List<TableView<?>> exportTables()"));
        assertTrue(environmentView.contains("public record EnvironmentPagesView("));
    }

    @Test
    void jvmDisconnectButtonRequiresConnectedSelection() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java"));

        assertTrue(source.contains("selectedConnection.connected"),
                "Disconnect must be disabled for discovered but unconnected JVM rows");
        assertFalse(source.contains("Bindings.isNull(jvmBrowserViewModel.selectedConnectionProperty())"),
                "Selected discovered rows must not be enough to enable Disconnect");
    }

    @Test
    void jvmBrowserDoesNotPersistDiscoveryCountInBottomStatus() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/jvms/JvmBrowserViewModel.java"));

        assertFalse(source.contains("Discovered \" + discovered.size()"),
                "The table already displays discovery results; bottom status should stay for transient state and errors");
    }

    @Test
    void jvmBrowserUsesPidJavaVersionAndNoUrlColumn() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java"));

        assertTrue(source.contains("jvms.column.pid"), "JVM table should include a PID column");
        assertTrue(source.contains("cell.getValue().pid()"), "PID column must read JvmConnection.pid()");
        assertTrue(source.contains("jvms.column.javaVersion"), "JVM table should include Java version");
        assertTrue(source.contains("cell.getValue().javaVersion()"), "Java version column must read metadata");
        assertFalse(source.contains("jvms.column.url"), "Connection URL column should be removed from JVM table");
        assertFalse(source.contains("cell.getValue().connectionUrl()"),
                "Connection URL should not be shown as a primary JVM Browser column");
    }

    @Test
    void jvmBrowserRefreshesWhenOpenedAndManuallyWithoutPeriodicTimer() throws Exception {
        String liveController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java"));
        String liveJvmWorkspace = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/ShellLiveJvmWorkspaceController.java"));

        assertTrue(liveController.contains("jvmsRefreshButton.setOnAction(event -> refresh())"),
                "Manual refresh button should trigger JVM Browser refresh");
        assertTrue(liveJvmWorkspace.contains("selectedSectionProperty().addListener"),
                "Opening JVM Browser should refresh the local JVM list");
        assertTrue(liveJvmWorkspace.contains("\"jvms\".equals(newValue) && jvmsPaneController != null"),
                "JVM Browser should refresh when the JVMs section is opened");
        assertTrue(liveJvmWorkspace.contains("jvmsPaneController.refresh()"),
                "Shell should delegate JVM refresh to the included Live JVM controller");
        assertFalse(liveController.contains("JVM_BROWSER_REFRESH_INTERVAL_SECONDS"),
                "JVM Browser should not run periodic refresh");
        assertFalse(liveController.contains("startJvmBrowserRefresh"),
                "JVM Browser should not use timer-based refresh");
        assertFalse(liveController.contains("jvmBrowserFirstRefreshDone"),
                "JVM Browser should refresh every time the section opens");
    }

    @Test
    void jvmBrowserEmptyPlaceholderWaitsForCompletedRefresh() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java"));

        assertTrue(source.contains("jvmsTable.setPlaceholder(emptyTablePlaceholder())"),
                "JVM Browser should not show no-data text before discovery finishes");
        assertTrue(source.contains("refreshCompletedProperty()"),
                "JVM Browser placeholder should depend on completed discovery");
        assertTrue(source.contains("localizedTablePlaceholder(\"jvms.empty\")"),
                "No-data text should appear only after completed empty discovery");
    }

@Test
void jvmBrowserSupportsDoubleClickConnectAndNoBottomStatus() throws Exception {
    String controller = java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java"));
    String shellView = java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellView.java"));

    assertTrue(controller.contains("setOnMouseClicked"), "JVM table should handle double-click connect");
    assertTrue(controller.contains("connectSelectedOrManual()"),
            "Connect button should preserve manual URL priority");
    assertTrue(controller.contains("jvmBrowserViewModel.connectSelected()"),
            "Double-click should connect only the selected local JVM");
    assertFalse(shellView.contains("jvmsStatusLabel"), "Bottom JVM status label should be removed");
    assertFalse(controller.contains("jvmsStatusLabel.textProperty()"),
            "Controller should not bind a removed bottom status label");
}

    @Test
    void jvmBrowserShellExposesSavedTargetsJdpAndSelectedStatus() throws Exception {
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java"));
        String english = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages.properties"));
        String chinese = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages_zh_CN.properties"));

        assertTrue(controller.contains("private TextField jvmsManualNameField;"));
        assertTrue(controller.contains("private Label jvmsManualUrlHintLabel;"));
        assertTrue(controller.contains("private Button jvmsSaveTargetButton;"));
        assertTrue(controller.contains("private Button jvmsRemoveSavedTargetButton;"));
        assertTrue(controller.contains("private Button jvmsRefreshJdpButton;"));
        assertTrue(controller.contains("private Label jvmsSelectedConnectionStatusLabel;"));
        assertTrue(controller.contains("jvmsManualNameField.textProperty().bindBidirectional("));
        assertTrue(controller.contains("jvmBrowserViewModel.manualConnectionNameProperty()"));
        assertTrue(controller.contains("jvmsManualUrlHintLabel.textProperty().bind(i18n.text(\"jvms.manualUrlHint\"))"));
        assertTrue(controller.contains("jvmsRefreshJdpButton.setOnAction(event -> jvmBrowserViewModel.refreshJdp())"));
        assertTrue(controller.contains("jvmsSaveTargetButton.setOnAction(event -> jvmBrowserViewModel.saveManualTarget())"));
        assertTrue(controller.contains("jvmsRemoveSavedTargetButton.setOnAction(event ->"));
        assertTrue(controller.contains("jvmBrowserViewModel.removeSelectedSavedTarget()"));
        assertTrue(controller.contains("jvmsRefreshJdpButton.disableProperty().bind("));
        assertTrue(controller.contains("jvmBrowserViewModel.loadingProperty().or(jvmBrowserViewModel.jdpRefreshInProgressProperty())"));
        assertTrue(controller.contains("jvmsSaveTargetButton.disableProperty().bind(jvmBrowserViewModel.loadingProperty()"));
        assertTrue(controller.contains("jvmBrowserViewModel.manualConnectionUrlProperty().get().trim().isEmpty()"));
        assertTrue(controller.contains("jvmsRemoveSavedTargetButton.disableProperty().bind(jvmBrowserViewModel.loadingProperty()"));
        assertTrue(controller.contains("selected == null || selected.source() != JvmConnectionSource.SAVED"));
        assertTrue(controller.contains("selected.connected()"));
        assertTrue(controller.contains("jvmsSelectedConnectionStatusLabel.textProperty().bind("));
        assertTrue(controller.contains("selectedConnectionStatusText("));
        assertTrue(controller.contains("jvmBrowserViewModel.jdpStatusMessageProperty()"));
        assertTrue(controller.contains("jvmsManualNameField.setDisable(true);"));
        assertTrue(controller.contains("jvmsManualUrlHintLabel.setDisable(true);"));
        assertTrue(controller.contains("jvmsSaveTargetButton.setDisable(true);"));
        assertTrue(controller.contains("jvmsRemoveSavedTargetButton.setDisable(true);"));
        assertTrue(controller.contains("jvmsRefreshJdpButton.setDisable(true);"));
        assertTrue(controller.contains("jvmsSelectedConnectionStatusLabel.textProperty().bind(i18n.text(\"jvms.jdp.status.idle\"))"));

        assertJvmBrowserJdpI18n(english, "jvms.manualUrlPrompt=RMI or Jolokia JMX service URL",
                "jvms.manualUrlHint=Jolokia example: service:jmx:jolokia://localhost:8778/jolokia",
                "jvms.manualNamePrompt=Display name",
                "jvms.saveTarget=Save Target",
                "jvms.removeSavedTarget=Remove Saved",
                "jvms.refreshJdp=Refresh JDP",
                "jvms.jdp.status.idle=JDP discovery is idle.",
                "jvms.jdp.status.refreshing=Refreshing JDP advertisements...",
                "jvms.jdp.status.none=No JDP advertisements found.",
                "jvms.jdp.status.found=Found {0} JDP advertisement(s).",
                "jvms.jdp.status.unconfigured=JDP discovery is not configured.",
                "jvms.jdp.status.failed=JDP discovery failed: {0}",
                "jvms.status.attachableLocal=Attachable local JVM.",
                "jvms.status.localUnavailable=Local JVM is not attachable.",
                "jvms.status.saved=Saved JMX target.",
                "jvms.status.jdp=Discovered through JDP.",
                "jvms.status.connected=Connected.");
        assertJvmBrowserJdpI18n(chinese, "jvms.manualUrlPrompt=RMI 或 Jolokia JMX service URL",
                "jvms.manualUrlHint=Jolokia 示例：service:jmx:jolokia://localhost:8778/jolokia",
                "jvms.manualNamePrompt=显示名称",
                "jvms.saveTarget=保存目标",
                "jvms.removeSavedTarget=移除保存项",
                "jvms.refreshJdp=刷新JDP",
                "jvms.jdp.status.idle=JDP发现空闲。",
                "jvms.jdp.status.refreshing=正在刷新JDP广播...",
                "jvms.jdp.status.none=未发现JDP广播。",
                "jvms.jdp.status.found=发现{0}个JDP广播。",
                "jvms.jdp.status.unconfigured=JDP 发现未配置。",
                "jvms.jdp.status.failed=JDP 发现失败：{0}",
                "jvms.status.attachableLocal=可连接的本地 JVM。",
                "jvms.status.localUnavailable=本地 JVM 不可连接。",
                "jvms.status.saved=已保存 JVM 目标。",
                "jvms.status.jdp=通过 JDP 发现。",
                "jvms.status.connected=已连接。");
    }

    @Test
    void jvmBrowserFormatsStateAndSourceThroughI18n() throws Exception {
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java"));

        assertTrue(controller.contains("formatJvmState"), "State column should use localized display labels");
        assertTrue(controller.contains("formatJvmSource"), "Source column should use localized display labels");
        assertFalse(controller.contains("state().name()"), "State column must not show raw enum names");
        assertFalse(controller.contains("source().name()"), "Source column must not show raw enum names");
    }

    @Test
    void jvmBrowserStatusTextIsLocalizedWithoutParsingEnglishDisplayText() {
        I18n i18n = new I18n(Locale.SIMPLIFIED_CHINESE);
        i18n.setLanguageMode(LanguageMode.CHINESE_SIMPLIFIED);
        LiveJvmPaneController controller = new LiveJvmPaneController(i18n);

        assertEquals("已保存 JVM 目标。", controller.selectedConnectionStatusText(
                JvmConnection.saved(new SavedJvmTarget("saved-1", "prod",
                        "service:jmx:rmi:///jndi/rmi://localhost:9010/jmxrmi", null)),
                ""));
        assertEquals("通过 JDP 发现。", controller.selectedConnectionStatusText(
                JvmConnection.jdp(new JdpJvmAdvertisement("jdp-1", "demo.Main",
                        "service:jmx:rmi:///jndi/rmi://localhost:9011/jmxrmi", "localhost", 9011,
                        "26.0.1")),
                ""));
        assertEquals("已连接。", controller.selectedConnectionStatusText(
                JvmConnection.saved(new SavedJvmTarget("saved-2", "dev",
                        "service:jmx:rmi:///jndi/rmi://localhost:9012/jmxrmi", null))
                        .asConnected("service:jmx:rmi:///jndi/rmi://localhost:9012/jmxrmi"),
                ""));
        assertEquals("JDP 发现未配置。", controller.selectedConnectionStatusText(null,
                "JDP discovery is not configured."));
        assertEquals("JDP 发现失败：network down", controller.selectedConnectionStatusText(null,
                "JDP discovery failed: network down"));
    }

    @Test
    void jvmBrowserBindsLiveSessionDetail() throws Exception {
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java"));

        assertTrue(controller.contains("jvmsSessionDetailPane.visibleProperty()"),
                "JVM session detail pane should be visible only for selected connected sessions");
        assertTrue(controller.contains("selectedSessionProperty()"),
                "JVM session detail should bind to the ViewModel session snapshot");
        assertTrue(controller.contains("jvmsCapabilitiesList.setItems"),
                "JVM capabilities should be shown in the detail list");
        assertTrue(controller.contains("formatJvmCapability"),
                "Capabilities should be formatted through a controller helper");
    }

    @Test
    void jvmBrowserBindsFlightRecordingControl() throws Exception {
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java"));

        assertTrue(controller.contains("jvmsRecordingsTable.setItems(jvmBrowserViewModel.flightRecordingsProperty())"),
                "JVM recordings table should bind to live recording rows");
        assertTrue(controller.contains("jvmsStartRecordingButton.setOnAction"),
                "Start recording button should invoke the JVM browser ViewModel");
        assertTrue(controller.contains("jvmsStopRecordingButton.setOnAction"),
                "Stop recording button should save and open a JFR file");
        String shellController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String shellRuntime = shellRuntimeSource();
        assertTrue(shellRuntime.contains("this::openRecordingInBackground"),
                "Saved recordings should reuse the existing shell recording open flow through the ViewModel callback");
    }

    @Test
    void jvmBrowserBindsMBeanBrowserControls() throws Exception {
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java"));

        assertTrue(controller.contains("jvmsMBeanTree.setRoot"),
                "MBean tree should be rebuilt from the ViewModel tree root");
        assertTrue(controller.contains("selectedMBeanProperty()"),
                "MBean tree selection should update the selected MBean property");
        assertTrue(controller.contains("jvmsMBeanAttributesTable.setItems"),
                "MBean attributes table should bind to ViewModel attributes");
        assertTrue(controller.contains("jvmsMBeanOperationsTable.setItems"),
                "MBean operations table should bind to ViewModel operations");
        assertTrue(controller.contains("invokeSelectedMBeanOperation"),
                "MBean invoke button should delegate to the ViewModel");
    }

    @Test
    void appShellFactoryInjectsMBeanBrowserService() throws Exception {
        String factory = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellFactory.java"));
        String liveJvmServices = java.nio.file.Files.readString(
                java.nio.file.Path.of("../jmc-fx-application/src/main/java/com/youngledo/jmcfx/application/LiveJvmApplicationServices.java"));
        String liveJvmController = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/youngledo/jmcfx/ui/shell/ShellLiveJvmWorkspaceController.java"));
        String assembly = java.nio.file.Files.readString(java.nio.file.Path.of(
                "../jmc-fx-launcher/src/main/java/com/youngledo/jmcfx/launcher/JmcFxLauncherServicesFactory.java"));

        assertTrue(liveJvmServices.contains("MBeanBrowserService mBeanBrowserService"),
                "Live JVM service bundle should carry the MBean Browser port");
        assertTrue(liveJvmController.contains("services.mBeanBrowserService()"),
                "Controller should pass the MBean Browser port to JvmBrowserViewModel");
        assertTrue(assembly.contains("new JmcMBeanBrowserService(jmxConnectionService)"),
                "Application assembly should reuse the existing JMX connection service for MBeans");
    }

    @Test
    void appShellFactoryInjectsLiveDiagnosticsServices() throws Exception {
        String factory = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellFactory.java"));
        String liveJvmServices = java.nio.file.Files.readString(
                java.nio.file.Path.of("../jmc-fx-application/src/main/java/com/youngledo/jmcfx/application/LiveJvmApplicationServices.java"));
        String liveJvmController = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/youngledo/jmcfx/ui/shell/ShellLiveJvmWorkspaceController.java"));
        String assembly = java.nio.file.Files.readString(java.nio.file.Path.of(
                "../jmc-fx-launcher/src/main/java/com/youngledo/jmcfx/launcher/JmcFxLauncherServicesFactory.java"));

        assertTrue(liveJvmServices.contains("DiagnosticCommandService diagnosticCommandService"),
                "Live JVM service bundle should carry the Diagnostic Command port");
        assertTrue(liveJvmServices.contains("LiveMetricService liveMetricService"),
                "Live JVM service bundle should carry the Live Metric port");
        assertTrue(liveJvmController.contains("services.diagnosticCommandService()"));
        assertTrue(liveJvmController.contains("services.liveMetricService()"),
                "Controller should pass diagnostics ports to JvmBrowserViewModel");
        assertTrue(assembly.contains("new JmcDiagnosticCommandService(jmxConnectionService)"),
                "Application assembly should reuse the existing JMX connection service for diagnostic commands");
        assertTrue(assembly.contains("new JmcLiveMetricService(jmxConnectionService)"),
                "Application assembly should reuse the existing JMX connection service for live metrics");
    }

    @Test
    void jvmBrowserBindsJmcAgentManagerControls() throws Exception {
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java"));
        String english = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages.properties"));
        String chinese = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages_zh_CN.properties"));

        assertTrue(controller.contains("private Tab jvmsAgentTab;"));
        assertTrue(controller.contains("private ComboBox<JmcAgentPreset> jvmsAgentPresetCombo;"));
        assertTrue(controller.contains("private TableView<JmcAgentTransform> jvmsAgentTransformsTable;"));
        assertTrue(controller.contains("private TextArea jvmsAgentConfigurationArea;"));
        assertTrue(controller.contains("private Label jvmsAgentStatusLabel;"));
        assertTrue(controller.contains("configureJmcAgentManager();"));
        assertTrue(controller.contains("bindJmcAgentManager();"));
        assertTrue(controller.contains("jvmsAgentPresetCombo.setItems(jvmBrowserViewModel.jmcAgentPresetsProperty())"));
        assertTrue(controller.contains("jvmsAgentTransformsTable.setItems(jvmBrowserViewModel.jmcAgentTransformsProperty())"));
        assertTrue(controller.contains("jvmsAgentConfigurationArea.textProperty().bindBidirectional("));
        assertTrue(controller.contains("jvmBrowserViewModel.jmcAgentConfigurationProperty()"));
        assertTrue(controller.contains("jvmsRefreshAgentButton.setOnAction(event -> jvmBrowserViewModel.refreshJmcAgent())"));
        assertTrue(controller.contains("jvmsLoadAgentPresetButton.setOnAction(event -> jvmBrowserViewModel.loadSelectedJmcAgentPreset())"));
        assertTrue(controller.contains("jvmsApplyAgentConfigurationButton.setOnAction(event -> jvmBrowserViewModel.applyJmcAgentConfiguration())"));
        assertTrue(controller.contains("jvmsAgentTab.textProperty().bind(i18n.text(\"jvms.agent.tab\"))"));
        assertTrue(controller.contains("localizedColumn(\"jvms.agent.transform.id\")"));

        assertTrue(english.contains("jvms.agent.tab=JMC Agent"));
        assertTrue(english.contains("jvms.agent.refresh=Refresh"));
        assertTrue(english.contains("jvms.agent.loadPreset=Load Preset"));
        assertTrue(english.contains("jvms.agent.apply=Apply Configuration"));
        assertTrue(english.contains("jvms.agent.configuration=Event Probe XML"));
        assertTrue(chinese.contains("jvms.agent.tab=JMC Agent"));
        assertTrue(chinese.contains("jvms.agent.refresh=刷新"));
        assertTrue(chinese.contains("jvms.agent.loadPreset=加载预设"));
        assertTrue(chinese.contains("jvms.agent.apply=应用配置"));
        assertTrue(chinese.contains("jvms.agent.configuration=事件探针 XML"));
    }

    @Test
    void appShellFactoryInjectsAdvancedJfrAnalysisService() throws Exception {
        String factory = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellFactory.java"));
        String recordingServices = java.nio.file.Files.readString(
                java.nio.file.Path.of("../jmc-fx-application/src/main/java/com/youngledo/jmcfx/application/RecordingApplicationServices.java"));
        String workspaceFactory = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/RecordingWorkspaceFactory.java"));
        String assembly = java.nio.file.Files.readString(java.nio.file.Path.of(
                "../jmc-fx-launcher/src/main/java/com/youngledo/jmcfx/launcher/JmcFxLauncherServicesFactory.java"));

        assertTrue(recordingServices.contains("AdvancedJfrAnalysisService advancedJfrAnalysisService"),
                "Recording service bundle should carry the advanced JFR analysis port");
        assertTrue(workspaceFactory.contains("new AdvancedJfrViewModel(services.advancedJfrAnalysisService())"),
                "Recording workspace factory should create the advanced JFR workspace view model when the port is available");
        assertTrue(assembly.contains("new JmcAdvancedJfrAnalysisService()"),
                "Application assembly should use the JMC-backed advanced JFR analysis adapter");
    }

    @Test
    void jvmRuntimeSummaryFormatsUptimeWithSharedDurationFormatter() throws Exception {
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java"));

        assertTrue(controller.contains("DisplayFormats.formatDuration(snapshot.runtime().uptimeMillis())"),
                "JVM runtime summary should use the shared duration formatter for uptime");
        assertFalse(controller.contains("uptime %d ms"),
                "JVM runtime summary must not show raw uptime milliseconds");
    }

    @Test
    void sidebarSearchAndThemeControlsAreEnabled() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppSidebar.java"));

        assertFalse(source.contains("searchButton.setDisable(true)"),
                "sidebar search should be a usable navigation search control");
        assertFalse(source.contains("themeButton"),
                "theme switching belongs in Settings, not the sidebar header");
        assertFalse(source.contains("toggleTheme"),
                "sidebar should not own theme switching");
    }

@Test
void settingsPageContainsThemeSelectorNextToLanguageSelector() {
    AppShellView view = new AppShellView();

    assertEquals(36.0, view.settings.pane.getSpacing());
    assertNotNull(view.settings.languageLabel);
    assertNotNull(view.settings.themeLabel);
    assertEquals(view.settings.languageToggleGroup, view.settings.languageFollowSystemRadio.getToggleGroup());
    assertEquals(view.settings.languageToggleGroup, view.settings.languageEnglishRadio.getToggleGroup());
    assertEquals(view.settings.languageToggleGroup, view.settings.languageChineseRadio.getToggleGroup());
    assertEquals(view.settings.themeToggleGroup, view.settings.themeFollowSystemRadio.getToggleGroup());
    assertEquals(view.settings.themeToggleGroup, view.settings.themeLightRadio.getToggleGroup());
    assertEquals(view.settings.themeToggleGroup, view.settings.themeDarkRadio.getToggleGroup());
}

    @Test
    void controllerBindsThemeSelectorAndSystemThemePreference() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/SettingsPaneController.java"));
        String factory = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellFactory.java"));

        assertTrue(source.contains("configureThemeSelector()"),
                "settings page controller should configure the theme selector");
        assertTrue(source.contains("view.themeToggleGroup.selectedToggleProperty()"),
                "theme selector should update the shell view model");
        assertTrue(source.contains("settings.theme.followSystem"),
                "theme selector should expose follow-system text");
        assertTrue(factory.contains("colorSchemeProperty().addListener"),
                "system theme mode should listen for JavaFX platform color scheme changes");
    }

    @Test
    void eventTimeFormatterDoesNotShowZoneSuffix() {
        assertEquals("1970-01-01 08:00:00.000",
                EventsPageController.formatEventTimeForDisplay(Instant.EPOCH, ZoneId.of("Asia/Shanghai")));
    }

    @Test
    void fileSizeFormatterShowsHumanReadableSizes() {
        assertEquals("0 B", DisplayFormats.formatFileSize(0));
        assertEquals("512 B", DisplayFormats.formatFileSize(512));
        assertEquals("1.0 KB", DisplayFormats.formatFileSize(1024));
        assertEquals("1.5 KB", DisplayFormats.formatFileSize(1536));
        assertEquals("1.0 MB", DisplayFormats.formatFileSize(1024 * 1024));
        assertEquals("1.5 MB", DisplayFormats.formatFileSize(1024 * 1024 + 512 * 1024));
        assertEquals("1.0 GB", DisplayFormats.formatFileSize(1024L * 1024 * 1024));
    }

    @Test
    void integerFormatterUsesThousandsSeparator() {
        assertEquals("0", DisplayFormats.formatInteger(0));
        assertEquals("999", DisplayFormats.formatInteger(999));
        assertEquals("1,000", DisplayFormats.formatInteger(1000));
        assertEquals("1,234,567,890", DisplayFormats.formatInteger(1_234_567_890L));
    }

    @Test
    void durationFormatterShowsHumanReadableDurations() {
        assertEquals("0 ms", DisplayFormats.formatDuration(0));
        assertEquals("500 ms", DisplayFormats.formatDuration(500));
        assertEquals("1.0 s", DisplayFormats.formatDuration(1000));
        assertEquals("1.5 s", DisplayFormats.formatDuration(1500));
        assertEquals("1 min", DisplayFormats.formatDuration(60000));
        assertEquals("1 min 30 s", DisplayFormats.formatDuration(90000));
        assertEquals("2 min 5 s", DisplayFormats.formatDuration(125000));
        assertEquals("1 h", DisplayFormats.formatDuration(3600000));
        assertEquals("1 h 30 min", DisplayFormats.formatDuration(5400000));
    }

    @Test
    void microDurationFormatterShowsHumanReadableDurations() {
        assertEquals("0 us", DisplayFormats.formatMicros(0));
        assertEquals("500 us", DisplayFormats.formatMicros(500));
        assertEquals("1.5 ms", DisplayFormats.formatMicros(1_500));
        assertEquals("1.0 s", DisplayFormats.formatMicros(1_000_000));
    }

    @Test
    void percentFormatterShowsOneDecimalPlace() {
        assertEquals("0.0%", DisplayFormats.formatPercent(0));
        assertEquals("12.3%", DisplayFormats.formatPercent(12.34));
    }

    @Test
    void booleanFormatterUsesYesNoText() {
        assertEquals("Yes", DisplayFormats.formatBoolean(true));
        assertEquals("No", DisplayFormats.formatBoolean(false));
    }

    @Test
    void timestampFormatterUsesApplicationTimeFormat() {
        assertEquals("1970-01-01 08:00:00.000",
                DisplayFormats.formatTimestamp(Instant.EPOCH, ZoneId.of("Asia/Shanghai")));
        assertEquals("", DisplayFormats.formatTimestamp(null, ZoneId.of("Asia/Shanghai")));
    }

    @Test
    void defaultEventTypesPaneWidthIsUsable() {
        assertEquals(260, EventsPageController.DEFAULT_EVENT_TYPES_WIDTH);
    }

    @Test
    void defaultEventTypesDividerKeepsTreePaneNarrow() {
        assertEquals(0.25, EventsPageController.DEFAULT_EVENT_TYPES_DIVIDER_POSITION);
    }

    @Test
    void eventTypesPaneWidthConstraintsAllowUserResizing() {
        assertEquals(180, EventsPageController.MIN_EVENT_TYPES_WIDTH);
        assertEquals(360, EventsPageController.MAX_EVENT_TYPES_WIDTH);
    }

    @Test
    void allEventsSelectionDoesNotSelectEventTypesTreeNode() {
        assertEquals(false, EventsPageController.shouldSelectEventTypesTreeNode(EventTypeSelection.ALL_ID));
        assertEquals(false, EventsPageController.shouldSelectEventTypesTreeNode(""));
        assertEquals(true, EventsPageController.shouldSelectEventTypesTreeNode("jdk.ThreadStart"));
    }

    @Test
    void allEventsSelectionClearsEventTypesTreeSelection() {
        assertEquals(true, EventsPageController.shouldClearEventTypesTreeSelection(EventTypeSelection.ALL_ID));
        assertEquals(true, EventsPageController.shouldClearEventTypesTreeSelection(""));
        assertEquals(true, EventsPageController.shouldClearEventTypesTreeSelection(null));
        assertEquals(false, EventsPageController.shouldClearEventTypesTreeSelection("jdk.ThreadStart"));
    }

    @Test
    void eventTypesDividerInitializesOnlyWhenEventsPaneFirstBecomesVisible() {
        assertEquals(true, EventsPageController.shouldInitializeEventTypesDivider(false, true));
        assertEquals(false, EventsPageController.shouldInitializeEventTypesDivider(false, false));
        assertEquals(false, EventsPageController.shouldInitializeEventTypesDivider(true, true));
    }

    @Test
    void tabTitleUsesWorkspaceRecordingName() {
        RecordingWorkspace workspace = new RecordingWorkspace(
                recording("rec-1", "first-recording.jfr"),
                new OverviewViewModel(),
                new EventBrowserViewModel(new FakeEventQueryService()),
                new RuleResultsViewModel(rec -> List.of()),
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                null);

        assertEquals("first-recording.jfr", WorkspaceTabsController.tabTitleFor(workspace));
    }

    @Test
    void heapDumpTabTitleUsesHeapDumpFileName() {
        HeapDumpWorkspace workspace = new HeapDumpWorkspace(Path.of("/tmp/demo.hprof"), null);

        assertEquals("demo.hprof", WorkspaceTabsController.tabTitleFor(workspace));
    }

    @Test
    void recordingTabsAreShownOnlyWhenWorkspacesExist() {
        assertFalse(WorkspaceTabsController.shouldShowRecordingTabs(0));
        assertTrue(WorkspaceTabsController.shouldShowRecordingTabs(1));
        assertTrue(WorkspaceTabsController.shouldShowRecordingTabs(2));
    }

    @Test
    void workspaceTabsAreShownWhenAnyWorkspaceExists() {
        assertFalse(WorkspaceTabsController.shouldShowWorkspaceTabs(0, 0));
        assertTrue(WorkspaceTabsController.shouldShowWorkspaceTabs(1, 0));
        assertTrue(WorkspaceTabsController.shouldShowWorkspaceTabs(0, 1));
    }

    @Test
    void liveJvmEmptyTablePlaceholderUsesBlankRegionInsteadOfLocalizedText() throws Exception {
        java.lang.reflect.Method method = LiveJvmPaneController.class.getDeclaredMethod("emptyTablePlaceholder");
        method.setAccessible(true);
        Region placeholder = (Region) method.invoke(null);

        assertEquals(Region.class, placeholder.getClass());
        assertFalse(placeholder.isManaged());
    }

    @Test
    void liveJvmTablePlaceholdersUseLocalizedBindings() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java"));

        assertTrue(source.contains("localizedTablePlaceholder("));
        assertFalse(source.contains("setPlaceholder(new Label(i18n.get("),
                "table placeholders should update when the UI language changes");
    }

    @Test
    void tlabPlaceholderDoesNotShowEmptyStateBeforeLazyLoadCompletes() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/tlab/TlabPageController.java"));
        String configureTlabTable = source.substring(source.indexOf("private void configureTable()"),
                source.indexOf("private void updateTablePlaceholder("));

        assertFalse(configureTlabTable.contains("view.table().setPlaceholder(localizedTablePlaceholder(\"tlab.empty\"))"),
                "TLAB is loaded lazily, so its initial placeholder must not say the recording has no TLAB data");
        assertTrue(source.contains("updateTablePlaceholder("),
                "TLAB placeholder must switch to the empty message only after the lazy load completes");
    }

    @Test
    void jvmInternalsTablesUseSharedDisplayFormats() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/jvm/JvmInternalsPagesController.java"));
        String jvmTables = source.substring(source.indexOf("private void configureGcSummaryTable()"),
                source.indexOf("private <T> TableColumn<T, String> localizedColumn("));

        assertFalse(jvmTables.contains("String.valueOf(data.getValue().durationMicros())"));
        assertFalse(jvmTables.contains("String.valueOf(data.getValue().totalDurationMicros())"));
        assertFalse(jvmTables.contains("String.valueOf(data.getValue().heapUsed())"));
        assertFalse(jvmTables.contains("String.valueOf(data.getValue().loadedCount())"));
        assertTrue(jvmTables.contains("DisplayFormats.formatMicros("));
        assertTrue(jvmTables.contains("DisplayFormats.formatFileSize("));
        assertTrue(jvmTables.contains("DisplayFormats.formatInteger("));
        assertTrue(jvmTables.contains("DisplayFormats.formatTimestamp("));
    }

    @Test
    void jvmInternalsTablesBindColumnTitlesToI18n() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/jvm/JvmInternalsPagesController.java"));
        String jvmTables = source.substring(source.indexOf("private void configureJvmFlagsTable()"),
                source.indexOf("private <T> TableColumn<T, String> localizedColumn("));

        assertFalse(jvmTables.contains("new TableColumn<>(\""),
                "JVM Internals column titles should be localized through i18n bindings");
        assertTrue(jvmTables.contains("localizedColumn("));
    }

    @Test
    void eventDetailFallbackTextComesFromI18n() {
        I18n i18n = new I18n(java.util.Locale.ENGLISH);

        assertEquals("Select an event to inspect timing.", i18n.get("events.details.selectTiming"));

        i18n.setLanguageMode(LanguageMode.CHINESE_SIMPLIFIED);

        assertEquals("选择一个事件以查看时间信息。", i18n.get("events.details.selectTiming"));
    }

    @Test
    void fileChooserStringsComeFromI18n() {
        I18n i18n = new I18n(java.util.Locale.ENGLISH);

        assertEquals("Open JFR Recording", WorkspaceOpenCoordinator.openRecordingChooserTitle(i18n));
        assertEquals("JFR recordings", WorkspaceOpenCoordinator.jfrRecordingsFilterDescription(i18n));
    }

    @Test
    void liveJvmSaveRecordingInitialFileNameUsesRecordingName() throws Exception {
        java.lang.reflect.Method method = LiveJvmPaneController.class.getDeclaredMethod(
                "saveRecordingInitialFileName", String.class);
        method.setAccessible(true);

        assertEquals("jmcfx-42-20260526160235.jfr",
                method.invoke(null, "jmcfx-42-20260526160235"));
    }

    @Test
    void liveJvmSaveRecordingInitialFileNameSanitizesUnsafeCharacters() throws Exception {
        java.lang.reflect.Method method = LiveJvmPaneController.class.getDeclaredMethod(
                "saveRecordingInitialFileName", String.class);
        method.setAccessible(true);

        assertEquals("My_Recording_01.jfr",
                method.invoke(null, "My Recording:01"));
    }

    @Test
    void settingsLanguageLabelsFollowCurrentLanguage() {
        I18n i18n = new I18n(java.util.Locale.ENGLISH);

        assertEquals("Follow System", i18n.get("settings.language.followSystem"));

        i18n.setLanguageMode(LanguageMode.CHINESE_SIMPLIFIED);

        assertEquals("跟随系统", i18n.get("settings.language.followSystem"));
    }

    @Test
    void openingRecordingStatusUsesSelectedFileName() {
        I18n i18n = new I18n(java.util.Locale.ENGLISH);

        assertEquals("Opening recording: sample.jfr",
                WorkspaceOpenCoordinator.openingRecordingStatus(i18n, Path.of("/tmp/sample.jfr")));
    }

    @Test
    void openingHeapDumpStatusUsesSelectedFileName() {
        I18n i18n = new I18n(java.util.Locale.ENGLISH);

        assertEquals("Opening heap dump demo.hprof.",
                WorkspaceOpenCoordinator.openingHeapDumpStatus(i18n, Path.of("demo.hprof")));
    }

    @Test
    void openRecordingButtonIsDisabledOnlyWhileOpening() {
        assertTrue(WorkspaceOpenCoordinator.shouldDisableOpenRecordingButton(true));
        assertFalse(WorkspaceOpenCoordinator.shouldDisableOpenRecordingButton(false));
    }

    @Test
    void preparingRecordingWorkspaceDoesNotPreloadAnalysisPages() {
        prepareRecordingWorkspace(throwingRuleAnalysisService(), null);
    }

    @Test
    void sectionLoadingIsQueuedOnRecordingExecutor() {
        QueueingRecordingOpenExecutor executor = new QueueingRecordingOpenExecutor();
        AtomicInteger analysisCalls = new AtomicInteger();
        PreparedRecordingWorkspace prepared = prepareRecordingWorkspace(recording -> {
            analysisCalls.incrementAndGet();
            return List.of();
        }, null);
        RecordingWorkspace workspace = new RecordingWorkspace(prepared.recording(), prepared.overview(), prepared.events(),
                prepared.analysis(), prepared.profiling(), prepared.exceptions(), prepared.threads(), prepared.fileio(),
                prepared.socketio(), prepared.locks(), prepared.heap(), prepared.leakSuspects(), prepared.tlab(),
                prepared.jvmInfo(), prepared.gcConfig(), prepared.gcSummary(), prepared.gcDetails(),
                prepared.compilations(), prepared.codeCache(), prepared.classLoading(), prepared.vmOperations(),
                prepared.environment(), prepared.javaAppOverview(), prepared.security(), prepared.nativeLibraries(),
                prepared.threadDumps());
        RecordingSectionLoader loader = new RecordingSectionLoader(executor, new I18n(java.util.Locale.ENGLISH),
                visible -> { }, summary -> { }, Runnable::run);

        loader.load(workspace, "analysis");

        assertEquals(1, executor.queuedTaskCount());
        assertEquals(0, analysisCalls.get());

        executor.runNext();

        assertEquals(1, analysisCalls.get());
    }

    @Test
    void queuedSectionLoadingRunsOnlyLatestRequestedHeavySection() {
        QueueingRecordingOpenExecutor executor = new QueueingRecordingOpenExecutor();
        AtomicInteger analysisCalls = new AtomicInteger();
        AtomicInteger profilingCalls = new AtomicInteger();
        PreparedRecordingWorkspace prepared = prepareRecordingWorkspace(recording -> {
            analysisCalls.incrementAndGet();
            return List.of();
        }, profilingService(profilingCalls));
        RecordingWorkspace workspace = new RecordingWorkspace(prepared.recording(), prepared.overview(), prepared.events(),
                prepared.analysis(), prepared.profiling(), prepared.exceptions(), prepared.threads(), prepared.fileio(),
                prepared.socketio(), prepared.locks(), prepared.heap(), prepared.leakSuspects(), prepared.tlab(),
                prepared.jvmInfo(), prepared.gcConfig(), prepared.gcSummary(), prepared.gcDetails(),
                prepared.compilations(), prepared.codeCache(), prepared.classLoading(), prepared.vmOperations(),
                prepared.environment(), prepared.javaAppOverview(), prepared.security(), prepared.nativeLibraries(),
                prepared.threadDumps());
        RecordingSectionLoader loader = new RecordingSectionLoader(executor, new I18n(java.util.Locale.ENGLISH),
                visible -> { }, summary -> { }, Runnable::run);

        loader.load(workspace, "analysis");
        loader.load(workspace, "profiling");

        assertEquals(2, executor.queuedTaskCount());

        executor.runNext();
        executor.runNext();

        assertEquals(0, analysisCalls.get());
        assertEquals(1, profilingCalls.get());
    }

    @Test
    void queuedSectionLoadingIsSkippedWhenUserNavigatesToLightSection() {
        QueueingRecordingOpenExecutor executor = new QueueingRecordingOpenExecutor();
        AtomicInteger analysisCalls = new AtomicInteger();
        PreparedRecordingWorkspace prepared = prepareRecordingWorkspace(recording -> {
            analysisCalls.incrementAndGet();
            return List.of();
        }, null);
        RecordingWorkspace workspace = new RecordingWorkspace(prepared.recording(), prepared.overview(), prepared.events(),
                prepared.analysis(), prepared.profiling(), prepared.exceptions(), prepared.threads(), prepared.fileio(),
                prepared.socketio(), prepared.locks(), prepared.heap(), prepared.leakSuspects(), prepared.tlab(),
                prepared.jvmInfo(), prepared.gcConfig(), prepared.gcSummary(), prepared.gcDetails(),
                prepared.compilations(), prepared.codeCache(), prepared.classLoading(), prepared.vmOperations(),
                prepared.environment(), prepared.javaAppOverview(), prepared.security(), prepared.nativeLibraries(),
                prepared.threadDumps());
        RecordingSectionLoader loader = new RecordingSectionLoader(executor, new I18n(java.util.Locale.ENGLISH),
                visible -> { }, summary -> { }, Runnable::run);

        loader.load(workspace, "analysis");
        loader.load(workspace, "overview");

        assertEquals(1, executor.queuedTaskCount());

        executor.runNext();

        assertEquals(0, analysisCalls.get());
    }

    @Test
    void openingRecordingDoesNotPreloadHeavySections() {
        assertEquals(List.of(), WorkspaceSelectionController.preloadedWorkspaceSections());
    }


    private static AppShellController shellController(AppShell shell) {
        assertNotNull(shell.controller());
        return shell.controller();
    }

    private static AppShell createShellOnFxThread(AppShellFactory factory) throws Exception {
        java.util.concurrent.atomic.AtomicReference<AppShell> shell = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                shell.set(factory.create());
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
        return shell.get();
    }

    private static RecordingSummary recording(String id, String fileName) {
        return new RecordingSummary(id, Path.of(fileName), fileName,
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);
    }

    private static RuleAnalysisService throwingRuleAnalysisService() {
        return recording -> {
            throw new AssertionError("Opening a recording must not run page analysis eagerly.");
        };
    }

    private static PreparedRecordingWorkspace prepareRecordingWorkspace(RuleAnalysisService ruleAnalysisService,
            ProfilingService profilingService) {
        RecordingApplicationServices services = new RecordingApplicationServices(new FakeRecordingRepository(), new FakeEventQueryService(),
                ruleAnalysisService, profilingService, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null);
        return new RecordingWorkspaceFactory(services, new I18n(java.util.Locale.ENGLISH)).prepare(Path.of("startup.jfr"));
    }

    private static ProfilingService profilingService(AtomicInteger calls) {
        return new ProfilingService() {
            @Override
            public List<com.youngledo.jmcfx.domain.model.HotMethod> loadHotMethods(RecordingSummary recording) {
                calls.incrementAndGet();
                return List.of();
            }

            @Override
            public StackTreeNode loadFlameGraphTree(RecordingSummary recording, boolean invertedStacks) {
                return StackTreeNode.EMPTY;
            }

            @Override
            public StackTreeNode loadFlameGraphTree(RecordingSummary recording, String method, boolean invertedStacks) {
                return StackTreeNode.EMPTY;
            }

            @Override
            public StackTreeNode loadStackTraceTree(RecordingSummary recording, String method, boolean callers) {
                return StackTreeNode.EMPTY;
            }

            @Override
            public com.youngledo.jmcfx.domain.model.DependencyGraphReport loadPackageDependencies(
                    RecordingSummary recording, int packageDepth) {
                return com.youngledo.jmcfx.domain.model.DependencyGraphReport.EMPTY;
            }
        };
    }

    private static final class QueueingRecordingOpenExecutor implements RecordingOpenExecutor {
        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable runnable) {
            tasks.add(runnable);
        }

        int queuedTaskCount() {
            return tasks.size();
        }

        void runNext() {
            tasks.remove().run();
        }
    }


    private static Document pom(String path) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        try (InputStream stream = java.nio.file.Files.newInputStream(java.nio.file.Path.of(path))) {
            return factory.newDocumentBuilder().parse(stream);
        }
    }

    private static String appCss() throws IOException {
        try (InputStream stream = AppShellController.class.getResourceAsStream("/css/app.css")) {
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private static String workspaceSelectionSource() throws IOException {
        return java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/WorkspaceSelectionController.java"));
    }

    private static String shellPageControllerRegistrySource() throws IOException {
        return java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/ShellPageControllerRegistry.java"));
    }

    private static String shellRuntimeSource() throws IOException {
        return java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/ShellRuntimeController.java"));
    }

    private static String cssBlock(String css, String selector) {
        int start = css.indexOf("\n" + selector + " {");
        if (start >= 0) {
            start++;
        } else if (css.startsWith(selector + " {")) {
            start = 0;
        }
        assertTrue(start >= 0, selector + " rule must exist");
        int end = css.indexOf('}', start);
        assertTrue(end > start, selector + " rule must be closed");
        return css.substring(start, end + 1);
    }


private static Map<Integer, Element> elements(Document document) {
    Map<Integer, Element> result = new java.util.HashMap<>();
    collectElements(document.getDocumentElement(), result);
    return result;
}

private static void collectElements(Element element, Map<Integer, Element> result) {
    result.put(result.size(), element);
    org.w3c.dom.NodeList children = element.getChildNodes();
    for (int index = 0; index < children.getLength(); index++) {
        org.w3c.dom.Node child = children.item(index);
        if (child instanceof Element childElement) {
            collectElements(childElement, result);
        }
    }
}


    private static void assertJvmBrowserJdpI18n(String bundle, String... expectedLines) {
        for (String expectedLine : expectedLines) {
            assertTrue(bundle.contains(expectedLine), () -> "Missing i18n line: " + expectedLine);
        }
    }

}
