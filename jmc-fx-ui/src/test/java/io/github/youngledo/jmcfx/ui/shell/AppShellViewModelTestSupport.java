package io.github.youngledo.jmcfx.ui.shell;

import java.nio.file.Path;
import java.time.Instant;

import io.github.youngledo.jmcfx.application.AnalyzeRulesUseCase;
import io.github.youngledo.jmcfx.application.BrowseEventsUseCase;
import io.github.youngledo.jmcfx.application.DiagnosticFindingsUseCase;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.service.EventQueryService;
import io.github.youngledo.jmcfx.domain.service.EventQuerySession;
import io.github.youngledo.jmcfx.ui.events.EventBrowserBackgroundExecutor;
import io.github.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import io.github.youngledo.jmcfx.ui.overview.OverviewViewModel;
import io.github.youngledo.jmcfx.ui.rules.RuleResultsViewModel;

final class AppShellViewModelTestSupport {

    private AppShellViewModelTestSupport() {
    }

    static RecordingWorkspace openMinimalRecording() {
        return new AppShellViewModel().openRecording(
                recording(), new OverviewViewModel(), eventBrowserViewModel(), ruleResultsViewModel());
    }

    static RecordingSummary recording() {
        Path path = Path.of("rec.jfr");
        return new RecordingSummary("rec", path, path.getFileName().toString(),
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);
    }

    static EventBrowserViewModel eventBrowserViewModel() {
        return new EventBrowserViewModel(new BrowseEventsUseCase(new EmptyEventQueryService()), new DirectExecutor());
    }

    static RuleResultsViewModel ruleResultsViewModel() {
        return new RuleResultsViewModel(AnalyzeRulesUseCase.empty(), new DiagnosticFindingsUseCase());
    }

    private static final class EmptyEventQueryService implements EventQueryService {
        @Override
        public EventQuerySession openSession(RecordingSummary recording) {
            throw new UnsupportedOperationException("Not used by shell tests.");
        }
    }

    private static final class DirectExecutor implements EventBrowserBackgroundExecutor {
        @Override
        public void execute(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void close() {
        }
    }
}
