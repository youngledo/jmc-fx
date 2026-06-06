package io.github.youngledo.jmcfx.ui.util;

import javafx.scene.control.TableView;

import java.util.function.Supplier;

public final class TableExportRequests {

    private TableExportRequests() {
    }

    public static TableExportRegistration currentView(
            TableView<?> table,
            String workspace,
            String page,
            String tableName,
            String source) {
        return new TableExportRegistration(table, () -> currentViewRequest(table, workspace, page, tableName, source));
    }

    public static TableExportRegistration currentView(
            TableView<?> table,
            String workspace,
            String page,
            String tableName,
            Supplier<String> sourceSupplier) {
        return currentView(table, workspace, page, tableName, sourceSupplier, () -> null, () -> null, () -> null);
    }

    public static TableExportRegistration currentView(
            TableView<?> table,
            String workspace,
            String page,
            String tableName,
            Supplier<String> sourceSupplier,
            Supplier<String> timeRangeSupplier,
            Supplier<String> filterSupplier,
            Supplier<String> selectionSupplier) {
        return new TableExportRegistration(table, () -> currentViewRequest(
                table,
                workspace,
                page,
                tableName,
                value(sourceSupplier),
                value(timeRangeSupplier),
                value(filterSupplier),
                value(selectionSupplier)));
    }

    public static TableExportRequest currentViewRequest(
            TableView<?> table,
            String workspace,
            String page,
            String tableName,
            String source) {
        return new TableExportRequest(
                table,
                CsvExportOptions.visibleColumns(),
                new TableExportContext(
                        workspace,
                        page,
                        tableName,
                        source,
                        null,
                        null,
                        null,
                        TableExportScope.CURRENT_VIEW,
                        TableExportScope.VISIBLE_COLUMNS));
    }

    public static TableExportRequest currentViewRequest(
            TableView<?> table,
            String workspace,
            String page,
            String tableName,
            String source,
            String timeRange,
            String filter,
            String selection) {
        return new TableExportRequest(
                table,
                CsvExportOptions.visibleColumns(),
                new TableExportContext(
                        workspace,
                        page,
                        tableName,
                        source,
                        timeRange,
                        filter,
                        selection,
                        TableExportScope.CURRENT_VIEW,
                        TableExportScope.VISIBLE_COLUMNS));
    }

    private static String value(Supplier<String> supplier) {
        return supplier == null ? null : supplier.get();
    }
}
