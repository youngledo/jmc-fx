package com.youngledo.jmcfx.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class JmcFxModuleBoundaryTest {

    private static final String OPENJDK_JMC_MODULE_PREFIX = "org.openjdk." + "jmc";

    @Test
    void domainModuleStaysUiAndAdapterNeutral() throws Exception {
        var descriptor = moduleInfo("../jmc-fx-domain");

        assertEquals(
                Set.of(
                        "exports com.youngledo.jmcfx.domain.model;",
                        "exports com.youngledo.jmcfx.domain.service;"),
                exportedStatements(descriptor));
        assertFalse(descriptor.contains("javafx."), "domain must not depend on JavaFX");
        assertFalse(descriptor.contains(OPENJDK_JMC_MODULE_PREFIX), "domain must not depend on JMC modules");
        assertFalse(descriptor.contains("com.youngledo.jmcfx.adapter"), "domain must not depend on adapters");
    }

    @Test
    void uiModuleDependsOnlyOnDomainAndUiLibraries() throws Exception {
        var descriptor = moduleInfo("../jmc-fx-ui");

        assertTrue(descriptor.contains("requires transitive com.youngledo.jmcfx.domain;"));
        assertTrue(descriptor.contains("requires com.youngledo.jmcfx.flamegraph;"));
        assertEquals(
                Set.of(
                        "exports com.youngledo.jmcfx.ui.i18n;",
                        "exports com.youngledo.jmcfx.ui.preferences;",
                        "exports com.youngledo.jmcfx.ui.shell;"),
                exportedStatements(descriptor));
        assertFalse(descriptor.contains("com.youngledo.jmcfx.adapter"), "UI must not require adapter modules");
        assertFalse(descriptor.contains(OPENJDK_JMC_MODULE_PREFIX), "UI must not require JMC modules");
    }

    @Test
    void launcherModuleAssemblesAdaptersWithoutOpeningItsPackageGlobally() throws Exception {
        var descriptor = moduleInfo(".");

        assertTrue(descriptor.contains("requires com.youngledo.jmcfx.adapter.jmc;"));
        assertTrue(descriptor.contains("requires com.youngledo.jmcfx.ui;"));
        assertTrue(descriptor.contains("exports com.youngledo.jmcfx.launcher to javafx.graphics;"));
        assertFalse(descriptor.contains("exports com.youngledo.jmcfx.launcher;"),
                "launcher entry package should only be exported to the JavaFX launcher");
        assertFalse(descriptor.contains("opens com.youngledo.jmcfx.launcher"),
                "launcher entry package does not need reflective opens");
    }

    @Test
    void flameGraphModuleRemainsReusableAndIndependent() throws Exception {
        var descriptor = moduleInfo("../jmc-fx-flamegraph");

        assertEquals(Set.of("exports com.youngledo.jmcfx.flamegraph;"), exportedStatements(descriptor));
        assertFalse(descriptor.contains("com.youngledo.jmcfx.ui"), "flamegraph module must not depend on app UI");
        assertFalse(descriptor.contains("com.youngledo.jmcfx.adapter"), "flamegraph module must not depend on adapters");
        assertFalse(descriptor.contains(OPENJDK_JMC_MODULE_PREFIX), "flamegraph module must not depend on JMC modules");
    }

    @Test
    void javafxRunUsesModuleLaunchWhileJpackageUsesClasspathJlink() throws Exception {
        var rootPom = Files.readString(Path.of("../pom.xml"));
        var appPom = Files.readString(Path.of("pom.xml"));

        assertTrue(rootPom.contains("<mainClass>com.youngledo.jmcfx.launcher/com.youngledo.jmcfx.launcher.JmcFxApplication</mainClass>"));
        assertTrue(appPom.contains("<mainClass>com.youngledo.jmcfx.launcher/com.youngledo.jmcfx.launcher.JmcFxApplication</mainClass>"));
        assertTrue(appPom.contains("<id>jpackage-classpath-jlink</id>"));
        assertTrue(appPom.contains("<mainJar>${project.build.finalName}.jar</mainJar>"));
        assertTrue(appPom.contains("<mainClass>com.youngledo.jmcfx.launcher.JmcFxApplication</mainClass>"));
        assertFalse(appPom.contains("<option>--module</option>"));
        assertFalse(appPom.contains("<option>com.youngledo.jmcfx.launcher/com.youngledo.jmcfx.launcher.JmcFxApplication</option>"));
        assertFalse(rootPom.contains("<modules>"));
        assertFalse(rootPom.contains("<module>"));
        assertFalse(appPom.contains("<modules>"));
        assertFalse(appPom.contains("<module>"));
    }

    private static String moduleInfo(String subproject) throws Exception {
        return Files.readString(Path.of(subproject, "src/main/java/module-info.java"));
    }

    private static Set<String> exportedStatements(String descriptor) {
        return descriptor.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("exports "))
                .collect(Collectors.toUnmodifiableSet());
    }
}
