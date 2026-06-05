module io.github.youngledo.jmcfx.adapter {
    requires io.github.youngledo.jmcfx.domain;
    requires java.management;
    requires java.prefs;
    requires jdk.attach;
    requires jdk.jfr;
    requires org.apache.logging.log4j;
    requires org.openjdk.jmc.common;
    requires org.openjdk.jmc.flightrecorder;
    requires org.openjdk.jmc.flightrecorder.rules;
    requires org.openjdk.jmc.flightrecorder.rules.jdk;
    requires org.openjdk.jmc.jdp;
    requires org.openjdk.jmc.joverflow;
    requires org.openjdk.jmc.rjmx.common;

    exports io.github.youngledo.jmcfx.adapter.jmc;
    exports io.github.youngledo.jmcfx.adapter.preferences;
}
