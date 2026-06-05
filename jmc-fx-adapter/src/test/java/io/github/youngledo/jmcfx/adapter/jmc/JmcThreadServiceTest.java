package io.github.youngledo.jmcfx.adapter.jmc;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.ThreadSummary;

import jdk.jfr.Recording;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class JmcThreadServiceTest {

	private final JmcThreadService service = new JmcThreadService();

	@Test
	void loadThreadSummaries_returnsResults(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<ThreadSummary> summaries = service.loadThreadSummaries(recording);
		assertNotNull(summaries);
	}

	@Test
	void loadThreadSummaries_threadNamesNotNull(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<ThreadSummary> summaries = service.loadThreadSummaries(recording);
		for (ThreadSummary summary : summaries) {
			assertNotNull(summary.threadName());
		}
	}

	private Path createMinimalRecording(Path tempDir) throws Exception {
		try (Recording recording = new Recording()) {
			recording.start();
			Thread.sleep(50);
			recording.stop();
			Path file = tempDir.resolve("thread-test.jfr");
			recording.dump(file);
			return file;
		}
	}
}
