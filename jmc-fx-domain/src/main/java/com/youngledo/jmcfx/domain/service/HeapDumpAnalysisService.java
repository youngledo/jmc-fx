package com.youngledo.jmcfx.domain.service;

import java.nio.file.Path;

import com.youngledo.jmcfx.domain.model.HeapDumpAnalysisReport;

public interface HeapDumpAnalysisService {

    HeapDumpAnalysisReport analyze(Path hprofPath);
}
