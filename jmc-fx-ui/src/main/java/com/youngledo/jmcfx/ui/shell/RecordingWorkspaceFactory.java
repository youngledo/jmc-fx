package com.youngledo.jmcfx.ui.shell;

import java.nio.file.Path;

import com.youngledo.jmcfx.application.RecordingApplicationServices;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
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
    private final I18n i18n;

    RecordingWorkspaceFactory(RecordingApplicationServices services, I18n i18n) {
        this.services = services;
        this.i18n = i18n;
    }

    PreparedRecordingWorkspace prepare(Path path) {
        RecordingSummary recording = services.recordingRepository().open(path);
        OverviewViewModel overview = new OverviewViewModel();
        EventBrowserViewModel events = new EventBrowserViewModel(services.eventQueryService(),
                new VirtualThreadEventBrowserExecutor(), i18n);
        RuleResultsViewModel analysis = new RuleResultsViewModel(services.ruleAnalysisService());
        ProfilingViewModel profiling = services.profilingService() != null
                ? new ProfilingViewModel(services.profilingService()) : null;
        ExceptionViewModel exceptions = services.exceptionService() != null
                ? new ExceptionViewModel(services.exceptionService()) : null;
        ThreadViewModel threads = services.threadService() != null ? new ThreadViewModel(services.threadService()) : null;
        FileIOViewModel fileio = services.fileIOService() != null ? new FileIOViewModel(services.fileIOService()) : null;
        SocketIOViewModel socketio = services.socketIOService() != null
                ? new SocketIOViewModel(services.socketIOService()) : null;
        LockViewModel locks = services.lockService() != null ? new LockViewModel(services.lockService()) : null;
        HeapViewModel heap = services.heapService() != null ? new HeapViewModel(services.heapService()) : null;
        LeakSuspectsViewModel leakSuspects = services.leakSuspectsService() != null
                ? new LeakSuspectsViewModel(services.leakSuspectsService()) : null;
        TlabViewModel tlab = services.tlabService() != null ? new TlabViewModel(services.tlabService()) : null;
        JvmInfoViewModel jvmInfo = services.jvmInternalsService() != null
                ? new JvmInfoViewModel(services.jvmInternalsService()) : null;
        GcConfigViewModel gcConfig = services.jvmInternalsService() != null
                ? new GcConfigViewModel(services.jvmInternalsService()) : null;
        GcSummaryViewModel gcSummary = services.jvmInternalsService() != null
                ? new GcSummaryViewModel(services.jvmInternalsService()) : null;
        GcDetailsViewModel gcDetails = services.jvmInternalsService() != null
                ? new GcDetailsViewModel(services.jvmInternalsService()) : null;
        G1GcViewModel g1Gc = services.g1GcService() != null ? new G1GcViewModel(services.g1GcService()) : null;
        JavaFxEventsViewModel javaFxEvents = services.javaFxEventService() != null
                ? new JavaFxEventsViewModel(services.javaFxEventService()) : null;
        CompilationsViewModel compilationsVm = services.jvmInternalsService() != null
                ? new CompilationsViewModel(services.jvmInternalsService()) : null;
        CodeCacheViewModel codeCache = services.jvmInternalsService() != null
                ? new CodeCacheViewModel(services.jvmInternalsService()) : null;
        ClassLoadingViewModel classLoading = services.jvmInternalsService() != null
                ? new ClassLoadingViewModel(services.jvmInternalsService()) : null;
        VmOperationsViewModel vmOperations = services.jvmInternalsService() != null
                ? new VmOperationsViewModel(services.jvmInternalsService()) : null;
        EnvironmentViewModel environment = services.environmentService() != null
                ? new EnvironmentViewModel(services.environmentService()) : null;
        JavaAppOverviewViewModel javaAppOverview = services.javaAppService() != null
                ? new JavaAppOverviewViewModel(services.javaAppService()) : null;
        SecurityViewModel security = services.javaAppService() != null
                ? new SecurityViewModel(services.javaAppService()) : null;
        NativeLibraryViewModel nativeLibraries = services.javaAppService() != null
                ? new NativeLibraryViewModel(services.javaAppService()) : null;
        ThreadDumpViewModel threadDumps = services.javaAppService() != null
                ? new ThreadDumpViewModel(services.javaAppService()) : null;
        JfrMetadataViewModel metadata = services.jfrMetadataService() != null
                ? new JfrMetadataViewModel(services.jfrMetadataService()) : null;
        AdvancedJfrViewModel advancedJfr = services.advancedJfrAnalysisService() != null
                ? new AdvancedJfrViewModel(services.advancedJfrAnalysisService()) : null;
        return new PreparedRecordingWorkspace(recording, overview, events, analysis, profiling, exceptions, threads,
                fileio, socketio, locks, heap, leakSuspects, tlab, jvmInfo, gcConfig, gcSummary, gcDetails,
                compilationsVm, codeCache, classLoading, vmOperations, environment, javaAppOverview, security,
                nativeLibraries, threadDumps, metadata, g1Gc, javaFxEvents, advancedJfr);
    }
}
