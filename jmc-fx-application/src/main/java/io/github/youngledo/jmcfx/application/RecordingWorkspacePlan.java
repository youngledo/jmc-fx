package io.github.youngledo.jmcfx.application;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;

public record RecordingWorkspacePlan(
        RecordingSummary recording,
        boolean hasProfiling,
        boolean hasExceptions,
        boolean hasThreads,
        boolean hasFileIO,
        boolean hasSocketIO,
        boolean hasLocks,
        boolean hasHeap,
        boolean hasLeakSuspects,
        boolean hasTlab,
        boolean hasJvmInternals,
        boolean hasEnvironment,
        boolean hasJavaApplication,
        boolean hasMetadata,
        boolean hasG1Gc,
        boolean hasJavaFxEvents,
        boolean hasAdvancedJfrAnalysis) {
}
