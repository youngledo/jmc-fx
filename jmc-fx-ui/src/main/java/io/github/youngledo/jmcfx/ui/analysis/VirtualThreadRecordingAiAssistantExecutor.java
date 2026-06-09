package io.github.youngledo.jmcfx.ui.analysis;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class VirtualThreadRecordingAiAssistantExecutor implements RecordingAiAssistantExecutor {

    private final ExecutorService delegate = Executors.newSingleThreadExecutor(
            Thread.ofVirtual().name("jmcfx-recording-ai-", 0).factory());

    @Override
    public void execute(Runnable runnable) {
        delegate.execute(runnable);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
