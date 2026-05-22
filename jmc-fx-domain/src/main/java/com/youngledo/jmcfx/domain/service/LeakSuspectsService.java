package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.LeakCandidate;
import com.youngledo.jmcfx.domain.model.LeakReferenceNode;
import com.youngledo.jmcfx.domain.model.RecordingSummary;

public interface LeakSuspectsService {
    List<LeakCandidate> loadLeakCandidates(RecordingSummary recording);
    LeakReferenceNode loadLeakReferenceTree(RecordingSummary recording, int candidateIndex);
}
