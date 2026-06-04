package com.youngledo.jmcfx.ui.leaks;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import com.youngledo.jmcfx.application.LoadLeakSuspectsUseCase;

import com.youngledo.jmcfx.domain.model.LeakCandidate;
import com.youngledo.jmcfx.domain.model.LeakReferenceNode;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.ui.testsupport.FakeLeakSuspectsService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LeakSuspectsViewModelTest {

    @Test
    void loadPopulatesCandidates() {
        FakeLeakSuspectsService service = new FakeLeakSuspectsService();
        service.addCandidate(new LeakCandidate("java.util.HashMap$Node", 5, "HashMap nodes", "0x7fa0", 75.0));

        LeakSuspectsViewModel vm = new LeakSuspectsViewModel(new LoadLeakSuspectsUseCase(service));
        vm.load(testRecording());

        assertEquals(1, vm.candidatesProperty().size());
        assertEquals("java.util.HashMap$Node", vm.candidatesProperty().getFirst().object());
    }

    @Test
    void selectCandidateLoadsReferenceTree() {
        FakeLeakSuspectsService service = new FakeLeakSuspectsService();
        service.addCandidate(new LeakCandidate("java.lang.Object[]", 3, "Array", "0x1a00", 50.0));
        service.setReferenceTree(new LeakReferenceNode("java.lang.Object[] @ 0x1a00",
                "Large array", "0x1a00", List.of()));

        LeakSuspectsViewModel vm = new LeakSuspectsViewModel(new LoadLeakSuspectsUseCase(service));
        vm.load(testRecording());
        vm.selectCandidate(0);

        assertNotNull(vm.referenceTreeProperty().get());
        assertEquals("java.lang.Object[] @ 0x1a00", vm.referenceTreeProperty().get().object());
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.now(), Instant.now(), 1000, 1024);
    }
}
