package io.github.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.application.AnalyzeRulesUseCase;
import io.github.youngledo.jmcfx.application.BrowseEventsUseCase;
import io.github.youngledo.jmcfx.application.DiagnosticFindingsUseCase;
import io.github.youngledo.jmcfx.application.LoadG1GcUseCase;
import io.github.youngledo.jmcfx.application.LoadJavaFxEventsUseCase;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.EventQueryService;
import io.github.youngledo.jmcfx.domain.service.EventQuerySession;
import io.github.youngledo.jmcfx.ui.testsupport.FakeG1GcService;
import io.github.youngledo.jmcfx.ui.testsupport.FakeJavaFxEventService;
import io.github.youngledo.jmcfx.ui.events.EventBrowserBackgroundExecutor;
import io.github.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import io.github.youngledo.jmcfx.ui.gc.G1GcViewModel;
import io.github.youngledo.jmcfx.ui.jfx.JavaFxEventsViewModel;
import io.github.youngledo.jmcfx.ui.overview.OverviewViewModel;
import io.github.youngledo.jmcfx.ui.preferences.AppTheme;
import io.github.youngledo.jmcfx.ui.rules.RuleResultsViewModel;

class AppShellViewModelTest {

    @Test
    void defaultsToHomeWithoutRecording() {
        AppShellViewModel viewModel = new AppShellViewModel();

        assertEquals("home", viewModel.selectedSectionProperty().get());
        assertEquals(AppWorkspaceKind.GLOBAL, viewModel.activeWorkspaceKindProperty().get());
        assertFalse(viewModel.recordingOpenProperty().get());
        assertEquals("", viewModel.currentRecordingNameProperty().get());
        assertEquals("", viewModel.statusMessageProperty().get());
        assertEquals("", viewModel.taskSummaryProperty().get());
        assertTrue(viewModel.recordingWorkspacesProperty().isEmpty());
        assertNull(viewModel.selectedWorkspaceProperty().get());
    }

    @Test
    void languageModeDefaultsToSystemAndCanChange() {
        AppShellViewModel viewModel = new AppShellViewModel();

        assertEquals(io.github.youngledo.jmcfx.ui.i18n.LanguageMode.SYSTEM,
                viewModel.languageModeProperty().get());

        viewModel.setLanguageMode(io.github.youngledo.jmcfx.ui.i18n.LanguageMode.CHINESE_SIMPLIFIED);

        assertEquals(io.github.youngledo.jmcfx.ui.i18n.LanguageMode.CHINESE_SIMPLIFIED,
                viewModel.languageModeProperty().get());
    }

    @Test
    void themeDefaultsToSystemAndCanChange() {
        AppShellViewModel viewModel = new AppShellViewModel();

        assertEquals(AppTheme.SYSTEM, viewModel.themeProperty().get());

        viewModel.setTheme(AppTheme.PRIMER_DARK);

        assertEquals(AppTheme.PRIMER_DARK, viewModel.themeProperty().get());
    }

    @Test
    void openingRecordingEntersAnalysisWorkspace() {
        AppShellViewModel viewModel = new AppShellViewModel();

        RecordingWorkspace workspace = viewModel.openRecording(
                recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());

        assertTrue(viewModel.recordingOpenProperty().get());
        assertEquals(AppWorkspaceKind.RECORDING, viewModel.activeWorkspaceKindProperty().get());
        assertEquals("rec.jfr", viewModel.currentRecordingNameProperty().get());
        assertEquals("analysis", viewModel.selectedSectionProperty().get());
        assertEquals("", viewModel.statusMessageProperty().get());
        assertEquals("", viewModel.taskSummaryProperty().get());
        assertEquals(1, viewModel.recordingWorkspacesProperty().size());
        assertSame(workspace, viewModel.selectedWorkspaceProperty().get());
        assertEquals(recording(), workspace.recording());
        assertEquals("analysis", workspace.selectedSectionProperty().get());
        assertFalse(workspace.id().isBlank());
    }

    @Test
    void updatesStatusAndTaskSummary() {
        AppShellViewModel viewModel = new AppShellViewModel();

        viewModel.showStatus("Loading recording...");
        viewModel.showTaskSummary("Events ready: 64 event types");

        assertEquals("Loading recording...", viewModel.statusMessageProperty().get());
        assertEquals("Events ready: 64 event types", viewModel.taskSummaryProperty().get());
    }

    @Test
    void switchesSections() {
        AppShellViewModel viewModel = new AppShellViewModel();

        viewModel.showSection("jvms");

        assertEquals("jvms", viewModel.selectedSectionProperty().get());
        assertEquals(AppWorkspaceKind.LIVE_JVM, viewModel.activeWorkspaceKindProperty().get());
        assertEquals("JVM", viewModel.currentTargetNameProperty().get());
        assertTrue(viewModel.liveJvmWorkspaceOpenProperty().get());
        assertNotNull(viewModel.selectedLiveJvmWorkspaceProperty().get());
    }

    @Test
    void metadataIsARecordingSection() {
        AppShellViewModel viewModel = new AppShellViewModel();
        RecordingWorkspace workspace = viewModel.openRecording(
                recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());

        viewModel.showSection("metadata");

        assertEquals("metadata", viewModel.selectedSectionProperty().get());
        assertEquals("metadata", workspace.selectedSectionProperty().get());
        assertEquals(AppWorkspaceKind.RECORDING, viewModel.activeWorkspaceKindProperty().get());
    }

    @Test
    void recordingParentOverviewPagesAreRecordingSections() {
        AppShellViewModel viewModel = new AppShellViewModel();
        RecordingWorkspace workspace = viewModel.openRecording(
                recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());

        viewModel.showSection("javaApplication");
        assertEquals("javaApplication", viewModel.selectedSectionProperty().get());
        assertEquals("javaApplication", workspace.selectedSectionProperty().get());

        viewModel.showSection("jvmInternals");
        assertEquals("jvmInternals", viewModel.selectedSectionProperty().get());
        assertEquals("jvmInternals", workspace.selectedSectionProperty().get());

        viewModel.showSection("environment");
        assertEquals("environment", viewModel.selectedSectionProperty().get());
        assertEquals("environment", workspace.selectedSectionProperty().get());
        assertEquals(AppWorkspaceKind.RECORDING, viewModel.activeWorkspaceKindProperty().get());
    }

    @Test
    void jvmLauncherSwitchesFromExistingWorkspaceContext() {
        AppShellViewModel viewModel = new AppShellViewModel();
        viewModel.openRecording(recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());
        viewModel.showSection("home");

        viewModel.showSection("jvms");

        assertEquals("jvms", viewModel.selectedSectionProperty().get());
        assertEquals(AppWorkspaceKind.LIVE_JVM, viewModel.activeWorkspaceKindProperty().get());
        assertNull(viewModel.selectedWorkspaceProperty().get());
        assertNull(viewModel.selectedHeapDumpWorkspaceProperty().get());
        LiveJvmWorkspace liveJvmWorkspace = viewModel.selectedLiveJvmWorkspaceProperty().get();
        assertNotNull(liveJvmWorkspace);

        HeapDumpWorkspace heapDump = new HeapDumpWorkspace(Path.of("demo.hprof"), null);
        viewModel.openHeapDump(heapDump);
        viewModel.showSection("home");

        viewModel.showSection("jvms");

        assertEquals("jvms", viewModel.selectedSectionProperty().get());
        assertEquals(AppWorkspaceKind.LIVE_JVM, viewModel.activeWorkspaceKindProperty().get());
        assertNull(viewModel.selectedWorkspaceProperty().get());
        assertNull(viewModel.selectedHeapDumpWorkspaceProperty().get());
        assertSame(liveJvmWorkspace, viewModel.selectedLiveJvmWorkspaceProperty().get());
    }

    @Test
    void opensSelectsAndClosesLiveJvmWorkspace() {
        AppShellViewModel viewModel = new AppShellViewModel();

        viewModel.openLiveJvmWorkspace();

        LiveJvmWorkspace workspace = viewModel.selectedLiveJvmWorkspaceProperty().get();
        assertNotNull(workspace);
        assertTrue(viewModel.liveJvmWorkspaceOpenProperty().get());
        assertEquals("jvms", viewModel.selectedSectionProperty().get());
        assertEquals(AppWorkspaceKind.LIVE_JVM, viewModel.activeWorkspaceKindProperty().get());
        assertEquals("JVM", workspace.name());

        viewModel.openLiveJvmWorkspace();

        assertSame(workspace, viewModel.selectedLiveJvmWorkspaceProperty().get());

        viewModel.closeLiveJvmWorkspace();

        assertFalse(viewModel.liveJvmWorkspaceOpenProperty().get());
        assertNull(viewModel.liveJvmWorkspaceProperty().get());
        assertNull(viewModel.selectedLiveJvmWorkspaceProperty().get());
        assertEquals(AppWorkspaceKind.GLOBAL, viewModel.activeWorkspaceKindProperty().get());
        assertEquals("home", viewModel.selectedSectionProperty().get());
        assertEquals("", viewModel.currentTargetNameProperty().get());
    }

    @Test
    void liveJvmWorkspaceIsMarkedOpenBeforeWorkspaceTabsNotifyListeners() {
        AppShellViewModel viewModel = new AppShellViewModel();

        viewModel.workspaceTabsProperty().addListener((javafx.collections.ListChangeListener<Object>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    assertTrue(viewModel.liveJvmWorkspaceOpenProperty().get(),
                            "JVM tab listeners must see the workspace as open during rebuild");
                }
            }
        });

        viewModel.openLiveJvmWorkspace();
    }

    @Test
    void switchesToHeapDumpAnalysisWithoutRecording() {
        AppShellViewModel viewModel = new AppShellViewModel();

        viewModel.showSection("heapDumpAnalysis");

        assertEquals("home", viewModel.selectedSectionProperty().get());
        assertEquals(AppWorkspaceKind.GLOBAL, viewModel.activeWorkspaceKindProperty().get());
    }

    @Test
    void opensAndClosesHeapDumpWorkspace() {
        AppShellViewModel viewModel = new AppShellViewModel();
        HeapDumpWorkspace workspace = new HeapDumpWorkspace(Path.of("demo.hprof"), null);

        viewModel.openHeapDump(workspace);

        assertEquals(List.of(workspace), viewModel.heapDumpWorkspacesProperty());
        assertSame(workspace, viewModel.selectedHeapDumpWorkspaceProperty().get());
        assertEquals("heapDumpAnalysis", viewModel.selectedSectionProperty().get());
        assertEquals(AppWorkspaceKind.HEAP_DUMP, viewModel.activeWorkspaceKindProperty().get());

        viewModel.closeHeapDumpWorkspace(workspace);

        assertTrue(viewModel.heapDumpWorkspacesProperty().isEmpty());
        assertNull(viewModel.selectedHeapDumpWorkspaceProperty().get());
        assertEquals("home", viewModel.selectedSectionProperty().get());
        assertEquals(AppWorkspaceKind.GLOBAL, viewModel.activeWorkspaceKindProperty().get());
    }

    @Test
    void openingSameRecordingPathSelectsExistingWorkspace() {
        AppShellViewModel viewModel = new AppShellViewModel();
        RecordingWorkspace first = viewModel.openRecording(
                recording("first", Path.of("demo.jfr")), new OverviewViewModel(),
                eventBrowserViewModel(), ruleResultsViewModel());

        viewModel.showSection("settings");
        RecordingWorkspace second = viewModel.openRecording(
                recording("second", Path.of(".").resolve("demo.jfr")), new OverviewViewModel(),
                eventBrowserViewModel(), ruleResultsViewModel());

        assertSame(first, second);
        assertEquals(List.of(first), viewModel.recordingWorkspacesProperty());
        assertEquals(List.of(first), viewModel.workspaceTabsProperty());
        assertSame(first, viewModel.selectedWorkspaceProperty().get());
        assertEquals(AppWorkspaceKind.RECORDING, viewModel.activeWorkspaceKindProperty().get());
    }

    @Test
    void openingSameHeapDumpPathSelectsExistingWorkspace() {
        AppShellViewModel viewModel = new AppShellViewModel();
        HeapDumpWorkspace first = new HeapDumpWorkspace(Path.of("demo.hprof"), null);
        HeapDumpWorkspace second = new HeapDumpWorkspace(Path.of(".").resolve("demo.hprof"), null);

        viewModel.openHeapDump(first);
        viewModel.showSection("settings");
        viewModel.openHeapDump(second);

        assertEquals(List.of(first), viewModel.heapDumpWorkspacesProperty());
        assertEquals(List.of(first), viewModel.workspaceTabsProperty());
        assertSame(first, viewModel.selectedHeapDumpWorkspaceProperty().get());
        assertEquals(AppWorkspaceKind.HEAP_DUMP, viewModel.activeWorkspaceKindProperty().get());
    }

    @Test
    void switchingBetweenRecordingAndHeapDumpWorkspacesUpdatesActiveWorkspaceKind() {
        AppShellViewModel viewModel = new AppShellViewModel();
        RecordingWorkspace recording = viewModel.openRecording(
                recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());
        HeapDumpWorkspace heapDump = new HeapDumpWorkspace(Path.of("demo.hprof"), null);

        viewModel.openHeapDump(heapDump);

        assertNull(viewModel.selectedWorkspaceProperty().get());
        assertSame(heapDump, viewModel.selectedHeapDumpWorkspaceProperty().get());
        assertEquals("heapDumpAnalysis", viewModel.selectedSectionProperty().get());
        assertEquals(AppWorkspaceKind.HEAP_DUMP, viewModel.activeWorkspaceKindProperty().get());
        assertNull(viewModel.selectedLiveJvmWorkspaceProperty().get());

        viewModel.selectWorkspace(recording);

        assertSame(recording, viewModel.selectedWorkspaceProperty().get());
        assertNull(viewModel.selectedHeapDumpWorkspaceProperty().get());
        assertEquals("analysis", viewModel.selectedSectionProperty().get());
        assertEquals(AppWorkspaceKind.RECORDING, viewModel.activeWorkspaceKindProperty().get());
        assertNull(viewModel.selectedLiveJvmWorkspaceProperty().get());
    }

    @Test
    void switchingAwayFromLiveJvmKeepsJvmWorkspaceOpen() {
        AppShellViewModel viewModel = new AppShellViewModel();
        viewModel.openLiveJvmWorkspace();
        LiveJvmWorkspace liveJvm = viewModel.liveJvmWorkspaceProperty().get();

        HeapDumpWorkspace heapDump = new HeapDumpWorkspace(Path.of("demo.hprof"), null);
        viewModel.openHeapDump(heapDump);

        assertSame(liveJvm, viewModel.liveJvmWorkspaceProperty().get());
        assertTrue(viewModel.liveJvmWorkspaceOpenProperty().get());
        assertNull(viewModel.selectedLiveJvmWorkspaceProperty().get());
        assertSame(heapDump, viewModel.selectedHeapDumpWorkspaceProperty().get());

        viewModel.selectLiveJvmWorkspace();

        assertSame(liveJvm, viewModel.selectedLiveJvmWorkspaceProperty().get());
        assertEquals(AppWorkspaceKind.LIVE_JVM, viewModel.activeWorkspaceKindProperty().get());
        assertEquals("jvms", viewModel.selectedSectionProperty().get());
    }

    @Test
    void workspaceTabsFollowOpenOrderAcrossWorkspaceTypes() {
        AppShellViewModel viewModel = new AppShellViewModel();

        viewModel.openLiveJvmWorkspace();
        LiveJvmWorkspace liveJvm = viewModel.liveJvmWorkspaceProperty().get();
        HeapDumpWorkspace heapDump = new HeapDumpWorkspace(Path.of("demo.hprof"), null);
        viewModel.openHeapDump(heapDump);
        RecordingWorkspace recording = viewModel.openRecording(
                recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());

        assertEquals(List.of(liveJvm, heapDump, recording), viewModel.workspaceTabsProperty());
        assertSame(recording, viewModel.selectedWorkspaceTabProperty().get());

        viewModel.closeHeapDumpWorkspace(heapDump);

        assertEquals(List.of(liveJvm, recording), viewModel.workspaceTabsProperty());
        assertSame(recording, viewModel.selectedWorkspaceTabProperty().get());
    }

    @Test
    void completedRecordingDoesNotStealFocusFromLaterOpenedWorkspace() {
        AppShellViewModel viewModel = new AppShellViewModel();
        long recordingOpenGeneration = viewModel.nextOpenGeneration();
        HeapDumpWorkspace heapDump = new HeapDumpWorkspace(Path.of("demo.hprof"), null);
        viewModel.openHeapDump(heapDump);

        RecordingWorkspace recording = viewModel.openRecording(
                recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel(),
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, recordingOpenGeneration);

        assertEquals(List.of(heapDump, recording), viewModel.workspaceTabsProperty());
        assertSame(heapDump, viewModel.selectedWorkspaceTabProperty().get());
        assertSame(heapDump, viewModel.selectedHeapDumpWorkspaceProperty().get());
        assertNull(viewModel.selectedWorkspaceProperty().get());
        assertEquals(AppWorkspaceKind.HEAP_DUMP, viewModel.activeWorkspaceKindProperty().get());
        assertEquals("heapDumpAnalysis", viewModel.selectedSectionProperty().get());
    }

    @Test
    void closingActiveRecordingSelectsNextWorkspaceTabAcrossTypes() {
        AppShellViewModel viewModel = new AppShellViewModel();
        RecordingWorkspace recording = viewModel.openRecording(
                recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());
        HeapDumpWorkspace heapDump = new HeapDumpWorkspace(Path.of("demo.hprof"), null);
        viewModel.openHeapDump(heapDump);
        viewModel.openLiveJvmWorkspace();
        viewModel.selectWorkspace(recording);

        viewModel.closeWorkspace(recording);

        assertEquals(List.of(heapDump, viewModel.liveJvmWorkspaceProperty().get()), viewModel.workspaceTabsProperty());
        assertNull(viewModel.selectedWorkspaceProperty().get());
        assertSame(heapDump, viewModel.selectedHeapDumpWorkspaceProperty().get());
        assertEquals(AppWorkspaceKind.HEAP_DUMP, viewModel.activeWorkspaceKindProperty().get());
        assertEquals("heapDumpAnalysis", viewModel.selectedSectionProperty().get());
        assertFalse(viewModel.recordingOpenProperty().get());
    }

    @Test
    void closingActiveHeapDumpSelectsNextWorkspaceTabAcrossTypes() {
        AppShellViewModel viewModel = new AppShellViewModel();
        viewModel.openLiveJvmWorkspace();
        LiveJvmWorkspace liveJvm = viewModel.liveJvmWorkspaceProperty().get();
        HeapDumpWorkspace heapDump = new HeapDumpWorkspace(Path.of("demo.hprof"), null);
        viewModel.openHeapDump(heapDump);
        RecordingWorkspace recording = viewModel.openRecording(
                recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());
        viewModel.selectHeapDumpWorkspace(heapDump);

        viewModel.closeHeapDumpWorkspace(heapDump);

        assertEquals(List.of(liveJvm, recording), viewModel.workspaceTabsProperty());
        assertNull(viewModel.selectedHeapDumpWorkspaceProperty().get());
        assertSame(recording, viewModel.selectedWorkspaceProperty().get());
        assertEquals(AppWorkspaceKind.RECORDING, viewModel.activeWorkspaceKindProperty().get());
        assertEquals("analysis", viewModel.selectedSectionProperty().get());
    }

    @Test
    void closingActiveLiveJvmSelectsNextWorkspaceTabAcrossTypes() {
        AppShellViewModel viewModel = new AppShellViewModel();
        viewModel.openLiveJvmWorkspace();
        HeapDumpWorkspace heapDump = new HeapDumpWorkspace(Path.of("demo.hprof"), null);
        viewModel.openHeapDump(heapDump);
        viewModel.selectLiveJvmWorkspace();

        viewModel.closeLiveJvmWorkspace();

        assertEquals(List.of(heapDump), viewModel.workspaceTabsProperty());
        assertNull(viewModel.selectedLiveJvmWorkspaceProperty().get());
        assertSame(heapDump, viewModel.selectedHeapDumpWorkspaceProperty().get());
        assertEquals(AppWorkspaceKind.HEAP_DUMP, viewModel.activeWorkspaceKindProperty().get());
        assertEquals("heapDumpAnalysis", viewModel.selectedSectionProperty().get());
    }

    @Test
    void switchingToHeapDumpClearsRecordingBeforeSelectingHeapDump() {
        AppShellViewModel viewModel = new AppShellViewModel();
        viewModel.openRecording(recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());
        HeapDumpWorkspace heapDump = new HeapDumpWorkspace(Path.of("demo.hprof"), null);

        viewModel.selectedHeapDumpWorkspaceProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                assertNull(viewModel.selectedWorkspaceProperty().get(),
                        "Selecting HPROF must not expose an intermediate state with both JFR and HPROF selected");
            }
        });

        viewModel.openHeapDump(heapDump);
    }

    @Test
    void switchingToRecordingClearsHeapDumpBeforeSelectingRecording() {
        AppShellViewModel viewModel = new AppShellViewModel();
        RecordingWorkspace recording = viewModel.openRecording(
                recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());
        viewModel.openHeapDump(new HeapDumpWorkspace(Path.of("demo.hprof"), null));

        viewModel.selectedWorkspaceProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                assertNull(viewModel.selectedHeapDumpWorkspaceProperty().get(),
                        "Selecting JFR must not expose an intermediate state with both HPROF and JFR selected");
            }
        });

        viewModel.selectWorkspace(recording);
    }

    @Test
    void tracksRecordingSectionsPerWorkspaceAndGlobalPagesStayAvailable() {
        AppShellViewModel viewModel = new AppShellViewModel();
        RecordingWorkspace first = viewModel.openRecording(
                recording("first", "first.jfr"), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());
        RecordingWorkspace second = viewModel.openRecording(
                recording("second", "second.jfr"), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());

        viewModel.showSection("events");
        viewModel.selectWorkspace(first);
        viewModel.showSection("overview");
        viewModel.showSection("settings");
        viewModel.selectWorkspace(second);

        assertEquals("events", viewModel.selectedSectionProperty().get());
        assertSame(second, viewModel.selectedWorkspaceProperty().get());
        assertEquals("events", second.selectedSectionProperty().get());

        viewModel.showSection("home");

        assertEquals("home", viewModel.selectedSectionProperty().get());
        assertEquals(AppWorkspaceKind.RECORDING, viewModel.activeWorkspaceKindProperty().get());
        assertSame(second, viewModel.selectedWorkspaceProperty().get());
        assertNull(viewModel.selectedHeapDumpWorkspaceProperty().get());
        assertEquals("second.jfr", viewModel.currentTargetNameProperty().get());

        viewModel.selectWorkspace(first);

        assertEquals("overview", viewModel.selectedSectionProperty().get());
        assertEquals(AppWorkspaceKind.RECORDING, viewModel.activeWorkspaceKindProperty().get());
        assertSame(first, viewModel.selectedWorkspaceProperty().get());
        assertEquals("overview", first.selectedSectionProperty().get());

        viewModel.showSection("settings");

        assertEquals("settings", viewModel.selectedSectionProperty().get());
        assertEquals(AppWorkspaceKind.RECORDING, viewModel.activeWorkspaceKindProperty().get());
        assertSame(first, viewModel.selectedWorkspaceProperty().get());

        viewModel.selectWorkspace(first);
        viewModel.showSection("analysis");

        assertEquals("analysis", viewModel.selectedSectionProperty().get());
        assertEquals("analysis", first.selectedSectionProperty().get());
    }

    @Test
    void globalPagesKeepWorkspaceNavigationContext() {
        AppShellViewModel viewModel = new AppShellViewModel();
        RecordingWorkspace recording = viewModel.openRecording(
                recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());

        viewModel.showSection("settings");

        assertEquals("settings", viewModel.selectedSectionProperty().get());
        assertEquals(AppWorkspaceKind.RECORDING, viewModel.activeWorkspaceKindProperty().get());
        assertSame(recording, viewModel.selectedWorkspaceProperty().get());
        assertSame(recording, viewModel.selectedWorkspaceTabProperty().get());

        viewModel.showSection("home");

        assertEquals("home", viewModel.selectedSectionProperty().get());
        assertEquals(AppWorkspaceKind.RECORDING, viewModel.activeWorkspaceKindProperty().get());
        assertSame(recording, viewModel.selectedWorkspaceProperty().get());
    }

    @Test
    void javaApplicationOverviewAndSubPagesAreRecordingSections() {
        AppShellViewModel viewModel = new AppShellViewModel();
        RecordingWorkspace workspace = viewModel.openRecording(
                recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());

        for (String sectionId : List.of("javaApplication", "threadHistogram", "security",
                "nativeLibraries", "threadDumps")) {
            viewModel.showSection(sectionId);

            assertEquals(sectionId, viewModel.selectedSectionProperty().get());
            assertEquals(sectionId, workspace.selectedSectionProperty().get());
        }
    }

    @Test
    void advancedJfrIsARecordingSection() {
        AppShellViewModel viewModel = new AppShellViewModel();
        RecordingWorkspace workspace = viewModel.openRecording(
                recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());

        viewModel.showSection("advancedJfr");

        assertEquals("advancedJfr", viewModel.selectedSectionProperty().get());
        assertEquals("advancedJfr", workspace.selectedSectionProperty().get());
    }

    @Test
    void exposesSidebarStateForSelectedWorkspace() {
        AppShellViewModel viewModel = new AppShellViewModel();
        RecordingWorkspace first = viewModel.openRecording(
                recording("first", "first.jfr"), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());
        RecordingWorkspace second = viewModel.openRecording(
                recording("second", "second.jfr"), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());

        assertTrue(viewModel.recordingOpenProperty().get());
        assertEquals("second.jfr", viewModel.currentRecordingNameProperty().get());

        viewModel.selectWorkspace(first);

        assertSame(first, viewModel.selectedWorkspaceProperty().get());
        assertEquals("first.jfr", viewModel.currentRecordingNameProperty().get());
        assertEquals(2, viewModel.recordingWorkspacesProperty().size());
        assertTrue(viewModel.recordingWorkspacesProperty().contains(second));
    }

    @Test
    void closingActiveWorkspaceSelectsNeighborThenReturnsHomeWhenEmpty() {
        AppShellViewModel viewModel = new AppShellViewModel();
        TestEventBrowserBackgroundExecutor firstExecutor = new TestEventBrowserBackgroundExecutor();
        TestEventBrowserBackgroundExecutor secondExecutor = new TestEventBrowserBackgroundExecutor();
        RecordingWorkspace first = viewModel.openRecording(
                recording("first", "first.jfr"), new OverviewViewModel(), eventBrowserViewModel(firstExecutor), ruleResultsViewModel());
        RecordingWorkspace second = viewModel.openRecording(
                recording("second", "second.jfr"), new OverviewViewModel(), eventBrowserViewModel(secondExecutor), ruleResultsViewModel());

        viewModel.closeWorkspace(second);

        assertTrue(secondExecutor.closed);
        assertSame(first, viewModel.selectedWorkspaceProperty().get());
        assertTrue(viewModel.recordingOpenProperty().get());
        assertEquals("first.jfr", viewModel.currentRecordingNameProperty().get());
        assertEquals("analysis", viewModel.selectedSectionProperty().get());

        viewModel.closeWorkspace(first);

        assertTrue(firstExecutor.closed);
        assertTrue(viewModel.recordingWorkspacesProperty().isEmpty());
        assertNull(viewModel.selectedWorkspaceProperty().get());
        assertFalse(viewModel.recordingOpenProperty().get());
        assertEquals("", viewModel.currentRecordingNameProperty().get());
        assertEquals("home", viewModel.selectedSectionProperty().get());
    }

    @Test
    void closingInactiveWorkspaceKeepsCurrentSelectionAndClosesWorkspace() {
        AppShellViewModel viewModel = new AppShellViewModel();
        TestEventBrowserBackgroundExecutor firstExecutor = new TestEventBrowserBackgroundExecutor();
        TestEventBrowserBackgroundExecutor secondExecutor = new TestEventBrowserBackgroundExecutor();
        RecordingWorkspace first = viewModel.openRecording(
                recording("first", "first.jfr"), new OverviewViewModel(), eventBrowserViewModel(firstExecutor), ruleResultsViewModel());
        RecordingWorkspace second = viewModel.openRecording(
                recording("second", "second.jfr"), new OverviewViewModel(), eventBrowserViewModel(secondExecutor), ruleResultsViewModel());

        viewModel.closeWorkspace(first);

        assertTrue(firstExecutor.closed);
        assertFalse(secondExecutor.closed);
        assertSame(second, viewModel.selectedWorkspaceProperty().get());
        assertEquals("second.jfr", viewModel.currentRecordingNameProperty().get());
        assertEquals(1, viewModel.recordingWorkspacesProperty().size());
    }

    @Test
    void rejectsMissingWorkspaceCollaborators() {
        AppShellViewModel viewModel = new AppShellViewModel();

        assertThrows(NullPointerException.class,
                () -> viewModel.openRecording(recording(), new OverviewViewModel(), null, ruleResultsViewModel()));
    }

    @Test
    void ignoresUnknownSectionsAndNullSections() {
        AppShellViewModel viewModel = new AppShellViewModel();
        RecordingWorkspace workspace = viewModel.openRecording(
                recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());

        viewModel.showSection("events");
        viewModel.showSection("typo");
        viewModel.showSection(null);

        assertEquals("events", viewModel.selectedSectionProperty().get());
        assertEquals("events", workspace.selectedSectionProperty().get());
    }

    @Test
    void recordingWorkspaceCarriesG1GcViewModelAndSection() {
        AppShellViewModel viewModel = new AppShellViewModel();
        G1GcViewModel g1Gc = new G1GcViewModel(new LoadG1GcUseCase(new FakeG1GcService()));

        RecordingWorkspace workspace = viewModel.openRecording(recording(), new OverviewViewModel(),
                eventBrowserViewModel(), ruleResultsViewModel(),
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, g1Gc, null, null);

        viewModel.showSection("g1Gc");

        assertSame(g1Gc, workspace.g1GcViewModel());
        assertEquals("g1Gc", viewModel.selectedSectionProperty().get());
        assertEquals("g1Gc", workspace.selectedSectionProperty().get());
    }

    @Test
    void recordingWorkspaceCarriesJavaFxEventsViewModelAndSection() {
        AppShellViewModel viewModel = new AppShellViewModel();
        JavaFxEventsViewModel javaFxEvents =
                new JavaFxEventsViewModel(new LoadJavaFxEventsUseCase(new FakeJavaFxEventService()));

        RecordingWorkspace workspace = viewModel.openRecording(recording(), new OverviewViewModel(),
                eventBrowserViewModel(), ruleResultsViewModel(),
                null, null, null,
                null, null, null,
                null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, javaFxEvents, null);

        viewModel.showSection("javaFxEvents");

        assertSame(javaFxEvents, workspace.javaFxEventsViewModel());
        assertEquals("javaFxEvents", viewModel.selectedSectionProperty().get());
        assertEquals("javaFxEvents", workspace.selectedSectionProperty().get());
    }

    @Test
    void workspaceCollectionsAreReadOnlyFromOutside() {
        AppShellViewModel viewModel = new AppShellViewModel();
        RecordingWorkspace workspace = viewModel.openRecording(
                recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());

        assertThrows(UnsupportedOperationException.class, () -> viewModel.recordingWorkspacesProperty().clear());
        assertSame(workspace, viewModel.selectedWorkspaceProperty().get());
        assertEquals(1, viewModel.recordingWorkspacesProperty().size());
    }

    private static RecordingSummary recording() {
        return recording("rec", "rec.jfr");
    }

    private static RecordingSummary recording(String id, String fileName) {
        return recording(id, Path.of(fileName));
    }

    private static RecordingSummary recording(String id, Path path) {
        return new RecordingSummary(id, path, path.getFileName().toString(),
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);
    }

    private static EventBrowserViewModel eventBrowserViewModel() {
        return eventBrowserViewModel(new TestEventBrowserBackgroundExecutor());
    }

    private static EventBrowserViewModel eventBrowserViewModel(EventBrowserBackgroundExecutor executor) {
        return new EventBrowserViewModel(new BrowseEventsUseCase(new EmptyEventQueryService()), executor);
    }

    private static RuleResultsViewModel ruleResultsViewModel() {
        return new RuleResultsViewModel(AnalyzeRulesUseCase.empty(), new DiagnosticFindingsUseCase());
    }

    private static final class EmptyEventQueryService implements EventQueryService {
        @Override
        public EventQuerySession openSession(RecordingSummary recording) {
            throw new UnsupportedOperationException("Not used by shell tests.");
        }
    }

    private static final class TestEventBrowserBackgroundExecutor implements EventBrowserBackgroundExecutor {
        private boolean closed;

        @Override
        public void execute(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
