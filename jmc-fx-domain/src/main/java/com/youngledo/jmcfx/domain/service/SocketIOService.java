package com.youngledo.jmcfx.domain.service;

import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.SocketIOEvent;
import com.youngledo.jmcfx.domain.model.SocketIOGrouping;
import com.youngledo.jmcfx.domain.model.SocketIOHistogram;

/// Port for socket I/O histogram and event analysis on a flight recording.
public interface SocketIOService {

    /// Loads the socket I/O histogram grouped by the specified strategy.
    List<SocketIOHistogram> loadSocketIOHistogram(RecordingSummary recording, SocketIOGrouping grouping);

    /// Loads individual socket I/O events for the event log tab.
    List<SocketIOEvent> loadSocketIOEvents(RecordingSummary recording);

    /// Loads a timeline chart of socket I/O throughput over time.
    ChartDefinition loadTimeline(RecordingSummary recording);
}
