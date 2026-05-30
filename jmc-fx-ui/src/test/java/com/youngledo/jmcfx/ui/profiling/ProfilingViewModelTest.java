package com.youngledo.jmcfx.ui.profiling;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.DependencyGraphEdge;
import com.youngledo.jmcfx.domain.model.DependencyGraphReport;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.StackTreeNode;
import com.youngledo.jmcfx.domain.service.ProfilingService;
import com.youngledo.jmcfx.testsupport.FakeProfilingService;

import org.junit.jupiter.api.Test;

import javafx.application.Platform;

import static org.junit.jupiter.api.Assertions.*;

class ProfilingViewModelTest {

    @Test
    void loadPopulatesHotMethods() {
        FakeProfilingService service = new FakeProfilingService();
        service.addHotMethod(new HotMethod("com.Foo.bar()", "JIT_COMPILED", 50, 50.0));
        service.addHotMethod(new HotMethod("com.Foo.baz()", "INTERPRETED", 30, 30.0));
        service.setDependencyReport(new DependencyGraphReport(
                List.of(new DependencyGraphEdge("com.foo", "com.bar", 7, 70.0)), 10, 2));

        ProfilingViewModel vm = new ProfilingViewModel(service);
        vm.load(testRecording());

        assertEquals(2, vm.hotMethodsProperty().size());
        assertEquals("com.Foo.bar()", vm.hotMethodsProperty().getFirst().method());
        assertEquals(1, vm.dependencyEdgesProperty().size());
        assertEquals("com.foo", vm.dependencyEdgesProperty().getFirst().source());
        assertEquals(2, vm.dependencyPackageDepthProperty().get());
    }

    @Test
    void loadBuildsDependencyGraphFromReportEdges() {
        FakeProfilingService service = new FakeProfilingService();
        service.setDependencyReport(new DependencyGraphReport(List.of(
                new DependencyGraphEdge("com.ui", "com.service", 8, 80.0),
                new DependencyGraphEdge("com.service", "com.repo", 2, 20.0)), 10, 2));

        ProfilingViewModel vm = new ProfilingViewModel(service);
        vm.load(testRecording());

        assertEquals(List.of("com.ui", "com.service", "com.repo"), labels(vm.dependencyGraphProperty().get()));
        assertEquals(2, vm.dependencyGraphProperty().get().edges().size());
        assertEquals("node-1", vm.dependencyGraphProperty().get().edges().getFirst().sourceId());
        assertEquals("node-2", vm.dependencyGraphProperty().get().edges().getFirst().targetId());
    }

    @Test
    void changingDependencyPackageDepthReloadsCurrentRecording() {
        FakeProfilingService service = new FakeProfilingService();
        service.setDependencyReport(new DependencyGraphReport(
                List.of(new DependencyGraphEdge("com.example", "org.example", 4, 100.0)), 4, 2));

        ProfilingViewModel vm = new ProfilingViewModel(service);
        vm.load(testRecording());

        service.setDependencyReport(new DependencyGraphReport(
                List.of(new DependencyGraphEdge("com", "org", 4, 100.0)), 4, 1));
        vm.setDependencyPackageDepth(1);

        assertEquals(1, vm.dependencyPackageDepthProperty().get());
        assertEquals("com", vm.dependencyEdgesProperty().getFirst().source());
        assertEquals(List.of("com", "org"), labels(vm.dependencyGraphProperty().get()));
    }

    @Test
    void loadClearsDependencyGraphAndSelection() {
        FakeProfilingService service = new FakeProfilingService();
        service.setDependencyReport(new DependencyGraphReport(
                List.of(new DependencyGraphEdge("a", "b", 1, 100.0)), 1, 1));

        ProfilingViewModel vm = new ProfilingViewModel(service);
        vm.load(testRecording());
        vm.selectedDependencyEdgeProperty().set(vm.dependencyEdgesProperty().getFirst());

        service.setDependencyReport(DependencyGraphReport.EMPTY);
        vm.load(testRecording());

        assertTrue(vm.dependencyEdgesProperty().isEmpty());
        assertNull(vm.selectedDependencyEdgeProperty().get());
        assertSame(CallGraphLayout.EMPTY, vm.dependencyGraphProperty().get());
    }

    @Test
    void selectMethodLoadsTree() {
        FakeProfilingService service = new FakeProfilingService();
        service.addHotMethod(new HotMethod("com.Foo.bar()", "JIT_COMPILED", 50, 50.0));

        ProfilingViewModel vm = new ProfilingViewModel(service);
        vm.load(testRecording());
        vm.selectMethod("com.Foo.bar()");

        // FakeProfilingService returns StackTreeNode.EMPTY for stack trees
        assertNotNull(vm.callersTreeProperty().get());
        assertNotNull(vm.calleesTreeProperty().get());
        assertEquals(StackTreeNode.EMPTY, vm.callersTreeProperty().get());
        assertEquals(StackTreeNode.EMPTY, vm.calleesTreeProperty().get());
    }

    @Test
    void selectMethodBuildsCallerAndCalleeFlameGraphs() {
        FakeProfilingService service = new FakeProfilingService();
        service.addHotMethod(new HotMethod("com.Foo.bar()", "JIT_COMPILED", 50, 50.0));
        service.setCallersTree(node("root", 100, node("caller", 60)));
        service.setCalleesTree(node("root", 100, node("callee", 70)));

        ProfilingViewModel vm = new ProfilingViewModel(service);
        vm.load(testRecording());
        vm.selectMethod("com.Foo.bar()");

        assertEquals("caller", vm.callersFlameGraphProperty().get().frames().getFirst().method());
        assertEquals("callee", vm.calleesFlameGraphProperty().get().frames().getFirst().method());
    }

    @Test
    void selectMethodBuildsCallGraphFromCurrentDirection() {
        FakeProfilingService service = new FakeProfilingService();
        service.addHotMethod(new HotMethod("com.Foo.bar()", "JIT_COMPILED", 50, 50.0));
        service.setCalleesTree(node("root", 100, node("callee", 70)));

        ProfilingViewModel vm = new ProfilingViewModel(service);
        vm.load(testRecording());
        vm.selectMethod("com.Foo.bar()");

        CallGraphLayout layout = vm.callGraphProperty().get();
        assertEquals("com.Foo.bar()", layout.nodes().getFirst().label());
        assertEquals("callee", layout.nodes().get(1).label());
        assertEquals("selected", layout.edges().getFirst().sourceId());
        assertEquals("node-1", layout.edges().getFirst().targetId());
    }

    @Test
    void changingCallGraphDirectionRebuildsFromLoadedTrees() {
        FakeProfilingService service = new FakeProfilingService();
        service.addHotMethod(new HotMethod("com.Foo.bar()", "JIT_COMPILED", 50, 50.0));
        service.setCallersTree(node("root", 100, node("caller", 60)));
        service.setCalleesTree(node("root", 100, node("callee", 70)));

        ProfilingViewModel vm = new ProfilingViewModel(service);
        vm.load(testRecording());
        vm.selectMethod("com.Foo.bar()");

        vm.setCallGraphDirection(CallGraphDirection.CALLERS);

        CallGraphLayout layout = vm.callGraphProperty().get();
        assertEquals(CallGraphDirection.CALLERS, vm.callGraphDirectionProperty().get());
        assertEquals("caller", layout.nodes().get(1).label());
        assertEquals("node-1", layout.edges().getFirst().sourceId());
        assertEquals("selected", layout.edges().getFirst().targetId());
    }

    @Test
    void changingCallGraphDepthRebuildsWithNewLimit() {
        FakeProfilingService service = new FakeProfilingService();
        service.addHotMethod(new HotMethod("com.Foo.bar()", "JIT_COMPILED", 50, 50.0));
        service.setCalleesTree(node("root", 100, node("callee", 70, node("deep callee", 40))));

        ProfilingViewModel vm = new ProfilingViewModel(service);
        vm.load(testRecording());
        vm.selectMethod("com.Foo.bar()");

        vm.setCallGraphMaxDepth(1);

        CallGraphLayout layout = vm.callGraphProperty().get();
        assertEquals(1, vm.callGraphMaxDepthProperty().get());
        assertTrue(layout.maxDepth() <= 1);
        assertEquals(List.of("com.Foo.bar()", "callee"), labels(layout));
    }

    @Test
    void loadAndNullSelectionClearCallGraph() {
        FakeProfilingService service = new FakeProfilingService();
        service.addHotMethod(new HotMethod("com.Foo.bar()", "JIT_COMPILED", 50, 50.0));
        service.setCalleesTree(node("root", 100, node("callee", 70)));

        ProfilingViewModel vm = new ProfilingViewModel(service);
        vm.load(testRecording());
        vm.selectMethod("com.Foo.bar()");

        assertFalse(vm.callGraphProperty().get().nodes().isEmpty());

        vm.selectMethod(null);

        assertSame(CallGraphLayout.EMPTY, vm.callGraphProperty().get());
    }

    @Test
    void loadClearsCallGraphAndPreventsStaleRebuild() {
        FakeProfilingService service = new FakeProfilingService();
        service.addHotMethod(new HotMethod("com.Foo.bar()", "JIT_COMPILED", 50, 50.0));
        service.setCallersTree(node("root", 100, node("caller", 60)));
        service.setCalleesTree(node("root", 100, node("callee", 70)));

        ProfilingViewModel vm = new ProfilingViewModel(service);
        vm.load(testRecording());
        vm.selectMethod("com.Foo.bar()");

        assertFalse(vm.callGraphProperty().get().nodes().isEmpty());

        vm.load(testRecording());
        vm.setCallGraphDirection(CallGraphDirection.CALLERS);
        vm.setCallGraphMaxDepth(1);

        assertNull(vm.selectedMethodProperty().get());
        assertSame(CallGraphLayout.EMPTY, vm.callGraphProperty().get());
    }

    @Test
    void nullCallGraphMaxDepthFallsBackDuringRebuild() {
        FakeProfilingService service = new FakeProfilingService();
        service.addHotMethod(new HotMethod("com.Foo.bar()", "JIT_COMPILED", 50, 50.0));
        service.setCallersTree(node("root", 100, node("caller", 60)));
        service.setCalleesTree(node("root", 100, node("callee", 70)));

        ProfilingViewModel vm = new ProfilingViewModel(service);
        vm.load(testRecording());
        vm.selectMethod("com.Foo.bar()");

        vm.callGraphMaxDepthProperty().set(null);

        assertDoesNotThrow(() -> vm.setCallGraphDirection(CallGraphDirection.CALLERS));
        assertEquals("caller", vm.callGraphProperty().get().nodes().get(1).label());
    }

    @Test
    void selectMethodBuildsCallGraphFromLatestDispatchState() {
        startJavaFx();

        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownLatch releaseFx = new CountDownLatch(1);
        CountDownLatch retuneScheduled = new CountDownLatch(1);
        AtomicReference<ProfilingViewModel> vmReference = new AtomicReference<>();
        AtomicInteger treeLoads = new AtomicInteger();

        ProfilingService service = new ProfilingService() {
            @Override
            public List<HotMethod> loadHotMethods(RecordingSummary recording) {
                return List.of(new HotMethod("com.Foo.bar()", "JIT_COMPILED", 50, 50.0));
            }

            @Override
            public StackTreeNode loadStackTraceTree(RecordingSummary recording, String method, boolean callers) {
                StackTreeNode tree = callers
                        ? node("root", 100, node("caller", 80, node("deep caller", 40)))
                        : node("root", 100, node("callee", 70, node("deep callee", 30)));
                if (treeLoads.incrementAndGet() == 2) {
                    Platform.runLater(() -> {
                        vmReference.get().setCallGraphDirection(CallGraphDirection.CALLERS);
                        vmReference.get().setCallGraphMaxDepth(1);
                    });
                    retuneScheduled.countDown();
                }
                return tree;
            }

            @Override
            public DependencyGraphReport loadPackageDependencies(RecordingSummary recording, int packageDepth) {
                return DependencyGraphReport.EMPTY;
            }
        };
        ProfilingViewModel vm = new ProfilingViewModel(service);
        vmReference.set(vm);
        vm.load(testRecording());

        Platform.runLater(() -> {
            blockerStarted.countDown();
            awaitUnchecked(releaseFx);
        });
        await(blockerStarted);

        Thread selectThread = new Thread(() -> vm.selectMethod("com.Foo.bar()"), "select-method-test");
        selectThread.start();
        await(retuneScheduled);
        awaitWaiting(selectThread);

        releaseFx.countDown();
        join(selectThread);

        CallGraphLayout layout = vm.callGraphProperty().get();
        assertEquals(CallGraphDirection.CALLERS, vm.callGraphDirectionProperty().get());
        assertEquals(1, vm.callGraphMaxDepthProperty().get());
        assertTrue(layout.maxDepth() <= 1);
        assertEquals(List.of("com.Foo.bar()", "caller"), labels(layout));
        assertEquals("node-1", layout.edges().getFirst().sourceId());
        assertEquals("selected", layout.edges().getFirst().targetId());
    }

    @Test
    void loadClearsFlameGraphs() {
        FakeProfilingService service = new FakeProfilingService();
        service.addHotMethod(new HotMethod("com.Foo.bar()", "JIT_COMPILED", 50, 50.0));
        service.setCallersTree(node("root", 100, node("caller", 60)));
        service.setCalleesTree(node("root", 100, node("callee", 70)));

        ProfilingViewModel vm = new ProfilingViewModel(service);
        vm.load(testRecording());
        vm.selectMethod("com.Foo.bar()");

        assertNotEquals(FlameGraphLayout.EMPTY, vm.callersFlameGraphProperty().get());

        vm.load(testRecording());

        assertEquals(FlameGraphLayout.EMPTY, vm.callersFlameGraphProperty().get());
        assertEquals(FlameGraphLayout.EMPTY, vm.calleesFlameGraphProperty().get());
    }

    @Test
    void selectNullMethodClearsStackDetails() {
        FakeProfilingService service = new FakeProfilingService();
        service.addHotMethod(new HotMethod("com.Foo.bar()", "JIT_COMPILED", 50, 50.0));
        service.setCallersTree(node("root", 100, node("caller", 60)));
        service.setCalleesTree(node("root", 100, node("callee", 70)));

        ProfilingViewModel vm = new ProfilingViewModel(service);
        vm.load(testRecording());
        vm.selectMethod("com.Foo.bar()");

        assertNotEquals(StackTreeNode.EMPTY, vm.callersTreeProperty().get());
        assertNotEquals(StackTreeNode.EMPTY, vm.calleesTreeProperty().get());
        assertNotEquals(FlameGraphLayout.EMPTY, vm.callersFlameGraphProperty().get());
        assertNotEquals(FlameGraphLayout.EMPTY, vm.calleesFlameGraphProperty().get());

        vm.selectMethod(null);

        assertEquals(StackTreeNode.EMPTY, vm.callersTreeProperty().get());
        assertEquals(StackTreeNode.EMPTY, vm.calleesTreeProperty().get());
        assertEquals(FlameGraphLayout.EMPTY, vm.callersFlameGraphProperty().get());
        assertEquals(FlameGraphLayout.EMPTY, vm.calleesFlameGraphProperty().get());
    }

    @Test
    void selectMethodNormalizesNullStackTreesToEmpty() {
        ProfilingService service = new ProfilingService() {
            @Override
            public List<HotMethod> loadHotMethods(RecordingSummary recording) {
                return List.of(new HotMethod("com.Foo.bar()", "JIT_COMPILED", 50, 50.0));
            }

            @Override
            public StackTreeNode loadStackTraceTree(RecordingSummary recording, String method, boolean callers) {
                return null;
            }

            @Override
            public DependencyGraphReport loadPackageDependencies(RecordingSummary recording, int packageDepth) {
                return DependencyGraphReport.EMPTY;
            }
        };
        ProfilingViewModel vm = new ProfilingViewModel(service);

        vm.load(testRecording());
        vm.selectMethod("com.Foo.bar()");

        assertEquals(StackTreeNode.EMPTY, vm.callersTreeProperty().get());
        assertEquals(StackTreeNode.EMPTY, vm.calleesTreeProperty().get());
        assertEquals(FlameGraphLayout.EMPTY, vm.callersFlameGraphProperty().get());
        assertEquals(FlameGraphLayout.EMPTY, vm.calleesFlameGraphProperty().get());
    }

    @Test
    void loadResetsPreviousState() {
        FakeProfilingService service = new FakeProfilingService();
        service.addHotMethod(new HotMethod("com.Foo.bar()", "JIT_COMPILED", 50, 50.0));
        service.setCallersTree(node("root", 100, node("caller", 60)));
        service.setCalleesTree(node("root", 100, node("callee", 70)));

        ProfilingViewModel vm = new ProfilingViewModel(service);
        vm.load(testRecording());
        vm.selectMethod("com.Foo.bar()");

        assertEquals("caller", vm.callersTreeProperty().get().children().getFirst().method());
        assertEquals("callee", vm.calleesTreeProperty().get().children().getFirst().method());

        vm.load(testRecording());

        assertEquals(1, vm.hotMethodsProperty().size());
        assertEquals("com.Foo.bar()", vm.hotMethodsProperty().getFirst().method());
        assertNull(vm.selectedMethodProperty().get());
        assertEquals(StackTreeNode.EMPTY, vm.callersTreeProperty().get());
        assertEquals(StackTreeNode.EMPTY, vm.calleesTreeProperty().get());
        assertEquals(FlameGraphLayout.EMPTY, vm.callersFlameGraphProperty().get());
        assertEquals(FlameGraphLayout.EMPTY, vm.calleesFlameGraphProperty().get());
    }

    @Test
    void selectMethodWithoutRecordingIsNoOp() {
        FakeProfilingService service = new FakeProfilingService();
        ProfilingViewModel vm = new ProfilingViewModel(service);

        vm.selectMethod("com.Foo.bar()");

        assertEquals(StackTreeNode.EMPTY, vm.callersTreeProperty().get());
        assertEquals(StackTreeNode.EMPTY, vm.calleesTreeProperty().get());
        assertEquals(FlameGraphLayout.EMPTY, vm.callersFlameGraphProperty().get());
        assertEquals(FlameGraphLayout.EMPTY, vm.calleesFlameGraphProperty().get());
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.now(), Instant.now(), 1000, 1024);
    }

    private StackTreeNode node(String method, int count, StackTreeNode... children) {
        return new StackTreeNode(method, count, 0, List.of(children));
    }

    private List<String> labels(CallGraphLayout layout) {
        return layout.nodes().stream()
                .map(CallGraphNode::label)
                .toList();
    }

    private static void startJavaFx() {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
            await(started);
        } catch (IllegalStateException exception) {
            // JavaFX toolkit is already running for this test JVM.
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for latch");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for latch");
        }
    }

    private static void awaitUnchecked(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for latch", exception);
        }
    }

    private static void awaitWaiting(Thread thread) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (thread.getState() == Thread.State.WAITING) {
                return;
            }
            Thread.onSpinWait();
        }
        fail("Timed out waiting for thread to enter WAITING state");
    }

    private static void join(Thread thread) {
        try {
            thread.join(5000);
            assertFalse(thread.isAlive(), "Thread did not finish");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            fail("Interrupted while joining thread");
        }
    }
}
