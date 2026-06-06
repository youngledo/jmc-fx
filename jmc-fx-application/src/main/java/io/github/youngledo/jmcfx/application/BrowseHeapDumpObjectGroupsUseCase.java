package io.github.youngledo.jmcfx.application;

import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseRequest;
import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseWindow;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroup;

public final class BrowseHeapDumpObjectGroupsUseCase {

    private final HeapDumpApplicationServices services;

    public BrowseHeapDumpObjectGroupsUseCase(HeapDumpApplicationServices services) {
        this.services = Objects.requireNonNull(services, "services");
    }

    public HeapDumpBrowseWindow<HeapDumpObjectGroup> browse(HeapDumpBrowseRequest request) {
        Objects.requireNonNull(request, "request");
        return services.heapDumpBrowsingService().browseObjectGroups(request);
    }
}
