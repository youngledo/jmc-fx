package io.github.youngledo.jmcfx.ui.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

class WorkbenchFocusSupportTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await(5, TimeUnit.SECONDS);
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized by another test class.
        }
    }

    @Test
    void preparesVisibleManagedTargetForFocus() throws Exception {
        Button button = new Button("Focus me");
        button.setFocusTraversable(false);

        runFxAndWait(() -> {
            WorkbenchFocusSupport.requestFocusWhenReady(button);
        });

        assertTrue(button.isFocusTraversable());
    }

    @Test
    void ignoresHiddenOrUnmanagedTarget() {
        Button button = new Button("Hidden");
        button.setVisible(false);
        button.setManaged(false);

        WorkbenchFocusSupport.requestFocusWhenReady(button);

        assertFalse(button.isFocused());
    }

    @Test
    void commandShortcutsDoNotStealNavigationFromTextInputs() {
        TextField field = new TextField();
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "f", "f", KeyCode.F,
                false, false, false, true);

        assertTrue(WorkbenchFocusSupport.isCommandShortcut(event, KeyCode.F));
        assertFalse(WorkbenchFocusSupport.shouldHandleNavigationShortcut(field, event));
    }

    @Test
    void navigationShortcutCanRunOutsideTextInputs() {
        Button button = new Button("Command target");
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "1", "1", KeyCode.DIGIT1,
                false, false, false, true);

        assertTrue(WorkbenchFocusSupport.shouldHandleNavigationShortcut(button, event));
    }

    private static void runFxAndWait(Runnable action) throws InterruptedException {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> failure =
                new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        if (failure.get() instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure.get() instanceof Error error) {
            throw error;
        }
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }
}
