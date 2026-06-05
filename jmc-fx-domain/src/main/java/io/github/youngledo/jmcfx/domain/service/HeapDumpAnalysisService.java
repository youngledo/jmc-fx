package io.github.youngledo.jmcfx.domain.service;

import java.nio.file.Path;

import io.github.youngledo.jmcfx.domain.model.HeapDumpAnalysisReport;

public interface HeapDumpAnalysisService {

    HeapDumpAnalysisReport analyze(Path hprofPath);
}
