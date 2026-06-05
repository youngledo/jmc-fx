package io.github.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EventFieldDescriptorTest {

    @Test
    void exposesRecommendedFilterableFieldMetadata() {
        EventFieldDescriptor descriptor = new EventFieldDescriptor("duration", "Duration", "Event duration",
                EventValueType.DURATION, "ns", true, true, true);

        assertEquals("duration", descriptor.id());
        assertEquals(EventValueType.DURATION, descriptor.valueType());
        assertTrue(descriptor.recommendedColumn());
        assertTrue(descriptor.filterable());
    }

    @Test
    void rejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
                () -> new EventFieldDescriptor("", "Duration", "", EventValueType.DURATION, "ns", true, true, true));
    }
}
