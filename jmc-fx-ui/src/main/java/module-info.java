module com.youngledo.jmcfx.ui {
    requires transitive com.youngledo.jmcfx.domain;
    requires com.youngledo.jmcfx.flamegraph;
    requires atlantafx.base;
    requires java.prefs;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires org.apache.logging.log4j;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.material2;

    exports com.youngledo.jmcfx.ui.i18n;
    exports com.youngledo.jmcfx.ui.preferences;
    exports com.youngledo.jmcfx.ui.shell;

    opens css;
    opens com.youngledo.jmcfx.ui.i18n;
}
