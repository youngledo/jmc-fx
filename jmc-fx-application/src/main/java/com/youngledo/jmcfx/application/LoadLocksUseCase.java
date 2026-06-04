package com.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import com.youngledo.jmcfx.domain.model.LockGrouping;
import com.youngledo.jmcfx.domain.model.LockHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.LockService;

public final class LoadLocksUseCase {

    private final LockService service;

    public LoadLocksUseCase(LockService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public List<LockHistogram> loadLockHistogram(RecordingSummary recording, LockGrouping grouping) {
        return service.loadLockHistogram(recording, grouping);
    }
}
