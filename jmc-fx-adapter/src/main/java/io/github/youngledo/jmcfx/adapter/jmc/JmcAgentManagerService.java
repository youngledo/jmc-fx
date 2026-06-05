package io.github.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import io.github.youngledo.jmcfx.domain.model.JmcAgentPreset;
import io.github.youngledo.jmcfx.domain.model.JmcAgentStatus;
import io.github.youngledo.jmcfx.domain.model.JmcAgentTransform;
import io.github.youngledo.jmcfx.domain.model.JvmConnection;
import io.github.youngledo.jmcfx.domain.service.JmcAgentService;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;

public class JmcAgentManagerService implements JmcAgentService {

    static final String AGENT_OBJECT_NAME = "org.openjdk.jmc.jfr.agent:type=AgentController";
    private static final String RETRIEVE_EVENT_PROBES = "retrieveEventProbes";
    private static final String RETRIEVE_CURRENT_TRANSFORMS = "retrieveCurrentTransforms";
    private static final String DEFINE_EVENT_PROBES = "defineEventProbes";
    private static final String BLANK_XML = """
            <jfragent>
              <events/>
            </jfragent>
            """;
    private static final String METHOD_PROBE_XML = """
            <jfragent>
              <config>
                <classprefix>io.github.youngledo.jmcfx.agent</classprefix>
                <allowtostring>false</allowtostring>
                <allowconverter>false</allowconverter>
              </config>
              <events>
                <event id="io.github.youngledo.jmcfx.agent.sample">
                  <label>Sample Method Probe</label>
                  <class>com.example.Service</class>
                  <method>
                    <name>run</name>
                    <descriptor>()V</descriptor>
                  </method>
                  <description>Replace class, method, and descriptor before applying.</description>
                  <path>JMC FX/Agent</path>
                  <stacktrace>true</stacktrace>
                  <location>WRAP</location>
                </event>
              </events>
            </jfragent>
            """;

    private final JmxConnectionAccessor connectionAccessor;

    public JmcAgentManagerService(JmcJmxConnectionService connectionService) {
        this((JmxConnectionAccessor) connectionService);
    }

    public JmcAgentManagerService(JmxConnectionAccessor connectionAccessor) {
        this.connectionAccessor = Objects.requireNonNull(connectionAccessor, "connectionAccessor");
    }

    @Override
    public JmcAgentStatus status(JvmConnection connection) {
        try {
            MBeanServerConnection server = server(connection);
            ObjectName agentName = objectName(AGENT_OBJECT_NAME);
            if (!server.isRegistered(agentName)) {
                return JmcAgentStatus.unavailable("JMC Agent MXBean is not registered on this JVM.");
            }
            String probes = Objects.toString(invoke(server, agentName, RETRIEVE_EVENT_PROBES), "");
            List<JmcAgentTransform> transforms = transforms(invoke(server, agentName, RETRIEVE_CURRENT_TRANSFORMS));
            return new JmcAgentStatus(true, "JMC Agent is available.", probes, transforms);
        } catch (IOException | JMException | RuntimeException exception) {
            throw new JmcFxException("Unable to inspect JMC Agent: " + message(exception), exception);
        }
    }

    @Override
    public List<JmcAgentPreset> presets() {
        return List.of(
                new JmcAgentPreset("blank", "Blank configuration", "Remove all dynamic JMC Agent event probes.",
                        BLANK_XML),
                new JmcAgentPreset("method-probe-template", "Method probe template",
                        "Template for wrapping one method with a custom JFR event.", METHOD_PROBE_XML));
    }

    @Override
    public void applyConfiguration(JvmConnection connection, String xmlDescription) {
        try {
            MBeanServerConnection server = server(connection);
            ObjectName agentName = objectName(AGENT_OBJECT_NAME);
            if (!server.isRegistered(agentName)) {
                throw new JmcFxException("JMC Agent MXBean is not registered on this JVM.");
            }
            server.invoke(agentName, DEFINE_EVENT_PROBES,
                    new Object[] {Objects.requireNonNullElse(xmlDescription, "")},
                    new String[] {String.class.getName()});
        } catch (JmcFxException exception) {
            throw exception;
        } catch (IOException | JMException | RuntimeException exception) {
            throw new JmcFxException("Unable to apply JMC Agent configuration: " + message(exception), exception);
        }
    }

    private Object invoke(MBeanServerConnection server, ObjectName agentName, String operation)
            throws IOException, JMException {
        try {
            return server.invoke(agentName, operation, new Object[0], new String[0]);
        } catch (InstanceNotFoundException exception) {
            throw new JmcFxException("JMC Agent MXBean is not registered on this JVM.", exception);
        }
    }

    private List<JmcAgentTransform> transforms(Object result) {
        if (!(result instanceof CompositeData[] rows)) {
            return List.of();
        }
        return java.util.Arrays.stream(rows)
                .map(this::transform)
                .toList();
    }

    private JmcAgentTransform transform(CompositeData row) {
        CompositeData method = value(row, "method", CompositeData.class);
        return new JmcAgentTransform(
                text(row, "id"),
                text(row, "className"),
                method == null ? "" : text(method, "name"),
                method == null ? "" : text(method, "signature"));
    }

    private String text(CompositeData data, String key) {
        Object value = data == null ? null : data.get(key);
        return value == null ? "" : value.toString();
    }

    private <T> T value(CompositeData data, String key, Class<T> type) {
        Object value = data == null ? null : data.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    private MBeanServerConnection server(JvmConnection connection) {
        try {
            return connectionAccessor.mBeanServerConnection(connection);
        } catch (IOException exception) {
            throw new JmcFxException("No live JVM session for connection: "
                    + (connection == null ? "" : connection.id()), exception);
        }
    }

    private static ObjectName objectName(String name) {
        try {
            return new ObjectName(name);
        } catch (javax.management.MalformedObjectNameException exception) {
            throw new IllegalArgumentException("Invalid ObjectName: " + name, exception);
        }
    }

    private static String message(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
