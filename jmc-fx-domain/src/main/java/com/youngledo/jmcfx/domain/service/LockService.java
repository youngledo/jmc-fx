package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.LockGrouping;
import com.youngledo.jmcfx.domain.model.LockHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;

/// Port for lock instance histogram analysis on a flight recording.
public interface LockService {

    /// Loads the lock histogram grouped by the specified strategy.
    List<LockHistogram> loadLockHistogram(RecordingSummary recording, LockGrouping grouping);
}
