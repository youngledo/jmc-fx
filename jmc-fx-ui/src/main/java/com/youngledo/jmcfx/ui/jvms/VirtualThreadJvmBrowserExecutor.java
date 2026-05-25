package com.youngledo.jmcfx.ui.jvms;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtualThreadJvmBrowserExecutor implements JvmBrowserExecutor {

    private final ExecutorService executor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("jmcfx-jvm-browser-", 0).factory());

    @Override
    public void execute(Runnable runnable) {
        executor.execute(runnable);
    }

    @Override
    public void close() {
        executor.close();
    }
}
