package io.github.youngledo.jmcfx.domain.service;

import io.github.youngledo.jmcfx.domain.model.JfrMetadataReport;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;

public interface JfrMetadataService {
    JfrMetadataReport loadMetadata(RecordingSummary recording);
}
