package io.github.youngledo.jmcfx.domain.service;

import java.nio.file.Path;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;

public interface RecordingRepository {
    RecordingSummary open(Path path);
}
