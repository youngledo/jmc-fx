module com.youngledo.jmcfx.adapter.jmc {
    requires com.youngledo.jmcfx.domain;
    requires java.management;
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

    exports com.youngledo.jmcfx.adapter.jmc;
}
