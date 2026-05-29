package com.youngledo.jmcfx.ui.shell;

import java.util.Objects;
import java.util.UUID;

/// Workspace tab identity for live JVM discovery and connected JVM tools.
public final class LiveJvmWorkspace {

    private final String id = UUID.randomUUID().toString();
    private final String name;

    public LiveJvmWorkspace(String name) {
        this.name = Objects.requireNonNullElse(name, "JVM");
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }
}
