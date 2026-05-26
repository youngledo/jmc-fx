package com.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class FlightRecordingInfoTest {

    @Test
    void recordingInfoNormalizesStringsAndDefaultsState() {
        FlightRecordingInfo info = new FlightRecordingInfo(7, null, null, 12_345, 2048);

        assertEquals(7, info.id());
        assertEquals("", info.name());
        assertEquals(FlightRecordingState.UNKNOWN, info.state());
        assertEquals(12_345, info.durationMillis());
        assertEquals(2048, info.sizeBytes());
    }

    @Test
    void predefinedTemplatesAreImmutable() {
        List<FlightRecordingTemplate> templates = FlightRecordingTemplate.predefined();

        assertEquals("profile", templates.getFirst().name());
        assertThrows(UnsupportedOperationException.class,
                () -> templates.add(new FlightRecordingTemplate("x", "X", "")));
    }

    @Test
    void stopRequestRequiresDestinationFile() {
        assertThrows(NullPointerException.class,
                () -> new FlightRecordingStopRequest(JvmConnection.local("42", "demo.Main", "26", true),
                        1, null));
    }

    @Test
    void startRequestNormalizesNameAndDefaultsTemplate() {
        FlightRecordingStartRequest request = new FlightRecordingStartRequest(
                JvmConnection.local("42", "demo.Main", "26", true), null, null);

        assertEquals("", request.name());
        assertEquals("profile", request.template().name());
    }

    @Test
    void stopRequestKeepsDestinationPath() {
        Path destination = Path.of("target/live-capture.jfr");
        FlightRecordingStopRequest request = new FlightRecordingStopRequest(
                JvmConnection.local("42", "demo.Main", "26", true), 11, destination);

        assertEquals(11, request.recordingId());
        assertEquals(destination, request.destinationFile());
    }
}
