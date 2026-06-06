package io.github.youngledo.jmcfx.application;

import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseRequest;
import io.github.youngledo.jmcfx.domain.model.HeapDumpObjectGroupDetail;

public final class LoadHeapDumpObjectGroupDetailUseCase {

    private final HeapDumpApplicationServices services;

    public LoadHeapDumpObjectGroupDetailUseCase(HeapDumpApplicationServices services) {
        this.services = Objects.requireNonNull(services, "services");
    }

    public HeapDumpObjectGroupDetail load(HeapDumpBrowseRequest request, String groupId) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(groupId, "groupId");
        return services.heapDumpBrowsingService().loadObjectGroupDetail(request, groupId);
    }
}
