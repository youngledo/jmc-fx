package io.github.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.ExceptionGrouping;
import io.github.youngledo.jmcfx.domain.model.ExceptionSummary;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.ExceptionService;

public final class LoadExceptionsUseCase {

    private final ExceptionService service;

    public LoadExceptionsUseCase(ExceptionService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public List<ExceptionSummary> loadHistogram(RecordingSummary recording, ExceptionGrouping grouping) {
        return service.loadHistogram(recording, grouping);
    }

    public ChartDefinition loadTimeline(RecordingSummary recording) {
        return service.loadTimeline(recording);
    }
}
