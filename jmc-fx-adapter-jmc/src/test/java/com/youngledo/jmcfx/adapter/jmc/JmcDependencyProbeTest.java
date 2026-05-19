package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;

class JmcDependencyProbeTest {

    @Test
    void canLoadJmcFlightRecorderApi() {
        assertNotNull(JfrLoaderToolkit.class);
    }
}
