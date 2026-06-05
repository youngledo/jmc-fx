package io.github.youngledo.jmcfx.ui.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;
import io.github.youngledo.jmcfx.domain.model.Severity;

class FakeRuleAnalysisServiceTest {

    @Test
    void returnsConfiguredRuleResults() {
        RecordingSummary recording = new RecordingSummary("r1", Path.of("sample.jfr"), "sample.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);
        FakeRuleAnalysisService service = new FakeRuleAnalysisService();
        service.addResult(new RuleResult("rule", "Rule", Severity.INFO, 25, "General", "Summary", "Explanation"));

        assertEquals(1, service.analyze(recording).size());
    }
}
