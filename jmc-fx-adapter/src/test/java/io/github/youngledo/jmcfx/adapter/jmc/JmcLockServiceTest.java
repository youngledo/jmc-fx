package io.github.youngledo.jmcfx.adapter.jmc;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.LockGrouping;
import io.github.youngledo.jmcfx.domain.model.LockHistogram;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;

import jdk.jfr.Recording;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class JmcLockServiceTest {

	private final JmcLockService service = new JmcLockService();

	@Test
	void loadLockHistogram_byClass_returnsResults(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<LockHistogram> histogram =
				service.loadLockHistogram(recording, LockGrouping.BY_CLASS);
		assertNotNull(histogram);
	}

	@Test
	void loadLockHistogram_byAddress_returnsResults(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<LockHistogram> histogram =
				service.loadLockHistogram(recording, LockGrouping.BY_ADDRESS);
		assertNotNull(histogram);
	}

	@Test
	void loadLockHistogram_byThread_returnsResults(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<LockHistogram> histogram =
				service.loadLockHistogram(recording, LockGrouping.BY_THREAD);
		assertNotNull(histogram);
	}

	private Path createMinimalRecording(Path tempDir) throws Exception {
		try (Recording recording = new Recording()) {
			recording.enable("jdk.JavaMonitorEnter");
			recording.start();
			Thread.sleep(50);
			recording.stop();
			Path file = tempDir.resolve("lock-test.jfr");
			recording.dump(file);
			return file;
		}
	}
}
