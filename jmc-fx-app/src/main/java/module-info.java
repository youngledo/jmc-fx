module com.youngledo.jmcfx.app {
    requires com.youngledo.jmcfx.adapter.jmc;
    requires com.youngledo.jmcfx.ui;
    requires javafx.controls;
    requires javafx.graphics;
    requires org.apache.logging.log4j;

    exports com.youngledo.jmcfx.app to javafx.graphics;
}
