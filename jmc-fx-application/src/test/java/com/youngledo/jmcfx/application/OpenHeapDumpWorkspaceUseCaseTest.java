package com.youngledo.jmcfx.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.util.List;

import com.youngledo.jmcfx.domain.model.HeapDumpAnalysisReport;
import com.youngledo.jmcfx.domain.service.HeapDumpAnalysisService;
import org.junit.jupiter.api.Test;

class OpenHeapDumpWorkspaceUseCaseTest {

    @Test
    void plansHeapDumpWorkspaceAnalysisUseCase() {
        var services = new HeapDumpApplicationServices(new FakeHeapDumpAnalysisService());
        var plan = new OpenHeapDumpWorkspaceUseCase(services).open(Path.of("sample.hprof"));

        assertEquals(Path.of("sample.hprof"), plan.path());
        assertNotNull(plan.analyzeHeapDump());
    }

    private static final class FakeHeapDumpAnalysisService implements HeapDumpAnalysisService {
        @Override
        public HeapDumpAnalysisReport analyze(Path hprofPath) {
            return new HeapDumpAnalysisReport(hprofPath, 0, 0, 0, 0, 0, 0, List.of(), "");
        }
    }
}
