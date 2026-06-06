package io.github.youngledo.jmcfx.ui.util;

import javafx.scene.control.TableView;

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
}
