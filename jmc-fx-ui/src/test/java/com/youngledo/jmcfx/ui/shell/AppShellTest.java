package com.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.youngledo.jmcfx.ui.util.DisplayFormats;
import com.youngledo.jmcfx.domain.model.EventTypeSelection;
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
                null, null, null,
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
        assertFalse(elementByFxId(document, "homeOpenRecordingButton").hasAttribute("styleClass"),
                "Home action buttons should keep JavaFX's default button style class; add local hooks in controller");
        assertFalse(elementByFxId(document, "homeConnectJvmButton").hasAttribute("styleClass"),
                "Home action buttons should keep JavaFX's default button style class; add local hooks in controller");
        assertFalse(elementByFxId(document, "homeConnectJvmButton").hasAttribute("disable"),
                "Connect JVM should be enabled now that the JVM browser page exists");
        assertEquals("Button", elementByFxId(document, "jvmsRefreshButton").getTagName());
        assertEquals("TextField", elementByFxId(document, "jvmsManualUrlField").getTagName());
        assertEquals("Button", elementByFxId(document, "jvmsConnectButton").getTagName());
        assertEquals("Button", elementByFxId(document, "jvmsDisconnectButton").getTagName());
        assertEquals("TableView", elementByFxId(document, "jvmsTable").getTagName());
        assertEquals(0, elementCountWithFxId(document, "statusLabel"));
        assertEquals(0, elementCountWithFxId(document, "taskSummaryLabel"));
        assertEquals("tlabTimelineContainer", elementByFxId(document, "tlabTimelineContainer").getAttribute("fx:id"));
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
    void openRecordingDialogIsDeferredUntilButtonActionCompletes() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));

        assertTrue(source.contains("Platform.runLater(this::showOpenRecordingChooser)"),
                "native file chooser should open after the button action finishes so pressed styling can clear");
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
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));

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
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));

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
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));

        assertTrue(source.contains("jvmsRefreshButton.setOnAction(event -> refreshJvmBrowser())"),
                "Manual refresh button should trigger JVM Browser refresh");
        assertTrue(source.contains("selectedSectionProperty().addListener"),
                "Opening JVM Browser should refresh the local JVM list");
        assertTrue(source.contains("\"jvms\".equals(newValue)"),
                "JVM Browser should refresh when the JVMs section is opened");
        assertFalse(source.contains("JVM_BROWSER_REFRESH_INTERVAL_SECONDS"),
                "JVM Browser should not run periodic refresh");
        assertFalse(source.contains("startJvmBrowserRefresh"),
                "JVM Browser should not use timer-based refresh");
        assertFalse(source.contains("jvmBrowserFirstRefreshDone"),
                "JVM Browser should refresh every time the section opens");
    }

    @Test
    void jvmBrowserEmptyPlaceholderWaitsForCompletedRefresh() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));

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
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));
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
    void jvmBrowserFormatsStateAndSourceThroughI18n() throws Exception {
        String controller = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java"));

        assertTrue(controller.contains("formatJvmState"), "State column should use localized display labels");
        assertTrue(controller.contains("formatJvmSource"), "Source column should use localized display labels");
        assertFalse(controller.contains("state().name()"), "State column must not show raw enum names");
        assertFalse(controller.contains("source().name()"), "Source column must not show raw enum names");
    }

    @Test
    void sidebarSearchAndThemeControlsAreEnabled() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/com/youngledo/jmcfx/ui/shell/AppSidebar.java"));

        assertFalse(source.contains("searchButton.setDisable(true)"),
                "sidebar search should be a usable navigation search control");
        assertFalse(source.contains("themeButton.setDisable(true)"),
                "theme button should toggle Primer light/dark themes");
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
        int start = css.indexOf(selector + " {");
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
