package com.youngledo.jmcfx.flamegraph.demo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FlameGraphDemoApplicationTest {

    @Test
    void demoUsesReusableFlameGraphModuleOnly() throws Exception {
        Path source = Path.of("src/main/java/com/youngledo/jmcfx/flamegraph/demo/FlameGraphDemoApplication.java");
        if (!Files.isRegularFile(source)) {
            source = Path.of("jmc-fx-flamegraph-demo/src/main/java/com/youngledo/jmcfx/flamegraph/demo/FlameGraphDemoApplication.java");
        }

        String text = Files.readString(source);

        assertTrue(text.contains("com.youngledo.jmcfx.flamegraph.FlameGraphView"));
        assertTrue(!text.contains("com.youngledo.jmcfx.domain"));
        assertTrue(!text.contains("com.youngledo.jmcfx.ui"));
        assertTrue(!text.contains("org.openjdk.jmc"));
    }
}
