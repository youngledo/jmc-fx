package com.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record JmcAgentPreset(String id, String name, String description, String xml) {

    public JmcAgentPreset {
        id = Objects.requireNonNullElse(id, "");
        name = Objects.requireNonNullElse(name, "");
        description = Objects.requireNonNullElse(description, "");
        xml = Objects.requireNonNullElse(xml, "");
    }
}
