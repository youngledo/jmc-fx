package com.youngledo.jmcfx.ui.shell;

import java.nio.file.Path;
import java.util.Objects;

import com.youngledo.jmcfx.application.RecordingPageUseCases;
import com.youngledo.jmcfx.application.RecordingWorkspacePlan;
import com.youngledo.jmcfx.ui.advanced.AdvancedJfrViewModel;
import com.youngledo.jmcfx.ui.environment.EnvironmentViewModel;
import com.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import com.youngledo.jmcfx.ui.events.VirtualThreadEventBrowserExecutor;
import com.youngledo.jmcfx.ui.exceptions.ExceptionViewModel;
import com.youngledo.jmcfx.ui.fileio.FileIOViewModel;
import com.youngledo.jmcfx.ui.gc.G1GcViewModel;
import com.youngledo.jmcfx.ui.heap.HeapViewModel;
import com.youngledo.jmcfx.ui.i18n.I18n;
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
import com.youngledo.jmcfx.ui.profiling.ProfilingViewModel;
import com.youngledo.jmcfx.ui.rules.RuleResultsViewModel;
import com.youngledo.jmcfx.ui.socketio.SocketIOViewModel;
import com.youngledo.jmcfx.ui.threads.ThreadViewModel;
import com.youngledo.jmcfx.ui.tlab.TlabViewModel;

final class RecordingWorkspaceFactory {

    private final RecordingPageUseCases useCases;
    private final I18n i18n;

    RecordingWorkspaceFactory(RecordingPageUseCases useCases, I18n i18n) {
        this.useCases = Objects.requireNonNull(useCases, "useCases");
        this.i18n = i18n;
    }

    PreparedRecordingWorkspace prepare(Path path) {
        RecordingWorkspacePlan plan = useCases.openRecordingWorkspace().open(path);
        OverviewViewModel overview = new OverviewViewModel();
        EventBrowserViewModel events = new EventBrowserViewModel(useCases.browseEvents(),
                new VirtualThreadEventBrowserExecutor(), i18n);
        RuleResultsViewModel analysis = new RuleResultsViewModel(useCases.analyzeRules());
        ProfilingViewModel profiling = plan.hasProfiling()
                ? new ProfilingViewModel(useCases.profiling()) : null;
        ExceptionViewModel exceptions = plan.hasExceptions()
                ? new ExceptionViewModel(useCases.exceptions()) : null;
        ThreadViewModel threads = plan.hasThreads() ? new ThreadViewModel(useCases.threads()) : null;
        FileIOViewModel fileio = plan.hasFileIO() ? new FileIOViewModel(useCases.fileIO()) : null;
        SocketIOViewModel socketio = plan.hasSocketIO()
                ? new SocketIOViewModel(useCases.socketIO()) : null;
        LockViewModel locks = plan.hasLocks() ? new LockViewModel(useCases.locks()) : null;
        HeapViewModel heap = plan.hasHeap() ? new HeapViewModel(useCases.heap()) : null;
        LeakSuspectsViewModel leakSuspects = plan.hasLeakSuspects()
                ? new LeakSuspectsViewModel(useCases.leakSuspects()) : null;
        TlabViewModel tlab = plan.hasTlab() ? new TlabViewModel(useCases.tlab()) : null;
        JvmInfoViewModel jvmInfo = plan.hasJvmInternals()
                ? new JvmInfoViewModel(useCases.jvmInternals()) : null;
        GcConfigViewModel gcConfig = plan.hasJvmInternals()
                ? new GcConfigViewModel(useCases.jvmInternals()) : null;
        GcSummaryViewModel gcSummary = plan.hasJvmInternals()
                ? new GcSummaryViewModel(useCases.jvmInternals()) : null;
        GcDetailsViewModel gcDetails = plan.hasJvmInternals()
                ? new GcDetailsViewModel(useCases.jvmInternals()) : null;
        G1GcViewModel g1Gc = plan.hasG1Gc() ? new G1GcViewModel(useCases.g1Gc()) : null;
        JavaFxEventsViewModel javaFxEvents = plan.hasJavaFxEvents()
                ? new JavaFxEventsViewModel(useCases.javaFxEvents()) : null;
        CompilationsViewModel compilationsVm = plan.hasJvmInternals()
                ? new CompilationsViewModel(useCases.jvmInternals()) : null;
        CodeCacheViewModel codeCache = plan.hasJvmInternals()
                ? new CodeCacheViewModel(useCases.jvmInternals()) : null;
        ClassLoadingViewModel classLoading = plan.hasJvmInternals()
                ? new ClassLoadingViewModel(useCases.jvmInternals()) : null;
        VmOperationsViewModel vmOperations = plan.hasJvmInternals()
                ? new VmOperationsViewModel(useCases.jvmInternals()) : null;
        EnvironmentViewModel environment = plan.hasEnvironment()
                ? new EnvironmentViewModel(useCases.environment()) : null;
        JavaAppOverviewViewModel javaAppOverview = plan.hasJavaApplication()
                ? new JavaAppOverviewViewModel(useCases.javaApplication()) : null;
        SecurityViewModel security = plan.hasJavaApplication()
                ? new SecurityViewModel(useCases.javaApplication()) : null;
        NativeLibraryViewModel nativeLibraries = plan.hasJavaApplication()
                ? new NativeLibraryViewModel(useCases.javaApplication()) : null;
        ThreadDumpViewModel threadDumps = plan.hasJavaApplication()
                ? new ThreadDumpViewModel(useCases.javaApplication()) : null;
        JfrMetadataViewModel metadata = plan.hasMetadata()
                ? new JfrMetadataViewModel(useCases.jfrMetadata()) : null;
        AdvancedJfrViewModel advancedJfr = plan.hasAdvancedJfrAnalysis()
                ? new AdvancedJfrViewModel(useCases.advancedJfr()) : null;
        return new PreparedRecordingWorkspace(plan.recording(), overview, events, analysis, profiling, exceptions, threads,
                fileio, socketio, locks, heap, leakSuspects, tlab, jvmInfo, gcConfig, gcSummary, gcDetails,
                compilationsVm, codeCache, classLoading, vmOperations, environment, javaAppOverview, security,
                nativeLibraries, threadDumps, metadata, g1Gc, javaFxEvents, advancedJfr);
    }
}
