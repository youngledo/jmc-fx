package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.JvmConnection;

class JmcFlightRecordingServiceTest {

    @Test
    void recordingControlIsDisabledUntilJmcApiIsVerified() {
        JmcFlightRecordingService service = new JmcFlightRecordingService();

        assertFalse(service.isRecordingControlAvailable(
                new JvmConnection("jvm", "Local JVM", "service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi", false)));
    }
}
