package com.youngledo.jmcfx.ui.leaks;

import java.util.List;

import com.youngledo.jmcfx.domain.model.LeakCandidate;
import com.youngledo.jmcfx.domain.model.LeakReferenceNode;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.LeakSuspectsService;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class LeakSuspectsViewModel {

    private final LeakSuspectsService leakSuspectsService;
    private final ObservableList<LeakCandidate> candidates = FXCollections.observableArrayList();
    private final ObjectProperty<LeakCandidate> selectedCandidate = new SimpleObjectProperty<>();
    private final ObjectProperty<LeakReferenceNode> referenceTree =
            new SimpleObjectProperty<>(LeakReferenceNode.EMPTY);
    private RecordingSummary currentRecording;

    public LeakSuspectsViewModel(LeakSuspectsService leakSuspectsService) {
        this.leakSuspectsService = leakSuspectsService;
    }

    public ObservableList<LeakCandidate> candidatesProperty() {
        return candidates;
    }

    public ObjectProperty<LeakCandidate> selectedCandidateProperty() {
        return selectedCandidate;
    }

    public ObjectProperty<LeakReferenceNode> referenceTreeProperty() {
        return referenceTree;
    }

    public void load(RecordingSummary recording) {
        currentRecording = recording;
        List<LeakCandidate> data = leakSuspectsService.loadLeakCandidates(recording);
        candidates.setAll(data);
        selectedCandidate.set(null);
        referenceTree.set(LeakReferenceNode.EMPTY);
    }

    public void selectCandidate(int index) {
        if (currentRecording == null) {
            return;
        }
        LeakReferenceNode tree = leakSuspectsService.loadLeakReferenceTree(currentRecording, index);
        referenceTree.set(tree);
    }
}
