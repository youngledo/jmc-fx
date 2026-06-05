package com.youngledo.jmcfx.packager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LeydenPackageMojoTest {

    @Test
    void keepsDefaultAotCacheNameStable() {
        assertEquals("jmcfx-startup.aot", LeydenPackageMojo.defaultAotCacheName());
    }

    @Test
    void resolvesDefaultJavaHomeFromSystemProperty() {
        assertEquals(Path.of(System.getProperty("java.home")), LeydenPackageMojo.defaultJavaHome());
    }

    @Test
    void declaresPackageGoalInPackagePhase() throws Exception {
        var descriptor = Files.readString(Path.of("target/classes/META-INF/maven/plugin.xml"));

        assertTrue(descriptor.contains("<goal>leyden-package</goal>"));
        assertTrue(descriptor.contains("<phase>package</phase>"));
    }
}
