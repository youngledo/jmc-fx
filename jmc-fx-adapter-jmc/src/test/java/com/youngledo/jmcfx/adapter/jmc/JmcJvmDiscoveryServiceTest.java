package com.youngledo.jmcfx.adapter.jmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.youngledo.jmcfx.domain.model.JvmConnectionSource;
import com.youngledo.jmcfx.domain.model.JvmConnectionState;

class JmcJvmDiscoveryServiceTest {

    @Test
    void mapsAttachableDescriptorsToLocalJvmConnections() {
        JmcJvmDiscoveryService service = new JmcJvmDiscoveryService(() -> List.of(
                new JmcJvmDiscoveryService.AttachJvmDescriptor("456", "com.intellij.idea.Main", true, "21.0.8"),
                new JmcJvmDiscoveryService.AttachJvmDescriptor("123", "demo.Main --port 8080", true, "26.0.1")));

        var discovered = service.discoverLocalJvms();

        assertEquals(2, discovered.size());
        assertEquals("123", discovered.getFirst().id());
        assertEquals("123", discovered.getFirst().pid());
        assertEquals("demo.Main --port 8080", discovered.getFirst().displayName());
        assertEquals("26.0.1", discovered.getFirst().javaVersion());
        assertFalse(discovered.getFirst().connected());
        assertTrue(discovered.getFirst().attachable());
        assertEquals(JvmConnectionSource.LOCAL, discovered.getFirst().source());
        assertEquals(JvmConnectionState.ATTACHABLE, discovered.getFirst().state());
        assertEquals("456", discovered.get(1).id());
        assertEquals("com.intellij.idea.Main", discovered.get(1).displayName());
    }

    @Test
    void marksDescriptorUnavailableWhenAttachInspectionFails() {
        JmcJvmDiscoveryService service = new JmcJvmDiscoveryService(() -> List.of(
                new JmcJvmDiscoveryService.AttachJvmDescriptor("789", "blocked.Main", false, "")));

        var discovered = service.discoverLocalJvms();

        assertEquals("789", discovered.getFirst().pid());
        assertFalse(discovered.getFirst().attachable());
        assertEquals(JvmConnectionState.UNAVAILABLE, discovered.getFirst().state());
    }

    @Test
    void currentProcessKeepsJavaVersionWhenAttachInspectionFails() {
        long currentPid = ProcessHandle.current().pid();
        JmcJvmDiscoveryService service = new JmcJvmDiscoveryService(
                () -> List.of(new JmcJvmDiscoveryService.RawAttachJvmDescriptor(
                        Long.toString(currentPid), "com.youngledo.jmcfx.app.JmcFxApplication")),
                descriptor -> new JmcJvmDiscoveryService.AttachJvmDescriptor(
                        descriptor.id(), descriptor.displayName(), false, ""));

        var discovered = service.discoverLocalJvms();

        assertEquals(Long.toString(currentPid), discovered.getFirst().pid());
        assertEquals(System.getProperty("java.version", ""), discovered.getFirst().javaVersion());
        assertFalse(discovered.getFirst().attachable());
        assertEquals(JvmConnectionState.UNAVAILABLE, discovered.getFirst().state());
    }

    @Test
    void usesPidWhenAttachDescriptorDisplayNameIsBlank() {
        JmcJvmDiscoveryService service = new JmcJvmDiscoveryService(() -> List.of(
                new JmcJvmDiscoveryService.AttachJvmDescriptor("789", "", true, "")));

        var discovered = service.discoverLocalJvms();

        assertEquals("Java process 789", discovered.getFirst().displayName());
    }

    @Test
    void returnsEmptyListWhenNoAttachDescriptorsAreFound() {
        JmcJvmDiscoveryService service = new JmcJvmDiscoveryService(List::of);

        assertTrue(service.discoverLocalJvms().isEmpty());
    }
}
