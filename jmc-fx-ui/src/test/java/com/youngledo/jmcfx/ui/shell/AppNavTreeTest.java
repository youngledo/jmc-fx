package com.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.ui.i18n.I18n;
import com.youngledo.jmcfx.ui.i18n.LanguageMode;

import javafx.application.Platform;

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

        List<AppNavSearchResult> results = tree.search("socket");

        assertFalse(results.isEmpty());
        assertEquals("socketio", results.getFirst().sectionId());
        assertEquals("Socket I/O", results.getFirst().title());
    }

    @Test
    void searchFindsNavigationPagesBySectionId() {
        AppNavTree tree = new AppNavTree(new I18n(Locale.ENGLISH));
        tree.setRecordingOpenForTesting(true);

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

        assertEquals("events", tree.search("events").getFirst().sectionId());
    }

    @Test
    void searchFindsAdvancedJfrRecordingPageAfterRecordingOpens() {
        AppNavTree tree = new AppNavTree(new I18n(Locale.ENGLISH));
        tree.setRecordingOpenForTesting(true);

        List<AppNavSearchResult> results = tree.search("advanced jfr");

        assertFalse(results.isEmpty());
        assertEquals("advancedJfr", results.getFirst().sectionId());
        assertEquals("Advanced JFR", results.getFirst().title());
    }

    @Test
    void searchIncludesJvmBrowserWithoutRecording() {
        AppNavTree tree = new AppNavTree(new I18n(Locale.ENGLISH));

        List<AppNavSearchResult> results = tree.search("jvms");

        assertFalse(results.isEmpty());
        assertEquals("jvms", results.getFirst().sectionId());
    }
}
