package com.youngledo.jmcfx.testsupport;

import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.domain.model.JavaFxEventReport;
import com.youngledo.jmcfx.domain.model.JavaFxInputEvent;
import com.youngledo.jmcfx.domain.model.JavaFxPulsePhase;
import com.youngledo.jmcfx.domain.model.JavaFxPulseSummary;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JavaFxEventService;

public class FakeJavaFxEventService implements JavaFxEventService {

    @Override
    public JavaFxEventReport loadJavaFxEvents(RecordingSummary recording) {
        List<JavaFxPulseSummary> summaries = List.of(
                new JavaFxPulseSummary(42, 2, 18_000, 12_000, Instant.parse("2026-01-01T00:00:01Z")));
        List<JavaFxPulsePhase> phases = List.of(
                new JavaFxPulsePhase(42, "Scene synchronization", 6_000,
                        Instant.parse("2026-01-01T00:00:01Z"), "JavaFX Application Thread"),
                new JavaFxPulsePhase(42, "Rendering", 12_000,
                        Instant.parse("2026-01-01T00:00:02Z"), "QuantumRenderer-0"));
        List<JavaFxInputEvent> inputs = List.of(
                new JavaFxInputEvent("MouseEvent", 500,
                        Instant.parse("2026-01-01T00:00:00Z"), "JavaFX Application Thread"),
                new JavaFxInputEvent("KeyEvent", 250,
                        Instant.parse("2026-01-01T00:00:03Z"), "JavaFX Application Thread"));
        return new JavaFxEventReport(summaries.size(), phases.size(), inputs.size(), 1,
                12_000, summaries, phases, inputs);
    }
}
