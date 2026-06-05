package io.github.youngledo.jmcfx.ui.heapdump;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface HeapDumpAnalysisExecutor extends AutoCloseable {

    <T> void execute(Supplier<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailure);

    @Override
    default void close() {
    }
}
