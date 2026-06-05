package io.github.youngledo.jmcfx.application;

import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.JavaFxEventReport;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.JavaFxEventService;

public final class LoadJavaFxEventsUseCase {

    private final JavaFxEventService service;

    public LoadJavaFxEventsUseCase(JavaFxEventService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public JavaFxEventReport loadJavaFxEvents(RecordingSummary recording) {
        return service.loadJavaFxEvents(recording);
    }
}
