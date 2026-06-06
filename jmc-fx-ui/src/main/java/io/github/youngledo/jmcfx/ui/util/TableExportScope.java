package io.github.youngledo.jmcfx.ui.util;

public enum TableExportScope {
    CURRENT_VIEW("Current view", "当前视图"),
    CURRENT_LOADED_ROWS("Current loaded rows", "当前已加载行"),
    VISIBLE_COLUMNS("Visible columns", "可见列"),
    ALL_COLUMNS("All columns", "全部列");

    private final String englishLabel;
    private final String chineseLabel;

    TableExportScope(String englishLabel, String chineseLabel) {
        this.englishLabel = englishLabel;
        this.chineseLabel = chineseLabel;
    }

    public String label(java.util.Locale locale) {
        java.util.Locale resolvedLocale = locale == null ? java.util.Locale.ENGLISH : locale;
        return resolvedLocale.getLanguage().equals(java.util.Locale.CHINESE.getLanguage())
                ? chineseLabel
                : englishLabel;
    }
}
