module io.github.youngledo.jmcfx.launcher {
    requires io.github.youngledo.jmcfx.adapter;
    requires io.github.youngledo.jmcfx.application;
    requires io.github.youngledo.jmcfx.ui;
    requires javafx.controls;
    requires javafx.graphics;
    requires org.apache.logging.log4j;

    exports io.github.youngledo.jmcfx.launcher to javafx.graphics;
}
