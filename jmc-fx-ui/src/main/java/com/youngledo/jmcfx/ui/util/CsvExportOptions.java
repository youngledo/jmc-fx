package com.youngledo.jmcfx.ui.util;

public record CsvExportOptions(boolean includeHiddenColumns) {

    public static CsvExportOptions visibleColumns() {
        return new CsvExportOptions(false);
    }

    public static CsvExportOptions allColumns() {
        return new CsvExportOptions(true);
    }
}
