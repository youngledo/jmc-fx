package com.youngledo.jmcfx.testsupport;

import java.util.ArrayList;
import java.util.List;

import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.StackTreeNode;
import com.youngledo.jmcfx.domain.service.ProfilingService;

public class FakeProfilingService implements ProfilingService {

    private final List<HotMethod> hotMethods = new ArrayList<>();

    public void addHotMethod(HotMethod method) {
        hotMethods.add(method);
    }

    @Override
    public List<HotMethod> loadHotMethods(RecordingSummary recording) {
        return List.copyOf(hotMethods);
    }

    @Override
    public StackTreeNode loadStackTraceTree(RecordingSummary recording, String method, boolean callers) {
        return StackTreeNode.EMPTY;
    }
}
