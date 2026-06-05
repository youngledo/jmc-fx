package io.github.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.LeakCandidate;
import io.github.youngledo.jmcfx.domain.model.LeakReferenceNode;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.LeakSuspectsService;

public final class LoadLeakSuspectsUseCase {

    private final LeakSuspectsService service;

    public LoadLeakSuspectsUseCase(LeakSuspectsService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public List<LeakCandidate> loadLeakCandidates(RecordingSummary recording) {
        return service.loadLeakCandidates(recording);
    }

    public LeakReferenceNode loadLeakReferenceTree(RecordingSummary recording, int candidateIndex) {
        return service.loadLeakReferenceTree(recording, candidateIndex);
    }
}
