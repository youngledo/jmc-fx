package com.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import com.youngledo.jmcfx.domain.model.DependencyGraphReport;
import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.StackTreeNode;
import com.youngledo.jmcfx.domain.service.ProfilingService;

public final class LoadProfilingUseCase {

    private final ProfilingService service;

    public LoadProfilingUseCase(ProfilingService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public List<HotMethod> loadHotMethods(RecordingSummary recording) {
        return service.loadHotMethods(recording);
    }

    public StackTreeNode loadFlameGraphTree(RecordingSummary recording, boolean invertedStacks) {
        return service.loadFlameGraphTree(recording, invertedStacks);
    }

    public StackTreeNode loadFlameGraphTree(RecordingSummary recording, String method, boolean invertedStacks) {
        return service.loadFlameGraphTree(recording, method, invertedStacks);
    }

    public StackTreeNode loadFlameGraphTree(
            RecordingSummary recording,
            String method,
            String frameType,
            boolean invertedStacks) {
        return service.loadFlameGraphTree(recording, method, frameType, invertedStacks);
    }

    public StackTreeNode loadStackTraceTree(RecordingSummary recording, String method, boolean callers) {
        return service.loadStackTraceTree(recording, method, callers);
    }

    public DependencyGraphReport loadPackageDependencies(RecordingSummary recording, int packageDepth) {
        return service.loadPackageDependencies(recording, packageDepth);
    }
}
