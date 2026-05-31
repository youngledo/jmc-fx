package com.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.i18n.LanguageMode;

import javafx.application.Platform;
import javafx.scene.control.TreeItem;

class AppNavTreeTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await(5, TimeUnit.SECONDS);
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized by another test class.
        }
    }

    @Test
    void searchFindsNavigationPagesByTitle() {
        AppNavTree tree = new AppNavTree(new I18n(Locale.ENGLISH));
        tree.setRecordingOpenForTesting(true);
        tree.setActiveWorkspaceKindForTesting(AppWorkspaceKind.RECORDING);

        List<AppNavSearchResult> results = tree.search("socket");

        assertFalse(results.isEmpty());
        assertEquals("socketio", results.getFirst().sectionId());
        assertEquals("Socket I/O", results.getFirst().title());
    }

    @Test
    void searchFindsNavigationPagesBySectionId() {
        AppNavTree tree = new AppNavTree(new I18n(Locale.ENGLISH));
        tree.setRecordingOpenForTesting(true);
        tree.setActiveWorkspaceKindForTesting(AppWorkspaceKind.RECORDING);

        List<AppNavSearchResult> results = tree.search("gcConfig");

        assertFalse(results.isEmpty());
        assertEquals("gcConfig", results.getFirst().sectionId());
    }

    @Test
    void searchFindsNavigationPagesByChineseTitle() {
        I18n i18n = new I18n(Locale.SIMPLIFIED_CHINESE);
        i18n.setLanguageMode(LanguageMode.CHINESE_SIMPLIFIED);
        AppNavTree tree = new AppNavTree(i18n);
        tree.setRecordingOpenForTesting(true);
        tree.setActiveWorkspaceKindForTesting(AppWorkspaceKind.RECORDING);

        List<AppNavSearchResult> results = tree.search("套接字");

        assertFalse(results.isEmpty());
        assertEquals("socketio", results.getFirst().sectionId());
        assertEquals("套接字I/O", results.getFirst().title());
    }

    @Test
    void searchUsesUpdatedI18nAfterSidebarRebindsLanguage() {
        AppNavTree tree = new AppNavTree(new I18n(Locale.ENGLISH));
        I18n i18n = new I18n(Locale.SIMPLIFIED_CHINESE);
        i18n.setLanguageMode(LanguageMode.CHINESE_SIMPLIFIED);
        tree.setI18n(i18n);
        tree.setRecordingOpenForTesting(true);
        tree.setActiveWorkspaceKindForTesting(AppWorkspaceKind.RECORDING);

        List<AppNavSearchResult> results = tree.search("文件");

        assertFalse(results.isEmpty());
        assertEquals("fileio", results.getFirst().sectionId());
        assertEquals("文件I/O", results.getFirst().title());
    }

    @Test
    void chineseLocaleDoesNotSearchAsciiOnlyCompositionText() {
        I18n i18n = new I18n(Locale.SIMPLIFIED_CHINESE);
        i18n.setLanguageMode(LanguageMode.CHINESE_SIMPLIFIED);

        assertFalse(AppSidebar.shouldSearchQueryImmediately(i18n, "wenjian"));
        assertFalse(AppSidebar.shouldSearchQueryImmediately(i18n, "I"));
        assertTrue(AppSidebar.shouldSearchQueryImmediately(i18n, "文件"));
        assertTrue(AppSidebar.shouldSearchQueryImmediately(i18n, "File"));
    }

    @Test
    void searchExcludesRecordingPagesUntilRecordingOpens() {
        AppNavTree tree = new AppNavTree(new I18n(Locale.ENGLISH));

        assertTrue(tree.search("events").isEmpty());

        tree.setRecordingOpenForTesting(true);
        tree.setActiveWorkspaceKindForTesting(AppWorkspaceKind.RECORDING);

        assertEquals("events", tree.search("events").getFirst().sectionId());
    }

    @Test
    void searchFindsAdvancedJfrRecordingPageAfterRecordingOpens() {
        AppNavTree tree = new AppNavTree(new I18n(Locale.ENGLISH));
        tree.setRecordingOpenForTesting(true);
        tree.setActiveWorkspaceKindForTesting(AppWorkspaceKind.RECORDING);

        List<AppNavSearchResult> results = tree.search("advanced jfr");

        assertFalse(results.isEmpty());
        assertEquals("advancedJfr", results.getFirst().sectionId());
        assertEquals("Advanced JFR", results.getFirst().title());
    }

    @Test
    void liveJvmContextDoesNotDuplicateJvmWorkspaceEntry() {
        AppNavTree tree = new AppNavTree(new I18n(Locale.ENGLISH));
        tree.setActiveWorkspaceKindForTesting(AppWorkspaceKind.LIVE_JVM);

        assertVisibleSections(tree, "home", "settings");
        assertNotVisibleSections(tree, "jvms");
        assertTrue(tree.search("jvms").isEmpty());
    }

    @Test
    void boundTreeShowsOnlyGlobalMenuAfterOpeningLiveJvmWorkspace() {
        AppShellViewModel viewModel = new AppShellViewModel();
        AppNavTree tree = new AppNavTree(new I18n(Locale.ENGLISH));
        tree.bind(viewModel);

        viewModel.openLiveJvmWorkspace();

        assertVisibleSections(tree, "home", "settings");
        assertNotVisibleSections(tree, "jvms");
    }

    @Test
    void globalContextSearchOnlyExposesGlobalPages() {
        AppNavTree tree = new AppNavTree(new I18n(Locale.ENGLISH));
        tree.setActiveWorkspaceKindForTesting(AppWorkspaceKind.GLOBAL);

        assertEquals("home", tree.search("home").getFirst().sectionId());
        assertTrue(tree.search("events").isEmpty());
        assertTrue(tree.search("heap dump").isEmpty());
        assertTrue(tree.search("diagnostic").isEmpty());
    }

    @Test
    void recordingContextSearchOnlyExposesRecordingPages() {
        AppNavTree tree = new AppNavTree(new I18n(Locale.ENGLISH));
        tree.setActiveWorkspaceKindForTesting(AppWorkspaceKind.RECORDING);

        assertEquals("events", tree.search("events").getFirst().sectionId());
        assertEquals("javaApplication", tree.search("java application").getFirst().sectionId());
        assertEquals("jvmInternals", tree.search("jvm internals").getFirst().sectionId());
        assertEquals("environment", tree.search("environment").getFirst().sectionId());
        assertEquals("home", tree.search("home").getFirst().sectionId());
        assertEquals("settings", tree.search("settings").getFirst().sectionId());
        assertTrue(tree.search("heap dump").isEmpty());
    }

    @Test
    void heapDumpContextSearchOnlyExposesHeapDumpPages() {
        AppNavTree tree = new AppNavTree(new I18n(Locale.ENGLISH));
        tree.setActiveWorkspaceKindForTesting(AppWorkspaceKind.HEAP_DUMP);

        assertEquals("heapDumpAnalysis", tree.search("heap dump").getFirst().sectionId());
        assertEquals("home", tree.search("home").getFirst().sectionId());
        assertEquals("settings", tree.search("settings").getFirst().sectionId());
        assertTrue(tree.search("events").isEmpty());
    }

    @Test
    void boundTreeShowsRecordingMenuAfterOpeningRecording() {
        AppShellViewModel viewModel = new AppShellViewModel();
        AppNavTree tree = new AppNavTree(new I18n(Locale.ENGLISH));
        tree.bind(viewModel);

        viewModel.openRecording(recording());

        assertVisibleSections(tree, "home", "settings", "analysis", "overview", "events", "advancedJfr",
                "javaApplication", "jvmInternals", "environment");
        assertNotVisibleSections(tree, "memoryAnalysis");
        assertGroupHasChildren(tree, "javaApplication", "heap", "leaks");
        assertGroupHasChildren(tree, "jvmInternals", "tlab");
        assertGroupHasChildren(tree, "environment", "nativeLibraries");
        assertGroupDoesNotHaveChildren(tree, "javaApplication", "nativeLibraries");
        assertGroupDoesNotHaveChildren(tree, "jvmInternals", "heap", "leaks");
        assertChildrenUseTone(tree, "javaApplication", NavIconTone.JAVA, "heap", "leaks");
        assertChildrenUseTone(tree, "jvmInternals", NavIconTone.JVM, "tlab");
        assertChildrenUseTone(tree, "environment", NavIconTone.ENVIRONMENT, "nativeLibraries");
        assertGroupUsesGroupItselfAsOverviewEntry(tree, "javaApplication", "profiling");
        assertGroupUsesGroupItselfAsOverviewEntry(tree, "jvmInternals", "jvmInfo");
        assertGroupUsesGroupItselfAsOverviewEntry(tree, "environment", "processes");
        assertExpandedSections(tree, "analysis", "overview", "events", "advancedJfr", "javaApplication",
                "jvmInternals", "environment");
        assertTrue(tree.search("events").stream().anyMatch(result -> "events".equals(result.sectionId())));
    }

    @Test
    void recordingParentGroupsNavigateToTheirOwnOverviewPages() {
        AppNavTree tree = new AppNavTree(new I18n(Locale.ENGLISH));
        tree.setRecordingOpenForTesting(true);
        tree.setActiveWorkspaceKindForTesting(AppWorkspaceKind.RECORDING);
        java.util.List<String> navigatedSections = new java.util.ArrayList<>();
        tree.setNavigationHandler(navigatedSections::add);

        tree.navigateToSection("javaApplication");
        tree.navigateToSection("jvmInternals");
        tree.navigateToSection("environment");

        assertEquals(List.of("javaApplication", "jvmInternals", "environment"), navigatedSections);
    }

    @Test
    void boundTreeShowsHeapDumpMenuAfterOpeningHeapDump() {
        AppShellViewModel viewModel = new AppShellViewModel();
        AppNavTree tree = new AppNavTree(new I18n(Locale.ENGLISH));
        tree.bind(viewModel);

        viewModel.openHeapDump(new HeapDumpWorkspace(Path.of("demo.hprof"), null));

        assertVisibleSections(tree, "home", "settings", "heapDumpAnalysis");
        assertExpandedSections(tree, "heapDumpAnalysis");
        assertTrue(tree.search("heap dump").stream().anyMatch(result -> "heapDumpAnalysis".equals(result.sectionId())));
    }

    private static void assertVisibleSections(AppNavTree tree, String... sectionIds) {
        List<String> visibleSectionIds = tree.getRoot().getChildren().stream()
                .flatMap(group -> java.util.stream.Stream.concat(java.util.stream.Stream.of(group),
                        group.getChildren().stream()))
                .map(item -> item.getValue().sectionId())
                .toList();
        for (String sectionId : sectionIds) {
            assertTrue(visibleSectionIds.contains(sectionId),
                    () -> "Expected visible section " + sectionId + " in " + visibleSectionIds);
        }
    }

    private static void assertNotVisibleSections(AppNavTree tree, String... sectionIds) {
        List<String> visibleSectionIds = tree.getRoot().getChildren().stream()
                .flatMap(group -> java.util.stream.Stream.concat(java.util.stream.Stream.of(group),
                        group.getChildren().stream()))
                .map(item -> item.getValue().sectionId())
                .toList();
        for (String sectionId : sectionIds) {
            assertFalse(visibleSectionIds.contains(sectionId),
                    () -> "Expected hidden section " + sectionId + " in " + visibleSectionIds);
        }
    }

    private static void assertExpandedSections(AppNavTree tree, String... sectionIds) {
        List<String> expandedSectionIds = java.util.stream.IntStream.range(0, tree.getExpandedItemCount())
                .mapToObj(tree::getTreeItem)
                .filter(item -> item != null && item.getValue() != null)
                .map(item -> item.getValue().sectionId())
                .toList();
        for (String sectionId : sectionIds) {
            assertTrue(expandedSectionIds.contains(sectionId),
                    () -> "Expected expanded section " + sectionId + " in " + expandedSectionIds);
        }
    }

    private static void assertGroupUsesGroupItselfAsOverviewEntry(AppNavTree tree, String groupSectionId,
            String expectedChildSectionId) {
        TreeItem<AppNavItem> groupItem = tree.getRoot().getChildren().stream()
                .filter(group -> groupSectionId.equals(group.getValue().sectionId()))
                .findFirst()
                .orElseThrow();

        assertTrue(groupItem.getValue().page());
        assertFalse(groupItem.getChildren().stream()
                .anyMatch(child -> groupSectionId.equals(child.getValue().sectionId())));
        assertTrue(groupItem.getChildren().stream()
                .anyMatch(child -> expectedChildSectionId.equals(child.getValue().sectionId())));
    }

    private static void assertGroupHasChildren(AppNavTree tree, String groupSectionId, String... childSectionIds) {
        List<String> actualChildren = childrenOfGroup(tree, groupSectionId);
        for (String childSectionId : childSectionIds) {
            assertTrue(actualChildren.contains(childSectionId),
                    () -> "Expected group " + groupSectionId + " to contain " + childSectionId
                            + " in " + actualChildren);
        }
    }

    private static void assertGroupDoesNotHaveChildren(AppNavTree tree, String groupSectionId,
            String... childSectionIds) {
        List<String> actualChildren = childrenOfGroup(tree, groupSectionId);
        for (String childSectionId : childSectionIds) {
            assertFalse(actualChildren.contains(childSectionId),
                    () -> "Expected group " + groupSectionId + " not to contain " + childSectionId
                            + " in " + actualChildren);
        }
    }

    private static void assertChildrenUseTone(AppNavTree tree, String groupSectionId, NavIconTone expectedTone,
            String... childSectionIds) {
        List<TreeItem<AppNavItem>> actualChildren = childItemsOfGroup(tree, groupSectionId);
        for (String childSectionId : childSectionIds) {
            AppNavItem child = actualChildren.stream()
                    .map(TreeItem::getValue)
                    .filter(item -> childSectionId.equals(item.sectionId()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(expectedTone, child.iconTone(),
                    () -> "Expected " + childSectionId + " under " + groupSectionId
                            + " to use icon tone " + expectedTone);
        }
    }

    private static List<String> childrenOfGroup(AppNavTree tree, String groupSectionId) {
        return childItemsOfGroup(tree, groupSectionId)
                .stream()
                .map(child -> child.getValue().sectionId())
                .toList();
    }

    private static List<TreeItem<AppNavItem>> childItemsOfGroup(AppNavTree tree, String groupSectionId) {
        return tree.getRoot().getChildren().stream()
                .filter(group -> groupSectionId.equals(group.getValue().sectionId()))
                .findFirst()
                .orElseThrow()
                .getChildren()
                .stream()
                .toList();
    }

    private static RecordingSummary recording() {
        return new RecordingSummary("rec", Path.of("rec.jfr"), "rec.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);
    }
}
