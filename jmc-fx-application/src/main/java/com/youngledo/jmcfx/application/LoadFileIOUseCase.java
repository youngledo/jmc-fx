package com.youngledo.jmcfx.application;

import java.util.List;
import java.util.Objects;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.FileIOEvent;
import com.youngledo.jmcfx.domain.model.FileIOHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.FileIOService;

public final class LoadFileIOUseCase {

    private final FileIOService service;

    public LoadFileIOUseCase(FileIOService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public List<FileIOHistogram> loadFileIOHistogram(RecordingSummary recording) {
        return service.loadFileIOHistogram(recording);
    }

    public List<FileIOEvent> loadFileIOEvents(RecordingSummary recording) {
        return service.loadFileIOEvents(recording);
    }

    public ChartDefinition loadTimeline(RecordingSummary recording) {
        return service.loadTimeline(recording);
    }
}
