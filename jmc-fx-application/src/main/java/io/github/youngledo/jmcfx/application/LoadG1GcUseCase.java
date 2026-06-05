package io.github.youngledo.jmcfx.application;

import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.G1GcReport;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.G1GcService;

public final class LoadG1GcUseCase {

    private final G1GcService service;

    public LoadG1GcUseCase(G1GcService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public G1GcReport loadG1GcReport(RecordingSummary recording) {
        return service.loadG1GcReport(recording);
    }
}
