package com.youngledo.jmcfx.ui.shell;

import com.youngledo.jmcfx.domain.model.*;
import com.youngledo.jmcfx.ui.profiling.CallGraphDirection;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

final class AppShellView {
    final BorderPane root = new BorderPane();
    final Button homeOpenRecordingButton = new Button();
    final Button homeOpenHeapDumpButton = new Button();
    final Button homeConnectJvmButton = new Button();
    final AppSidebar sidebar = new AppSidebar();
    final TabPane recordingTabs = new TabPane();
    final StackPane workspaceStack = new StackPane();
    final VBox homePane = new VBox();
    final VBox overviewPane = new VBox();
    final VBox eventsPane = new VBox();
    final VBox analysisPane = new VBox();
    final VBox metadataPane = new VBox();
    final VBox advancedJfrPane = new VBox();
    final VBox heapDumpAnalysisPane = new VBox();
    final VBox jvmsPaneHost = new VBox();
    final VBox javaApplicationPane = new VBox();
    final VBox jvmInternalsPane = new VBox();
    final VBox environmentPane = new VBox();
    final VBox profilingPane = new VBox();
    final VBox exceptionsPane = new VBox();
    final VBox threadsPane = new VBox();
    final VBox fileioPane = new VBox();
    final VBox socketioPane = new VBox();
    final VBox locksPane = new VBox();
    final VBox threadHistogramPane = new VBox();
    final VBox securityPane = new VBox();
    final VBox nativeLibrariesPane = new VBox();
    final VBox threadDumpsPane = new VBox();
    final VBox heapPane = new VBox();
    final VBox leaksPane = new VBox();
    final VBox tlabPane = new VBox();
    final VBox jvmInfoPane = new VBox();
    final VBox gcConfigPane = new VBox();
    final VBox gcSummaryPane = new VBox();
    final VBox gcDetailsPane = new VBox();
    final VBox g1GcPane = new VBox();
    final VBox javaFxEventsPane = new VBox();
    final VBox compilationsPane = new VBox();
    final VBox codeCachePane = new VBox();
    final VBox classLoadingPane = new VBox();
    final VBox vmOperationsPane = new VBox();
    final VBox processesPane = new VBox();
    final VBox envVarsPane = new VBox();
    final VBox sysPropsPane = new VBox();
    final VBox recordingInfoPane = new VBox();
    final VBox agentsPane = new VBox();
    final VBox constantPoolsPane = new VBox();
    final VBox settingsPane = new VBox();
    final ProgressBar progressBar = new ProgressBar(0);
    final Label homeKickerLabel = new Label();
    final Label homeTitleLabel = new Label();
    final Label homeSubtitleLabel = new Label();
    final Label homeOpenWorkflowTitleLabel = new Label();
    final Label homeOpenWorkflowDescriptionLabel = new Label();
    final Label homeHeapDumpWorkflowTitleLabel = new Label();
    final Label homeHeapDumpWorkflowDescriptionLabel = new Label();
    final Label homeJvmWorkflowTitleLabel = new Label();
    final Label homeJvmWorkflowDescriptionLabel = new Label();
    final Label homeDisclaimerLabel = new Label();
    final VBox homeJfrTile = new VBox();
    final VBox homeHeapDumpTile = new VBox();
    final VBox homeJvmTile = new VBox();
    final Label overviewTitleLabel = new Label();
    final Label overviewRecordingNameLabel = new Label();
    final Label overviewRecordingDetailsLabel = new Label();
    final Label overviewAnalysisTitleLabel = new Label();
    final Label overviewAnalysisStatusLabel = new Label();
    final Label overviewJvmsTitleLabel = new Label();
    final Label overviewJvmStatusLabel = new Label();
    final Label eventsTitleLabel = new Label();
    final TreeView<EventTypeNode> eventTypesTree = new TreeView<>();
    final TextField eventSearchField = new TextField();
    final TextField threadFilterField = new TextField();
    final TextField fieldFilterField = new TextField();
    final Button clearEventFiltersButton = new Button();
    final MenuButton columnsButton = new MenuButton();
    final SplitPane eventsSplitPane = new SplitPane();
    final TableView<EventRow> eventsTable = denseTable();
    final Label eventWindowStatusLabel = new Label();
    final TabPane eventDetailsTabs = new TabPane();
    final Tab eventPropertiesTab = tab();
    final Tab eventTimingTab = tab();
    final Tab eventThreadTab = tab();
    final Tab eventStackTraceTab = tab();
    final TableView<EventProperty> eventPropertiesTable = denseTable();
    final Label eventTimingLabel = new Label();
    final Label eventThreadLabel = new Label();
    final ListView<String> eventStackTraceList = new ListView<>();
    final Label analysisTitleLabel = new Label();
    final TextField analysisSearchField = new TextField();
    final Label analysisMinimumScoreLabel = new Label();
    final Spinner<Integer> analysisMinimumScoreSpinner = new Spinner<>();
    final CheckBox analysisShowOkCheckBox = new CheckBox();
    final CheckBox analysisShowIgnoredCheckBox = new CheckBox();
    final CheckBox analysisShowUnavailableCheckBox = new CheckBox();
    final TableView<RuleResult> analysisTable = denseTable();
    final Label analysisDetailExplanationCaption = new Label();
    final TextArea analysisDetailExplanationArea = textArea();
    final Label analysisDetailEvidenceCaption = new Label();
    final TextArea analysisDetailEvidenceArea = textArea();
    final Label analysisDetailRecommendationCaption = new Label();
    final TextArea analysisDetailRecommendationArea = textArea();
    final Label metadataTitleLabel = new Label();
    final Label metadataSummaryLabel = new Label();
    final TableView<JfrMetadataEventType> metadataEventTypesTable = denseTable();
    final Label metadataDetailTitleLabel = new Label();
    final TextArea metadataDetailArea = textArea();
    final Label advancedJfrTitleLabel = new Label();
    final Label advancedJfrSummaryLabel = new Label();
    final TabPane advancedJfrTabs = new TabPane();
    final Tab advancedJfrHeatmapTab = tab();
    final Tab advancedJfrMemoryTab = tab();
    final VBox advancedJfrHeatmapContainer = new VBox();
    final Label advancedJfrSelectionTitleLabel = new Label();
    final Label advancedJfrSelectedEventTypeCaptionLabel = new Label();
    final Label advancedJfrSelectedEventTypeLabel = new Label();
    final Label advancedJfrSelectedCountCaptionLabel = new Label();
    final Label advancedJfrSelectedCountLabel = new Label();
    final Label advancedJfrMemorySummaryLabel = new Label();
    final TableView<MemoryIssue> advancedJfrMemoryTable = denseTable();
    final Label advancedJfrMemoryDetailTitleLabel = new Label();
    final TextArea advancedJfrMemoryDetailArea = textArea();
    final Label javaApplicationTitleLabel = new Label();
    final Label javaApplicationSummaryLabel = new Label();
    final Label javaApplicationProfilingTitleLabel = new Label();
    final Label javaApplicationProfilingSummaryLabel = new Label();
    final Button javaApplicationProfilingButton = new Button();
    final Label javaApplicationIoTitleLabel = new Label();
    final Label javaApplicationIoSummaryLabel = new Label();
    final Button javaApplicationIoButton = new Button();
    final Label javaApplicationLocksTitleLabel = new Label();
    final Label javaApplicationLocksSummaryLabel = new Label();
    final Button javaApplicationLocksButton = new Button();
    final Label javaApplicationThreadsTitleLabel = new Label();
    final Label javaApplicationThreadsSummaryLabel = new Label();
    final Button javaApplicationThreadsButton = new Button();
    final Label javaApplicationExceptionsTitleLabel = new Label();
    final Label javaApplicationExceptionsSummaryLabel = new Label();
    final Button javaApplicationExceptionsButton = new Button();
    final Label javaApplicationClassLoadingTitleLabel = new Label();
    final Label javaApplicationClassLoadingSummaryLabel = new Label();
    final Button javaApplicationClassLoadingButton = new Button();
    final Label javaApplicationAllocationTitleLabel = new Label();
    final Label javaApplicationAllocationSummaryLabel = new Label();
    final Button javaApplicationAllocationButton = new Button();
    final Label jvmInternalsTitleLabel = new Label();
    final Label jvmInternalsSummaryLabel = new Label();
    final Label jvmInternalsInformationTitleLabel = new Label();
    final Label jvmInternalsInformationSummaryLabel = new Label();
    final Button jvmInternalsInformationButton = new Button();
    final Label jvmInternalsGcTitleLabel = new Label();
    final Label jvmInternalsGcSummaryLabel = new Label();
    final Button jvmInternalsGcButton = new Button();
    final Label jvmInternalsG1TitleLabel = new Label();
    final Label jvmInternalsG1SummaryLabel = new Label();
    final Button jvmInternalsG1Button = new Button();
    final Label jvmInternalsCompilationTitleLabel = new Label();
    final Label jvmInternalsCompilationSummaryLabel = new Label();
    final Button jvmInternalsCompilationButton = new Button();
    final Label jvmInternalsCodeCacheTitleLabel = new Label();
    final Label jvmInternalsCodeCacheSummaryLabel = new Label();
    final Button jvmInternalsCodeCacheButton = new Button();
    final Label jvmInternalsClassLoadingTitleLabel = new Label();
    final Label jvmInternalsClassLoadingSummaryLabel = new Label();
    final Button jvmInternalsClassLoadingButton = new Button();
    final Label jvmInternalsVmOperationsTitleLabel = new Label();
    final Label jvmInternalsVmOperationsSummaryLabel = new Label();
    final Button jvmInternalsVmOperationsButton = new Button();
    final Label environmentTitleLabel = new Label();
    final Label environmentSummaryLabel = new Label();
    final Label environmentProcessesTitleLabel = new Label();
    final Label environmentProcessesSummaryLabel = new Label();
    final Button environmentProcessesButton = new Button();
    final Label environmentVariablesTitleLabel = new Label();
    final Label environmentVariablesSummaryLabel = new Label();
    final Button environmentVariablesButton = new Button();
    final Label environmentPropertiesTitleLabel = new Label();
    final Label environmentPropertiesSummaryLabel = new Label();
    final Button environmentPropertiesButton = new Button();
    final Label environmentRecordingTitleLabel = new Label();
    final Label environmentRecordingSummaryLabel = new Label();
    final Button environmentRecordingButton = new Button();
    final Label environmentAgentsTitleLabel = new Label();
    final Label environmentAgentsSummaryLabel = new Label();
    final Button environmentAgentsButton = new Button();
    final Label environmentConstantPoolsTitleLabel = new Label();
    final Label environmentConstantPoolsSummaryLabel = new Label();
    final Button environmentConstantPoolsButton = new Button();
    final Label heapDumpAnalysisTitleLabel = new Label();
    final TableView<HeapDumpIssue> heapDumpIssuesTable = denseTable();
    final TabPane heapDumpDetailsTabs = new TabPane();
    final Tab heapDumpIssueDetailTab = tab();
    final Tab heapDumpTextReportTab = tab();
    final Label heapDumpIssueDetailTitleLabel = new Label();
    final TextArea heapDumpIssueDetailArea = textArea();
    final TextArea heapDumpTextReportArea = textArea();
    final Label profilingTitleLabel = new Label();
    final TableView<HotMethod> profilingTable = denseTable();
    final TabPane profilingTreeTabs = new TabPane();
    final Tab profilingCallGraphTab = tab();
    final HBox profilingCallGraphToolbar = new HBox();
    final ComboBox<CallGraphDirection> profilingCallGraphDirectionCombo = new ComboBox<>();
    final Label profilingCallGraphDepthLabel = new Label();
    final Spinner<Integer> profilingCallGraphDepthSpinner = new Spinner<>();
    final Button profilingCallGraphZoomOutButton = new Button();
    final Button profilingCallGraphResetZoomButton = new Button();
    final Button profilingCallGraphZoomInButton = new Button();
    final Button profilingCallGraphFitButton = new Button();
    final ScrollPane profilingCallGraphScrollPane = new ScrollPane();
    final VBox profilingCallGraphContainer = new VBox();
    final Tab profilingDependencyGraphTab = tab();
    final HBox profilingDependencyToolbar = new HBox();
    final Label profilingDependencyDepthLabel = new Label();
    final Spinner<Integer> profilingDependencyDepthSpinner = new Spinner<>();
    final Button profilingDependencyZoomOutButton = new Button();
    final Button profilingDependencyResetZoomButton = new Button();
    final Button profilingDependencyZoomInButton = new Button();
    final Button profilingDependencyFitButton = new Button();
    final TableView<DependencyGraphEdge> profilingDependencyTable = denseTable();
    final ScrollPane profilingDependencyGraphScrollPane = new ScrollPane();
    final VBox profilingDependencyGraphContainer = new VBox();
    final Tab profilingCallersFlameTab = tab();
    final HBox profilingCallersFlameToolbar = new HBox();
    final Button profilingCallersFlameOrientationButton = new Button();
    final Button profilingCallersFlameZoomOutButton = new Button();
    final Button profilingCallersFlameResetZoomButton = new Button();
    final Button profilingCallersFlameZoomInButton = new Button();
    final Button profilingCallersFlameFitButton = new Button();
    final VBox profilingCallersFlameContainer = new VBox();
    final Tab profilingCalleesFlameTab = tab();
    final HBox profilingCalleesFlameToolbar = new HBox();
    final Button profilingCalleesFlameOrientationButton = new Button();
    final Button profilingCalleesFlameZoomOutButton = new Button();
    final Button profilingCalleesFlameResetZoomButton = new Button();
    final Button profilingCalleesFlameZoomInButton = new Button();
    final Button profilingCalleesFlameFitButton = new Button();
    final VBox profilingCalleesFlameContainer = new VBox();
    final Tab profilingCallersTab = tab();
    final TreeView<StackTreeNode> profilingCallersTree = new TreeView<>();
    final Tab profilingCalleesTab = tab();
    final TreeView<StackTreeNode> profilingCalleesTree = new TreeView<>();
    final Label exceptionsTitleLabel = new Label();
    final Button exceptionsGroupByClass = new Button();
    final Button exceptionsGroupByMessage = new Button();
    final Button exceptionsGroupByClassAndMessage = new Button();
    final TableView<ExceptionSummary> exceptionsTable = denseTable();
    final VBox exceptionsTimelineContainer = new VBox();
    final Label threadsTitleLabel = new Label();
    final TableView<ThreadSummary> threadsTable = denseTable();
    final Label fileioTitleLabel = new Label();
    final TabPane fileioTabPane = new TabPane();
    final Tab fileioTimelineTab = tab();
    final VBox fileioTimelineContainer = new VBox();
    final Tab fileioDurationTab = tab();
    final TableView<FileIOHistogram> fileioHistogramTable = denseTable();
    final Tab fileioEventLogTab = tab();
    final TableView<FileIOEvent> fileioEventTable = denseTable();
    final Label socketioTitleLabel = new Label();
    final HBox socketioGroupingBar = new HBox();
    final Button socketioGroupByHostAndPort = new Button();
    final Button socketioGroupByHost = new Button();
    final Button socketioGroupByPort = new Button();
    final TabPane socketioTabPane = new TabPane();
    final Tab socketioTimelineTab = tab();
    final VBox socketioTimelineContainer = new VBox();
    final Tab socketioDurationTab = tab();
    final TableView<SocketIOHistogram> socketioHistogramTable = denseTable();
    final Tab socketioEventLogTab = tab();
    final TableView<SocketIOEvent> socketioEventTable = denseTable();
    final Label locksTitleLabel = new Label();
    final HBox locksGroupingBar = new HBox();
    final Button locksGroupByClass = new Button();
    final Button locksGroupByAddress = new Button();
    final Button locksGroupByThread = new Button();
    final TabPane locksTabPane = new TabPane();
    final Tab locksByClassTab = tab();
    final TableView<LockHistogram> locksByClassTable = denseTable();
    final Tab locksByAddressTab = tab();
    final TableView<LockHistogram> locksByAddressTable = denseTable();
    final Tab locksByThreadTab = tab();
    final TableView<LockHistogram> locksByThreadTable = denseTable();
    final Label threadHistogramTitleLabel = new Label();
    final VBox threadHistogramChartContainer = new VBox();
    final TableView<ThreadHistogramRow> threadHistogramTable = denseTable();
    final Label securityTitleLabel = new Label();
    final TableView<X509CertificateEntry> securityTable = denseTable();
    final Label nativeLibrariesTitleLabel = new Label();
    final TableView<NativeLibraryEntry> nativeLibrariesTable = denseTable();
    final Label threadDumpsTitleLabel = new Label();
    final TableView<ThreadDumpEntry> threadDumpsTable = denseTable();
    final TextArea threadDumpTextArea = textArea();
    final Label heapTitleLabel = new Label();
    final TableView<HeapClassHistogram> heapTable = denseTable();
    final VBox heapTimelineContainer = new VBox();
    final Label leaksTitleLabel = new Label();
    final TableView<LeakCandidate> leaksTable = denseTable();
    final TreeView<LeakReferenceNode> leaksReferenceTree = new TreeView<>();
    final Label tlabTitleLabel = new Label();
    final TableView<TlabAllocation> tlabTable = denseTable();
    final VBox tlabTimelineContainer = new VBox();
    final TableView<JvmFlag> jvmFlagsTable = denseTable();
    final TableView<JvmFlagChange> jvmFlagChangesTable = denseTable();
    final TableView<GcSummary> gcSummaryTable = denseTable();
    final TableView<GcEvent> gcEventsTable = denseTable();
    final TableView<GcReferenceStat> gcReferenceStatsTable = denseTable();
    final TableView<GcHeapSummary> gcHeapSummaryTable = denseTable();
    final Label g1GcTitleLabel = new Label();
    final Label g1GcSummaryLabel = new Label();
    final Label g1GcRegionStatesLabel = new Label();
    final Label g1GcRegionSummaryLabel = new Label();
    final Label g1GcPausesLabel = new Label();
    final TableView<G1GcRegionSummary> g1GcRegionSummaryTable = denseTable();
    final TableView<G1GcRegionState> g1GcRegionStatesTable = denseTable();
    final TableView<GcEvent> g1GcPauseTable = denseTable();
    final Label g1GcDetailTitleLabel = new Label();
    final TextArea g1GcDetailArea = textArea();
    final Label javaFxEventsTitleLabel = new Label();
    final Label javaFxEventsSummaryLabel = new Label();
    final Label javaFxEventsPhaseLabel = new Label();
    final Label javaFxEventsPulseLabel = new Label();
    final Label javaFxEventsInputLabel = new Label();
    final TableView<JavaFxPulsePhase> javaFxEventsPhaseTable = denseTable();
    final TableView<JavaFxPulseSummary> javaFxEventsPulseTable = denseTable();
    final TableView<JavaFxInputEvent> javaFxEventsInputTable = denseTable();
    final Label javaFxEventsDetailTitleLabel = new Label();
    final TextArea javaFxEventsDetailArea = textArea();
    final VBox gcHeapChartContainer = new VBox();
    final VBox gcMetaspaceChartContainer = new VBox();
    final VBox gcPauseChartContainer = new VBox();
    final TableView<CompilationEvent> compilationsTable = denseTable();
    final VBox compilationDurationChartContainer = new VBox();
    final TableView<CompilationEvent> compilationFailuresTable = denseTable();
    final TableView<CodeCacheSweep> codeCacheSweepsTable = denseTable();
    final VBox codeCacheEntriesChartContainer = new VBox();
    final VBox codeCacheSweepChartContainer = new VBox();
    final TableView<CodeCacheStats> codeCacheStatsTable = denseTable();
    final TableView<ClassloaderSummary> classLoadingHistogramTable = denseTable();
    final VBox classLoadingChartContainer = new VBox();
    final TableView<ClassloadEvent> classLoadingEventsTable = denseTable();
    final TableView<ClassloaderStatistics> classLoadingStatsTable = denseTable();
    final TableView<VmOperationSummary> vmOperationSummaryTable = denseTable();
    final TableView<VmOperationEvent> vmOperationEventsTable = denseTable();
    final TableView<ProcessInfo> processesTable = denseTable();
    final TableView<EnvironmentVariable> envVarsTable = denseTable();
    final TextField envVarsSearchField = new TextField();
    final TableView<SystemProperty> sysPropsTable = denseTable();
    final TextField sysPropsSearchField = new TextField();
    final TableView<ActiveRecordingInfo> recordingsTable = denseTable();
    final TableView<ActiveSetting> settingsTable = denseTable();
    final TabPane recordingInfoTabs = new TabPane();
    final Tab recordingInfoRecordingsTab = tab();
    final Tab recordingInfoSettingsTab = tab();
    final TableView<AgentInfo> agentsTable = denseTable();
    final TableView<ConstantPoolType> constantPoolsTable = denseTable();
    final Label jvmInfoTitleLabel = new Label();
    final Label jvmFlagsLabel = new Label();
    final Label jvmFlagChangesLabel = new Label();
    final Label gcConfigTitleLabel = new Label();
    final Label gcConfigDescriptionLabel = new Label();
    final Label gcSummaryTitleLabel = new Label();
    final Label gcDetailsTitleLabel = new Label();
    final Label gcEventsLabel = new Label();
    final Label gcReferenceStatsLabel = new Label();
    final Label gcHeapSummaryLabel = new Label();
    final Label compilationsTitleLabel = new Label();
    final Label compilationEventsLabel = new Label();
    final Label compilationFailuresLabel = new Label();
    final Label codeCacheTitleLabel = new Label();
    final Label codeCacheSweepsLabel = new Label();
    final Label codeCacheStatsLabel = new Label();
    final Label classLoadingTitleLabel = new Label();
    final Label classLoadingHistogramLabel = new Label();
    final Label classLoadingEventsLabel = new Label();
    final Label classLoadingStatsLabel = new Label();
    final Label vmOperationsTitleLabel = new Label();
    final Label vmOperationSummaryLabel = new Label();
    final Label vmOperationEventsLabel = new Label();
    final Label processesTitleLabel = new Label();
    final Label envVarsTitleLabel = new Label();
    final Label sysPropsTitleLabel = new Label();
    final Label recordingInfoTitleLabel = new Label();
    final Label agentsTitleLabel = new Label();
    final Label constantPoolsTitleLabel = new Label();
    final Label settingsTitleLabel = new Label();
    final Label settingsLanguageLabel = new Label();
    final ToggleGroup languageToggleGroup = new ToggleGroup();
    final RadioButton languageFollowSystemRadio = new RadioButton();
    final RadioButton languageEnglishRadio = new RadioButton();
    final RadioButton languageChineseRadio = new RadioButton();
    final Label settingsThemeLabel = new Label();
    final ToggleGroup themeToggleGroup = new ToggleGroup();
    final RadioButton themeFollowSystemRadio = new RadioButton();
    final RadioButton themeLightRadio = new RadioButton();
    final RadioButton themeDarkRadio = new RadioButton();

    AppShellView() {
        configureShell();
        configureHome();
        configureOverview();
        configureEvents();
        configureAnalysis();
        configureMetadata();
        configureAdvancedJfr();
        configureHeapDump();
        configureProfiling();
        configureOverviewPages();
        configureJfrDataPages();
        configureGcPages();
        configureEnvironmentPages();
        configureSettings();
    }

    private void configureShell() {
        styles(root, "enterprise-shell", "app-shell");
        root.setLeft(sidebar);
        VBox workspaceShell = new VBox();
        styles(workspaceShell, "workspace-shell");
        styles(recordingTabs, "recording-tabs");
        styles(workspaceStack, "work-area");
        VBox.setVgrow(workspaceStack, Priority.ALWAYS);
        workspaceShell.getChildren().setAll(recordingTabs, workspaceStack);
        root.setCenter(workspaceShell);

        HBox statusBar = new HBox(8, progressBar);
        styles(statusBar, "status-bar");
        progressBar.setPrefWidth(160);
        root.setBottom(statusBar);

        workspaceStack.getChildren().setAll(
                homePane, overviewPane, eventsPane, analysisPane, metadataPane, advancedJfrPane,
                heapDumpAnalysisPane, jvmsPaneHost, javaApplicationPane, jvmInternalsPane, environmentPane,
                profilingPane, exceptionsPane, threadsPane, fileioPane, socketioPane, locksPane,
                threadHistogramPane, securityPane, nativeLibrariesPane, threadDumpsPane, heapPane, leaksPane,
                tlabPane, jvmInfoPane, gcConfigPane, gcSummaryPane, gcDetailsPane, g1GcPane,
                javaFxEventsPane, compilationsPane, codeCachePane, classLoadingPane, vmOperationsPane,
                processesPane, envVarsPane, sysPropsPane, recordingInfoPane, agentsPane, constantPoolsPane,
                settingsPane);
    }

    private void configureHome() {
        homePane.setSpacing(18);
        styles(homePane, "welcome-pane");
        styles(homeKickerLabel, "home-kicker");
        styles(homeTitleLabel, "welcome-title");
        styles(homeSubtitleLabel, "welcome-subtitle");
        wrap(homeSubtitleLabel, homeOpenWorkflowDescriptionLabel, homeHeapDumpWorkflowDescriptionLabel,
                homeJvmWorkflowDescriptionLabel, homeDisclaimerLabel);
        styles(homeOpenWorkflowTitleLabel, "workflow-tile-title");
        styles(homeHeapDumpWorkflowTitleLabel, "workflow-tile-title");
        styles(homeJvmWorkflowTitleLabel, "workflow-tile-title");
        styles(homeOpenWorkflowDescriptionLabel, "workflow-tile-copy");
        styles(homeHeapDumpWorkflowDescriptionLabel, "workflow-tile-copy");
        styles(homeJvmWorkflowDescriptionLabel, "workflow-tile-copy");
        styles(homeDisclaimerLabel, "legal-disclaimer");
        styles(homeJfrTile, "workflow-tile");
        styles(homeHeapDumpTile, "workflow-tile");
        styles(homeJvmTile, "workflow-tile");
        homeJfrTile.setSpacing(6);
        homeHeapDumpTile.setSpacing(6);
        homeJvmTile.setSpacing(6);
        HBox actions = hbox(8, homeOpenRecordingButton, homeOpenHeapDumpButton, homeConnectJvmButton);
        styles(actions, "home-actions");
        VBox hero = vbox(10, homeKickerLabel, homeTitleLabel, homeSubtitleLabel, actions);
        styles(hero, "home-hero");
        homeJfrTile.getChildren().setAll(homeOpenWorkflowTitleLabel, homeOpenWorkflowDescriptionLabel);
        homeHeapDumpTile.getChildren().setAll(homeHeapDumpWorkflowTitleLabel, homeHeapDumpWorkflowDescriptionLabel);
        homeJvmTile.getChildren().setAll(homeJvmWorkflowTitleLabel, homeJvmWorkflowDescriptionLabel);
        HBox tiles = hbox(12, homeJfrTile, homeHeapDumpTile, homeJvmTile);
        styles(tiles, "workflow-tiles");
        HBox.setHgrow(homeJfrTile, Priority.ALWAYS);
        HBox.setHgrow(homeHeapDumpTile, Priority.ALWAYS);
        HBox.setHgrow(homeJvmTile, Priority.ALWAYS);
        homePane.getChildren().setAll(hero, tiles, homeDisclaimerLabel);
    }

    private void configureOverview() {
        overviewPane.setSpacing(12);
        styles(overviewTitleLabel, "view-title");
        styles(overviewRecordingNameLabel, "detail-title");
        styles(overviewAnalysisTitleLabel, "detail-title");
        styles(overviewJvmsTitleLabel, "detail-title");
        styles(overviewAnalysisStatusLabel, "unavailable-state");
        styles(overviewJvmStatusLabel, "unavailable-state");
        wrap(overviewRecordingDetailsLabel, overviewAnalysisStatusLabel, overviewJvmStatusLabel);
        VBox recording = vbox(6, overviewRecordingNameLabel, overviewRecordingDetailsLabel);
        styles(recording, "summary-panel");
        VBox analysis = vbox(6, overviewAnalysisTitleLabel, overviewAnalysisStatusLabel);
        styles(analysis, "summary-panel");
        VBox jvms = vbox(6, overviewJvmsTitleLabel, overviewJvmStatusLabel);
        styles(jvms, "summary-panel");
        HBox row = hbox(12, analysis, jvms);
        HBox.setHgrow(analysis, Priority.ALWAYS);
        HBox.setHgrow(jvms, Priority.ALWAYS);
        overviewPane.getChildren().setAll(overviewTitleLabel, recording, row);
    }

    private void configureEvents() {
        eventsPane.setSpacing(8);
        styles(eventsTitleLabel, "view-title");
        HBox filters = hbox(8, eventSearchField, threadFilterField, fieldFilterField, clearEventFiltersButton, columnsButton);
        styles(filters, "event-filter-bar");
        HBox.setHgrow(eventSearchField, Priority.ALWAYS);
        styles(eventsTable, "dense-table");
        eventsSplitPane.getItems().setAll(eventTypesTree, vbox(6, eventsTable, eventWindowStatusLabel));
        VBox.setVgrow(eventsTable, Priority.ALWAYS);
        tab(eventPropertiesTab, eventPropertiesTable);
        tab(eventTimingTab, vbox(4, eventTimingLabel));
        tab(eventThreadTab, vbox(4, eventThreadLabel));
        tab(eventStackTraceTab, eventStackTraceList);
        eventDetailsTabs.getTabs().setAll(eventPropertiesTab, eventTimingTab, eventThreadTab, eventStackTraceTab);
        eventDetailsTabs.setPrefHeight(220);
        wrap(eventTimingLabel, eventThreadLabel);
        styles(eventWindowStatusLabel, "event-window-status");
        VBox.setVgrow(eventsSplitPane, Priority.ALWAYS);
        eventsPane.getChildren().setAll(eventsTitleLabel, filters, eventsSplitPane, eventDetailsTabs);
    }

    private void configureAnalysis() {
        analysisPane.setSpacing(8);
        styles(analysisPane, "split-table-detail-page");
        styles(analysisTitleLabel, "view-title");
        HBox filterBar = hbox(8, analysisSearchField, analysisMinimumScoreLabel, analysisMinimumScoreSpinner,
                analysisShowOkCheckBox, analysisShowIgnoredCheckBox, analysisShowUnavailableCheckBox);
        styles(filterBar, "page-toolbar", "analysis-filter-bar");
        styles(analysisDetailExplanationCaption, "detail-section-label");
        styles(analysisDetailEvidenceCaption, "detail-section-label");
        styles(analysisDetailRecommendationCaption, "detail-section-label");
        readonly(analysisDetailExplanationArea, analysisDetailEvidenceArea, analysisDetailRecommendationArea);
        styles(analysisDetailExplanationArea, "detail-panel-body");
        styles(analysisDetailEvidenceArea, "detail-panel-body");
        styles(analysisDetailRecommendationArea, "detail-panel-body");
        VBox details = vbox(6, analysisDetailExplanationCaption, analysisDetailExplanationArea,
                analysisDetailEvidenceCaption, analysisDetailEvidenceArea,
                analysisDetailRecommendationCaption, analysisDetailRecommendationArea);
        styles(details, "detail-panel");
        SplitPane split = new SplitPane(analysisTable, details);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.6);
        VBox.setVgrow(split, Priority.ALWAYS);
        analysisPane.getChildren().setAll(analysisTitleLabel, filterBar, split);
    }

    private void configureMetadata() {
        styles(metadataPane, "page", "split-table-detail-page");
        styles(metadataTitleLabel, "view-title");
        styles(metadataSummaryLabel, "event-window-status");
        wrap(metadataSummaryLabel);
        styles(metadataDetailTitleLabel, "detail-panel-title");
        styles(metadataDetailArea, "detail-panel-body");
        readonly(metadataDetailArea);
        VBox detail = vbox(0, metadataDetailTitleLabel, metadataDetailArea);
        styles(detail, "detail-panel");
        SplitPane split = verticalSplit(0.62, metadataEventTypesTable, detail);
        styles(split, "page-content");
        VBox header = vbox(0, metadataTitleLabel, metadataSummaryLabel);
        styles(header, "page-header");
        metadataPane.getChildren().setAll(header, split);
        VBox.setVgrow(split, Priority.ALWAYS);
    }

    private void configureAdvancedJfr() {
        advancedJfrPane.setSpacing(8);
        styles(advancedJfrTitleLabel, "view-title");
        styles(advancedJfrSummaryLabel, "event-window-status");
        wrap(advancedJfrSummaryLabel, advancedJfrSelectedEventTypeLabel, advancedJfrMemorySummaryLabel);
        styles(advancedJfrHeatmapContainer, "advanced-jfr-heatmap-content");
        VBox selection = vbox(6, advancedJfrSelectionTitleLabel,
                hbox(16, vbox(2, advancedJfrSelectedEventTypeCaptionLabel, advancedJfrSelectedEventTypeLabel),
                        vbox(2, advancedJfrSelectedCountCaptionLabel, advancedJfrSelectedCountLabel)));
        styles(selection, "advanced-jfr-selection-pane");
        ScrollPane heatmapScroll = new ScrollPane(advancedJfrHeatmapContainer);
        heatmapScroll.setFitToWidth(true);
        heatmapScroll.setFitToHeight(true);
        styles(heatmapScroll, "advanced-jfr-heatmap-scroll");
        tab(advancedJfrHeatmapTab, vbox(8, heatmapScroll, selection));
        styles(advancedJfrMemoryDetailTitleLabel, "detail-panel-title");
        styles(advancedJfrMemoryDetailArea, "detail-panel-body");
        readonly(advancedJfrMemoryDetailArea);
        VBox memoryDetail = vbox(0, advancedJfrMemoryDetailTitleLabel, advancedJfrMemoryDetailArea);
        styles(memoryDetail, "detail-panel");
        SplitPane memorySplit = verticalSplit(0.62, advancedJfrMemoryTable, memoryDetail);
        tab(advancedJfrMemoryTab, vbox(8, advancedJfrMemorySummaryLabel, memorySplit));
        advancedJfrTabs.getTabs().setAll(advancedJfrHeatmapTab, advancedJfrMemoryTab);
        VBox.setVgrow(advancedJfrTabs, Priority.ALWAYS);
        advancedJfrPane.getChildren().setAll(advancedJfrTitleLabel, advancedJfrSummaryLabel, advancedJfrTabs);
    }

    private void configureHeapDump() {
        styles(heapDumpAnalysisPane, "page", "split-table-detail-page", "heap-dump-page");
        styles(heapDumpAnalysisTitleLabel, "view-title");
        VBox header = vbox(0, heapDumpAnalysisTitleLabel);
        styles(header, "page-header");
        styles(heapDumpIssueDetailTitleLabel, "detail-panel-title");
        styles(heapDumpIssueDetailArea, "detail-panel-body");
        styles(heapDumpTextReportArea, "dump-text-area", "detail-panel-body");
        readonly(heapDumpIssueDetailArea, heapDumpTextReportArea);
        VBox issueDetail = vbox(0, heapDumpIssueDetailTitleLabel, heapDumpIssueDetailArea);
        styles(issueDetail, "detail-panel");
        VBox textReport = vbox(0, heapDumpTextReportArea);
        styles(textReport, "detail-panel");
        tab(heapDumpIssueDetailTab, issueDetail);
        tab(heapDumpTextReportTab, textReport);
        styles(heapDumpDetailsTabs, "page-detail-tabs");
        heapDumpDetailsTabs.getTabs().setAll(heapDumpIssueDetailTab, heapDumpTextReportTab);
        SplitPane content = verticalSplit(0.62, heapDumpIssuesTable, heapDumpDetailsTabs);
        styles(content, "page-content");
        heapDumpAnalysisPane.getChildren().setAll(header, content);
        VBox.setVgrow(content, Priority.ALWAYS);
    }

    private void configureProfiling() {
        profilingPane.setSpacing(8);
        styles(profilingTitleLabel, "view-title");
        configureProfilingTab(profilingCallGraphTab, profilingCallGraphToolbar,
                new Node[] { profilingCallGraphDirectionCombo, profilingCallGraphDepthLabel,
                        profilingCallGraphDepthSpinner, profilingCallGraphZoomOutButton,
                        profilingCallGraphResetZoomButton, profilingCallGraphZoomInButton, profilingCallGraphFitButton },
                profilingCallGraphScrollPane, profilingCallGraphContainer, false);
        configureProfilingTab(profilingDependencyGraphTab, profilingDependencyToolbar,
                new Node[] { profilingDependencyDepthLabel, profilingDependencyDepthSpinner,
                        profilingDependencyZoomOutButton, profilingDependencyResetZoomButton,
                        profilingDependencyZoomInButton, profilingDependencyFitButton },
                profilingDependencyGraphScrollPane, profilingDependencyGraphContainer, true);
        configureFlameTab(profilingCallersFlameTab, profilingCallersFlameToolbar, profilingCallersFlameContainer,
                profilingCallersFlameOrientationButton, profilingCallersFlameZoomOutButton,
                profilingCallersFlameResetZoomButton, profilingCallersFlameZoomInButton, profilingCallersFlameFitButton);
        configureFlameTab(profilingCalleesFlameTab, profilingCalleesFlameToolbar, profilingCalleesFlameContainer,
                profilingCalleesFlameOrientationButton, profilingCalleesFlameZoomOutButton,
                profilingCalleesFlameResetZoomButton, profilingCalleesFlameZoomInButton, profilingCalleesFlameFitButton);
        tab(profilingCallersTab, profilingCallersTree);
        tab(profilingCalleesTab, profilingCalleesTree);
        profilingTreeTabs.getTabs().setAll(profilingCallGraphTab, profilingCallersFlameTab,
                profilingCalleesFlameTab, profilingDependencyGraphTab, profilingCallersTab, profilingCalleesTab);
        SplitPane split = new SplitPane(profilingTable, profilingTreeTabs);
        split.setDividerPositions(0.5);
        VBox.setVgrow(split, Priority.ALWAYS);
        profilingPane.getChildren().setAll(profilingTitleLabel, split);
    }

    private void configureOverviewPages() {
        configureActionOverview(javaApplicationPane, javaApplicationTitleLabel, javaApplicationSummaryLabel,
                new Node[] { summaryAction(javaApplicationProfilingTitleLabel, javaApplicationProfilingSummaryLabel, javaApplicationProfilingButton),
                        summaryAction(javaApplicationIoTitleLabel, javaApplicationIoSummaryLabel, javaApplicationIoButton),
                        summaryAction(javaApplicationLocksTitleLabel, javaApplicationLocksSummaryLabel, javaApplicationLocksButton),
                        summaryAction(javaApplicationThreadsTitleLabel, javaApplicationThreadsSummaryLabel, javaApplicationThreadsButton),
                        summaryAction(javaApplicationExceptionsTitleLabel, javaApplicationExceptionsSummaryLabel, javaApplicationExceptionsButton),
                        summaryAction(javaApplicationClassLoadingTitleLabel, javaApplicationClassLoadingSummaryLabel, javaApplicationClassLoadingButton),
                        summaryAction(javaApplicationAllocationTitleLabel, javaApplicationAllocationSummaryLabel, javaApplicationAllocationButton) },
                "java-application-overview-page");
        configureActionOverview(jvmInternalsPane, jvmInternalsTitleLabel, jvmInternalsSummaryLabel,
                new Node[] { summaryAction(jvmInternalsInformationTitleLabel, jvmInternalsInformationSummaryLabel, jvmInternalsInformationButton),
                        summaryAction(jvmInternalsGcTitleLabel, jvmInternalsGcSummaryLabel, jvmInternalsGcButton),
                        summaryAction(jvmInternalsG1TitleLabel, jvmInternalsG1SummaryLabel, jvmInternalsG1Button),
                        summaryAction(jvmInternalsCompilationTitleLabel, jvmInternalsCompilationSummaryLabel, jvmInternalsCompilationButton),
                        summaryAction(jvmInternalsCodeCacheTitleLabel, jvmInternalsCodeCacheSummaryLabel, jvmInternalsCodeCacheButton),
                        summaryAction(jvmInternalsClassLoadingTitleLabel, jvmInternalsClassLoadingSummaryLabel, jvmInternalsClassLoadingButton),
                        summaryAction(jvmInternalsVmOperationsTitleLabel, jvmInternalsVmOperationsSummaryLabel, jvmInternalsVmOperationsButton) },
                "jvm-internals-overview-page");
        configureActionOverview(environmentPane, environmentTitleLabel, environmentSummaryLabel,
                new Node[] { summaryAction(environmentProcessesTitleLabel, environmentProcessesSummaryLabel, environmentProcessesButton),
                        summaryAction(environmentVariablesTitleLabel, environmentVariablesSummaryLabel, environmentVariablesButton),
                        summaryAction(environmentPropertiesTitleLabel, environmentPropertiesSummaryLabel, environmentPropertiesButton),
                        summaryAction(environmentRecordingTitleLabel, environmentRecordingSummaryLabel, environmentRecordingButton),
                        summaryAction(environmentAgentsTitleLabel, environmentAgentsSummaryLabel, environmentAgentsButton),
                        summaryAction(environmentConstantPoolsTitleLabel, environmentConstantPoolsSummaryLabel, environmentConstantPoolsButton) },
                "environment-overview-page");
    }

    private void configureJfrDataPages() {
        configureTablePage(exceptionsPane, exceptionsTitleLabel,
                hbox(8, exceptionsGroupByClass, exceptionsGroupByMessage, exceptionsGroupByClassAndMessage),
                new SplitPane(exceptionsTable, exceptionsTimelineContainer));
        configureTablePage(threadsPane, threadsTitleLabel, threadsTable);
        tab(fileioTimelineTab, fileioTimelineContainer);
        tab(fileioDurationTab, fileioHistogramTable);
        tab(fileioEventLogTab, fileioEventTable);
        fileioTabPane.getTabs().setAll(fileioTimelineTab, fileioDurationTab, fileioEventLogTab);
        configureTablePage(fileioPane, fileioTitleLabel, fileioTabPane);
        socketioGroupingBar.setSpacing(8);
        socketioGroupingBar.getChildren().setAll(socketioGroupByHostAndPort, socketioGroupByHost, socketioGroupByPort);
        styles(socketioGroupingBar, "socketio-grouping-bar");
        tab(socketioTimelineTab, socketioTimelineContainer);
        tab(socketioDurationTab, socketioHistogramTable);
        tab(socketioEventLogTab, socketioEventTable);
        socketioTabPane.getTabs().setAll(socketioTimelineTab, socketioDurationTab, socketioEventLogTab);
        configureTablePage(socketioPane, socketioTitleLabel, socketioGroupingBar, socketioTabPane);
        locksGroupingBar.setSpacing(8);
        locksGroupingBar.getChildren().setAll(locksGroupByClass, locksGroupByAddress, locksGroupByThread);
        styles(locksGroupingBar, "locks-grouping-bar");
        tab(locksByClassTab, locksByClassTable);
        tab(locksByAddressTab, locksByAddressTable);
        tab(locksByThreadTab, locksByThreadTable);
        locksTabPane.getTabs().setAll(locksByClassTab, locksByAddressTab, locksByThreadTab);
        configureTablePage(locksPane, locksTitleLabel, locksGroupingBar, locksTabPane);
        configureTablePage(threadHistogramPane, threadHistogramTitleLabel, threadHistogramChartContainer, threadHistogramTable);
        configureTablePage(securityPane, securityTitleLabel, securityTable);
        configureTablePage(nativeLibrariesPane, nativeLibrariesTitleLabel, nativeLibrariesTable);
        configureTablePage(threadDumpsPane, threadDumpsTitleLabel, new SplitPane(threadDumpsTable, threadDumpTextArea));
        readonly(threadDumpTextArea);
        styles(threadDumpTextArea, "dump-text-area");
        configureTablePage(heapPane, heapTitleLabel, new SplitPane(heapTable, heapTimelineContainer));
        configureTablePage(leaksPane, leaksTitleLabel, new SplitPane(leaksTable, leaksReferenceTree));
        configureTablePage(tlabPane, tlabTitleLabel, new SplitPane(tlabTable, tlabTimelineContainer));
    }

    private void configureGcPages() {
        configureTablePage(jvmInfoPane, jvmInfoTitleLabel, jvmFlagsLabel, jvmFlagsTable, jvmFlagChangesLabel, jvmFlagChangesTable);
        configureTablePage(gcConfigPane, gcConfigTitleLabel, gcConfigDescriptionLabel);
        wrap(gcConfigDescriptionLabel);
        configureTablePage(gcSummaryPane, gcSummaryTitleLabel, gcSummaryTable);
        configureTablePage(gcDetailsPane, gcDetailsTitleLabel, gcHeapChartContainer, gcMetaspaceChartContainer,
                gcPauseChartContainer, gcEventsLabel, gcEventsTable, gcReferenceStatsLabel, gcReferenceStatsTable,
                gcHeapSummaryLabel, gcHeapSummaryTable);
        configureDetailPage(g1GcPane, g1GcTitleLabel, g1GcSummaryLabel,
                vbox(8, g1GcRegionStatesLabel, g1GcRegionStatesTable),
                g1GcDetailTitleLabel, g1GcDetailArea,
                vbox(8, g1GcRegionSummaryLabel, g1GcRegionSummaryTable, g1GcPausesLabel, g1GcPauseTable));
        configureDetailPage(javaFxEventsPane, javaFxEventsTitleLabel, javaFxEventsSummaryLabel,
                vbox(8, javaFxEventsPhaseLabel, javaFxEventsPhaseTable),
                javaFxEventsDetailTitleLabel, javaFxEventsDetailArea,
                vbox(8, javaFxEventsPulseLabel, javaFxEventsPulseTable, javaFxEventsInputLabel, javaFxEventsInputTable));
        configureTablePage(compilationsPane, compilationsTitleLabel, compilationDurationChartContainer,
                compilationEventsLabel, compilationsTable, compilationFailuresLabel, compilationFailuresTable);
        configureTablePage(codeCachePane, codeCacheTitleLabel, codeCacheEntriesChartContainer, codeCacheSweepChartContainer,
                codeCacheSweepsLabel, codeCacheSweepsTable, codeCacheStatsLabel, codeCacheStatsTable);
        configureTablePage(classLoadingPane, classLoadingTitleLabel, classLoadingChartContainer,
                classLoadingHistogramLabel, classLoadingHistogramTable, classLoadingEventsLabel, classLoadingEventsTable,
                classLoadingStatsLabel, classLoadingStatsTable);
        configureTablePage(vmOperationsPane, vmOperationsTitleLabel, vmOperationSummaryLabel, vmOperationSummaryTable,
                vmOperationEventsLabel, vmOperationEventsTable);
    }

    private void configureEnvironmentPages() {
        configureTablePage(processesPane, processesTitleLabel, processesTable);
        configureTablePage(envVarsPane, envVarsTitleLabel, hbox(8, envVarsSearchField), envVarsTable);
        configureTablePage(sysPropsPane, sysPropsTitleLabel, hbox(8, sysPropsSearchField), sysPropsTable);
        tab(recordingInfoRecordingsTab, recordingsTable);
        tab(recordingInfoSettingsTab, settingsTable);
        recordingInfoTabs.getTabs().setAll(recordingInfoRecordingsTab, recordingInfoSettingsTab);
        configureTablePage(recordingInfoPane, recordingInfoTitleLabel, recordingInfoTabs);
        configureTablePage(agentsPane, agentsTitleLabel, agentsTable);
        configureTablePage(constantPoolsPane, constantPoolsTitleLabel, constantPoolsTable);
    }

    private void configureSettings() {
        settingsPane.setSpacing(36);
        styles(settingsTitleLabel, "view-title");
        styles(settingsLanguageLabel, "detail-title");
        styles(settingsThemeLabel, "detail-title");
        languageFollowSystemRadio.setToggleGroup(languageToggleGroup);
        languageEnglishRadio.setToggleGroup(languageToggleGroup);
        languageChineseRadio.setToggleGroup(languageToggleGroup);
        themeFollowSystemRadio.setToggleGroup(themeToggleGroup);
        themeLightRadio.setToggleGroup(themeToggleGroup);
        themeDarkRadio.setToggleGroup(themeToggleGroup);
        VBox languageGroup = vbox(16, settingsLanguageLabel,
                hbox(24, languageFollowSystemRadio, languageEnglishRadio, languageChineseRadio));
        VBox themeGroup = vbox(16, settingsThemeLabel,
                hbox(24, themeFollowSystemRadio, themeLightRadio, themeDarkRadio));
        settingsPane.getChildren().setAll(settingsTitleLabel, languageGroup, themeGroup);
    }

    private void configureProfilingTab(Tab tab, HBox toolbar, Node[] controls, ScrollPane scrollPane,
            VBox container, boolean includeTable) {
        toolbar.setSpacing(8);
        styles(toolbar, "page-toolbar", "profiling-graph-toolbar");
        toolbar.getChildren().setAll(controls);
        styles(container, "profiling-call-graph-container");
        scrollPane.setContent(container);
        scrollPane.setPannable(true);
        if (includeTable) {
            SplitPane split = new SplitPane(profilingDependencyTable, scrollPane);
            split.setDividerPositions(0.35);
            tab(tab, vbox(8, toolbar, split));
        } else {
            tab(tab, vbox(8, toolbar, scrollPane));
        }
    }

    private void configureFlameTab(Tab tab, HBox toolbar, VBox container, Button... buttons) {
        toolbar.setSpacing(8);
        styles(toolbar, "page-toolbar", "profiling-graph-toolbar");
        toolbar.getChildren().setAll(buttons);
        styles(container, "profiling-flame-container");
        tab(tab, vbox(8, toolbar, container));
    }

    private void configureActionOverview(VBox pane, Label title, Label summary, Node[] actions, String pageClass) {
        styles(pane, "page", "overview-page", pageClass);
        styles(title, "view-title");
        wrap(summary);
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        styles(grid, "metric-grid");
        for (int index = 0; index < actions.length; index++) {
            grid.add(actions[index], index % 2, index / 2);
        }
        VBox header = vbox(0, title, summary);
        styles(header, "page-header");
        pane.getChildren().setAll(header, grid);
    }

    private VBox summaryAction(Label title, Label summary, Button button) {
        styles(title, "detail-title");
        wrap(summary);
        VBox panel = vbox(6, title, summary, button);
        styles(panel, "summary-panel");
        return panel;
    }

    private void configureTablePage(VBox pane, Label title, Node... content) {
        pane.setSpacing(8);
        styles(title, "view-title");
        pane.getChildren().setAll(title);
        pane.getChildren().addAll(content);
        for (Node node : content) {
            if (node instanceof TableView<?> || node instanceof TabPane || node instanceof SplitPane) {
                VBox.setVgrow(node, Priority.ALWAYS);
            }
        }
    }

    private void configureDetailPage(VBox pane, Label title, Label summary, Node primary,
            Label detailTitle, TextArea detailArea, Node secondary) {
        styles(pane, "page", "split-table-detail-page");
        styles(title, "view-title");
        styles(summary, "event-window-status");
        wrap(summary);
        styles(detailTitle, "detail-panel-title");
        styles(detailArea, "detail-panel-body");
        readonly(detailArea);
        VBox detail = vbox(0, detailTitle, detailArea);
        styles(detail, "detail-panel");
        SplitPane split = verticalSplit(0.60, primary, detail, secondary);
        styles(split, "page-content");
        VBox header = vbox(0, title, summary);
        styles(header, "page-header");
        pane.getChildren().setAll(header, split);
        VBox.setVgrow(split, Priority.ALWAYS);
    }

    private static SplitPane verticalSplit(double dividerPosition, Node... nodes) {
        SplitPane split = new SplitPane(nodes);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(dividerPosition);
        return split;
    }

    private static VBox vbox(double spacing, Node... children) {
        VBox box = new VBox(spacing, children);
        return box;
    }

    private static HBox hbox(double spacing, Node... children) {
        HBox box = new HBox(spacing, children);
        return box;
    }

    private static void tab(Tab tab, Node content) {
        tab.setClosable(false);
        tab.setContent(content);
    }

    private static Tab tab() {
        Tab tab = new Tab();
        tab.setClosable(false);
        return tab;
    }

    private static TextArea textArea() {
        TextArea area = new TextArea();
        area.setWrapText(true);
        return area;
    }

    private static <T> TableView<T> denseTable() {
        TableView<T> table = new TableView<>();
        styles(table, "dense-table");
        return table;
    }

    private static void readonly(TextArea... areas) {
        for (TextArea area : areas) {
            area.setEditable(false);
        }
    }

    private static void wrap(Label... labels) {
        for (Label label : labels) {
            label.setWrapText(true);
        }
    }

    private static void styles(Node node, String... styleClasses) {
        node.getStyleClass().addAll(styleClasses);
    }
}
