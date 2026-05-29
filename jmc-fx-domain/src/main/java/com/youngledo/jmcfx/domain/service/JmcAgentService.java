package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.JmcAgentPreset;
import com.youngledo.jmcfx.domain.model.JmcAgentStatus;
import com.youngledo.jmcfx.domain.model.JvmConnection;

public interface JmcAgentService {

    default JmcAgentStatus status(JvmConnection connection) {
        throw new JmcFxException("JMC Agent management is not supported by this service.");
    }

    default List<JmcAgentPreset> presets() {
        return List.of();
    }

    default void applyConfiguration(JvmConnection connection, String xmlDescription) {
        throw new JmcFxException("JMC Agent configuration is not supported by this service.");
    }
}
