package io.github.youngledo.jmcfx.flamegraph;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class FlameGraphModuleBoundaryTest {

    private static final List<String> FORBIDDEN_DEPENDENCIES = List.of(
            "io.github.youngledo.jmcfx.domain",
            "io.github.youngledo.jmcfx.ui",
            "io.github.youngledo.jmcfx.adapter",
            "org.openjdk.jmc");

    @Test
    void reusableModuleDoesNotDependOnJmcFxApplicationLayersOrJmcApis() throws IOException {
        Path sourceRoot = sourceRoot();
        List<Path> sources;
        try (var stream = Files.walk(sourceRoot)) {
            sources = stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }

        for (Path source : sources) {
            String text = Files.readString(source);
            for (String forbidden : FORBIDDEN_DEPENDENCIES) {
                assertTrue(!text.contains(forbidden), source + " must not depend on " + forbidden);
            }
        }
    }

    @Test
    void demoApplicationStaysInFlameGraphTestSources() throws IOException {
        Path projectRoot = projectRoot();

        assertTrue(
                Files.isRegularFile(projectRoot.resolve(
                        "jmc-fx-flamegraph/src/test/java/io/github/youngledo/jmcfx/flamegraph/demo/FlameGraphDemoApplication.java")),
                "flame graph demo should live in flamegraph test sources");
        assertTrue(
                Files.notExists(projectRoot.resolve("jmc-fx-flamegraph-demo")),
                "flame graph demo should not be a separate Maven subproject");
        assertTrue(
                !Files.readString(projectRoot.resolve("pom.xml")).contains("<subproject>jmc-fx-flamegraph-demo</subproject>"),
                "root reactor should not include the flame graph demo subproject");
    }

    private Path sourceRoot() {
        Path direct = Path.of("src/main/java");
        if (Files.isDirectory(direct.resolve("io/github/youngledo/jmcfx/flamegraph"))) {
            return direct;
        }
        return Path.of("jmc-fx-flamegraph/src/main/java");
    }

    private Path projectRoot() {
        Path direct = Path.of(".");
        if (Files.isRegularFile(direct.resolve("pom.xml"))
                && Files.isDirectory(direct.resolve("jmc-fx-flamegraph"))) {
            return direct;
        }
        return Path.of("..");
    }
}
