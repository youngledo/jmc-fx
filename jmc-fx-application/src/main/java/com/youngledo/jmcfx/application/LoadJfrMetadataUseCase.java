package com.youngledo.jmcfx.application;

import java.util.Objects;

import com.youngledo.jmcfx.domain.model.JfrMetadataReport;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JfrMetadataService;

public final class LoadJfrMetadataUseCase {

    private final JfrMetadataService service;

    public LoadJfrMetadataUseCase(JfrMetadataService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public JfrMetadataReport loadMetadata(RecordingSummary recording) {
        return service.loadMetadata(recording);
    }
}
