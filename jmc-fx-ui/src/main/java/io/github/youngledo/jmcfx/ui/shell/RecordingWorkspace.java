package io.github.youngledo.jmcfx.ui.shell;

import java.util.UUID;
import java.util.Objects;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.ui.analysis.RecordingAiAssistantViewModel;
import io.github.youngledo.jmcfx.ui.advanced.AdvancedJfrViewModel;
import io.github.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import io.github.youngledo.jmcfx.ui.environment.EnvironmentViewModel;
import io.github.youngledo.jmcfx.ui.exceptions.ExceptionViewModel;
import io.github.youngledo.jmcfx.ui.fileio.FileIOViewModel;
import io.github.youngledo.jmcfx.ui.gc.G1GcViewModel;
import io.github.youngledo.jmcfx.ui.heap.HeapViewModel;
import io.github.youngledo.jmcfx.ui.leaks.LeakSuspectsViewModel;
import io.github.youngledo.jmcfx.ui.locks.LockViewModel;
import io.github.youngledo.jmcfx.ui.overview.OverviewViewModel;
import io.github.youngledo.jmcfx.ui.profiling.ProfilingViewModel;
import io.github.youngledo.jmcfx.ui.recording.RecordingTimeRange;
import io.github.youngledo.jmcfx.ui.rules.RuleResultsViewModel;
import io.github.youngledo.jmcfx.ui.socketio.SocketIOViewModel;
import io.github.youngledo.jmcfx.ui.jvm.ClassLoadingViewModel;
import io.github.youngledo.jmcfx.ui.jvm.CodeCacheViewModel;
import io.github.youngledo.jmcfx.ui.jvm.CompilationsViewModel;
import io.github.youngledo.jmcfx.ui.jvm.GcConfigViewModel;
import io.github.youngledo.jmcfx.ui.jvm.GcDetailsViewModel;
import io.github.youngledo.jmcfx.ui.jvm.GcSummaryViewModel;
import io.github.youngledo.jmcfx.ui.jvm.JvmInfoViewModel;
import io.github.youngledo.jmcfx.ui.javaapp.JavaAppOverviewViewModel;
import io.github.youngledo.jmcfx.ui.javaapp.NativeLibraryViewModel;
import io.github.youngledo.jmcfx.ui.javaapp.SecurityViewModel;
import io.github.youngledo.jmcfx.ui.javaapp.ThreadDumpViewModel;
import io.github.youngledo.jmcfx.ui.jfx.JavaFxEventsViewModel;
import io.github.youngledo.jmcfx.ui.jvms.LiveFlightRecordingOrigin;
import io.github.youngledo.jmcfx.ui.jvm.VmOperationsViewModel;
import io.github.youngledo.jmcfx.ui.metadata.JfrMetadataViewModel;
import io.github.youngledo.jmcfx.ui.tlab.TlabViewModel;
import io.github.youngledo.jmcfx.ui.threads.ThreadViewModel;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/// UI-layer state for one opened recording workspace.
public final class RecordingWorkspace {

    private final String id = UUID.randomUUID().toString();
    private final RecordingSummary recording;
    private final LiveFlightRecordingOrigin liveOrigin;
    private final StringProperty selectedSection = new SimpleStringProperty("analysis");
    private final ObjectProperty<RecordingTimeRange> selectedTimeRange = new SimpleObjectProperty<>();
    private final Set<String> loadedSections = new HashSet<>();
    private final Set<String> loadingSections = new HashSet<>();
    private final Map<String, RecordingPageLayoutState> pageLayoutStates = new HashMap<>();
    private volatile String latestRequestedLoadSection = "analysis";
    private final OverviewViewModel overviewViewModel;
    private final EventBrowserViewModel eventBrowserViewModel;
    private final RuleResultsViewModel ruleResultsViewModel;
    private final ProfilingViewModel profilingViewModel;
    private final ExceptionViewModel exceptionViewModel;
    private final ThreadViewModel threadViewModel;
    private final FileIOViewModel fileIOViewModel;
    private final SocketIOViewModel socketIOViewModel;
    private final LockViewModel lockViewModel;
    private final HeapViewModel heapViewModel;
    private final LeakSuspectsViewModel leakSuspectsViewModel;
    private final TlabViewModel tlabViewModel;
    private final JvmInfoViewModel jvmInfoViewModel;
    private final GcConfigViewModel gcConfigViewModel;
    private final GcSummaryViewModel gcSummaryViewModel;
    private final GcDetailsViewModel gcDetailsViewModel;
    private final CompilationsViewModel compilationsViewModel;
    private final CodeCacheViewModel codeCacheViewModel;
    private final ClassLoadingViewModel classLoadingViewModel;
    private final VmOperationsViewModel vmOperationsViewModel;
    private final EnvironmentViewModel environmentViewModel;
    private final JavaAppOverviewViewModel javaAppOverviewViewModel;
    private final SecurityViewModel securityViewModel;
    private final NativeLibraryViewModel nativeLibraryViewModel;
    private final ThreadDumpViewModel threadDumpViewModel;
    private final JfrMetadataViewModel jfrMetadataViewModel;
    private final G1GcViewModel g1GcViewModel;
    private final JavaFxEventsViewModel javaFxEventsViewModel;
    private final AdvancedJfrViewModel advancedJfrViewModel;
    private final RecordingAiAssistantViewModel aiAssistantViewModel;

    public RecordingWorkspace(RecordingSummary recording, OverviewViewModel overviewViewModel,
            EventBrowserViewModel eventBrowserViewModel, RuleResultsViewModel ruleResultsViewModel,
            ProfilingViewModel profilingViewModel, ExceptionViewModel exceptionViewModel,
            ThreadViewModel threadViewModel, FileIOViewModel fileIOViewModel,
            SocketIOViewModel socketIOViewModel, LockViewModel lockViewModel,
            HeapViewModel heapViewModel, LeakSuspectsViewModel leakSuspectsViewModel,
            TlabViewModel tlabViewModel,
            JvmInfoViewModel jvmInfoViewModel,
            GcConfigViewModel gcConfigViewModel,
            GcSummaryViewModel gcSummaryViewModel,
            GcDetailsViewModel gcDetailsViewModel,
            CompilationsViewModel compilationsViewModel,
            CodeCacheViewModel codeCacheViewModel,
            ClassLoadingViewModel classLoadingViewModel,
            VmOperationsViewModel vmOperationsViewModel,
            EnvironmentViewModel environmentViewModel,
            JavaAppOverviewViewModel javaAppOverviewViewModel,
            SecurityViewModel securityViewModel,
            NativeLibraryViewModel nativeLibraryViewModel,
            ThreadDumpViewModel threadDumpViewModel) {
        this(recording, overviewViewModel, eventBrowserViewModel, ruleResultsViewModel,
                profilingViewModel, exceptionViewModel, threadViewModel, fileIOViewModel,
                socketIOViewModel, lockViewModel, heapViewModel, leakSuspectsViewModel,
                tlabViewModel, jvmInfoViewModel, gcConfigViewModel, gcSummaryViewModel,
                gcDetailsViewModel, compilationsViewModel, codeCacheViewModel,
                classLoadingViewModel, vmOperationsViewModel, environmentViewModel,
                javaAppOverviewViewModel, securityViewModel, nativeLibraryViewModel,
                threadDumpViewModel, null);
    }

    public RecordingWorkspace(RecordingSummary recording, OverviewViewModel overviewViewModel,
            EventBrowserViewModel eventBrowserViewModel, RuleResultsViewModel ruleResultsViewModel,
            ProfilingViewModel profilingViewModel, ExceptionViewModel exceptionViewModel,
            ThreadViewModel threadViewModel, FileIOViewModel fileIOViewModel,
            SocketIOViewModel socketIOViewModel, LockViewModel lockViewModel,
            HeapViewModel heapViewModel, LeakSuspectsViewModel leakSuspectsViewModel,
            TlabViewModel tlabViewModel,
            JvmInfoViewModel jvmInfoViewModel,
            GcConfigViewModel gcConfigViewModel,
            GcSummaryViewModel gcSummaryViewModel,
            GcDetailsViewModel gcDetailsViewModel,
            CompilationsViewModel compilationsViewModel,
            CodeCacheViewModel codeCacheViewModel,
            ClassLoadingViewModel classLoadingViewModel,
            VmOperationsViewModel vmOperationsViewModel,
            EnvironmentViewModel environmentViewModel,
            JavaAppOverviewViewModel javaAppOverviewViewModel,
            SecurityViewModel securityViewModel,
            NativeLibraryViewModel nativeLibraryViewModel,
            ThreadDumpViewModel threadDumpViewModel,
            AdvancedJfrViewModel advancedJfrViewModel) {
        this(recording, overviewViewModel, eventBrowserViewModel, ruleResultsViewModel,
                profilingViewModel, exceptionViewModel, threadViewModel, fileIOViewModel,
                socketIOViewModel, lockViewModel, heapViewModel, leakSuspectsViewModel,
                tlabViewModel, jvmInfoViewModel, gcConfigViewModel, gcSummaryViewModel,
                gcDetailsViewModel, compilationsViewModel, codeCacheViewModel,
                classLoadingViewModel, vmOperationsViewModel, environmentViewModel,
                javaAppOverviewViewModel, securityViewModel, nativeLibraryViewModel,
                threadDumpViewModel, null, advancedJfrViewModel);
    }

    public RecordingWorkspace(RecordingSummary recording, OverviewViewModel overviewViewModel,
            EventBrowserViewModel eventBrowserViewModel, RuleResultsViewModel ruleResultsViewModel,
            ProfilingViewModel profilingViewModel, ExceptionViewModel exceptionViewModel,
            ThreadViewModel threadViewModel, FileIOViewModel fileIOViewModel,
            SocketIOViewModel socketIOViewModel, LockViewModel lockViewModel,
            HeapViewModel heapViewModel, LeakSuspectsViewModel leakSuspectsViewModel,
            TlabViewModel tlabViewModel,
            JvmInfoViewModel jvmInfoViewModel,
            GcConfigViewModel gcConfigViewModel,
            GcSummaryViewModel gcSummaryViewModel,
            GcDetailsViewModel gcDetailsViewModel,
            CompilationsViewModel compilationsViewModel,
            CodeCacheViewModel codeCacheViewModel,
            ClassLoadingViewModel classLoadingViewModel,
            VmOperationsViewModel vmOperationsViewModel,
            EnvironmentViewModel environmentViewModel,
            JavaAppOverviewViewModel javaAppOverviewViewModel,
            SecurityViewModel securityViewModel,
            NativeLibraryViewModel nativeLibraryViewModel,
            ThreadDumpViewModel threadDumpViewModel,
            JfrMetadataViewModel jfrMetadataViewModel,
            AdvancedJfrViewModel advancedJfrViewModel) {
        this(recording, overviewViewModel, eventBrowserViewModel, ruleResultsViewModel,
                profilingViewModel, exceptionViewModel, threadViewModel, fileIOViewModel,
                socketIOViewModel, lockViewModel, heapViewModel, leakSuspectsViewModel,
                tlabViewModel, jvmInfoViewModel, gcConfigViewModel, gcSummaryViewModel,
                gcDetailsViewModel, compilationsViewModel, codeCacheViewModel,
                classLoadingViewModel, vmOperationsViewModel, environmentViewModel,
                javaAppOverviewViewModel, securityViewModel, nativeLibraryViewModel,
                threadDumpViewModel, jfrMetadataViewModel, null, null, advancedJfrViewModel);
    }

    public RecordingWorkspace(RecordingSummary recording, OverviewViewModel overviewViewModel,
            EventBrowserViewModel eventBrowserViewModel, RuleResultsViewModel ruleResultsViewModel,
            ProfilingViewModel profilingViewModel, ExceptionViewModel exceptionViewModel,
            ThreadViewModel threadViewModel, FileIOViewModel fileIOViewModel,
            SocketIOViewModel socketIOViewModel, LockViewModel lockViewModel,
            HeapViewModel heapViewModel, LeakSuspectsViewModel leakSuspectsViewModel,
            TlabViewModel tlabViewModel,
            JvmInfoViewModel jvmInfoViewModel,
            GcConfigViewModel gcConfigViewModel,
            GcSummaryViewModel gcSummaryViewModel,
            GcDetailsViewModel gcDetailsViewModel,
            CompilationsViewModel compilationsViewModel,
            CodeCacheViewModel codeCacheViewModel,
            ClassLoadingViewModel classLoadingViewModel,
            VmOperationsViewModel vmOperationsViewModel,
            EnvironmentViewModel environmentViewModel,
            JavaAppOverviewViewModel javaAppOverviewViewModel,
            SecurityViewModel securityViewModel,
            NativeLibraryViewModel nativeLibraryViewModel,
            ThreadDumpViewModel threadDumpViewModel,
            JfrMetadataViewModel jfrMetadataViewModel,
            G1GcViewModel g1GcViewModel,
            JavaFxEventsViewModel javaFxEventsViewModel,
            AdvancedJfrViewModel advancedJfrViewModel) {
        this(recording, overviewViewModel, eventBrowserViewModel, ruleResultsViewModel,
                profilingViewModel, exceptionViewModel, threadViewModel, fileIOViewModel,
                socketIOViewModel, lockViewModel, heapViewModel, leakSuspectsViewModel,
                tlabViewModel, jvmInfoViewModel, gcConfigViewModel, gcSummaryViewModel,
                gcDetailsViewModel, compilationsViewModel, codeCacheViewModel,
                classLoadingViewModel, vmOperationsViewModel, environmentViewModel,
                javaAppOverviewViewModel, securityViewModel, nativeLibraryViewModel,
                threadDumpViewModel, jfrMetadataViewModel, g1GcViewModel, javaFxEventsViewModel,
                advancedJfrViewModel, null);
    }

    public RecordingWorkspace(RecordingSummary recording, OverviewViewModel overviewViewModel,
            EventBrowserViewModel eventBrowserViewModel, RuleResultsViewModel ruleResultsViewModel,
            ProfilingViewModel profilingViewModel, ExceptionViewModel exceptionViewModel,
            ThreadViewModel threadViewModel, FileIOViewModel fileIOViewModel,
            SocketIOViewModel socketIOViewModel, LockViewModel lockViewModel,
            HeapViewModel heapViewModel, LeakSuspectsViewModel leakSuspectsViewModel,
            TlabViewModel tlabViewModel,
            JvmInfoViewModel jvmInfoViewModel,
            GcConfigViewModel gcConfigViewModel,
            GcSummaryViewModel gcSummaryViewModel,
            GcDetailsViewModel gcDetailsViewModel,
            CompilationsViewModel compilationsViewModel,
            CodeCacheViewModel codeCacheViewModel,
            ClassLoadingViewModel classLoadingViewModel,
            VmOperationsViewModel vmOperationsViewModel,
            EnvironmentViewModel environmentViewModel,
            JavaAppOverviewViewModel javaAppOverviewViewModel,
            SecurityViewModel securityViewModel,
            NativeLibraryViewModel nativeLibraryViewModel,
            ThreadDumpViewModel threadDumpViewModel,
            JfrMetadataViewModel jfrMetadataViewModel,
            G1GcViewModel g1GcViewModel,
            JavaFxEventsViewModel javaFxEventsViewModel,
            AdvancedJfrViewModel advancedJfrViewModel,
            RecordingAiAssistantViewModel aiAssistantViewModel) {
        this(recording, overviewViewModel, eventBrowserViewModel, ruleResultsViewModel,
                profilingViewModel, exceptionViewModel, threadViewModel, fileIOViewModel,
                socketIOViewModel, lockViewModel, heapViewModel, leakSuspectsViewModel,
                tlabViewModel, jvmInfoViewModel, gcConfigViewModel, gcSummaryViewModel,
                gcDetailsViewModel, compilationsViewModel, codeCacheViewModel,
                classLoadingViewModel, vmOperationsViewModel, environmentViewModel,
                javaAppOverviewViewModel, securityViewModel, nativeLibraryViewModel,
                threadDumpViewModel, jfrMetadataViewModel, g1GcViewModel, javaFxEventsViewModel,
                advancedJfrViewModel, aiAssistantViewModel, null);
    }

    public RecordingWorkspace(RecordingSummary recording, OverviewViewModel overviewViewModel,
            EventBrowserViewModel eventBrowserViewModel, RuleResultsViewModel ruleResultsViewModel,
            ProfilingViewModel profilingViewModel, ExceptionViewModel exceptionViewModel,
            ThreadViewModel threadViewModel, FileIOViewModel fileIOViewModel,
            SocketIOViewModel socketIOViewModel, LockViewModel lockViewModel,
            HeapViewModel heapViewModel, LeakSuspectsViewModel leakSuspectsViewModel,
            TlabViewModel tlabViewModel,
            JvmInfoViewModel jvmInfoViewModel,
            GcConfigViewModel gcConfigViewModel,
            GcSummaryViewModel gcSummaryViewModel,
            GcDetailsViewModel gcDetailsViewModel,
            CompilationsViewModel compilationsViewModel,
            CodeCacheViewModel codeCacheViewModel,
            ClassLoadingViewModel classLoadingViewModel,
            VmOperationsViewModel vmOperationsViewModel,
            EnvironmentViewModel environmentViewModel,
            JavaAppOverviewViewModel javaAppOverviewViewModel,
            SecurityViewModel securityViewModel,
            NativeLibraryViewModel nativeLibraryViewModel,
            ThreadDumpViewModel threadDumpViewModel,
            JfrMetadataViewModel jfrMetadataViewModel,
            G1GcViewModel g1GcViewModel,
            JavaFxEventsViewModel javaFxEventsViewModel,
            AdvancedJfrViewModel advancedJfrViewModel,
            RecordingAiAssistantViewModel aiAssistantViewModel,
            LiveFlightRecordingOrigin liveOrigin) {
        this.recording = Objects.requireNonNull(recording, "recording");
        this.liveOrigin = liveOrigin;
        this.overviewViewModel = Objects.requireNonNull(overviewViewModel, "overviewViewModel");
        this.eventBrowserViewModel = Objects.requireNonNull(eventBrowserViewModel, "eventBrowserViewModel");
        this.ruleResultsViewModel = Objects.requireNonNull(ruleResultsViewModel, "ruleResultsViewModel");
        this.profilingViewModel = profilingViewModel;
        this.exceptionViewModel = exceptionViewModel;
        this.threadViewModel = threadViewModel;
        this.fileIOViewModel = fileIOViewModel;
        this.socketIOViewModel = socketIOViewModel;
        this.lockViewModel = lockViewModel;
        this.heapViewModel = heapViewModel;
        this.leakSuspectsViewModel = leakSuspectsViewModel;
        this.tlabViewModel = tlabViewModel;
        this.jvmInfoViewModel = jvmInfoViewModel;
        this.gcConfigViewModel = gcConfigViewModel;
        this.gcSummaryViewModel = gcSummaryViewModel;
        this.gcDetailsViewModel = gcDetailsViewModel;
        this.compilationsViewModel = compilationsViewModel;
        this.codeCacheViewModel = codeCacheViewModel;
        this.classLoadingViewModel = classLoadingViewModel;
        this.vmOperationsViewModel = vmOperationsViewModel;
        this.environmentViewModel = environmentViewModel;
        this.javaAppOverviewViewModel = javaAppOverviewViewModel;
        this.securityViewModel = securityViewModel;
        this.nativeLibraryViewModel = nativeLibraryViewModel;
        this.threadDumpViewModel = threadDumpViewModel;
        this.jfrMetadataViewModel = jfrMetadataViewModel;
        this.g1GcViewModel = g1GcViewModel;
        this.javaFxEventsViewModel = javaFxEventsViewModel;
        this.advancedJfrViewModel = advancedJfrViewModel;
        this.aiAssistantViewModel = aiAssistantViewModel;
    }

    public String id() {
        return id;
    }

    public RecordingSummary recording() {
        return recording;
    }

    public LiveFlightRecordingOrigin liveOrigin() {
        return liveOrigin;
    }

    public StringProperty selectedSectionProperty() {
        return selectedSection;
    }

    public ObjectProperty<RecordingTimeRange> selectedTimeRangeProperty() {
        return selectedTimeRange;
    }

    public boolean isSectionLoaded(String sectionId) {
        synchronized (loadedSections) {
            return loadedSections.contains(sectionId);
        }
    }

    public boolean markSectionLoading(String sectionId) {
        synchronized (loadedSections) {
            latestRequestedLoadSection = sectionId;
            if (loadedSections.contains(sectionId) || loadingSections.contains(sectionId)) {
                return false;
            }
            loadingSections.add(sectionId);
            return true;
        }
    }

    public void cancelPendingSectionLoads() {
        synchronized (loadedSections) {
            latestRequestedLoadSection = null;
        }
    }

    public RecordingPageLayoutState pageLayoutState(String sectionId) {
        RecordingPageDescriptor page = RecordingPageCatalog.page(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown recording page: " + sectionId));
        synchronized (pageLayoutStates) {
            return pageLayoutStates.computeIfAbsent(page.id(),
                    id -> new RecordingPageLayoutState(id, page.defaultSplitPosition(), ""));
        }
    }

    public void updatePageLayoutState(String sectionId, RecordingPageLayoutState state) {
        Objects.requireNonNull(state, "state");
        RecordingPageCatalog.page(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown recording page: " + sectionId));
        if (!Objects.equals(sectionId, state.sectionId())) {
            throw new IllegalArgumentException("state section does not match sectionId");
        }
        synchronized (pageLayoutStates) {
            pageLayoutStates.put(sectionId, state);
        }
    }

    public boolean shouldLoadSection(String sectionId) {
        synchronized (loadedSections) {
            return loadingSections.contains(sectionId)
                    && !loadedSections.contains(sectionId)
                    && Objects.equals(latestRequestedLoadSection, sectionId);
        }
    }

    public boolean markSectionLoaded(String sectionId) {
        synchronized (loadedSections) {
            loadingSections.remove(sectionId);
            loadedSections.add(sectionId);
            return !loadingSections.isEmpty();
        }
    }

    public boolean markSectionLoadSkipped(String sectionId) {
        synchronized (loadedSections) {
            loadingSections.remove(sectionId);
            return !loadingSections.isEmpty();
        }
    }

    public boolean markSectionLoadFailed(String sectionId) {
        synchronized (loadedSections) {
            loadingSections.remove(sectionId);
            return !loadingSections.isEmpty();
        }
    }

    public OverviewViewModel overviewViewModel() {
        return overviewViewModel;
    }

    public EventBrowserViewModel eventBrowserViewModel() {
        return eventBrowserViewModel;
    }

    public RuleResultsViewModel ruleResultsViewModel() {
        return ruleResultsViewModel;
    }

    public ProfilingViewModel profilingViewModel() {
        return profilingViewModel;
    }

    public ExceptionViewModel exceptionViewModel() {
        return exceptionViewModel;
    }

    public ThreadViewModel threadViewModel() {
        return threadViewModel;
    }

    public FileIOViewModel fileIOViewModel() {
        return fileIOViewModel;
    }

    public SocketIOViewModel socketIOViewModel() {
        return socketIOViewModel;
    }

    public LockViewModel lockViewModel() {
        return lockViewModel;
    }

    public HeapViewModel heapViewModel() {
        return heapViewModel;
    }

    public LeakSuspectsViewModel leakSuspectsViewModel() {
        return leakSuspectsViewModel;
    }

    public TlabViewModel tlabViewModel() {
        return tlabViewModel;
    }

    public JvmInfoViewModel jvmInfoViewModel() {
        return jvmInfoViewModel;
    }

    public GcConfigViewModel gcConfigViewModel() {
        return gcConfigViewModel;
    }

    public GcSummaryViewModel gcSummaryViewModel() {
        return gcSummaryViewModel;
    }

    public GcDetailsViewModel gcDetailsViewModel() {
        return gcDetailsViewModel;
    }

    public CompilationsViewModel compilationsViewModel() {
        return compilationsViewModel;
    }

    public CodeCacheViewModel codeCacheViewModel() {
        return codeCacheViewModel;
    }

    public ClassLoadingViewModel classLoadingViewModel() {
        return classLoadingViewModel;
    }

    public VmOperationsViewModel vmOperationsViewModel() {
        return vmOperationsViewModel;
    }

    public EnvironmentViewModel environmentViewModel() {
        return environmentViewModel;
    }

    public JavaAppOverviewViewModel javaAppOverviewViewModel() {
        return javaAppOverviewViewModel;
    }

    public SecurityViewModel securityViewModel() {
        return securityViewModel;
    }

    public NativeLibraryViewModel nativeLibraryViewModel() {
        return nativeLibraryViewModel;
    }

    public ThreadDumpViewModel threadDumpViewModel() {
        return threadDumpViewModel;
    }

    public JfrMetadataViewModel jfrMetadataViewModel() {
        return jfrMetadataViewModel;
    }

    public G1GcViewModel g1GcViewModel() {
        return g1GcViewModel;
    }

    public JavaFxEventsViewModel javaFxEventsViewModel() {
        return javaFxEventsViewModel;
    }

    public AdvancedJfrViewModel advancedJfrViewModel() {
        return advancedJfrViewModel;
    }

    public RecordingAiAssistantViewModel aiAssistantViewModel() {
        return aiAssistantViewModel;
    }

    public void close() {
        eventBrowserViewModel.close();
        if (aiAssistantViewModel != null) {
            aiAssistantViewModel.close();
        }
    }
}
