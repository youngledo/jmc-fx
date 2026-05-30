package com.youngledo.jmcfx.domain.service;

import com.youngledo.jmcfx.domain.model.G1GcReport;
import com.youngledo.jmcfx.domain.model.RecordingSummary;

public interface G1GcService {

    G1GcReport loadG1GcReport(RecordingSummary recording);
}
