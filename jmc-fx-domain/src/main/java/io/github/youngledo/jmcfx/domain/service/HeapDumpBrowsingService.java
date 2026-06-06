package io.github.youngledo.jmcfx.domain.service;

import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseRequest;
import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseWindow;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroup;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroupDetail;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePath;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePathRequest;

public interface HeapDumpBrowsingService {

    HeapDumpBrowseWindow<HeapDumpObjectGroup> browseObjectGroups(HeapDumpBrowseRequest request);

    HeapDumpObjectGroupDetail loadObjectGroupDetail(HeapDumpBrowseRequest request, String groupId);

    HeapDumpBrowseWindow<HeapDumpReferencePath> loadReferencePaths(HeapDumpReferencePathRequest request);
}
