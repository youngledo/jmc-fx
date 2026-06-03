package com.youngledo.jmcfx.flamegraph;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class FlameGraphModuleBoundaryTest {

    private static final List<String> FORBIDDEN_DEPENDENCIES = List.of(
            "com.youngledo.jmcfx.domain",
            "com.youngledo.jmcfx.ui",
            "com.youngledo.jmcfx.adapter",
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

    private Path sourceRoot() {
        Path direct = Path.of("src/main/java");
        if (Files.isDirectory(direct.resolve("com/youngledo/jmcfx/flamegraph"))) {
            return direct;
        }
        return Path.of("jmc-fx-flamegraph/src/main/java");
    }
}
