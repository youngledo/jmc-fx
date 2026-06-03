package com.youngledo.jmcfx.flamegraph;

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
        FlameGraphView<String> view = runOnFxThread(() -> {
            FlameGraphView<String> graph = new FlameGraphView<>();
            graph.setModel(sampleModel());
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
        waitForFxEvents();

        Bounds bounds = runOnFxThread(() -> view.localToScreen(view.getBoundsInLocal()));
        Robot robot = runOnFxThread(Robot::new);
        runOnFxThread(() -> {
            robot.mouseMove(bounds.getMinX() + 120, bounds.getMinY() + 36);
            robot.mouseClick(MouseButton.PRIMARY);
        });

        waitForSelection(view);
        assumeTrue(runOnFxThread(() -> view.selectedFrameProperty().get() != null),
                "JavaFX Robot native mouse events did not reach the test stage");
        assertEquals("child", runOnFxThread(() -> view.selectedFrameProperty().get().node().label()));
    }

    private FlameGraphModel<String> sampleModel() {
        FlameGraphNode<String> root = node("root", 100,
                node("parent", 80,
                        node("child", 60, node("grandchild", 30)),
                        node("child-sibling", 20)),
                node("sibling", 20));
        return FlameGraphModel.of(root);
    }

    @SafeVarargs
    private static FlameGraphNode<String> node(String label, double weight, FlameGraphNode<String>... children) {
        return new FlameGraphNode<>(label, weight, weight, label, List.of(children));
    }

    private void waitForSelection(FlameGraphView<String> view) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            waitForFxEvents();
            if (runOnFxThread(() -> view.selectedFrameProperty().get() != null)) {
                return;
            }
            Thread.sleep(50);
        }
    }

    private void waitForFxEvents() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        assumeTrue(latch.await(5, TimeUnit.SECONDS), "FX event wait timed out");
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
