package io.github.youngledo.jmcfx.ui.shell;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

import io.github.youngledo.jmcfx.ui.heapdump.HeapDumpAnalysisViewModel;

/// File-backed workspace for one HPROF heap dump analysis.
public final class HeapDumpWorkspace {

    private final String id = UUID.randomUUID().toString();
    private final Path path;
    private final HeapDumpAnalysisViewModel viewModel;

    public HeapDumpWorkspace(Path path, HeapDumpAnalysisViewModel viewModel) {
        this.path = Objects.requireNonNull(path, "path");
        this.viewModel = viewModel;
    }

    public String id() {
        return id;
    }

    public Path path() {
        return path;
    }

    public String name() {
        Path fileName = path.getFileName();
        return fileName == null ? path.toString() : fileName.toString();
    }

    public HeapDumpAnalysisViewModel viewModel() {
        return viewModel;
    }

    public void close() {
        if (viewModel != null) {
            viewModel.close();
        }
    }
}
