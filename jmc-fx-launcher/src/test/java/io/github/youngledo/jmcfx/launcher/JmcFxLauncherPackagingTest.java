package io.github.youngledo.jmcfx.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class JmcFxLauncherPackagingTest {

    private static final String MAVEN_NAMESPACE = "http://maven.apache.org/POM/4.1.0";

    @Test
    void appPomKeepsExplicitLeydenInstallerProfileAndReservesNativeNamingForGraalVm() throws Exception {
        var pom = readAppPom();
        var profiles = pom.getElementsByTagNameNS(MAVEN_NAMESPACE, "profile");
        var profileIds = new java.util.HashSet<String>();
        var jpackageProfiles = 0;
        for (var i = 0; i < profiles.getLength(); i++) {
            var profile = (Element) profiles.item(i);
            var profileId = childText(profile, "id");
            profileIds.add(profileId);
            if (profileId.startsWith("jpackage")) {
                jpackageProfiles++;
            }
        }
        assertEquals(1, jpackageProfiles);
        assertFalse(profileIds.stream().anyMatch(profileId -> profileId.startsWith("native")),
                "native profile names should be reserved for future GraalVM native-image packaging");
        assertFalse(profileIds.contains("jpackage-classpath-jlink"),
                "the non-Leyden jpackage fallback should be removed once Leyden is the only installer path");
        assertNotNull(leydenJpackageProfile(pom));
        assertFalse(profileIds.contains("jpackage-classpath-jlink-leyden-macos"));
        assertFalse(profileIds.contains("jpackage-classpath-jlink-leyden-linux"));
        assertFalse(profileIds.contains("jpackage-classpath-jlink-leyden-windows"));
    }

    @Test
    void leydenInstallerProfileDocumentsWhyPackagingIsNotInDefaultBuild() throws Exception {
        var pomText = java.nio.file.Files.readString(Path.of("pom.xml"));

        assertTrue(pomText.contains("Leyden packaging intentionally stays behind an explicit profile"),
                "POM should explain why package/verify do not build installers by default");
        assertTrue(pomText.contains("The standalone io.github.youngledo:jpackage-maven-plugin provides a Leyden-enhanced goal"),
                "POM should explain that platform details are handled by the packager plugin");
    }

    @Test
    void appPomUsesMaven4SubprojectSyntaxOnly() throws Exception {
        var pomText = java.nio.file.Files.readString(Path.of("pom.xml"));

        assertTrue(pomText.contains("<modelVersion>4.1.0</modelVersion>"));
        assertFalse(pomText.contains("<modules>"));
        assertFalse(pomText.contains("<module>"));
    }

    @Test
    void leydenJpackageProfileDelegatesWorkflowToPackagerPlugin() throws Exception {
        var pom = readAppPom();
        var profile = leydenJpackageProfile(pom);

        assertEquals("1.0.0", childText(profile, "properties", "jmcfx.package.version"));
        assertFalse(profile.getTextContent().contains("Contents/MacOS"),
                "launcher profile should not hard-code an OS-specific app-image layout");
        assertNull(findPlugin(profile, "org.panteleyev", "jlink-maven-plugin"));
        assertNull(findPlugin(profile, "org.panteleyev", "jpackage-maven-plugin"));
        assertNull(findPlugin(profile, "org.apache.maven.plugins", "maven-antrun-plugin"));

        var packagerPlugin = findPlugin(profile, "io.github.youngledo",
                "jpackage-maven-plugin");
        assertNotNull(packagerPlugin);
        var execution = findExecution(packagerPlugin, "leyden-jpackage-installer");
        assertNotNull(execution);
        assertEquals("package", childText(execution, "phase"));
        assertEquals("leyden", childText(execution, "goals", "goal"));
        assertEquals("JMC FX", childText(execution, "configuration", "name"));
        assertEquals("io.github.youngledo.jmcfx.launcher.JmcFxLauncher",
                childText(execution, "configuration", "mainClass"));
        assertEquals("${project.build.finalName}.jar", childText(execution, "configuration", "mainJar"));
        assertEquals("${jmcfx.package.version}", childText(execution, "configuration", "packageVersion"));
        assertEquals("jmcfx.leyden.training", childText(execution, "configuration", "trainingProperty"));
        assertEquals("jmcfx-startup.aot", childText(execution, "configuration", "aotCacheName"));
        assertEquals("${project.build.directory}/jpackage-leyden-input",
                childText(execution, "configuration", "inputDirectory"));
        assertEquals("${project.build.directory}/jpackage-leyden-runtime",
                childText(execution, "configuration", "runtimeDirectory"));
        assertEquals("${project.build.directory}/jpackage-leyden-app-image",
                childText(execution, "configuration", "appImageDirectory"));
        assertEquals("${project.build.directory}/jpackage-leyden",
                childText(execution, "configuration", "packageDirectory"));
        assertEquals("${project.basedir}/src/main/jpackage",
                childText(execution, "configuration", "resourceDir"));
        assertConfiguredModules(packagerPlugin,
                "java.desktop",
                "java.management",
                "java.naming",
                "java.net.http",
                "java.rmi",
                "java.sql",
                "jdk.attach",
                "jdk.jfr",
                "jdk.management.agent",
                "jdk.unsupported",
                "javafx.controls");
        assertTrue(execution.getTextContent().contains("--enable-native-access=javafx.graphics"));

        var pomText = java.nio.file.Files.readString(Path.of("pom.xml"));
        assertFalse(pomText.contains("measure-macos-app-startup"),
                "formal Leyden packaging must not depend on measurement shell scripts");
    }

    @Test
    void launcherPackagesUseProjectSpecificIcons() throws Exception {
        assertTrue(java.nio.file.Files.isRegularFile(Path.of("src/main/jpackage/JMC FX.icns")),
                "macOS jpackage resource override should provide the JMC FX application icon");
        assertTrue(java.nio.file.Files.isRegularFile(Path.of("src/main/jpackage/JMC FX-volume.icns")),
                "macOS DMG volume icon should not fall back to the default Java package icon");
        assertTrue(java.nio.file.Files.isRegularFile(Path.of("src/main/jpackage/JMC FX.png")),
                "shared icon source should be available for Linux and release metadata");
        assertTrue(java.nio.file.Files.isRegularFile(Path.of("src/main/jpackage/jmc-fx-icon.svg")),
                "vector source should be kept with generated application icons");
        assertTrue(java.nio.file.Files.isRegularFile(Path.of("src/main/resources/icons/jmc-fx-icon.png")),
                "launcher runtime icon should be available on the application classpath");

        var launcherSource = java.nio.file.Files.readString(
                Path.of("src/main/java/io/github/youngledo/jmcfx/launcher/JmcFxLauncher.java"));
        assertTrue(launcherSource.contains("\"/icons/jmc-fx-icon.png\""),
                "JavaFX stage icon should use the project-specific icon resource");
        assertTrue(launcherSource.contains("stage.getIcons().add"),
                "ordinary JavaFX launches should not fall back to the default JavaFX window icon");
    }

    @Test
    void launcherHasLeydenTrainingHookWithoutStartupMeasurementProbe() throws Exception {
        var launcherSource = java.nio.file.Files.readString(
                Path.of("src/main/java/io/github/youngledo/jmcfx/launcher/JmcFxLauncher.java"));

        assertTrue(launcherSource.contains("\"jmcfx.leyden.training\""),
                "Maven-driven Leyden training needs an opt-in launcher exit hook");
        assertTrue(launcherSource.contains("Platform.runLater(Platform::exit)"),
                "training mode should exit after JavaFX has shown the initial window");
        assertFalse(launcherSource.contains("JMCFX_STARTUP"),
                "formal Leyden packaging should not keep the experimental startup measurement probe");
    }

    private static Document readAppPom() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(Path.of("pom.xml").toFile());
    }

    private static Element leydenJpackageProfile(Document pom) {
        var profile = profile(pom, "jpackage-classpath-jlink-leyden");
        assertNotNull(profile, "jmc-fx-launcher should expose jpackage-classpath-jlink-leyden");
        return profile;
    }

    private static Element profile(Document pom, String id) {
        var profile = findElementByChildText(
                pom.getElementsByTagNameNS(MAVEN_NAMESPACE, "profile"), "id", id);
        return profile;
    }

    private static Element findPlugin(Element root, String groupId, String artifactId) {
        var plugins = root.getElementsByTagNameNS(MAVEN_NAMESPACE, "plugin");
        for (var i = 0; i < plugins.getLength(); i++) {
            var plugin = (Element) plugins.item(i);
            if (groupId.equals(childText(plugin, "groupId")) && artifactId.equals(childText(plugin, "artifactId"))) {
                return plugin;
            }
        }
        return null;
    }

    private static Element findElementByChildText(NodeList elements, String childName, String expectedText) {
        for (var i = 0; i < elements.getLength(); i++) {
            var element = (Element) elements.item(i);
            if (expectedText.equals(childText(element, childName))) {
                return element;
            }
        }
        return null;
    }

    private static Element findExecution(Element plugin, String executionId) {
        return findElementByChildText(
                plugin.getElementsByTagNameNS(MAVEN_NAMESPACE, "execution"), "id", executionId);
    }

    private static void assertConfiguredModules(Element plugin, String... expectedModules) {
        var modules = plugin.getElementsByTagNameNS(MAVEN_NAMESPACE, "runtimeModule");
        var configuredModules = new java.util.HashSet<String>();
        for (var i = 0; i < modules.getLength(); i++) {
            configuredModules.add(modules.item(i).getTextContent().trim());
        }
        for (var expectedModule : expectedModules) {
            assertTrue(configuredModules.contains(expectedModule), () -> "missing jlink module " + expectedModule);
        }
    }

    private static String childText(Element element, String... path) {
        Element current = element;
        for (var name : path) {
            current = directChild(current, name);
            if (current == null) {
                return "";
            }
        }
        return current.getTextContent().trim();
    }

    private static Element directChild(Element element, String name) {
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element childElement && MAVEN_NAMESPACE.equals(childElement.getNamespaceURI())
                    && name.equals(childElement.getLocalName())) {
                return childElement;
            }
        }
        return null;
    }
}
