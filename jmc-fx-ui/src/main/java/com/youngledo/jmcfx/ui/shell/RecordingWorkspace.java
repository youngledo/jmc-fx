package com.youngledo.jmcfx.ui.shell;

import java.util.UUID;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import com.youngledo.jmcfx.ui.environment.EnvironmentViewModel;
import com.youngledo.jmcfx.ui.exceptions.ExceptionViewModel;
import com.youngledo.jmcfx.ui.fileio.FileIOViewModel;
import com.youngledo.jmcfx.ui.heap.HeapViewModel;
import com.youngledo.jmcfx.ui.leaks.LeakSuspectsViewModel;
import com.youngledo.jmcfx.ui.locks.LockViewModel;
import com.youngledo.jmcfx.ui.overview.OverviewViewModel;
import com.youngledo.jmcfx.ui.profiling.ProfilingViewModel;
import com.youngledo.jmcfx.ui.rules.RuleResultsViewModel;
import com.youngledo.jmcfx.ui.socketio.SocketIOViewModel;
import com.youngledo.jmcfx.ui.jvm.ClassLoadingViewModel;
import com.youngledo.jmcfx.ui.jvm.CodeCacheViewModel;
import com.youngledo.jmcfx.ui.jvm.CompilationsViewModel;
import com.youngledo.jmcfx.ui.jvm.GcConfigViewModel;
import com.youngledo.jmcfx.ui.jvm.GcDetailsViewModel;
import com.youngledo.jmcfx.ui.jvm.GcSummaryViewModel;
import com.youngledo.jmcfx.ui.jvm.JvmInfoViewModel;
import com.youngledo.jmcfx.ui.javaapp.JavaAppOverviewViewModel;
import com.youngledo.jmcfx.ui.javaapp.NativeLibraryViewModel;
import com.youngledo.jmcfx.ui.javaapp.SecurityViewModel;
import com.youngledo.jmcfx.ui.javaapp.ThreadDumpViewModel;
import com.youngledo.jmcfx.ui.jvm.VmOperationsViewModel;
import com.youngledo.jmcfx.ui.tlab.TlabViewModel;
import com.youngledo.jmcfx.ui.threads.ThreadViewModel;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/// UI-layer state for one opened recording workspace.
public final class RecordingWorkspace {

    private final String id = UUID.randomUUID().toString();
    private final RecordingSummary recording;
    private final StringProperty selectedSection = new SimpleStringProperty("analysis");
    private final Set<String> loadedSections = new HashSet<>();
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
        this.recording = Objects.requireNonNull(recording, "recording");
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
    }

    public String id() {
        return id;
    }

    public RecordingSummary recording() {
        return recording;
    }

    public StringProperty selectedSectionProperty() {
        return selectedSection;
    }

    public boolean isSectionLoaded(String sectionId) {
        return loadedSections.contains(sectionId);
    }

    public void markSectionLoaded(String sectionId) {
        loadedSections.add(sectionId);
    }

    public boolean markSectionLoading(String sectionId) {
        return loadedSections.add(sectionId);
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

    public void close() {
        eventBrowserViewModel.close();
    }
}
