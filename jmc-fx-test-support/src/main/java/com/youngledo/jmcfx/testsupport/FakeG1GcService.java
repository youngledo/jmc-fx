package com.youngledo.jmcfx.testsupport;

import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.domain.model.G1GcRegionState;
import com.youngledo.jmcfx.domain.model.G1GcRegionSummary;
import com.youngledo.jmcfx.domain.model.G1GcReport;
import com.youngledo.jmcfx.domain.model.GcEvent;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.G1GcService;

public class FakeG1GcService implements G1GcService {

    @Override
    public G1GcReport loadG1GcReport(RecordingSummary recording) {
        List<G1GcRegionSummary> summaries = List.of(
                new G1GcRegionSummary("Eden", 2, 32_000_000, 64_000_000),
                new G1GcRegionSummary("Humongous", 1, 24_000_000, 32_000_000));
        List<G1GcRegionState> states = List.of(
                new G1GcRegionState(0, "Eden", "", "Snapshot", 16_000_000, 32_000_000, 0,
                        Instant.parse("2026-01-01T00:00:01Z")),
                new G1GcRegionState(1, "Humongous", "Old", "Transition", 24_000_000, 32_000_000, 1,
                        Instant.parse("2026-01-01T00:00:02Z")),
                new G1GcRegionState(2, "Eden", "", "Snapshot", 16_000_000, 32_000_000, 0,
                        Instant.parse("2026-01-01T00:00:03Z")));
        List<GcEvent> pauses = List.of(
                new GcEvent(1, "G1New", "G1 Evacuation Pause", 5_000, 8_000,
                        Instant.parse("2026-01-01T00:00:02Z")),
                new GcEvent(2, "G1Old", "G1 Full GC", 50_000, 90_000,
                        Instant.parse("2026-01-01T00:00:04Z")));
        return new G1GcReport(2, 1, pauses.size(), 3, 56_000_000, 96_000_000,
                Instant.parse("2026-01-01T00:00:03Z"), summaries, states, pauses);
    }
}
