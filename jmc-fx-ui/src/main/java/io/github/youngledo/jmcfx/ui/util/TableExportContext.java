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

    public String summary() {
        return summary(java.util.Locale.ENGLISH);
    }

    public String summary(java.util.Locale locale) {
        return java.util.stream.Stream.of(
                        workspace,
                        page,
                        table,
                        source,
                        timeRange,
                        filter,
                        selection,
                        scopeName(rowScope, locale),
                        scopeName(columnScope, locale))
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining(" / "));
    }

    private static String scopeName(TableExportScope scope, java.util.Locale locale) {
        return scope == null ? "" : scope.label(locale);
    }
}
