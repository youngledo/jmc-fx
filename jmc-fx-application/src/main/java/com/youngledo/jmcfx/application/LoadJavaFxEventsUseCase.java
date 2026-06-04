package com.youngledo.jmcfx.application;

import java.util.Objects;

import com.youngledo.jmcfx.domain.model.JavaFxEventReport;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JavaFxEventService;

public final class LoadJavaFxEventsUseCase {

    private final JavaFxEventService service;

    public LoadJavaFxEventsUseCase(JavaFxEventService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public JavaFxEventReport loadJavaFxEvents(RecordingSummary recording) {
        return service.loadJavaFxEvents(recording);
    }
}
