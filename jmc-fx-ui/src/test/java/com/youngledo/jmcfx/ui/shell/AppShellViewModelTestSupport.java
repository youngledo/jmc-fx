package com.youngledo.jmcfx.ui.shell;

import java.nio.file.Path;
import java.time.Instant;

import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.EventQueryService;
import com.youngledo.jmcfx.domain.service.EventQuerySession;
import com.youngledo.jmcfx.ui.events.EventBrowserBackgroundExecutor;
import com.youngledo.jmcfx.ui.events.EventBrowserViewModel;
import com.youngledo.jmcfx.ui.overview.OverviewViewModel;
import com.youngledo.jmcfx.ui.rules.RuleResultsViewModel;

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
        return new EventBrowserViewModel(new EmptyEventQueryService(), new DirectExecutor());
    }

    static RuleResultsViewModel ruleResultsViewModel() {
        return new RuleResultsViewModel(rec -> java.util.List.of());
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
