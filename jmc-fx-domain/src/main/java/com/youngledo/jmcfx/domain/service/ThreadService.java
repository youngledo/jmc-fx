package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.ThreadSummary;

public interface ThreadService {
    List<ThreadSummary> loadThreadSummaries(RecordingSummary recording);
}
