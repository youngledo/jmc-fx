package com.youngledo.jmcfx.ui.testsupport;

import java.util.ArrayList;
import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.ExceptionGrouping;
import com.youngledo.jmcfx.domain.model.ExceptionSummary;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.ExceptionService;

public class FakeExceptionService implements ExceptionService {

    private final List<ExceptionSummary> histogram = new ArrayList<>();

    public void addException(ExceptionSummary summary) {
        histogram.add(summary);
    }

    @Override
    public List<ExceptionSummary> loadHistogram(RecordingSummary recording, ExceptionGrouping grouping) {
        return List.copyOf(histogram);
    }

    @Override
    public ChartDefinition loadTimeline(RecordingSummary recording) {
        return new ChartDefinition("Time", "Count", List.of());
    }
}
