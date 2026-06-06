package io.github.youngledo.jmcfx.ui.shell;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.ui.advanced.AdvancedJfrPageController;
import io.github.youngledo.jmcfx.ui.analysis.AnalysisPageController;
import io.github.youngledo.jmcfx.ui.environment.EnvironmentPagesController;
import io.github.youngledo.jmcfx.ui.events.EventsPageController;
import io.github.youngledo.jmcfx.ui.exceptions.ExceptionsPageController;
import io.github.youngledo.jmcfx.ui.fileio.FileIoPageController;
import io.github.youngledo.jmcfx.ui.gc.G1GcPageController;
import io.github.youngledo.jmcfx.ui.heap.HeapPageController;
import io.github.youngledo.jmcfx.ui.heapdump.HeapDumpAnalysisPageController;
import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.javaapp.JavaApplicationDataPagesController;
import io.github.youngledo.jmcfx.ui.jfx.JavaFxEventsPageController;
import io.github.youngledo.jmcfx.ui.jvm.JvmInternalsPagesController;
import io.github.youngledo.jmcfx.ui.leaks.LeakSuspectsPageController;
import io.github.youngledo.jmcfx.ui.locks.LocksPageController;
import io.github.youngledo.jmcfx.ui.metadata.MetadataPageController;
import io.github.youngledo.jmcfx.ui.overview.OverviewPageController;
import io.github.youngledo.jmcfx.ui.profiling.ProfilingPageController;
import io.github.youngledo.jmcfx.ui.socketio.SocketIoPageController;
import io.github.youngledo.jmcfx.ui.threads.ThreadsPageController;
import io.github.youngledo.jmcfx.ui.tlab.TlabPageController;
import io.github.youngledo.jmcfx.ui.util.TableExportRequests;

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
        installJfrExport(installer, analysisPageController.table(), "Automated Analysis", "Rule Results");
        installJfrExport(installer, profilingPageController.table(), "Method Profiling", "Hot Methods");
        installJfrExport(installer, exceptionsPageController.table(), "Exception Events", "Exception Histogram");
        installJfrExport(installer, threadsPageController.table(), "Thread Activity", "Thread Summary");
        installJfrExports(installer, "File I/O", fileIoPageController.exportTables(),
                "File I/O Histogram", "File I/O Events");
        installJfrExports(installer, "Socket I/O", socketIoPageController.exportTables(),
                "Socket I/O Histogram", "Socket I/O Events");
        installJfrExports(installer, "Locks", locksPageController.exportTables(),
                "Locks By Class", "Locks By Address", "Locks By Thread");
        installJfrExports(installer, "Java Application", javaApplicationDataPagesController.exportTables(),
                "Thread Histogram", "Security Certificates", "Native Libraries", "Thread Dumps");
        installJfrExport(installer, heapPageController.table(), "Heap", "Class Histogram");
        installJfrExport(installer, leakSuspectsPageController.table(), "Leak Suspects", "Leak Candidates");
        installJfrExport(installer, tlabPageController.table(), "TLAB Allocations", "TLAB Allocations");
        installJfrExports(installer, "JVM Internals", jvmInternalsPagesController.exportTables(),
                "JVM Flags", "JVM Flag Changes", "GC Events", "GC Reference Statistics", "GC Heap Summary",
                "Compilations", "Compilation Failures",
                "Code Cache Sweeps", "Code Cache Statistics", "Class Loading Histogram", "Class Loading Events",
                "Class Loading Statistics", "VM Operation Summary", "VM Operation Events");
        installJfrExports(installer, "G1 GC", g1GcPageController.exportTables(),
                "Region Summary", "Region States", "GC Pauses");
        installJfrExports(installer, "JavaFX Events", javaFxEventsPageController.exportTables(),
                "Pulse Phases", "Pulse Summary", "Input Events");
        installJfrExports(installer, "Environment", environmentPagesController.exportTables(),
                "Processes", "Environment Variables", "System Properties", "Recordings", "Active Settings",
                "Agents", "Constant Pools");
    }

    private static void installJfrExport(
            ExportMenuInstaller installer,
            javafx.scene.control.TableView<?> table,
            String page,
            String tableName) {
        installer.install(TableExportRequests.currentView(table, "JFR Recording", page, tableName, "TableView"));
    }

    private static void installJfrExports(
            ExportMenuInstaller installer,
            String page,
            java.util.List<javafx.scene.control.TableView<?>> tables,
            String... tableNames) {
        int count = Math.min(tableNames.length, tables.size());
        for (int index = 0; index < count; index++) {
            installJfrExport(installer, tables.get(index), page, tableNames[index]);
        }
    }

    void refreshOverviewLocale() {
        overviewPageController.refreshLocale();
    }

    String formatRecordingDetails(RecordingSummary recording) {
        return overviewPageController.formatRecordingDetails(recording);
    }
}
