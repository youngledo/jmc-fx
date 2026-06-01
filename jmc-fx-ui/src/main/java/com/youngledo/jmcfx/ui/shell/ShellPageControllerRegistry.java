package com.youngledo.jmcfx.ui.shell;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.ui.advanced.AdvancedJfrPageController;
import com.youngledo.jmcfx.ui.analysis.AnalysisPageController;
import com.youngledo.jmcfx.ui.environment.EnvironmentPagesController;
import com.youngledo.jmcfx.ui.events.EventsPageController;
import com.youngledo.jmcfx.ui.exceptions.ExceptionsPageController;
import com.youngledo.jmcfx.ui.fileio.FileIoPageController;
import com.youngledo.jmcfx.ui.gc.G1GcPageController;
import com.youngledo.jmcfx.ui.heap.HeapPageController;
import com.youngledo.jmcfx.ui.heapdump.HeapDumpAnalysisPageController;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.javaapp.JavaApplicationDataPagesController;
import com.youngledo.jmcfx.ui.jfx.JavaFxEventsPageController;
import com.youngledo.jmcfx.ui.jvm.JvmInternalsPagesController;
import com.youngledo.jmcfx.ui.leaks.LeakSuspectsPageController;
import com.youngledo.jmcfx.ui.locks.LocksPageController;
import com.youngledo.jmcfx.ui.metadata.MetadataPageController;
import com.youngledo.jmcfx.ui.overview.OverviewPageController;
import com.youngledo.jmcfx.ui.profiling.ProfilingPageController;
import com.youngledo.jmcfx.ui.socketio.SocketIoPageController;
import com.youngledo.jmcfx.ui.threads.ThreadsPageController;
import com.youngledo.jmcfx.ui.tlab.TlabPageController;

final class ShellPageControllerRegistry {

    private final AppShellView view;
    private final AppShellViewModel viewModel;
    private final I18n i18n;
    private OverviewPageController overviewPageController;
    private AnalysisPageController analysisPageController;
    private EventsPageController eventsPageController;
    private MetadataPageController metadataPageController;
    private AdvancedJfrPageController advancedJfrPageController;
    private HeapDumpAnalysisPageController heapDumpAnalysisPageController;
    private ProfilingPageController profilingPageController;
    private ExceptionsPageController exceptionsPageController;
    private ThreadsPageController threadsPageController;
    private JavaApplicationDataPagesController javaApplicationDataPagesController;
    private FileIoPageController fileIoPageController;
    private SocketIoPageController socketIoPageController;
    private LocksPageController locksPageController;
    private HeapPageController heapPageController;
    private LeakSuspectsPageController leakSuspectsPageController;
    private TlabPageController tlabPageController;
    private JvmInternalsPagesController jvmInternalsPagesController;
    private G1GcPageController g1GcPageController;
    private JavaFxEventsPageController javaFxEventsPageController;
    private EnvironmentPagesController environmentPagesController;
    private RecordingOverviewPagesController recordingOverviewPagesController;

    ShellPageControllerRegistry(AppShellView view, AppShellViewModel viewModel, I18n i18n) {
        this.view = view;
        this.viewModel = viewModel;
        this.i18n = i18n;
    }

    void configure() {
        recordingOverviewPagesController = new RecordingOverviewPagesController(view.recordingOverviewPages(), viewModel, i18n);
        recordingOverviewPagesController.configure();
        overviewPageController = new OverviewPageController(view.overviewPage(), i18n);
        overviewPageController.configure();
        analysisPageController = new AnalysisPageController(view.analysisPage(), i18n, viewModel::showSection);
        analysisPageController.configure();
        eventsPageController = new EventsPageController(view.eventsPage(), i18n);
        eventsPageController.configure();
        metadataPageController = new MetadataPageController(view.metadataPage(), i18n);
        metadataPageController.configure();
        advancedJfrPageController = new AdvancedJfrPageController(view.advancedJfrPage(), i18n);
        advancedJfrPageController.configure();
        heapDumpAnalysisPageController = new HeapDumpAnalysisPageController(view.heapDumpAnalysisPage(), i18n);
        heapDumpAnalysisPageController.configure();
        profilingPageController = new ProfilingPageController(view.profilingPage(), i18n);
        profilingPageController.configure();
        exceptionsPageController = new ExceptionsPageController(view.exceptionsPage(), i18n);
        exceptionsPageController.configure();
        threadsPageController = new ThreadsPageController(view.threadsPage(), i18n);
        threadsPageController.configure();
        javaApplicationDataPagesController = new JavaApplicationDataPagesController(view.javaApplicationDataPages(), i18n);
        javaApplicationDataPagesController.configure();
        fileIoPageController = new FileIoPageController(view.fileIoPage(), i18n);
        fileIoPageController.configure();
        socketIoPageController = new SocketIoPageController(view.socketIoPage(), i18n);
        socketIoPageController.configure();
        locksPageController = new LocksPageController(view.locksPage(), i18n);
        locksPageController.configure();
        heapPageController = new HeapPageController(view.heapPage(), i18n);
        heapPageController.configure();
        leakSuspectsPageController = new LeakSuspectsPageController(view.leakSuspectsPage(), i18n);
        leakSuspectsPageController.configure();
        tlabPageController = new TlabPageController(view.tlabPage(), i18n);
        tlabPageController.configure();
        jvmInternalsPagesController = new JvmInternalsPagesController(view.jvmInternalsPages(), i18n);
        jvmInternalsPagesController.configure();
        g1GcPageController = new G1GcPageController(view.g1GcPage(), i18n);
        g1GcPageController.configure();
        javaFxEventsPageController = new JavaFxEventsPageController(view.javaFxEventsPage(), i18n);
        javaFxEventsPageController.configure();
        environmentPagesController = new EnvironmentPagesController(view.environmentPages(), i18n);
        environmentPagesController.configure();
    }

    WorkspacePageControllers workspacePageControllers() {
        return new WorkspacePageControllers(overviewPageController, eventsPageController, analysisPageController,
                profilingPageController, exceptionsPageController, threadsPageController, fileIoPageController,
                socketIoPageController, locksPageController, javaApplicationDataPagesController, heapPageController,
                leakSuspectsPageController, tlabPageController, jvmInternalsPagesController, g1GcPageController,
                javaFxEventsPageController, environmentPagesController, metadataPageController,
                advancedJfrPageController, heapDumpAnalysisPageController);
    }

    void installExportMenus(ExportMenuInstaller installer) {
        installer.install(analysisPageController.table());
        installer.install(profilingPageController.table());
        installer.install(exceptionsPageController.table());
        installer.install(threadsPageController.table());
        fileIoPageController.exportTables().forEach(installer::install);
        socketIoPageController.exportTables().forEach(installer::install);
        locksPageController.exportTables().forEach(installer::install);
        javaApplicationDataPagesController.exportTables().forEach(installer::install);
        installer.install(heapPageController.table());
        installer.install(leakSuspectsPageController.table());
        installer.install(tlabPageController.table());
        jvmInternalsPagesController.exportTables().forEach(installer::install);
        g1GcPageController.exportTables().forEach(installer::install);
        javaFxEventsPageController.exportTables().forEach(installer::install);
        environmentPagesController.exportTables().forEach(installer::install);
    }

    void refreshOverviewLocale() {
        overviewPageController.refreshLocale();
    }

    String formatRecordingDetails(RecordingSummary recording) {
        return overviewPageController.formatRecordingDetails(recording);
    }
}
