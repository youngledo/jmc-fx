package io.github.youngledo.jmcfx.ui.heapdump;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class VirtualThreadHeapDumpAnalysisExecutor implements HeapDumpAnalysisExecutor {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public <T> void execute(Supplier<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        executor.submit(() -> {
            try {
                onSuccess.accept(task.get());
            } catch (Throwable throwable) {
                onFailure.accept(throwable);
            }
        });
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
