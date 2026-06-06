package io.github.youngledo.jmcfx.ui.util;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Collectors;

/// Exports TableView data to CSV format.
///
/// Uses only Java standard library. Handles CSV escaping for
/// commas, quotes, and newlines per RFC 4180.
public final class CsvExport {

    private CsvExport() {}

    /// Exports all rows and columns of a TableView to a CSV file.
    ///
    /// @param table the TableView to export
    /// @param target the file path to write to
    /// @throws IOException if writing fails
    public static void export(TableView<?> table, Path target) throws IOException {
        export(table, target, CsvExportOptions.visibleColumns());
    }

    public static void export(TableView<?> table, Path target, CsvExportOptions options) throws IOException {
        CsvExportOptions exportOptions = options == null ? CsvExportOptions.visibleColumns() : options;
        try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            writeTable(table, writer, exportOptions);
        }
    }

    public static void export(TableExportRequest request, Path target) throws IOException {
        TableExportRequest exportRequest = Objects.requireNonNull(request, "request");
        CsvExportOptions exportOptions = exportRequest.options() == null
                ? CsvExportOptions.visibleColumns()
                : exportRequest.options();
        try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            writeContext(writer, exportRequest.context());
            writeTable(exportRequest.table(), writer, exportOptions);
        }
    }

    private static void writeContext(BufferedWriter writer, TableExportContext context) throws IOException {
        if (context == null) {
            return;
        }
        writeContextLine(writer, "workspace", context.workspace());
        writeContextLine(writer, "page", context.page());
        writeContextLine(writer, "table", context.table());
        writeContextLine(writer, "source", context.source());
        writeContextLine(writer, "timeRange", context.timeRange());
        writeContextLine(writer, "filter", context.filter());
        writeContextLine(writer, "selection", context.selection());
        writeContextLine(writer, "rowScope", context.rowScope() == null ? "" : context.rowScope().name());
        writeContextLine(writer, "columnScope", context.columnScope() == null ? "" : context.columnScope().name());
    }

    private static void writeContextLine(BufferedWriter writer, String key, String value) throws IOException {
        if (value == null || value.isBlank()) {
            return;
        }
        writer.write("# ");
        writer.write(escapeCsv(key));
        writer.write(",");
        writer.write(escapeCsv(value));
        writer.newLine();
    }

    private static void writeTable(TableView<?> table, BufferedWriter writer, CsvExportOptions exportOptions)
            throws IOException {
        // Header row
        String header = table.getColumns().stream()
                .filter(col -> exportOptions.includeHiddenColumns() || col.isVisible())
                .map(col -> escapeCsv(col.getText()))
                .collect(Collectors.joining(","));
        writer.write(header);
        writer.newLine();

        // Data rows
        for (Object item : table.getItems()) {
            String row = table.getColumns().stream()
                    .filter(col -> exportOptions.includeHiddenColumns() || col.isVisible())
                    .map(col -> cellValue(col, item))
                    .collect(Collectors.joining(","));
            writer.write(row);
            writer.newLine();
        }
    }

    static String escapeCsv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String cellValue(TableColumn<?, ?> col, Object item) {
        TableView rawTable = col.getTableView();
        TableColumn rawCol = col;
        var cellData = col.getCellValueFactory().call(
                new TableColumn.CellDataFeatures<>(rawTable, rawCol, item));
        if (cellData == null) {
            return "";
        }
        Object value = cellData.getValue();
        return escapeCsv(value != null ? value.toString() : "");
    }
}
