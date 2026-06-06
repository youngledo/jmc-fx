package io.github.youngledo.jmcfx.ui.util;

import javafx.scene.control.TableView;

public record TableExportRequest(
        TableView<?> table,
        CsvExportOptions options,
        TableExportContext context) {
}
