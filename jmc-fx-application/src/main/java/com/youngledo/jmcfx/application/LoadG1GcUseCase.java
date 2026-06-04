package com.youngledo.jmcfx.application;

import java.util.Objects;

import com.youngledo.jmcfx.domain.model.G1GcReport;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.G1GcService;

public final class LoadG1GcUseCase {

    private final G1GcService service;

    public LoadG1GcUseCase(G1GcService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public G1GcReport loadG1GcReport(RecordingSummary recording) {
        return service.loadG1GcReport(recording);
    }
}
