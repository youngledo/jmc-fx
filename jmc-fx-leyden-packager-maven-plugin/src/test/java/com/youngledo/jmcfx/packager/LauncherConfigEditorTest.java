package com.youngledo.jmcfx.packager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LauncherConfigEditorTest {

    @TempDir
    Path tempDir;

    @Test
    void rewritesTrainingOptionsToRuntimeAotCache() throws Exception {
        var config = tempDir.resolve("JMC FX.cfg");
        Files.writeString(config, """
                [JavaOptions]
                java-options=--enable-native-access=javafx.graphics
                java-options=-Djmcfx.leyden.training=true
                java-options=-XX:AOTCacheOutput=$APPDIR/jmcfx-startup.aot
                """);

        new LauncherConfigEditor().rewriteForRuntime(config, "jmcfx.leyden.training", "jmcfx-startup.aot");

        assertEquals("""
                [JavaOptions]
                java-options=--enable-native-access=javafx.graphics
                java-options=-XX:AOTCache=$APPDIR/jmcfx-startup.aot
                """, Files.readString(config));
    }

    @Test
    void failsWhenAotOutputOptionIsMissing() throws Exception {
        var config = tempDir.resolve("JMC FX.cfg");
        Files.writeString(config, """
                [JavaOptions]
                java-options=-Djmcfx.leyden.training=true
                """);

        var error = assertThrows(IllegalStateException.class,
                () -> new LauncherConfigEditor().rewriteForRuntime(config, "jmcfx.leyden.training",
                        "jmcfx-startup.aot"));

        assertEquals("Missing Leyden AOT cache output option in " + config, error.getMessage());
    }
}
