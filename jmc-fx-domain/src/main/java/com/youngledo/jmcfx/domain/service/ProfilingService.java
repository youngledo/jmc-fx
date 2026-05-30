package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.DependencyGraphReport;
import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.StackTreeNode;

public interface ProfilingService {
    List<HotMethod> loadHotMethods(RecordingSummary recording);
    StackTreeNode loadStackTraceTree(RecordingSummary recording, String method, boolean callers);
    DependencyGraphReport loadPackageDependencies(RecordingSummary recording, int packageDepth);
}
