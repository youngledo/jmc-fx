package io.github.youngledo.jmcfx.application;

import java.nio.file.Path;
import java.util.Objects;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.RecordingRepository;

public final class OpenRecordingUseCase {

    private final RecordingRepository recordingRepository;

    public OpenRecordingUseCase(RecordingRepository recordingRepository) {
        this.recordingRepository = Objects.requireNonNull(recordingRepository, "recordingRepository");
    }

    public RecordingSummary open(Path path) {
        Objects.requireNonNull(path, "path");
        return recordingRepository.open(path);
    }
}
