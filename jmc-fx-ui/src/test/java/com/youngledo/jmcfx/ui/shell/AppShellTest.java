package com.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.i18n.LanguageMode;
import com.youngledo.jmcfx.ui.overview.OverviewViewModel;
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

import javafx.fxml.FXMLLoader;
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
    void factorySupportsShellController() {
        AppShellFactory factory = new AppShellFactory(new FakeRecordingRepository(), new FakeEventQueryService(), new FakeRuleAnalysisService());

        Object controller = factory.controllerFor(AppShellController.class, new AppShellViewModel());

        assertEquals(AppShellController.class, controller.getClass());
    }

    @Test
    void factorySupportsIncludedLiveJvmPaneController() {
        AppShellFactory factory = new AppShellFactory(new FakeRecordingRepository(), new FakeEventQueryService(),
                new FakeRuleAnalysisService());

        Object controller = factory.controllerFor(LiveJvmPaneController.class, new AppShellViewModel());

        assertEquals(LiveJvmPaneController.class, controller.getClass());
    }

    @Test
    void codeFirstMigrationTracksRemainingFxmlFilesExplicitly() throws Exception {
        List<String> fxmlFiles = java.nio.file.Files.walk(java.nio.file.Path.of("src/main/resources"))
                .filter(path -> path.toString().endsWith(".fxml"))
                .map(path -> java.nio.file.Path.of("src/main/resources").relativize(path).toString())
                .sorted()
                .toList();

        assertEquals(List.of("com/youngledo/jmcfx/ui/shell/app-shell.fxml"), fxmlFiles);
    }

    @Test
    void appShellFactoryStillUsesFxmlLoaderUntilShellViewMigration() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellFactory.java"));

        assertTrue(source.contains("new FXMLLoader("));
        assertTrue(source.contains("controllerFor("));
    }

    @Test
    void factoryPassesSavedTargetsAndJdpDiscoveryToShellController() {
        FakeSavedJvmTargetRepository savedTargets = new FakeSavedJvmTargetRepository();
        FakeJdpDiscoveryService jdpDiscovery = new FakeJdpDiscoveryService();
        AppShellFactory factory = new AppShellFactory(new FakeRecordingRepository(), new FakeEventQueryService(),
                new FakeRuleAnalysisService(), null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, savedTargets, jdpDiscovery, null,
                new I18n(Locale.ENGLISH));

        AppShellController controller = (AppShellController) factory.controllerFor(AppShellController.class,
                new AppShellViewModel());

        assertEquals(savedTargets, controller.savedTargetRepository());
        assertEquals(jdpDiscovery, controller.jdpDiscoveryService());
    }

    @Test
    void factoryPassesJmxMonitoringDependenciesToShellController() {
        FakeJmxMonitoringService monitoringService = new FakeJmxMonitoringService();
        FakeJmxMonitoringRepository monitoringRepository = new FakeJmxMonitoringRepository();
        AppShellFactory factory = new AppShellFactory(new FakeRecordingRepository(), new FakeEventQueryService(),
                new FakeRuleAnalysisService(), null, null, null, null, null, null, null, null, null, null,
                null, null, new FakeJvmDiscoveryService(), new FakeJmxConnectionService(), null, null,
                null, null, null, monitoringService, monitoringRepository, null, null, null, null,
                new I18n(Locale.ENGLISH));

        AppShellController controller = (AppShellController) factory.controllerFor(AppShellController.class,
                new AppShellViewModel());

        assertEquals(monitoringService, controller.jmxMonitoringService());
        assertEquals(monitoringRepository, controller.jmxMonitoringRepository());
    }

    @Test
    void shellControllerDefaultsToEnglishUiLocale() {
        AppShellController controller = new AppShellController(
                new AppShellViewModel(),
                new FakeRecordingRepository(),
                new FakeEventQueryService(),
                new FakeRuleAnalysisService(),
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                new I18n(java.util.Locale.SIMPLIFIED_CHINESE));

        assertEquals(java.util.Locale.ENGLISH, controller.i18n().localeProperty().get());
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
        assertEquals(0.5, AppShellController.scrollValueAfterZoom(
                0.5, 1000, 2000, 200, 100), 0.000001);
        assertEquals(0, AppShellController.scrollValueAfterZoom(
                0, 1000, 2000, 200, 0), 0.000001);
        assertEquals(1, AppShellController.scrollValueAfterZoom(
                1, 1000, 2000, 200, 200), 0.000001);
        assertEquals(0, AppShellController.scrollValueAfterZoom(
                0.5, 1000, 150, 200, 100), 0.000001);
    }

    @Test
    void workspaceTabsIncludeSingletonLiveJvmWorkspace() {
        assertFalse(AppShellController.shouldShowWorkspaceTabs(0, 0, false));
        assertTrue(AppShellController.shouldShowWorkspaceTabs(0, 0, true));
        assertTrue(AppShellController.shouldShowWorkspaceTabs(1, 0, false));
        assertTrue(AppShellController.shouldShowWorkspaceTabs(0, 1, false));
        assertEquals("JVM", AppShellController.tabTitleFor(new LiveJvmWorkspace("JVM")));
    }

    @Test
    void uiPomIncludesMaterialIconPack() throws Exception {
        Document document = pom("pom.xml");

        assertEquals(true, elements(document).values().stream()
                .anyMatch(element -> "artifactId".equals(element.getTagName())
                        && "ikonli-material2-pack".equals(element.getTextContent())));
    }

    @Test
    void fxmlStructureExposesRecordingWorkspaceStyleHooks() throws Exception {
        Document document = appShellFxml();

        assertEquals("BorderPane", document.getDocumentElement().getTagName());
        assertEquals(0, document.getElementsByTagName("right").getLength());
        assertEquals(0, document.getElementsByTagName("ToolBar").getLength());
        assertEquals(0, elementCountWithFxId(document, "openRecordingButton"));
        Element sidebar = elementByFxId(document, "sidebar");
        assertEquals("AppSidebar", sidebar.getTagName());
        assertEquals(0, elementCountWithStyleClass(document, "left-nav"));
        assertEquals(0, elementCountWithStyleClass(document, "nav-section-label"));
        assertEquals(0, elementCountWithStyleClass(document, "sidebar-recording-card"));
        assertEquals(0, elementCountWithFxId(document, "workspaceTopbar"));
        assertEquals(0, elementCountWithFxId(document, "workspaceContextLabel"));
        assertEquals(0, elementCountWithFxId(document, "workspaceHomeButton"));
        assertEquals("TabPane", elementByFxId(document, "recordingTabs").getTagName());
        assertEquals("StackPane", elementByFxId(document, "workspaceStack").getTagName());

        Element homePane = elementByFxId(document, "homePane");
        assertEquals("18", homePane.getAttribute("spacing"));
        assertTrue(hasStyleClass(homePane, "welcome-pane"));
        assertTrue(hasStyleClass(elementByFxId(document, "homeKickerLabel"), "home-kicker"));
        assertTrue(hasStyleClass(elementByFxId(document, "homeTitleLabel"), "welcome-title"));
        assertTrue(hasStyleClass(elementByFxId(document, "homeSubtitleLabel"), "welcome-subtitle"));
        assertEquals("Button", elementByFxId(document, "homeOpenHeapDumpButton").getTagName());
        assertTrue(hasStyleClass(elementByFxId(document, "homeJfrTile"), "workflow-tile"));
        assertTrue(hasStyleClass(elementByFxId(document, "homeHeapDumpTile"), "workflow-tile"));
        assertTrue(hasStyleClass(elementByFxId(document, "homeJvmTile"), "workflow-tile"));
        assertTrue(hasStyleClass(elementByFxId(document, "homeOpenWorkflowTitleLabel"), "workflow-tile-title"));
        assertTrue(hasStyleClass(elementByFxId(document, "homeHeapDumpWorkflowTitleLabel"), "workflow-tile-title"));
        assertTrue(hasStyleClass(elementByFxId(document, "homeHeapDumpWorkflowDescriptionLabel"), "workflow-tile-copy"));
        assertTrue(hasStyleClass(elementByFxId(document, "homeJvmWorkflowDescriptionLabel"), "workflow-tile-copy"));
        assertTrue("true".equals(elementByFxId(document, "homeJvmWorkflowDescriptionLabel").getAttribute("wrapText")));

        assertEquals("analysisPane", elementByFxId(document, "analysisPane").getAttribute("fx:id"));
        assertTrue(hasStyleClass(elementByFxId(document, "analysisPane"), "split-table-detail-page"));
        assertEquals("HBox", elementByFxId(document, "analysisFilterBar").getTagName());
        assertTrue(hasStyleClass(elementByFxId(document, "analysisFilterBar"), "page-toolbar"));
        assertEquals("TextField", elementByFxId(document, "analysisSearchField").getTagName());
        assertEquals("Spinner", elementByFxId(document, "analysisMinimumScoreSpinner").getTagName());
        assertEquals("CheckBox", elementByFxId(document, "analysisShowOkCheckBox").getTagName());
        assertEquals("CheckBox", elementByFxId(document, "analysisShowIgnoredCheckBox").getTagName());
        assertEquals("CheckBox", elementByFxId(document, "analysisShowUnavailableCheckBox").getTagName());
        assertTrue(hasStyleClass(elementByFxId(document, "analysisDetailPane"), "detail-panel"));
        assertEquals("6", elementByFxId(document, "analysisDetailPane").getAttribute("spacing"));
        assertNull(elementByFxIdOrNull(document, "analysisDetailTitle"));
        assertNull(elementByFxIdOrNull(document, "analysisRelatedPageButton"));
        assertNull(elementByFxIdOrNull(document, "analysisDetailMeta"));
        assertNull(elementByFxIdOrNull(document, "analysisDetailResultIdLabel"));
        assertNull(elementByFxIdOrNull(document, "analysisDetailSummaryCaption"));
        assertNull(elementByFxIdOrNull(document, "analysisDetailSummaryArea"));
        assertTrue(hasStyleClass(elementByFxId(document, "analysisDetailExplanationCaption"), "detail-section-label"));
        assertTrue(hasStyleClass(elementByFxId(document, "analysisDetailEvidenceCaption"), "detail-section-label"));
        assertTrue(hasStyleClass(elementByFxId(document, "analysisDetailRecommendationCaption"), "detail-section-label"));
        assertTrue(hasStyleClass(elementByFxId(document, "analysisDetailExplanationArea"), "detail-panel-body"));
        assertTrue(hasStyleClass(elementByFxId(document, "analysisDetailEvidenceArea"), "detail-panel-body"));
        assertTrue(hasStyleClass(elementByFxId(document, "analysisDetailRecommendationArea"), "detail-panel-body"));
        Element metadataPane = elementByFxId(document, "metadataPane");
        assertEquals("VBox", metadataPane.getTagName());
        assertTrue(hasStyleClass(metadataPane, "page"));
        assertTrue(hasStyleClass(metadataPane, "split-table-detail-page"));
        assertEquals("Label", elementByFxId(document, "metadataTitleLabel").getTagName());
        assertEquals("Label", elementByFxId(document, "metadataSummaryLabel").getTagName());
        assertEquals("SplitPane", elementByFxId(document, "metadataContent").getTagName());
        assertEquals("TableView", elementByFxId(document, "metadataEventTypesTable").getTagName());
        assertTrue(hasStyleClass(elementByFxId(document, "metadataEventTypesTable"), "dense-table"));
        assertEquals("VBox", elementByFxId(document, "metadataDetailPane").getTagName());
        assertTrue(hasStyleClass(elementByFxId(document, "metadataDetailPane"), "detail-panel"));
        assertEquals("Label", elementByFxId(document, "metadataDetailTitleLabel").getTagName());
        assertTrue(hasStyleClass(elementByFxId(document, "metadataDetailTitleLabel"), "detail-panel-title"));
        assertEquals("TextArea", elementByFxId(document, "metadataDetailArea").getTagName());
        assertEquals("false", elementByFxId(document, "metadataDetailArea").getAttribute("editable"));
        assertEquals("true", elementByFxId(document, "metadataDetailArea").getAttribute("wrapText"));
        assertTrue(hasStyleClass(elementByFxId(document, "metadataDetailArea"), "detail-panel-body"));
        assertEquals("VBox", elementByFxId(document, "advancedJfrPane").getTagName());
        assertEquals("Label", elementByFxId(document, "advancedJfrTitleLabel").getTagName());
        assertEquals("Label", elementByFxId(document, "advancedJfrSummaryLabel").getTagName());
        assertEquals("TabPane", elementByFxId(document, "advancedJfrTabs").getTagName());
        List<Element> advancedJfrTabs = childElements(childElement(elementByFxId(document, "advancedJfrTabs"), "tabs"), "Tab");
        assertEquals(List.of("advancedJfrHeatmapTab", "advancedJfrMemoryTab"),
                advancedJfrTabs.stream().map(tab -> tab.getAttribute("fx:id")).toList());
        assertEquals("VBox", elementByFxId(document, "advancedJfrHeatmapContainer").getTagName());
        assertEquals(0, elementCountWithFxId(document, "advancedJfrSplitPane"));
        assertTrue(hasStyleClass(elementByFxId(document, "advancedJfrSelectionPane"), "advanced-jfr-selection-pane"));
        assertEquals("HBox", elementByFxId(document, "advancedJfrSelectionValues").getTagName());
        assertEquals("Label", elementByFxId(document, "advancedJfrSelectedEventTypeLabel").getTagName());
        assertEquals("Label", elementByFxId(document, "advancedJfrSelectedCountLabel").getTagName());
        assertEquals("Label", elementByFxId(document, "advancedJfrMemorySummaryLabel").getTagName());
        assertEquals("TableView", elementByFxId(document, "advancedJfrMemoryTable").getTagName());
        assertTrue(hasStyleClass(elementByFxId(document, "advancedJfrMemoryTable"), "dense-table"));
        assertEquals("Label", elementByFxId(document, "advancedJfrMemoryDetailTitleLabel").getTagName());
        assertTrue(hasStyleClass(elementByFxId(document, "advancedJfrMemoryDetailTitleLabel"), "detail-panel-title"));
        assertEquals("TextArea", elementByFxId(document, "advancedJfrMemoryDetailArea").getTagName());
        assertEquals("false", elementByFxId(document, "advancedJfrMemoryDetailArea").getAttribute("editable"));
        assertEquals("true", elementByFxId(document, "advancedJfrMemoryDetailArea").getAttribute("wrapText"));
        Element javaApplicationPane = elementByFxId(document, "javaApplicationPane");
        assertEquals("VBox", javaApplicationPane.getTagName());
        assertTrue(hasStyleClass(javaApplicationPane, "page"));
        assertTrue(hasStyleClass(javaApplicationPane, "overview-page"));
        assertEquals("Label", elementByFxId(document, "javaApplicationTitleLabel").getTagName());
        assertEquals("GridPane", elementByFxId(document, "javaApplicationSummaryGrid").getTagName());
        assertTrue(hasStyleClass(elementByFxId(document, "javaApplicationSummaryGrid"), "metric-grid"));
        for (String fxId : List.of("javaApplicationProfilingButton", "javaApplicationIoButton",
                "javaApplicationLocksButton", "javaApplicationThreadsButton",
                "javaApplicationExceptionsButton", "javaApplicationClassLoadingButton",
                "javaApplicationAllocationButton")) {
            assertEquals("Button", elementByFxId(document, fxId).getTagName());
        }
        Element jvmInternalsPane = elementByFxId(document, "jvmInternalsPane");
        assertEquals("VBox", jvmInternalsPane.getTagName());
        assertTrue(hasStyleClass(jvmInternalsPane, "page"));
        assertTrue(hasStyleClass(jvmInternalsPane, "overview-page"));
        assertEquals("Label", elementByFxId(document, "jvmInternalsTitleLabel").getTagName());
        assertEquals("GridPane", elementByFxId(document, "jvmInternalsSummaryGrid").getTagName());
        assertTrue(hasStyleClass(elementByFxId(document, "jvmInternalsSummaryGrid"), "metric-grid"));
        for (String fxId : List.of("jvmInternalsInformationButton", "jvmInternalsGcButton",
                "jvmInternalsG1Button", "jvmInternalsCompilationButton", "jvmInternalsCodeCacheButton",
                "jvmInternalsClassLoadingButton", "jvmInternalsVmOperationsButton")) {
            assertEquals("Button", elementByFxId(document, fxId).getTagName());
        }
        Element environmentPane = elementByFxId(document, "environmentPane");
        assertEquals("VBox", environmentPane.getTagName());
        assertTrue(hasStyleClass(environmentPane, "page"));
        assertTrue(hasStyleClass(environmentPane, "overview-page"));
        assertEquals("Label", elementByFxId(document, "environmentTitleLabel").getTagName());
        assertEquals("GridPane", elementByFxId(document, "environmentSummaryGrid").getTagName());
        assertTrue(hasStyleClass(elementByFxId(document, "environmentSummaryGrid"), "metric-grid"));
        for (String fxId : List.of("environmentProcessesButton", "environmentVariablesButton",
                "environmentPropertiesButton", "environmentRecordingButton", "environmentAgentsButton",
                "environmentConstantPoolsButton")) {
            assertEquals("Button", elementByFxId(document, fxId).getTagName());
        }
        Element heapDumpPane = elementByFxId(document, "heapDumpAnalysisPane");
        assertTrue(hasStyleClass(heapDumpPane, "page"));
        assertTrue(hasStyleClass(heapDumpPane, "split-table-detail-page"));
        assertTrue(hasStyleClass(heapDumpPane, "heap-dump-page"));
        assertTrue(hasStyleClass(elementByFxId(document, "heapDumpHeader"), "page-header"));
        assertEquals(0, elementCountWithFxId(document, "heapDumpToolbar"),
                "HPROF files are opened from Home; opened heap dump workspaces should not repeat the open action");
        assertEquals(0, elementCountWithFxId(document, "heapDumpOpenButton"),
                "HPROF open action belongs on Home, not inside an opened heap dump workspace");
        assertEquals(0, elementCountWithFxId(document, "heapDumpNameLabel"),
                "The opened HPROF file name is already represented by the workspace tab");
        assertEquals(0, elementCountWithFxId(document, "heapDumpSummaryLabel"),
                "The HPROF page should focus on analysis results instead of repeating file summary chrome");
        assertEquals(0, elementCountWithFxId(document, "heapDumpProgressBar"),
                "HPROF long-running progress belongs in the shell status area, not the page toolbar");
        assertTrue(hasStyleClass(elementByFxId(document, "heapDumpContent"), "page-content"));
        assertTrue(hasStyleClass(elementByFxId(document, "heapDumpDetailsTabs"), "page-detail-tabs"));
        assertFalse(elementByFxId(document, "heapDumpDetailsTabs").hasAttribute("prefHeight"),
                "HPROF detail tabs must use the shared split table detail CSS sizing instead of a fixed FXML height");
        assertTrue(hasStyleClass(elementByFxId(document, "heapDumpTextReportPane"), "detail-panel"));
        assertTrue(hasStyleClass(elementByFxId(document, "heapDumpIssueDetailPane"), "detail-panel"));
        assertTrue(hasStyleClass(elementByFxId(document, "heapDumpTextReportArea"), "detail-panel-body"));
        assertTrue(hasStyleClass(elementByFxId(document, "heapDumpIssueDetailTitleLabel"), "detail-panel-title"));
        assertEquals("homeOpenRecordingButton", elementByFxId(document, "homeOpenRecordingButton").getAttribute("fx:id"));
        assertFalse(elementByFxId(document, "homeOpenRecordingButton").hasAttribute("styleClass"),
                "Home action buttons should keep JavaFX's default button style class; add local hooks in controller");
        assertFalse(elementByFxId(document, "homeConnectJvmButton").hasAttribute("styleClass"),
                "Home action buttons should keep JavaFX's default button style class; add local hooks in controller");
        assertFalse(elementByFxId(document, "homeConnectJvmButton").hasAttribute("disable"),
                "Connect JVM should be enabled now that the JVM browser page exists");
        assertEquals(0, elementCountWithFxId(document, "statusLabel"));
        assertEquals(0, elementCountWithFxId(document, "taskSummaryLabel"));
        assertEquals("tlabTimelineContainer", elementByFxId(document, "tlabTimelineContainer").getAttribute("fx:id"));
    }

    @Test
    void appShellIncludesLiveJvmPaneInsteadOfEmbeddingIt() throws Exception {
        Document document = appShellFxml();
        Element host = elementByFxId(document, "jvmsPaneHost");

        assertEquals("VBox", host.getTagName());
        assertNull(elementByFxIdOrNull(document, "jvmsTable"));
        assertNull(elementByFxIdOrNull(document, "jvmsMonitoringToolbar"));
        assertNull(elementByFxIdOrNull(document, "jvmsAgentTransformsTable"));
    }

    @Test
    void appShellDoesNotOwnLiveJvmControlFieldsAfterExtraction() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));

        assertTrue(source.contains("@FXML private VBox jvmsPaneHost;"));
        assertTrue(source.contains("private LiveJvmPaneController jvmsPaneController;"));
        assertTrue(source.contains("new LiveJvmPaneController()"));
        assertFalse(source.contains("@FXML private TableView<JvmConnection> jvmsTable;"));
        assertFalse(source.contains("@FXML private Button jvmsAddNotificationSubscriptionButton;"));
        assertFalse(source.contains("private void configureJmxMonitoring()"));
        assertFalse(source.contains("private void bindJmcAgentManager()"));
    }

    @Test
    void profilingPageContainsFlameGraphTabsBeforeTreeTabs() throws Exception {
        Document document = appShellFxml();

        Element profilingTreeTabs = elementByFxId(document, "profilingTreeTabs");
        List<Element> tabs = childElements(childElement(profilingTreeTabs, "tabs"), "Tab");

        assertEquals(List.of("profilingCallGraphTab", "profilingCallersFlameTab", "profilingCalleesFlameTab",
                        "profilingDependencyGraphTab", "profilingCallersTab", "profilingCalleesTab"),
                tabs.stream().map(tab -> tab.getAttribute("fx:id")).toList());

        Element callGraphTab = elementByFxId(document, "profilingCallGraphTab");
        assertEquals("Tab", callGraphTab.getTagName());
        Element callGraphToolbar = elementByFxId(document, "profilingCallGraphToolbar");
        assertEquals("HBox", callGraphToolbar.getTagName());
        assertTrue(hasStyleClass(callGraphToolbar, "page-toolbar"));
        assertEquals("CENTER_LEFT", callGraphToolbar.getAttribute("alignment"));
        assertEquals("ComboBox", elementByFxId(document, "profilingCallGraphDirectionCombo").getTagName());
        assertEquals("Label", elementByFxId(document, "profilingCallGraphDepthLabel").getTagName());
        assertEquals("Spinner", elementByFxId(document, "profilingCallGraphDepthSpinner").getTagName());
        Element callGraphContainer = elementByFxId(document, "profilingCallGraphContainer");
        assertEquals("VBox", callGraphContainer.getTagName());
        assertTrue(hasStyleClass(callGraphContainer, "profiling-call-graph-container"));
        Element callGraphScrollPane = (Element) callGraphContainer.getParentNode();
        assertEquals("true", callGraphScrollPane.getAttribute("pannable"));
        assertEquals("false", callGraphScrollPane.getAttribute("fitToWidth"));
        assertEquals("false", callGraphScrollPane.getAttribute("fitToHeight"));

        assertFlameGraphTab(document, "profilingCallersFlameContainer");
        assertFlameGraphTab(document, "profilingCalleesFlameContainer");
        Element dependencyGraphTab = elementByFxId(document, "profilingDependencyGraphTab");
        assertEquals("Tab", dependencyGraphTab.getTagName());
        assertEquals("Spinner", elementByFxId(document, "profilingDependencyDepthSpinner").getTagName());
        assertEquals("TableView", elementByFxId(document, "profilingDependencyTable").getTagName());
        Element dependencyContainer = elementByFxId(document, "profilingDependencyGraphContainer");
        assertEquals("VBox", dependencyContainer.getTagName());
        assertTrue(hasStyleClass(dependencyContainer, "profiling-call-graph-container"));
        assertEquals("TreeView", elementByFxId(document, "profilingCallersTree").getTagName());
        assertEquals("TreeView", elementByFxId(document, "profilingCalleesTree").getTagName());
    }

    @Test
    void profilingGraphShellWiringUsesViewModelLayoutsAndI18n() throws Exception {
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String english = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages.properties"));
        String chinese = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages_zh_CN.properties"));
        String css = appCss();

        assertTrue(controller.contains("private Tab profilingCallGraphTab;"));
        assertTrue(controller.contains("private HBox profilingCallGraphToolbar;"));
        assertTrue(controller.contains("private ComboBox<CallGraphDirection> profilingCallGraphDirectionCombo;"));
        assertTrue(controller.contains("private Label profilingCallGraphDepthLabel;"));
        assertTrue(controller.contains("private Spinner<Integer> profilingCallGraphDepthSpinner;"));
        assertTrue(controller.contains("private Button profilingCallGraphZoomOutButton;"));
        assertTrue(controller.contains("private Button profilingCallGraphResetZoomButton;"));
        assertTrue(controller.contains("private Button profilingCallGraphZoomInButton;"));
        assertTrue(controller.contains("private Button profilingCallGraphFitButton;"));
        assertTrue(controller.contains("private VBox profilingCallGraphContainer;"));
        assertTrue(controller.contains("private CallGraphView profilingCallGraphView;"));
        assertTrue(controller.contains("private Tab profilingDependencyGraphTab;"));
        assertTrue(controller.contains("private Spinner<Integer> profilingDependencyDepthSpinner;"));
        assertTrue(controller.contains("private TableView<DependencyGraphEdge> profilingDependencyTable;"));
        assertTrue(controller.contains("private VBox profilingDependencyGraphContainer;"));
        assertTrue(controller.contains("private CallGraphView profilingDependencyGraphView;"));
        assertTrue(controller.contains("private Tab profilingCallersFlameTab;"));
        assertTrue(controller.contains("private Button profilingCallersFlameOrientationButton;"));
        assertTrue(controller.contains("private VBox profilingCallersFlameContainer;"));
        assertTrue(controller.contains("private Tab profilingCalleesFlameTab;"));
        assertTrue(controller.contains("private Button profilingCalleesFlameOrientationButton;"));
        assertTrue(controller.contains("private VBox profilingCalleesFlameContainer;"));
        assertTrue(controller.contains("private FlameGraphView profilingCallersFlameGraphView;"));
        assertTrue(controller.contains("private FlameGraphView profilingCalleesFlameGraphView;"));
        assertTrue(controller.contains("profilingCallGraphView.emptyTextProperty().bind(i18n.text(\"profiling.callGraph.empty\"))"));
        assertTrue(controller.contains("profilingDependencyGraphView.emptyTextProperty().bind(i18n.text(\"profiling.dependency.empty\"))"));
        assertTrue(controller.contains("profilingCallGraphContainer.getChildren().setAll(profilingCallGraphView)"));
        assertTrue(controller.contains("profilingDependencyGraphContainer.getChildren().setAll(profilingDependencyGraphView)"));
        assertTrue(controller.contains("profilingCallGraphView.setLayout(null)"));
        assertTrue(controller.contains("profilingDependencyGraphView.setLayout(null)"));
        assertTrue(controller.contains("profilingCallersFlameContainer.getChildren().setAll(profilingCallersFlameGraphView)"));
        assertTrue(controller.contains("profilingCalleesFlameContainer.getChildren().setAll(profilingCalleesFlameGraphView)"));
        assertTrue(controller.contains("profilingCallersFlameGraphView.setLayout(null)"));
        assertTrue(controller.contains("profilingCalleesFlameGraphView.setLayout(null)"));
        assertTrue(controller.contains("currentProfilingViewModel.callGraphProperty().removeListener(callGraphListener)"));
        assertTrue(controller.contains("currentProfilingViewModel.dependencyGraphProperty().removeListener(dependencyGraphListener)"));
        assertTrue(controller.contains("currentProfilingViewModel.callersTreeProperty().removeListener(callersTreeListener)"));
        assertTrue(controller.contains("currentProfilingViewModel.calleesTreeProperty().removeListener(calleesTreeListener)"));
        assertTrue(controller.contains("currentProfilingViewModel.callersFlameGraphProperty().removeListener(callersFlameGraphListener)"));
        assertTrue(controller.contains("currentProfilingViewModel.calleesFlameGraphProperty().removeListener(calleesFlameGraphListener)"));
        assertTrue(controller.contains("nextViewModel.callGraphProperty().addListener(callGraphListener)"));
        assertTrue(controller.contains("nextViewModel.dependencyGraphProperty().addListener(dependencyGraphListener)"));
        assertTrue(controller.contains("nextViewModel.callersTreeProperty().addListener(callersTreeListener)"));
        assertTrue(controller.contains("nextViewModel.calleesTreeProperty().addListener(calleesTreeListener)"));
        assertTrue(controller.contains("nextViewModel.callersFlameGraphProperty().addListener(callersFlameGraphListener)"));
        assertTrue(controller.contains("nextViewModel.calleesFlameGraphProperty().addListener(calleesFlameGraphListener)"));
        assertTrue(controller.contains("nextViewModel.callGraphProperty().get()"));
        assertTrue(controller.contains("nextViewModel.dependencyGraphProperty().get()"));
        assertTrue(controller.contains("nextViewModel.callersFlameGraphProperty().get()"));
        assertTrue(controller.contains("nextViewModel.calleesFlameGraphProperty().get()"));
        assertTrue(controller.contains("setCallGraphDirection"));
        assertTrue(controller.contains("setCallGraphMaxDepth"));
        assertTrue(controller.contains("profiling.callGraph.direction.callers"));
        assertTrue(controller.contains("profiling.callGraph.direction.callees"));
        assertTrue(controller.contains("i18n.localeProperty().addListener"));
        assertTrue(controller.contains("refreshProfilingCallGraphDirectionLabel"));
        assertTrue(controller.contains("CallGraphDirection selectedDirection = profilingCallGraphDirectionCombo.getSelectionModel().getSelectedItem()"));
        assertTrue(controller.contains("profilingCallGraphDirectionCombo.getSelectionModel().select(selectedDirection)"));
        assertTrue(controller.contains("profilingCallGraphTab.textProperty().bind(i18n.text(\"profiling.tab.callGraph\"))"));
        assertTrue(controller.contains("profilingDependencyGraphTab.textProperty().bind(i18n.text(\"profiling.tab.dependencyGraph\"))"));
        assertTrue(controller.contains("profilingCallGraphDirectionCombo.promptTextProperty().bind(i18n.text(\"profiling.callGraph.direction\"))"));
        assertTrue(controller.contains("profilingCallGraphDepthLabel.textProperty().bind(i18n.text(\"profiling.callGraph.depth\"))"));
        assertTrue(controller.contains("profilingDependencyDepthLabel.textProperty().bind(i18n.text(\"profiling.dependency.depth\"))"));
        assertTrue(controller.contains("profilingCallersFlameTab.textProperty().bind(i18n.text(\"profiling.tab.callersFlame\"))"));
        assertTrue(controller.contains("profilingCalleesFlameTab.textProperty().bind(i18n.text(\"profiling.tab.calleesFlame\"))"));
        assertTrue(controller.contains("profilingCallersFlameGraphView.emptyTextProperty().bind(i18n.text(\"profiling.flame.empty\"))"));
        assertTrue(controller.contains("profilingCalleesFlameGraphView.emptyTextProperty().bind(i18n.text(\"profiling.flame.empty\"))"));
        assertTrue(controller.contains("configureGraphZoomButtons(profilingCallGraphView"));
        assertTrue(controller.contains("configureGraphZoomButtons(profilingDependencyGraphView"));
        assertTrue(controller.contains("configureFlameGraphButtons(profilingCallersFlameGraphView"));
        assertTrue(controller.contains("toggleFlameGraphOrientation"));
        assertTrue(controller.contains("bindFlameGraphToolbarVisibility(profilingCallersFlameToolbar"));
        assertTrue(controller.contains("bindFlameGraphToolbarVisibility(profilingCalleesFlameToolbar"));
        assertTrue(controller.contains("toolbar.visibleProperty().bind(graphView.hasFramesProperty())"));
        assertTrue(controller.contains("toolbar.managedProperty().bind(toolbar.visibleProperty())"));
        assertTrue(controller.contains("graphView.fitToWidth(graphViewportWidth(graphView))"));
        assertTrue(controller.contains("configureCallGraphGestures"));
        assertTrue(controller.contains("addEventFilter(ScrollEvent.SCROLL"));
        assertTrue(controller.contains("addEventFilter(ZoomEvent.ZOOM_STARTED"));
        assertTrue(controller.contains("zoomCallGraphAt"));
        assertTrue(controller.contains("scrollValueAfterZoom"));
        assertTrue(controller.contains("addEventFilter(ZoomEvent.ZOOM_FINISHED"));
        assertFalse(controller.contains("PauseTransition"));
        assertTrue(controller.contains("panCallGraphViewport"));
        assertTrue(controller.contains("scrollPane.setHvalue"));
        assertTrue(controller.contains("scrollPane.setVvalue"));
        assertTrue(controller.contains("addEventFilter(ZoomEvent.ZOOM"));
        assertTrue(controller.contains("addEventFilter(MouseEvent.MOUSE_CLICKED"));
        assertTrue(controller.contains("event.isShortcutDown()"));
        assertTrue(controller.contains("graphView.zoomBy"));

        assertTrue(css.contains(".profiling-call-graph-container"));
        assertTrue(css.contains(".profiling-graph-tab-content"));
        assertTrue(css.contains(".profiling-graph-toolbar"));
        assertTrue(css.contains(".profiling-graph-tool-button"));
        assertTrue(english.contains("profiling.tab.callGraph=Call Graph"));
        assertTrue(english.contains("profiling.tab.dependencyGraph=Dependency Graph"));
        assertTrue(english.contains("profiling.tab.callersFlame=Caller Flame Graph"));
        assertTrue(english.contains("profiling.tab.calleesFlame=Callee Flame Graph"));
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
        assertTrue(english.contains("profiling.flame.empty=Select a method to view the graph."));
        assertTrue(english.contains("profiling.flame.orientation=Switch flame/icicle orientation"));
        assertTrue(english.contains("profiling.flame.orientation.icicle=Icicle"));
        assertTrue(english.contains("profiling.flame.orientation.flame=Flame"));
        assertTrue(chinese.contains("profiling.tab.callGraph=调用图"));
        assertTrue(chinese.contains("profiling.tab.dependencyGraph=依赖图"));
        assertTrue(chinese.contains("profiling.tab.callersFlame=调用者火焰图"));
        assertTrue(chinese.contains("profiling.tab.calleesFlame=被调用者火焰图"));
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
        assertTrue(chinese.contains("profiling.flame.empty=选择一个方法查看图表。"));
        assertTrue(chinese.contains("profiling.flame.orientation=切换火焰图/冰柱图方向"));
        assertTrue(chinese.contains("profiling.flame.orientation.icicle=冰柱图"));
        assertTrue(chinese.contains("profiling.flame.orientation.flame=火焰图"));
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
        assertFalse(css.contains(".jvms-live-tab-content .chart:refreshing"));
        assertFalse(css.contains(".jvms-live-tab-content .table-view:refreshing"));
        assertTrue(css.contains(".jvms-overview-group"));
        assertTrue(css.contains(".jvms-overview-chart-panel .scroll-pane"));
        assertTrue(css.contains(".jvms-overview-metric-toggles"));
        assertTrue(css.contains(".jvms-overview-metric-toggle-content"));
        assertTrue(css.contains("-fx-spacing: 6px"));
        assertTrue(css.contains(".jvms-overview-metric-swatch.default-color0"));
        assertTrue(css.contains("-fx-background-color: #f3622d"));

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
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String english = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages.properties"));
        String chinese = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages_zh_CN.properties"));
        String css = appCss();

        assertTrue(controller.contains("import com.youngledo.jmcfx.domain.model.MemoryIssue;"));
        assertTrue(controller.contains("private TabPane advancedJfrTabs;"));
        assertTrue(controller.contains("private Tab advancedJfrHeatmapTab;"));
        assertTrue(controller.contains("private Tab advancedJfrMemoryTab;"));
        assertTrue(controller.contains("private Label advancedJfrMemorySummaryLabel;"));
        assertTrue(controller.contains("private TableView<MemoryIssue> advancedJfrMemoryTable;"));
        assertTrue(controller.contains("private Label advancedJfrMemoryDetailTitleLabel;"));
        assertTrue(controller.contains("private TextArea advancedJfrMemoryDetailArea;"));
        assertTrue(controller.contains("private boolean rebindingAdvancedJfrMemory;"));
        assertTrue(controller.contains("configureAdvancedJfrMemoryTable();"));
        assertTrue(controller.contains("advancedJfrHeatmapTab.textProperty().bind(i18n.text(\"advancedJfr.heatmap.tab\"))"));
        assertTrue(controller.contains("advancedJfrMemoryTab.textProperty().bind(i18n.text(\"advancedJfr.memory.tab\"))"));
        assertTrue(controller.contains("advancedJfrMemoryTable.setPlaceholder(localizedTablePlaceholder(\"advancedJfr.memory.empty\"))"));
        assertTrue(controller.contains("localizedColumn(\"advancedJfr.memory.column.severity\")"));
        assertTrue(controller.contains("localizedColumn(\"advancedJfr.memory.column.category\")"));
        assertTrue(controller.contains("localizedColumn(\"advancedJfr.memory.column.subject\")"));
        assertTrue(controller.contains("localizedColumn(\"advancedJfr.memory.column.estimatedBytes\")"));
        assertTrue(controller.contains("localizedColumn(\"advancedJfr.memory.column.count\")"));
        assertTrue(controller.contains("localizedColumn(\"advancedJfr.memory.column.score\")"));
        assertTrue(controller.contains("advancedJfrMemoryTable.setItems(nextViewModel.memoryIssues())"));
        assertTrue(controller.contains("if (!rebindingAdvancedJfrMemory && advancedJfrViewModel != null)"));
        assertTrue(controller.contains("advancedJfrViewModel.selectMemoryIssue(issue)"));
        assertTrue(controller.contains("rebindingAdvancedJfrMemory = true;"));
        assertTrue(controller.contains("rebindingAdvancedJfrMemory = false;"));
        assertTrue(controller.contains("bindAdvancedJfrMemoryText(nextViewModel)"));
        assertTrue(controller.contains("formatAdvancedJfrMemorySummary"));
        assertTrue(controller.contains("formatAdvancedJfrMemoryIssueTitle"));
        assertTrue(controller.contains("formatAdvancedJfrMemoryIssueDetails"));
        assertTrue(controller.contains("i18n.format(\"advancedJfr.memory.summary.format\""));
        assertTrue(controller.contains("i18n.format(\"advancedJfr.memory.detail.category\""));
        assertTrue(controller.contains("i18n.format(\"advancedJfr.memory.detail.recommendation\""));
        assertFalse(controller.contains("memorySummaryProperty()"));
        assertFalse(controller.contains("selectedMemoryIssueTitleProperty()"));
        assertFalse(controller.contains("selectedMemoryIssueDetailsProperty()"));
        assertTrue(controller.contains("advancedJfrMemoryTable.setItems(FXCollections.emptyObservableList())"));
        assertTrue(controller.contains("advancedJfrMemoryTable.getSelectionModel().clearSelection()"));

        assertTrue(css.contains(".advanced-jfr-memory-content"));
        assertTrue(css.contains(".detail-panel"));
        assertTrue(css.contains(".analysis-filter-bar"));

        assertTrue(controller.contains("private TextField analysisSearchField;"));
        assertTrue(controller.contains("private Spinner<Integer> analysisMinimumScoreSpinner;"));
        assertTrue(controller.contains("private CheckBox analysisShowOkCheckBox;"));
        assertTrue(controller.contains("private CheckBox analysisShowIgnoredCheckBox;"));
        assertTrue(controller.contains("private CheckBox analysisShowUnavailableCheckBox;"));
        assertTrue(controller.contains("analysisSearchField.textProperty().bindBidirectional"));
        assertTrue(controller.contains("analysisMinimumScoreSpinner.getValueFactory().valueProperty().bindBidirectional"));
        assertTrue(controller.contains("analysisShowOkCheckBox.selectedProperty().bindBidirectional"));
        assertTrue(controller.contains("analysisShowIgnoredCheckBox.selectedProperty().bindBidirectional"));
        assertTrue(controller.contains("analysisShowUnavailableCheckBox.selectedProperty().bindBidirectional"));
        assertTrue(controller.contains("localizedColumn(\"analysis.column.resultId\")"));
        assertTrue(controller.contains("localizedColumn(\"analysis.column.rulePage\")"));
        assertTrue(controller.contains("analysisViewModel.selectedResultProperty().set(val)"));
        assertTrue(controller.contains("analysisTable.setRowFactory(table ->"));
        assertTrue(controller.contains("event.getButton() == MouseButton.PRIMARY"));
        assertTrue(controller.contains("event.getClickCount() == 2"));
        assertTrue(controller.contains("openAnalysisRelatedPage(row.getItem())"));
        assertTrue(controller.contains("viewModel.showSection(detail.relatedPageId())"));
        assertFalse(controller.contains("analysisRelatedPageButton"));
        assertFalse(controller.contains("analysisDetailTitle"));
        assertFalse(controller.contains("analysisDetailSummaryArea"));
        assertFalse(controller.contains("analysisDetailResultIdLabel"));
        assertFalse(controller.contains("analysisDetailMeta"));
        assertTrue(controller.contains("analysisDetailEvidenceArea.setText(detail.evidence())"));
        assertTrue(controller.contains("analysisDetailRecommendationArea.setText(detail.recommendation())"));

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
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String english = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages.properties"));
        String chinese = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages_zh_CN.properties"));

        assertTrue(controller.contains("import com.youngledo.jmcfx.domain.model.JfrMetadataEventType;"));
        assertTrue(controller.contains("import com.youngledo.jmcfx.ui.metadata.JfrMetadataViewModel;"));
        assertTrue(controller.contains("private VBox metadataPane;"));
        assertTrue(controller.contains("private Label metadataTitleLabel;"));
        assertTrue(controller.contains("private Label metadataSummaryLabel;"));
        assertTrue(controller.contains("private TableView<JfrMetadataEventType> metadataEventTypesTable;"));
        assertTrue(controller.contains("private Label metadataDetailTitleLabel;"));
        assertTrue(controller.contains("private TextArea metadataDetailArea;"));
        assertTrue(controller.contains("metadataPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo(\"metadata\"))"));
        assertTrue(controller.contains("configureMetadataTable();"));
        assertTrue(controller.contains("metadataTitleLabel.textProperty().bind(i18n.text(\"metadata.title\"))"));
        assertTrue(controller.contains("metadataEventTypesTable.setPlaceholder(localizedTablePlaceholder(\"metadata.empty\"))"));
        assertTrue(controller.contains("localizedColumn(\"metadata.column.category\")"));
        assertTrue(controller.contains("localizedColumn(\"metadata.column.name\")"));
        assertTrue(controller.contains("localizedColumn(\"metadata.column.eventCount\")"));
        assertTrue(controller.contains("localizedColumn(\"metadata.column.fieldCount\")"));
        assertTrue(controller.contains("metadataEventTypesTable.setItems(nextViewModel.eventTypesProperty())"));
        assertTrue(controller.contains("metadataDetailArea.textProperty().bind(nextViewModel.selectedDetailProperty())"));
        assertTrue(controller.contains("case \"metadata\" -> loadIfPresent(workspace.jfrMetadataViewModel(), recording);"));

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
    void g1GcShellUsesSplitTableDetailBindingsAndI18n() throws Exception {
        Document document = appShellFxml();
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String english = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages.properties"));
        String chinese = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages_zh_CN.properties"));

        Element g1GcPane = elementByFxId(document, "g1GcPane");
        assertEquals("VBox", g1GcPane.getTagName());
        assertTrue(hasStyleClass(g1GcPane, "page"));
        assertTrue(hasStyleClass(g1GcPane, "split-table-detail-page"));
        assertEquals("TableView", elementByFxId(document, "g1GcRegionSummaryTable").getTagName());
        assertEquals("TableView", elementByFxId(document, "g1GcRegionStatesTable").getTagName());
        assertEquals("TableView", elementByFxId(document, "g1GcPauseTable").getTagName());
        assertTrue(hasStyleClass(elementByFxId(document, "g1GcDetailPane"), "detail-panel"));
        assertTrue(hasStyleClass(elementByFxId(document, "g1GcDetailTitleLabel"), "detail-panel-title"));
        assertTrue(hasStyleClass(elementByFxId(document, "g1GcDetailArea"), "detail-panel-body"));

        assertTrue(controller.contains("import com.youngledo.jmcfx.domain.model.G1GcRegionState;"));
        assertTrue(controller.contains("import com.youngledo.jmcfx.ui.gc.G1GcViewModel;"));
        assertTrue(controller.contains("private VBox g1GcPane;"));
        assertTrue(controller.contains("private TableView<G1GcRegionSummary> g1GcRegionSummaryTable;"));
        assertTrue(controller.contains("private TableView<G1GcRegionState> g1GcRegionStatesTable;"));
        assertTrue(controller.contains("private TableView<GcEvent> g1GcPauseTable;"));
        assertTrue(controller.contains("g1GcPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo(\"g1Gc\"))"));
        assertTrue(controller.contains("configureG1GcTables();"));
        assertTrue(controller.contains("g1GcRegionStatesTable.setItems(nextViewModel.recentRegionStatesProperty())"));
        assertTrue(controller.contains("g1GcDetailArea.textProperty().bind(nextViewModel.selectedDetailProperty())"));
        assertTrue(controller.contains("case \"g1Gc\" -> loadIfPresent(workspace.g1GcViewModel(), recording);"));

        assertTrue(english.contains("nav.g1Gc=G1 GC"));
        assertTrue(english.contains("g1Gc.title=G1 GC"));
        assertTrue(english.contains("g1Gc.detail.title=Region Details"));
        assertTrue(english.contains("g1Gc.column.region=Region"));
        assertTrue(english.contains("g1Gc.column.eventKind=Event"));
        assertTrue(chinese.contains("nav.g1Gc=G1 GC"));
        assertTrue(chinese.contains("g1Gc.title=G1 GC"));
        assertTrue(chinese.contains("g1Gc.detail.title=Region详情"));
        assertTrue(chinese.contains("g1Gc.column.region=Region"));
        assertTrue(chinese.contains("g1Gc.column.eventKind=事件"));
    }

    @Test
    void javaFxEventsShellUsesSplitTableDetailBindingsAndI18n() throws Exception {
        Document document = appShellFxml();
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String english = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages.properties"));
        String chinese = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/i18n/messages_zh_CN.properties"));

        Element javaFxEventsPane = elementByFxId(document, "javaFxEventsPane");
        assertEquals("VBox", javaFxEventsPane.getTagName());
        assertTrue(hasStyleClass(javaFxEventsPane, "page"));
        assertTrue(hasStyleClass(javaFxEventsPane, "split-table-detail-page"));
        assertEquals("TableView", elementByFxId(document, "javaFxEventsPhaseTable").getTagName());
        assertEquals("TableView", elementByFxId(document, "javaFxEventsPulseTable").getTagName());
        assertEquals("TableView", elementByFxId(document, "javaFxEventsInputTable").getTagName());
        assertTrue(hasStyleClass(elementByFxId(document, "javaFxEventsDetailPane"), "detail-panel"));
        assertTrue(hasStyleClass(elementByFxId(document, "javaFxEventsDetailTitleLabel"), "detail-panel-title"));
        assertTrue(hasStyleClass(elementByFxId(document, "javaFxEventsDetailArea"), "detail-panel-body"));

        assertTrue(controller.contains("import com.youngledo.jmcfx.domain.model.JavaFxPulsePhase;"));
        assertTrue(controller.contains("import com.youngledo.jmcfx.ui.jfx.JavaFxEventsViewModel;"));
        assertTrue(controller.contains("private VBox javaFxEventsPane;"));
        assertTrue(controller.contains("private TableView<JavaFxPulsePhase> javaFxEventsPhaseTable;"));
        assertTrue(controller.contains("private TableView<JavaFxPulseSummary> javaFxEventsPulseTable;"));
        assertTrue(controller.contains("private TableView<JavaFxInputEvent> javaFxEventsInputTable;"));
        assertTrue(controller.contains("javaFxEventsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo(\"javaFxEvents\"))"));
        assertTrue(controller.contains("configureJavaFxEventsTables();"));
        assertTrue(controller.contains("javaFxEventsPhaseTable.setItems(nextViewModel.pulsePhasesProperty())"));
        assertTrue(controller.contains("javaFxEventsDetailArea.textProperty().bind(nextViewModel.selectedDetailProperty())"));
        assertTrue(controller.contains("case \"javaFxEvents\" -> loadIfPresent(workspace.javaFxEventsViewModel(), recording);"));

        assertTrue(english.contains("nav.javaFxEvents=JavaFX Events"));
        assertTrue(english.contains("javaFxEvents.title=JavaFX Events"));
        assertTrue(english.contains("javaFxEvents.detail.title=Pulse Phase Details"));
        assertTrue(english.contains("javaFxEvents.column.pulse=Pulse"));
        assertTrue(english.contains("javaFxEvents.column.input=Input"));
        assertTrue(chinese.contains("nav.javaFxEvents=JavaFX事件"));
        assertTrue(chinese.contains("javaFxEvents.title=JavaFX事件"));
        assertTrue(chinese.contains("javaFxEvents.detail.title=脉冲阶段详情"));
        assertTrue(chinese.contains("javaFxEvents.column.pulse=脉冲"));
        assertTrue(chinese.contains("javaFxEvents.column.input=输入"));
    }

    @Test
    void appShellFxmlDoesNotHardcodeVisibleLocalizedText() throws Exception {
        Document document = appShellFxml();

        for (Element element : elements(document).values()) {
            if (element.hasAttribute("text") && !element.getAttribute("text").isEmpty()) {
                failWithHardcodedText(element);
            }
        }
    }

    private void failWithHardcodedText(Element element) {
        throw new AssertionError("Hardcoded text in FXML: " + element.getTagName()
                + " text=\"" + element.getAttribute("text") + "\"");
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
    void loadedFxmlSplitsDetailPanelStyleClassesForCssMatching() throws Exception {
        FXMLLoader loader = loadShell();

        assertLoadedStyleClass(loader, "advancedJfrMemoryDetailTitleLabel", "detail-panel-title");
        assertLoadedStyleClass(loader, "advancedJfrMemoryDetailPane", "detail-panel");
        assertLoadedStyleClass(loader, "analysisDetailExplanationArea", "detail-panel-body");
        assertLoadedStyleClass(loader, "metadataDetailTitleLabel", "detail-panel-title");
        assertLoadedStyleClass(loader, "metadataDetailPane", "detail-panel");
        assertLoadedStyleClass(loader, "heapDumpIssueDetailTitleLabel", "detail-panel-title");
        assertLoadedStyleClass(loader, "heapDumpIssueDetailPane", "detail-panel");
    }

    @Test
    void fxmlDoesNotPackMultipleStyleClassesIntoOneAttribute() throws Exception {
        Document document = appShellFxml();

        elements(document).values().forEach(element -> {
            String styleClass = element.getAttribute("styleClass");
            assertFalse(styleClass.contains(" "),
                    () -> element.getTagName() + " packs multiple JavaFX style classes into one attribute: "
                            + styleClass);
        });
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
        String method = source.substring(source.indexOf("private void showHeapDumpWorkspace"));
        method = method.substring(0, method.indexOf("private void bindAdvancedJfr"));

        assertTrue(method.contains("bindHeapDumpAnalysis(null)"),
                "Closing the last HPROF workspace must clear the previously bound analysis view model");
        String bindMethod = source.substring(source.indexOf("private void bindHeapDumpAnalysis"));
        bindMethod = bindMethod.substring(0, bindMethod.indexOf("private void showOpenHeapDumpChooser"));
        assertTrue(bindMethod.contains("heapDumpAnalysisViewModel = null"),
                "Clearing HPROF binding must also clear the controller-level view model reference");
    }

    @Test
    void fileBackedWorkspaceOpenShortCircuitsWhenPathIsAlreadyOpen() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String openRecording = source.substring(source.indexOf("private void openRecordingInBackground"),
                source.indexOf("PreparedRecordingWorkspace prepareRecordingWorkspace"));
        String openHeapDump = source.substring(source.indexOf("private void openHeapDumpInBackground"),
                source.indexOf("private void showOpenRecordingFailure"));

        assertTrue(openRecording.contains("selectExistingRecordingWorkspace(path)"),
                "Opening an already-open JFR should select the existing workspace before parsing");
        assertTrue(openRecording.indexOf("selectExistingRecordingWorkspace(path)")
                        < openRecording.indexOf("setRecordingOpening(true)"),
                "JFR duplicate detection must happen before showing parse progress");
        assertTrue(openHeapDump.contains("selectExistingHeapDumpWorkspace(path)"),
                "Opening an already-open HPROF should select the existing workspace before analysis");
        assertTrue(openHeapDump.indexOf("selectExistingHeapDumpWorkspace(path)")
                        < openHeapDump.indexOf("setBackgroundWorkVisible(true)"),
                "HPROF duplicate detection must happen before starting analysis progress");
    }

    @Test
    void heapDumpRebindingRemovesOldSelectionListenersBeforeClearingViewModel() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String bindMethod = source.substring(source.indexOf("private void bindHeapDumpAnalysis"));
        bindMethod = bindMethod.substring(0, bindMethod.indexOf("private void showOpenHeapDumpChooser"));

        assertTrue(source.contains("heapDumpTableSelectionListener"));
        assertTrue(source.contains("heapDumpSelectedIssueListener"));
        assertTrue(bindMethod.indexOf("removeListener(heapDumpTableSelectionListener)")
                        < bindMethod.indexOf("heapDumpAnalysisViewModel = null"),
                "Table selection listener must be removed before the HPROF view model reference is cleared");
        assertTrue(bindMethod.contains("removeListener(heapDumpSelectedIssueListener)"),
                "Selected issue listener must be removed when rebinding HPROF workspaces");
        assertFalse(bindMethod.contains("addListener((observable, oldValue, newValue) -> heapDumpAnalysisViewModel"),
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
    }

    @Test
    void openRecordingDialogIsDeferredUntilButtonActionCompletes() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));

        assertTrue(source.contains("Platform.runLater(this::showOpenRecordingChooser)"),
                "native file chooser should open after the button action finishes so pressed styling can clear");
    }

    @Test
    void clearingProfilingSelectionPassesNullThroughToViewModel() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));

        assertFalse(source.contains("profilingViewModel == null || method == null"),
                "Clearing table selection must still clear profiling stack details");
        assertTrue(source.contains("profilingViewModel.selectMethod(method == null ? null : method.method())"),
                "Shell must pass null selection through to ProfilingViewModel");
    }

    @Test
    void homeActionButtonsBothUseLeadingIcons() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));

        assertTrue(source.contains("configureActionButton(homeOpenRecordingButton"),
                "Open recording home action should keep its leading icon");
        assertTrue(source.contains("configureActionButton(homeConnectJvmButton"),
                "Connect JVM home action should use the same leading icon treatment");
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
        String shellController = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));

        assertTrue(liveController.contains("jvmsRefreshButton.setOnAction(event -> refresh())"),
                "Manual refresh button should trigger JVM Browser refresh");
        assertTrue(shellController.contains("selectedSectionProperty().addListener"),
                "Opening JVM Browser should refresh the local JVM list");
        assertTrue(shellController.contains("\"jvms\".equals(newValue) && jvmsPaneController != null"),
                "JVM Browser should refresh when the JVMs section is opened");
        assertTrue(shellController.contains("jvmsPaneController.refresh()"),
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
        String fxml = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/resources/com/youngledo/jmcfx/ui/shell/app-shell.fxml"));

        assertTrue(controller.contains("setOnMouseClicked"), "JVM table should handle double-click connect");
        assertTrue(controller.contains("connectSelectedOrManual()"),
                "Connect button should preserve manual URL priority");
        assertTrue(controller.contains("jvmBrowserViewModel.connectSelected()"),
                "Double-click should connect only the selected local JVM");
        assertFalse(fxml.contains("jvmsStatusLabel"), "Bottom JVM status label should be removed");
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
        assertTrue(shellController.contains("this::openRecordingInBackground"),
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
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String app = java.nio.file.Files.readString(
                java.nio.file.Path.of("../jmc-fx-app/src/main/java/com/youngledo/jmcfx/app/JmcFxApplication.java"));

        assertTrue(factory.contains("MBeanBrowserService mBeanBrowserService"),
                "Factory should accept the MBean Browser port");
        assertTrue(factory.contains("mBeanBrowserService"),
                "Factory should pass the MBean Browser port to AppShellController");
        assertTrue(controller.contains("MBeanBrowserService mBeanBrowserService"),
                "Controller should inject the MBean Browser port");
        assertTrue(controller.contains("flightRecordingService, mBeanBrowserService"),
                "Controller should pass the MBean Browser port to JvmBrowserViewModel");
        assertTrue(app.contains("new JmcMBeanBrowserService(jmxConnectionService)"),
                "Application assembly should reuse the existing JMX connection service for MBeans");
    }

    @Test
    void appShellFactoryInjectsLiveDiagnosticsServices() throws Exception {
        String factory = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellFactory.java"));
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String app = java.nio.file.Files.readString(
                java.nio.file.Path.of("../jmc-fx-app/src/main/java/com/youngledo/jmcfx/app/JmcFxApplication.java"));

        assertTrue(factory.contains("DiagnosticCommandService diagnosticCommandService"),
                "Factory should accept the Diagnostic Command port");
        assertTrue(factory.contains("LiveMetricService liveMetricService"),
                "Factory should accept the Live Metric port");
        assertTrue(controller.contains("DiagnosticCommandService diagnosticCommandService"),
                "Controller should inject the Diagnostic Command port");
        assertTrue(controller.contains("LiveMetricService liveMetricService"),
                "Controller should inject the Live Metric port");
        assertTrue(controller.contains("mBeanBrowserService, diagnosticCommandService, liveMetricService"),
                "Controller should pass diagnostics ports to JvmBrowserViewModel");
        assertTrue(app.contains("new JmcDiagnosticCommandService(jmxConnectionService)"),
                "Application assembly should reuse the existing JMX connection service for diagnostic commands");
        assertTrue(app.contains("new JmcLiveMetricService(jmxConnectionService)"),
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
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String app = java.nio.file.Files.readString(
                java.nio.file.Path.of("../jmc-fx-app/src/main/java/com/youngledo/jmcfx/app/JmcFxApplication.java"));

        assertTrue(factory.contains("AdvancedJfrAnalysisService advancedJfrAnalysisService"),
                "Factory should accept the advanced JFR analysis port");
        assertTrue(controller.contains("AdvancedJfrAnalysisService advancedJfrAnalysisService"),
                "Controller should inject the advanced JFR analysis port");
        assertTrue(controller.contains("new AdvancedJfrViewModel(advancedJfrAnalysisService)"),
                "Controller should create the advanced JFR workspace view model when the port is available");
        assertTrue(app.contains("new JmcAdvancedJfrAnalysisService()"),
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
    void settingsPageContainsThemeSelectorNextToLanguageSelector() throws Exception {
        Document document = appShellFxml();

        assertEquals("36", elementByFxId(document, "settingsPane").getAttribute("spacing"));
        assertEquals("VBox", elementByFxId(document, "settingsLanguageGroup").getTagName());
        assertEquals("16", elementByFxId(document, "settingsLanguageGroup").getAttribute("spacing"));
        assertEquals("VBox", elementByFxId(document, "settingsThemeGroup").getTagName());
        assertEquals("16", elementByFxId(document, "settingsThemeGroup").getAttribute("spacing"));
        assertEquals("24", elementByStyleClass(document, "settings-language-options").getAttribute("spacing"));
        assertEquals("24", elementByStyleClass(document, "settings-theme-options").getAttribute("spacing"));
        assertEquals("Label", elementByFxId(document, "settingsThemeLabel").getTagName());
        assertEquals("RadioButton", elementByFxId(document, "themeFollowSystemRadio").getTagName());
        assertEquals("RadioButton", elementByFxId(document, "themeLightRadio").getTagName());
        assertEquals("RadioButton", elementByFxId(document, "themeDarkRadio").getTagName());
    }

    @Test
    void controllerBindsThemeSelectorAndSystemThemePreference() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String factory = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellFactory.java"));

        assertTrue(source.contains("configureThemeSelector()"),
                "settings page should configure the theme selector");
        assertTrue(source.contains("themeToggleGroup.selectedToggleProperty()"),
                "theme selector should update the shell view model");
        assertTrue(source.contains("settings.theme.followSystem"),
                "theme selector should expose follow-system text");
        assertTrue(factory.contains("colorSchemeProperty().addListener"),
                "system theme mode should listen for JavaFX platform color scheme changes");
    }

    @Test
    void eventTimeFormatterDoesNotShowZoneSuffix() {
        assertEquals("1970-01-01 08:00:00.000",
                AppShellController.formatEventTimeForDisplay(Instant.EPOCH, ZoneId.of("Asia/Shanghai")));
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
        assertEquals(260, AppShellController.DEFAULT_EVENT_TYPES_WIDTH);
    }

    @Test
    void defaultEventTypesDividerKeepsTreePaneNarrow() {
        assertEquals(0.25, AppShellController.DEFAULT_EVENT_TYPES_DIVIDER_POSITION);
    }

    @Test
    void eventTypesPaneWidthConstraintsAllowUserResizing() {
        assertEquals(180, AppShellController.MIN_EVENT_TYPES_WIDTH);
        assertEquals(360, AppShellController.MAX_EVENT_TYPES_WIDTH);
    }

    @Test
    void allEventsSelectionDoesNotSelectEventTypesTreeNode() {
        assertEquals(false, AppShellController.shouldSelectEventTypesTreeNode(EventTypeSelection.ALL_ID));
        assertEquals(false, AppShellController.shouldSelectEventTypesTreeNode(""));
        assertEquals(true, AppShellController.shouldSelectEventTypesTreeNode("jdk.ThreadStart"));
    }

    @Test
    void allEventsSelectionClearsEventTypesTreeSelection() {
        assertEquals(true, AppShellController.shouldClearEventTypesTreeSelection(EventTypeSelection.ALL_ID));
        assertEquals(true, AppShellController.shouldClearEventTypesTreeSelection(""));
        assertEquals(true, AppShellController.shouldClearEventTypesTreeSelection(null));
        assertEquals(false, AppShellController.shouldClearEventTypesTreeSelection("jdk.ThreadStart"));
    }

    @Test
    void eventTypesDividerInitializesOnlyWhenEventsPaneFirstBecomesVisible() {
        assertEquals(true, AppShellController.shouldInitializeEventTypesDivider(false, true));
        assertEquals(false, AppShellController.shouldInitializeEventTypesDivider(false, false));
        assertEquals(false, AppShellController.shouldInitializeEventTypesDivider(true, true));
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

        assertEquals("first-recording.jfr", AppShellController.tabTitleFor(workspace));
    }

    @Test
    void heapDumpTabTitleUsesHeapDumpFileName() {
        HeapDumpWorkspace workspace = new HeapDumpWorkspace(Path.of("/tmp/demo.hprof"), null);

        assertEquals("demo.hprof", AppShellController.tabTitleFor(workspace));
    }

    @Test
    void recordingTabsAreShownOnlyWhenWorkspacesExist() {
        assertFalse(AppShellController.shouldShowRecordingTabs(0));
        assertTrue(AppShellController.shouldShowRecordingTabs(1));
        assertTrue(AppShellController.shouldShowRecordingTabs(2));
    }

    @Test
    void workspaceTabsAreShownWhenAnyWorkspaceExists() {
        assertFalse(AppShellController.shouldShowWorkspaceTabs(0, 0));
        assertTrue(AppShellController.shouldShowWorkspaceTabs(1, 0));
        assertTrue(AppShellController.shouldShowWorkspaceTabs(0, 1));
    }

    @Test
    void emptyTablePlaceholderUsesBlankRegionInsteadOfLocalizedText() {
        Region placeholder = AppShellController.emptyTablePlaceholder();

        assertEquals(Region.class, placeholder.getClass());
        assertFalse(placeholder.isManaged());
    }

    @Test
    void tablePlaceholdersUseLocalizedBindings() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));

        assertTrue(source.contains("localizedTablePlaceholder("));
        assertFalse(source.contains("setPlaceholder(new Label(i18n.get("),
                "table placeholders should update when the UI language changes");
    }

    @Test
    void tlabPlaceholderDoesNotShowEmptyStateBeforeLazyLoadCompletes() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String configureTlabTable = source.substring(source.indexOf("private void configureTlabTable()"),
                source.indexOf("private void bindHeap("));

        assertFalse(configureTlabTable.contains("tlabTable.setPlaceholder(localizedTablePlaceholder(\"tlab.empty\"))"),
                "TLAB is loaded lazily, so its initial placeholder must not say the recording has no TLAB data");
        assertTrue(source.contains("updateTlabTablePlaceholder("),
                "TLAB placeholder must switch to the empty message only after the lazy load completes");
    }

    @Test
    void jvmInternalsTablesUseSharedDisplayFormats() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String jvmTables = source.substring(source.indexOf("private void configureGcSummaryTable()"),
                source.indexOf("// --- JVM Internals: bind methods ---"));

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
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
        String jvmTables = source.substring(source.indexOf("private void configureJvmFlagsTable()"),
                source.indexOf("// --- JVM Internals: bind methods ---"));

        assertFalse(jvmTables.contains("new TableColumn<>(\""),
                "JVM Internals column titles should be localized through i18n bindings");
        assertTrue(jvmTables.contains("localizedColumn("));
    }

    @Test
    void eventDetailFallbackTextComesFromI18n() {
        I18n i18n = new I18n(java.util.Locale.ENGLISH);

        assertEquals("Select an event to inspect timing.", AppShellController.noTimingSelectionText(i18n));

        i18n.setLanguageMode(LanguageMode.CHINESE_SIMPLIFIED);

        assertEquals("选择一个事件以查看时间信息。", AppShellController.noTimingSelectionText(i18n));
    }

    @Test
    void fileChooserStringsComeFromI18n() {
        I18n i18n = new I18n(java.util.Locale.ENGLISH);

        assertEquals("Open JFR Recording", AppShellController.openRecordingChooserTitle(i18n));
        assertEquals("JFR recordings", AppShellController.jfrRecordingsFilterDescription(i18n));
    }

    @Test
    void saveRecordingInitialFileNameUsesRecordingName() {
        assertEquals("jmcfx-42-20260526160235.jfr",
                AppShellController.saveRecordingInitialFileName("jmcfx-42-20260526160235"));
    }

    @Test
    void saveRecordingInitialFileNameSanitizesUnsafeCharacters() {
        assertEquals("My_Recording_01.jfr",
                AppShellController.saveRecordingInitialFileName("My Recording:01"));
    }

    @Test
    void languageModeDisplayNamesFollowCurrentLanguage() {
        I18n i18n = new I18n(java.util.Locale.ENGLISH);

        assertEquals("Follow System", AppShellController.languageModeDisplayName(i18n, LanguageMode.SYSTEM));

        i18n.setLanguageMode(LanguageMode.CHINESE_SIMPLIFIED);

        assertEquals("跟随系统", AppShellController.languageModeDisplayName(i18n, LanguageMode.SYSTEM));
    }

    @Test
    void openingRecordingStatusUsesSelectedFileName() {
        I18n i18n = new I18n(java.util.Locale.ENGLISH);

        assertEquals("Opening recording: sample.jfr",
                AppShellController.openingRecordingStatus(i18n, Path.of("/tmp/sample.jfr")));
    }

    @Test
    void openingHeapDumpStatusUsesSelectedFileName() {
        I18n i18n = new I18n(java.util.Locale.ENGLISH);

        assertEquals("Opening heap dump demo.hprof.",
                AppShellController.openingHeapDumpStatus(i18n, Path.of("demo.hprof")));
    }

    @Test
    void openRecordingButtonIsDisabledOnlyWhileOpening() {
        assertTrue(AppShellController.shouldDisableOpenRecordingButton(true));
        assertFalse(AppShellController.shouldDisableOpenRecordingButton(false));
    }

    @Test
    void preparingRecordingWorkspaceDoesNotPreloadAnalysisPages() {
        AppShellController controller = new AppShellController(
                new AppShellViewModel(),
                new FakeRecordingRepository(),
                new FakeEventQueryService(),
                throwingRuleAnalysisService(),
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                new I18n(java.util.Locale.ENGLISH));

        controller.prepareRecordingWorkspace(Path.of("startup.jfr"));
    }

    @Test
    void sectionLoadingIsQueuedOnRecordingExecutor() {
        QueueingRecordingOpenExecutor executor = new QueueingRecordingOpenExecutor();
        AtomicInteger analysisCalls = new AtomicInteger();
        AppShellController controller = new AppShellController(
                new AppShellViewModel(),
                new FakeRecordingRepository(),
                new FakeEventQueryService(),
                recording -> {
                    analysisCalls.incrementAndGet();
                    return List.of();
                },
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                new I18n(java.util.Locale.ENGLISH),
                executor);
        AppShellController.PreparedRecordingWorkspace prepared = controller.prepareRecordingWorkspace(Path.of("startup.jfr"));
        RecordingWorkspace workspace = new RecordingWorkspace(prepared.recording(), prepared.overview(), prepared.events(),
                prepared.analysis(), prepared.profiling(), prepared.exceptions(), prepared.threads(), prepared.fileio(),
                prepared.socketio(), prepared.locks(), prepared.heap(), prepared.leakSuspects(), prepared.tlab(),
                prepared.jvmInfo(), prepared.gcConfig(), prepared.gcSummary(), prepared.gcDetails(),
                prepared.compilations(), prepared.codeCache(), prepared.classLoading(), prepared.vmOperations(),
                prepared.environment(), prepared.javaAppOverview(), prepared.security(), prepared.nativeLibraries(),
                prepared.threadDumps());

        controller.loadWorkspaceSection(workspace, "analysis");

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
        AppShellController controller = new AppShellController(
                new AppShellViewModel(),
                new FakeRecordingRepository(),
                new FakeEventQueryService(),
                recording -> {
                    analysisCalls.incrementAndGet();
                    return List.of();
                },
                profilingService(profilingCalls),
                null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                new I18n(java.util.Locale.ENGLISH),
                executor);
        AppShellController.PreparedRecordingWorkspace prepared = controller.prepareRecordingWorkspace(Path.of("startup.jfr"));
        RecordingWorkspace workspace = new RecordingWorkspace(prepared.recording(), prepared.overview(), prepared.events(),
                prepared.analysis(), prepared.profiling(), prepared.exceptions(), prepared.threads(), prepared.fileio(),
                prepared.socketio(), prepared.locks(), prepared.heap(), prepared.leakSuspects(), prepared.tlab(),
                prepared.jvmInfo(), prepared.gcConfig(), prepared.gcSummary(), prepared.gcDetails(),
                prepared.compilations(), prepared.codeCache(), prepared.classLoading(), prepared.vmOperations(),
                prepared.environment(), prepared.javaAppOverview(), prepared.security(), prepared.nativeLibraries(),
                prepared.threadDumps());

        controller.loadWorkspaceSection(workspace, "analysis");
        controller.loadWorkspaceSection(workspace, "profiling");

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
        AppShellController controller = new AppShellController(
                new AppShellViewModel(),
                new FakeRecordingRepository(),
                new FakeEventQueryService(),
                recording -> {
                    analysisCalls.incrementAndGet();
                    return List.of();
                },
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                new I18n(java.util.Locale.ENGLISH),
                executor);
        AppShellController.PreparedRecordingWorkspace prepared = controller.prepareRecordingWorkspace(Path.of("startup.jfr"));
        RecordingWorkspace workspace = new RecordingWorkspace(prepared.recording(), prepared.overview(), prepared.events(),
                prepared.analysis(), prepared.profiling(), prepared.exceptions(), prepared.threads(), prepared.fileio(),
                prepared.socketio(), prepared.locks(), prepared.heap(), prepared.leakSuspects(), prepared.tlab(),
                prepared.jvmInfo(), prepared.gcConfig(), prepared.gcSummary(), prepared.gcDetails(),
                prepared.compilations(), prepared.codeCache(), prepared.classLoading(), prepared.vmOperations(),
                prepared.environment(), prepared.javaAppOverview(), prepared.security(), prepared.nativeLibraries(),
                prepared.threadDumps());

        controller.loadWorkspaceSection(workspace, "analysis");
        controller.loadWorkspaceSection(workspace, "overview");

        assertEquals(1, executor.queuedTaskCount());

        executor.runNext();

        assertEquals(0, analysisCalls.get());
    }

    @Test
    void openingRecordingDoesNotPreloadHeavySections() {
        QueueingRecordingOpenExecutor executor = new QueueingRecordingOpenExecutor();
        AppShellController controller = new AppShellController(
                new AppShellViewModel(),
                new FakeRecordingRepository(),
                new FakeEventQueryService(),
                throwingRuleAnalysisService(),
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null,
                new I18n(java.util.Locale.ENGLISH),
                executor);
        AppShellController.PreparedRecordingWorkspace prepared = controller.prepareRecordingWorkspace(Path.of("startup.jfr"));
        RecordingWorkspace workspace = new RecordingWorkspace(prepared.recording(), prepared.overview(), prepared.events(),
                prepared.analysis(), prepared.profiling(), prepared.exceptions(), prepared.threads(), prepared.fileio(),
                prepared.socketio(), prepared.locks(), prepared.heap(), prepared.leakSuspects(), prepared.tlab(),
                prepared.jvmInfo(), prepared.gcConfig(), prepared.gcSummary(), prepared.gcDetails(),
                prepared.compilations(), prepared.codeCache(), prepared.classLoading(), prepared.vmOperations(),
                prepared.environment(), prepared.javaAppOverview(), prepared.security(), prepared.nativeLibraries(),
                prepared.threadDumps());

        controller.preloadRecordingWorkspace(workspace);

        assertEquals(0, executor.queuedTaskCount());
    }

    private static Document appShellFxml() throws ParserConfigurationException, SAXException, IOException {
        return fxml("app-shell.fxml");
    }

    private static FXMLLoader loadShell() throws IOException {
        FXMLLoader loader = new FXMLLoader(AppShellController.class.getResource("app-shell.fxml"));
        AppShellFactory factory = new AppShellFactory(new FakeRecordingRepository(), new FakeEventQueryService(),
                new FakeRuleAnalysisService());
        AppShellViewModel viewModel = new AppShellViewModel();
        loader.setControllerFactory(type -> {
            if (type == AppShellController.class) {
                return factory.controllerFor(type, viewModel);
            }
            if (type == LiveJvmPaneController.class) {
                return new LiveJvmPaneController();
            }
            throw new IllegalArgumentException("Unsupported controller: " + type.getName());
        });
        loader.<Region>load();
        return loader;
    }

    private static void assertLoadedStyleClass(FXMLLoader loader, String nodeId, String styleClass) {
        Object loaded = loader.getNamespace().get(nodeId);
        assertTrue(loaded instanceof Node, "Missing loaded node " + nodeId);
        Node node = (Node) loaded;
        assertTrue(node.getStyleClass().contains(styleClass),
                () -> nodeId + " loaded style classes " + node.getStyleClass() + " must contain " + styleClass);
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

    private static ProfilingService profilingService(AtomicInteger calls) {
        return new ProfilingService() {
            @Override
            public List<com.youngledo.jmcfx.domain.model.HotMethod> loadHotMethods(RecordingSummary recording) {
                calls.incrementAndGet();
                return List.of();
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

    private static Document fxml(String name) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        try (InputStream stream = AppShellController.class.getResourceAsStream(name)) {
            return factory.newDocumentBuilder().parse(stream);
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

    private static Element elementByFxId(Document document, String fxId) {
        return elements(document).entrySet().stream()
                .filter(entry -> fxId.equals(entry.getValue().getAttribute("fx:id")))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing fx:id " + fxId));
    }

    private static Element elementByFxIdOrNull(Document document, String fxId) {
        return elements(document).entrySet().stream()
                .filter(entry -> fxId.equals(entry.getValue().getAttribute("fx:id")))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static boolean hasFxId(Document document, String fxId) {
        return elements(document).values().stream()
                .anyMatch(element -> fxId.equals(element.getAttribute("fx:id")));
    }

    private static Element elementByStyleClass(Document document, String styleClass) {
        return elements(document).entrySet().stream()
                .filter(entry -> hasStyleClass(entry.getValue(), styleClass))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing styleClass " + styleClass));
    }

    private static Element childElement(Element parent, String tagName) {
        return childElements(parent, tagName).stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing child " + tagName + " in " + parent.getTagName()));
    }

    private static List<Element> childElements(Element parent, String tagName) {
        List<Element> result = new java.util.ArrayList<>();
        for (org.w3c.dom.Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && tagName.equals(element.getTagName())) {
                result.add(element);
            }
        }
        return result;
    }

    private static void assertFlameGraphTab(Document document, String containerFxId) {
        Element container = elementByFxId(document, containerFxId);
        assertEquals("VBox", container.getTagName());
        assertTrue(hasStyleClass(container, "profiling-flame-container"));
        Element scrollPane = (Element) container.getParentNode();
        assertEquals("ScrollPane", scrollPane.getTagName());
        assertEquals("true", scrollPane.getAttribute("fitToWidth"));
        assertEquals("true", scrollPane.getAttribute("fitToHeight"));
        Element tabContent = (Element) scrollPane.getParentNode();
        assertEquals("VBox", tabContent.getTagName());
        assertTrue(hasStyleClass(tabContent, "profiling-graph-tab-content"));
        Element toolbar = childElements(tabContent, "HBox").getFirst();
        assertTrue(hasStyleClass(toolbar, "profiling-graph-toolbar"));
        assertEquals("CENTER_LEFT", toolbar.getAttribute("alignment"));
    }

    private static void assertJvmBrowserJdpI18n(String bundle, String... expectedLines) {
        for (String expectedLine : expectedLines) {
            assertTrue(bundle.contains(expectedLine), () -> "Missing i18n line: " + expectedLine);
        }
    }

    private static int elementCountWithStyleClass(Document document, String styleClass) {
        return (int) elements(document).values().stream()
                .filter(element -> hasStyleClass(element, styleClass))
                .count();
    }

    private static int elementCountWithFxId(Document document, String fxId) {
        return (int) elements(document).values().stream()
                .filter(element -> fxId.equals(element.getAttribute("fx:id")))
                .count();
    }

    private static boolean hasStyleClass(Element element, String styleClass) {
        if (styleClass.equals(element.getAttribute("styleClass"))) {
            return true;
        }
        for (Element child : childElements(element, "styleClass")) {
            for (Element item : childElements(child, "String")) {
                if (styleClass.equals(item.getAttribute("fx:value"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Map<Integer, Element> elements(Document document) {
        org.w3c.dom.NodeList nodes = document.getElementsByTagName("*");
        java.util.HashMap<Integer, Element> elements = new java.util.HashMap<>();
        for (int index = 0; index < nodes.getLength(); index++) {
            elements.put(index, (Element) nodes.item(index));
        }
        return elements;
    }
}
