package com.youngledo.jmcfx.domain.model;

import java.util.Objects;

public record FlightRecordingStartRequest(
        JvmConnection connection,
        String name,
        FlightRecordingTemplate template) {

    public FlightRecordingStartRequest {
        connection = Objects.requireNonNull(connection, "connection");
        name = Objects.requireNonNullElse(name, "");
        template = template == null ? FlightRecordingTemplate.profile() : template;
    }
}
