package io.github.youngledo.jmcfx.ui.shell;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.nio.file.Path;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.ActiveRecordingInfo;
import io.github.youngledo.jmcfx.domain.model.ActiveSetting;
import io.github.youngledo.jmcfx.domain.model.AgentInfo;
import io.github.youngledo.jmcfx.domain.model.ClassloaderStatistics;
import io.github.youngledo.jmcfx.domain.model.ClassloaderSummary;
import io.github.youngledo.jmcfx.domain.model.ClassloadEvent;
import io.github.youngledo.jmcfx.domain.model.CodeCacheStats;
import io.github.youngledo.jmcfx.domain.model.CodeCacheSweep;
import io.github.youngledo.jmcfx.domain.model.CompilationEvent;
import io.github.youngledo.jmcfx.domain.model.ConstantPoolEntry;
import io.github.youngledo.jmcfx.domain.model.ConstantPoolType;
import io.github.youngledo.jmcfx.domain.model.DependencyGraphEdge;
import io.github.youngledo.jmcfx.domain.model.DiagnosticCommandInfo;
import io.github.youngledo.jmcfx.domain.model.EnvironmentVariable;
import io.github.youngledo.jmcfx.domain.model.EventColumn;
import io.github.youngledo.jmcfx.domain.model.EventHeatmap;
import io.github.youngledo.jmcfx.domain.model.EventDetails;
import io.github.youngledo.jmcfx.domain.model.EventFieldCondition;
import io.github.youngledo.jmcfx.domain.model.EventFieldDescriptor;
import io.github.youngledo.jmcfx.domain.model.EventFilter;
import io.github.youngledo.jmcfx.domain.model.EventFilterOperator;
import io.github.youngledo.jmcfx.domain.model.EventProperty;
import io.github.youngledo.jmcfx.domain.model.EventRow;
import io.github.youngledo.jmcfx.domain.model.EventSelectionProperties;
import io.github.youngledo.jmcfx.domain.model.EventStackFrame;
import io.github.youngledo.jmcfx.domain.model.EventThreadInfo;
import io.github.youngledo.jmcfx.domain.model.EventTiming;
import io.github.youngledo.jmcfx.domain.model.EventTypeNode;
import io.github.youngledo.jmcfx.domain.model.EventTypeNodeKind;
import io.github.youngledo.jmcfx.domain.model.EventTypeSelection;
import io.github.youngledo.jmcfx.domain.model.ExceptionGrouping;
import io.github.youngledo.jmcfx.domain.model.ExceptionSummary;
import io.github.youngledo.jmcfx.domain.model.FileIOEvent;
import io.github.youngledo.jmcfx.domain.model.FileIOHistogram;
import io.github.youngledo.jmcfx.domain.model.FlightRecordingInfo;
import io.github.youngledo.jmcfx.domain.model.G1GcRegionState;
import io.github.youngledo.jmcfx.domain.model.G1GcRegionSummary;
import io.github.youngledo.jmcfx.domain.model.GcEvent;
import io.github.youngledo.jmcfx.domain.model.GcHeapSummary;
import io.github.youngledo.jmcfx.domain.model.GcReferenceStat;
import io.github.youngledo.jmcfx.domain.model.GcSummary;
import io.github.youngledo.jmcfx.domain.model.HeapDumpAnalysisState;
import io.github.youngledo.jmcfx.domain.model.HeapDumpIssue;
import io.github.youngledo.jmcfx.domain.model.HeapClassHistogram;
import io.github.youngledo.jmcfx.domain.model.JavaFxInputEvent;
import io.github.youngledo.jmcfx.domain.model.JavaFxPulsePhase;
import io.github.youngledo.jmcfx.domain.model.JavaFxPulseSummary;
import io.github.youngledo.jmcfx.domain.model.ProcessInfo;
import io.github.youngledo.jmcfx.domain.model.HotMethod;
import io.github.youngledo.jmcfx.domain.model.JvmCapabilitySnapshot;
import io.github.youngledo.jmcfx.domain.model.JmcAgentPreset;
import io.github.youngledo.jmcfx.domain.model.JmcAgentTransform;
import io.github.youngledo.jmcfx.domain.model.JfrMetadataEventType;
import io.github.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import io.github.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import io.github.youngledo.jmcfx.domain.model.JmxSubscriptionSample;
import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.model.JvmConnectionSource;
import io.github.youngledo.jmcfx.domain.model.JvmConnectionState;
import io.github.youngledo.jmcfx.domain.model.JvmFlag;
import io.github.youngledo.jmcfx.domain.model.JvmFlagChange;
import io.github.youngledo.jmcfx.domain.model.JvmSessionSnapshot;
import io.github.youngledo.jmcfx.domain.model.LeakCandidate;
import io.github.youngledo.jmcfx.domain.model.LeakReferenceNode;
import io.github.youngledo.jmcfx.domain.model.LiveMetricDefinition;
import io.github.youngledo.jmcfx.domain.model.LiveMetricKind;
import io.github.youngledo.jmcfx.domain.model.LockGrouping;
import io.github.youngledo.jmcfx.domain.model.LockHistogram;
import io.github.youngledo.jmcfx.domain.model.MBeanAttributeInfo;
import io.github.youngledo.jmcfx.domain.model.MBeanNode;
import io.github.youngledo.jmcfx.domain.model.MBeanOperationInfo;
import io.github.youngledo.jmcfx.domain.model.MemoryAnalysisReport;
import io.github.youngledo.jmcfx.domain.model.MemoryIssue;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;
import io.github.youngledo.jmcfx.domain.model.Severity;
import io.github.youngledo.jmcfx.domain.model.SocketIOEvent;
import io.github.youngledo.jmcfx.domain.model.SocketIOGrouping;
import io.github.youngledo.jmcfx.domain.model.SocketIOHistogram;
import io.github.youngledo.jmcfx.domain.model.StackTreeNode;
import io.github.youngledo.jmcfx.domain.model.SystemProperty;
import io.github.youngledo.jmcfx.domain.model.ThreadDumpEntry;
import io.github.youngledo.jmcfx.domain.model.ThreadHistogramRow;
import io.github.youngledo.jmcfx.domain.model.ThreadSummary;
import io.github.youngledo.jmcfx.domain.model.TriggerActionType;
import io.github.youngledo.jmcfx.domain.model.TriggerEvent;
import io.github.youngledo.jmcfx.domain.model.TriggerOperator;
import io.github.youngledo.jmcfx.domain.model.TriggerRule;
import io.github.youngledo.jmcfx.domain.model.NativeLibraryEntry;
import io.github.youngledo.jmcfx.domain.model.TlabAllocation;
import io.github.youngledo.jmcfx.domain.model.VmOperationEvent;
import io.github.youngledo.jmcfx.domain.model.VmOperationSummary;
import io.github.youngledo.jmcfx.domain.model.X509CertificateEntry;
import io.github.youngledo.jmcfx.ui.advanced.AdvancedJfrViewModel;
import io.github.youngledo.jmcfx.ui.advanced.EventHeatmapView;
import io.github.youngledo.jmcfx.ui.analysis.AnalysisSeverityCell;
import io.github.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import io.github.youngledo.jmcfx.ui.events.VirtualThreadEventBrowserExecutor;
import io.github.youngledo.jmcfx.ui.environment.EnvironmentViewModel;
import io.github.youngledo.jmcfx.ui.exceptions.ExceptionViewModel;
import io.github.youngledo.jmcfx.ui.fileio.FileIOViewModel;
import io.github.youngledo.jmcfx.ui.gc.G1GcViewModel;
import io.github.youngledo.jmcfx.ui.heap.HeapViewModel;
import io.github.youngledo.jmcfx.ui.heapdump.HeapDumpAnalysisViewModel;
import io.github.youngledo.jmcfx.ui.heapdump.VirtualThreadHeapDumpAnalysisExecutor;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;
import io.github.youngledo.jmcfx.ui.javaapp.JavaAppOverviewViewModel;
import io.github.youngledo.jmcfx.ui.javaapp.NativeLibraryViewModel;
import io.github.youngledo.jmcfx.ui.javaapp.SecurityViewModel;
import io.github.youngledo.jmcfx.ui.javaapp.ThreadDumpViewModel;
import io.github.youngledo.jmcfx.ui.jfx.JavaFxEventsViewModel;
import io.github.youngledo.jmcfx.ui.jvm.ClassLoadingViewModel;
import io.github.youngledo.jmcfx.ui.jvm.CodeCacheViewModel;
import io.github.youngledo.jmcfx.ui.jvm.CompilationsViewModel;
import io.github.youngledo.jmcfx.ui.jvm.GcConfigViewModel;
import io.github.youngledo.jmcfx.ui.jvm.GcDetailsViewModel;
import io.github.youngledo.jmcfx.ui.jvm.GcSummaryViewModel;
import io.github.youngledo.jmcfx.ui.jvm.JvmInfoViewModel;
import io.github.youngledo.jmcfx.ui.jvm.VmOperationsViewModel;
import io.github.youngledo.jmcfx.ui.jvms.JvmBrowserViewModel;
import io.github.youngledo.jmcfx.ui.jvms.LiveJvmPersistenceOverview;
import io.github.youngledo.jmcfx.ui.jvms.LiveJvmOverviewMetric;
import io.github.youngledo.jmcfx.ui.leaks.LeakSuspectsViewModel;
import io.github.youngledo.jmcfx.ui.locks.LockViewModel;
import io.github.youngledo.jmcfx.ui.metadata.JfrMetadataViewModel;
import io.github.youngledo.jmcfx.ui.chart.TimelineChart;
import io.github.youngledo.jmcfx.ui.util.CsvExport;
import io.github.youngledo.jmcfx.ui.util.DisplayFormats;
import io.github.youngledo.jmcfx.ui.overview.OverviewViewModel;
import io.github.youngledo.jmcfx.ui.preferences.AppTheme;
import io.github.youngledo.jmcfx.ui.profiling.CallGraphDirection;
import io.github.youngledo.jmcfx.ui.profiling.CallGraphLayout;
import io.github.youngledo.jmcfx.ui.profiling.CallGraphLayoutBuilder;
import io.github.youngledo.jmcfx.ui.profiling.CallGraphView;
import io.github.youngledo.jmcfx.ui.profiling.ProfilingViewModel;
import io.github.youngledo.jmcfx.ui.rules.RuleResultDetail;
import io.github.youngledo.jmcfx.ui.rules.RuleResultsViewModel;
import io.github.youngledo.jmcfx.ui.socketio.SocketIOViewModel;
import io.github.youngledo.jmcfx.ui.threads.ThreadViewModel;
import io.github.youngledo.jmcfx.ui.tlab.TlabViewModel;

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

/// Controller for the code-first Live JVM pane view.
///
/// Owns Live JVM controls, table/chart setup, action wiring, and localized text.
public final class LiveJvmPaneController {

    private static final Logger LOGGER = LogManager.getLogger(LiveJvmPaneController.class);
    private static final Map<String, Set<LiveMetricKind>> DEFAULT_OVERVIEW_CHART_METRICS = Map.of(
            "Dashboard", Set.of(LiveMetricKind.THREAD_COUNT),
            "Processor", Set.of(LiveMetricKind.PROCESS_CPU_LOAD_PERCENT, LiveMetricKind.SYSTEM_CPU_LOAD_PERCENT),
            "Memory", Set.of(LiveMetricKind.HEAP_USED_PERCENT));

    private final Map<String, Map<LiveMetricKind, CheckBox>> overviewMetricToggles = new HashMap<>();
    private final Map<String, Map<LiveMetricKind, XYChart.Series<Number, Number>>> overviewChartSeries =
            new HashMap<>();
    private boolean updatingOverviewMetricSelection;
    private boolean overviewFullRefreshPending = true;
    private Timeline liveJvmOverviewRefreshTimeline;
    private final LiveJvmPaneView view;

    private VBox jvmsPane;
    private Label jvmsTitleLabel;
    private Button jvmsRefreshButton;
    private TextField jvmsManualUrlField;
    private Label jvmsManualUrlHintLabel;
    private TextField jvmsManualNameField;
    private Button jvmsSaveTargetButton;
    private Button jvmsRemoveSavedTargetButton;
    private Button jvmsRefreshJdpButton;
    private Button jvmsConnectButton;
    private Button jvmsDisconnectButton;
    private Label jvmsSelectedConnectionStatusLabel;
    private TableView<JvmConnection> jvmsTable;
    private VBox jvmsSessionDetailPane;
    private TabPane jvmsLiveTabs;
    private Tab jvmsOverviewTab;
    private Tab jvmsSessionTab;
    private Tab jvmsMBeanTab;
    private Tab jvmsDiagnosticsTab;
    private Tab jvmsTriggersTab;
    private Label jvmsSessionTitleLabel;
    private Label jvmsOverviewPersistenceTitleLabel;
    private Label jvmsOverviewPersistenceLabel;
    private TableView<LiveJvmOverviewMetric> jvmsOverviewPersistenceTable;
    private Label jvmsOverviewDashboardTitleLabel;
    private TabPane jvmsOverviewDashboardTabs;
    private Tab jvmsOverviewDashboardChartTab;
    private Tab jvmsOverviewDashboardTableTab;
    private FlowPane jvmsOverviewDashboardMetricToggles;
    private LineChart<Number, Number> jvmsOverviewDashboardChart;
    private TableView<LiveJvmOverviewMetric> jvmsOverviewDashboardTable;
    private Label jvmsOverviewProcessorTitleLabel;
    private TabPane jvmsOverviewProcessorTabs;
    private Tab jvmsOverviewProcessorChartTab;
    private Tab jvmsOverviewProcessorTableTab;
    private FlowPane jvmsOverviewProcessorMetricToggles;
    private LineChart<Number, Number> jvmsOverviewProcessorChart;
    private TableView<LiveJvmOverviewMetric> jvmsOverviewProcessorTable;
    private Label jvmsOverviewMemoryTitleLabel;
    private TabPane jvmsOverviewMemoryTabs;
    private Tab jvmsOverviewMemoryChartTab;
    private Tab jvmsOverviewMemoryTableTab;
    private FlowPane jvmsOverviewMemoryMetricToggles;
    private LineChart<Number, Number> jvmsOverviewMemoryChart;
    private TableView<LiveJvmOverviewMetric> jvmsOverviewMemoryTable;
    private Label jvmsOverviewErrorLabel;
    private Label jvmsRuntimeSummaryLabel;
    private ListView<JvmCapabilitySnapshot> jvmsCapabilitiesList;
    private Button jvmsStartRecordingButton;
    private Button jvmsStopRecordingButton;
    private TableView<FlightRecordingInfo> jvmsRecordingsTable;
    private Label jvmsRecordingStatusLabel;
    private Label jvmsSessionErrorLabel;
    private TreeView<MBeanNode> jvmsMBeanTree;
    private TableView<MBeanAttributeInfo> jvmsMBeanAttributesTable;
    private TableView<MBeanOperationInfo> jvmsMBeanOperationsTable;
    private TextField jvmsMBeanOperationArgumentsField;
    private Button jvmsRefreshMBeanButton;
    private Button jvmsInvokeMBeanOperationButton;
    private Label jvmsMBeanResultLabel;
    private Label jvmsMBeanErrorLabel;
    private TableView<DiagnosticCommandInfo> jvmsDiagnosticCommandsTable;
    private TextField jvmsDiagnosticArgumentsField;
    private Button jvmsExecuteDiagnosticCommandButton;
    private Button jvmsSaveDiagnosticOutputButton;
    private TextArea jvmsDiagnosticOutputArea;
    private Label jvmsDiagnosticErrorLabel;
    private TextField jvmsTriggerNameField;
    private ComboBox<LiveMetricDefinition> jvmsTriggerMetricCombo;
    private ComboBox<TriggerOperator> jvmsTriggerOperatorCombo;
    private TextField jvmsTriggerThresholdField;
    private ComboBox<TriggerActionType> jvmsTriggerActionCombo;
    private ComboBox<DiagnosticCommandInfo> jvmsTriggerCommandCombo;
    private Button jvmsAddTriggerButton;
    private Button jvmsRemoveTriggerButton;
    private Button jvmsEvaluateTriggersButton;
    private TableView<TriggerRule> jvmsTriggerRulesTable;
    private TableView<TriggerEvent> jvmsTriggerEventsTable;
    private Label jvmsTriggerErrorLabel;
    private Tab jvmsMonitoringTab;
    private Button jvmsAddMonitoringSubscriptionButton;
    private Button jvmsSampleSubscriptionButton;
    private Button jvmsAddNotificationSubscriptionButton;
    private Button jvmsStartNotificationsButton;
    private Button jvmsStopNotificationsButton;
    private TableView<JmxAttributeSubscription> jvmsMonitoringSubscriptionsTable;
    private LineChart<Number, Number> jvmsMonitoringChart;
    private TableView<JmxSubscriptionSample> jvmsMonitoringSamplesTable;
    private TableView<JmxNotificationEvent> jvmsMonitoringNotificationsTable;
    private Label jvmsMonitoringErrorLabel;
    private Tab jvmsAgentTab;
    private ComboBox<JmcAgentPreset> jvmsAgentPresetCombo;
    private Button jvmsRefreshAgentButton;
    private Button jvmsLoadAgentPresetButton;
    private Button jvmsApplyAgentConfigurationButton;
    private TableView<JmcAgentTransform> jvmsAgentTransformsTable;
    private Label jvmsAgentPresetDescriptionLabel;
    private Label jvmsAgentConfigurationTitleLabel;
    private TextArea jvmsAgentConfigurationArea;
    private Label jvmsAgentStatusLabel;
    private Label jvmsAgentApplyStatusLabel;

    private I18n i18n;
    private JvmBrowserViewModel jvmBrowserViewModel;

    LiveJvmPaneController() {
        this(new LiveJvmPaneView());
    }

    LiveJvmPaneController(I18n i18n) {
        this();
        this.i18n = java.util.Objects.requireNonNull(i18n, "i18n");
    }

    LiveJvmPaneController(LiveJvmPaneView view) {
        this.view = java.util.Objects.requireNonNull(view, "view");
        assignViewFields(view);
    }

    VBox root() {
        return view.root;
    }

    private void assignViewFields(LiveJvmPaneView view) {
        this.jvmsPane = view.root;
        this.jvmsTitleLabel = view.jvmsTitleLabel;
        this.jvmsRefreshButton = view.jvmsRefreshButton;
        this.jvmsManualUrlField = view.jvmsManualUrlField;
        this.jvmsManualUrlHintLabel = view.jvmsManualUrlHintLabel;
        this.jvmsManualNameField = view.jvmsManualNameField;
        this.jvmsSaveTargetButton = view.jvmsSaveTargetButton;
        this.jvmsRemoveSavedTargetButton = view.jvmsRemoveSavedTargetButton;
        this.jvmsRefreshJdpButton = view.jvmsRefreshJdpButton;
        this.jvmsConnectButton = view.jvmsConnectButton;
        this.jvmsDisconnectButton = view.jvmsDisconnectButton;
        this.jvmsSelectedConnectionStatusLabel = view.jvmsSelectedConnectionStatusLabel;
        this.jvmsTable = view.jvmsTable;
        this.jvmsSessionDetailPane = view.jvmsSessionDetailPane;
        this.jvmsLiveTabs = view.jvmsLiveTabs;
        this.jvmsOverviewTab = view.jvmsOverviewTab;
        this.jvmsSessionTab = view.jvmsSessionTab;
        this.jvmsMBeanTab = view.jvmsMBeanTab;
        this.jvmsDiagnosticsTab = view.jvmsDiagnosticsTab;
        this.jvmsTriggersTab = view.jvmsTriggersTab;
        this.jvmsSessionTitleLabel = view.jvmsSessionTitleLabel;
        this.jvmsOverviewPersistenceTitleLabel = view.jvmsOverviewPersistenceTitleLabel;
        this.jvmsOverviewPersistenceLabel = view.jvmsOverviewPersistenceLabel;
        this.jvmsOverviewPersistenceTable = view.jvmsOverviewPersistenceTable;
        this.jvmsOverviewDashboardTitleLabel = view.jvmsOverviewDashboardTitleLabel;
        this.jvmsOverviewDashboardTabs = view.jvmsOverviewDashboardTabs;
        this.jvmsOverviewDashboardChartTab = view.jvmsOverviewDashboardChartTab;
        this.jvmsOverviewDashboardTableTab = view.jvmsOverviewDashboardTableTab;
        this.jvmsOverviewDashboardMetricToggles = view.jvmsOverviewDashboardMetricToggles;
        this.jvmsOverviewDashboardChart = view.jvmsOverviewDashboardChart;
        this.jvmsOverviewDashboardTable = view.jvmsOverviewDashboardTable;
        this.jvmsOverviewProcessorTitleLabel = view.jvmsOverviewProcessorTitleLabel;
        this.jvmsOverviewProcessorTabs = view.jvmsOverviewProcessorTabs;
        this.jvmsOverviewProcessorChartTab = view.jvmsOverviewProcessorChartTab;
        this.jvmsOverviewProcessorTableTab = view.jvmsOverviewProcessorTableTab;
        this.jvmsOverviewProcessorMetricToggles = view.jvmsOverviewProcessorMetricToggles;
        this.jvmsOverviewProcessorChart = view.jvmsOverviewProcessorChart;
        this.jvmsOverviewProcessorTable = view.jvmsOverviewProcessorTable;
        this.jvmsOverviewMemoryTitleLabel = view.jvmsOverviewMemoryTitleLabel;
        this.jvmsOverviewMemoryTabs = view.jvmsOverviewMemoryTabs;
        this.jvmsOverviewMemoryChartTab = view.jvmsOverviewMemoryChartTab;
        this.jvmsOverviewMemoryTableTab = view.jvmsOverviewMemoryTableTab;
        this.jvmsOverviewMemoryMetricToggles = view.jvmsOverviewMemoryMetricToggles;
        this.jvmsOverviewMemoryChart = view.jvmsOverviewMemoryChart;
        this.jvmsOverviewMemoryTable = view.jvmsOverviewMemoryTable;
        this.jvmsOverviewErrorLabel = view.jvmsOverviewErrorLabel;
        this.jvmsRuntimeSummaryLabel = view.jvmsRuntimeSummaryLabel;
        this.jvmsCapabilitiesList = view.jvmsCapabilitiesList;
        this.jvmsStartRecordingButton = view.jvmsStartRecordingButton;
        this.jvmsStopRecordingButton = view.jvmsStopRecordingButton;
        this.jvmsRecordingsTable = view.jvmsRecordingsTable;
        this.jvmsRecordingStatusLabel = view.jvmsRecordingStatusLabel;
        this.jvmsSessionErrorLabel = view.jvmsSessionErrorLabel;
        this.jvmsMBeanTree = view.jvmsMBeanTree;
        this.jvmsMBeanAttributesTable = view.jvmsMBeanAttributesTable;
        this.jvmsMBeanOperationsTable = view.jvmsMBeanOperationsTable;
        this.jvmsMBeanOperationArgumentsField = view.jvmsMBeanOperationArgumentsField;
        this.jvmsRefreshMBeanButton = view.jvmsRefreshMBeanButton;
        this.jvmsInvokeMBeanOperationButton = view.jvmsInvokeMBeanOperationButton;
        this.jvmsMBeanResultLabel = view.jvmsMBeanResultLabel;
        this.jvmsMBeanErrorLabel = view.jvmsMBeanErrorLabel;
        this.jvmsDiagnosticCommandsTable = view.jvmsDiagnosticCommandsTable;
        this.jvmsDiagnosticArgumentsField = view.jvmsDiagnosticArgumentsField;
        this.jvmsExecuteDiagnosticCommandButton = view.jvmsExecuteDiagnosticCommandButton;
        this.jvmsSaveDiagnosticOutputButton = view.jvmsSaveDiagnosticOutputButton;
        this.jvmsDiagnosticOutputArea = view.jvmsDiagnosticOutputArea;
        this.jvmsDiagnosticErrorLabel = view.jvmsDiagnosticErrorLabel;
        this.jvmsTriggerNameField = view.jvmsTriggerNameField;
        this.jvmsTriggerMetricCombo = view.jvmsTriggerMetricCombo;
        this.jvmsTriggerOperatorCombo = view.jvmsTriggerOperatorCombo;
        this.jvmsTriggerThresholdField = view.jvmsTriggerThresholdField;
        this.jvmsTriggerActionCombo = view.jvmsTriggerActionCombo;
        this.jvmsTriggerCommandCombo = view.jvmsTriggerCommandCombo;
        this.jvmsAddTriggerButton = view.jvmsAddTriggerButton;
        this.jvmsRemoveTriggerButton = view.jvmsRemoveTriggerButton;
        this.jvmsEvaluateTriggersButton = view.jvmsEvaluateTriggersButton;
        this.jvmsTriggerRulesTable = view.jvmsTriggerRulesTable;
        this.jvmsTriggerEventsTable = view.jvmsTriggerEventsTable;
        this.jvmsTriggerErrorLabel = view.jvmsTriggerErrorLabel;
        this.jvmsMonitoringTab = view.jvmsMonitoringTab;
        this.jvmsAddMonitoringSubscriptionButton = view.jvmsAddMonitoringSubscriptionButton;
        this.jvmsSampleSubscriptionButton = view.jvmsSampleSubscriptionButton;
        this.jvmsAddNotificationSubscriptionButton = view.jvmsAddNotificationSubscriptionButton;
        this.jvmsStartNotificationsButton = view.jvmsStartNotificationsButton;
        this.jvmsStopNotificationsButton = view.jvmsStopNotificationsButton;
        this.jvmsMonitoringSubscriptionsTable = view.jvmsMonitoringSubscriptionsTable;
        this.jvmsMonitoringChart = view.jvmsMonitoringChart;
        this.jvmsMonitoringSamplesTable = view.jvmsMonitoringSamplesTable;
        this.jvmsMonitoringNotificationsTable = view.jvmsMonitoringNotificationsTable;
        this.jvmsMonitoringErrorLabel = view.jvmsMonitoringErrorLabel;
        this.jvmsAgentTab = view.jvmsAgentTab;
        this.jvmsAgentPresetCombo = view.jvmsAgentPresetCombo;
        this.jvmsRefreshAgentButton = view.jvmsRefreshAgentButton;
        this.jvmsLoadAgentPresetButton = view.jvmsLoadAgentPresetButton;
        this.jvmsApplyAgentConfigurationButton = view.jvmsApplyAgentConfigurationButton;
        this.jvmsAgentTransformsTable = view.jvmsAgentTransformsTable;
        this.jvmsAgentPresetDescriptionLabel = view.jvmsAgentPresetDescriptionLabel;
        this.jvmsAgentConfigurationTitleLabel = view.jvmsAgentConfigurationTitleLabel;
        this.jvmsAgentConfigurationArea = view.jvmsAgentConfigurationArea;
        this.jvmsAgentStatusLabel = view.jvmsAgentStatusLabel;
        this.jvmsAgentApplyStatusLabel = view.jvmsAgentApplyStatusLabel;
    }

    void configure(I18n i18n, JvmBrowserViewModel viewModel) {
        this.i18n = java.util.Objects.requireNonNull(i18n, "i18n");
        this.jvmBrowserViewModel = viewModel;
        bindLocalizedText();
        configureJvmBrowserTable();
        configureJvmRecordingsTable();
        configureMBeanBrowser();
        configureLiveJvmOverview();
        configureDiagnosticCommands();
        configureTriggers();
        configureJmxMonitoring();
        configureJmcAgentManager();
        bindJvmBrowser();
    }

    void refresh() {
        if (jvmBrowserViewModel != null) {
            jvmBrowserViewModel.refresh();
        }
    }

    void close() {
        stopLiveJvmOverviewRefreshTimer();
    }

    private static <T> void useFormattedIntegerCells(TableColumn<T, Number> column) {
        column.setCellFactory(col -> new TableCell<>() {
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

    private static Region emptyTablePlaceholder() {
        Region placeholder = new Region();
        placeholder.setManaged(false);
        return placeholder;
    }

    private static boolean canDisconnectJvm(JvmConnection selectedConnection) {
        return selectedConnection != null && selectedConnection.connected();
    }

    private static String jfrRecordingsFilterDescription(I18n i18n) {
        return i18n.get("fileChooser.jfrRecordings");
    }

    private static String saveRecordingInitialFileName(String recordingName) {
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

    private static String formatEventTimeForDisplay(java.time.Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return "";
        }
        ZoneId resolvedZone = zoneId == null ? ZoneId.systemDefault() : zoneId;
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(resolvedZone)
                .format(instant);
    }
    private void configureJvmBrowserTable() {
        jvmsTable.setPlaceholder(emptyTablePlaceholder());

        TableColumn<JvmConnection, String> pidCol = localizedColumn("jvms.column.pid");
        pidCol.setPrefWidth(90);
        pidCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().pid()));

        TableColumn<JvmConnection, String> nameCol = localizedColumn("jvms.column.name");
        nameCol.setPrefWidth(360);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().displayName()));

        TableColumn<JvmConnection, String> javaVersionCol = localizedColumn("jvms.column.javaVersion");
        javaVersionCol.setPrefWidth(140);
        javaVersionCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().javaVersion()));

        TableColumn<JvmConnection, String> stateCol = localizedColumn("jvms.column.state");
        stateCol.setPrefWidth(130);
        stateCol.setCellValueFactory(cell -> Bindings.createStringBinding(
                () -> formatJvmState(cell.getValue().state()), i18n.localeProperty()));

        TableColumn<JvmConnection, String> sourceCol = localizedColumn("jvms.column.source");
        sourceCol.setPrefWidth(100);
        sourceCol.setCellValueFactory(cell -> Bindings.createStringBinding(
                () -> formatJvmSource(cell.getValue().source()), i18n.localeProperty()));

        jvmsTable.getColumns().setAll(List.of(pidCol, nameCol, javaVersionCol, stateCol, sourceCol));
    }

    private void configureMBeanBrowser() {
        jvmsMBeanTree.setShowRoot(false);
        jvmsMBeanTree.setRoot(new TreeItem<>());
        jvmsMBeanTree.setCellFactory(tree -> new javafx.scene.control.TreeCell<>() {
            @Override
            protected void updateItem(MBeanNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });

        jvmsMBeanAttributesTable.setPlaceholder(emptyTablePlaceholder());
        TableColumn<MBeanAttributeInfo, String> attributeNameCol =
                localizedColumn("jvms.mbeans.attribute.name");
        attributeNameCol.setPrefWidth(180);
        attributeNameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));

        TableColumn<MBeanAttributeInfo, String> attributeTypeCol =
                localizedColumn("jvms.mbeans.attribute.type");
        attributeTypeCol.setPrefWidth(220);
        attributeTypeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().type()));

        TableColumn<MBeanAttributeInfo, String> attributeValueCol =
                localizedColumn("jvms.mbeans.attribute.value");
        attributeValueCol.setPrefWidth(360);
        attributeValueCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatMBeanAttributeValue(cell.getValue())));

        jvmsMBeanAttributesTable.getColumns().setAll(List.of(attributeNameCol, attributeTypeCol, attributeValueCol));

        jvmsMBeanOperationsTable.setPlaceholder(emptyTablePlaceholder());
        TableColumn<MBeanOperationInfo, String> operationNameCol =
                localizedColumn("jvms.mbeans.operation.name");
        operationNameCol.setPrefWidth(180);
        operationNameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));

        TableColumn<MBeanOperationInfo, String> operationSignatureCol =
                localizedColumn("jvms.mbeans.operation.signature");
        operationSignatureCol.setPrefWidth(360);
        operationSignatureCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatMBeanOperationSignature(cell.getValue())));

        TableColumn<MBeanOperationInfo, String> operationReturnTypeCol =
                localizedColumn("jvms.mbeans.operation.returnType");
        operationReturnTypeCol.setPrefWidth(180);
        operationReturnTypeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().returnType()));

        jvmsMBeanOperationsTable.getColumns().setAll(
                List.of(operationNameCol, operationSignatureCol, operationReturnTypeCol));
    }

    private void configureDiagnosticCommands() {
        jvmsDiagnosticCommandsTable.setPlaceholder(emptyTablePlaceholder());

        TableColumn<DiagnosticCommandInfo, String> nameCol =
                localizedColumn("jvms.diagnostics.column.name");
        nameCol.setPrefWidth(220);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                displayDiagnosticCommandName(cell.getValue())));

        TableColumn<DiagnosticCommandInfo, String> descriptionCol =
                localizedColumn("jvms.diagnostics.column.description");
        descriptionCol.setPrefWidth(420);
        descriptionCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().description()));

        TableColumn<DiagnosticCommandInfo, String> parametersCol =
                localizedColumn("jvms.diagnostics.column.parameters");
        parametersCol.setPrefWidth(260);
        parametersCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                formatDiagnosticCommandParameters(cell.getValue())));

        jvmsDiagnosticCommandsTable.getColumns().setAll(List.of(nameCol, descriptionCol, parametersCol));
    }

    private void configureLiveJvmOverview() {
        configureOverviewChart(jvmsOverviewDashboardChart);
        configureOverviewChart(jvmsOverviewProcessorChart);
        configureOverviewChart(jvmsOverviewMemoryChart);
        jvmsOverviewDashboardTabs.getSelectionModel().select(jvmsOverviewDashboardChartTab);
        jvmsOverviewProcessorTabs.getSelectionModel().select(jvmsOverviewProcessorChartTab);
        jvmsOverviewMemoryTabs.getSelectionModel().select(jvmsOverviewMemoryChartTab);
        configureOverviewTable(jvmsOverviewPersistenceTable, false);
        configureOverviewTable(jvmsOverviewDashboardTable, true);
        configureOverviewTable(jvmsOverviewProcessorTable, true);
        configureOverviewTable(jvmsOverviewMemoryTable, true);
    }

    private void configureOverviewChart(LineChart<Number, Number> chart) {
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);
        NumberAxis xAxis = (NumberAxis) chart.getXAxis();
        NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        xAxis.setTickLabelFormatter(new OverviewSequenceTickFormatter());
    }

    private void configureOverviewTable(TableView<LiveJvmOverviewMetric> table, boolean includeObservedColumn) {
        table.setPlaceholder(localizedTablePlaceholder("jvms.overview.metrics.empty"));

        TableColumn<LiveJvmOverviewMetric, String> metricCol =
                localizedColumn("jvms.overview.metric.name");
        metricCol.setPrefWidth(220);
        metricCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().label()));

        TableColumn<LiveJvmOverviewMetric, String> valueCol =
                localizedColumn("jvms.overview.metric.value");
        valueCol.setPrefWidth(160);
        valueCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().displayValue()));

        if (!includeObservedColumn) {
            table.getColumns().setAll(List.of(metricCol, valueCol));
            return;
        }

        TableColumn<LiveJvmOverviewMetric, String> observedCol =
                localizedColumn("jvms.overview.metric.observed");
        observedCol.setPrefWidth(180);
        observedCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                formatEventTimeForDisplay(cell.getValue().observedAt(), ZoneId.systemDefault())));

        table.getColumns().setAll(List.of(metricCol, valueCol, observedCol));
    }

    private void configureTriggers() {
        jvmsTriggerRulesTable.setPlaceholder(localizedTablePlaceholder("jvms.triggers.rules.empty"));
        jvmsTriggerEventsTable.setPlaceholder(localizedTablePlaceholder("jvms.triggers.events.empty"));

        jvmsTriggerMetricCombo.setCellFactory(combo -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(LiveMetricDefinition item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
            }
        });
        jvmsTriggerMetricCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(LiveMetricDefinition item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
            }
        });
        jvmsTriggerOperatorCombo.setItems(FXCollections.observableArrayList(TriggerOperator.values()));
        jvmsTriggerOperatorCombo.setCellFactory(combo -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(TriggerOperator item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.symbol());
            }
        });
        jvmsTriggerOperatorCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(TriggerOperator item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.symbol());
            }
        });
        jvmsTriggerActionCombo.setItems(FXCollections.observableArrayList(TriggerActionType.values()));
        jvmsTriggerActionCombo.setCellFactory(combo -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(TriggerActionType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatTriggerActionType(item));
            }
        });
        jvmsTriggerActionCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(TriggerActionType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatTriggerActionType(item));
            }
        });
        jvmsTriggerCommandCombo.setCellFactory(combo -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(DiagnosticCommandInfo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : displayDiagnosticCommandName(item));
            }
        });
        jvmsTriggerCommandCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(DiagnosticCommandInfo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : displayDiagnosticCommandName(item));
            }
        });

        TableColumn<TriggerRule, String> ruleNameCol = localizedColumn("jvms.triggers.rule.name");
        ruleNameCol.setPrefWidth(220);
        ruleNameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));

        TableColumn<TriggerRule, String> conditionCol = localizedColumn("jvms.triggers.rule.condition");
        conditionCol.setPrefWidth(320);
        conditionCol.setCellValueFactory(cell -> Bindings.createStringBinding(
                () -> formatTriggerCondition(cell.getValue()), i18n.localeProperty()));

        TableColumn<TriggerRule, String> actionCol = localizedColumn("jvms.triggers.rule.action");
        actionCol.setPrefWidth(260);
        actionCol.setCellValueFactory(cell -> Bindings.createStringBinding(
                () -> formatTriggerAction(cell.getValue()), i18n.localeProperty()));

        jvmsTriggerRulesTable.getColumns().setAll(List.of(ruleNameCol, conditionCol, actionCol));

        TableColumn<TriggerEvent, String> eventTimeCol = localizedColumn("jvms.triggers.event.time");
        eventTimeCol.setPrefWidth(180);
        eventTimeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                formatEventTimeForDisplay(cell.getValue().firedAt(), ZoneId.systemDefault())));

        TableColumn<TriggerEvent, String> eventRuleCol = localizedColumn("jvms.triggers.event.rule");
        eventRuleCol.setPrefWidth(220);
        eventRuleCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().ruleName()));

        TableColumn<TriggerEvent, String> eventValueCol = localizedColumn("jvms.triggers.event.value");
        eventValueCol.setPrefWidth(120);
        eventValueCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatTriggerEventValue(cell.getValue())));

        TableColumn<TriggerEvent, String> eventMessageCol = localizedColumn("jvms.triggers.event.message");
        eventMessageCol.setPrefWidth(440);
        eventMessageCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().message()));

        jvmsTriggerEventsTable.getColumns().setAll(List.of(eventTimeCol, eventRuleCol, eventValueCol, eventMessageCol));
    }

    private void configureJmxMonitoring() {
        jvmsMonitoringSubscriptionsTable.setPlaceholder(localizedTablePlaceholder("jvms.monitoring.subscriptions.empty"));
        jvmsMonitoringSamplesTable.setPlaceholder(localizedTablePlaceholder("jvms.monitoring.samples.empty"));
        jvmsMonitoringNotificationsTable.setPlaceholder(localizedTablePlaceholder("jvms.monitoring.notifications.empty"));
        jvmsMonitoringChart.setCreateSymbols(false);

        TableColumn<JmxAttributeSubscription, String> labelCol =
                localizedColumn("jvms.monitoring.subscription.label");
        labelCol.setPrefWidth(180);
        labelCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().label()));

        TableColumn<JmxAttributeSubscription, String> attributeCol =
                localizedColumn("jvms.monitoring.subscription.attribute");
        attributeCol.setPrefWidth(340);
        attributeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().objectName() + " / " + cell.getValue().attributeName()));

        TableColumn<JmxAttributeSubscription, String> intervalCol =
                localizedColumn("jvms.monitoring.subscription.interval");
        intervalCol.setPrefWidth(120);
        intervalCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().samplingInterval().toSeconds() + "s"));

        jvmsMonitoringSubscriptionsTable.getColumns().setAll(List.of(labelCol, attributeCol, intervalCol));

        TableColumn<JmxSubscriptionSample, String> sampleTimeCol =
                localizedColumn("jvms.monitoring.sample.time");
        sampleTimeCol.setPrefWidth(180);
        sampleTimeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                formatEventTimeForDisplay(cell.getValue().observedAt(), ZoneId.systemDefault())));

        TableColumn<JmxSubscriptionSample, String> sampleValueCol =
                localizedColumn("jvms.monitoring.sample.value");
        sampleValueCol.setPrefWidth(220);
        sampleValueCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().displayValue()));

        TableColumn<JmxSubscriptionSample, String> sampleUnitCol =
                localizedColumn("jvms.monitoring.sample.unit");
        sampleUnitCol.setPrefWidth(120);
        sampleUnitCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().unit()));

        jvmsMonitoringSamplesTable.getColumns().setAll(List.of(sampleTimeCol, sampleValueCol, sampleUnitCol));

        TableColumn<JmxNotificationEvent, String> eventTimeCol =
                localizedColumn("jvms.monitoring.notification.time");
        eventTimeCol.setPrefWidth(180);
        eventTimeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                formatEventTimeForDisplay(cell.getValue().observedAt(), ZoneId.systemDefault())));

        TableColumn<JmxNotificationEvent, String> eventTypeCol =
                localizedColumn("jvms.monitoring.notification.type");
        eventTypeCol.setPrefWidth(220);
        eventTypeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().type()));

        TableColumn<JmxNotificationEvent, String> eventMessageCol =
                localizedColumn("jvms.monitoring.notification.message");
        eventMessageCol.setPrefWidth(420);
        eventMessageCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().message()));

        jvmsMonitoringNotificationsTable.getColumns().setAll(List.of(eventTimeCol, eventTypeCol, eventMessageCol));
    }

    private void configureJmcAgentManager() {
        jvmsAgentTransformsTable.setPlaceholder(localizedTablePlaceholder("jvms.agent.transforms.empty"));

        jvmsAgentPresetCombo.setCellFactory(combo -> new ListCell<>() {
            @Override
            protected void updateItem(JmcAgentPreset item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });
        jvmsAgentPresetCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(JmcAgentPreset item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });

        TableColumn<JmcAgentTransform, String> idCol = localizedColumn("jvms.agent.transform.id");
        idCol.setPrefWidth(220);
        idCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().id()));

        TableColumn<JmcAgentTransform, String> classCol = localizedColumn("jvms.agent.transform.class");
        classCol.setPrefWidth(280);
        classCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().className()));

        TableColumn<JmcAgentTransform, String> methodCol = localizedColumn("jvms.agent.transform.method");
        methodCol.setPrefWidth(180);
        methodCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().methodName()));

        TableColumn<JmcAgentTransform, String> descriptorCol =
                localizedColumn("jvms.agent.transform.descriptor");
        descriptorCol.setPrefWidth(180);
        descriptorCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().methodDescriptor()));

        jvmsAgentTransformsTable.getColumns().setAll(List.of(idCol, classCol, methodCol, descriptorCol));
    }

    private void bindJvmBrowser() {
        if (jvmBrowserViewModel == null) {
            jvmsTable.setItems(FXCollections.emptyObservableList());
            jvmsRefreshButton.setDisable(true);
            jvmsManualUrlField.setDisable(true);
            jvmsManualUrlHintLabel.setDisable(true);
            jvmsManualNameField.setDisable(true);
            jvmsSaveTargetButton.setDisable(true);
            jvmsRemoveSavedTargetButton.setDisable(true);
            jvmsRefreshJdpButton.setDisable(true);
            jvmsConnectButton.setDisable(true);
            jvmsDisconnectButton.setDisable(true);
            jvmsSelectedConnectionStatusLabel.textProperty().bind(i18n.text("jvms.jdp.status.idle"));
            jvmsStartRecordingButton.setDisable(true);
            jvmsStopRecordingButton.setDisable(true);
            jvmsOverviewPersistenceTable.setItems(FXCollections.emptyObservableList());
            jvmsOverviewDashboardTable.setItems(FXCollections.emptyObservableList());
            jvmsOverviewProcessorTable.setItems(FXCollections.emptyObservableList());
            jvmsOverviewMemoryTable.setItems(FXCollections.emptyObservableList());
            jvmsOverviewDashboardChart.getData().clear();
            jvmsOverviewProcessorChart.getData().clear();
            jvmsOverviewMemoryChart.getData().clear();
            jvmsOverviewErrorLabel.setVisible(false);
            jvmsOverviewErrorLabel.setManaged(false);
            jvmsRecordingsTable.setItems(FXCollections.emptyObservableList());
            jvmsMBeanAttributesTable.setItems(FXCollections.emptyObservableList());
            jvmsMBeanOperationsTable.setItems(FXCollections.emptyObservableList());
            jvmsMBeanTree.setRoot(new TreeItem<>());
            jvmsMBeanOperationArgumentsField.setDisable(true);
            jvmsRefreshMBeanButton.setDisable(true);
            jvmsInvokeMBeanOperationButton.setDisable(true);
            jvmsDiagnosticCommandsTable.setItems(FXCollections.emptyObservableList());
            jvmsDiagnosticArgumentsField.setDisable(true);
            jvmsExecuteDiagnosticCommandButton.setDisable(true);
            jvmsSaveDiagnosticOutputButton.setDisable(true);
            jvmsDiagnosticOutputArea.setText("");
            jvmsDiagnosticOutputArea.setDisable(true);
            jvmsDiagnosticErrorLabel.setVisible(false);
            jvmsDiagnosticErrorLabel.setManaged(false);
            jvmsTriggerRulesTable.setItems(FXCollections.emptyObservableList());
            jvmsTriggerEventsTable.setItems(FXCollections.emptyObservableList());
            jvmsTriggerMetricCombo.setItems(FXCollections.emptyObservableList());
            jvmsTriggerOperatorCombo.setItems(FXCollections.observableArrayList(TriggerOperator.values()));
            jvmsTriggerActionCombo.setItems(FXCollections.observableArrayList(TriggerActionType.values()));
            jvmsTriggerCommandCombo.setItems(FXCollections.emptyObservableList());
            jvmsTriggerNameField.setDisable(true);
            jvmsTriggerMetricCombo.setDisable(true);
            jvmsTriggerOperatorCombo.setDisable(true);
            jvmsTriggerThresholdField.setDisable(true);
            jvmsTriggerActionCombo.setDisable(true);
            jvmsTriggerCommandCombo.setDisable(true);
            jvmsAddTriggerButton.setDisable(true);
            jvmsRemoveTriggerButton.setDisable(true);
            jvmsEvaluateTriggersButton.setDisable(true);
            jvmsTriggerErrorLabel.setVisible(false);
            jvmsTriggerErrorLabel.setManaged(false);
            jvmsMonitoringSubscriptionsTable.setItems(FXCollections.emptyObservableList());
            jvmsMonitoringSamplesTable.setItems(FXCollections.emptyObservableList());
            jvmsMonitoringNotificationsTable.setItems(FXCollections.emptyObservableList());
            jvmsMonitoringChart.getData().clear();
            jvmsAddMonitoringSubscriptionButton.setDisable(true);
            jvmsSampleSubscriptionButton.setDisable(true);
            jvmsAddNotificationSubscriptionButton.setDisable(true);
            jvmsStartNotificationsButton.setDisable(true);
            jvmsStopNotificationsButton.setDisable(true);
            jvmsMonitoringErrorLabel.setVisible(false);
            jvmsMonitoringErrorLabel.setManaged(false);
            jvmsAgentPresetCombo.setItems(FXCollections.emptyObservableList());
            jvmsAgentTransformsTable.setItems(FXCollections.emptyObservableList());
            jvmsAgentPresetDescriptionLabel.setText("");
            jvmsAgentConfigurationArea.setText("");
            jvmsAgentPresetCombo.setDisable(true);
            jvmsRefreshAgentButton.setDisable(true);
            jvmsLoadAgentPresetButton.setDisable(true);
            jvmsApplyAgentConfigurationButton.setDisable(true);
            jvmsAgentConfigurationArea.setDisable(true);
            jvmsAgentStatusLabel.setVisible(false);
            jvmsAgentStatusLabel.setManaged(false);
            jvmsAgentApplyStatusLabel.setVisible(false);
            jvmsAgentApplyStatusLabel.setManaged(false);
            jvmsSessionDetailPane.setVisible(false);
            jvmsSessionDetailPane.setManaged(false);
            return;
        }

        jvmsTable.setItems(jvmBrowserViewModel.connectionsProperty());
        jvmsTable.placeholderProperty().bind(Bindings.createObjectBinding(
                () -> jvmBrowserViewModel.refreshCompletedProperty().get()
                        && !jvmBrowserViewModel.loadingProperty().get()
                        && !jvmBrowserViewModel.errorProperty().get()
                                ? localizedTablePlaceholder("jvms.empty") : emptyTablePlaceholder(),
                jvmBrowserViewModel.refreshCompletedProperty(),
                jvmBrowserViewModel.loadingProperty(),
                jvmBrowserViewModel.errorProperty(),
                i18n.localeProperty()));
        jvmsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                jvmBrowserViewModel.selectedConnectionProperty().set(newValue));
        jvmBrowserViewModel.selectedConnectionProperty().addListener((observable, oldValue, newValue) ->
                jvmsTable.getSelectionModel().select(newValue));
        jvmsManualUrlField.textProperty().bindBidirectional(jvmBrowserViewModel.manualConnectionUrlProperty());
        jvmsManualNameField.textProperty().bindBidirectional(
                jvmBrowserViewModel.manualConnectionNameProperty());
        jvmsRefreshButton.disableProperty().bind(jvmBrowserViewModel.loadingProperty());
        jvmsManualUrlField.disableProperty().bind(jvmBrowserViewModel.loadingProperty());
        jvmsManualUrlHintLabel.disableProperty().bind(jvmBrowserViewModel.loadingProperty());
        jvmsManualNameField.disableProperty().bind(jvmBrowserViewModel.loadingProperty());
        jvmsRefreshJdpButton.disableProperty().bind(
                jvmBrowserViewModel.loadingProperty().or(jvmBrowserViewModel.jdpRefreshInProgressProperty()));
        jvmsSaveTargetButton.disableProperty().bind(jvmBrowserViewModel.loadingProperty()
                .or(Bindings.createBooleanBinding(
                        () -> jvmBrowserViewModel.manualConnectionUrlProperty().get().trim().isEmpty(),
                        jvmBrowserViewModel.manualConnectionUrlProperty())));
        jvmsRemoveSavedTargetButton.disableProperty().bind(jvmBrowserViewModel.loadingProperty()
                .or(Bindings.createBooleanBinding(
                        () -> {
                            JvmConnection selected = jvmBrowserViewModel.selectedConnectionProperty().get();
                            return selected == null || selected.source() != JvmConnectionSource.SAVED
                                    || selected.connected();
                        },
                        jvmBrowserViewModel.selectedConnectionProperty())));
        jvmsConnectButton.disableProperty().bind(jvmBrowserViewModel.loadingProperty()
                .or(Bindings.createBooleanBinding(
                        () -> jvmBrowserViewModel.manualConnectionUrlProperty().get().trim().isEmpty()
                                && !JvmBrowserViewModel.canConnectJvm(
                                        jvmBrowserViewModel.selectedConnectionProperty().get()),
                        jvmBrowserViewModel.manualConnectionUrlProperty(),
                        jvmBrowserViewModel.selectedConnectionProperty())));
        jvmsDisconnectButton.disableProperty().bind(jvmBrowserViewModel.loadingProperty()
                .or(Bindings.createBooleanBinding(
                        () -> !canDisconnectJvm(jvmBrowserViewModel.selectedConnectionProperty().get()),
                        jvmBrowserViewModel.selectedConnectionProperty())));
        jvmsSelectedConnectionStatusLabel.textProperty().bind(Bindings.createStringBinding(
                () -> selectedConnectionStatusText(
                        jvmBrowserViewModel.selectedConnectionProperty().get(),
                        jvmBrowserViewModel.jdpStatusMessageProperty().get()),
                jvmBrowserViewModel.selectedConnectionProperty(),
                jvmBrowserViewModel.jdpStatusMessageProperty(),
                i18n.localeProperty()));
        jvmsSessionDetailPane.visibleProperty().bind(jvmBrowserViewModel.selectedSessionProperty().isNotNull()
                .or(jvmBrowserViewModel.sessionErrorProperty()));
        jvmsSessionDetailPane.managedProperty().bind(jvmsSessionDetailPane.visibleProperty());
        jvmsSessionTitleLabel.textProperty().bind(i18n.text("jvms.session.title"));
        jvmsRuntimeSummaryLabel.textProperty().bind(Bindings.createStringBinding(
                () -> formatJvmRuntime(jvmBrowserViewModel.selectedSessionProperty().get()),
                jvmBrowserViewModel.selectedSessionProperty(), i18n.localeProperty()));
        jvmsCapabilitiesList.setItems(FXCollections.observableArrayList());
        jvmBrowserViewModel.selectedSessionProperty().addListener((observable, oldValue, newValue) ->
                jvmsCapabilitiesList.setItems(newValue == null
                        ? FXCollections.emptyObservableList()
                        : FXCollections.observableArrayList(newValue.capabilities())));
        jvmsCapabilitiesList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(JvmCapabilitySnapshot item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatJvmCapability(item));
            }
        });
        jvmsSessionErrorLabel.visibleProperty().bind(jvmBrowserViewModel.sessionErrorProperty());
        jvmsSessionErrorLabel.managedProperty().bind(jvmsSessionErrorLabel.visibleProperty());
        jvmsSessionErrorLabel.textProperty().bind(jvmBrowserViewModel.sessionErrorMessageProperty());
        bindLiveJvmOverview();
        jvmsRecordingsTable.setItems(jvmBrowserViewModel.flightRecordingsProperty());
        jvmsRecordingsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                jvmBrowserViewModel.selectedFlightRecordingProperty().set(newValue));
        jvmBrowserViewModel.selectedFlightRecordingProperty().addListener((observable, oldValue, newValue) ->
                jvmsRecordingsTable.getSelectionModel().select(newValue));
        jvmsStartRecordingButton.textProperty().bind(i18n.text("jvms.recordings.start"));
        jvmsStopRecordingButton.textProperty().bind(i18n.text("jvms.recordings.stopSave"));
        jvmsStartRecordingButton.disableProperty().bind(jvmBrowserViewModel.recordingLoadingProperty()
                .or(jvmBrowserViewModel.recordingControlAvailableProperty().not()));
        jvmsStopRecordingButton.disableProperty().bind(jvmBrowserViewModel.recordingLoadingProperty()
                .or(jvmBrowserViewModel.recordingControlAvailableProperty().not())
                .or(jvmBrowserViewModel.selectedFlightRecordingProperty().isNull()));
        jvmsRecordingStatusLabel.textProperty().bind(Bindings.createStringBinding(
                () -> jvmBrowserViewModel.recordingErrorProperty().get()
                        ? jvmBrowserViewModel.recordingErrorMessageProperty().get()
                        : jvmBrowserViewModel.recordingStatusMessageProperty().get(),
                jvmBrowserViewModel.recordingErrorProperty(),
                jvmBrowserViewModel.recordingErrorMessageProperty(),
                jvmBrowserViewModel.recordingStatusMessageProperty()));
        bindMBeanBrowser();
        bindDiagnosticCommands();
        bindTriggers();
        bindJmxMonitoring();
        bindJmcAgentManager();

        jvmsRefreshButton.setOnAction(event -> refresh());
        jvmsRefreshJdpButton.setOnAction(event -> jvmBrowserViewModel.refreshJdp());
        jvmsSaveTargetButton.setOnAction(event -> jvmBrowserViewModel.saveManualTarget());
        jvmsRemoveSavedTargetButton.setOnAction(event ->
                jvmBrowserViewModel.removeSelectedSavedTarget());
        jvmsConnectButton.setOnAction(event -> jvmBrowserViewModel.connectSelectedOrManual());
        jvmsDisconnectButton.setOnAction(event -> jvmBrowserViewModel.disconnectSelected());
        jvmsStartRecordingButton.setOnAction(event -> jvmBrowserViewModel.startFlightRecording());
        jvmsStopRecordingButton.setOnAction(event -> saveSelectedFlightRecording());
        jvmsRefreshMBeanButton.setOnAction(event -> jvmBrowserViewModel.refreshSelectedMBeanAttributes());
        jvmsInvokeMBeanOperationButton.setOnAction(event -> jvmBrowserViewModel.invokeSelectedMBeanOperation());
        jvmsExecuteDiagnosticCommandButton.setOnAction(event -> jvmBrowserViewModel.executeSelectedDiagnosticCommand());
        jvmsSaveDiagnosticOutputButton.setOnAction(event -> saveDiagnosticCommandOutput());
        jvmsAddTriggerButton.setOnAction(event -> jvmBrowserViewModel.addTriggerRule());
        jvmsRemoveTriggerButton.setOnAction(event -> removeSelectedTriggerRule());
        jvmsEvaluateTriggersButton.setOnAction(event -> jvmBrowserViewModel.evaluateTriggersNow());
        jvmsAddMonitoringSubscriptionButton.setOnAction(event -> addSelectedMonitoringSubscription());
        jvmsSampleSubscriptionButton.setOnAction(event -> jvmBrowserViewModel.sampleSelectedJmxSubscriptionNow());
        jvmsAddNotificationSubscriptionButton.setOnAction(event -> addSelectedNotificationSubscription());
        jvmsStartNotificationsButton.setOnAction(event -> jvmBrowserViewModel.startSelectedJmxNotifications());
        jvmsStopNotificationsButton.setOnAction(event -> jvmBrowserViewModel.stopSelectedJmxNotifications());
        jvmsRefreshAgentButton.setOnAction(event -> jvmBrowserViewModel.refreshJmcAgent());
        jvmsLoadAgentPresetButton.setOnAction(event -> jvmBrowserViewModel.loadSelectedJmcAgentPreset());
        jvmsApplyAgentConfigurationButton.setOnAction(event -> jvmBrowserViewModel.applyJmcAgentConfiguration());
        jvmsTable.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                jvmBrowserViewModel.connectSelected();
            }
        });
    }

    private void bindMBeanBrowser() {
        jvmsMBeanAttributesTable.setItems(jvmBrowserViewModel.mbeanAttributesProperty());
        jvmsMBeanOperationsTable.setItems(jvmBrowserViewModel.mbeanOperationsProperty());
        jvmBrowserViewModel.mbeanTreeProperty().addListener(
                (ListChangeListener<MBeanNode>) change -> rebuildMBeanTree());
        rebuildMBeanTree();

        jvmsMBeanTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                jvmBrowserViewModel.selectedMBeanProperty().set(newValue == null ? null : newValue.getValue()));
        jvmBrowserViewModel.selectedMBeanProperty().addListener((observable, oldValue, newValue) ->
                selectMBeanTreeNode(newValue));

        jvmsMBeanOperationsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                jvmBrowserViewModel.selectedMBeanOperationProperty().set(newValue));
        jvmBrowserViewModel.selectedMBeanOperationProperty().addListener((observable, oldValue, newValue) ->
                jvmsMBeanOperationsTable.getSelectionModel().select(newValue));
        jvmsMBeanOperationArgumentsField.textProperty().bindBidirectional(
                jvmBrowserViewModel.mbeanOperationArgumentsProperty());
        jvmsMBeanResultLabel.textProperty().bind(jvmBrowserViewModel.mbeanOperationResultProperty());
        jvmsMBeanErrorLabel.textProperty().bind(jvmBrowserViewModel.mbeanErrorMessageProperty());
        jvmsMBeanErrorLabel.visibleProperty().bind(jvmBrowserViewModel.mbeanErrorProperty());
        jvmsMBeanErrorLabel.managedProperty().bind(jvmsMBeanErrorLabel.visibleProperty());

        jvmsMBeanTree.disableProperty().bind(jvmBrowserViewModel.mbeanBrowserAvailableProperty().not());
        jvmsMBeanAttributesTable.disableProperty().bind(jvmBrowserViewModel.mbeanBrowserAvailableProperty().not());
        jvmsMBeanOperationsTable.disableProperty().bind(jvmBrowserViewModel.mbeanBrowserAvailableProperty().not());
        jvmsMBeanOperationArgumentsField.disableProperty().bind(jvmBrowserViewModel.mbeanBrowserAvailableProperty().not()
                .or(jvmBrowserViewModel.mbeanLoadingProperty()));
        jvmsRefreshMBeanButton.disableProperty().bind(jvmBrowserViewModel.mbeanBrowserAvailableProperty().not()
                .or(jvmBrowserViewModel.selectedMBeanProperty().isNull())
                .or(jvmBrowserViewModel.mbeanLoadingProperty()));
        jvmsInvokeMBeanOperationButton.disableProperty().bind(jvmBrowserViewModel.mbeanBrowserAvailableProperty().not()
                .or(jvmBrowserViewModel.selectedMBeanOperationProperty().isNull())
                .or(jvmBrowserViewModel.mbeanLoadingProperty()));
    }

    private void bindLiveJvmOverview() {
        jvmsOverviewPersistenceTable.setItems(FXCollections.observableArrayList());
        bindOverviewGroupTable(jvmsOverviewDashboardTable, "Dashboard");
        bindOverviewGroupTable(jvmsOverviewProcessorTable, "Processor");
        bindOverviewGroupTable(jvmsOverviewMemoryTable, "Memory");
        jvmBrowserViewModel.overviewMetricsProperty()
                .addListener((ListChangeListener<LiveJvmOverviewMetric>) change -> updateLiveJvmOverviewCharts());
        jvmBrowserViewModel.overviewPersistenceProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (overviewFullRefreshPending) {
                        rebuildLiveJvmOverviewGroups();
                        overviewFullRefreshPending = false;
                    }
                });
        i18n.localeProperty().addListener((observable, oldValue, newValue) -> {
            overviewFullRefreshPending = true;
            rebuildLiveJvmOverviewGroups();
            overviewFullRefreshPending = false;
        });
        rebuildLiveJvmOverviewGroups();
        jvmsOverviewPersistenceLabel.textProperty().bind(Bindings.createStringBinding(
                () -> formatOverviewPersistence(jvmBrowserViewModel.overviewPersistenceProperty().get()),
                jvmBrowserViewModel.overviewPersistenceProperty(),
                i18n.localeProperty()));
        jvmsOverviewErrorLabel.textProperty().bind(jvmBrowserViewModel.overviewErrorMessageProperty());
        jvmsOverviewErrorLabel.visibleProperty().bind(jvmBrowserViewModel.overviewErrorProperty());
        jvmsOverviewErrorLabel.managedProperty().bind(jvmsOverviewErrorLabel.visibleProperty());
        jvmsOverviewPersistenceTable.disableProperty().bind(jvmBrowserViewModel.selectedSessionProperty().isNull());
        jvmsOverviewDashboardTable.disableProperty().bind(jvmBrowserViewModel.selectedSessionProperty().isNull());
        jvmsOverviewProcessorTable.disableProperty().bind(jvmBrowserViewModel.selectedSessionProperty().isNull());
        jvmsOverviewMemoryTable.disableProperty().bind(jvmBrowserViewModel.selectedSessionProperty().isNull());
        jvmBrowserViewModel.selectedSessionProperty().addListener((observable, oldValue, newValue) -> {
            overviewFullRefreshPending = true;
            updateLiveJvmOverviewRefreshTimer();
        });
        updateLiveJvmOverviewRefreshTimer();
    }

    private void bindOverviewGroupTable(TableView<LiveJvmOverviewMetric> table, String group) {
        table.setItems(FXCollections.observableArrayList(latestOverviewRows(group)));
    }

    private String formatOverviewPersistence(LiveJvmPersistenceOverview persistence) {
        if (persistence == null) {
            return i18n.get("jvms.overview.persistence.empty");
        }
        if (!persistence.configured()) {
            return i18n.get("jvms.overview.persistence.unconfigured");
        }
        if (persistence.empty()) {
            return i18n.get("jvms.overview.persistence.none");
        }
        String samples = persistence.maxSamples() > 0
                ? i18n.format("jvms.overview.persistence.samples", persistence.maxSamples())
                : i18n.get("jvms.overview.persistence.samples.default");
        String notifications = persistence.maxEvents() > 0
                ? i18n.format("jvms.overview.persistence.notifications", persistence.maxEvents())
                : i18n.get("jvms.overview.persistence.notifications.default");
        return i18n.format("jvms.overview.persistence.summary",
                persistence.persistedAttributeSubscriptions(),
                persistence.attributeSubscriptions(),
                persistence.persistedNotificationSubscriptions(),
                persistence.notificationSubscriptions(),
                samples,
                notifications);
    }

    private Instant lastOverviewObservation() {
        return jvmBrowserViewModel.overviewMetricsProperty().stream()
                .map(LiveJvmOverviewMetric::observedAt)
                .max(Instant::compareTo)
                .orElse(Instant.EPOCH);
    }

    private void updateLiveJvmOverviewRefreshTimer() {
        if (jvmBrowserViewModel == null) {
            stopLiveJvmOverviewRefreshTimer();
            return;
        }
        if (jvmBrowserViewModel.selectedSessionProperty().get() == null) {
            stopLiveJvmOverviewRefreshTimer();
            return;
        }
        if (liveJvmOverviewRefreshTimeline == null) {
            liveJvmOverviewRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2),
                    event -> refreshLiveJvmOverviewCharts()));
            liveJvmOverviewRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        }
        liveJvmOverviewRefreshTimeline.play();
    }

    private void stopLiveJvmOverviewRefreshTimer() {
        if (liveJvmOverviewRefreshTimeline != null) {
            liveJvmOverviewRefreshTimeline.stop();
        }
    }

    private void bindDiagnosticCommands() {
        jvmsDiagnosticCommandsTable.setItems(jvmBrowserViewModel.diagnosticCommandsProperty());
        jvmsDiagnosticCommandsTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) ->
                        jvmBrowserViewModel.selectedDiagnosticCommandProperty().set(newValue));
        jvmBrowserViewModel.selectedDiagnosticCommandProperty().addListener((observable, oldValue, newValue) ->
                jvmsDiagnosticCommandsTable.getSelectionModel().select(newValue));
        jvmsDiagnosticArgumentsField.textProperty().bindBidirectional(
                jvmBrowserViewModel.diagnosticCommandArgumentsProperty());
        jvmsDiagnosticOutputArea.textProperty().bind(jvmBrowserViewModel.diagnosticCommandOutputProperty());
        jvmsDiagnosticErrorLabel.textProperty().bind(jvmBrowserViewModel.diagnosticCommandErrorMessageProperty());
        jvmsDiagnosticErrorLabel.visibleProperty().bind(jvmBrowserViewModel.diagnosticCommandErrorProperty());
        jvmsDiagnosticErrorLabel.managedProperty().bind(jvmsDiagnosticErrorLabel.visibleProperty());

        jvmsDiagnosticCommandsTable.disableProperty().bind(jvmBrowserViewModel.diagnosticCommandsAvailableProperty().not());
        jvmsDiagnosticArgumentsField.disableProperty().bind(jvmBrowserViewModel.diagnosticCommandsAvailableProperty().not()
                .or(jvmBrowserViewModel.diagnosticCommandLoadingProperty())
                .or(jvmBrowserViewModel.selectedDiagnosticCommandProperty().isNull()));
        jvmsExecuteDiagnosticCommandButton.disableProperty().bind(jvmBrowserViewModel.diagnosticCommandsAvailableProperty().not()
                .or(jvmBrowserViewModel.diagnosticCommandLoadingProperty())
                .or(jvmBrowserViewModel.selectedDiagnosticCommandProperty().isNull()));
        jvmsSaveDiagnosticOutputButton.disableProperty().bind(jvmBrowserViewModel.diagnosticCommandOutputProperty().isEmpty()
                .or(jvmBrowserViewModel.diagnosticCommandLoadingProperty()));
        jvmsDiagnosticOutputArea.disableProperty().bind(jvmBrowserViewModel.diagnosticCommandsAvailableProperty().not());
    }

    private void bindTriggers() {
        jvmsTriggerRulesTable.setItems(jvmBrowserViewModel.triggerRulesProperty());
        jvmsTriggerEventsTable.setItems(jvmBrowserViewModel.triggerEventsProperty());
        jvmsTriggerMetricCombo.setItems(jvmBrowserViewModel.liveMetricDefinitionsProperty());
        jvmsTriggerCommandCombo.setItems(jvmBrowserViewModel.diagnosticCommandsProperty());
        jvmsTriggerMetricCombo.valueProperty().bindBidirectional(jvmBrowserViewModel.selectedTriggerMetricProperty());
        jvmsTriggerOperatorCombo.valueProperty().bindBidirectional(jvmBrowserViewModel.selectedTriggerOperatorProperty());
        jvmsTriggerActionCombo.valueProperty().bindBidirectional(jvmBrowserViewModel.selectedTriggerActionTypeProperty());
        jvmsTriggerCommandCombo.valueProperty().bindBidirectional(jvmBrowserViewModel.selectedTriggerCommandProperty());
        jvmsTriggerNameField.textProperty().bindBidirectional(jvmBrowserViewModel.triggerNameProperty());
        jvmsTriggerThresholdField.textProperty().bindBidirectional(jvmBrowserViewModel.triggerThresholdProperty());
        jvmsTriggerErrorLabel.textProperty().bind(jvmBrowserViewModel.triggerErrorMessageProperty());
        jvmsTriggerErrorLabel.visibleProperty().bind(jvmBrowserViewModel.triggerErrorProperty());
        jvmsTriggerErrorLabel.managedProperty().bind(jvmsTriggerErrorLabel.visibleProperty());

        var triggerMetricsUnavailable = Bindings.isEmpty(jvmBrowserViewModel.liveMetricDefinitionsProperty());
        var diagnosticCommandTriggerSelected = jvmBrowserViewModel.selectedTriggerActionTypeProperty()
                .isEqualTo(TriggerActionType.DIAGNOSTIC_COMMAND);
        var diagnosticCommandTriggerUnavailable = diagnosticCommandTriggerSelected
                .and(jvmBrowserViewModel.diagnosticCommandsAvailableProperty().not()
                        .or(jvmBrowserViewModel.selectedTriggerCommandProperty().isNull()));
        jvmsTriggerNameField.disableProperty().bind(jvmBrowserViewModel.triggerLoadingProperty()
                .or(triggerMetricsUnavailable));
        jvmsTriggerMetricCombo.disableProperty().bind(jvmBrowserViewModel.triggerLoadingProperty()
                .or(triggerMetricsUnavailable));
        jvmsTriggerOperatorCombo.disableProperty().bind(jvmBrowserViewModel.triggerLoadingProperty()
                .or(triggerMetricsUnavailable));
        jvmsTriggerThresholdField.disableProperty().bind(jvmBrowserViewModel.triggerLoadingProperty()
                .or(triggerMetricsUnavailable));
        jvmsTriggerActionCombo.disableProperty().bind(jvmBrowserViewModel.triggerLoadingProperty()
                .or(triggerMetricsUnavailable));
        jvmsTriggerCommandCombo.disableProperty().bind(jvmBrowserViewModel.triggerLoadingProperty()
                .or(diagnosticCommandTriggerSelected.not())
                .or(jvmBrowserViewModel.diagnosticCommandsAvailableProperty().not()));
        jvmsAddTriggerButton.disableProperty().bind(jvmBrowserViewModel.triggerLoadingProperty()
                .or(triggerMetricsUnavailable)
                .or(jvmBrowserViewModel.selectedTriggerMetricProperty().isNull())
                .or(diagnosticCommandTriggerUnavailable));
        jvmsRemoveTriggerButton.disableProperty().bind(jvmBrowserViewModel.triggerLoadingProperty()
                .or(jvmsTriggerRulesTable.getSelectionModel().selectedItemProperty().isNull()));
        jvmsEvaluateTriggersButton.disableProperty().bind(jvmBrowserViewModel.triggerLoadingProperty()
                .or(Bindings.isEmpty(jvmBrowserViewModel.triggerRulesProperty())));
    }

    private void bindJmxMonitoring() {
        jvmsMonitoringSubscriptionsTable.setItems(jvmBrowserViewModel.jmxAttributeSubscriptionsProperty());
        jvmsMonitoringSamplesTable.setItems(jvmBrowserViewModel.jmxSubscriptionSamplesProperty());
        jvmsMonitoringNotificationsTable.setItems(jvmBrowserViewModel.jmxNotificationEventsProperty());
        jvmsMonitoringSubscriptionsTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) ->
                        jvmBrowserViewModel.selectedJmxAttributeSubscriptionProperty().set(newValue));
        jvmBrowserViewModel.selectedJmxAttributeSubscriptionProperty().addListener((observable, oldValue, newValue) ->
                jvmsMonitoringSubscriptionsTable.getSelectionModel().select(newValue));
        jvmBrowserViewModel.jmxSubscriptionSamplesProperty()
                .addListener((ListChangeListener<JmxSubscriptionSample>) change -> rebuildJmxMonitoringChart());
        jvmBrowserViewModel.selectedJmxAttributeSubscriptionProperty()
                .addListener((observable, oldValue, newValue) -> rebuildJmxMonitoringChart());
        rebuildJmxMonitoringChart();

        jvmsMonitoringErrorLabel.textProperty().bind(jvmBrowserViewModel.jmxMonitoringErrorMessageProperty());
        jvmsMonitoringErrorLabel.visibleProperty().bind(jvmBrowserViewModel.jmxMonitoringErrorProperty());
        jvmsMonitoringErrorLabel.managedProperty().bind(jvmsMonitoringErrorLabel.visibleProperty());

        jvmsMonitoringSubscriptionsTable.disableProperty().bind(jvmBrowserViewModel.jmxMonitoringAvailableProperty().not());
        jvmsMonitoringSamplesTable.disableProperty().bind(jvmBrowserViewModel.jmxMonitoringAvailableProperty().not());
        jvmsMonitoringNotificationsTable.disableProperty().bind(jvmBrowserViewModel.jmxMonitoringAvailableProperty().not());
        jvmsAddMonitoringSubscriptionButton.disableProperty().bind(jvmBrowserViewModel.jmxMonitoringAvailableProperty().not()
                .or(jvmBrowserViewModel.selectedMBeanProperty().isNull()));
        jvmsSampleSubscriptionButton.disableProperty().bind(jvmBrowserViewModel.jmxMonitoringLoadingProperty()
                .or(jvmBrowserViewModel.selectedJmxAttributeSubscriptionProperty().isNull()));
        jvmsAddNotificationSubscriptionButton.disableProperty().bind(jvmBrowserViewModel.jmxMonitoringAvailableProperty().not()
                .or(jvmBrowserViewModel.selectedMBeanProperty().isNull())
                .or(jvmBrowserViewModel.jmxMonitoringLoadingProperty()));
        jvmsStartNotificationsButton.disableProperty().bind(jvmBrowserViewModel.jmxMonitoringAvailableProperty().not()
                .or(jvmBrowserViewModel.selectedJmxNotificationSubscriptionProperty().isNull())
                .or(jvmBrowserViewModel.jmxMonitoringLoadingProperty()));
        jvmsStopNotificationsButton.disableProperty().bind(jvmBrowserViewModel.jmxMonitoringAvailableProperty().not()
                .or(jvmBrowserViewModel.selectedJmxNotificationSubscriptionProperty().isNull())
                .or(jvmBrowserViewModel.jmxMonitoringLoadingProperty()));
    }

    private void bindJmcAgentManager() {
        jvmsAgentPresetCombo.setItems(jvmBrowserViewModel.jmcAgentPresetsProperty());
        jvmsAgentTransformsTable.setItems(jvmBrowserViewModel.jmcAgentTransformsProperty());
        jvmsAgentPresetCombo.valueProperty().bindBidirectional(
                jvmBrowserViewModel.selectedJmcAgentPresetProperty());
        jvmsAgentConfigurationArea.textProperty().bindBidirectional(
                jvmBrowserViewModel.jmcAgentConfigurationProperty());
        jvmsAgentPresetDescriptionLabel.textProperty().bind(
                jvmBrowserViewModel.selectedJmcAgentPresetDescriptionProperty());
        jvmsAgentPresetDescriptionLabel.visibleProperty().bind(
                jvmBrowserViewModel.selectedJmcAgentPresetDescriptionProperty().isNotEmpty());
        jvmsAgentPresetDescriptionLabel.managedProperty().bind(jvmsAgentPresetDescriptionLabel.visibleProperty());
        jvmsAgentStatusLabel.textProperty().bind(Bindings.createStringBinding(
                () -> jvmBrowserViewModel.jmcAgentErrorProperty().get()
                        ? jvmBrowserViewModel.jmcAgentErrorMessageProperty().get()
                        : jvmBrowserViewModel.jmcAgentStatusMessageProperty().get(),
                jvmBrowserViewModel.jmcAgentErrorProperty(),
                jvmBrowserViewModel.jmcAgentErrorMessageProperty(),
                jvmBrowserViewModel.jmcAgentStatusMessageProperty()));
        jvmsAgentStatusLabel.visibleProperty().bind(jvmBrowserViewModel.jmcAgentStatusMessageProperty().isNotEmpty()
                .or(jvmBrowserViewModel.jmcAgentErrorProperty()));
        jvmsAgentStatusLabel.managedProperty().bind(jvmsAgentStatusLabel.visibleProperty());
        jvmsAgentApplyStatusLabel.textProperty().bind(jvmBrowserViewModel.jmcAgentApplyStatusMessageProperty());
        jvmsAgentApplyStatusLabel.visibleProperty().bind(
                jvmBrowserViewModel.jmcAgentApplyStatusMessageProperty().isNotEmpty());
        jvmsAgentApplyStatusLabel.managedProperty().bind(jvmsAgentApplyStatusLabel.visibleProperty());

        jvmsAgentPresetCombo.disableProperty().bind(jvmBrowserViewModel.jmcAgentLoadingProperty()
                .or(Bindings.isEmpty(jvmBrowserViewModel.jmcAgentPresetsProperty())));
        jvmsRefreshAgentButton.disableProperty().bind(jvmBrowserViewModel.selectedSessionProperty().isNull()
                .or(jvmBrowserViewModel.jmcAgentLoadingProperty()));
        jvmsLoadAgentPresetButton.disableProperty().bind(jvmBrowserViewModel.jmcAgentLoadingProperty()
                .or(jvmBrowserViewModel.selectedJmcAgentPresetProperty().isNull()));
        jvmsApplyAgentConfigurationButton.disableProperty().bind(jvmBrowserViewModel.jmcAgentLoadingProperty()
                .or(jvmBrowserViewModel.jmcAgentAvailableProperty().not()));
        jvmsAgentTransformsTable.disableProperty().bind(jvmBrowserViewModel.jmcAgentAvailableProperty().not());
        jvmsAgentConfigurationArea.disableProperty().bind(jvmBrowserViewModel.jmcAgentAvailableProperty().not()
                .or(jvmBrowserViewModel.jmcAgentLoadingProperty()));
    }

    private void rebuildMBeanTree() {
        TreeItem<MBeanNode> rootItem = new TreeItem<>(MBeanNode.domain(i18n.get("jvms.mbeans.root"),
                jvmBrowserViewModel.mbeanTreeProperty()));
        jvmBrowserViewModel.mbeanTreeProperty().stream()
                .map(this::toMBeanTreeItem)
                .forEach(rootItem.getChildren()::add);
        rootItem.setExpanded(true);
        jvmsMBeanTree.setRoot(rootItem);
        selectMBeanTreeNode(jvmBrowserViewModel.selectedMBeanProperty().get());
    }

    private TreeItem<MBeanNode> toMBeanTreeItem(MBeanNode node) {
        TreeItem<MBeanNode> item = new TreeItem<>(node);
        node.children().stream()
                .map(this::toMBeanTreeItem)
                .forEach(item.getChildren()::add);
        item.setExpanded(node.domain());
        return item;
    }

    private void selectMBeanTreeNode(MBeanNode node) {
        TreeItem<MBeanNode> item = findMBeanTreeItem(jvmsMBeanTree.getRoot(), node);
        if (item == null) {
            jvmsMBeanTree.getSelectionModel().clearSelection();
        } else {
            jvmsMBeanTree.getSelectionModel().select(item);
        }
    }

    private TreeItem<MBeanNode> findMBeanTreeItem(TreeItem<MBeanNode> item, MBeanNode node) {
        if (item == null || node == null) {
            return null;
        }
        if (node.equals(item.getValue())) {
            return item;
        }
        for (TreeItem<MBeanNode> child : item.getChildren()) {
            TreeItem<MBeanNode> found = findMBeanTreeItem(child, node);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void configureJvmRecordingsTable() {
        jvmsRecordingsTable.setPlaceholder(localizedTablePlaceholder("jvms.recordings.empty"));

        TableColumn<FlightRecordingInfo, Number> idCol = new TableColumn<>();
        idCol.textProperty().bind(i18n.text("jvms.recordings.column.id"));
        idCol.setPrefWidth(70);
        idCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleLongProperty(cell.getValue().id()));

        TableColumn<FlightRecordingInfo, String> nameCol = localizedColumn("jvms.recordings.column.name");
        nameCol.setPrefWidth(260);
        nameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));

        TableColumn<FlightRecordingInfo, String> stateCol = localizedColumn("jvms.recordings.column.state");
        stateCol.setPrefWidth(110);
        stateCol.setCellValueFactory(cell -> Bindings.createStringBinding(
                () -> formatFlightRecordingState(cell.getValue().state()), i18n.localeProperty()));

        TableColumn<FlightRecordingInfo, String> durationCol = localizedColumn("jvms.recordings.column.duration");
        durationCol.setPrefWidth(120);
        durationCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatDuration(cell.getValue().durationMillis())));

        TableColumn<FlightRecordingInfo, String> sizeCol = localizedColumn("jvms.recordings.column.size");
        sizeCol.setPrefWidth(110);
        sizeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                DisplayFormats.formatFileSize(cell.getValue().sizeBytes())));

        jvmsRecordingsTable.getColumns().setAll(List.of(idCol, nameCol, stateCol, durationCol, sizeCol));
    }

    private void saveSelectedFlightRecording() {
        if (jvmBrowserViewModel == null || jvmsPane == null || jvmsPane.getScene() == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(i18n.get("fileChooser.saveRecording.title"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(jfrRecordingsFilterDescription(i18n), "*.jfr"));
        FlightRecordingInfo selectedRecording = jvmBrowserViewModel.selectedFlightRecording();
        chooser.setInitialFileName(saveRecordingInitialFileName(
                selectedRecording == null ? "" : selectedRecording.name()));
        java.io.File file = chooser.showSaveDialog(jvmsPane.getScene().getWindow());
        if (file != null) {
            jvmBrowserViewModel.stopAndSaveSelectedFlightRecording(file.toPath());
        }
    }

    private void saveDiagnosticCommandOutput() {
        if (jvmBrowserViewModel == null || jvmsPane == null || jvmsPane.getScene() == null
                || jvmBrowserViewModel.diagnosticCommandOutputProperty().get().isBlank()) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(i18n.get("fileChooser.saveDiagnosticOutput.title"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                i18n.get("fileChooser.textFiles"), "*.txt"));
        chooser.setInitialFileName("diagnostic-command-output.txt");
        java.io.File file = chooser.showSaveDialog(jvmsPane.getScene().getWindow());
        if (file == null) {
            return;
        }
        String output = jvmBrowserViewModel.diagnosticCommandOutputProperty().get();
        Thread.ofVirtual().name("jmc-fx-save-diagnostic-output").start(() -> {
            try {
                Files.writeString(file.toPath(), output, StandardCharsets.UTF_8);
            } catch (java.io.IOException exception) {
                LOGGER.error("Unable to save Diagnostic Command output", exception);
                Platform.runLater(() -> {
                    jvmBrowserViewModel.diagnosticCommandErrorProperty().set(true);
                    jvmBrowserViewModel.diagnosticCommandErrorMessageProperty().set(
                            exception.getMessage() == null ? exception.getClass().getSimpleName()
                                    : exception.getMessage());
                });
            }
        });
    }

    private void removeSelectedTriggerRule() {
        if (jvmBrowserViewModel == null) {
            return;
        }
        jvmBrowserViewModel.removeSelectedTriggerRule(
                jvmsTriggerRulesTable.getSelectionModel().getSelectedItem());
    }

    private void addSelectedMonitoringSubscription() {
        if (jvmBrowserViewModel == null) {
            return;
        }
        jvmBrowserViewModel.addMBeanAttributeSubscription(
                jvmsMBeanAttributesTable.getSelectionModel().getSelectedItem(),
                java.time.Duration.ofSeconds(1),
                120,
                true);
    }

    private void addSelectedNotificationSubscription() {
        if (jvmBrowserViewModel == null) {
            return;
        }
        jvmBrowserViewModel.addMBeanNotificationSubscription(
                jvmBrowserViewModel.selectedMBeanProperty().get(),
                100,
                true);
    }

    private void rebuildLiveJvmOverviewGroups() {
        if (jvmBrowserViewModel == null) {
            return;
        }
        replaceOverviewTableRows(jvmsOverviewPersistenceTable, overviewPersistenceRows());
        replaceOverviewTableRows(jvmsOverviewDashboardTable, latestOverviewRows("Dashboard"));
        replaceOverviewTableRows(jvmsOverviewProcessorTable, latestOverviewRows("Processor"));
        replaceOverviewTableRows(jvmsOverviewMemoryTable, latestOverviewRows("Memory"));
        rebuildOverviewMetricToggles("Dashboard", jvmsOverviewDashboardMetricToggles);
        rebuildOverviewMetricToggles("Processor", jvmsOverviewProcessorMetricToggles);
        rebuildOverviewMetricToggles("Memory", jvmsOverviewMemoryMetricToggles);
        updateLiveJvmOverviewChart(jvmsOverviewDashboardChart, "Dashboard");
        updateLiveJvmOverviewChart(jvmsOverviewProcessorChart, "Processor");
        updateLiveJvmOverviewChart(jvmsOverviewMemoryChart, "Memory");
    }

    private void updateLiveJvmOverviewCharts() {
        if (jvmBrowserViewModel == null) {
            return;
        }
        if (overviewMetricTogglesUninitialized()) {
            rebuildLiveJvmOverviewGroups();
            overviewFullRefreshPending = false;
            return;
        }
        updateLiveJvmOverviewChart(jvmsOverviewDashboardChart, "Dashboard");
        updateLiveJvmOverviewChart(jvmsOverviewProcessorChart, "Processor");
        updateLiveJvmOverviewChart(jvmsOverviewMemoryChart, "Memory");
    }

    private boolean overviewMetricTogglesUninitialized() {
        return !latestOverviewRows("Dashboard").isEmpty() && jvmsOverviewDashboardMetricToggles.getChildren().isEmpty()
                || !latestOverviewRows("Processor").isEmpty()
                        && jvmsOverviewProcessorMetricToggles.getChildren().isEmpty()
                || !latestOverviewRows("Memory").isEmpty() && jvmsOverviewMemoryMetricToggles.getChildren().isEmpty();
    }

    private void refreshLiveJvmOverviewCharts() {
        if (jvmBrowserViewModel == null || jvmBrowserViewModel.overviewLoadingProperty().get()) {
            return;
        }
        jvmBrowserViewModel.refreshOverview();
    }

    private void replaceOverviewTableRows(TableView<LiveJvmOverviewMetric> table, List<LiveJvmOverviewMetric> rows) {
        if (table.getItems() == null) {
            table.setItems(FXCollections.observableArrayList(rows));
        } else {
            table.getItems().setAll(rows);
        }
    }

    private void rebuildOverviewMetricToggles(String group, FlowPane container) {
        Map<LiveMetricKind, CheckBox> toggles = overviewMetricToggles.computeIfAbsent(group, key -> new HashMap<>());
        List<LiveJvmOverviewMetric> rows = latestOverviewRows(group);
        for (LiveJvmOverviewMetric metric : rows) {
            toggles.computeIfAbsent(metric.kind(), kind -> {
                CheckBox checkBox = new CheckBox();
                checkBox.setSelected(defaultOverviewMetricVisible(group, kind));
                checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                    if (updatingOverviewMetricSelection) {
                        return;
                    }
                    if (newValue) {
                        selectOverviewMetricAxis(group, kind);
                    }
                    updateLiveJvmOverviewChart(chartForOverviewGroup(group), group);
                    refreshOverviewMetricToggleGraphics(group);
                });
                return checkBox;
            });
            CheckBox checkBox = toggles.get(metric.kind());
            updateOverviewMetricToggle(checkBox, group, metric);
        }
        List<CheckBox> visibleToggles = rows.stream()
                .map(LiveJvmOverviewMetric::kind)
                .distinct()
                .map(toggles::get)
                .toList();
        if (!container.getChildren().equals(visibleToggles)) {
            container.getChildren().setAll(visibleToggles);
        }
    }

    private void refreshOverviewMetricToggleGraphics(String group) {
        Map<LiveMetricKind, CheckBox> toggles = overviewMetricToggles.getOrDefault(group, Map.of());
        latestOverviewRows(group).forEach(metric -> {
            CheckBox checkBox = toggles.get(metric.kind());
            if (checkBox != null) {
                updateOverviewMetricToggle(checkBox, group, metric);
            }
        });
    }

    private void updateOverviewMetricToggle(CheckBox checkBox, String group, LiveJvmOverviewMetric metric) {
        checkBox.setText(null);
        checkBox.setGraphic(overviewMetricToggleGraphic(group, metric));
        String tooltip = i18n.format("jvms.overview.metric.toggleTooltip",
                metric.label(), metric.displayValue(), metric.unit());
        if (checkBox.getTooltip() == null) {
            checkBox.setTooltip(new Tooltip(tooltip));
        } else {
            checkBox.getTooltip().setText(tooltip);
        }
    }

    private void selectOverviewMetricAxis(String group, LiveMetricKind selectedKind) {
        OverviewChartAxis selectedAxis = latestOverviewMetric(group, selectedKind)
                .map(metric -> overviewMetricAxis(metric.unit()))
                .orElse(OverviewChartAxis.COUNT);
        Map<LiveMetricKind, CheckBox> toggles = overviewMetricToggles.getOrDefault(group, Map.of());
        updatingOverviewMetricSelection = true;
        try {
            toggles.forEach((kind, checkBox) -> {
                if (kind == selectedKind || !checkBox.isSelected()) {
                    return;
                }
                OverviewChartAxis axis = latestOverviewMetric(group, kind)
                        .map(metric -> overviewMetricAxis(metric.unit()))
                        .orElse(OverviewChartAxis.COUNT);
                if (axis != selectedAxis) {
                    checkBox.setSelected(false);
                }
            });
        } finally {
            updatingOverviewMetricSelection = false;
        }
    }

    private Node overviewMetricToggleGraphic(String group, LiveJvmOverviewMetric metric) {
        Region swatch = new Region();
        swatch.getStyleClass().setAll("jvms-overview-metric-swatch",
                overviewMetricColorStyle(group, metric.kind()));
        Label label = new Label(overviewMetricDisplayName(metric));
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);
        HBox graphic = new HBox(6, swatch, label);
        graphic.getStyleClass().add("jvms-overview-metric-toggle-content");
        graphic.setPadding(new Insets(0, 0, 0, 8));
        graphic.setMaxWidth(Double.MAX_VALUE);
        return graphic;
    }

    private boolean overviewMetricVisible(String group, LiveMetricKind kind) {
        CheckBox checkBox = overviewMetricToggles.getOrDefault(group, Map.of()).get(kind);
        return checkBox == null ? defaultOverviewMetricVisible(group, kind) : checkBox.isSelected();
    }

    private boolean defaultOverviewMetricVisible(String group, LiveMetricKind kind) {
        return DEFAULT_OVERVIEW_CHART_METRICS.getOrDefault(group, Set.of()).contains(kind);
    }

    private LineChart<Number, Number> chartForOverviewGroup(String group) {
        return switch (group) {
            case "Dashboard" -> jvmsOverviewDashboardChart;
            case "Processor" -> jvmsOverviewProcessorChart;
            case "Memory" -> jvmsOverviewMemoryChart;
            default -> throw new IllegalArgumentException(group);
        };
    }

    private void updateLiveJvmOverviewChart(LineChart<Number, Number> chart, String group) {
        if (jvmBrowserViewModel == null) {
            chart.getData().clear();
            return;
        }
        List<LiveMetricKind> kinds = jvmBrowserViewModel.overviewMetricsProperty().stream()
                .filter(metric -> group.equals(metric.group()))
                .map(LiveJvmOverviewMetric::kind)
                .distinct()
                .filter(kind -> overviewMetricVisible(group, kind))
                .toList();
        updateOverviewChartAxis(chart, group, kinds);
        Map<LiveMetricKind, XYChart.Series<Number, Number>> seriesByKind =
                overviewChartSeries.computeIfAbsent(group, key -> new HashMap<>());
        seriesByKind.entrySet().removeIf(entry -> {
            if (kinds.contains(entry.getKey())) {
                return false;
            }
            chart.getData().remove(entry.getValue());
            return true;
        });
        for (LiveMetricKind kind : kinds) {
            XYChart.Series<Number, Number> series = seriesByKind.computeIfAbsent(kind, key -> new XYChart.Series<>());
            latestOverviewMetric(group, kind).ifPresent(metric -> series.setName(overviewMetricDisplayName(metric)));
            series.getData().setAll(jvmBrowserViewModel.overviewMetricsProperty().stream()
                    .filter(metric -> group.equals(metric.group()))
                    .filter(metric -> metric.kind() == kind)
                    .map(metric -> new XYChart.Data<Number, Number>(
                            metric.sequence(), overviewChartValue(metric)))
                    .toList());
        }
        List<XYChart.Series<Number, Number>> orderedSeries = new ArrayList<>();
        for (LiveMetricKind kind : kinds) {
            XYChart.Series<Number, Number> series = seriesByKind.get(kind);
            if (series != null && !series.getData().isEmpty()) {
                orderedSeries.add(series);
            }
        }
        chart.getData().setAll(orderedSeries);
    }

    private void updateOverviewChartAxis(LineChart<Number, Number> chart, String group, List<LiveMetricKind> kinds) {
        NumberAxis xAxis = (NumberAxis) chart.getXAxis();
        NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        xAxis.setLabel(i18n.get("jvms.overview.axis.sample"));
        OverviewChartAxis axis = overviewChartAxis(group, kinds);
        yAxis.setLabel(switch (axis) {
            case PERCENT -> i18n.get("jvms.overview.axis.percent");
            case MEMORY -> i18n.get("jvms.overview.axis.memory");
            case COUNT -> i18n.get("jvms.overview.axis.count");
            case LOAD -> i18n.get("jvms.overview.axis.load");
        });
        yAxis.setTickLabelFormatter(new OverviewValueTickFormatter(axis));
        if (axis == OverviewChartAxis.PERCENT) {
            yAxis.setAutoRanging(false);
            yAxis.setLowerBound(0.0);
            yAxis.setUpperBound(100.0);
            yAxis.setTickUnit(20.0);
        } else {
            yAxis.setAutoRanging(true);
        }
    }

    private OverviewChartAxis overviewChartAxis(String group, List<LiveMetricKind> kinds) {
        return kinds.stream()
                .map(kind -> latestOverviewMetric(group, kind))
                .flatMap(java.util.Optional::stream)
                .map(metric -> overviewMetricAxis(metric.unit()))
                .findFirst()
                .orElse(OverviewChartAxis.COUNT);
    }

    private OverviewChartAxis overviewMetricAxis(String unit) {
        return switch (unit) {
            case "%" -> OverviewChartAxis.PERCENT;
            case "bytes" -> OverviewChartAxis.MEMORY;
            case "load" -> OverviewChartAxis.LOAD;
            default -> OverviewChartAxis.COUNT;
        };
    }

    private double overviewChartValue(LiveJvmOverviewMetric metric) {
        return metric.value();
    }

    private String overviewMetricDisplayName(LiveJvmOverviewMetric metric) {
        String unit = overviewMetricDisplayUnit(metric.unit());
        return unit.isBlank() ? metric.label() : metric.label() + " (" + unit + ")";
    }

    private String overviewMetricDisplayUnit(String unit) {
        return switch (unit) {
            case "%" -> "%";
            case "bytes" -> "";
            default -> unit;
        };
    }

    private String overviewMetricColorStyle(String group, LiveMetricKind kind) {
        List<LiveMetricKind> visibleKinds = latestOverviewRows(group).stream()
                .map(LiveJvmOverviewMetric::kind)
                .distinct()
                .filter(candidate -> overviewMetricVisible(group, candidate))
                .toList();
        int visibleIndex = visibleKinds.indexOf(kind);
        if (visibleIndex >= 0) {
            return "default-color" + Math.floorMod(visibleIndex, 8);
        }
        List<LiveMetricKind> allKinds = latestOverviewRows(group).stream()
                .map(LiveJvmOverviewMetric::kind)
                .distinct()
                .toList();
        int allIndex = allKinds.indexOf(kind);
        return "default-color" + Math.floorMod(Math.max(allIndex, 0), 8);
    }

    private List<LiveJvmOverviewMetric> latestOverviewRows(String group) {
        if (jvmBrowserViewModel == null) {
            return List.of();
        }
        return jvmBrowserViewModel.overviewMetricsProperty().stream()
                .filter(metric -> group.equals(metric.group()))
                .collect(java.util.stream.Collectors.groupingBy(
                        LiveJvmOverviewMetric::kind,
                        java.util.stream.Collectors.maxBy(
                                java.util.Comparator.comparingLong(LiveJvmOverviewMetric::sequence))))
                .values()
                .stream()
                .flatMap(java.util.Optional::stream)
                .sorted(java.util.Comparator.comparing(metric -> overviewMetricOrder(metric.kind())))
                .toList();
    }

    private java.util.Optional<LiveJvmOverviewMetric> latestOverviewMetric(String group, LiveMetricKind kind) {
        return jvmBrowserViewModel.overviewMetricsProperty().stream()
                .filter(metric -> group.equals(metric.group()))
                .filter(metric -> metric.kind() == kind)
                .max(java.util.Comparator.comparingLong(LiveJvmOverviewMetric::sequence));
    }

    private List<LiveJvmOverviewMetric> overviewPersistenceRows() {
        LiveJvmPersistenceOverview persistence = jvmBrowserViewModel.overviewPersistenceProperty().get();
        if (persistence == null) {
            persistence = LiveJvmPersistenceOverview.notConfigured();
        }
        Instant observedAt = lastOverviewObservation();
        return List.of(
                overviewPersistenceRow("jvms.overview.persistence.metric.configured",
                        persistence.configured()
                                ? i18n.get("jvms.overview.persistence.metric.yes")
                                : i18n.get("jvms.overview.persistence.metric.no"),
                        observedAt),
                overviewPersistenceRow("jvms.overview.persistence.metric.attributeSubscriptions",
                        DisplayFormats.formatInteger(persistence.attributeSubscriptions()),
                        observedAt),
                overviewPersistenceRow("jvms.overview.persistence.metric.persistedAttributes",
                        DisplayFormats.formatInteger(persistence.persistedAttributeSubscriptions()),
                        observedAt),
                overviewPersistenceRow("jvms.overview.persistence.metric.notificationSubscriptions",
                        DisplayFormats.formatInteger(persistence.notificationSubscriptions()),
                        observedAt),
                overviewPersistenceRow("jvms.overview.persistence.metric.persistedNotifications",
                        DisplayFormats.formatInteger(persistence.persistedNotificationSubscriptions()),
                        observedAt),
                overviewPersistenceRow("jvms.overview.persistence.metric.retainedSamples",
                        persistence.maxSamples() > 0
                                ? DisplayFormats.formatInteger(persistence.maxSamples())
                                : i18n.get("jvms.overview.persistence.samples.default"),
                        observedAt),
                overviewPersistenceRow("jvms.overview.persistence.metric.retainedNotifications",
                persistence.maxEvents() > 0
                        ? DisplayFormats.formatInteger(persistence.maxEvents())
                        : i18n.get("jvms.overview.persistence.notifications.default"),
                        observedAt));
    }

    private LiveJvmOverviewMetric overviewPersistenceRow(String labelKey, String value, Instant observedAt) {
        return new LiveJvmOverviewMetric("JMX Data Persistence Settings", LiveMetricKind.THREAD_COUNT,
                i18n.get(labelKey), 0.0, value, "", observedAt, 0);
    }

    private int overviewMetricOrder(LiveMetricKind kind) {
        return switch (kind) {
            case THREAD_COUNT -> 10;
            case PEAK_THREAD_COUNT -> 20;
            case DAEMON_THREAD_COUNT -> 30;
            case LOADED_CLASS_COUNT -> 40;
            case TOTAL_LOADED_CLASS_COUNT -> 50;
            case UNLOADED_CLASS_COUNT -> 60;
            case PROCESS_CPU_LOAD_PERCENT -> 110;
            case SYSTEM_CPU_LOAD_PERCENT -> 120;
            case AVAILABLE_PROCESSORS -> 130;
            case SYSTEM_LOAD_AVERAGE -> 140;
            case HEAP_USED_PERCENT -> 210;
            case HEAP_USED_BYTES -> 220;
            case HEAP_COMMITTED_BYTES -> 230;
            case HEAP_MAX_BYTES -> 240;
            case NON_HEAP_USED_BYTES -> 250;
            case NON_HEAP_COMMITTED_BYTES -> 260;
        };
    }

    private void rebuildJmxMonitoringChart() {
        jvmsMonitoringChart.getData().clear();
        JmxAttributeSubscription subscription = jvmBrowserViewModel == null
                ? null : jvmBrowserViewModel.selectedJmxAttributeSubscriptionProperty().get();
        if (subscription == null) {
            return;
        }
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(subscription.label());
        int index = 0;
        for (JmxSubscriptionSample sample : jvmBrowserViewModel.jmxSubscriptionSamplesProperty()) {
            if (sample.numeric()) {
                series.getData().add(new XYChart.Data<>(index, sample.numericValue()));
            }
            index++;
        }
        jvmsMonitoringChart.getData().add(series);
    }

    private String formatJvmState(JvmConnectionState state) {
        return i18n.get("jvms.state." + state.name().toLowerCase(java.util.Locale.ROOT));
    }

    private String formatJvmSource(JvmConnectionSource source) {
        return i18n.get("jvms.source." + source.name().toLowerCase(java.util.Locale.ROOT));
    }

    String selectedConnectionStatusText(JvmConnection selectedConnection, String jdpStatusMessage) {
        if (selectedConnection != null && !selectedConnection.statusMessage().isBlank()) {
            return localizedConnectionStatus(selectedConnection);
        }
        return localizedJdpStatus(jdpStatusMessage);
    }

    private String localizedConnectionStatus(JvmConnection selectedConnection) {
        if (selectedConnection.connected() || selectedConnection.state() == JvmConnectionState.CONNECTED) {
            return i18n.get("jvms.status.connected");
        }
        return switch (selectedConnection.source()) {
            case LOCAL -> selectedConnection.attachable()
                    ? i18n.get("jvms.status.attachableLocal")
                    : i18n.get("jvms.status.localUnavailable");
            case SAVED -> i18n.get("jvms.status.saved");
            case JDP -> i18n.get("jvms.status.jdp");
            case MANUAL -> selectedConnection.statusMessage();
        };
    }

    private String localizedJdpStatus(String jdpStatusMessage) {
        String message = jdpStatusMessage == null ? "" : jdpStatusMessage.trim();
        if (message.equals("Refreshing JDP targets.")) {
            return i18n.get("jvms.jdp.status.refreshing");
        }
        if (message.equals("JDP discovery is not configured.")) {
            return i18n.get("jvms.jdp.status.unconfigured");
        }
        if (message.startsWith("JDP discovery failed: ")) {
            return i18n.format("jvms.jdp.status.failed",
                    message.substring("JDP discovery failed: ".length()));
        }
        if (message.equals("No JDP targets found.")) {
            return i18n.get("jvms.jdp.status.none");
        }
        if (message.equals("Found 1 JDP target.")) {
            return i18n.format("jvms.jdp.status.found", 1);
        }
        if (message.startsWith("Found ") && message.endsWith(" JDP targets.")) {
            try {
                int count = Integer.parseInt(message.substring(6, message.indexOf(" JDP targets.")));
                return i18n.format("jvms.jdp.status.found", count);
            } catch (NumberFormatException exception) {
                return message;
            }
        }
        return message.isBlank() || message.equals("Idle.") ? i18n.get("jvms.jdp.status.idle") : message;
    }

    private String formatFlightRecordingState(io.github.youngledo.jmcfx.domain.model.FlightRecordingState state) {
        return i18n.get("jvms.recordings.state." + state.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static String formatMBeanAttributeValue(MBeanAttributeInfo attribute) {
        if (attribute == null) {
            return "";
        }
        return attribute.error().isBlank() ? attribute.value() : attribute.error();
    }

    private static String formatMBeanOperationSignature(MBeanOperationInfo operation) {
        if (operation == null) {
            return "";
        }
        return operation.parameters().stream()
                .map(parameter -> parameter.name() + ": " + parameter.type())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static String displayDiagnosticCommandName(DiagnosticCommandInfo command) {
        if (command == null) {
            return "";
        }
        return command.displayName().isBlank() ? command.name() : command.displayName();
    }

    private static String formatDiagnosticCommandParameters(DiagnosticCommandInfo command) {
        if (command == null || command.parameters().isEmpty()) {
            return "";
        }
        return command.parameters().stream()
                .map(parameter -> parameter.name() + ": " + parameter.type()
                        + (parameter.required() ? " *" : ""))
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private String formatTriggerCondition(TriggerRule rule) {
        if (rule == null) {
            return "";
        }
        String metricName = jvmBrowserViewModel == null ? rule.metric().name()
                : jvmBrowserViewModel.liveMetricDefinitionsProperty().stream()
                        .filter(definition -> definition.kind() == rule.metric())
                        .findFirst()
                        .map(LiveMetricDefinition::label)
                        .filter(label -> !label.isBlank())
                        .orElse(rule.metric().name());
        return metricName + " " + rule.operator().symbol() + " " + rule.threshold();
    }

    private String formatTriggerAction(TriggerRule rule) {
        if (rule == null || rule.action() == null) {
            return "";
        }
        if (rule.action().type() != TriggerActionType.DIAGNOSTIC_COMMAND) {
            return i18n.get("jvms.triggers.action.notify");
        }
        return i18n.format("jvms.triggers.action.diagnosticCommand", rule.action().commandName());
    }

    private String formatTriggerActionType(TriggerActionType actionType) {
        return i18n.get("jvms.triggers.actionType."
                + actionType.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static String formatTriggerEventValue(TriggerEvent event) {
        if (event == null) {
            return "";
        }
        return event.unit().isBlank() ? String.valueOf(event.value()) : event.value() + " " + event.unit();
    }

    private String formatJvmRuntime(JvmSessionSnapshot snapshot) {
        if (snapshot == null) {
            return i18n.get("jvms.session.empty");
        }
        return String.format(java.util.Locale.ROOT, "%s %s, %s, uptime %s",
                snapshot.runtime().vmName(),
                snapshot.runtime().vmVersion(),
                snapshot.runtime().vmVendor(),
                DisplayFormats.formatDuration(snapshot.runtime().uptimeMillis()));
    }

    private String formatJvmCapability(JvmCapabilitySnapshot snapshot) {
        return i18n.get("jvms.capability." + snapshot.capability().name().toLowerCase(java.util.Locale.ROOT))
                + ": "
                + i18n.get("jvms.capability.status." + snapshot.status().name().toLowerCase(java.util.Locale.ROOT));
    }

    private void bindLocalizedText() {
        jvmsTitleLabel.textProperty().bind(i18n.text("jvms.title"));
        jvmsRefreshButton.textProperty().bind(i18n.text("jvms.refresh"));
        jvmsManualUrlField.promptTextProperty().bind(i18n.text("jvms.manualUrlPrompt"));
        jvmsManualUrlHintLabel.textProperty().bind(i18n.text("jvms.manualUrlHint"));
        jvmsManualNameField.promptTextProperty().bind(i18n.text("jvms.manualNamePrompt"));
        jvmsSaveTargetButton.textProperty().bind(i18n.text("jvms.saveTarget"));
        jvmsRemoveSavedTargetButton.textProperty().bind(i18n.text("jvms.removeSavedTarget"));
        jvmsRefreshJdpButton.textProperty().bind(i18n.text("jvms.refreshJdp"));
        jvmsConnectButton.textProperty().bind(i18n.text("jvms.connect"));
        jvmsDisconnectButton.textProperty().bind(i18n.text("jvms.disconnect"));
        jvmsOverviewTab.textProperty().bind(i18n.text("jvms.overview.tab"));
        jvmsOverviewPersistenceTitleLabel.textProperty().bind(i18n.text("jvms.overview.persistence.title"));
        jvmsOverviewDashboardTitleLabel.textProperty().bind(i18n.text("jvms.overview.dashboard.title"));
        jvmsOverviewProcessorTitleLabel.textProperty().bind(i18n.text("jvms.overview.processor.title"));
        jvmsOverviewMemoryTitleLabel.textProperty().bind(i18n.text("jvms.overview.memory.title"));
        jvmsOverviewDashboardChartTab.textProperty().bind(i18n.text("jvms.overview.chart"));
        jvmsOverviewDashboardTableTab.textProperty().bind(i18n.text("jvms.overview.table"));
        jvmsOverviewProcessorChartTab.textProperty().bind(i18n.text("jvms.overview.chart"));
        jvmsOverviewProcessorTableTab.textProperty().bind(i18n.text("jvms.overview.table"));
        jvmsOverviewMemoryChartTab.textProperty().bind(i18n.text("jvms.overview.chart"));
        jvmsOverviewMemoryTableTab.textProperty().bind(i18n.text("jvms.overview.table"));
        jvmsSessionTab.textProperty().bind(i18n.text("jvms.session.tab"));
        jvmsMBeanTab.textProperty().bind(i18n.text("jvms.mbeans.tab"));
        jvmsDiagnosticsTab.textProperty().bind(i18n.text("jvms.diagnostics.tab"));
        jvmsTriggersTab.textProperty().bind(i18n.text("jvms.triggers.tab"));
        jvmsMonitoringTab.textProperty().bind(i18n.text("jvms.monitoring.tab"));
        jvmsAgentTab.textProperty().bind(i18n.text("jvms.agent.tab"));
        jvmsRefreshMBeanButton.textProperty().bind(i18n.text("jvms.mbeans.refresh"));
        jvmsInvokeMBeanOperationButton.textProperty().bind(i18n.text("jvms.mbeans.invoke"));
        jvmsMBeanOperationArgumentsField.promptTextProperty().bind(i18n.text("jvms.mbeans.arguments"));
        jvmsDiagnosticArgumentsField.promptTextProperty().bind(i18n.text("jvms.diagnostics.arguments"));
        jvmsExecuteDiagnosticCommandButton.textProperty().bind(i18n.text("jvms.diagnostics.execute"));
        jvmsSaveDiagnosticOutputButton.textProperty().bind(i18n.text("jvms.diagnostics.saveOutput"));
        jvmsTriggerNameField.promptTextProperty().bind(i18n.text("jvms.triggers.name"));
        jvmsTriggerMetricCombo.promptTextProperty().bind(i18n.text("jvms.triggers.metric"));
        jvmsTriggerOperatorCombo.promptTextProperty().bind(i18n.text("jvms.triggers.operator"));
        jvmsTriggerThresholdField.promptTextProperty().bind(i18n.text("jvms.triggers.threshold"));
        jvmsTriggerActionCombo.promptTextProperty().bind(i18n.text("jvms.triggers.action"));
        jvmsTriggerCommandCombo.promptTextProperty().bind(i18n.text("jvms.triggers.command"));
        jvmsAddTriggerButton.textProperty().bind(i18n.text("jvms.triggers.add"));
        jvmsRemoveTriggerButton.textProperty().bind(i18n.text("jvms.triggers.remove"));
        jvmsEvaluateTriggersButton.textProperty().bind(i18n.text("jvms.triggers.evaluate"));
        jvmsAddMonitoringSubscriptionButton.textProperty().bind(i18n.text("jvms.monitoring.addSubscription"));
        jvmsSampleSubscriptionButton.textProperty().bind(i18n.text("jvms.monitoring.sampleNow"));
        jvmsAddNotificationSubscriptionButton.textProperty().bind(i18n.text("jvms.monitoring.addNotification"));
        jvmsStartNotificationsButton.textProperty().bind(i18n.text("jvms.monitoring.startNotifications"));
        jvmsStopNotificationsButton.textProperty().bind(i18n.text("jvms.monitoring.stopNotifications"));
        jvmsAgentPresetCombo.promptTextProperty().bind(i18n.text("jvms.agent.preset"));
        jvmsRefreshAgentButton.textProperty().bind(i18n.text("jvms.agent.refresh"));
        jvmsLoadAgentPresetButton.textProperty().bind(i18n.text("jvms.agent.loadPreset"));
        jvmsApplyAgentConfigurationButton.textProperty().bind(i18n.text("jvms.agent.apply"));
        jvmsAgentConfigurationTitleLabel.textProperty().bind(i18n.text("jvms.agent.configuration"));
    }
    private static final class OverviewSequenceTickFormatter extends StringConverter<Number> {
        @Override
        public String toString(Number value) {
            if (value == null) {
                return "";
            }
            return DisplayFormats.formatInteger(Math.round(value.doubleValue()));
        }

        @Override
        public Number fromString(String string) {
            return 0;
        }
    }

    private enum OverviewChartAxis {
        PERCENT,
        MEMORY,
        COUNT,
        LOAD
    }

    private static final class OverviewValueTickFormatter extends StringConverter<Number> {

        private final OverviewChartAxis axis;

        private OverviewValueTickFormatter(OverviewChartAxis axis) {
            this.axis = axis == null ? OverviewChartAxis.COUNT : axis;
        }

        @Override
        public String toString(Number value) {
            if (value == null) {
                return "";
            }
            double numeric = value.doubleValue();
            if (axis == OverviewChartAxis.PERCENT) {
                return DisplayFormats.formatInteger(Math.round(numeric)) + "%";
            }
            if (axis == OverviewChartAxis.MEMORY) {
                return DisplayFormats.formatFileSize(Math.round(numeric));
            }
            if (axis == OverviewChartAxis.LOAD && Math.abs(numeric) < 100) {
                return String.format(java.util.Locale.US, "%.2f", numeric);
            }
            return compactNumber(numeric);
        }

        private static String compactNumber(double numeric) {
            double absolute = Math.abs(numeric);
            if (absolute >= 1_000_000_000) {
                return String.format(java.util.Locale.US, "%.1fB", numeric / 1_000_000_000.0);
            }
            if (absolute >= 1_000_000) {
                return String.format(java.util.Locale.US, "%.1fM", numeric / 1_000_000.0);
            }
            if (absolute >= 1_000) {
                return String.format(java.util.Locale.US, "%.1fK", numeric / 1_000.0);
            }
            if (absolute >= 100) {
                return DisplayFormats.formatInteger(Math.round(numeric));
            }
            return String.format(java.util.Locale.US, "%.1f", numeric);
        }

        @Override
        public Number fromString(String string) {
            return 0;
        }
    }
}
