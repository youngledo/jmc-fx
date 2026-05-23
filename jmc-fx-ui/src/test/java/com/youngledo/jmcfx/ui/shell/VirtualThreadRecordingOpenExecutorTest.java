package com.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class VirtualThreadRecordingOpenExecutorTest {

    @Test
    void executesRecordingOpenWorkOnVirtualThread() throws Exception {
        try (VirtualThreadRecordingOpenExecutor executor = new VirtualThreadRecordingOpenExecutor()) {
            CountDownLatch finished = new CountDownLatch(1);
            AtomicBoolean virtualThread = new AtomicBoolean(false);

            executor.execute(() -> {
                virtualThread.set(Thread.currentThread().isVirtual());
                finished.countDown();
            });

            assertTrue(finished.await(5, TimeUnit.SECONDS));
            assertTrue(virtualThread.get());
        }
    }
}
