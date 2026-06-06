package io.github.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class JmxNotificationHistoryFilterTest {

    @Test
    void blankFiltersMatchAnyNotificationEvent() {
        JmxNotificationHistoryFilter filter = new JmxNotificationHistoryFilter(" ", null, null, null);
        JmxNotificationEvent event = new JmxNotificationEvent(
                "notif-1", Instant.EPOCH, "memory.threshold", "heap", 1, "Heap high", "");

        assertTrue(filter.matches(event));
    }

    @Test
    void filtersTypeMessageAndInclusiveTimeWindow() {
        JmxNotificationHistoryFilter filter = new JmxNotificationHistoryFilter(
                "MEMORY", "heap",
                Instant.parse("2026-06-06T01:00:00Z"),
                Instant.parse("2026-06-06T01:05:00Z"));

        assertTrue(filter.matches(new JmxNotificationEvent(
                "notif-1", Instant.parse("2026-06-06T01:00:00Z"),
                "memory.threshold", "heap", 1, "Heap high", "")));
        assertTrue(filter.matches(new JmxNotificationEvent(
                "notif-1", Instant.parse("2026-06-06T01:05:00Z"),
                "memory.threshold", "heap", 2, "Heap high", "")));
        assertFalse(filter.matches(new JmxNotificationEvent(
                "notif-1", Instant.parse("2026-06-06T01:06:00Z"),
                "memory.threshold", "heap", 3, "Heap high", "")));
    }

    @Test
    void rejectsInvertedTimeWindow() {
        assertThrows(IllegalArgumentException.class, () -> new JmxNotificationHistoryFilter(
                null, null,
                Instant.parse("2026-06-06T01:05:00Z"),
                Instant.parse("2026-06-06T01:00:00Z")));
    }
}
