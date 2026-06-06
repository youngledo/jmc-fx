package io.github.youngledo.jmcfx.ui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.ui.i18n.I18n;
import io.github.youngledo.jmcfx.ui.i18n.LanguageMode;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

class WorkbenchTableSupportTest {

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
    void denseTableSetupAddsDenseClassAndAccessibleText() {
        TableView<String> table = new TableView<>();

        WorkbenchTableSupport.configureDenseTable(table, "Events");

        assertTrue(table.getStyleClass().contains("dense-table"));
        assertEquals("Events", table.getAccessibleText());
    }

    @Test
    void localizedPlaceholderBindsToI18nTextAndAccessibleText() {
        I18n i18n = new I18n(Locale.ENGLISH);
        Label placeholder = WorkbenchTableSupport.localizedPlaceholder(i18n, "analysis.empty");

        assertEquals(i18n.get("analysis.empty"), placeholder.getText());
        assertEquals(placeholder.getText(), placeholder.getAccessibleText());

        i18n.setLanguageMode(LanguageMode.CHINESE_SIMPLIFIED);

        assertEquals(i18n.get("analysis.empty"), placeholder.getText());
        assertEquals(placeholder.getText(), placeholder.getAccessibleText());
    }
}
