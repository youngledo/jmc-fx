package io.github.youngledo.jmcfx.domain.model;

import java.util.List;
import java.util.Objects;

public record FlightRecordingTemplate(String name, String displayName, String description) {

    public FlightRecordingTemplate {
        name = Objects.requireNonNullElse(name, "");
        displayName = Objects.requireNonNullElse(displayName, "");
        description = Objects.requireNonNullElse(description, "");
    }

    public static FlightRecordingTemplate profile() {
        return new FlightRecordingTemplate("profile", "Profile", "Low-overhead profiling settings.");
    }

    public static FlightRecordingTemplate continuous() {
        return new FlightRecordingTemplate("default", "Continuous", "Default continuous recording settings.");
    }

    public static List<FlightRecordingTemplate> predefined() {
        return List.of(profile(), continuous());
    }
}
