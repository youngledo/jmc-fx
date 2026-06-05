package io.github.youngledo.jmcfx.domain.service;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.LeakCandidate;
import io.github.youngledo.jmcfx.domain.model.LeakReferenceNode;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;

public interface LeakSuspectsService {
    List<LeakCandidate> loadLeakCandidates(RecordingSummary recording);
    LeakReferenceNode loadLeakReferenceTree(RecordingSummary recording, int candidateIndex);
}
