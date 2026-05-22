package com.youngledo.jmcfx.adapter.jmc;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.StackTreeNode;

import jdk.jfr.Recording;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class JmcProfilingServiceTest {

	private final JmcProfilingService service = new JmcProfilingService();

	@Test
	void loadHotMethods_returnsResults(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<HotMethod> methods = service.loadHotMethods(recording);
		assertNotNull(methods);
	}

	@Test
	void loadStackTraceTree_returnsNode(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		StackTreeNode tree = service.loadStackTraceTree(recording, "java.lang.Thread.run", true);
		assertNotNull(tree);
	}

	private Path createMinimalRecording(Path tempDir) throws Exception {
		try (Recording recording = new Recording()) {
			recording.start();
			Thread.sleep(50);
			recording.stop();
			Path file = tempDir.resolve("profiling-test.jfr");
			recording.dump(file);
			return file;
		}
	}
}
