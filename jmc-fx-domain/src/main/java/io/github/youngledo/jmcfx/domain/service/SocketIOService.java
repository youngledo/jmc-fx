package io.github.youngledo.jmcfx.domain.service;

import java.util.List;

import io.github.youngledo.jmcfx.domain.model.ChartDefinition;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.SocketIOEvent;
import io.github.youngledo.jmcfx.domain.model.SocketIOGrouping;
import io.github.youngledo.jmcfx.domain.model.SocketIOHistogram;

/// Port for socket I/O histogram and event analysis on a flight recording.
public interface SocketIOService {

    /// Loads the socket I/O histogram grouped by the specified strategy.
    List<SocketIOHistogram> loadSocketIOHistogram(RecordingSummary recording, SocketIOGrouping grouping);

    /// Loads individual socket I/O events for the event log tab.
    List<SocketIOEvent> loadSocketIOEvents(RecordingSummary recording);

    /// Loads a timeline chart of socket I/O throughput over time.
    ChartDefinition loadTimeline(RecordingSummary recording);
}
