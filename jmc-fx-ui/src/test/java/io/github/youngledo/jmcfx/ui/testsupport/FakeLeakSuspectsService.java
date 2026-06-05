package io.github.youngledo.jmcfx.ui.testsupport;

import java.util.ArrayList;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.LeakCandidate;
import io.github.youngledo.jmcfx.domain.model.LeakReferenceNode;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.LeakSuspectsService;

public class FakeLeakSuspectsService implements LeakSuspectsService {

    private final List<LeakCandidate> candidates = new ArrayList<>();
    private LeakReferenceNode referenceTree = LeakReferenceNode.EMPTY;

    public void addCandidate(LeakCandidate candidate) {
        candidates.add(candidate);
    }

    public void setReferenceTree(LeakReferenceNode tree) {
        this.referenceTree = tree;
    }

    @Override
    public List<LeakCandidate> loadLeakCandidates(RecordingSummary recording) {
        return List.copyOf(candidates);
    }

    @Override
    public LeakReferenceNode loadLeakReferenceTree(RecordingSummary recording, int candidateIndex) {
        return referenceTree;
    }
}
