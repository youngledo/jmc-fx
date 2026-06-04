package com.youngledo.jmcfx.ui.testsupport;

import java.nio.file.Path;

import com.youngledo.jmcfx.domain.model.HeapDumpAnalysisReport;
import com.youngledo.jmcfx.domain.service.HeapDumpAnalysisService;

public class FakeHeapDumpAnalysisService implements HeapDumpAnalysisService {

    private HeapDumpAnalysisReport report;
    private RuntimeException exception;
    private Path lastPath;

    public void setReport(HeapDumpAnalysisReport report) {
        this.report = report;
    }

    public void setException(RuntimeException exception) {
        this.exception = exception;
    }

    public Path lastPath() {
        return lastPath;
    }

    @Override
    public HeapDumpAnalysisReport analyze(Path hprofPath) {
        lastPath = hprofPath;
        if (exception != null) {
            throw exception;
        }
        return report;
    }
}
