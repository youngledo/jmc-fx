package io.github.youngledo.jmcfx.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class DiagnosticCommandModelTest {

    @Test
    void commandInfoNormalizesTextAndCopiesParameters() {
        DiagnosticCommandInfo info = new DiagnosticCommandInfo(null, null, null,
                List.of(new DiagnosticCommandParameter("all", "boolean", null, false)));

        assertEquals("", info.name());
        assertEquals("", info.displayName());
        assertEquals("", info.description());
        assertEquals(1, info.parameters().size());
        assertEquals("", info.parameters().getFirst().description());
        assertThrows(UnsupportedOperationException.class,
                () -> info.parameters().add(new DiagnosticCommandParameter("x", "String", "", false)));
    }

    @Test
    void commandRequestRequiresConnectionAndCopiesArguments() {
        JvmConnection connection = new JvmConnection("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi",
                "localhost:9999", "service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi", false)
                .asConnected("service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi");

        DiagnosticCommandRequest request = new DiagnosticCommandRequest(connection, "threadPrint",
                List.of("-l"));

        assertEquals("threadPrint", request.commandName());
        assertEquals(List.of("-l"), request.arguments());
        assertThrows(UnsupportedOperationException.class, () -> request.arguments().add("-e"));
        assertThrows(NullPointerException.class,
                () -> new DiagnosticCommandRequest(null, "threadPrint", List.of()));
    }

    @Test
    void commandResultNormalizesOutputAndError() {
        DiagnosticCommandResult result = new DiagnosticCommandResult(true, null, null);

        assertEquals("", result.output());
        assertEquals("", result.error());
    }
}
