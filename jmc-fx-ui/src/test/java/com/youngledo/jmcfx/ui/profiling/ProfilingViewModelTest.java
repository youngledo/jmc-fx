package com.youngledo.jmcfx.ui.profiling;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.StackTreeNode;
import com.youngledo.jmcfx.domain.service.ProfilingService;
import com.youngledo.jmcfx.testsupport.FakeProfilingService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfilingViewModelTest {

    @Test
    void loadPopulatesHotMethods() {
        FakeProfilingService service = new FakeProfilingService();
        service.addHotMethod(new HotMethod("com.Foo.bar()", "JIT_COMPILED", 50, 50.0));
        service.addHotMethod(new HotMethod("com.Foo.baz()", "INTERPRETED", 30, 30.0));

        ProfilingViewModel vm = new ProfilingViewModel(service);
        vm.load(testRecording());

        assertEquals(2, vm.hotMethodsProperty().size());
        assertEquals("com.Foo.bar()", vm.hotMethodsProperty().getFirst().method());
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
}
