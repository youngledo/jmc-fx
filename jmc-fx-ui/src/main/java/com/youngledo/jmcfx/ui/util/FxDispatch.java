package com.youngledo.jmcfx.ui.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;

public final class FxDispatch {

    private static final Logger LOGGER = LoggerFactory.getLogger(FxDispatch.class);

    private FxDispatch() {
    }

    public static void run(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
            return;
        }
        try {
            CountDownLatch finished = new CountDownLatch(1);
            AtomicReference<RuntimeException> runtimeException = new AtomicReference<>();
            AtomicReference<Error> error = new AtomicReference<>();
            Platform.runLater(() -> {
                try {
                    runnable.run();
                } catch (RuntimeException exception) {
                    LOGGER.error("Runtime exception on JavaFX dispatch", exception);
                    runtimeException.set(exception);
                } catch (Error thrown) {
                    LOGGER.error("Error on JavaFX dispatch", thrown);
                    error.set(thrown);
                } finally {
                    finished.countDown();
                }
            });
            finished.await();
            if (runtimeException.get() != null) {
                throw runtimeException.get();
            }
            if (error.get() != null) {
                throw error.get();
            }
        } catch (IllegalStateException exception) {
            runnable.run();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for JavaFX state update", exception);
        }
    }
}
