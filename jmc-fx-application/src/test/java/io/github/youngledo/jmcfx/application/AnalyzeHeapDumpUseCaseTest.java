package io.github.youngledo.jmcfx.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.HeapDumpAnalysisReport;
import io.github.youngledo.jmcfx.domain.service.HeapDumpAnalysisService;
import org.junit.jupiter.api.Test;

class AnalyzeHeapDumpUseCaseTest {

    @Test
    void delegatesHeapDumpAnalysisToApplicationService() {
        Path path = Path.of("jmcfx.hprof");
        HeapDumpAnalysisReport report = new HeapDumpAnalysisReport(path, 4096, 2048, 10, 8, 1, 0,
                List.of(), "raw report");
        RecordingHeapDumpAnalysisService service = new RecordingHeapDumpAnalysisService(report);
        AnalyzeHeapDumpUseCase useCase = new AnalyzeHeapDumpUseCase(new HeapDumpApplicationServices(service));

        HeapDumpAnalysisReport result = useCase.analyze(path);

        assertEquals(path, service.lastPath);
        assertEquals(report, result);
    }

    private static final class RecordingHeapDumpAnalysisService implements HeapDumpAnalysisService {
        private final HeapDumpAnalysisReport report;
        private Path lastPath;

        RecordingHeapDumpAnalysisService(HeapDumpAnalysisReport report) {
            this.report = report;
        }

        @Override
        public HeapDumpAnalysisReport analyze(Path hprofPath) {
            lastPath = hprofPath;
            return report;
        }
    }
}
