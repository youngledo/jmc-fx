package io.github.youngledo.jmcfx.ui.shell;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.ui.analysis.RecordingAiAssistantViewModel;
import io.github.youngledo.jmcfx.ui.advanced.AdvancedJfrViewModel;
import io.github.youngledo.jmcfx.ui.environment.EnvironmentViewModel;
import io.github.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import io.github.youngledo.jmcfx.ui.exceptions.ExceptionViewModel;
import io.github.youngledo.jmcfx.ui.fileio.FileIOViewModel;
import io.github.youngledo.jmcfx.ui.gc.G1GcViewModel;
import io.github.youngledo.jmcfx.ui.heap.HeapViewModel;
import io.github.youngledo.jmcfx.ui.javaapp.JavaAppOverviewViewModel;
import io.github.youngledo.jmcfx.ui.javaapp.NativeLibraryViewModel;
import io.github.youngledo.jmcfx.ui.javaapp.SecurityViewModel;
import io.github.youngledo.jmcfx.ui.javaapp.ThreadDumpViewModel;
import io.github.youngledo.jmcfx.ui.jfx.JavaFxEventsViewModel;
import io.github.youngledo.jmcfx.ui.jvms.LiveFlightRecordingOrigin;
import io.github.youngledo.jmcfx.ui.jvm.ClassLoadingViewModel;
import io.github.youngledo.jmcfx.ui.jvm.CodeCacheViewModel;
import io.github.youngledo.jmcfx.ui.jvm.CompilationsViewModel;
import io.github.youngledo.jmcfx.ui.jvm.GcConfigViewModel;
import io.github.youngledo.jmcfx.ui.jvm.GcDetailsViewModel;
import io.github.youngledo.jmcfx.ui.jvm.GcSummaryViewModel;
import io.github.youngledo.jmcfx.ui.jvm.JvmInfoViewModel;
import io.github.youngledo.jmcfx.ui.jvm.VmOperationsViewModel;
import io.github.youngledo.jmcfx.ui.leaks.LeakSuspectsViewModel;
import io.github.youngledo.jmcfx.ui.locks.LockViewModel;
import io.github.youngledo.jmcfx.ui.metadata.JfrMetadataViewModel;
import io.github.youngledo.jmcfx.ui.overview.OverviewViewModel;
import io.github.youngledo.jmcfx.ui.profiling.ProfilingViewModel;
import io.github.youngledo.jmcfx.ui.rules.RuleResultsViewModel;
import io.github.youngledo.jmcfx.ui.socketio.SocketIOViewModel;
import io.github.youngledo.jmcfx.ui.threads.ThreadViewModel;
import io.github.youngledo.jmcfx.ui.tlab.TlabViewModel;

record PreparedRecordingWorkspace(
        RecordingSummary recording,
        OverviewViewModel overview,
        EventBrowserViewModel events,
        RuleResultsViewModel analysis,
        ProfilingViewModel profiling,
        ExceptionViewModel exceptions,
        ThreadViewModel threads,
        FileIOViewModel fileio,
        SocketIOViewModel socketio,
        LockViewModel locks,
        HeapViewModel heap,
        LeakSuspectsViewModel leakSuspects,
        TlabViewModel tlab,
        JvmInfoViewModel jvmInfo,
        GcConfigViewModel gcConfig,
        GcSummaryViewModel gcSummary,
        GcDetailsViewModel gcDetails,
        CompilationsViewModel compilations,
        CodeCacheViewModel codeCache,
        ClassLoadingViewModel classLoading,
        VmOperationsViewModel vmOperations,
        EnvironmentViewModel environment,
        JavaAppOverviewViewModel javaAppOverview,
        SecurityViewModel security,
        NativeLibraryViewModel nativeLibraries,
        ThreadDumpViewModel threadDumps,
        JfrMetadataViewModel metadata,
        G1GcViewModel g1Gc,
        JavaFxEventsViewModel javaFxEvents,
        AdvancedJfrViewModel advancedJfr,
        RecordingAiAssistantViewModel aiAssistant,
        LiveFlightRecordingOrigin liveOrigin) {

    PreparedRecordingWorkspace withLiveOrigin(LiveFlightRecordingOrigin origin) {
        if (origin == null) {
            return this;
        }
        return new PreparedRecordingWorkspace(recording, overview, events, analysis, profiling, exceptions, threads,
                fileio, socketio, locks, heap, leakSuspects, tlab, jvmInfo, gcConfig, gcSummary, gcDetails,
                compilations, codeCache, classLoading, vmOperations, environment, javaAppOverview, security,
                nativeLibraries, threadDumps, metadata, g1Gc, javaFxEvents, advancedJfr, aiAssistant, origin);
    }
}
