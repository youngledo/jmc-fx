package io.github.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    void serializesRecordingWorkToAvoidConcurrentJfrScans() throws Exception {
        try (VirtualThreadRecordingOpenExecutor executor = new VirtualThreadRecordingOpenExecutor()) {
            CountDownLatch firstStarted = new CountDownLatch(1);
            CountDownLatch releaseFirst = new CountDownLatch(1);
            CountDownLatch secondStarted = new CountDownLatch(1);
            CountDownLatch secondFinished = new CountDownLatch(1);
            AtomicInteger secondRuns = new AtomicInteger();

            executor.execute(() -> {
                firstStarted.countDown();
                await(releaseFirst);
            });
            executor.execute(() -> {
                secondStarted.countDown();
                secondRuns.incrementAndGet();
                secondFinished.countDown();
            });

            assertTrue(firstStarted.await(5, TimeUnit.SECONDS));
            try {
                assertFalse(secondStarted.await(200, TimeUnit.MILLISECONDS));
                assertEquals(0, secondRuns.get());
            } finally {
                releaseFirst.countDown();
            }

            assertTrue(secondFinished.await(5, TimeUnit.SECONDS));
            assertEquals(1, secondRuns.get());
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
