package com.youngledo.jmcfx.testsupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.youngledo.jmcfx.domain.model.DependencyGraphReport;
import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.StackTreeNode;
import com.youngledo.jmcfx.domain.service.ProfilingService;

public class FakeProfilingService implements ProfilingService {

    private final List<HotMethod> hotMethods = new ArrayList<>();
    private StackTreeNode callersTree = StackTreeNode.EMPTY;
    private StackTreeNode calleesTree = StackTreeNode.EMPTY;
    private StackTreeNode flameGraphTree = StackTreeNode.EMPTY;
    private StackTreeNode invertedFlameGraphTree = StackTreeNode.EMPTY;
    private final Map<String, StackTreeNode> methodFlameGraphTrees = new HashMap<>();
    private final Map<String, StackTreeNode> methodInvertedFlameGraphTrees = new HashMap<>();
    private final Map<MethodFrameTypeKey, StackTreeNode> methodFrameTypeFlameGraphTrees = new HashMap<>();
    private final Map<MethodFrameTypeKey, StackTreeNode> methodFrameTypeInvertedFlameGraphTrees = new HashMap<>();
    private DependencyGraphReport dependencyReport = DependencyGraphReport.EMPTY;

    public void addHotMethod(HotMethod method) {
        hotMethods.add(method);
    }

    public void setCallersTree(StackTreeNode callersTree) {
        this.callersTree = callersTree == null ? StackTreeNode.EMPTY : callersTree;
    }

    public void setCalleesTree(StackTreeNode calleesTree) {
        this.calleesTree = calleesTree == null ? StackTreeNode.EMPTY : calleesTree;
    }

    public void setFlameGraphTree(StackTreeNode flameGraphTree) {
        this.flameGraphTree = flameGraphTree == null ? StackTreeNode.EMPTY : flameGraphTree;
    }

    public void setInvertedFlameGraphTree(StackTreeNode invertedFlameGraphTree) {
        this.invertedFlameGraphTree = invertedFlameGraphTree == null ? StackTreeNode.EMPTY : invertedFlameGraphTree;
    }

    public void setMethodFlameGraphTree(String method, StackTreeNode flameGraphTree) {
        methodFlameGraphTrees.put(method, flameGraphTree == null ? StackTreeNode.EMPTY : flameGraphTree);
    }

    public void setMethodInvertedFlameGraphTree(String method, StackTreeNode invertedFlameGraphTree) {
        methodInvertedFlameGraphTrees.put(method,
                invertedFlameGraphTree == null ? StackTreeNode.EMPTY : invertedFlameGraphTree);
    }

    public void setMethodFrameTypeFlameGraphTree(String method, String frameType, StackTreeNode flameGraphTree) {
        methodFrameTypeFlameGraphTrees.put(new MethodFrameTypeKey(method, frameType),
                flameGraphTree == null ? StackTreeNode.EMPTY : flameGraphTree);
    }

    public void setMethodFrameTypeInvertedFlameGraphTree(
            String method,
            String frameType,
            StackTreeNode invertedFlameGraphTree) {
        methodFrameTypeInvertedFlameGraphTrees.put(new MethodFrameTypeKey(method, frameType),
                invertedFlameGraphTree == null ? StackTreeNode.EMPTY : invertedFlameGraphTree);
    }

    public void setDependencyReport(DependencyGraphReport dependencyReport) {
        this.dependencyReport = dependencyReport == null ? DependencyGraphReport.EMPTY : dependencyReport;
    }

    @Override
    public List<HotMethod> loadHotMethods(RecordingSummary recording) {
        return List.copyOf(hotMethods);
    }

    @Override
    public StackTreeNode loadFlameGraphTree(RecordingSummary recording, boolean invertedStacks) {
        return invertedStacks ? invertedFlameGraphTree : flameGraphTree;
    }

    @Override
    public StackTreeNode loadFlameGraphTree(RecordingSummary recording, String method, boolean invertedStacks) {
        Map<String, StackTreeNode> trees = invertedStacks ? methodInvertedFlameGraphTrees : methodFlameGraphTrees;
        StackTreeNode tree = trees.get(method);
        if (tree != null) {
            return tree;
        }
        return loadFlameGraphTree(recording, invertedStacks);
    }

    @Override
    public StackTreeNode loadFlameGraphTree(
            RecordingSummary recording,
            String method,
            String frameType,
            boolean invertedStacks) {
        Map<MethodFrameTypeKey, StackTreeNode> trees =
                invertedStacks ? methodFrameTypeInvertedFlameGraphTrees : methodFrameTypeFlameGraphTrees;
        StackTreeNode tree = trees.get(new MethodFrameTypeKey(method, frameType));
        if (tree != null) {
            return tree;
        }
        return loadFlameGraphTree(recording, method, invertedStacks);
    }

    @Override
    public StackTreeNode loadStackTraceTree(RecordingSummary recording, String method, boolean callers) {
        return callers ? callersTree : calleesTree;
    }

    @Override
    public DependencyGraphReport loadPackageDependencies(RecordingSummary recording, int packageDepth) {
        return dependencyReport;
    }

    private record MethodFrameTypeKey(String method, String frameType) {
    }
}
