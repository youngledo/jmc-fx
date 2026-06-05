package io.github.youngledo.jmcfx.ui.testsupport;

import java.util.ArrayList;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.LockGrouping;
import io.github.youngledo.jmcfx.domain.model.LockHistogram;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.LockService;

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
