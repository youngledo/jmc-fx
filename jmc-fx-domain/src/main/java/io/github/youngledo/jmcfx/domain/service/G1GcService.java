package io.github.youngledo.jmcfx.domain.service;

import io.github.youngledo.jmcfx.domain.model.G1GcReport;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;

public interface G1GcService {

    G1GcReport loadG1GcReport(RecordingSummary recording);
}
