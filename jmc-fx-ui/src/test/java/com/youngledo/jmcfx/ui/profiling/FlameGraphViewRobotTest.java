package com.youngledo.jmcfx.ui.profiling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FlameGraphViewRobotTest {

    private static final String ENABLE_NATIVE_ROBOT_PROPERTY = "jmcfx.test.nativeRobot";

    private Stage stage;

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

    @AfterEach
    void closeStage() throws Exception {
        if (stage != null) {
            runOnFxThread(stage::close);
            stage = null;
        }
    }

    @Test
    void javaFxRobotCanBeCreatedForUiAutomationSmokeTests() throws Exception {
        assumeTrue(!java.awt.GraphicsEnvironment.isHeadless(), "JavaFX Robot needs a display");

        Robot robot = runOnFxThread(Robot::new);

        assertNotNull(robot);
    }

    @Test
    void robotClickSelectsFlameGraphFrameWhenNativeMouseEventsAreEnabled() throws Exception {
        assumeTrue(!java.awt.GraphicsEnvironment.isHeadless(), "JavaFX Robot needs a display");
        assumeTrue(Boolean.getBoolean(ENABLE_NATIVE_ROBOT_PROPERTY),
                "Native JavaFX Robot mouse tests require -D" + ENABLE_NATIVE_ROBOT_PROPERTY + "=true");
        FlameGraphView view = runOnFxThread(() -> {
            FlameGraphView graph = new FlameGraphView();
            graph.setLayout(new FlameGraphLayout(List.of(
                    new FlameGraphFrame("parent", 80, 80, 0, 0, 0.8),
                    new FlameGraphFrame("sibling", 20, 20, 0, 0.8, 0.2),
                    new FlameGraphFrame("child", 60, 60, 1, 0.2, 0.6),
                    new FlameGraphFrame("grandchild", 30, 30, 2, 0.3, 0.3)), 3));
            graph.resize(640, graph.prefHeight(-1));
            stage = new Stage();
            stage.setScene(new Scene(new StackPane(graph), 720, 220));
            stage.show();
            stage.toFront();
            stage.requestFocus();
            graph.requestFocus();
            graph.layout();
            return graph;
        });

        Bounds childBounds = runOnFxThread(() -> view.getChildren().get(2).localToScreen(
                view.getChildren().get(2).getBoundsInLocal()));
        Robot robot = runOnFxThread(Robot::new);
        runOnFxThread(() -> {
            robot.mouseMove(childBounds.getCenterX(), childBounds.getCenterY());
            robot.mouseClick(MouseButton.PRIMARY);
        });

        waitForNativeMouseEvents();
        assumeTrue(runOnFxThread(() -> view.getChildren().get(2).getStyleClass()
                .contains("flame-graph-frame-selected")), "JavaFX Robot native mouse events did not reach the test stage");
        assertEquals(4, runOnFxThread(view::frameCount));
    }

    private void waitForNativeMouseEvents() throws InterruptedException {
        Thread.sleep(250);
    }

    private <T> T runOnFxThread(java.util.concurrent.Callable<T> action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return action.call();
        }
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(action.call());
            } catch (Exception ex) {
                failure.set(ex);
            } finally {
                latch.countDown();
            }
        });
        assumeTrue(latch.await(5, TimeUnit.SECONDS), "FX action timed out");
        if (failure.get() != null) {
            throw failure.get();
        }
        return result.get();
    }

    private void runOnFxThread(Runnable action) throws Exception {
        runOnFxThread(() -> {
            action.run();
            return null;
        });
    }
}
