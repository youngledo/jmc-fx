package com.youngledo.jmcfx.ui.shell;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;

import com.youngledo.jmcfx.domain.model.ActiveRecordingInfo;
import com.youngledo.jmcfx.domain.model.ActiveSetting;
import com.youngledo.jmcfx.domain.model.AgentInfo;
import com.youngledo.jmcfx.domain.model.ClassloaderStatistics;
import com.youngledo.jmcfx.domain.model.ClassloaderSummary;
import com.youngledo.jmcfx.domain.model.ClassloadEvent;
import com.youngledo.jmcfx.domain.model.CodeCacheStats;
import com.youngledo.jmcfx.domain.model.CodeCacheSweep;
import com.youngledo.jmcfx.domain.model.CompilationEvent;
import com.youngledo.jmcfx.domain.model.ConstantPoolEntry;
import com.youngledo.jmcfx.domain.model.ConstantPoolType;
import com.youngledo.jmcfx.domain.model.EnvironmentVariable;
import com.youngledo.jmcfx.domain.model.EventColumn;
import com.youngledo.jmcfx.domain.model.EventDetails;
import com.youngledo.jmcfx.domain.model.EventFieldCondition;
import com.youngledo.jmcfx.domain.model.EventFieldDescriptor;
import com.youngledo.jmcfx.domain.model.EventFilter;
import com.youngledo.jmcfx.domain.model.EventFilterOperator;
import com.youngledo.jmcfx.domain.model.EventProperty;
import com.youngledo.jmcfx.domain.model.EventRow;
import com.youngledo.jmcfx.domain.model.EventSelectionProperties;
import com.youngledo.jmcfx.domain.model.EventStackFrame;
import com.youngledo.jmcfx.domain.model.EventThreadInfo;
import com.youngledo.jmcfx.domain.model.EventTiming;
import com.youngledo.jmcfx.domain.model.EventTypeNode;
import com.youngledo.jmcfx.domain.model.EventTypeNodeKind;
import com.youngledo.jmcfx.domain.model.EventTypeSelection;
import com.youngledo.jmcfx.domain.model.ExceptionGrouping;
import com.youngledo.jmcfx.domain.model.ExceptionSummary;
import com.youngledo.jmcfx.domain.model.FileIOEvent;
import com.youngledo.jmcfx.domain.model.FileIOHistogram;
import com.youngledo.jmcfx.domain.model.GcEvent;
import com.youngledo.jmcfx.domain.model.GcHeapSummary;
import com.youngledo.jmcfx.domain.model.GcReferenceStat;
import com.youngledo.jmcfx.domain.model.GcSummary;
import com.youngledo.jmcfx.domain.model.HeapClassHistogram;
import com.youngledo.jmcfx.domain.model.ProcessInfo;
import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.JvmFlag;
import com.youngledo.jmcfx.domain.model.JvmFlagChange;
import com.youngledo.jmcfx.domain.model.LeakCandidate;
import com.youngledo.jmcfx.domain.model.LeakReferenceNode;
import com.youngledo.jmcfx.domain.model.LockGrouping;
import com.youngledo.jmcfx.domain.model.LockHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.RuleResult;
import com.youngledo.jmcfx.domain.model.Severity;
import com.youngledo.jmcfx.domain.model.SocketIOEvent;
import com.youngledo.jmcfx.domain.model.SocketIOGrouping;
import com.youngledo.jmcfx.domain.model.SocketIOHistogram;
import com.youngledo.jmcfx.domain.model.StackTreeNode;
import com.youngledo.jmcfx.domain.model.SystemProperty;
import com.youngledo.jmcfx.domain.model.ThreadSummary;
import com.youngledo.jmcfx.domain.model.TlabAllocation;
import com.youngledo.jmcfx.domain.model.VmOperationEvent;
import com.youngledo.jmcfx.domain.model.VmOperationSummary;
import com.youngledo.jmcfx.domain.service.EnvironmentService;
import com.youngledo.jmcfx.domain.service.EventQueryService;
import com.youngledo.jmcfx.domain.service.ExceptionService;
import com.youngledo.jmcfx.domain.service.FileIOService;
import com.youngledo.jmcfx.domain.service.HeapService;
import com.youngledo.jmcfx.domain.service.JvmInternalsService;
import com.youngledo.jmcfx.domain.service.LeakSuspectsService;
import com.youngledo.jmcfx.domain.service.LockService;
import com.youngledo.jmcfx.domain.service.ProfilingService;
import com.youngledo.jmcfx.domain.service.RecordingRepository;
import com.youngledo.jmcfx.domain.service.RuleAnalysisService;
import com.youngledo.jmcfx.domain.service.SocketIOService;
import com.youngledo.jmcfx.domain.service.ThreadService;
import com.youngledo.jmcfx.domain.service.TlabService;
import com.youngledo.jmcfx.ui.analysis.AnalysisSeverityCell;
import com.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import com.youngledo.jmcfx.ui.events.VirtualThreadEventBrowserExecutor;
import com.youngledo.jmcfx.ui.environment.EnvironmentViewModel;
import com.youngledo.jmcfx.ui.exceptions.ExceptionViewModel;
import com.youngledo.jmcfx.ui.fileio.FileIOViewModel;
import com.youngledo.jmcfx.ui.heap.HeapViewModel;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.i18n.LanguageMode;
import com.youngledo.jmcfx.ui.jvm.ClassLoadingViewModel;
import com.youngledo.jmcfx.ui.jvm.CodeCacheViewModel;
import com.youngledo.jmcfx.ui.jvm.CompilationsViewModel;
import com.youngledo.jmcfx.ui.jvm.GcConfigViewModel;
import com.youngledo.jmcfx.ui.jvm.GcDetailsViewModel;
import com.youngledo.jmcfx.ui.jvm.GcSummaryViewModel;
import com.youngledo.jmcfx.ui.jvm.JvmInfoViewModel;
import com.youngledo.jmcfx.ui.jvm.VmOperationsViewModel;
import com.youngledo.jmcfx.ui.leaks.LeakSuspectsViewModel;
import com.youngledo.jmcfx.ui.locks.LockViewModel;
import com.youngledo.jmcfx.ui.util.DisplayFormats;
import com.youngledo.jmcfx.ui.util.HtmlToTextFlow;
import com.youngledo.jmcfx.ui.overview.OverviewViewModel;
import com.youngledo.jmcfx.ui.profiling.ProfilingViewModel;
import com.youngledo.jmcfx.ui.rules.RuleResultsViewModel;
import com.youngledo.jmcfx.ui.socketio.SocketIOViewModel;
import com.youngledo.jmcfx.ui.threads.ThreadViewModel;
import com.youngledo.jmcfx.ui.tlab.TlabViewModel;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;

/// FXML controller for `app-shell.fxml`.
///
/// The controller wires shell actions and bindings while feature behavior stays
/// in view models.
public class AppShellController {

    static final int MIN_EVENT_TYPES_WIDTH = 180;
    static final int DEFAULT_EVENT_TYPES_WIDTH = 260;
    static final double MAX_EVENT_TYPES_WIDTH = 360;
    static final double DEFAULT_EVENT_TYPES_DIVIDER_POSITION = 0.25;

    private final AppShellViewModel viewModel;
    private final RecordingRepository recordingRepository;
    private final EventQueryService eventQueryService;
    private final RuleAnalysisService ruleAnalysisService;
    private final ProfilingService profilingService;
    private final ExceptionService exceptionService;
    private final ThreadService threadService;
    private final FileIOService fileIOService;
    private final SocketIOService socketIOService;
    private final LockService lockService;
    private final HeapService heapService;
    private final LeakSuspectsService leakSuspectsService;
    private final TlabService tlabService;
    private final JvmInternalsService jvmInternalsService;
    private final EnvironmentService environmentService;
    private final I18n i18n;
    private final ListChangeListener<EventTypeNode> eventTypeTreeListener = change -> rebuildEventTypeTree();
    private final ListChangeListener<EventColumn> eventColumnsListener = change -> rebuildEventColumns();
    private final ListChangeListener<EventFieldDescriptor> fieldDescriptorsListener = change -> rebuildColumnsMenu();
    private final ListChangeListener<EventRow> eventRowsListener = change -> selectFirstEventRow();
    private final ChangeListener<EventDetails> selectedDetailsListener =
            (observable, oldValue, newValue) -> showEventDetails(newValue);
    private final ChangeListener<EventSelectionProperties> selectionPropertiesListener =
            (observable, oldValue, newValue) -> showSelectionProperties(newValue);
    private OverviewViewModel overviewViewModel;
    private EventBrowserViewModel eventBrowserViewModel;
    private ProfilingViewModel profilingViewModel;
    private ExceptionViewModel exceptionViewModel;
    private ThreadViewModel threadViewModel;
    private FileIOViewModel fileIOViewModel;
    private SocketIOViewModel socketIOViewModel;
    private LockViewModel lockViewModel;
    private HeapViewModel heapViewModel;
    private LeakSuspectsViewModel leaksViewModel;
    private TlabViewModel tlabViewModel;
    private EnvironmentViewModel environmentViewModel;
    private boolean eventTypesDividerInitialized;
    private boolean updatingRecordingTabs;

    @FXML private BorderPane root;
    @FXML private Button homeOpenRecordingButton;
    @FXML private Button homeConnectJvmButton;
    @FXML private AppSidebar sidebar;
    @FXML private TabPane recordingTabs;
    @FXML private VBox homePane;
    @FXML private VBox overviewPane;
    @FXML private VBox eventsPane;
    @FXML private VBox analysisPane;
    @FXML private VBox jvmsPane;
    @FXML private VBox profilingPane;
    @FXML private VBox exceptionsPane;
    @FXML private VBox threadsPane;
    @FXML private VBox fileioPane;
    @FXML private VBox socketioPane;
    @FXML private VBox locksPane;
    @FXML private VBox heapPane;
    @FXML private VBox leaksPane;
    @FXML private VBox tlabPane;
    @FXML private VBox jvmInfoPane;
    @FXML private VBox gcConfigPane;
    @FXML private VBox gcSummaryPane;
    @FXML private VBox gcDetailsPane;
    @FXML private VBox compilationsPane;
    @FXML private VBox codeCachePane;
    @FXML private VBox classLoadingPane;
    @FXML private VBox vmOperationsPane;
    @FXML private VBox processesPane;
    @FXML private VBox envVarsPane;
    @FXML private VBox sysPropsPane;
    @FXML private VBox recordingInfoPane;
    @FXML private VBox agentsPane;
    @FXML private VBox constantPoolsPane;
    @FXML private VBox settingsPane;
    @FXML private Label statusLabel;
    @FXML private Label taskSummaryLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label homeKickerLabel;
    @FXML private Label homeTitleLabel;
    @FXML private Label homeSubtitleLabel;
    @FXML private Label homeOpenWorkflowTitleLabel;
    @FXML private Label homeOpenWorkflowDescriptionLabel;
    @FXML private Label homeEventsWorkflowTitleLabel;
    @FXML private Label homeEventsWorkflowDescriptionLabel;
    @FXML private Label homeJvmWorkflowTitleLabel;
    @FXML private Label homeJvmWorkflowDescriptionLabel;
    @FXML private Label homeDisclaimerLabel;
    @FXML private Label overviewTitleLabel;
    @FXML private Label overviewRecordingNameLabel;
    @FXML private Label overviewRecordingDetailsLabel;
    @FXML private Label overviewAnalysisTitleLabel;
    @FXML private Label overviewAnalysisStatusLabel;
    @FXML private Label overviewJvmsTitleLabel;
    @FXML private Label overviewJvmStatusLabel;
    @FXML private Label eventsTitleLabel;
    @FXML private TreeView<EventTypeNode> eventTypesTree;
    @FXML private TextField eventSearchField;
    @FXML private TextField threadFilterField;
    @FXML private TextField fieldFilterField;
    @FXML private Button clearEventFiltersButton;
    @FXML private MenuButton columnsButton;
    @FXML private SplitPane eventsSplitPane;
    @FXML private TableView<EventRow> eventsTable;
    @FXML private Label eventWindowStatusLabel;
    @FXML private TabPane eventDetailsTabs;
    @FXML private Tab eventPropertiesTab;
    @FXML private Tab eventTimingTab;
    @FXML private Tab eventThreadTab;
    @FXML private Tab eventStackTraceTab;
    @FXML private TableView<EventProperty> eventPropertiesTable;
    @FXML private Label eventTimingLabel;
    @FXML private Label eventThreadLabel;
    @FXML private ListView<String> eventStackTraceList;
    @FXML private Label analysisTitleLabel;
    @FXML private TableView<RuleResult> analysisTable;
    @FXML private Label analysisDetailTitle;
    @FXML private TextArea analysisDetailExplanation;
    @FXML private Label jvmsTitleLabel;
    @FXML private Label jvmsUnavailableLabel;
    @FXML private Label profilingTitleLabel;
    @FXML private TableView<HotMethod> profilingTable;
    @FXML private TabPane profilingTreeTabs;
    @FXML private Tab profilingCallersTab;
    @FXML private TreeView<StackTreeNode> profilingCallersTree;
    @FXML private Tab profilingCalleesTab;
    @FXML private TreeView<StackTreeNode> profilingCalleesTree;
    @FXML private Label exceptionsTitleLabel;
    @FXML private Button exceptionsGroupByClass;
    @FXML private Button exceptionsGroupByMessage;
    @FXML private Button exceptionsGroupByClassAndMessage;
    @FXML private TableView<ExceptionSummary> exceptionsTable;
    @FXML private Label exceptionsTimelineLabel;
    @FXML private Label threadsTitleLabel;
    @FXML private TableView<ThreadSummary> threadsTable;
    @FXML private Label fileioTitleLabel;
    @FXML private TabPane fileioTabPane;
    @FXML private Tab fileioTimelineTab;
    @FXML private Label fileioTimelinePlaceholderLabel;
    @FXML private Tab fileioDurationTab;
    @FXML private TableView<FileIOHistogram> fileioHistogramTable;
    @FXML private Tab fileioEventLogTab;
    @FXML private TableView<FileIOEvent> fileioEventTable;
    @FXML private Label socketioTitleLabel;
    @FXML private HBox socketioGroupingBar;
    @FXML private Button socketioGroupByHostAndPort;
    @FXML private Button socketioGroupByHost;
    @FXML private Button socketioGroupByPort;
    @FXML private TabPane socketioTabPane;
    @FXML private Tab socketioTimelineTab;
    @FXML private Label socketioTimelinePlaceholderLabel;
    @FXML private Tab socketioDurationTab;
    @FXML private TableView<SocketIOHistogram> socketioHistogramTable;
    @FXML private Tab socketioEventLogTab;
    @FXML private TableView<SocketIOEvent> socketioEventTable;
    @FXML private Label locksTitleLabel;
    @FXML private HBox locksGroupingBar;
    @FXML private Button locksGroupByClass;
    @FXML private Button locksGroupByAddress;
    @FXML private Button locksGroupByThread;
    @FXML private TabPane locksTabPane;
    @FXML private Tab locksByClassTab;
    @FXML private TableView<LockHistogram> locksByClassTable;
    @FXML private Tab locksByAddressTab;
    @FXML private TableView<LockHistogram> locksByAddressTable;
    @FXML private Tab locksByThreadTab;
    @FXML private TableView<LockHistogram> locksByThreadTable;
    @FXML private Label heapTitleLabel;
    @FXML private TableView<HeapClassHistogram> heapTable;
    @FXML private Label heapTimelineLabel;
    @FXML private Label leaksTitleLabel;
    @FXML private TableView<LeakCandidate> leaksTable;
    @FXML private TreeView<LeakReferenceNode> leaksReferenceTree;
    @FXML private Label tlabTitleLabel;
    @FXML private TableView<TlabAllocation> tlabTable;
    @FXML private Label tlabTimelineLabel;
    @FXML private TableView<JvmFlag> jvmFlagsTable;
    @FXML private TableView<JvmFlagChange> jvmFlagChangesTable;
    @FXML private TableView<GcSummary> gcSummaryTable;
    @FXML private TableView<GcEvent> gcEventsTable;
    @FXML private TableView<GcReferenceStat> gcReferenceStatsTable;
    @FXML private TableView<GcHeapSummary> gcHeapSummaryTable;
    @FXML private TableView<CompilationEvent> compilationsTable;
    @FXML private TableView<CompilationEvent> compilationFailuresTable;
    @FXML private TableView<CodeCacheSweep> codeCacheSweepsTable;
    @FXML private TableView<CodeCacheStats> codeCacheStatsTable;
    @FXML private TableView<ClassloaderSummary> classLoadingHistogramTable;
    @FXML private TableView<ClassloadEvent> classLoadingEventsTable;
    @FXML private TableView<ClassloaderStatistics> classLoadingStatsTable;
    @FXML private TableView<VmOperationSummary> vmOperationSummaryTable;
    @FXML private TableView<VmOperationEvent> vmOperationEventsTable;
    @FXML private TableView<ProcessInfo> processesTable;
    @FXML private TableView<EnvironmentVariable> envVarsTable;
    @FXML private TextField envVarsSearchField;
    @FXML private TableView<SystemProperty> sysPropsTable;
    @FXML private TextField sysPropsSearchField;
    @FXML private TableView<ActiveRecordingInfo> recordingsTable;
    @FXML private TableView<ActiveSetting> settingsTable;
    @FXML private TabPane recordingInfoTabs;
    @FXML private Tab recordingInfoRecordingsTab;
    @FXML private Tab recordingInfoSettingsTab;
    @FXML private TableView<AgentInfo> agentsTable;
    @FXML private TableView<ConstantPoolType> constantPoolsTable;
    @FXML private Label jvmInfoTitleLabel;
    @FXML private Label jvmFlagsLabel;
    @FXML private Label jvmFlagChangesLabel;
    @FXML private Label gcConfigTitleLabel;
    @FXML private Label gcConfigDescriptionLabel;
    @FXML private Label gcSummaryTitleLabel;
    @FXML private Label gcDetailsTitleLabel;
    @FXML private Label gcEventsLabel;
    @FXML private Label gcReferenceStatsLabel;
    @FXML private Label gcHeapSummaryLabel;
    @FXML private Label compilationsTitleLabel;
    @FXML private Label compilationEventsLabel;
    @FXML private Label compilationFailuresLabel;
    @FXML private Label codeCacheTitleLabel;
    @FXML private Label codeCacheSweepsLabel;
    @FXML private Label codeCacheStatsLabel;
    @FXML private Label classLoadingTitleLabel;
    @FXML private Label classLoadingHistogramLabel;
    @FXML private Label classLoadingEventsLabel;
    @FXML private Label classLoadingStatsLabel;
    @FXML private Label vmOperationsTitleLabel;
    @FXML private Label vmOperationSummaryLabel;
    @FXML private Label vmOperationEventsLabel;
    @FXML private Label processesTitleLabel;
    @FXML private Label envVarsTitleLabel;
    @FXML private Label sysPropsTitleLabel;
    @FXML private Label recordingInfoTitleLabel;
    @FXML private Label agentsTitleLabel;
    @FXML private Label constantPoolsTitleLabel;
    @FXML private Label settingsTitleLabel;
    @FXML private Label settingsLanguageLabel;
    @FXML private ToggleGroup languageToggleGroup;
    @FXML private RadioButton languageFollowSystemRadio;
    @FXML private RadioButton languageEnglishRadio;
    @FXML private RadioButton languageChineseRadio;

    public AppShellController(AppShellViewModel viewModel, RecordingRepository recordingRepository,
            EventQueryService eventQueryService, RuleAnalysisService ruleAnalysisService,
            ProfilingService profilingService, ExceptionService exceptionService,
            ThreadService threadService, FileIOService fileIOService,
            SocketIOService socketIOService, LockService lockService,
            HeapService heapService, LeakSuspectsService leakSuspectsService,
            TlabService tlabService,
            JvmInternalsService jvmInternalsService,
            EnvironmentService environmentService,
            I18n i18n) {
        this.viewModel = viewModel;
        this.recordingRepository = recordingRepository;
        this.eventQueryService = eventQueryService;
        this.ruleAnalysisService = ruleAnalysisService;
        this.profilingService = profilingService;
        this.exceptionService = exceptionService;
        this.threadService = threadService;
        this.fileIOService = fileIOService;
        this.socketIOService = socketIOService;
        this.lockService = lockService;
        this.heapService = heapService;
        this.leakSuspectsService = leakSuspectsService;
        this.tlabService = tlabService;
        this.jvmInternalsService = jvmInternalsService;
        this.environmentService = environmentService;
        this.i18n = i18n;
    }

    I18n i18n() {
        return i18n;
    }

    @FXML
    void initialize() {
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());
        taskSummaryLabel.textProperty().bind(viewModel.taskSummaryProperty());
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        sidebar.bind(viewModel);
        sidebar.setNavigationHandler(viewModel::showSection);
        sidebar.setI18n(i18n);
        bindLocalizedText();
        configureActionIcons();
        configureLanguageSelector();
        homeOpenRecordingButton.setOnAction(event -> openRecording());
        configureRecordingTabs();
        homePane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("home"));
        homePane.managedProperty().bind(homePane.visibleProperty());
        overviewPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("overview"));
        overviewPane.managedProperty().bind(overviewPane.visibleProperty());
        eventsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("events"));
        eventsPane.managedProperty().bind(eventsPane.visibleProperty());
        analysisPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("analysis"));
        analysisPane.managedProperty().bind(analysisPane.visibleProperty());
        jvmsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("jvms"));
        jvmsPane.managedProperty().bind(jvmsPane.visibleProperty());
        profilingPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("profiling"));
        profilingPane.managedProperty().bind(profilingPane.visibleProperty());
        exceptionsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("exceptions"));
        exceptionsPane.managedProperty().bind(exceptionsPane.visibleProperty());
        threadsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("threads"));
        threadsPane.managedProperty().bind(threadsPane.visibleProperty());
        fileioPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("fileio"));
        fileioPane.managedProperty().bind(fileioPane.visibleProperty());
        socketioPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("socketio"));
        socketioPane.managedProperty().bind(socketioPane.visibleProperty());
        locksPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("locks"));
        locksPane.managedProperty().bind(locksPane.visibleProperty());
        heapPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("heap"));
        heapPane.managedProperty().bind(heapPane.visibleProperty());
        leaksPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("leaks"));
        leaksPane.managedProperty().bind(leaksPane.visibleProperty());
        tlabPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("tlab"));
        tlabPane.managedProperty().bind(tlabPane.visibleProperty());
        jvmInfoPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("jvmInfo"));
        jvmInfoPane.managedProperty().bind(jvmInfoPane.visibleProperty());
        gcConfigPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("gcConfig"));
        gcConfigPane.managedProperty().bind(gcConfigPane.visibleProperty());
        gcSummaryPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("gcSummary"));
        gcSummaryPane.managedProperty().bind(gcSummaryPane.visibleProperty());
        gcDetailsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("gcDetails"));
        gcDetailsPane.managedProperty().bind(gcDetailsPane.visibleProperty());
        compilationsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("compilations"));
        compilationsPane.managedProperty().bind(compilationsPane.visibleProperty());
        codeCachePane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("codeCache"));
        codeCachePane.managedProperty().bind(codeCachePane.visibleProperty());
        classLoadingPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("classLoading"));
        classLoadingPane.managedProperty().bind(classLoadingPane.visibleProperty());
        vmOperationsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("vmOperations"));
        vmOperationsPane.managedProperty().bind(vmOperationsPane.visibleProperty());
        processesPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("processes"));
        processesPane.managedProperty().bind(processesPane.visibleProperty());
        envVarsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("envVars"));
        envVarsPane.managedProperty().bind(envVarsPane.visibleProperty());
        sysPropsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("sysProps"));
        sysPropsPane.managedProperty().bind(sysPropsPane.visibleProperty());
        recordingInfoPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("recordingInfo"));
        recordingInfoPane.managedProperty().bind(recordingInfoPane.visibleProperty());
        agentsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("agents"));
        agentsPane.managedProperty().bind(agentsPane.visibleProperty());
        constantPoolsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("constantPools"));
        constantPoolsPane.managedProperty().bind(constantPoolsPane.visibleProperty());
        settingsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("settings"));
        settingsPane.managedProperty().bind(settingsPane.visibleProperty());
        bindOverview(null);
        bindEvents();
        configureAnalysisTable();
        configureProfilingTable();
        configureExceptionTable();
        configureThreadTable();
        configureFileIOTable();
        configureSocketIOTable();
        configureLockTables();
        configureHeapTable();
        configureLeaksTable();
        configureTlabTable();
        configureJvmFlagsTable();
        configureJvmFlagChangesTable();
        configureGcSummaryTable();
        configureGcEventsTable();
        configureGcReferenceStatsTable();
        configureGcHeapSummaryTable();
        configureCompilationsTable();
        configureCompilationFailuresTable();
        configureCodeCacheSweepsTable();
        configureCodeCacheStatsTable();
        configureClassLoadingHistogramTable();
        configureClassLoadingEventsTable();
        configureClassLoadingStatsTable();
        configureVmOperationSummaryTable();
        configureVmOperationEventsTable();
        configureProcessesTable();
        configureEnvVarsTable();
        configureSysPropsTable();
        configureRecordingsTable();
        configureSettingsTable();
        configureAgentsTable();
        configureConstantPoolsTable();
        bindWorkspaceSelection();
        i18n.localeProperty().addListener((observable, oldValue, newValue) -> refreshOverviewOnLocaleChange());
    }

    private void refreshOverviewOnLocaleChange() {
        if (overviewViewModel == null) {
            overviewRecordingNameLabel.setText(i18n.get("overview.noRecording"));
            overviewRecordingDetailsLabel.setText(i18n.get("overview.openPrompt"));
        } else {
            RecordingSummary recording = overviewViewModel.recordingProperty().get();
            if (recording != null) {
                overviewViewModel.recordingDetailsProperty().set(i18n.format("overview.details.format",
                        recording.path(),
                        formatEventTime(recording.startTime()),
                        formatEventTime(recording.endTime()),
                        DisplayFormats.formatDuration(recording.durationMillis()),
                        DisplayFormats.formatFileSize(recording.sizeBytes())));
            }
        }
        overviewAnalysisStatusLabel.setText(i18n.get("overview.analysisUnavailable"));
        overviewJvmStatusLabel.setText(i18n.get("overview.jvmUnavailable"));
    }

    private RuleResultsViewModel analysisViewModel;

    private void configureAnalysisTable() {
        analysisTable.setPlaceholder(new Label(i18n.get("analysis.empty")));

        TableColumn<RuleResult, Severity> severityCol = new TableColumn<>();
        severityCol.textProperty().bind(i18n.text("analysis.column.severity"));
        severityCol.setPrefWidth(80);
        severityCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().severity()));
        severityCol.setCellFactory(col -> new AnalysisSeverityCell<>());

        TableColumn<RuleResult, String> nameCol = new TableColumn<>();
        nameCol.textProperty().bind(i18n.text("analysis.column.name"));
        nameCol.setPrefWidth(300);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));

        TableColumn<RuleResult, Number> scoreCol = new TableColumn<>();
        scoreCol.textProperty().bind(i18n.text("analysis.column.score"));
        scoreCol.setPrefWidth(60);
        scoreCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().score()));

        TableColumn<RuleResult, String> summaryCol = new TableColumn<>();
        summaryCol.textProperty().bind(i18n.text("analysis.column.summary"));
        summaryCol.setPrefWidth(800);
        summaryCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().summary()));

        analysisTable.getColumns().setAll(List.of(severityCol, nameCol, scoreCol, summaryCol));
        analysisTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, val) -> showAnalysisDetail(val));
    }

    private void bindAnalysis(RuleResultsViewModel nextViewModel) {
        analysisTable.setItems(FXCollections.emptyObservableList());
        analysisDetailTitle.setText("");
        analysisDetailExplanation.setText("");
        analysisViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        analysisTable.setItems(nextViewModel.resultsProperty());
        analysisTable.getSelectionModel().selectFirst();
    }

    private void showAnalysisDetail(RuleResult result) {
        if (result == null) {
            analysisDetailTitle.setText("");
            analysisDetailExplanation.setText("");
            return;
        }
        analysisDetailTitle.setText(result.name());
        analysisDetailExplanation.setText(
                HtmlToTextFlow.toPlainText(result.explanation()));
    }

    private void configureProfilingTable() {
        profilingTable.setPlaceholder(new Label(i18n.get("profiling.empty")));

        TableColumn<HotMethod, String> methodCol = new TableColumn<>();
        methodCol.textProperty().bind(i18n.text("profiling.column.method"));
        methodCol.setPrefWidth(500);
        methodCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().method()));

        TableColumn<HotMethod, String> frameTypeCol = new TableColumn<>();
        frameTypeCol.textProperty().bind(i18n.text("profiling.column.frameType"));
        frameTypeCol.setPrefWidth(100);
        frameTypeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().frameType()));

        TableColumn<HotMethod, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("profiling.column.count"));
        countCol.setPrefWidth(80);
        countCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().count()));

        TableColumn<HotMethod, String> pctCol = new TableColumn<>();
        pctCol.textProperty().bind(i18n.text("profiling.column.percentage"));
        pctCol.setPrefWidth(80);
        pctCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                String.format("%.1f%%", cell.getValue().percentage())));

        profilingTable.getColumns().setAll(List.of(methodCol, frameTypeCol, countCol, pctCol));
        profilingTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, val) -> selectProfilingMethod(val));

        profilingCallersTree.setShowRoot(false);
        profilingCallersTree.setCellFactory(tree -> new javafx.scene.control.TreeCell<>() {
            @Override
            protected void updateItem(StackTreeNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.method() + " (" + item.count() + ")");
            }
        });
        profilingCalleesTree.setShowRoot(false);
        profilingCalleesTree.setCellFactory(tree -> new javafx.scene.control.TreeCell<>() {
            @Override
            protected void updateItem(StackTreeNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.method() + " (" + item.count() + ")");
            }
        });
    }

    private void configureExceptionTable() {
        exceptionsTable.setPlaceholder(new Label(i18n.get("exceptions.empty")));

        TableColumn<ExceptionSummary, String> keyCol = new TableColumn<>();
        keyCol.textProperty().bind(i18n.text("exceptions.column.key"));
        keyCol.setPrefWidth(500);
        keyCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().key()));

        TableColumn<ExceptionSummary, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("exceptions.column.count"));
        countCol.setPrefWidth(80);
        countCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().count()));

        TableColumn<ExceptionSummary, String> pctCol = new TableColumn<>();
        pctCol.textProperty().bind(i18n.text("exceptions.column.percentage"));
        pctCol.setPrefWidth(80);
        pctCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                String.format("%.1f%%", cell.getValue().percentage())));

        exceptionsTable.getColumns().setAll(List.of(keyCol, countCol, pctCol));

        exceptionsGroupByClass.setOnAction(event -> setExceptionGrouping(ExceptionGrouping.BY_CLASS));
        exceptionsGroupByMessage.setOnAction(event -> setExceptionGrouping(ExceptionGrouping.BY_MESSAGE));
        exceptionsGroupByClassAndMessage.setOnAction(event -> setExceptionGrouping(ExceptionGrouping.BY_CLASS_AND_MESSAGE));
    }

    private void configureThreadTable() {
        threadsTable.setPlaceholder(new Label(i18n.get("threads.empty")));

        TableColumn<ThreadSummary, String> nameCol = new TableColumn<>();
        nameCol.textProperty().bind(i18n.text("threads.column.name"));
        nameCol.setPrefWidth(400);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().threadName()));

        TableColumn<ThreadSummary, Number> samplesCol = new TableColumn<>();
        samplesCol.textProperty().bind(i18n.text("threads.column.samples"));
        samplesCol.setPrefWidth(100);
        samplesCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().sampleCount()));

        TableColumn<ThreadSummary, Number> blockedCol = new TableColumn<>();
        blockedCol.textProperty().bind(i18n.text("threads.column.blockedMs"));
        blockedCol.setPrefWidth(120);
        blockedCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().blockedDurationMillis()));

        threadsTable.getColumns().setAll(List.of(nameCol, samplesCol, blockedCol));
    }

    private void configureFileIOTable() {
        fileioHistogramTable.setPlaceholder(new Label(i18n.get("fileio.empty")));

        TableColumn<FileIOHistogram, String> pathCol = new TableColumn<>();
        pathCol.textProperty().bind(i18n.text("fileio.column.path"));
        pathCol.setPrefWidth(400);
        pathCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().path()));

        TableColumn<FileIOHistogram, Number> readCountCol = new TableColumn<>();
        readCountCol.textProperty().bind(i18n.text("fileio.column.readCount"));
        readCountCol.setPrefWidth(80);
        readCountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().readCount()));

        TableColumn<FileIOHistogram, Number> writeCountCol = new TableColumn<>();
        writeCountCol.textProperty().bind(i18n.text("fileio.column.writeCount"));
        writeCountCol.setPrefWidth(80);
        writeCountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().writeCount()));

        TableColumn<FileIOHistogram, Number> readSizeCol = new TableColumn<>();
        readSizeCol.textProperty().bind(i18n.text("fileio.column.readSize"));
        readSizeCol.setPrefWidth(100);
        readSizeCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().readSize()));

        TableColumn<FileIOHistogram, Number> writeSizeCol = new TableColumn<>();
        writeSizeCol.textProperty().bind(i18n.text("fileio.column.writeSize"));
        writeSizeCol.setPrefWidth(100);
        writeSizeCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().writeSize()));

        TableColumn<FileIOHistogram, String> avgDurationCol = new TableColumn<>();
        avgDurationCol.textProperty().bind(i18n.text("fileio.column.avgDuration"));
        avgDurationCol.setPrefWidth(100);
        avgDurationCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                String.format("%.2f ms", cell.getValue().avgDuration())));

        fileioHistogramTable.getColumns().setAll(List.of(pathCol, readCountCol, writeCountCol,
                readSizeCol, writeSizeCol, avgDurationCol));

        fileioEventTable.setPlaceholder(new Label(i18n.get("fileio.events.empty")));

        TableColumn<FileIOEvent, String> eventTypeCol = new TableColumn<>();
        eventTypeCol.textProperty().bind(i18n.text("fileio.events.column.eventType"));
        eventTypeCol.setPrefWidth(140);
        eventTypeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().eventType()));

        TableColumn<FileIOEvent, String> eventPathCol = new TableColumn<>();
        eventPathCol.textProperty().bind(i18n.text("fileio.events.column.path"));
        eventPathCol.setPrefWidth(400);
        eventPathCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().path()));

        TableColumn<FileIOEvent, Number> eventBytesCol = new TableColumn<>();
        eventBytesCol.textProperty().bind(i18n.text("fileio.events.column.bytes"));
        eventBytesCol.setPrefWidth(100);
        eventBytesCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().bytes()));

        TableColumn<FileIOEvent, String> eventDurationCol = new TableColumn<>();
        eventDurationCol.textProperty().bind(i18n.text("fileio.events.column.duration"));
        eventDurationCol.setPrefWidth(100);
        eventDurationCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                String.format("%.2f ms", cell.getValue().durationMillis())));

        TableColumn<FileIOEvent, String> eventThreadCol = new TableColumn<>();
        eventThreadCol.textProperty().bind(i18n.text("fileio.events.column.thread"));
        eventThreadCol.setPrefWidth(200);
        eventThreadCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().threadName()));

        fileioEventTable.getColumns().setAll(List.of(eventTypeCol, eventPathCol, eventBytesCol,
                eventDurationCol, eventThreadCol));
    }

    private void configureSocketIOTable() {
        socketioHistogramTable.setPlaceholder(new Label(i18n.get("socketio.empty")));

        TableColumn<SocketIOHistogram, String> keyCol = new TableColumn<>();
        keyCol.textProperty().bind(i18n.text("socketio.column.key"));
        keyCol.setPrefWidth(300);
        keyCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().key()));

        TableColumn<SocketIOHistogram, Number> sockReadCountCol = new TableColumn<>();
        sockReadCountCol.textProperty().bind(i18n.text("socketio.column.readCount"));
        sockReadCountCol.setPrefWidth(80);
        sockReadCountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().readCount()));

        TableColumn<SocketIOHistogram, Number> sockWriteCountCol = new TableColumn<>();
        sockWriteCountCol.textProperty().bind(i18n.text("socketio.column.writeCount"));
        sockWriteCountCol.setPrefWidth(80);
        sockWriteCountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().writeCount()));

        TableColumn<SocketIOHistogram, Number> sockReadSizeCol = new TableColumn<>();
        sockReadSizeCol.textProperty().bind(i18n.text("socketio.column.readSize"));
        sockReadSizeCol.setPrefWidth(100);
        sockReadSizeCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().readSize()));

        TableColumn<SocketIOHistogram, Number> sockWriteSizeCol = new TableColumn<>();
        sockWriteSizeCol.textProperty().bind(i18n.text("socketio.column.writeSize"));
        sockWriteSizeCol.setPrefWidth(100);
        sockWriteSizeCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().writeSize()));

        TableColumn<SocketIOHistogram, String> sockAvgDurationCol = new TableColumn<>();
        sockAvgDurationCol.textProperty().bind(i18n.text("socketio.column.avgDuration"));
        sockAvgDurationCol.setPrefWidth(100);
        sockAvgDurationCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                String.format("%.2f ms", cell.getValue().avgDuration())));

        socketioHistogramTable.getColumns().setAll(List.of(keyCol, sockReadCountCol, sockWriteCountCol,
                sockReadSizeCol, sockWriteSizeCol, sockAvgDurationCol));

        socketioGroupByHostAndPort.setOnAction(event -> setSocketIOGrouping(SocketIOGrouping.BY_HOST_AND_PORT));
        socketioGroupByHost.setOnAction(event -> setSocketIOGrouping(SocketIOGrouping.BY_HOST));
        socketioGroupByPort.setOnAction(event -> setSocketIOGrouping(SocketIOGrouping.BY_PORT));

        socketioEventTable.setPlaceholder(new Label(i18n.get("socketio.events.empty")));

        TableColumn<SocketIOEvent, String> sockEventTypeCol = new TableColumn<>();
        sockEventTypeCol.textProperty().bind(i18n.text("socketio.events.column.eventType"));
        sockEventTypeCol.setPrefWidth(140);
        sockEventTypeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().eventType()));

        TableColumn<SocketIOEvent, String> sockHostCol = new TableColumn<>();
        sockHostCol.textProperty().bind(i18n.text("socketio.events.column.host"));
        sockHostCol.setPrefWidth(200);
        sockHostCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().host()));

        TableColumn<SocketIOEvent, Number> sockPortCol = new TableColumn<>();
        sockPortCol.textProperty().bind(i18n.text("socketio.events.column.port"));
        sockPortCol.setPrefWidth(80);
        sockPortCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().port()));

        TableColumn<SocketIOEvent, Number> sockBytesCol = new TableColumn<>();
        sockBytesCol.textProperty().bind(i18n.text("socketio.events.column.bytes"));
        sockBytesCol.setPrefWidth(100);
        sockBytesCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().bytes()));

        TableColumn<SocketIOEvent, String> sockDurationCol = new TableColumn<>();
        sockDurationCol.textProperty().bind(i18n.text("socketio.events.column.duration"));
        sockDurationCol.setPrefWidth(100);
        sockDurationCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                String.format("%.2f ms", cell.getValue().durationMillis())));

        TableColumn<SocketIOEvent, String> sockThreadCol = new TableColumn<>();
        sockThreadCol.textProperty().bind(i18n.text("socketio.events.column.thread"));
        sockThreadCol.setPrefWidth(200);
        sockThreadCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().threadName()));

        socketioEventTable.getColumns().setAll(List.of(sockEventTypeCol, sockHostCol, sockPortCol,
                sockBytesCol, sockDurationCol, sockThreadCol));
    }

    private void configureLockTables() {
        configureSingleLockTable(locksByClassTable, "locks.empty");
        configureSingleLockTable(locksByAddressTable, "locks.empty");
        configureSingleLockTable(locksByThreadTable, "locks.empty");

        locksGroupByClass.setOnAction(event -> {
            if (lockViewModel != null) {
                lockViewModel.setPrimaryGrouping(LockGrouping.BY_CLASS);
            }
        });
        locksGroupByAddress.setOnAction(event -> {
            if (lockViewModel != null) {
                lockViewModel.setPrimaryGrouping(LockGrouping.BY_ADDRESS);
            }
        });
        locksGroupByThread.setOnAction(event -> {
            if (lockViewModel != null) {
                lockViewModel.setPrimaryGrouping(LockGrouping.BY_THREAD);
            }
        });
    }

    private void configureSingleLockTable(TableView<LockHistogram> table, String emptyKey) {
        table.setPlaceholder(new Label(i18n.get(emptyKey)));

        TableColumn<LockHistogram, String> keyCol = new TableColumn<>();
        keyCol.textProperty().bind(i18n.text("locks.column.key"));
        keyCol.setPrefWidth(400);
        keyCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().key()));

        TableColumn<LockHistogram, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("locks.column.count"));
        countCol.setPrefWidth(80);
        countCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().count()));

        TableColumn<LockHistogram, String> totalDurCol = new TableColumn<>();
        totalDurCol.textProperty().bind(i18n.text("locks.column.totalDuration"));
        totalDurCol.setPrefWidth(120);
        totalDurCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDuration(cell.getValue().totalDuration())));

        TableColumn<LockHistogram, String> maxDurCol = new TableColumn<>();
        maxDurCol.textProperty().bind(i18n.text("locks.column.maxDuration"));
        maxDurCol.setPrefWidth(120);
        maxDurCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDuration(cell.getValue().maxDuration())));

        TableColumn<LockHistogram, String> avgDurCol = new TableColumn<>();
        avgDurCol.textProperty().bind(i18n.text("locks.column.avgDuration"));
        avgDurCol.setPrefWidth(120);
        avgDurCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                String.format("%.2f ms", cell.getValue().avgDuration())));

        table.getColumns().setAll(List.of(keyCol, countCol, totalDurCol, maxDurCol, avgDurCol));
    }

    private void bindProfiling(ProfilingViewModel nextViewModel) {
        profilingTable.setItems(FXCollections.emptyObservableList());
        profilingCallersTree.setRoot(new TreeItem<>());
        profilingCalleesTree.setRoot(new TreeItem<>());
        profilingViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        profilingTable.setItems(nextViewModel.hotMethodsProperty());
        nextViewModel.callersTreeProperty().addListener((obs, old, val) -> rebuildStackTree(profilingCallersTree, val));
        nextViewModel.calleesTreeProperty().addListener((obs, old, val) -> rebuildStackTree(profilingCalleesTree, val));
        rebuildStackTree(profilingCallersTree, nextViewModel.callersTreeProperty().get());
        rebuildStackTree(profilingCalleesTree, nextViewModel.calleesTreeProperty().get());
    }

    private void bindExceptions(ExceptionViewModel nextViewModel) {
        exceptionsTable.setItems(FXCollections.emptyObservableList());
        exceptionViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        exceptionsTable.setItems(nextViewModel.histogramProperty());
    }

    private void bindThreads(ThreadViewModel nextViewModel) {
        threadsTable.setItems(FXCollections.emptyObservableList());
        threadViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        threadsTable.setItems(nextViewModel.threadSummariesProperty());
    }

    private void bindFileIO(FileIOViewModel nextViewModel) {
        fileioHistogramTable.setItems(FXCollections.emptyObservableList());
        fileioEventTable.setItems(FXCollections.emptyObservableList());
        fileIOViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        fileioHistogramTable.setItems(nextViewModel.histogramProperty());
        fileioEventTable.setItems(nextViewModel.eventsProperty());
    }

    private void bindSocketIO(SocketIOViewModel nextViewModel) {
        socketioHistogramTable.setItems(FXCollections.emptyObservableList());
        socketioEventTable.setItems(FXCollections.emptyObservableList());
        socketIOViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        socketioHistogramTable.setItems(nextViewModel.histogramProperty());
        socketioEventTable.setItems(nextViewModel.eventsProperty());
    }

    private void bindLocks(LockViewModel nextViewModel) {
        locksByClassTable.setItems(FXCollections.emptyObservableList());
        locksByAddressTable.setItems(FXCollections.emptyObservableList());
        locksByThreadTable.setItems(FXCollections.emptyObservableList());
        lockViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        locksByClassTable.setItems(nextViewModel.classHistogramProperty());
        locksByAddressTable.setItems(nextViewModel.addressHistogramProperty());
        locksByThreadTable.setItems(nextViewModel.threadHistogramProperty());
    }

    private void configureHeapTable() {
        heapTable.setPlaceholder(new Label(i18n.get("heap.empty")));

        TableColumn<HeapClassHistogram, String> classNameCol = new TableColumn<>();
        classNameCol.textProperty().bind(i18n.text("heap.column.className"));
        classNameCol.setPrefWidth(300);
        classNameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().className()));

        TableColumn<HeapClassHistogram, Number> instancesCol = new TableColumn<>();
        instancesCol.textProperty().bind(i18n.text("heap.column.instances"));
        instancesCol.setPrefWidth(100);
        instancesCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().instances()));

        TableColumn<HeapClassHistogram, Number> sizeCol = new TableColumn<>();
        sizeCol.textProperty().bind(i18n.text("heap.column.size"));
        sizeCol.setPrefWidth(100);
        sizeCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().size()));

        TableColumn<HeapClassHistogram, String> pctCol = new TableColumn<>();
        pctCol.textProperty().bind(i18n.text("heap.column.allocationPct"));
        pctCol.setPrefWidth(120);
        pctCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                String.format("%.1f%%", cell.getValue().allocationPct())));

        heapTable.getColumns().setAll(List.of(classNameCol, instancesCol, sizeCol, pctCol));
    }

    private void configureLeaksTable() {
        leaksTable.setPlaceholder(new Label(i18n.get("leaks.empty")));

        TableColumn<LeakCandidate, String> objectCol = new TableColumn<>();
        objectCol.textProperty().bind(i18n.text("leaks.column.object"));
        objectCol.setPrefWidth(300);
        objectCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().object()));

        TableColumn<LeakCandidate, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("leaks.column.count"));
        countCol.setPrefWidth(80);
        countCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().count()));

        TableColumn<LeakCandidate, String> descCol = new TableColumn<>();
        descCol.textProperty().bind(i18n.text("leaks.column.description"));
        descCol.setPrefWidth(200);
        descCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().description()));

        TableColumn<LeakCandidate, String> addressCol = new TableColumn<>();
        addressCol.textProperty().bind(i18n.text("leaks.column.address"));
        addressCol.setPrefWidth(100);
        addressCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().address()));

        TableColumn<LeakCandidate, String> relevanceCol = new TableColumn<>();
        relevanceCol.textProperty().bind(i18n.text("leaks.column.relevance"));
        relevanceCol.setPrefWidth(120);
        relevanceCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                String.format("%.1f%%", cell.getValue().relevance())));

        leaksTable.getColumns().setAll(List.of(objectCol, countCol, descCol, addressCol, relevanceCol));
        leaksTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, val) -> {
                    if (val != null) {
                        int idx = leaksTable.getItems().indexOf(val);
                        leaksViewModel.selectCandidate(idx);
                    }
                });

        leaksReferenceTree.setShowRoot(false);
        leaksReferenceTree.setCellFactory(tree -> new javafx.scene.control.TreeCell<>() {
            @Override
            protected void updateItem(LeakReferenceNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.object());
            }
        });
    }

    private void configureTlabTable() {
        tlabTable.setPlaceholder(new Label(i18n.get("tlab.empty")));

        TableColumn<TlabAllocation, String> threadCol = new TableColumn<>();
        threadCol.textProperty().bind(i18n.text("tlab.column.thread"));
        threadCol.setPrefWidth(200);
        threadCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().thread()));

        TableColumn<TlabAllocation, Number> insideCountCol = new TableColumn<>();
        insideCountCol.textProperty().bind(i18n.text("tlab.column.insideCount"));
        insideCountCol.setPrefWidth(100);
        insideCountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().insideCount()));

        TableColumn<TlabAllocation, Number> outsideCountCol = new TableColumn<>();
        outsideCountCol.textProperty().bind(i18n.text("tlab.column.outsideCount"));
        outsideCountCol.setPrefWidth(100);
        outsideCountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().outsideCount()));

        TableColumn<TlabAllocation, Number> insideTotalCol = new TableColumn<>();
        insideTotalCol.textProperty().bind(i18n.text("tlab.column.insideTotalSize"));
        insideTotalCol.setPrefWidth(120);
        insideTotalCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().insideTotalSize()));

        TableColumn<TlabAllocation, Number> outsideTotalCol = new TableColumn<>();
        outsideTotalCol.textProperty().bind(i18n.text("tlab.column.outsideTotalSize"));
        outsideTotalCol.setPrefWidth(120);
        outsideTotalCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().outsideTotalSize()));

        tlabTable.getColumns().setAll(List.of(threadCol, insideCountCol, outsideCountCol, insideTotalCol, outsideTotalCol));
    }

    private void bindHeap(HeapViewModel nextViewModel) {
        heapTable.setItems(FXCollections.emptyObservableList());
        heapTimelineLabel.textProperty().unbind();
        heapTimelineLabel.setText("");
        heapViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        heapTable.setItems(nextViewModel.histogramProperty());
        heapTable.getSelectionModel().selectFirst();
    }

    private void bindLeaks(LeakSuspectsViewModel nextViewModel) {
        leaksTable.setItems(FXCollections.emptyObservableList());
        leaksReferenceTree.setRoot(null);
        leaksViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        leaksTable.setItems(nextViewModel.candidatesProperty());
        nextViewModel.referenceTreeProperty().addListener((obs, old, val) -> updateLeakReferenceTree(val));
        leaksTable.getSelectionModel().selectFirst();
    }

    private void bindTlab(TlabViewModel nextViewModel) {
        tlabTable.setItems(FXCollections.emptyObservableList());
        tlabTimelineLabel.textProperty().unbind();
        tlabTimelineLabel.setText("");
        tlabViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        tlabTable.setItems(nextViewModel.allocationsProperty());
        tlabTable.getSelectionModel().selectFirst();
    }

    private void updateLeakReferenceTree(LeakReferenceNode node) {
        if (node == null || node == LeakReferenceNode.EMPTY) {
            leaksReferenceTree.setRoot(null);
            return;
        }
        TreeItem<LeakReferenceNode> root = buildReferenceTreeItem(node);
        root.setExpanded(true);
        leaksReferenceTree.setRoot(root);
    }

    private TreeItem<LeakReferenceNode> buildReferenceTreeItem(LeakReferenceNode node) {
        TreeItem<LeakReferenceNode> item = new TreeItem<>(node);
        for (LeakReferenceNode child : node.children()) {
            item.getChildren().add(buildReferenceTreeItem(child));
        }
        return item;
    }

    private void setSocketIOGrouping(SocketIOGrouping grouping) {
        if (socketIOViewModel == null) {
            return;
        }
        socketIOViewModel.setGrouping(grouping);
    }

    private void selectProfilingMethod(HotMethod method) {
        if (profilingViewModel == null || method == null) {
            return;
        }
        profilingViewModel.selectMethod(method.method());
    }

    private void setExceptionGrouping(ExceptionGrouping grouping) {
        if (exceptionViewModel == null) {
            return;
        }
        exceptionViewModel.setGrouping(grouping);
    }

    private void rebuildStackTree(TreeView<StackTreeNode> tree, StackTreeNode root) {
        if (root == null || root == StackTreeNode.EMPTY) {
            tree.setRoot(new TreeItem<>());
            return;
        }
        TreeItem<StackTreeNode> rootItem = toStackTreeNodeItem(root);
        rootItem.setExpanded(true);
        tree.setRoot(rootItem);
    }

    private TreeItem<StackTreeNode> toStackTreeNodeItem(StackTreeNode node) {
        TreeItem<StackTreeNode> item = new TreeItem<>(node);
        if (node.children() != null) {
            node.children().stream()
                    .map(this::toStackTreeNodeItem)
                    .forEach(item.getChildren()::add);
        }
        return item;
    }

    private void bindLocalizedText() {
        homeKickerLabel.textProperty().bind(i18n.text("home.kicker"));
        homeTitleLabel.textProperty().bind(i18n.text("home.title"));
        homeSubtitleLabel.textProperty().bind(i18n.text("home.subtitle"));
        homeOpenRecordingButton.textProperty().bind(i18n.text("home.openRecording"));
        homeConnectJvmButton.textProperty().bind(i18n.text("home.connectJvm"));
        homeOpenWorkflowTitleLabel.textProperty().bind(i18n.text("home.workflow.openTitle"));
        homeOpenWorkflowDescriptionLabel.textProperty().bind(i18n.text("home.workflow.openDescription"));
        homeEventsWorkflowTitleLabel.textProperty().bind(i18n.text("home.workflow.eventsTitle"));
        homeEventsWorkflowDescriptionLabel.textProperty().bind(i18n.text("home.workflow.eventsDescription"));
        homeJvmWorkflowTitleLabel.textProperty().bind(i18n.text("home.workflow.jvmTitle"));
        homeJvmWorkflowDescriptionLabel.textProperty().bind(i18n.text("home.workflow.jvmDescription"));
        homeDisclaimerLabel.textProperty().bind(i18n.text("home.disclaimer"));
        overviewTitleLabel.textProperty().bind(i18n.text("overview.title"));
        overviewAnalysisTitleLabel.textProperty().bind(i18n.text("overview.card.analysis"));
        overviewJvmsTitleLabel.textProperty().bind(i18n.text("overview.card.jvms"));
        eventsTitleLabel.textProperty().bind(i18n.text("events.title"));
        eventSearchField.promptTextProperty().bind(i18n.text("events.search.prompt"));
        threadFilterField.promptTextProperty().bind(i18n.text("events.thread.prompt"));
        fieldFilterField.promptTextProperty().bind(i18n.text("events.field.prompt"));
        clearEventFiltersButton.textProperty().bind(i18n.text("events.filters.clear"));
        columnsButton.textProperty().bind(i18n.text("events.columns"));
        eventPropertiesTab.textProperty().bind(i18n.text("events.details.properties"));
        eventTimingTab.textProperty().bind(i18n.text("events.details.timing"));
        eventThreadTab.textProperty().bind(i18n.text("events.details.thread"));
        eventStackTraceTab.textProperty().bind(i18n.text("events.details.stackTrace"));
        analysisTitleLabel.textProperty().bind(i18n.text("analysis.title"));
        jvmsTitleLabel.textProperty().bind(i18n.text("jvms.title"));
        jvmsUnavailableLabel.textProperty().bind(i18n.text("jvms.unavailable"));
        profilingTitleLabel.textProperty().bind(i18n.text("profiling.title"));
        profilingCallersTab.textProperty().bind(i18n.text("profiling.tab.callers"));
        profilingCalleesTab.textProperty().bind(i18n.text("profiling.tab.callees"));
        exceptionsTitleLabel.textProperty().bind(i18n.text("exceptions.title"));
        exceptionsGroupByClass.textProperty().bind(i18n.text("exceptions.grouping.byClass"));
        exceptionsGroupByMessage.textProperty().bind(i18n.text("exceptions.grouping.byMessage"));
        exceptionsGroupByClassAndMessage.textProperty().bind(i18n.text("exceptions.grouping.byClassAndMessage"));
        exceptionsTimelineLabel.textProperty().bind(i18n.text("exceptions.timeline"));
        threadsTitleLabel.textProperty().bind(i18n.text("threads.title"));
        fileioTitleLabel.textProperty().bind(i18n.text("fileio.title"));
        fileioTimelineTab.textProperty().bind(i18n.text("fileio.tab.timeline"));
        fileioTimelinePlaceholderLabel.textProperty().bind(i18n.text("fileio.timeline.unavailable"));
        fileioDurationTab.textProperty().bind(i18n.text("fileio.tab.duration"));
        fileioEventLogTab.textProperty().bind(i18n.text("fileio.tab.eventLog"));
        socketioTitleLabel.textProperty().bind(i18n.text("socketio.title"));
        socketioGroupByHostAndPort.textProperty().bind(i18n.text("socketio.grouping.byHostAndPort"));
        socketioGroupByHost.textProperty().bind(i18n.text("socketio.grouping.byHost"));
        socketioGroupByPort.textProperty().bind(i18n.text("socketio.grouping.byPort"));
        socketioTimelineTab.textProperty().bind(i18n.text("socketio.tab.timeline"));
        socketioTimelinePlaceholderLabel.textProperty().bind(i18n.text("socketio.timeline.unavailable"));
        socketioDurationTab.textProperty().bind(i18n.text("socketio.tab.duration"));
        socketioEventLogTab.textProperty().bind(i18n.text("socketio.tab.eventLog"));
        locksTitleLabel.textProperty().bind(i18n.text("locks.title"));
        locksGroupByClass.textProperty().bind(i18n.text("locks.grouping.byClass"));
        locksGroupByAddress.textProperty().bind(i18n.text("locks.grouping.byAddress"));
        locksGroupByThread.textProperty().bind(i18n.text("locks.grouping.byThread"));
        locksByClassTab.textProperty().bind(i18n.text("locks.tab.byClass"));
        locksByAddressTab.textProperty().bind(i18n.text("locks.tab.byAddress"));
        locksByThreadTab.textProperty().bind(i18n.text("locks.tab.byThread"));
        heapTitleLabel.textProperty().bind(i18n.text("heap.title"));
        heapTimelineLabel.textProperty().bind(i18n.text("heap.timeline"));
        leaksTitleLabel.textProperty().bind(i18n.text("leaks.title"));
        tlabTitleLabel.textProperty().bind(i18n.text("tlab.title"));
        tlabTimelineLabel.textProperty().bind(i18n.text("tlab.timeline"));
        jvmInfoTitleLabel.textProperty().bind(i18n.text("jvmInfo.title"));
        jvmFlagsLabel.textProperty().bind(i18n.text("jvmInfo.flags"));
        jvmFlagChangesLabel.textProperty().bind(i18n.text("jvmInfo.flagChanges"));
        gcConfigTitleLabel.textProperty().bind(i18n.text("gcConfig.title"));
        gcConfigDescriptionLabel.textProperty().bind(i18n.text("gcConfig.empty"));
        gcSummaryTitleLabel.textProperty().bind(i18n.text("gcSummary.title"));
        gcDetailsTitleLabel.textProperty().bind(i18n.text("gcDetails.title"));
        gcEventsLabel.textProperty().bind(i18n.text("gcDetails.events"));
        gcReferenceStatsLabel.textProperty().bind(i18n.text("gcDetails.referenceStats"));
        gcHeapSummaryLabel.textProperty().bind(i18n.text("gcDetails.heapSummaries"));
        compilationsTitleLabel.textProperty().bind(i18n.text("compilations.title"));
        compilationEventsLabel.textProperty().bind(i18n.text("compilations.events"));
        compilationFailuresLabel.textProperty().bind(i18n.text("compilations.failures"));
        codeCacheTitleLabel.textProperty().bind(i18n.text("codeCache.title"));
        codeCacheSweepsLabel.textProperty().bind(i18n.text("codeCache.sweeps"));
        codeCacheStatsLabel.textProperty().bind(i18n.text("codeCache.statistics"));
        classLoadingTitleLabel.textProperty().bind(i18n.text("classLoading.title"));
        classLoadingHistogramLabel.textProperty().bind(i18n.text("classLoading.histogram"));
        classLoadingEventsLabel.textProperty().bind(i18n.text("classLoading.events"));
        classLoadingStatsLabel.textProperty().bind(i18n.text("classLoading.statistics"));
        vmOperationsTitleLabel.textProperty().bind(i18n.text("vmOperations.title"));
        vmOperationSummaryLabel.textProperty().bind(i18n.text("vmOperations.summary"));
        vmOperationEventsLabel.textProperty().bind(i18n.text("vmOperations.events"));
        processesTitleLabel.textProperty().bind(i18n.text("processes.title"));
        envVarsTitleLabel.textProperty().bind(i18n.text("envVars.title"));
        sysPropsTitleLabel.textProperty().bind(i18n.text("sysProps.title"));
        recordingInfoTitleLabel.textProperty().bind(i18n.text("recordingInfo.title"));
        recordingInfoRecordingsTab.textProperty().bind(i18n.text("recordingInfo.tab.recordings"));
        recordingInfoSettingsTab.textProperty().bind(i18n.text("recordingInfo.tab.settings"));
        agentsTitleLabel.textProperty().bind(i18n.text("agents.title"));
        constantPoolsTitleLabel.textProperty().bind(i18n.text("constantPools.title"));
        envVarsSearchField.promptTextProperty().bind(i18n.text("envVars.search.prompt"));
        sysPropsSearchField.promptTextProperty().bind(i18n.text("sysProps.search.prompt"));
        settingsTitleLabel.textProperty().bind(i18n.text("settings.title"));
        settingsLanguageLabel.textProperty().bind(i18n.text("settings.language"));
    }

    private void configureLanguageSelector() {
        languageFollowSystemRadio.setUserData(LanguageMode.SYSTEM);
        languageEnglishRadio.setUserData(LanguageMode.ENGLISH);
        languageChineseRadio.setUserData(LanguageMode.CHINESE_SIMPLIFIED);

        languageFollowSystemRadio.textProperty().bind(i18n.text("settings.language.followSystem"));
        languageEnglishRadio.textProperty().bind(i18n.text("settings.language.english"));
        languageChineseRadio.textProperty().bind(i18n.text("settings.language.chineseSimplified"));

        languageToggleGroup.selectToggle(modeToToggle(viewModel.languageModeProperty().get()));

        languageToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.getUserData() instanceof LanguageMode mode) {
                viewModel.setLanguageMode(mode);
                i18n.setLanguageMode(mode);
            }
        });
    }

    private Toggle modeToToggle(LanguageMode mode) {
        if (mode == LanguageMode.ENGLISH) {
            return languageEnglishRadio;
        }
        if (mode == LanguageMode.CHINESE_SIMPLIFIED) {
            return languageChineseRadio;
        }
        return languageFollowSystemRadio;
    }

    static boolean shouldSelectEventTypesTreeNode(String eventTypeId) {
        return eventTypeId != null && !eventTypeId.isBlank() && !EventTypeSelection.ALL_ID.equals(eventTypeId);
    }

    static boolean shouldClearEventTypesTreeSelection(String eventTypeId) {
        return !shouldSelectEventTypesTreeNode(eventTypeId);
    }

    static boolean shouldInitializeEventTypesDivider(boolean initialized, boolean eventsVisible) {
        return !initialized && eventsVisible;
    }

    static Region emptyTablePlaceholder() {
        Region placeholder = new Region();
        placeholder.setManaged(false);
        return placeholder;
    }

    static String tabTitleFor(RecordingWorkspace workspace) {
        return workspace.recording().name();
    }

    static boolean shouldShowRecordingTabs(int workspaceCount) {
        return workspaceCount > 0;
    }

    static String noTimingSelectionText(I18n i18n) {
        return i18n.get("events.details.selectTiming");
    }

    static String noThreadSelectionText(I18n i18n) {
        return i18n.get("events.details.selectThread");
    }

    static String openRecordingChooserTitle(I18n i18n) {
        return i18n.get("fileChooser.openRecording.title");
    }

    static String jfrRecordingsFilterDescription(I18n i18n) {
        return i18n.get("fileChooser.jfrRecordings");
    }

    static String languageModeDisplayName(I18n i18n, LanguageMode mode) {
        return switch (mode) {
            case ENGLISH -> i18n.get("settings.language.english");
            case CHINESE_SIMPLIFIED -> i18n.get("settings.language.chineseSimplified");
            case SYSTEM -> i18n.get("settings.language.followSystem");
        };
    }

    public BorderPane root() {
        return root;
    }

    public void close() {
        List.copyOf(viewModel.recordingWorkspacesProperty()).forEach(viewModel::closeWorkspace);
    }

    private void configureActionIcons() {
        configureActionButton(homeOpenRecordingButton, Material2AL.FOLDER_OPEN, i18n.get("home.openRecording"));
    }

    private static void configureActionButton(Button button, Ikon icon, String accessibleText) {
        button.setGraphic(new FontIcon(icon));
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setAccessibleText(accessibleText);
        button.setTooltip(new javafx.scene.control.Tooltip(accessibleText));
    }

    private void configureRecordingTabs() {
        recordingTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        recordingTabs.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingRecordingTabs || newValue == null || !(newValue.getUserData() instanceof RecordingWorkspace workspace)) {
                return;
            }
            viewModel.selectWorkspace(workspace);
        });
        viewModel.recordingWorkspacesProperty()
                .addListener((ListChangeListener<RecordingWorkspace>) change -> rebuildRecordingTabs());
        rebuildRecordingTabs();
    }

    private void bindWorkspaceSelection() {
        viewModel.selectedWorkspaceProperty()
                .addListener((observable, oldValue, newValue) -> showWorkspace(newValue));
        showWorkspace(viewModel.selectedWorkspaceProperty().get());
    }

    private void rebuildRecordingTabs() {
        updatingRecordingTabs = true;
        try {
            List<Tab> tabs = viewModel.recordingWorkspacesProperty().stream()
                    .map(this::toRecordingTab)
                    .toList();
            recordingTabs.getTabs().setAll(tabs);
            boolean showTabs = shouldShowRecordingTabs(tabs.size());
            recordingTabs.setVisible(showTabs);
            recordingTabs.setManaged(showTabs);
            selectRecordingTab(viewModel.selectedWorkspaceProperty().get());
        } finally {
            updatingRecordingTabs = false;
        }
    }

    private Tab toRecordingTab(RecordingWorkspace workspace) {
        Tab tab = new Tab(tabTitleFor(workspace));
        tab.setUserData(workspace);
        tab.setClosable(true);
        tab.setOnClosed(event -> viewModel.closeWorkspace(workspace));
        return tab;
    }

    private void selectRecordingTab(RecordingWorkspace workspace) {
        if (workspace == null) {
            recordingTabs.getSelectionModel().clearSelection();
            return;
        }
        recordingTabs.getTabs().stream()
                .filter(tab -> tab.getUserData() == workspace)
                .findFirst()
                .ifPresent(recordingTabs.getSelectionModel()::select);
    }

    private void showWorkspace(RecordingWorkspace workspace) {
        selectRecordingTab(workspace);
        bindOverview(workspace == null ? null : workspace.overviewViewModel());
        bindEventBrowser(workspace == null ? null : workspace.eventBrowserViewModel());
        bindAnalysis(workspace == null ? null : workspace.ruleResultsViewModel());
        bindProfiling(workspace == null ? null : workspace.profilingViewModel());
        bindExceptions(workspace == null ? null : workspace.exceptionViewModel());
        bindThreads(workspace == null ? null : workspace.threadViewModel());
        bindFileIO(workspace == null ? null : workspace.fileIOViewModel());
        bindSocketIO(workspace == null ? null : workspace.socketIOViewModel());
        bindLocks(workspace == null ? null : workspace.lockViewModel());
        bindHeap(workspace == null ? null : workspace.heapViewModel());
        bindLeaks(workspace == null ? null : workspace.leakSuspectsViewModel());
        bindTlab(workspace == null ? null : workspace.tlabViewModel());
        bindJvmInfo(workspace == null ? null : workspace.jvmInfoViewModel());
        bindGcConfig(workspace == null ? null : workspace.gcConfigViewModel());
        bindGcSummary(workspace == null ? null : workspace.gcSummaryViewModel());
        bindGcDetails(workspace == null ? null : workspace.gcDetailsViewModel());
        bindCompilations(workspace == null ? null : workspace.compilationsViewModel());
        bindCodeCache(workspace == null ? null : workspace.codeCacheViewModel());
        bindClassLoading(workspace == null ? null : workspace.classLoadingViewModel());
        bindVmOperations(workspace == null ? null : workspace.vmOperationsViewModel());
        bindEnvironment(workspace == null ? null : workspace.environmentViewModel());
    }

    private void bindOverview(OverviewViewModel nextViewModel) {
        overviewRecordingNameLabel.textProperty().unbind();
        overviewRecordingDetailsLabel.textProperty().unbind();
        overviewAnalysisStatusLabel.textProperty().unbind();
        overviewJvmStatusLabel.textProperty().unbind();
        overviewViewModel = nextViewModel;
        if (nextViewModel == null) {
            overviewRecordingNameLabel.setText(i18n.get("overview.noRecording"));
            overviewRecordingDetailsLabel.setText(i18n.get("overview.openPrompt"));
            overviewAnalysisStatusLabel.setText(i18n.get("overview.analysisUnavailable"));
            overviewJvmStatusLabel.setText(i18n.get("overview.jvmUnavailable"));
            return;
        }
        overviewRecordingNameLabel.textProperty().bind(nextViewModel.recordingNameProperty());
        overviewRecordingDetailsLabel.textProperty().bind(nextViewModel.recordingDetailsProperty());
        overviewAnalysisStatusLabel.setText(i18n.get("overview.analysisUnavailable"));
        overviewJvmStatusLabel.setText(i18n.get("overview.jvmUnavailable"));
    }

    private void bindEvents() {
        eventTypesTree.setShowRoot(false);
        eventTypesTree.setMinWidth(MIN_EVENT_TYPES_WIDTH);
        eventTypesTree.setPrefWidth(DEFAULT_EVENT_TYPES_WIDTH);
        eventTypesTree.setMaxWidth(MAX_EVENT_TYPES_WIDTH);
        SplitPane.setResizableWithParent(eventTypesTree, true);
        eventsPane.visibleProperty().addListener((observable, oldValue, newValue) -> initializeEventTypesDivider());
        initializeEventTypesDivider();
        eventTypesTree.setCellFactory(tree -> new javafx.scene.control.TreeCell<>() {
            @Override
            protected void updateItem(EventTypeNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : eventTypeText(item));
            }
        });
        eventTypesTree.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> selectEventType(newValue));

        eventsTable.setPlaceholder(emptyTablePlaceholder());
        eventsTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> selectEventRow(newValue));

        clearEventFiltersButton.setOnAction(event -> clearEventFilters());
        eventSearchField.setOnAction(event -> refreshVisibleRange());
        threadFilterField.setOnAction(event -> refreshVisibleRange());
        fieldFilterField.setOnAction(event -> refreshVisibleRange());

        configureEventPropertiesTable();
        bindEventBrowser(null);
    }

    private void bindEventBrowser(EventBrowserViewModel nextViewModel) {
        if (eventBrowserViewModel != null) {
            eventBrowserViewModel.eventTypeTreeProperty().removeListener(eventTypeTreeListener);
            eventBrowserViewModel.columnsProperty().removeListener(eventColumnsListener);
            eventBrowserViewModel.fieldDescriptorsProperty().removeListener(fieldDescriptorsListener);
            eventBrowserViewModel.rowsProperty().removeListener(eventRowsListener);
            eventBrowserViewModel.selectedDetailsProperty().removeListener(selectedDetailsListener);
            eventBrowserViewModel.selectionPropertiesProperty().removeListener(selectionPropertiesListener);
        }
        eventWindowStatusLabel.textProperty().unbind();
        eventBrowserViewModel = nextViewModel;
        if (nextViewModel == null) {
            eventTypesTree.setRoot(new TreeItem<>());
            eventsTable.setItems(FXCollections.emptyObservableList());
            eventsTable.getColumns().clear();
            columnsButton.getItems().clear();
            eventWindowStatusLabel.setText(i18n.get("events.window.openPrompt"));
            showEventDetails(null);
            showSelectionProperties(null);
            return;
        }
        nextViewModel.eventTypeTreeProperty().addListener(eventTypeTreeListener);
        nextViewModel.columnsProperty().addListener(eventColumnsListener);
        nextViewModel.fieldDescriptorsProperty().addListener(fieldDescriptorsListener);
        nextViewModel.rowsProperty().addListener(eventRowsListener);
        nextViewModel.selectedDetailsProperty().addListener(selectedDetailsListener);
        nextViewModel.selectionPropertiesProperty().addListener(selectionPropertiesListener);
        eventsTable.setItems(nextViewModel.rowsProperty());
        eventWindowStatusLabel.textProperty().bind(nextViewModel.statusMessageProperty());
        rebuildEventTypeTree();
        rebuildEventColumns();
        showEventDetails(nextViewModel.selectedDetailsProperty().get());
        showSelectionProperties(nextViewModel.selectionPropertiesProperty().get());
    }

    private void initializeEventTypesDivider() {
        if (!shouldInitializeEventTypesDivider(eventTypesDividerInitialized, eventsPane.isVisible())) {
            return;
        }
        eventTypesDividerInitialized = true;
        Platform.runLater(() -> eventsSplitPane.setDividerPositions(DEFAULT_EVENT_TYPES_DIVIDER_POSITION));
    }

    private void openRecording() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(openRecordingChooserTitle(i18n));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(jfrRecordingsFilterDescription(i18n), "*.jfr"));
        java.io.File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) {
            return;
        }
        RecordingSummary recording = recordingRepository.open(file.toPath());
        OverviewViewModel overview = new OverviewViewModel();
        EventBrowserViewModel events = new EventBrowserViewModel(eventQueryService,
                new VirtualThreadEventBrowserExecutor(), i18n);
        RuleResultsViewModel analysis = new RuleResultsViewModel(ruleAnalysisService);
        ProfilingViewModel profiling = profilingService != null ? new ProfilingViewModel(profilingService) : null;
        ExceptionViewModel exceptions = exceptionService != null ? new ExceptionViewModel(exceptionService) : null;
        ThreadViewModel threads = threadService != null ? new ThreadViewModel(threadService) : null;
        FileIOViewModel fileio = fileIOService != null ? new FileIOViewModel(fileIOService) : null;
        SocketIOViewModel socketio = socketIOService != null ? new SocketIOViewModel(socketIOService) : null;
        LockViewModel locks = lockService != null ? new LockViewModel(lockService) : null;
        HeapViewModel heap = heapService != null ? new HeapViewModel(heapService) : null;
        LeakSuspectsViewModel leakSuspects = leakSuspectsService != null ? new LeakSuspectsViewModel(leakSuspectsService) : null;
        TlabViewModel tlab = tlabService != null ? new TlabViewModel(tlabService) : null;
        JvmInfoViewModel jvmInfo = jvmInternalsService != null ? new JvmInfoViewModel(jvmInternalsService) : null;
        GcConfigViewModel gcConfig = jvmInternalsService != null ? new GcConfigViewModel(jvmInternalsService) : null;
        GcSummaryViewModel gcSummary = jvmInternalsService != null ? new GcSummaryViewModel(jvmInternalsService) : null;
        GcDetailsViewModel gcDetails = jvmInternalsService != null ? new GcDetailsViewModel(jvmInternalsService) : null;
        CompilationsViewModel compilationsVm = jvmInternalsService != null ? new CompilationsViewModel(jvmInternalsService) : null;
        CodeCacheViewModel codeCache = jvmInternalsService != null ? new CodeCacheViewModel(jvmInternalsService) : null;
        ClassLoadingViewModel classLoading = jvmInternalsService != null ? new ClassLoadingViewModel(jvmInternalsService) : null;
        VmOperationsViewModel vmOperations = jvmInternalsService != null ? new VmOperationsViewModel(jvmInternalsService) : null;
        EnvironmentViewModel environment = environmentService != null
                ? new EnvironmentViewModel(environmentService) : null;
        viewModel.openRecording(recording, overview, events, analysis, profiling, exceptions, threads,
                fileio, socketio, locks, heap, leakSuspects, tlab,
                jvmInfo, gcConfig, gcSummary, gcDetails, compilationsVm, codeCache, classLoading, vmOperations,
                environment);
        overview.showRecording(recording, i18n.format("overview.details.format",
                recording.path(),
                formatEventTime(recording.startTime()),
                formatEventTime(recording.endTime()),
                DisplayFormats.formatDuration(recording.durationMillis()),
                DisplayFormats.formatFileSize(recording.sizeBytes())));
        events.loadRecording(recording);
        analysis.analyze(recording);
        if (profiling != null) {
            profiling.load(recording);
        }
        if (exceptions != null) {
            exceptions.load(recording);
        }
        if (threads != null) {
            threads.load(recording);
        }
        if (fileio != null) {
            fileio.load(recording);
        }
        if (socketio != null) {
            socketio.load(recording);
        }
        if (locks != null) {
            locks.load(recording);
        }
        if (heap != null) {
            heap.load(recording);
        }
        if (leakSuspects != null) {
            leakSuspects.load(recording);
        }
        if (tlab != null) {
            tlab.load(recording);
        }
        if (jvmInfo != null) {
            jvmInfo.load(recording);
        }
        if (gcConfig != null) {
            gcConfig.load(recording);
        }
        if (gcSummary != null) {
            gcSummary.load(recording);
        }
        if (gcDetails != null) {
            gcDetails.load(recording);
        }
        if (compilationsVm != null) {
            compilationsVm.load(recording);
        }
        if (codeCache != null) {
            codeCache.load(recording);
        }
        if (classLoading != null) {
            classLoading.load(recording);
        }
        if (vmOperations != null) {
            vmOperations.load(recording);
        }
        if (environment != null) {
            environment.load(recording);
        }
    }

    private void rebuildEventTypeTree() {
        if (eventBrowserViewModel == null) {
            eventTypesTree.setRoot(new TreeItem<>());
            return;
        }
        TreeItem<EventTypeNode> rootItem = new TreeItem<>();
        eventBrowserViewModel.eventTypeTreeProperty().stream()
                .map(this::toTreeItem)
                .forEach(rootItem.getChildren()::add);
        rootItem.setExpanded(true);
        eventTypesTree.setRoot(rootItem);
        selectTreeItem(rootItem, eventBrowserViewModel.selectedEventTypeIdProperty().get());
    }

    private TreeItem<EventTypeNode> toTreeItem(EventTypeNode node) {
        TreeItem<EventTypeNode> item = new TreeItem<>(node);
        node.children().stream()
                .map(this::toTreeItem)
                .forEach(item.getChildren()::add);
        item.setExpanded(true);
        return item;
    }

    private void selectTreeItem(TreeItem<EventTypeNode> item, String eventTypeId) {
        if (shouldClearEventTypesTreeSelection(eventTypeId)) {
            eventTypesTree.getSelectionModel().clearSelection();
            return;
        }
        for (TreeItem<EventTypeNode> child : item.getChildren()) {
            EventTypeNode node = child.getValue();
            if (node != null && eventTypeId.equals(node.eventTypeId())) {
                eventTypesTree.getSelectionModel().select(child);
                return;
            }
            selectTreeItem(child, eventTypeId);
        }
    }

    private void selectEventType(TreeItem<EventTypeNode> item) {
        if (eventBrowserViewModel == null) {
            return;
        }
        eventBrowserViewModel.selectEventTypeNode(item == null ? null : item.getValue());
    }

    private String eventTypeText(EventTypeNode node) {
        if (node.kind() != EventTypeNodeKind.EVENT_TYPE) {
            return node.label();
        }
        return node.label() + " (" + node.count() + ")";
    }

    private void rebuildEventColumns() {
        if (eventBrowserViewModel == null) {
            eventsTable.getColumns().clear();
            columnsButton.getItems().clear();
            return;
        }
        eventsTable.getColumns().setAll(eventBrowserViewModel.columnsProperty().stream()
                .map(this::toTableColumn)
                .toList());
        rebuildColumnsMenu();
    }

    private void rebuildColumnsMenu() {
        if (eventBrowserViewModel == null) {
            columnsButton.getItems().clear();
            return;
        }
        columnsButton.getItems().setAll(eventBrowserViewModel.fieldDescriptorsProperty().stream()
                .map(field -> {
                    javafx.scene.control.CheckMenuItem item = new javafx.scene.control.CheckMenuItem(field.label());
                    item.setSelected(eventBrowserViewModel.columnsProperty().stream()
                            .anyMatch(column -> field.id().equals(column.fieldId())));
                    item.setOnAction(event -> {
                        if (item.isSelected()) {
                            eventBrowserViewModel.addFieldColumn(field.id());
                        } else {
                            eventBrowserViewModel.removeColumn("field:" + field.id());
                        }
                    });
                    return item;
                })
                .toList());
    }

    private TableColumn<EventRow, String> toTableColumn(EventColumn column) {
        TableColumn<EventRow, String> tableColumn = new TableColumn<>(column.label());
        tableColumn.setPrefWidth(column.width());
        tableColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(columnValue(column, cell.getValue())));
        return tableColumn;
    }

    private String columnValue(EventColumn column, EventRow row) {
        return switch (column.kind()) {
            case COMMON -> commonColumnValue(column.id(), row);
            case FIELD -> row.fieldValues().getOrDefault(column.fieldId(), "");
        };
    }

    private String commonColumnValue(String columnId, EventRow row) {
        return switch (columnId) {
            case "eventType" -> row.eventTypeId();
            case "startTime" -> formatEventTime(row.startTime());
            case "duration" -> row.durationText();
            case "eventThread" -> row.threadName();
            default -> "";
        };
    }

    private String formatEventTime(java.time.Instant instant) {
        return formatEventTimeForDisplay(instant, ZoneId.systemDefault());
    }

    static String formatEventTimeForDisplay(java.time.Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return "";
        }
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                .withZone(zoneId)
                .format(instant);
    }

    private void selectFirstEventRow() {
        if (eventsTable.getItems().isEmpty()) {
            if (eventBrowserViewModel != null) {
                eventBrowserViewModel.selectedDetailsProperty().set(null);
            }
            return;
        }
        eventsTable.getSelectionModel().selectFirst();
    }

    private void selectEventRow(EventRow row) {
        if (eventBrowserViewModel == null) {
            return;
        }
        EventDetails details = eventBrowserViewModel.selectedDetailsProperty().get();
        if (row == null || details == null || !row.id().equals(details.eventId())) {
            eventBrowserViewModel.selectRow(row);
        }
        eventDetailsTabs.setDisable(row == null && eventBrowserViewModel.selectedDetailsProperty().get() == null);
    }

    private void clearEventFilters() {
        eventSearchField.clear();
        threadFilterField.clear();
        fieldFilterField.clear();
        refreshVisibleRange();
    }

    private void refreshVisibleRange() {
        if (eventBrowserViewModel == null) {
            return;
        }
        eventBrowserViewModel.setFilter(new EventFilter(eventSearchField.getText(), threadFilterField.getText(),
                null, null, fieldConditions(fieldFilterField.getText())));
    }

    private List<EventFieldCondition> fieldConditions(String expression) {
        if (expression == null || expression.isBlank()) {
            return List.of();
        }
        String[] parts = expression.trim().split("\\s+", 3);
        if (parts.length < 3) {
            return List.of();
        }
        EventFilterOperator operator = switch (parts[1]) {
            case "contains" -> EventFilterOperator.CONTAINS;
            case "=", "==" -> EventFilterOperator.EQUALS;
            case "!=", "<>" -> EventFilterOperator.NOT_EQUALS;
            case ">" -> EventFilterOperator.GREATER_THAN;
            case ">=" -> EventFilterOperator.GREATER_THAN_OR_EQUAL;
            case "<" -> EventFilterOperator.LESS_THAN;
            case "<=" -> EventFilterOperator.LESS_THAN_OR_EQUAL;
            default -> null;
        };
        return operator == null ? List.of() : List.of(new EventFieldCondition(parts[0], operator, parts[2]));
    }

    private void configureEventPropertiesTable() {
        eventPropertiesTable.setPlaceholder(emptyTablePlaceholder());
        TableColumn<EventProperty, String> nameColumn = new TableColumn<>();
        nameColumn.textProperty().bind(i18n.text("events.properties.field"));
        nameColumn.setPrefWidth(220);
        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().label()));
        TableColumn<EventProperty, String> valueColumn = new TableColumn<>();
        valueColumn.textProperty().bind(i18n.text("events.properties.value"));
        valueColumn.setPrefWidth(420);
        valueColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().value()));
        eventPropertiesTable.getColumns().setAll(List.of(nameColumn, valueColumn));
    }

    private void showEventDetails(EventDetails details) {
        if (details == null) {
            eventTimingLabel.setText(noTimingSelectionText(i18n));
            eventThreadLabel.setText(noThreadSelectionText(i18n));
            eventStackTraceList.setItems(FXCollections.emptyObservableList());
            return;
        }
        eventTimingLabel.setText(timingText(details.timing()));
        eventThreadLabel.setText(threadText(details.thread()));
        eventStackTraceList.setItems(FXCollections.observableArrayList(details.stackTrace().stream()
                .map(this::stackFrameText)
                .toList()));
    }

    private void showSelectionProperties(EventSelectionProperties properties) {
        if (properties == null) {
            eventPropertiesTable.setItems(FXCollections.emptyObservableList());
            return;
        }
        eventPropertiesTable.setItems(FXCollections.observableArrayList(properties.properties()));
    }

    private String timingText(EventTiming timing) {
        if (timing == null) {
            return i18n.get("events.details.noTiming");
        }
        return new StringJoiner("\n")
                .add(i18n.format("events.details.start", formatEventTime(timing.startTime())))
                .add(i18n.format("events.details.end", formatEventTime(timing.endTime())))
                .add(i18n.format("events.details.duration", timing.durationText()))
                .add(i18n.format("events.details.recordingOffset", timing.recordingOffsetText()))
                .toString();
    }

    private String threadText(EventThreadInfo thread) {
        if (thread == null) {
            return i18n.get("events.details.noThread");
        }
        return new StringJoiner("\n")
                .add(i18n.format("events.details.threadName", thread.name()))
                .add(i18n.format("events.details.threadId", thread.id()))
                .add(i18n.format("events.details.threadVirtual", thread.virtual()))
                .toString();
    }

    private String stackFrameText(EventStackFrame frame) {
        String location = frame.fileName() == null || frame.fileName().isBlank()
                ? ""
                : " (" + frame.fileName() + ":" + frame.lineNumber() + ")";
        return frame.typeName() + "." + frame.methodName() + location;
    }

    // --- JVM Internals: configure methods ---

    private void configureJvmFlagsTable() {
        TableColumn<JvmFlag, String> nameCol = new TableColumn<>("Flag");
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().name()));
        TableColumn<JvmFlag, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().value()));
        TableColumn<JvmFlag, String> originCol = new TableColumn<>("Origin");
        originCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().origin()));
        jvmFlagsTable.getColumns().setAll(List.of(nameCol, valueCol, originCol));
    }

    private void configureJvmFlagChangesTable() {
        TableColumn<JvmFlagChange, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().startTime() != null ? data.getValue().startTime().toString() : ""));
        TableColumn<JvmFlagChange, String> flagCol = new TableColumn<>("Flag");
        flagCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().flagName()));
        TableColumn<JvmFlagChange, String> oldCol = new TableColumn<>("Old Value");
        oldCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().oldValue()));
        TableColumn<JvmFlagChange, String> newCol = new TableColumn<>("New Value");
        newCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().newValue()));
        TableColumn<JvmFlagChange, String> originCol = new TableColumn<>("Origin");
        originCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().origin()));
        jvmFlagChangesTable.getColumns().setAll(List.of(timeCol, flagCol, oldCol, newCol, originCol));
    }

    private void configureGcSummaryTable() {
        TableColumn<GcSummary, String> genCol = new TableColumn<>("Generation");
        genCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().generation()));
        TableColumn<GcSummary, String> countCol = new TableColumn<>("Count");
        countCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().collectionCount())));
        TableColumn<GcSummary, String> totalCol = new TableColumn<>("Total (ms)");
        totalCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().totalDurationMillis())));
        TableColumn<GcSummary, String> avgCol = new TableColumn<>("Avg (ms)");
        avgCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.format("%.2f", data.getValue().avgDurationMillis())));
        TableColumn<GcSummary, String> maxCol = new TableColumn<>("Max (ms)");
        maxCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().maxDurationMillis())));
        gcSummaryTable.getColumns().setAll(List.of(genCol, countCol, totalCol, avgCol, maxCol));
    }

    private void configureGcEventsTable() {
        TableColumn<GcEvent, String> idCol = new TableColumn<>("GC ID");
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().gcId())));
        TableColumn<GcEvent, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().name()));
        TableColumn<GcEvent, String> causeCol = new TableColumn<>("Cause");
        causeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().cause()));
        TableColumn<GcEvent, String> pauseCol = new TableColumn<>("Longest Pause");
        pauseCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().longestPauseMicros())));
        TableColumn<GcEvent, String> totalPauseCol = new TableColumn<>("Total Pause");
        totalPauseCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().totalPauseMicros())));
        TableColumn<GcEvent, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().startTime() != null ? data.getValue().startTime().toString() : ""));
        gcEventsTable.getColumns().setAll(List.of(idCol, nameCol, causeCol, pauseCol, totalPauseCol, timeCol));
    }

    private void configureGcReferenceStatsTable() {
        TableColumn<GcReferenceStat, String> idCol = new TableColumn<>("GC ID");
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().gcId())));
        TableColumn<GcReferenceStat, String> typeCol = new TableColumn<>("Reference Type");
        typeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().referenceType()));
        TableColumn<GcReferenceStat, String> countCol = new TableColumn<>("Count");
        countCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().count())));
        gcReferenceStatsTable.getColumns().setAll(List.of(idCol, typeCol, countCol));
    }

    private void configureGcHeapSummaryTable() {
        TableColumn<GcHeapSummary, String> idCol = new TableColumn<>("GC ID");
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().gcId())));
        TableColumn<GcHeapSummary, String> whenCol = new TableColumn<>("When");
        whenCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().when()));
        TableColumn<GcHeapSummary, String> usedCol = new TableColumn<>("Heap Used");
        usedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().heapUsed())));
        TableColumn<GcHeapSummary, String> committedCol = new TableColumn<>("Heap Committed");
        committedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().heapCommitted())));
        TableColumn<GcHeapSummary, String> metaUsedCol = new TableColumn<>("Metaspace Used");
        metaUsedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().metaspaceUsed())));
        TableColumn<GcHeapSummary, String> metaCommittedCol = new TableColumn<>("Metaspace Committed");
        metaCommittedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().metaspaceCommitted())));
        gcHeapSummaryTable.getColumns().setAll(List.of(idCol, whenCol, usedCol, committedCol, metaUsedCol, metaCommittedCol));
    }

    private void configureCompilationsTable() {
        TableColumn<CompilationEvent, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().compilationId())));
        TableColumn<CompilationEvent, String> methodCol = new TableColumn<>("Method");
        methodCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().method()));
        TableColumn<CompilationEvent, String> okCol = new TableColumn<>("Succeeded");
        okCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().succeeded())));
        TableColumn<CompilationEvent, String> durCol = new TableColumn<>("Duration");
        durCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().durationMicros())));
        TableColumn<CompilationEvent, String> sizeCol = new TableColumn<>("Code Size");
        sizeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().codeSize())));
        TableColumn<CompilationEvent, String> inlineCol = new TableColumn<>("Inlined");
        inlineCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().inlinedBytes())));
        TableColumn<CompilationEvent, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().startTime() != null ? data.getValue().startTime().toString() : ""));
        compilationsTable.getColumns().setAll(List.of(idCol, methodCol, okCol, durCol, sizeCol, inlineCol, timeCol));
    }

    private void configureCompilationFailuresTable() {
        TableColumn<CompilationEvent, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().compilationId())));
        TableColumn<CompilationEvent, String> methodCol = new TableColumn<>("Method");
        methodCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().method()));
        TableColumn<CompilationEvent, String> durCol = new TableColumn<>("Duration");
        durCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().durationMicros())));
        TableColumn<CompilationEvent, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().startTime() != null ? data.getValue().startTime().toString() : ""));
        compilationFailuresTable.getColumns().setAll(List.of(idCol, methodCol, durCol, timeCol));
    }

    private void configureCodeCacheSweepsTable() {
        TableColumn<CodeCacheSweep, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().startTime() != null ? data.getValue().startTime().toString() : ""));
        TableColumn<CodeCacheSweep, String> idxCol = new TableColumn<>("Index");
        idxCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().sweepIndex())));
        TableColumn<CodeCacheSweep, String> durCol = new TableColumn<>("Duration");
        durCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().durationMicros())));
        TableColumn<CodeCacheSweep, String> flushedCol = new TableColumn<>("Flushed");
        flushedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().flushed())));
        TableColumn<CodeCacheSweep, String> sweptCol = new TableColumn<>("Swept");
        sweptCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().swept())));
        TableColumn<CodeCacheSweep, String> countCol = new TableColumn<>("Swept Count");
        countCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().sweptCount())));
        codeCacheSweepsTable.getColumns().setAll(List.of(timeCol, idxCol, durCol, flushedCol, sweptCol, countCol));
    }

    private void configureCodeCacheStatsTable() {
        TableColumn<CodeCacheStats, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().startTime() != null ? data.getValue().startTime().toString() : ""));
        TableColumn<CodeCacheStats, String> heapCol = new TableColumn<>("Code Heap");
        heapCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().codeHeap()));
        TableColumn<CodeCacheStats, String> entriesCol = new TableColumn<>("Entries");
        entriesCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().entries())));
        TableColumn<CodeCacheStats, String> methodsCol = new TableColumn<>("Methods");
        methodsCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().methods())));
        TableColumn<CodeCacheStats, String> adaptersCol = new TableColumn<>("Adapters");
        adaptersCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().adapters())));
        TableColumn<CodeCacheStats, String> unallocCol = new TableColumn<>("Unallocated");
        unallocCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().unallocated())));
        codeCacheStatsTable.getColumns().setAll(List.of(timeCol, heapCol, entriesCol, methodsCol, adaptersCol, unallocCol));
    }

    private void configureClassLoadingHistogramTable() {
        TableColumn<ClassloaderSummary, String> loaderCol = new TableColumn<>("Classloader");
        loaderCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().classloader()));
        TableColumn<ClassloaderSummary, String> loadedCol = new TableColumn<>("Loaded");
        loadedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().loadedCount())));
        TableColumn<ClassloaderSummary, String> unloadedCol = new TableColumn<>("Unloaded");
        unloadedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().unloadedCount())));
        classLoadingHistogramTable.getColumns().setAll(List.of(loaderCol, loadedCol, unloadedCol));
    }

    private void configureClassLoadingEventsTable() {
        TableColumn<ClassloadEvent, String> typeCol = new TableColumn<>("Event");
        typeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().eventType()));
        TableColumn<ClassloadEvent, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().startTime() != null ? data.getValue().startTime().toString() : ""));
        TableColumn<ClassloadEvent, String> classCol = new TableColumn<>("Class");
        classCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().loadedClass()));
        TableColumn<ClassloadEvent, String> defLoaderCol = new TableColumn<>("Defining Loader");
        defLoaderCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().definingClassloader()));
        TableColumn<ClassloadEvent, String> initLoaderCol = new TableColumn<>("Initiating Loader");
        initLoaderCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().initiatingClassloader()));
        TableColumn<ClassloadEvent, String> durCol = new TableColumn<>("Duration");
        durCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().durationMicros())));
        classLoadingEventsTable.getColumns().setAll(List.of(typeCol, timeCol, classCol, defLoaderCol, initLoaderCol, durCol));
    }

    private void configureClassLoadingStatsTable() {
        TableColumn<ClassloaderStatistics, String> loaderCol = new TableColumn<>("Classloader");
        loaderCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().classloader()));
        TableColumn<ClassloaderStatistics, String> parentCol = new TableColumn<>("Parent");
        parentCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().parentClassloader()));
        TableColumn<ClassloaderStatistics, String> countCol = new TableColumn<>("Loaded Classes");
        countCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().loadedClassCount())));
        TableColumn<ClassloaderStatistics, String> chunkCol = new TableColumn<>("Chunk Size");
        chunkCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().anonymousBlockChunkSize())));
        TableColumn<ClassloaderStatistics, String> blockCol = new TableColumn<>("Block Size");
        blockCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().anonymousBlockSize())));
        TableColumn<ClassloaderStatistics, String> anonCol = new TableColumn<>("Anonymous Classes");
        anonCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().anonymousClassCount())));
        classLoadingStatsTable.getColumns().setAll(List.of(loaderCol, parentCol, countCol, chunkCol, blockCol, anonCol));
    }

    private void configureVmOperationSummaryTable() {
        TableColumn<VmOperationSummary, String> opCol = new TableColumn<>("Operation");
        opCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().operation()));
        TableColumn<VmOperationSummary, String> countCol = new TableColumn<>("Count");
        countCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().count())));
        TableColumn<VmOperationSummary, String> totalCol = new TableColumn<>("Total Duration");
        totalCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().totalDurationMicros())));
        TableColumn<VmOperationSummary, String> maxCol = new TableColumn<>("Max Duration");
        maxCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().maxDurationMicros())));
        vmOperationSummaryTable.getColumns().setAll(List.of(opCol, countCol, totalCol, maxCol));
    }

    private void configureVmOperationEventsTable() {
        TableColumn<VmOperationEvent, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().startTime() != null ? data.getValue().startTime().toString() : ""));
        TableColumn<VmOperationEvent, String> opCol = new TableColumn<>("Operation");
        opCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().operation()));
        TableColumn<VmOperationEvent, String> blockCol = new TableColumn<>("Blocking");
        blockCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().blocking())));
        TableColumn<VmOperationEvent, String> safeCol = new TableColumn<>("Safepoint");
        safeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().safepoint())));
        TableColumn<VmOperationEvent, String> durCol = new TableColumn<>("Duration");
        durCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().durationMicros())));
        TableColumn<VmOperationEvent, String> threadCol = new TableColumn<>("Thread");
        threadCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().threadName()));
        vmOperationEventsTable.getColumns().setAll(List.of(timeCol, opCol, blockCol, safeCol, durCol, threadCol));
    }

    // --- JVM Internals: bind methods ---

    private JvmInfoViewModel jvmInfoViewModel;
    private GcConfigViewModel gcConfigViewModel;
    private GcSummaryViewModel gcSummaryViewModel;
    private GcDetailsViewModel gcDetailsViewModel;
    private CompilationsViewModel compilationsViewModel;
    private CodeCacheViewModel codeCacheViewModel;
    private ClassLoadingViewModel classLoadingViewModel;
    private VmOperationsViewModel vmOperationsViewModel;

    private void bindJvmInfo(JvmInfoViewModel nextViewModel) {
        jvmFlagsTable.setItems(FXCollections.emptyObservableList());
        jvmFlagChangesTable.setItems(FXCollections.emptyObservableList());
        jvmInfoViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        jvmFlagsTable.setItems(nextViewModel.flags());
        jvmFlagChangesTable.setItems(nextViewModel.flagChanges());
    }

    private void bindGcConfig(GcConfigViewModel nextViewModel) {
        gcConfigViewModel = nextViewModel;
    }

    private void bindGcSummary(GcSummaryViewModel nextViewModel) {
        gcSummaryTable.setItems(FXCollections.emptyObservableList());
        gcSummaryViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        gcSummaryTable.setItems(nextViewModel.summaries());
    }

    private void bindGcDetails(GcDetailsViewModel nextViewModel) {
        gcEventsTable.setItems(FXCollections.emptyObservableList());
        gcReferenceStatsTable.setItems(FXCollections.emptyObservableList());
        gcHeapSummaryTable.setItems(FXCollections.emptyObservableList());
        gcDetailsViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        gcEventsTable.setItems(nextViewModel.gcEvents());
        gcReferenceStatsTable.setItems(nextViewModel.referenceStats());
        gcHeapSummaryTable.setItems(nextViewModel.heapSummaries());
    }

    private void bindCompilations(CompilationsViewModel nextViewModel) {
        compilationsTable.setItems(FXCollections.emptyObservableList());
        compilationFailuresTable.setItems(FXCollections.emptyObservableList());
        compilationsViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        compilationsTable.setItems(nextViewModel.compilations());
        compilationFailuresTable.setItems(nextViewModel.failures());
    }

    private void bindCodeCache(CodeCacheViewModel nextViewModel) {
        codeCacheSweepsTable.setItems(FXCollections.emptyObservableList());
        codeCacheStatsTable.setItems(FXCollections.emptyObservableList());
        codeCacheViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        codeCacheSweepsTable.setItems(nextViewModel.sweeps());
        codeCacheStatsTable.setItems(nextViewModel.statistics());
    }

    private void bindClassLoading(ClassLoadingViewModel nextViewModel) {
        classLoadingHistogramTable.setItems(FXCollections.emptyObservableList());
        classLoadingEventsTable.setItems(FXCollections.emptyObservableList());
        classLoadingStatsTable.setItems(FXCollections.emptyObservableList());
        classLoadingViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        classLoadingHistogramTable.setItems(nextViewModel.histogram());
        classLoadingEventsTable.setItems(nextViewModel.events());
        classLoadingStatsTable.setItems(nextViewModel.statistics());
    }

    private void bindVmOperations(VmOperationsViewModel nextViewModel) {
        vmOperationSummaryTable.setItems(FXCollections.emptyObservableList());
        vmOperationEventsTable.setItems(FXCollections.emptyObservableList());
        vmOperationsViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        vmOperationSummaryTable.setItems(nextViewModel.summary());
        vmOperationEventsTable.setItems(nextViewModel.events());
    }

    // --- Environment: configure methods ---

    private void configureProcessesTable() {
        processesTable.setPlaceholder(new Label(i18n.get("processes.empty")));
        TableColumn<ProcessInfo, String> pidCol = new TableColumn<>();
        pidCol.textProperty().bind(i18n.text("processes.column.pid"));
        pidCol.setPrefWidth(80);
        pidCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().pid()));
        TableColumn<ProcessInfo, String> cmdCol = new TableColumn<>();
        cmdCol.textProperty().bind(i18n.text("processes.column.commandLine"));
        cmdCol.setPrefWidth(600);
        cmdCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().commandLine()));
        TableColumn<ProcessInfo, String> firstCol = new TableColumn<>();
        firstCol.textProperty().bind(i18n.text("processes.column.firstSample"));
        firstCol.setPrefWidth(180);
        firstCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().startTime()));
        TableColumn<ProcessInfo, String> lastCol = new TableColumn<>();
        lastCol.textProperty().bind(i18n.text("processes.column.lastSample"));
        lastCol.setPrefWidth(180);
        lastCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().lastSample()));
        processesTable.getColumns().setAll(List.of(pidCol, cmdCol, firstCol, lastCol));
    }

    private void configureEnvVarsTable() {
        envVarsTable.setPlaceholder(new Label(i18n.get("envVars.empty")));
        TableColumn<EnvironmentVariable, String> keyCol = new TableColumn<>();
        keyCol.textProperty().bind(i18n.text("envVars.column.key"));
        keyCol.setPrefWidth(300);
        keyCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().key()));
        TableColumn<EnvironmentVariable, String> valCol = new TableColumn<>();
        valCol.textProperty().bind(i18n.text("envVars.column.value"));
        valCol.setPrefWidth(700);
        valCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().value()));
        envVarsTable.getColumns().setAll(List.of(keyCol, valCol));
        envVarsSearchField.textProperty().addListener((obs, old, val) -> {
            if (environmentViewModel != null) {
                environmentViewModel.setEnvironmentSearchFilter(val);
            }
        });
    }

    private void configureSysPropsTable() {
        sysPropsTable.setPlaceholder(new Label(i18n.get("sysProps.empty")));
        TableColumn<SystemProperty, String> keyCol = new TableColumn<>();
        keyCol.textProperty().bind(i18n.text("sysProps.column.key"));
        keyCol.setPrefWidth(350);
        keyCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().key()));
        TableColumn<SystemProperty, String> valCol = new TableColumn<>();
        valCol.textProperty().bind(i18n.text("sysProps.column.value"));
        valCol.setPrefWidth(650);
        valCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().value()));
        sysPropsTable.getColumns().setAll(List.of(keyCol, valCol));
        sysPropsSearchField.textProperty().addListener((obs, old, val) -> {
            if (environmentViewModel != null) {
                environmentViewModel.setSystemPropertySearchFilter(val);
            }
        });
    }

    private void configureRecordingsTable() {
        recordingsTable.setPlaceholder(new Label(i18n.get("recordingInfo.empty")));
        TableColumn<ActiveRecordingInfo, String> idCol = new TableColumn<>();
        idCol.textProperty().bind(i18n.text("recordingInfo.column.id"));
        idCol.setPrefWidth(50);
        idCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().id()));
        TableColumn<ActiveRecordingInfo, String> nameCol = new TableColumn<>();
        nameCol.textProperty().bind(i18n.text("recordingInfo.column.name"));
        nameCol.setPrefWidth(200);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
        TableColumn<ActiveRecordingInfo, String> destCol = new TableColumn<>();
        destCol.textProperty().bind(i18n.text("recordingInfo.column.destination"));
        destCol.setPrefWidth(200);
        destCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().destination()));
        TableColumn<ActiveRecordingInfo, String> startCol = new TableColumn<>();
        startCol.textProperty().bind(i18n.text("recordingInfo.column.startTime"));
        startCol.setPrefWidth(180);
        startCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().startTime()));
        TableColumn<ActiveRecordingInfo, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("recordingInfo.column.eventCount"));
        countCol.setPrefWidth(80);
        countCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().eventCount()));
        recordingsTable.getColumns().setAll(List.of(idCol, nameCol, destCol, startCol, countCol));
    }

    private void configureSettingsTable() {
        settingsTable.setPlaceholder(new Label(i18n.get("recordingInfo.settings.empty")));
        TableColumn<ActiveSetting, String> eventCol = new TableColumn<>();
        eventCol.textProperty().bind(i18n.text("recordingInfo.settings.column.eventId"));
        eventCol.setPrefWidth(300);
        eventCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().eventId()));
        TableColumn<ActiveSetting, String> nameCol = new TableColumn<>();
        nameCol.textProperty().bind(i18n.text("recordingInfo.settings.column.name"));
        nameCol.setPrefWidth(200);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().settingName()));
        TableColumn<ActiveSetting, String> valCol = new TableColumn<>();
        valCol.textProperty().bind(i18n.text("recordingInfo.settings.column.value"));
        valCol.setPrefWidth(300);
        valCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().settingValue()));
        settingsTable.getColumns().setAll(List.of(eventCol, nameCol, valCol));
    }

    private void configureAgentsTable() {
        agentsTable.setPlaceholder(new Label(i18n.get("agents.empty")));
        TableColumn<AgentInfo, String> nameCol = new TableColumn<>();
        nameCol.textProperty().bind(i18n.text("agents.column.name"));
        nameCol.setPrefWidth(300);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
        TableColumn<AgentInfo, String> optCol = new TableColumn<>();
        optCol.textProperty().bind(i18n.text("agents.column.options"));
        optCol.setPrefWidth(300);
        optCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().options()));
        TableColumn<AgentInfo, String> initCol = new TableColumn<>();
        initCol.textProperty().bind(i18n.text("agents.column.initTime"));
        initCol.setPrefWidth(180);
        initCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().initTime()));
        TableColumn<AgentInfo, String> dynCol = new TableColumn<>();
        dynCol.textProperty().bind(i18n.text("agents.column.dynamic"));
        dynCol.setPrefWidth(70);
        dynCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(String.valueOf(cell.getValue().dynamic())));
        TableColumn<AgentInfo, String> kindCol = new TableColumn<>();
        kindCol.textProperty().bind(i18n.text("agents.column.kind"));
        kindCol.setPrefWidth(80);
        kindCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().kind()));
        agentsTable.getColumns().setAll(List.of(nameCol, optCol, initCol, dynCol, kindCol));
    }

    private void configureConstantPoolsTable() {
        constantPoolsTable.setPlaceholder(new Label(i18n.get("constantPools.empty")));
        TableColumn<ConstantPoolType, String> nameCol = new TableColumn<>();
        nameCol.textProperty().bind(i18n.text("constantPools.column.typeName"));
        nameCol.setPrefWidth(500);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().typeName()));
        TableColumn<ConstantPoolType, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("constantPools.column.entryCount"));
        countCol.setPrefWidth(100);
        countCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().entryCount()));
        constantPoolsTable.getColumns().setAll(List.of(nameCol, countCol));
    }

    // --- Environment: bind method ---

    private void bindEnvironment(EnvironmentViewModel nextViewModel) {
        processesTable.setItems(FXCollections.emptyObservableList());
        envVarsTable.setItems(FXCollections.emptyObservableList());
        sysPropsTable.setItems(FXCollections.emptyObservableList());
        recordingsTable.setItems(FXCollections.emptyObservableList());
        settingsTable.setItems(FXCollections.emptyObservableList());
        agentsTable.setItems(FXCollections.emptyObservableList());
        constantPoolsTable.setItems(FXCollections.emptyObservableList());
        environmentViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        processesTable.setItems(nextViewModel.processesProperty());
        envVarsTable.setItems(nextViewModel.filteredEnvironmentVariablesProperty());
        sysPropsTable.setItems(nextViewModel.filteredSystemPropertiesProperty());
        recordingsTable.setItems(nextViewModel.activeRecordingsProperty());
        settingsTable.setItems(nextViewModel.activeSettingsProperty());
        agentsTable.setItems(nextViewModel.agentsProperty());
        constantPoolsTable.setItems(nextViewModel.constantPoolsProperty());
    }
}
