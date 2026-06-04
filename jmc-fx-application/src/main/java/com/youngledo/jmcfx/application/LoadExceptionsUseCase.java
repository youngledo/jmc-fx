package com.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.ExceptionGrouping;
import com.youngledo.jmcfx.domain.model.ExceptionSummary;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.ExceptionService;

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
