package io.github.youngledo.jmcfx.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SocketIOGroupingTest {

    @Test
    void hasThreeGroupingStrategies() {
        assertEquals(3, SocketIOGrouping.values().length);
        assertNotNull(SocketIOGrouping.BY_HOST);
        assertNotNull(SocketIOGrouping.BY_PORT);
        assertNotNull(SocketIOGrouping.BY_HOST_AND_PORT);
    }
}
