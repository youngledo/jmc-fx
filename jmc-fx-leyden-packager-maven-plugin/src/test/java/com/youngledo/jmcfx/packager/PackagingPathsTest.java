package com.youngledo.jmcfx.packager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PackagingPathsTest {

    @Test
    void derivesMacOsJpackageLayout() {
        var paths = PackagingPaths.derive(Path.of("target"), "JMC FX", "1.0.0", "jmcfx-startup.aot",
                OperatingSystem.MACOS);

        assertEquals(Path.of("target", "jpackage-leyden-input"), paths.inputDirectory());
        assertEquals(Path.of("target", "jpackage-leyden-runtime"), paths.runtimeDirectory());
        assertEquals(Path.of("target", "jpackage-leyden-app-image", "JMC FX.app"), paths.appImage());
        assertEquals(Path.of("target", "jpackage-leyden-app-image", "JMC FX.app", "Contents", "MacOS", "JMC FX"),
                paths.appExecutable());
        assertEquals(Path.of("target", "jpackage-leyden-app-image", "JMC FX.app", "Contents", "app", "JMC FX.cfg"),
                paths.appConfig());
        assertEquals(Path.of("target", "jpackage-leyden-app-image", "JMC FX.app", "Contents", "app",
                "jmcfx-startup.aot"), paths.aotCache());
        assertEquals(Path.of("target", "jpackage-leyden", "JMC FX-1.0.0.dmg"), paths.installer());
    }

    @Test
    void derivesLinuxJpackageLayout() {
        var paths = PackagingPaths.derive(Path.of("target"), "JMC FX", "1.0.0", "jmcfx-startup.aot",
                OperatingSystem.LINUX);

        assertEquals(Path.of("target", "jpackage-leyden-app-image", "JMC FX"), paths.appImage());
        assertEquals(Path.of("target", "jpackage-leyden-app-image", "JMC FX", "bin", "JMC FX"),
                paths.appExecutable());
        assertEquals(Path.of("target", "jpackage-leyden-app-image", "JMC FX", "lib", "app", "JMC FX.cfg"),
                paths.appConfig());
        assertEquals(Path.of("target", "jpackage-leyden", "JMC FX-1.0.0.deb"), paths.installer());
    }

    @Test
    void derivesWindowsJpackageLayout() {
        var paths = PackagingPaths.derive(Path.of("target"), "JMC FX", "1.0.0", "jmcfx-startup.aot",
                OperatingSystem.WINDOWS);

        assertEquals(Path.of("target", "jpackage-leyden-app-image", "JMC FX"), paths.appImage());
        assertEquals(Path.of("target", "jpackage-leyden-app-image", "JMC FX", "JMC FX.exe"),
                paths.appExecutable());
        assertEquals(Path.of("target", "jpackage-leyden-app-image", "JMC FX", "app", "JMC FX.cfg"),
                paths.appConfig());
        assertEquals(Path.of("target", "jpackage-leyden", "JMC FX-1.0.0.msi"), paths.installer());
    }
}
