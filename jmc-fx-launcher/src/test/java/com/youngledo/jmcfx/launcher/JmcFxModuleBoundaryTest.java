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
        assertTrue(descriptor.contains("requires com.youngledo.jmcfx.application;"));
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
    void applicationModuleStaysUseCaseOnly() throws Exception {
        var descriptor = moduleInfo("../jmc-fx-application");

        assertEquals(Set.of("exports com.youngledo.jmcfx.application;"), exportedStatements(descriptor));
        assertTrue(descriptor.contains("requires com.youngledo.jmcfx.domain;"));
        assertFalse(descriptor.contains("com.youngledo.jmcfx.adapter"), "application must not require adapters");
        assertFalse(descriptor.contains("com.youngledo.jmcfx.ui"), "application must not require UI");
        assertFalse(descriptor.contains("com.youngledo.jmcfx.launcher"), "application must not require launcher");
        assertFalse(descriptor.contains("javafx."), "application must not depend on JavaFX");
        assertFalse(descriptor.contains(OPENJDK_JMC_MODULE_PREFIX), "application must not require JMC modules");
        assertFalse(descriptor.contains("org.jolokia"), "application must not require Jolokia modules");
    }

    @Test
    void adapterModuleOwnsJmcAndPreferencesAdapters() throws Exception {
        var descriptor = moduleInfo("../jmc-fx-adapter");

        assertEquals(
                Set.of(
                        "exports com.youngledo.jmcfx.adapter.jmc;",
                        "exports com.youngledo.jmcfx.adapter.preferences;"),
                exportedStatements(descriptor));
        assertTrue(descriptor.contains("requires com.youngledo.jmcfx.domain;"));
        assertTrue(descriptor.contains("requires java.prefs;"));
        assertTrue(descriptor.contains("requires " + OPENJDK_JMC_MODULE_PREFIX + ".flightrecorder;"));
        assertFalse(descriptor.contains("com.youngledo.jmcfx.application"),
                "adapter must not require the application use-case layer");
        assertFalse(descriptor.contains("com.youngledo.jmcfx.ui"), "adapter must not require UI");
        assertFalse(descriptor.contains("com.youngledo.jmcfx.launcher"), "adapter must not require launcher");
        assertFalse(descriptor.contains("javafx."), "adapter must not depend on JavaFX UI modules");
    }

    @Test
    void launcherModuleAssemblesAdaptersWithoutOpeningItsPackageGlobally() throws Exception {
        var descriptor = moduleInfo(".");

        assertTrue(descriptor.contains("requires com.youngledo.jmcfx.adapter;"));
        assertTrue(descriptor.contains("requires com.youngledo.jmcfx.ui;"));
        assertTrue(descriptor.contains("exports com.youngledo.jmcfx.launcher to javafx.graphics;"));
        assertFalse(descriptor.contains("exports com.youngledo.jmcfx.launcher;"),
                "launcher entry package should only be exported to the JavaFX launcher");
        assertFalse(descriptor.contains("opens com.youngledo.jmcfx.launcher"),
                "launcher entry package does not need reflective opens");
    }

    @Test
    void javafxLauncherDelegatesConcreteJmcAdapterAssembly() throws Exception {
        var applicationSource = Files.readString(
                Path.of("src/main/java/com/youngledo/jmcfx/launcher/JmcFxLauncher.java"));

        assertFalse(applicationSource.contains("import com.youngledo.jmcfx.adapter.jmc.Jmc"),
                "JavaFX entry point should delegate concrete JMC adapter construction");
        assertTrue(applicationSource.contains("new JmcFxLauncherServicesFactory()"));
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
    void uiTestSupportFakesStayInUiTestSources() throws Exception {
        var rootPom = Files.readString(Path.of("../pom.xml"));
        var uiPom = Files.readString(Path.of("../jmc-fx-ui/pom.xml"));

        assertFalse(rootPom.contains("<subproject>jmc-fx-test-support</subproject>"),
                "test support fakes should not be a separate Maven subproject");
        assertFalse(uiPom.contains("<artifactId>jmc-fx-test-support</artifactId>"),
                "UI tests should use local test-source fakes instead of a test-support module");
        assertFalse(uiPom.contains("com.youngledo.jmcfx.testsupport"),
                "UI tests should not require add-reads for a test-support module");
        assertTrue(Files.isRegularFile(Path.of(
                "../jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/testsupport/FakeEventQueryService.java")),
                "UI test fakes should live with the UI tests that consume them");
    }

    @Test
    void javafxRunUsesModuleLaunchWhileJpackageUsesClasspathJlink() throws Exception {
        var rootPom = Files.readString(Path.of("../pom.xml"));
        var appPom = Files.readString(Path.of("pom.xml"));

        assertTrue(rootPom.contains("<mainClass>com.youngledo.jmcfx.launcher/com.youngledo.jmcfx.launcher.JmcFxLauncher</mainClass>"));
        assertTrue(appPom.contains("<mainClass>com.youngledo.jmcfx.launcher/com.youngledo.jmcfx.launcher.JmcFxLauncher</mainClass>"));
        assertTrue(appPom.contains("<id>jpackage-classpath-jlink</id>"));
        assertTrue(appPom.contains("<mainJar>${project.build.finalName}.jar</mainJar>"));
        assertTrue(appPom.contains("<mainClass>com.youngledo.jmcfx.launcher.JmcFxLauncher</mainClass>"));
        assertFalse(appPom.contains("<option>--module</option>"));
        assertFalse(appPom.contains("<option>com.youngledo.jmcfx.launcher/com.youngledo.jmcfx.launcher.JmcFxLauncher</option>"));
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
