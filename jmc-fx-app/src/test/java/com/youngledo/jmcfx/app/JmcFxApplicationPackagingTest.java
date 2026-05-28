package com.youngledo.jmcfx.app;

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

class JmcFxApplicationPackagingTest {

    private static final String MAVEN_NAMESPACE = "http://maven.apache.org/POM/4.1.0";

    @Test
    void nativePackageProfileStagesRuntimeDependenciesForJpackage() throws Exception {
        var pom = readAppPom();

        var profile = findElementByChildText(pom.getElementsByTagNameNS(MAVEN_NAMESPACE, "profile"), "id", "native-package");
        assertNotNull(profile, "jmc-fx-app should expose a native-package profile");

        var dependencyPlugin = findPlugin(profile, "org.apache.maven.plugins", "maven-dependency-plugin");
        assertNotNull(dependencyPlugin, "native-package should stage runtime dependencies for jpackage");
        assertEquals(
                "copy-dependencies",
                childText(dependencyPlugin, "executions", "execution", "goals", "goal"),
                "runtime dependencies should be copied during package");
        assertEquals(
                "runtime",
                childText(dependencyPlugin, "executions", "execution", "configuration", "includeScope"),
                "only runtime dependencies should be staged");
        assertEquals(
                "${jmcfx.package.input.dir}",
                childText(dependencyPlugin, "executions", "execution", "configuration", "outputDirectory"));
    }

    @Test
    void nativePackageProfileConfiguresPanteleyevJpackagePlugin() throws Exception {
        var pom = readAppPom();
        var profile = findElementByChildText(pom.getElementsByTagNameNS(MAVEN_NAMESPACE, "profile"), "id", "native-package");

        var jpackagePlugin = findPlugin(profile, "org.panteleyev", "jpackage-maven-plugin");
        assertNotNull(jpackagePlugin, "native-package should configure org.panteleyev:jpackage-maven-plugin");
        assertEquals("1.7.4", childText(jpackagePlugin, "version"));
        assertEquals("JMC FX", childText(jpackagePlugin, "configuration", "name"));
        assertEquals("${jmcfx.package.version}", childText(jpackagePlugin, "configuration", "appVersion"));
        assertEquals("Youngledo", childText(jpackagePlugin, "configuration", "vendor"));
        assertEquals("${project.description}", childText(jpackagePlugin, "configuration", "description"));
        assertEquals("${jmcfx.package.input.dir}", childText(jpackagePlugin, "configuration", "input"));
        assertEquals("${project.build.finalName}.jar", childText(jpackagePlugin, "configuration", "mainJar"));
        assertEquals("com.youngledo.jmcfx.app.JmcFxApplication", childText(jpackagePlugin, "configuration", "mainClass"));
        assertEquals("${project.build.directory}/jpackage", childText(jpackagePlugin, "configuration", "destination"));
        assertEquals("${java.home}", childText(jpackagePlugin, "configuration", "runtimeImage"));
        assertEquals("true", childText(jpackagePlugin, "configuration", "removeDestination"));
        assertEquals("true", childText(jpackagePlugin, "configuration", "verbose"));
        assertEquals(
                "${project.build.directory}/jpackage-input",
                childText(profile, "properties", "jmcfx.package.input.dir"),
                "staged input must be outside the removable jpackage destination");
        assertEquals(
                "1.0.0",
                childText(profile, "properties", "jmcfx.package.version"),
                "default native package version must satisfy macOS CFBundleVersion rules");
    }

    @Test
    void nativePackageProfileUsesMaven4SubprojectSyntaxOnly() throws Exception {
        var pomText = java.nio.file.Files.readString(Path.of("pom.xml"));

        assertTrue(pomText.contains("<modelVersion>4.1.0</modelVersion>"));
        assertFalse(pomText.contains("<modules>"));
        assertFalse(pomText.contains("<module>"));
    }

    private static Document readAppPom() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(Path.of("pom.xml").toFile());
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
