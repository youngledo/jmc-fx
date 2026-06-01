package com.youngledo.jmcfx.ui.shell;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.nio.file.Path;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
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
import com.youngledo.jmcfx.domain.model.DependencyGraphEdge;
import com.youngledo.jmcfx.domain.model.DiagnosticCommandInfo;
import com.youngledo.jmcfx.domain.model.EnvironmentVariable;
import com.youngledo.jmcfx.domain.model.EventColumn;
import com.youngledo.jmcfx.domain.model.EventHeatmap;
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
import com.youngledo.jmcfx.domain.model.FlightRecordingInfo;
import com.youngledo.jmcfx.domain.model.G1GcRegionState;
import com.youngledo.jmcfx.domain.model.G1GcRegionSummary;
import com.youngledo.jmcfx.domain.model.GcEvent;
import com.youngledo.jmcfx.domain.model.GcHeapSummary;
import com.youngledo.jmcfx.domain.model.GcReferenceStat;
import com.youngledo.jmcfx.domain.model.GcSummary;
import com.youngledo.jmcfx.domain.model.HeapDumpAnalysisState;
import com.youngledo.jmcfx.domain.model.HeapDumpIssue;
import com.youngledo.jmcfx.domain.model.HeapClassHistogram;
import com.youngledo.jmcfx.domain.model.JavaFxInputEvent;
import com.youngledo.jmcfx.domain.model.JavaFxPulsePhase;
import com.youngledo.jmcfx.domain.model.JavaFxPulseSummary;
import com.youngledo.jmcfx.domain.model.ProcessInfo;
import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.JvmCapabilitySnapshot;
import com.youngledo.jmcfx.domain.model.JmcAgentPreset;
import com.youngledo.jmcfx.domain.model.JmcAgentTransform;
import com.youngledo.jmcfx.domain.model.JfrMetadataEventType;
import com.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import com.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import com.youngledo.jmcfx.domain.model.JmxSubscriptionSample;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.JvmConnectionSource;
import com.youngledo.jmcfx.domain.model.JvmConnectionState;
import com.youngledo.jmcfx.domain.model.JvmFlag;
import com.youngledo.jmcfx.domain.model.JvmFlagChange;
import com.youngledo.jmcfx.domain.model.JvmSessionSnapshot;
import com.youngledo.jmcfx.domain.model.LeakCandidate;
import com.youngledo.jmcfx.domain.model.LeakReferenceNode;
import com.youngledo.jmcfx.domain.model.LiveMetricDefinition;
import com.youngledo.jmcfx.domain.model.LiveMetricKind;
import com.youngledo.jmcfx.domain.model.LockGrouping;
import com.youngledo.jmcfx.domain.model.LockHistogram;
import com.youngledo.jmcfx.domain.model.MBeanAttributeInfo;
import com.youngledo.jmcfx.domain.model.MBeanNode;
import com.youngledo.jmcfx.domain.model.MBeanOperationInfo;
import com.youngledo.jmcfx.domain.model.MemoryAnalysisReport;
import com.youngledo.jmcfx.domain.model.MemoryIssue;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.RuleResult;
import com.youngledo.jmcfx.domain.model.Severity;
import com.youngledo.jmcfx.domain.model.SocketIOEvent;
import com.youngledo.jmcfx.domain.model.SocketIOGrouping;
import com.youngledo.jmcfx.domain.model.SocketIOHistogram;
import com.youngledo.jmcfx.domain.model.StackTreeNode;
import com.youngledo.jmcfx.domain.model.SystemProperty;
import com.youngledo.jmcfx.domain.model.ThreadDumpEntry;
import com.youngledo.jmcfx.domain.model.ThreadHistogramRow;
import com.youngledo.jmcfx.domain.model.ThreadSummary;
import com.youngledo.jmcfx.domain.model.TriggerActionType;
import com.youngledo.jmcfx.domain.model.TriggerEvent;
import com.youngledo.jmcfx.domain.model.TriggerOperator;
import com.youngledo.jmcfx.domain.model.TriggerRule;
import com.youngledo.jmcfx.domain.model.NativeLibraryEntry;
import com.youngledo.jmcfx.domain.model.TlabAllocation;
import com.youngledo.jmcfx.domain.model.VmOperationEvent;
import com.youngledo.jmcfx.domain.model.VmOperationSummary;
import com.youngledo.jmcfx.domain.model.X509CertificateEntry;
import com.youngledo.jmcfx.domain.service.AdvancedJfrAnalysisService;
import com.youngledo.jmcfx.domain.service.DiagnosticCommandService;
import com.youngledo.jmcfx.domain.service.EnvironmentService;
import com.youngledo.jmcfx.domain.service.EventQueryService;
import com.youngledo.jmcfx.domain.service.ExceptionService;
import com.youngledo.jmcfx.domain.service.FileIOService;
import com.youngledo.jmcfx.domain.service.FlightRecordingService;
import com.youngledo.jmcfx.domain.service.G1GcService;
import com.youngledo.jmcfx.domain.service.HeapDumpAnalysisService;
import com.youngledo.jmcfx.domain.service.HeapService;
import com.youngledo.jmcfx.domain.service.JmcAgentService;
import com.youngledo.jmcfx.domain.service.JfrMetadataService;
import com.youngledo.jmcfx.domain.service.JavaFxEventService;
import com.youngledo.jmcfx.domain.service.JvmInternalsService;
import com.youngledo.jmcfx.domain.service.JavaAppService;
import com.youngledo.jmcfx.domain.service.JdpDiscoveryService;
import com.youngledo.jmcfx.domain.service.JmxConnectionService;
import com.youngledo.jmcfx.domain.service.JmxMonitoringRepository;
import com.youngledo.jmcfx.domain.service.JmxMonitoringService;
import com.youngledo.jmcfx.domain.service.JvmDiscoveryService;
import com.youngledo.jmcfx.domain.service.LeakSuspectsService;
import com.youngledo.jmcfx.domain.service.LiveMetricService;
import com.youngledo.jmcfx.domain.service.LockService;
import com.youngledo.jmcfx.domain.service.MBeanBrowserService;
import com.youngledo.jmcfx.domain.service.ProfilingService;
import com.youngledo.jmcfx.domain.service.RecordingRepository;
import com.youngledo.jmcfx.domain.service.RuleAnalysisService;
import com.youngledo.jmcfx.domain.service.SavedJvmTargetRepository;
import com.youngledo.jmcfx.domain.service.SocketIOService;
import com.youngledo.jmcfx.domain.service.ThreadService;
import com.youngledo.jmcfx.domain.service.TlabService;
import com.youngledo.jmcfx.ui.advanced.AdvancedJfrViewModel;
import com.youngledo.jmcfx.ui.advanced.EventHeatmapView;
import com.youngledo.jmcfx.ui.analysis.AnalysisSeverityCell;
import com.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import com.youngledo.jmcfx.ui.events.VirtualThreadEventBrowserExecutor;
import com.youngledo.jmcfx.ui.environment.EnvironmentViewModel;
import com.youngledo.jmcfx.ui.exceptions.ExceptionViewModel;
import com.youngledo.jmcfx.ui.fileio.FileIOViewModel;
import com.youngledo.jmcfx.ui.gc.G1GcViewModel;
import com.youngledo.jmcfx.ui.heap.HeapViewModel;
import com.youngledo.jmcfx.ui.heapdump.HeapDumpAnalysisViewModel;
import com.youngledo.jmcfx.ui.heapdump.VirtualThreadHeapDumpAnalysisExecutor;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.i18n.LanguageMode;
import com.youngledo.jmcfx.ui.javaapp.JavaAppOverviewViewModel;
import com.youngledo.jmcfx.ui.javaapp.NativeLibraryViewModel;
import com.youngledo.jmcfx.ui.javaapp.SecurityViewModel;
import com.youngledo.jmcfx.ui.javaapp.ThreadDumpViewModel;
import com.youngledo.jmcfx.ui.jfx.JavaFxEventsViewModel;
import com.youngledo.jmcfx.ui.jvm.ClassLoadingViewModel;
import com.youngledo.jmcfx.ui.jvm.CodeCacheViewModel;
import com.youngledo.jmcfx.ui.jvm.CompilationsViewModel;
import com.youngledo.jmcfx.ui.jvm.GcConfigViewModel;
import com.youngledo.jmcfx.ui.jvm.GcDetailsViewModel;
import com.youngledo.jmcfx.ui.jvm.GcSummaryViewModel;
import com.youngledo.jmcfx.ui.jvm.JvmInfoViewModel;
import com.youngledo.jmcfx.ui.jvm.VmOperationsViewModel;
import com.youngledo.jmcfx.ui.jvms.JvmBrowserViewModel;
import com.youngledo.jmcfx.ui.jvms.LiveJvmPersistenceOverview;
import com.youngledo.jmcfx.ui.jvms.LiveJvmOverviewMetric;
import com.youngledo.jmcfx.ui.leaks.LeakSuspectsViewModel;
import com.youngledo.jmcfx.ui.locks.LockViewModel;
import com.youngledo.jmcfx.ui.metadata.JfrMetadataViewModel;
import com.youngledo.jmcfx.ui.chart.TimelineChart;
import com.youngledo.jmcfx.ui.util.CsvExport;
import com.youngledo.jmcfx.ui.util.DisplayFormats;
import com.youngledo.jmcfx.ui.overview.OverviewViewModel;
import com.youngledo.jmcfx.ui.preferences.AppTheme;
import com.youngledo.jmcfx.ui.profiling.CallGraphDirection;
import com.youngledo.jmcfx.ui.profiling.CallGraphLayout;
import com.youngledo.jmcfx.ui.profiling.CallGraphLayoutBuilder;
import com.youngledo.jmcfx.ui.profiling.CallGraphView;
import com.youngledo.jmcfx.ui.profiling.FlameGraphLayout;
import com.youngledo.jmcfx.ui.profiling.FlameGraphView;
import com.youngledo.jmcfx.ui.profiling.ProfilingViewModel;
import com.youngledo.jmcfx.ui.rules.RuleResultDetail;
import com.youngledo.jmcfx.ui.rules.RuleResultsViewModel;
import com.youngledo.jmcfx.ui.socketio.SocketIOViewModel;
import com.youngledo.jmcfx.ui.threads.ThreadViewModel;
import com.youngledo.jmcfx.ui.tlab.TlabViewModel;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/// Controller for the code-first application shell view.
///
/// The controller wires shell actions and bindings while feature behavior stays
/// in view models.
public class AppShellController {

    private static final Logger LOGGER = LogManager.getLogger(AppShellController.class);
    static final int MIN_EVENT_TYPES_WIDTH = 180;
    static final int DEFAULT_EVENT_TYPES_WIDTH = 260;
    static final double MAX_EVENT_TYPES_WIDTH = 360;
    static final double DEFAULT_EVENT_TYPES_DIVIDER_POSITION = 0.25;
    private final AppShellView view;
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
    private final JavaAppService javaAppService;
    private final JvmDiscoveryService jvmDiscoveryService;
    private final JmxConnectionService jmxConnectionService;
    private final FlightRecordingService flightRecordingService;
    private final MBeanBrowserService mBeanBrowserService;
    private final DiagnosticCommandService diagnosticCommandService;
    private final LiveMetricService liveMetricService;
    private final JmcAgentService jmcAgentService;
    private final JmxMonitoringService jmxMonitoringService;
    private final JmxMonitoringRepository jmxMonitoringRepository;
    private final JfrMetadataService jfrMetadataService;
    private final G1GcService g1GcService;
    private final JavaFxEventService javaFxEventService;
    private final AdvancedJfrAnalysisService advancedJfrAnalysisService;
    private final SavedJvmTargetRepository savedTargetRepository;
    private final JdpDiscoveryService jdpDiscoveryService;
    private final HeapDumpAnalysisService heapDumpAnalysisService;
    private final I18n i18n;
    private final RecordingOpenExecutor recordingOpenExecutor;
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
    private JavaAppOverviewViewModel javaAppOverviewViewModel;
    private SecurityViewModel securityViewModel;
    private NativeLibraryViewModel nativeLibraryViewModel;
    private ThreadDumpViewModel threadDumpViewModel;
    private JfrMetadataViewModel jfrMetadataViewModel;
    private AdvancedJfrViewModel advancedJfrViewModel;
    private HeapDumpAnalysisViewModel heapDumpAnalysisViewModel;
    private JvmBrowserViewModel jvmBrowserViewModel;
    private EventHeatmapView advancedJfrHeatmapView;
    private final ChangeListener<EventHeatmap> advancedHeatmapListener =
            (observable, oldValue, newValue) -> advancedJfrHeatmapView.setHeatmap(newValue);
    private final ChangeListener<HeapDumpIssue> heapDumpTableSelectionListener =
            (observable, oldValue, newValue) -> selectHeapDumpIssue(newValue);
    private final ChangeListener<HeapDumpIssue> heapDumpSelectedIssueListener =
            (observable, oldValue, newValue) -> selectHeapDumpIssueInTable(newValue);
    private boolean rebindingAdvancedJfrMemory;
    private boolean eventTypesDividerInitialized;
    private boolean updatingRecordingTabs;
    private boolean recordingOpening;
    private RecordingWorkspace loadedWorkspace;
    private BorderPane root;
    private Button homeOpenRecordingButton;
    private Button homeOpenHeapDumpButton;
    private Button homeConnectJvmButton;
    private AppSidebar sidebar;
    private TabPane recordingTabs;
    private VBox homePane;
    private VBox overviewPane;
    private VBox eventsPane;
    private VBox analysisPane;
    private VBox metadataPane;
    private VBox advancedJfrPane;
    private VBox heapDumpAnalysisPane;
    private VBox jvmsPaneHost;
    private VBox javaApplicationPane;
    private VBox jvmInternalsPane;
    private VBox environmentPane;
    private VBox profilingPane;
    private VBox exceptionsPane;
    private VBox threadsPane;
    private VBox fileioPane;
    private VBox socketioPane;
    private VBox locksPane;
    private VBox threadHistogramPane;
    private VBox securityPane;
    private VBox nativeLibrariesPane;
    private VBox threadDumpsPane;
    private VBox heapPane;
    private VBox leaksPane;
    private VBox tlabPane;
    private VBox jvmInfoPane;
    private VBox gcConfigPane;
    private VBox gcSummaryPane;
    private VBox gcDetailsPane;
    private VBox g1GcPane;
    private VBox javaFxEventsPane;
    private VBox compilationsPane;
    private VBox codeCachePane;
    private VBox classLoadingPane;
    private VBox vmOperationsPane;
    private VBox processesPane;
    private VBox envVarsPane;
    private VBox sysPropsPane;
    private VBox recordingInfoPane;
    private VBox agentsPane;
    private VBox constantPoolsPane;
    private VBox settingsPane;
    private ProgressBar progressBar;
    private Label homeKickerLabel;
    private Label homeTitleLabel;
    private Label homeSubtitleLabel;
    private Label homeOpenWorkflowTitleLabel;
    private Label homeOpenWorkflowDescriptionLabel;
    private Label homeHeapDumpWorkflowTitleLabel;
    private Label homeHeapDumpWorkflowDescriptionLabel;
    private Label homeJvmWorkflowTitleLabel;
    private Label homeJvmWorkflowDescriptionLabel;
    private Label homeDisclaimerLabel;
    private VBox homeJfrTile;
    private VBox homeHeapDumpTile;
    private VBox homeJvmTile;
    private Label overviewTitleLabel;
    private Label overviewRecordingNameLabel;
    private Label overviewRecordingDetailsLabel;
    private Label overviewAnalysisTitleLabel;
    private Label overviewAnalysisStatusLabel;
    private Label overviewJvmsTitleLabel;
    private Label overviewJvmStatusLabel;
    private Label eventsTitleLabel;
    private TreeView<EventTypeNode> eventTypesTree;
    private TextField eventSearchField;
    private TextField threadFilterField;
    private TextField fieldFilterField;
    private Button clearEventFiltersButton;
    private MenuButton columnsButton;
    private SplitPane eventsSplitPane;
    private TableView<EventRow> eventsTable;
    private Label eventWindowStatusLabel;
    private TabPane eventDetailsTabs;
    private Tab eventPropertiesTab;
    private Tab eventTimingTab;
    private Tab eventThreadTab;
    private Tab eventStackTraceTab;
    private TableView<EventProperty> eventPropertiesTable;
    private Label eventTimingLabel;
    private Label eventThreadLabel;
    private ListView<String> eventStackTraceList;
    private Label analysisTitleLabel;
    private TextField analysisSearchField;
    private Label analysisMinimumScoreLabel;
    private Spinner<Integer> analysisMinimumScoreSpinner;
    private CheckBox analysisShowOkCheckBox;
    private CheckBox analysisShowIgnoredCheckBox;
    private CheckBox analysisShowUnavailableCheckBox;
    private TableView<RuleResult> analysisTable;
    private Label analysisDetailExplanationCaption;
    private TextArea analysisDetailExplanationArea;
    private Label analysisDetailEvidenceCaption;
    private TextArea analysisDetailEvidenceArea;
    private Label analysisDetailRecommendationCaption;
    private TextArea analysisDetailRecommendationArea;
    private Label metadataTitleLabel;
    private Label metadataSummaryLabel;
    private TableView<JfrMetadataEventType> metadataEventTypesTable;
    private Label metadataDetailTitleLabel;
    private TextArea metadataDetailArea;
    private final ChangeListener<JfrMetadataEventType> metadataSelectedEventTypeListener =
            (observable, oldValue, newValue) -> metadataEventTypesTable.getSelectionModel().select(newValue);
    private Label advancedJfrTitleLabel;
    private Label advancedJfrSummaryLabel;
    private TabPane advancedJfrTabs;
    private Tab advancedJfrHeatmapTab;
    private Tab advancedJfrMemoryTab;
    private VBox advancedJfrHeatmapContainer;
    private Label advancedJfrSelectionTitleLabel;
    private Label advancedJfrSelectedEventTypeCaptionLabel;
    private Label advancedJfrSelectedEventTypeLabel;
    private Label advancedJfrSelectedCountCaptionLabel;
    private Label advancedJfrSelectedCountLabel;
    private Label advancedJfrMemorySummaryLabel;
    private TableView<MemoryIssue> advancedJfrMemoryTable;
    private Label advancedJfrMemoryDetailTitleLabel;
    private TextArea advancedJfrMemoryDetailArea;
    private Label javaApplicationTitleLabel;
    private Label javaApplicationSummaryLabel;
    private Label javaApplicationProfilingTitleLabel;
    private Label javaApplicationProfilingSummaryLabel;
    private Button javaApplicationProfilingButton;
    private Label javaApplicationIoTitleLabel;
    private Label javaApplicationIoSummaryLabel;
    private Button javaApplicationIoButton;
    private Label javaApplicationLocksTitleLabel;
    private Label javaApplicationLocksSummaryLabel;
    private Button javaApplicationLocksButton;
    private Label javaApplicationThreadsTitleLabel;
    private Label javaApplicationThreadsSummaryLabel;
    private Button javaApplicationThreadsButton;
    private Label javaApplicationExceptionsTitleLabel;
    private Label javaApplicationExceptionsSummaryLabel;
    private Button javaApplicationExceptionsButton;
    private Label javaApplicationClassLoadingTitleLabel;
    private Label javaApplicationClassLoadingSummaryLabel;
    private Button javaApplicationClassLoadingButton;
    private Label javaApplicationAllocationTitleLabel;
    private Label javaApplicationAllocationSummaryLabel;
    private Button javaApplicationAllocationButton;
    private Label jvmInternalsTitleLabel;
    private Label jvmInternalsSummaryLabel;
    private Label jvmInternalsInformationTitleLabel;
    private Label jvmInternalsInformationSummaryLabel;
    private Button jvmInternalsInformationButton;
    private Label jvmInternalsGcTitleLabel;
    private Label jvmInternalsGcSummaryLabel;
    private Button jvmInternalsGcButton;
    private Label jvmInternalsG1TitleLabel;
    private Label jvmInternalsG1SummaryLabel;
    private Button jvmInternalsG1Button;
    private Label jvmInternalsCompilationTitleLabel;
    private Label jvmInternalsCompilationSummaryLabel;
    private Button jvmInternalsCompilationButton;
    private Label jvmInternalsCodeCacheTitleLabel;
    private Label jvmInternalsCodeCacheSummaryLabel;
    private Button jvmInternalsCodeCacheButton;
    private Label jvmInternalsClassLoadingTitleLabel;
    private Label jvmInternalsClassLoadingSummaryLabel;
    private Button jvmInternalsClassLoadingButton;
    private Label jvmInternalsVmOperationsTitleLabel;
    private Label jvmInternalsVmOperationsSummaryLabel;
    private Button jvmInternalsVmOperationsButton;
    private Label environmentTitleLabel;
    private Label environmentSummaryLabel;
    private Label environmentProcessesTitleLabel;
    private Label environmentProcessesSummaryLabel;
    private Button environmentProcessesButton;
    private Label environmentVariablesTitleLabel;
    private Label environmentVariablesSummaryLabel;
    private Button environmentVariablesButton;
    private Label environmentPropertiesTitleLabel;
    private Label environmentPropertiesSummaryLabel;
    private Button environmentPropertiesButton;
    private Label environmentRecordingTitleLabel;
    private Label environmentRecordingSummaryLabel;
    private Button environmentRecordingButton;
    private Label environmentAgentsTitleLabel;
    private Label environmentAgentsSummaryLabel;
    private Button environmentAgentsButton;
    private Label environmentConstantPoolsTitleLabel;
    private Label environmentConstantPoolsSummaryLabel;
    private Button environmentConstantPoolsButton;
    private Label heapDumpAnalysisTitleLabel;
    private TableView<HeapDumpIssue> heapDumpIssuesTable;
    private TabPane heapDumpDetailsTabs;
    private Tab heapDumpIssueDetailTab;
    private Tab heapDumpTextReportTab;
    private Label heapDumpIssueDetailTitleLabel;
    private TextArea heapDumpIssueDetailArea;
    private TextArea heapDumpTextReportArea;
    private LiveJvmPaneController jvmsPaneController;
    private Label profilingTitleLabel;
    private TableView<HotMethod> profilingTable;
    private TabPane profilingTreeTabs;
    private Tab profilingCallGraphTab;
    private HBox profilingCallGraphToolbar;
    private ComboBox<CallGraphDirection> profilingCallGraphDirectionCombo;
    private Label profilingCallGraphDepthLabel;
    private Spinner<Integer> profilingCallGraphDepthSpinner;
    private Button profilingCallGraphZoomOutButton;
    private Button profilingCallGraphResetZoomButton;
    private Button profilingCallGraphZoomInButton;
    private Button profilingCallGraphFitButton;
    private ScrollPane profilingCallGraphScrollPane;
    private VBox profilingCallGraphContainer;
    private Tab profilingDependencyGraphTab;
    private HBox profilingDependencyToolbar;
    private Label profilingDependencyDepthLabel;
    private Spinner<Integer> profilingDependencyDepthSpinner;
    private Button profilingDependencyZoomOutButton;
    private Button profilingDependencyResetZoomButton;
    private Button profilingDependencyZoomInButton;
    private Button profilingDependencyFitButton;
    private TableView<DependencyGraphEdge> profilingDependencyTable;
    private ScrollPane profilingDependencyGraphScrollPane;
    private VBox profilingDependencyGraphContainer;
    private Tab profilingCallersFlameTab;
    private HBox profilingCallersFlameToolbar;
    private Button profilingCallersFlameOrientationButton;
    private Button profilingCallersFlameZoomOutButton;
    private Button profilingCallersFlameResetZoomButton;
    private Button profilingCallersFlameZoomInButton;
    private Button profilingCallersFlameFitButton;
    private VBox profilingCallersFlameContainer;
    private Tab profilingCalleesFlameTab;
    private HBox profilingCalleesFlameToolbar;
    private Button profilingCalleesFlameOrientationButton;
    private Button profilingCalleesFlameZoomOutButton;
    private Button profilingCalleesFlameResetZoomButton;
    private Button profilingCalleesFlameZoomInButton;
    private Button profilingCalleesFlameFitButton;
    private VBox profilingCalleesFlameContainer;
    private Tab profilingCallersTab;
    private TreeView<StackTreeNode> profilingCallersTree;
    private Tab profilingCalleesTab;
    private TreeView<StackTreeNode> profilingCalleesTree;
    private CallGraphView profilingCallGraphView;
    private CallGraphView profilingDependencyGraphView;
    private FlameGraphView profilingCallersFlameGraphView;
    private FlameGraphView profilingCalleesFlameGraphView;
    private boolean callGraphZoomGestureActive;
    private final ChangeListener<StackTreeNode> callersTreeListener =
            (observable, oldValue, newValue) -> rebuildStackTree(profilingCallersTree, newValue);
    private final ChangeListener<StackTreeNode> calleesTreeListener =
            (observable, oldValue, newValue) -> rebuildStackTree(profilingCalleesTree, newValue);
    private final ChangeListener<CallGraphLayout> callGraphListener =
            (observable, oldValue, newValue) -> profilingCallGraphView.setLayout(newValue);
    private final ChangeListener<CallGraphLayout> dependencyGraphListener =
            (observable, oldValue, newValue) -> profilingDependencyGraphView.setLayout(newValue);
    private final ChangeListener<FlameGraphLayout> callersFlameGraphListener =
            (observable, oldValue, newValue) -> profilingCallersFlameGraphView.setLayout(newValue);
    private final ChangeListener<FlameGraphLayout> calleesFlameGraphListener =
            (observable, oldValue, newValue) -> profilingCalleesFlameGraphView.setLayout(newValue);
    private Label exceptionsTitleLabel;
    private Button exceptionsGroupByClass;
    private Button exceptionsGroupByMessage;
    private Button exceptionsGroupByClassAndMessage;
    private TableView<ExceptionSummary> exceptionsTable;
    private VBox exceptionsTimelineContainer;
    private TimelineChart exceptionsTimelineChart;
    private Label threadsTitleLabel;
    private TableView<ThreadSummary> threadsTable;
    private Label fileioTitleLabel;
    private TabPane fileioTabPane;
    private Tab fileioTimelineTab;
    private VBox fileioTimelineContainer;
    private TimelineChart fileioTimelineChart;
    private Tab fileioDurationTab;
    private TableView<FileIOHistogram> fileioHistogramTable;
    private Tab fileioEventLogTab;
    private TableView<FileIOEvent> fileioEventTable;
    private Label socketioTitleLabel;
    private HBox socketioGroupingBar;
    private Button socketioGroupByHostAndPort;
    private Button socketioGroupByHost;
    private Button socketioGroupByPort;
    private TabPane socketioTabPane;
    private Tab socketioTimelineTab;
    private VBox socketioTimelineContainer;
    private TimelineChart socketioTimelineChart;
    private Tab socketioDurationTab;
    private TableView<SocketIOHistogram> socketioHistogramTable;
    private Tab socketioEventLogTab;
    private TableView<SocketIOEvent> socketioEventTable;
    private Label locksTitleLabel;
    private HBox locksGroupingBar;
    private Button locksGroupByClass;
    private Button locksGroupByAddress;
    private Button locksGroupByThread;
    private TabPane locksTabPane;
    private Tab locksByClassTab;
    private TableView<LockHistogram> locksByClassTable;
    private Tab locksByAddressTab;
    private TableView<LockHistogram> locksByAddressTable;
    private Tab locksByThreadTab;
    private TableView<LockHistogram> locksByThreadTable;
    private Label threadHistogramTitleLabel;
    private VBox threadHistogramChartContainer;
    private TimelineChart threadHistogramChart;
    private TableView<ThreadHistogramRow> threadHistogramTable;
    private Label securityTitleLabel;
    private TableView<X509CertificateEntry> securityTable;
    private Label nativeLibrariesTitleLabel;
    private TableView<NativeLibraryEntry> nativeLibrariesTable;
    private Label threadDumpsTitleLabel;
    private TableView<ThreadDumpEntry> threadDumpsTable;
    private TextArea threadDumpTextArea;
    private Label heapTitleLabel;
    private TableView<HeapClassHistogram> heapTable;
    private VBox heapTimelineContainer;
    private TimelineChart heapTimelineChart;
    private Label leaksTitleLabel;
    private TableView<LeakCandidate> leaksTable;
    private TreeView<LeakReferenceNode> leaksReferenceTree;
    private Label tlabTitleLabel;
    private TableView<TlabAllocation> tlabTable;
    private VBox tlabTimelineContainer;
    private TimelineChart tlabTimelineChart;
    private TableView<JvmFlag> jvmFlagsTable;
    private TableView<JvmFlagChange> jvmFlagChangesTable;
    private TableView<GcSummary> gcSummaryTable;
    private TableView<GcEvent> gcEventsTable;
    private TableView<GcReferenceStat> gcReferenceStatsTable;
    private TableView<GcHeapSummary> gcHeapSummaryTable;
    private Label g1GcTitleLabel;
    private Label g1GcSummaryLabel;
    private Label g1GcRegionStatesLabel;
    private Label g1GcRegionSummaryLabel;
    private Label g1GcPausesLabel;
    private TableView<G1GcRegionSummary> g1GcRegionSummaryTable;
    private TableView<G1GcRegionState> g1GcRegionStatesTable;
    private TableView<GcEvent> g1GcPauseTable;
    private Label g1GcDetailTitleLabel;
    private TextArea g1GcDetailArea;
    private final ChangeListener<G1GcRegionState> g1GcSelectedRegionStateListener =
            (observable, oldValue, newValue) -> g1GcRegionStatesTable.getSelectionModel().select(newValue);
    private Label javaFxEventsTitleLabel;
    private Label javaFxEventsSummaryLabel;
    private Label javaFxEventsPhaseLabel;
    private Label javaFxEventsPulseLabel;
    private Label javaFxEventsInputLabel;
    private TableView<JavaFxPulsePhase> javaFxEventsPhaseTable;
    private TableView<JavaFxPulseSummary> javaFxEventsPulseTable;
    private TableView<JavaFxInputEvent> javaFxEventsInputTable;
    private Label javaFxEventsDetailTitleLabel;
    private TextArea javaFxEventsDetailArea;
    private final ChangeListener<JavaFxPulsePhase> javaFxSelectedPulsePhaseListener =
            (observable, oldValue, newValue) -> javaFxEventsPhaseTable.getSelectionModel().select(newValue);
    private VBox gcHeapChartContainer;
    private TimelineChart gcHeapChart;
    private VBox gcMetaspaceChartContainer;
    private TimelineChart gcMetaspaceChart;
    private VBox gcPauseChartContainer;
    private TimelineChart gcPauseChart;
    private TableView<CompilationEvent> compilationsTable;
    private VBox compilationDurationChartContainer;
    private TimelineChart compilationDurationChart;
    private TableView<CompilationEvent> compilationFailuresTable;
    private TableView<CodeCacheSweep> codeCacheSweepsTable;
    private VBox codeCacheEntriesChartContainer;
    private TimelineChart codeCacheEntriesChart;
    private VBox codeCacheSweepChartContainer;
    private TimelineChart codeCacheSweepChart;
    private TableView<CodeCacheStats> codeCacheStatsTable;
    private TableView<ClassloaderSummary> classLoadingHistogramTable;
    private VBox classLoadingChartContainer;
    private TimelineChart classLoadingChart;
    private TableView<ClassloadEvent> classLoadingEventsTable;
    private TableView<ClassloaderStatistics> classLoadingStatsTable;
    private TableView<VmOperationSummary> vmOperationSummaryTable;
    private TableView<VmOperationEvent> vmOperationEventsTable;
    private Timeline liveJvmOverviewRefreshTimeline;
    private TableView<ProcessInfo> processesTable;
    private TableView<EnvironmentVariable> envVarsTable;
    private TextField envVarsSearchField;
    private TableView<SystemProperty> sysPropsTable;
    private TextField sysPropsSearchField;
    private TableView<ActiveRecordingInfo> recordingsTable;
    private TableView<ActiveSetting> settingsTable;
    private TabPane recordingInfoTabs;
    private Tab recordingInfoRecordingsTab;
    private Tab recordingInfoSettingsTab;
    private TableView<AgentInfo> agentsTable;
    private TableView<ConstantPoolType> constantPoolsTable;
    private Label jvmInfoTitleLabel;
    private Label jvmFlagsLabel;
    private Label jvmFlagChangesLabel;
    private Label gcConfigTitleLabel;
    private Label gcConfigDescriptionLabel;
    private Label gcSummaryTitleLabel;
    private Label gcDetailsTitleLabel;
    private Label gcEventsLabel;
    private Label gcReferenceStatsLabel;
    private Label gcHeapSummaryLabel;
    private Label compilationsTitleLabel;
    private Label compilationEventsLabel;
    private Label compilationFailuresLabel;
    private Label codeCacheTitleLabel;
    private Label codeCacheSweepsLabel;
    private Label codeCacheStatsLabel;
    private Label classLoadingTitleLabel;
    private Label classLoadingHistogramLabel;
    private Label classLoadingEventsLabel;
    private Label classLoadingStatsLabel;
    private Label vmOperationsTitleLabel;
    private Label vmOperationSummaryLabel;
    private Label vmOperationEventsLabel;
    private Label processesTitleLabel;
    private Label envVarsTitleLabel;
    private Label sysPropsTitleLabel;
    private Label recordingInfoTitleLabel;
    private Label agentsTitleLabel;
    private Label constantPoolsTitleLabel;
    private Label settingsTitleLabel;
    private Label settingsLanguageLabel;
    private ToggleGroup languageToggleGroup;
    private RadioButton languageFollowSystemRadio;
    private RadioButton languageEnglishRadio;
    private RadioButton languageChineseRadio;
    private Label settingsThemeLabel;
    private ToggleGroup themeToggleGroup;
    private RadioButton themeFollowSystemRadio;
    private RadioButton themeLightRadio;
    private RadioButton themeDarkRadio;

    public AppShellController(AppShellViewModel viewModel, RecordingRepository recordingRepository,
            EventQueryService eventQueryService, RuleAnalysisService ruleAnalysisService,
            ProfilingService profilingService, ExceptionService exceptionService,
            ThreadService threadService, FileIOService fileIOService,
            SocketIOService socketIOService, LockService lockService,
            HeapService heapService, LeakSuspectsService leakSuspectsService,
            TlabService tlabService,
            JvmInternalsService jvmInternalsService,
            EnvironmentService environmentService,
            JavaAppService javaAppService,
            I18n i18n) {
        this(new AppShellView(), viewModel, recordingRepository, eventQueryService, ruleAnalysisService,
                profilingService, exceptionService, threadService,
                fileIOService, socketIOService, lockService,
                heapService, leakSuspectsService, tlabService,
                jvmInternalsService, environmentService, javaAppService,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, i18n,
                new VirtualThreadRecordingOpenExecutor());
    }

    public AppShellController(AppShellViewModel viewModel, RecordingRepository recordingRepository,
            EventQueryService eventQueryService, RuleAnalysisService ruleAnalysisService,
            ProfilingService profilingService, ExceptionService exceptionService,
            ThreadService threadService, FileIOService fileIOService,
            SocketIOService socketIOService, LockService lockService,
            HeapService heapService, LeakSuspectsService leakSuspectsService,
            TlabService tlabService,
            JvmInternalsService jvmInternalsService,
            EnvironmentService environmentService,
            JavaAppService javaAppService,
            JvmDiscoveryService jvmDiscoveryService,
            JmxConnectionService jmxConnectionService,
            FlightRecordingService flightRecordingService,
            I18n i18n) {
        this(new AppShellView(), viewModel, recordingRepository, eventQueryService, ruleAnalysisService,
                profilingService, exceptionService, threadService,
                fileIOService, socketIOService, lockService,
                heapService, leakSuspectsService, tlabService,
                jvmInternalsService, environmentService, javaAppService,
                jvmDiscoveryService, jmxConnectionService, flightRecordingService, null, null, null, null, null,
                null, null, null, null, null, null, null, null, i18n, new VirtualThreadRecordingOpenExecutor());
    }

    public AppShellController(AppShellViewModel viewModel, RecordingRepository recordingRepository,
            EventQueryService eventQueryService, RuleAnalysisService ruleAnalysisService,
            ProfilingService profilingService, ExceptionService exceptionService,
            ThreadService threadService, FileIOService fileIOService,
            SocketIOService socketIOService, LockService lockService,
            HeapService heapService, LeakSuspectsService leakSuspectsService,
            TlabService tlabService,
            JvmInternalsService jvmInternalsService,
            EnvironmentService environmentService,
            JavaAppService javaAppService,
            JvmDiscoveryService jvmDiscoveryService,
            JmxConnectionService jmxConnectionService,
            FlightRecordingService flightRecordingService,
            MBeanBrowserService mBeanBrowserService,
            I18n i18n) {
        this(new AppShellView(), viewModel, recordingRepository, eventQueryService, ruleAnalysisService,
                profilingService, exceptionService, threadService,
                fileIOService, socketIOService, lockService,
                heapService, leakSuspectsService, tlabService,
                jvmInternalsService, environmentService, javaAppService,
                jvmDiscoveryService, jmxConnectionService, flightRecordingService, mBeanBrowserService, null, null,
                null, null, null, null, null, null, null, null, null, null, i18n, new VirtualThreadRecordingOpenExecutor());
    }

    public AppShellController(AppShellViewModel viewModel, RecordingRepository recordingRepository,
            EventQueryService eventQueryService, RuleAnalysisService ruleAnalysisService,
            ProfilingService profilingService, ExceptionService exceptionService,
            ThreadService threadService, FileIOService fileIOService,
            SocketIOService socketIOService, LockService lockService,
            HeapService heapService, LeakSuspectsService leakSuspectsService,
            TlabService tlabService,
            JvmInternalsService jvmInternalsService,
            EnvironmentService environmentService,
            JavaAppService javaAppService,
            JvmDiscoveryService jvmDiscoveryService,
            JmxConnectionService jmxConnectionService,
            FlightRecordingService flightRecordingService,
            MBeanBrowserService mBeanBrowserService,
            DiagnosticCommandService diagnosticCommandService,
            LiveMetricService liveMetricService,
            AdvancedJfrAnalysisService advancedJfrAnalysisService,
            I18n i18n) {
        this(new AppShellView(), viewModel, recordingRepository, eventQueryService, ruleAnalysisService,
                profilingService, exceptionService, threadService,
                fileIOService, socketIOService, lockService,
                heapService, leakSuspectsService, tlabService,
                jvmInternalsService, environmentService, javaAppService,
                jvmDiscoveryService, jmxConnectionService, flightRecordingService, mBeanBrowserService,
                diagnosticCommandService, liveMetricService, null, null, null, null, null, null, advancedJfrAnalysisService, null, null,
                null, i18n, new VirtualThreadRecordingOpenExecutor());
    }

    public AppShellController(AppShellViewModel viewModel, RecordingRepository recordingRepository,
            EventQueryService eventQueryService, RuleAnalysisService ruleAnalysisService,
            ProfilingService profilingService, ExceptionService exceptionService,
            ThreadService threadService, FileIOService fileIOService,
            SocketIOService socketIOService, LockService lockService,
            HeapService heapService, LeakSuspectsService leakSuspectsService,
            TlabService tlabService,
            JvmInternalsService jvmInternalsService,
            EnvironmentService environmentService,
            JavaAppService javaAppService,
            JvmDiscoveryService jvmDiscoveryService,
            JmxConnectionService jmxConnectionService,
            FlightRecordingService flightRecordingService,
            MBeanBrowserService mBeanBrowserService,
            DiagnosticCommandService diagnosticCommandService,
            LiveMetricService liveMetricService,
            JmcAgentService jmcAgentService,
            AdvancedJfrAnalysisService advancedJfrAnalysisService,
            SavedJvmTargetRepository savedTargetRepository,
            JdpDiscoveryService jdpDiscoveryService,
            HeapDumpAnalysisService heapDumpAnalysisService,
            I18n i18n) {
        this(new AppShellView(), viewModel, recordingRepository, eventQueryService, ruleAnalysisService,
                profilingService, exceptionService, threadService,
                fileIOService, socketIOService, lockService,
                heapService, leakSuspectsService, tlabService,
                jvmInternalsService, environmentService, javaAppService,
                jvmDiscoveryService, jmxConnectionService, flightRecordingService, mBeanBrowserService,
                diagnosticCommandService, liveMetricService, jmcAgentService, null, null, null, null, null,
                advancedJfrAnalysisService, savedTargetRepository, jdpDiscoveryService, heapDumpAnalysisService, i18n,
                new VirtualThreadRecordingOpenExecutor());
    }

    public AppShellController(AppShellViewModel viewModel, RecordingRepository recordingRepository,
            EventQueryService eventQueryService, RuleAnalysisService ruleAnalysisService,
            ProfilingService profilingService, ExceptionService exceptionService,
            ThreadService threadService, FileIOService fileIOService,
            SocketIOService socketIOService, LockService lockService,
            HeapService heapService, LeakSuspectsService leakSuspectsService,
            TlabService tlabService,
            JvmInternalsService jvmInternalsService,
            EnvironmentService environmentService,
            JavaAppService javaAppService,
            JvmDiscoveryService jvmDiscoveryService,
            JmxConnectionService jmxConnectionService,
            FlightRecordingService flightRecordingService,
            MBeanBrowserService mBeanBrowserService,
            DiagnosticCommandService diagnosticCommandService,
            LiveMetricService liveMetricService,
            JmcAgentService jmcAgentService,
            JmxMonitoringService jmxMonitoringService,
            JmxMonitoringRepository jmxMonitoringRepository,
            JfrMetadataService jfrMetadataService,
            G1GcService g1GcService,
            JavaFxEventService javaFxEventService,
            AdvancedJfrAnalysisService advancedJfrAnalysisService,
            SavedJvmTargetRepository savedTargetRepository,
            JdpDiscoveryService jdpDiscoveryService,
            HeapDumpAnalysisService heapDumpAnalysisService,
            I18n i18n) {
        this(new AppShellView(), viewModel, recordingRepository, eventQueryService, ruleAnalysisService,
                profilingService, exceptionService, threadService,
                fileIOService, socketIOService, lockService,
                heapService, leakSuspectsService, tlabService,
                jvmInternalsService, environmentService, javaAppService,
                jvmDiscoveryService, jmxConnectionService, flightRecordingService, mBeanBrowserService,
                diagnosticCommandService, liveMetricService, jmcAgentService,
                jmxMonitoringService, jmxMonitoringRepository, jfrMetadataService, g1GcService,
                javaFxEventService, advancedJfrAnalysisService,
                savedTargetRepository, jdpDiscoveryService, heapDumpAnalysisService, i18n,
                new VirtualThreadRecordingOpenExecutor());
    }

    public AppShellController(AppShellViewModel viewModel, RecordingRepository recordingRepository,
            EventQueryService eventQueryService, RuleAnalysisService ruleAnalysisService,
            ProfilingService profilingService, ExceptionService exceptionService,
            ThreadService threadService, FileIOService fileIOService,
            SocketIOService socketIOService, LockService lockService,
            HeapService heapService, LeakSuspectsService leakSuspectsService,
            TlabService tlabService,
            JvmInternalsService jvmInternalsService,
            EnvironmentService environmentService,
            JavaAppService javaAppService,
            JvmDiscoveryService jvmDiscoveryService,
            JmxConnectionService jmxConnectionService,
            I18n i18n) {
        this(new AppShellView(), viewModel, recordingRepository, eventQueryService, ruleAnalysisService,
                profilingService, exceptionService, threadService,
                fileIOService, socketIOService, lockService,
                heapService, leakSuspectsService, tlabService,
                jvmInternalsService, environmentService, javaAppService,
                jvmDiscoveryService, jmxConnectionService, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, i18n, new VirtualThreadRecordingOpenExecutor());
    }

    AppShellController(AppShellViewModel viewModel, RecordingRepository recordingRepository,
            EventQueryService eventQueryService, RuleAnalysisService ruleAnalysisService,
            ProfilingService profilingService, ExceptionService exceptionService,
            ThreadService threadService, FileIOService fileIOService,
            SocketIOService socketIOService, LockService lockService,
            HeapService heapService, LeakSuspectsService leakSuspectsService,
            TlabService tlabService, JvmInternalsService jvmInternalsService,
            EnvironmentService environmentService,
            JavaAppService javaAppService,
            I18n i18n,
            RecordingOpenExecutor recordingOpenExecutor) {
        this(new AppShellView(), viewModel, recordingRepository, eventQueryService, ruleAnalysisService,
                profilingService, exceptionService, threadService,
                fileIOService, socketIOService, lockService,
                heapService, leakSuspectsService, tlabService,
                jvmInternalsService, environmentService, javaAppService,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, i18n,
                recordingOpenExecutor);
    }

    AppShellController(AppShellViewModel viewModel, RecordingRepository recordingRepository,
            EventQueryService eventQueryService, RuleAnalysisService ruleAnalysisService,
            ProfilingService profilingService, ExceptionService exceptionService,
            ThreadService threadService, FileIOService fileIOService,
            SocketIOService socketIOService, LockService lockService,
            HeapService heapService, LeakSuspectsService leakSuspectsService,
            TlabService tlabService, JvmInternalsService jvmInternalsService,
            EnvironmentService environmentService,
            JavaAppService javaAppService,
            JvmDiscoveryService jvmDiscoveryService,
            JmxConnectionService jmxConnectionService,
            FlightRecordingService flightRecordingService,
            I18n i18n,
            RecordingOpenExecutor recordingOpenExecutor) {
        this(new AppShellView(), viewModel, recordingRepository, eventQueryService, ruleAnalysisService,
                profilingService, exceptionService, threadService,
                fileIOService, socketIOService, lockService,
                heapService, leakSuspectsService, tlabService,
                jvmInternalsService, environmentService, javaAppService,
                jvmDiscoveryService, jmxConnectionService, flightRecordingService, null, null, null, null, null,
                null, null, null, null, null, null, null, null, i18n, recordingOpenExecutor);
    }

    AppShellController(AppShellView view, AppShellViewModel viewModel, RecordingRepository recordingRepository,
            EventQueryService eventQueryService, RuleAnalysisService ruleAnalysisService,
            ProfilingService profilingService, ExceptionService exceptionService,
            ThreadService threadService, FileIOService fileIOService,
            SocketIOService socketIOService, LockService lockService,
            HeapService heapService, LeakSuspectsService leakSuspectsService,
            TlabService tlabService, JvmInternalsService jvmInternalsService,
            EnvironmentService environmentService,
            JavaAppService javaAppService,
            JvmDiscoveryService jvmDiscoveryService,
            JmxConnectionService jmxConnectionService,
            FlightRecordingService flightRecordingService,
            MBeanBrowserService mBeanBrowserService,
            DiagnosticCommandService diagnosticCommandService,
            LiveMetricService liveMetricService,
            JmcAgentService jmcAgentService,
            JmxMonitoringService jmxMonitoringService,
            JmxMonitoringRepository jmxMonitoringRepository,
            JfrMetadataService jfrMetadataService,
            G1GcService g1GcService,
            JavaFxEventService javaFxEventService,
            AdvancedJfrAnalysisService advancedJfrAnalysisService,
            SavedJvmTargetRepository savedTargetRepository,
            JdpDiscoveryService jdpDiscoveryService,
            HeapDumpAnalysisService heapDumpAnalysisService,
            I18n i18n,
            RecordingOpenExecutor recordingOpenExecutor) {
        this.view = Objects.requireNonNull(view, "view");
        assignViewFields(view);
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
        this.javaAppService = javaAppService;
        this.jvmDiscoveryService = jvmDiscoveryService;
        this.jmxConnectionService = jmxConnectionService;
        this.flightRecordingService = flightRecordingService;
        this.mBeanBrowserService = mBeanBrowserService;
        this.diagnosticCommandService = diagnosticCommandService;
        this.liveMetricService = liveMetricService;
        this.jmcAgentService = jmcAgentService;
        this.jmxMonitoringService = jmxMonitoringService;
        this.jmxMonitoringRepository = jmxMonitoringRepository;
        this.jfrMetadataService = jfrMetadataService;
        this.g1GcService = g1GcService;
        this.javaFxEventService = javaFxEventService;
        this.advancedJfrAnalysisService = advancedJfrAnalysisService;
        this.savedTargetRepository = savedTargetRepository;
        this.jdpDiscoveryService = jdpDiscoveryService;
        this.heapDumpAnalysisService = heapDumpAnalysisService;
        this.i18n = i18n;
        this.recordingOpenExecutor = recordingOpenExecutor;
    }

    private void assignViewFields(AppShellView view) {
        this.root = view.root;
        this.homeOpenRecordingButton = view.homeOpenRecordingButton;
        this.homeOpenHeapDumpButton = view.homeOpenHeapDumpButton;
        this.homeConnectJvmButton = view.homeConnectJvmButton;
        this.sidebar = view.sidebar;
        this.recordingTabs = view.recordingTabs;
        this.homePane = view.homePane;
        this.overviewPane = view.overviewPane;
        this.eventsPane = view.eventsPane;
        this.analysisPane = view.analysisPane;
        this.metadataPane = view.metadataPane;
        this.advancedJfrPane = view.advancedJfrPane;
        this.heapDumpAnalysisPane = view.heapDumpAnalysisPane;
        this.jvmsPaneHost = view.jvmsPaneHost;
        this.javaApplicationPane = view.javaApplicationPane;
        this.jvmInternalsPane = view.jvmInternalsPane;
        this.environmentPane = view.environmentPane;
        this.profilingPane = view.profilingPane;
        this.exceptionsPane = view.exceptionsPane;
        this.threadsPane = view.threadsPane;
        this.fileioPane = view.fileioPane;
        this.socketioPane = view.socketioPane;
        this.locksPane = view.locksPane;
        this.threadHistogramPane = view.threadHistogramPane;
        this.securityPane = view.securityPane;
        this.nativeLibrariesPane = view.nativeLibrariesPane;
        this.threadDumpsPane = view.threadDumpsPane;
        this.heapPane = view.heapPane;
        this.leaksPane = view.leaksPane;
        this.tlabPane = view.tlabPane;
        this.jvmInfoPane = view.jvmInfoPane;
        this.gcConfigPane = view.gcConfigPane;
        this.gcSummaryPane = view.gcSummaryPane;
        this.gcDetailsPane = view.gcDetailsPane;
        this.g1GcPane = view.g1GcPane;
        this.javaFxEventsPane = view.javaFxEventsPane;
        this.compilationsPane = view.compilationsPane;
        this.codeCachePane = view.codeCachePane;
        this.classLoadingPane = view.classLoadingPane;
        this.vmOperationsPane = view.vmOperationsPane;
        this.processesPane = view.processesPane;
        this.envVarsPane = view.envVarsPane;
        this.sysPropsPane = view.sysPropsPane;
        this.recordingInfoPane = view.recordingInfoPane;
        this.agentsPane = view.agentsPane;
        this.constantPoolsPane = view.constantPoolsPane;
        this.settingsPane = view.settingsPane;
        this.progressBar = view.progressBar;
        this.homeKickerLabel = view.homeKickerLabel;
        this.homeTitleLabel = view.homeTitleLabel;
        this.homeSubtitleLabel = view.homeSubtitleLabel;
        this.homeOpenWorkflowTitleLabel = view.homeOpenWorkflowTitleLabel;
        this.homeOpenWorkflowDescriptionLabel = view.homeOpenWorkflowDescriptionLabel;
        this.homeHeapDumpWorkflowTitleLabel = view.homeHeapDumpWorkflowTitleLabel;
        this.homeHeapDumpWorkflowDescriptionLabel = view.homeHeapDumpWorkflowDescriptionLabel;
        this.homeJvmWorkflowTitleLabel = view.homeJvmWorkflowTitleLabel;
        this.homeJvmWorkflowDescriptionLabel = view.homeJvmWorkflowDescriptionLabel;
        this.homeDisclaimerLabel = view.homeDisclaimerLabel;
        this.homeJfrTile = view.homeJfrTile;
        this.homeHeapDumpTile = view.homeHeapDumpTile;
        this.homeJvmTile = view.homeJvmTile;
        this.overviewTitleLabel = view.overviewTitleLabel;
        this.overviewRecordingNameLabel = view.overviewRecordingNameLabel;
        this.overviewRecordingDetailsLabel = view.overviewRecordingDetailsLabel;
        this.overviewAnalysisTitleLabel = view.overviewAnalysisTitleLabel;
        this.overviewAnalysisStatusLabel = view.overviewAnalysisStatusLabel;
        this.overviewJvmsTitleLabel = view.overviewJvmsTitleLabel;
        this.overviewJvmStatusLabel = view.overviewJvmStatusLabel;
        this.eventsTitleLabel = view.eventsTitleLabel;
        this.eventTypesTree = view.eventTypesTree;
        this.eventSearchField = view.eventSearchField;
        this.threadFilterField = view.threadFilterField;
        this.fieldFilterField = view.fieldFilterField;
        this.clearEventFiltersButton = view.clearEventFiltersButton;
        this.columnsButton = view.columnsButton;
        this.eventsSplitPane = view.eventsSplitPane;
        this.eventsTable = view.eventsTable;
        this.eventWindowStatusLabel = view.eventWindowStatusLabel;
        this.eventDetailsTabs = view.eventDetailsTabs;
        this.eventPropertiesTab = view.eventPropertiesTab;
        this.eventTimingTab = view.eventTimingTab;
        this.eventThreadTab = view.eventThreadTab;
        this.eventStackTraceTab = view.eventStackTraceTab;
        this.eventPropertiesTable = view.eventPropertiesTable;
        this.eventTimingLabel = view.eventTimingLabel;
        this.eventThreadLabel = view.eventThreadLabel;
        this.eventStackTraceList = view.eventStackTraceList;
        this.analysisTitleLabel = view.analysisTitleLabel;
        this.analysisSearchField = view.analysisSearchField;
        this.analysisMinimumScoreLabel = view.analysisMinimumScoreLabel;
        this.analysisMinimumScoreSpinner = view.analysisMinimumScoreSpinner;
        this.analysisShowOkCheckBox = view.analysisShowOkCheckBox;
        this.analysisShowIgnoredCheckBox = view.analysisShowIgnoredCheckBox;
        this.analysisShowUnavailableCheckBox = view.analysisShowUnavailableCheckBox;
        this.analysisTable = view.analysisTable;
        this.analysisDetailExplanationCaption = view.analysisDetailExplanationCaption;
        this.analysisDetailExplanationArea = view.analysisDetailExplanationArea;
        this.analysisDetailEvidenceCaption = view.analysisDetailEvidenceCaption;
        this.analysisDetailEvidenceArea = view.analysisDetailEvidenceArea;
        this.analysisDetailRecommendationCaption = view.analysisDetailRecommendationCaption;
        this.analysisDetailRecommendationArea = view.analysisDetailRecommendationArea;
        this.metadataTitleLabel = view.metadataTitleLabel;
        this.metadataSummaryLabel = view.metadataSummaryLabel;
        this.metadataEventTypesTable = view.metadataEventTypesTable;
        this.metadataDetailTitleLabel = view.metadataDetailTitleLabel;
        this.metadataDetailArea = view.metadataDetailArea;
        this.advancedJfrTitleLabel = view.advancedJfrTitleLabel;
        this.advancedJfrSummaryLabel = view.advancedJfrSummaryLabel;
        this.advancedJfrTabs = view.advancedJfrTabs;
        this.advancedJfrHeatmapTab = view.advancedJfrHeatmapTab;
        this.advancedJfrMemoryTab = view.advancedJfrMemoryTab;
        this.advancedJfrHeatmapContainer = view.advancedJfrHeatmapContainer;
        this.advancedJfrSelectionTitleLabel = view.advancedJfrSelectionTitleLabel;
        this.advancedJfrSelectedEventTypeCaptionLabel = view.advancedJfrSelectedEventTypeCaptionLabel;
        this.advancedJfrSelectedEventTypeLabel = view.advancedJfrSelectedEventTypeLabel;
        this.advancedJfrSelectedCountCaptionLabel = view.advancedJfrSelectedCountCaptionLabel;
        this.advancedJfrSelectedCountLabel = view.advancedJfrSelectedCountLabel;
        this.advancedJfrMemorySummaryLabel = view.advancedJfrMemorySummaryLabel;
        this.advancedJfrMemoryTable = view.advancedJfrMemoryTable;
        this.advancedJfrMemoryDetailTitleLabel = view.advancedJfrMemoryDetailTitleLabel;
        this.advancedJfrMemoryDetailArea = view.advancedJfrMemoryDetailArea;
        this.javaApplicationTitleLabel = view.javaApplicationTitleLabel;
        this.javaApplicationSummaryLabel = view.javaApplicationSummaryLabel;
        this.javaApplicationProfilingTitleLabel = view.javaApplicationProfilingTitleLabel;
        this.javaApplicationProfilingSummaryLabel = view.javaApplicationProfilingSummaryLabel;
        this.javaApplicationProfilingButton = view.javaApplicationProfilingButton;
        this.javaApplicationIoTitleLabel = view.javaApplicationIoTitleLabel;
        this.javaApplicationIoSummaryLabel = view.javaApplicationIoSummaryLabel;
        this.javaApplicationIoButton = view.javaApplicationIoButton;
        this.javaApplicationLocksTitleLabel = view.javaApplicationLocksTitleLabel;
        this.javaApplicationLocksSummaryLabel = view.javaApplicationLocksSummaryLabel;
        this.javaApplicationLocksButton = view.javaApplicationLocksButton;
        this.javaApplicationThreadsTitleLabel = view.javaApplicationThreadsTitleLabel;
        this.javaApplicationThreadsSummaryLabel = view.javaApplicationThreadsSummaryLabel;
        this.javaApplicationThreadsButton = view.javaApplicationThreadsButton;
        this.javaApplicationExceptionsTitleLabel = view.javaApplicationExceptionsTitleLabel;
        this.javaApplicationExceptionsSummaryLabel = view.javaApplicationExceptionsSummaryLabel;
        this.javaApplicationExceptionsButton = view.javaApplicationExceptionsButton;
        this.javaApplicationClassLoadingTitleLabel = view.javaApplicationClassLoadingTitleLabel;
        this.javaApplicationClassLoadingSummaryLabel = view.javaApplicationClassLoadingSummaryLabel;
        this.javaApplicationClassLoadingButton = view.javaApplicationClassLoadingButton;
        this.javaApplicationAllocationTitleLabel = view.javaApplicationAllocationTitleLabel;
        this.javaApplicationAllocationSummaryLabel = view.javaApplicationAllocationSummaryLabel;
        this.javaApplicationAllocationButton = view.javaApplicationAllocationButton;
        this.jvmInternalsTitleLabel = view.jvmInternalsTitleLabel;
        this.jvmInternalsSummaryLabel = view.jvmInternalsSummaryLabel;
        this.jvmInternalsInformationTitleLabel = view.jvmInternalsInformationTitleLabel;
        this.jvmInternalsInformationSummaryLabel = view.jvmInternalsInformationSummaryLabel;
        this.jvmInternalsInformationButton = view.jvmInternalsInformationButton;
        this.jvmInternalsGcTitleLabel = view.jvmInternalsGcTitleLabel;
        this.jvmInternalsGcSummaryLabel = view.jvmInternalsGcSummaryLabel;
        this.jvmInternalsGcButton = view.jvmInternalsGcButton;
        this.jvmInternalsG1TitleLabel = view.jvmInternalsG1TitleLabel;
        this.jvmInternalsG1SummaryLabel = view.jvmInternalsG1SummaryLabel;
        this.jvmInternalsG1Button = view.jvmInternalsG1Button;
        this.jvmInternalsCompilationTitleLabel = view.jvmInternalsCompilationTitleLabel;
        this.jvmInternalsCompilationSummaryLabel = view.jvmInternalsCompilationSummaryLabel;
        this.jvmInternalsCompilationButton = view.jvmInternalsCompilationButton;
        this.jvmInternalsCodeCacheTitleLabel = view.jvmInternalsCodeCacheTitleLabel;
        this.jvmInternalsCodeCacheSummaryLabel = view.jvmInternalsCodeCacheSummaryLabel;
        this.jvmInternalsCodeCacheButton = view.jvmInternalsCodeCacheButton;
        this.jvmInternalsClassLoadingTitleLabel = view.jvmInternalsClassLoadingTitleLabel;
        this.jvmInternalsClassLoadingSummaryLabel = view.jvmInternalsClassLoadingSummaryLabel;
        this.jvmInternalsClassLoadingButton = view.jvmInternalsClassLoadingButton;
        this.jvmInternalsVmOperationsTitleLabel = view.jvmInternalsVmOperationsTitleLabel;
        this.jvmInternalsVmOperationsSummaryLabel = view.jvmInternalsVmOperationsSummaryLabel;
        this.jvmInternalsVmOperationsButton = view.jvmInternalsVmOperationsButton;
        this.environmentTitleLabel = view.environmentTitleLabel;
        this.environmentSummaryLabel = view.environmentSummaryLabel;
        this.environmentProcessesTitleLabel = view.environmentProcessesTitleLabel;
        this.environmentProcessesSummaryLabel = view.environmentProcessesSummaryLabel;
        this.environmentProcessesButton = view.environmentProcessesButton;
        this.environmentVariablesTitleLabel = view.environmentVariablesTitleLabel;
        this.environmentVariablesSummaryLabel = view.environmentVariablesSummaryLabel;
        this.environmentVariablesButton = view.environmentVariablesButton;
        this.environmentPropertiesTitleLabel = view.environmentPropertiesTitleLabel;
        this.environmentPropertiesSummaryLabel = view.environmentPropertiesSummaryLabel;
        this.environmentPropertiesButton = view.environmentPropertiesButton;
        this.environmentRecordingTitleLabel = view.environmentRecordingTitleLabel;
        this.environmentRecordingSummaryLabel = view.environmentRecordingSummaryLabel;
        this.environmentRecordingButton = view.environmentRecordingButton;
        this.environmentAgentsTitleLabel = view.environmentAgentsTitleLabel;
        this.environmentAgentsSummaryLabel = view.environmentAgentsSummaryLabel;
        this.environmentAgentsButton = view.environmentAgentsButton;
        this.environmentConstantPoolsTitleLabel = view.environmentConstantPoolsTitleLabel;
        this.environmentConstantPoolsSummaryLabel = view.environmentConstantPoolsSummaryLabel;
        this.environmentConstantPoolsButton = view.environmentConstantPoolsButton;
        this.heapDumpAnalysisTitleLabel = view.heapDumpAnalysisTitleLabel;
        this.heapDumpIssuesTable = view.heapDumpIssuesTable;
        this.heapDumpDetailsTabs = view.heapDumpDetailsTabs;
        this.heapDumpIssueDetailTab = view.heapDumpIssueDetailTab;
        this.heapDumpTextReportTab = view.heapDumpTextReportTab;
        this.heapDumpIssueDetailTitleLabel = view.heapDumpIssueDetailTitleLabel;
        this.heapDumpIssueDetailArea = view.heapDumpIssueDetailArea;
        this.heapDumpTextReportArea = view.heapDumpTextReportArea;
        this.profilingTitleLabel = view.profilingTitleLabel;
        this.profilingTable = view.profilingTable;
        this.profilingTreeTabs = view.profilingTreeTabs;
        this.profilingCallGraphTab = view.profilingCallGraphTab;
        this.profilingCallGraphToolbar = view.profilingCallGraphToolbar;
        this.profilingCallGraphDirectionCombo = view.profilingCallGraphDirectionCombo;
        this.profilingCallGraphDepthLabel = view.profilingCallGraphDepthLabel;
        this.profilingCallGraphDepthSpinner = view.profilingCallGraphDepthSpinner;
        this.profilingCallGraphZoomOutButton = view.profilingCallGraphZoomOutButton;
        this.profilingCallGraphResetZoomButton = view.profilingCallGraphResetZoomButton;
        this.profilingCallGraphZoomInButton = view.profilingCallGraphZoomInButton;
        this.profilingCallGraphFitButton = view.profilingCallGraphFitButton;
        this.profilingCallGraphScrollPane = view.profilingCallGraphScrollPane;
        this.profilingCallGraphContainer = view.profilingCallGraphContainer;
        this.profilingDependencyGraphTab = view.profilingDependencyGraphTab;
        this.profilingDependencyToolbar = view.profilingDependencyToolbar;
        this.profilingDependencyDepthLabel = view.profilingDependencyDepthLabel;
        this.profilingDependencyDepthSpinner = view.profilingDependencyDepthSpinner;
        this.profilingDependencyZoomOutButton = view.profilingDependencyZoomOutButton;
        this.profilingDependencyResetZoomButton = view.profilingDependencyResetZoomButton;
        this.profilingDependencyZoomInButton = view.profilingDependencyZoomInButton;
        this.profilingDependencyFitButton = view.profilingDependencyFitButton;
        this.profilingDependencyTable = view.profilingDependencyTable;
        this.profilingDependencyGraphScrollPane = view.profilingDependencyGraphScrollPane;
        this.profilingDependencyGraphContainer = view.profilingDependencyGraphContainer;
        this.profilingCallersFlameTab = view.profilingCallersFlameTab;
        this.profilingCallersFlameToolbar = view.profilingCallersFlameToolbar;
        this.profilingCallersFlameOrientationButton = view.profilingCallersFlameOrientationButton;
        this.profilingCallersFlameZoomOutButton = view.profilingCallersFlameZoomOutButton;
        this.profilingCallersFlameResetZoomButton = view.profilingCallersFlameResetZoomButton;
        this.profilingCallersFlameZoomInButton = view.profilingCallersFlameZoomInButton;
        this.profilingCallersFlameFitButton = view.profilingCallersFlameFitButton;
        this.profilingCallersFlameContainer = view.profilingCallersFlameContainer;
        this.profilingCalleesFlameTab = view.profilingCalleesFlameTab;
        this.profilingCalleesFlameToolbar = view.profilingCalleesFlameToolbar;
        this.profilingCalleesFlameOrientationButton = view.profilingCalleesFlameOrientationButton;
        this.profilingCalleesFlameZoomOutButton = view.profilingCalleesFlameZoomOutButton;
        this.profilingCalleesFlameResetZoomButton = view.profilingCalleesFlameResetZoomButton;
        this.profilingCalleesFlameZoomInButton = view.profilingCalleesFlameZoomInButton;
        this.profilingCalleesFlameFitButton = view.profilingCalleesFlameFitButton;
        this.profilingCalleesFlameContainer = view.profilingCalleesFlameContainer;
        this.profilingCallersTab = view.profilingCallersTab;
        this.profilingCallersTree = view.profilingCallersTree;
        this.profilingCalleesTab = view.profilingCalleesTab;
        this.profilingCalleesTree = view.profilingCalleesTree;
        this.exceptionsTitleLabel = view.exceptionsTitleLabel;
        this.exceptionsGroupByClass = view.exceptionsGroupByClass;
        this.exceptionsGroupByMessage = view.exceptionsGroupByMessage;
        this.exceptionsGroupByClassAndMessage = view.exceptionsGroupByClassAndMessage;
        this.exceptionsTable = view.exceptionsTable;
        this.exceptionsTimelineContainer = view.exceptionsTimelineContainer;
        this.threadsTitleLabel = view.threadsTitleLabel;
        this.threadsTable = view.threadsTable;
        this.fileioTitleLabel = view.fileioTitleLabel;
        this.fileioTabPane = view.fileioTabPane;
        this.fileioTimelineTab = view.fileioTimelineTab;
        this.fileioTimelineContainer = view.fileioTimelineContainer;
        this.fileioDurationTab = view.fileioDurationTab;
        this.fileioHistogramTable = view.fileioHistogramTable;
        this.fileioEventLogTab = view.fileioEventLogTab;
        this.fileioEventTable = view.fileioEventTable;
        this.socketioTitleLabel = view.socketioTitleLabel;
        this.socketioGroupingBar = view.socketioGroupingBar;
        this.socketioGroupByHostAndPort = view.socketioGroupByHostAndPort;
        this.socketioGroupByHost = view.socketioGroupByHost;
        this.socketioGroupByPort = view.socketioGroupByPort;
        this.socketioTabPane = view.socketioTabPane;
        this.socketioTimelineTab = view.socketioTimelineTab;
        this.socketioTimelineContainer = view.socketioTimelineContainer;
        this.socketioDurationTab = view.socketioDurationTab;
        this.socketioHistogramTable = view.socketioHistogramTable;
        this.socketioEventLogTab = view.socketioEventLogTab;
        this.socketioEventTable = view.socketioEventTable;
        this.locksTitleLabel = view.locksTitleLabel;
        this.locksGroupingBar = view.locksGroupingBar;
        this.locksGroupByClass = view.locksGroupByClass;
        this.locksGroupByAddress = view.locksGroupByAddress;
        this.locksGroupByThread = view.locksGroupByThread;
        this.locksTabPane = view.locksTabPane;
        this.locksByClassTab = view.locksByClassTab;
        this.locksByClassTable = view.locksByClassTable;
        this.locksByAddressTab = view.locksByAddressTab;
        this.locksByAddressTable = view.locksByAddressTable;
        this.locksByThreadTab = view.locksByThreadTab;
        this.locksByThreadTable = view.locksByThreadTable;
        this.threadHistogramTitleLabel = view.threadHistogramTitleLabel;
        this.threadHistogramChartContainer = view.threadHistogramChartContainer;
        this.threadHistogramTable = view.threadHistogramTable;
        this.securityTitleLabel = view.securityTitleLabel;
        this.securityTable = view.securityTable;
        this.nativeLibrariesTitleLabel = view.nativeLibrariesTitleLabel;
        this.nativeLibrariesTable = view.nativeLibrariesTable;
        this.threadDumpsTitleLabel = view.threadDumpsTitleLabel;
        this.threadDumpsTable = view.threadDumpsTable;
        this.threadDumpTextArea = view.threadDumpTextArea;
        this.heapTitleLabel = view.heapTitleLabel;
        this.heapTable = view.heapTable;
        this.heapTimelineContainer = view.heapTimelineContainer;
        this.leaksTitleLabel = view.leaksTitleLabel;
        this.leaksTable = view.leaksTable;
        this.leaksReferenceTree = view.leaksReferenceTree;
        this.tlabTitleLabel = view.tlabTitleLabel;
        this.tlabTable = view.tlabTable;
        this.tlabTimelineContainer = view.tlabTimelineContainer;
        this.jvmFlagsTable = view.jvmFlagsTable;
        this.jvmFlagChangesTable = view.jvmFlagChangesTable;
        this.gcSummaryTable = view.gcSummaryTable;
        this.gcEventsTable = view.gcEventsTable;
        this.gcReferenceStatsTable = view.gcReferenceStatsTable;
        this.gcHeapSummaryTable = view.gcHeapSummaryTable;
        this.g1GcTitleLabel = view.g1GcTitleLabel;
        this.g1GcSummaryLabel = view.g1GcSummaryLabel;
        this.g1GcRegionStatesLabel = view.g1GcRegionStatesLabel;
        this.g1GcRegionSummaryLabel = view.g1GcRegionSummaryLabel;
        this.g1GcPausesLabel = view.g1GcPausesLabel;
        this.g1GcRegionSummaryTable = view.g1GcRegionSummaryTable;
        this.g1GcRegionStatesTable = view.g1GcRegionStatesTable;
        this.g1GcPauseTable = view.g1GcPauseTable;
        this.g1GcDetailTitleLabel = view.g1GcDetailTitleLabel;
        this.g1GcDetailArea = view.g1GcDetailArea;
        this.javaFxEventsTitleLabel = view.javaFxEventsTitleLabel;
        this.javaFxEventsSummaryLabel = view.javaFxEventsSummaryLabel;
        this.javaFxEventsPhaseLabel = view.javaFxEventsPhaseLabel;
        this.javaFxEventsPulseLabel = view.javaFxEventsPulseLabel;
        this.javaFxEventsInputLabel = view.javaFxEventsInputLabel;
        this.javaFxEventsPhaseTable = view.javaFxEventsPhaseTable;
        this.javaFxEventsPulseTable = view.javaFxEventsPulseTable;
        this.javaFxEventsInputTable = view.javaFxEventsInputTable;
        this.javaFxEventsDetailTitleLabel = view.javaFxEventsDetailTitleLabel;
        this.javaFxEventsDetailArea = view.javaFxEventsDetailArea;
        this.gcHeapChartContainer = view.gcHeapChartContainer;
        this.gcMetaspaceChartContainer = view.gcMetaspaceChartContainer;
        this.gcPauseChartContainer = view.gcPauseChartContainer;
        this.compilationsTable = view.compilationsTable;
        this.compilationDurationChartContainer = view.compilationDurationChartContainer;
        this.compilationFailuresTable = view.compilationFailuresTable;
        this.codeCacheSweepsTable = view.codeCacheSweepsTable;
        this.codeCacheEntriesChartContainer = view.codeCacheEntriesChartContainer;
        this.codeCacheSweepChartContainer = view.codeCacheSweepChartContainer;
        this.codeCacheStatsTable = view.codeCacheStatsTable;
        this.classLoadingHistogramTable = view.classLoadingHistogramTable;
        this.classLoadingChartContainer = view.classLoadingChartContainer;
        this.classLoadingEventsTable = view.classLoadingEventsTable;
        this.classLoadingStatsTable = view.classLoadingStatsTable;
        this.vmOperationSummaryTable = view.vmOperationSummaryTable;
        this.vmOperationEventsTable = view.vmOperationEventsTable;
        this.processesTable = view.processesTable;
        this.envVarsTable = view.envVarsTable;
        this.envVarsSearchField = view.envVarsSearchField;
        this.sysPropsTable = view.sysPropsTable;
        this.sysPropsSearchField = view.sysPropsSearchField;
        this.recordingsTable = view.recordingsTable;
        this.settingsTable = view.settingsTable;
        this.recordingInfoTabs = view.recordingInfoTabs;
        this.recordingInfoRecordingsTab = view.recordingInfoRecordingsTab;
        this.recordingInfoSettingsTab = view.recordingInfoSettingsTab;
        this.agentsTable = view.agentsTable;
        this.constantPoolsTable = view.constantPoolsTable;
        this.jvmInfoTitleLabel = view.jvmInfoTitleLabel;
        this.jvmFlagsLabel = view.jvmFlagsLabel;
        this.jvmFlagChangesLabel = view.jvmFlagChangesLabel;
        this.gcConfigTitleLabel = view.gcConfigTitleLabel;
        this.gcConfigDescriptionLabel = view.gcConfigDescriptionLabel;
        this.gcSummaryTitleLabel = view.gcSummaryTitleLabel;
        this.gcDetailsTitleLabel = view.gcDetailsTitleLabel;
        this.gcEventsLabel = view.gcEventsLabel;
        this.gcReferenceStatsLabel = view.gcReferenceStatsLabel;
        this.gcHeapSummaryLabel = view.gcHeapSummaryLabel;
        this.compilationsTitleLabel = view.compilationsTitleLabel;
        this.compilationEventsLabel = view.compilationEventsLabel;
        this.compilationFailuresLabel = view.compilationFailuresLabel;
        this.codeCacheTitleLabel = view.codeCacheTitleLabel;
        this.codeCacheSweepsLabel = view.codeCacheSweepsLabel;
        this.codeCacheStatsLabel = view.codeCacheStatsLabel;
        this.classLoadingTitleLabel = view.classLoadingTitleLabel;
        this.classLoadingHistogramLabel = view.classLoadingHistogramLabel;
        this.classLoadingEventsLabel = view.classLoadingEventsLabel;
        this.classLoadingStatsLabel = view.classLoadingStatsLabel;
        this.vmOperationsTitleLabel = view.vmOperationsTitleLabel;
        this.vmOperationSummaryLabel = view.vmOperationSummaryLabel;
        this.vmOperationEventsLabel = view.vmOperationEventsLabel;
        this.processesTitleLabel = view.processesTitleLabel;
        this.envVarsTitleLabel = view.envVarsTitleLabel;
        this.sysPropsTitleLabel = view.sysPropsTitleLabel;
        this.recordingInfoTitleLabel = view.recordingInfoTitleLabel;
        this.agentsTitleLabel = view.agentsTitleLabel;
        this.constantPoolsTitleLabel = view.constantPoolsTitleLabel;
        this.settingsTitleLabel = view.settingsTitleLabel;
        this.settingsLanguageLabel = view.settingsLanguageLabel;
        this.languageToggleGroup = view.languageToggleGroup;
        this.languageFollowSystemRadio = view.languageFollowSystemRadio;
        this.languageEnglishRadio = view.languageEnglishRadio;
        this.languageChineseRadio = view.languageChineseRadio;
        this.settingsThemeLabel = view.settingsThemeLabel;
        this.themeToggleGroup = view.themeToggleGroup;
        this.themeFollowSystemRadio = view.themeFollowSystemRadio;
        this.themeLightRadio = view.themeLightRadio;
        this.themeDarkRadio = view.themeDarkRadio;
    }

    I18n i18n() {
        return i18n;
    }

    SavedJvmTargetRepository savedTargetRepository() {
        return savedTargetRepository;
    }

    JdpDiscoveryService jdpDiscoveryService() {
        return jdpDiscoveryService;
    }

    JmxMonitoringService jmxMonitoringService() {
        return jmxMonitoringService;
    }

    JmxMonitoringRepository jmxMonitoringRepository() {
        return jmxMonitoringRepository;
    }

    void initialize() {
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        sidebar.bind(viewModel);
        sidebar.setNavigationHandler(viewModel::showSection);
        sidebar.setI18n(i18n);
        viewModel.selectedSectionProperty()
                .addListener((observable, oldValue, newValue) -> loadSelectedWorkspaceSection());
        bindLocalizedText();
        configureActionIcons();
        configureLanguageSelector();
        configureThemeSelector();
        homeOpenRecordingButton.setOnAction(event -> openRecording());
        homeOpenHeapDumpButton.setOnAction(event -> showOpenHeapDumpChooser());
        homeConnectJvmButton.setOnAction(event -> viewModel.openLiveJvmWorkspace());
        homeJfrTile.setOnMouseClicked(event -> openRecording());
        homeHeapDumpTile.setOnMouseClicked(event -> showOpenHeapDumpChooser());
        homeJvmTile.setOnMouseClicked(event -> viewModel.openLiveJvmWorkspace());
        configureJavaApplicationOverviewActions();
        configureJvmInternalsOverviewActions();
        configureEnvironmentOverviewActions();
        configureRecordingTabs();
        jvmBrowserViewModel = jvmDiscoveryService != null && jmxConnectionService != null
                ? new JvmBrowserViewModel(jvmDiscoveryService, jmxConnectionService, flightRecordingService,
                        mBeanBrowserService, diagnosticCommandService, liveMetricService,
                        jmcAgentService, jmxMonitoringService, jmxMonitoringRepository, savedTargetRepository,
                        jdpDiscoveryService,
                        new com.youngledo.jmcfx.ui.jvms.VirtualThreadJvmBrowserExecutor(), Platform::runLater,
                        this::openRecordingInBackground) : null;
        heapDumpAnalysisViewModel = heapDumpAnalysisService == null ? null
                : new HeapDumpAnalysisViewModel(heapDumpAnalysisService,
                        new VirtualThreadHeapDumpAnalysisExecutor(), i18n);
        jvmsPaneController = new LiveJvmPaneController();
        VBox liveJvmRoot = jvmsPaneController.root();
        VBox.setVgrow(liveJvmRoot, Priority.ALWAYS);
        jvmsPaneHost.getChildren().setAll(liveJvmRoot);
        homePane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("home"));
        homePane.managedProperty().bind(homePane.visibleProperty());
        overviewPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("overview"));
        overviewPane.managedProperty().bind(overviewPane.visibleProperty());
        eventsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("events"));
        eventsPane.managedProperty().bind(eventsPane.visibleProperty());
        analysisPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("analysis"));
        analysisPane.managedProperty().bind(analysisPane.visibleProperty());
        metadataPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("metadata"));
        metadataPane.managedProperty().bind(metadataPane.visibleProperty());
        advancedJfrPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("advancedJfr"));
        advancedJfrPane.managedProperty().bind(advancedJfrPane.visibleProperty());
        heapDumpAnalysisPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("heapDumpAnalysis"));
        heapDumpAnalysisPane.managedProperty().bind(heapDumpAnalysisPane.visibleProperty());
        jvmsPaneHost.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("jvms"));
        jvmsPaneHost.managedProperty().bind(jvmsPaneHost.visibleProperty());
        javaApplicationPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("javaApplication"));
        javaApplicationPane.managedProperty().bind(javaApplicationPane.visibleProperty());
        jvmInternalsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("jvmInternals"));
        jvmInternalsPane.managedProperty().bind(jvmInternalsPane.visibleProperty());
        environmentPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("environment"));
        environmentPane.managedProperty().bind(environmentPane.visibleProperty());
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
        threadHistogramPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("threadHistogram"));
        threadHistogramPane.managedProperty().bind(threadHistogramPane.visibleProperty());
        securityPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("security"));
        securityPane.managedProperty().bind(securityPane.visibleProperty());
        nativeLibrariesPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("nativeLibraries"));
        nativeLibrariesPane.managedProperty().bind(nativeLibrariesPane.visibleProperty());
        threadDumpsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("threadDumps"));
        threadDumpsPane.managedProperty().bind(threadDumpsPane.visibleProperty());
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
        g1GcPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("g1Gc"));
        g1GcPane.managedProperty().bind(g1GcPane.visibleProperty());
        javaFxEventsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("javaFxEvents"));
        javaFxEventsPane.managedProperty().bind(javaFxEventsPane.visibleProperty());
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
        initializeCharts();
        bindOverview(null);
        bindEvents();
        configureAnalysisTable();
        configureMetadataTable();
        configureAdvancedJfrMemoryTable();
        configureHeapDumpAnalysis();
        if (jvmsPaneController != null) {
            jvmsPaneController.configure(i18n, jvmBrowserViewModel);
        }
        viewModel.selectedSectionProperty().addListener((observable, oldValue, newValue) -> {
            if ("jvms".equals(newValue) && jvmsPaneController != null) {
                jvmsPaneController.refresh();
            }
        });
        if ("jvms".equals(viewModel.selectedSectionProperty().get()) && jvmsPaneController != null) {
            jvmsPaneController.refresh();
        }
        configureProfilingTable();
        configureExceptionTable();
        configureThreadTable();
        configureFileIOTable();
        configureSocketIOTable();
        configureLockTables();
        configureThreadHistogramTable();
        configureSecurityTable();
        configureNativeLibrariesTable();
        configureThreadDumpsTable();
        configureHeapTable();
        configureLeaksTable();
        configureTlabTable();
        configureJvmFlagsTable();
        configureJvmFlagChangesTable();
        configureGcSummaryTable();
        configureGcEventsTable();
        configureGcReferenceStatsTable();
        configureGcHeapSummaryTable();
        configureG1GcTables();
        configureJavaFxEventsTables();
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
        attachExportMenus();
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

    private void initializeCharts() {
        advancedJfrHeatmapView = new EventHeatmapView();
        advancedJfrHeatmapContainer.getChildren().setAll(advancedJfrHeatmapView);
        exceptionsTimelineChart = addChart(exceptionsTimelineContainer);
        fileioTimelineChart = addChart(fileioTimelineContainer);
        socketioTimelineChart = addChart(socketioTimelineContainer);
        heapTimelineChart = addChart(heapTimelineContainer);
        tlabTimelineChart = addChart(tlabTimelineContainer);
        threadHistogramChart = addChart(threadHistogramChartContainer);
        gcHeapChart = addChart(gcHeapChartContainer);
        gcMetaspaceChart = addChart(gcMetaspaceChartContainer);
        gcPauseChart = addChart(gcPauseChartContainer);
        compilationDurationChart = addChart(compilationDurationChartContainer);
        codeCacheEntriesChart = addChart(codeCacheEntriesChartContainer);
        codeCacheSweepChart = addChart(codeCacheSweepChartContainer);
        classLoadingChart = addChart(classLoadingChartContainer);
    }

    private TimelineChart addChart(VBox container) {
        TimelineChart chart = new TimelineChart();
        container.getChildren().add(chart);
        return chart;
    }

    private static <T> void useFormattedIntegerCells(TableColumn<T, Number> column) {
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : DisplayFormats.formatInteger(item.longValue()));
            }
        });
    }

    private Label localizedTablePlaceholder(String key) {
        Label label = new Label();
        label.textProperty().bind(i18n.text(key));
        return label;
    }

    private <T> TableColumn<T, String> localizedColumn(String key) {
        TableColumn<T, String> column = new TableColumn<>();
        column.textProperty().bind(i18n.text(key));
        return column;
    }

    private RuleResultsViewModel analysisViewModel;
    private javafx.beans.property.ObjectProperty<Integer> analysisMinimumScoreBinding;

    private void configureAnalysisTable() {
        analysisTable.setPlaceholder(localizedTablePlaceholder("analysis.empty"));
        analysisMinimumScoreSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(-3, 100, 0));

        TableColumn<RuleResult, Severity> severityCol = new TableColumn<>();
        severityCol.textProperty().bind(i18n.text("analysis.column.severity"));
        severityCol.setPrefWidth(80);
        severityCol.setId("severity");
        severityCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().severity()));
        severityCol.setCellFactory(col -> new AnalysisSeverityCell<>());

        TableColumn<RuleResult, Number> scoreCol = new TableColumn<>();
        scoreCol.textProperty().bind(i18n.text("analysis.column.score"));
        scoreCol.setPrefWidth(72);
        scoreCol.setId("score");
        scoreCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().score()));
        useFormattedIntegerCells(scoreCol);

        TableColumn<RuleResult, String> pageCol = localizedColumn("analysis.column.rulePage");
        pageCol.setPrefWidth(140);
        pageCol.setId("rulePage");
        pageCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().topic()));

        TableColumn<RuleResult, String> resultIdCol = localizedColumn("analysis.column.resultId");
        resultIdCol.setPrefWidth(180);
        resultIdCol.setId("resultId");
        resultIdCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().id()));

        TableColumn<RuleResult, String> nameCol = localizedColumn("analysis.column.name");
        nameCol.setPrefWidth(280);
        nameCol.setId("rule");
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));

        TableColumn<RuleResult, String> summaryCol = localizedColumn("analysis.column.summary");
        summaryCol.setPrefWidth(520);
        summaryCol.setId("summary");
        summaryCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().summary()));

        analysisTable.getColumns().setAll(List.of(severityCol, scoreCol, pageCol, resultIdCol, nameCol, summaryCol));
        analysisTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, val) -> {
                    if (analysisViewModel != null) {
                        analysisViewModel.selectedResultProperty().set(val);
                    }
                    showAnalysisDetail(val);
                });
        analysisTable.setRowFactory(table -> {
            TableRow<RuleResult> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2
                        && !row.isEmpty()) {
                    openAnalysisRelatedPage(row.getItem());
                }
            });
            return row;
        });
    }

    private void configureAdvancedJfrMemoryTable() {
        advancedJfrMemoryTable.setPlaceholder(localizedTablePlaceholder("advancedJfr.memory.empty"));

        TableColumn<MemoryIssue, String> severityCol = localizedColumn("advancedJfr.memory.column.severity");
        severityCol.setPrefWidth(110);
        severityCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().severity().name()));

        TableColumn<MemoryIssue, String> categoryCol = localizedColumn("advancedJfr.memory.column.category");
        categoryCol.setPrefWidth(180);
        categoryCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().category().name()));

        TableColumn<MemoryIssue, String> subjectCol = localizedColumn("advancedJfr.memory.column.subject");
        subjectCol.setPrefWidth(360);
        subjectCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().subject()));

        TableColumn<MemoryIssue, String> estimatedBytesCol =
                localizedColumn("advancedJfr.memory.column.estimatedBytes");
        estimatedBytesCol.setPrefWidth(140);
        estimatedBytesCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(DisplayFormats.formatFileSize(cell.getValue().estimatedBytes())));

        TableColumn<MemoryIssue, String> countLabelCol =
                localizedColumn("advancedJfr.memory.column.count");
        TableColumn<MemoryIssue, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(countLabelCol.textProperty());
        countCol.setPrefWidth(100);
        countCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleLongProperty(cell.getValue().count()));
        useFormattedIntegerCells(countCol);

        TableColumn<MemoryIssue, String> scoreCol = localizedColumn("advancedJfr.memory.column.score");
        scoreCol.setPrefWidth(90);
        scoreCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(DisplayFormats.formatPercent(cell.getValue().score())));

        advancedJfrMemoryTable.getColumns().setAll(List.of(severityCol, categoryCol, subjectCol,
                estimatedBytesCol, countCol, scoreCol));
        advancedJfrMemoryTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldIssue, issue) -> {
                    if (!rebindingAdvancedJfrMemory && advancedJfrViewModel != null) {
                        advancedJfrViewModel.selectMemoryIssue(issue);
                    }
                });
    }

    private void configureMetadataTable() {
        metadataEventTypesTable.setPlaceholder(localizedTablePlaceholder("metadata.empty"));

        TableColumn<JfrMetadataEventType, String> categoryCol = localizedColumn("metadata.column.category");
        categoryCol.setPrefWidth(220);
        categoryCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().category()));

        TableColumn<JfrMetadataEventType, String> nameCol = localizedColumn("metadata.column.name");
        nameCol.setPrefWidth(240);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));

        TableColumn<JfrMetadataEventType, String> idCol = localizedColumn("metadata.column.id");
        idCol.setPrefWidth(280);
        idCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().id()));

        TableColumn<JfrMetadataEventType, String> eventCountLabelCol =
                localizedColumn("metadata.column.eventCount");
        TableColumn<JfrMetadataEventType, Number> eventCountCol = new TableColumn<>();
        eventCountCol.textProperty().bind(eventCountLabelCol.textProperty());
        eventCountCol.setPrefWidth(100);
        eventCountCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleLongProperty(cell.getValue().eventCount()));
        useFormattedIntegerCells(eventCountCol);

        TableColumn<JfrMetadataEventType, String> fieldCountLabelCol =
                localizedColumn("metadata.column.fieldCount");
        TableColumn<JfrMetadataEventType, Number> fieldCountCol = new TableColumn<>();
        fieldCountCol.textProperty().bind(fieldCountLabelCol.textProperty());
        fieldCountCol.setPrefWidth(90);
        fieldCountCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleIntegerProperty(cell.getValue().fieldCount()));
        useFormattedIntegerCells(fieldCountCol);

        metadataEventTypesTable.getColumns().setAll(List.of(categoryCol, nameCol, idCol,
                eventCountCol, fieldCountCol));
        metadataEventTypesTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, eventType) -> {
                    if (jfrMetadataViewModel != null) {
                        jfrMetadataViewModel.selectedEventTypeProperty().set(eventType);
                    }
                });
    }

    private void configureHeapDumpAnalysis() {
        heapDumpIssuesTable.setPlaceholder(localizedTablePlaceholder("heapDump.openPrompt"));

        TableColumn<HeapDumpIssue, String> categoryCol = localizedColumn("heapDump.column.category");
        categoryCol.setPrefWidth(190);
        categoryCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().category().name()));

        TableColumn<HeapDumpIssue, String> subjectCol = localizedColumn("heapDump.column.subject");
        subjectCol.setPrefWidth(440);
        subjectCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().subject()));

        TableColumn<HeapDumpIssue, String> wastedBytesCol = localizedColumn("heapDump.column.wastedBytes");
        wastedBytesCol.setPrefWidth(120);
        wastedBytesCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(DisplayFormats.formatFileSize(cell.getValue().wastedBytes())));

        TableColumn<HeapDumpIssue, Number> objectCountCol = new TableColumn<>();
        objectCountCol.textProperty().bind(i18n.text("heapDump.column.objectCount"));
        objectCountCol.setPrefWidth(110);
        objectCountCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleLongProperty(cell.getValue().objectCount()));
        useFormattedIntegerCells(objectCountCol);

        TableColumn<HeapDumpIssue, String> scoreCol = localizedColumn("heapDump.column.score");
        scoreCol.setPrefWidth(90);
        scoreCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(DisplayFormats.formatPercent(cell.getValue().score() * 100.0)));

        heapDumpIssuesTable.getColumns().setAll(List.of(categoryCol, subjectCol, wastedBytesCol,
                objectCountCol, scoreCol));
        if (heapDumpAnalysisViewModel == null) {
            heapDumpIssueDetailTitleLabel.setText("");
            heapDumpIssueDetailArea.setText(i18n.get("heapDump.detail.empty"));
            heapDumpTextReportArea.setText("");
            heapDumpIssuesTable.setItems(FXCollections.emptyObservableList());
            return;
        }
        bindHeapDumpAnalysis(heapDumpAnalysisViewModel);
    }

    private void bindHeapDumpAnalysis(HeapDumpAnalysisViewModel nextViewModel) {
        if (heapDumpAnalysisViewModel != null) {
            heapDumpIssuesTable.getSelectionModel().selectedItemProperty()
                    .removeListener(heapDumpTableSelectionListener);
            heapDumpAnalysisViewModel.selectedIssueProperty().removeListener(heapDumpSelectedIssueListener);
        }
        heapDumpIssueDetailArea.textProperty().unbind();
        heapDumpTextReportArea.textProperty().unbind();
        heapDumpIssueDetailTitleLabel.textProperty().unbind();
        if (nextViewModel == null) {
            heapDumpAnalysisViewModel = null;
            heapDumpIssueDetailArea.setText(i18n.get("heapDump.detail.empty"));
            heapDumpTextReportArea.setText("");
            heapDumpIssuesTable.setItems(FXCollections.emptyObservableList());
            heapDumpIssueDetailTitleLabel.setText("");
            return;
        }
        heapDumpAnalysisViewModel = nextViewModel;
        heapDumpIssueDetailArea.textProperty().bind(heapDumpAnalysisViewModel.selectedIssueDetailsProperty());
        heapDumpTextReportArea.textProperty().bind(heapDumpAnalysisViewModel.textReportProperty());
        heapDumpIssuesTable.setItems(heapDumpAnalysisViewModel.issues());
        heapDumpIssuesTable.getSelectionModel().selectedItemProperty()
                .addListener(heapDumpTableSelectionListener);
        heapDumpAnalysisViewModel.selectedIssueProperty().addListener(heapDumpSelectedIssueListener);
        heapDumpIssueDetailTitleLabel.textProperty().bind(Bindings.createStringBinding(
                () -> {
                    HeapDumpIssue issue = heapDumpAnalysisViewModel.selectedIssueProperty().get();
                    return issue == null ? "" : issue.subject();
                },
                heapDumpAnalysisViewModel.selectedIssueProperty()));
    }

    private void selectHeapDumpIssue(HeapDumpIssue issue) {
        HeapDumpAnalysisViewModel viewModel = heapDumpAnalysisViewModel;
        if (viewModel != null) {
            viewModel.selectIssue(issue);
        }
    }

    private void selectHeapDumpIssueInTable(HeapDumpIssue issue) {
        if (heapDumpIssuesTable != null) {
            heapDumpIssuesTable.getSelectionModel().select(issue);
        }
    }

    private void showOpenHeapDumpChooser() {
        if (heapDumpAnalysisService == null || root == null || root.getScene() == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(i18n.get("heapDump.fileChooser.title"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(i18n.get("heapDump.fileChooser.hprof"), "*.hprof"));
        java.io.File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            openHeapDumpInBackground(file.toPath());
        }
    }

    private void bindAnalysis(RuleResultsViewModel nextViewModel) {
        analysisTable.placeholderProperty().unbind();
        analysisTable.setItems(FXCollections.emptyObservableList());
        if (analysisViewModel != null) {
            analysisSearchField.textProperty().unbindBidirectional(analysisViewModel.searchTextProperty());
            if (analysisMinimumScoreBinding != null) {
                analysisMinimumScoreSpinner.getValueFactory().valueProperty().unbindBidirectional(
                        analysisMinimumScoreBinding);
                analysisMinimumScoreBinding = null;
            }
            analysisShowOkCheckBox.selectedProperty().unbindBidirectional(analysisViewModel.showOkResultsProperty());
            analysisShowIgnoredCheckBox.selectedProperty().unbindBidirectional(
                    analysisViewModel.showIgnoredResultsProperty());
            analysisShowUnavailableCheckBox.selectedProperty().unbindBidirectional(
                    analysisViewModel.showUnavailableResultsProperty());
        }
        showAnalysisDetail(null);
        analysisViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        analysisTable.setItems(nextViewModel.resultsProperty());
        analysisSearchField.textProperty().bindBidirectional(nextViewModel.searchTextProperty());
        analysisMinimumScoreBinding = nextViewModel.minimumScoreProperty().asObject();
        analysisMinimumScoreSpinner.getValueFactory().valueProperty().bindBidirectional(
                analysisMinimumScoreBinding);
        analysisShowOkCheckBox.selectedProperty().bindBidirectional(nextViewModel.showOkResultsProperty());
        analysisShowIgnoredCheckBox.selectedProperty().bindBidirectional(nextViewModel.showIgnoredResultsProperty());
        analysisShowUnavailableCheckBox.selectedProperty().bindBidirectional(
                nextViewModel.showUnavailableResultsProperty());
        analysisTable.placeholderProperty().bind(Bindings.createObjectBinding(
                () -> analysisPlaceholder(nextViewModel),
                nextViewModel.loadingProperty(),
                nextViewModel.loadedProperty(),
                nextViewModel.errorProperty(),
                nextViewModel.errorMessageProperty(),
                i18n.localeProperty()));
        analysisTable.getSelectionModel().selectFirst();
    }

    private Label analysisPlaceholder(RuleResultsViewModel viewModel) {
        if (viewModel.loadingProperty().get()) {
            return localizedTablePlaceholder("analysis.loading");
        }
        if (viewModel.errorProperty().get()) {
            Label label = new Label();
            label.setText(i18n.format("analysis.failed", viewModel.errorMessageProperty().get()));
            return label;
        }
        return localizedTablePlaceholder(viewModel.loadedProperty().get()
                ? "analysis.empty" : "analysis.loading");
    }

    private void showAnalysisDetail(RuleResult result) {
        RuleResultDetail detail = RuleResultDetail.from(result);
        if (detail == null) {
            analysisDetailExplanationArea.setText("");
            analysisDetailEvidenceArea.setText("");
            analysisDetailRecommendationArea.setText("");
            return;
        }
        analysisDetailExplanationArea.setText(detail.explanation());
        analysisDetailEvidenceArea.setText(detail.evidence());
        analysisDetailRecommendationArea.setText(detail.recommendation());
    }

    private void openAnalysisRelatedPage(RuleResult result) {
        RuleResultDetail detail = RuleResultDetail.from(result);
        if (detail != null && detail.hasRelatedPage()) {
            viewModel.showSection(detail.relatedPageId());
        }
    }

    private void configureProfilingTable() {
        profilingTable.setPlaceholder(localizedTablePlaceholder("profiling.empty"));
        profilingCallGraphView = new CallGraphView();
        profilingDependencyGraphView = new CallGraphView();
        profilingCallersFlameGraphView = new FlameGraphView();
        profilingCalleesFlameGraphView = new FlameGraphView();
        profilingCallGraphView.emptyTextProperty().bind(i18n.text("profiling.callGraph.empty"));
        profilingDependencyGraphView.emptyTextProperty().bind(i18n.text("profiling.dependency.empty"));
        profilingCallersFlameGraphView.emptyTextProperty().bind(i18n.text("profiling.flame.empty"));
        profilingCalleesFlameGraphView.emptyTextProperty().bind(i18n.text("profiling.flame.empty"));
        profilingCallGraphContainer.getChildren().setAll(profilingCallGraphView);
        profilingDependencyGraphContainer.getChildren().setAll(profilingDependencyGraphView);
        profilingCallersFlameContainer.getChildren().setAll(profilingCallersFlameGraphView);
        profilingCalleesFlameContainer.getChildren().setAll(profilingCalleesFlameGraphView);
        profilingCallGraphDirectionCombo.setItems(FXCollections.observableArrayList(CallGraphDirection.values()));
        profilingCallGraphDirectionCombo.setButtonCell(callGraphDirectionCell());
        profilingCallGraphDirectionCombo.setCellFactory(combo -> callGraphDirectionCell());
        profilingCallGraphDirectionCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(CallGraphDirection direction) {
                return formatCallGraphDirection(direction);
            }

            @Override
            public CallGraphDirection fromString(String value) {
                return null;
            }
        });
        i18n.localeProperty().addListener((obs, old, val) -> refreshProfilingCallGraphDirectionLabel());
        profilingCallGraphDirectionCombo.getSelectionModel().select(CallGraphDirection.CALLEES);
        profilingCallGraphDirectionCombo.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, val) -> {
                    if (profilingViewModel != null && val != null) {
                        profilingViewModel.setCallGraphDirection(val);
                    }
                });
        profilingCallGraphDepthSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1, 6, CallGraphLayoutBuilder.DEFAULT_MAX_DEPTH));
        profilingCallGraphDepthSpinner.valueProperty()
                .addListener((obs, old, val) -> {
                    if (profilingViewModel != null && val != null) {
                        profilingViewModel.setCallGraphMaxDepth(val);
                    }
                });
        configureGraphZoomButtons(profilingCallGraphView,
                profilingCallGraphZoomOutButton,
                profilingCallGraphResetZoomButton,
                profilingCallGraphZoomInButton,
                profilingCallGraphFitButton);
        configureCallGraphGestures(profilingCallGraphView, profilingCallGraphScrollPane);
        profilingDependencyDepthSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 6, 2));
        profilingDependencyDepthSpinner.valueProperty()
                .addListener((obs, old, val) -> {
                    if (profilingViewModel != null && val != null) {
                        profilingViewModel.setDependencyPackageDepth(val);
                    }
                });
        configureGraphZoomButtons(profilingDependencyGraphView,
                profilingDependencyZoomOutButton,
                profilingDependencyResetZoomButton,
                profilingDependencyZoomInButton,
                profilingDependencyFitButton);
        configureCallGraphGestures(profilingDependencyGraphView, profilingDependencyGraphScrollPane);
        configureFlameGraphButtons(profilingCallersFlameGraphView,
                profilingCallersFlameOrientationButton,
                profilingCallersFlameZoomOutButton,
                profilingCallersFlameResetZoomButton,
                profilingCallersFlameZoomInButton,
                profilingCallersFlameFitButton);
        bindFlameGraphToolbarVisibility(profilingCallersFlameToolbar, profilingCallersFlameGraphView);
        configureFlameGraphButtons(profilingCalleesFlameGraphView,
                profilingCalleesFlameOrientationButton,
                profilingCalleesFlameZoomOutButton,
                profilingCalleesFlameResetZoomButton,
                profilingCalleesFlameZoomInButton,
                profilingCalleesFlameFitButton);
        bindFlameGraphToolbarVisibility(profilingCalleesFlameToolbar, profilingCalleesFlameGraphView);

        TableColumn<HotMethod, String> methodCol = new TableColumn<>();
        methodCol.textProperty().bind(i18n.text("profiling.column.method"));
        methodCol.setPrefWidth(620);
        methodCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().method()));

        TableColumn<HotMethod, String> frameTypeCol = new TableColumn<>();
        frameTypeCol.textProperty().bind(i18n.text("profiling.column.frameType"));
        frameTypeCol.setPrefWidth(100);
        frameTypeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().frameType()));

        TableColumn<HotMethod, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("profiling.column.count"));
        countCol.setPrefWidth(80);
        countCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().count()));
        useFormattedIntegerCells(countCol);

        TableColumn<HotMethod, String> pctCol = new TableColumn<>();
        pctCol.textProperty().bind(i18n.text("profiling.column.percentage"));
        pctCol.setPrefWidth(80);
        pctCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatPercent(cell.getValue().percentage())));

        profilingTable.getColumns().setAll(List.of(methodCol, frameTypeCol, countCol, pctCol));
        profilingTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, val) -> selectProfilingMethod(val));

        TableColumn<DependencyGraphEdge, String> sourceCol = new TableColumn<>();
        sourceCol.textProperty().bind(i18n.text("profiling.dependency.column.source"));
        sourceCol.setPrefWidth(150);
        sourceCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().source()));

        TableColumn<DependencyGraphEdge, String> targetCol = new TableColumn<>();
        targetCol.textProperty().bind(i18n.text("profiling.dependency.column.target"));
        targetCol.setPrefWidth(150);
        targetCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().target()));

        TableColumn<DependencyGraphEdge, Number> dependencyCountCol = new TableColumn<>();
        dependencyCountCol.textProperty().bind(i18n.text("profiling.dependency.column.count"));
        dependencyCountCol.setPrefWidth(80);
        dependencyCountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().count()));
        useFormattedIntegerCells(dependencyCountCol);

        TableColumn<DependencyGraphEdge, String> dependencyPctCol = new TableColumn<>();
        dependencyPctCol.textProperty().bind(i18n.text("profiling.dependency.column.percentage"));
        dependencyPctCol.setPrefWidth(90);
        dependencyPctCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatPercent(cell.getValue().percentage())));

        profilingDependencyTable.setPlaceholder(localizedTablePlaceholder("profiling.dependency.empty"));
        profilingDependencyTable.getColumns().setAll(List.of(sourceCol, targetCol, dependencyCountCol, dependencyPctCol));
        profilingDependencyTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, val) -> {
                    if (profilingViewModel != null) {
                        profilingViewModel.selectedDependencyEdgeProperty().set(val);
                    }
                });

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
        exceptionsTable.setPlaceholder(localizedTablePlaceholder("exceptions.empty"));

        TableColumn<ExceptionSummary, String> keyCol = new TableColumn<>();
        keyCol.textProperty().bind(i18n.text("exceptions.column.key"));
        keyCol.setPrefWidth(620);
        keyCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().key()));

        TableColumn<ExceptionSummary, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("exceptions.column.count"));
        countCol.setPrefWidth(80);
        countCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().count()));
        useFormattedIntegerCells(countCol);

        TableColumn<ExceptionSummary, String> pctCol = new TableColumn<>();
        pctCol.textProperty().bind(i18n.text("exceptions.column.percentage"));
        pctCol.setPrefWidth(80);
        pctCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatPercent(cell.getValue().percentage())));

        exceptionsTable.getColumns().setAll(List.of(keyCol, countCol, pctCol));

        exceptionsGroupByClass.setOnAction(event -> setExceptionGrouping(ExceptionGrouping.BY_CLASS));
        exceptionsGroupByMessage.setOnAction(event -> setExceptionGrouping(ExceptionGrouping.BY_MESSAGE));
        exceptionsGroupByClassAndMessage.setOnAction(event -> setExceptionGrouping(ExceptionGrouping.BY_CLASS_AND_MESSAGE));
    }

    private void configureThreadTable() {
        threadsTable.setPlaceholder(localizedTablePlaceholder("threads.empty"));

        TableColumn<ThreadSummary, String> nameCol = new TableColumn<>();
        nameCol.textProperty().bind(i18n.text("threads.column.name"));
        nameCol.setPrefWidth(520);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().threadName()));

        TableColumn<ThreadSummary, Number> samplesCol = new TableColumn<>();
        samplesCol.textProperty().bind(i18n.text("threads.column.samples"));
        samplesCol.setPrefWidth(100);
        samplesCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().sampleCount()));
        useFormattedIntegerCells(samplesCol);

        TableColumn<ThreadSummary, String> blockedCol = new TableColumn<>();
        blockedCol.textProperty().bind(i18n.text("threads.column.blockedMs"));
        blockedCol.setPrefWidth(120);
        blockedCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDuration(cell.getValue().blockedDurationMillis())));

        threadsTable.getColumns().setAll(List.of(nameCol, samplesCol, blockedCol));
    }

    private void configureFileIOTable() {
        fileioHistogramTable.setPlaceholder(localizedTablePlaceholder("fileio.empty"));

        TableColumn<FileIOHistogram, String> pathCol = new TableColumn<>();
        pathCol.textProperty().bind(i18n.text("fileio.column.path"));
        pathCol.setPrefWidth(560);
        pathCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().path()));

        TableColumn<FileIOHistogram, Number> readCountCol = new TableColumn<>();
        readCountCol.textProperty().bind(i18n.text("fileio.column.readCount"));
        readCountCol.setPrefWidth(80);
        readCountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().readCount()));
        useFormattedIntegerCells(readCountCol);

        TableColumn<FileIOHistogram, Number> writeCountCol = new TableColumn<>();
        writeCountCol.textProperty().bind(i18n.text("fileio.column.writeCount"));
        writeCountCol.setPrefWidth(80);
        writeCountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().writeCount()));
        useFormattedIntegerCells(writeCountCol);

        TableColumn<FileIOHistogram, String> readSizeCol = new TableColumn<>();
        readSizeCol.textProperty().bind(i18n.text("fileio.column.readSize"));
        readSizeCol.setPrefWidth(100);
        readSizeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().readSize())));

        TableColumn<FileIOHistogram, String> writeSizeCol = new TableColumn<>();
        writeSizeCol.textProperty().bind(i18n.text("fileio.column.writeSize"));
        writeSizeCol.setPrefWidth(100);
        writeSizeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().writeSize())));

        TableColumn<FileIOHistogram, String> avgDurationCol = new TableColumn<>();
        avgDurationCol.textProperty().bind(i18n.text("fileio.column.avgDuration"));
        avgDurationCol.setPrefWidth(100);
        avgDurationCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDurationMillis(cell.getValue().avgDuration())));

        fileioHistogramTable.getColumns().setAll(List.of(pathCol, readCountCol, writeCountCol,
                readSizeCol, writeSizeCol, avgDurationCol));

        fileioEventTable.setPlaceholder(localizedTablePlaceholder("fileio.events.empty"));

        TableColumn<FileIOEvent, String> eventTypeCol = new TableColumn<>();
        eventTypeCol.textProperty().bind(i18n.text("fileio.events.column.eventType"));
        eventTypeCol.setPrefWidth(140);
        eventTypeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().eventType()));

        TableColumn<FileIOEvent, String> eventPathCol = new TableColumn<>();
        eventPathCol.textProperty().bind(i18n.text("fileio.events.column.path"));
        eventPathCol.setPrefWidth(560);
        eventPathCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().path()));

        TableColumn<FileIOEvent, String> eventBytesCol = new TableColumn<>();
        eventBytesCol.textProperty().bind(i18n.text("fileio.events.column.bytes"));
        eventBytesCol.setPrefWidth(100);
        eventBytesCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().bytes())));

        TableColumn<FileIOEvent, String> eventDurationCol = new TableColumn<>();
        eventDurationCol.textProperty().bind(i18n.text("fileio.events.column.duration"));
        eventDurationCol.setPrefWidth(100);
        eventDurationCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDurationMillis(cell.getValue().durationMillis())));

        TableColumn<FileIOEvent, String> eventThreadCol = new TableColumn<>();
        eventThreadCol.textProperty().bind(i18n.text("fileio.events.column.thread"));
        eventThreadCol.setPrefWidth(260);
        eventThreadCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().threadName()));

        fileioEventTable.getColumns().setAll(List.of(eventTypeCol, eventPathCol, eventBytesCol,
                eventDurationCol, eventThreadCol));
    }

    private void configureSocketIOTable() {
        socketioHistogramTable.setPlaceholder(localizedTablePlaceholder("socketio.empty"));

        TableColumn<SocketIOHistogram, String> keyCol = new TableColumn<>();
        keyCol.textProperty().bind(i18n.text("socketio.column.key"));
        keyCol.setPrefWidth(420);
        keyCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().key()));

        TableColumn<SocketIOHistogram, Number> sockReadCountCol = new TableColumn<>();
        sockReadCountCol.textProperty().bind(i18n.text("socketio.column.readCount"));
        sockReadCountCol.setPrefWidth(80);
        sockReadCountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().readCount()));
        useFormattedIntegerCells(sockReadCountCol);

        TableColumn<SocketIOHistogram, Number> sockWriteCountCol = new TableColumn<>();
        sockWriteCountCol.textProperty().bind(i18n.text("socketio.column.writeCount"));
        sockWriteCountCol.setPrefWidth(80);
        sockWriteCountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().writeCount()));
        useFormattedIntegerCells(sockWriteCountCol);

        TableColumn<SocketIOHistogram, String> sockReadSizeCol = new TableColumn<>();
        sockReadSizeCol.textProperty().bind(i18n.text("socketio.column.readSize"));
        sockReadSizeCol.setPrefWidth(100);
        sockReadSizeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().readSize())));

        TableColumn<SocketIOHistogram, String> sockWriteSizeCol = new TableColumn<>();
        sockWriteSizeCol.textProperty().bind(i18n.text("socketio.column.writeSize"));
        sockWriteSizeCol.setPrefWidth(100);
        sockWriteSizeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().writeSize())));

        TableColumn<SocketIOHistogram, String> sockAvgDurationCol = new TableColumn<>();
        sockAvgDurationCol.textProperty().bind(i18n.text("socketio.column.avgDuration"));
        sockAvgDurationCol.setPrefWidth(100);
        sockAvgDurationCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDurationMillis(cell.getValue().avgDuration())));

        socketioHistogramTable.getColumns().setAll(List.of(keyCol, sockReadCountCol, sockWriteCountCol,
                sockReadSizeCol, sockWriteSizeCol, sockAvgDurationCol));

        socketioGroupByHostAndPort.setOnAction(event -> setSocketIOGrouping(SocketIOGrouping.BY_HOST_AND_PORT));
        socketioGroupByHost.setOnAction(event -> setSocketIOGrouping(SocketIOGrouping.BY_HOST));
        socketioGroupByPort.setOnAction(event -> setSocketIOGrouping(SocketIOGrouping.BY_PORT));

        socketioEventTable.setPlaceholder(localizedTablePlaceholder("socketio.events.empty"));

        TableColumn<SocketIOEvent, String> sockEventTypeCol = new TableColumn<>();
        sockEventTypeCol.textProperty().bind(i18n.text("socketio.events.column.eventType"));
        sockEventTypeCol.setPrefWidth(140);
        sockEventTypeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().eventType()));

        TableColumn<SocketIOEvent, String> sockHostCol = new TableColumn<>();
        sockHostCol.textProperty().bind(i18n.text("socketio.events.column.host"));
        sockHostCol.setPrefWidth(280);
        sockHostCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().host()));

        TableColumn<SocketIOEvent, Number> sockPortCol = new TableColumn<>();
        sockPortCol.textProperty().bind(i18n.text("socketio.events.column.port"));
        sockPortCol.setPrefWidth(80);
        sockPortCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().port()));
        useFormattedIntegerCells(sockPortCol);

        TableColumn<SocketIOEvent, String> sockBytesCol = new TableColumn<>();
        sockBytesCol.textProperty().bind(i18n.text("socketio.events.column.bytes"));
        sockBytesCol.setPrefWidth(100);
        sockBytesCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().bytes())));

        TableColumn<SocketIOEvent, String> sockDurationCol = new TableColumn<>();
        sockDurationCol.textProperty().bind(i18n.text("socketio.events.column.duration"));
        sockDurationCol.setPrefWidth(100);
        sockDurationCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDurationMillis(cell.getValue().durationMillis())));

        TableColumn<SocketIOEvent, String> sockThreadCol = new TableColumn<>();
        sockThreadCol.textProperty().bind(i18n.text("socketio.events.column.thread"));
        sockThreadCol.setPrefWidth(260);
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
        table.setPlaceholder(localizedTablePlaceholder(emptyKey));

        TableColumn<LockHistogram, String> keyCol = new TableColumn<>();
        keyCol.textProperty().bind(i18n.text("locks.column.key"));
        keyCol.setPrefWidth(520);
        keyCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().key()));

        TableColumn<LockHistogram, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("locks.column.count"));
        countCol.setPrefWidth(80);
        countCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().count()));
        useFormattedIntegerCells(countCol);

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
                DisplayFormats.formatDurationMillis(cell.getValue().avgDuration())));

        table.getColumns().setAll(List.of(keyCol, countCol, totalDurCol, maxDurCol, avgDurCol));
    }

    private void configureThreadHistogramTable() {
        threadHistogramTable.setPlaceholder(localizedTablePlaceholder("threadHistogram.empty"));

        TableColumn<ThreadHistogramRow, String> threadCol = new TableColumn<>();
        threadCol.textProperty().bind(i18n.text("threadHistogram.column.threadName"));
        threadCol.setPrefWidth(360);
        threadCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().threadName()));

        TableColumn<ThreadHistogramRow, Number> profilingCol = new TableColumn<>();
        profilingCol.textProperty().bind(i18n.text("threadHistogram.column.profilingCount"));
        profilingCol.setPrefWidth(100);
        profilingCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().profilingCount()));
        useFormattedIntegerCells(profilingCol);

        TableColumn<ThreadHistogramRow, String> ioCol = new TableColumn<>();
        ioCol.textProperty().bind(i18n.text("threadHistogram.column.ioDurationMs"));
        ioCol.setPrefWidth(100);
        ioCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDuration(cell.getValue().ioDurationMillis())));

        TableColumn<ThreadHistogramRow, String> blockedCol = new TableColumn<>();
        blockedCol.textProperty().bind(i18n.text("threadHistogram.column.blockedDurationMs"));
        blockedCol.setPrefWidth(100);
        blockedCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDuration(cell.getValue().blockedDurationMillis())));

        TableColumn<ThreadHistogramRow, String> allocCol = new TableColumn<>();
        allocCol.textProperty().bind(i18n.text("threadHistogram.column.allocatedBytes"));
        allocCol.setPrefWidth(120);
        allocCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().allocatedBytes())));

        TableColumn<ThreadHistogramRow, Number> excCol = new TableColumn<>();
        excCol.textProperty().bind(i18n.text("threadHistogram.column.exceptionCount"));
        excCol.setPrefWidth(80);
        excCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().exceptionCount()));
        useFormattedIntegerCells(excCol);

        threadHistogramTable.getColumns().setAll(List.of(threadCol, profilingCol, ioCol, blockedCol, allocCol, excCol));
    }

    private void configureSecurityTable() {
        securityTable.setPlaceholder(localizedTablePlaceholder("security.empty"));

        TableColumn<X509CertificateEntry, String> algoCol = new TableColumn<>();
        algoCol.textProperty().bind(i18n.text("security.column.algorithm"));
        algoCol.setPrefWidth(100);
        algoCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().algorithm()));

        TableColumn<X509CertificateEntry, String> subjectCol = new TableColumn<>();
        subjectCol.textProperty().bind(i18n.text("security.column.subject"));
        subjectCol.setPrefWidth(420);
        subjectCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().subject()));

        TableColumn<X509CertificateEntry, String> issuerCol = new TableColumn<>();
        issuerCol.textProperty().bind(i18n.text("security.column.issuer"));
        issuerCol.setPrefWidth(420);
        issuerCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().issuer()));

        TableColumn<X509CertificateEntry, String> serialCol = new TableColumn<>();
        serialCol.textProperty().bind(i18n.text("security.column.serialNumber"));
        serialCol.setPrefWidth(150);
        serialCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().serialNumber()));

        TableColumn<X509CertificateEntry, String> validFromCol = new TableColumn<>();
        validFromCol.textProperty().bind(i18n.text("security.column.validFrom"));
        validFromCol.setPrefWidth(160);
        validFromCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                formatEventTimeForDisplay(cell.getValue().validFrom(), ZoneId.systemDefault())));

        TableColumn<X509CertificateEntry, String> validToCol = new TableColumn<>();
        validToCol.textProperty().bind(i18n.text("security.column.validTo"));
        validToCol.setPrefWidth(160);
        validToCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                formatEventTimeForDisplay(cell.getValue().validTo(), ZoneId.systemDefault())));

        TableColumn<X509CertificateEntry, Number> keyLenCol = new TableColumn<>();
        keyLenCol.textProperty().bind(i18n.text("security.column.keyLength"));
        keyLenCol.setPrefWidth(80);
        keyLenCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().keyLength()));
        useFormattedIntegerCells(keyLenCol);

        securityTable.getColumns().setAll(List.of(algoCol, subjectCol, issuerCol, serialCol, validFromCol, validToCol, keyLenCol));
    }

    private void configureNativeLibrariesTable() {
        nativeLibrariesTable.setPlaceholder(localizedTablePlaceholder("nativeLibraries.empty"));

        TableColumn<NativeLibraryEntry, String> nameCol = new TableColumn<>();
        nameCol.textProperty().bind(i18n.text("nativeLibraries.column.name"));
        nameCol.setPrefWidth(250);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));

        TableColumn<NativeLibraryEntry, String> baseCol = new TableColumn<>();
        baseCol.textProperty().bind(i18n.text("nativeLibraries.column.basePath"));
        baseCol.setPrefWidth(420);
        baseCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().basePath()));

        TableColumn<NativeLibraryEntry, String> absCol = new TableColumn<>();
        absCol.textProperty().bind(i18n.text("nativeLibraries.column.absolutePath"));
        absCol.setPrefWidth(560);
        absCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().absolutePath()));

        nativeLibrariesTable.getColumns().setAll(List.of(nameCol, baseCol, absCol));
    }

    private void configureThreadDumpsTable() {
        threadDumpsTable.setPlaceholder(localizedTablePlaceholder("threadDumps.empty"));

        TableColumn<ThreadDumpEntry, String> timeCol = new TableColumn<>();
        timeCol.textProperty().bind(i18n.text("threadDumps.column.time"));
        timeCol.setPrefWidth(200);
        timeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                formatEventTimeForDisplay(cell.getValue().startTime(), ZoneId.systemDefault())));

        threadDumpsTable.getColumns().setAll(List.of(timeCol));
        threadDumpsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> threadDumpTextArea.setText(newVal != null ? newVal.dumpText() : ""));
    }

    private void bindProfiling(ProfilingViewModel nextViewModel) {
        ProfilingViewModel currentProfilingViewModel = profilingViewModel;
        if (currentProfilingViewModel != null) {
            currentProfilingViewModel.callGraphProperty().removeListener(callGraphListener);
            currentProfilingViewModel.dependencyGraphProperty().removeListener(dependencyGraphListener);
            currentProfilingViewModel.callersTreeProperty().removeListener(callersTreeListener);
            currentProfilingViewModel.calleesTreeProperty().removeListener(calleesTreeListener);
            currentProfilingViewModel.callersFlameGraphProperty().removeListener(callersFlameGraphListener);
            currentProfilingViewModel.calleesFlameGraphProperty().removeListener(calleesFlameGraphListener);
        }
        profilingTable.setItems(FXCollections.emptyObservableList());
        profilingDependencyTable.setItems(FXCollections.emptyObservableList());
        profilingCallersTree.setRoot(new TreeItem<>());
        profilingCalleesTree.setRoot(new TreeItem<>());
        profilingCallGraphView.setLayout(null);
        profilingDependencyGraphView.setLayout(null);
        profilingCallersFlameGraphView.setLayout(null);
        profilingCalleesFlameGraphView.setLayout(null);
        profilingViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        profilingTable.setItems(nextViewModel.hotMethodsProperty());
        profilingDependencyTable.setItems(nextViewModel.dependencyEdgesProperty());
        nextViewModel.callGraphProperty().addListener(callGraphListener);
        nextViewModel.dependencyGraphProperty().addListener(dependencyGraphListener);
        nextViewModel.callersTreeProperty().addListener(callersTreeListener);
        nextViewModel.calleesTreeProperty().addListener(calleesTreeListener);
        nextViewModel.callersFlameGraphProperty().addListener(callersFlameGraphListener);
        nextViewModel.calleesFlameGraphProperty().addListener(calleesFlameGraphListener);
        rebuildStackTree(profilingCallersTree, nextViewModel.callersTreeProperty().get());
        rebuildStackTree(profilingCalleesTree, nextViewModel.calleesTreeProperty().get());
        profilingCallGraphView.setLayout(nextViewModel.callGraphProperty().get());
        profilingDependencyGraphView.setLayout(nextViewModel.dependencyGraphProperty().get());
        profilingCallersFlameGraphView.setLayout(nextViewModel.callersFlameGraphProperty().get());
        profilingCalleesFlameGraphView.setLayout(nextViewModel.calleesFlameGraphProperty().get());
        profilingCallGraphDirectionCombo.getSelectionModel().select(nextViewModel.callGraphDirectionProperty().get());
        profilingCallGraphDepthSpinner.getValueFactory().setValue(nextViewModel.callGraphMaxDepthProperty().get());
        profilingDependencyDepthSpinner.getValueFactory().setValue(nextViewModel.dependencyPackageDepthProperty().get());
    }

    private void bindExceptions(ExceptionViewModel nextViewModel) {
        exceptionsTimelineChart.setData(null);
        exceptionsTable.setItems(FXCollections.emptyObservableList());
        exceptionViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        exceptionsTable.setItems(nextViewModel.histogramProperty());
        nextViewModel.timelineProperty().addListener((obs, old, val) -> exceptionsTimelineChart.setData(val));
        exceptionsTimelineChart.setData(nextViewModel.timelineProperty().get());
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
        fileioTimelineChart.setData(null);
        fileioHistogramTable.setItems(FXCollections.emptyObservableList());
        fileioEventTable.setItems(FXCollections.emptyObservableList());
        fileIOViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        fileioHistogramTable.setItems(nextViewModel.histogramProperty());
        fileioEventTable.setItems(nextViewModel.eventsProperty());
        nextViewModel.timelineProperty().addListener((obs, old, val) -> fileioTimelineChart.setData(val));
        fileioTimelineChart.setData(nextViewModel.timelineProperty().get());
    }

    private void bindSocketIO(SocketIOViewModel nextViewModel) {
        socketioTimelineChart.setData(null);
        socketioHistogramTable.setItems(FXCollections.emptyObservableList());
        socketioEventTable.setItems(FXCollections.emptyObservableList());
        socketIOViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        socketioHistogramTable.setItems(nextViewModel.histogramProperty());
        socketioEventTable.setItems(nextViewModel.eventsProperty());
        nextViewModel.timelineProperty().addListener((obs, old, val) -> socketioTimelineChart.setData(val));
        socketioTimelineChart.setData(nextViewModel.timelineProperty().get());
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

    private void bindThreadHistogram(JavaAppOverviewViewModel nextViewModel) {
        threadHistogramChart.setData(null);
        threadHistogramTable.setItems(FXCollections.emptyObservableList());
        javaAppOverviewViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        threadHistogramTable.setItems(nextViewModel.histogramRowsProperty());
        nextViewModel.chartProperty().addListener((obs, old, val) -> threadHistogramChart.setData(val));
        threadHistogramChart.setData(nextViewModel.chartProperty().get());
    }

    private void bindSecurity(SecurityViewModel nextViewModel) {
        securityTable.setItems(FXCollections.emptyObservableList());
        securityViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        securityTable.setItems(nextViewModel.certificatesProperty());
    }

    private void bindNativeLibraries(NativeLibraryViewModel nextViewModel) {
        nativeLibrariesTable.setItems(FXCollections.emptyObservableList());
        nativeLibraryViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        nativeLibrariesTable.setItems(nextViewModel.librariesProperty());
    }

    private void bindThreadDumps(ThreadDumpViewModel nextViewModel) {
        threadDumpsTable.setItems(FXCollections.emptyObservableList());
        threadDumpTextArea.setText("");
        threadDumpViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        threadDumpsTable.setItems(nextViewModel.dumpsProperty());
    }

    private void configureHeapTable() {
        heapTable.setPlaceholder(localizedTablePlaceholder("heap.empty"));

        TableColumn<HeapClassHistogram, String> classNameCol = new TableColumn<>();
        classNameCol.textProperty().bind(i18n.text("heap.column.className"));
        classNameCol.setPrefWidth(300);
        classNameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().className()));

        TableColumn<HeapClassHistogram, Number> instancesCol = new TableColumn<>();
        instancesCol.textProperty().bind(i18n.text("heap.column.instances"));
        instancesCol.setPrefWidth(100);
        instancesCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().instances()));
        useFormattedIntegerCells(instancesCol);

        TableColumn<HeapClassHistogram, String> sizeCol = new TableColumn<>();
        sizeCol.textProperty().bind(i18n.text("heap.column.size"));
        sizeCol.setPrefWidth(100);
        sizeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().size())));

        TableColumn<HeapClassHistogram, String> pctCol = new TableColumn<>();
        pctCol.textProperty().bind(i18n.text("heap.column.allocationPct"));
        pctCol.setPrefWidth(120);
        pctCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatPercent(cell.getValue().allocationPct())));

        heapTable.getColumns().setAll(List.of(classNameCol, instancesCol, sizeCol, pctCol));
    }

    private void configureLeaksTable() {
        leaksTable.setPlaceholder(localizedTablePlaceholder("leaks.empty"));

        TableColumn<LeakCandidate, String> objectCol = new TableColumn<>();
        objectCol.textProperty().bind(i18n.text("leaks.column.object"));
        objectCol.setPrefWidth(300);
        objectCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().object()));

        TableColumn<LeakCandidate, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("leaks.column.count"));
        countCol.setPrefWidth(80);
        countCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().count()));
        useFormattedIntegerCells(countCol);

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
                DisplayFormats.formatPercent(cell.getValue().relevance())));

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
        tlabTable.setPlaceholder(emptyTablePlaceholder());

        TableColumn<TlabAllocation, String> threadCol = new TableColumn<>();
        threadCol.textProperty().bind(i18n.text("tlab.column.thread"));
        threadCol.setPrefWidth(200);
        threadCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().thread()));

        TableColumn<TlabAllocation, Number> insideCountCol = new TableColumn<>();
        insideCountCol.textProperty().bind(i18n.text("tlab.column.insideCount"));
        insideCountCol.setPrefWidth(100);
        insideCountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().insideCount()));
        useFormattedIntegerCells(insideCountCol);

        TableColumn<TlabAllocation, Number> outsideCountCol = new TableColumn<>();
        outsideCountCol.textProperty().bind(i18n.text("tlab.column.outsideCount"));
        outsideCountCol.setPrefWidth(100);
        outsideCountCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().outsideCount()));
        useFormattedIntegerCells(outsideCountCol);

        TableColumn<TlabAllocation, String> insideAvgCol = new TableColumn<>();
        insideAvgCol.textProperty().bind(i18n.text("tlab.column.insideAvgSize"));
        insideAvgCol.setPrefWidth(120);
        insideAvgCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(Math.round(cell.getValue().insideAvgSize()))));

        TableColumn<TlabAllocation, String> outsideAvgCol = new TableColumn<>();
        outsideAvgCol.textProperty().bind(i18n.text("tlab.column.outsideAvgSize"));
        outsideAvgCol.setPrefWidth(120);
        outsideAvgCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(Math.round(cell.getValue().outsideAvgSize()))));

        TableColumn<TlabAllocation, String> insideTotalCol = new TableColumn<>();
        insideTotalCol.textProperty().bind(i18n.text("tlab.column.insideTotalSize"));
        insideTotalCol.setPrefWidth(120);
        insideTotalCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().insideTotalSize())));

        TableColumn<TlabAllocation, String> outsideTotalCol = new TableColumn<>();
        outsideTotalCol.textProperty().bind(i18n.text("tlab.column.outsideTotalSize"));
        outsideTotalCol.setPrefWidth(120);
        outsideTotalCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().outsideTotalSize())));

        tlabTable.getColumns().setAll(List.of(threadCol, insideCountCol, outsideCountCol, insideAvgCol,
                outsideAvgCol, insideTotalCol, outsideTotalCol));
    }

    private void bindHeap(HeapViewModel nextViewModel) {
        heapTable.setItems(FXCollections.emptyObservableList());
        heapTimelineChart.setData(null);
        heapViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        heapTable.setItems(nextViewModel.histogramProperty());
        heapTable.getSelectionModel().selectFirst();
        nextViewModel.timelineProperty().addListener((obs, old, val) -> heapTimelineChart.setData(val));
        heapTimelineChart.setData(nextViewModel.timelineProperty().get());
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
        tlabTable.setPlaceholder(emptyTablePlaceholder());
        tlabTimelineChart.setData(null);
        tlabViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        tlabTable.setItems(nextViewModel.allocationsProperty());
        tlabTable.getSelectionModel().selectFirst();
        updateTlabTablePlaceholder(nextViewModel);
        nextViewModel.loadingProperty().addListener((obs, old, val) -> updateTlabTablePlaceholder(nextViewModel));
        nextViewModel.loadedProperty().addListener((obs, old, val) -> updateTlabTablePlaceholder(nextViewModel));
        nextViewModel.timelineProperty().addListener((obs, old, val) -> tlabTimelineChart.setData(val));
        tlabTimelineChart.setData(nextViewModel.timelineProperty().get());
    }

    private void updateTlabTablePlaceholder(TlabViewModel viewModel) {
        if (viewModel == null || !viewModel.loadedProperty().get()) {
            tlabTable.setPlaceholder(viewModel != null && viewModel.loadingProperty().get()
                    ? localizedTablePlaceholder("tlab.loading")
                    : emptyTablePlaceholder());
            return;
        }
        tlabTable.setPlaceholder(localizedTablePlaceholder("tlab.empty"));
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
        if (profilingViewModel == null) {
            return;
        }
        profilingViewModel.selectMethod(method == null ? null : method.method());
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

    private String formatCallGraphDirection(CallGraphDirection direction) {
        if (direction == null) {
            return "";
        }
        return switch (direction) {
            case CALLERS -> i18n.text("profiling.callGraph.direction.callers").get();
            case CALLEES -> i18n.text("profiling.callGraph.direction.callees").get();
        };
    }

    private void refreshProfilingCallGraphDirectionLabel() {
        CallGraphDirection selectedDirection = profilingCallGraphDirectionCombo.getSelectionModel().getSelectedItem();
        profilingCallGraphDirectionCombo.setButtonCell(callGraphDirectionCell());
        profilingCallGraphDirectionCombo.setCellFactory(combo -> callGraphDirectionCell());
        profilingCallGraphDirectionCombo.getSelectionModel().select(selectedDirection);
    }

    private ListCell<CallGraphDirection> callGraphDirectionCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(CallGraphDirection direction, boolean empty) {
                super.updateItem(direction, empty);
                setText(empty ? null : formatCallGraphDirection(direction));
            }
        };
    }

    private void configureGraphZoomButtons(CallGraphView graphView, Button zoomOutButton,
            Button resetZoomButton, Button zoomInButton, Button fitButton) {
        configureIconButton(zoomOutButton, Material2MZ.ZOOM_OUT, "profiling.graph.zoomOut");
        configureIconButton(resetZoomButton, Material2MZ.REFRESH, "profiling.graph.resetZoom");
        configureIconButton(zoomInButton, Material2MZ.ZOOM_IN, "profiling.graph.zoomIn");
        configureIconButton(fitButton, Material2MZ.ZOOM_OUT_MAP, "profiling.graph.fit");
        zoomOutButton.setOnAction(event -> graphView.zoomOut());
        resetZoomButton.setOnAction(event -> graphView.resetZoom());
        zoomInButton.setOnAction(event -> graphView.zoomIn());
        fitButton.setOnAction(event -> graphView.fitToWidth(graphViewportWidth(graphView)));
    }

    private void configureCallGraphGestures(CallGraphView graphView, ScrollPane scrollPane) {
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (callGraphZoomGestureActive) {
                event.consume();
                return;
            }
            if (event.isShortcutDown()) {
                zoomCallGraphAt(graphView, scrollPane, event.getX(), event.getY(),
                        event.getDeltaY() > 0 ? 1.1 : 1 / 1.1);
                event.consume();
                return;
            }
            panCallGraphViewport(scrollPane, event.getDeltaX(), event.getDeltaY());
            event.consume();
        });
        scrollPane.addEventFilter(ZoomEvent.ZOOM_STARTED, event -> {
            callGraphZoomGestureActive = true;
            event.consume();
        });
        scrollPane.addEventFilter(ZoomEvent.ZOOM, event -> {
            zoomCallGraphAt(graphView, scrollPane, event.getX(), event.getY(), event.getZoomFactor());
            event.consume();
        });
        scrollPane.addEventFilter(ZoomEvent.ZOOM_FINISHED, event -> {
            callGraphZoomGestureActive = false;
            event.consume();
        });
        scrollPane.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                zoomCallGraphAt(graphView, scrollPane, event.getX(), event.getY(), 1.1);
                event.consume();
            }
        });
    }

    private void zoomCallGraphAt(CallGraphView graphView, ScrollPane scrollPane,
            double viewportX, double viewportY, double factor) {
        double oldContentWidth = scrollContentWidth(scrollPane);
        double oldContentHeight = scrollContentHeight(scrollPane);
        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();

        graphView.zoomBy(factor);

        double newContentWidth = scrollContentWidth(scrollPane);
        double newContentHeight = scrollContentHeight(scrollPane);
        scrollPane.setHvalue(scrollValueAfterZoom(scrollPane.getHvalue(),
                oldContentWidth, newContentWidth, viewportWidth, viewportX));
        scrollPane.setVvalue(scrollValueAfterZoom(scrollPane.getVvalue(),
                oldContentHeight, newContentHeight, viewportHeight, viewportY));
    }

    private double scrollContentWidth(ScrollPane scrollPane) {
        return Math.max(scrollPane.getContent().getBoundsInLocal().getWidth(),
                scrollPane.getContent().prefWidth(-1));
    }

    private double scrollContentHeight(ScrollPane scrollPane) {
        return Math.max(scrollPane.getContent().getBoundsInLocal().getHeight(),
                scrollPane.getContent().prefHeight(-1));
    }

    static double scrollValueAfterZoom(double currentValue, double oldContentSize, double newContentSize,
            double viewportSize, double viewportCoordinate) {
        double oldScrollableSize = Math.max(0, oldContentSize - viewportSize);
        double newScrollableSize = Math.max(0, newContentSize - viewportSize);
        if (oldContentSize <= 0 || viewportSize <= 0 || newScrollableSize <= 0) {
            return 0;
        }
        double anchorInViewport = Math.clamp(viewportCoordinate, 0, viewportSize);
        double anchorInContent = (Math.clamp(currentValue, 0, 1) * oldScrollableSize) + anchorInViewport;
        double scaledAnchorInContent = anchorInContent * (newContentSize / oldContentSize);
        return Math.clamp((scaledAnchorInContent - anchorInViewport) / newScrollableSize, 0, 1);
    }

    private void panCallGraphViewport(ScrollPane scrollPane, double deltaX, double deltaY) {
        double horizontalRange = Math.max(0, scrollPane.getContent().getBoundsInLocal().getWidth()
                - scrollPane.getViewportBounds().getWidth());
        double verticalRange = Math.max(0, scrollPane.getContent().getBoundsInLocal().getHeight()
                - scrollPane.getViewportBounds().getHeight());
        if (horizontalRange > 0) {
            scrollPane.setHvalue(Math.clamp(scrollPane.getHvalue() - deltaX / horizontalRange,
                    scrollPane.getHmin(), scrollPane.getHmax()));
        }
        if (verticalRange > 0) {
            scrollPane.setVvalue(Math.clamp(scrollPane.getVvalue() - deltaY / verticalRange,
                    scrollPane.getVmin(), scrollPane.getVmax()));
        }
    }

    private void configureFlameGraphButtons(FlameGraphView graphView, Button orientationButton,
            Button zoomOutButton, Button resetZoomButton, Button zoomInButton, Button fitButton) {
        configureIconButton(orientationButton, Material2MZ.SWAP_VERT, "profiling.flame.orientation");
        configureIconButton(zoomOutButton, Material2MZ.ZOOM_OUT, "profiling.graph.zoomOut");
        configureIconButton(resetZoomButton, Material2MZ.REFRESH, "profiling.graph.resetZoom");
        configureIconButton(zoomInButton, Material2MZ.ZOOM_IN, "profiling.graph.zoomIn");
        configureIconButton(fitButton, Material2MZ.ZOOM_OUT_MAP, "profiling.graph.fit");
        orientationButton.setOnAction(event -> toggleFlameGraphOrientation(graphView));
        zoomOutButton.setOnAction(event -> graphView.zoomOut());
        resetZoomButton.setOnAction(event -> graphView.resetZoom());
        zoomInButton.setOnAction(event -> graphView.zoomIn());
        fitButton.setOnAction(event -> graphView.fitToWidth(graphViewportWidth(graphView)));
    }

    private void bindFlameGraphToolbarVisibility(HBox toolbar, FlameGraphView graphView) {
        toolbar.visibleProperty().bind(graphView.hasFramesProperty());
        toolbar.managedProperty().bind(toolbar.visibleProperty());
    }

    private void toggleFlameGraphOrientation(FlameGraphView graphView) {
        graphView.setOrientation(graphView.getOrientation() == FlameGraphView.Orientation.ICICLE
                ? FlameGraphView.Orientation.FLAME
                : FlameGraphView.Orientation.ICICLE);
    }

    private double graphViewportWidth(Region graphView) {
        for (Node parent = graphView.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof ScrollPane scrollPane) {
                return scrollPane.getViewportBounds().getWidth();
            }
        }
        return graphView.getWidth();
    }

    private void configureIconButton(Button button, Ikon icon, String tooltipKey) {
        button.setGraphic(new FontIcon(icon));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(i18n.tooltip(tooltipKey));
        button.accessibleTextProperty().bind(i18n.text(tooltipKey));
    }

    private void bindLocalizedText() {
        homeKickerLabel.textProperty().bind(i18n.text("home.kicker"));
        homeTitleLabel.textProperty().bind(i18n.text("home.title"));
        homeSubtitleLabel.textProperty().bind(i18n.text("home.subtitle"));
        homeOpenRecordingButton.textProperty().bind(i18n.text("home.openRecording"));
        homeOpenHeapDumpButton.textProperty().bind(i18n.text("home.openHeapDump"));
        homeConnectJvmButton.textProperty().bind(i18n.text("home.connectJvm"));
        homeOpenWorkflowTitleLabel.textProperty().bind(i18n.text("home.workflow.openTitle"));
        homeOpenWorkflowDescriptionLabel.textProperty().bind(i18n.text("home.workflow.openDescription"));
        homeHeapDumpWorkflowTitleLabel.textProperty().bind(i18n.text("home.workflow.heapDumpTitle"));
        homeHeapDumpWorkflowDescriptionLabel.textProperty().bind(i18n.text("home.workflow.heapDumpDescription"));
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
        analysisSearchField.promptTextProperty().bind(i18n.text("analysis.filter.search"));
        analysisMinimumScoreLabel.textProperty().bind(i18n.text("analysis.filter.minimumScore"));
        analysisShowOkCheckBox.textProperty().bind(i18n.text("analysis.filter.showOk"));
        analysisShowIgnoredCheckBox.textProperty().bind(i18n.text("analysis.filter.showIgnored"));
        analysisShowUnavailableCheckBox.textProperty().bind(i18n.text("analysis.filter.showUnavailable"));
        analysisDetailExplanationCaption.textProperty().bind(i18n.text("analysis.detail.explanation"));
        analysisDetailEvidenceCaption.textProperty().bind(i18n.text("analysis.detail.evidence"));
        analysisDetailRecommendationCaption.textProperty().bind(i18n.text("analysis.detail.recommendation"));
        metadataTitleLabel.textProperty().bind(i18n.text("metadata.title"));
        metadataDetailTitleLabel.textProperty().bind(i18n.text("metadata.detail.title"));
        g1GcTitleLabel.textProperty().bind(i18n.text("g1Gc.title"));
        g1GcRegionStatesLabel.textProperty().bind(i18n.text("g1Gc.regionStates"));
        g1GcRegionSummaryLabel.textProperty().bind(i18n.text("g1Gc.regionSummary"));
        g1GcPausesLabel.textProperty().bind(i18n.text("g1Gc.pauses"));
        g1GcDetailTitleLabel.textProperty().bind(i18n.text("g1Gc.detail.title"));
        javaFxEventsTitleLabel.textProperty().bind(i18n.text("javaFxEvents.title"));
        javaFxEventsPhaseLabel.textProperty().bind(i18n.text("javaFxEvents.phases"));
        javaFxEventsPulseLabel.textProperty().bind(i18n.text("javaFxEvents.pulses"));
        javaFxEventsInputLabel.textProperty().bind(i18n.text("javaFxEvents.inputs"));
        javaFxEventsDetailTitleLabel.textProperty().bind(i18n.text("javaFxEvents.detail.title"));
        advancedJfrTitleLabel.textProperty().bind(i18n.text("advancedJfr.title"));
        advancedJfrHeatmapTab.textProperty().bind(i18n.text("advancedJfr.heatmap.tab"));
        advancedJfrMemoryTab.textProperty().bind(i18n.text("advancedJfr.memory.tab"));
        advancedJfrSelectionTitleLabel.textProperty().bind(i18n.text("advancedJfr.selection"));
        advancedJfrSelectedEventTypeCaptionLabel.textProperty().bind(i18n.text("advancedJfr.selectedEventType"));
        advancedJfrSelectedCountCaptionLabel.textProperty().bind(i18n.text("advancedJfr.selectedCount"));
        heapDumpAnalysisTitleLabel.textProperty().bind(i18n.text("heapDump.title"));
        heapDumpIssueDetailTab.textProperty().bind(i18n.text("heapDump.detail.tab"));
        heapDumpTextReportTab.textProperty().bind(i18n.text("heapDump.report.tab"));
        profilingTitleLabel.textProperty().bind(i18n.text("profiling.title"));
        javaApplicationTitleLabel.textProperty().bind(i18n.text("javaApplication.title"));
        javaApplicationSummaryLabel.textProperty().bind(i18n.text("javaApplication.summary"));
        javaApplicationProfilingTitleLabel.textProperty().bind(i18n.text("javaApplication.profiling.title"));
        javaApplicationProfilingSummaryLabel.textProperty().bind(i18n.text("javaApplication.profiling.summary"));
        javaApplicationProfilingButton.textProperty().bind(i18n.text("javaApplication.profiling.action"));
        javaApplicationIoTitleLabel.textProperty().bind(i18n.text("javaApplication.io.title"));
        javaApplicationIoSummaryLabel.textProperty().bind(i18n.text("javaApplication.io.summary"));
        javaApplicationIoButton.textProperty().bind(i18n.text("javaApplication.io.action"));
        javaApplicationLocksTitleLabel.textProperty().bind(i18n.text("javaApplication.locks.title"));
        javaApplicationLocksSummaryLabel.textProperty().bind(i18n.text("javaApplication.locks.summary"));
        javaApplicationLocksButton.textProperty().bind(i18n.text("javaApplication.locks.action"));
        javaApplicationThreadsTitleLabel.textProperty().bind(i18n.text("javaApplication.threads.title"));
        javaApplicationThreadsSummaryLabel.textProperty().bind(i18n.text("javaApplication.threads.summary"));
        javaApplicationThreadsButton.textProperty().bind(i18n.text("javaApplication.threads.action"));
        javaApplicationExceptionsTitleLabel.textProperty().bind(i18n.text("javaApplication.exceptions.title"));
        javaApplicationExceptionsSummaryLabel.textProperty().bind(i18n.text("javaApplication.exceptions.summary"));
        javaApplicationExceptionsButton.textProperty().bind(i18n.text("javaApplication.exceptions.action"));
        javaApplicationClassLoadingTitleLabel.textProperty().bind(i18n.text("javaApplication.classLoading.title"));
        javaApplicationClassLoadingSummaryLabel.textProperty().bind(i18n.text("javaApplication.classLoading.summary"));
        javaApplicationClassLoadingButton.textProperty().bind(i18n.text("javaApplication.classLoading.action"));
        javaApplicationAllocationTitleLabel.textProperty().bind(i18n.text("javaApplication.allocation.title"));
        javaApplicationAllocationSummaryLabel.textProperty().bind(i18n.text("javaApplication.allocation.summary"));
        javaApplicationAllocationButton.textProperty().bind(i18n.text("javaApplication.allocation.action"));
        jvmInternalsTitleLabel.textProperty().bind(i18n.text("jvmInternals.title"));
        jvmInternalsSummaryLabel.textProperty().bind(i18n.text("jvmInternals.summary"));
        jvmInternalsInformationTitleLabel.textProperty().bind(i18n.text("jvmInternals.information.title"));
        jvmInternalsInformationSummaryLabel.textProperty().bind(i18n.text("jvmInternals.information.summary"));
        jvmInternalsInformationButton.textProperty().bind(i18n.text("jvmInternals.information.action"));
        jvmInternalsGcTitleLabel.textProperty().bind(i18n.text("jvmInternals.gc.title"));
        jvmInternalsGcSummaryLabel.textProperty().bind(i18n.text("jvmInternals.gc.summary"));
        jvmInternalsGcButton.textProperty().bind(i18n.text("jvmInternals.gc.action"));
        jvmInternalsG1TitleLabel.textProperty().bind(i18n.text("jvmInternals.g1.title"));
        jvmInternalsG1SummaryLabel.textProperty().bind(i18n.text("jvmInternals.g1.summary"));
        jvmInternalsG1Button.textProperty().bind(i18n.text("jvmInternals.g1.action"));
        jvmInternalsCompilationTitleLabel.textProperty().bind(i18n.text("jvmInternals.compilation.title"));
        jvmInternalsCompilationSummaryLabel.textProperty().bind(i18n.text("jvmInternals.compilation.summary"));
        jvmInternalsCompilationButton.textProperty().bind(i18n.text("jvmInternals.compilation.action"));
        jvmInternalsCodeCacheTitleLabel.textProperty().bind(i18n.text("jvmInternals.codeCache.title"));
        jvmInternalsCodeCacheSummaryLabel.textProperty().bind(i18n.text("jvmInternals.codeCache.summary"));
        jvmInternalsCodeCacheButton.textProperty().bind(i18n.text("jvmInternals.codeCache.action"));
        jvmInternalsClassLoadingTitleLabel.textProperty().bind(i18n.text("jvmInternals.classLoading.title"));
        jvmInternalsClassLoadingSummaryLabel.textProperty().bind(i18n.text("jvmInternals.classLoading.summary"));
        jvmInternalsClassLoadingButton.textProperty().bind(i18n.text("jvmInternals.classLoading.action"));
        jvmInternalsVmOperationsTitleLabel.textProperty().bind(i18n.text("jvmInternals.vmOperations.title"));
        jvmInternalsVmOperationsSummaryLabel.textProperty().bind(i18n.text("jvmInternals.vmOperations.summary"));
        jvmInternalsVmOperationsButton.textProperty().bind(i18n.text("jvmInternals.vmOperations.action"));
        environmentTitleLabel.textProperty().bind(i18n.text("environment.title"));
        environmentSummaryLabel.textProperty().bind(i18n.text("environment.summary"));
        environmentProcessesTitleLabel.textProperty().bind(i18n.text("environment.processes.title"));
        environmentProcessesSummaryLabel.textProperty().bind(i18n.text("environment.processes.summary"));
        environmentProcessesButton.textProperty().bind(i18n.text("environment.processes.action"));
        environmentVariablesTitleLabel.textProperty().bind(i18n.text("environment.variables.title"));
        environmentVariablesSummaryLabel.textProperty().bind(i18n.text("environment.variables.summary"));
        environmentVariablesButton.textProperty().bind(i18n.text("environment.variables.action"));
        environmentPropertiesTitleLabel.textProperty().bind(i18n.text("environment.properties.title"));
        environmentPropertiesSummaryLabel.textProperty().bind(i18n.text("environment.properties.summary"));
        environmentPropertiesButton.textProperty().bind(i18n.text("environment.properties.action"));
        environmentRecordingTitleLabel.textProperty().bind(i18n.text("environment.recording.title"));
        environmentRecordingSummaryLabel.textProperty().bind(i18n.text("environment.recording.summary"));
        environmentRecordingButton.textProperty().bind(i18n.text("environment.recording.action"));
        environmentAgentsTitleLabel.textProperty().bind(i18n.text("environment.agents.title"));
        environmentAgentsSummaryLabel.textProperty().bind(i18n.text("environment.agents.summary"));
        environmentAgentsButton.textProperty().bind(i18n.text("environment.agents.action"));
        environmentConstantPoolsTitleLabel.textProperty().bind(i18n.text("environment.constantPools.title"));
        environmentConstantPoolsSummaryLabel.textProperty().bind(i18n.text("environment.constantPools.summary"));
        environmentConstantPoolsButton.textProperty().bind(i18n.text("environment.constantPools.action"));
        profilingCallGraphTab.textProperty().bind(i18n.text("profiling.tab.callGraph"));
        profilingCallGraphDirectionCombo.promptTextProperty().bind(i18n.text("profiling.callGraph.direction"));
        profilingCallGraphDepthLabel.textProperty().bind(i18n.text("profiling.callGraph.depth"));
        profilingDependencyGraphTab.textProperty().bind(i18n.text("profiling.tab.dependencyGraph"));
        profilingDependencyDepthLabel.textProperty().bind(i18n.text("profiling.dependency.depth"));
        profilingCallersFlameTab.textProperty().bind(i18n.text("profiling.tab.callersFlame"));
        profilingCalleesFlameTab.textProperty().bind(i18n.text("profiling.tab.calleesFlame"));
        profilingCallersTab.textProperty().bind(i18n.text("profiling.tab.callers"));
        profilingCalleesTab.textProperty().bind(i18n.text("profiling.tab.callees"));
        exceptionsTitleLabel.textProperty().bind(i18n.text("exceptions.title"));
        exceptionsGroupByClass.textProperty().bind(i18n.text("exceptions.grouping.byClass"));
        exceptionsGroupByMessage.textProperty().bind(i18n.text("exceptions.grouping.byMessage"));
        exceptionsGroupByClassAndMessage.textProperty().bind(i18n.text("exceptions.grouping.byClassAndMessage"));
        threadsTitleLabel.textProperty().bind(i18n.text("threads.title"));
        fileioTitleLabel.textProperty().bind(i18n.text("fileio.title"));
        fileioTimelineTab.textProperty().bind(i18n.text("fileio.tab.timeline"));
        fileioDurationTab.textProperty().bind(i18n.text("fileio.tab.duration"));
        fileioEventLogTab.textProperty().bind(i18n.text("fileio.tab.eventLog"));
        socketioTitleLabel.textProperty().bind(i18n.text("socketio.title"));
        socketioGroupByHostAndPort.textProperty().bind(i18n.text("socketio.grouping.byHostAndPort"));
        socketioGroupByHost.textProperty().bind(i18n.text("socketio.grouping.byHost"));
        socketioGroupByPort.textProperty().bind(i18n.text("socketio.grouping.byPort"));
        socketioTimelineTab.textProperty().bind(i18n.text("socketio.tab.timeline"));
        socketioDurationTab.textProperty().bind(i18n.text("socketio.tab.duration"));
        socketioEventLogTab.textProperty().bind(i18n.text("socketio.tab.eventLog"));
        locksTitleLabel.textProperty().bind(i18n.text("locks.title"));
        locksGroupByClass.textProperty().bind(i18n.text("locks.grouping.byClass"));
        locksGroupByAddress.textProperty().bind(i18n.text("locks.grouping.byAddress"));
        locksGroupByThread.textProperty().bind(i18n.text("locks.grouping.byThread"));
        locksByClassTab.textProperty().bind(i18n.text("locks.tab.byClass"));
        locksByAddressTab.textProperty().bind(i18n.text("locks.tab.byAddress"));
        locksByThreadTab.textProperty().bind(i18n.text("locks.tab.byThread"));
        threadHistogramTitleLabel.textProperty().bind(i18n.text("threadHistogram.title"));
        securityTitleLabel.textProperty().bind(i18n.text("security.title"));
        nativeLibrariesTitleLabel.textProperty().bind(i18n.text("nativeLibraries.title"));
        threadDumpsTitleLabel.textProperty().bind(i18n.text("threadDumps.title"));
        heapTitleLabel.textProperty().bind(i18n.text("heap.title"));
        leaksTitleLabel.textProperty().bind(i18n.text("leaks.title"));
        tlabTitleLabel.textProperty().bind(i18n.text("tlab.title"));
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
        settingsThemeLabel.textProperty().bind(i18n.text("settings.theme"));
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

    private void configureThemeSelector() {
        themeFollowSystemRadio.setUserData(AppTheme.SYSTEM);
        themeLightRadio.setUserData(AppTheme.PRIMER_LIGHT);
        themeDarkRadio.setUserData(AppTheme.PRIMER_DARK);

        themeFollowSystemRadio.textProperty().bind(i18n.text("settings.theme.followSystem"));
        themeLightRadio.textProperty().bind(i18n.text("settings.theme.light"));
        themeDarkRadio.textProperty().bind(i18n.text("settings.theme.dark"));

        themeToggleGroup.selectToggle(themeToToggle(viewModel.themeProperty().get()));

        themeToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.getUserData() instanceof AppTheme theme) {
                viewModel.setTheme(theme);
            }
        });
    }

    private Toggle themeToToggle(AppTheme theme) {
        if (theme == AppTheme.PRIMER_LIGHT) {
            return themeLightRadio;
        }
        if (theme == AppTheme.PRIMER_DARK) {
            return themeDarkRadio;
        }
        return themeFollowSystemRadio;
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

    static String tabTitleFor(HeapDumpWorkspace workspace) {
        return workspace.name();
    }

    static String tabTitleFor(LiveJvmWorkspace workspace) {
        return workspace.name();
    }

    static boolean shouldShowRecordingTabs(int workspaceCount) {
        return shouldShowWorkspaceTabs(workspaceCount, 0, false);
    }

    static boolean shouldShowWorkspaceTabs(int recordingWorkspaceCount, int heapDumpWorkspaceCount) {
        return shouldShowWorkspaceTabs(recordingWorkspaceCount, heapDumpWorkspaceCount, false);
    }

    static boolean shouldShowWorkspaceTabs(int recordingWorkspaceCount, int heapDumpWorkspaceCount,
            boolean liveJvmWorkspaceOpen) {
        return recordingWorkspaceCount + heapDumpWorkspaceCount > 0 || liveJvmWorkspaceOpen;
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

    static String openingHeapDumpStatus(I18n i18n, Path path) {
        return i18n.format("status.openingHeapDump", path.getFileName());
    }

    static String saveRecordingInitialFileName(String recordingName) {
        String baseName = recordingName == null ? "" : recordingName.trim();
        if (baseName.endsWith(".jfr")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }
        baseName = baseName.replaceAll("[^A-Za-z0-9._-]+", "_");
        baseName = baseName.replaceAll("_+", "_");
        baseName = baseName.replaceAll("^[_ .-]+|[_ .-]+$", "");
        if (baseName.isBlank()) {
            baseName = "jmcfx-recording";
        }
        return baseName + ".jfr";
    }

    static String languageModeDisplayName(I18n i18n, LanguageMode mode) {
        return switch (mode) {
            case ENGLISH -> i18n.get("settings.language.english");
            case CHINESE_SIMPLIFIED -> i18n.get("settings.language.chineseSimplified");
            case SYSTEM -> i18n.get("settings.language.followSystem");
        };
    }

    static boolean shouldDisableOpenRecordingButton(boolean opening) {
        return opening;
    }

    static boolean canDisconnectJvm(JvmConnection selectedConnection) {
        return selectedConnection != null && selectedConnection.connected();
    }

    static String openingRecordingStatus(I18n i18n, Path path) {
        return i18n.format("status.openingRecording", path.getFileName());
    }

    public BorderPane root() {
        return root;
    }

    public void close() {
        if (jvmsPaneController != null) {
            jvmsPaneController.close();
        }
        List.copyOf(viewModel.recordingWorkspacesProperty()).forEach(viewModel::closeWorkspace);
        List.copyOf(viewModel.heapDumpWorkspacesProperty()).forEach(viewModel::closeHeapDumpWorkspace);
        if (jvmBrowserViewModel != null) {
            jvmBrowserViewModel.close();
        }
        if (viewModel.heapDumpWorkspacesProperty().isEmpty() && heapDumpAnalysisViewModel != null) {
            heapDumpAnalysisViewModel.close();
        }
        recordingOpenExecutor.close();
    }

    private void configureActionIcons() {
        homeOpenRecordingButton.getStyleClass().add("toolbar-primary");
        homeOpenHeapDumpButton.getStyleClass().add("toolbar-secondary");
        homeConnectJvmButton.getStyleClass().add("toolbar-secondary");
        configureActionButton(homeOpenRecordingButton, Material2AL.FOLDER_OPEN, i18n.get("home.openRecording"));
        configureActionButton(homeOpenHeapDumpButton, Material2MZ.STORAGE, i18n.get("home.openHeapDump"));
        configureActionButton(homeConnectJvmButton, Material2MZ.MEMORY, i18n.get("home.connectJvm"));
    }

    private void configureJavaApplicationOverviewActions() {
        javaApplicationProfilingButton.setOnAction(event -> viewModel.showSection("profiling"));
        javaApplicationIoButton.setOnAction(event -> viewModel.showSection("fileio"));
        javaApplicationLocksButton.setOnAction(event -> viewModel.showSection("locks"));
        javaApplicationThreadsButton.setOnAction(event -> viewModel.showSection("threadHistogram"));
        javaApplicationExceptionsButton.setOnAction(event -> viewModel.showSection("exceptions"));
        javaApplicationClassLoadingButton.setOnAction(event -> viewModel.showSection("classLoading"));
        javaApplicationAllocationButton.setOnAction(event -> viewModel.showSection("tlab"));
    }

    private void configureJvmInternalsOverviewActions() {
        jvmInternalsInformationButton.setOnAction(event -> viewModel.showSection("jvmInfo"));
        jvmInternalsGcButton.setOnAction(event -> viewModel.showSection("gcSummary"));
        jvmInternalsG1Button.setOnAction(event -> viewModel.showSection("g1Gc"));
        jvmInternalsCompilationButton.setOnAction(event -> viewModel.showSection("compilations"));
        jvmInternalsCodeCacheButton.setOnAction(event -> viewModel.showSection("codeCache"));
        jvmInternalsClassLoadingButton.setOnAction(event -> viewModel.showSection("classLoading"));
        jvmInternalsVmOperationsButton.setOnAction(event -> viewModel.showSection("vmOperations"));
    }

    private void configureEnvironmentOverviewActions() {
        environmentProcessesButton.setOnAction(event -> viewModel.showSection("processes"));
        environmentVariablesButton.setOnAction(event -> viewModel.showSection("envVars"));
        environmentPropertiesButton.setOnAction(event -> viewModel.showSection("sysProps"));
        environmentRecordingButton.setOnAction(event -> viewModel.showSection("recordingInfo"));
        environmentAgentsButton.setOnAction(event -> viewModel.showSection("agents"));
        environmentConstantPoolsButton.setOnAction(event -> viewModel.showSection("constantPools"));
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
            if (updatingRecordingTabs || newValue == null) {
                return;
            }
            switch (newValue.getUserData()) {
                case RecordingWorkspace workspace -> viewModel.selectWorkspace(workspace);
                case HeapDumpWorkspace workspace -> viewModel.selectHeapDumpWorkspace(workspace);
                case LiveJvmWorkspace ignored -> viewModel.selectLiveJvmWorkspace();
                default -> {
                }
            }
        });
        viewModel.workspaceTabsProperty()
                .addListener((ListChangeListener<Object>) change -> rebuildRecordingTabs());
        rebuildRecordingTabs();
    }

    private void bindWorkspaceSelection() {
        viewModel.selectedWorkspaceProperty()
                .addListener((observable, oldValue, newValue) -> showWorkspace(newValue));
        viewModel.selectedHeapDumpWorkspaceProperty()
                .addListener((observable, oldValue, newValue) -> showHeapDumpWorkspace(newValue));
        viewModel.selectedLiveJvmWorkspaceProperty()
                .addListener((observable, oldValue, newValue) -> showLiveJvmWorkspace(newValue));
        showWorkspace(viewModel.selectedWorkspaceProperty().get());
        showHeapDumpWorkspace(viewModel.selectedHeapDumpWorkspaceProperty().get());
        showLiveJvmWorkspace(viewModel.selectedLiveJvmWorkspaceProperty().get());
    }

    private void rebuildRecordingTabs() {
        updatingRecordingTabs = true;
        try {
            List<Tab> tabs = viewModel.workspaceTabsProperty().stream()
                    .map(this::toWorkspaceTab)
                    .toList();
            recordingTabs.getTabs().setAll(tabs);
            boolean showTabs = shouldShowWorkspaceTabs(
                    viewModel.recordingWorkspacesProperty().size(),
                    viewModel.heapDumpWorkspacesProperty().size(),
                    viewModel.liveJvmWorkspaceOpenProperty().get());
            recordingTabs.setVisible(showTabs);
            recordingTabs.setManaged(showTabs);
            selectWorkspaceTab(viewModel.selectedWorkspaceProperty().get(),
                    viewModel.selectedHeapDumpWorkspaceProperty().get(),
                    viewModel.selectedLiveJvmWorkspaceProperty().get());
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

    private Tab toHeapDumpTab(HeapDumpWorkspace workspace) {
        Tab tab = new Tab(tabTitleFor(workspace));
        tab.setUserData(workspace);
        tab.setClosable(true);
        tab.setOnClosed(event -> viewModel.closeHeapDumpWorkspace(workspace));
        return tab;
    }

    private Tab toLiveJvmTab(LiveJvmWorkspace workspace) {
        Tab tab = new Tab(tabTitleFor(workspace));
        tab.setUserData(workspace);
        tab.setClosable(true);
        tab.setOnClosed(event -> viewModel.closeLiveJvmWorkspace());
        return tab;
    }

    private Tab toWorkspaceTab(Object workspace) {
        return switch (workspace) {
            case RecordingWorkspace recordingWorkspace -> toRecordingTab(recordingWorkspace);
            case HeapDumpWorkspace heapDumpWorkspace -> toHeapDumpTab(heapDumpWorkspace);
            case LiveJvmWorkspace liveJvmWorkspace -> toLiveJvmTab(liveJvmWorkspace);
            default -> throw new IllegalArgumentException("Unsupported workspace tab: " + workspace);
        };
    }

    private void selectRecordingTab(RecordingWorkspace workspace) {
        selectWorkspaceTab(workspace, null, null);
    }

    private void selectWorkspaceTab(RecordingWorkspace recordingWorkspace, HeapDumpWorkspace heapDumpWorkspace,
            LiveJvmWorkspace liveJvmWorkspace) {
        Object workspace = liveJvmWorkspace != null ? liveJvmWorkspace
                : heapDumpWorkspace != null ? heapDumpWorkspace : recordingWorkspace;
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
        loadedWorkspace = null;
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
        bindThreadHistogram(workspace == null ? null : workspace.javaAppOverviewViewModel());
        bindSecurity(workspace == null ? null : workspace.securityViewModel());
        bindNativeLibraries(workspace == null ? null : workspace.nativeLibraryViewModel());
        bindThreadDumps(workspace == null ? null : workspace.threadDumpViewModel());
        bindHeap(workspace == null ? null : workspace.heapViewModel());
        bindLeaks(workspace == null ? null : workspace.leakSuspectsViewModel());
        bindTlab(workspace == null ? null : workspace.tlabViewModel());
        bindJvmInfo(workspace == null ? null : workspace.jvmInfoViewModel());
        bindGcConfig(workspace == null ? null : workspace.gcConfigViewModel());
        bindGcSummary(workspace == null ? null : workspace.gcSummaryViewModel());
        bindGcDetails(workspace == null ? null : workspace.gcDetailsViewModel());
        bindG1Gc(workspace == null ? null : workspace.g1GcViewModel());
        bindJavaFxEvents(workspace == null ? null : workspace.javaFxEventsViewModel());
        bindCompilations(workspace == null ? null : workspace.compilationsViewModel());
        bindCodeCache(workspace == null ? null : workspace.codeCacheViewModel());
        bindClassLoading(workspace == null ? null : workspace.classLoadingViewModel());
        bindVmOperations(workspace == null ? null : workspace.vmOperationsViewModel());
        bindEnvironment(workspace == null ? null : workspace.environmentViewModel());
        bindJfrMetadata(workspace == null ? null : workspace.jfrMetadataViewModel());
        bindAdvancedJfr(workspace == null ? null : workspace.advancedJfrViewModel());
        loadedWorkspace = workspace;
        loadSelectedWorkspaceSection();
    }

    private void showHeapDumpWorkspace(HeapDumpWorkspace workspace) {
        if (workspace == null) {
            bindHeapDumpAnalysis(null);
            return;
        }
        selectWorkspaceTab(null, workspace, null);
        bindHeapDumpAnalysis(workspace.viewModel());
    }

    private void showLiveJvmWorkspace(LiveJvmWorkspace workspace) {
        if (workspace == null) {
            return;
        }
        selectWorkspaceTab(null, null, workspace);
    }

    private void bindAdvancedJfr(AdvancedJfrViewModel nextViewModel) {
        if (advancedJfrViewModel != null) {
            advancedJfrViewModel.heatmapProperty().removeListener(advancedHeatmapListener);
        }
        advancedJfrSummaryLabel.textProperty().unbind();
        advancedJfrSelectedEventTypeLabel.textProperty().unbind();
        advancedJfrSelectedCountLabel.textProperty().unbind();
        advancedJfrMemorySummaryLabel.textProperty().unbind();
        advancedJfrMemoryDetailTitleLabel.textProperty().unbind();
        advancedJfrMemoryDetailArea.textProperty().unbind();
        advancedJfrHeatmapView.setHeatmap(null);
        advancedJfrHeatmapView.setOnCellSelected(cell -> { });
        rebindingAdvancedJfrMemory = true;
        try {
            advancedJfrMemoryTable.setItems(FXCollections.emptyObservableList());
            advancedJfrMemoryTable.getSelectionModel().clearSelection();
        } finally {
            rebindingAdvancedJfrMemory = false;
        }
        advancedJfrMemoryDetailTitleLabel.setText("");
        advancedJfrMemoryDetailArea.setText("");
        advancedJfrViewModel = nextViewModel;
        if (nextViewModel == null) {
            advancedJfrSummaryLabel.setText(i18n.get("advancedJfr.summary"));
            advancedJfrSelectedEventTypeLabel.setText("");
            advancedJfrSelectedCountLabel.setText("");
            advancedJfrMemorySummaryLabel.setText(i18n.get("advancedJfr.memory.summary"));
            return;
        }
        nextViewModel.heatmapProperty().addListener(advancedHeatmapListener);
        advancedJfrSummaryLabel.textProperty().bind(nextViewModel.summaryProperty());
        advancedJfrSelectedEventTypeLabel.textProperty().bind(nextViewModel.selectedEventTypeProperty());
        advancedJfrSelectedCountLabel.textProperty().bind(nextViewModel.selectedCountProperty());
        advancedJfrMemoryTable.setItems(nextViewModel.memoryIssues());
        bindAdvancedJfrMemoryText(nextViewModel);
        advancedJfrHeatmapView.setOnCellSelected(nextViewModel::selectCell);
        advancedJfrHeatmapView.setHeatmap(nextViewModel.heatmapProperty().get());
    }

    private void bindJfrMetadata(JfrMetadataViewModel nextViewModel) {
        if (jfrMetadataViewModel != null) {
            jfrMetadataViewModel.selectedEventTypeProperty().removeListener(metadataSelectedEventTypeListener);
        }
        metadataSummaryLabel.textProperty().unbind();
        metadataDetailArea.textProperty().unbind();
        metadataEventTypesTable.setItems(FXCollections.emptyObservableList());
        metadataEventTypesTable.getSelectionModel().clearSelection();
        metadataSummaryLabel.setText(i18n.get("metadata.summary"));
        metadataDetailArea.setText("");
        jfrMetadataViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        metadataSummaryLabel.textProperty().bind(nextViewModel.summaryProperty());
        metadataDetailArea.textProperty().bind(nextViewModel.selectedDetailProperty());
        metadataEventTypesTable.setItems(nextViewModel.eventTypesProperty());
        metadataEventTypesTable.getSelectionModel().select(nextViewModel.selectedEventTypeProperty().get());
        nextViewModel.selectedEventTypeProperty().addListener(metadataSelectedEventTypeListener);
    }

    private G1GcViewModel g1GcViewModel;

    private void bindG1Gc(G1GcViewModel nextViewModel) {
        if (g1GcViewModel != null) {
            g1GcViewModel.selectedRegionStateProperty().removeListener(g1GcSelectedRegionStateListener);
        }
        g1GcSummaryLabel.textProperty().unbind();
        g1GcDetailArea.textProperty().unbind();
        g1GcRegionSummaryTable.setItems(FXCollections.emptyObservableList());
        g1GcRegionStatesTable.setItems(FXCollections.emptyObservableList());
        g1GcPauseTable.setItems(FXCollections.emptyObservableList());
        g1GcRegionStatesTable.getSelectionModel().clearSelection();
        g1GcSummaryLabel.setText(i18n.get("g1Gc.summary"));
        g1GcDetailArea.setText("");
        g1GcViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        g1GcSummaryLabel.textProperty().bind(nextViewModel.summaryProperty());
        g1GcDetailArea.textProperty().bind(nextViewModel.selectedDetailProperty());
        g1GcRegionSummaryTable.setItems(nextViewModel.regionSummariesProperty());
        g1GcRegionStatesTable.setItems(nextViewModel.recentRegionStatesProperty());
        g1GcPauseTable.setItems(nextViewModel.gcPausesProperty());
        g1GcRegionStatesTable.getSelectionModel().select(nextViewModel.selectedRegionStateProperty().get());
        nextViewModel.selectedRegionStateProperty().addListener(g1GcSelectedRegionStateListener);
    }

    private JavaFxEventsViewModel javaFxEventsViewModel;

    private void bindJavaFxEvents(JavaFxEventsViewModel nextViewModel) {
        if (javaFxEventsViewModel != null) {
            javaFxEventsViewModel.selectedPulsePhaseProperty().removeListener(javaFxSelectedPulsePhaseListener);
        }
        javaFxEventsSummaryLabel.textProperty().unbind();
        javaFxEventsDetailArea.textProperty().unbind();
        javaFxEventsPulseTable.setItems(FXCollections.emptyObservableList());
        javaFxEventsPhaseTable.setItems(FXCollections.emptyObservableList());
        javaFxEventsInputTable.setItems(FXCollections.emptyObservableList());
        javaFxEventsPhaseTable.getSelectionModel().clearSelection();
        javaFxEventsSummaryLabel.setText(i18n.get("javaFxEvents.summary"));
        javaFxEventsDetailArea.setText("");
        javaFxEventsViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        javaFxEventsSummaryLabel.textProperty().bind(nextViewModel.summaryProperty());
        javaFxEventsDetailArea.textProperty().bind(nextViewModel.selectedDetailProperty());
        javaFxEventsPulseTable.setItems(nextViewModel.pulseSummariesProperty());
        javaFxEventsPhaseTable.setItems(nextViewModel.pulsePhasesProperty());
        javaFxEventsInputTable.setItems(nextViewModel.inputEventsProperty());
        javaFxEventsPhaseTable.getSelectionModel().select(nextViewModel.selectedPulsePhaseProperty().get());
        nextViewModel.selectedPulsePhaseProperty().addListener(javaFxSelectedPulsePhaseListener);
    }

    private void bindAdvancedJfrMemoryText(AdvancedJfrViewModel nextViewModel) {
        advancedJfrMemorySummaryLabel.textProperty().bind(Bindings.createStringBinding(
                () -> formatAdvancedJfrMemorySummary(nextViewModel.memoryReportProperty().get()),
                nextViewModel.memoryReportProperty(),
                i18n.localeProperty()));
        advancedJfrMemoryDetailTitleLabel.textProperty().bind(Bindings.createStringBinding(
                () -> formatAdvancedJfrMemoryIssueTitle(nextViewModel.selectedMemoryIssueProperty().get()),
                nextViewModel.selectedMemoryIssueProperty(),
                i18n.localeProperty()));
        advancedJfrMemoryDetailArea.textProperty().bind(Bindings.createStringBinding(
                () -> formatAdvancedJfrMemoryIssueDetails(nextViewModel.selectedMemoryIssueProperty().get()),
                nextViewModel.selectedMemoryIssueProperty(),
                i18n.localeProperty()));
    }

    private String formatAdvancedJfrMemorySummary(MemoryAnalysisReport report) {
        if (report == null) {
            return i18n.get("advancedJfr.memory.summary");
        }
        return i18n.format("advancedJfr.memory.summary.format",
                report.issues().size(),
                DisplayFormats.formatFileSize(report.totalEstimatedBytes()),
                DisplayFormats.formatInteger(report.totalCount()));
    }

    private String formatAdvancedJfrMemoryIssueTitle(MemoryIssue issue) {
        if (issue == null) {
            return "";
        }
        return i18n.format("advancedJfr.memory.detail.title", issue.severity(), issue.subject());
    }

    private String formatAdvancedJfrMemoryIssueDetails(MemoryIssue issue) {
        if (issue == null) {
            return "";
        }
        return String.join(System.lineSeparator(),
                i18n.format("advancedJfr.memory.detail.category", issue.category()),
                i18n.format("advancedJfr.memory.detail.estimatedBytes",
                        DisplayFormats.formatFileSize(issue.estimatedBytes())),
                i18n.format("advancedJfr.memory.detail.count", DisplayFormats.formatInteger(issue.count())),
                i18n.format("advancedJfr.memory.detail.score", DisplayFormats.formatPercent(issue.score())),
                i18n.format("advancedJfr.memory.detail.evidence", issue.evidence()),
                i18n.format("advancedJfr.memory.detail.recommendation", issue.recommendation()));
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
        if (recordingOpening) {
            return;
        }
        Platform.runLater(this::showOpenRecordingChooser);
    }

    private void showOpenRecordingChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(openRecordingChooserTitle(i18n));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(jfrRecordingsFilterDescription(i18n), "*.jfr"));
        java.io.File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) {
            return;
        }
        openRecordingInBackground(file.toPath());
    }

    private void openRecordingInBackground(Path path) {
        if (selectExistingRecordingWorkspace(path)) {
            return;
        }
        setRecordingOpening(true);
        setBackgroundWorkVisible(true);
        viewModel.showStatus(openingRecordingStatus(i18n, path));
        viewModel.showTaskSummary(i18n.get("taskSummary.openingRecording"));
        recordingOpenExecutor.execute(() -> {
            try {
                PreparedRecordingWorkspace preparedWorkspace = prepareRecordingWorkspace(path);
                onFxThread(() -> attachPreparedRecordingWorkspace(preparedWorkspace));
            } catch (RuntimeException exception) {
                LOGGER.atError()
                        .withThrowable(exception)
                        .log("Unable to open recording {}", path);
                onFxThread(() -> showOpenRecordingFailure(exception));
            }
        });
    }

    PreparedRecordingWorkspace prepareRecordingWorkspace(Path path) {
        RecordingSummary recording = recordingRepository.open(path);
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
        G1GcViewModel g1Gc = g1GcService != null ? new G1GcViewModel(g1GcService) : null;
        JavaFxEventsViewModel javaFxEvents = javaFxEventService != null ? new JavaFxEventsViewModel(javaFxEventService) : null;
        CompilationsViewModel compilationsVm = jvmInternalsService != null ? new CompilationsViewModel(jvmInternalsService) : null;
        CodeCacheViewModel codeCache = jvmInternalsService != null ? new CodeCacheViewModel(jvmInternalsService) : null;
        ClassLoadingViewModel classLoading = jvmInternalsService != null ? new ClassLoadingViewModel(jvmInternalsService) : null;
        VmOperationsViewModel vmOperations = jvmInternalsService != null ? new VmOperationsViewModel(jvmInternalsService) : null;
        EnvironmentViewModel environment = environmentService != null
                ? new EnvironmentViewModel(environmentService) : null;
        JavaAppOverviewViewModel javaAppOverview = javaAppService != null ? new JavaAppOverviewViewModel(javaAppService) : null;
        SecurityViewModel security = javaAppService != null ? new SecurityViewModel(javaAppService) : null;
        NativeLibraryViewModel nativeLibraries = javaAppService != null ? new NativeLibraryViewModel(javaAppService) : null;
        ThreadDumpViewModel threadDumps = javaAppService != null ? new ThreadDumpViewModel(javaAppService) : null;
        JfrMetadataViewModel metadata = jfrMetadataService != null
                ? new JfrMetadataViewModel(jfrMetadataService) : null;
        AdvancedJfrViewModel advancedJfr = advancedJfrAnalysisService != null
                ? new AdvancedJfrViewModel(advancedJfrAnalysisService) : null;
        return new PreparedRecordingWorkspace(recording, overview, events, analysis, profiling, exceptions, threads,
                fileio, socketio, locks, heap, leakSuspects, tlab, jvmInfo, gcConfig, gcSummary, gcDetails,
                compilationsVm, codeCache, classLoading, vmOperations, environment, javaAppOverview, security,
                nativeLibraries, threadDumps, metadata, g1Gc, javaFxEvents, advancedJfr);
    }

    private void attachPreparedRecordingWorkspace(PreparedRecordingWorkspace prepared) {
        prepared.overview().showRecording(prepared.recording(), i18n.format("overview.details.format",
                prepared.recording().path(),
                formatEventTime(prepared.recording().startTime()),
                formatEventTime(prepared.recording().endTime()),
                DisplayFormats.formatDuration(prepared.recording().durationMillis()),
                DisplayFormats.formatFileSize(prepared.recording().sizeBytes())));
        viewModel.openRecording(prepared.recording(), prepared.overview(), prepared.events(), prepared.analysis(),
                prepared.profiling(), prepared.exceptions(), prepared.threads(), prepared.fileio(),
                prepared.socketio(), prepared.locks(), prepared.heap(), prepared.leakSuspects(), prepared.tlab(),
                prepared.jvmInfo(), prepared.gcConfig(), prepared.gcSummary(), prepared.gcDetails(),
                prepared.compilations(), prepared.codeCache(), prepared.classLoading(), prepared.vmOperations(),
                prepared.environment(), prepared.javaAppOverview(), prepared.security(), prepared.nativeLibraries(),
                prepared.threadDumps(), prepared.metadata(), prepared.g1Gc(), prepared.javaFxEvents(),
                prepared.advancedJfr());
        viewModel.showStatus(i18n.format("status.openedRecording", prepared.recording().name()));
        setRecordingOpening(false);
    }

    private void openHeapDumpInBackground(Path path) {
        if (selectExistingHeapDumpWorkspace(path)) {
            return;
        }
        setBackgroundWorkVisible(true);
        viewModel.showStatus(openingHeapDumpStatus(i18n, path));
        viewModel.showTaskSummary(i18n.get("taskSummary.openingHeapDump"));
        HeapDumpAnalysisViewModel nextViewModel = new HeapDumpAnalysisViewModel(heapDumpAnalysisService,
                new VirtualThreadHeapDumpAnalysisExecutor(), i18n);
        HeapDumpWorkspace workspace = new HeapDumpWorkspace(path, nextViewModel);
        viewModel.openHeapDump(workspace);
        nextViewModel.stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != HeapDumpAnalysisState.ANALYZING) {
                setBackgroundWorkVisible(false);
                viewModel.showTaskSummary(nextViewModel.statusMessageProperty().get());
            }
        });
        nextViewModel.analyze(path);
    }

    private boolean selectExistingRecordingWorkspace(Path path) {
        if (viewModel.selectRecordingWorkspaceByPath(path)) {
            viewModel.showStatus(i18n.format("status.openedRecording", path.getFileName()));
            viewModel.showTaskSummary("");
            return true;
        }
        return false;
    }

    private boolean selectExistingHeapDumpWorkspace(Path path) {
        if (viewModel.selectHeapDumpWorkspaceByPath(path)) {
            viewModel.showStatus(openingHeapDumpStatus(i18n, path));
            viewModel.showTaskSummary("");
            return true;
        }
        return false;
    }

    private void showOpenRecordingFailure(RuntimeException exception) {
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        viewModel.showStatus(i18n.format("status.openRecordingFailed", message));
        viewModel.showTaskSummary("");
        setBackgroundWorkVisible(false);
        setRecordingOpening(false);
    }

    private void setRecordingOpening(boolean opening) {
        recordingOpening = opening;
        if (homeOpenRecordingButton != null) {
            homeOpenRecordingButton.setDisable(shouldDisableOpenRecordingButton(opening));
        }
        if (homeOpenHeapDumpButton != null) {
            homeOpenHeapDumpButton.setDisable(opening);
        }
    }

    private void setBackgroundWorkVisible(boolean visible) {
        if (progressBar == null) {
            return;
        }
        progressBar.setProgress(visible ? ProgressBar.INDETERMINATE_PROGRESS : 0);
        progressBar.setVisible(visible);
        progressBar.setManaged(visible);
    }

    private void onFxThread(Runnable runnable) {
        try {
            if (Platform.isFxApplicationThread()) {
                runnable.run();
            } else {
                Platform.runLater(runnable);
            }
        } catch (IllegalStateException exception) {
            runnable.run();
        }
    }

    private void loadSelectedWorkspaceSection() {
        RecordingWorkspace workspace = loadedWorkspace;
        if (workspace == null) {
            return;
        }
        loadWorkspaceSection(workspace, viewModel.selectedSectionProperty().get());
    }

    void preloadRecordingWorkspace(RecordingWorkspace workspace) {
        setBackgroundWorkVisible(false);
    }

    static List<String> preloadedWorkspaceSections() {
        return List.of();
    }

    void loadWorkspaceSection(RecordingWorkspace workspace, String sectionId) {
        String canonicalSectionId = canonicalLoadSectionId(sectionId);
        if (workspace == null) {
            return;
        }
        if (canonicalSectionId == null) {
            workspace.cancelPendingSectionLoads();
            return;
        }
        if (!workspace.markSectionLoading(canonicalSectionId)) {
            return;
        }
        setBackgroundWorkVisible(true);
        viewModel.showTaskSummary(i18n.format("taskSummary.preparingSection", displayNameForSection(sectionId)));
        recordingOpenExecutor.execute(() -> {
            if (!workspace.shouldLoadSection(canonicalSectionId)) {
                boolean stillLoading = workspace.markSectionLoadSkipped(canonicalSectionId);
                if (!stillLoading) {
                    onFxThread(() -> setBackgroundWorkVisible(false));
                }
                return;
            }
            try {
                loadWorkspaceSectionNow(workspace, canonicalSectionId);
                boolean stillLoading = workspace.markSectionLoaded(canonicalSectionId);
                onFxThread(() -> viewModel.showTaskSummary(i18n.get("taskSummary.recordingReady")));
                if (!stillLoading) {
                    onFxThread(() -> setBackgroundWorkVisible(false));
                }
            } catch (RuntimeException exception) {
                LOGGER.atError()
                        .withThrowable(exception)
                        .log("Unable to load recording section {} for {}",
                                canonicalSectionId, workspace.recording().path());
                boolean stillLoading = workspace.markSectionLoadFailed(canonicalSectionId);
                onFxThread(() -> viewModel.showTaskSummary(i18n.format("taskSummary.sectionFailed",
                        displayNameForSection(sectionId))));
                if (!stillLoading) {
                    onFxThread(() -> setBackgroundWorkVisible(false));
                }
            }
        });
    }

    private String canonicalLoadSectionId(String sectionId) {
        return switch (sectionId) {
            case null -> null;
            case "home", "overview", "javaApplication", "jvmInternals", "environment", "jvms", "settings" -> null;
            case "envVars", "sysProps", "recordingInfo", "agents", "constantPools" -> "processes";
            default -> sectionId;
        };
    }

    private void loadWorkspaceSectionNow(RecordingWorkspace workspace, String sectionId) {
        RecordingSummary recording = workspace.recording();
        switch (sectionId) {
            case "analysis" -> workspace.ruleResultsViewModel().analyze(recording);
            case "events" -> workspace.eventBrowserViewModel().loadRecording(recording);
            case "metadata" -> loadIfPresent(workspace.jfrMetadataViewModel(), recording);
            case "advancedJfr" -> loadIfPresent(workspace.advancedJfrViewModel(), recording);
            case "profiling" -> loadIfPresent(workspace.profilingViewModel(), recording);
            case "exceptions" -> loadIfPresent(workspace.exceptionViewModel(), recording);
            case "threads" -> loadIfPresent(workspace.threadViewModel(), recording);
            case "fileio" -> loadIfPresent(workspace.fileIOViewModel(), recording);
            case "socketio" -> loadIfPresent(workspace.socketIOViewModel(), recording);
            case "locks" -> loadIfPresent(workspace.lockViewModel(), recording);
            case "threadHistogram" -> loadIfPresent(workspace.javaAppOverviewViewModel(), recording);
            case "security" -> loadIfPresent(workspace.securityViewModel(), recording);
            case "nativeLibraries" -> loadIfPresent(workspace.nativeLibraryViewModel(), recording);
            case "threadDumps" -> loadIfPresent(workspace.threadDumpViewModel(), recording);
            case "heap" -> loadIfPresent(workspace.heapViewModel(), recording);
            case "leaks" -> loadIfPresent(workspace.leakSuspectsViewModel(), recording);
            case "tlab" -> loadIfPresent(workspace.tlabViewModel(), recording);
            case "jvmInfo" -> loadIfPresent(workspace.jvmInfoViewModel(), recording);
            case "gcConfig" -> loadIfPresent(workspace.gcConfigViewModel(), recording);
            case "gcSummary" -> loadIfPresent(workspace.gcSummaryViewModel(), recording);
            case "gcDetails" -> loadIfPresent(workspace.gcDetailsViewModel(), recording);
            case "g1Gc" -> loadIfPresent(workspace.g1GcViewModel(), recording);
            case "javaFxEvents" -> loadIfPresent(workspace.javaFxEventsViewModel(), recording);
            case "compilations" -> loadIfPresent(workspace.compilationsViewModel(), recording);
            case "codeCache" -> loadIfPresent(workspace.codeCacheViewModel(), recording);
            case "classLoading" -> loadIfPresent(workspace.classLoadingViewModel(), recording);
            case "vmOperations" -> loadIfPresent(workspace.vmOperationsViewModel(), recording);
            case "processes", "envVars", "sysProps", "recordingInfo", "agents", "constantPools" ->
                    loadIfPresent(workspace.environmentViewModel(), recording);
            default -> {
            }
        }
    }

    private String displayNameForSection(String sectionId) {
        String key = "nav." + sectionId;
        String value = i18n.get(key);
        return value.equals(key) ? sectionId : value;
    }

    private void loadIfPresent(ProfilingViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(ExceptionViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(ThreadViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(FileIOViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(SocketIOViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(LockViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(JavaAppOverviewViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(SecurityViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(NativeLibraryViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(ThreadDumpViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(HeapViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(LeakSuspectsViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(TlabViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(JvmInfoViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(GcConfigViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(GcSummaryViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(GcDetailsViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(CompilationsViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(CodeCacheViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(ClassLoadingViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(VmOperationsViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(EnvironmentViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(JfrMetadataViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(G1GcViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(JavaFxEventsViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    private void loadIfPresent(AdvancedJfrViewModel viewModel, RecordingSummary recording) {
        if (viewModel != null) {
            viewModel.load(recording);
        }
    }

    record PreparedRecordingWorkspace(
            RecordingSummary recording,
            OverviewViewModel overview,
            EventBrowserViewModel events,
            RuleResultsViewModel analysis,
            ProfilingViewModel profiling,
            ExceptionViewModel exceptions,
            ThreadViewModel threads,
            FileIOViewModel fileio,
            SocketIOViewModel socketio,
            LockViewModel locks,
            HeapViewModel heap,
            LeakSuspectsViewModel leakSuspects,
            TlabViewModel tlab,
            JvmInfoViewModel jvmInfo,
            GcConfigViewModel gcConfig,
            GcSummaryViewModel gcSummary,
            GcDetailsViewModel gcDetails,
            CompilationsViewModel compilations,
            CodeCacheViewModel codeCache,
            ClassLoadingViewModel classLoading,
            VmOperationsViewModel vmOperations,
            EnvironmentViewModel environment,
            JavaAppOverviewViewModel javaAppOverview,
            SecurityViewModel security,
            NativeLibraryViewModel nativeLibraries,
            ThreadDumpViewModel threadDumps,
            JfrMetadataViewModel metadata,
            G1GcViewModel g1Gc,
            JavaFxEventsViewModel javaFxEvents,
            AdvancedJfrViewModel advancedJfr) {
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
        TableColumn<JvmFlag, String> nameCol = localizedColumn("jvmInfo.column.flag");
        nameCol.setPrefWidth(260);
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().name()));
        TableColumn<JvmFlag, String> valueCol = localizedColumn("jvmInfo.column.value");
        valueCol.setPrefWidth(320);
        valueCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().value()));
        TableColumn<JvmFlag, String> originCol = localizedColumn("jvmInfo.column.origin");
        originCol.setPrefWidth(160);
        originCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().origin()));
        jvmFlagsTable.getColumns().setAll(List.of(nameCol, valueCol, originCol));
    }

    private void configureJvmFlagChangesTable() {
        TableColumn<JvmFlagChange, String> timeCol = localizedColumn("jvmInfo.column.time");
        timeCol.setPrefWidth(220);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        TableColumn<JvmFlagChange, String> flagCol = localizedColumn("jvmInfo.column.flag");
        flagCol.setPrefWidth(240);
        flagCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().flagName()));
        TableColumn<JvmFlagChange, String> oldCol = localizedColumn("jvmInfo.column.oldValue");
        oldCol.setPrefWidth(240);
        oldCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().oldValue()));
        TableColumn<JvmFlagChange, String> newCol = localizedColumn("jvmInfo.column.newValue");
        newCol.setPrefWidth(240);
        newCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().newValue()));
        TableColumn<JvmFlagChange, String> originCol = localizedColumn("jvmInfo.column.origin");
        originCol.setPrefWidth(160);
        originCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().origin()));
        jvmFlagChangesTable.getColumns().setAll(List.of(timeCol, flagCol, oldCol, newCol, originCol));
    }

    private void configureGcSummaryTable() {
        TableColumn<GcSummary, String> genCol = localizedColumn("gcSummary.column.generation");
        genCol.setPrefWidth(180);
        genCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().generation()));
        TableColumn<GcSummary, String> countCol = localizedColumn("common.column.count");
        countCol.setPrefWidth(90);
        countCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().collectionCount())));
        TableColumn<GcSummary, String> totalCol = localizedColumn("gcSummary.column.total");
        totalCol.setPrefWidth(120);
        totalCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDuration(data.getValue().totalDurationMillis())));
        TableColumn<GcSummary, String> avgCol = localizedColumn("gcSummary.column.average");
        avgCol.setPrefWidth(120);
        avgCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDurationMillis(data.getValue().avgDurationMillis())));
        TableColumn<GcSummary, String> maxCol = localizedColumn("gcSummary.column.max");
        maxCol.setPrefWidth(120);
        maxCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDuration(data.getValue().maxDurationMillis())));
        gcSummaryTable.getColumns().setAll(List.of(genCol, countCol, totalCol, avgCol, maxCol));
    }

    private void configureGcEventsTable() {
        TableColumn<GcEvent, String> idCol = localizedColumn("gc.column.id");
        idCol.setPrefWidth(80);
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().gcId())));
        TableColumn<GcEvent, String> nameCol = localizedColumn("common.column.name");
        nameCol.setPrefWidth(220);
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().name()));
        TableColumn<GcEvent, String> causeCol = localizedColumn("gcDetails.column.cause");
        causeCol.setPrefWidth(260);
        causeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().cause()));
        TableColumn<GcEvent, String> pauseCol = localizedColumn("gcDetails.column.longestPause");
        pauseCol.setPrefWidth(130);
        pauseCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().longestPauseMicros())));
        TableColumn<GcEvent, String> totalPauseCol = localizedColumn("gcDetails.column.totalPause");
        totalPauseCol.setPrefWidth(120);
        totalPauseCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().totalPauseMicros())));
        TableColumn<GcEvent, String> timeCol = localizedColumn("common.column.time");
        timeCol.setPrefWidth(220);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        gcEventsTable.getColumns().setAll(List.of(idCol, nameCol, causeCol, pauseCol, totalPauseCol, timeCol));
    }

    private void configureGcReferenceStatsTable() {
        TableColumn<GcReferenceStat, String> idCol = localizedColumn("gc.column.id");
        idCol.setPrefWidth(80);
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().gcId())));
        TableColumn<GcReferenceStat, String> typeCol = localizedColumn("gcDetails.column.referenceType");
        typeCol.setPrefWidth(220);
        typeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().referenceType()));
        TableColumn<GcReferenceStat, String> countCol = localizedColumn("common.column.count");
        countCol.setPrefWidth(100);
        countCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().count())));
        gcReferenceStatsTable.getColumns().setAll(List.of(idCol, typeCol, countCol));
    }

    private void configureGcHeapSummaryTable() {
        TableColumn<GcHeapSummary, String> idCol = localizedColumn("gc.column.id");
        idCol.setPrefWidth(80);
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().gcId())));
        TableColumn<GcHeapSummary, String> whenCol = localizedColumn("gcDetails.column.when");
        whenCol.setPrefWidth(120);
        whenCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().when()));
        TableColumn<GcHeapSummary, String> usedCol = localizedColumn("gcDetails.column.heapUsed");
        usedCol.setPrefWidth(130);
        usedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().heapUsed())));
        TableColumn<GcHeapSummary, String> committedCol = localizedColumn("gcDetails.column.heapCommitted");
        committedCol.setPrefWidth(150);
        committedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().heapCommitted())));
        TableColumn<GcHeapSummary, String> metaUsedCol = localizedColumn("gcDetails.column.metaspaceUsed");
        metaUsedCol.setPrefWidth(150);
        metaUsedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().metaspaceUsed())));
        TableColumn<GcHeapSummary, String> metaCommittedCol = localizedColumn("gcDetails.column.metaspaceCommitted");
        metaCommittedCol.setPrefWidth(180);
        metaCommittedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().metaspaceCommitted())));
        gcHeapSummaryTable.getColumns().setAll(List.of(idCol, whenCol, usedCol, committedCol, metaUsedCol, metaCommittedCol));
    }

    private void configureG1GcTables() {
        g1GcRegionSummaryTable.setPlaceholder(localizedTablePlaceholder("g1Gc.empty"));
        TableColumn<G1GcRegionSummary, String> typeCol = localizedColumn("g1Gc.column.type");
        typeCol.setPrefWidth(180);
        typeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().type()));
        TableColumn<G1GcRegionSummary, String> countCol = localizedColumn("common.column.count");
        countCol.setPrefWidth(100);
        countCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().regionCount())));
        TableColumn<G1GcRegionSummary, String> usedCol = localizedColumn("g1Gc.column.used");
        usedCol.setPrefWidth(140);
        usedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().usedBytes())));
        TableColumn<G1GcRegionSummary, String> capacityCol = localizedColumn("g1Gc.column.capacity");
        capacityCol.setPrefWidth(140);
        capacityCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().capacityBytes())));
        g1GcRegionSummaryTable.getColumns().setAll(List.of(typeCol, countCol, usedCol, capacityCol));

        g1GcRegionStatesTable.setPlaceholder(localizedTablePlaceholder("g1Gc.empty"));
        TableColumn<G1GcRegionState, String> regionCol = localizedColumn("g1Gc.column.region");
        regionCol.setPrefWidth(90);
        regionCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().regionIndex())));
        TableColumn<G1GcRegionState, String> eventKindCol = localizedColumn("g1Gc.column.eventKind");
        eventKindCol.setPrefWidth(120);
        eventKindCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().eventKind()));
        TableColumn<G1GcRegionState, String> stateTypeCol = localizedColumn("g1Gc.column.type");
        stateTypeCol.setPrefWidth(160);
        stateTypeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().type()));
        TableColumn<G1GcRegionState, String> previousTypeCol = localizedColumn("g1Gc.column.previousType");
        previousTypeCol.setPrefWidth(160);
        previousTypeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().previousType()));
        TableColumn<G1GcRegionState, String> stateUsedCol = localizedColumn("g1Gc.column.used");
        stateUsedCol.setPrefWidth(140);
        stateUsedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().usedBytes())));
        TableColumn<G1GcRegionState, String> stateCapacityCol = localizedColumn("g1Gc.column.capacity");
        stateCapacityCol.setPrefWidth(140);
        stateCapacityCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().capacityBytes())));
        TableColumn<G1GcRegionState, String> timeCol = localizedColumn("common.column.time");
        timeCol.setPrefWidth(220);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        g1GcRegionStatesTable.getColumns().setAll(List.of(regionCol, eventKindCol, stateTypeCol,
                previousTypeCol, stateUsedCol, stateCapacityCol, timeCol));
        g1GcRegionStatesTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (g1GcViewModel != null && newValue != g1GcViewModel.selectedRegionStateProperty().get()) {
                        g1GcViewModel.selectedRegionStateProperty().set(newValue);
                    }
                });

        g1GcPauseTable.setPlaceholder(localizedTablePlaceholder("g1Gc.pauses.empty"));
        TableColumn<GcEvent, String> pauseIdCol = localizedColumn("gc.column.id");
        pauseIdCol.setPrefWidth(80);
        pauseIdCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().gcId())));
        TableColumn<GcEvent, String> nameCol = localizedColumn("common.column.name");
        nameCol.setPrefWidth(180);
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().name()));
        TableColumn<GcEvent, String> causeCol = localizedColumn("gcDetails.column.cause");
        causeCol.setPrefWidth(240);
        causeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().cause()));
        TableColumn<GcEvent, String> totalPauseCol = localizedColumn("gcDetails.column.totalPause");
        totalPauseCol.setPrefWidth(120);
        totalPauseCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().totalPauseMicros())));
        TableColumn<GcEvent, String> pauseTimeCol = localizedColumn("common.column.time");
        pauseTimeCol.setPrefWidth(220);
        pauseTimeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        g1GcPauseTable.getColumns().setAll(List.of(pauseIdCol, nameCol, causeCol, totalPauseCol, pauseTimeCol));
    }

    private void configureJavaFxEventsTables() {
        javaFxEventsPhaseTable.setPlaceholder(localizedTablePlaceholder("javaFxEvents.empty"));
        TableColumn<JavaFxPulsePhase, String> pulseIdCol = localizedColumn("javaFxEvents.column.pulse");
        pulseIdCol.setPrefWidth(90);
        pulseIdCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().pulseId())));
        TableColumn<JavaFxPulsePhase, String> phaseNameCol = localizedColumn("javaFxEvents.column.phase");
        phaseNameCol.setPrefWidth(200);
        phaseNameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().phaseName()));
        TableColumn<JavaFxPulsePhase, String> phaseDurationCol = localizedColumn("common.column.duration");
        phaseDurationCol.setPrefWidth(130);
        phaseDurationCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().durationMicros())));
        TableColumn<JavaFxPulsePhase, String> phaseThreadCol = localizedColumn("common.column.thread");
        phaseThreadCol.setPrefWidth(220);
        phaseThreadCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().threadName()));
        TableColumn<JavaFxPulsePhase, String> phaseTimeCol = localizedColumn("common.column.time");
        phaseTimeCol.setPrefWidth(220);
        phaseTimeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        javaFxEventsPhaseTable.getColumns().setAll(List.of(pulseIdCol, phaseNameCol, phaseDurationCol,
                phaseThreadCol, phaseTimeCol));
        javaFxEventsPhaseTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (javaFxEventsViewModel != null
                            && newValue != javaFxEventsViewModel.selectedPulsePhaseProperty().get()) {
                        javaFxEventsViewModel.selectedPulsePhaseProperty().set(newValue);
                    }
                });

        javaFxEventsPulseTable.setPlaceholder(localizedTablePlaceholder("javaFxEvents.empty"));
        TableColumn<JavaFxPulseSummary, String> summaryPulseIdCol = localizedColumn("javaFxEvents.column.pulse");
        summaryPulseIdCol.setPrefWidth(90);
        summaryPulseIdCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().pulseId())));
        TableColumn<JavaFxPulseSummary, String> phaseCountCol = localizedColumn("javaFxEvents.column.phases");
        phaseCountCol.setPrefWidth(110);
        phaseCountCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().phaseCount())));
        TableColumn<JavaFxPulseSummary, String> totalDurationCol = localizedColumn("javaFxEvents.column.totalDuration");
        totalDurationCol.setPrefWidth(140);
        totalDurationCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().totalDurationMicros())));
        TableColumn<JavaFxPulseSummary, String> maxDurationCol = localizedColumn("javaFxEvents.column.maxDuration");
        maxDurationCol.setPrefWidth(140);
        maxDurationCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().maxPhaseDurationMicros())));
        TableColumn<JavaFxPulseSummary, String> pulseTimeCol = localizedColumn("common.column.time");
        pulseTimeCol.setPrefWidth(220);
        pulseTimeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        javaFxEventsPulseTable.getColumns().setAll(List.of(summaryPulseIdCol, phaseCountCol,
                totalDurationCol, maxDurationCol, pulseTimeCol));

        javaFxEventsInputTable.setPlaceholder(localizedTablePlaceholder("javaFxEvents.inputs.empty"));
        TableColumn<JavaFxInputEvent, String> inputTypeCol = localizedColumn("javaFxEvents.column.input");
        inputTypeCol.setPrefWidth(180);
        inputTypeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().inputType()));
        TableColumn<JavaFxInputEvent, String> inputDurationCol = localizedColumn("common.column.duration");
        inputDurationCol.setPrefWidth(130);
        inputDurationCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().durationMicros())));
        TableColumn<JavaFxInputEvent, String> inputThreadCol = localizedColumn("common.column.thread");
        inputThreadCol.setPrefWidth(220);
        inputThreadCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().threadName()));
        TableColumn<JavaFxInputEvent, String> inputTimeCol = localizedColumn("common.column.time");
        inputTimeCol.setPrefWidth(220);
        inputTimeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        javaFxEventsInputTable.getColumns().setAll(List.of(inputTypeCol, inputDurationCol, inputThreadCol,
                inputTimeCol));
    }

    private void configureCompilationsTable() {
        TableColumn<CompilationEvent, String> idCol = localizedColumn("common.column.id");
        idCol.setPrefWidth(80);
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().compilationId())));
        TableColumn<CompilationEvent, String> methodCol = localizedColumn("compilations.column.method");
        methodCol.setPrefWidth(620);
        methodCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().method()));
        TableColumn<CompilationEvent, String> okCol = localizedColumn("compilations.column.succeeded");
        okCol.setPrefWidth(100);
        okCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatBoolean(data.getValue().succeeded())));
        TableColumn<CompilationEvent, String> durCol = localizedColumn("common.column.duration");
        durCol.setPrefWidth(120);
        durCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().durationMicros())));
        TableColumn<CompilationEvent, String> sizeCol = localizedColumn("compilations.column.codeSize");
        sizeCol.setPrefWidth(110);
        sizeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().codeSize())));
        TableColumn<CompilationEvent, String> inlineCol = localizedColumn("compilations.column.inlined");
        inlineCol.setPrefWidth(100);
        inlineCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().inlinedBytes())));
        TableColumn<CompilationEvent, String> timeCol = localizedColumn("common.column.time");
        timeCol.setPrefWidth(220);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        compilationsTable.getColumns().setAll(List.of(idCol, methodCol, okCol, durCol, sizeCol, inlineCol, timeCol));
    }

    private void configureCompilationFailuresTable() {
        TableColumn<CompilationEvent, String> idCol = localizedColumn("common.column.id");
        idCol.setPrefWidth(80);
        idCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().compilationId())));
        TableColumn<CompilationEvent, String> methodCol = localizedColumn("compilations.column.method");
        methodCol.setPrefWidth(620);
        methodCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().method()));
        TableColumn<CompilationEvent, String> durCol = localizedColumn("common.column.duration");
        durCol.setPrefWidth(120);
        durCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().durationMicros())));
        TableColumn<CompilationEvent, String> timeCol = localizedColumn("common.column.time");
        timeCol.setPrefWidth(220);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        compilationFailuresTable.getColumns().setAll(List.of(idCol, methodCol, durCol, timeCol));
    }

    private void configureCodeCacheSweepsTable() {
        TableColumn<CodeCacheSweep, String> timeCol = localizedColumn("common.column.time");
        timeCol.setPrefWidth(220);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        TableColumn<CodeCacheSweep, String> idxCol = localizedColumn("codeCache.column.index");
        idxCol.setPrefWidth(80);
        idxCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().sweepIndex())));
        TableColumn<CodeCacheSweep, String> durCol = localizedColumn("common.column.duration");
        durCol.setPrefWidth(120);
        durCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().durationMicros())));
        TableColumn<CodeCacheSweep, String> flushedCol = localizedColumn("codeCache.column.flushed");
        flushedCol.setPrefWidth(100);
        flushedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().flushed())));
        TableColumn<CodeCacheSweep, String> sweptCol = localizedColumn("codeCache.column.swept");
        sweptCol.setPrefWidth(100);
        sweptCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().swept())));
        TableColumn<CodeCacheSweep, String> countCol = localizedColumn("codeCache.column.sweptCount");
        countCol.setPrefWidth(120);
        countCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().sweptCount())));
        codeCacheSweepsTable.getColumns().setAll(List.of(timeCol, idxCol, durCol, flushedCol, sweptCol, countCol));
    }

    private void configureCodeCacheStatsTable() {
        TableColumn<CodeCacheStats, String> timeCol = localizedColumn("common.column.time");
        timeCol.setPrefWidth(220);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        TableColumn<CodeCacheStats, String> heapCol = localizedColumn("codeCache.column.codeHeap");
        heapCol.setPrefWidth(220);
        heapCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().codeHeap()));
        TableColumn<CodeCacheStats, String> entriesCol = localizedColumn("codeCache.column.entries");
        entriesCol.setPrefWidth(100);
        entriesCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().entries())));
        TableColumn<CodeCacheStats, String> methodsCol = localizedColumn("codeCache.column.methods");
        methodsCol.setPrefWidth(100);
        methodsCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().methods())));
        TableColumn<CodeCacheStats, String> adaptersCol = localizedColumn("codeCache.column.adapters");
        adaptersCol.setPrefWidth(100);
        adaptersCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().adapters())));
        TableColumn<CodeCacheStats, String> unallocCol = localizedColumn("codeCache.column.unallocated");
        unallocCol.setPrefWidth(130);
        unallocCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().unallocated())));
        codeCacheStatsTable.getColumns().setAll(List.of(timeCol, heapCol, entriesCol, methodsCol, adaptersCol, unallocCol));
    }

    private void configureClassLoadingHistogramTable() {
        TableColumn<ClassloaderSummary, String> loaderCol = localizedColumn("classLoading.column.classloader");
        loaderCol.setPrefWidth(520);
        loaderCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().classloader()));
        TableColumn<ClassloaderSummary, String> loadedCol = localizedColumn("classLoading.column.loaded");
        loadedCol.setPrefWidth(100);
        loadedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().loadedCount())));
        TableColumn<ClassloaderSummary, String> unloadedCol = localizedColumn("classLoading.column.unloaded");
        unloadedCol.setPrefWidth(100);
        unloadedCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().unloadedCount())));
        classLoadingHistogramTable.getColumns().setAll(List.of(loaderCol, loadedCol, unloadedCol));
    }

    private void configureClassLoadingEventsTable() {
        TableColumn<ClassloadEvent, String> typeCol = localizedColumn("common.column.event");
        typeCol.setPrefWidth(160);
        typeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().eventType()));
        TableColumn<ClassloadEvent, String> timeCol = localizedColumn("common.column.time");
        timeCol.setPrefWidth(220);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        TableColumn<ClassloadEvent, String> classCol = localizedColumn("classLoading.column.class");
        classCol.setPrefWidth(360);
        classCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().loadedClass()));
        TableColumn<ClassloadEvent, String> defLoaderCol = localizedColumn("classLoading.column.definingLoader");
        defLoaderCol.setPrefWidth(320);
        defLoaderCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().definingClassloader()));
        TableColumn<ClassloadEvent, String> initLoaderCol = localizedColumn("classLoading.column.initiatingLoader");
        initLoaderCol.setPrefWidth(320);
        initLoaderCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().initiatingClassloader()));
        TableColumn<ClassloadEvent, String> durCol = localizedColumn("common.column.duration");
        durCol.setPrefWidth(120);
        durCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().durationMicros())));
        classLoadingEventsTable.getColumns().setAll(List.of(typeCol, timeCol, classCol, defLoaderCol, initLoaderCol, durCol));
    }

    private void configureClassLoadingStatsTable() {
        TableColumn<ClassloaderStatistics, String> loaderCol = localizedColumn("classLoading.column.classloader");
        loaderCol.setPrefWidth(420);
        loaderCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().classloader()));
        TableColumn<ClassloaderStatistics, String> parentCol = localizedColumn("classLoading.column.parent");
        parentCol.setPrefWidth(420);
        parentCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().parentClassloader()));
        TableColumn<ClassloaderStatistics, String> countCol = localizedColumn("classLoading.column.loadedClasses");
        countCol.setPrefWidth(130);
        countCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().loadedClassCount())));
        TableColumn<ClassloaderStatistics, String> chunkCol = localizedColumn("classLoading.column.chunkSize");
        chunkCol.setPrefWidth(120);
        chunkCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().anonymousBlockChunkSize())));
        TableColumn<ClassloaderStatistics, String> blockCol = localizedColumn("classLoading.column.blockSize");
        blockCol.setPrefWidth(120);
        blockCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(data.getValue().anonymousBlockSize())));
        TableColumn<ClassloaderStatistics, String> anonCol = localizedColumn("classLoading.column.anonymousClasses");
        anonCol.setPrefWidth(150);
        anonCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().anonymousClassCount())));
        classLoadingStatsTable.getColumns().setAll(List.of(loaderCol, parentCol, countCol, chunkCol, blockCol, anonCol));
    }

    private void configureVmOperationSummaryTable() {
        TableColumn<VmOperationSummary, String> opCol = localizedColumn("vmOperations.column.operation");
        opCol.setPrefWidth(360);
        opCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().operation()));
        TableColumn<VmOperationSummary, String> countCol = localizedColumn("common.column.count");
        countCol.setPrefWidth(100);
        countCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatInteger(data.getValue().count())));
        TableColumn<VmOperationSummary, String> totalCol = localizedColumn("common.column.totalDuration");
        totalCol.setPrefWidth(140);
        totalCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().totalDurationMicros())));
        TableColumn<VmOperationSummary, String> maxCol = localizedColumn("common.column.maxDuration");
        maxCol.setPrefWidth(140);
        maxCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().maxDurationMicros())));
        vmOperationSummaryTable.getColumns().setAll(List.of(opCol, countCol, totalCol, maxCol));
    }

    private void configureVmOperationEventsTable() {
        TableColumn<VmOperationEvent, String> timeCol = localizedColumn("common.column.time");
        timeCol.setPrefWidth(220);
        timeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatTimestamp(data.getValue().startTime(), ZoneId.systemDefault())));
        TableColumn<VmOperationEvent, String> opCol = localizedColumn("vmOperations.column.operation");
        opCol.setPrefWidth(320);
        opCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().operation()));
        TableColumn<VmOperationEvent, String> blockCol = localizedColumn("vmOperations.column.blocking");
        blockCol.setPrefWidth(100);
        blockCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatBoolean(data.getValue().blocking())));
        TableColumn<VmOperationEvent, String> safeCol = localizedColumn("vmOperations.column.safepoint");
        safeCol.setPrefWidth(100);
        safeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatBoolean(data.getValue().safepoint())));
        TableColumn<VmOperationEvent, String> durCol = localizedColumn("common.column.duration");
        durCol.setPrefWidth(120);
        durCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                DisplayFormats.formatMicros(data.getValue().durationMicros())));
        TableColumn<VmOperationEvent, String> threadCol = localizedColumn("common.column.thread");
        threadCol.setPrefWidth(260);
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
        gcHeapChart.setData(null);
        gcMetaspaceChart.setData(null);
        gcPauseChart.setData(null);
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
        nextViewModel.heapChartProperty().addListener((obs, old, val) -> gcHeapChart.setData(val));
        gcHeapChart.setData(nextViewModel.heapChartProperty().get());
        nextViewModel.metaspaceChartProperty().addListener((obs, old, val) -> gcMetaspaceChart.setData(val));
        gcMetaspaceChart.setData(nextViewModel.metaspaceChartProperty().get());
        nextViewModel.pauseChartProperty().addListener((obs, old, val) -> gcPauseChart.setData(val));
        gcPauseChart.setData(nextViewModel.pauseChartProperty().get());
    }

    private void bindCompilations(CompilationsViewModel nextViewModel) {
        compilationDurationChart.setData(null);
        compilationsTable.setItems(FXCollections.emptyObservableList());
        compilationFailuresTable.setItems(FXCollections.emptyObservableList());
        compilationsViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        compilationsTable.setItems(nextViewModel.compilations());
        compilationFailuresTable.setItems(nextViewModel.failures());
        nextViewModel.durationChartProperty().addListener((obs, old, val) -> compilationDurationChart.setData(val));
        compilationDurationChart.setData(nextViewModel.durationChartProperty().get());
    }

    private void bindCodeCache(CodeCacheViewModel nextViewModel) {
        codeCacheEntriesChart.setData(null);
        codeCacheSweepChart.setData(null);
        codeCacheSweepsTable.setItems(FXCollections.emptyObservableList());
        codeCacheStatsTable.setItems(FXCollections.emptyObservableList());
        codeCacheViewModel = nextViewModel;
        if (nextViewModel == null) {
            return;
        }
        codeCacheSweepsTable.setItems(nextViewModel.sweeps());
        codeCacheStatsTable.setItems(nextViewModel.statistics());
        nextViewModel.entriesChartProperty().addListener((obs, old, val) -> codeCacheEntriesChart.setData(val));
        codeCacheEntriesChart.setData(nextViewModel.entriesChartProperty().get());
        nextViewModel.sweepChartProperty().addListener((obs, old, val) -> codeCacheSweepChart.setData(val));
        codeCacheSweepChart.setData(nextViewModel.sweepChartProperty().get());
    }

    private void bindClassLoading(ClassLoadingViewModel nextViewModel) {
        classLoadingChart.setData(null);
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
        nextViewModel.chartProperty().addListener((obs, old, val) -> classLoadingChart.setData(val));
        classLoadingChart.setData(nextViewModel.chartProperty().get());
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
        processesTable.setPlaceholder(localizedTablePlaceholder("processes.empty"));
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
        envVarsTable.setPlaceholder(localizedTablePlaceholder("envVars.empty"));
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
        sysPropsTable.setPlaceholder(localizedTablePlaceholder("sysProps.empty"));
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
        recordingsTable.setPlaceholder(localizedTablePlaceholder("recordingInfo.empty"));
        TableColumn<ActiveRecordingInfo, String> idCol = new TableColumn<>();
        idCol.textProperty().bind(i18n.text("recordingInfo.column.id"));
        idCol.setPrefWidth(80);
        idCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().id()));
        TableColumn<ActiveRecordingInfo, String> nameCol = new TableColumn<>();
        nameCol.textProperty().bind(i18n.text("recordingInfo.column.name"));
        nameCol.setPrefWidth(260);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
        TableColumn<ActiveRecordingInfo, String> destCol = new TableColumn<>();
        destCol.textProperty().bind(i18n.text("recordingInfo.column.destination"));
        destCol.setPrefWidth(360);
        destCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().destination()));
        TableColumn<ActiveRecordingInfo, String> startCol = new TableColumn<>();
        startCol.textProperty().bind(i18n.text("recordingInfo.column.startTime"));
        startCol.setPrefWidth(180);
        startCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().startTime()));
        TableColumn<ActiveRecordingInfo, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("recordingInfo.column.eventCount"));
        countCol.setPrefWidth(120);
        countCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().eventCount()));
        useFormattedIntegerCells(countCol);
        recordingsTable.getColumns().setAll(List.of(idCol, nameCol, destCol, startCol, countCol));
    }

    private void configureSettingsTable() {
        settingsTable.setPlaceholder(localizedTablePlaceholder("recordingInfo.settings.empty"));
        TableColumn<ActiveSetting, String> eventCol = new TableColumn<>();
        eventCol.textProperty().bind(i18n.text("recordingInfo.settings.column.eventId"));
        eventCol.setPrefWidth(420);
        eventCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().eventId()));
        TableColumn<ActiveSetting, String> nameCol = new TableColumn<>();
        nameCol.textProperty().bind(i18n.text("recordingInfo.settings.column.name"));
        nameCol.setPrefWidth(200);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().settingName()));
        TableColumn<ActiveSetting, String> valCol = new TableColumn<>();
        valCol.textProperty().bind(i18n.text("recordingInfo.settings.column.value"));
        valCol.setPrefWidth(360);
        valCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().settingValue()));
        settingsTable.getColumns().setAll(List.of(eventCol, nameCol, valCol));
    }

    private void configureAgentsTable() {
        agentsTable.setPlaceholder(localizedTablePlaceholder("agents.empty"));
        TableColumn<AgentInfo, String> nameCol = new TableColumn<>();
        nameCol.textProperty().bind(i18n.text("agents.column.name"));
        nameCol.setPrefWidth(360);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
        TableColumn<AgentInfo, String> optCol = new TableColumn<>();
        optCol.textProperty().bind(i18n.text("agents.column.options"));
        optCol.setPrefWidth(420);
        optCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().options()));
        TableColumn<AgentInfo, String> initCol = new TableColumn<>();
        initCol.textProperty().bind(i18n.text("agents.column.initTime"));
        initCol.setPrefWidth(180);
        initCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().initTime()));
        TableColumn<AgentInfo, String> dynCol = new TableColumn<>();
        dynCol.textProperty().bind(i18n.text("agents.column.dynamic"));
        dynCol.setPrefWidth(90);
        dynCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatBoolean(cell.getValue().dynamic())));
        TableColumn<AgentInfo, String> kindCol = new TableColumn<>();
        kindCol.textProperty().bind(i18n.text("agents.column.kind"));
        kindCol.setPrefWidth(120);
        kindCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().kind()));
        agentsTable.getColumns().setAll(List.of(nameCol, optCol, initCol, dynCol, kindCol));
    }

    private void configureConstantPoolsTable() {
        constantPoolsTable.setPlaceholder(localizedTablePlaceholder("constantPools.empty"));
        TableColumn<ConstantPoolType, String> nameCol = new TableColumn<>();
        nameCol.textProperty().bind(i18n.text("constantPools.column.typeName"));
        nameCol.setPrefWidth(620);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().typeName()));
        TableColumn<ConstantPoolType, Number> countCol = new TableColumn<>();
        countCol.textProperty().bind(i18n.text("constantPools.column.entryCount"));
        countCol.setPrefWidth(130);
        countCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().entryCount()));
        useFormattedIntegerCells(countCol);
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

    private void attachExportMenus() {
        attachExportMenu(analysisTable);
        attachExportMenu(profilingTable);
        attachExportMenu(exceptionsTable);
        attachExportMenu(threadsTable);
        attachExportMenu(fileioHistogramTable);
        attachExportMenu(fileioEventTable);
        attachExportMenu(socketioHistogramTable);
        attachExportMenu(socketioEventTable);
        attachExportMenu(locksByClassTable);
        attachExportMenu(locksByAddressTable);
        attachExportMenu(locksByThreadTable);
        attachExportMenu(threadHistogramTable);
        attachExportMenu(securityTable);
        attachExportMenu(nativeLibrariesTable);
        attachExportMenu(threadDumpsTable);
        attachExportMenu(heapTable);
        attachExportMenu(leaksTable);
        attachExportMenu(tlabTable);
        attachExportMenu(jvmFlagsTable);
        attachExportMenu(jvmFlagChangesTable);
        attachExportMenu(gcEventsTable);
        attachExportMenu(gcReferenceStatsTable);
        attachExportMenu(gcHeapSummaryTable);
        attachExportMenu(compilationsTable);
        attachExportMenu(compilationFailuresTable);
        attachExportMenu(codeCacheSweepsTable);
        attachExportMenu(codeCacheStatsTable);
        attachExportMenu(classLoadingHistogramTable);
        attachExportMenu(classLoadingEventsTable);
        attachExportMenu(classLoadingStatsTable);
        attachExportMenu(vmOperationSummaryTable);
        attachExportMenu(vmOperationEventsTable);
        attachExportMenu(processesTable);
        attachExportMenu(envVarsTable);
        attachExportMenu(sysPropsTable);
        attachExportMenu(recordingsTable);
        attachExportMenu(settingsTable);
        attachExportMenu(agentsTable);
        attachExportMenu(constantPoolsTable);
    }

    private void attachExportMenu(TableView<?> table) {
        MenuItem exportItem = new MenuItem(i18n.get("context.exportCsv"));
        exportItem.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(i18n.get("fileChooser.saveCsv.title"));
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(i18n.get("fileChooser.csvFiles"), "*.csv"));
            java.io.File target = chooser.showSaveDialog(root.getScene().getWindow());
            if (target != null) {
                try {
                    CsvExport.export(table, target.toPath());
                    viewModel.showStatus(i18n.format("status.exported", target.getName()));
                } catch (Exception e) {
                    viewModel.showStatus(i18n.get("status.exportFailed"));
                }
            }
        });
        ContextMenu menu = new ContextMenu(exportItem);
        table.setContextMenu(menu);
    }


}
