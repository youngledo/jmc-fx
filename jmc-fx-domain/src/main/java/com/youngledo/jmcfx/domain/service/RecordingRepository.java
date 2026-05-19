package com.youngledo.jmcfx.domain.service;

import java.nio.file.Path;

import com.youngledo.jmcfx.domain.model.RecordingSummary;

public interface RecordingRepository {
    RecordingSummary open(Path path);
}
