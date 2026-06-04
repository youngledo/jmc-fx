package com.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import com.youngledo.jmcfx.domain.model.LeakCandidate;
import com.youngledo.jmcfx.domain.model.LeakReferenceNode;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.LeakSuspectsService;

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
