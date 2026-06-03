package com.youngledo.jmcfx.ui.profiling;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import com.youngledo.jmcfx.domain.model.DependencyGraphEdge;
import com.youngledo.jmcfx.domain.model.DependencyGraphReport;
import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.StackFrameInfo;
import com.youngledo.jmcfx.domain.model.StackTreeNode;
import com.youngledo.jmcfx.domain.service.ProfilingService;
import com.youngledo.jmcfx.flamegraph.FlameGraphModel;
import com.youngledo.jmcfx.ui.util.FxDispatch;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// View model for the v1 Profiling page.
///
/// Manages hot-methods list and callers/callees stack trees for a selected recording.
public class ProfilingViewModel {

    private final ProfilingService profilingService;
    private final ObservableList<HotMethod> hotMethods = FXCollections.observableArrayList();
    private final ObservableList<DependencyGraphEdge> dependencyEdges = FXCollections.observableArrayList();
    private final ObjectProperty<HotMethod> selectedMethod = new SimpleObjectProperty<>();
    private final ObjectProperty<DependencyGraphEdge> selectedDependencyEdge = new SimpleObjectProperty<>();
    private final ObjectProperty<StackTreeNode> callersTree = new SimpleObjectProperty<>(StackTreeNode.EMPTY);
    private final ObjectProperty<StackTreeNode> calleesTree = new SimpleObjectProperty<>(StackTreeNode.EMPTY);
    private final ObjectProperty<FlameGraphModel<StackFrameInfo>> callersFlameGraph =
            new SimpleObjectProperty<>(FlameGraphModel.empty());
    private final ObjectProperty<FlameGraphModel<StackFrameInfo>> calleesFlameGraph =
            new SimpleObjectProperty<>(FlameGraphModel.empty());
    private final ObjectProperty<Integer> flameGraphEventCount = new SimpleObjectProperty<>();
    private final ObjectProperty<CallGraphDirection> callGraphDirection =
            new SimpleObjectProperty<>(CallGraphDirection.CALLEES);
    private final ObjectProperty<CallGraphLayout> callGraph =
            new SimpleObjectProperty<>(CallGraphLayout.EMPTY);
    private final ObjectProperty<CallGraphLayout> dependencyGraph =
            new SimpleObjectProperty<>(CallGraphLayout.EMPTY);
    private final ObjectProperty<Integer> callGraphMaxDepth =
            new SimpleObjectProperty<>(CallGraphLayoutBuilder.DEFAULT_MAX_DEPTH);
    private final ObjectProperty<Integer> dependencyPackageDepth =
            new SimpleObjectProperty<>(2);
    private RecordingSummary currentRecording;
    private String selectedMethodName;

    public ProfilingViewModel(ProfilingService profilingService) {
        this.profilingService = profilingService;
    }

    public ObservableList<HotMethod> hotMethodsProperty() {
        return hotMethods;
    }

    public ObjectProperty<HotMethod> selectedMethodProperty() {
        return selectedMethod;
    }

    public ObservableList<DependencyGraphEdge> dependencyEdgesProperty() {
        return dependencyEdges;
    }

    public ObjectProperty<DependencyGraphEdge> selectedDependencyEdgeProperty() {
        return selectedDependencyEdge;
    }

    public ObjectProperty<StackTreeNode> callersTreeProperty() {
        return callersTree;
    }

    public ObjectProperty<StackTreeNode> calleesTreeProperty() {
        return calleesTree;
    }

    public ObjectProperty<FlameGraphModel<StackFrameInfo>> callersFlameGraphProperty() {
        return callersFlameGraph;
    }

    public ObjectProperty<FlameGraphModel<StackFrameInfo>> calleesFlameGraphProperty() {
        return calleesFlameGraph;
    }

    public ObjectProperty<Integer> flameGraphEventCountProperty() {
        return flameGraphEventCount;
    }

    public ObjectProperty<CallGraphDirection> callGraphDirectionProperty() {
        return callGraphDirection;
    }

    public ObjectProperty<CallGraphLayout> callGraphProperty() {
        return callGraph;
    }

    public ObjectProperty<CallGraphLayout> dependencyGraphProperty() {
        return dependencyGraph;
    }

    public ObjectProperty<Integer> callGraphMaxDepthProperty() {
        return callGraphMaxDepth;
    }

    public ObjectProperty<Integer> dependencyPackageDepthProperty() {
        return dependencyPackageDepth;
    }

    public void load(RecordingSummary recording) {
        currentRecording = recording;
        List<HotMethod> methods = profilingService.loadHotMethods(recording);
        DependencyGraphReport dependencyReport =
                profilingService.loadPackageDependencies(recording, resolvedPackageDepth());
        CallGraphLayout dependencyGraphLayout = buildDependencyGraph(dependencyReport);
        FxDispatch.run(() -> {
            hotMethods.setAll(methods);
            dependencyEdges.setAll(dependencyReport.edges());
            selectedDependencyEdge.set(null);
            selectedMethod.set(null);
            selectedMethodName = null;
            clearStackDetails();
            dependencyGraph.set(dependencyGraphLayout);
        });
    }

    public void setDependencyPackageDepth(int packageDepth) {
        int resolvedDepth = Math.max(1, packageDepth);
        dependencyPackageDepth.set(resolvedDepth);
        if (currentRecording == null) {
            return;
        }
        DependencyGraphReport dependencyReport =
                profilingService.loadPackageDependencies(currentRecording, resolvedDepth);
        CallGraphLayout dependencyGraphLayout = buildDependencyGraph(dependencyReport);
        FxDispatch.run(() -> {
            dependencyEdges.setAll(dependencyReport.edges());
            selectedDependencyEdge.set(null);
            dependencyGraph.set(dependencyGraphLayout);
        });
    }

    public void setCallGraphDirection(CallGraphDirection direction) {
        FxDispatch.run(() -> {
            callGraphDirection.set(direction == null ? CallGraphDirection.CALLEES : direction);
            rebuildCallGraph();
        });
    }

    public void setCallGraphMaxDepth(int maxDepth) {
        FxDispatch.run(() -> {
            callGraphMaxDepth.set(Math.max(1, maxDepth));
            rebuildCallGraph();
        });
    }

    public void selectMethod(String method) {
        selectMethod(method, null);
    }

    public void selectMethod(HotMethod method) {
        if (method == null) {
            selectMethod((String) null);
            return;
        }
        selectMethod(method.method(), method.frameType());
    }

    private void selectMethod(String method, String frameType) {
        if (currentRecording == null) {
            return;
        }
        if (method == null) {
            FxDispatch.run(() -> {
                selectedMethodName = null;
                clearStackDetails();
            });
            return;
        }
        StackTreeNode callers = stackTreeOrEmpty(profilingService.loadStackTraceTree(currentRecording, method, true));
        StackTreeNode callees = stackTreeOrEmpty(profilingService.loadStackTraceTree(currentRecording, method, false));
        StackTreeNode flameGraphTree =
                stackTreeOrEmpty(profilingService.loadFlameGraphTree(currentRecording, method, frameType, false));
        StackTreeNode invertedFlameGraphTree =
                stackTreeOrEmpty(profilingService.loadFlameGraphTree(currentRecording, method, frameType, true));
        FlameGraphModel<StackFrameInfo> flameGraph = ProfilingFlameGraphAdapter.toModel(flameGraphTree);
        FlameGraphModel<StackFrameInfo> invertedFlameGraph = ProfilingFlameGraphAdapter.toModel(invertedFlameGraphTree);
        FxDispatch.run(() -> {
            selectedMethodName = method;
            callersTree.set(callers);
            calleesTree.set(callees);
            callGraph.set(buildCallGraph(method, callers, callees));
            callersFlameGraph.set(flameGraph);
            calleesFlameGraph.set(invertedFlameGraph);
            flameGraphEventCount.set(flameGraphEventCount(flameGraphTree));
        });
    }

    private void clearStackDetails() {
        clearCallDetails();
        callersFlameGraph.set(FlameGraphModel.empty());
        calleesFlameGraph.set(FlameGraphModel.empty());
        flameGraphEventCount.set(null);
    }

    private void clearCallDetails() {
        callersTree.set(StackTreeNode.EMPTY);
        calleesTree.set(StackTreeNode.EMPTY);
        callGraph.set(CallGraphLayout.EMPTY);
    }

    private void rebuildCallGraph() {
        if (selectedMethodName == null) {
            callGraph.set(CallGraphLayout.EMPTY);
            return;
        }
        callGraph.set(buildCallGraph(selectedMethodName, callersTree.get(), calleesTree.get()));
    }

    private CallGraphLayout buildCallGraph(String method, StackTreeNode callers, StackTreeNode callees) {
        CallGraphDirection direction = callGraphDirection.get();
        CallGraphDirection resolvedDirection = direction == null ? CallGraphDirection.CALLEES : direction;
        StackTreeNode sourceTree = resolvedDirection == CallGraphDirection.CALLERS ? callers : callees;
        return new CallGraphLayoutBuilder(resolvedMaxDepth(), CallGraphLayoutBuilder.DEFAULT_MAX_NODES)
                .build(method, sourceTree, resolvedDirection);
    }

    private int resolvedMaxDepth() {
        Integer maxDepth = callGraphMaxDepth.get();
        if (maxDepth == null) {
            return CallGraphLayoutBuilder.DEFAULT_MAX_DEPTH;
        }
        return Math.max(1, maxDepth);
    }

    private int resolvedPackageDepth() {
        Integer packageDepth = dependencyPackageDepth.get();
        if (packageDepth == null) {
            return 1;
        }
        return Math.max(1, packageDepth);
    }

    private StackTreeNode stackTreeOrEmpty(StackTreeNode stackTree) {
        return stackTree == null ? StackTreeNode.EMPTY : stackTree;
    }

    private Integer flameGraphEventCount(StackTreeNode root) {
        if (root == null || root == StackTreeNode.EMPTY || root.count() <= 0) {
            return null;
        }
        return root.count();
    }

    private CallGraphLayout buildDependencyGraph(DependencyGraphReport report) {
        if (report == null || report.edges().isEmpty()) {
            return CallGraphLayout.EMPTY;
        }
        Map<String, CallGraphNode> nodes = new LinkedHashMap<>();
        List<CallGraphEdge> edges = new java.util.ArrayList<>();
        int maxSourceDepth = 0;
        for (DependencyGraphEdge edge : report.edges()) {
            CallGraphNode source = nodes.computeIfAbsent(edge.source(), label -> dependencyNode(nodes.size(), label));
            CallGraphNode target = nodes.computeIfAbsent(edge.target(), label -> dependencyNode(nodes.size(), label));
            edges.add(new CallGraphEdge(source.id(), target.id(), edge.count(), edge.percentage()));
            maxSourceDepth = Math.max(maxSourceDepth, Math.max(source.depth(), target.depth()));
        }
        return new CallGraphLayout(List.copyOf(nodes.values()), edges, maxSourceDepth);
    }

    private static CallGraphNode dependencyNode(int index, String label) {
        double y = (index % 2 == 0) ? 0.25 : 0.75;
        double x = 0.1 + Math.min(0.8, index * 0.12);
        return new CallGraphNode("node-" + (index + 1), label, 0, 0, index, x, y, index == 0);
    }
}
