module io.github.youngledo.jmcfx.adapter {
    requires com.fasterxml.jackson.databind;
    requires io.github.youngledo.jmcfx.domain;
    requires java.management;
    requires java.net.http;
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

    exports io.github.youngledo.jmcfx.adapter.ai;
    exports io.github.youngledo.jmcfx.adapter.jmc;
    exports io.github.youngledo.jmcfx.adapter.preferences;
    exports io.github.youngledo.jmcfx.adapter.preferences.ai;
}
