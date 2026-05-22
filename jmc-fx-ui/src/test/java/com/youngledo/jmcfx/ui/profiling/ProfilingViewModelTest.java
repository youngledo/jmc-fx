package com.youngledo.jmcfx.ui.profiling;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.domain.model.HotMethod;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.model.StackTreeNode;
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
    void loadResetsPreviousState() {
        FakeProfilingService service = new FakeProfilingService();
        service.addHotMethod(new HotMethod("com.Foo.bar()", "JIT_COMPILED", 50, 50.0));

        ProfilingViewModel vm = new ProfilingViewModel(service);
        vm.load(testRecording());
        vm.selectMethod("com.Foo.bar()");

        // Load again with empty service — should reset everything
        FakeProfilingService emptyService = new FakeProfilingService();
        ProfilingViewModel vm2 = new ProfilingViewModel(emptyService);
        vm2.load(testRecording());

        assertEquals(0, vm2.hotMethodsProperty().size());
        assertEquals(StackTreeNode.EMPTY, vm2.callersTreeProperty().get());
        assertEquals(StackTreeNode.EMPTY, vm2.calleesTreeProperty().get());
    }

    @Test
    void selectMethodWithoutRecordingIsNoOp() {
        FakeProfilingService service = new FakeProfilingService();
        ProfilingViewModel vm = new ProfilingViewModel(service);

        vm.selectMethod("com.Foo.bar()");

        assertEquals(StackTreeNode.EMPTY, vm.callersTreeProperty().get());
        assertEquals(StackTreeNode.EMPTY, vm.calleesTreeProperty().get());
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.now(), Instant.now(), 1000, 1024);
    }
}
