package com.youngledo.jmcfx.ui.profiling;

import java.util.List;

import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.StackTreeNode;
import com.youngledo.jmcfx.domain.service.ProfilingService;
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
    private final ObjectProperty<HotMethod> selectedMethod = new SimpleObjectProperty<>();
    private final ObjectProperty<StackTreeNode> callersTree = new SimpleObjectProperty<>(StackTreeNode.EMPTY);
    private final ObjectProperty<StackTreeNode> calleesTree = new SimpleObjectProperty<>(StackTreeNode.EMPTY);
    private final ObjectProperty<FlameGraphLayout> callersFlameGraph =
            new SimpleObjectProperty<>(FlameGraphLayout.EMPTY);
    private final ObjectProperty<FlameGraphLayout> calleesFlameGraph =
            new SimpleObjectProperty<>(FlameGraphLayout.EMPTY);
    private final ObjectProperty<CallGraphDirection> callGraphDirection =
            new SimpleObjectProperty<>(CallGraphDirection.CALLEES);
    private final ObjectProperty<CallGraphLayout> callGraph =
            new SimpleObjectProperty<>(CallGraphLayout.EMPTY);
    private final ObjectProperty<Integer> callGraphMaxDepth =
            new SimpleObjectProperty<>(CallGraphLayoutBuilder.DEFAULT_MAX_DEPTH);
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

    public ObjectProperty<StackTreeNode> callersTreeProperty() {
        return callersTree;
    }

    public ObjectProperty<StackTreeNode> calleesTreeProperty() {
        return calleesTree;
    }

    public ObjectProperty<FlameGraphLayout> callersFlameGraphProperty() {
        return callersFlameGraph;
    }

    public ObjectProperty<FlameGraphLayout> calleesFlameGraphProperty() {
        return calleesFlameGraph;
    }

    public ObjectProperty<CallGraphDirection> callGraphDirectionProperty() {
        return callGraphDirection;
    }

    public ObjectProperty<CallGraphLayout> callGraphProperty() {
        return callGraph;
    }

    public ObjectProperty<Integer> callGraphMaxDepthProperty() {
        return callGraphMaxDepth;
    }

    public void load(RecordingSummary recording) {
        currentRecording = recording;
        List<HotMethod> methods = profilingService.loadHotMethods(recording);
        FxDispatch.run(() -> {
            hotMethods.setAll(methods);
            selectedMethod.set(null);
            selectedMethodName = null;
            clearStackDetails();
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
        FlameGraphLayoutBuilder builder = FlameGraphLayoutBuilder.defaultBuilder();
        FlameGraphLayout callerFlameGraph = builder.build(callers);
        FlameGraphLayout calleeFlameGraph = builder.build(callees);
        FxDispatch.run(() -> {
            selectedMethodName = method;
            callersTree.set(callers);
            calleesTree.set(callees);
            callersFlameGraph.set(callerFlameGraph);
            calleesFlameGraph.set(calleeFlameGraph);
            callGraph.set(buildCallGraph(method, callers, callees));
        });
    }

    private void clearStackDetails() {
        callersTree.set(StackTreeNode.EMPTY);
        calleesTree.set(StackTreeNode.EMPTY);
        callersFlameGraph.set(FlameGraphLayout.EMPTY);
        calleesFlameGraph.set(FlameGraphLayout.EMPTY);
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

    private StackTreeNode stackTreeOrEmpty(StackTreeNode stackTree) {
        return stackTree == null ? StackTreeNode.EMPTY : stackTree;
    }
}
