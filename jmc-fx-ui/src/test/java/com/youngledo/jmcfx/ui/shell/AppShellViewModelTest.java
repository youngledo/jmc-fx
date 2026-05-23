package com.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.EventQueryService;
import com.youngledo.jmcfx.domain.service.EventQuerySession;
import com.youngledo.jmcfx.ui.events.EventBrowserBackgroundExecutor;
import com.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import com.youngledo.jmcfx.ui.overview.OverviewViewModel;
import com.youngledo.jmcfx.ui.rules.RuleResultsViewModel;

class AppShellViewModelTest {

    @Test
    void defaultsToHomeWithoutRecording() {
        AppShellViewModel viewModel = new AppShellViewModel();

        assertEquals("home", viewModel.selectedSectionProperty().get());
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

        assertEquals(com.youngledo.jmcfx.ui.i18n.LanguageMode.SYSTEM,
                viewModel.languageModeProperty().get());

        viewModel.setLanguageMode(com.youngledo.jmcfx.ui.i18n.LanguageMode.CHINESE_SIMPLIFIED);

        assertEquals(com.youngledo.jmcfx.ui.i18n.LanguageMode.CHINESE_SIMPLIFIED,
                viewModel.languageModeProperty().get());
    }

    @Test
    void openingRecordingEntersAnalysisWorkspace() {
        AppShellViewModel viewModel = new AppShellViewModel();

        RecordingWorkspace workspace = viewModel.openRecording(
                recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());

        assertTrue(viewModel.recordingOpenProperty().get());
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
    }

    @Test
    void tracksRecordingSectionsPerWorkspaceAndGlobalSectionsInShell() {
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

        assertEquals("settings", viewModel.selectedSectionProperty().get());
        assertSame(second, viewModel.selectedWorkspaceProperty().get());
        assertEquals("events", second.selectedSectionProperty().get());

        viewModel.showSection("home");
        viewModel.selectWorkspace(first);

        assertEquals("home", viewModel.selectedSectionProperty().get());
        assertSame(first, viewModel.selectedWorkspaceProperty().get());
        assertEquals("overview", first.selectedSectionProperty().get());

        viewModel.showSection("analysis");

        assertEquals("analysis", viewModel.selectedSectionProperty().get());
        assertEquals("analysis", first.selectedSectionProperty().get());
    }

    @Test
    void javaApplicationSubPagesAreRecordingSections() {
        AppShellViewModel viewModel = new AppShellViewModel();
        RecordingWorkspace workspace = viewModel.openRecording(
                recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());

        for (String sectionId : List.of("threadHistogram", "security", "nativeLibraries", "threadDumps")) {
            viewModel.showSection(sectionId);

            assertEquals(sectionId, viewModel.selectedSectionProperty().get());
            assertEquals(sectionId, workspace.selectedSectionProperty().get());
        }
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
        return new RecordingSummary(id, Path.of(fileName), fileName,
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);
    }

    private static EventBrowserViewModel eventBrowserViewModel() {
        return eventBrowserViewModel(new TestEventBrowserBackgroundExecutor());
    }

    private static EventBrowserViewModel eventBrowserViewModel(EventBrowserBackgroundExecutor executor) {
        return new EventBrowserViewModel(new EmptyEventQueryService(), executor);
    }

    private static RuleResultsViewModel ruleResultsViewModel() {
        return new RuleResultsViewModel(rec -> java.util.List.of());
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
