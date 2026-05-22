package com.youngledo.jmcfx.adapter.jmc;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.RuleResult;
import com.youngledo.jmcfx.domain.model.Severity;

import jdk.jfr.Recording;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
			assertTrue(result.score() >= 0);
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
		assertEquals(Severity.UNKNOWN, JmcRuleAnalysisService.mapSeverity(
				org.openjdk.jmc.flightrecorder.rules.Severity.NA));
		assertEquals(Severity.UNKNOWN, JmcRuleAnalysisService.mapSeverity(
				org.openjdk.jmc.flightrecorder.rules.Severity.IGNORE));
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
