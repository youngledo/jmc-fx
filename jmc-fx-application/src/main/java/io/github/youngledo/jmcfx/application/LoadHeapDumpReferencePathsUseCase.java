package io.github.youngledo.jmcfx.application;

import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.HeapDumpBrowseWindow;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePath;
import io.github.youngledo.jmcfx.domain.model.HeapDumpReferencePathRequest;

public final class LoadHeapDumpReferencePathsUseCase {

    private final HeapDumpApplicationServices services;

    public LoadHeapDumpReferencePathsUseCase(HeapDumpApplicationServices services) {
        this.services = Objects.requireNonNull(services, "services");
    }

    public HeapDumpBrowseWindow<HeapDumpReferencePath> load(HeapDumpReferencePathRequest request) {
        Objects.requireNonNull(request, "request");
        return services.heapDumpBrowsingService().loadReferencePaths(request);
    }
}
