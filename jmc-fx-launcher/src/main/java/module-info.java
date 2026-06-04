module com.youngledo.jmcfx.launcher {
    requires com.youngledo.jmcfx.adapter.jmc;
    requires com.youngledo.jmcfx.ui;
    requires javafx.controls;
    requires javafx.graphics;
    requires org.apache.logging.log4j;

    exports com.youngledo.jmcfx.launcher to javafx.graphics;
}
