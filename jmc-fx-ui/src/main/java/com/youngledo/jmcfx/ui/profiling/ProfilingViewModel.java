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
    private RecordingSummary currentRecording;

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

    public void load(RecordingSummary recording) {
        currentRecording = recording;
        List<HotMethod> methods = profilingService.loadHotMethods(recording);
        FxDispatch.run(() -> {
            hotMethods.setAll(methods);
            selectedMethod.set(null);
            clearStackDetails();
        });
    }

    public void selectMethod(String method) {
        if (currentRecording == null) {
            return;
        }
        if (method == null) {
            FxDispatch.run(this::clearStackDetails);
            return;
        }
        StackTreeNode callers = stackTreeOrEmpty(profilingService.loadStackTraceTree(currentRecording, method, true));
        StackTreeNode callees = stackTreeOrEmpty(profilingService.loadStackTraceTree(currentRecording, method, false));
        FlameGraphLayoutBuilder builder = FlameGraphLayoutBuilder.defaultBuilder();
        FlameGraphLayout callerFlameGraph = builder.build(callers);
        FlameGraphLayout calleeFlameGraph = builder.build(callees);
        FxDispatch.run(() -> {
            callersTree.set(callers);
            calleesTree.set(callees);
            callersFlameGraph.set(callerFlameGraph);
            calleesFlameGraph.set(calleeFlameGraph);
        });
    }

    private void clearStackDetails() {
        callersTree.set(StackTreeNode.EMPTY);
        calleesTree.set(StackTreeNode.EMPTY);
        callersFlameGraph.set(FlameGraphLayout.EMPTY);
        calleesFlameGraph.set(FlameGraphLayout.EMPTY);
    }

    private StackTreeNode stackTreeOrEmpty(StackTreeNode stackTree) {
        return stackTree == null ? StackTreeNode.EMPTY : stackTree;
    }
}
