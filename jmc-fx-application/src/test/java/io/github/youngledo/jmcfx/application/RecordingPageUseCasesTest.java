package io.github.youngledo.jmcfx.application;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionRequest;
import io.github.youngledo.jmcfx.domain.model.ai.AiCompletionResponse;
import io.github.youngledo.jmcfx.domain.service.EventQueryService;
import io.github.youngledo.jmcfx.domain.service.EventQuerySession;
import io.github.youngledo.jmcfx.domain.service.RecordingRepository;
import io.github.youngledo.jmcfx.domain.service.RuleAnalysisService;
import io.github.youngledo.jmcfx.domain.service.ai.AiCompletionService;
import org.junit.jupiter.api.Test;

class RecordingPageUseCasesTest {
    @Test
    void exposesPageUseCasesWithoutLeakingServiceBundleToUi() {
        RecordingApplicationServices services = new RecordingApplicationServices(
                new FakeRecordingRepository(),
                new FakeEventQueryService(),
                new FakeRuleAnalysisService(),
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);

        RecordingPageUseCases useCases = RecordingPageUseCases.from(services);

        assertNotNull(useCases.openRecordingWorkspace());
        assertNotNull(useCases.browseEvents());
        assertNotNull(useCases.analyzeRules());
        assertNotNull(useCases.buildRecordingAiContext());
        assertSame(useCases.openRecordingWorkspace(), useCases.openRecordingWorkspace());
    }

    @Test
    void exposesAiUseCasesWhenCompletionServiceIsConfigured() {
        RecordingApplicationServices services = new RecordingApplicationServices(
                new FakeRecordingRepository(),
                new FakeEventQueryService(),
                new FakeRuleAnalysisService(),
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                new FakeAiCompletionService());

        RecordingPageUseCases useCases = RecordingPageUseCases.from(services);

        assertNotNull(useCases.analyzeRecordingWithAi());
        assertNotNull(useCases.askRecordingAssistant());
    }

    private static final class FakeRecordingRepository implements RecordingRepository {
        @Override
        public RecordingSummary open(Path path) {
            return new RecordingSummary("fake-recording", path, path.getFileName().toString(),
                    Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);
        }
    }

    private static final class FakeEventQueryService implements EventQueryService {
        @Override
        public EventQuerySession openSession(RecordingSummary recording) {
            throw new UnsupportedOperationException("not used by this test");
        }
    }

    private static final class FakeRuleAnalysisService implements RuleAnalysisService {
        @Override
        public List<RuleResult> analyze(RecordingSummary recording) {
            return List.of();
        }
    }

    private static final class FakeAiCompletionService implements AiCompletionService {
        @Override
        public AiCompletionResponse complete(AiCompletionRequest request) {
            return new AiCompletionResponse("{}");
        }
    }
}
