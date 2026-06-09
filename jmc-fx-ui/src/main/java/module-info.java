module io.github.youngledo.jmcfx.ui {
    requires transitive io.github.youngledo.jmcfx.domain;
    requires io.github.youngledo.jmcfx.application;
    requires io.github.youngledo.jmcfx.flamegraph;
    requires atlantafx.base;
    requires java.prefs;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires org.commonmark;
    requires org.apache.logging.log4j;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.material2;

    exports io.github.youngledo.jmcfx.ui.i18n;
    exports io.github.youngledo.jmcfx.ui.preferences;
    exports io.github.youngledo.jmcfx.ui.shell;

    opens css;
    opens io.github.youngledo.jmcfx.ui.i18n;
}
