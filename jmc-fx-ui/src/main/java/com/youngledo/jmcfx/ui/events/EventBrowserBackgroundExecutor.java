package com.youngledo.jmcfx.ui.events;

public interface EventBrowserBackgroundExecutor extends AutoCloseable {
    void execute(Runnable runnable);

    @Override
    default void close() {
    }
}
