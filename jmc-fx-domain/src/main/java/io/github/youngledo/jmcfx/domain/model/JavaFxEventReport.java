package io.github.youngledo.jmcfx.domain.model;

import java.util.List;

public record JavaFxEventReport(
        long pulseCount,
        long phaseCount,
        long inputCount,
        long slowPhaseCount,
        long maxPhaseDurationMicros,
        List<JavaFxPulseSummary> pulseSummaries,
        List<JavaFxPulsePhase> pulsePhases,
        List<JavaFxInputEvent> inputEvents) {
}
