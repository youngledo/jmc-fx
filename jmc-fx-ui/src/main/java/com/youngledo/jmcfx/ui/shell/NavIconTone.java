package com.youngledo.jmcfx.ui.shell;

enum NavIconTone {
    NEUTRAL("nav-icon-neutral"),
    WORKSPACE("nav-icon-workspace"),
    RECORDING("nav-icon-recording"),
    JAVA("nav-icon-java"),
    JVM("nav-icon-jvm"),
    MEMORY("nav-icon-memory"),
    ENVIRONMENT("nav-icon-environment"),
    APPLICATION("nav-icon-application");

    private final String styleClass;

    NavIconTone(String styleClass) {
        this.styleClass = styleClass;
    }

    String styleClass() {
        return styleClass;
    }
}
