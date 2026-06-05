package com.youngledo.jmcfx.packager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LeydenPackageMojoTest {

    @Test
    void keepsDefaultAotCacheNameStable() {
        assertEquals("startup.aot", LeydenPackageMojo.defaultAotCacheName());
    }

    @Test
    void resolvesDefaultJavaHomeFromSystemProperty() {
        assertEquals(Path.of(System.getProperty("java.home")), LeydenPackageMojo.defaultJavaHome());
    }

    @Test
    void declaresPackageGoalInPackagePhase() throws Exception {
        var descriptor = Files.readString(Path.of("target/classes/META-INF/maven/plugin.xml"));

        assertTrue(descriptor.contains("<goal>leyden</goal>"));
        assertTrue(descriptor.contains("<phase>package</phase>"));
    }

    @Test
    void exposesGenericLeydenWorkDirectories() throws Exception {
        var descriptor = Files.readString(Path.of("target/classes/META-INF/maven/plugin.xml"));

        assertTrue(descriptor.contains("<name>inputDirectory</name>"));
        assertTrue(descriptor.contains("<name>runtimeDirectory</name>"));
        assertTrue(descriptor.contains("<name>appImageDirectory</name>"));
        assertTrue(descriptor.contains("<name>packageDirectory</name>"));
        assertTrue(descriptor.contains("${project.build.directory}/jpackage-input"));
        assertTrue(descriptor.contains("${project.build.directory}/jlink-runtime"));
        assertTrue(descriptor.contains("${project.build.directory}/jpackage-app-image"));
        assertTrue(descriptor.contains("${project.build.directory}/jpackage"));
    }

    @Test
    void exposesLeydenJlinkConfiguration() throws Exception {
        var descriptor = Files.readString(Path.of("target/classes/META-INF/maven/plugin.xml"));

        assertTrue(descriptor.contains("<name>runtimeModules</name>"));
        assertTrue(descriptor.contains("<name>modulePath</name>"));
        assertTrue(descriptor.contains("<name>includeInputDirectoryOnModulePath</name>"));
        assertTrue(descriptor.contains("<name>noHeaderFiles</name>"));
        assertTrue(descriptor.contains("<name>noManPages</name>"));
        assertTrue(descriptor.contains("<name>stripDebug</name>"));
        assertTrue(descriptor.contains("<name>compress</name>"));
        assertTrue(descriptor.contains("<name>bindServices</name>"));
        assertTrue(descriptor.contains("<name>jlinkOptions</name>"));
    }

    @Test
    void exposesModuleLaunchAndJpackageConfiguration() throws Exception {
        var descriptor = Files.readString(Path.of("target/classes/META-INF/maven/plugin.xml"));

        assertTrue(descriptor.contains("<name>module</name>"));
        assertTrue(descriptor.contains("<name>addLaunchers</name>"));
        assertTrue(descriptor.contains("<name>arguments</name>"));
        assertTrue(descriptor.contains("<name>appContent</name>"));
        assertTrue(descriptor.contains("<name>resourceDir</name>"));
        assertTrue(descriptor.contains("<name>licenseFile</name>"));
        assertTrue(descriptor.contains("<name>fileAssociations</name>"));
        assertTrue(descriptor.contains("<name>macSign</name>"));
        assertTrue(descriptor.contains("<name>linuxPackageDeps</name>"));
        assertTrue(descriptor.contains("<name>winUpgradeUuid</name>"));
    }
}
