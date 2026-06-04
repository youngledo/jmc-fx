package com.youngledo.jmcfx.ui.testsupport;

import java.util.ArrayList;
import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.SocketIOEvent;
import com.youngledo.jmcfx.domain.model.SocketIOGrouping;
import com.youngledo.jmcfx.domain.model.SocketIOHistogram;
import com.youngledo.jmcfx.domain.service.SocketIOService;

public class FakeSocketIOService implements SocketIOService {

    private final List<SocketIOHistogram> histogram = new ArrayList<>();
    private final List<SocketIOEvent> events = new ArrayList<>();

    public void addHistogramRow(SocketIOHistogram row) {
        histogram.add(row);
    }

    public void addEvent(SocketIOEvent event) {
        events.add(event);
    }

    @Override
    public List<SocketIOHistogram> loadSocketIOHistogram(RecordingSummary recording,
            SocketIOGrouping grouping) {
        return List.copyOf(histogram);
    }

    @Override
    public List<SocketIOEvent> loadSocketIOEvents(RecordingSummary recording) {
        return List.copyOf(events);
    }

    @Override
    public ChartDefinition loadTimeline(RecordingSummary recording) {
        return new ChartDefinition("Time", "Duration (ms)", List.of());
    }
}
