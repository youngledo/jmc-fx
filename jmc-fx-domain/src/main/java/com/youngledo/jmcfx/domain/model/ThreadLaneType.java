package com.youngledo.jmcfx.domain.model;

public enum ThreadLaneType {
    CPU_SAMPLE("CPU Sample"),
    BLOCKED("Blocked"),
    PARKED("Parked"),
    SLEEPING("Sleeping"),
    SOCKET_IO("Socket I/O"),
    FILE_IO("File I/O"),
    COMPILATION("Compilation"),
    CLASS_LOAD("Class Load");

    private final String displayName;

    ThreadLaneType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
