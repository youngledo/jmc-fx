package io.github.youngledo.jmcfx.ui.util;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CsvExportTest {

    private static volatile boolean toolkitReady = false;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await(5, TimeUnit.SECONDS);
            toolkitReady = true;
        } catch (IllegalStateException e) {
            // Toolkit already initialized
            toolkitReady = true;
        } catch (Throwable ignored) {
            // Headless CI — only escapeCsv tests will run
        }
    }

    @Test
    void escapeCsv_plainText() {
        assertEquals("hello", CsvExport.escapeCsv("hello"));
    }

    @Test
    void escapeCsv_withComma() {
        assertEquals("\"hello,world\"", CsvExport.escapeCsv("hello,world"));
    }

    @Test
    void escapeCsv_withQuote() {
        assertEquals("\"say \"\"hi\"\"\"", CsvExport.escapeCsv("say \"hi\""));
    }

    @Test
    void escapeCsv_null() {
        assertEquals("", CsvExport.escapeCsv(null));
    }

    @Test
    void tableExportContextSummarizesCurrentViewScope() {
        TableExportContext context = new TableExportContext(
                "JFR recording",
                "Method Profiling",
                "Hot Methods",
                "demo.jfr",
                "2026-06-06T10:00:00Z..2026-06-06T10:05:00Z",
                "duration > 10 ms",
                "2 selected rows",
                TableExportScope.CURRENT_VIEW,
                TableExportScope.VISIBLE_COLUMNS);

        assertEquals("JFR recording", context.workspace());
        assertEquals("Method Profiling", context.page());
        assertEquals("Hot Methods", context.table());
        assertEquals("demo.jfr", context.source());
        assertEquals("2026-06-06T10:00:00Z..2026-06-06T10:05:00Z", context.timeRange());
        assertEquals("duration > 10 ms", context.filter());
        assertEquals("2 selected rows", context.selection());
        assertEquals(TableExportScope.CURRENT_VIEW, context.rowScope());
        assertEquals(TableExportScope.VISIBLE_COLUMNS, context.columnScope());
    }

    @Test
    void existingExportKeepsHeaderAsFirstLine() throws IOException {
        if (!toolkitReady) {
            return; // Skip on headless CI
        }

        TableView<String> table = tableWithNameAndCount();
        Path target = Files.createTempFile("jmcfx-existing-export", ".csv");
        try {
            CsvExport.export(table, target);

            assertEquals("Name,Count", Files.readAllLines(target).getFirst());
        } finally {
            Files.deleteIfExists(target);
        }
    }

    @Test
    void requestExportWritesContextMetadataBeforeRows() throws IOException {
        if (!toolkitReady) {
            return; // Skip on headless CI
        }

        TableView<String> table = tableWithNameAndCount();
        Path target = Files.createTempFile("jmcfx-context-export", ".csv");
        TableExportContext context = new TableExportContext(
                "JFR recording",
                "Method Profiling",
                "Hot Methods",
                "demo.jfr",
                "all time",
                "none",
                "none",
                TableExportScope.CURRENT_VIEW,
                TableExportScope.VISIBLE_COLUMNS);

        CsvExport.export(new TableExportRequest(table, CsvExportOptions.visibleColumns(), context), target);

        try {
            List<String> lines = Files.readAllLines(target);
            assertEquals("# workspace,JFR recording", lines.get(0));
            assertEquals("# page,Method Profiling", lines.get(1));
            assertEquals("# table,Hot Methods", lines.get(2));
            assertTrue(lines.contains("Name,Count"));
        } finally {
            Files.deleteIfExists(target);
        }
    }

    @Test
    void export_writesHeaderAndRows() throws IOException {
        if (!toolkitReady) {
            return; // Skip on headless CI
        }

        TableView<String> table = new TableView<>();
        TableColumn<String, String> col1 = new TableColumn<>("Name");
        col1.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue()));
        TableColumn<String, String> col2 = new TableColumn<>("Value");
        col2.setCellValueFactory(data -> new ReadOnlyStringWrapper("val-" + data.getValue()));

        table.getColumns().addAll(col1, col2);
        table.setItems(FXCollections.observableArrayList("row1", "row2"));

        Path tempFile = Files.createTempFile("test", ".csv");
        try {
            CsvExport.export(table, tempFile);
            String content = Files.readString(tempFile);
            assertTrue(content.startsWith("Name,Value\n"));
            assertTrue(content.contains("row1,val-row1\n"));
            assertTrue(content.contains("row2,val-row2\n"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void export_canIncludeHiddenColumnsWhenRequested() throws IOException {
        if (!toolkitReady) {
            return; // Skip on headless CI
        }

        TableView<String> table = new TableView<>();
        TableColumn<String, String> visible = new TableColumn<>("Visible");
        visible.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue()));
        TableColumn<String, String> hidden = new TableColumn<>("Hidden");
        hidden.setVisible(false);
        hidden.setCellValueFactory(data -> new ReadOnlyStringWrapper("hidden-" + data.getValue()));
        table.getColumns().addAll(visible, hidden);
        table.setItems(FXCollections.observableArrayList("row1"));

        Path visibleOnly = Files.createTempFile("visible", ".csv");
        Path allColumns = Files.createTempFile("all", ".csv");
        try {
            CsvExport.export(table, visibleOnly);
            CsvExport.export(table, allColumns, CsvExportOptions.allColumns());

            assertEquals("Visible\nrow1\n", Files.readString(visibleOnly));
            assertEquals("Visible,Hidden\nrow1,hidden-row1\n", Files.readString(allColumns));
        } finally {
            Files.deleteIfExists(visibleOnly);
            Files.deleteIfExists(allColumns);
        }
    }

    private static TableView<String> tableWithNameAndCount() {
        TableView<String> table = new TableView<>();
        TableColumn<String, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue()));
        TableColumn<String, String> count = new TableColumn<>("Count");
        count.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().length())));
        table.getColumns().addAll(name, count);
        table.setItems(FXCollections.observableArrayList("row1", "row2"));
        return table;
    }
}
