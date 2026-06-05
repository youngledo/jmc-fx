package io.github.youngledo.jmcfx.application;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class JmcFxApplicationArchitectureTest {

    private static final String OPENJDK_JMC_PACKAGE = "org.openjdk." + "jmc..";

    private static final JavaClasses APPLICATION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.youngledo.jmcfx.application");

    @Test
    void applicationLayerDependsOnlyOnDomainAndJdkTypes() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "javafx..",
                        "atlantafx..",
                        "org.kordamp.ikonli..",
                        OPENJDK_JMC_PACKAGE,
                        "org.jolokia..",
                        "..adapter..",
                        "..ui..",
                        "..launcher..")
                .check(APPLICATION_CLASSES);
    }
}
