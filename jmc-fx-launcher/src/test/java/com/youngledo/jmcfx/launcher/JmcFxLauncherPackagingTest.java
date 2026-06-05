package com.youngledo.jmcfx.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void appPomKeepsOnlyLeydenInstallerProfilesAndReservesNativeNamingForGraalVm() throws Exception {
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
        assertEquals(4, jpackageProfiles);
        assertFalse(profileIds.stream().anyMatch(profileId -> profileId.startsWith("native")),
                "native profile names should be reserved for future GraalVM native-image packaging");
        assertFalse(profileIds.contains("jpackage-classpath-jlink"),
                "the non-Leyden jpackage fallback should be removed once Leyden is the only installer path");
        assertNotNull(leydenJpackageProfile(pom));
        assertNotNull(profile(pom, "jpackage-classpath-jlink-leyden-macos"));
        assertNotNull(profile(pom, "jpackage-classpath-jlink-leyden-linux"));
        assertNotNull(profile(pom, "jpackage-classpath-jlink-leyden-windows"));
    }

    @Test
    void leydenInstallerProfileDocumentsWhyPackagingIsNotInDefaultBuild() throws Exception {
        var pomText = java.nio.file.Files.readString(Path.of("pom.xml"));

        assertTrue(pomText.contains("Leyden packaging intentionally stays behind an explicit profile"),
                "POM should explain why package/verify do not build installers by default");
        assertTrue(pomText.contains("The macOS, Linux, and Windows Leyden profiles are OS-activated"),
                "POM should explain that platform profiles are selected automatically by Maven");
    }

    @Test
    void appPomUsesMaven4SubprojectSyntaxOnly() throws Exception {
        var pomText = java.nio.file.Files.readString(Path.of("pom.xml"));

        assertTrue(pomText.contains("<modelVersion>4.1.0</modelVersion>"));
        assertFalse(pomText.contains("<modules>"));
        assertFalse(pomText.contains("<module>"));
    }

    @Test
    void leydenJpackageProfileBuildsAppImageThenTrainsAotCacheBeforeFinalInstaller() throws Exception {
        var pom = readAppPom();
        var profile = leydenJpackageProfile(pom);

        assertEquals("${project.build.directory}/jpackage-leyden-app-image",
                childText(profile, "properties", "jmcfx.leyden.app.image.dir"));
        assertEquals("${project.build.directory}/jpackage-leyden",
                childText(profile, "properties", "jmcfx.leyden.package.dir"));
        assertEquals("", childText(profile, "properties", "jmcfx.leyden.app.image"),
                "common Leyden flow should not hard-code an OS-specific app-image layout");
        assertEquals("", childText(profile, "properties", "jmcfx.leyden.app.executable"),
                "common Leyden flow should not hard-code an OS-specific launcher path");
        assertEquals("", childText(profile, "properties", "jmcfx.leyden.app.config"),
                "common Leyden flow should not hard-code an OS-specific cfg path");
        assertFalse(profile.getTextContent().contains("Contents/MacOS"),
                "common Leyden flow must not be macOS-only");

        var antrunPlugin = findPlugin(profile, "org.apache.maven.plugins", "maven-antrun-plugin");
        assertNotNull(antrunPlugin, "Leyden packaging should train the AOT cache through Maven plugins");
        assertTrue(pluginIndex(profile, "org.panteleyev", "jpackage-maven-plugin")
                        < pluginIndex(profile, "org.apache.maven.plugins", "maven-antrun-plugin"),
                "Leyden training must run after the app-image has been generated");
        var trainExecution = findExecution(antrunPlugin, "train-leyden-aot-cache");
        assertNotNull(trainExecution);
        assertEquals("prepare-package", childText(trainExecution, "phase"));
        assertEquals("run", childText(trainExecution, "goals", "goal"));
        assertEquals("${jmcfx.leyden.app.executable}",
                trainExecution.getElementsByTagName("exec").item(0).getAttributes()
                        .getNamedItem("executable").getTextContent());
        var aotCacheReplace = findTaskByAttribute(trainExecution, "replace", "token",
                "java-options=-XX:AOTCacheOutput=$APPDIR/jmcfx-startup.aot");
        assertNotNull(aotCacheReplace);
        assertEquals("java-options=-XX:AOTCache=$APPDIR/jmcfx-startup.aot",
                aotCacheReplace.getAttribute("value"));
        var trainingPropertyReplace = findTaskByAttribute(trainExecution, "replace", "token",
                "java-options=-Djmcfx.leyden.training=true");
        assertNotNull(trainingPropertyReplace);
        assertEquals("", trainingPropertyReplace.getAttribute("value"));

        var jlinkPlugin = findPlugin(profile, "org.panteleyev", "jlink-maven-plugin");
        assertNotNull(jlinkPlugin, "Leyden packaging should create a trimmed runtime image");
        assertEquals("${jmcfx.package.runtime.dir}", childText(jlinkPlugin, "configuration", "output"));
        assertEquals("${jmcfx.package.input.dir}", childText(jlinkPlugin, "configuration", "modulePaths", "modulePath"));
        assertConfiguredModules(jlinkPlugin,
                "java.desktop",
                "java.management",
                "java.naming",
                "java.rmi",
                "java.sql",
                "jdk.attach",
                "jdk.jfr",
                "jdk.management.agent",
                "jdk.unsupported",
                "javafx.controls");

        var jpackagePlugin = findPlugin(profile, "org.panteleyev", "jpackage-maven-plugin");
        assertNotNull(jpackagePlugin, "Leyden packaging should use jpackage for app-image and installer output");
        var appImageExecution = findExecution(jpackagePlugin, "create-leyden-app-image");
        assertNotNull(appImageExecution);
        assertEquals("prepare-package", childText(appImageExecution, "phase"));
        assertEquals("APP_IMAGE", childText(appImageExecution, "configuration", "type"));
        assertEquals("${jmcfx.leyden.app.image.dir}",
                childText(appImageExecution, "configuration", "destination"));
        assertEquals("${jmcfx.package.runtime.dir}",
                childText(appImageExecution, "configuration", "runtimeImage"));
        assertTrue(appImageExecution.getTextContent().contains("-Djmcfx.leyden.training=true"));
        assertTrue(appImageExecution.getTextContent().contains("-XX:AOTCacheOutput=$APPDIR/jmcfx-startup.aot"));

        var installerExecution = findExecution(jpackagePlugin, "package-leyden-installer");
        assertNotNull(installerExecution,
                "final installer packaging should use jpackage-maven-plugin instead of Ant exec argument lists");
        assertEquals("package", childText(installerExecution, "phase"));
        assertEquals("${jmcfx.leyden.package.type}", childText(installerExecution, "configuration", "type"));
        assertEquals("${jmcfx.leyden.app.image}", childText(installerExecution, "configuration", "appImage"));
        assertEquals("${jmcfx.leyden.package.dir}", childText(installerExecution, "configuration", "destination"));

        var pomText = java.nio.file.Files.readString(Path.of("pom.xml"));
        assertFalse(pomText.contains("measure-macos-app-startup"),
                "formal Leyden packaging must not depend on measurement shell scripts");
    }

    @Test
    void leydenPostProcessingKeepsOnlyAotTrainingAndMacSigningInAntrun() throws Exception {
        var pom = readAppPom();
        var profile = leydenJpackageProfile(pom);
        var antrunPlugin = findPlugin(profile, "org.apache.maven.plugins", "maven-antrun-plugin");
        var trainExecution = findExecution(antrunPlugin, "train-leyden-aot-cache");

        var macSign = findTaskByAttribute(trainExecution, "exec", "osfamily", "mac");
        assertNotNull(macSign, "modified macOS app images should be signed before packaging");
        assertEquals("${jmcfx.leyden.sign.executable}", macSign.getAttribute("executable"));
        assertTaskHasArg(macSign, "${jmcfx.leyden.app.image}");
        assertFalse(trainExecution.getTextContent().contains("package-leyden-macos"),
                "Ant post-processing should not duplicate jpackage installer commands");
        assertFalse(trainExecution.getTextContent().contains("package-leyden-linux"),
                "Ant post-processing should not duplicate jpackage installer commands");
        assertFalse(trainExecution.getTextContent().contains("package-leyden-windows"),
                "Ant post-processing should not duplicate jpackage installer commands");
    }

    @Test
    void leydenOsProfilesDefinePlatformSpecificAppImageLayout() throws Exception {
        var pom = readAppPom();

        var macos = profile(pom, "jpackage-classpath-jlink-leyden-macos");
        assertEquals("mac", childText(macos, "activation", "os", "family"));
        assertEquals("${jmcfx.leyden.app.image.dir}/JMC FX.app",
                childText(macos, "properties", "jmcfx.leyden.app.image"));
        assertEquals("${jmcfx.leyden.app.image}/Contents/MacOS/JMC FX",
                childText(macos, "properties", "jmcfx.leyden.app.executable"));
        assertEquals("${jmcfx.leyden.app.image}/Contents/app/JMC FX.cfg",
                childText(macos, "properties", "jmcfx.leyden.app.config"));
        assertEquals("${jmcfx.leyden.app.image}/Contents/app/jmcfx-startup.aot",
                childText(macos, "properties", "jmcfx.leyden.aot.cache"));
        assertEquals("DMG", childText(macos, "properties", "jmcfx.leyden.package.type"));
        assertEquals("/usr/bin/codesign", childText(macos, "properties", "jmcfx.leyden.sign.executable"));

        var linux = profile(pom, "jpackage-classpath-jlink-leyden-linux");
        assertEquals("linux", childText(linux, "activation", "os", "family"));
        assertEquals("${jmcfx.leyden.app.image.dir}/JMC FX",
                childText(linux, "properties", "jmcfx.leyden.app.image"));
        assertEquals("${jmcfx.leyden.app.image}/bin/JMC FX",
                childText(linux, "properties", "jmcfx.leyden.app.executable"));
        assertEquals("${jmcfx.leyden.app.image}/lib/app/JMC FX.cfg",
                childText(linux, "properties", "jmcfx.leyden.app.config"));
        assertEquals("${jmcfx.leyden.app.image}/lib/app/jmcfx-startup.aot",
                childText(linux, "properties", "jmcfx.leyden.aot.cache"));
        assertEquals("DEB", childText(linux, "properties", "jmcfx.leyden.package.type"));

        var windows = profile(pom, "jpackage-classpath-jlink-leyden-windows");
        assertEquals("windows", childText(windows, "activation", "os", "family"));
        assertEquals("${jmcfx.leyden.app.image.dir}/JMC FX",
                childText(windows, "properties", "jmcfx.leyden.app.image"));
        assertEquals("${jmcfx.leyden.app.image}/JMC FX.exe",
                childText(windows, "properties", "jmcfx.leyden.app.executable"));
        assertEquals("${jmcfx.leyden.app.image}/app/JMC FX.cfg",
                childText(windows, "properties", "jmcfx.leyden.app.config"));
        assertEquals("${jmcfx.leyden.app.image}/app/jmcfx-startup.aot",
                childText(windows, "properties", "jmcfx.leyden.aot.cache"));
        assertEquals("MSI", childText(windows, "properties", "jmcfx.leyden.package.type"));
    }

    @Test
    void launcherHasLeydenTrainingHookWithoutStartupMeasurementProbe() throws Exception {
        var launcherSource = java.nio.file.Files.readString(
                Path.of("src/main/java/com/youngledo/jmcfx/launcher/JmcFxLauncher.java"));

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

    private static int pluginIndex(Element root, String groupId, String artifactId) {
        var plugins = root.getElementsByTagNameNS(MAVEN_NAMESPACE, "plugin");
        for (var i = 0; i < plugins.getLength(); i++) {
            var plugin = (Element) plugins.item(i);
            if (groupId.equals(childText(plugin, "groupId")) && artifactId.equals(childText(plugin, "artifactId"))) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
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

    private static Element findTaskByAttribute(Element root, String taskName, String attributeName, String expectedValue) {
        var tasks = root.getElementsByTagName(taskName);
        for (var i = 0; i < tasks.getLength(); i++) {
            var task = (Element) tasks.item(i);
            if (expectedValue.equals(task.getAttribute(attributeName))) {
                return task;
            }
        }
        return null;
    }

    private static void assertTaskHasArg(Element task, String expectedValue) {
        var args = task.getElementsByTagName("arg");
        for (var i = 0; i < args.getLength(); i++) {
            var arg = (Element) args.item(i);
            if (expectedValue.equals(arg.getAttribute("value"))) {
                return;
            }
        }
        throw new AssertionError("missing Ant arg value " + expectedValue);
    }

    private static void assertConfiguredModules(Element plugin, String... expectedModules) {
        var modules = plugin.getElementsByTagNameNS(MAVEN_NAMESPACE, "addModule");
        var configuredModules = new java.util.HashSet<String>();
        for (var i = 0; i < modules.getLength(); i++) {
            configuredModules.add(modules.item(i).getTextContent().trim());
        }
        for (var expectedModule : expectedModules) {
            assertTrue(configuredModules.contains(expectedModule), () -> "missing jlink module " + expectedModule);
        }
    }

    private static void assertConfiguredFilesets(Element plugin, String... expectedDirectories) {
        var directories = plugin.getElementsByTagNameNS(MAVEN_NAMESPACE, "directory");
        var configuredDirectories = new java.util.HashSet<String>();
        for (var i = 0; i < directories.getLength(); i++) {
            configuredDirectories.add(directories.item(i).getTextContent().trim());
        }
        for (var expectedDirectory : expectedDirectories) {
            assertTrue(configuredDirectories.contains(expectedDirectory),
                    () -> "missing clean fileset " + expectedDirectory);
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
