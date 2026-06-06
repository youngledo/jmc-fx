package io.github.youngledo.jmcfx.ui.util;

public record TableExportContext(
        String workspace,
        String page,
        String table,
        String source,
        String timeRange,
        String filter,
        String selection,
        TableExportScope rowScope,
        TableExportScope columnScope) {
}
