package com.youngledo.jmcfx.ui.shell;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class VirtualThreadRecordingOpenExecutor implements RecordingOpenExecutor {

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
