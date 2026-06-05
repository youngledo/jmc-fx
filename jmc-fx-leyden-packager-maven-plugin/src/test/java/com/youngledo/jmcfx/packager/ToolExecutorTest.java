package com.youngledo.jmcfx.packager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolExecutorTest {

    @Test
    void buildsJlinkCommand() {
        var executor = new ToolExecutor(Path.of("/jdk"), new RecordingRunner());

        var command = executor.jlinkCommand(Path.of("input"), Path.of("runtime"),
                List.of("java.desktop", "jdk.jfr"));

        assertEquals(List.of("/jdk/bin/jlink", "--no-header-files", "--no-man-pages", "--strip-debug",
                "--output", "runtime", "--module-path", "input", "--add-modules", "java.desktop,jdk.jfr"),
                command);
    }

    @Test
    void buildsTrainingAppImageJpackageCommand() {
        var paths = PackagingPaths.derive(Path.of("target"), "JMC FX", "1.0.0", "jmcfx-startup.aot",
                OperatingSystem.MACOS);
        var executor = new ToolExecutor(Path.of("/jdk"), new RecordingRunner());

        var command = executor.appImageCommand(paths, OperatingSystem.MACOS, "JMC FX", "1.0.0", "Youngledo",
                "Description", "jmc-fx-launcher.jar", "com.acme.Main",
                List.of("--enable-native-access=javafx.graphics"), "jmcfx.leyden.training",
                "com.acme.app", "JMC FX", "jmc-fx", "Development", true, true, "JMC FX");

        assertEquals("app-image", command.get(command.indexOf("--type") + 1));
        assertTrue(command.contains("-Djmcfx.leyden.training=true"));
        assertTrue(command.contains("-XX:AOTCacheOutput=$APPDIR/jmcfx-startup.aot"));
    }

    @Test
    void appImageCommandAddsOnlyMacOsPackageOptionsOnMacOs() {
        var paths = PackagingPaths.derive(Path.of("target"), "JMC FX", "1.0.0", "jmcfx-startup.aot",
                OperatingSystem.MACOS);
        var executor = new ToolExecutor(Path.of("/jdk"), new RecordingRunner());

        var command = executor.appImageCommand(paths, OperatingSystem.MACOS, "JMC FX", "1.0.0", "Youngledo",
                "Description", "jmc-fx-launcher.jar", "com.acme.Main", List.of(), "jmcfx.leyden.training",
                "com.acme.app", "JMC FX", "jmc-fx", "Development", true, true, "JMC FX");

        assertTrue(command.contains("--mac-package-identifier"));
        assertTrue(command.contains("--mac-package-name"));
        assertFalse(command.contains("--linux-package-name"));
        assertFalse(command.contains("--linux-app-category"));
        assertFalse(command.contains("--win-menu"));
        assertFalse(command.contains("--win-shortcut"));
        assertFalse(command.contains("--win-menu-group"));
    }

    @Test
    void appImageCommandAddsOnlyLinuxPackageOptionsOnLinux() {
        var paths = PackagingPaths.derive(Path.of("target"), "JMC FX", "1.0.0", "jmcfx-startup.aot",
                OperatingSystem.LINUX);
        var executor = new ToolExecutor(Path.of("/jdk"), new RecordingRunner());

        var command = executor.appImageCommand(paths, OperatingSystem.LINUX, "JMC FX", "1.0.0", "Youngledo",
                "Description", "jmc-fx-launcher.jar", "com.acme.Main", List.of(), "jmcfx.leyden.training",
                "com.acme.app", "JMC FX", "jmc-fx", "Development", true, true, "JMC FX");

        assertFalse(command.contains("--mac-package-identifier"));
        assertFalse(command.contains("--mac-package-name"));
        assertTrue(command.contains("--linux-package-name"));
        assertTrue(command.contains("--linux-app-category"));
        assertFalse(command.contains("--win-menu"));
        assertFalse(command.contains("--win-shortcut"));
        assertFalse(command.contains("--win-menu-group"));
    }

    @Test
    void appImageCommandAddsOnlyWindowsPackageOptionsOnWindows() {
        var paths = PackagingPaths.derive(Path.of("target"), "JMC FX", "1.0.0", "jmcfx-startup.aot",
                OperatingSystem.WINDOWS);
        var executor = new ToolExecutor(Path.of("/jdk"), new RecordingRunner());

        var command = executor.appImageCommand(paths, OperatingSystem.WINDOWS, "JMC FX", "1.0.0", "Youngledo",
                "Description", "jmc-fx-launcher.jar", "com.acme.Main", List.of(), "jmcfx.leyden.training",
                "com.acme.app", "JMC FX", "jmc-fx", "Development", true, true, "JMC FX");

        assertFalse(command.contains("--mac-package-identifier"));
        assertFalse(command.contains("--mac-package-name"));
        assertFalse(command.contains("--linux-package-name"));
        assertFalse(command.contains("--linux-app-category"));
        assertTrue(command.contains("--win-menu"));
        assertTrue(command.contains("--win-shortcut"));
        assertTrue(command.contains("--win-menu-group"));
    }

    @Test
    void buildsInstallerCommandFromAppImage() {
        var paths = PackagingPaths.derive(Path.of("target"), "JMC FX", "1.0.0", "jmcfx-startup.aot",
                OperatingSystem.MACOS);
        var executor = new ToolExecutor(Path.of("/jdk"), new RecordingRunner());

        var command = executor.installerCommand(paths, OperatingSystem.MACOS, "JMC FX",
                OperatingSystem.MACOS.jpackageType(), "1.0.0", "Youngledo", "Description",
                "com.acme.app", "JMC FX", "jmc-fx", "Development",
                true, true, "JMC FX");

        assertEquals("dmg", command.get(command.indexOf("--type") + 1));
        assertEquals(paths.appImage().toString(), command.get(command.indexOf("--app-image") + 1));
    }

    private static final class RecordingRunner implements CommandRunner {
        @Override
        public void run(List<String> command) {
        }
    }
}
