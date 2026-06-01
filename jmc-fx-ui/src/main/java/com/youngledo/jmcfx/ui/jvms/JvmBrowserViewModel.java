package com.youngledo.jmcfx.ui.jvms;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.youngledo.jmcfx.domain.model.DiagnosticCommandInfo;
import com.youngledo.jmcfx.domain.model.DiagnosticCommandRequest;
import com.youngledo.jmcfx.domain.model.DiagnosticCommandResult;
import com.youngledo.jmcfx.domain.model.FlightRecordingInfo;
import com.youngledo.jmcfx.domain.model.FlightRecordingStartRequest;
import com.youngledo.jmcfx.domain.model.FlightRecordingState;
import com.youngledo.jmcfx.domain.model.FlightRecordingStopRequest;
import com.youngledo.jmcfx.domain.model.FlightRecordingTemplate;
import com.youngledo.jmcfx.domain.model.JmcAgentPreset;
import com.youngledo.jmcfx.domain.model.JmcAgentStatus;
import com.youngledo.jmcfx.domain.model.JmcAgentTransform;
import com.youngledo.jmcfx.domain.model.JmxAttributeSubscription;
import com.youngledo.jmcfx.domain.model.JmxNotificationEvent;
import com.youngledo.jmcfx.domain.model.JmxNotificationSubscription;
import com.youngledo.jmcfx.domain.model.JmxSubscriptionSample;
import com.youngledo.jmcfx.domain.model.JvmCapability;
import com.youngledo.jmcfx.domain.model.JvmCapabilityStatus;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.model.JvmConnectionSource;
import com.youngledo.jmcfx.domain.model.JvmSessionSnapshot;
import com.youngledo.jmcfx.domain.model.LiveMetricDefinition;
import com.youngledo.jmcfx.domain.model.LiveMetricKind;
import com.youngledo.jmcfx.domain.model.LiveMetricSnapshot;
import com.youngledo.jmcfx.domain.model.MBeanAttributeInfo;
import com.youngledo.jmcfx.domain.model.MBeanNode;
import com.youngledo.jmcfx.domain.model.MBeanOperationInfo;
import com.youngledo.jmcfx.domain.model.MBeanOperationParameter;
import com.youngledo.jmcfx.domain.model.MBeanOperationRequest;
import com.youngledo.jmcfx.domain.model.MBeanOperationResult;
import com.youngledo.jmcfx.domain.model.SavedJvmTarget;
import com.youngledo.jmcfx.domain.model.TriggerAction;
import com.youngledo.jmcfx.domain.model.TriggerActionType;
import com.youngledo.jmcfx.domain.model.TriggerEvent;
import com.youngledo.jmcfx.domain.model.TriggerOperator;
import com.youngledo.jmcfx.domain.model.TriggerRule;
import com.youngledo.jmcfx.domain.service.DiagnosticCommandService;
import com.youngledo.jmcfx.domain.service.FlightRecordingService;
import com.youngledo.jmcfx.domain.service.JmcAgentService;
import com.youngledo.jmcfx.domain.service.JmcFxException;
import com.youngledo.jmcfx.domain.service.JmxConnectionService;
import com.youngledo.jmcfx.domain.service.JmxMonitoringRepository;
import com.youngledo.jmcfx.domain.service.JmxMonitoringService;
import com.youngledo.jmcfx.domain.service.JdpDiscoveryService;
import com.youngledo.jmcfx.domain.service.JvmDiscoveryService;
import com.youngledo.jmcfx.domain.service.LiveMetricService;
import com.youngledo.jmcfx.domain.service.MBeanBrowserService;
import com.youngledo.jmcfx.domain.service.SavedJvmTargetRepository;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class JvmBrowserViewModel implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger(JvmBrowserViewModel.class);
    private static final Duration JDP_DISCOVERY_TIMEOUT = Duration.ofMillis(750);
    private static final int OVERVIEW_HISTORY_LIMIT = 120;
    private static final DateTimeFormatter RECORDING_NAME_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final JvmDiscoveryService discoveryService;
    private final JmxConnectionService connectionService;
    private final FlightRecordingService flightRecordingService;
    private final MBeanBrowserService mBeanBrowserService;
    private final DiagnosticCommandService diagnosticCommandService;
    private final LiveMetricService liveMetricService;
    private final JmcAgentService jmcAgentService;
    private final JmxMonitoringService jmxMonitoringService;
    private final JmxMonitoringRepository jmxMonitoringRepository;
    private final SavedJvmTargetRepository savedTargetRepository;
    private final JdpDiscoveryService jdpDiscoveryService;
    private final JvmBrowserExecutor executor;
    private final Consumer<Runnable> fxRunner;
    private final Consumer<Path> savedRecordingHandler;
    private final ObservableList<JvmConnection> connections = FXCollections.observableArrayList();
    private final ObservableList<FlightRecordingInfo> flightRecordings = FXCollections.observableArrayList();
    private final ObservableList<MBeanNode> mbeanTree = FXCollections.observableArrayList();
    private final ObservableList<MBeanAttributeInfo> mbeanAttributes = FXCollections.observableArrayList();
    private final ObservableList<MBeanOperationInfo> mbeanOperations = FXCollections.observableArrayList();
    private final ObservableList<DiagnosticCommandInfo> diagnosticCommands = FXCollections.observableArrayList();
    private final ObservableList<LiveMetricDefinition> liveMetricDefinitions = FXCollections.observableArrayList();
    private final ObservableList<JmcAgentPreset> jmcAgentPresets = FXCollections.observableArrayList();
    private final ObservableList<JmcAgentTransform> jmcAgentTransforms = FXCollections.observableArrayList();
    private final ObservableList<TriggerRule> triggerRules = FXCollections.observableArrayList();
    private final ObservableList<TriggerEvent> triggerEvents = FXCollections.observableArrayList();
    private final ObservableList<JmxAttributeSubscription> jmxAttributeSubscriptions =
            FXCollections.observableArrayList();
    private final ObservableList<JmxSubscriptionSample> jmxSubscriptionSamples = FXCollections.observableArrayList();
    private final ObservableList<JmxNotificationSubscription> jmxNotificationSubscriptions =
            FXCollections.observableArrayList();
    private final ObservableList<JmxNotificationEvent> jmxNotificationEvents = FXCollections.observableArrayList();
    private final ObservableList<LiveJvmOverviewMetric> overviewMetrics = FXCollections.observableArrayList();
    private final ObjectProperty<JvmConnection> selectedConnection = new SimpleObjectProperty<>();
    private final ObjectProperty<FlightRecordingInfo> selectedFlightRecording = new SimpleObjectProperty<>();
    private final ObjectProperty<MBeanNode> selectedMBean = new SimpleObjectProperty<>();
    private final ObjectProperty<MBeanOperationInfo> selectedMBeanOperation = new SimpleObjectProperty<>();
    private final ObjectProperty<DiagnosticCommandInfo> selectedDiagnosticCommand = new SimpleObjectProperty<>();
    private final ObjectProperty<LiveMetricDefinition> selectedTriggerMetric = new SimpleObjectProperty<>();
    private final ObjectProperty<JmcAgentPreset> selectedJmcAgentPreset = new SimpleObjectProperty<>();
    private final ObjectProperty<JmxAttributeSubscription> selectedJmxAttributeSubscription =
            new SimpleObjectProperty<>();
    private final ObjectProperty<JmxNotificationSubscription> selectedJmxNotificationSubscription =
            new SimpleObjectProperty<>();
    private final ObjectProperty<TriggerOperator> selectedTriggerOperator =
            new SimpleObjectProperty<>(TriggerOperator.GREATER_THAN_OR_EQUAL);
    private final ObjectProperty<TriggerActionType> selectedTriggerActionType =
            new SimpleObjectProperty<>(TriggerActionType.NOTIFY);
    private final ObjectProperty<DiagnosticCommandInfo> selectedTriggerCommand = new SimpleObjectProperty<>();
    private final StringProperty manualConnectionName = new SimpleStringProperty("");
    private final StringProperty manualConnectionUrl = new SimpleStringProperty("");
    private final BooleanProperty jdpRefreshInProgress = new SimpleBooleanProperty(false);
    private final StringProperty jdpStatusMessage = new SimpleStringProperty("Idle.");
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final StringProperty mbeanErrorMessage = new SimpleStringProperty("");
    private final StringProperty mbeanOperationArguments = new SimpleStringProperty("");
    private final StringProperty mbeanOperationResult = new SimpleStringProperty("");
    private final StringProperty diagnosticCommandArguments = new SimpleStringProperty("");
    private final StringProperty diagnosticCommandOutput = new SimpleStringProperty("");
    private final StringProperty diagnosticCommandErrorMessage = new SimpleStringProperty("");
    private final StringProperty jmcAgentConfiguration = new SimpleStringProperty("");
    private final StringProperty jmcAgentStatusMessage = new SimpleStringProperty("");
    private final StringProperty jmcAgentErrorMessage = new SimpleStringProperty("");
    private final StringProperty triggerName = new SimpleStringProperty("");
    private final StringProperty triggerThreshold = new SimpleStringProperty("");
    private final StringProperty triggerErrorMessage = new SimpleStringProperty("");
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final BooleanProperty error = new SimpleBooleanProperty(false);
    private final BooleanProperty refreshCompleted = new SimpleBooleanProperty(false);
    private final ObjectProperty<JvmSessionSnapshot> selectedSession = new SimpleObjectProperty<>();
    private final BooleanProperty sessionLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty sessionError = new SimpleBooleanProperty(false);
    private final StringProperty sessionErrorMessage = new SimpleStringProperty("");
    private final BooleanProperty recordingControlAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty recordingLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty recordingError = new SimpleBooleanProperty(false);
    private final StringProperty recordingErrorMessage = new SimpleStringProperty("");
    private final StringProperty recordingStatusMessage = new SimpleStringProperty("");
    private final BooleanProperty mbeanBrowserAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty mbeanLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty mbeanError = new SimpleBooleanProperty(false);
    private final BooleanProperty diagnosticCommandsAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty diagnosticCommandLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty diagnosticCommandError = new SimpleBooleanProperty(false);
    private final BooleanProperty jmcAgentAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty jmcAgentLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty jmcAgentError = new SimpleBooleanProperty(false);
    private final BooleanProperty jmxMonitoringAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty jmxMonitoringLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty jmxMonitoringError = new SimpleBooleanProperty(false);
    private final StringProperty jmxMonitoringErrorMessage = new SimpleStringProperty("");
    private final ObjectProperty<LiveJvmPersistenceOverview> overviewPersistence =
            new SimpleObjectProperty<>(LiveJvmPersistenceOverview.notConfigured());
    private final BooleanProperty triggerLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty triggerError = new SimpleBooleanProperty(false);
    private final BooleanProperty overviewLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty overviewError = new SimpleBooleanProperty(false);
    private final StringProperty overviewErrorMessage = new SimpleStringProperty("");
    private final Map<Long, JvmConnection> sessionStartedRecordings = new HashMap<>();
    private final Map<String, JvmConnection> liveConnectionsByStableKey = new HashMap<>();
    private int pendingWorkCount;
    private long sessionLoadGeneration;
    private long mbeanRequestGeneration;
    private long diagnosticCommandRequestGeneration;
    private long jmcAgentRequestGeneration;
    private long jmxMonitoringGeneration;
    private long overviewRequestGeneration;
    private long overviewSampleSequence;
    private long triggerRuleSequence;
    private long triggerEvaluationGeneration;

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService) {
        this(discoveryService, connectionService, null, new VirtualThreadJvmBrowserExecutor(),
                javafx.application.Platform::runLater, path -> { });
    }

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService,
            JvmBrowserExecutor executor, Consumer<Runnable> fxRunner) {
        this(discoveryService, connectionService, null, executor, fxRunner, path -> { });
    }

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService,
            MBeanBrowserService mBeanBrowserService, JvmBrowserExecutor executor, Consumer<Runnable> fxRunner) {
        this(discoveryService, connectionService, null, mBeanBrowserService, null, null, executor, fxRunner,
                path -> { });
    }

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService,
            FlightRecordingService flightRecordingService, JvmBrowserExecutor executor, Consumer<Runnable> fxRunner,
            Consumer<Path> savedRecordingHandler) {
        this(discoveryService, connectionService, flightRecordingService, null, null, null, executor, fxRunner,
                savedRecordingHandler);
    }

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService,
            FlightRecordingService flightRecordingService, MBeanBrowserService mBeanBrowserService,
            JvmBrowserExecutor executor, Consumer<Runnable> fxRunner, Consumer<Path> savedRecordingHandler) {
        this(discoveryService, connectionService, flightRecordingService, mBeanBrowserService, null, null, executor,
                fxRunner, savedRecordingHandler);
    }

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService,
            FlightRecordingService flightRecordingService, MBeanBrowserService mBeanBrowserService,
            DiagnosticCommandService diagnosticCommandService, LiveMetricService liveMetricService,
            JvmBrowserExecutor executor, Consumer<Runnable> fxRunner, Consumer<Path> savedRecordingHandler) {
        this(discoveryService, connectionService, flightRecordingService, mBeanBrowserService,
                diagnosticCommandService, liveMetricService, null, null, null, null, null, executor, fxRunner,
                savedRecordingHandler);
    }

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService,
            FlightRecordingService flightRecordingService, MBeanBrowserService mBeanBrowserService,
            DiagnosticCommandService diagnosticCommandService, LiveMetricService liveMetricService,
            JmcAgentService jmcAgentService, JvmBrowserExecutor executor, Consumer<Runnable> fxRunner,
            Consumer<Path> savedRecordingHandler) {
        this(discoveryService, connectionService, flightRecordingService, mBeanBrowserService,
                diagnosticCommandService, liveMetricService, jmcAgentService, null, null, null, null, executor, fxRunner,
                savedRecordingHandler);
    }

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService,
            FlightRecordingService flightRecordingService, MBeanBrowserService mBeanBrowserService,
            DiagnosticCommandService diagnosticCommandService, LiveMetricService liveMetricService,
            SavedJvmTargetRepository savedTargetRepository, JdpDiscoveryService jdpDiscoveryService,
            JvmBrowserExecutor executor, Consumer<Runnable> fxRunner, Consumer<Path> savedRecordingHandler) {
        this(discoveryService, connectionService, flightRecordingService, mBeanBrowserService,
                diagnosticCommandService, liveMetricService, null, null, null, savedTargetRepository,
                jdpDiscoveryService, executor, fxRunner, savedRecordingHandler);
    }

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService,
            FlightRecordingService flightRecordingService, MBeanBrowserService mBeanBrowserService,
            DiagnosticCommandService diagnosticCommandService, LiveMetricService liveMetricService,
            JmcAgentService jmcAgentService, SavedJvmTargetRepository savedTargetRepository,
            JdpDiscoveryService jdpDiscoveryService, JvmBrowserExecutor executor, Consumer<Runnable> fxRunner,
            Consumer<Path> savedRecordingHandler) {
        this(discoveryService, connectionService, flightRecordingService, mBeanBrowserService,
                diagnosticCommandService, liveMetricService, jmcAgentService, null, null, savedTargetRepository,
                jdpDiscoveryService, executor, fxRunner, savedRecordingHandler);
    }

    public JvmBrowserViewModel(JvmDiscoveryService discoveryService, JmxConnectionService connectionService,
            FlightRecordingService flightRecordingService, MBeanBrowserService mBeanBrowserService,
            DiagnosticCommandService diagnosticCommandService, LiveMetricService liveMetricService,
            JmcAgentService jmcAgentService, JmxMonitoringService jmxMonitoringService,
            JmxMonitoringRepository jmxMonitoringRepository, SavedJvmTargetRepository savedTargetRepository,
            JdpDiscoveryService jdpDiscoveryService, JvmBrowserExecutor executor, Consumer<Runnable> fxRunner,
            Consumer<Path> savedRecordingHandler) {
        this.discoveryService = Objects.requireNonNull(discoveryService, "discoveryService");
        this.connectionService = Objects.requireNonNull(connectionService, "connectionService");
        this.flightRecordingService = flightRecordingService;
        this.mBeanBrowserService = mBeanBrowserService;
        this.diagnosticCommandService = diagnosticCommandService;
        this.liveMetricService = liveMetricService;
        this.jmcAgentService = jmcAgentService;
        this.jmxMonitoringService = jmxMonitoringService;
        this.jmxMonitoringRepository = jmxMonitoringRepository;
        this.savedTargetRepository = savedTargetRepository;
        this.jdpDiscoveryService = jdpDiscoveryService;
        this.executor = Objects.requireNonNull(executor, "executor");
        this.fxRunner = Objects.requireNonNull(fxRunner, "fxRunner");
        this.savedRecordingHandler = Objects.requireNonNull(savedRecordingHandler, "savedRecordingHandler");
        this.selectedConnection.addListener((observable, oldValue, newValue) -> loadSessionForSelection(newValue));
        this.selectedMBean.addListener((observable, oldValue, newValue) -> loadSelectedMBeanDetails(newValue));
        this.selectedMBeanOperation.addListener((observable, oldValue, newValue) -> clearMBeanOperationResult());
        this.selectedDiagnosticCommand.addListener((observable, oldValue, newValue) -> clearDiagnosticCommandResult());
        this.selectedJmxAttributeSubscription.addListener((observable, oldValue, newValue) ->
                loadSamplesForSelection(newValue));
        this.selectedJmxNotificationSubscription.addListener((observable, oldValue, newValue) ->
                loadNotificationEventsForSelection(newValue));
    }

    public ObservableList<JvmConnection> connectionsProperty() {
        return connections;
    }

    public ObjectProperty<JvmConnection> selectedConnectionProperty() {
        return selectedConnection;
    }

    public ObservableList<FlightRecordingInfo> flightRecordingsProperty() {
        return flightRecordings;
    }

    public ObjectProperty<FlightRecordingInfo> selectedFlightRecordingProperty() {
        return selectedFlightRecording;
    }

    public StringProperty manualConnectionUrlProperty() {
        return manualConnectionUrl;
    }

    public StringProperty manualConnectionNameProperty() {
        return manualConnectionName;
    }

    public BooleanProperty jdpRefreshInProgressProperty() {
        return jdpRefreshInProgress;
    }

    public StringProperty jdpStatusMessageProperty() {
        return jdpStatusMessage;
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public BooleanProperty errorProperty() {
        return error;
    }

    public BooleanProperty refreshCompletedProperty() {
        return refreshCompleted;
    }

    public ObjectProperty<JvmSessionSnapshot> selectedSessionProperty() {
        return selectedSession;
    }

    public BooleanProperty sessionLoadingProperty() {
        return sessionLoading;
    }

    public BooleanProperty sessionErrorProperty() {
        return sessionError;
    }

    public StringProperty sessionErrorMessageProperty() {
        return sessionErrorMessage;
    }

    public BooleanProperty recordingControlAvailableProperty() {
        return recordingControlAvailable;
    }

    public BooleanProperty recordingLoadingProperty() {
        return recordingLoading;
    }

    public BooleanProperty recordingErrorProperty() {
        return recordingError;
    }

    public StringProperty recordingErrorMessageProperty() {
        return recordingErrorMessage;
    }

    public StringProperty recordingStatusMessageProperty() {
        return recordingStatusMessage;
    }

    public ObservableList<MBeanNode> mbeanTreeProperty() {
        return mbeanTree;
    }

    public ObservableList<MBeanAttributeInfo> mbeanAttributesProperty() {
        return mbeanAttributes;
    }

    public ObservableList<MBeanOperationInfo> mbeanOperationsProperty() {
        return mbeanOperations;
    }

    public ObjectProperty<MBeanNode> selectedMBeanProperty() {
        return selectedMBean;
    }

    public ObjectProperty<MBeanOperationInfo> selectedMBeanOperationProperty() {
        return selectedMBeanOperation;
    }

    public BooleanProperty mbeanBrowserAvailableProperty() {
        return mbeanBrowserAvailable;
    }

    public BooleanProperty mbeanLoadingProperty() {
        return mbeanLoading;
    }

    public BooleanProperty mbeanErrorProperty() {
        return mbeanError;
    }

    public StringProperty mbeanErrorMessageProperty() {
        return mbeanErrorMessage;
    }

    public StringProperty mbeanOperationArgumentsProperty() {
        return mbeanOperationArguments;
    }

    public StringProperty mbeanOperationResultProperty() {
        return mbeanOperationResult;
    }

    public ObservableList<DiagnosticCommandInfo> diagnosticCommandsProperty() {
        return diagnosticCommands;
    }

    public ObjectProperty<DiagnosticCommandInfo> selectedDiagnosticCommandProperty() {
        return selectedDiagnosticCommand;
    }

    public StringProperty diagnosticCommandArgumentsProperty() {
        return diagnosticCommandArguments;
    }

    public StringProperty diagnosticCommandOutputProperty() {
        return diagnosticCommandOutput;
    }

    public StringProperty diagnosticCommandErrorMessageProperty() {
        return diagnosticCommandErrorMessage;
    }

    public BooleanProperty diagnosticCommandsAvailableProperty() {
        return diagnosticCommandsAvailable;
    }

    public BooleanProperty diagnosticCommandLoadingProperty() {
        return diagnosticCommandLoading;
    }

    public BooleanProperty diagnosticCommandErrorProperty() {
        return diagnosticCommandError;
    }

    public ObservableList<JmcAgentPreset> jmcAgentPresetsProperty() {
        return jmcAgentPresets;
    }

    public ObjectProperty<JmcAgentPreset> selectedJmcAgentPresetProperty() {
        return selectedJmcAgentPreset;
    }

    public ObservableList<JmcAgentTransform> jmcAgentTransformsProperty() {
        return jmcAgentTransforms;
    }

    public StringProperty jmcAgentConfigurationProperty() {
        return jmcAgentConfiguration;
    }

    public StringProperty jmcAgentStatusMessageProperty() {
        return jmcAgentStatusMessage;
    }

    public StringProperty jmcAgentErrorMessageProperty() {
        return jmcAgentErrorMessage;
    }

    public BooleanProperty jmcAgentAvailableProperty() {
        return jmcAgentAvailable;
    }

    public BooleanProperty jmcAgentLoadingProperty() {
        return jmcAgentLoading;
    }

    public BooleanProperty jmcAgentErrorProperty() {
        return jmcAgentError;
    }

    public ObservableList<JmxAttributeSubscription> jmxAttributeSubscriptionsProperty() {
        return jmxAttributeSubscriptions;
    }

    public ObservableList<JmxSubscriptionSample> jmxSubscriptionSamplesProperty() {
        return jmxSubscriptionSamples;
    }

    public ObservableList<JmxNotificationSubscription> jmxNotificationSubscriptionsProperty() {
        return jmxNotificationSubscriptions;
    }

    public ObservableList<JmxNotificationEvent> jmxNotificationEventsProperty() {
        return jmxNotificationEvents;
    }

    public ObjectProperty<JmxAttributeSubscription> selectedJmxAttributeSubscriptionProperty() {
        return selectedJmxAttributeSubscription;
    }

    public ObjectProperty<JmxNotificationSubscription> selectedJmxNotificationSubscriptionProperty() {
        return selectedJmxNotificationSubscription;
    }

    public BooleanProperty jmxMonitoringAvailableProperty() {
        return jmxMonitoringAvailable;
    }

    public BooleanProperty jmxMonitoringLoadingProperty() {
        return jmxMonitoringLoading;
    }

    public BooleanProperty jmxMonitoringErrorProperty() {
        return jmxMonitoringError;
    }

    public StringProperty jmxMonitoringErrorMessageProperty() {
        return jmxMonitoringErrorMessage;
    }

    public ObservableList<LiveJvmOverviewMetric> overviewMetricsProperty() {
        return overviewMetrics;
    }

    public ObjectProperty<LiveJvmPersistenceOverview> overviewPersistenceProperty() {
        return overviewPersistence;
    }

    public BooleanProperty overviewLoadingProperty() {
        return overviewLoading;
    }

    public BooleanProperty overviewErrorProperty() {
        return overviewError;
    }

    public StringProperty overviewErrorMessageProperty() {
        return overviewErrorMessage;
    }

    public ObservableList<LiveMetricDefinition> liveMetricDefinitionsProperty() {
        return liveMetricDefinitions;
    }

    public ObservableList<TriggerRule> triggerRulesProperty() {
        return triggerRules;
    }

    public ObservableList<TriggerEvent> triggerEventsProperty() {
        return triggerEvents;
    }

    public ObjectProperty<LiveMetricDefinition> selectedTriggerMetricProperty() {
        return selectedTriggerMetric;
    }

    public ObjectProperty<TriggerOperator> selectedTriggerOperatorProperty() {
        return selectedTriggerOperator;
    }

    public ObjectProperty<TriggerActionType> selectedTriggerActionTypeProperty() {
        return selectedTriggerActionType;
    }

    public ObjectProperty<DiagnosticCommandInfo> selectedTriggerCommandProperty() {
        return selectedTriggerCommand;
    }

    public StringProperty triggerNameProperty() {
        return triggerName;
    }

    public StringProperty triggerThresholdProperty() {
        return triggerThreshold;
    }

    public StringProperty triggerErrorMessageProperty() {
        return triggerErrorMessage;
    }

    public BooleanProperty triggerLoadingProperty() {
        return triggerLoading;
    }

    public BooleanProperty triggerErrorProperty() {
        return triggerError;
    }

    public FlightRecordingInfo selectedFlightRecording() {
        return selectedFlightRecording.get();
    }

    public void refresh() {
        beginWork();
        executor.execute(() -> {
            try {
                List<JvmConnection> discovered = discoveryService.discoverLocalJvms();
                if (savedTargetRepository != null) {
                    discovered = new ArrayList<>(discovered);
                    discovered.addAll(savedTargetRepository.findAll().stream()
                            .map(JvmConnection::saved)
                            .toList());
                }
                List<JvmConnection> refreshed = discovered;
                runOnFx(() -> {
                    mergeDiscovered(refreshed);
                    refreshCompleted.set(true);
                    statusMessage.set("");
                    clearError();
                    finishWork();
                });
            } catch (RuntimeException exception) {
                fail(exception);
            }
        });
    }

    public void connectSelectedOrManual() {
        String url = manualConnectionUrl.get() == null ? "" : manualConnectionUrl.get().trim();
        if (!url.isBlank()) {
            connectManual(url);
            return;
        }
        connectSelected();
    }

    public void connectSelected() {
        JvmConnection selected = selectedConnection.get();
        if (!canConnectJvm(selected)) {
            error.set(true);
            errorMessage.set("Enter a JMX service URL or select an attachable JVM.");
            return;
        }
        if (selected.source() == JvmConnectionSource.LOCAL) {
            connectLocal(selected);
        } else {
            connectRemote(selected);
        }
    }

    public void connectManual() {
        connectSelectedOrManual();
    }

    public void saveManualTarget() {
        if (savedTargetRepository == null) {
            error.set(true);
            errorMessage.set("Saved JVM targets are not configured.");
            return;
        }
        String url = Objects.requireNonNullElse(manualConnectionUrl.get(), "").trim();
        if (url.isBlank()) {
            error.set(true);
            errorMessage.set("Enter a JMX service URL to save.");
            return;
        }
        String name = Objects.requireNonNullElse(manualConnectionName.get(), "").trim();
        if (name.isBlank()) {
            name = url;
        }
        try {
            SavedJvmTarget saved = savedTargetRepository.save(new SavedJvmTarget("", name, url, null));
            JvmConnection savedConnection = JvmConnection.saved(saved);
            replaceSavedCandidate(savedConnection);
            manualConnectionName.set("");
            manualConnectionUrl.set("");
            statusMessage.set("Saved " + savedConnection.displayName() + ".");
            clearError();
        } catch (RuntimeException exception) {
            logActionFailure("Saved JVM target action failed", exception);
            error.set(true);
            errorMessage.set(displayMessage(exception));
        }
    }

    public void removeSelectedSavedTarget() {
        JvmConnection selected = selectedConnection.get();
        if (savedTargetRepository == null || selected == null || selected.source() != JvmConnectionSource.SAVED
                || selected.connected()) {
            error.set(true);
            errorMessage.set("Select a disconnected saved JVM target to remove.");
            return;
        }
        try {
            savedTargetRepository.deleteById(selected.id());
            connections.remove(selected);
            selectedConnection.set(connections.isEmpty() ? null : connections.getFirst());
            statusMessage.set("Removed saved JVM target.");
            clearError();
        } catch (RuntimeException exception) {
            logActionFailure("Saved JVM target action failed", exception);
            error.set(true);
            errorMessage.set(displayMessage(exception));
        }
    }

    public void refreshJdp() {
        if (jdpDiscoveryService == null) {
            jdpRefreshInProgress.set(false);
            jdpStatusMessage.set("JDP discovery is not configured.");
            return;
        }
        jdpRefreshInProgress.set(true);
        jdpStatusMessage.set("Refreshing JDP targets.");
        executor.execute(() -> {
            try {
                List<JvmConnection> discovered = jdpDiscoveryService.discover(JDP_DISCOVERY_TIMEOUT).stream()
                        .map(JvmConnection::jdp)
                        .toList();
                runOnFx(() -> {
                    mergeJdp(discovered);
                    jdpStatusMessage.set(jdpStatus(discovered.size()));
                    jdpRefreshInProgress.set(false);
                });
            } catch (RuntimeException exception) {
                logActionFailure("JDP discovery failed", exception);
                runOnFx(() -> {
                    jdpStatusMessage.set("JDP discovery failed: " + displayMessage(exception));
                    jdpRefreshInProgress.set(false);
                });
            }
        });
    }

    public void disconnectSelected() {
        JvmConnection selected = selectedConnection.get();
        if (selected == null || !selected.connected()) {
            error.set(true);
            errorMessage.set("Select a connected JVM to disconnect.");
            return;
        }
        beginWork();
        executor.execute(() -> {
            try {
                connectionService.disconnect(liveConnectionFor(selected));
                JvmConnection disconnected = selected.asDisconnected("Disconnected");
                runOnFx(() -> {
                    liveConnectionsByStableKey.remove(stableKey(selected));
                    replaceOrAdd(disconnected, "Disconnected.");
                    selectedSession.set(null);
                    clearRecordingControl();
                    clearDiagnosticCommands();
                    clearTriggerSessionState();
                    clearSessionError();
                });
            } catch (RuntimeException exception) {
                fail(exception);
            }
        });
    }

    public void startFlightRecording() {
        JvmConnection selected = selectedConnection.get();
        if (!canUseRecordingControl(selected)) {
            recordingError.set(true);
            recordingErrorMessage.set("Select a connected JVM with Flight Recorder available.");
            return;
        }
        recordingLoading.set(true);
        clearRecordingError();
        executor.execute(() -> {
            try {
                JvmConnection liveConnection = liveConnectionFor(selected);
                FlightRecordingStartRequest request = new FlightRecordingStartRequest(liveConnection,
                        recordingName(selected), FlightRecordingTemplate.profile());
                FlightRecordingInfo started = flightRecordingService.startRecording(request);
                sessionStartedRecordings.put(started.id(), liveConnection);
                List<FlightRecordingInfo> updated = flightRecordingService.recordings(liveConnection);
                runOnFx(() -> {
                    flightRecordings.setAll(updated);
                    selectedFlightRecording.set(updated.isEmpty() ? null : updated.getLast());
                    recordingStatusMessage.set("");
                    recordingLoading.set(false);
                });
            } catch (RuntimeException exception) {
                failRecording(exception);
            }
        });
    }

    public void stopAndSaveSelectedFlightRecording(Path destinationFile) {
        JvmConnection selectedConnection = this.selectedConnection.get();
        FlightRecordingInfo selectedRecording = selectedFlightRecording.get();
        if (!canUseRecordingControl(selectedConnection) || selectedRecording == null) {
            recordingError.set(true);
            recordingErrorMessage.set("Select a running Flight Recording to save.");
            return;
        }
        recordingLoading.set(true);
        clearRecordingError();
        executor.execute(() -> {
            try {
                JvmConnection liveConnection = liveConnectionFor(selectedConnection);
                Path saved = flightRecordingService.stopAndSaveRecording(new FlightRecordingStopRequest(
                        liveConnection, selectedRecording.id(), destinationFile));
                sessionStartedRecordings.remove(selectedRecording.id());
                List<FlightRecordingInfo> updated = flightRecordingService.recordings(liveConnection);
                runOnFx(() -> {
                    flightRecordings.setAll(updated);
                    selectedFlightRecording.set(updated.isEmpty() ? null : updated.getFirst());
                    recordingStatusMessage.set("");
                    recordingLoading.set(false);
                    savedRecordingHandler.accept(saved);
                });
            } catch (RuntimeException exception) {
                failRecording(exception);
            }
        });
    }

    public void refreshSelectedMBeanAttributes() {
        loadSelectedMBeanDetails(selectedMBean.get());
    }

    public void invokeSelectedMBeanOperation() {
        JvmSessionSnapshot snapshot = selectedSession.get();
        MBeanNode node = selectedMBean.get();
        MBeanOperationInfo operation = selectedMBeanOperation.get();
        if (!canUseMBeanBrowser(snapshot) || node == null || node.domain() || operation == null) {
            mbeanError.set(true);
            mbeanErrorMessage.set("Select an MBean operation to invoke.");
            return;
        }

        List<String> parameterTypes = operation.parameters().stream()
                .map(MBeanOperationParameter::type)
                .toList();
        List<String> arguments = parseMBeanArguments(mbeanOperationArguments.get());
        MBeanOperationRequest request = new MBeanOperationRequest(snapshot.connection(), node.objectName(),
                operation.name(), parameterTypes, arguments);
        long generation = nextMBeanRequestGeneration();
        mbeanLoading.set(true);
        clearMBeanError();
        executor.execute(() -> {
            try {
                MBeanOperationResult result = mBeanBrowserService.invoke(request);
                runOnFx(() -> {
                    if (!isCurrentMBeanRequest(generation, snapshot, node, operation)) {
                        return;
                    }
                    if (result.success()) {
                        mbeanOperationResult.set(result.value());
                        clearMBeanError();
                    } else {
                        String message = displayMBeanResult(result);
                        mbeanOperationResult.set(message);
                        mbeanError.set(true);
                        mbeanErrorMessage.set(message);
                    }
                    mbeanLoading.set(false);
                });
            } catch (RuntimeException exception) {
                failMBean(generation, snapshot, node, operation, exception);
            }
        });
    }

    public void executeSelectedDiagnosticCommand() {
        JvmSessionSnapshot snapshot = selectedSession.get();
        DiagnosticCommandInfo command = selectedDiagnosticCommand.get();
        if (!canUseDiagnosticCommands(snapshot) || command == null) {
            diagnosticCommandError.set(true);
            diagnosticCommandErrorMessage.set("Select a Diagnostic Command to execute.");
            return;
        }

        List<String> arguments = parseDiagnosticCommandArguments(diagnosticCommandArguments.get());
        DiagnosticCommandRequest request = new DiagnosticCommandRequest(snapshot.connection(), command.name(),
                arguments);
        long generation = nextDiagnosticCommandRequestGeneration();
        diagnosticCommandLoading.set(true);
        clearDiagnosticCommandError();
        executor.execute(() -> {
            try {
                DiagnosticCommandResult result = diagnosticCommandService.execute(request);
                runOnFx(() -> {
                    if (!isCurrentDiagnosticCommandRequest(generation, snapshot, command)) {
                        return;
                    }
                    if (result.success()) {
                        diagnosticCommandOutput.set(result.output());
                        clearDiagnosticCommandError();
                    } else {
                        String message = displayDiagnosticCommandResult(result);
                        diagnosticCommandOutput.set(message);
                        diagnosticCommandError.set(true);
                        diagnosticCommandErrorMessage.set(message);
                    }
                    diagnosticCommandLoading.set(false);
                });
            } catch (RuntimeException exception) {
                failDiagnosticCommand(generation, snapshot, command, exception);
            }
        });
    }

    public void refreshJmcAgent() {
        JvmSessionSnapshot snapshot = selectedSession.get();
        if (!canLoadJmcAgent(snapshot)) {
            jmcAgentError.set(true);
            jmcAgentErrorMessage.set("Select a connected JVM to inspect JMC Agent.");
            return;
        }
        loadJmcAgent(snapshot);
    }

    public void loadSelectedJmcAgentPreset() {
        JmcAgentPreset preset = selectedJmcAgentPreset.get();
        if (preset == null) {
            return;
        }
        jmcAgentConfiguration.set(preset.xml());
    }

    public void applyJmcAgentConfiguration() {
        JvmSessionSnapshot snapshot = selectedSession.get();
        if (!canUseJmcAgent(snapshot)) {
            jmcAgentError.set(true);
            jmcAgentErrorMessage.set("Select a connected JVM with JMC Agent available.");
            return;
        }
        long generation = nextJmcAgentRequestGeneration();
        String xmlDescription = jmcAgentConfiguration.get();
        jmcAgentLoading.set(true);
        clearJmcAgentError();
        executor.execute(() -> {
            try {
                jmcAgentService.applyConfiguration(snapshot.connection(), xmlDescription);
                JmcAgentStatus status = jmcAgentService.status(snapshot.connection());
                runOnFx(() -> {
                    if (!isCurrentJmcAgentRequest(generation, snapshot)) {
                        return;
                    }
                    applyJmcAgentStatus(status);
                    jmcAgentLoading.set(false);
                    clearJmcAgentError();
                });
            } catch (RuntimeException exception) {
                failJmcAgent(generation, snapshot, exception);
            }
        });
    }

    public void addMBeanAttributeSubscription(
            MBeanAttributeInfo attribute,
            Duration samplingInterval,
            int maxSamples,
            boolean persisted) {
        JvmSessionSnapshot snapshot = selectedSession.get();
        MBeanNode node = selectedMBean.get();
        if (snapshot == null || node == null || attribute == null || node.domain() || !attribute.readable()) {
            failJmxMonitoring("Select a readable MBean attribute to subscribe.");
            return;
        }
        JmxAttributeSubscription subscription = new JmxAttributeSubscription(
                "",
                snapshot.connection().id(),
                node.objectName(),
                attribute.name(),
                attribute.name(),
                "",
                samplingInterval,
                maxSamples,
                true,
                persisted);
        jmxAttributeSubscriptions.add(subscription);
        selectedJmxAttributeSubscription.set(subscription);
        if (persisted && jmxMonitoringRepository != null) {
            jmxMonitoringRepository.saveAttributeSubscription(subscription);
        }
        clearJmxMonitoringError();
    }

    public void addMBeanNotificationSubscription(MBeanNode node, int maxEvents, boolean persisted) {
        JvmSessionSnapshot snapshot = selectedSession.get();
        if (snapshot == null || !jmxMonitoringAvailable.get() || jmxMonitoringService == null) {
            failJmxMonitoring("Select a connected JVM with JMX monitoring available.");
            return;
        }
        if (node == null || node.domain()) {
            failJmxMonitoring("Select an MBean object to subscribe to notifications.");
            return;
        }
        String label = node.name() == null || node.name().isBlank() ? node.objectName() : node.name();
        JmxNotificationSubscription subscription = new JmxNotificationSubscription(
                "",
                snapshot.connection().id(),
                node.objectName(),
                label,
                maxEvents,
                true,
                persisted);
        jmxNotificationSubscriptions.add(subscription);
        selectedJmxNotificationSubscription.set(subscription);
        if (persisted && jmxMonitoringRepository != null) {
            jmxMonitoringRepository.saveNotificationSubscription(subscription);
        }
        updateOverviewPersistenceSummary();
        clearJmxMonitoringError();
    }

    public void sampleSelectedJmxSubscriptionNow() {
        JvmSessionSnapshot snapshot = selectedSession.get();
        JmxAttributeSubscription subscription = selectedJmxAttributeSubscription.get();
        if (snapshot == null || subscription == null || jmxMonitoringService == null) {
            failJmxMonitoring("Select a JMX attribute subscription to sample.");
            return;
        }
        long generation = nextJmxMonitoringGeneration();
        jmxMonitoringLoading.set(true);
        clearJmxMonitoringError();
        executor.execute(() -> {
            try {
                JmxSubscriptionSample sample = jmxMonitoringService.sampleAttribute(snapshot.connection(), subscription);
                runOnFx(() -> {
                    if (!isCurrentJmxMonitoringGeneration(generation, snapshot)) {
                        return;
                    }
                    appendBoundedSample(subscription, sample);
                    if (subscription.persisted() && jmxMonitoringRepository != null) {
                        jmxMonitoringRepository.appendSample(sample);
                    }
                    jmxMonitoringLoading.set(false);
                    clearJmxMonitoringError();
                });
            } catch (RuntimeException exception) {
                failJmxMonitoring(generation, snapshot, exception);
            }
        });
    }

    public void startSelectedJmxNotifications() {
        JvmSessionSnapshot snapshot = selectedSession.get();
        JmxNotificationSubscription subscription = selectedJmxNotificationSubscription.get();
        if (snapshot == null || subscription == null || jmxMonitoringService == null) {
            failJmxMonitoring("Select a JMX notification subscription to start.");
            return;
        }
        startJmxNotifications(subscription);
    }

    public void startJmxNotifications(JmxNotificationSubscription subscription) {
        JvmSessionSnapshot snapshot = selectedSession.get();
        if (snapshot == null || subscription == null || jmxMonitoringService == null) {
            failJmxMonitoring("Select a JMX notification subscription to start.");
            return;
        }
        if (!jmxNotificationSubscriptions.contains(subscription)) {
            jmxNotificationSubscriptions.add(subscription);
        }
        selectedJmxNotificationSubscription.set(subscription);
        if (subscription.persisted() && jmxMonitoringRepository != null) {
            jmxMonitoringRepository.saveNotificationSubscription(subscription);
        }
        long generation = nextJmxMonitoringGeneration();
        jmxMonitoringLoading.set(true);
        clearJmxMonitoringError();
        executor.execute(() -> {
            try {
                List<JmxNotificationEvent> initialEvents = jmxMonitoringService.startNotifications(
                        snapshot.connection(), subscription,
                        event -> runOnFx(() -> appendNotificationEvent(subscription, event)));
                runOnFx(() -> {
                    if (!isCurrentJmxMonitoringGeneration(generation, snapshot)) {
                        return;
                    }
                    initialEvents.forEach(event -> appendNotificationEvent(subscription, event));
                    jmxMonitoringLoading.set(false);
                    clearJmxMonitoringError();
                });
            } catch (RuntimeException exception) {
                failJmxMonitoring(generation, snapshot, exception);
            }
        });
    }

    public void stopSelectedJmxNotifications() {
        JvmSessionSnapshot snapshot = selectedSession.get();
        JmxNotificationSubscription subscription = selectedJmxNotificationSubscription.get();
        if (snapshot == null || subscription == null || jmxMonitoringService == null) {
            failJmxMonitoring("Select a JMX notification subscription to stop.");
            return;
        }
        long generation = nextJmxMonitoringGeneration();
        jmxMonitoringLoading.set(true);
        clearJmxMonitoringError();
        executor.execute(() -> {
            try {
                jmxMonitoringService.stopNotifications(snapshot.connection(), subscription.id());
                runOnFx(() -> {
                    if (!isCurrentJmxMonitoringGeneration(generation, snapshot)) {
                        return;
                    }
                    jmxMonitoringLoading.set(false);
                    clearJmxMonitoringError();
                });
            } catch (RuntimeException exception) {
                failJmxMonitoring(generation, snapshot, exception);
            }
        });
    }

    public void addTriggerRule() {
        LiveMetricDefinition metric = selectedTriggerMetric.get();
        if (metric == null) {
            failTrigger("Select a metric for the trigger.");
            return;
        }
        if (selectedTriggerActionType.get() == TriggerActionType.DIAGNOSTIC_COMMAND
                && (!diagnosticCommandsAvailable.get() || selectedTriggerCommand.get() == null)) {
            failTrigger("Select a Diagnostic Command for this trigger.");
            return;
        }

        double threshold;
        try {
            threshold = Double.parseDouble(Objects.requireNonNullElse(triggerThreshold.get(), "").trim());
        } catch (NumberFormatException exception) {
            failTrigger("Enter a numeric trigger threshold.");
            return;
        }

        TriggerOperator operator = Objects.requireNonNullElse(selectedTriggerOperator.get(),
                TriggerOperator.GREATER_THAN_OR_EQUAL);
        String name = Objects.requireNonNullElse(triggerName.get(), "").trim();
        if (name.isBlank()) {
            name = metric.label().isBlank() ? "Trigger " + (triggerRuleSequence + 1)
                    : metric.label() + " " + operator.symbol() + " " + threshold;
        }
        triggerRules.add(new TriggerRule("trigger-" + ++triggerRuleSequence, name, true, metric.kind(), operator,
                threshold, selectedTriggerAction()));
        clearTriggerError();
    }

    public void removeSelectedTriggerRule(TriggerRule rule) {
        triggerRules.remove(rule);
    }

    public void evaluateTriggersNow() {
        JvmSessionSnapshot snapshot = selectedSession.get();
        if (snapshot == null || !snapshot.connection().connected() || liveMetricService == null
                || triggerRules.isEmpty()) {
            failTrigger("Add a trigger rule for a connected JVM.");
            return;
        }

        List<TriggerRule> rules = List.copyOf(triggerRules);
        long generation = nextTriggerEvaluationGeneration();
        triggerLoading.set(true);
        clearTriggerError();
        executor.execute(() -> {
            try {
                List<LiveMetricSnapshot> samples = liveMetricService.snapshot(snapshot.connection());
                List<TriggerEvent> events = evaluateTriggerRules(snapshot, samples, rules);
                runOnFx(() -> {
                    if (!isCurrentTriggerEvaluation(generation, snapshot)) {
                        return;
                    }
                    triggerEvents.addAll(events);
                    triggerLoading.set(false);
                    clearTriggerError();
                });
            } catch (RuntimeException exception) {
                failTrigger(generation, snapshot, exception);
            }
        });
    }

    @Override
    public void close() {
        discardSessionStartedRecordings();
        executor.close();
    }

    public static boolean canConnectJvm(JvmConnection selected) {
        return selected != null && !selected.connected()
                && ((selected.source() == JvmConnectionSource.LOCAL && selected.attachable())
                        || ((selected.source() == JvmConnectionSource.SAVED
                                || selected.source() == JvmConnectionSource.JDP)
                                && !selected.connectionUrl().isBlank()));
    }

    private void connectManual(String url) {
        beginWork();
        executor.execute(() -> {
            try {
                JvmConnection connected = connectionService.connect(url);
                runOnFx(() -> {
                    connections.add(connected);
                    selectedConnection.set(connected);
                    manualConnectionUrl.set("");
                    statusMessage.set("Connected to " + url + ".");
                    clearError();
                    finishWork();
                });
            } catch (RuntimeException exception) {
                fail(exception);
            }
        });
    }

    private void connectLocal(JvmConnection selected) {
        beginWork();
        executor.execute(() -> {
            try {
                JvmConnection connected = connectionService.connectLocal(selected);
                runOnFx(() -> replaceOrAdd(connected, "Connected to " + selected.displayName() + "."));
            } catch (RuntimeException exception) {
                fail(exception);
            }
        });
    }

    private void connectRemote(JvmConnection selected) {
        beginWork();
        executor.execute(() -> {
            try {
                JvmConnection liveConnection = connectionService.connect(selected.connectionUrl());
                JvmConnection connected = selected.asConnected(selected.connectionUrl());
                if (selected.source() == JvmConnectionSource.SAVED && savedTargetRepository != null) {
                    savedTargetRepository.markConnected(selected.id(), Instant.now());
                }
                runOnFx(() -> {
                    liveConnectionsByStableKey.put(stableKey(connected), liveConnection);
                    replaceOrAdd(connected, "Connected to " + selected.displayName() + ".");
                });
            } catch (RuntimeException exception) {
                fail(exception);
            }
        });
    }

    private void mergeDiscovered(List<JvmConnection> discovered) {
        JvmConnection selectedBefore = selectedConnection.get();
        String selectedKey = stableKey(selectedBefore);
        Set<String> discoveredKeys = discovered.stream()
                .map(JvmBrowserViewModel::stableKey)
                .collect(Collectors.toSet());
        List<JvmConnection> merged = new ArrayList<>();

        for (JvmConnection existing : connections) {
            if (existing.connected() || existing.source() == JvmConnectionSource.MANUAL) {
                merged.add(existing);
            } else if (existing.source() == JvmConnectionSource.JDP) {
                merged.add(existing);
            } else if ((existing.source() == JvmConnectionSource.LOCAL
                    || existing.source() == JvmConnectionSource.SAVED)
                    && !discoveredKeys.contains(stableKey(existing))) {
                selectedKey = clearSelectedIfRemoved(selectedKey, existing);
            }
        }

        Set<String> protectedKeys = merged.stream().map(JvmBrowserViewModel::stableKey).collect(Collectors.toSet());
        for (JvmConnection next : discovered) {
            String nextKey = stableKey(next);
            if (!protectedKeys.contains(nextKey)) {
                merged.add(next);
                protectedKeys.add(nextKey);
            }
        }

        connections.setAll(merged);
        String lookupKey = selectedKey;
        if (lookupKey != null) {
            selectedConnection.set(connections.stream()
                    .filter(connection -> lookupKey.equals(stableKey(connection)))
                    .findFirst()
                    .orElse(connections.isEmpty() ? null : connections.getFirst()));
        } else {
            selectedConnection.set(connections.isEmpty() ? null : connections.getFirst());
        }
    }

    private void mergeJdp(List<JvmConnection> discovered) {
        JvmConnection selectedBefore = selectedConnection.get();
        String selectedKey = stableKey(selectedBefore);
        Set<String> discoveredKeys = discovered.stream()
                .map(JvmBrowserViewModel::stableKey)
                .collect(Collectors.toSet());
        List<JvmConnection> merged = new ArrayList<>();

        for (JvmConnection existing : connections) {
            if (existing.connected() || existing.source() != JvmConnectionSource.JDP) {
                merged.add(existing);
            } else if (!discoveredKeys.contains(stableKey(existing))) {
                selectedKey = clearSelectedIfRemoved(selectedKey, existing);
            }
        }

        Set<String> protectedKeys = merged.stream().map(JvmBrowserViewModel::stableKey).collect(Collectors.toSet());
        for (JvmConnection next : discovered) {
            String nextKey = stableKey(next);
            if (!protectedKeys.contains(nextKey)) {
                merged.add(next);
                protectedKeys.add(nextKey);
            }
        }

        connections.setAll(merged);
        restoreSelection(selectedKey);
    }

    private void replaceSavedCandidate(JvmConnection connection) {
        int index = indexOfStable(connection);
        if (index >= 0) {
            connections.set(index, connection);
        } else {
            connections.add(connection);
        }
        selectedConnection.set(connection);
    }

    private static String clearSelectedIfRemoved(String selectedKey, JvmConnection removed) {
        return Objects.equals(selectedKey, stableKey(removed)) ? null : selectedKey;
    }

    private void replaceOrAdd(JvmConnection connection, String status) {
        int index = indexOfStable(connection);
        if (index >= 0) {
            connections.set(index, connection);
        } else {
            connections.add(connection);
        }
        selectedConnection.set(connection);
        statusMessage.set(status);
        clearError();
        finishWork();
    }

    private int indexOfStable(JvmConnection target) {
        String targetKey = stableKey(target);
        for (int i = 0; i < connections.size(); i++) {
            if (Objects.equals(stableKey(connections.get(i)), targetKey)) {
                return i;
            }
        }
        return -1;
    }

    private static String stableKey(JvmConnection connection) {
        if (connection == null) {
            return null;
        }
        if (connection.source() == JvmConnectionSource.LOCAL && !connection.pid().isBlank()) {
            return "local:" + connection.pid();
        }
        if (connection.source() == JvmConnectionSource.SAVED) {
            return "saved:" + connection.id();
        }
        if (connection.source() == JvmConnectionSource.JDP) {
            String id = connection.id().isBlank() ? connection.connectionUrl() : connection.id();
            return "jdp:" + id;
        }
        return "manual:" + connection.id();
    }

    private void restoreSelection(String selectedKey) {
        if (selectedKey != null) {
            selectedConnection.set(connections.stream()
                    .filter(connection -> selectedKey.equals(stableKey(connection)))
                    .findFirst()
                    .orElse(connections.isEmpty() ? null : connections.getFirst()));
        } else {
            selectedConnection.set(connections.isEmpty() ? null : connections.getFirst());
        }
    }

    private static String jdpStatus(int count) {
        if (count == 0) {
            return "No JDP targets found.";
        }
        if (count == 1) {
            return "Found 1 JDP target.";
        }
        return "Found " + count + " JDP targets.";
    }

    private void beginWork() {
        pendingWorkCount++;
        loading.set(true);
        clearError();
    }

    private void finishWork() {
        if (pendingWorkCount > 0) {
            pendingWorkCount--;
        }
        loading.set(pendingWorkCount > 0);
    }

    private void clearError() {
        error.set(false);
        errorMessage.set("");
    }

    private void loadSessionForSelection(JvmConnection connection) {
        long generation = nextSessionLoadGeneration();
        selectedSession.set(null);
        clearRecordingControl();
        clearMBeanBrowser();
        clearDiagnosticCommands();
        clearJmcAgent();
        clearJmxMonitoring();
        clearOverview();
        clearTriggerSessionState();
        clearSessionError();
        if (connection == null || !connection.connected()) {
            sessionLoading.set(false);
            return;
        }
        sessionLoading.set(true);
        executor.execute(() -> {
            try {
                JvmSessionSnapshot snapshot = connectionService.sessionSnapshot(liveConnectionFor(connection));
                runOnFx(() -> {
                    if (!isCurrentSessionLoad(generation, connection)) {
                        return;
                    }
                    selectedSession.set(snapshot);
                    clearSessionError();
                    loadRecordingControl(snapshot);
                    loadMBeanBrowser(snapshot);
                    loadDiagnosticCommands(snapshot);
                    loadJmcAgent(snapshot);
                    loadJmxMonitoring(snapshot);
                    loadOverview(snapshot);
                    loadTriggerMetrics(snapshot);
                    sessionLoading.set(false);
                });
            } catch (RuntimeException exception) {
                logActionFailure("Unable to load JVM session for " + connection.displayName(), exception);
                runOnFx(() -> {
                    if (!isCurrentSessionLoad(generation, connection)) {
                        return;
                    }
                    selectedSession.set(null);
                    clearRecordingControl();
                    clearMBeanBrowser();
                    clearDiagnosticCommands();
                    clearJmcAgent();
                    clearJmxMonitoring();
                    clearOverview();
                    clearTriggerSessionState();
                    sessionError.set(true);
                    sessionErrorMessage.set(exception.getMessage() == null
                            ? exception.getClass().getSimpleName() : exception.getMessage());
                    sessionLoading.set(false);
                });
            }
        });
    }

    private void clearSessionError() {
        sessionError.set(false);
        sessionErrorMessage.set("");
    }

    private JvmConnection liveConnectionFor(JvmConnection connection) {
        if (connection == null) {
            return null;
        }
        return liveConnectionsByStableKey.getOrDefault(stableKey(connection), connection);
    }

    private void loadRecordingControl(JvmSessionSnapshot snapshot) {
        if (flightRecordingService == null
                || snapshot.statusOf(JvmCapability.FLIGHT_RECORDER) != JvmCapabilityStatus.AVAILABLE
                || !flightRecordingService.isRecordingControlAvailable(snapshot.connection())) {
            clearRecordingControl();
            return;
        }
        try {
            List<FlightRecordingInfo> recordings = flightRecordingService.recordings(snapshot.connection());
            flightRecordings.setAll(recordings);
            selectedFlightRecording.set(recordings.isEmpty() ? null : recordings.getFirst());
            recordingControlAvailable.set(true);
            clearRecordingError();
        } catch (RuntimeException exception) {
            logActionFailure("Unable to load Flight Recorder state for " + snapshot.connection().displayName(),
                    exception);
            clearRecordingControl();
            recordingError.set(true);
            recordingErrorMessage.set(exception.getMessage() == null
                    ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }

    private boolean canUseRecordingControl(JvmConnection connection) {
        return flightRecordingService != null && connection != null && connection.connected()
                && recordingControlAvailable.get();
    }

    private void loadMBeanBrowser(JvmSessionSnapshot snapshot) {
        if (!canUseMBeanBrowser(snapshot)) {
            clearMBeanBrowser();
            return;
        }
        long generation = nextMBeanRequestGeneration();
        mbeanBrowserAvailable.set(true);
        mbeanLoading.set(true);
        clearMBeanError();
        clearMBeanDetails();
        executor.execute(() -> {
            try {
                List<MBeanNode> tree = mBeanBrowserService.tree(snapshot.connection());
                runOnFx(() -> {
                    if (!isCurrentMBeanSessionRequest(generation, snapshot)) {
                        return;
                    }
                    mbeanTree.setAll(tree);
                    mbeanLoading.set(false);
                    clearMBeanError();
                });
            } catch (RuntimeException exception) {
                failMBean(generation, snapshot, null, null, exception);
            }
        });
    }

    private void loadSelectedMBeanDetails(MBeanNode node) {
        JvmSessionSnapshot snapshot = selectedSession.get();
        if (!canUseMBeanBrowser(snapshot) || node == null || node.domain()) {
            nextMBeanRequestGeneration();
            clearMBeanDetails();
            mbeanOperationResult.set("");
            mbeanLoading.set(false);
            clearMBeanError();
            return;
        }
        clearMBeanDetails();
        mbeanOperationResult.set("");
        long generation = nextMBeanRequestGeneration();
        mbeanLoading.set(true);
        clearMBeanError();
        executor.execute(() -> {
            try {
                List<MBeanAttributeInfo> attributes = mBeanBrowserService.attributes(snapshot.connection(),
                        node.objectName());
                List<MBeanOperationInfo> operations = mBeanBrowserService.operations(snapshot.connection(),
                        node.objectName());
                runOnFx(() -> {
                    if (!isCurrentMBeanRequest(generation, snapshot, node)) {
                        return;
                    }
                    mbeanAttributes.setAll(attributes);
                    mbeanOperations.setAll(operations);
                    selectedMBeanOperation.set(operations.isEmpty() ? null : operations.getFirst());
                    mbeanLoading.set(false);
                    clearMBeanError();
                });
            } catch (RuntimeException exception) {
                failMBean(generation, snapshot, node, null, exception);
            }
        });
    }

    private boolean canUseMBeanBrowser(JvmSessionSnapshot snapshot) {
        return mBeanBrowserService != null && snapshot != null
                && snapshot.statusOf(JvmCapability.MBEAN_SERVER) == JvmCapabilityStatus.AVAILABLE;
    }

    private void loadDiagnosticCommands(JvmSessionSnapshot snapshot) {
        if (!canLoadDiagnosticCommands(snapshot)) {
            clearDiagnosticCommands();
            return;
        }
        long generation = nextDiagnosticCommandRequestGeneration();
        diagnosticCommandsAvailable.set(true);
        diagnosticCommandLoading.set(true);
        clearDiagnosticCommandError();
        diagnosticCommands.clear();
        selectedDiagnosticCommand.set(null);
        executor.execute(() -> {
            try {
                List<DiagnosticCommandInfo> commands = diagnosticCommandService.commands(snapshot.connection());
                runOnFx(() -> {
                    if (!isCurrentDiagnosticCommandSessionRequest(generation, snapshot)) {
                        return;
                    }
                    diagnosticCommands.setAll(commands);
                    selectedDiagnosticCommand.set(commands.isEmpty() ? null : commands.getFirst());
                    diagnosticCommandLoading.set(false);
                    clearDiagnosticCommandError();
                });
            } catch (RuntimeException exception) {
                failDiagnosticCommand(generation, snapshot, null, exception);
            }
        });
    }

    private boolean canUseDiagnosticCommands(JvmSessionSnapshot snapshot) {
        return canLoadDiagnosticCommands(snapshot) && diagnosticCommandsAvailable.get();
    }

    private boolean canLoadDiagnosticCommands(JvmSessionSnapshot snapshot) {
        return diagnosticCommandService != null && snapshot != null
                && snapshot.statusOf(JvmCapability.DIAGNOSTIC_COMMANDS) == JvmCapabilityStatus.AVAILABLE;
    }

    private void loadJmcAgent(JvmSessionSnapshot snapshot) {
        if (!canLoadJmcAgent(snapshot)) {
            clearJmcAgent();
            return;
        }
        long generation = nextJmcAgentRequestGeneration();
        jmcAgentLoading.set(true);
        clearJmcAgentError();
        executor.execute(() -> {
            try {
                List<JmcAgentPreset> presets = jmcAgentService.presets();
                JmcAgentStatus status = jmcAgentService.status(snapshot.connection());
                runOnFx(() -> {
                    if (!isCurrentJmcAgentRequest(generation, snapshot)) {
                        return;
                    }
                    jmcAgentPresets.setAll(presets);
                    selectedJmcAgentPreset.set(presets.isEmpty() ? null : presets.getFirst());
                    applyJmcAgentStatus(status);
                    jmcAgentLoading.set(false);
                    clearJmcAgentError();
                });
            } catch (RuntimeException exception) {
                failJmcAgent(generation, snapshot, exception);
            }
        });
    }

    private boolean canUseJmcAgent(JvmSessionSnapshot snapshot) {
        return canLoadJmcAgent(snapshot) && jmcAgentAvailable.get();
    }

    private boolean canLoadJmcAgent(JvmSessionSnapshot snapshot) {
        return jmcAgentService != null && snapshot != null && snapshot.connection().connected();
    }

    private void applyJmcAgentStatus(JmcAgentStatus status) {
        jmcAgentAvailable.set(status.available());
        jmcAgentStatusMessage.set(status.message());
        jmcAgentConfiguration.set(status.available() ? status.eventProbeXml() : "");
        jmcAgentTransforms.setAll(status.transforms());
    }

    private void loadJmxMonitoring(JvmSessionSnapshot snapshot) {
        if (!canUseJmxMonitoring(snapshot)) {
            clearJmxMonitoring();
            return;
        }
        jmxMonitoringAvailable.set(true);
        clearJmxMonitoringError();
        if (jmxMonitoringRepository == null) {
            return;
        }
        List<JmxAttributeSubscription> attributeSubscriptions =
                jmxMonitoringRepository.findAttributeSubscriptions(snapshot.connection().id());
        jmxAttributeSubscriptions.setAll(attributeSubscriptions);
        selectedJmxAttributeSubscription.set(attributeSubscriptions.isEmpty() ? null : attributeSubscriptions.getFirst());
        List<JmxNotificationSubscription> notificationSubscriptions =
                jmxMonitoringRepository.findNotificationSubscriptions(snapshot.connection().id());
        jmxNotificationSubscriptions.setAll(notificationSubscriptions);
        selectedJmxNotificationSubscription.set(notificationSubscriptions.isEmpty()
                ? null : notificationSubscriptions.getFirst());
        updateOverviewPersistenceSummary();
    }

    private boolean canUseJmxMonitoring(JvmSessionSnapshot snapshot) {
        return jmxMonitoringService != null && snapshot != null
                && snapshot.statusOf(JvmCapability.MBEAN_SERVER) == JvmCapabilityStatus.AVAILABLE;
    }

    private void loadSamplesForSelection(JmxAttributeSubscription subscription) {
        if (jmxMonitoringRepository == null || subscription == null) {
            jmxSubscriptionSamples.clear();
            return;
        }
        jmxSubscriptionSamples.setAll(jmxMonitoringRepository.findSamples(subscription.id()));
    }

    private void loadNotificationEventsForSelection(JmxNotificationSubscription subscription) {
        if (jmxMonitoringRepository == null || subscription == null) {
            jmxNotificationEvents.clear();
            return;
        }
        jmxNotificationEvents.setAll(jmxMonitoringRepository.findNotificationEvents(subscription.id()));
    }

    private void loadTriggerMetrics(JvmSessionSnapshot snapshot) {
        if (liveMetricService == null || snapshot == null) {
            clearTriggerSessionState();
            return;
        }
        long generation = nextTriggerEvaluationGeneration();
        triggerLoading.set(true);
        clearTriggerError();
        liveMetricDefinitions.clear();
        selectedTriggerMetric.set(null);
        executor.execute(() -> {
            try {
                List<LiveMetricDefinition> definitions = liveMetricService.definitions(snapshot.connection());
                runOnFx(() -> {
                    if (!isCurrentTriggerEvaluation(generation, snapshot)) {
                        return;
                    }
                    liveMetricDefinitions.setAll(definitions);
                    selectedTriggerMetric.set(definitions.isEmpty() ? null : definitions.getFirst());
                    triggerLoading.set(false);
                    clearTriggerError();
                });
            } catch (RuntimeException exception) {
                failTrigger(generation, snapshot, exception);
            }
        });
    }

    public void refreshOverview() {
        loadOverview(selectedSession.get());
    }

    private void loadOverview(JvmSessionSnapshot snapshot) {
        if (liveMetricService == null || snapshot == null || !snapshot.connection().connected()) {
            clearOverview();
            return;
        }
        long generation = nextOverviewRequestGeneration();
        overviewLoading.set(true);
        clearOverviewError();
        executor.execute(() -> {
            try {
                List<LiveMetricDefinition> definitions = liveMetricService.definitions(snapshot.connection());
                List<LiveMetricSnapshot> samples = liveMetricService.snapshot(snapshot.connection());
                List<LiveJvmOverviewMetric> rows = overviewRows(definitions, samples);
                runOnFx(() -> {
                    if (!isCurrentOverviewRequest(generation, snapshot)) {
                        return;
                    }
                    appendOverviewRows(rows);
                    updateOverviewPersistenceSummary();
                    overviewLoading.set(false);
                    clearOverviewError();
                });
            } catch (RuntimeException exception) {
                failOverview(generation, snapshot, exception);
            }
        });
    }

    private List<LiveJvmOverviewMetric> overviewRows(
            List<LiveMetricDefinition> definitions, List<LiveMetricSnapshot> samples) {
        Map<LiveMetricKind, LiveMetricDefinition> byKind = definitions.stream()
                .collect(Collectors.toMap(LiveMetricDefinition::kind, definition -> definition,
                        (first, second) -> first));
        return samples.stream()
                .filter(sample -> byKind.containsKey(sample.kind()))
                .map(sample -> {
                    LiveMetricDefinition definition = byKind.get(sample.kind());
                    return new LiveJvmOverviewMetric(overviewGroup(sample.kind()), sample.kind(), definition.label(),
                            sample.value(), overviewDisplayValue(sample), sample.unit(), sample.observedAt(),
                            overviewSampleSequence + 1);
                })
                .toList();
    }

    private static String overviewGroup(LiveMetricKind kind) {
        return switch (kind) {
            case PROCESS_CPU_LOAD_PERCENT, SYSTEM_CPU_LOAD_PERCENT, AVAILABLE_PROCESSORS, SYSTEM_LOAD_AVERAGE ->
                    "Processor";
            case HEAP_USED_PERCENT, HEAP_USED_BYTES, HEAP_COMMITTED_BYTES, HEAP_MAX_BYTES,
                    NON_HEAP_USED_BYTES, NON_HEAP_COMMITTED_BYTES -> "Memory";
            case THREAD_COUNT, PEAK_THREAD_COUNT, DAEMON_THREAD_COUNT, LOADED_CLASS_COUNT,
                    TOTAL_LOADED_CLASS_COUNT, UNLOADED_CLASS_COUNT -> "Dashboard";
        };
    }

    private static String overviewDisplayValue(LiveMetricSnapshot sample) {
        if ("%".equals(sample.unit())) {
            return String.format(java.util.Locale.US, "%.1f%%", sample.value());
        }
        if ("bytes".equals(sample.unit())) {
            return com.youngledo.jmcfx.ui.util.DisplayFormats.formatFileSize(Math.round(sample.value()));
        }
        if (Double.isFinite(sample.value())) {
            return java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(Math.round(sample.value()));
        }
        return "";
    }

    private void appendOverviewRows(List<LiveJvmOverviewMetric> rows) {
        if (rows.isEmpty()) {
            return;
        }
        overviewSampleSequence++;
        overviewMetrics.addAll(rows.stream()
                .map(row -> new LiveJvmOverviewMetric(row.group(), row.kind(), row.label(), row.value(),
                        row.displayValue(), row.unit(), row.observedAt(), overviewSampleSequence))
                .toList());
        Map<LiveMetricKind, Long> counts = overviewMetrics.stream()
                .collect(Collectors.groupingBy(LiveJvmOverviewMetric::kind, Collectors.counting()));
        overviewMetrics.removeIf(metric ->
                counts.getOrDefault(metric.kind(), 0L) > OVERVIEW_HISTORY_LIMIT
                        && metric.sequence() <= overviewSampleSequence - OVERVIEW_HISTORY_LIMIT);
    }

    private TriggerAction selectedTriggerAction() {
        TriggerActionType actionType = Objects.requireNonNullElse(selectedTriggerActionType.get(),
                TriggerActionType.NOTIFY);
        if (actionType != TriggerActionType.DIAGNOSTIC_COMMAND) {
            return TriggerAction.notifyOnly();
        }
        DiagnosticCommandInfo command = selectedTriggerCommand.get();
        return TriggerAction.diagnosticCommand(command == null ? "" : command.name(), List.of());
    }

    private List<TriggerEvent> evaluateTriggerRules(JvmSessionSnapshot snapshot, List<LiveMetricSnapshot> samples,
            List<TriggerRule> rules) {
        Map<LiveMetricKind, LiveMetricSnapshot> byMetric = samples.stream()
                .collect(Collectors.toMap(LiveMetricSnapshot::kind, sample -> sample, (first, second) -> second));
        List<TriggerEvent> events = new ArrayList<>();
        for (TriggerRule rule : rules) {
            LiveMetricSnapshot sample = byMetric.get(rule.metric());
            if (rule.matches(sample)) {
                events.add(triggerEvent(snapshot, rule, sample));
            }
        }
        return events;
    }

    private TriggerEvent triggerEvent(JvmSessionSnapshot snapshot, TriggerRule rule, LiveMetricSnapshot sample) {
        return new TriggerEvent(rule.id(), rule.name(), rule.metric(), sample.value(), sample.unit(),
                Objects.requireNonNullElse(sample.observedAt(), Instant.now()),
                triggerMessage(snapshot, rule, sample));
    }

    private String triggerMessage(JvmSessionSnapshot snapshot, TriggerRule rule, LiveMetricSnapshot sample) {
        String notification = "Triggered at " + sample.value() + unitSuffix(sample.unit());
        if (rule.action().type() != TriggerActionType.DIAGNOSTIC_COMMAND) {
            return notification;
        }
        if (diagnosticCommandService == null || rule.action().commandName().isBlank()) {
            return notification + ". Diagnostic command is not available.";
        }
        try {
            DiagnosticCommandResult result = diagnosticCommandService.execute(new DiagnosticCommandRequest(
                    snapshot.connection(), rule.action().commandName(), rule.action().arguments()));
            String output = displayDiagnosticCommandResult(result);
            return output.isBlank() ? notification : notification + ". " + output;
        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null ? exception.getClass().getSimpleName()
                    : exception.getMessage();
            return notification + ". " + message;
        }
    }

    private static String unitSuffix(String unit) {
        return unit == null || unit.isBlank() ? "" : " " + unit;
    }

    private void discardSessionStartedRecordings() {
        if (flightRecordingService == null || sessionStartedRecordings.isEmpty()) {
            return;
        }
        Map<Long, JvmConnection> started = Map.copyOf(sessionStartedRecordings);
        sessionStartedRecordings.clear();
        for (Map.Entry<Long, JvmConnection> entry : started.entrySet()) {
            try {
                if (isStillRunning(entry.getValue(), entry.getKey())) {
                    flightRecordingService.stopAndDiscardRecording(entry.getValue(), entry.getKey());
                }
            } catch (RuntimeException exception) {
                LOGGER.atWarn()
                        .withThrowable(exception)
                        .log("Unable to discard Flight Recorder recording {} on {}",
                                entry.getKey(), entry.getValue().displayName());
            }
        }
    }

    private boolean isStillRunning(JvmConnection connection, long recordingId) {
        return flightRecordingService.recordings(connection).stream()
                .anyMatch(recording -> recording.id() == recordingId
                        && recording.state() == FlightRecordingState.RUNNING);
    }

    private static String recordingName(JvmConnection connection) {
        String id = connection.pid().isBlank() ? connection.id() : connection.pid();
        String safeId = id.replaceAll("[^A-Za-z0-9]+", "");
        if (safeId.isBlank()) {
            safeId = "jvm";
        }
        return "jmcfx-" + safeId + "-" + LocalDateTime.now(ZoneOffset.UTC).format(RECORDING_NAME_TIMESTAMP);
    }

    private void clearRecordingControl() {
        flightRecordings.clear();
        selectedFlightRecording.set(null);
        recordingControlAvailable.set(false);
        recordingLoading.set(false);
        recordingStatusMessage.set("");
        clearRecordingError();
    }

    private void clearRecordingError() {
        recordingError.set(false);
        recordingErrorMessage.set("");
    }

    private void clearMBeanBrowser() {
        nextMBeanRequestGeneration();
        mbeanTree.clear();
        selectedMBean.set(null);
        mbeanBrowserAvailable.set(false);
        mbeanLoading.set(false);
        mbeanOperationArguments.set("");
        mbeanOperationResult.set("");
        clearMBeanDetails();
        clearMBeanError();
    }

    private void clearMBeanDetails() {
        mbeanAttributes.clear();
        mbeanOperations.clear();
        selectedMBeanOperation.set(null);
    }

    private void clearMBeanError() {
        mbeanError.set(false);
        mbeanErrorMessage.set("");
    }

    private void clearMBeanOperationResult() {
        nextMBeanRequestGeneration();
        mbeanOperationResult.set("");
        mbeanLoading.set(false);
        clearMBeanError();
    }

    private void clearDiagnosticCommands() {
        nextDiagnosticCommandRequestGeneration();
        diagnosticCommands.clear();
        selectedDiagnosticCommand.set(null);
        diagnosticCommandsAvailable.set(false);
        diagnosticCommandLoading.set(false);
        diagnosticCommandArguments.set("");
        diagnosticCommandOutput.set("");
        clearDiagnosticCommandError();
    }

    private void clearDiagnosticCommandError() {
        diagnosticCommandError.set(false);
        diagnosticCommandErrorMessage.set("");
    }

    private void clearDiagnosticCommandResult() {
        nextDiagnosticCommandRequestGeneration();
        diagnosticCommandOutput.set("");
        diagnosticCommandLoading.set(false);
        clearDiagnosticCommandError();
    }

    private void clearJmcAgent() {
        nextJmcAgentRequestGeneration();
        jmcAgentPresets.clear();
        selectedJmcAgentPreset.set(null);
        jmcAgentTransforms.clear();
        jmcAgentConfiguration.set("");
        jmcAgentStatusMessage.set("");
        jmcAgentAvailable.set(false);
        jmcAgentLoading.set(false);
        clearJmcAgentError();
    }

    private void clearJmcAgentError() {
        jmcAgentError.set(false);
        jmcAgentErrorMessage.set("");
    }

    private void clearJmxMonitoring() {
        nextJmxMonitoringGeneration();
        jmxAttributeSubscriptions.clear();
        jmxSubscriptionSamples.clear();
        jmxNotificationSubscriptions.clear();
        jmxNotificationEvents.clear();
        selectedJmxAttributeSubscription.set(null);
        selectedJmxNotificationSubscription.set(null);
        jmxMonitoringAvailable.set(false);
        jmxMonitoringLoading.set(false);
        clearJmxMonitoringError();
        updateOverviewPersistenceSummary();
    }

    private void clearOverview() {
        nextOverviewRequestGeneration();
        overviewMetrics.clear();
        overviewSampleSequence = 0;
        overviewPersistence.set(LiveJvmPersistenceOverview.notConfigured());
        overviewLoading.set(false);
        clearOverviewError();
    }

    private void appendBoundedSample(JmxAttributeSubscription subscription, JmxSubscriptionSample sample) {
        if (!Objects.equals(subscription.id(), sample.subscriptionId())) {
            return;
        }
        if (selectedJmxAttributeSubscription.get() == subscription) {
            jmxSubscriptionSamples.add(sample);
            trimObservableToNewest(jmxSubscriptionSamples, subscription.maxSamples());
        }
    }

    private void appendNotificationEvent(JmxNotificationSubscription subscription, JmxNotificationEvent event) {
        if (!Objects.equals(subscription.id(), event.subscriptionId())) {
            return;
        }
        if (subscription.persisted() && jmxMonitoringRepository != null) {
            jmxMonitoringRepository.appendNotificationEvent(event);
        }
        if (selectedJmxNotificationSubscription.get() == subscription) {
            jmxNotificationEvents.add(event);
            trimObservableToNewest(jmxNotificationEvents, subscription.maxEvents());
        }
    }

    private static <T> void trimObservableToNewest(ObservableList<T> rows, int maxSize) {
        while (rows.size() > maxSize) {
            rows.removeFirst();
        }
    }

    private void clearJmxMonitoringError() {
        jmxMonitoringError.set(false);
        jmxMonitoringErrorMessage.set("");
    }

    private void failJmxMonitoring(String message) {
        jmxMonitoringError.set(true);
        jmxMonitoringErrorMessage.set(message);
        jmxMonitoringLoading.set(false);
    }

    private void clearOverviewError() {
        overviewError.set(false);
        overviewErrorMessage.set("");
    }

    private void failOverview(long generation, JvmSessionSnapshot snapshot, RuntimeException exception) {
        logActionFailure("Unable to load JVM overview for " + snapshot.connection().displayName(), exception);
        runOnFx(() -> {
            if (!isCurrentOverviewRequest(generation, snapshot)) {
                return;
            }
            overviewError.set(true);
            overviewErrorMessage.set(exception.getMessage() == null ? exception.getClass().getSimpleName()
                    : exception.getMessage());
            overviewLoading.set(false);
        });
    }

    private void clearTriggerSessionState() {
        nextTriggerEvaluationGeneration();
        liveMetricDefinitions.clear();
        selectedTriggerMetric.set(null);
        selectedTriggerOperator.set(TriggerOperator.GREATER_THAN_OR_EQUAL);
        selectedTriggerActionType.set(TriggerActionType.NOTIFY);
        selectedTriggerCommand.set(null);
        triggerRules.clear();
        triggerEvents.clear();
        triggerName.set("");
        triggerThreshold.set("");
        triggerLoading.set(false);
        clearTriggerError();
    }

    private void clearTriggerError() {
        triggerError.set(false);
        triggerErrorMessage.set("");
    }

    private void failTrigger(String message) {
        triggerError.set(true);
        triggerErrorMessage.set(message);
        triggerLoading.set(false);
    }

    private void failRecording(RuntimeException exception) {
        logActionFailure("Flight Recorder action failed", exception);
        runOnFx(() -> {
            recordingError.set(true);
            recordingErrorMessage.set(exception.getMessage() == null ? exception.getClass().getSimpleName()
                    : exception.getMessage());
            recordingLoading.set(false);
        });
    }

    private void failMBean(long generation, JvmSessionSnapshot snapshot, MBeanNode node,
            MBeanOperationInfo operation, RuntimeException exception) {
        logActionFailure("MBean browser action failed", exception);
        runOnFx(() -> {
            if (node == null) {
                if (!isCurrentMBeanSessionRequest(generation, snapshot)) {
                    return;
                }
            } else if (operation == null && !isCurrentMBeanRequest(generation, snapshot, node)) {
                return;
            } else if (operation != null && !isCurrentMBeanRequest(generation, snapshot, node, operation)) {
                return;
            }
            mbeanError.set(true);
            mbeanErrorMessage.set(exception.getMessage() == null ? exception.getClass().getSimpleName()
                    : exception.getMessage());
            mbeanLoading.set(false);
        });
    }

    private void failDiagnosticCommand(long generation, JvmSessionSnapshot snapshot,
            DiagnosticCommandInfo command, RuntimeException exception) {
        logActionFailure("Diagnostic Command action failed", exception);
        runOnFx(() -> {
            if (command == null) {
                if (!isCurrentDiagnosticCommandSessionRequest(generation, snapshot)) {
                    return;
                }
            } else if (!isCurrentDiagnosticCommandRequest(generation, snapshot, command)) {
                return;
            }
            diagnosticCommandError.set(true);
            diagnosticCommandErrorMessage.set(exception.getMessage() == null ? exception.getClass().getSimpleName()
                    : exception.getMessage());
            diagnosticCommandLoading.set(false);
        });
    }

    private void failJmcAgent(long generation, JvmSessionSnapshot snapshot, RuntimeException exception) {
        logActionFailure("JMC Agent action failed", exception);
        runOnFx(() -> {
            if (!isCurrentJmcAgentRequest(generation, snapshot)) {
                return;
            }
            jmcAgentAvailable.set(false);
            jmcAgentError.set(true);
            jmcAgentErrorMessage.set(exception.getMessage() == null ? exception.getClass().getSimpleName()
                    : exception.getMessage());
            jmcAgentLoading.set(false);
        });
    }

    private void failJmxMonitoring(long generation, JvmSessionSnapshot snapshot, RuntimeException exception) {
        logActionFailure("JMX monitoring action failed", exception);
        runOnFx(() -> {
            if (!isCurrentJmxMonitoringGeneration(generation, snapshot)) {
                return;
            }
            jmxMonitoringError.set(true);
            jmxMonitoringErrorMessage.set(exception.getMessage() == null ? exception.getClass().getSimpleName()
                    : exception.getMessage());
            jmxMonitoringLoading.set(false);
        });
    }

    private void failTrigger(long generation, JvmSessionSnapshot snapshot, RuntimeException exception) {
        logActionFailure("Trigger action failed", exception);
        runOnFx(() -> {
            if (!isCurrentTriggerEvaluation(generation, snapshot)) {
                return;
            }
            triggerError.set(true);
            triggerErrorMessage.set(exception.getMessage() == null ? exception.getClass().getSimpleName()
                    : exception.getMessage());
            triggerLoading.set(false);
        });
    }

    private void fail(RuntimeException exception) {
        logActionFailure("JVM browser action failed", exception);
        runOnFx(() -> {
            finishWork();
            error.set(true);
            errorMessage.set(exception.getMessage() == null ? exception.getClass().getSimpleName()
                    : exception.getMessage());
        });
    }

    private static void logActionFailure(String action, RuntimeException exception) {
        if (exception instanceof JmcFxException) {
            LOGGER.warn("{}: {}", action, displayMessage(exception));
            LOGGER.debug(action, exception);
            return;
        }
        LOGGER.error(action, exception);
    }

    private static String displayMessage(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private void runOnFx(Runnable runnable) {
        fxRunner.accept(runnable);
    }

    private long nextSessionLoadGeneration() {
        return ++sessionLoadGeneration;
    }

    private boolean isCurrentSessionLoad(long generation, JvmConnection connection) {
        return sessionLoadGeneration == generation && selectedConnection.get() == connection
                && connection != null && connection.connected();
    }

    private long nextMBeanRequestGeneration() {
        return ++mbeanRequestGeneration;
    }

    private boolean isCurrentMBeanSessionRequest(long generation, JvmSessionSnapshot snapshot) {
        return mbeanRequestGeneration == generation && selectedSession.get() == snapshot && mbeanBrowserAvailable.get();
    }

    private boolean isCurrentMBeanRequest(long generation, JvmSessionSnapshot snapshot, MBeanNode node) {
        return isCurrentMBeanSessionRequest(generation, snapshot) && selectedMBean.get() == node;
    }

    private boolean isCurrentMBeanRequest(long generation, JvmSessionSnapshot snapshot, MBeanNode node,
            MBeanOperationInfo operation) {
        return isCurrentMBeanRequest(generation, snapshot, node) && selectedMBeanOperation.get() == operation;
    }

    private long nextDiagnosticCommandRequestGeneration() {
        return ++diagnosticCommandRequestGeneration;
    }

    private boolean isCurrentDiagnosticCommandSessionRequest(long generation, JvmSessionSnapshot snapshot) {
        return diagnosticCommandRequestGeneration == generation && selectedSession.get() == snapshot
                && diagnosticCommandsAvailable.get();
    }

    private boolean isCurrentDiagnosticCommandRequest(long generation, JvmSessionSnapshot snapshot,
            DiagnosticCommandInfo command) {
        return isCurrentDiagnosticCommandSessionRequest(generation, snapshot)
                && selectedDiagnosticCommand.get() == command;
    }

    private long nextJmcAgentRequestGeneration() {
        return ++jmcAgentRequestGeneration;
    }

    private boolean isCurrentJmcAgentRequest(long generation, JvmSessionSnapshot snapshot) {
        return jmcAgentRequestGeneration == generation && selectedSession.get() == snapshot;
    }

    private long nextJmxMonitoringGeneration() {
        return ++jmxMonitoringGeneration;
    }

    private long nextOverviewRequestGeneration() {
        return ++overviewRequestGeneration;
    }

    private boolean isCurrentJmxMonitoringGeneration(long generation, JvmSessionSnapshot snapshot) {
        return jmxMonitoringGeneration == generation && selectedSession.get() == snapshot
                && jmxMonitoringAvailable.get();
    }

    private boolean isCurrentOverviewRequest(long generation, JvmSessionSnapshot snapshot) {
        return overviewRequestGeneration == generation && selectedSession.get() == snapshot;
    }

    private void updateOverviewPersistenceSummary() {
        if (jmxMonitoringRepository == null) {
            overviewPersistence.set(LiveJvmPersistenceOverview.notConfigured());
            return;
        }
        int attributeCount = jmxAttributeSubscriptions.size();
        int persistedAttributes = (int) jmxAttributeSubscriptions.stream()
                .filter(JmxAttributeSubscription::persisted)
                .count();
        int notificationCount = jmxNotificationSubscriptions.size();
        int persistedNotifications = (int) jmxNotificationSubscriptions.stream()
                .filter(JmxNotificationSubscription::persisted)
                .count();
        int maxSamples = jmxAttributeSubscriptions.stream()
                .mapToInt(JmxAttributeSubscription::maxSamples)
                .max()
                .orElse(0);
        int maxEvents = jmxNotificationSubscriptions.stream()
                .mapToInt(JmxNotificationSubscription::maxEvents)
                .max()
                .orElse(0);
        overviewPersistence.set(new LiveJvmPersistenceOverview(true, attributeCount, persistedAttributes,
                notificationCount, persistedNotifications, maxSamples, maxEvents));
    }

    private long nextTriggerEvaluationGeneration() {
        return ++triggerEvaluationGeneration;
    }

    private boolean isCurrentTriggerEvaluation(long generation, JvmSessionSnapshot snapshot) {
        return triggerEvaluationGeneration == generation && selectedSession.get() == snapshot;
    }

    private static List<String> parseMBeanArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return List.of();
        }
        return List.of(arguments.split(",", -1)).stream()
                .map(String::trim)
                .toList();
    }

    private static String displayMBeanResult(MBeanOperationResult result) {
        if (!result.error().isBlank()) {
            return result.error();
        }
        return result.value();
    }

    private static List<String> parseDiagnosticCommandArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return List.of();
        }
        return List.of(arguments.trim().split("\\s+"));
    }

    private static String displayDiagnosticCommandResult(DiagnosticCommandResult result) {
        if (!result.error().isBlank()) {
            return result.error();
        }
        return result.output();
    }
}
