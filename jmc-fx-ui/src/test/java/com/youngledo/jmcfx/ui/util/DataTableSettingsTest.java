package com.youngledo.jmcfx.ui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

class DataTableSettingsTest {

    private static volatile boolean toolkitReady = false;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await(5, TimeUnit.SECONDS);
            toolkitReady = true;
        } catch (IllegalStateException e) {
            toolkitReady = true;
        } catch (Throwable ignored) {
            // Headless CI should skip JavaFX control tests.
        }
    }

    @Test
    void capturesAndRestoresColumnVisibilityWidthAndSortOrder() {
        if (!toolkitReady) {
            return;
        }

        TableView<String> table = new TableView<>();
        TableColumn<String, String> name = column("name", "Name", 180);
        TableColumn<String, String> score = column("score", "Score", 96);
        TableColumn<String, String> details = column("details", "Details", 320);
        table.getColumns().setAll(List.of(name, score, details));
        score.setVisible(false);
        details.setPrefWidth(360);
        table.getSortOrder().setAll(details, name);

        DataTableState state = DataTableState.capture(table);

        assertEquals(List.of("details", "name"), state.sortColumnIds());
        assertFalse(state.column("score").orElseThrow().visible());
        assertEquals(360, state.column("details").orElseThrow().width());

        score.setVisible(true);
        details.setPrefWidth(200);
        table.getSortOrder().clear();

        state.applyTo(table);

        assertFalse(score.isVisible());
        assertEquals(360, details.getPrefWidth());
        assertEquals(List.of(details, name), table.getSortOrder());
    }

    private static TableColumn<String, String> column(String id, String title, double width) {
        TableColumn<String, String> column = new TableColumn<>(title);
        column.setId(id);
        column.setPrefWidth(width);
        return column;
    }
}
