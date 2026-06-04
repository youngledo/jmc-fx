package com.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.ThreadSummary;
import com.youngledo.jmcfx.domain.service.ThreadService;

public final class LoadThreadsUseCase {

    private final ThreadService service;

    public LoadThreadsUseCase(ThreadService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public List<ThreadSummary> loadThreadSummaries(RecordingSummary recording) {
        return service.loadThreadSummaries(recording);
    }
}
