package io.github.youngledo.jmcfx.application;

import java.nio.file.Path;
import java.util.Objects;

public final class OpenHeapDumpWorkspaceUseCase {

    private final HeapDumpApplicationServices services;

    public OpenHeapDumpWorkspaceUseCase(HeapDumpApplicationServices services) {
        this.services = Objects.requireNonNull(services, "services");
    }

    public HeapDumpWorkspacePlan open(Path path) {
        Objects.requireNonNull(path, "path");
        return new HeapDumpWorkspacePlan(path, new AnalyzeHeapDumpUseCase(services));
    }
}
