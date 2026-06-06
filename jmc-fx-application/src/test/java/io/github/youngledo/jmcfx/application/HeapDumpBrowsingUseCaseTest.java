package io.github.youngledo.jmcfx.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.domain.model.HeapDumpAnalysisReport;
import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseRequest;
import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseSort;
import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseWindow;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroup;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroupDetail;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroupKind;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferenceDirection;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePath;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePathRequest;
import io.github.youngledo.jmcfx.domain.service.HeapDumpAnalysisService;
import io.github.youngledo.jmcfx.domain.service.HeapDumpBrowsingService;

class HeapDumpBrowsingUseCaseTest {

    @Test
    void browsingUseCasesDelegateToBrowsingService() {
        RecordingBrowsingService browsing = new RecordingBrowsingService();
        var services = new HeapDumpApplicationServices(new FakeAnalysisService(), browsing);
        var request = new HeapDumpBrowseRequest(Path.of("demo.hprof"), HeapDumpObjectGroupKind.CLASS,
                HeapDumpBrowseSort.RETAINED_SIZE_BYTES, false, 0, 50, "");

        HeapDumpBrowseWindow<HeapDumpObjectGroup> groups =
                new BrowseHeapDumpObjectGroupsUseCase(services).browse(request);
        HeapDumpObjectGroupDetail detail =
                new LoadHeapDumpObjectGroupDetailUseCase(services).load(request, "group-1");

        assertSame(request, browsing.lastBrowseRequest);
        assertSame(request, browsing.lastDetailRequest);
        assertEquals("group-1", browsing.lastGroupId);
        assertEquals("java.lang.String", groups.rows().getFirst().label());
        assertEquals("java.lang.String", detail.group().label());
    }

    @Test
    void heapDumpWorkspacePlanCarriesBrowsingUseCases() {
        var services = new HeapDumpApplicationServices(new FakeAnalysisService(), new RecordingBrowsingService());
        HeapDumpWorkspacePlan plan = new OpenHeapDumpWorkspaceUseCase(services).open(Path.of("demo.hprof"));

        assertNotNull(plan.analyzeHeapDump());
        assertNotNull(plan.browseObjectGroups());
        assertNotNull(plan.loadObjectGroupDetail());
        assertNotNull(plan.loadReferencePaths());
    }

    @Test
    void referencePathUseCaseDelegatesToBrowsingService() {
        RecordingBrowsingService browsing = new RecordingBrowsingService();
        var services = new HeapDumpApplicationServices(new FakeAnalysisService(), browsing);
        var request = new HeapDumpReferencePathRequest(Path.of("demo.hprof"), "object-1",
                HeapDumpReferenceDirection.INBOUND, 12, 20, 0, 20);

        HeapDumpBrowseWindow<HeapDumpReferencePath> paths =
                new LoadHeapDumpReferencePathsUseCase(services).load(request);

        assertSame(request, browsing.lastReferencePathRequest);
        assertEquals(0, paths.rows().size());
    }

    private static final class FakeAnalysisService implements HeapDumpAnalysisService {
        @Override
        public HeapDumpAnalysisReport analyze(Path hprofPath) {
            return new HeapDumpAnalysisReport(hprofPath, 0, 0, 0, 0, 0, 0, List.of(), "");
        }
    }

    private static final class RecordingBrowsingService implements HeapDumpBrowsingService {
        private HeapDumpBrowseRequest lastBrowseRequest;
        private HeapDumpBrowseRequest lastDetailRequest;
        private HeapDumpReferencePathRequest lastReferencePathRequest;
        private String lastGroupId;

        @Override
        public HeapDumpBrowseWindow<HeapDumpObjectGroup> browseObjectGroups(HeapDumpBrowseRequest request) {
            lastBrowseRequest = request;
            return new HeapDumpBrowseWindow<>(List.of(sampleGroup()), request.offset(), request.limit(), 1, false);
        }

        @Override
        public HeapDumpObjectGroupDetail loadObjectGroupDetail(HeapDumpBrowseRequest request, String groupId) {
            lastDetailRequest = request;
            lastGroupId = groupId;
            return new HeapDumpObjectGroupDetail(sampleGroup(),
                    new HeapDumpBrowseWindow<>(List.of(), 0, 50, 0, false), "");
        }

        @Override
        public HeapDumpBrowseWindow<HeapDumpReferencePath> loadReferencePaths(HeapDumpReferencePathRequest request) {
            lastReferencePathRequest = request;
            return new HeapDumpBrowseWindow<>(List.of(), request.offset(), request.limit(), 0, false);
        }

        private static HeapDumpObjectGroup sampleGroup() {
            return new HeapDumpObjectGroup("group-1", "java.lang.String", HeapDumpObjectGroupKind.CLASS,
                    12, 1024, 4096, 128, true);
        }
    }
}
