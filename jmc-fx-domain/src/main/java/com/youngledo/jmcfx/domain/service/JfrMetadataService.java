package com.youngledo.jmcfx.domain.service;

import com.youngledo.jmcfx.domain.model.JfrMetadataReport;
import com.youngledo.jmcfx.domain.model.RecordingSummary;

public interface JfrMetadataService {
    JfrMetadataReport loadMetadata(RecordingSummary recording);
}
