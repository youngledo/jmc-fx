package com.youngledo.jmcfx.ui.heapdump;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class DirectHeapDumpAnalysisExecutor implements HeapDumpAnalysisExecutor {

    @Override
    public <T> void execute(Supplier<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        try {
            onSuccess.accept(task.get());
        } catch (Throwable throwable) {
            onFailure.accept(throwable);
        }
    }
}
