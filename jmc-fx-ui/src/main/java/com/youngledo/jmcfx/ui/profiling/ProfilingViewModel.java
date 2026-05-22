package com.youngledo.jmcfx.ui.profiling;

import java.util.List;

import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.StackTreeNode;
import com.youngledo.jmcfx.domain.service.ProfilingService;

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

    public void load(RecordingSummary recording) {
        currentRecording = recording;
        List<HotMethod> methods = profilingService.loadHotMethods(recording);
        hotMethods.setAll(methods);
        selectedMethod.set(null);
        callersTree.set(StackTreeNode.EMPTY);
        calleesTree.set(StackTreeNode.EMPTY);
    }

    public void selectMethod(String method) {
        if (currentRecording == null || method == null) {
            return;
        }
        callersTree.set(profilingService.loadStackTraceTree(currentRecording, method, true));
        calleesTree.set(profilingService.loadStackTraceTree(currentRecording, method, false));
    }
}
