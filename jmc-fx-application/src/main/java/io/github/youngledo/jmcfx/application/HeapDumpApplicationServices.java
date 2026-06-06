package io.github.youngledo.jmcfx.application;

import io.github.youngledo.jmcfx.domain.service.HeapDumpAnalysisService;
import io.github.youngledo.jmcfx.domain.service.HeapDumpBrowsingService;

public record HeapDumpApplicationServices(
        HeapDumpAnalysisService heapDumpAnalysisService,
        HeapDumpBrowsingService heapDumpBrowsingService) {
}
