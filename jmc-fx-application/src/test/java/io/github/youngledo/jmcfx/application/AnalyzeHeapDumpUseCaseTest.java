package io.github.youngledo.jmcfx.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseRequest;
import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseWindow;
import io.github.youngledo.jmcfx.domain.model.HeapDumpAnalysisReport;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroup;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroupDetail;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroupKind;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePath;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePathRequest;
import io.github.youngledo.jmcfx.domain.service.HeapDumpAnalysisService;
import io.github.youngledo.jmcfx.domain.service.HeapDumpBrowsingService;
import org.junit.jupiter.api.Test;

class AnalyzeHeapDumpUseCaseTest {

    @Test
    void delegatesHeapDumpAnalysisToApplicationService() {
        Path path = Path.of("jmcfx.hprof");
        HeapDumpAnalysisReport report = new HeapDumpAnalysisReport(path, 4096, 2048, 10, 8, 1, 0,
                List.of(), "raw report");
        RecordingHeapDumpAnalysisService service = new RecordingHeapDumpAnalysisService(report);
        AnalyzeHeapDumpUseCase useCase = new AnalyzeHeapDumpUseCase(services(service));

        HeapDumpAnalysisReport result = useCase.analyze(path);

        assertEquals(path, service.lastPath);
        assertEquals(report, result);
    }

    private static HeapDumpApplicationServices services(HeapDumpAnalysisService analysisService) {
        return new HeapDumpApplicationServices(analysisService, new FakeHeapDumpBrowsingService());
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

    private static final class FakeHeapDumpBrowsingService implements HeapDumpBrowsingService {
        @Override
        public HeapDumpBrowseWindow<HeapDumpObjectGroup> browseObjectGroups(HeapDumpBrowseRequest request) {
            return new HeapDumpBrowseWindow<>(List.of(), request.offset(), request.limit(), 0, false);
        }

        @Override
        public HeapDumpObjectGroupDetail loadObjectGroupDetail(HeapDumpBrowseRequest request, String groupId) {
            return new HeapDumpObjectGroupDetail(new HeapDumpObjectGroup(groupId, groupId,
                    HeapDumpObjectGroupKind.CLASS, 0, 0, 0, 0, false),
                    new HeapDumpBrowseWindow<>(List.of(), 0, 50, 0, false), "");
        }

        @Override
        public HeapDumpBrowseWindow<HeapDumpReferencePath> loadReferencePaths(HeapDumpReferencePathRequest request) {
            return new HeapDumpBrowseWindow<>(List.of(), request.offset(), request.limit(), 0, false);
        }
    }
}
