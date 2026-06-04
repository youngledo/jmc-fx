package com.youngledo.jmcfx.ui.testsupport;

import java.nio.file.Path;
import java.time.Instant;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.RecordingRepository;

public class FakeRecordingRepository implements RecordingRepository {

    @Override
    public RecordingSummary open(Path path) {
        return new RecordingSummary("fake-recording", path, path.getFileName().toString(),
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);
    }
}
