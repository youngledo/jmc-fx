package io.github.youngledo.jmcfx.adapter.jmc;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;
import io.github.youngledo.jmcfx.domain.model.Severity;

import jdk.jfr.Recording;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;

import static org.junit.jupiter.api.Assertions.*;

class JmcRuleAnalysisServiceTest {

	private final JmcRuleAnalysisService service = new JmcRuleAnalysisService();

	@Test
	void analyzeReturnsResults(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<RuleResult> results = service.analyze(recording);
		assertNotNull(results);
	}

	@Test
	void resultFieldsArePopulated(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<RuleResult> results = service.analyze(recording);
		for (RuleResult result : results) {
			assertNotNull(result.id());
			assertNotNull(result.name());
			assertNotNull(result.severity());
			assertTrue(result.score() >= -3);
		}
	}

	@Test
	void analyzedResultsCarryRelatedPageForMappedTopics(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);

		List<RuleResult> results = service.analyze(recording);

		assertFalse(results.isEmpty(), "Rule analysis should produce JMC rule results for a valid recording");
		for (RuleResult result : results) {
			String expected = JmcRuleAnalysisService.relatedPageIdFor(result.topic());
			if (!expected.isBlank()) {
				assertEquals(expected, result.relatedPageId(),
						() -> result.id() + " should link topic " + result.topic() + " to " + expected);
			}
		}
	}

	@Test
	void summariesContainNoHtmlTags(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<RuleResult> results = service.analyze(recording);
		for (RuleResult result : results) {
			assertFalse(result.summary().contains("<"), "HTML in summary: " + result.summary());
		}
	}

	@Test
	void resultsHaveNoUnresolvedPositionalPlaceholders(@TempDir Path tempDir) throws Exception {
		Path jfrFile = createMinimalRecording(tempDir);
		RecordingSummary recording = new RecordingSummary("test", jfrFile, "test",
				Instant.now(), Instant.now(), 1000, 1024);
		List<RuleResult> results = service.analyze(recording);
		for (RuleResult result : results) {
			assertFalse(result.summary().matches(".*\\{\\d+}.*"),
					"Unresolved placeholder in summary: " + result.summary());
			assertFalse(result.explanation().matches(".*\\{\\d+}.*"),
					"Unresolved placeholder in explanation: " + result.explanation());
		}
	}

	@Test
	void severityMapping() {
		assertEquals(Severity.OK, JmcRuleAnalysisService.mapSeverity(
				org.openjdk.jmc.flightrecorder.rules.Severity.OK));
		assertEquals(Severity.INFO, JmcRuleAnalysisService.mapSeverity(
				org.openjdk.jmc.flightrecorder.rules.Severity.INFO));
		assertEquals(Severity.WARNING, JmcRuleAnalysisService.mapSeverity(
				org.openjdk.jmc.flightrecorder.rules.Severity.WARNING));
		assertEquals(Severity.UNAVAILABLE, JmcRuleAnalysisService.mapSeverity(
				org.openjdk.jmc.flightrecorder.rules.Severity.NA));
		assertEquals(Severity.IGNORED, JmcRuleAnalysisService.mapSeverity(
				org.openjdk.jmc.flightrecorder.rules.Severity.IGNORE));
	}

	@Test
	void mapsJmcRuleTopicsToRecordingPages() {
		assertEquals("profiling", JmcRuleAnalysisService.relatedPageIdFor(JfrRuleTopics.METHOD_PROFILING));
		assertEquals("fileio", JmcRuleAnalysisService.relatedPageIdFor(JfrRuleTopics.FILE_IO));
		assertEquals("socketio", JmcRuleAnalysisService.relatedPageIdFor(JfrRuleTopics.SOCKET_IO));
		assertEquals("locks", JmcRuleAnalysisService.relatedPageIdFor(JfrRuleTopics.LOCK_INSTANCES));
		assertEquals("exceptions", JmcRuleAnalysisService.relatedPageIdFor(JfrRuleTopics.EXCEPTIONS));
		assertEquals("tlab", JmcRuleAnalysisService.relatedPageIdFor(JfrRuleTopics.TLAB));
		assertEquals("gcDetails", JmcRuleAnalysisService.relatedPageIdFor(JfrRuleTopics.GARBAGE_COLLECTION));
		assertEquals("jvmInfo", JmcRuleAnalysisService.relatedPageIdFor(JfrRuleTopics.JVM_INFORMATION));
		assertEquals("recordingInfo", JmcRuleAnalysisService.relatedPageIdFor(JfrRuleTopics.RECORDING));
		assertEquals("recordingInfo", JmcRuleAnalysisService.relatedPageIdFor("DMS"));
		assertEquals("javaFxEvents", JmcRuleAnalysisService.relatedPageIdFor("javaFx"));
		assertEquals("", JmcRuleAnalysisService.relatedPageIdFor("unknown-topic"));
		assertEquals("", JmcRuleAnalysisService.relatedPageIdFor(null));
	}

	@Test
	void allRegisteredJmcRuleTopicsMapToRecordingPages() {
		List<String> unmappedTopics = RuleRegistry.getRules().stream()
				.map(IRule::getTopic)
				.filter(topic -> topic != null && !topic.isBlank())
				.distinct()
				.filter(topic -> JmcRuleAnalysisService.relatedPageIdFor(topic).isBlank())
				.sorted(Comparator.naturalOrder())
				.toList();

		assertEquals(List.of(), unmappedTopics);
	}

	private Path createMinimalRecording(Path tempDir) throws Exception {
		try (Recording recording = new Recording()) {
			recording.start();
			Thread.sleep(50);
			recording.stop();
			Path file = tempDir.resolve("rules-test.jfr");
			recording.dump(file);
			return file;
		}
	}
}
