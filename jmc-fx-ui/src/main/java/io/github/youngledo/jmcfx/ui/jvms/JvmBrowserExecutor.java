package io.github.youngledo.jmcfx.ui.jvms;

@FunctionalInterface
public interface JvmBrowserExecutor extends AutoCloseable {
    void execute(Runnable runnable);

    @Override
    default void close() {
    }
}
