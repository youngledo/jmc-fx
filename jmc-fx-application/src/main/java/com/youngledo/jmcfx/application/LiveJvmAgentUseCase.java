package com.youngledo.jmcfx.application;

import java.util.List;

import com.youngledo.jmcfx.domain.model.JmcAgentPreset;
import com.youngledo.jmcfx.domain.model.JmcAgentStatus;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.service.JmcAgentService;

public final class LiveJvmAgentUseCase {

    private final JmcAgentService service;

    public LiveJvmAgentUseCase(JmcAgentService service) {
        this.service = service;
    }

    public boolean available() {
        return service != null;
    }

    public JmcAgentStatus status(JvmConnection connection) {
        return service.status(connection);
    }

    public List<JmcAgentPreset> presets() {
        return service.presets();
    }

    public void applyConfiguration(JvmConnection connection, String xmlDescription) {
        service.applyConfiguration(connection, xmlDescription);
    }
}
