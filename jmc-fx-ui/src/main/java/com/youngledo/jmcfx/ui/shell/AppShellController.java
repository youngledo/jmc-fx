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
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.EventQueryService;
import com.youngledo.jmcfx.domain.service.RecordingRepository;
import com.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import com.youngledo.jmcfx.ui.events.VirtualThreadEventBrowserExecutor;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.i18n.LanguageMode;
import com.youngledo.jmcfx.ui.util.DisplayFormats;
import com.youngledo.jmcfx.ui.overview.OverviewViewModel;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
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
    @FXML private Label analysisUnavailableLabel;
    @FXML private Label jvmsTitleLabel;
    @FXML private Label jvmsUnavailableLabel;
    @FXML private Label settingsTitleLabel;
    @FXML private Label settingsLanguageLabel;
    @FXML private ToggleGroup languageToggleGroup;
    @FXML private RadioButton languageFollowSystemRadio;
    @FXML private RadioButton languageEnglishRadio;
    @FXML private RadioButton languageChineseRadio;

    public AppShellController(AppShellViewModel viewModel, RecordingRepository recordingRepository,
            EventQueryService eventQueryService, I18n i18n) {
        this.viewModel = viewModel;
        this.recordingRepository = recordingRepository;
        this.eventQueryService = eventQueryService;
        this.i18n = i18n;
    }

    I18n i18n() {
        return i18n;
    }

    @FXML
    void initialize() {
        viewModel.showStatus(i18n.get("status.ready"));
        viewModel.currentRecordingNameProperty().set(i18n.get("status.noRecording"));
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
        settingsPane.visibleProperty().bind(viewModel.selectedSectionProperty().isEqualTo("settings"));
        settingsPane.managedProperty().bind(settingsPane.visibleProperty());
        bindOverview(null);
        bindEvents();
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
        analysisUnavailableLabel.textProperty().bind(i18n.text("analysis.unavailable"));
        jvmsTitleLabel.textProperty().bind(i18n.text("jvms.title"));
        jvmsUnavailableLabel.textProperty().bind(i18n.text("jvms.unavailable"));
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
                new VirtualThreadEventBrowserExecutor());
        viewModel.openRecording(recording, overview, events);
        overview.showRecording(recording, i18n.format("overview.details.format",
                recording.path(),
                formatEventTime(recording.startTime()),
                formatEventTime(recording.endTime()),
                DisplayFormats.formatDuration(recording.durationMillis()),
                DisplayFormats.formatFileSize(recording.sizeBytes())));
        events.loadRecording(recording);
        viewModel.showStatus(i18n.format("status.openedRecording", recording.name()));
        viewModel.showTaskSummary(i18n.get("taskSummary.eventsLoading"));
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
