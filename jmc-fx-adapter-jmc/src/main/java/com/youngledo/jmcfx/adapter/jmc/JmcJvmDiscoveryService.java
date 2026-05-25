package com.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.youngledo.jmcfx.domain.model.JvmConnection;
import com.youngledo.jmcfx.domain.service.JvmDiscoveryService;

public class JmcJvmDiscoveryService implements JvmDiscoveryService {

    private final Supplier<List<AttachJvmDescriptor>> descriptorScanner;

    public JmcJvmDiscoveryService() {
        this(() -> scanAttachDescriptors().stream()
                .map(JmcJvmDiscoveryService::inspectDescriptor)
                .map(JmcJvmDiscoveryService::withCurrentProcessJavaVersionFallback)
                .toList());
    }

    JmcJvmDiscoveryService(Supplier<List<AttachJvmDescriptor>> descriptorScanner) {
        this.descriptorScanner = Objects.requireNonNull(descriptorScanner, "descriptorScanner");
    }

    JmcJvmDiscoveryService(Supplier<List<RawAttachJvmDescriptor>> descriptorScanner,
            Function<RawAttachJvmDescriptor, AttachJvmDescriptor> descriptorInspector) {
        Objects.requireNonNull(descriptorScanner, "descriptorScanner");
        Objects.requireNonNull(descriptorInspector, "descriptorInspector");
        this.descriptorScanner = () -> descriptorScanner.get().stream()
                .map(descriptorInspector)
                .map(JmcJvmDiscoveryService::withCurrentProcessJavaVersionFallback)
                .toList();
    }

    @Override
    public List<JvmConnection> discoverLocalJvms() {
        return descriptorScanner.get().stream()
                .sorted(Comparator.comparingLong(AttachJvmDescriptor::pidAsLong)
                        .thenComparing(AttachJvmDescriptor::id))
                .map(descriptor -> JvmConnection.local(descriptor.id(), descriptor.displayName(),
                        descriptor.javaVersion(), descriptor.attachable()))
                .toList();
    }

    private static List<RawAttachJvmDescriptor> scanAttachDescriptors() {
        return VirtualMachine.list().stream()
                .map(descriptor -> new RawAttachJvmDescriptor(descriptor.id(), descriptor.displayName()))
                .toList();
    }

    private static AttachJvmDescriptor inspectDescriptor(RawAttachJvmDescriptor descriptor) {
        VirtualMachine vm = null;
        try {
            vm = VirtualMachine.attach(descriptor.id());
            Properties properties = vm.getSystemProperties();
            return new AttachJvmDescriptor(descriptor.id(), descriptor.displayName(), true,
                    properties.getProperty("java.version", ""));
        } catch (AttachNotSupportedException | IOException | RuntimeException exception) {
            return new AttachJvmDescriptor(descriptor.id(), descriptor.displayName(), false, "");
        } finally {
            if (vm != null) {
                try {
                    vm.detach();
                } catch (IOException exception) {
                    // Discovery should not fail because detach cleanup failed.
                }
            }
        }
    }

    private static AttachJvmDescriptor withCurrentProcessJavaVersionFallback(AttachJvmDescriptor descriptor) {
        if (descriptor.attachable() || !descriptor.javaVersion().isBlank() || !isCurrentProcess(descriptor.id())) {
            return descriptor;
        }
        return new AttachJvmDescriptor(descriptor.id(), descriptor.displayName(), false,
                System.getProperty("java.version", ""));
    }

    private static boolean isCurrentProcess(String pid) {
        try {
            return Long.parseLong(pid) == ProcessHandle.current().pid();
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    record RawAttachJvmDescriptor(String id, String displayName) {
        RawAttachJvmDescriptor {
            id = Objects.requireNonNullElse(id, "");
            displayName = Objects.requireNonNullElse(displayName, "");
        }

        long pidAsLong() {
            try {
                return Long.parseLong(id);
            } catch (NumberFormatException exception) {
                return Long.MAX_VALUE;
            }
        }
    }

    record AttachJvmDescriptor(String id, String displayName, boolean attachable, String javaVersion) {
        AttachJvmDescriptor {
            id = Objects.requireNonNullElse(id, "");
            displayName = Objects.requireNonNullElse(displayName, "");
            javaVersion = Objects.requireNonNullElse(javaVersion, "");
        }

        long pidAsLong() {
            try {
                return Long.parseLong(id);
            } catch (NumberFormatException exception) {
                return Long.MAX_VALUE;
            }
        }

        public String displayName() {
            return displayName.isBlank() ? "Java process " + id : displayName;
        }
    }
}
