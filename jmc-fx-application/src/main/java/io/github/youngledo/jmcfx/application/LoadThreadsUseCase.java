package io.github.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.ThreadSummary;
import io.github.youngledo.jmcfx.domain.service.ThreadService;

public final class LoadThreadsUseCase {

    private final ThreadService service;

    public LoadThreadsUseCase(ThreadService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public List<ThreadSummary> loadThreadSummaries(RecordingSummary recording) {
        return service.loadThreadSummaries(recording);
    }
}
