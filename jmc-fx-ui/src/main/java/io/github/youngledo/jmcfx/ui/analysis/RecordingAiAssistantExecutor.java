package io.github.youngledo.jmcfx.ui.analysis;

public interface RecordingAiAssistantExecutor extends AutoCloseable {
    void execute(Runnable runnable);

    @Override
    default void close() {
    }
}
