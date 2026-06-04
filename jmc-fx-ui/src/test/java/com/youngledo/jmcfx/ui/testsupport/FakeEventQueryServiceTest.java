package com.youngledo.jmcfx.ui.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.EventFilter;
import com.youngledo.jmcfx.domain.model.EventWindow;
import com.youngledo.jmcfx.domain.model.EventWindowRequest;
import com.youngledo.jmcfx.domain.model.RecordingSummary;

class FakeEventQueryServiceTest {

    @Test
    void returnsJmcCategoryTreeAndWindowRows() {
        FakeEventQueryService service = new FakeEventQueryService();

        assertEquals("Operating System", service.loadEventTypeTree(recording()).getFirst().label());

        EventWindow window = service.loadEventWindow(recording(),
                new EventWindowRequest("jdk.CPULoad", 0, 50, 0, 50, List.of("jvmUser"), EventFilter.empty()));

        assertEquals("jdk.CPULoad", service.lastWindowRequest().eventTypeId());
        assertFalse(window.rows().isEmpty());
        assertEquals("0.12", window.rows().getFirst().fieldValues().get("jvmUser"));
    }

    @Test
    void returnsDetailsForSelectedEvent() {
        FakeEventQueryService service = new FakeEventQueryService();

        assertEquals("jdk.CPULoad#0", service.loadEventDetails(recording(), "jdk.CPULoad#0").eventId());
    }

    private RecordingSummary recording() {
        return new RecordingSummary("rec", Path.of("rec.jfr"), "rec.jfr",
                Instant.EPOCH, Instant.EPOCH.plusSeconds(1), 1000, 128);
    }
}
