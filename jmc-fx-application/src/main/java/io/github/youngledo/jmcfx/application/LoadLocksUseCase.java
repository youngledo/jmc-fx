package io.github.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.LockGrouping;
import io.github.youngledo.jmcfx.domain.model.LockHistogram;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.LockService;

public final class LoadLocksUseCase {

    private final LockService service;

    public LoadLocksUseCase(LockService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public List<LockHistogram> loadLockHistogram(RecordingSummary recording, LockGrouping grouping) {
        return service.loadLockHistogram(recording, grouping);
    }
}
