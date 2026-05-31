package com.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class RecordingPageCatalogTest {

    @Test
    void exposesRecordingPageMetadataWithoutOpeningFxml() {
        List<RecordingPageDescriptor> pages = RecordingPageCatalog.pages();

        assertEquals("analysis", RecordingPageCatalog.defaultSectionId());
        assertEquals(37, pages.size());
        assertEquals(pages.stream().map(RecordingPageDescriptor::id).distinct().count(), pages.size());

        RecordingPageDescriptor analysis = RecordingPageCatalog.page("analysis").orElseThrow();
        assertEquals("analysis.title", analysis.titleKey());
        assertEquals("recording", analysis.groupId());
        assertEquals(RecordingPageTemplate.SPLIT_TABLE_DETAIL, analysis.template());
        assertEquals(0.36, analysis.defaultSplitPosition());
        assertTrue(analysis.defaultColumns().stream().anyMatch(column -> column.id().equals("score")));

        RecordingPageDescriptor events = RecordingPageCatalog.page("events").orElseThrow();
        assertEquals("events.title", events.titleKey());
        assertEquals("recording", events.groupId());
        assertEquals(RecordingPageTemplate.SPLIT_TABLE_DETAIL, events.template());
        assertTrue(events.defaultColumns().stream().anyMatch(column -> column.id().equals("eventType")));

        RecordingPageDescriptor metadata = RecordingPageCatalog.page("metadata").orElseThrow();
        assertEquals("recording", metadata.groupId());

        RecordingPageDescriptor javaApplication = RecordingPageCatalog.page("javaApplication").orElseThrow();
        assertEquals("javaApplication.title", javaApplication.titleKey());
        assertEquals("javaApplication", javaApplication.groupId());
        assertEquals(RecordingPageTemplate.OVERVIEW, javaApplication.template());
        assertTrue(pages.indexOf(javaApplication) < pages.indexOf(RecordingPageCatalog.page("profiling").orElseThrow()));

        RecordingPageDescriptor jvmInternals = RecordingPageCatalog.page("jvmInternals").orElseThrow();
        assertEquals("jvmInternals.title", jvmInternals.titleKey());
        assertEquals("jvmInternals", jvmInternals.groupId());
        assertEquals(RecordingPageTemplate.OVERVIEW, jvmInternals.template());
        assertTrue(pages.indexOf(jvmInternals) < pages.indexOf(RecordingPageCatalog.page("jvmInfo").orElseThrow()));

        RecordingPageDescriptor environment = RecordingPageCatalog.page("environment").orElseThrow();
        assertEquals("environment.title", environment.titleKey());
        assertEquals("environment", environment.groupId());
        assertEquals(RecordingPageTemplate.OVERVIEW, environment.template());
        assertTrue(pages.indexOf(environment) < pages.indexOf(RecordingPageCatalog.page("processes").orElseThrow()));
    }

    @Test
    void appShellUsesCatalogAsRecordingSectionSource() {
        AppShellViewModel viewModel = new AppShellViewModel();
        RecordingWorkspace workspace = viewModel.openRecording(
                AppShellViewModelTestSupport.recording(),
                new com.youngledo.jmcfx.ui.overview.OverviewViewModel(),
                AppShellViewModelTestSupport.eventBrowserViewModel(),
                AppShellViewModelTestSupport.ruleResultsViewModel());

        for (RecordingPageDescriptor page : RecordingPageCatalog.pages()) {
            viewModel.showSection(page.id());

            assertEquals(page.id(), viewModel.selectedSectionProperty().get());
            assertEquals(page.id(), workspace.selectedSectionProperty().get());
            assertSame(page, viewModel.recordingPage(page.id()).orElseThrow());
        }
    }

    @Test
    void recordingWorkspaceStoresLocalPageLayoutStateBySection() {
        RecordingWorkspace workspace = AppShellViewModelTestSupport.openMinimalRecording();

        RecordingPageLayoutState initial = workspace.pageLayoutState("events");

        assertNotNull(initial);
        assertEquals("events", initial.sectionId());
        assertEquals(RecordingPageCatalog.page("events").orElseThrow().defaultSplitPosition(),
                initial.splitPosition());

        workspace.updatePageLayoutState("events", initial.withSplitPosition(0.58).withSelectedDetailTab("Stack Trace"));

        RecordingPageLayoutState updated = workspace.pageLayoutState("events");
        assertEquals(0.58, updated.splitPosition());
        assertEquals("Stack Trace", updated.selectedDetailTab());
    }
}
