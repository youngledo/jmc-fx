package com.youngledo.jmcfx.ui.testsupport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.youngledo.jmcfx.domain.model.JmcAgentPreset;
import com.youngledo.jmcfx.domain.model.JmcAgentStatus;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.service.JmcAgentService;

public class FakeJmcAgentService implements JmcAgentService {

    private final Map<String, JmcAgentStatus> statuses = new HashMap<>();
    private List<JmcAgentPreset> presets = List.of();
    private RuntimeException failure;
    private String lastAppliedConnectionId = "";
    private String lastAppliedXml = "";

    public void setStatus(String connectionId, JmcAgentStatus status) {
        statuses.put(Objects.requireNonNullElse(connectionId, ""), status);
    }

    public void setPresets(List<JmcAgentPreset> presets) {
        this.presets = List.copyOf(presets);
    }

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public String lastAppliedConnectionId() {
        return lastAppliedConnectionId;
    }

    public String lastAppliedXml() {
        return lastAppliedXml;
    }

    @Override
    public JmcAgentStatus status(JvmConnection connection) {
        failIfConfigured();
        return statuses.getOrDefault(connectionId(connection),
                JmcAgentStatus.unavailable("JMC Agent MXBean is not registered on this JVM."));
    }

    @Override
    public List<JmcAgentPreset> presets() {
        failIfConfigured();
        return List.copyOf(presets);
    }

    @Override
    public void applyConfiguration(JvmConnection connection, String xmlDescription) {
        failIfConfigured();
        lastAppliedConnectionId = connectionId(connection);
        lastAppliedXml = Objects.requireNonNullElse(xmlDescription, "");
    }

    private void failIfConfigured() {
        if (failure != null) {
            throw failure;
        }
    }

    private static String connectionId(JvmConnection connection) {
        return connection == null ? "" : connection.id();
    }
}
