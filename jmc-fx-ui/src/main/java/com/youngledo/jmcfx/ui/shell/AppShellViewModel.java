package com.youngledo.jmcfx.ui.shell;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.ui.advanced.AdvancedJfrViewModel;
import com.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import com.youngledo.jmcfx.ui.environment.EnvironmentViewModel;
import com.youngledo.jmcfx.ui.exceptions.ExceptionViewModel;
import com.youngledo.jmcfx.ui.fileio.FileIOViewModel;
import com.youngledo.jmcfx.ui.gc.G1GcViewModel;
import com.youngledo.jmcfx.ui.heap.HeapViewModel;
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
import com.youngledo.jmcfx.ui.leaks.LeakSuspectsViewModel;
import com.youngledo.jmcfx.ui.locks.LockViewModel;
import com.youngledo.jmcfx.ui.metadata.JfrMetadataViewModel;
import com.youngledo.jmcfx.ui.overview.OverviewViewModel;
import com.youngledo.jmcfx.domain.service.EventQueryService;
import com.youngledo.jmcfx.ui.preferences.AppTheme;
import com.youngledo.jmcfx.ui.profiling.ProfilingViewModel;
import com.youngledo.jmcfx.ui.rules.RuleResultsViewModel;
import com.youngledo.jmcfx.ui.socketio.SocketIOViewModel;
import com.youngledo.jmcfx.ui.tlab.TlabViewModel;
import com.youngledo.jmcfx.ui.threads.ThreadViewModel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the primary application shell.
///
/// The shell owns navigation, the current recording context, and cross-view
/// operational status. Diagnostic behavior stays in feature-specific view
/// models.
public class AppShellViewModel {

    private static final String HOME_SECTION = "home";
    private static final String SETTINGS_SECTION = "settings";
    private static final String JVMS_SECTION = "jvms";
    private static final String HEAP_DUMP_ANALYSIS_SECTION = "heapDumpAnalysis";
    private static final String DEFAULT_RECORDING_SECTION = "analysis";
    private static final Set<String> RECORDING_SECTIONS = Set.of("analysis", "overview", "events", "metadata", "advancedJfr", "profiling",
            "exceptions", "threads", "fileio", "socketio", "locks", "threadHistogram", "security",
            "nativeLibraries", "threadDumps", "heap", "leaks", "tlab", "jvmInfo", "gcConfig", "gcSummary",
            "gcDetails", "g1Gc", "javaFxEvents", "compilations", "codeCache", "classLoading", "vmOperations", "processes", "envVars",
            "sysProps", "recordingInfo", "agents", "constantPools");

    private final ObservableList<RecordingWorkspace> recordingWorkspaces = FXCollections.observableArrayList();
    private final ObservableList<RecordingWorkspace> readOnlyRecordingWorkspaces =
            FXCollections.unmodifiableObservableList(recordingWorkspaces);
    private final ObservableList<HeapDumpWorkspace> heapDumpWorkspaces = FXCollections.observableArrayList();
    private final ObservableList<HeapDumpWorkspace> readOnlyHeapDumpWorkspaces =
            FXCollections.unmodifiableObservableList(heapDumpWorkspaces);
    private final ObservableList<Object> workspaceTabs = FXCollections.observableArrayList();
    private final ObservableList<Object> readOnlyWorkspaceTabs =
            FXCollections.unmodifiableObservableList(workspaceTabs);
    private final ObjectProperty<RecordingWorkspace> selectedWorkspace = new SimpleObjectProperty<>();
    private final ObjectProperty<HeapDumpWorkspace> selectedHeapDumpWorkspace = new SimpleObjectProperty<>();
    private final ObjectProperty<LiveJvmWorkspace> liveJvmWorkspace = new SimpleObjectProperty<>();
    private final ObjectProperty<LiveJvmWorkspace> selectedLiveJvmWorkspace = new SimpleObjectProperty<>();
    private final ObjectProperty<AppWorkspaceKind> activeWorkspaceKind =
            new SimpleObjectProperty<>(AppWorkspaceKind.GLOBAL);
    private final StringProperty selectedSection = new SimpleStringProperty("home");
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final StringProperty taskSummary = new SimpleStringProperty("");
    private final StringProperty currentTargetName = new SimpleStringProperty("");
    private final BooleanProperty recordingOpen = new SimpleBooleanProperty(false);
    private final BooleanProperty liveJvmWorkspaceOpen = new SimpleBooleanProperty(false);
    private final ObjectProperty<LanguageMode> languageMode = new SimpleObjectProperty<>(LanguageMode.SYSTEM);
    private final ObjectProperty<AppTheme> theme = new SimpleObjectProperty<>(AppTheme.SYSTEM);

    public StringProperty selectedSectionProperty() {
        return selectedSection;
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public StringProperty taskSummaryProperty() {
        return taskSummary;
    }

    public StringProperty currentRecordingNameProperty() {
        return currentTargetName;
    }

    public StringProperty currentTargetNameProperty() {
        return currentTargetName;
    }

    public BooleanProperty recordingOpenProperty() {
        return recordingOpen;
    }

    public ObjectProperty<LanguageMode> languageModeProperty() {
        return languageMode;
    }

    public void setLanguageMode(LanguageMode mode) {
        languageMode.set(mode == null ? LanguageMode.ENGLISH : mode);
    }

    public ObjectProperty<AppTheme> themeProperty() {
        return theme;
    }

    public void setTheme(AppTheme theme) {
        this.theme.set(theme == null ? AppTheme.SYSTEM : theme);
    }

    public ObservableList<RecordingWorkspace> recordingWorkspacesProperty() {
        return readOnlyRecordingWorkspaces;
    }

    public ReadOnlyObjectProperty<RecordingWorkspace> selectedWorkspaceProperty() {
        return selectedWorkspace;
    }

    public ObservableList<HeapDumpWorkspace> heapDumpWorkspacesProperty() {
        return readOnlyHeapDumpWorkspaces;
    }

    public ObservableList<Object> workspaceTabsProperty() {
        return readOnlyWorkspaceTabs;
    }

    public ReadOnlyObjectProperty<HeapDumpWorkspace> selectedHeapDumpWorkspaceProperty() {
        return selectedHeapDumpWorkspace;
    }

    public ReadOnlyObjectProperty<AppWorkspaceKind> activeWorkspaceKindProperty() {
        return activeWorkspaceKind;
    }

    public BooleanProperty liveJvmWorkspaceOpenProperty() {
        return liveJvmWorkspaceOpen;
    }

    public ReadOnlyObjectProperty<LiveJvmWorkspace> liveJvmWorkspaceProperty() {
        return liveJvmWorkspace;
    }

    public ReadOnlyObjectProperty<LiveJvmWorkspace> selectedLiveJvmWorkspaceProperty() {
        return selectedLiveJvmWorkspace;
    }

    public void showSection(String sectionId) {
        if (!knownSection(sectionId)) {
            return;
        }
        if (JVMS_SECTION.equals(sectionId)) {
            openLiveJvmWorkspace();
            return;
        }
        if (!sectionAllowedForActiveWorkspace(sectionId)) {
            return;
        }
        RecordingWorkspace workspace = selectedWorkspace.get();
        if (workspace != null && isRecordingSection(sectionId)) {
            workspace.selectedSectionProperty().set(sectionId);
        }
        selectedSection.set(sectionId);
    }

    public RecordingWorkspace openRecording(RecordingSummary recording) {
        return openRecording(recording, new OverviewViewModel(),
                new EventBrowserViewModel(new UnavailableEventQueryService()),
                new RuleResultsViewModel(rec -> List.of()),
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null);
    }

    public RecordingWorkspace openRecording(RecordingSummary recording, OverviewViewModel overview,
            EventBrowserViewModel events, RuleResultsViewModel ruleResults) {
        return openRecording(recording, overview, events, ruleResults, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null);
    }

    public RecordingWorkspace openRecording(RecordingSummary recording, OverviewViewModel overview,
            EventBrowserViewModel events, RuleResultsViewModel ruleResults,
            ProfilingViewModel profiling, ExceptionViewModel exceptions, ThreadViewModel threads,
            FileIOViewModel fileio, SocketIOViewModel socketio, LockViewModel locks) {
        return openRecording(recording, overview, events, ruleResults, profiling, exceptions, threads,
                fileio, socketio, locks, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null);
    }

    public RecordingWorkspace openRecording(RecordingSummary recording, OverviewViewModel overview,
            EventBrowserViewModel events, RuleResultsViewModel ruleResults,
            ProfilingViewModel profiling, ExceptionViewModel exceptions, ThreadViewModel threads,
            FileIOViewModel fileio, SocketIOViewModel socketio, LockViewModel locks,
            HeapViewModel heap, LeakSuspectsViewModel leakSuspects, TlabViewModel tlab) {
        return openRecording(recording, overview, events, ruleResults, profiling, exceptions, threads,
                fileio, socketio, locks, heap, leakSuspects, tlab,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null);
    }

    public RecordingWorkspace openRecording(RecordingSummary recording, OverviewViewModel overview,
            EventBrowserViewModel events, RuleResultsViewModel ruleResults,
            ProfilingViewModel profiling, ExceptionViewModel exceptions, ThreadViewModel threads,
            FileIOViewModel fileio, SocketIOViewModel socketio, LockViewModel locks,
            HeapViewModel heap, LeakSuspectsViewModel leakSuspects, TlabViewModel tlab,
            JvmInfoViewModel jvmInfo, GcConfigViewModel gcConfig, GcSummaryViewModel gcSummary,
            GcDetailsViewModel gcDetails, CompilationsViewModel compilations,
            CodeCacheViewModel codeCache, ClassLoadingViewModel classLoading,
            VmOperationsViewModel vmOperations) {
        return openRecording(recording, overview, events, ruleResults, profiling, exceptions, threads,
                fileio, socketio, locks, heap, leakSuspects, tlab,
                jvmInfo, gcConfig, gcSummary, gcDetails, compilations, codeCache, classLoading, vmOperations,
                null, null, null, null, null, null);
    }

    public RecordingWorkspace openRecording(RecordingSummary recording, OverviewViewModel overview,
            EventBrowserViewModel events, RuleResultsViewModel ruleResults,
            ProfilingViewModel profiling, ExceptionViewModel exceptions, ThreadViewModel threads,
            FileIOViewModel fileio, SocketIOViewModel socketio, LockViewModel locks,
            HeapViewModel heap, LeakSuspectsViewModel leakSuspects, TlabViewModel tlab,
            JvmInfoViewModel jvmInfo, GcConfigViewModel gcConfig, GcSummaryViewModel gcSummary,
            GcDetailsViewModel gcDetails, CompilationsViewModel compilations,
            CodeCacheViewModel codeCache, ClassLoadingViewModel classLoading,
            VmOperationsViewModel vmOperations, EnvironmentViewModel environment) {
        return openRecording(recording, overview, events, ruleResults, profiling, exceptions, threads,
                fileio, socketio, locks, heap, leakSuspects, tlab,
                jvmInfo, gcConfig, gcSummary, gcDetails, compilations, codeCache, classLoading, vmOperations,
                environment, null, null, null, null, null);
    }

    public RecordingWorkspace openRecording(RecordingSummary recording, OverviewViewModel overview,
            EventBrowserViewModel events, RuleResultsViewModel ruleResults,
            ProfilingViewModel profiling, ExceptionViewModel exceptions, ThreadViewModel threads,
            FileIOViewModel fileio, SocketIOViewModel socketio, LockViewModel locks,
            HeapViewModel heap, LeakSuspectsViewModel leakSuspects, TlabViewModel tlab,
            JvmInfoViewModel jvmInfo, GcConfigViewModel gcConfig, GcSummaryViewModel gcSummary,
            GcDetailsViewModel gcDetails, CompilationsViewModel compilations,
            CodeCacheViewModel codeCache, ClassLoadingViewModel classLoading,
            VmOperationsViewModel vmOperations, EnvironmentViewModel environment,
            JavaAppOverviewViewModel javaAppOverviewViewModel,
            SecurityViewModel securityViewModel,
            NativeLibraryViewModel nativeLibraryViewModel,
            ThreadDumpViewModel threadDumpViewModel,
            AdvancedJfrViewModel advancedJfrViewModel) {
        return openRecording(recording, overview, events, ruleResults, profiling, exceptions, threads,
                fileio, socketio, locks, heap, leakSuspects, tlab,
                jvmInfo, gcConfig, gcSummary, gcDetails, compilations, codeCache, classLoading, vmOperations,
                environment, javaAppOverviewViewModel, securityViewModel, nativeLibraryViewModel, threadDumpViewModel,
                null, null, null, advancedJfrViewModel);
    }

    public RecordingWorkspace openRecording(RecordingSummary recording, OverviewViewModel overview,
            EventBrowserViewModel events, RuleResultsViewModel ruleResults,
            ProfilingViewModel profiling, ExceptionViewModel exceptions, ThreadViewModel threads,
            FileIOViewModel fileio, SocketIOViewModel socketio, LockViewModel locks,
            HeapViewModel heap, LeakSuspectsViewModel leakSuspects, TlabViewModel tlab,
            JvmInfoViewModel jvmInfo, GcConfigViewModel gcConfig, GcSummaryViewModel gcSummary,
            GcDetailsViewModel gcDetails, CompilationsViewModel compilations,
            CodeCacheViewModel codeCache, ClassLoadingViewModel classLoading,
            VmOperationsViewModel vmOperations, EnvironmentViewModel environment,
            JavaAppOverviewViewModel javaAppOverviewViewModel,
            SecurityViewModel securityViewModel,
            NativeLibraryViewModel nativeLibraryViewModel,
            ThreadDumpViewModel threadDumpViewModel,
            JfrMetadataViewModel jfrMetadataViewModel,
            AdvancedJfrViewModel advancedJfrViewModel) {
        return openRecording(recording, overview, events, ruleResults, profiling, exceptions, threads,
                fileio, socketio, locks, heap, leakSuspects, tlab,
                jvmInfo, gcConfig, gcSummary, gcDetails, compilations, codeCache, classLoading, vmOperations,
                environment, javaAppOverviewViewModel, securityViewModel, nativeLibraryViewModel, threadDumpViewModel,
                jfrMetadataViewModel, null, null, advancedJfrViewModel);
    }

    public RecordingWorkspace openRecording(RecordingSummary recording, OverviewViewModel overview,
            EventBrowserViewModel events, RuleResultsViewModel ruleResults,
            ProfilingViewModel profiling, ExceptionViewModel exceptions, ThreadViewModel threads,
            FileIOViewModel fileio, SocketIOViewModel socketio, LockViewModel locks,
            HeapViewModel heap, LeakSuspectsViewModel leakSuspects, TlabViewModel tlab,
            JvmInfoViewModel jvmInfo, GcConfigViewModel gcConfig, GcSummaryViewModel gcSummary,
            GcDetailsViewModel gcDetails, CompilationsViewModel compilations,
            CodeCacheViewModel codeCache, ClassLoadingViewModel classLoading,
            VmOperationsViewModel vmOperations, EnvironmentViewModel environment,
            JavaAppOverviewViewModel javaAppOverviewViewModel,
            SecurityViewModel securityViewModel,
            NativeLibraryViewModel nativeLibraryViewModel,
            ThreadDumpViewModel threadDumpViewModel,
            JfrMetadataViewModel jfrMetadataViewModel,
            G1GcViewModel g1GcViewModel,
            JavaFxEventsViewModel javaFxEventsViewModel,
            AdvancedJfrViewModel advancedJfrViewModel) {
        Objects.requireNonNull(recording, "recording");
        Objects.requireNonNull(overview, "overview");
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(ruleResults, "ruleResults");
        RecordingWorkspace existingWorkspace = recordingWorkspaceFor(recording.path());
        if (existingWorkspace != null) {
            events.close();
            selectWorkspace(existingWorkspace);
            return existingWorkspace;
        }
        RecordingWorkspace workspace = new RecordingWorkspace(recording, overview, events, ruleResults,
                profiling, exceptions, threads, fileio, socketio, locks, heap, leakSuspects, tlab,
                jvmInfo, gcConfig, gcSummary, gcDetails, compilations, codeCache, classLoading, vmOperations,
                environment, javaAppOverviewViewModel, securityViewModel, nativeLibraryViewModel, threadDumpViewModel,
                jfrMetadataViewModel, g1GcViewModel, javaFxEventsViewModel, advancedJfrViewModel);
        workspace.selectedSectionProperty().set(DEFAULT_RECORDING_SECTION);
        recordingWorkspaces.add(workspace);
        workspaceTabs.add(workspace);
        selectWorkspace(workspace);
        selectedSection.set(DEFAULT_RECORDING_SECTION);
        statusMessage.set("");
        taskSummary.set("");
        return workspace;
    }

    public HeapDumpWorkspace openHeapDump(HeapDumpWorkspace workspace) {
        Objects.requireNonNull(workspace, "workspace");
        HeapDumpWorkspace existingWorkspace = heapDumpWorkspaceFor(workspace.path());
        if (existingWorkspace != null) {
            workspace.close();
            selectHeapDumpWorkspace(existingWorkspace);
            return existingWorkspace;
        }
        heapDumpWorkspaces.add(workspace);
        workspaceTabs.add(workspace);
        selectHeapDumpWorkspace(workspace);
        return workspace;
    }

    public void openLiveJvmWorkspace() {
        if (liveJvmWorkspace.get() == null) {
            LiveJvmWorkspace workspace = new LiveJvmWorkspace("JVM");
            liveJvmWorkspace.set(workspace);
            liveJvmWorkspaceOpen.set(true);
            workspaceTabs.add(workspace);
        }
        selectLiveJvmWorkspace();
    }

    public void selectLiveJvmWorkspace() {
        if (liveJvmWorkspace.get() == null) {
            return;
        }
        selectedWorkspace.set(null);
        selectedHeapDumpWorkspace.set(null);
        selectedLiveJvmWorkspace.set(liveJvmWorkspace.get());
        activeWorkspaceKind.set(AppWorkspaceKind.LIVE_JVM);
        currentTargetName.set(liveJvmWorkspace.get().name());
        selectedSection.set(JVMS_SECTION);
    }

    public void closeLiveJvmWorkspace() {
        LiveJvmWorkspace workspace = liveJvmWorkspace.get();
        if (workspace == null) {
            return;
        }
        boolean active = selectedLiveJvmWorkspace.get() == workspace;
        workspaceTabs.remove(workspace);
        liveJvmWorkspace.set(null);
        selectedLiveJvmWorkspace.set(null);
        liveJvmWorkspaceOpen.set(false);
        if (!active) {
            return;
        }
        activeWorkspaceKind.set(AppWorkspaceKind.GLOBAL);
        currentTargetName.set("");
        selectedSection.set(HOME_SECTION);
    }

    public void selectHeapDumpWorkspace(HeapDumpWorkspace workspace) {
        if (workspace == null || !heapDumpWorkspaces.contains(workspace)) {
            return;
        }
        selectedWorkspace.set(null);
        selectedLiveJvmWorkspace.set(null);
        selectedHeapDumpWorkspace.set(workspace);
        activeWorkspaceKind.set(AppWorkspaceKind.HEAP_DUMP);
        currentTargetName.set(workspace.path().getFileName().toString());
        selectedSection.set(HEAP_DUMP_ANALYSIS_SECTION);
    }

    public void closeHeapDumpWorkspace(HeapDumpWorkspace workspace) {
        if (workspace == null || !heapDumpWorkspaces.contains(workspace)) {
            return;
        }
        boolean active = workspace == selectedHeapDumpWorkspace.get();
        int closedIndex = heapDumpWorkspaces.indexOf(workspace);
        heapDumpWorkspaces.remove(workspace);
        workspaceTabs.remove(workspace);
        workspace.close();
        if (!active) {
            return;
        }
        if (heapDumpWorkspaces.isEmpty()) {
            selectedHeapDumpWorkspace.set(null);
            activeWorkspaceKind.set(AppWorkspaceKind.GLOBAL);
            currentTargetName.set("");
            selectedSection.set(HOME_SECTION);
            return;
        }
        int nextIndex = Math.min(closedIndex, heapDumpWorkspaces.size() - 1);
        selectHeapDumpWorkspace(heapDumpWorkspaces.get(nextIndex));
    }

    public void selectWorkspace(RecordingWorkspace workspace) {
        if (workspace == null || !recordingWorkspaces.contains(workspace)) {
            return;
        }
        selectedHeapDumpWorkspace.set(null);
        selectedLiveJvmWorkspace.set(null);
        selectedWorkspace.set(workspace);
        activeWorkspaceKind.set(AppWorkspaceKind.RECORDING);
        recordingOpen.set(true);
        currentTargetName.set(workspace.recording().name());
        selectedSection.set(workspace.selectedSectionProperty().get());
    }

    public boolean selectRecordingWorkspaceByPath(Path path) {
        RecordingWorkspace workspace = recordingWorkspaceFor(path);
        if (workspace == null) {
            return false;
        }
        selectWorkspace(workspace);
        return true;
    }

    public boolean selectHeapDumpWorkspaceByPath(Path path) {
        HeapDumpWorkspace workspace = heapDumpWorkspaceFor(path);
        if (workspace == null) {
            return false;
        }
        selectHeapDumpWorkspace(workspace);
        return true;
    }

    public void closeWorkspace(RecordingWorkspace workspace) {
        if (workspace == null || !recordingWorkspaces.contains(workspace)) {
            return;
        }
        boolean active = workspace == selectedWorkspace.get();
        int closedIndex = recordingWorkspaces.indexOf(workspace);
        recordingWorkspaces.remove(workspace);
        workspaceTabs.remove(workspace);
        workspace.close();
        if (!active) {
            return;
        }
        if (recordingWorkspaces.isEmpty()) {
            selectedWorkspace.set(null);
            activeWorkspaceKind.set(AppWorkspaceKind.GLOBAL);
            recordingOpen.set(false);
            currentTargetName.set("");
            selectedSection.set(HOME_SECTION);
            return;
        }
        int nextIndex = Math.min(closedIndex, recordingWorkspaces.size() - 1);
        selectWorkspace(recordingWorkspaces.get(nextIndex));
        if (isRecordingSection(selectedSection.get())) {
            selectedSection.set(selectedWorkspace.get().selectedSectionProperty().get());
        }
    }

    public void showStatus(String message) {
        statusMessage.set(Objects.requireNonNullElse(message, ""));
    }

    public void showTaskSummary(String message) {
        taskSummary.set(Objects.requireNonNullElse(message, ""));
    }

    private static boolean isRecordingSection(String sectionId) {
        return sectionId != null && RECORDING_SECTIONS.contains(sectionId);
    }

    private static boolean knownSection(String sectionId) {
        return HOME_SECTION.equals(sectionId)
                || SETTINGS_SECTION.equals(sectionId)
                || JVMS_SECTION.equals(sectionId)
                || HEAP_DUMP_ANALYSIS_SECTION.equals(sectionId)
                || isRecordingSection(sectionId);
    }

    private RecordingWorkspace recordingWorkspaceFor(Path path) {
        Path normalizedPath = normalizedPath(path);
        return recordingWorkspaces.stream()
                .filter(workspace -> normalizedPath(workspace.recording().path()).equals(normalizedPath))
                .findFirst()
                .orElse(null);
    }

    private HeapDumpWorkspace heapDumpWorkspaceFor(Path path) {
        Path normalizedPath = normalizedPath(path);
        return heapDumpWorkspaces.stream()
                .filter(workspace -> normalizedPath(workspace.path()).equals(normalizedPath))
                .findFirst()
                .orElse(null);
    }

    private static Path normalizedPath(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private boolean sectionAllowedForActiveWorkspace(String sectionId) {
        return switch (activeWorkspaceKind.get()) {
            case GLOBAL -> HOME_SECTION.equals(sectionId) || SETTINGS_SECTION.equals(sectionId) || JVMS_SECTION.equals(sectionId);
            case RECORDING -> HOME_SECTION.equals(sectionId) || SETTINGS_SECTION.equals(sectionId) || isRecordingSection(sectionId);
            case HEAP_DUMP -> HOME_SECTION.equals(sectionId) || SETTINGS_SECTION.equals(sectionId)
                    || HEAP_DUMP_ANALYSIS_SECTION.equals(sectionId);
            case LIVE_JVM -> HOME_SECTION.equals(sectionId) || SETTINGS_SECTION.equals(sectionId) || JVMS_SECTION.equals(sectionId);
        };
    }

    private static final class UnavailableEventQueryService implements EventQueryService {
        @Override
        public com.youngledo.jmcfx.domain.service.EventQuerySession openSession(RecordingSummary recording) {
            throw new UnsupportedOperationException("Event browser service is not connected for this workspace.");
        }
    }
}
