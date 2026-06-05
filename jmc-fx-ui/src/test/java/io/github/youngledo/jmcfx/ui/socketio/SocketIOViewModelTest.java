package io.github.youngledo.jmcfx.ui.socketio;

import java.nio.file.Path;
import java.time.Instant;

import io.github.youngledo.jmcfx.application.LoadSocketIOUseCase;

import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.SocketIOGrouping;
import io.github.youngledo.jmcfx.domain.model.SocketIOHistogram;
import io.github.youngledo.jmcfx.ui.testsupport.FakeSocketIOService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SocketIOViewModelTest {

    @Test
    void loadPopulatesHistogram() {
        FakeSocketIOService service = new FakeSocketIOService();
        service.addHistogramRow(new SocketIOHistogram("10.0.0.1:8080",
                "10.0.0.1", 8080, 50, 30, 102400, 51200, 300, 25, 3.75));

        SocketIOViewModel vm = new SocketIOViewModel(new LoadSocketIOUseCase(service));
        vm.load(testRecording());

        assertEquals(1, vm.histogramProperty().size());
        assertEquals("10.0.0.1:8080", vm.histogramProperty().getFirst().key());
    }

    @Test
    void setGroupingReloadsHistogram() {
        FakeSocketIOService service = new FakeSocketIOService();
        service.addHistogramRow(new SocketIOHistogram("10.0.0.1:8080",
                "10.0.0.1", 8080, 50, 30, 102400, 51200, 300, 25, 3.75));

        SocketIOViewModel vm = new SocketIOViewModel(new LoadSocketIOUseCase(service));
        vm.load(testRecording());

        assertEquals(SocketIOGrouping.BY_HOST_AND_PORT, vm.groupingProperty().get());

        vm.setGrouping(SocketIOGrouping.BY_HOST);

        assertEquals(SocketIOGrouping.BY_HOST, vm.groupingProperty().get());
        assertEquals(1, vm.histogramProperty().size());
    }

    private RecordingSummary testRecording() {
        return new RecordingSummary("test", Path.of("test.jfr"), "test",
                Instant.now(), Instant.now(), 1000, 1024);
    }
}
