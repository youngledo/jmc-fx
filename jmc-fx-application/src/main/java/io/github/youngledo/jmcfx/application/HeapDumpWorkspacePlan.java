package io.github.youngledo.jmcfx.application;

import java.nio.file.Path;
import java.util.Objects;

public record HeapDumpWorkspacePlan(
        Path path,
        AnalyzeHeapDumpUseCase analyzeHeapDump,
        BrowseHeapDumpObjectGroupsUseCase browseObjectGroups,
        LoadHeapDumpObjectGroupDetailUseCase loadObjectGroupDetail,
        LoadHeapDumpReferencePathsUseCase loadReferencePaths) {

    public HeapDumpWorkspacePlan {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(analyzeHeapDump, "analyzeHeapDump");
        Objects.requireNonNull(browseObjectGroups, "browseObjectGroups");
        Objects.requireNonNull(loadObjectGroupDetail, "loadObjectGroupDetail");
        Objects.requireNonNull(loadReferencePaths, "loadReferencePaths");
    }
}
