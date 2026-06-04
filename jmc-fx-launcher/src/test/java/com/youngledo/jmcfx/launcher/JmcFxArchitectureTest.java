package com.youngledo.jmcfx.launcher;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class JmcFxArchitectureTest {

    private static final String OPENJDK_JMC_PACKAGE = "org.openjdk." + "jmc..";

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.youngledo.jmcfx");

    @Test
    void domainStaysIndependentOfAdaptersUiAndFrameworks() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..adapter..",
                        "..ui..",
                        "..launcher..",
                        "javafx..",
                        "atlantafx..",
                        "org.kordamp.ikonli..",
                        OPENJDK_JMC_PACKAGE,
                        "org.jolokia..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void uiDoesNotDependOnSecondaryAdaptersOrLauncher() {
        noClasses()
                .that().resideInAPackage("..ui..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..adapter..",
                        "..launcher..",
                        OPENJDK_JMC_PACKAGE,
                        "org.jolokia..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void jmcAdapterDoesNotDependOnUiLauncherOrJavaFx() {
        noClasses()
                .that().resideInAPackage("..adapter.jmc..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..ui..",
                        "..launcher..",
                        "javafx..",
                        "atlantafx..",
                        "org.kordamp.ikonli..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void reusableFlamegraphDoesNotDependOnApplicationLayersOrJmcApis() {
        noClasses()
                .that().resideInAPackage("..flamegraph..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..domain..",
                        "..ui..",
                        "..adapter..",
                        "..launcher..",
                        OPENJDK_JMC_PACKAGE,
                        "org.jolokia..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void launcherIsNotUsedByOtherProductionModules() {
        noClasses()
                .that().resideOutsideOfPackage("..launcher..")
                .should().dependOnClassesThat().resideInAPackage("..launcher..")
                .check(PRODUCTION_CLASSES);
    }
}
