package io.github.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.youngledo.jmcfx.domain.model.JmcAgentPreset;
import io.github.youngledo.jmcfx.domain.model.JmcAgentStatus;
import io.github.youngledo.jmcfx.domain.model.JmcAgentTransform;
import io.github.youngledo.jmcfx.domain.model.JvmConnection;

class JmcAgentManagerServiceTest {

    private static final ObjectName AGENT_NAME = objectName("org.openjdk.jmc.jfr.agent:type=AgentController");
    private static final JvmConnection CONNECTION = new JvmConnection("local", "Local", "", true);

    private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    private final JmcAgentManagerService service = new JmcAgentManagerService(connection -> server);

    @BeforeEach
    void cleanAgentBean() throws Exception {
        unregisterAgentBean();
    }

    @AfterEach
    void unregisterAgentBean() throws Exception {
        if (server.isRegistered(AGENT_NAME)) {
            server.unregisterMBean(AGENT_NAME);
        }
    }

    @Test
    void statusReportsUnavailableWhenAgentMBeanIsMissing() {
        JmcAgentStatus status = service.status(CONNECTION);

        assertFalse(status.available());
        assertEquals("JMC Agent MXBean is not registered on this JVM.", status.message());
        assertEquals("", status.eventProbeXml());
        assertTrue(status.transforms().isEmpty());
    }

    @Test
    void statusReadsEventProbesAndCurrentTransformsWhenAgentIsAvailable() throws Exception {
        AgentController agent = new AgentController();
        agent.eventProbeXml = "<jfragent><events/></jfragent>";
        agent.transforms = new CompositeData[] {
                transform("demo.Probe", "com.example.Service", "run", "()V")
        };
        server.registerMBean(agent, AGENT_NAME);

        JmcAgentStatus status = service.status(CONNECTION);

        assertTrue(status.available());
        assertEquals("JMC Agent is available.", status.message());
        assertEquals("<jfragent><events/></jfragent>", status.eventProbeXml());
        assertEquals(List.of(new JmcAgentTransform("demo.Probe", "com.example.Service", "run", "()V")),
                status.transforms());
    }

    @Test
    void applyConfigurationInvokesDefineEventProbes() throws Exception {
        AgentController agent = new AgentController();
        server.registerMBean(agent, AGENT_NAME);

        service.applyConfiguration(CONNECTION, "<jfragent><events/></jfragent>");

        assertEquals("<jfragent><events/></jfragent>", agent.definedXml);
    }

    @Test
    void presetsExposeBlankAndMethodProbeTemplates() {
        List<JmcAgentPreset> presets = service.presets();

        assertEquals("blank", presets.getFirst().id());
        assertTrue(presets.getFirst().xml().contains("<jfragent>"));
        assertTrue(presets.stream().anyMatch(preset ->
                preset.id().equals("method-probe-template")
                        && preset.xml().contains("<location>WRAP</location>")));
    }

    private static ObjectName objectName(String name) {
        try {
            return new ObjectName(name);
        } catch (Exception exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    private static CompositeData transform(String id, String className, String methodName, String descriptor)
            throws Exception {
        CompositeType methodType = new CompositeType("method", "method",
                new String[] {"name", "signature"},
                new String[] {"name", "signature"},
                new OpenType<?>[] {SimpleType.STRING, SimpleType.STRING});
        CompositeData method = new CompositeDataSupport(methodType,
                new String[] {"name", "signature"},
                new Object[] {methodName, descriptor});
        CompositeType transformType = new CompositeType("transform", "transform",
                new String[] {"id", "className", "method"},
                new String[] {"id", "className", "method"},
                new OpenType<?>[] {SimpleType.STRING, SimpleType.STRING, methodType});
        return new CompositeDataSupport(transformType,
                new String[] {"id", "className", "method"},
                new Object[] {id, className, method});
    }

    public interface AgentControllerMBean {
        String retrieveEventProbes();

        CompositeData[] retrieveCurrentTransforms();

        void defineEventProbes(String xmlDescription);
    }

    public static final class AgentController implements AgentControllerMBean {
        private String eventProbeXml = "";
        private CompositeData[] transforms = new CompositeData[0];
        private String definedXml = "";

        @Override
        public String retrieveEventProbes() {
            return eventProbeXml;
        }

        @Override
        public CompositeData[] retrieveCurrentTransforms() {
            return transforms;
        }

        @Override
        public void defineEventProbes(String xmlDescription) {
            definedXml = xmlDescription;
            eventProbeXml = xmlDescription;
        }
    }
}
