package io.github.youngledo.jmcfx.domain.service;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.DependencyGraphReport;
import io.github.youngledo.jmcfx.domain.model.HotMethod;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.StackTreeNode;

public interface ProfilingService {
    List<HotMethod> loadHotMethods(RecordingSummary recording);
    StackTreeNode loadFlameGraphTree(RecordingSummary recording, boolean invertedStacks);
    StackTreeNode loadFlameGraphTree(RecordingSummary recording, String method, boolean invertedStacks);
    default StackTreeNode loadFlameGraphTree(
            RecordingSummary recording,
            String method,
            String frameType,
            boolean invertedStacks) {
        return loadFlameGraphTree(recording, method, invertedStacks);
    }
    StackTreeNode loadStackTraceTree(RecordingSummary recording, String method, boolean callers);
    DependencyGraphReport loadPackageDependencies(RecordingSummary recording, int packageDepth);
}
