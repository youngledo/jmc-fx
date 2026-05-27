package com.youngledo.jmcfx.testsupport;

import java.util.ArrayList;
import java.util.List;

import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.StackTreeNode;
import com.youngledo.jmcfx.domain.service.ProfilingService;

public class FakeProfilingService implements ProfilingService {

    private final List<HotMethod> hotMethods = new ArrayList<>();
    private StackTreeNode callersTree = StackTreeNode.EMPTY;
    private StackTreeNode calleesTree = StackTreeNode.EMPTY;

    public void addHotMethod(HotMethod method) {
        hotMethods.add(method);
    }

    public void setCallersTree(StackTreeNode callersTree) {
        this.callersTree = callersTree == null ? StackTreeNode.EMPTY : callersTree;
    }

    public void setCalleesTree(StackTreeNode calleesTree) {
        this.calleesTree = calleesTree == null ? StackTreeNode.EMPTY : calleesTree;
    }

    @Override
    public List<HotMethod> loadHotMethods(RecordingSummary recording) {
        return List.copyOf(hotMethods);
    }

    @Override
    public StackTreeNode loadStackTraceTree(RecordingSummary recording, String method, boolean callers) {
        return callers ? callersTree : calleesTree;
    }
}
