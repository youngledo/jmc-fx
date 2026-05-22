package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.FileIOEvent;
import com.youngledo.jmcfx.domain.model.FileIOHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;

/// Port for file I/O histogram and event analysis on a flight recording.
public interface FileIOService {

    /// Loads the file I/O histogram aggregated by file path.
    List<FileIOHistogram> loadFileIOHistogram(RecordingSummary recording);

    /// Loads individual file I/O events for the event log tab.
    List<FileIOEvent> loadFileIOEvents(RecordingSummary recording);

    /// Loads a timeline chart of I/O throughput over time.
    ChartDefinition loadTimeline(RecordingSummary recording);
}
