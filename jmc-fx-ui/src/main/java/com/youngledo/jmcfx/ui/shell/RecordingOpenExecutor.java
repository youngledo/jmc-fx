package com.youngledo.jmcfx.ui.shell;

interface RecordingOpenExecutor extends AutoCloseable {
    void execute(Runnable runnable);

    @Override
    default void close() {
    }
}
