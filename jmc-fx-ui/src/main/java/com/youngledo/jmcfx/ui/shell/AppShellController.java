package com.youngledo.jmcfx.ui.shell;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;

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
import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.LockGrouping;
import com.youngledo.jmcfx.domain.model.LockHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.RuleResult;
import com.youngledo.jmcfx.domain.model.Severity;
import com.youngledo.jmcfx.domain.model.SocketIOEvent;
import com.youngledo.jmcfx.domain.model.SocketIOGrouping;
import com.youngledo.jmcfx.domain.model.SocketIOHistogram;
import com.youngledo.jmcfx.domain.model.StackTreeNode;
import com.youngledo.jmcfx.domain.model.ThreadSummary;
import com.youngledo.jmcfx.domain.service.EventQueryService;
import com.youngledo.jmcfx.domain.service.ExceptionService;
import com.youngledo.jmcfx.domain.service.FileIOService;
import com.youngledo.jmcfx.domain.service.LockService;
import com.youngledo.jmcfx.domain.service.ProfilingService;
import com.youngledo.jmcfx.domain.service.RecordingRepository;
import com.youngledo.jmcfx.domain.service.RuleAnalysisService;
import com.youngledo.jmcfx.domain.service.SocketIOService;
import com.youngledo.jmcfx.domain.service.ThreadService;
import com.youngledo.jmcfx.ui.analysis.AnalysisSeverityCell;
import com.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import com.youngledo.jmcfx.ui.events.VirtualThreadEventBrowserExecutor;
import com.youngledo.jmcfx.ui.exceptions.ExceptionViewModel;
import com.youngledo.jmcfx.ui.fileio.FileIOViewModel;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.i18n.LanguageMode;
import com.youngledo.jmcfx.ui.locks.LockViewModel;
import com.youngledo.jmcfx.ui.util.DisplayFormats;
import com.youngledo.jmcfx.ui.util.HtmlToTextFlow;
import com.youngledo.jmcfx.ui.overview.OverviewViewModel;
import com.youngledo.jmcfx.ui.profiling.ProfilingViewModel;
import com.youngledo.jmcfx.ui.rules.RuleResultsViewModel;
import com.youngledo.jmcfx.ui.socketio.SocketIOViewModel;
import com.youngledo.jmcfx.ui.threads.ThreadViewModel;

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
            SocketIOService socketIOService, LockService lockService, I18n i18n) {
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
        viewModel.openRecording(recording, overview, events, analysis, profiling, exceptions, threads,
                fileio, socketio, locks);
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
}
