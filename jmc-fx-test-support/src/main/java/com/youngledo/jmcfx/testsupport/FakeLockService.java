package com.youngledo.jmcfx.testsupport;

import java.util.ArrayList;
import java.util.List;

import com.youngledo.jmcfx.domain.model.LockGrouping;
import com.youngledo.jmcfx.domain.model.LockHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.LockService;

public class FakeLockService implements LockService {

    private final List<LockHistogram> histogram = new ArrayList<>();

    public void addHistogramRow(LockHistogram row) {
        histogram.add(row);
    }

    @Override
    public List<LockHistogram> loadLockHistogram(RecordingSummary recording, LockGrouping grouping) {
        return List.copyOf(histogram);
    }
}
