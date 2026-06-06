package io.github.youngledo.jmcfx.adapter.jmc;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseRequest;
import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseWindow;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroup;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroupDetail;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroupKind;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePath;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePathRequest;
import io.github.youngledo.jmcfx.domain.service.HeapDumpBrowsingService;

public class JmcHeapDumpBrowsingService implements HeapDumpBrowsingService {

    @Override
    public HeapDumpBrowseWindow<HeapDumpObjectGroup> browseObjectGroups(HeapDumpBrowseRequest request) {
        return new HeapDumpBrowseWindow<>(List.of(), request.offset(), request.limit(), 0, false);
    }

    @Override
    public HeapDumpObjectGroupDetail loadObjectGroupDetail(HeapDumpBrowseRequest request, String groupId) {
        HeapDumpObjectGroup group = new HeapDumpObjectGroup(groupId, groupId, HeapDumpObjectGroupKind.CLASS,
                0, 0, 0, 0, false);
        return new HeapDumpObjectGroupDetail(group, new HeapDumpBrowseWindow<>(List.of(), 0, request.limit(), 0, false),
                "Heap dump browsing data is not available yet.");
    }

    @Override
    public HeapDumpBrowseWindow<HeapDumpReferencePath> loadReferencePaths(HeapDumpReferencePathRequest request) {
        return new HeapDumpBrowseWindow<>(List.of(), request.offset(), request.limit(), 0, false);
    }
}
