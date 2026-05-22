package com.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.youngledo.jmcfx.ui.util.DisplayFormats;
import com.youngledo.jmcfx.domain.model.EventTypeSelection;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.i18n.LanguageMode;
import com.youngledo.jmcfx.ui.overview.OverviewViewModel;
import com.youngledo.jmcfx.ui.rules.RuleResultsViewModel;
import com.youngledo.jmcfx.testsupport.FakeEventQueryService;
import com.youngledo.jmcfx.testsupport.FakeRecordingRepository;
import com.youngledo.jmcfx.testsupport.FakeRuleAnalysisService;

import javafx.scene.layout.Region;

class AppShellTest {

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
    void shellControllerDefaultsToEnglishUiLocale() {
        AppShellController controller = new AppShellController(
                new AppShellViewModel(),
                new FakeRecordingRepository(),
                new FakeEventQueryService(),
                new FakeRuleAnalysisService(),
                null, null, null,
                null, null, null,
                null, null, null,
                null,
                new I18n(java.util.Locale.SIMPLIFIED_CHINESE));

        assertEquals(java.util.Locale.ENGLISH, controller.i18n().localeProperty().get());
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
        assertEquals("TabPane", elementByFxId(document, "recordingTabs").getTagName());
        assertEquals("StackPane", elementByFxId(document, "workspaceStack").getTagName());

        Element homePane = elementByFxId(document, "homePane");
        assertEquals("18", homePane.getAttribute("spacing"));
        assertTrue(hasStyleClass(homePane, "welcome-pane"));
        assertTrue(hasStyleClass(elementByFxId(document, "homeKickerLabel"), "home-kicker"));
        assertTrue(hasStyleClass(elementByFxId(document, "homeTitleLabel"), "welcome-title"));
        assertTrue(hasStyleClass(elementByFxId(document, "homeSubtitleLabel"), "welcome-subtitle"));
        assertTrue(hasStyleClass(elementByFxId(document, "homeOpenWorkflowTitleLabel"), "workflow-tile-title"));
        assertTrue(hasStyleClass(elementByFxId(document, "homeEventsWorkflowTitleLabel"), "workflow-tile-title"));
        assertTrue(hasStyleClass(elementByFxId(document, "homeJvmWorkflowDescriptionLabel"), "workflow-tile-copy"));
        assertTrue("true".equals(elementByFxId(document, "homeJvmWorkflowDescriptionLabel").getAttribute("wrapText")));

        assertEquals("analysisPane", elementByFxId(document, "analysisPane").getAttribute("fx:id"));
        assertEquals("homeOpenRecordingButton", elementByFxId(document, "homeOpenRecordingButton").getAttribute("fx:id"));
        assertEquals("taskSummaryLabel", elementByFxId(document, "taskSummaryLabel").getAttribute("fx:id"));
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
        assertTrue(css.contains(".sidebar-recording-card"));
        assertTrue(css.contains(".app-nav-tree"));
        assertTrue(css.contains(".app-nav-tree-cell"));
        assertTrue(css.contains(":group"));
        assertTrue(css.contains(":unavailable"));
        assertTrue(css.contains(".sidebar-footer"));
        assertTrue(css.contains(".toolbar-primary"));
        assertTrue(css.contains(".recording-tabs"));
        assertTrue(css.contains(".home-hero"));
        assertTrue(css.contains(".workflow-tile"));
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
                null, null);

        assertEquals("first-recording.jfr", AppShellController.tabTitleFor(workspace));
    }

    @Test
    void recordingTabsAreShownOnlyWhenWorkspacesExist() {
        assertFalse(AppShellController.shouldShowRecordingTabs(0));
        assertTrue(AppShellController.shouldShowRecordingTabs(1));
        assertTrue(AppShellController.shouldShowRecordingTabs(2));
    }

    @Test
    void emptyTablePlaceholderUsesBlankRegionInsteadOfLocalizedText() {
        Region placeholder = AppShellController.emptyTablePlaceholder();

        assertEquals(Region.class, placeholder.getClass());
        assertFalse(placeholder.isManaged());
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
    void languageModeDisplayNamesFollowCurrentLanguage() {
        I18n i18n = new I18n(java.util.Locale.ENGLISH);

        assertEquals("Follow System", AppShellController.languageModeDisplayName(i18n, LanguageMode.SYSTEM));

        i18n.setLanguageMode(LanguageMode.CHINESE_SIMPLIFIED);

        assertEquals("跟随系统", AppShellController.languageModeDisplayName(i18n, LanguageMode.SYSTEM));
    }

    private static Document appShellFxml() throws ParserConfigurationException, SAXException, IOException {
        return fxml("app-shell.fxml");
    }

    private static RecordingSummary recording(String id, String fileName) {
        return new RecordingSummary(id, Path.of(fileName), fileName,
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);
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

    private static Element elementByFxId(Document document, String fxId) {
        return elements(document).entrySet().stream()
                .filter(entry -> fxId.equals(entry.getValue().getAttribute("fx:id")))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing fx:id " + fxId));
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
        return (" " + element.getAttribute("styleClass") + " ").contains(" " + styleClass + " ");
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
