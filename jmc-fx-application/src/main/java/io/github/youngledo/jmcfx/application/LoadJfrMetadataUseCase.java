package io.github.youngledo.jmcfx.application;

import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.JfrMetadataReport;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.JfrMetadataService;

public final class LoadJfrMetadataUseCase {

    private final JfrMetadataService service;

    public LoadJfrMetadataUseCase(JfrMetadataService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public JfrMetadataReport loadMetadata(RecordingSummary recording) {
        return service.loadMetadata(recording);
    }
}
