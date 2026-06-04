package com.youngledo.jmcfx.ui.shell;

import java.nio.file.Path;

import com.youngledo.jmcfx.application.OpenRecordingWorkspaceUseCase;
import com.youngledo.jmcfx.application.RecordingApplicationServices;
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

    private final RecordingApplicationServices services;
    private final OpenRecordingWorkspaceUseCase openRecordingWorkspace;
    private final I18n i18n;

    RecordingWorkspaceFactory(RecordingApplicationServices services, I18n i18n) {
        this.services = services;
        this.openRecordingWorkspace = new OpenRecordingWorkspaceUseCase(services);
        this.i18n = i18n;
    }

    PreparedRecordingWorkspace prepare(Path path) {
        RecordingWorkspacePlan plan = openRecordingWorkspace.open(path);
        OverviewViewModel overview = new OverviewViewModel();
        EventBrowserViewModel events = new EventBrowserViewModel(services.eventQueryService(),
                new VirtualThreadEventBrowserExecutor(), i18n);
        RuleResultsViewModel analysis = new RuleResultsViewModel(services.ruleAnalysisService());
        ProfilingViewModel profiling = plan.hasProfiling()
                ? new ProfilingViewModel(services.profilingService()) : null;
        ExceptionViewModel exceptions = plan.hasExceptions()
                ? new ExceptionViewModel(services.exceptionService()) : null;
        ThreadViewModel threads = plan.hasThreads() ? new ThreadViewModel(services.threadService()) : null;
        FileIOViewModel fileio = plan.hasFileIO() ? new FileIOViewModel(services.fileIOService()) : null;
        SocketIOViewModel socketio = plan.hasSocketIO()
                ? new SocketIOViewModel(services.socketIOService()) : null;
        LockViewModel locks = plan.hasLocks() ? new LockViewModel(services.lockService()) : null;
        HeapViewModel heap = plan.hasHeap() ? new HeapViewModel(services.heapService()) : null;
        LeakSuspectsViewModel leakSuspects = plan.hasLeakSuspects()
                ? new LeakSuspectsViewModel(services.leakSuspectsService()) : null;
        TlabViewModel tlab = plan.hasTlab() ? new TlabViewModel(services.tlabService()) : null;
        JvmInfoViewModel jvmInfo = plan.hasJvmInternals()
                ? new JvmInfoViewModel(services.jvmInternalsService()) : null;
        GcConfigViewModel gcConfig = plan.hasJvmInternals()
                ? new GcConfigViewModel(services.jvmInternalsService()) : null;
        GcSummaryViewModel gcSummary = plan.hasJvmInternals()
                ? new GcSummaryViewModel(services.jvmInternalsService()) : null;
        GcDetailsViewModel gcDetails = plan.hasJvmInternals()
                ? new GcDetailsViewModel(services.jvmInternalsService()) : null;
        G1GcViewModel g1Gc = plan.hasG1Gc() ? new G1GcViewModel(services.g1GcService()) : null;
        JavaFxEventsViewModel javaFxEvents = plan.hasJavaFxEvents()
                ? new JavaFxEventsViewModel(services.javaFxEventService()) : null;
        CompilationsViewModel compilationsVm = plan.hasJvmInternals()
                ? new CompilationsViewModel(services.jvmInternalsService()) : null;
        CodeCacheViewModel codeCache = plan.hasJvmInternals()
                ? new CodeCacheViewModel(services.jvmInternalsService()) : null;
        ClassLoadingViewModel classLoading = plan.hasJvmInternals()
                ? new ClassLoadingViewModel(services.jvmInternalsService()) : null;
        VmOperationsViewModel vmOperations = plan.hasJvmInternals()
                ? new VmOperationsViewModel(services.jvmInternalsService()) : null;
        EnvironmentViewModel environment = plan.hasEnvironment()
                ? new EnvironmentViewModel(services.environmentService()) : null;
        JavaAppOverviewViewModel javaAppOverview = plan.hasJavaApplication()
                ? new JavaAppOverviewViewModel(services.javaAppService()) : null;
        SecurityViewModel security = plan.hasJavaApplication()
                ? new SecurityViewModel(services.javaAppService()) : null;
        NativeLibraryViewModel nativeLibraries = plan.hasJavaApplication()
                ? new NativeLibraryViewModel(services.javaAppService()) : null;
        ThreadDumpViewModel threadDumps = plan.hasJavaApplication()
                ? new ThreadDumpViewModel(services.javaAppService()) : null;
        JfrMetadataViewModel metadata = plan.hasMetadata()
                ? new JfrMetadataViewModel(services.jfrMetadataService()) : null;
        AdvancedJfrViewModel advancedJfr = plan.hasAdvancedJfrAnalysis()
                ? new AdvancedJfrViewModel(services.advancedJfrAnalysisService()) : null;
        return new PreparedRecordingWorkspace(plan.recording(), overview, events, analysis, profiling, exceptions, threads,
                fileio, socketio, locks, heap, leakSuspects, tlab, jvmInfo, gcConfig, gcSummary, gcDetails,
                compilationsVm, codeCache, classLoading, vmOperations, environment, javaAppOverview, security,
                nativeLibraries, threadDumps, metadata, g1Gc, javaFxEvents, advancedJfr);
    }
}
