package io.github.youngledo.jmcfx.domain.model;

import java.util.List;
import java.util.Objects;

public record JmcAgentStatus(
        boolean available,
        String message,
        String eventProbeXml,
        List<JmcAgentTransform> transforms) {

    public JmcAgentStatus {
        message = Objects.requireNonNullElse(message, "");
        eventProbeXml = Objects.requireNonNullElse(eventProbeXml, "");
        transforms = List.copyOf(Objects.requireNonNullElse(transforms, List.of()));
    }

    public static JmcAgentStatus unavailable(String message) {
        return new JmcAgentStatus(false, message, "", List.of());
    }
}
