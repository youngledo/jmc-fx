package io.github.youngledo.jmcfx.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LockGroupingTest {

    @Test
    void hasThreeGroupingStrategies() {
        assertEquals(3, LockGrouping.values().length);
        assertNotNull(LockGrouping.BY_CLASS);
        assertNotNull(LockGrouping.BY_ADDRESS);
        assertNotNull(LockGrouping.BY_THREAD);
    }
}
