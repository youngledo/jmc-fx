package com.youngledo.jmcfx.adapter.jmc;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.FileIOEvent;
import com.youngledo.jmcfx.domain.model.FileIOHistogram;
import com.youngledo.jmcfx.domain.model.RecordingSummary;

import jdk.jfr.Recording;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class JmcFileIOServiceTest {

	private final JmcFileIOService service = new JmcFileIOService();

	@Test
	void loadFileIOHistogram_returnsNonNull(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<FileIOHistogram> histogram = service.loadFileIOHistogram(recording);
		assertNotNull(histogram);
	}

	@Test
	void loadFileIOEvents_returnsNonNull(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<FileIOEvent> events = service.loadFileIOEvents(recording);
		assertNotNull(events);
	}

	@Test
	void loadTimeline_returnsNonNull(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		ChartDefinition timeline = service.loadTimeline(recording);
		assertNotNull(timeline);
	}

	@Test
	void loadFileIOHistogram_sortedByTotalDurationDescending(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<FileIOHistogram> histogram = service.loadFileIOHistogram(recording);
		for (int i = 1; i < histogram.size(); i++) {
			assertTrue(histogram.get(i - 1).totalDuration() >= histogram.get(i).totalDuration(),
					"Histogram should be sorted by totalDuration descending");
		}
	}

	@Test
	void loadFileIOEvents_sortedByTimestampAscending(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<FileIOEvent> events = service.loadFileIOEvents(recording);
		for (int i = 1; i < events.size(); i++) {
			assertTrue(events.get(i - 1).timestamp() <= events.get(i).timestamp(),
					"Events should be sorted by timestamp ascending");
		}
	}

	private Path createMinimalRecording(Path tempDir) throws Exception {
		try (Recording recording = new Recording()) {
			recording.enable("jdk.FileRead");
			recording.enable("jdk.FileWrite");
			recording.start();
			// Perform minimal file I/O to generate events
			Path tempFile = tempDir.resolve("io-test-data.txt");
			java.nio.file.Files.writeString(tempFile, "test data");
			java.nio.file.Files.readString(tempFile);
			Thread.sleep(50);
			recording.stop();
			Path file = tempDir.resolve("fileio-test.jfr");
			recording.dump(file);
			return file;
		}
	}
}
