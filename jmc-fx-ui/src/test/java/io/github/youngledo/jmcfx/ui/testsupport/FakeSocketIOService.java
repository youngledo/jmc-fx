package io.github.youngledo.jmcfx.ui.testsupport;

import java.util.ArrayList;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.ChartDataPoint;
import io.github.youngledo.jmcfx.domain.model.ChartSeries;
import io.github.youngledo.jmcfx.domain.model.ChartSeriesType;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.SocketIOEvent;
import io.github.youngledo.jmcfx.domain.model.SocketIOGrouping;
import io.github.youngledo.jmcfx.domain.model.SocketIOHistogram;
import io.github.youngledo.jmcfx.domain.service.SocketIOService;

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
        return new ChartDefinition("Time", "Duration (ms)", List.of(new ChartSeries("io", "I/O",
                ChartSeriesType.LINE, List.of(
                        new ChartDataPoint(recording.startTime().toEpochMilli(), 1),
                        new ChartDataPoint(recording.endTime().toEpochMilli(), 2)))));
    }
}
