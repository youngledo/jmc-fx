package io.github.youngledo.jmcfx.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.HeapDumpAnalysisReport;
import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseRequest;
import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseWindow;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroup;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroupDetail;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroupKind;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePath;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePathRequest;
import io.github.youngledo.jmcfx.domain.service.HeapDumpAnalysisService;
import io.github.youngledo.jmcfx.domain.service.HeapDumpBrowsingService;
import org.junit.jupiter.api.Test;

class OpenHeapDumpWorkspaceUseCaseTest {

    @Test
    void plansHeapDumpWorkspaceAnalysisUseCase() {
        var services = services(new FakeHeapDumpAnalysisService());
        var plan = new OpenHeapDumpWorkspaceUseCase(services).open(Path.of("sample.hprof"));

        assertEquals(Path.of("sample.hprof"), plan.path());
        assertNotNull(plan.analyzeHeapDump());
        assertNotNull(plan.browseObjectGroups());
        assertNotNull(plan.loadObjectGroupDetail());
        assertNotNull(plan.loadReferencePaths());
    }

    private static HeapDumpApplicationServices services(HeapDumpAnalysisService analysisService) {
        return new HeapDumpApplicationServices(analysisService, new FakeHeapDumpBrowsingService());
    }

    private static final class FakeHeapDumpAnalysisService implements HeapDumpAnalysisService {
        @Override
        public HeapDumpAnalysisReport analyze(Path hprofPath) {
            return new HeapDumpAnalysisReport(hprofPath, 0, 0, 0, 0, 0, 0, List.of(), "");
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
