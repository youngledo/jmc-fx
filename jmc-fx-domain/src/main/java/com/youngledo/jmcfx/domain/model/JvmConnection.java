package com.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record JvmConnection(
        String id,
        String displayName,
        String connectionUrl,
        boolean connected,
        JvmConnectionSource source,
        JvmConnectionState state,
        String statusMessage,
        String pid,
        String javaVersion,
        boolean attachable) {

    public JvmConnection {
        id = Objects.requireNonNullElse(id, "");
        displayName = Objects.requireNonNullElse(displayName, "");
        connectionUrl = Objects.requireNonNullElse(connectionUrl, "");
        source = source == null ? JvmConnectionSource.MANUAL : source;
        if (state == null) {
            state = connected ? JvmConnectionState.CONNECTED : JvmConnectionState.DISCONNECTED;
        }
        statusMessage = Objects.requireNonNullElse(statusMessage, "");
        pid = Objects.requireNonNullElse(pid, "");
        javaVersion = Objects.requireNonNullElse(javaVersion, "");
    }

    public JvmConnection(String id, String displayName, String connectionUrl, boolean connected,
            JvmConnectionSource source, JvmConnectionState state, String statusMessage) {
        this(id, displayName, connectionUrl, connected, source, state, statusMessage, "", "", false);
    }

    public JvmConnection(String id, String displayName, String connectionUrl, boolean connected) {
        this(id, displayName, connectionUrl, connected, JvmConnectionSource.MANUAL,
                connected ? JvmConnectionState.CONNECTED : JvmConnectionState.DISCONNECTED, "", "", "", false);
    }

    public static JvmConnection local(String pid, String displayName, String javaVersion, boolean attachable) {
        String normalizedPid = Objects.requireNonNullElse(pid, "");
        String normalizedDisplayName = Objects.requireNonNullElse(displayName, "");
        String name = normalizedDisplayName.isBlank() ? "Java process " + normalizedPid : normalizedDisplayName;
        JvmConnectionState localState = attachable ? JvmConnectionState.ATTACHABLE : JvmConnectionState.UNAVAILABLE;
        String status = attachable ? "Attachable local JVM." : "Local JVM is not attachable.";
        return new JvmConnection(normalizedPid, name, "", false, JvmConnectionSource.LOCAL, localState, status,
                normalizedPid, javaVersion, attachable);
    }

    public JvmConnection asConnected(String connectedUrl) {
        return new JvmConnection(id, displayName, connectedUrl, true, source,
                JvmConnectionState.CONNECTED, "Connected", pid, javaVersion, attachable);
    }

    public JvmConnection asDisconnected(String message) {
        return new JvmConnection(id, displayName, connectionUrl, false, source,
                JvmConnectionState.DISCONNECTED, message, pid, javaVersion, attachable);
    }
}
