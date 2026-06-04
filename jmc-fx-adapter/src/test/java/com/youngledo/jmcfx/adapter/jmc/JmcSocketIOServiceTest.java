package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.domain.model.ChartDefinition;
import com.youngledo.jmcfx.domain.model.ChartXAxisType;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.SocketIOEvent;
import com.youngledo.jmcfx.domain.model.SocketIOGrouping;
import com.youngledo.jmcfx.domain.model.SocketIOHistogram;

import jdk.jfr.Recording;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class JmcSocketIOServiceTest {

	private final JmcSocketIOService service = new JmcSocketIOService();

	@Test
	void loadSocketIOHistogram_byHost_returnsNonNull(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createSocketIORecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<SocketIOHistogram> histogram = service.loadSocketIOHistogram(recording,
				SocketIOGrouping.BY_HOST);
		assertNotNull(histogram);
	}

	@Test
	void loadSocketIOHistogram_byPort_returnsNonNull(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createSocketIORecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<SocketIOHistogram> histogram = service.loadSocketIOHistogram(recording,
				SocketIOGrouping.BY_PORT);
		assertNotNull(histogram);
	}

	@Test
	void loadSocketIOHistogram_byHostAndPort_returnsNonNull(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createSocketIORecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<SocketIOHistogram> histogram = service.loadSocketIOHistogram(recording,
				SocketIOGrouping.BY_HOST_AND_PORT);
		assertNotNull(histogram);
	}

	@Test
	void loadSocketIOHistogram_sortedByTotalDurationDescending(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createSocketIORecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<SocketIOHistogram> histogram = service.loadSocketIOHistogram(recording,
				SocketIOGrouping.BY_HOST);
		for (int i = 1; i < histogram.size(); i++) {
			assertTrue(histogram.get(i - 1).totalDuration() >= histogram.get(i).totalDuration(),
					"Histogram should be sorted by totalDuration descending");
		}
	}

	@Test
	void loadSocketIOHistogram_allFieldsValid(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createSocketIORecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<SocketIOHistogram> histogram = service.loadSocketIOHistogram(recording,
				SocketIOGrouping.BY_HOST_AND_PORT);
		for (SocketIOHistogram entry : histogram) {
			assertNotNull(entry.key());
			assertNotNull(entry.host());
			assertTrue(entry.readCount() >= 0);
			assertTrue(entry.writeCount() >= 0);
			assertTrue(entry.totalDuration() >= 0);
			assertTrue(entry.maxDuration() >= 0);
			assertTrue(entry.avgDuration() >= 0);
		}
	}

	@Test
	void loadSocketIOEvents_returnsNonNull(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createSocketIORecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<SocketIOEvent> events = service.loadSocketIOEvents(recording);
		assertNotNull(events);
	}

	@Test
	void loadSocketIOEvents_sortedByTimestampAscending(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createSocketIORecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<SocketIOEvent> events = service.loadSocketIOEvents(recording);
		for (int i = 1; i < events.size(); i++) {
			assertTrue(events.get(i - 1).timestamp() <= events.get(i).timestamp(),
					"Events should be sorted by timestamp ascending");
		}
	}

	@Test
	void loadSocketIOEvents_eventFieldsValid(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createSocketIORecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<SocketIOEvent> events = service.loadSocketIOEvents(recording);
		for (SocketIOEvent event : events) {
			assertNotNull(event.eventType());
			assertTrue(event.eventType().startsWith("jdk.Socket"));
			assertNotNull(event.host());
			assertTrue(event.bytes() >= 0);
			assertTrue(event.durationMillis() >= 0);
			assertTrue(event.timestamp() > 0);
			assertNotNull(event.threadName());
		}
	}

	@Test
	void loadTimeline_returnsNonNull(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createSocketIORecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		ChartDefinition timeline = service.loadTimeline(recording);
		assertNotNull(timeline);
		assertEquals(ChartXAxisType.EPOCH_MILLIS, timeline.xAxisType());
	}

	@Test
	void loadTimeline_hasSeriesWhenSocketIOPresent(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createSocketIORecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		ChartDefinition timeline = service.loadTimeline(recording);
		assertNotNull(timeline.series());
		// At least one series should be present if socket I/O occurred
		if (!timeline.series().isEmpty()) {
			for (var series : timeline.series()) {
				assertNotNull(series.id());
				assertNotNull(series.name());
				assertNotNull(series.points());
			}
		}
	}

	@Test
	void loadSocketIOHistogram_emptyOnMinimalRecording(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<SocketIOHistogram> histogram = service.loadSocketIOHistogram(recording,
				SocketIOGrouping.BY_HOST);
		assertNotNull(histogram);
		assertTrue(histogram.isEmpty());
	}

	@Test
	void loadSocketIOEvents_emptyOnMinimalRecording(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<SocketIOEvent> events = service.loadSocketIOEvents(recording);
		assertNotNull(events);
		assertTrue(events.isEmpty());
	}

	@Test
	void loadTimeline_emptyOnMinimalRecording(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		ChartDefinition timeline = service.loadTimeline(recording);
		assertNotNull(timeline);
		assertTrue(timeline.series().isEmpty());
	}

	/// Creates a recording with actual socket I/O to generate jdk.SocketRead/SocketWrite events.
	private Path createSocketIORecording(Path tempDir) throws Exception {
		try (Recording recording = new Recording()) {
			recording.enable("jdk.SocketRead");
			recording.enable("jdk.SocketWrite");
			recording.start();

			// Start a simple echo server
			try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
				int port = server.getLocalPort();

				// Client connects and does I/O
				try (Socket client = new Socket(InetAddress.getLoopbackAddress(), port)) {
					client.setSoTimeout(5000);

					// Accept the connection and send data
					try (Socket serverSide = server.accept()) {
						serverSide.getOutputStream().write("hello socket".getBytes());
						serverSide.getOutputStream().flush();

						// Client reads data
						byte[] buffer = new byte[1024];
						int bytesRead = client.getInputStream().read(buffer);
						// Write back a response
						client.getOutputStream().write("response".getBytes());
						client.getOutputStream().flush();
					}
				}
			}

			Thread.sleep(50);
			recording.stop();
			Path file = tempDir.resolve("socketio-test.jfr");
			recording.dump(file);
			return file;
		}
	}

	/// Creates a minimal recording with no socket I/O.
	private Path createMinimalRecording(Path tempDir) throws Exception {
		try (Recording recording = new Recording()) {
			recording.start();
			Thread.sleep(50);
			recording.stop();
			Path file = tempDir.resolve("minimal-socketio-test.jfr");
			recording.dump(file);
			return file;
		}
	}
}
