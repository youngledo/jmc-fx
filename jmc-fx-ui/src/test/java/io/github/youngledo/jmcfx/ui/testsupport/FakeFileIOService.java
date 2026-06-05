package io.github.youngledo.jmcfx.ui.testsupport;

import java.util.ArrayList;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.FileIOEvent;
import io.github.youngledo.jmcfx.domain.model.FileIOHistogram;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.FileIOService;

public class FakeFileIOService implements FileIOService {

    private final List<FileIOHistogram> histogram = new ArrayList<>();
    private final List<FileIOEvent> events = new ArrayList<>();

    public void addHistogramRow(FileIOHistogram row) {
        histogram.add(row);
    }

    public void addEvent(FileIOEvent event) {
        events.add(event);
    }

    @Override
    public List<FileIOHistogram> loadFileIOHistogram(RecordingSummary recording) {
        return List.copyOf(histogram);
    }

    @Override
    public List<FileIOEvent> loadFileIOEvents(RecordingSummary recording) {
        return List.copyOf(events);
    }

    @Override
    public ChartDefinition loadTimeline(RecordingSummary recording) {
        return new ChartDefinition("Time", "Duration (ms)", List.of());
    }
}
