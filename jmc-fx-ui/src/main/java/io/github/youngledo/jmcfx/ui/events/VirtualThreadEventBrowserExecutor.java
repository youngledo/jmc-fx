package io.github.youngledo.jmcfx.ui.events;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtualThreadEventBrowserExecutor implements EventBrowserBackgroundExecutor {

    private final ExecutorService delegate = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void execute(Runnable runnable) {
        delegate.execute(runnable);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
