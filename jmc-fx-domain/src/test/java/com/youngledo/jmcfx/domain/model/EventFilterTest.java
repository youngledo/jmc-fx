package com.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class EventFilterTest {

    @Test
    void createsCompositeFilterWithFieldCondition() {
        EventFieldCondition condition = new EventFieldCondition("duration", EventFilterOperator.GREATER_THAN, "10 ms");
        EventFilter filter = new EventFilter("timeout", "worker", Instant.EPOCH, Instant.EPOCH.plusSeconds(10),
                List.of(condition));

        assertEquals("timeout", filter.text());
        assertEquals("worker", filter.thread());
        assertEquals(condition, filter.fieldConditions().getFirst());
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventFilter("", "", Instant.EPOCH.plusSeconds(1), Instant.EPOCH, List.of()));
    }
}
