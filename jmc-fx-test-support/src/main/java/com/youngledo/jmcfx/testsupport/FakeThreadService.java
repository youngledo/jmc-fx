package com.youngledo.jmcfx.testsupport;

import java.util.ArrayList;
import java.util.List;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.ThreadSummary;
import com.youngledo.jmcfx.domain.service.ThreadService;

public class FakeThreadService implements ThreadService {

    private final List<ThreadSummary> summaries = new ArrayList<>();

    public void addThread(ThreadSummary summary) {
        summaries.add(summary);
    }

    @Override
    public List<ThreadSummary> loadThreadSummaries(RecordingSummary recording) {
        return List.copyOf(summaries);
    }
}
